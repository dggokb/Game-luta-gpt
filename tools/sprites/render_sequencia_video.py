#!/usr/bin/env python3
"""Real before/after INTRO GIFs, physical atlas roots and approved P02 IDLE."""
from __future__ import annotations
import argparse
import bisect
import json
import math
from pathlib import Path
from PIL import Image,ImageDraw
from refinar_ciclo_video import cells

ROOT=Path(__file__).resolve().parents[2]
W,H,FLOOR=670,460,395

def draw(cell,geometry,tag):
    f=1.075/float(geometry['pixelScale'])
    sprite=cell.resize((round(cell.width*f),round(cell.height*f)),Image.Resampling.BILINEAR)
    target=Image.new('RGBA',(W,H),(35,36,43,255))
    brush=ImageDraw.Draw(target)
    brush.line((0,FLOOR,W,FLOOR),fill='#bbbbbb',width=2)
    brush.line((W//2,FLOOR-6,W//2,FLOOR+6),fill='#f5be6e',width=2)
    left=round(W//2-geometry['rootX']*f)
    top=round(FLOOR-geometry['rootY']*f)
    target.alpha_composite(sprite,(left,top))
    brush.text((12,12),tag,fill='white')
    return target.convert('RGB')

def render_sequence(root,frames,geometry,ids,ms,tag):
    assert len(ids)==len(ms) and all(d>0 for d in ms)
    prepared=[draw(frames[i],geometry,f'{tag} | frame {j+1}/{len(ids)}') for j,i in enumerate(ids)]
    cumulative=[0]
    for d in ms:cumulative.append(cumulative[-1]+d)
    return prepared,cumulative

def produce(root,original_atlas,original_report,original_character):
    cfg=json.loads((root/'characters/p02/character.json').read_text())['animations']
    approved=cfg['IDLE']
    old=json.loads(original_character.read_text())['animations']['INTRO']
    new=cfg['INTRO']
    assert sum(old['durationsMs'])==sum(new['durationsMs'])
    idlepack=json.loads((root/'tools/sprites/reports/p02_idle.report.json').read_text())['packed']
    oldpack=json.loads(original_report.read_text())['packed']
    newpack=json.loads((root/'tools/sprites/reports/p02_intro.report.json').read_text())['packed']
    idle=cells(Image.open(root/'android/app/src/main/res/drawable-nodpi/p02_idle.png').convert('RGBA'),idlepack)
    oldframes=cells(Image.open(original_atlas).convert('RGBA'),oldpack)
    newframes=cells(Image.open(root/'android/app/src/main/res/drawable-nodpi/p02_intro.png').convert('RGBA'),newpack)
    oldviews,oldtimes=render_sequence(root,oldframes,oldpack,old['frames'],old['durationsMs'],'INTRO ANTIGA')
    newviews,newtimes=render_sequence(root,newframes,newpack,new['frames'],new['durationsMs'],'INTRO REFINADA')
    times=sorted(set(oldtimes[:-1]+newtimes[:-1]))
    if oldtimes[-1]!=newtimes[-1]:raise ValueError('Timing mismatch')
    before,after,side=[],[],[]
    duration=[]
    ref=draw(idle[approved['frames'][0]],idlepack,'IDLE APROVADO / TAMANHO CANONICO')
    head=Image.new('RGB',(W*3,H))
    head.paste(ref,(0,0))
    head.paste(oldviews[0],(W,0))
    head.paste(newviews[0],(W*2,0))
    folder=root/'android/app/build/sprite-review/p02-intro-video'
    folder.mkdir(parents=True,exist_ok=True)
    head.save(folder/'04_idle_intro_antes_depois.png')
    for idx,t in enumerate(times):
        i=max(0,bisect.bisect_right(oldtimes,t)-1)
        j=max(0,bisect.bisect_right(newtimes,t)-1)
        a,b=oldviews[i],newviews[j]
        before.append(a);after.append(b)
        combined=Image.new('RGB',(W*2,H))
        combined.paste(a,(0,0));combined.paste(b,(W,0))
        side.append(combined)
        next_t=times[idx+1] if idx+1<len(times) else oldtimes[-1]
        duration.append(max(10,int(round((next_t-t)/10))*10))
    for file,series in (('01_antes.gif',before),('02_depois.gif',after),('03_lado_a_lado.gif',side)):
        series[0].save(folder/file,save_all=True,append_images=series[1:],
                       duration=duration,loop=0,disposal=2,optimize=False)
    print(f'INTRO GIF: {len(times)} synchronized video timeline points, {oldtimes[-1]} ms, source Android atlases')

if __name__=='__main__':
    ap=argparse.ArgumentParser()
    ap.add_argument('--old-atlas',type=Path,required=True)
    ap.add_argument('--old-report',type=Path,required=True)
    ap.add_argument('--old-character',type=Path,required=True)
    a=ap.parse_args()
    produce(ROOT,a.old_atlas,a.old_report,a.old_character)
