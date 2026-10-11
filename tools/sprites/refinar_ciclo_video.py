#!/usr/bin/env python3
"""Rebuild one reproducible RUN loop from the original video, not AI drawings.

Generic character/clip inputs; never alters gameplay physics. Finds a complete
natural cycle by matching repeated poses, keeps chronological frames, and
normalizes one animation-wide *optical mass* against the approved IDLE.
Optical mass is a review signal, not proof of anatomical proportions.
"""
import argparse
import hashlib
import json
import math
import statistics
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

from video_para_sprite import key, read_frames, registration_x

ROOT = Path(__file__).resolve().parents[2]


def alpha_area(im):
    return int(np.count_nonzero(np.asarray(im.getchannel('A')) > 10))


def cells(sheet, packed):
    w,h,c,n = (int(packed[k]) for k in ('frameWidth','frameHeight','columns','frameCount'))
    if sheet.size != (w*c, h*math.ceil(n/c)):
        raise ValueError('reference sheet/layout mismatch')
    return [sheet.crop(((i%c)*w,(i//c)*h,(i%c+1)*w,(i//c+1)*h)) for i in range(n)]


def select_cycle(video, begin, end, min_period=9, max_period=16):
    """Pose recurrence for 3 independent samples plus seam vs normal cadence."""
    keyed = {i:key(video[i],floor=True) for i in range(begin,min(len(video),end+max_period+4))}
    projected={}
    for i,im in keyed.items():
        alpha=(im[...,3]>128).astype(np.uint8)*255
        yy,xx=np.nonzero(alpha)
        if len(xx)<5000: raise ValueError(f'empty video frame {i}')
        tx=registration_x(im); bottom=int(yy.max())
        centered=cv2.warpAffine(alpha,np.float32([[1,0,620-tx],[0,1,650-bottom]]),
                                (1280,720),flags=cv2.INTER_NEAREST)
        projected[i]=cv2.resize(centered,(128,72),interpolation=cv2.INTER_AREA).astype(np.float32)/255.
    def diff(i,j):
        return float(np.mean(np.abs(projected[i]-projected[j])))
    picks=[]
    for start in range(begin,end):
        for period in range(min_period,max_period+1):
            if start+period+2 not in projected: continue
            recurrence=statistics.mean(diff(start+k,start+period+k) for k in (0,1,2))
            normal=statistics.median(diff(k,k+1) for k in range(start,start+period-1))
            seam=diff(start+period-1,start)
            penalty=max(0.,seam-normal)*.5
            picks.append((recurrence+penalty,start,period,recurrence,seam,normal))
    if not picks:raise ValueError('no source frames for a complete loop')
    score,start,period,recurrence,seam,normal=min(picks)
    if recurrence > normal*.8 or seam > max(.09,normal*2.5):
        raise ValueError(f'NO_RELIABLE_VIDEO_CYCLE recurrence={recurrence:.4f} seam={seam:.4f}')
    return start,period,{'cost':round(score,6),'recurrence':round(recurrence,6),
                         'seam':round(seam,6),'normalStep':round(normal,6)},keyed


def generate(root,video_file,character,clip_id,begin,end,total_ms):
    clip_file=root/'tools/sprites/clips'/f'{clip_id}.json'
    pack_file=root/'characters'/character/'character.json'
    cfg=json.loads(clip_file.read_text())
    ch=json.loads(pack_file.read_text())
    state=clip_id[len(character)+1:].upper()
    if cfg['id']!=clip_id or ch['id']!=character:
        raise ValueError('cross-character video output refused')
    assert state=='DASH', 'first execution limited to DASH'
    video,fps=read_frames(str(video_file))
    start,n,loop,keyed=select_cycle(video,begin,end)
    indices=list(range(start,start+n))
    # Use the real IDLE Android atlas and each approved frame to set optical mass.
    idle_state=ch['animations']['IDLE']
    idle_cfg=json.loads((root/f'tools/sprites/reports/{idle_state["atlas"]}.report.json').read_text())['packed']
    idle_sheet=Image.open(root/f'android/app/src/main/res/drawable-nodpi/{idle_state["atlas"]}.png').convert('RGBA')
    idle=cells(idle_sheet,idle_cfg)
    idle_areas=[alpha_area(idle[i]) for i in idle_state['frames']]
    if not idle_areas:raise ValueError('missing approved idle')
    # A single factor for the complete clip; zero per-frame scale adjustments.
    nominal=2.*float(cfg['video']['escala'])
    raw_counts=[int(np.count_nonzero(keyed[i][...,3]>10)) for i in indices]
    optical=math.sqrt(statistics.median(idle_areas)/statistics.median(raw_counts))/nominal
    if not .78<=optical<=1.10:
        raise ValueError(f'Optical discrepancy too large for safe automatic normalization: {optical}')
    s=nominal*optical
    fw,fh=int(cfg['frameWidth']),int(cfg['frameHeight'])
    columns=int(cfg['columns'])
    rx,ry=int(cfg['rootX']),int(cfg['rootY'])
    sheet=Image.new('RGBA',(columns*fw,math.ceil(n/columns)*fh))
    kept=[]
    for ix,source in enumerate(indices):
        rgba=keyed[source].copy()
        yy,xx=np.where(rgba[...,3]>10)
        if len(xx)<3000:raise ValueError(f'Frame {source}: no body')
        l,t,r,b=int(xx.min()),int(yy.min()),int(xx.max()+1),int(yy.max()+1)
        tx=registration_x(rgba);foot=float(yy.max())
        sub=Image.fromarray(rgba[t:b,l:r],'RGBA')
        scaled=sub.resize((round(sub.width*s),round(sub.height*s)),Image.Resampling.LANCZOS)
        x=round(rx-(tx-l)*s)
        y=round(ry-(foot-t)*s)
        cell=Image.new('RGBA',(fw,fh))
        cell.paste(scaled,(x,y))
        if min(cell.getchannel('A').getbbox()[0:2])<8 or cell.getchannel('A').getbbox()[2]>fw-8:
            raise ValueError(f'Frame {source}: atlas cropping danger')
        actual=alpha_area(cell)
        if actual<5000: raise ValueError('vanishing body')
        kept.append(actual)
        sheet.paste(cell,((ix%columns)*fw,(ix//columns)*fh))
    sheet.save(root/cfg['source'])
    cfg['expectedFrames']=n
    cfg['sourceNote']='Recovered native complete running cycle from original video; all frames chronological.'
    cfg['video']['quadros']=','.join(map(str,indices))
    cfg['video']['normalizedOpticalScale']=round(optical,6)
    clip_file.write_text(json.dumps(cfg,ensure_ascii=False,indent=2)+'\n')
    state_cfg=ch['animations'][state]
    assert state_cfg['atlas']==clip_id and state_cfg['loop']
    state_cfg['frames']=list(range(n))
    state_cfg['durationsMs']=[round((i+1)*total_ms/n)-round(i*total_ms/n) for i in range(n)]
    assert sum(state_cfg['durationsMs'])==total_ms
    pack_file.write_text(json.dumps(ch,ensure_ascii=False,indent=2)+'\n')
    report={'clip':clip_id,'character':character,'video_sha256':hashlib.sha256(video_file.read_bytes()).hexdigest(),
            'method':'chronological recurrent video cycle + one optical mass factor',
            'videoFrames':indices,'sourceFPS':fps,'loop':loop,
            'opticalScale':round(optical,6),'totalMs':total_ms,
            'idleMedianPaintedArea':statistics.median(idle_areas),
            'dashMedianPaintedArea':statistics.median(kept),
            'caveat':'Optical area can change with pose; user review required.'}
    out=root/f'tools/sprites/reports/{clip_id}_cycle.json'
    out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(report,indent=2))
    return report


if __name__=='__main__':
    ap=argparse.ArgumentParser()
    ap.add_argument('--video',type=Path,required=True)
    ap.add_argument('--character',required=True)
    ap.add_argument('--clip',required=True)
    ap.add_argument('--begin',type=int,default=18)
    ap.add_argument('--end',type=int,default=25)
    ap.add_argument('--total-ms',type=int,default=260)
    a=ap.parse_args()
    generate(ROOT,a.video,a.character,a.clip,a.begin,a.end,a.total_ms)
