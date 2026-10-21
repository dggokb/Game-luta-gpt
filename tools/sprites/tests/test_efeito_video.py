from pathlib import Path
import json
import sys
import tempfile
import unittest

import cv2
import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import efeito_video

VERDE = (40, 230, 30)  # RGB


def video(caminho, quadros=10):
    """Um disco branco crescendo no verde, metade dele a 50% (a fumaça meio transparente)."""
    w, h = 160, 120
    out = cv2.VideoWriter(str(caminho), cv2.VideoWriter_fourcc(*'MJPG'), 24, (w, h))
    for i in range(quadros):
        q = np.zeros((h, w, 3), np.float32)
        q[:] = VERDE
        r = 6 + 3 * i
        yy, xx = np.mgrid[0:h, 0:w]
        disco = (xx - 80) ** 2 + (yy - 60) ** 2 <= r * r
        q[disco & (xx < 80)] = (255, 255, 255)
        q[disco & (xx >= 80)] = 0.5 * np.array([255, 255, 255]) + 0.5 * np.array(VERDE)
        out.write(cv2.cvtColor(q.astype(np.uint8), cv2.COLOR_RGB2BGR))
    out.release()


class EfeitoVideoTests(unittest.TestCase):
    def test_verde_vira_transparencia_e_o_meio_transparente_fica_branco(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            video(tmp / 'efeito.avi')
            destino, dados = efeito_video.converter(tmp / 'efeito.avi', 'teste', ancora='centro', altura=100,
                                                    saida=tmp / 'assets', previa=tmp / 'previa')
            self.assertEqual(10, len(dados['quadros']))
            self.assertTrue((destino / 'folha.webp').exists())
            self.assertEqual(dados, json.loads((destino / 'efeito.json').read_text()))
            folha = np.asarray(Image.open(destino / 'folha.webp').convert('RGBA')).astype(int)
            x, y, w, h, dx, dy = dados['quadros'][-1]
            q = folha[y:y + h, x:x + w]
            esquerda, direita = q[h // 2, w // 4], q[h // 2, 3 * w // 4]
            self.assertGreater(esquerda[3], 230, 'o branco cheio fica opaco')
            self.assertTrue(90 < direita[3] < 170, f'o branco a 50% fica meio transparente: {direita[3]}')
            self.assertLess(direita[1] - max(direita[0], direita[2]), 20, 'sem verde na cor')
            self.assertGreater(direita[:3].min(), 200, 'e continua branco')
            # Altura do efeito inteiro: 100 px do mundo (a folha guarda pixelScale).
            maior = max(q2[3] for q2 in dados['quadros'])
            self.assertAlmostEqual(100 * efeito_video.PIXEL_SCALE, maior, delta=4)
            # Origem no meio do vídeo: o disco fica em volta dela.
            self.assertAlmostEqual(-w / 2, dx, delta=4)
            self.assertTrue((tmp / 'previa' / 'teste.gif').exists())

    def test_ancora_no_chao_poe_a_origem_embaixo(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            video(tmp / 'efeito.avi', 6)
            _, dados = efeito_video.converter(tmp / 'efeito.avi', 'chao', ancora='chao', altura=60,
                                              saida=tmp / 'assets', previa=tmp / 'previa')
            # O quadro maior termina na origem (y = 0): o efeito fica em cima do chão.
            x, y, w, h, dx, dy = max(dados['quadros'], key=lambda q: q[3])
            self.assertAlmostEqual(0, dy + h, delta=3)


if __name__ == '__main__':
    unittest.main()
