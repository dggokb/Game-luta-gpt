import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from p01_motor_trial import ROOT, run, load_clips, atlas_frames


class P01TrialTests(unittest.TestCase):
    def test_p01_video_derived_sprite_sheet_decodes(self):
        sheets=load_clips(ROOT)
        pixels=atlas_frames(ROOT,sheets["player_base_jab"],[0,1,10])
        self.assertEqual(len(pixels),3)
        self.assertEqual(pixels[0].shape[2],4)
        self.assertGreater((pixels[0][:,:,3]>10).sum(),100)

    def test_full_p01_trial_is_read_only(self):
        p=ROOT/"characters/player_base/character.json"
        before=p.read_bytes()
        with tempfile.TemporaryDirectory() as folder:
            result=run(out=Path(folder),visual_states=["LIGHT_JAB"])
            self.assertEqual(result["statesAudited"],42)
            self.assertTrue(result["gameplayUnchanged"])
            self.assertFalse(result["imageChangesApplied"])
            self.assertFalse(result["timingChangesApplied"])
            self.assertTrue((Path(folder)/"summary.json").exists())
            self.assertTrue((Path(folder)/"light_jab.html").exists())
        self.assertEqual(before,p.read_bytes())
