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

    def test_recipe_kinds(self):
        # Receitas como as do p02 antes e depois do refazer.py (lá toda escala tem motivo).
        class Pack:
            def __init__(self, video):
                self.video = video

            def cfg(self, state):
                return {'video': self.video}
        kind = lambda video: regua.recipe(Pack(video), 'X')
        self.assertEqual('forte', kind({'medida': '1º quadro = inicio_centro (nota 0.96)'})[0])
        self.assertEqual('fraca', kind({'medida': 'quadros 0, 6, 12 = guarda do IDLE (nota 0.63)'})[0])
        self.assertEqual('fraca', kind({'medida': '1º quadro = inicio_centro (nota 0.96)', 'reescala': [1.0, 1.1]})[0])
        self.assertEqual('manual', kind({'escala': 0.52, 'motivo': 'régua da cabeça: um tamanho só'})[0])
        self.assertEqual('manual', regua.recipe(regua.Pack('p02'), 'DASH')[0])
        self.assertEqual(0.88, kind({'medida': 'x', 'normalizedOpticalScale': 0.88})[2])


if __name__ == '__main__':
    unittest.main()
