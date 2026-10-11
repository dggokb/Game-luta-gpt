#!/usr/bin/env python3
"""Compile exactly one requested prepared-grid clip without touching any other fighter."""
import argparse
import hashlib
import json
import re
from pathlib import Path

import build_characters as compiler
import import_sprites as importer

ROOT=Path(__file__).resolve().parents[2]


def assemble(character,clip_id,root=ROOT):
    clip_path=root/'tools/sprites/clips'/f'{clip_id}.json'
    cfg=json.loads(clip_path.read_text())
    pack=json.loads((root/'characters'/character/'character.json').read_text())
    if cfg['id'] != clip_id or pack['id'] != character or not clip_id.startswith(character+'_'):
        raise ValueError('Cross-character writes are forbidden')
    state=clip_id[len(character)+1:].upper()
    if pack['animations'][state]['atlas']!=clip_id:
        raise ValueError('Atlas/animation identity mismatch')
    if cfg.get('segmentation')!='prepared-grid':
        raise ValueError('Scoped rebuild currently supports only reviewed prepared-grid sources')
    generated,report=importer.process_clip(clip_path)
    compiler.pack_atlas(root,generated,report)
    src=root/generated['source']
    profile=root/'tools/sprites/profiles'/generated['profile']
    report['provenance']={'pipelineVersion':2,'sha256':{
        'source':hashlib.sha256(src.read_bytes()).hexdigest(),
        'config':hashlib.sha256(clip_path.read_bytes()).hexdigest(),
        'profile':hashlib.sha256(profile.read_bytes()).hexdigest()}}
    if generated.get('sourceReferences'):
        report['provenance']['referenceSources']={
            item:hashlib.sha256((root/item).read_bytes()).hexdigest()
            for item in generated['sourceReferences']}
    (root/generated['report']).write_text(json.dumps(report,indent=2)+'\n')
    p=report['packed']
    animation=pack['animations'][state]
    indices=animation['frames']
    durations=animation['durationsMs']
    if len(indices)!=len(durations) or any(i<0 or i>=p['frameCount'] for i in indices):
        raise ValueError('Invalid regenerated animation indices')
    atlas='new CharacterDefinition.Atlas("%s",%s)'%(clip_id,','.join(map(str,[
        p['frameWidth'],p['frameHeight'],p['rootX'],p['rootY'],
        p['columns'],p['frameCount']]))+f',{p["pixelScale"]:.8f}f)'
    # Keep original Java serialization format, update exactly one P02 DASH line.
    line=(f' a.put("{state}",new CharacterDefinition.Animation("{state}",{atlas},'
          'new int[]{'+','.join(map(str,indices))+'},'
          'new float[]{'+','.join(f'{ms/1000:.8f}f' for ms in durations)+'},'
          f'{str(animation["loop"]).lower()},0.00000000f));')
    target=root/'android/app/src/main/java/com/gamelutagpt/GeneratedCharacters.java'
    text=target.read_text()
    matches=[row for row in text.splitlines() if f'a.put("{state}"' in row and f'Atlas("{clip_id}"' in row]
    if len(matches)!=1:
        raise ValueError(f'Cannot find unique {character}/{state} mapping in generated Java')
    target.write_text(text.replace(matches[0],line))
    layouts=root/'android/app/src/main/java/com/gamelutagpt/GeneratedSpriteLayouts.java'
    old=layouts.read_text();new=old
    for field,value in (('FRAME_WIDTH',p['frameWidth']),('FRAME_HEIGHT',p['frameHeight']),
                        ('ROOT_X',p['rootX']),('ROOT_Y',p['rootY']),
                        ('FRAME_COUNT',p['frameCount'])):
        regex=r'(static final int '+clip_id.upper()+'_'+field+r' = )\d+;'
        new,count=re.subn(regex,lambda m:m.group(1)+str(value)+';',new)
        if count!=1:raise ValueError(f'Missing generated layout field {field}')
    layouts.write_text(new)
    print(f'SCOPED SPRITE BUILD PASS: {character}/{state}, {len(indices)} frames, '
          f'{p["frameWidth"]}x{p["frameHeight"]}, only one atlas and Java mapping changed')
    return report


if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--character',required=True)
    parser.add_argument('--clip',required=True)
    a=parser.parse_args();assemble(a.character,a.clip)
