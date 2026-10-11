#!/usr/bin/env python3
"""Mandatory P02 DASH quality gate: real atlas, genuine 12-phase loop, no placeholders."""
import io
import json
import math
import statistics
import subprocess
from pathlib import Path

import numpy as np
from PIL import Image
from refinar_ciclo_video import cells, alpha_area

ROOT=Path(__file__).resolve().parents[2]
BASE='826d1cd45d8dbe34ae425b79a1fc4ed2e666cc1c'


def opacity_step(a,b):
    """Common native 2x coordinate grid; stable 2D registration and no zoom."""
    ra=np.asarray(a.getchannel('A').resize((132,112),Image.Resampling.BILINEAR)).astype(float)
    rb=np.asarray(b.getchannel('A').resize((132,112),Image.Resampling.BILINEAR)).astype(float)
    return float(np.mean(np.abs(ra-rb))/255.)


def motion(rows):
    return [opacity_step(a,b) for a,b in zip(rows,rows[1:]+rows[:1])]


def validate_frame_area(idle,dash):
    canonical=statistics.median(alpha_area(x) for x in idle)
    other=statistics.median(alpha_area(x) for x in dash)
    if not .85<=other/canonical<=1.15:
        raise ValueError(f'Visual mass is uncalibrated: dash/idle={other/canonical:.3f}')
    return canonical,other


def validate_cycle(source,prior):
    if len(source)<9:raise ValueError('Not enough native in-between frames')
    old=motion(prior);new=motion(source)
    # A jump only passes if the new real adjacent video frames improve on the
    # old skipping sequence. Do NOT hide a jump by changing the threshold.
    if max(new)>=max(old)*.88:
        raise ValueError(f'Frame-continuity did not improve: max {max(old):.5f} -> {max(new):.5f}')
    if new[-1]>max(statistics.median(new[:-1])*1.85,.09):
        raise ValueError(f'Loop seam still visibly jumps: {new[-1]:.5f}')
    return dict(oldMax=max(old),newMax=max(new),oldMedian=statistics.median(old),
                newMedian=statistics.median(new),closingSeam=new[-1],newPairScores=new)


def evaluate(root=ROOT):
    p02=json.loads((root/'characters/p02/character.json').read_text())
    assert p02['animations']['IDLE']['frames']==list(range(15,45))
    clip=p02['animations']['DASH']
    ids=clip['frames']
    if ids!=list(range(len(ids))) or not 9<=len(ids)<=16 or sum(clip['durationsMs'])!=260:
        raise ValueError('Invalid chronological DASH cycle or changed movement duration')
    reports={}
    for name in ('idle','dash'):
        reports[name]=json.loads((root/f'tools/sprites/reports/p02_{name}.report.json').read_text())['packed']
    images={name:cells(Image.open(root/f'android/app/src/main/res/drawable-nodpi/p02_{name}.png').convert('RGBA'),rep)
            for name,rep in reports.items()}
    raw=subprocess.check_output(['git','show',f'{BASE}:android/app/src/main/res/drawable-nodpi/p02_dash.png'],cwd=root)
    old_pack=json.loads(subprocess.check_output(['git','show',f'{BASE}:tools/sprites/reports/p02_dash.report.json'],
                                                  cwd=root))['packed']
    old=cells(Image.open(io.BytesIO(raw)).convert('RGBA'),old_pack)[:4]
    ref=[images['idle'][i] for i in p02['animations']['IDLE']['frames']]
    newest=[images['dash'][i] for i in ids]
    optical_id,optical_dash=validate_frame_area(ref,newest)
    continuity=validate_cycle(newest,old)
    ground=(reports['dash']['rootY'])
    feet=[]
    for i,frame in enumerate(newest):
        bbox=frame.getchannel('A').getbbox()
        if bbox is None:raise ValueError(f'Blank DASH frame {i}')
        dist=abs(bbox[3]-1-ground)
        if dist>5:raise ValueError(f'Frame {i} floats/sinks {dist}px')
        feet.append(dist)
    output={'result':'AUTOMATED_QUALITY_GATE_PASS_VISUAL_REVIEW_PENDING',
            'originalFrames':4,'newFrames':len(newest),
            'dashMs':sum(clip['durationsMs']),'opticalAreaIdle':optical_id,
            'opticalAreaDash':optical_dash,'footRootDistances':feet,
            'continuity':continuity}
    dest=root/'android/app/build/sprite-review/p02-dash-cycle'
    dest.mkdir(parents=True,exist_ok=True)
    (dest/'validation.json').write_text(json.dumps(output,indent=2)+'\n')
    print(json.dumps(output,indent=2))
    return output


if __name__=='__main__':evaluate()
