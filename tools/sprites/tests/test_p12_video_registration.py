import sys
import unittest
from pathlib import Path

import numpy as np
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from p12_video_registration import waist_row


class WaistLandmarkTest(unittest.TestCase):
    def test_sash_landmark_does_not_follow_unfolding_feet(self):
        short=np.zeros((100,50,4),dtype=np.uint8)
        short[10:60,10:40]=[130,110,90,255]
        short[40:45,10:40]=[115,35,30,255]
        long=short.copy()
        long[60:95,20:35]=[130,110,90,255]
        self.assertEqual(waist_row(short),waist_row(long))

    def test_missing_sash_is_rejected_instead_of_guessed(self):
        with self.assertRaisesRegex(ValueError,'not identifiable'):
            waist_row(np.full((20,20,4),255,dtype=np.uint8))
