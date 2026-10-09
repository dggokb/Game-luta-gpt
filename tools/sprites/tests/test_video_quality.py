import sys
import unittest
from pathlib import Path
import numpy as np

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from video_quality import inspect,choose_frames,write_report


def figure(x=18, head=6, width=70, height=60):
    img=np.zeros((height,width,4),dtype=np.uint8)
    img[9:24,x:x+head,:3]=[210,155,130]
    img[9:24,x:x+head,3]=255
    img[24:54,x-4:x+head+4,:3]=[50,65,180]
    img[24:54,x-4:x+head+4,3]=255
    return img


class VideoQualityTests(unittest.TestCase):
    def test_empty_frame_is_blocking_not_silent(self):
        rep=inspect([figure(),np.zeros_like(figure()),figure()],24)
        self.assertIn("empty_frame",[f["code"] for f in rep["flags"] if f["severity"]=="error"])

    def test_no_false_motion_error_for_smooth_walk(self):
        rep=inspect([figure(x=18+i) for i in range(6)],24)
        self.assertFalse([f for f in rep["flags"] if f["severity"]=="error"])
        self.assertEqual(6,rep["frameCount"])

    def test_catches_abrupt_head_flicker(self):
        rep=inspect([figure(head=6),figure(head=26),figure(head=6)],24)
        self.assertIn("head_pop",[f["code"] for f in rep["flags"]])

    def test_frame_selection_keeps_endpoints_and_impact(self):
        f=[{"width":8+i,"torso":i,"silhouette":np.full((3,3),i,dtype=np.uint8)} for i in range(9)]
        picks=choose_frames(f,4,required=[5])
        self.assertEqual(0,picks[0])
        self.assertEqual(8,picks[-1])
        self.assertIn(5,picks)

    def test_invalid_impact_is_error(self):
        rep=inspect([figure(),figure()],24,expected_impact=3)
        self.assertIn("impact_out_of_range",[f["code"] for f in rep["flags"]])


if __name__=="__main__":
    unittest.main()
