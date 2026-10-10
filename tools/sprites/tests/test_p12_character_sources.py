import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import build_characters as C


class TransactionalSourceStagingTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.root=Path(self.temp.name)/'repo'
        self.stage=Path(self.temp.name)/'stage'
        self.root.mkdir();self.stage.mkdir()
        self.path='art/keys/p12/poses/light_jab_peak.png'
        self.source=self.root/self.path
        self.source.parent.mkdir(parents=True)
        self.source.write_bytes(b'original video pixels')

    def tearDown(self):
        self.temp.cleanup()

    def test_external_pose_is_staged_without_changing_original(self):
        staged=C.stage_reference(self.root,self.stage,self.path,'p12_full_light_jab')
        self.assertEqual(staged.read_bytes(),self.source.read_bytes())
        self.assertEqual(self.source.read_bytes(),b'original video pixels')

    def test_missing_source_is_rejected(self):
        self.source.unlink()
        with self.assertRaisesRegex(ValueError,'missing referenced source'):
            C.stage_reference(self.root,self.stage,self.path,'p12_full_light_jab')

    def test_corrupted_staged_source_is_rejected(self):
        staged=C.stage_reference(self.root,self.stage,self.path,'p12_full_light_jab')
        staged.write_bytes(b'wrong art')
        with self.assertRaisesRegex(ValueError,'staged source differs'):
            C.stage_reference(self.root,self.stage,self.path,'p12_full_light_jab')

    def test_path_cannot_escape_repository(self):
        with self.assertRaisesRegex(ValueError,'escapes project'):
            C.stage_reference(self.root,self.stage,'../outside.png','p12_full_light_jab')
