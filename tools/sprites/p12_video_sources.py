#!/usr/bin/env python3
"""Extract existing P12 Drive videos. No generated drawings or Idle fallback.

Retain every decoded frame and its timestamp before selecting combat segments.
Existing chroma-key logic isolates the fighter; no scaling, limb deformation,
pose synthesis, timing mutation or runtime publication occurs at this stage.
"""
import argparse
import hashlib
import json
import math
import subprocess
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw

from video_para_sprite import key

ROOT = Path(__file__).resolve().parents[2]
INVENTORY = ROOT / 'tools/sprites/videos/p12-drive-inventory.json'
VERSION = 'drive-video-frames-v1'
STATE_FILES = {
    'IDLE': 'idle.mp4', 'COMBAT': 'idle.mp4',
    'WALK_FORWARD': 'frente.mp4', 'WALK_BACK': 'tras.mp4',
    'CROUCH': 'agachado.mp4', 'RISE': 'agachado.mp4',
    'JUMP': 'jump.mp4', 'FALL': 'jump.mp4', 'LAND': 'jump.mp4',
    'DASH': 'dash.mp4', 'BACKDASH': 'backDash.mp4',
    'LIGHT_JAB': 'L.mp4', 'MEDIUM_KICK': 'M.mp4', 'HEAVY_STRAIGHT': 'H.mp4',
    'CROUCH_LIGHT': '2L.mp4', 'CROUCH_MEDIUM': '2M.mp4', 'CROUCH_HEAVY': '2H.mp4',
    'JUMP_LIGHT': 'jL.mp4', 'JUMP_MEDIUM': 'jM.mp4', 'JUMP_HEAVY': 'jH.mp4',
    'DEFENSE_STAND': 'defendeCima.mp4', 'DEFENSE_CROUCH': 'defendeBaixo.mp4',
    'DEFENSE_AIR': 'defesaPulo.mp4', 'HIT_CROUCH': 'danoBaixo.mp4', 'HIT_AIR': 'danoPulo.mp4',
    # Candidate derivations from visibly present source phases. Segment approval
    # is still required; filenames alone never establish a finished animation.
    'HIT_STAND': 'derruba_levanta.mp4', 'JUMP_HEAVY_DOWN': 'jH.mp4',
    'KNOCKDOWN': 'derruba_levanta.mp4', 'GROUNDED': 'derruba_levanta.mp4', 'GETUP': 'derruba_levanta.mp4',
    'THROW_GRAB': 'agarrao.mp4', 'THROW_TOSS': 'agarrao.mp4',
    'SPECIAL_ENERGY': 's1.mp4', 'SPECIAL_S2': 's2.mp4', 'SPECIAL_S3': 's3.mp4', 'SPECIAL_S4': 's4.mp4',
    'SUPER_WAVE': 'super.mp4', 'ULTRA_BEAM': 'ultra.mp4',
    'INTRO': 'intro.mp4', 'VICTORY': 'vitoria.mp4', 'DEFEAT': 'derrota.mp4', 'TAUNT': 'provocacao.mp4',
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix('.pending.json')
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')
    temporary.replace(path)


def write_contact(record, folder):
    """One source scale/camera space for the entire video: do not hide zoom."""
    frames = record['frames']
    bounds = [min(r['sourceBox'][0] for r in frames), min(r['sourceBox'][1] for r in frames),
              max(r['sourceBox'][2] for r in frames), max(r['sourceBox'][3] for r in frames)]
    scale = min(220 / (bounds[2] - bounds[0]), 180 / (bounds[3] - bounds[1]))
    selected = frames[::8]
    contact = Image.new('RGB', (240 * 4, 220 * math.ceil(len(selected) / 4)), '#20242d')
    draw = ImageDraw.Draw(contact)
    for i, frame in enumerate(selected):
        picture = Image.open(folder / frame['file']).convert('RGBA')
        picture = picture.resize((max(1, round(picture.width * scale)), max(1, round(picture.height * scale))), Image.Resampling.LANCZOS)
        x, y = i % 4 * 240, i // 4 * 220
        left = x + 10 + round((frame['sourceBox'][0] - bounds[0]) * scale)
        top = y + 30 + round((frame['sourceBox'][1] - bounds[1]) * scale)
        contact.paste(picture, (left, top), picture)
        draw.text((x + 5, y + 4), f"{Path(record['name']).stem} f{frame['index']} / {frame['time']:.3f}s", fill='white')
        draw.text((x + 5, y + 207), 'SOURCE VIDEO / NOT APPROVED', fill='#f1b464')
    contact.save(folder / 'contact.jpg', quality=88)
    record['contactLayout'] = 'fixed-source-scale-v2'
    record['sourceBoundsUnion'] = bounds


def probe(path):
    result = subprocess.run(['ffprobe', '-v', 'error', '-select_streams', 'v:0',
        '-show_streams', '-show_frames', '-show_entries',
        'stream=width,height,avg_frame_rate,nb_frames:frame=best_effort_timestamp_time',
        '-of', 'json', str(path)], capture_output=True, text=True, check=True)
    if result.stderr.strip():
        raise ValueError('video decode/probe error: ' + result.stderr.strip())
    metadata = json.loads(result.stdout)
    # Strict decode rejects truncated transfers even if ffprobe finds the header.
    decoded = subprocess.run(['ffmpeg', '-v', 'error', '-xerror', '-i', str(path),
        '-map', '0:v:0', '-f', 'null', '-'], capture_output=True, text=True, check=True)
    if decoded.stderr.strip():
        raise ValueError(decoded.stderr.strip())
    return metadata


def extract_video(file, source_dir, output, key_hash):
    path = source_dir / file['name']
    if not path.exists() or path.stat().st_size != file['size']:
        raise ValueError('missing/truncated download: ' + file['name'])
    digest = sha(path)
    folder = output / path.stem
    manifest_path = folder / 'frames.json'
    if manifest_path.exists():
        existing = json.loads(manifest_path.read_text())
        if (existing.get('videoSha256') == digest and existing.get('keyerSha256') == key_hash
                and existing.get('extractorVersion') == VERSION
                and all((folder / r['file']).exists() and sha(folder / r['file']) == r['sha256']
                        for r in existing['frames'])):
            if existing.get('contactLayout') != 'fixed-source-scale-v2':
                write_contact(existing, folder)
                write_json(manifest_path, existing)
            print(file['name'] + ': cached extracted frames', flush=True)
            return existing
    metadata = probe(path)
    stamps = [float(f['best_effort_timestamp_time']) for f in metadata['frames']]
    folder.mkdir(parents=True, exist_ok=True)
    cap = cv2.VideoCapture(str(path))
    frames = []
    index = 0
    while True:
        ok, bgr = cap.read()
        if not ok:
            break
        rgba = key(bgr)
        ys, xs = np.where(rgba[:, :, 3] > 10)
        if not len(xs):
            raise ValueError(file['name'] + f': empty matte at frame {index}')
        box = [int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1]
        picture = Image.fromarray(rgba).crop(box)
        pixels = np.asarray(picture).copy()
        pixels[pixels[:, :, 3] == 0, :3] = 0
        picture = Image.fromarray(pixels)
        name = f'{index:04d}.png'
        destination = folder / name
        temporary = destination.with_suffix('.pending.png')
        picture.save(temporary)
        with Image.open(temporary) as check:
            check.verify()
        temporary.replace(destination)
        frames.append({'index': index, 'time': stamps[index], 'file': name,
                       'sourceBox': box, 'sha256': sha(destination)})
        index += 1
    cap.release()
    if len(frames) != len(stamps) or len(frames) != int(metadata['streams'][0]['nb_frames']):
        raise ValueError(file['name'] + ': decoded frame count does not match complete source')
    record = {'name': file['name'], 'driveId': file['id'], 'driveUrl': file['url'],
              'videoSha256': digest, 'videoBytes': path.stat().st_size,
              'extractorVersion': VERSION, 'keyerSha256': key_hash,
              'stream': metadata['streams'][0], 'frames': frames,
              'status': 'EXTRACTED_NOT_ART_APPROVED',
              'note': 'Actual sequential video frames; alpha isolation only. No pose generation, uniform scale or runtime timing applied yet.'}
    write_contact(record, folder)
    write_json(manifest_path, record)
    print(f"{file['name']}: {len(frames)} actual video frames", flush=True)
    return record


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('source_dir', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'android/app/build/p12-video-extraction')
    parser.add_argument('--only', nargs='*')
    args = parser.parse_args()
    inventory = json.loads(INVENTORY.read_text())
    key_hash = sha(ROOT / 'tools/sprites/video_para_sprite.py')
    records, errors = {}, []
    for file in inventory['files']:
        if file['mimeType'] != 'video/mp4' or (args.only and file['name'] not in args.only):
            continue
        try:
            records[file['name']] = extract_video(file, args.source_dir, args.output, key_hash)
        except (ValueError, subprocess.CalledProcessError) as error:
            errors.append({'video': file['name'], 'error': str(error)})
            print('REJECTED ' + file['name'] + ': ' + str(error), flush=True)
    contract = json.loads((ROOT / 'docs/art/p12-motion-contract.json').read_text())
    bindings = {state: {'file': name, 'status': 'SEGMENT_REVIEW_PENDING',
                       'videoSha256': records[name]['videoSha256'] if name in records else None}
                for state, name in STATE_FILES.items()}
    bindings['HIT_STAND']['candidatePhase'] = 'initial standing hit/recoil before the knockdown; impact beam must be removed'
    bindings['JUMP_HEAVY_DOWN']['candidatePhase'] = 'overhead hands to downward slam, frames 64–71; source zoom/effects require correction'
    bindings['HIT_STAND']['dedicatedFileMissing'] = True
    bindings['JUMP_HEAVY_DOWN']['dedicatedFileMissing'] = True
    missing = [s for s in contract['animations'] if s not in STATE_FILES]
    report = {'character': 'p12', 'status': 'NOT_APPROVED', 'sourcePolicy': 'EXISTING_DRIVE_VIDEOS_ONLY',
              'folder': inventory['folder'], 'videosExtracted': len(records),
              'actualFrames': sum(len(r['frames']) for r in records.values()),
              'potentialStateBindings': bindings, 'missingVideoStates': missing, 'errors': errors,
              'pending': ['Review source motions and effect mattes', 'Select preparation/impact/recovery intervals',
                          'Preserve combat timing and impact indices', 'Anatomy, roots and support feet',
                          'Cached SR', 'Character Pack import and mandatory audits', 'Android tests, emulator and APK']}
    write_json(args.output / 'summary.json', report)
    print(f"{len(records)} existing videos extracted; missing state sources {missing}; NOT APPROVED", flush=True)
    return bool(errors)


if __name__ == '__main__':
    raise SystemExit(main())
