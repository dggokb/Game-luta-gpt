"""The optimized bounded renderer must exactly match full-canvas OpenCV."""
import unittest
import cv2
import numpy as np
from sprite_pipeline.imaging import affine_rgba, affine_rgba_region


class RegionRasterTests(unittest.TestCase):
    def test_global_subpixel_rounding_and_premultiplied_edges(self):
        rng=np.random.default_rng(20261015)
        image=np.zeros((173,251,4),np.uint8)
        image[32:148,48:195]=rng.integers(0,256,(116,147,4),dtype=np.uint8)
        for scale in (.37,429/700,1.,1.23):
            for translation in ((-29.5,8.25),(41.375,-36.125),(20.,40.)):
                with self.subTest(scale=scale,translation=translation):
                    expected=affine_rgba(image,scale,translation,[280,240])
                    actual=affine_rgba_region(image,scale,translation,[280,240])
                    np.testing.assert_array_equal(actual,expected)
