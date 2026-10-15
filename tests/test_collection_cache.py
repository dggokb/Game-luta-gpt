"""Adding motion measurements must never silently reuse an uncorrected export."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from process_collection import process_character
from sprite_pipeline.extraction import digest


class CollectionCacheTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.output=self.root/'processed';self.folder=self.output/'p02'
        self.folder.mkdir(parents=True);self.motion=self.root/'motion';self.motion.mkdir()
        self.marker=self.folder/'complete.json';self.setting=self.motion/'p02.json'
        self.setting.write_text(json.dumps({'clips':{'dash':{'source_sha256':'source'}}}))

    def run_cached(self):
        return process_character({'id':'p02'},self.root,self.output,self.root,self.root,self.motion)

    def test_new_measurements_refuse_legacy_completion_marker_without_overwriting(self):
        self.marker.write_text(json.dumps({'character':'p02','converted':29}));before=self.marker.read_bytes()
        with self.assertRaisesRegex(ValueError,'Anotações de movimento mudaram'):
            self.run_cached()
        self.assertEqual(self.marker.read_bytes(),before)

    def test_identical_measurements_can_reuse_a_completed_export(self):
        summary={'character':'p02','converted':29,'motion_annotations_sha256':digest(self.setting)}
        self.marker.write_text(json.dumps(summary));self.assertEqual(self.run_cached(),summary)

    def test_changing_or_removing_measurements_invalidates_completion_marker(self):
        self.marker.write_text(json.dumps({'motion_annotations_sha256':digest(self.setting)}))
        self.setting.write_text(json.dumps({'clips':{}}))
        with self.assertRaisesRegex(ValueError,'Anotações de movimento mudaram'):
            self.run_cached()
        self.setting.unlink()
        with self.assertRaisesRegex(ValueError,'Anotações de movimento mudaram'):
            self.run_cached()


if __name__=='__main__':unittest.main()
