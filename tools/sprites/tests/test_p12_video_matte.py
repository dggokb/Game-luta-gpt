"""Matte recovery copies no new art and is bound to the exact decoded frame."""
import hashlib
import sys
import unittest
from pathlib import Path

import cv2
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from p12_video_sources import restore_original_matte
from video_para_sprite import key


class SourceMatteTest(unittest.TestCase):
    def setUp(self):
        # Saturated green background; a muted green neck disconnects the head
        # in the old HSV matte. The source RGB itself still contains both.
        rgb = np.full((100, 100, 3), [0, 210, 0], dtype=np.uint8)
        rgb[40:90, 20:80] = [150, 120, 90]
        rgb[8:24, 36:64] = [100, 80, 65]
        rgb[24:41, 43:57] = [70, 100, 70]
        self.bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
        self.policy = {'method': 'restore-muted-green-alpha-v1',
                       'decodedRgbSha256': hashlib.sha256(rgb.tobytes()).hexdigest(),
                       'regions': [[30, 3, 70, 43]]}

    def test_recovers_existing_head_and_neck_without_changing_rgb(self):
        before = key(self.bgr)
        after = restore_original_matte(self.bgr, self.policy)
        self.assertEqual(int(before[15, 50, 3]), 0)
        self.assertEqual(int(after[15, 50, 3]), 255)
        self.assertEqual(int(after[30, 50, 3]), 255)
        self.assertEqual(int(after[5, 32, 3]), 0)
        np.testing.assert_array_equal(after[..., :3], before[..., :3])
        np.testing.assert_array_equal(after[45:], before[45:])
        self.assertTrue(np.all(after[..., 3] >= before[..., 3]))

    def test_wrong_decoded_source_is_rejected(self):
        self.bgr[0, 0, 0] = 1
        with self.assertRaisesRegex(ValueError, 'different original video pixels'):
            restore_original_matte(self.bgr, self.policy)

    def test_unbounded_or_unreviewed_region_is_rejected(self):
        for regions in ([], [[-1, 0, 50, 50]], [[0, 0, 101, 90]], [[0, 0, 1.5, 10]]):
            with self.subTest(regions=regions), self.assertRaises(ValueError):
                restore_original_matte(self.bgr, dict(self.policy, regions=regions))


if __name__ == '__main__':
    unittest.main()
