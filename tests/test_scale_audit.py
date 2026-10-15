"""Cross-pose regressions: matching Idle must not hide growth during an action."""
import copy
import json
import os
from pathlib import Path
import unittest
import numpy as np
from sprite_pipeline.scale_audit import audit_scale, require_scale_audit
from sprite_pipeline.corrections import frame_transforms
from sprite_pipeline.errors import CalibrationError


class ScaleAuditTests(unittest.TestCase):
    def setUp(self):
        self.metadata={'source_sha256':'source','resolution':[300,300],'frame_count':3}
        self.matrices=[[[1,0,0],[0,1,0]] for _ in range(3)]
        self.setting={'source_sha256':'source','required_phases':['start','action','end'],
            'checkpoints':[{'frame':i,'phase':phase,'segments':{'bone':[[20,20],[20,20+length]]}}
                           for i,phase,length in ((0,'start',40),(1,'action',50),(2,'end',40))]}

    def test_equal_start_and_end_do_not_hide_growth_in_another_pose(self):
        result=audit_scale(self.setting,self.metadata,self.matrices)
        self.assertEqual(result['status'],'drift_detected')
        self.assertEqual(result['changes'],[{'frame':1,'phase':'action','segment':'bone','ratio':1.25}])
        with self.assertRaisesRegex(CalibrationError,'frame 1'):
            require_scale_audit(result)

    def test_rotation_and_translation_do_not_count_as_size_changes(self):
        self.setting['checkpoints'][1]['segments']['bone']=[[80,80],[120,80]]
        self.matrices[1]=[[0,-1,250],[1,0,-40]]
        self.assertEqual(audit_scale(self.setting,self.metadata,self.matrices)['status'],'within_annotation_tolerance')

    def test_output_matrices_are_checked_instead_of_assuming_correction_worked(self):
        self.matrices[1]=[[.8,0,120],[0,.8,-90]]
        result=audit_scale(self.setting,self.metadata,self.matrices)
        self.assertEqual(result['status'],'within_annotation_tolerance')
        self.assertFalse(result['full_frame_anatomical_validation'])
        self.assertEqual(result['checkpoint_count'],3)

    def test_unmeasured_clips_are_explicitly_unchecked(self):
        result=audit_scale(None,self.metadata,self.matrices)
        self.assertEqual(result['status'],'not_checked')
        self.assertFalse(result['full_frame_anatomical_validation'])

    def test_source_phase_coverage_and_landmarks_are_required(self):
        invalid=[]
        s=copy.deepcopy(self.setting);s['source_sha256']='wrong';invalid.append(s)
        s=copy.deepcopy(self.setting);s['checkpoints'].pop(1);invalid.append(s)
        s=copy.deepcopy(self.setting);s['checkpoints'][1]['phase']='start';invalid.append(s)
        s=copy.deepcopy(self.setting);s['checkpoints'][1]['frame']=False;invalid.append(s)
        s=copy.deepcopy(self.setting);s['checkpoints'][1]['segments']={'other':[[20,20],[20,60]]};invalid.append(s)
        s=copy.deepcopy(self.setting);s['checkpoints'][1]['segments']['bone']=[[20,20],[20,float('nan')]];invalid.append(s)
        s=copy.deepcopy(self.setting);s['required_phases']=[{}];invalid.append(s)
        s=copy.deepcopy(self.setting);s['tolerance_fraction']=True;invalid.append(s)
        s=copy.deepcopy(self.setting);s['checkpoints'][1]['segments']['bone']=[[20,20],[20,300]];invalid.append(s)
        for setting in invalid:
            with self.subTest(setting=setting),self.assertRaises(CalibrationError):
                audit_scale(setting,self.metadata,self.matrices)

    def test_uncorrected_secondary_part_can_still_block_a_single_part_correction(self):
        for key in self.setting['checkpoints']:
            key['segments']['second']=[[100,20],[100,60]]
        self.matrices[1]=[[.8,0,0],[0,.8,0]]
        result=audit_scale(self.setting,self.metadata,self.matrices)
        self.assertEqual(result['status'],'drift_detected')
        self.assertEqual(result['changes'][0]['segment'],'second')


@unittest.skipUnless(os.environ.get('SPRITE_P02_RESULT'),'Provide the original P02 collection result')
class RealP02DashTests(unittest.TestCase):
    def setUp(self):
        self.metadata=json.loads((Path(os.environ['SPRITE_P02_RESULT'])/'dash-metadata.json').read_text())
        root=Path(__file__).resolve().parents[1]
        self.setting=json.loads((root/'examples/collection/motion-landmarks/p02.json').read_text())['clips']['dash']

    def test_previously_passing_idle_hides_more_than_20_percent_action_growth(self):
        self.assertGreater(self.metadata['reference_similarity'],.97)
        self.assertEqual(self.metadata['peer_height_spread'],0)
        self.assertEqual(self.metadata['frames'][0]['bbox_source'],self.metadata['frames'][-1]['bbox_source'])
        result=audit_scale(self.setting['scale_audit'],self.metadata,
                           [f['transform_source_to_output'] for f in self.metadata['frames']])
        self.assertEqual(result['status'],'drift_detected')
        self.assertGreater(max(s['segments']['head_axis']['output_ratio_to_reference'] for s in result['samples']),1.24)

    def test_eight_unfitted_samples_and_separate_shoulders_check_corrected_output(self):
        matrices,_=frame_transforms(self.setting,self.metadata,self.metadata)
        result=audit_scale(self.setting['scale_audit'],self.metadata,matrices)
        self.assertEqual(result['status'],'within_annotation_tolerance')
        keys={k['frame'] for k in self.setting['scale_correction']['keyframes']}
        controls=[s for s in result['samples'] if s['purpose']=='holdout']
        self.assertEqual(len(controls),8)
        self.assertFalse(keys&{s['frame'] for s in controls})
        self.assertLess(max(abs(s['segments']['head_axis']['output_ratio_to_reference']-1) for s in controls),.06)
        self.assertEqual(sum('shoulder_span' in s['segments'] for s in result['samples']),5)
        for matrix in matrices:
            np.testing.assert_equal(matrix[0][0],matrix[1][1])


if __name__=='__main__':
    unittest.main()
