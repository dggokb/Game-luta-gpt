#!/usr/bin/env python3
"""Fail-closed anatomical-scale auditor, read-only unless explicitly --apply.

The approved IDLE defines a distribution, not a single bounding-box height.
Colour-labelled cranial landmarks are calibrated per character and measured in
opaque atlas pixels. Uniform scaling requires corroborating, pose-tolerant
measures; ambiguity is REVIEW, never a size PASS or an unsafe resize.
"""
from __future__ import annotations
import argparse
import json
import math
import statistics
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]


def cells(path, geometry):
    sheet = Image.open(path).convert('RGBA')
    w, h, columns, count = (geometry[k] for k in ('frameWidth', 'frameHeight', 'columns', 'frameCount'))
    if sheet.size != (w * columns, h * math.ceil(count / columns)):
        raise ValueError(f'{path}: atlas/layout mismatch')
    return [sheet.crop((i % columns * w, i // columns * h,
                        (i % columns + 1) * w, (i // columns + 1) * h)) for i in range(count)]


def landmark(frame, config):
    rgba = np.asarray(frame.convert('RGBA'))
    alpha = rgba[:, :, 3] > 10
    ys, _ = np.nonzero(alpha)
    if not len(ys):
        return None
    y0, y1 = int(ys.min()), int(ys.max())
    rgb = rgba[:, :, :3].astype(np.int16)
    limit = y0 + max(1, round((y1 - y0 + 1) * config['headTopFraction']))
    # Character-specific colour bounds come from the reviewed IDLE art.
    # Other characters MUST supply a calibrated mask instead of relying on
    # a generic head/skin colour guess.
    low, high = config['rgbMin'], config['rgbMax']
    colour = alpha & np.all(rgb >= low, axis=2) & np.all(rgb <= high, axis=2)
    colour[limit:] = False
    if config.get('minBtoGRatio') is not None:
        colour &= rgb[:, :, 2] >= rgb[:, :, 1] * config['minBtoGRatio']
    _, _, stats, _ = cv2.connectedComponentsWithStats(colour.astype(np.uint8), 8)
    matches = sorted((s for s in stats[1:] if s[4] >= config['minPixels']), key=lambda s:s[4], reverse=True)
    if not matches:
        return None
    x, y, w, h, area = [int(v) for v in matches[0]]
    if y - y0 > (y1 - y0 + 1) * config['topToleranceFraction']:
        return None
    return {'area': area, 'envelope': math.sqrt(w * h), 'bbox': [x, y, w, h]}


def envelope(values, lower=.05, upper=.95):
    return [float(np.quantile(values, lower)), float(np.quantile(values, upper))]


def calibrate(idle, indices, cfg):
    rows = [landmark(idle[i], cfg) for i in indices]
    if any(x is None for x in rows):
        raise ValueError('Approved IDLE has frames without a reliable cranial landmark')
    data = {}
    for key in ('area', 'envelope'):
        values = [x[key] ** (.5 if key == 'area' else 1.) for x in rows]
        data[key] = {'median': float(statistics.median(values)), 'range': envelope(values),
                     'min': min(values), 'max': max(values)}
    return data


def classify(measure, baseline, cfg):
    if measure is None:
        return {'status': 'REVIEW', 'reason': 'head landmark not confidently identifiable', 'size': 1.}
    values = {'area': math.sqrt(measure['area']), 'envelope': measure['envelope']}
    factors = {key: baseline[key]['median'] / value for key, value in values.items()}
    eps = cfg['rasterUncertainty']
    signs = {}
    for key, value in values.items():
        low, high = baseline[key]['range']
        signs[key] = -1 if value < low * (1 - eps) else (1 if value > high * (1 + eps) else 0)
    candidate = statistics.median(factors.values())
    row = {'status': 'STABLE', 'reason': 'cranial measures overlap approved IDLE variation',
           'size': 1., 'rawSizeEstimate': round(candidate, 6),
           'scaleCandidates': {k:round(v,6) for k,v in factors.items()},
           'signs': signs, 'landmark': measure}
    if signs['area'] != 0 and signs['area'] == signs['envelope']:
        disagreement = abs(math.log(factors['area'] / factors['envelope']))
        if disagreement <= cfg['maxScaleDisagreement']:
            row.update(status='SCALE_CANDIDATE', reason='two cranial features outside IDLE envelope',
                       size=round(candidate, 6))
        else:
            row.update(status='REVIEW',reason='head area and envelope disagree: pose/deformation')
    elif any(signs.values()):
        row.update(status='REVIEW', reason='anatomical landmarks disagree; cannot infer uniform scale')
    return row


def decide(rows, cfg):
    """Reject unsupported all-frame corrections; isolate same-state outliers."""
    accepted = []
    for i, row in enumerate(rows):
        if row['status'] != 'SCALE_CANDIDATE':
            continue
        peers = [x for j,x in enumerate(rows) if i!=j and x['status']=='STABLE']
        if len(peers)<2:
            row.update(status='REVIEW',reason='no independent stable neighbours; uniform clip scale uncertain',size=1.)
            continue
        pvals = [x['landmark'] for x in peers]
        local = {
            'area': statistics.median(math.sqrt(x['area']) for x in pvals),
            'envelope': statistics.median(x['envelope'] for x in pvals)
        }
        target = row['landmark']
        karea = local['area'] / math.sqrt(target['area'])
        kenv = local['envelope'] / target['envelope']
        if abs(math.log(karea/kenv))>cfg['maxScaleDisagreement']:
            row.update(status='REVIEW',reason='peer anatomy estimates disagree',size=1.)
            continue
        k=statistics.median([karea,kenv])
        if not cfg['minAutoScale']<=k<=cfg['maxAutoScale']:
            row.update(status='REVIEW',reason='candidate outside safe non-destructive runtime range',size=1.)
            continue
        if (k-1)*(row['rawSizeEstimate']-1)<=0:
            row.update(status='REVIEW',reason='IDLE/peer estimates contradict',size=1.)
            continue
        row.update(status='CORRECTABLE',reason='IDLE and at least two stable DASH peers agree',size=round(k,6))
        accepted.append(i)
    return accepted


def audit(idle, dash, idle_ids, dash_ids, cfg):
    base=calibrate(idle,idle_ids,cfg)
    rows=[classify(landmark(dash[i],cfg),base,cfg) for i in dash_ids]
    indices=decide(rows,cfg)
    return base, rows, indices


def corrections(character, atlas, ids, rows, existing):
    fixes=json.loads(json.dumps(existing))
    if fixes.get('schema')!=1 or not isinstance(fixes.get('corrections'),list):
        raise ValueError('Unexpected SpriteAutoFix format')
    if any(e.get('character')==character and e.get('atlas')==atlas for e in fixes['corrections']):
        raise ValueError('Existing corrections require explicit review; refusing overwrite')
    chosen=[[ids[i],r['size'],0.,0.] for i,r in enumerate(rows) if r['status']=='CORRECTABLE']
    if chosen:
        fixes['corrections'].append({'character':character,'atlas':atlas,'state':'DASH',
            'frames':chosen,'evidence':{'method':'cranial IDLE + two same-state peers',
            'review':'HUMAN_APPROVAL_REQUIRED'}})
    return fixes


def run(root=ROOT, apply=False):
    config=json.loads((root/'tools/sprites/anatomy/p02.json').read_text())
    char=json.loads((root/'characters/p02/character.json').read_text())
    packs={}
    for kind in ('idle','dash'):
        report=json.loads((root/f'tools/sprites/reports/p02_{kind}.report.json').read_text())
        packs[kind]=cells(root/f'android/app/src/main/res/drawable-nodpi/p02_{kind}.png',report['packed'])
        if report['packed']['pixelScale']!=2 or report['worldScale']!=1.075:
            raise ValueError('Unexpected world/pixel scale; renderer projection must be re-audited')
    idle_ids=char['animations']['IDLE']['frames'];dash_ids=char['animations']['DASH']['frames']
    if idle_ids!=list(range(15,45)) or dash_ids!=[0,1,2,3]:
        raise ValueError('Approved P02 timeline changed')
    baseline,rows,idx=audit(packs['idle'],packs['dash'],idle_ids,dash_ids,config)
    data={'character':'p02','reference':'IDLE approved frames 15..44',
          'method':'color-labelled cranial area + cranial envelope; conservative gate',
          'baseline':baseline,'dash':[{**x,'frame':f} for x,f in zip(rows,dash_ids)],
          'correctableFrames':[dash_ids[i] for i in idx],
          'status':'CORRECTION_AVAILABLE' if idx else ('REVIEW' if any(x['status']=='REVIEW' for x in rows) else 'NO_SCALE_CHANGE_JUSTIFIED')}
    out=root/'android/app/build/sprite-review/p02-anatomy'
    out.mkdir(parents=True,exist_ok=True)
    (out/'report.json').write_text(json.dumps(data,indent=2)+'\n')
    if apply:
        path=root/'android/app/src/main/assets/sprite-autofix.json'
        current=json.loads(path.read_text());updated=corrections('p02','p02_dash',dash_ids,rows,current)
        if idx:
            backup=out/'sprite-autofix.before.json';backup.write_text(json.dumps(current,indent=2)+'\n')
            path.write_text(json.dumps(updated,indent=2)+'\n')
        else:
            print('No demonstrably correct DASH scaling; existing runtime asset preserved.')
    print(json.dumps({'status':data['status'],'correctableFrames':data['correctableFrames'],
                      'frameStatus':[x['status'] for x in rows]},indent=2))
    return data

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--apply',action='store_true');run(apply=p.parse_args().apply)
