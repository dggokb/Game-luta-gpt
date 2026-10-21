import sys
import unittest
from pathlib import Path

try:  # numpy e opencv vêm com as ferramentas de autoria; a CI instala só o Pillow
    import numpy as np
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    import video_para_sprite as V
except ModuleNotFoundError as missing:
    if missing.name not in ('numpy', 'cv2'):
        raise
    raise unittest.SkipTest('numpy/opencv only in the authoring tools')


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



class BrilhoNoCabeloTests(unittest.TestCase):
    """Gola branca entre as mechas atrás do pescoço (p15): vira cabelo; o branco do olho fica."""

    def rgba(self, island):
        f = np.zeros((300, 100, 4), np.uint8)
        f[..., 3] = 255
        f[..., :3] = (20, 20, 22)                     # cabelo
        y, x, h, w = island
        f[y:y + h, x:x + w, :3] = (235, 235, 230)    # ilha clara
        return f

    def test_small_pale_island_in_the_hair_becomes_hair(self):
        f = self.rgba((30, 40, 6, 5))
        V.hair_shine(f)
        self.assertLess(int(f[32, 42, :3].max()), 60)

    def test_eye_white_and_lower_body_stay(self):
        eye = self.rgba((30, 40, 2, 2))
        V.hair_shine(eye)
        self.assertGreater(int(eye[30, 40, :3].min()), 200)
        low = self.rgba((200, 40, 6, 5))              # abaixo da cabeça: roupa
        V.hair_shine(low)
        self.assertGreater(int(low[202, 42, :3].min()), 200)


if __name__ == '__main__':
    unittest.main()
