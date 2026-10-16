import copy
import unittest
from sprite_pipeline.triage import translate_registered_x,required_horizontal_padding


class SelectiveTranslationTests(unittest.TestCase):
    def test_only_x_changes_and_inputs_are_preserved(self):
        frames=[{'transform_source_to_output':[[.8,0,-100],[0,.8,-200]],'bbox_output':[5,10,90,120]},
                {'transform_source_to_output':[[.8,0,-140],[0,.8,-190]],'bbox_output':[2,12,90,122]}]
        saved=copy.deepcopy(frames);matrices,dx=translate_registered_x(frames,[200,300],70)
        self.assertEqual(frames,saved)
        for f,m,x in zip(frames,matrices,[200,300]):
            self.assertEqual(m[0][:2],f['transform_source_to_output'][0][:2])
            self.assertEqual(m[1],f['transform_source_to_output'][1])
            self.assertAlmostEqual(x*m[0][0]+m[0][2],70)
        self.assertEqual(dx,[10.,-30.])

    def test_padding_keeps_translated_effects_and_sprite_in_bounds(self):
        f=[{'bbox_output':[4,0,96,100]},{'bbox_output':[3,0,97,100]}]
        shifts=[-32,40];left,right=required_horizontal_padding(f,shifts,100)
        self.assertGreater(left,28);self.assertGreater(right,37)
        for frame,dx in zip(f,shifts):
            self.assertGreaterEqual(frame['bbox_output'][0]+dx+left,0)
            self.assertLessEqual(frame['bbox_output'][2]+dx+left,100+left+right)

    def test_unchanged_motion_requires_no_new_canvas(self):
        self.assertEqual(required_horizontal_padding([{'bbox_output':[1,1,99,99]}],[0],100),(0,0))


if __name__=='__main__':unittest.main()
