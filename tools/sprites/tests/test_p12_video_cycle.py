"""The production source reader cannot replace a video frame with new art."""
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import p12_video_cycle as V


class OriginalFrameProofTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.override = patch.object(V, 'ROOT', self.root)
        self.override.start()
        self.path = self.root / 'art/keys/p12/poses/light_jab_start.png'
        self.path.parent.mkdir(parents=True)
        self.original = Image.new('RGBA', (12, 10), (180, 130, 100, 255))
        self.original.save(self.path)
        self.record = {'sha256': V.sha(self.path), 'sourceBox': [70, 100, 82, 110], 'time': 2.25}
        self.origin = {'driveId': 'original-drive-video', 'videoSha256': 'recorded-video-hash'}

    def tearDown(self):
        self.override.stop()
        self.temp.cleanup()

    def load(self, frame_spec=None):
        return V.original_frame('LIGHT_JAB', 54, 0, 2, self.origin, self.record, frame_spec or {})

    def test_actual_source_pixels_are_read(self):
        im, refs, recipe = self.load()
        self.assertEqual(im.tobytes(), self.original.tobytes())
        self.assertEqual(refs, ['art/keys/p12/poses/light_jab_start.png'])
        self.assertEqual(recipe['videoFrame'], 54)
        self.assertEqual(recipe['sourceTimeSeconds'], 2.25)

    def test_new_drawing_with_video_metadata_is_rejected(self):
        Image.new('RGBA', self.original.size, (90, 20, 170, 255)).save(self.path)
        with self.assertRaisesRegex(ValueError, 'not the recorded original'):
            self.load()

    def test_mask_only_removes_alpha_and_keeps_body_rgb(self):
        relative = 'art/keys/p12/video_masks/light_jab/0054.png'
        mask_path = self.root / relative
        mask_path.parent.mkdir(parents=True)
        mask = np.full((10, 12), 255, dtype=np.uint8)
        mask[:, :3] = 0
        Image.fromarray(mask).save(mask_path)
        spec = {'alphaMask': {'path': relative, 'sha256': V.sha(mask_path)}}
        im, refs, recipe = self.load(spec)
        pixels = np.asarray(im)
        self.assertTrue((pixels[:, :3] == 0).all())
        self.assertTrue((pixels[:, 3:] == np.asarray(self.original)[:, 3:]).all())
        self.assertIn(relative, refs)

    def test_stale_mask_is_rejected(self):
        relative = 'art/keys/p12/video_masks/light_jab/0054.png'
        path = self.root / relative
        path.parent.mkdir(parents=True)
        Image.new('L', self.original.size, 255).save(path)
        with self.assertRaisesRegex(ValueError, 'mask provenance'):
            self.load({'alphaMask': {'path': relative, 'sha256': 'stale-mask'}})


if __name__ == '__main__':
    unittest.main()
