import sys
import unittest
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
try:  # numpy só vem com requirements-animar.txt; a CI instala apenas o Pillow
    import numpy as np
    import regua
except ImportError:
    regua = None


def figure(scale=1.0, dx=0, legs=40):
    """Boneco (cabeça, tronco e pernas) com a raiz no meio dos pés: (rgba, raiz_x, raiz_y)."""
    w, h = 300, 400
    img = Image.new('RGBA', (w, h))
    d = ImageDraw.Draw(img)
    rx, ry = 150 + dx, 380
    s = lambda v: v * scale
    d.ellipse((rx - s(22), ry - s(260), rx + s(22), ry - s(214)), fill=(200, 160, 140, 255))
    d.rectangle((rx - s(35), ry - s(214), rx + s(35), ry - s(110)), fill=(30, 60, 120, 255))
    d.polygon([(rx - s(30), ry - s(110)), (rx - s(legs) - s(10), ry), (rx - s(legs) + s(14), ry),
               (rx, ry - s(80)), (rx + s(legs) - s(14), ry), (rx + s(legs) + s(10), ry),
               (rx + s(30), ry - s(110))], fill=(20, 20, 40, 255))
    return np.array(img), 150, ry


@unittest.skipIf(regua is None, 'precisa de numpy (requirements-animar.txt)')
class ReguaTests(unittest.TestCase):
    def test_same_pose_gives_size_and_foot_offset(self):
        s, dx, iou = regua.fit(figure(), figure(scale=0.9, dx=6))
        self.assertAlmostEqual(1 / s, 0.9, delta=0.015)
        self.assertLessEqual(abs(dx + 6), 2)  # fit devolve a correção
        self.assertGreater(iou, regua.POSE_IOU)

    def test_identical_frames_fit_exactly(self):
        s, dx, iou = regua.fit(figure(), figure())
        self.assertAlmostEqual(s, 1.0, delta=0.005)
        self.assertEqual(dx, 0)
        self.assertGreater(iou, 0.98)

    def test_wide_stance_is_a_pose_change_not_a_size_change(self):
        # Mesma cabeça e tronco, pernas abertas: a silhueta não encaixa na guarda.
        _, _, iou = regua.fit(figure(), figure(legs=110))
        self.assertLess(iou, regua.POSE_IOU)

    def test_checked_in_p02_recipe_kinds(self):
        p = regua.Pack('p02')
        self.assertEqual('forte', regua.recipe(p, 'LIGHT_JAB')[0])
        self.assertEqual('manual', regua.recipe(p, 'MEDIUM_KICK')[0])
        self.assertEqual('fraca', regua.recipe(p, 'INTRO')[0])
        self.assertEqual(1.0, regua.recipe(p, 'DASH')[2])  # fator óptico 0,88 desfeito pela régua


if __name__ == '__main__':
    unittest.main()
