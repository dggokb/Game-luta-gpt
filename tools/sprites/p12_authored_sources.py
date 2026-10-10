#!/usr/bin/env python3
"""Extract independently drawn P12 poses without warping or approving them.

Connected components prevent a long fist from being clipped at nominal grid
boundaries. The source pixels and their alpha are preserved. This stage cannot
pass the runtime source gate: only the renderer may bind consumed references.
"""
import hashlib
import json
import math
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SHEETS = ROOT / 'art/keys/p12/sheets'
POSES = ROOT / 'art/keys/p12/poses'
LEGACY = {'IDLE': (8, 3), 'WALK_FORWARD': (8, 3), 'WALK_BACK': (8, 3),
          'CROUCH': (10, 8), 'DASH': (8, 3), 'LIGHT_JAB': (10, 2),
          'MEDIUM_KICK': (10, 2), 'HEAVY_STRAIGHT': (10, 5),
          'CROUCH_LIGHT': (8, 3), 'CROUCH_MEDIUM': (8, 3),
          'CROUCH_HEAVY': (8, 3), 'JUMP_LIGHT': (8, 3)}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')


def extract(sheet, expected, columns=None):
    rgba = np.asarray(Image.open(sheet).convert('RGBA')).copy()
    if rgba[:, :, 3].min() != 0 or rgba[:, :, 3].max() < 240:
        raise ValueError(f'{sheet.name}: genuine transparent foreground required')
    n, labels, stats, centers = cv2.connectedComponentsWithStats(
        (rgba[:, :, 3] > 10).astype(np.uint8), 8)
    bodies = [i for i in range(1, n) if stats[i, cv2.CC_STAT_AREA] > 1500]
    if len(bodies) != expected:
        raise ValueError(f'{sheet.name}: expected {expected} isolated figures, got {len(bodies)}')
    bodies.sort(key=lambda i: centers[i, 1])
    columns = columns or expected // 2
    if expected % columns:
        raise ValueError('figure count must form complete rows')
    bodies = [i for start in range(0, expected, columns)
              for i in sorted(bodies[start:start + columns], key=lambda i: centers[i, 0])]
    result = []
    for component in bodies:
        mask = cv2.dilate((labels == component).astype(np.uint8), np.ones((3, 3), np.uint8))
        ys, xs = np.where(mask)
        x, y, right, bottom = xs.min(), ys.min(), xs.max() + 1, ys.max() + 1
        if min(x, y, rgba.shape[1] - right, rgba.shape[0] - bottom) == 0:
            raise ValueError(f'{sheet.name}: figure touches sheet border')
        pixels = rgba[y:bottom, x:right].copy()
        pixels[:, :, 3] *= mask[y:bottom, x:right]
        pixels[pixels[:, :, 3] == 0, :3] = 0
        result.append((Image.fromarray(pixels), [int(x), int(y), int(right), int(bottom)], component))
    return result


def main():
    pack = json.loads((ROOT / 'characters/p12/character.json').read_text())
    manifest = {'character': 'p12', 'approval': 'PENDING', 'states': {}, 'errors': []}
    POSES.mkdir(parents=True, exist_ok=True)
    previews = []
    for state in pack['animations']:
        tag = state.lower()
        sheet = SHEETS / f'{tag}.png'
        if not sheet.exists():
            continue
        metadata = SHEETS / f'{tag}.source.json'
        spec = json.loads(metadata.read_text()) if metadata.exists() else {
            'state': state, 'count': LEGACY[state][0], 'peak': LEGACY[state][1],
            'status': 'source-draft', 'tool': 'built-in imagegen',
            'prompt': 'Recovered prior independent redraw; exact original prompt unavailable.'}
        try:
            frames = extract(sheet, spec['count'], spec.get('columns'))
            peak = spec['peak']
            if not 0 < peak < len(frames):
                raise ValueError('peak must select a real action frame')
            records = []
            for i, (frame, box, component) in enumerate(frames):
                path = POSES / f'{tag}_{i:02d}.png'
                frame.save(path)
                records.append({'path': path.relative_to(ROOT).as_posix(), 'sha256': sha(path),
                                'sourceBox': box, 'sourceComponent': int(component)})
            keys = {}
            for label, index in [('start', 0), ('peak', peak), ('recover', len(frames) - 1)]:
                path = POSES / f'{tag}_{label}.png'
                frames[index][0].save(path)
                keys[label] = {'path': path.relative_to(ROOT).as_posix(), 'sha256': sha(path), 'frame': index}
            manifest['states'][state] = {'source': sheet.relative_to(ROOT).as_posix(),
                'sourceSha256': sha(sheet), 'count': len(frames), 'peak': peak,
                'frames': records, 'keys': keys, 'review': 'PENDING', 'spec': spec}
            previews.append((state, [frames[i][0] for i in [0, peak, len(frames) - 1]]))
        except ValueError as error:
            manifest['errors'].append({'state': state, 'error': str(error)})
    manifest['missingStates'] = [s for s in pack['animations'] if s not in manifest['states']]
    write_json(POSES / 'manifest.json', manifest)
    w, h = 450, 230
    contact = Image.new('RGB', (3 * w, max(1, math.ceil(len(previews) / 3)) * h), '#20242d')
    draw = ImageDraw.Draw(contact)
    for index, (state, frames) in enumerate(previews):
        x, y = index % 3 * w, index // 3 * h
        draw.text((x + 8, y + 5), state + ' — SOURCE DRAFT', fill='white')
        # One scale for the entire state: independently fitting each thumbnail
        # hid scale drift and made extended punches look artificially smaller.
        scale = min(140 / max(f.width for f in frames), 184 / max(f.height for f in frames))
        for j, frame in enumerate(frames):
            frame = frame.copy()
            frame = frame.resize((max(1, round(frame.width * scale)), max(1, round(frame.height * scale))), Image.Resampling.LANCZOS)
            contact.paste(frame, (x + j * 148 + (140 - frame.width) // 2, y + 28 + 184 - frame.height), frame)
            draw.text((x + j * 148 + 8, y + 214), ['START', 'PEAK', 'RECOVER'][j], fill='#aabbcc')
    contact.save(ROOT / 'docs/art/p12-authored-source-preview.png')
    print(f"P12 extracted {len(manifest['states'])}/42 source drafts; approval PENDING")
    for error in manifest['errors']:
        print(error)
    return bool(manifest['errors'])


if __name__ == '__main__':
    raise SystemExit(main())
