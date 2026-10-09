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
        roster=self.root/'characters/roster.json';r=json.loads(roster.read_text());r['team'][1]='second_fighter';r['teams']=[r['team']];roster.write_text(json.dumps(r))
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
    def test_cancel_window_outside_the_move_is_rejected(self):
        self.edit(lambda d:d['moves']['L']['cancelWindows'].update(hit=[2,90]))
        with self.assertRaisesRegex(ValueError,'L.cancelWindows.hit.*outside the move'):pipeline.build(self.root)
    def test_hit_window_cannot_open_before_contact(self):
        self.edit(lambda d:d['moves']['H']['cancelWindows'].update(block=[0,12]))
        with self.assertRaisesRegex(ValueError,'H.cancelWindows.block'):pipeline.build(self.root)
    def test_millisecond_timing_of_schema_2_is_rejected(self):
        self.edit(lambda d:d['moves']['L'].update(totalMs=160))
        with self.assertRaisesRegex(ValueError,'replaced by frame data'):pipeline.build(self.root)
    def test_cancel_route_must_point_to_an_existing_move(self):
        self.edit(lambda d:d['moves']['M']['cancelInto'].append('LIGHT_2'))
        with self.assertRaisesRegex(ValueError,'cancelInto LIGHT_2 does not exist'):pipeline.build(self.root)
    def test_cancel_route_cannot_cross_ground_and_air(self):
        self.edit(lambda d:d['moves']['jL']['cancelInto'].append('M'))
        with self.assertRaisesRegex(ValueError,'changes ground/air'):pipeline.build(self.root)
    def test_auto_combo_must_be_a_declared_route(self):
        self.edit(lambda d:d['fighter'].update(autoCombo=['H','L']))
        with self.assertRaisesRegex(ValueError,'autoCombo H -> L is not a declared cancel route'):pipeline.build(self.root)
    def test_command_specials_must_not_share_command_and_button(self):
        self.edit(lambda d:d['specialMoves']['S4'].update(buttons=['M','H']))
        with self.assertRaisesRegex(ValueError,'S3 and S4 share command and button M'):pipeline.build(self.root)
    def test_command_special_needs_a_valid_command(self):
        self.edit(lambda d:d['specialMoves']['S2'].update(command=[1,9]))
        with self.assertRaisesRegex(ValueError,'S2.command'):pipeline.build(self.root)
    def test_unknown_atlas_is_rejected(self):
        self.edit(lambda d:d['animations']['IDLE'].update(atlas='missing'))
        with self.assertRaisesRegex(ValueError,'unknown atlas'):pipeline.build(self.root)
    def test_duplicate_outputs_are_rejected(self):
        path=self.root/'tools/sprites/clips/player_base_jab.json';d=json.loads(path.read_text());d['output']='android/app/src/main/res/drawable-nodpi/player_base_idle.png';path.write_text(json.dumps(d))
        with self.assertRaisesRegex(ValueError,'duplicate output'):pipeline.build(self.root)
    def test_missing_source_is_rejected(self):
        (self.root/'art/sprites/source/player_base_jab_video_normalized.png').unlink()
        with self.assertRaises(FileNotFoundError):pipeline.build(self.root)
    def test_wrong_prepared_grid_geometry_is_rejected(self):
        path=self.root/'tools/sprites/clips/player_base_jab.json';d=json.loads(path.read_text());d['columns']=2;path.write_text(json.dumps(d))
        with self.assertRaisesRegex(ValueError,'dimensions'):pipeline.build(self.root)

    def test_non_comparable_anatomy_reference_is_rejected(self):
        path=self.root/'tools/sprites/clips/player_two_jab.json'
        data=json.loads(path.read_text());data['anatomyReferenceFrame']=1
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError,'anatomy reference frame'):
            pipeline.build(self.root)


    def test_checked_in_roster_contains_real_second_character(self):
        roster=json.loads((self.root/'characters/roster.json').read_text())
        self.assertEqual(['player_base','p03'],roster['team'])
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
        def shared(pack,binding):
            # Cancels into the character's own command specials (S2...) are not shared data.
            m=copy.deepcopy(pack['moves'][binding])
            m['cancelInto']=[t for t in m['cancelInto'] if t not in pack.get('specialMoves',{})]
            # Reach and hit height follow each character's art size (worldScale).
            for k in ('reach','hitHeight'): m.pop(k,None)
            return m
        for binding in ('L','M','H'):
            self.assertEqual(shared(first,binding),shared(second,binding))
        self.assertEqual({'S2','S3','S4'},set(first['specialMoves']))
        self.assertEqual(set(pipeline.BINDINGS),set(first['moves'])-set(pipeline.AIR_DOWN_BINDINGS))
        self.assertIn('j2H',first['moves'])
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
        self.edit(lambda d:d['moves'].__setitem__('L',{k:v for k,v in d['moves']['jM'].items() if k!='animation'}|{'pose':'AIR'}))
        with self.assertRaisesRegex(ValueError,'pose AIR is not valid'):pipeline.build(self.root)

    def test_move_needs_exactly_one_of_animation_or_pose(self):
        self.edit(lambda d:d['moves']['2H'].update(pose='CROUCH'))
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

    def test_hud_name_is_rejected_in_favor_of_display_name(self):
        self.edit(lambda d:d['fighter'].update(hudName='P1'))
        with self.assertRaisesRegex(ValueError,'hudName was removed'):pipeline.build(self.root)

    def test_standing_visual_height_must_match_measured_idle(self):
        path=self.root/'tools/sprites/profiles/player_base.json';d=json.loads(path.read_text())
        d['standingVisualHeight']=300;path.write_text(json.dumps(d))
        with self.assertRaisesRegex(ValueError,'standingVisualHeight'):pipeline.build(self.root)

    def test_projectile_spawn_must_be_inside_body(self):
        self.edit(lambda d:d['fighter']['energy'].update(crouchSpawnY=400))
        with self.assertRaisesRegex(ValueError,'inside the body'):pipeline.build(self.root)

    def test_pushbox_must_fit_inside_hurtbox(self):
        self.edit(lambda d:d['fighter']['body'].update(pushHalfWidth=80))
        with self.assertRaisesRegex(ValueError,'pushbox'):pipeline.build(self.root)

    def test_regroup_returns_limbs_that_cross_into_a_neighbour_cell(self):
        import import_sprites as imp
        from PIL import Image
        sheet=Image.new('RGBA',(200,100))
        for x in range(20,115):sheet.putpixel((x,50),(255,0,0,255))   # frame 0 arm crossing x=100
        for y in range(40,60):sheet.putpixel((150,y),(0,255,0,255))   # frame 1 body
        sheet.putpixel((170,10),(0,0,255,255))                          # speck
        frames,w,h,rx,ry,rep=imp.regroup_components({'id':'t'},sheet,2,2,100,100,50,90,{'pad':20,'dropSmallerThan':4})
        self.assertEqual((140,140,70,110),(w,h,rx,ry))
        self.assertEqual((255,0,0,255),frames[0].getpixel((114+20,70)))   # tip kept by its owner
        self.assertEqual(0,frames[1].getpixel((14+20,70))[3])            # no fragment in the neighbour
        self.assertEqual(1,rep['droppedPixels'])
        self.assertEqual([{'frame':0,'pixelsRecovered':15}],rep['recovered'])

    def test_magenta_cleanup_repaints_only_the_artefact(self):
        import import_sprites as imp
        from PIL import Image
        cell=Image.new('RGBA',(20,20),(120,80,60,255))
        cell.putpixel((10,10),(255,40,220,255))
        out,n=imp.remove_magenta(cell,[5,5,15,15])
        r,g,b,a=out.getpixel((10,10))
        self.assertLess(b,g+18);self.assertGreater(n,0)
        self.assertEqual((120,80,60,255),out.getpixel((0,0)))

    def test_upscaling_a_master_requires_an_explicit_reason(self):
        path=self.root/'tools/sprites/clips/player_two_movement.json';d=json.loads(path.read_text())
        d['transform']['scale']=1.2;path.write_text(json.dumps(d))
        with self.assertRaisesRegex(ValueError,'requires allowUpscale'):pipeline.build(self.root)

    def test_component_segmentation_separates_poses_that_overlap_in_x(self):
        import import_sprites as imp
        from PIL import Image
        sheet=Image.new('RGBA',(120,60))
        for x in range(10,70):sheet.putpixel((x,20),(255,0,0,255))   # pose A: long arm over B
        for y in range(30,50):sheet.putpixel((60,y),(0,0,255,255))   # pose B, inside A's x range
        regions,images=imp.connected_frames(sheet,10)
        self.assertEqual([(10,20,70,21),(60,30,61,50)],regions)
        self.assertEqual((60,1),images[0].size)
        self.assertEqual((255,0,0,255),images[0].getpixel((50,0)))
        self.assertEqual((0,0,255,255),images[1].getpixel((0,0)))

    def test_impact_frame_must_land_in_the_active_window(self):
        self.edit(lambda d:d['animations']['LIGHT_JAB'].update(impactFrame=9))
        with self.assertRaisesRegex(ValueError,'L: impact frame 9 of LIGHT_JAB .* outside the active window'):pipeline.build(self.root)

    def test_fixed_scale_requires_a_reason(self):
        path=self.root/'tools/sprites/clips/player_base_hit_crouch.json';d=json.loads(path.read_text())
        # Drawn sheet with a fixed scale and no reason (no checked-in clip uses fixed now).
        d.update(segmentation='alpha-components',rootMode='ground-feet',scaleMode='fixed',scale=0.5)
        path.write_text(json.dumps(d))
        with self.assertRaisesRegex(ValueError,'requires scaleReason'):pipeline.build(self.root)


class SizeAndTimingStandardTests(unittest.TestCase):
    """The playable cast follows the size and timing standard (no build needed)."""
    def test_playable_attacks_declare_their_impact_frame(self):
        roster=json.loads((pipeline.ROOT/'characters/roster.json').read_text())
        for cid in sorted({c for team in roster.get('teams',[roster['team']]) for c in team}):
            pack=json.loads((pipeline.ROOT/'characters'/cid/'character.json').read_text(encoding='utf-8'))
            for name,animation,_ in pipeline.attack_animations(pack):
                self.assertIn('impactFrame',pack['animations'][animation],f'{cid}/{name} ({animation})')
    def test_video_recipes_record_how_the_scale_was_found(self):
        keys=pipeline.ROOT/'art/keys'
        for path in sorted((pipeline.ROOT/'tools/sprites/clips').glob('*.json')):
            recipe=json.loads(path.read_text(encoding='utf-8')).get('video')
            if recipe is None: continue
            with self.subTest(clip=path.name):
                self.assertTrue(recipe['arquivo'].endswith('.mp4'))
                self.assertGreater(recipe['escala'],0)
                # Medida automática ou escala manual com motivo, nunca as duas nem nenhuma.
                self.assertEqual(1,('medida' in recipe)+('motivo' in recipe))
                if 'medida' in recipe:
                    self.assertTrue((keys/recipe['personagem']/'tamanho.json').exists())
    def test_key_images_have_their_measured_scale(self):
        for path in sorted((pipeline.ROOT/'art/keys').glob('*/tamanho.json')):
            data=json.loads(path.read_text(encoding='utf-8'))
            with self.subTest(personagem=path.parent.name):
                self.assertTrue((pipeline.ROOT/'characters'/data['pacote']/'character.json').exists())
                self.assertIn('inicio_centro',data['chaves'])
                for name,escala in data['chaves'].items():
                    self.assertTrue((path.parent/f'{name}.png').exists(),name)
                    self.assertTrue(0.2<escala<1.5,name)


class BuiltPackTests(unittest.TestCase):
    """Read-only checks of one build of the checked-in packs (the pipeline runs once)."""
    @classmethod
    def setUpClass(cls):
        cls.tmp=tempfile.TemporaryDirectory();cls.root=Path(cls.tmp.name)
        for folder in ('characters','art/sprites/source','tools/sprites/clips','tools/sprites/profiles'):
            shutil.copytree(pipeline.ROOT/folder,cls.root/folder)
        shutil.copyfile(pipeline.ROOT/'tools/sprites/preview.html',cls.root/'tools/sprites/preview.html')
        cls.path=cls.root/'characters/player_base/character.json'
        pipeline.build(cls.root)
    @classmethod
    def tearDownClass(cls): cls.tmp.cleanup()

    def test_frame_data_is_compiled_into_attack_definitions(self):
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn('sm.put("S2",new CharacterDefinition.Special(new CharacterDefinition.Move("S2",a.get("SPECIAL_S2")',java)
        self.assertIn('new int[]{1,3,2},"LMH"));',java)
        self.assertIn('new int[]{3,5},"H"));',java)
        self.assertIn('new AttackDefinition.Builder("2H",AttackDefinition.Kind.NORMAL).damage(800).frames(9,6,9)',java)
        self.assertIn('.launch(AttackDefinition.Launch.LAUNCH)',java)
        self.assertIn('new AttackDefinition.Builder("SUPER",AttackDefinition.Kind.SUPER)',java)
    def test_shipped_atlases_are_packed(self):
        java=(self.root/pipeline.JAVA/'GeneratedSpriteLayouts.java').read_text()
        report=json.loads((self.root/'tools/sprites/reports/player_base_idle.report.json').read_text())
        self.assertLess(report['packed']['decodedBytes']['packed'],report['packed']['decodedBytes']['canonical'])
        self.assertIn(f"IDLE_FRAME_WIDTH = {report['packed']['frameWidth']};",java)
        self.assertEqual(512,report['layout']['frameWidth'])
        self.assertEqual(2,report['packed']['pixelScale'])

    def test_hd_idle_preserves_timing_registration_and_runtime_budget(self):
        from PIL import Image
        character=json.loads(self.path.read_text())
        idle=character['animations']['IDLE']
        self.assertEqual(list(range(76)),idle['frames'])
        self.assertEqual([42]*76,idle['durationsMs'])
        self.assertTrue(idle['loop'])
        report=json.loads((self.root/'tools/sprites/reports/player_base_idle.report.json').read_text())
        self.assertEqual([512,512,256,476], [report['layout'][k] for k in ('frameWidth','frameHeight','rootX','rootY')])
        packed=report['packed']
        self.assertLessEqual(packed['frameWidth']*packed['columns'],4096)
        self.assertLessEqual(packed['frameHeight']*8,4096)
        self.assertLess(packed['decodedBytes']['packed'],64*1024*1024)
        original=Image.open(self.root/'art/sprites/source/player_base_idle_video_normalized.png').convert('RGBA')
        master=Image.open(self.root/'art/sprites/source/player_base_idle_sr_2x.png').convert('RGBA')
        for i in range(76):
            old=original.crop((i%8*256,i//8*256,(i%8+1)*256,(i//8+1)*256))
            new=master.crop((i%10*512,i//10*512,(i%10+1)*512,(i//10+1)*512))
            self.assertEqual(old.getchannel('A').resize((512,512),Image.Resampling.LANCZOS).tobytes(),new.getchannel('A').tobytes(),i)
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn(',10,76,2.00000000f)',java)
        self.assertIn('art/sprites/source/player_base_idle_video_normalized.png',report['provenance']['referenceSources'])

    def test_hd_walk_forward_preserves_distance_root_and_runtime_budget(self):
        from PIL import Image
        walk=json.loads(self.path.read_text())['animations']['WALK_FORWARD']
        self.assertEqual(list(range(20)),walk['frames'])
        self.assertEqual(12.5,walk['distancePerFrame'])
        self.assertTrue(walk['loop'])
        self.assertNotIn('durationsMs',walk)
        report=json.loads((self.root/'tools/sprites/reports/player_base_walk_forward.report.json').read_text())
        self.assertEqual([512,512,256,476],[report['layout'][k] for k in ('frameWidth','frameHeight','rootX','rootY')])
        packed=report['packed']
        self.assertEqual(2,packed['pixelScale'])
        self.assertLessEqual(packed['frameWidth']*packed['columns'],4096)
        self.assertLessEqual(packed['frameHeight']*3,4096)
        self.assertLess(packed['decodedBytes']['packed'],32*1024*1024)
        original=Image.open(self.root/'art/sprites/source/player_base_walk_forward_video_normalized.png').convert('RGBA')
        hd=Image.open(self.root/'art/sprites/source/player_base_walk_forward_sr_2x.png').convert('RGBA')
        for i in range(20):
            src=original.crop((i%8*256,i//8*256,(i%8+1)*256,(i//8+1)*256))
            cell=hd.crop((i%8*512,i//8*512,(i%8+1)*512,(i//8+1)*512))
            self.assertEqual(src.getchannel('A').resize((512,512),Image.Resampling.LANCZOS).tobytes(),cell.getchannel('A').tobytes(),i)
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn(',8,20,2.00000000f)',java)
        self.assertIn('art/sprites/source/player_base_walk_forward_video_normalized.png',report['provenance']['referenceSources'])

    def test_visual_heights_and_stats_are_generated(self):
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn('new CharacterDefinition.Fighter(0xFFD9485F',java)
        self.assertIn('m.put("jH",new CharacterDefinition.Move("jH",a.get("JUMP_HEAVY"),null',java)
        self.assertIn('m.put("2L",new CharacterDefinition.Move("2L",null,"CROUCH"',java.split('all.put("player_base"')[1])
        states=(self.root/pipeline.JAVA/'SpriteStates.java').read_text()
        self.assertIn('static final String HIT_AIR = "HIT_AIR";',states)


    def test_player_base_crouch_light_is_a_real_declarative_move(self):
        report=json.loads((self.root/'tools/sprites/reports/player_base_crouch_light.report.json').read_text())
        self.assertEqual([384,256,140,238],[report['layout'][k] for k in ('frameWidth','frameHeight','rootX','rootY')])
        self.assertEqual(11,report['layout']['frameCount'])
        self.assertTrue(all(frame['opaquePixels'] >= 10000 for frame in report['frames']))
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn('m.put("2L"',java)
        self.assertIn('a.get("CROUCH_LIGHT")',java)

    def test_player_base_crouch_medium_uses_wide_authored_prepared_grid(self):
        report=json.loads((self.root/'tools/sprites/reports/player_base_crouch_medium.report.json').read_text())
        self.assertEqual([512,256,250,238],[report['layout'][k] for k in ('frameWidth','frameHeight','rootX','rootY')])
        self.assertEqual(16,report['layout']['frameCount'])
        self.assertTrue(all(frame['minimumMargin'] >= 8 for frame in report['frames']))
        self.assertTrue(all(frame['opaquePixels'] >= 10000 for frame in report['frames']))
        java=(self.root/pipeline.JAVA/'GeneratedCharacters.java').read_text()
        self.assertIn('m.put("2M"',java)
        self.assertIn('a.get("CROUCH_MEDIUM")',java)

    def test_player_base_crouch_heavy_launcher_has_video_art(self):
        report=json.loads((self.root/'tools/sprites/reports/player_base_crouch_heavy.report.json').read_text())
        self.assertEqual(12,report['layout']['frameCount'])
        self.assertTrue(report['passed'])
        self.assertTrue(all(f['opaquePixels']>=10000 for f in report['frames']))

    def test_player_base_air_attacks_are_declared_with_video_art(self):
        report=json.loads((self.root/'tools/sprites/reports/player_base_jump_light.report.json').read_text())
        self.assertTrue(report['passed'])
        self.assertEqual(7,report['layout']['frameCount'])
        self.assertTrue(all(f['minimumMargin']>=8 for f in report['frames']))
        pack=json.loads(self.path.read_text())
        self.assertEqual('JUMP_LIGHT',pack['moves']['jL']['animation'])
        self.assertEqual('JUMP_MEDIUM',pack['moves']['jM']['animation'])
        self.assertEqual('JUMP_HEAVY',pack['moves']['jH']['animation'])

    def test_player_base_defense_and_fall_pass_and_new_states(self):
        # Guards and the fall come from video (prepared grid, scaled by the video tool).
        for key in ('defense_stand','defense_crouch','defense_air','fall'):
            self.assertTrue(json.loads((self.root/f'tools/sprites/reports/player_base_{key}.report.json').read_text())['passed'])
        pack=json.loads(self.path.read_text())
        self.assertEqual('player_base_defense_air',pack['animations']['DEFENSE_AIR']['atlas'])
        self.assertEqual('player_base_fall',pack['animations']['GROUNDED']['atlas'])
        states=(self.root/pipeline.JAVA/'SpriteStates.java').read_text()
        self.assertIn('DEFENSE_AIR = "DEFENSE_AIR"',states)

    def test_player_two_generated_art_passes_own_profile(self):
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

    def test_drawn_sheet_uses_canonical_anatomy_scale(self):
        # player_base now comes from video; player_two keeps the drawn sheets.
        report=json.loads((self.root/'tools/sprites/reports/player_two_jab.report.json').read_text())
        self.assertEqual('canonical-anatomy',report['anatomy']['mode'])
        self.assertTrue(report['anatomy']['passed'])
        self.assertLess(report['scale'],0.66)
        self.assertEqual(192,report['layout']['rootX'])
        self.assertEqual(246,report['layout']['rootY'])

    def test_get_up_starts_lying_head_back_like_the_end_of_the_fall(self):
        from PIL import Image
        import harmony
        cache={}
        def lying_head_side(atlas,frame):
            cell,_=harmony.frame_cell(self.root,atlas,frame,cache)
            a=cell.getchannel('A').point(lambda v:255 if v>10 else 0);x0,y0,x1,y1=a.getbbox()
            # the highest part of a body lying face down is the back/head side
            px=a.load();cols=[min((y for y in range(y0,y1) if px[x,y]),default=y1) for x in range(x0,x1)]
            left=sum(cols[:len(cols)//3])/(len(cols)//3);right=sum(cols[-(len(cols)//3):])/(len(cols)//3)
            return 'left' if left<right else 'right'
        pack=json.loads(self.path.read_text())
        last_lying=pack['animations']['GROUNDED']['frames'][-1]
        self.assertEqual(lying_head_side('player_base_fall',last_lying),lying_head_side('player_base_getup',0))


if __name__=='__main__':unittest.main()
