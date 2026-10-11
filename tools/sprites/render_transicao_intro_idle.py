#!/usr/bin/env python3
"""Original and corrected full-state INTRO -> IDLE render at gameplay clocks.

Generic: accepts --character and uses the actual generated Android atlas,
clip indices/durations, roots, worldScale, pixelScale, and runtime interpolation.
No new drawings, no approximations based on canvas box dimensions.
Only the transition (not the authored animation/IDLE files) is changed.
"""
import argparse
import bisect
import json
import math
from pathlib import Path
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parents[2]
W,H,ROOTX,BASEY=690,490,340,420

def unpack(root,atlas):
    info=json.loads((root/f'tools/sprites/reports/{atlas}.report.json').read_text())['packed']
    bitmap=Image.open(root/f'android/app/src/main/res/drawable-nodpi/{atlas}.png').convert('RGBA')
    w,h,c,n=(info[k] for k in ('frameWidth','frameHeight','columns','frameCount'))
    assert bitmap.size==(w*c,h*math.ceil(n/c))
    frames=[bitmap.crop(((i%c)*w,(i//c)*h,(i%c+1)*w,(i//c+1)*h)) for i in range(n)]
    return info,frames

def frame_at(state,t):
    if state.get('loop',False):t=t%sum(state['durationsMs'])
    for i,d in zip(state['frames'],state['durationsMs']):
        if t<d:return i
        t-=d
    return state['frames'][-1]

def fade(intro,t):
    total=sum(intro['durationsMs'])
    window=min(420,total*0.16)
    if not window:return 0.0
    p=max(0.,min(1.,(t-(total-window))/window))
    return p*p*(3.-2.*p)

def draw(canvas,cell,geometry,world,opacity):
    scale=world/geometry['pixelScale']
    scaled=cell.resize((round(cell.width*scale),round(cell.height*scale)),Image.Resampling.BILINEAR)
    if opacity<255:
        alpha=scaled.getchannel('A').point(lambda a:round(a*opacity/255))
        scaled.putalpha(alpha)
    x=round(ROOTX-geometry['rootX']*scale)
    y=round(BASEY-geometry['rootY']*scale)
    canvas.alpha_composite(scaled,(x,y))

def panel(intro,idle,time,blended,world,total,elapsed):
    image=Image.new('RGBA',(W,H),(32,35,45,255))
    cfg_intro,frames_intro,def_intro=intro
    cfg_idle,frames_idle,def_idle=idle
    if time<total:
        f=frames_intro[frame_at(def_intro,time)]
        k=fade(def_intro,time) if blended else 0
        if k<1:draw(image,f,cfg_intro,world,round(255*(1-k)))
        if k>0:draw(image,frames_idle[frame_at(def_idle,0)],cfg_idle,world,round(255*k))
    else:
        draw(image,frames_idle[frame_at(def_idle,time-total)],cfg_idle,world,255)
    d=ImageDraw.Draw(image)
    d.line((0,BASEY,W,BASEY),fill=(210,210,210),width=2)
    d.text((12,12),('DEPOIS - TRANSICAO DO MOTOR' if blended else 'ANTES - TROCA SECA'),fill='white')
    d.text((12,34),f'tempo: {elapsed/1000:.2f}s | '+('INTRO' if time<total else 'IDLE'),fill='white')
    return image.convert('RGB')

def error(a,b):
    # Same exact ground, root and bitmap transforms; all BG identical.
    import numpy as np
    x=np.asarray(a.crop((65,58,W-65,H-12)).convert('RGB')).astype('int16')
    y=np.asarray(b.crop((65,58,W-65,H-12)).convert('RGB')).astype('int16')
    return float(np.abs(x-y).mean()/255.)

def main(root,character,out):
    ch=json.loads((root/f'characters/{character}/character.json').read_text())
    cfg=ch['animations']
    intro_state=cfg['INTRO'];idle_state=cfg['IDLE']
    if intro_state['loop'] or not idle_state['loop']:
        raise ValueError('Must be a one-shot intro followed by a looping IDLE')
    intro=(*unpack(root,intro_state['atlas']),intro_state)
    idle=(*unpack(root,idle_state['atlas']),idle_state)
    world=float(json.loads((root/f'tools/sprites/reports/{intro_state["atlas"]}.report.json').read_text())['worldScale'])
    total=sum(intro_state['durationsMs'])
    # Fade occupies the final 420 ms; show about 900 ms before and 700 ms after.
    begin=max(0,total-900);end=total+700;step=20
    times=list(range(begin,end+1,step))
    before=[];after=[];both=[]
    for t in times:
        a=panel(intro,idle,t,False,world,total,t-begin)
        b=panel(intro,idle,t,True,world,total,t-begin)
        before.append(a);after.append(b)
        pair=Image.new('RGB',(W*2,H))
        pair.paste(a,(0,0));pair.paste(b,(W,0))
        both.append(pair)
    out.mkdir(parents=True,exist_ok=True)
    for name,frames in (('01_antes_intro_idle.gif',before),
                        ('02_depois_intro_idle.gif',after),
                        ('03_comparacao_intro_idle.gif',both)):
        frames[0].save(out/name,save_all=True,append_images=frames[1:],
                       duration=step,disposal=2,loop=0,optimize=False)
    idle_first=panel(intro,idle,total,False,world,total,0)
    old_end=panel(intro,idle,total-20,False,world,total,0)
    new_end=panel(intro,idle,total-20,True,world,total,0)
    old_error=error(old_end,idle_first);new_error=error(new_end,idle_first)
    if old_error<.003:raise ValueError('No measurable ORIGINAL boundary problem')
    if new_error>=max(.003,old_error*.15):
        raise ValueError(f'False fix! INTRO->IDLE boundary old={old_error:.5f} new={new_error:.5f}')
    report={'character':character,'introAtlas':intro_state['atlas'],'idleAtlas':idle_state['atlas'],
            'introDurationMs':total,'endToIdleBefore':round(old_error,6),
            'endToIdleAfter':round(new_error,6),
            'boundaryReductionPct':round(100*(1-new_error/old_error),2),
            'blendMs':min(420,total*.16),
            'usedRealAndroidAtlases':True,'unchangedArt':True,
            'note':'A much smaller boundary difference is necessary but not sufficient for visual approval.'}
    (out/'runtime_transition.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--character',required=True)
    parser.add_argument('--out',type=Path,default=ROOT/'android/app/build/sprite-review/intro-idle-transition')
    x=parser.parse_args()
    main(ROOT,x.character,x.out)
