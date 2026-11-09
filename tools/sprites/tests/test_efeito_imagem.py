"""Imagem parada (gerada) -> folha de efeito: recorte do fundo, estouro nos quadros e folha de N quadros."""
import sys
import unittest
from pathlib import Path

from PIL import Image

try:  # o numpy vem com as ferramentas de autoria; a CI instala só o Pillow
    import numpy as np
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    import efeito_imagem as ei
except ModuleNotFoundError as missing:
    if missing.name != 'numpy':
        raise
    raise unittest.SkipTest('numpy only in the authoring tools')


def brilho(lado=200, raio=40, cor=(255, 200, 80)):
    """Um disco de luz com halo no meio de um fundo preto."""
    y, x = np.mgrid[0:lado, 0:lado].astype(np.float32)
    r = np.hypot(x - lado / 2, y - lado / 2)
    luz = np.clip(1.2 - r / raio, 0, 1)[..., None]
    return Image.fromarray((luz * np.array(cor, np.float32)).astype(np.uint8), 'RGB')


class EfeitoImagemTest(unittest.TestCase):
    def test_black_becomes_transparent_and_light_keeps_its_colour(self):
        rgba = ei.recortar_fundo(brilho(), 'preto')
        self.assertEqual(rgba[0, 0, 3], 0.0)
        self.assertGreater(rgba[100, 100, 3], 0.95)
        # No halo (meio transparente) a cor é a da luz, não a luz escurecida pelo preto.
        ys, xs = np.nonzero((rgba[..., 3] > 0.3) & (rgba[..., 3] < 0.6))
        r, g, b = rgba[ys[0], xs[0], :3]
        self.assertGreater(r, 0.95)
        self.assertAlmostEqual(g / r, 200 / 255, delta=0.05)
        self.assertAlmostEqual(b / r, 80 / 255, delta=0.05)

    def test_the_burst_pops_past_its_size_settles_and_fades(self):
        rgba = ei.recortar_fundo(brilho(), 'preto')
        quadros, tempos, lado = ei.quadros_de(rgba, 128.0, (0.5, 0.5), ei.ESTOURO)
        self.assertEqual(len(quadros), len(ei.ESTOURO))
        self.assertEqual(len(tempos), len(quadros))
        larguras = [q.getchannel('A').point(lambda v: 255 if v > 3 else 0).getbbox() for q in quadros]
        larguras = [b[2] - b[0] for b in larguras]
        self.assertLess(larguras[0], larguras[2])          # nasce pequena
        self.assertGreater(larguras[1], larguras[2])       # passa do tamanho
        self.assertAlmostEqual(larguras[2], 128 * ei.PIXEL_SCALE, delta=4)   # assenta no tamanho pedido
        self.assertLess(quadros[-1].getchannel('A').getextrema()[1], 80)     # some
        # O ponto de contato fica no meio da tela em todos os quadros.
        for q in quadros[:3]:
            a = np.asarray(q.getchannel('A')).astype(np.float32)
            ys, xs = np.nonzero(a > 3)
            self.assertAlmostEqual((xs.min() + xs.max()) / 2, lado / 2, delta=2)
            self.assertAlmostEqual((ys.min() + ys.max()) / 2, lado / 2, delta=2)

    def test_a_strip_of_frames_is_split_in_equal_cells_at_one_scale(self):
        faixa = Image.new('RGB', (800, 200))
        for i, raio in enumerate((15, 40, 30, 20)):
            faixa.paste(brilho(200, raio), (i * 200, 0))
        rgba = ei.recortar_fundo(faixa, 'preto')
        quadros, tempos, _ = ei.quadros_da_folha(rgba, 4, 128.0, (0.5, 0.5))
        self.assertEqual(len(quadros), 4)
        self.assertEqual(len(tempos), 4)
        larguras = []
        for q in quadros:
            b = q.getchannel('A').point(lambda v: 255 if v > 3 else 0).getbbox()
            larguras.append(b[2] - b[0])
        self.assertEqual(max(range(4), key=lambda i: larguras[i]), 1)        # o segundo é o máximo
        self.assertAlmostEqual(larguras[1], 128 * ei.PIXEL_SCALE, delta=4)   # e ele é o tamanho pedido


if __name__ == '__main__':
    unittest.main()
