import sys
import unittest
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import video_para_sprite as V


def frame():
    """Fundo verde (BGR), corpo escuro com um vão de fundo dentro e uma mecha fina fora."""
    f = np.zeros((200, 200, 3), np.uint8)
    f[:] = (40, 220, 40)
    f[40:180, 60:140] = (25, 25, 25)
    f[100:108, 96:104] = (40, 220, 40)              # vão entre as mechas (fundo de verdade)
    f[60:62, 140:146] = (32, 122, 32)               # mecha: metade cabelo, metade fundo
    return f


class RecorteSuaveTests(unittest.TestCase):
    def test_binary_key_fills_the_gap(self):
        self.assertEqual(255, V.key(frame())[103, 99, 3])

    def test_soft_key_opens_background_gaps(self):
        self.assertEqual(0, V.key(frame(), soft=True)[103, 99, 3])

    def test_soft_key_keeps_thin_strands_half_transparent_without_green(self):
        out = V.key(frame(), soft=True)
        a = int(out[60, 143, 3])
        self.assertTrue(60 < a < 200, a)
        r, g, b = (int(v) for v in out[60, 143, :3])
        self.assertLessEqual(g, max(r, b))
        self.assertLess(max(r, g, b), 80)            # a cor é a do cabelo, não a mistura

    def test_soft_key_does_not_change_the_body(self):
        hard, soft = V.key(frame()), V.key(frame(), soft=True)
        self.assertTrue((hard[50:90, 70:90] == soft[50:90, 70:90]).all())


if __name__ == '__main__':
    unittest.main()
