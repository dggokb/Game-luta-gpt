#!/usr/bin/env python3
"""Draft registration of existing P12 video pixels, never art approval.

Reuses the video engine's palette cleanup and anatomically measured, whole-video
scale. All removals are separate grayscale masks. This command writes only a
review directory; production preflight remains responsible for rejecting drafts.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw

from p12_video_sources import ROOT, STATE_FILES, sha, write_json
from p12_video_review import PLAN, CONTRACT, resolve_selection
from p12_video_cycle import render_pose, FW, FH, RX, RY
from limpar import clean_cell, hsv_bins, PALETTE_SHARE
from tamanho import match_zoom

CALIBRATION = ROOT / 'tools/sprites/videos/p12-video-scale-draft.json'
REGISTERED = {'IDLE', 'COMBAT', 'WALK_FORWARD', 'WALK_BACK', 'CROUCH', 'RISE',
              'LAND', 'DASH', 'BACKDASH', 'JUMP', 'FALL', 'DEFENSE_STAND',
              'DEFENSE_CROUCH', 'DEFENSE_AIR', 'HIT_STAND', 'HIT_CROUCH', 'GETUP'}
AIR = {'HIT_AIR', 'JUMP_LIGHT', 'JUMP_MEDIUM', 'JUMP_HEAVY', 'JUMP_HEAVY_DOWN',
       'SPECIAL_S2', 'SPECIAL_S4', 'KNOCKDOWN', 'GROUNDED'}
HIP_REGISTERED = {'JUMP', 'FALL', 'DEFENSE_AIR', 'HIT_AIR', 'JUMP_LIGHT',
                  'JUMP_MEDIUM', 'JUMP_HEAVY', 'JUMP_HEAVY_DOWN'}
NO_FX = {'idle', 'frente', 'tras', 'agachado', 'backDash', 'intro',
         'vitoria', 'derrota', 'provocacao', 'defendeBaixo', 'defesaPulo'}


def torso(rgba):
    ys, xs = np.where(rgba[..., 3] > 10)
    h = int(ys.max() - ys.min() + 1)
    mid = []
    for y in range(int(ys.min()) + int(h * .30), int(ys.min()) + int(h * .55)):
        row = np.where(rgba[y, :, 3] > 10)[0]
        if len(row):
            mid.append((int(row.min()) + int(row.max())) / 2)
    return float(np.median(mid))


def foot(rgba):
    mask = rgba[..., 3] > 128
    ys, xs = np.where(mask)
    floor = int(ys.max())
    band = mask[max(0, floor - max(2, round((floor - int(ys.min())) * .025))):floor + 1]
    cols = np.flatnonzero(band.any(axis=0))
    splits = np.split(cols, np.where(np.diff(cols) > 1)[0] + 1)
    # A planted rear foot, not the complete two-foot silhouette centre.
    valid = [s for s in splits if len(s) >= 4]
    return (float(np.mean(valid[0])) if valid else float(np.mean(cols))), floor


def source_palette(extraction):
    count = np.zeros(24 * 256)
    # Neutral source videos only: no rejected runtime Idle or enemy colours.
    for name in ('idle', 'frente', 'tras', 'agachado', 'defendeBaixo'):
        for index in (0, 24, 48, 72, 96):
            rgba = np.array(Image.open(extraction / name / f'{index:04d}.png').convert('RGBA'))
            count += np.bincount(hsv_bins(rgba)[rgba[..., 3] > 128], minlength=len(count))
    return count >= count.sum() * PALETTE_SHARE


def waist_row(rgba):
    """Upper part of the red sash: an intrinsic landmark, not a foot/bbox.

    In air, unfolding the knees must not lift the entire torso. This measured
    landmark registers the pelvis while the combat engine supplies world Y.
    Unidentifiable landmarks reject the draft instead of guessing a pivot.
    """
    r, g, b = [rgba[..., i].astype(float) for i in range(3)]
    red = ((rgba[..., 3] > 128) & (r > 35) & (r > 1.7 * g) & (r > 1.7 * b)).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(red, connectivity=8)
    if count < 2 or stats[1:, cv2.CC_STAT_AREA].max() < 50:
        raise ValueError('red waist landmark not identifiable in original source')
    largest = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    rows = np.where(labels == largest)[0]
    return float(np.quantile(rows, .20))


def generate(extraction, output):
    # Drafts cannot overwrite shipping sprites or a reviewed registration.
    output = output.resolve()
    allowed = [(ROOT / 'android/app/build').resolve(), (ROOT / 'docs/art/p12-video-normalized-review').resolve()]
    if not any(output == p or p in output.parents for p in allowed):
        raise ValueError('draft output must be a build directory or normalized-review directory')
    output.mkdir(parents=True, exist_ok=True)
    cv2.setNumThreads(1)
    plan, contract, calibration = [json.loads(p.read_text()) for p in (PLAN, CONTRACT, CALIBRATION)]
    if plan['sourcePolicy'] != 'EXISTING_DRIVE_VIDEOS_ONLY' or set(plan['states']) != set(contract['animations']):
        raise ValueError('all 42 real video selections required')
    known = source_palette(extraction)
    idle = np.array(Image.open(extraction / 'idle/0000.png').convert('RGBA'))
    idle_back, _ = foot(idle)
    back_offset = (idle_back - torso(idle)) * calibration['videos']['idle']['scale']
    hip_offset = (foot(idle)[1] - waist_row(idle)) * calibration['videos']['idle']['scale']
    report = {'status': 'DRAFT_NOT_APPROVED', 'sourcePolicy': plan['sourcePolicy'],
              'calibrationSha256': sha(CALIBRATION), 'contractSha256': sha(CONTRACT),
              'canvas': [FW, FH], 'root': [RX, RY], 'states': {}}
    cards = []
    for state, animation in contract['animations'].items():
        spec = plan['states'][state]
        stem = Path(STATE_FILES[state]).stem
        folder = extraction / stem
        record = json.loads((folder / 'frames.json').read_text())
        durable = json.loads((ROOT / 'docs/art/p12-video-sources' / (stem + '.frames.json')).read_text())
        if record['videoSha256'] != durable['videoSha256'] or record['frames'] != durable['frames']:
            raise ValueError(state + ': original extraction differs from durable provenance')
        sources, peak = resolve_selection(state, spec, animation, record, folder)
        anchor = [RX, 256 if state in HIP_REGISTERED else RY]
        cells, recipes, native = [], [], []
        for ordinal, (source_name, index, record, folder) in enumerate(sources):
            stem = Path(source_name).stem
            baseline = calibration['videos'][stem]['scale']
            reference_index = calibration['videos'][stem].get('headReferenceFrame', 0)
            reference_path = folder / f'{reference_index:04d}.png'
            if sha(reference_path) != calibration['videos'][stem]['referenceFrameSha256']:
                raise ValueError(state + ': head calibration belongs to a different source frame')
            first = np.array(Image.open(reference_path).convert('RGBA'))
            box = calibration['videos'][stem]['headBox']
            head = first[box[1]:box[3], box[0]:box[2]]
            ground = max(f['sourceBox'][3] for f in record['frames']) - 1
            entry = record['frames'][index]
            source = Image.open(folder / entry['file']).convert('RGBA')
            rgba = np.array(source)
            after, erased = (rgba.copy(), 0) if stem in NO_FX else clean_cell(rgba, known)
            # Per-frame camera correction is measured from the SAME source
            # head, not from total body height (crouching changes that height).
            cache_key = hashlib.sha256((calibration['videos'][stem]['referenceFrameSha256']
                + repr(box) + entry['sha256'] + 'head-zoom-v1').encode()).hexdigest()
            cache_path = ROOT / 'android/app/build/p12-head-cache' / (cache_key + '.json')
            if cache_path.exists():
                score, zoom = json.loads(cache_path.read_text())
            else:
                score, zoom = match_zoom(head, rgba, .65, 1.8)
                write_json(cache_path, [score, zoom])
            scale = baseline / zoom if score >= .7 else baseline
            # A mask is incapable of drawing/reconstructing occluded body parts.
            removal = np.full(rgba.shape[:2], 255, dtype=np.uint8)
            removal[(rgba[..., 3] > 0) & (after[..., 3] == 0)] = 0
            mask_file = (stem + '_' if spec.get('frameSources') is not None else '') + entry['file']
            mask_path = output / 'masks' / state.lower() / mask_file
            if (removal == 0).any():
                mask_path.parent.mkdir(parents=True, exist_ok=True)
                Image.fromarray(removal).save(mask_path)
            back, floor = foot(after)
            root_x = torso(after) if state in REGISTERED or state in AIR else back - back_offset / scale
            root_y = ground - entry['sourceBox'][1] if state in {'BACKDASH','SPECIAL_S2','SPECIAL_S4'} else floor
            if state in HIP_REGISTERED and not (state == 'JUMP' and index < 40):
                root_y = waist_row(after) + hip_offset / scale
            frame = render_pose(Image.fromarray(after), scale, [root_x, root_y], anchor)
            path = output / 'native' / state.lower() / f'{ordinal:02d}.png'
            path.parent.mkdir(parents=True, exist_ok=True)
            frame.save(path)
            native.append(frame)
            shown = Image.new('RGBA', (FW, FH), '#20242d')
            shown.alpha_composite(frame)
            draw = ImageDraw.Draw(shown)
            draw.line((0, anchor[1] + 1, FW, anchor[1] + 1), fill='#445362')
            draw.line((RX, anchor[1] - 7, RX, anchor[1] + 7), fill='#7ca0bf')
            draw.text((8, 8), f'{state} / {source_name} {index}' + ((' / IMPACT' if 'impactFrame' in animation else ' / MAIN') if ordinal == peak else ''), fill='white')
            draw.text((8, 24), 'DRAFT / NOT APPROVED', fill='#ffc078')
            cells.append(shown.convert('RGB'))
            recipes.append({'videoFrame': index, 'sourceTimeSeconds': entry['time'],
                'videoName': source_name, 'driveId': record['driveId'],
                'videoSha256': record['videoSha256'], 'maskFile': mask_file,
                'sourceSha256': entry['sha256'], 'sourceBox': entry['sourceBox'],
                'scale': scale, 'root': [root_x, root_y], 'anchor': anchor,
                'airHipRegistration': state in HIP_REGISTERED and not (state == 'JUMP' and index < 40),
                'removedSourcePixels': int((removal == 0).sum()),
                'headTemplateScore': score, 'headTemplateZoom': zoom,
                'cameraCorrectionConfirmed': score >= .7,
                'maskSha256': sha(mask_path) if (removal == 0).any() else None,
                'nativePixelSha256': hashlib.sha256(frame.tobytes()).hexdigest()})
        durations = animation.get('durationsMs')
        if durations is None:
            pts = [r['sourceTimeSeconds'] for r in recipes]
            durations = [(b-a)*1000 for a,b in zip(pts,pts[1:])]
            durations += durations[-1:]
        cells[0].save(output / (state.lower() + '.gif'), save_all=True, append_images=cells[1:],
                      duration=[max(10,round(d/10)*10) for d in durations], loop=0, disposal=2)
        contact = Image.new('RGB', (FW * len(cells), FH), '#20242d')
        for i, cell in enumerate(cells):
            contact.paste(cell, (i*FW, 0))
        contact.save(output / (state.lower() + '-all.jpg'), quality=91)
        card = Image.new('RGB', (FW*3, FH + 36), '#20242d')
        for i, idx in enumerate((0, peak, len(cells)-1)):
            card.paste(cells[idx], (i*FW, 0))
        ImageDraw.Draw(card).text((8,FH+8), spec['observedMotion'][:200], fill='#bbcbdc')
        cards.append(card)
        report['states'][state] = {'status': 'DRAFT_NOT_APPROVED',
            'anchor': anchor,
            'videoOrigin': {'driveId': durable['driveId'], 'videoSha256': durable['videoSha256']},
            'mainIndex': peak, 'recipes': recipes, 'observedMotion': spec['observedMotion'],
            'findings': spec['findings'],
            'pending': ['manual mask inspection', 'anatomy scale confirmation', 'support tracking', 'transitions', 'semantic movement audit']}
        print(f'{state}: original pixels registered, NOT APPROVED', flush=True)
    for start in range(0,len(cards),7):
        sheet = Image.new('RGB', (FW*3,(FH+36)*len(cards[start:start+7])), '#20242d')
        for i,card in enumerate(cards[start:start+7]):
            sheet.paste(card,(0,i*(FH+36)))
        sheet.save(output / f'page-{start//7+1:02d}.jpg', quality=91)
    write_json(output / 'registration-draft.json',report)
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--extraction',type=Path,default=ROOT/'android/app/build/p12-video-extraction')
    parser.add_argument('--output',type=Path,default=ROOT/'android/app/build/p12-video-normalized-review')
    args = parser.parse_args()
    generate(args.extraction,args.output)
