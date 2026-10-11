#!/usr/bin/env python3
"""Before/after GIFs from exact Android PNG cells, physical roots and timestamps."""
import argparse
import bisect
import json
from pathlib import Path

from PIL import Image, ImageDraw
from refinar_ciclo_video import cells

ROOT=Path(__file__).resolve().parents[2]
W,H,G=700,450,385


def pack(path,packed):
    return cells(Image.open(path).convert('RGBA'),packed)


def draw(frame,packed):
    scale=1.075/float(packed['pixelScale'])
    bitmap=frame.resize((round(frame.width*scale),round(frame.height*scale)),Image.Resampling.BILINEAR)
    root=W//2
    left=round(root-packed['rootX']*scale)
    top=round(G-packed['rootY']*scale)
    board=Image.new('RGBA',(W,H),(32,36,44,255))
    d=ImageDraw.Draw(board)
    d.line((0,G,W,G),fill='#bababa',width=2)
    board.alpha_composite(bitmap,(left,top))
    return board.convert('RGB')


def schedule(idle,idle_cfg,dash,dash_cfg):
    frames=[]
    for k,state in (('IDLE',idle_cfg),('DASH',dash_cfg),('IDLE',idle_cfg)):
        pack=idle if k=='IDLE' else dash
        for physical,ms in zip(state['frames'],state['durationsMs']):
            frames.append((pack[physical],ms,k))
    return frames


def snap(seq,t):
    cum=[0]
    for _,ms,_ in seq:cum.append(cum[-1]+ms)
    idx=min(len(seq)-1,bisect.bisect_right(cum,t)-1)
    return seq[idx][0]


def main(root,before,before_layout):
    character=json.loads((root/'characters/p02/character.json').read_text())
    idle_cfg=character['animations']['IDLE'];after_cfg=character['animations']['DASH']
    old_cfg={'frames':[0,1,2,3],'durationsMs':[65]*4}
    report_idle=json.loads((root/'tools/sprites/reports/p02_idle.report.json').read_text())['packed']
    report_after=json.loads((root/'tools/sprites/reports/p02_dash.report.json').read_text())['packed']
    report_before=json.loads(before_layout.read_text())['packed']
    idle=pack(root/'android/app/src/main/res/drawable-nodpi/p02_idle.png',report_idle)
    current=pack(root/'android/app/src/main/res/drawable-nodpi/p02_dash.png',report_after)
    original=pack(before,report_before)
    def seq(p,config):
        out=[]
        for k,state in (('IDLE',idle_cfg),('DASH',config),('IDLE',idle_cfg)):
            arr=idle if k=='IDLE' else p
            atlas=report_idle if k=='IDLE' else (report_before if p is original else report_after)
            for f,ms in zip(state['frames'],state['durationsMs']):
                out.append((draw(arr[f],atlas),ms,k))
        return out
    a,b=seq(original,old_cfg),seq(current,after_cfg)
    def edges(rows):
        values=[0]
        for _,ms,_ in rows:values.append(values[-1]+ms)
        return values
    A,B=edges(a),edges(b)
    if A[-1]!=B[-1]:raise ValueError('Dash mechanic duration changed')
    moments=sorted(set(A[:-1])|set(B[:-1]))
    end=A[-1];images=([],[],[])
    durations=[]
    for i,t in enumerate(moments):
        ix=min(len(a)-1,bisect.bisect_right(A,t)-1)
        jx=min(len(b)-1,bisect.bisect_right(B,t)-1)
        la,lb=a[ix][0],b[jx][0]
        both=Image.new('RGB',(W*2,H),(32,36,44))
        both.paste(la,(0,0));both.paste(lb,(W,0))
        d=ImageDraw.Draw(both)
        d.text((10,12),'ANTES - atlas original (4 frames)',fill='white')
        d.text((W+10,12),'DEPOIS - video original (ciclo nativo)',fill='white')
        images[0].append(la);images[1].append(lb);images[2].append(both)
        nxt=moments[i+1] if i+1<len(moments) else end
        durations.append(max(10,round((nxt-t)/10)*10))
    out=root/'android/app/build/sprite-review/p02-dash-cycle'
    out.mkdir(parents=True,exist_ok=True)
    for name,imgs in zip(('01_antes.gif','02_depois.gif','03_lado_a_lado.gif'),images):
        imgs[0].save(out/name,save_all=True,append_images=imgs[1:],
            duration=durations,loop=0,disposal=2,optimize=False)
    still=Image.new('RGB',(W*5,H),(32,36,44))
    still.paste(draw(idle[15],report_idle),(0,0))
    for k in range(4):
        ix=round(k*(len(after_cfg['frames'])-1)/3)
        still.paste(draw(current[after_cfg['frames'][ix]],report_after),((k+1)*W,0))
    still.save(out/'04_referencia_e_quatro_fases.png')
    print(f'GIF evidence: {len(moments)} synchronized poses, {end}ms from actual Android packs')


if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--before',type=Path,required=True)
    p.add_argument('--before-layout',type=Path,required=True)
    a=p.parse_args();main(ROOT,a.before,a.before_layout)
