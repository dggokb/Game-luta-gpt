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
        del data['animations']['LIGHT_JAB']  # replaced; unused clips are rejected
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
        with self.assertRaisesRegex(ValueError,'active window outside totalMs'):pipeline.build(self.root)
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

    def test_heavy_uses_canonical_anatomy_scale(self):
        pipeline.build(self.root)
        report=json.loads((self.root/'tools/sprites/reports/player_base_heavy_straight.report.json').read_text())
        self.assertEqual('canonical-anatomy',report['anatomy']['mode'])
        self.assertTrue(report['anatomy']['passed'])
        self.assertLess(report['scale'],0.66)
        self.assertEqual(128,report['layout']['rootX'])
        self.assertEqual(238,report['layout']['rootY'])

    def test_non_comparable_anatomy_reference_is_rejected(self):
        path=self.root/'tools/sprites/clips/player_base_heavy_straight.json'
        data=json.loads(path.read_text());data['anatomyReferenceFrame']=1
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError,'anatomy reference frame'):
            pipeline.build(self.root)


    def test_checked_in_roster_contains_real_second_character(self):
        roster=json.loads((self.root/'characters/roster.json').read_text())
        self.assertEqual(['player_base','player_two'],roster['team'])
        self.assertEqual('monster_npc',roster['opponentCharacter'])
        monster=json.loads((self.root/'characters/monster_npc/character.json').read_text())
        self.assertEqual('monster_npc.json',monster['profile'])
        self.assertEqual({'monster_npc_pack'},{a['atlas'] for a in monster['animations'].values()})
        first=json.loads((self.root/'characters/player_base/character.json').read_text())
        second=json.loads((self.root/'characters/player_two/character.json').read_text())
        self.assertNotEqual(first['id'],second['id'])
        self.assertEqual('player_two.json',second['profile'])
        self.assertNotEqual(first['profile'],second['profile'])
        self.assertTrue(set(second['animations']).issubset(set(first['animations'])))
        self.assertTrue(all(a['atlas'].startswith('player_two_') for a in second['animations'].values()))
        for binding in ('L','M','H'):
            self.assertEqual(first['moves'][binding],second['moves'][binding])
        self.assertEqual(set(pipeline.BINDINGS),set(first['moves']))
        self.assertEqual(set(pipeline.BINDINGS),set(second['moves']))
        self.assertIn('animation',first['moves']['2L'])
        self.assertEqual('CROUCH',second['moves']['2L']['pose'])

    def test_typo_in_state_name_is_rejected(self):
        self.edit(lambda d:d['animations'].__setitem__('HIT_STAN',d['animations'].pop('HIT_STAND')))
        with self.assertRaisesRegex(ValueError,'HIT_STAN.*not a known state'):pipeline.build(self.root)

    def test_every_input_must_be_declared(self):
        self.edit(lambda d:d['moves'].pop('jH'))
        with self.assertRaisesRegex(ValueError,'missing moves'):pipeline.build(self.root)

    def test_pose_must_match_input(self):
        self.edit(lambda d:d['moves'].__setitem__('L',{**d['moves']['jL']}))
        with self.assertRaisesRegex(ValueError,'pose AIR is not valid'):pipeline.build(self.root)

    def test_move_needs_exactly_one_of_animation_or_pose(self):
        self.edit(lambda d:d['moves']['2H'].update(animation='CROUCH_LIGHT'))
        with self.assertRaisesRegex(ValueError,'exactly one'):pipeline.build(self.root)

    def test_partial_knockdown_set_is_rejected(self):
        self.edit(lambda d:d['animations'].pop('GETUP'))
        with self.assertRaisesRegex(ValueError,'declared together'):pipeline.build(self.root)

    def test_fighter_rules_are_validated(self):
        self.edit(lambda d:d['fighter']['body'].update(crouchHeight=999))
        with self.assertRaisesRegex(ValueError,'crouchHeight'):pipeline.build(self.root)

    def test_unreferenced_source_art_is_rejected(self):
        (self.root/'art/sprites/source/forgotten.png').write_bytes(b'x')
        with self.assertRaisesRegex(ValueError,'forgotten.png'):pipeline.build(self.root)

    def test_orphan_outputs_fail_check_and_are_removed_by_write(self):
        pipeline.build(self.root)
        orphan=self.root/'android/app/src/main/res/drawable-nodpi/removed_clip.png';orphan.write_bytes(b'x')
        with self.assertRaisesRegex(ValueError,'orphan'):pipeline.build(self.root,check=True)
        self.assertTrue(orphan.exists())
        pipeline.build(self.root)
        self.assertFalse(orphan.exists())
        pipeline.build(self.root,check=True)

    def test_packing_crops_empty_border_without_moving_pixels(self):
        from PIL import Image
        stage=self.root/'stage';(stage/'out').mkdir(parents=True)
        img=Image.new('RGBA',(200,100))
        for i,(x,y) in enumerate(((40,30),(150,50))):img.putpixel((x,y),(255,0,0,255))
        img.save(stage/'out/a.png')
        report={'layout':{'frameWidth':100,'frameHeight':100,'rootX':50,'rootY':90,'columns':2,'frameCount':2}}
        pipeline.pack_atlas(stage,{'output':'out/a.png'},report,)
        packed=report['packed'];ox,oy=packed['cropOffset']
        self.assertEqual([32,22],[ox,oy])
        self.assertEqual((50-32,90-22),(packed['rootX'],packed['rootY']))
        out=Image.open(stage/'out/a.png');pw,ph=packed['frameWidth'],packed['frameHeight']
        self.assertEqual((2*pw,ph),out.size)
        self.assertEqual((255,0,0,255),out.getpixel((40-ox,30-oy)))
        self.assertEqual((255,0,0,255),out.getpixel((pw+50-ox,50-oy)))

    def test_shipped_atlases_are_packed(self):
        pipeline.build(self.root)
        java=(self.root/pipeline.JAVA/'GeneratedSpriteLayouts.java').read_text()
        report=json.loads((self.root/'tools/sprites/reports/player_base_idle.report.json').read_text())
        self.assertLess(report['packed']['decodedBytes']['packed'],report['packed']['decodedBytes']['canonical'])
        self.assertIn(f"IDLE_FRAME_WIDTH = {report['packed']['frameWidth']};",java)
        self.assertEqual(256,report['layout']['frameWidth'])

    def test_hud_name_is_rejected_in_favor_of_display_name(self):
        self.edit(lambda d:d['fighter'].update(hudName='P1'))
        with self.assertRaisesRegex(ValueError,'hudName was removed'):pipeline.build(self.root)

    def test_standing_visual_height_must_match_measured_idle(self):
        path=self.root/'tools/sprites/profiles/player_base.json';d=json.loads(path.read_text())
        d['standingVisualHeight']=300;path.write_text(json.dumps(d))
        # bbox-normalized clips also read this value; only prepared/anatomy clips use player_base here.
        with self.assertRaisesRegex(ValueError,'standingVisualHeight'):pipeline.build(self.root)

    def test_projectile_spawn_must_be_inside_body(self):
        self.edit(lambda d:d['fighter']['energy'].update(crouchSpawnY=400))
        with self.assertRaisesRegex(ValueError,'inside the body'):pipeline.build(self.root)

    def test_visual_heights_and_stats_are_generated(self):
        pipeline.build(self.root)
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn('new CharacterDefinition.Fighter(0xFFD9485F',java)
        self.assertIn('m.put("jH",new CharacterDefinition.Move("jH",null,"AIR"',java)
        states=(self.root/pipeline.JAVA/'SpriteStates.java').read_text()
        self.assertIn('static final String HIT_AIR = "HIT_AIR";',states)


    def test_player_base_crouch_light_is_a_real_declarative_move(self):
        pipeline.build(self.root)
        report=json.loads((self.root/'tools/sprites/reports/player_base_crouch_light.report.json').read_text())
        self.assertEqual([256,256,128,238],[report['layout'][k] for k in ('frameWidth','frameHeight','rootX','rootY')])
        self.assertEqual(4,report['layout']['frameCount'])
        self.assertTrue(all(frame['opaquePixels'] >= 10000 for frame in report['frames']))
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn('m.put("2L"',java)
        self.assertIn('a.get("CROUCH_LIGHT")',java)

    def test_player_base_crouch_medium_uses_wide_authored_prepared_grid(self):
        pipeline.build(self.root)
        report=json.loads((self.root/'tools/sprites/reports/player_base_crouch_medium.report.json').read_text())
        self.assertEqual([384,256,128,238],[report['layout'][k] for k in ('frameWidth','frameHeight','rootX','rootY')])
        self.assertEqual(4,report['layout']['frameCount'])
        self.assertTrue(all(frame['minimumMargin'] >= 8 for frame in report['frames']))
        self.assertTrue(all(frame['opaquePixels'] >= 10000 for frame in report['frames']))
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn('m.put("2M"',java)
        self.assertIn('a.get("CROUCH_MEDIUM")',java)

    def test_player_two_generated_art_passes_own_profile(self):
        pipeline.build(self.root)
        idle=json.loads((self.root/'tools/sprites/reports/player_two_idle.report.json').read_text())
        movement=json.loads((self.root/'tools/sprites/reports/player_two_movement.report.json').read_text())
        jab=json.loads((self.root/'tools/sprites/reports/player_two_jab.report.json').read_text())
        medium=json.loads((self.root/'tools/sprites/reports/player_two_medium_kick.report.json').read_text())
        heavy=json.loads((self.root/'tools/sprites/reports/player_two_heavy_straight.report.json').read_text())
        self.assertEqual([384,256,192,246],[idle['layout'][k] for k in ('frameWidth','frameHeight','rootX','rootY')])
        self.assertEqual(16,movement['layout']['frameCount'])
        for report in (jab,medium,heavy):
            self.assertEqual('player_two',report['characterProfile'])
            self.assertEqual('canonical-anatomy',report['anatomy']['mode'])
            self.assertTrue(report['anatomy']['passed'])
            self.assertEqual(246,report['layout']['rootY'])
        self.assertEqual(3,jab['layout']['frameCount'])
        self.assertEqual(3,medium['layout']['frameCount'])
        self.assertEqual(9,heavy['layout']['frameCount'])

if __name__=='__main__':unittest.main()
