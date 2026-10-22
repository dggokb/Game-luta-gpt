from pathlib import Path
import json
import sys
import tempfile
import unittest

from PIL import Image

try:  # numpy e opencv vêm com as ferramentas de autoria; a CI instala só o Pillow
    import cv2
    import numpy as np
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    import super_video
except ModuleNotFoundError as missing:
    if missing.name not in ('numpy', 'cv2'):
        raise
    raise unittest.SkipTest('numpy/opencv only in the authoring tools')

VERDE = (40, 230, 30)  # RGB


def video(caminho, quadros=6):
    """Um retângulo escuro (o lutador) entrando pela esquerda até o chão, a marca d'água branca
    no canto de cima e uma mancha clara de compressão solta no verde."""
    w, h = 192, 108
    out = cv2.VideoWriter(str(caminho), cv2.VideoWriter_fourcc(*'MJPG'), 24, (w, h))
    for i in range(quadros):
        q = np.zeros((h, w, 3), np.uint8)
        q[:] = VERDE
        q[4:10, 4:30] = (255, 255, 255)
        q[30:h, 0:20 + 12 * i] = (60, 40, 30)
        q[50:60, 160:170] = (60, 200, 50)
        out.write(cv2.cvtColor(q, cv2.COLOR_RGB2BGR))
    out.release()


class SuperVideoTests(unittest.TestCase):
    def test_quadros_na_borda_de_baixo_sem_marca_nem_mancha(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            video(tmp / 'entrada.avi')
            destino, dados = super_video.converter(tmp / 'entrada.avi', 'teste', fps=12, saida_ms=100,
                                                   marca=(0, 0, 40, 16), saida=tmp / 'assets',
                                                   previa=tmp / 'previa')
            self.assertEqual(dados, json.loads((destino / 'super.json').read_text()))
            self.assertEqual(6, len(dados['quadros']))
            self.assertAlmostEqual(1000 / 12, dados['quadroMs'], delta=0.1)
            k = super_video.TELA_A / 108
            for i, q in enumerate(dados['quadros']):
                self.assertTrue((destino / q['imagem']).exists())
                # Só o lutador: na borda esquerda, com o pé no chão da tela (sem a marca em cima
                # nem a mancha à direita esticando o corte).
                self.assertEqual(0.0, q['x'])
                self.assertAlmostEqual(-(108 - 30) * k, q['y'], delta=2 * k)
                self.assertAlmostEqual(q['y'] + q['altura'], 0.0, delta=0.6 * k)
                # (a borda do MJPG borra 1-2 px)
                self.assertAlmostEqual((20 + 12 * i) * k, q['largura'], delta=3 * k)
            im = Image.open(destino / dados['quadros'][-1]['imagem']).convert('RGBA')
            self.assertGreater(im.getpixel((im.width // 2, im.height // 2))[3], 230)
