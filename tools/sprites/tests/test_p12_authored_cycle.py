import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).parents[1]))
import p12_authored_cycle as cycle


class AuthoredCycleTest(unittest.TestCase):
    def test_impact_drawings_land_on_original_impact_indices(self):
        contract = json.loads(cycle.CONTRACT.read_text())
        for state, animation in contract['animations'].items():
            if 'impactFrame' not in animation:
                continue
            before = json.dumps(animation, sort_keys=True)
            mapping = cycle.frame_map(animation, 8, 3)
            self.assertEqual(len(mapping), len(animation['frames']), state)
            self.assertEqual(mapping[animation['impactFrame']], 3, state)
            self.assertEqual((mapping[0], mapping[-1]), (0, 7), state)
            self.assertEqual(before, json.dumps(animation, sort_keys=True), state)

    def test_whole_pose_pixels_and_alpha_are_preserved_at_unit_scale(self):
        pixels = np.zeros((20, 10, 4), dtype=np.uint8)
        pixels[1:19, 1:9] = (100, 120, 130, 251)
        rendered = cycle.render_pose(Image.fromarray(pixels), 1.0, [5, 19])
        crop = rendered.crop((cycle.RX - 5, cycle.RY - 19, cycle.RX + 5, cycle.RY + 1))
        self.assertEqual(Image.fromarray(pixels).tobytes(), crop.tobytes())

    def test_clipping_is_rejected_without_per_frame_shrink(self):
        with self.assertRaisesRegex(ValueError, 'clipped'):
            cycle.render_pose(Image.new('RGBA', (600, 300), 'white'), 1, [300, 300])

    def test_cache_reuses_exact_frame_and_invalidates_alpha_corruption(self):
        import torch
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            model_path = folder / 'model.pth'
            model_path.write_bytes(b'test-model')
            with patch.object(cycle, 'sha', return_value=cycle.MODEL_HASH):
                upscaler = cycle.CachedUpscaler(model_path, folder / 'cache')
            upscaler.model = torch.nn.Upsample(scale_factor=4, mode='nearest')
            frame = Image.new('RGBA', (16, 16))
            frame.paste((150, 160, 170, 250), (4, 3, 12, 14))
            first = upscaler.sr(frame)
            second = upscaler.sr(frame)
            self.assertEqual(first.tobytes(), second.tobytes())
            self.assertEqual((upscaler.hits, upscaler.misses), (1, 1))
            path = next((folder / 'cache').glob('*.png'))
            Image.new('RGBA', (32, 32), 'white').save(path)
            repaired = upscaler.sr(frame)
            self.assertEqual(repaired.getchannel('A').tobytes(), frame.getchannel('A').resize((32, 32), Image.Resampling.LANCZOS).tobytes())
            self.assertEqual((upscaler.hits, upscaler.misses), (1, 2))
            changed = frame.copy()
            changed.putpixel((8, 8), (200, 80, 30, 250))
            upscaler.sr(changed)
            self.assertEqual(upscaler.misses, 3)


if __name__ == '__main__':
    unittest.main()
