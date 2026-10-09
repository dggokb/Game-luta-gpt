import sys
import tempfile
import unittest
from pathlib import Path
import cv2
import numpy as np

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from quality_guard import ROOT,measure,conservative_sharpen,safe_gain,audit


def anime_like():
    a=np.zeros((140,100,4),dtype=np.uint8)
    a[10:130,10:90,:3]=[220,130,60]
    a[10:130,10:90,3]=255
    for i in range(15,125,6):
        a[i:i+2,15:85,:3]=[25,25,30]
    return a


class QualityGuardTest(unittest.TestCase):
    def test_blurred_content_detected_without_alpha_artifacts(self):
        x=anime_like()
        soft=x.copy()
        soft[:,:,:3]=cv2.GaussianBlur(x[:,:,:3],(9,9),2.0)
        self.assertGreater(measure(x)["sharpness"],measure(soft)["sharpness"]*1.3)

    def test_auto_sharpen_preserves_alpha_and_transparent_edges(self):
        original=anime_like()
        after=conservative_sharpen(original)
        np.testing.assert_array_equal(original[:,:,3],after[:,:,3])
        np.testing.assert_array_equal(original[0,0],after[0,0])
        self.assertEqual(after.shape,original.shape)

    def test_low_quality_or_overprocessed_frames_not_silently_accepted(self):
        original=anime_like()
        changed=original.copy()
        changed[:,:,:3]=255
        ok,_,_=safe_gain(original,changed)
        self.assertFalse(ok)

    def test_p01_audit_no_art_changes(self):
        path=ROOT/"characters/player_base/character.json"
        before=path.read_bytes()
        with tempfile.TemporaryDirectory() as temp:
            report=audit(output=Path(temp))
            self.assertEqual(42,report["statesAudited"])
            self.assertTrue((Path(temp)/"QUALITY.json").exists())
            self.assertTrue((Path(temp)/"QUALITY.md").exists())
            self.assertFalse(report["originalArtModified"])
        self.assertEqual(before,path.read_bytes())
