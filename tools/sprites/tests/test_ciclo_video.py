"""Failing controls for real P02 video-cycle assembly and atlas size."""
import sys
import json
import unittest
from pathlib import Path

from PIL import Image

ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'tools/sprites'))
import validar_ciclo_video as check
from refinar_ciclo_video import cells


class VideoLoopRegression(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        char=json.loads((ROOT/'characters/p02/character.json').read_text())
        cls.char=char
        cls.idle_cfg=json.loads((ROOT/'tools/sprites/reports/p02_idle.report.json').read_text())['packed']
        cls.dash_cfg=json.loads((ROOT/'tools/sprites/reports/p02_dash.report.json').read_text())['packed']
        cls.idle=cells(Image.open(ROOT/'android/app/src/main/res/drawable-nodpi/p02_idle.png').convert('RGBA'),cls.idle_cfg)
        cls.dash=cells(Image.open(ROOT/'android/app/src/main/res/drawable-nodpi/p02_dash.png').convert('RGBA'),cls.dash_cfg)

    def test_approved_idle_untouched(self):
        self.assertEqual(self.char['animations']['IDLE']['frames'],list(range(15,45)))
        self.assertEqual(self.idle_cfg['frameCount'],56)

    def test_cycle_has_genuine_temporal_phases(self):
        seq=self.char['animations']['DASH']
        self.assertEqual(seq['frames'],list(range(len(seq['frames']))))
        self.assertGreaterEqual(len(seq['frames']),9)
        self.assertEqual(sum(seq['durationsMs']),260)
        self.assertTrue(all(ms>0 for ms in seq['durationsMs']))

    def test_mass_rejects_artificially_enlarged_dash(self):
        idle=[self.idle[i] for i in self.char['animations']['IDLE']['frames']]
        dash=[self.dash[i] for i in self.char['animations']['DASH']['frames']]
        base,actual=check.validate_frame_area(idle,dash)
        self.assertAlmostEqual(base/actual,1,delta=.15)
        huge=[im.resize((round(im.width*1.3),round(im.height*1.3)),
                        Image.Resampling.NEAREST) for im in dash]
        with self.assertRaises(ValueError):
            check.validate_frame_area(idle,huge)

    def test_single_corrupted_frame_causes_motion_gate_failure(self):
        real=[self.dash[i] for i in self.char['animations']['DASH']['frames']]
        old=[self.dash[0] for _ in range(4)]
        # A grossly displaced frame must invalidate a cycle even if the
        # aggregate median motion remains similar.
        wrong=real.copy()
        wrong[len(wrong)//2]=Image.new('RGBA',real[0].size)
        with self.assertRaises(ValueError):
            check.validate_cycle(wrong,old)

    def test_p02_ground_in_every_native_phase(self):
        rootY=self.dash_cfg['rootY']
        for i in self.char['animations']['DASH']['frames']:
            b=self.dash[i].getchannel('A').getbbox()
            self.assertIsNotNone(b)
            self.assertLessEqual(abs(b[3]-1-rootY),5)


if __name__=='__main__':
    unittest.main()
