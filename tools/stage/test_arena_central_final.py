"""Regressoes da montagem aprovada da Arena Central (sem Android)."""

import json
import tempfile
import unittest
from pathlib import Path

import numpy as np
from PIL import Image

from arena_central_final import (BASE, DEST, LEFT, OVERLAP, RIGHT, SKY_OVERSCAN,
                                 panorama, render_rest_preview)


# O jogo usa hoje o recorte do panorama em camadas (tools/stage/cenario_panorama.py). Os testes
# que leem as imagens exportadas por este gerador só valem quando o stage.json usa elas: rode o
# arena_central_final.py e troque o stage.json para voltar a esta montagem.
EM_USO = "fundo_panorama.webp" in (DEST / "stage.json").read_text(encoding="utf-8")
SO_EM_USO = unittest.skipUnless(EM_USO, "stage.json usa o panorama em camadas (cenario_panorama.py)")


class ArenaCentralFinalTest(unittest.TestCase):
    def test_panorama_preserves_both_illustrations_outside_join(self):
        with Image.open(LEFT) as left, Image.open(RIGHT) as right:
            result = np.asarray(panorama(left, right))
            a = np.asarray(left.convert("RGB"))
            b = np.asarray(right.convert("RGB"))
        w = a.shape[1]
        self.assertEqual((825, 2 * w - OVERLAP, 3), result.shape)
        np.testing.assert_array_equal(result[:, :w - OVERLAP], a[:, :w - OVERLAP])
        np.testing.assert_array_equal(result[:, w:], b[:, OVERLAP:])

    @SO_EM_USO
    def test_stage_has_real_parallax_without_mirroring_architecture(self):
        stage = json.loads((DEST / "stage.json").read_text(encoding="utf-8"))
        layers = stage["camadas"]
        self.assertEqual(2, len(layers))
        self.assertGreater(layers[0]["profundidade"], layers[1]["profundidade"])
        for layer in layers:
            self.assertEqual("nao", layer["repetir"])
            self.assertFalse(layer.get("virar", False))
            self.assertTrue((DEST / layer["imagem"]).is_file())
            self.assertEqual(BASE, layer["base"])
        self.assertEqual(-SKY_OVERSCAN, layers[0]["topo"])
        self.assertEqual(0, layers[1]["topo"])
        self.assertNotEqual(stage["chao"]["imagem"], stage["chao"]["imagemLajotas"])
        for key in ("imagem", "imagemLajotas"):
            self.assertTrue((DEST / stage["chao"][key]).is_file())
        with Image.open(DEST / stage["chao"]["imagem"]) as emblem, \
             Image.open(DEST / stage["chao"]["imagemLajotas"]) as plain:
            self.assertEqual(emblem.size, plain.size,
                             "lajotas nao podem virar um borrão ampliado")

    @SO_EM_USO
    def test_sky_extends_above_screen_without_flat_color_strip(self):
        with Image.open(DEST / "fundo_panorama.webp") as image:
            image = np.asarray(image.convert("RGB"), dtype=np.int16)
        overscan = round(825 * SKY_OVERSCAN)
        self.assertEqual(round(825 * (BASE + SKY_OVERSCAN)), image.shape[0])
        self.assertLess(np.abs(image[overscan - 1] - image[overscan]).mean(), 8)

    @SO_EM_USO
    def test_distant_sky_contains_no_second_camarote(self):
        with Image.open(DEST / "arquibancadas_panorama.webp") as image:
            near = np.asarray(image.convert("RGBA"))
        with Image.open(DEST / "fundo_panorama.webp") as image:
            far = np.asarray(image.convert("RGB"))
        overscan = round(825 * SKY_OVERSCAN)
        # O camarote central so existe no recorte proximo, e e opaco.
        center = near.shape[1] // 2
        self.assertGreater(near[255:330, center - 100:center + 100, 3].min(), 250)
        self.assertGreater(np.abs(
            far[overscan + 255:overscan + 330, center - 100:center + 100].astype(np.int16)
            - near[255:330, center - 100:center + 100, :3].astype(np.int16)
        ).mean(), 20)

    @SO_EM_USO
    def test_rest_camera_reconstructs_approved_floor(self):
        with Image.open(LEFT) as left, Image.open(RIGHT) as right:
            original = panorama(left, right)
        with tempfile.TemporaryDirectory() as folder:
            difference = render_rest_preview(original, Path(folder) / "repouso.png")
        # A diferenca restante e da compressao WebP e da dupla interpolacao;
        # o piso nao pode se deslocar ou trocar de perspectiva na camera inicial.
        self.assertLess(difference, 10.0)

    @SO_EM_USO
    def test_floor_outer_edges_use_stones_instead_of_stretched_border(self):
        with Image.open(DEST / "piso_panorama.webp") as image:
            floor = np.asarray(image.convert("RGB"), dtype=np.int16)
        with Image.open(DEST / "piso_lajotas.webp") as image:
            stones = np.asarray(image.convert("RGB"), dtype=np.int16)
        self.assertLess(np.abs(floor[-100:, :75] - stones[-100:, :75]).mean(), 8)
        self.assertGreater(floor[-100:, :75].std(), 15,
                           "a borda nao pode ser uma coluna achatada da pintura")


if __name__ == "__main__":
    unittest.main()
