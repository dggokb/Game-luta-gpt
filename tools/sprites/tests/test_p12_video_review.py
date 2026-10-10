"""Source provenance checks: technical preview success never approves art."""
import hashlib
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from p12_video_review import validate_selection


class VideoSelectionProofTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.folder = Path(self.temp.name)
        self.record = {'frames': []}
        for i in range(3):
            data = f'original decoded source pixels {i}'.encode()
            path = self.folder / f'{i}.png'
            path.write_bytes(data)
            self.record['frames'].append({'index': i, 'time': i / 24,
                'file': path.name, 'sha256': hashlib.sha256(data).hexdigest()})
        self.spec = {'selectedVideoFrames': [0, 1, 2], 'mainIndex': 1}
        self.animation = {'frames': [0, 1, 2], 'durationsMs': [20, 30, 40], 'impactFrame': 1}

    def tearDown(self):
        self.temp.cleanup()

    def check(self):
        return validate_selection('LIGHT_JAB', self.spec, self.animation, self.record, self.folder)

    def test_valid_source_keeps_impact(self):
        self.assertEqual(self.check(), 1)

    def test_tampered_source_is_rejected(self):
        (self.folder / '1.png').write_bytes(b'different source despite claimed provenance')
        with self.assertRaisesRegex(ValueError, 'pixels differ'):
            self.check()

    def test_duplicate_idle_frame_is_rejected(self):
        self.spec['selectedVideoFrames'] = [0, 0, 2]
        with self.assertRaisesRegex(ValueError, 'without duplicates'):
            self.check()

    def test_shifted_impact_is_rejected(self):
        self.spec['mainIndex'] = 2
        with self.assertRaisesRegex(ValueError, 'impact index'):
            self.check()

    def test_fabricated_timestamp_is_rejected(self):
        self.record['frames'][1]['time'] = 0
        with self.assertRaisesRegex(ValueError, 'timestamps'):
            self.check()

    def test_distance_driven_walk_has_no_invented_ms(self):
        self.animation = {'frames': [0, 1, 2], 'distancePerFrame': 25}
        self.assertEqual(self.check(), 1)


if __name__ == '__main__':
    unittest.main()
