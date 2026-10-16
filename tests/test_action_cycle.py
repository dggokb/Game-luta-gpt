import copy
import unittest
import cv2
import numpy as np
from sprite_pipeline.action_cycle import extract_cycle,register_patch,cycle_transforms
from sprite_pipeline.errors import CalibrationError


class ActionCycleTests(unittest.TestCase):
    def test_trim_preserves_original_ids_pts_and_durations_without_mutation(self):
        original={'frame_count':6,'frames':[{'index':i,'pts':p,'time_s':t,'duration_s':d}
            for i,(p,t,d) in enumerate([(0,0,.05),(50,.05,.04),(90,.09,.06),
                                       (150,.15,.04),(190,.19,.05),(240,.24,.04)])]}
        saved=copy.deepcopy(original)
        cycle=extract_cycle(original,1,5)
        self.assertEqual(original,saved)
        self.assertEqual([f['pts'] for f in cycle['frames']],[50,90,150,190])
        self.assertEqual([f['source_frame_index'] for f in cycle['frames']],[1,2,3,4])
        np.testing.assert_allclose([f['time_s'] for f in cycle['frames']],[0,.04,.10,.14])
        self.assertAlmostEqual(cycle['duration_s'],.19)
        self.assertTrue(cycle['source_selection']['playback_times_rebased'])
        for interval in [(0,7),(True,4),(4,2),(0,2)]:
            with self.assertRaises(CalibrationError):extract_cycle(original,*interval)

    def test_known_growth_is_measured_inside_the_action(self):
        rng=np.random.default_rng(52)
        texture=cv2.GaussianBlur(rng.integers(0,256,(110,110,3),dtype=np.uint8),(5,5),0)
        images=[]
        for scale,dx in [(.96,-2),(1,0),(1.04,2)]:
            m=cv2.getRotationMatrix2D((55,55),0,scale);m[0,2]+=dx
            images.append(cv2.warpAffine(texture,m,(110,110)))
        fitted=register_patch(images,1,[[35,35],[75,35],[75,75],[35,75]],minimum_correlation=.95)
        np.testing.assert_allclose([f['scale_to_action_reference'] for f in fitted],[.96,1,1.04],atol=.008)
        self.assertTrue(all(f['correlation']>.95 for f in fitted))

    def test_uncertain_matches_fail_instead_of_using_a_silhouette_or_idle(self):
        rng=np.random.default_rng(3)
        a=cv2.GaussianBlur(rng.integers(0,256,(100,100,3),dtype=np.uint8),(5,5),0)
        with self.assertRaises(CalibrationError):
            register_patch([a,np.zeros_like(a),a],0,[[30,30],[60,30],[60,60],[30,60]])

    def test_internal_stabilization_and_constant_global_scale_are_separate(self):
        fitted=[{'scale_to_action_reference':s,'patch_center_source_px':[100+i,50]}
                for i,s in enumerate([1,1.05,1.05,1.05,1])]
        a,report=cycle_transforms(fitted,.6,.85,200)
        b,_=cycle_transforms(fitted,.6,1,200)
        for fit,m,n in zip(fitted,a,b):
            self.assertAlmostEqual(m[0][0]/n[0][0],.85)
            self.assertAlmostEqual(fit['scale_to_action_reference']*m[0][0],.6*.85)
            self.assertAlmostEqual(fit['patch_center_source_px'][0]*m[0][0]+m[0][2],0)
            self.assertEqual(m[0][0],m[1][1])
            self.assertEqual(m[0][1],m[1][0])
            self.assertAlmostEqual(180*m[1][1]+m[1][2],-20*m[1][1])
        self.assertEqual(report['local_reference'],'running_pose_only')


class DashTrialGateTests(unittest.TestCase):
    def test_large_source_translation_can_be_registered_without_changing_the_scale(self):
        rng=np.random.default_rng(8)
        image=np.zeros((140,360,3),np.uint8)
        texture=cv2.GaussianBlur(rng.integers(0,256,(50,50,3),dtype=np.uint8),(5,5),0)
        image[40:90,30:80]=texture
        moved=cv2.warpAffine(image,np.array([[1,0,180],[0,1,0]],float),(360,140))
        fitted=register_patch([image,moved],0,[[35,45],[75,45],[75,85],[35,85]],
                              search_radius_px=250,minimum_correlation=.95)
        self.assertAlmostEqual(fitted[1]['scale_to_action_reference'],1,delta=.008)
        self.assertAlmostEqual(fitted[1]['patch_center_source_px'][0]-fitted[0]['patch_center_source_px'][0],180,delta=.5)

    def test_one_global_axis_factor_does_not_change_the_internal_curve(self):
        from sprite_pipeline.action_cycle import global_axis_factor
        a=global_axis_factor([[0,0],[0,80]],.75,[[0,0],[0,100]],1)
        b=global_axis_factor([[0,0],[0,80]],1.5,[[0,0],[0,100]],1)
        self.assertAlmostEqual(a,.6);self.assertAlmostEqual(b,1.2)
        with self.assertRaises(CalibrationError):global_axis_factor([[0,0],[0,2]],1,[[0,0],[0,100]],1)

    def test_a_stable_face_does_not_hide_independent_body_growth(self):
        from sprite_pipeline.action_cycle import assess_cycle
        fitted=[{'scale_to_action_reference':1.,'correlation':.95} for _ in range(4)]
        matrices=[[[1,0,0],[0,1,0]] for _ in fitted]
        report=assess_cycle(fitted,matrices,[{'output_length_px':100},{'output_length_px':112}])
        self.assertEqual(report['status'],'trial_gates_failed')
        self.assertIn('independent_body_control',report['failure_reasons'])
        self.assertFalse(report['full_frame_anatomical_validation'])

    def test_residual_deformation_and_low_confidence_are_not_approved(self):
        from sprite_pipeline.action_cycle import assess_cycle
        fitted=[{'scale_to_action_reference':s,'correlation':.95,'patch_center_source_px':[100,100]}
                for s in (1,1,1.06,1,1)]
        matrices,_=cycle_transforms(fitted,1,1,200)
        controls=[{'output_length_px':100},{'output_length_px':100}]
        report=assess_cycle(fitted,matrices,controls)
        self.assertIn('local_patch_residual',report['failure_reasons'])
        fitted[2]['scale_to_action_reference']=1;fitted[2]['correlation']=.72
        report=assess_cycle(fitted,matrices,controls)
        self.assertIn('registration_confidence',report['failure_reasons'])
        self.assertFalse(report['approved_for_all_characters'])


if __name__=='__main__':unittest.main()
