"""Regressoes da montagem aprovada da Arena Central (sem Android)."""

import json
import tempfile
import unittest
from pathlib import Path

import numpy as np
from PIL import Image

from arena_central_final import (ART, BASE, DEST, LEFT, OVERLAP, RIGHT,
                                 panorama, render_rest_preview)


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

    def test_stage_uses_depth_parallax_without_mirror_or_repeated_emblems(self):
        stage = json.loads((DEST / "stage.json").read_text(encoding="utf-8"))
        layers = stage["camadas"]
        self.assertEqual(2, len(layers))
        self.assertGreater(layers[0]["profundidade"], layers[1]["profundidade"])
        for layer in layers:
            self.assertEqual("nao", layer["repetir"])
            self.assertFalse(layer.get("virar", False))
            self.assertTrue((DEST / layer["imagem"]).is_file())
            self.assertEqual(BASE, layer["base"])
        self.assertNotEqual(stage["chao"]["imagem"], stage["chao"]["imagemLajotas"])
        for key in ("imagem", "imagemLajotas"):
            self.assertTrue((DEST / stage["chao"][key]).is_file())

    def test_alpha_layer_does_not_cover_distant_sky(self):
        with Image.open(DEST / "arquibancadas_panorama.webp") as image:
            alpha = np.asarray(image.convert("RGBA"))[:, :, 3]
        self.assertLess(alpha[75].max(), 2)
        self.assertGreater(alpha[-1].min(), 253)

    def test_rest_camera_reconstructs_approved_floor(self):
        with Image.open(LEFT) as left, Image.open(RIGHT) as right:
            original = panorama(left, right)
        with tempfile.TemporaryDirectory() as folder:
            difference = render_rest_preview(original, Path(folder) / "repouso.png")
        # A diferenca restante e da compressao WebP e da dupla interpolacao;
        # o piso nao pode se deslocar ou trocar de perspectiva na camera inicial.
        self.assertLess(difference, 10.0)


if __name__ == "__main__":
    unittest.main()
