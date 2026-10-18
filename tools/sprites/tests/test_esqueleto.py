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
        values = [0.86, 0.90, 0.85, 0.88] * 3 + [0.95, 0.99, 0.94, 0.97] * 3
        z = esqueleto.factors(values, loop=False)
        self.assertEqual(len(z), len(values))
        self.assertLess(max(z[:6]), 0.9)
        self.assertGreater(min(z[-6:]), 0.93)
        self.assertTrue(all(abs(a - b) < 0.06 for a, b in zip(z, z[1:])))

    def test_missing_frames_are_filled(self):
        z = esqueleto.factors([None] + [0.9] * 20, loop=False)
        self.assertAlmostEqual(z[0], 0.9)

    def test_nothing_found(self):
        self.assertIsNone(esqueleto.factors([None, None], loop=True))


if __name__ == '__main__':
    unittest.main()
