#!/usr/bin/env python3
"""GIF evidence from real Android PNGs, with SpriteFighterRenderer.drawAtlas geometry.
GIF timing uses error-distributed 10ms ticks (Android uses millisecond timing).
No AI, screenshots, arbitrary preview resizes, or gameplay modifications.
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw
import escala_anatomica as measure

ROOT=Path(__file__).resolve().parents[2]
W,H,GROUND=800,470,398
WORLD_SCALE=1.075

def loader(root):
    defs=json.loads((root/'characters/p02/character.json').read_text())['animations']
    packs={}
    for state in ('IDLE','DASH'):
        atlas=defs[state]['atlas']
        p=json.loads((root/f'tools/sprites/reports/{atlas}.report.json').read_text())['packed']
        raw=measure.cells(root/f'android/app/src/main/res/drawable-nodpi/{atlas}.png',p)
        packs[state]=(p,raw,defs[state])
    return packs

def get_fixes(root):
    data=json.loads((root/'android/app/src/main/assets/sprite-autofix.json').read_text())
    table={}
    for correction in data['corrections']:
        if correction['character']!='p02': continue
        if correction['atlas']=='p02_idle':
            raise ValueError('Approved IDLE cannot be transformed')
        for i,k,dx,dy in correction['frames']:
            table[(correction['atlas'],i)]=(k,dx,dy)
    return table

def view(frame,p,scale=1.,dx=0.,dy=0.,world=WORLD_SCALE,root_world_x=W//2):
    if not 0.8<=scale<=1.25: raise ValueError('Runtime scale out of bounds')
    if p['pixelScale']!=2: raise ValueError('Unexpected pixelScale')
    factor=world/p['pixelScale']
    bitmap=frame.resize((round(frame.width*factor*scale),round(frame.height*factor*scale)),
                         Image.Resampling.BILINEAR)
    left=round(root_world_x+dx*factor-p['rootX']*factor*scale)
    top=round(GROUND+dy*factor-p['rootY']*factor*scale)
    canvas=Image.new('RGBA',(W,H),(32,36,44,255))
    d=ImageDraw.Draw(canvas)
    d.line((0,GROUND,W,GROUND),fill=(230,230,230),width=2)
    d.line((root_world_x,GROUND-4,root_world_x,GROUND+4),fill=(255,181,83),width=2)
    canvas.alpha_composite(bitmap,(left,top))
    return canvas.convert('RGB')

def transitions(root,changes):
    packs=loader(root)
    poses=[]
    for state in ('IDLE','DASH','IDLE'):
        p,frames,config=packs[state]
        for f,ms in zip(config['frames'],config['durationsMs']):
            k,dx,dy=changes.get((config['atlas'],f),(1.,0.,0.))
            poses.append((view(frames[f],p,k,dx,dy),ms))
    return poses

def durations(timeline):
    elapsed=0;rounded=0;out=[]
    for _,ms in timeline:
        elapsed+=ms;next_tick=round(elapsed/10)*10
        d=next_tick-rounded
        if d<=0:raise ValueError('GIF duration cannot encode this animation')
        out.append(d);rounded=next_tick
    return out

def save(out,frames):
    frames[0][0].save(out,save_all=True,append_images=[x[0] for x in frames[1:]],
       duration=durations(frames),loop=0,disposal=2,optimize=False)

def main(root=ROOT):
    out=root/'android/app/build/sprite-review/p02-anatomy'
    out.mkdir(parents=True,exist_ok=True)
    fixes=get_fixes(root)
    a=transitions(root,{})
    b=transitions(root,fixes)
    save(out/'01_antes.gif',a)
    save(out/'02_depois.gif',b)
    both=[]
    for (im1,ms1),(im2,ms2) in zip(a,b):
        if ms1!=ms2:raise ValueError('Unmatched timelines')
        frame=Image.new('RGB',(W*2,H));frame.paste(im1,(0,0));frame.paste(im2,(W,0))
        d=ImageDraw.Draw(frame)
        d.text((12,12),'ANTES - original renderer',fill='white')
        d.text((W+12,12),'DEPOIS - effective SpriteAutoFix',fill='white')
        both.append((frame,ms1))
    save(out/'03_lado_a_lado.gif',both)
    packs=loader(root)
    row=Image.new('RGB',(W*5,H),(32,36,44))
    p0,f0,_=packs['IDLE'];p1,f1,_=packs['DASH']
    row.paste(view(f0[15],p0),(0,0))
    for i in range(4):
        k,dx,dy=fixes.get(('p02_dash',i),(1.,0.,0.))
        row.paste(view(f1[i],p1,k,dx,dy),((i+1)*W,0))
    d=ImageDraw.Draw(row)
    for i,t in enumerate(('IDLE 15','DASH 0','DASH 1','DASH 2','DASH 3')):
        d.text((i*W+12,12),t,fill='white')
    row.save(out/'04_comparacao_estatica.png')

if __name__=='__main__':main()
