import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
try:  # numpy só vem com requirements-animar.txt; a CI instala apenas o Pillow
    import esqueleto
except ImportError:
    esqueleto = None


@unittest.skipIf(esqueleto is None, 'precisa de numpy (requirements-animar.txt)')
class FactorTests(unittest.TestCase):
    """O fator do esqueleto é suavizado: loop e clipe curto num valor só, clipe longo na mediana móvel."""

    def test_loop_gets_one_factor(self):
        self.assertEqual(esqueleto.factors([1.02, 0.97, None, 1.05], loop=True), [1.02] * 4)

    def test_long_clip_follows_a_zoom_without_jitter(self):
        # zoom de 0,86 a 0,95 com ruído de ±3% e um quadro que a medida errou
        noise = [0.03, -0.02, 0.01, -0.03, 0.02, 0.0] * 4
        values = [0.86 + 0.09 * i / 23 + noise[i] for i in range(24)]
        values[10] = 0.70
        z = esqueleto.factors(values, loop=False)
        self.assertEqual(len(z), len(values))
        self.assertAlmostEqual(z[0], 0.86, delta=0.02)
        self.assertAlmostEqual(z[-1], 0.95, delta=0.02)
        steps = [b - a for a, b in zip(z, z[1:])]
        self.assertTrue(all(abs(s - steps[0]) < 1e-9 for s in steps), 'reta: o tamanho não pulsa')

    def test_missing_frames_are_filled(self):
        z = esqueleto.factors([None] + [0.9] * 20, loop=False)
        self.assertAlmostEqual(z[0], 0.9)

    def test_nothing_found(self):
        self.assertIsNone(esqueleto.factors([None, None], loop=True))


if __name__ == '__main__':
    unittest.main()
