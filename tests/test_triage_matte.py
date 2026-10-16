import unittest
import cv2
import numpy as np
from sprite_pipeline.imaging import image_rgba
from sprite_pipeline.errors import CalibrationError


class EnclosedGreenArtworkTests(unittest.TestCase):
    def source(self):
        rgb=np.full((100,140,3),[0,210,0],np.uint8)
        rgb[15:85,65:125]=[35,30,30]  # ink outlines of a garment
        rgb[35:65,80:110]=[110,185,15]  # intended lime clothing detail
        rgb[25:75,10:50]=[42,180,42]  # contaminated green still touching screen
        return cv2.cvtColor(rgb,cv2.COLOR_RGB2BGRA)

    def test_exterior_spill_is_not_restored_as_clothing(self):
        source=self.source();before=source.copy()
        old=image_rgba(source,{'protect_green_art':True})
        new=image_rgba(source,{'protect_green_art':True,'green_art_enclosed_only':True})
        self.assertEqual(old[45,30,3],255)
        self.assertEqual(new[45,30,3],0)
        self.assertEqual(new[45,90,3],255)
        np.testing.assert_array_equal(old[:,:,:3],new[:,:,:3])
        np.testing.assert_array_equal(source,before)

    def test_opt_in_does_not_change_existing_defaults(self):
        source=self.source()
        np.testing.assert_array_equal(image_rgba(source,{'protect_green_art':True}),
            image_rgba(source,{'protect_green_art':True,'green_art_enclosed_only':False}))
        a=image_rgba(source,{'protect_green_art':True})
        b=image_rgba(source,{'protect_green_art':True,'green_art_enclosed_only':True})
        # Existing opaque non-green ink and transparent screen stay unchanged.
        np.testing.assert_array_equal(a[20:30,70:120],b[20:30,70:120])
        np.testing.assert_array_equal(a[:10],b[:10])

    def test_invalid_option_is_rejected(self):
        with self.assertRaises(CalibrationError):
            image_rgba(self.source(),{'protect_green_art':True,'green_art_enclosed_only':'yes'})


if __name__=='__main__':unittest.main()
