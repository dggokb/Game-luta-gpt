"""Regression gates for the generic non-loop source-video refiner, P02 INTRO."""
import json
import sys
import unittest
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'tools/sprites'))
from refinar_sequencia_video import relative_timeline,project_source_registration
from refinar_ciclo_video import cells
from video_para_sprite import registration_x
import numpy as np

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

    def test_source_xy_motion_survives_full_pipeline(self):
        # Regression that the previous motor was incapable of passing:
        # every frame must preserve its own source-video displacement.
        g=self.report['packed']
        geo=self.generated['frameGeometry']
        self.assertEqual(len(geo),len(self.poses))
        for i,(frame,record) in enumerate(zip(self.poses,geo)):
            b=frame.getchannel('A').getbbox()
            self.assertIsNotNone(b,f'Frame {i} is blank')
            self.assertGreaterEqual(min(b[0],b[1],frame.width-b[2],frame.height-b[3]),7)
            foot=b[3]-1
            trunk=registration_x(np.asarray(frame))
            self.assertAlmostEqual(foot,record['expectedFootY'],delta=2.5)
            self.assertAlmostEqual(trunk,record['expectedTorsoX'],delta=3.5)

    def test_fixed_registration_preserves_synthetic_camera_space_translation(self):
        # Same character rendered 30 px to the right and 24 px higher must
        # remain displaced: NOT normalized back to zero per sprite frame.
        size=1.125
        rootX,rootY=200,450
        ref_x,ref_y=100.0,400.0
        first=(75,125,145,401)
        moved=(105,101,175,377)
        p0=project_source_registration(first,ref_x,ref_y,rootX,rootY,size)
        p1=project_source_registration(moved,ref_x,ref_y,rootX,rootY,size)
        self.assertEqual(p1[0]-p0[0],round(30*size))
        self.assertEqual(p1[1]-p0[1],round(-24*size))
        with self.assertRaises(ValueError):
            project_source_registration(first,float('nan'),ref_y,rootX,rootY,size)

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
