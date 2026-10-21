from pathlib import Path
import sys
import tempfile
import unittest

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import intermediarios


def quadrado(x, tamanho=(96, 160)):
    """Um bloco texturizado (o fluxo precisa de detalhe) com o canto esquerdo em x."""
    h, w = tamanho
    img = np.zeros((h, w, 4), np.uint8)
    yy, xx = np.mgrid[0:40, 0:40]
    img[28:68, x:x + 40, 0] = 60 + (xx * 5) % 160
    img[28:68, x:x + 40, 1] = 60 + (yy * 7) % 160
    img[28:68, x:x + 40, 2] = 120
    img[28:68, x:x + 40, 3] = 255
    return img


def centro_x(img):
    a = img[..., 3].astype(np.float32)
    xs = np.arange(img.shape[1], dtype=np.float32)
    return float((a.sum(0) * xs).sum() / a.sum())


class IntermediariosTests(unittest.TestCase):
    def test_salto_mede_quanto_o_desenho_anda(self):
        self.assertAlmostEqual(12.0, intermediarios.salto(quadrado(40), quadrado(52)), delta=3.0)
        self.assertLess(intermediarios.salto(quadrado(40), quadrado(40)), 0.5)

    def test_confianca_alta_num_deslize_e_baixa_numa_pose_nova(self):
        self.assertGreater(intermediarios.confianca(quadrado(40), quadrado(52)), 0.8)
        outro = quadrado(40)
        outro[..., :3] = np.flip(outro[..., :3], axis=0)  # mesma silhueta, desenho trocado
        espelhado = np.zeros_like(outro)
        espelhado[10:90, 20:40] = outro[28:68, 40:80].repeat(2, axis=0)[:, :20]
        espelhado[10:90, 20:40, 3] = 255
        self.assertLess(intermediarios.confianca(quadrado(40), espelhado), 0.6)

    def test_o_quadro_do_meio_fica_no_meio_e_sem_fantasma(self):
        a, b = quadrado(40), quadrado(56)
        meio = intermediarios.intermediario(a, b)
        self.assertAlmostEqual((centro_x(a) + centro_x(b)) / 2, centro_x(meio), delta=2.0)
        # Um bloco só (não dois meio apagados): a área opaca é a de um quadrado.
        opaco = (meio[..., 3] > 127).sum()
        self.assertAlmostEqual(40 * 40, opaco, delta=40 * 40 * 0.2)

    def test_gerar_reparte_o_tempo_e_salva_a_revisao(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'characters/teste').mkdir(parents=True)
            (root / 'tools/sprites/clips').mkdir(parents=True)
            (root / 'tools/sprites/reports').mkdir(parents=True)
            folha = np.hstack([quadrado(40), quadrado(56), quadrado(72)])
            Image.fromarray(folha).save(root / 'folha.png')
            frames = [[160 * i, 0, 160, 96, 0, 0] for i in range(3)]
            (root / 'tools/sprites/clips/folha.json').write_text(
                '{"output": "folha.png", "report": "tools/sprites/reports/folha.json"}')
            (root / 'tools/sprites/reports/folha.json').write_text(
                '{"worldScale": 1, "packed": {"frameWidth": 160, "frameHeight": 96, "pixelScale": 1, "frames": %s}}'
                % str(frames))
            (root / 'characters/teste/character.json').write_text(
                '{"animations": {"SOCO": {"atlas": "folha", "frames": [0, 1, 2], "durationsMs": [40, 30, 30], "loop": false}}}')
            saida = root / 'saida'
            gerados = intermediarios.gerar(root, 'teste', 'SOCO', {0}, saida)
            self.assertEqual(1, len(gerados))
            self.assertTrue((saida / 'SOCO_00-01_revisao.png').exists())
            with Image.open(saida / 'SOCO_com_intermediarios.gif') as gif:
                self.assertEqual(4, gif.n_frames)
            linhas = intermediarios.auditar(root, 'teste')
            self.assertEqual({(0, 1), (1, 2)}, {(l['de'], l['para']) for l in linhas})


if __name__ == '__main__':
    unittest.main()
