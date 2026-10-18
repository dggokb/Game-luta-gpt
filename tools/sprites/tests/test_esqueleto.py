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

    def test_noise_alone_gives_one_factor(self):
        self.assertEqual(len(set(esqueleto.factors([1.02, 0.97, None, 1.05, 0.99, 1.01], loop=True))), 1)

    def test_size_change_between_phases_is_followed_without_pulsing(self):
        # em pé ~6% maior que na base (intro.mp4), com ±4% de ruído e um quadro errado
        noise = [0.03, -0.04, 0.02, -0.03, 0.04, 0.0, -0.02]
        values = [1.06 + noise[i % 7] for i in range(18)] + [1.00 + noise[i % 7] for i in range(10)]
        values[5] = 0.80
        z = esqueleto.factors(values, loop=False)
        self.assertEqual(len(z), len(values))
        self.assertAlmostEqual(sum(z[:10]) / 10, 1.06, delta=0.02)
        self.assertAlmostEqual(sum(z[-5:]) / 5, 1.00, delta=0.02)
        self.assertTrue(all(abs(b - a) <= esqueleto.RATE + 1e-9 for a, b in zip(z, z[1:])), 'nunca pulsa')
        # o vaivém que sobra é menor que 1% (a cabeça mexe ~1 px): o ruído de 4% não passa
        travel = sum(abs(b - a) for a, b in zip(z, z[1:]))
        self.assertLess(travel - (max(z) - min(z)), 0.03)

    def test_missing_frames_are_filled(self):
        z = esqueleto.factors([None] + [0.9] * 20, loop=False)
        self.assertAlmostEqual(z[0], 0.9)

    def test_nothing_found(self):
        self.assertIsNone(esqueleto.factors([None, None], loop=True))


if __name__ == '__main__':
    unittest.main()
