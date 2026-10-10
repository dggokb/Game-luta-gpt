import importlib.util
import tempfile
import unittest
from pathlib import Path

import numpy as np
from PIL import Image

SPEC = importlib.util.spec_from_file_location('p12_sources', Path(__file__).parents[1] / 'p12_authored_sources.py')
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class AuthoredExtractionTest(unittest.TestCase):
    def sheet(self, path, touching=False):
        pixels = np.zeros((200, 400, 4), dtype=np.uint8)
        for row in range(2):
            for col in range(4):
                x = col * 100 + (0 if touching and col == 0 else 20)
                y = row * 100 + 20
                pixels[y:y + 60, x:x + 40] = (70 + col, 90 + row, 120, 250)
                pixels[y, x + 1:x + 39, 3] = 11
        Image.fromarray(pixels).save(path)

    def test_preserves_color_alpha_and_reading_order(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'sheet.png'
            self.sheet(path)
            frames = MODULE.extract(path, 8)
            self.assertEqual(len(frames), 8)
            for i, (frame, box, _) in enumerate(frames):
                pixels = np.asarray(frame)
                opaque = pixels[pixels[:, :, 3] > 0]
                self.assertTrue((opaque[:, 0] == 70 + i % 4).all())
                self.assertTrue((opaque[:, 1] == 90 + i // 4).all())
                self.assertEqual(set(opaque[:, 3]), {11, 250})
                self.assertEqual(box[0], i % 4 * 100 + 19)

    def test_rejects_missing_or_extra_figures(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'sheet.png'
            self.sheet(path)
            with self.assertRaisesRegex(ValueError, 'expected 10'):
                MODULE.extract(path, 10)

    def test_rejects_clipped_figure(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'sheet.png'
            self.sheet(path, touching=True)
            with self.assertRaisesRegex(ValueError, 'border'):
                MODULE.extract(path, 8)

    def test_rejects_opaque_background(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'sheet.png'
            Image.new('RGBA', (400, 200), (30, 30, 30, 255)).save(path)
            with self.assertRaisesRegex(ValueError, 'transparent'):
                MODULE.extract(path, 8)


if __name__ == '__main__':
    unittest.main()
