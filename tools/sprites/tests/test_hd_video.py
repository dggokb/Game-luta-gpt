import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import harmony  # noqa: E402

# CI installs only Pillow; numpy/opencv come with the authoring tools.
HAS_NUMPY = importlib.util.find_spec('numpy') is not None
HAS_CV2 = HAS_NUMPY and importlib.util.find_spec('cv2') is not None


def body(w, h, x0, y0, x1, y1, color=(200, 60, 60)):
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(img).rectangle((x0, y0, x1 - 1, y1 - 1), fill=color + (255,))
    return img


def array(*args):
    import numpy as np
    return np.array(body(*args))


class HdHarmonyTests(unittest.TestCase):
    def test_harmony_measures_world_units_on_hd_atlas(self):
        cell = body(200, 160, 80, 20, 120, 150)
        hd = harmony.measure(cell, {'rootX': 100, 'rootY': 150, 'pixelScale': 2}, 1.0)
        sd = harmony.measure(cell.resize((100, 80), Image.NEAREST), {'rootX': 50, 'rootY': 75}, 1.0)
        self.assertAlmostEqual(sd['height'], hd['height'], delta=1.0)  # 1 logical px of resampling
        self.assertAlmostEqual(sd['torso'], hd['torso'], delta=1.0)


@unittest.skipUnless(HAS_NUMPY, 'numpy only in the authoring tools')
class LogicalSheetTests(unittest.TestCase):
    """HD sheets ("pixelScale": 2) are measured at the logical size by the review tools."""

    def test_sheet_measures_hd_at_logical_size(self):
        import numpy as np
        import auditoria
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            Image.fromarray(np.hstack([array(200, 160, 80, 20, 120, 150)] * 2), 'RGBA').save(root / 'hd.png')
            old = auditoria.ROOT
            auditoria.ROOT = root
            try:
                sheet = auditoria.Sheet({'source': 'hd.png', 'frameWidth': 200, 'frameHeight': 160,
                                         'rootX': 100, 'rootY': 150, 'columns': 2, 'pixelScale': 2.0})
                self.assertEqual((100, 80, 50, 75), (sheet.w, sheet.h, sheet.rx, sheet.ry))
                self.assertEqual((80, 100, 4), sheet.frame(1).shape)
                self.assertEqual((160, 200, 4), sheet.cell(1).shape)
                m = auditoria.measure(sheet.frame(0), sheet.rx, sheet.ry)
                self.assertEqual(65, m['height'])
            finally:
                auditoria.ROOT = old
                auditoria.Sheet.cache.clear()


@unittest.skipUnless(HAS_CV2, 'opencv only in the authoring tools')
class HandTouchTransferTests(unittest.TestCase):
    """retoque_hd repeats on the HD sheet what was done by hand on the reviewed 1x sheet."""

    def test_shift_and_erase_follow_the_reference(self):
        import retoque_hd
        hd = array(200, 160, 80, 20, 120, 150)
        hd[30:50, 130:170] = (240, 240, 120, 255)          # effect the 1x review erased
        ref = body(100, 80, 41, 10, 61, 75)                # body moved 1 logical px right
        up = retoque_hd.ref_cell(ref, 0,
                                 {'celula': [100, 80], 'raiz': [50, 75], 'colunas': 1}, hd.shape, (100, 150), 2)
        out, off, erased, score = retoque_hd.transfer_cell(hd, up, 2)
        self.assertEqual((2, 0), off)
        self.assertGreater(erased, 700)
        self.assertEqual(0, out[30:50, 135:170, 3].max())
        self.assertEqual(255, out[100, 100, 3])
        self.assertGreater(score, 0.95)

    def test_transfer_records_nothing_when_sheets_agree(self):
        import numpy as np
        import retoque_hd
        hd = array(200, 160, 80, 20, 120, 150)
        ref = body(100, 80, 40, 10, 60, 75)
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            Image.fromarray(hd, 'RGBA').save(root / 'hd.png')
            ref.save(root / 'ref.png')
            clip = {'pixelScale': 2.0, 'frameWidth': 200, 'frameHeight': 160, 'rootX': 100, 'rootY': 150,
                    'columns': 1, 'expectedFrames': 1}
            report = retoque_hd.transfer(root / 'hd.png', clip, {'folha': 'ref.png', 'celula': [100, 80],
                                                                 'raiz': [50, 75], 'colunas': 1}, root)
            self.assertEqual([], report)
            self.assertEqual(hd.tobytes(), np.array(Image.open(root / 'hd.png')).tobytes())


class HdRecipeTests(unittest.TestCase):
    """Every HD clip keeps its reviewed 1x sheet and logical geometry in the recipe."""

    def test_hd_video_clips_keep_the_reviewed_reference(self):
        clips = Path(__file__).resolve().parents[1] / 'clips'
        for path in sorted(clips.glob('*.json')):
            cfg = json.loads(path.read_text(encoding='utf-8'))
            video = cfg.get('video', {})
            if video.get('densidade', 1) == 1:
                continue
            d = video['densidade']
            self.assertEqual(d, cfg['pixelScale'], path.name)
            self.assertEqual(video.get('largura', video.get('celula', 256)) * d, cfg['frameWidth'], path.name)
            self.assertEqual(video.get('celula', 256) * d, cfg['frameHeight'], path.name)
            self.assertIn(video['referencia']['folha'], cfg['sourceReferences'], path.name)
            with Image.open(Path(__file__).resolve().parents[3] / cfg['source']) as img:
                self.assertLessEqual(max(img.size), 4096, path.name)


if __name__ == '__main__':
    unittest.main()
