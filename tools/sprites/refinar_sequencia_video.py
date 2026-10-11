#!/usr/bin/env python3
"""Regenerate a non-looping sprite clip from the source video.

Generic policy: preserve story chronology, preserve total timeline and its
relative pacing, measure a global character scale from approved standing IDLE
and the *standing* phase of the incoming clip. Never resize individual frames,
invent in-betweens, reverse movement, or touch other atlases.
"""
from __future__ import annotations
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
from refinar_ciclo_video import cells

ROOT = Path(__file__).resolve().parents[2]

def relative_timeline(indices, old_samples, durations, total):
    if len(old_samples)!=len(durations) or old_samples!=sorted(set(old_samples)):
        raise ValueError('Existing authored video timeline is not chronological')
    old_start=[0]
    for d in durations:old_start.append(old_start[-1]+d)
    if old_start[-1]!=total:raise ValueError('Timeline inconsistent')
    source_end=old_samples[-1]+max(1, round(statistics.median(np.diff(old_samples))))
    xs=old_samples+[source_end]
    boundaries=[int(round(np.interp(x,xs,old_start))) for x in indices+[source_end]]
    # Last source timestamp may be after the last chosen frame; preserve the
    # complete hold from the authored animation, not simply the video FPS.
    if indices[0]!=old_samples[0] or indices[-1]!=old_samples[-1]:
        raise ValueError('Must preserve first and last authored video frames')
    result=[b-a for a,b in zip(boundaries,boundaries[1:])]
    if min(result)<=0 or sum(result)!=total:
        raise ValueError('Re-timed movement lost total duration or created invalid frames')
    return result

def project_source_registration(bbox, ref_torso, ref_foot, root_x, root_y, scale):
    """Map an entire source frame into the authored atlas using ONE fixed origin.

    The same origin is used for every frame. This preserves displacement of
    torso, feet and the body in the source; only a uniform scale is applied.
    An independent, frame-by-frame foot/torso anchor destroys motion and must
    not be used for animations with movement.
    """
    left, top, _, _ = bbox
    if not math.isfinite(ref_torso) or not math.isfinite(ref_foot) or not math.isfinite(scale) or scale<=0:
        raise ValueError('Non-finite or non-positive clip registration')
    return round(root_x+(left-ref_torso)*scale), round(root_y+(top-ref_foot)*scale)


def reconstruct(root, video, character, clip_id, stride=2, stand_count=8):
    configpath=root/'tools/sprites/clips'/f'{clip_id}.json'
    charpath=root/'characters'/character/'character.json'
    cfg=json.loads(configpath.read_text())
    char=json.loads(charpath.read_text())
    if cfg['id']!=clip_id or char['id']!=character or not clip_id.startswith(character+'_'):
        raise ValueError('Requested character/clip mismatch')
    state=clip_id[len(character)+1:].upper()
    anim=char['animations'][state]
    if anim['atlas']!=clip_id or anim['loop']:
        raise ValueError('Non-looping character animation required')
    original_samples=[int(v) for v in cfg['video']['quadros'].split(',')]
    if not original_samples or original_samples[0]!=0:
        raise ValueError('Missing original video timeline')
    native,fps=read_frames(str(video))
    if len(native)<=original_samples[-1]:
        raise ValueError('Video truncated vs authored animation')
    total=sum(anim['durationsMs'])
    new_samples=list(range(0,original_samples[-1]+1,stride))
    if new_samples[-1]!=original_samples[-1]:new_samples.append(original_samples[-1])
    durations=relative_timeline(new_samples,original_samples,anim['durationsMs'],total)

    # Approved IDLE, never reference another character or a crouch silhouette.
    idle=char['animations']['IDLE']
    idlepack=json.loads((root/f'tools/sprites/reports/{idle["atlas"]}.report.json').read_text())['packed']
    idlesheet=Image.open(root/f'android/app/src/main/res/drawable-nodpi/{idle["atlas"]}.png').convert('RGBA')
    frames=cells(idlesheet,idlepack)
    approved=[frames[i] for i in idle['frames']]
    idleheights=[idlepack['rootY']-f.getchannel('A').getbbox()[1] for f in approved]
    baseline=statistics.median(idleheights)
    if not 150<baseline<idlepack['frameHeight']:
        raise ValueError('Invalid canonical standing reference')
    keyed=[key(native[i],floor=True) for i in new_samples]
    standing=[]
    for frame in keyed[:stand_count]:
        yy,xx=np.nonzero(frame[...,3]>10)
        if len(yy)<3000:raise ValueError('Empty standing reference in video')
        standing.append(float(yy.max()-yy.min()+1))
    # Standing first phase of intro must be independently consistent;
    # changes larger than ~8% suggest movement, not a stable calibration pose.
    subset=standing[:min(4,len(standing))]
    spread=(max(subset)-min(subset))/statistics.median(subset)
    if spread>.09:raise ValueError(f'Unstable standing reference: {spread:.3%}')
    measured=statistics.median(subset)
    scale=baseline/measured
    prior_scale=2*float(cfg['video'].get('escala',1.))
    if not .45 <= scale <= 1.15:
        raise ValueError(f'Unsafe global scale: {scale}')
    ratio=scale/prior_scale
    w,h,cols,rx,ry=[int(cfg[z]) for z in ('frameWidth','frameHeight','columns','rootX','rootY')]
    # STATIC CAMERA assumption is explicit: calibrate the global sprite origin
    # from early stable frames. Keep the *same* origin across the whole clip.
    # In particular, NEVER snap foot_y or torso_x back to root on each frame.
    registration_samples=keyed[:min(4,len(keyed))]
    reference_torso_x=statistics.median(registration_x(k) for k in registration_samples)
    reference_foot_y=statistics.median(
        float(np.nonzero(k[...,3]>10)[0].max()) for k in registration_samples
    )
    sheet=Image.new('RGBA',(w*cols,h*math.ceil(len(keyed)/cols)))
    stats=[]
    for i,(source,rgba) in enumerate(zip(new_samples,keyed)):
        ys,xs=np.where(rgba[...,3]>10)
        if len(ys)<3000:raise ValueError(f'Frame {source} has no actual character')
        l,t,r,b=int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)
        trunk=float(registration_x(rgba))
        foot=float(ys.max())
        segment=Image.fromarray(rgba[t:b,l:r],'RGBA')
        scaled=segment.resize((round(segment.width*scale),round(segment.height*scale)),Image.Resampling.LANCZOS)
        x,y=project_source_registration((l,t,r,b),reference_torso_x,reference_foot_y,
                                        rx,ry,scale)
        box=Image.new('RGBA',(w,h))
        box.paste(scaled,(x,y))
        bounds=box.getchannel('A').getbbox()
        if bounds is None or min(bounds[0],bounds[1],w-bounds[2],h-bounds[3])<8:
            raise ValueError(f'Frame {source}: real motion clips the atlas; cannot flatten or crop it: {bounds}')
        expected_foot=ry+(foot-reference_foot_y)*scale
        actual_foot=bounds[3]-1
        if abs(expected_foot-actual_foot)>2.5:
            raise ValueError(f'Frame {source}: source vertical motion was lost')
        # Check the torso, not bbox centre: limb extensions change bbox width.
        observed_trunk=registration_x(np.asarray(box))
        expected_trunk=rx+(trunk-reference_torso_x)*scale
        if abs(observed_trunk-expected_trunk)>3.5:
            raise ValueError(f'Frame {source}: source horizontal motion was lost')
        sheet.paste(box,((i%cols)*w,(i//cols)*h))
        stats.append({'videoFrame':source,
                      'sourceTorsoX':round(trunk,3),'sourceFootY':foot,
                      'sourceBBox':(l,t,r,b),'packedBBox':bounds,
                      'expectedTorsoX':round(expected_trunk,3),
                      'expectedFootY':round(expected_foot,3),
                      'renderTorsoX':round(observed_trunk,3),
                      'renderFootY':actual_foot,
                      'offsetXFromOrigin':round((trunk-reference_torso_x)*scale,3),
                      'offsetYFromOrigin':round((foot-reference_foot_y)*scale,3)})
    sheet.save(root/cfg['source'])
    anim['frames']=list(range(len(new_samples)))
    anim['durationsMs']=durations
    charpath.write_text(json.dumps(char,indent=2,ensure_ascii=False)+'\n')
    cfg['expectedFrames']=len(new_samples)
    cfg['video']['quadros']=','.join(map(str,new_samples))
    cfg['video']['derivedGlobalScale']=round(scale,6)
    cfg['sourceNote']='Source-video chronology preserved; uniform IDLE-calibrated scale and fixed initial clip origin; all native XY motion retained.'
    configpath.write_text(json.dumps(cfg,indent=2,ensure_ascii=False)+'\n')
    report={'character':character,'state':state,'source':video.name,
            'videoSHA256':hashlib.sha256(video.read_bytes()).hexdigest(),'originalFrames':len(original_samples),
            'newFrames':len(new_samples),'frames':new_samples,'sourceFps':fps,
            'oldGlobalScale':prior_scale,'newGlobalScale':round(scale,6),
            'relativeSize':round(ratio,6),'idleHeightPx':baseline,
            'sourceStandingHeight':measured,'standingSpread':round(spread,5),
            'originalTotalMs':total,'newTotalMs':sum(durations),
            'policy':'Uniform standing-IDLE scale + FIXED initial video registration; preserve every frame XY movement (no snap to root)',
            'registration':{'mode':'static-camera fixed origin, no per-frame torso/foot locking',
                            'referenceTorsoX':round(reference_torso_x,3),
                            'referenceFootY':round(reference_foot_y,3),
                            'maxHorizontalMotionPx':round(max(abs(v['offsetXFromOrigin']) for v in stats),3),
                            'maxVerticalMotionPx':round(max(abs(v['offsetYFromOrigin']) for v in stats),3)},
            'frameGeometry':stats}
    outfile=root/f'tools/sprites/reports/{clip_id}_video_refinement.json'
    outfile.write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k not in ('frames','frameGeometry')},indent=2))
    return report

if __name__=='__main__':
    ap=argparse.ArgumentParser()
    ap.add_argument('--video',type=Path,required=True)
    ap.add_argument('--character',required=True)
    ap.add_argument('--clip',required=True)
    ap.add_argument('--stride',type=int,default=2)
    ap.add_argument('--standing-samples',type=int,default=8)
    a=ap.parse_args()
    reconstruct(ROOT,a.video,a.character,a.clip,a.stride,a.standing_samples)
