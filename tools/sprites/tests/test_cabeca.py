import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
try:  # numpy e opencv só vêm com requirements-animar.txt; a CI instala apenas o Pillow
    import cabeca
except ImportError:
    cabeca = None


@unittest.skipIf(cabeca is None, 'precisa de numpy e opencv (requirements-animar.txt)')
class StepTests(unittest.TestCase):
    """O tamanho do clipe é por trecho: um valor só, com degrau só onde o vídeo muda de verdade."""

    NOISE = [0.03, -0.04, 0.02, -0.03, 0.04, 0.0, -0.02]

    def noisy(self, level, n):
        return [level * (1 + self.NOISE[i % 7]) for i in range(n)]

    def test_noise_alone_gives_one_size(self):
        values = self.noisy(1.0, 40)
        values[7] = None
        self.assertEqual(1, len(set(cabeca.degraus(values))))

    def test_small_difference_is_not_a_step(self):
        # 5% entre os trechos: o erro da medida, não o vídeo
        self.assertEqual(1, len(set(cabeca.degraus(self.noisy(1.0, 12) + self.noisy(1.05, 20)))))

    def test_run_bigger_than_guard_is_a_step_without_ramp(self):
        # dash.mp4 da p02: parada, correndo ~16% maior, parada de novo
        values = self.noisy(1.0, 10) + self.noisy(1.16, 25) + self.noisy(1.0, 12)
        values[3] = None
        out = cabeca.degraus(values)
        self.assertEqual(len(values), len(out))
        # um valor por trecho; só o quadro do degrau pode seguir a medida
        self.assertEqual(1, len(set(out[:9])))
        self.assertEqual(1, len(set(out[11:34])))
        self.assertEqual(1, len(set(out[36:])))
        self.assertAlmostEqual(1.16, out[20] / out[0], delta=0.03)
        self.assertAlmostEqual(out[0], out[-1], delta=0.03)

    def test_video_zoom_frames_follow_the_measure(self):
        # intro.mp4 da p02: 12% menor em 4 quadros no fim; a passagem segue o vídeo, sem tranco
        values = [1.50] * 30 + [1.47, 1.44, 1.38, 1.35] + [1.33] * 20
        out = cabeca.degraus(values)
        self.assertEqual({1.50}, {round(v, 2) for v in out[:31]})
        self.assertEqual([1.44, 1.38], [round(v, 2) for v in out[31:33]])
        self.assertEqual({1.33}, {round(v, 2) for v in out[33:]})

    def test_nothing_found(self):
        with self.assertRaises(SystemExit):
            cabeca.degraus([None, None])


if __name__ == '__main__':
    unittest.main()
