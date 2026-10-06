#!/usr/bin/env python3
"""Compile validated sprite packs transactionally. No runtime JSON or image conversion."""
import argparse
import base64
import json
import hashlib
import math
import re
import shutil
import tempfile
from pathlib import Path
from PIL import Image
import import_sprites as importer

ROOT = Path(__file__).resolve().parents[2]
JAVA = 'android/app/src/main/java/com/gamelutagpt/'
# Semantic vocabulary. The runtime (generated SpriteStates.java) and this validator share it.
REQUIRED = ('IDLE','COMBAT','WALK_FORWARD','WALK_BACK','CROUCH','RISE','JUMP','FALL','DASH','BACKDASH','LAND')
OPTIONAL = ('DEFENSE_STAND','DEFENSE_CROUCH','HIT_STAND','HIT_CROUCH','HIT_AIR','KNOCKDOWN','GROUNDED','GETUP')
KNOCKDOWN_SET = {'KNOCKDOWN','GROUNDED','GETUP'}
# Every input the simulation can request. Ground L/M/H, crouching 2X and airborne jX.
BINDINGS = ('L','M','H','2L','2M','2H','jL','jM','jH')
# A move without its own animation must say which posture the body keeps.
POSES = {'CROUCH':('2L','2M','2H'),'AIR':('jL','jM','jH')}
SPECIALS = ('S','SUPER')
FACINGS = {'right':1,'left':-1}

def read(path):
    return json.loads(path.read_text())

def positive(value, name):
    if isinstance(value, bool) or not isinstance(value, (int,float)) or not math.isfinite(value) or value <= 0:
        raise ValueError(f'{name}: expected a positive finite number')

def positive_int(value, name):
    if type(value) is not int or value <= 0:
        raise ValueError(f'{name}: expected a positive integer')

def safe(root, value):
    p = (root / value).resolve()
    if not p.is_relative_to(root.resolve()):
        raise ValueError(f'Path escapes project: {value}')
    return p

def validate_projectile(data, name, body):
    positive_int(data['damage'], name+'.damage')
    positive(data['range'], name+'.range');positive(data['speed'], name+'.speed')
    # Launch point relative to the root: forward offset and height above the ground.
    positive(data['spawnX'], name+'.spawnX')
    for key,limit in (('spawnY',body['standHeight']),('crouchSpawnY',body['crouchHeight']),('airSpawnY',body['standHeight'])):
        if key in data:
            positive(data[key], f'{name}.{key}')
            if data[key] > limit:
                raise ValueError(f'{name}.{key}: projectile must spawn inside the body height ({limit})')

def validate_fighter(pack):
    f = pack['fighter'];name = pack['id']+'.fighter'
    if 'hudName' in f:
        raise ValueError(f'{name}: hudName was removed; the HUD shows displayName and the team slot')
    if not isinstance(pack.get('displayName'),str) or not pack['displayName'].strip():
        raise ValueError(f'{pack["id"]}: displayName is required')
    if not re.fullmatch('#[0-9A-Fa-f]{6}', f.get('color','')):
        raise ValueError(f'{name}: color must be #RRGGBB')
    positive_int(f['maxLife'], name+'.maxLife')
    if not isinstance(f['autoCombo'],list) or any(b not in ('L','M','H') for b in f['autoCombo']):
        raise ValueError(f'{name}: autoCombo accepts only L, M and H')
    body = f['body']
    for key in ('halfWidth','standHeight','crouchHeight','pushHalfWidth','pushHeight'): positive(body[key], f'{name}.body.{key}')
    if body['crouchHeight'] > body['standHeight']:
        raise ValueError(f'{name}: crouchHeight cannot exceed standHeight')
    # The pushbox keeps bodies apart; it sits inside the hurtbox so it never blocks a hit.
    if body['pushHalfWidth'] > body['halfWidth'] or body['pushHeight'] > body['crouchHeight'] + 60:
        raise ValueError(f'{name}: pushbox must stay inside the hurtbox (width) and near crouch height')
    if 'energy' in f:
        validate_projectile(f['energy'], name+'.energy', body)
        command = f['energy']['command']
        if not command or any(type(d) is not int or not 1 <= d <= 8 for d in command):
            raise ValueError(f'{name}.energy: command uses D-pad directions 1..8')
    if 'super' in f: validate_projectile(f['super'], name+'.super', body)

def validate_move(pack, binding, m):
    animations = pack['animations']
    if ('animation' in m) == ('pose' in m):
        raise ValueError(f'{binding}: declare exactly one of animation or pose')
    if 'animation' in m:
        if m['animation'] not in animations:
            raise ValueError(f'{binding}: unknown animation')
        a = animations[m['animation']]
        if a['loop'] or 'durationsMs' not in a:
            raise ValueError(f'{binding}: attack must be a timed one-shot')
    elif binding not in POSES.get(m['pose'],()):
        raise ValueError(f'{binding}: pose {m["pose"]} is not valid for this input')
    positive(m['totalMs'], binding)
    if not 0 <= m['activeStartMs'] < m['activeEndMs'] <= m['totalMs']:
        raise ValueError(f'{binding}: active window outside totalMs')
    positive(m['reach'], binding)
    if 'hitHeight' in m: positive(m['hitHeight'], binding)
    if type(m['damage']) is not int or m['damage'] <= 0:
        raise ValueError(f'{binding}: damage must be a positive integer')

def visual_heights(pack, atlases):
    """World-space visual height (opaque pixels) measured from import reports."""
    def height(state):
        a = pack['animations'][state];cfg,report = atlases[a['atlas']]
        frames = {f['index']:f for f in report['frames']}
        top = min(frames[i]['outputBbox'][1] for i in a['frames'])
        return (report['layout']['rootY'] - top) * pack['_profile']['worldScale']
    return height('IDLE'), height('CROUCH')

def compile_packs(root, results):
    atlases = {cfg['id']:(cfg,report) for cfg,report in results}
    packs = []
    for path in sorted((root / 'characters').glob('*/character.json')):
        pack = read(path)
        if pack.get('schemaVersion') != 2 or not re.fullmatch('[a-z][a-z0-9_]*', pack['id']):
            raise ValueError(f'{path}: unsupported version (expected schemaVersion 2) or invalid id')
        if pack['id'] != path.parent.name or any(p['id'] == pack['id'] for p in packs):
            raise ValueError(f'{path}: id must be unique and match folder')
        if pack.get('artFacing') not in FACINGS:
            raise ValueError(f'{pack["id"]}: artFacing must be "right" or "left"')
        profile = read(safe(root / 'tools/sprites/profiles', pack['profile']))
        for key in ('baseFrameWidth','baseFrameHeight','worldScale','standingVisualHeight'):
            positive(profile[key], key)
        animations = pack['animations']
        if set(REQUIRED) - animations.keys():
            raise ValueError(f"{pack['id']}: missing states {sorted(set(REQUIRED) - animations.keys())}")
        present = KNOCKDOWN_SET & animations.keys()
        if present and present != KNOCKDOWN_SET:
            raise ValueError(f"{pack['id']}: KNOCKDOWN, GROUNDED and GETUP must be declared together")
        for key,a in animations.items():
            if a['atlas'] not in atlases:
                raise ValueError(f'{key}: unknown atlas {a["atlas"]}')
            cfg,report = atlases[a['atlas']]
            if cfg['profile'] != pack['profile']:
                raise ValueError(f'{key}: atlas and character profiles differ')
            frames = a['frames']
            if not frames or any(type(i) is not int or i < 0 or i >= report['layout']['frameCount'] for i in frames):
                raise ValueError(f'{key}: invalid frame index')
            if type(a['loop']) is not bool:
                raise ValueError(f'{key}: loop must be boolean')
            if 'distancePerFrame' in a:
                positive(a['distancePerFrame'], key)
                if not a['loop'] or 'durationsMs' in a:
                    raise ValueError(f'{key}: distance animation must loop and cannot have durations')
            else:
                if len(a['durationsMs']) != len(frames):
                    raise ValueError(f'{key}: one duration per frame is required')
                for d in a['durationsMs']: positive(d, key)
        moves = pack['moves']
        unknown = set(moves) - set(BINDINGS)
        if unknown:
            raise ValueError(f'{sorted(unknown)}: supported inputs are {", ".join(BINDINGS)}')
        missing = set(BINDINGS) - set(moves)
        if missing:
            raise ValueError(f'{pack["id"]}: missing moves {sorted(missing)}; use "pose" for inputs without art')
        for binding in BINDINGS: validate_move(pack, binding, moves[binding])
        specials = pack.get('specialAnimations', {})
        for key,animation in specials.items():
            if key not in SPECIALS or animation not in animations or animations[animation]['loop'] or 'durationsMs' not in animations[animation]:
                raise ValueError(f'{key}: special animation must be S/SUPER bound to a timed one-shot')
        # Free-form names are allowed only for clips something actually plays; this
        # turns a typo such as "HIT_STAN" into a build error instead of a silent fallback.
        used = {m['animation'] for m in moves.values() if 'animation' in m} | set(specials.values())
        orphan = set(animations) - set(REQUIRED) - set(OPTIONAL) - used
        if orphan:
            raise ValueError(f'{pack["id"]}: animations {sorted(orphan)} are not a known state nor used by a move')
        validate_fighter(pack)
        pack['_profile'] = profile
        pack['_visual'] = visual_heights(pack, atlases)
        # standingVisualHeight drives bbox normalization; it must describe the shipped Idle.
        measured = pack['_visual'][0] / profile['worldScale']
        if abs(measured - profile['standingVisualHeight']) > 0.05 * profile['standingVisualHeight']:
            raise ValueError(f"{pack['id']}: Idle measures {measured:.0f}px but profile standingVisualHeight is {profile['standingVisualHeight']}")
        packs.append(pack)
    roster = read(root / 'characters/roster.json')
    ids = {p['id'] for p in packs}
    opponent = roster.get('opponentCharacter')
    if roster.get('schemaVersion') != 1 or roster['defaultCharacter'] not in ids or len(roster['team']) != 2 or any(c not in ids for c in roster['team']) or opponent not in ids:
        raise ValueError('Roster requires a known default, two known team slots and a known opponentCharacter')
    if roster['defaultCharacter'] != roster['team'][0]:
        raise ValueError('Default character must match initial team slot')
    write_states(root)
    q = json.dumps
    f = lambda v: f'{float(v):.8f}f'
    def projectile(data):
        if data is None: return 'null'
        return ('new CharacterDefinition.Projectile('+str(data['damage'])+','+f(data['range'])+','+f(data['speed'])+','+f(data['spawnX'])
                +','+f(data['spawnY'])+','+f(data.get('crouchSpawnY',data['spawnY']))+','+f(data.get('airSpawnY',data['spawnY']))+')')
    lines = ['package com.gamelutagpt;', 'import java.util.*;',
             '/** Generated by build_characters.py. Edit character packs, not this file. */',
             'final class GeneratedCharacters {',
             ' static final String[] TEAM = new String[]{'+','.join(q(v) for v in roster['team'])+'};',
             ' static final String OPPONENT = '+q(opponent)+';',
             ' private static final Map<String,CharacterDefinition> ALL = build();',
             ' static CharacterDefinition get(String id) { CharacterDefinition c=ALL.get(id); if(c==null)throw new IllegalArgumentException(id);return c; }',
             ' static CharacterDefinition defaultCharacter() { return get('+q(roster['defaultCharacter'])+'); }',
             ' static CharacterDefinition opponentCharacter() { return get(OPPONENT); }',
             ' private static Map<String,CharacterDefinition> build() {',
             ' Map<String,CharacterDefinition> all=new LinkedHashMap<>();']
    for pack in packs:
        lines += [' {', ' Map<String,CharacterDefinition.Animation> a=new LinkedHashMap<>();',
                  ' Map<String,CharacterDefinition.Move> m=new LinkedHashMap<>();',
                  ' Map<String,CharacterDefinition.Animation> s=new LinkedHashMap<>();']
        for key,a in pack['animations'].items():
            cfg,report = atlases[a['atlas']]; l = report['packed']
            atlas = 'new CharacterDefinition.Atlas('+','.join([q(Path(cfg['output']).stem)]+[str(l[k]) for k in ('frameWidth','frameHeight','rootX','rootY')]+[str(l.get('columns',l['frameCount'])),str(l['frameCount'])])+')'
            lines += [' a.put('+q(key)+',new CharacterDefinition.Animation('+q(key)+','+atlas+',new int[]{'+','.join(map(str,a['frames']))+'},new float[]{'+','.join(f(v/1000) for v in a.get('durationsMs',[]))+'},'+str(a['loop']).lower()+','+f(a.get('distancePerFrame',0))+'));']
        for key in BINDINGS:
            m = pack['moves'][key]
            animation = 'a.get('+q(m['animation'])+')' if 'animation' in m else 'null'
            pose = q(m['pose']) if 'pose' in m else 'null'
            height = m.get('hitHeight', 42 if key.startswith('2') else 78)
            lines += [' m.put('+q(key)+',new CharacterDefinition.Move('+q(key)+','+animation+','+pose+','+str(m['damage'])+','+f(m['totalMs']/1000)+','+f(m['activeStartMs']/1000)+','+f(m['activeEndMs']/1000)+','+f(m['reach'])+','+f(height)+'));']
        for key,animation in pack.get('specialAnimations',{}).items():
            lines += [' s.put('+q(key)+',a.get('+q(animation)+'));']
        p = pack['_profile'];fi = pack['fighter'];body = fi['body']
        profile = 'new CharacterVisualProfile('+','.join([q(p['id']),str(p['baseFrameWidth']),str(p['baseFrameHeight']),f(p['preferredRootX']),f(p['preferredRootY']),f(p['worldScale'])])+')'
        energy = fi.get('energy')
        fighter = ('new CharacterDefinition.Fighter(0xFF'+fi['color'][1:].upper()+','+str(fi['maxLife'])
                   +',new String[]{'+','.join(q(b) for b in fi['autoCombo'])+'},'+projectile(energy)
                   +',new int[]{'+(','.join(map(str,energy['command'])) if energy else '')+'},'+projectile(fi.get('super'))
                   +',new CharacterDefinition.Body('+','.join(f(body[k]) for k in ('halfWidth','standHeight','crouchHeight','pushHalfWidth','pushHeight'))+'))')
        stand,crouch = pack['_visual']
        lines += [' all.put('+q(pack['id'])+',new CharacterDefinition('+q(pack['id'])+','+q(pack['displayName'])+','+profile+','+str(FACINGS[pack['artFacing']])+','+f(stand)+','+f(crouch)+','+fighter+',a,m,s));',' }']
    lines += [' return Collections.unmodifiableMap(all);',' }','}','']
    (root / JAVA / 'GeneratedCharacters.java').write_text('\n'.join(lines))
    return packs

def write_states(root):
    q = json.dumps
    lines = ['package com.gamelutagpt;',
             '/** Generated by build_characters.py from its state vocabulary. Do not edit. */',
             'final class SpriteStates {',' private SpriteStates() {}']
    lines += [f' static final String {s} = {q(s)};' for s in REQUIRED + OPTIONAL]
    lines += [f' static final String POSE_{p} = {q(p)};' for p in POSES]
    lines += ['}','']
    (root / JAVA / 'SpriteStates.java').write_text('\n'.join(lines))

PACK_MARGIN = 8

def pack_atlas(stage, cfg, report):
    """Crops the empty border shared by every cell of an atlas.

    The canonical cell (report['layout']) stays the authoring/validation geometry; the
    packed cell (report['packed']) is what ships in the APK and what the runtime reads.
    The crop keeps PACK_MARGIN px around any non-transparent pixel and always contains
    the root, so drawing at the root lands every pixel exactly where it was.
    """
    l = report['layout']
    w, h, n = l['frameWidth'], l['frameHeight'], l['frameCount']
    cols = l.get('columns', n);rows = (n + cols - 1) // cols
    path = stage / cfg['output']
    image = Image.open(path);image.load()
    alpha = image.convert('RGBA').getchannel('A')
    boxes = [alpha.crop((i % cols * w, i // cols * h, i % cols * w + w, i // cols * h + h)).getbbox() for i in range(n)]
    boxes = [b for b in boxes if b]
    x0 = max(0, min(min(b[0] for b in boxes) - PACK_MARGIN, l['rootX']))
    y0 = max(0, min(min(b[1] for b in boxes) - PACK_MARGIN, l['rootY']))
    x1 = min(w, max(max(b[2] for b in boxes) + PACK_MARGIN, l['rootX'] + 1))
    y1 = min(h, max(max(b[3] for b in boxes) + PACK_MARGIN, l['rootY'] + 1))
    pw, ph = x1 - x0, y1 - y0
    options = {}
    if image.mode == 'P':
        background = image.info.get('transparency', 0)
        packed = Image.new('P', (pw * cols, ph * rows), background if isinstance(background, int) else 0)
        packed.putpalette(image.getpalette())
        if 'transparency' in image.info: options['transparency'] = image.info['transparency']
    else:
        packed = Image.new(image.mode, (pw * cols, ph * rows))
    for i in range(n):
        sx, sy = i % cols * w + x0, i // cols * h + y0
        packed.paste(image.crop((sx, sy, sx + pw, sy + ph)), (i % cols * pw, i // cols * ph))
    packed.save(path, format='PNG', **options)
    report['packed'] = {'frameWidth': pw, 'frameHeight': ph, 'rootX': l['rootX'] - x0, 'rootY': l['rootY'] - y0,
                        'columns': cols, 'frameCount': n, 'cropOffset': [x0, y0],
                        'decodedBytes': {'canonical': w * h * cols * rows * 4, 'packed': pw * ph * cols * rows * 4}}

def orphans(root, outputs, results):
    """Files in generated/authored folders that no clip produces or reads."""
    found = []
    for folder,patterns in (('android/app/src/main/res/drawable-nodpi',('*.png','*.webp')),('tools/sprites/reports',('*.json',))):
        for pattern in patterns:
            found += [p.relative_to(root).as_posix() for p in sorted((root/folder).glob(pattern))]
    stale = [rel for rel in found if rel not in outputs]
    sources = {cfg['source'] for cfg,_ in results}
    sources |= {read(p).get('anatomyReference',{}).get('source') for p in (root/'tools/sprites/profiles').glob('*.json')}
    unused = [p.relative_to(root).as_posix() for p in sorted((root/'art/sprites/source').iterdir()) if p.is_file() and p.relative_to(root).as_posix() not in sources]
    return stale, unused

def build(root=ROOT, check=False):
    # Isolated staging: no partial assets/Java are published if any pack fails.
    with tempfile.TemporaryDirectory(prefix='sprite-build-') as directory:
        stage=Path(directory)
        for folder in ('art/sprites/source','tools/sprites/clips','tools/sprites/profiles','characters'):
            shutil.copytree(root / folder, stage / folder)
        (stage / JAVA).mkdir(parents=True)
        old=(importer.ROOT,importer.PROFILES_DIR,importer.GENERATED_JAVA)
        importer.ROOT=stage;importer.PROFILES_DIR=stage/'tools/sprites/profiles';importer.GENERATED_JAVA=stage/JAVA/'GeneratedSpriteLayouts.java'
        try:
            results=[];outputs=set();ids=set();names=set()
            for path in sorted((stage/'tools/sprites/clips').glob('*.json')):
                cfg=read(path)
                if not re.fullmatch('[a-z][a-z0-9_]*',cfg['id']) or not re.fullmatch('[A-Z][A-Z0-9_]*',cfg['javaName']): raise ValueError('Invalid atlas id/javaName')
                if cfg['id'] in ids or cfg['javaName'] in names: raise ValueError('Duplicate atlas id/javaName')
                ids.add(cfg['id']);names.add(cfg['javaName'])
                safe(stage,cfg['source']);safe(importer.PROFILES_DIR,cfg['profile'])
                for key in ('output','report','preview'):
                    target=cfg[key];safe(stage,target)
                    expected={'output':'android/app/src/main/res/drawable-nodpi/','report':'tools/sprites/reports/','preview':'android/app/build/sprite-review/'}[key]
                    if not target.startswith(expected) or target in outputs: raise ValueError(f'Invalid or duplicate output {target}')
                    outputs.add(target)
                cfg,report=importer.process_clip(path)
                pack_atlas(stage,cfg,report)
                report['provenance']={'pipelineVersion':2,'sha256':{
                    'source':hashlib.sha256((stage/cfg['source']).read_bytes()).hexdigest(),
                    'config':hashlib.sha256(path.read_bytes()).hexdigest(),
                    'profile':hashlib.sha256((importer.PROFILES_DIR/cfg['profile']).read_bytes()).hexdigest()}}
                (stage/cfg['report']).write_text(json.dumps(report,indent=2)+'\n')
                results.append((cfg,report))
            importer.write_generated_java(results)
            packs=compile_packs(stage,results)
            outputs.update((JAVA+'GeneratedSpriteLayouts.java',JAVA+'GeneratedCharacters.java',JAVA+'SpriteStates.java'))
            # Preview embeds atlases; it works offline and never gets packaged in APK.
            payload={'characters':packs,'atlases':{c['id']:{**r['packed'],'image':'data:image/png;base64,'+base64.b64encode((stage/c['output']).read_bytes()).decode()} for c,r in results}}
            preview='android/app/build/sprite-review/index.html'
            (stage/preview).write_text((root/'tools/sprites/preview.html').read_text().replace('__SPRITE_DATA__',json.dumps(payload).replace('<','\\u003c')))
            outputs.add(preview)
            # Removing a clip must not leave its old atlas packaged in the APK.
            orphan_outputs,unused_sources=orphans(root,outputs,results)
            if unused_sources:
                raise ValueError('Source art not referenced by any clip or profile; remove or reference it:\n'+'\n'.join(unused_sources))
            if orphan_outputs and check:
                raise ValueError('Generated files are stale (orphan outputs); run build_characters.py --write:\n'+'\n'.join(orphan_outputs))
            stale=[]
            for rel in sorted(outputs):
                generated=stage/rel; target=root/rel
                if check and '/build/' not in rel:
                    if not target.exists() or target.read_bytes()!=generated.read_bytes(): stale.append(rel)
                else:
                    target.parent.mkdir(parents=True,exist_ok=True)
                    shutil.copyfile(generated,target)
            if not check:
                for rel in orphan_outputs: (root/rel).unlink()
            if stale: raise ValueError('Generated files are stale; run build_characters.py --write:\n'+'\n'.join(stale))
            print(f'Sprite packs PASS: {len(packs)} characters, {len(results)} atlases; preview: {preview}')
        finally:
            importer.ROOT,importer.PROFILES_DIR,importer.GENERATED_JAVA=old

if __name__=='__main__':
    parser=argparse.ArgumentParser();mode=parser.add_mutually_exclusive_group()
    mode.add_argument('--check',action='store_true');mode.add_argument('--write',action='store_true')
    args=parser.parse_args()
    try: build(check=args.check)
    except (ValueError,KeyError,TypeError,OSError) as error: parser.exit(1,f'Sprite build FAILED: {error}\n')
