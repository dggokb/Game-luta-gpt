import copy
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import build_characters as pipeline

class CharacterPackTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.root=Path(self.tmp.name)
        for folder in ('characters','art/sprites/source','tools/sprites/clips','tools/sprites/profiles'):
            shutil.copytree(pipeline.ROOT/folder,self.root/folder)
        shutil.copyfile(pipeline.ROOT/'tools/sprites/preview.html',self.root/'tools/sprites/preview.html')
        self.path=self.root/'characters/player_base/character.json'
    def tearDown(self): self.tmp.cleanup()
    def edit(self,fn):
        data=json.loads(self.path.read_text());fn(data);self.path.write_text(json.dumps(data))
    def test_second_character_and_custom_attack_are_registered_without_java_edits(self):
        data=json.loads(self.path.read_text());data['id']='second_fighter';data['displayName']='Second fighter'
        data['animations']['CUSTOM_PUNCH']=copy.deepcopy(data['animations']['LIGHT_JAB'])
        data['animations']['CUSTOM_PUNCH']['frames']=[0,1,1,2]
        data['animations']['CUSTOM_PUNCH']['durationsMs']=[40,30,30,60]
        data['moves']['L']['animation']='CUSTOM_PUNCH'
        second=self.root/'characters/second_fighter';second.mkdir();(second/'character.json').write_text(json.dumps(data))
        roster=self.root/'characters/roster.json';r=json.loads(roster.read_text());r['team'][1]='second_fighter';roster.write_text(json.dumps(r))
        pipeline.build(self.root)
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn('all.put("second_fighter"',java);self.assertIn('a.get("CUSTOM_PUNCH")',java)
        pipeline.build(self.root,check=True)
        generated=self.root/pipeline.JAVA/'GeneratedCharacters.java';generated.write_text('stale')
        with self.assertRaisesRegex(ValueError,'stale'):pipeline.build(self.root,check=True)
        self.assertEqual('stale',generated.read_text())
    def test_invalid_frame_does_not_publish_any_assets(self):
        self.edit(lambda d:d['animations']['IDLE']['frames'].append(999))
        with self.assertRaisesRegex(ValueError,'frame index'):pipeline.build(self.root)
        self.assertFalse((self.root/'android/app/src/main/res').exists())
    def test_missing_movement_state_is_rejected(self):
        self.edit(lambda d:d['animations'].pop('JUMP'))
        with self.assertRaisesRegex(ValueError,'missing states'):pipeline.build(self.root)
    def test_active_window_outside_animation_is_rejected(self):
        self.edit(lambda d:d['moves']['L'].update(activeEndMs=900))
        with self.assertRaisesRegex(ValueError,'active window'):pipeline.build(self.root)
    def test_unknown_atlas_is_rejected(self):
        self.edit(lambda d:d['animations']['IDLE'].update(atlas='missing'))
        with self.assertRaisesRegex(ValueError,'unknown atlas'):pipeline.build(self.root)
    def test_duplicate_outputs_are_rejected(self):
        path=self.root/'tools/sprites/clips/player_base_jab.json';d=json.loads(path.read_text());d['output']='android/app/src/main/res/drawable-nodpi/player_base_idle.png';path.write_text(json.dumps(d))
        with self.assertRaisesRegex(ValueError,'duplicate output'):pipeline.build(self.root)
    def test_missing_source_is_rejected(self):
        (self.root/'art/sprites/source/player_base_jab_normalized.png').unlink()
        with self.assertRaises(FileNotFoundError):pipeline.build(self.root)
    def test_wrong_prepared_grid_geometry_is_rejected(self):
        path=self.root/'tools/sprites/clips/player_base_jab.json';d=json.loads(path.read_text());d['columns']=2;path.write_text(json.dumps(d))
        with self.assertRaisesRegex(ValueError,'dimensions'):pipeline.build(self.root)

if __name__=='__main__':unittest.main()
