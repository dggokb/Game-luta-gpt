"""Regression gates for the generic non-loop source-video refiner, P02 INTRO."""
import json
import sys
import unittest
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'tools/sprites'))
from refinar_sequencia_video import relative_timeline
from refinar_ciclo_video import cells

class IntroVideoTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ch=json.loads((ROOT/'characters/p02/character.json').read_text())
        cls.report=json.loads((ROOT/'tools/sprites/reports/p02_intro.report.json').read_text())
        cls.idle=json.loads((ROOT/'tools/sprites/reports/p02_idle.report.json').read_text())
        cls.poses=cells(Image.open(ROOT/'android/app/src/main/res/drawable-nodpi/p02_intro.png').convert('RGBA'),cls.report['packed'])
        cls.idles=cells(Image.open(ROOT/'android/app/src/main/res/drawable-nodpi/p02_idle.png').convert('RGBA'),cls.idle['packed'])
        cls.generated=json.loads((ROOT/'tools/sprites/reports/p02_intro_video_refinement.json').read_text())

    def test_full_natural_nonloop_timeline(self):
        intro=self.ch['animations']['INTRO']
        self.assertFalse(intro['loop'])
        self.assertEqual(intro['frames'],list(range(len(self.poses))))
        self.assertGreaterEqual(len(self.poses),45)
        self.assertEqual(len(self.poses),self.generated['newFrames'])
        self.assertEqual(sum(intro['durationsMs']),self.generated['originalTotalMs'])
        self.assertTrue(all(t>0 for t in intro['durationsMs']))
        self.assertEqual(self.generated['frames'],sorted(set(self.generated['frames'])))
        self.assertEqual(self.generated['frames'][0],0)
        self.assertEqual(self.generated['frames'][-1],96)

    def test_scale_matches_approved_idle_standing_frame(self):
        idle=self.ch['animations']['IDLE']
        reference=sorted(self.idle['packed']['rootY']-self.idles[x].getchannel('A').getbbox()[1]
                         for x in idle['frames'])
        expected=(reference[14]+reference[15])/2
        intro=self.report['packed']
        upright=[intro['rootY']-im.getchannel('A').getbbox()[1] for im in self.poses[:4]]
        for h in upright:
            self.assertLess(abs(h-expected)/expected,.06,
                            'INTRO standing body must match canonical IDLE stature')
        self.assertLess(self.generated['relativeSize'],.98,
                        'Oversized original should not be falsely approved')

    def test_all_frames_remain_whole_and_feet_anchored(self):
        g=self.report['packed']
        for i,im in enumerate(self.poses):
            b=im.getchannel('A').getbbox()
            self.assertIsNotNone(b,f'Frame {i} is blank')
            self.assertGreaterEqual(min(b[0],b[1],im.width-b[2],im.height-b[3]),7)
            self.assertLessEqual(abs((b[3]-1)-g['rootY']),3)

    def test_original_idle_and_approved_dash_unchanged(self):
        self.assertEqual(self.ch['animations']['IDLE']['frames'],list(range(15,45)))
        self.assertEqual(self.ch['animations']['IDLE']['durationsMs'],[41]*30)
        self.assertEqual(len(self.ch['animations']['DASH']['frames']),12)
        self.assertEqual(sum(self.ch['animations']['DASH']['durationsMs']),260)

    def test_negative_control_time_reordering_must_fail(self):
        original=[0,4,7,11,15,18,22,26,30,33,37,41,44,48,52,55,59,63,66,70,74,78,81,85,89,92,96]
        ms=[156]*16+[104]*7+[234]*4
        valid=list(range(0,97,2))
        self.assertEqual(sum(relative_timeline(valid,original,ms,4160)),4160)
        invalid=valid.copy()
        invalid[15],invalid[22]=invalid[22],invalid[15]
        with self.assertRaises(ValueError):
            relative_timeline(invalid,original,ms,4160)

if __name__=='__main__':unittest.main()
