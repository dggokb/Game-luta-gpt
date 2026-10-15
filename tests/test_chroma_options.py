"""Explicit treatment for the actors' desaturated green backgrounds."""
import unittest
import cv2
import numpy as np
from sprite_pipeline.imaging import image_rgba
from sprite_pipeline.errors import CalibrationError


class ChromaOptionsTests(unittest.TestCase):
    def setUp(self):
        self.rgb=np.array([[[102,120,82],[180,70,55]]],np.uint8)
        self.source=cv2.cvtColor(self.rgb,cv2.COLOR_RGB2BGR)

    def test_default_soft_matte_is_unchanged(self):
        rgba=image_rgba(self.source)
        np.testing.assert_array_equal(rgba[:,:,3],[[127,255]])
        np.testing.assert_array_equal(rgba[:,:,:3],self.rgb)

    def test_explicit_actor_matte_removes_muted_green_and_keeps_artwork_rgb(self):
        rgba=image_rgba(self.source,{'edge_mode':'hard','minimum_green_dominance':6})
        np.testing.assert_array_equal(rgba[:,:,3],[[0,255]])
        np.testing.assert_array_equal(rgba[:,:,:3],self.rgb)

    def test_invalid_matte_options_are_rejected(self):
        for value in (0,33,True,2.5):
            with self.subTest(value=value),self.assertRaises(CalibrationError):
                image_rgba(self.source,{'minimum_green_dominance':value})
        with self.assertRaises(CalibrationError):
            image_rgba(self.source,{'edge_mode':'unknown'})

    def test_neutral_exterior_key_preserves_matching_white_clothing_inside_outline(self):
        rgb=np.full((30,40,3),240,np.uint8)
        rgb[6:24,12:28]=20
        rgb[9:21,15:25]=245
        rgba=image_rgba(cv2.cvtColor(rgb,cv2.COLOR_RGB2BGR),{'mode':'border_color','tolerance':30})
        self.assertEqual(rgba[0,0,3],0)
        self.assertEqual(rgba[7,13,3],255)
        self.assertEqual(rgba[12,19,3],255)
        np.testing.assert_array_equal(rgba[12,19,:3],[245,245,245])

    def test_green_clothing_is_kept_without_restoring_background_holes_or_spill(self):
        rgb=np.full((40,50,3),[0,255,0],np.uint8)
        rgb[5:35,15:35]=[20,20,20]
        rgb[18:24,15:35]=[80,215,60]
        rgb[10:13,23:27]=[0,210,0]
        rgb[7,14]=[40,230,30]
        source=cv2.cvtColor(rgb,cv2.COLOR_RGB2BGR)
        baseline=image_rgba(source)
        protected=image_rgba(source,{'protect_green_art':True})
        self.assertEqual(baseline[20,25,3],0)
        self.assertEqual(protected[20,25,3],255)
        np.testing.assert_array_equal(protected[20,25,:3],[80,215,60])
        self.assertEqual(protected[11,24,3],0)
        self.assertEqual(protected[7,14,3],baseline[7,14,3])
        self.assertEqual(protected[0,0,3],0)
