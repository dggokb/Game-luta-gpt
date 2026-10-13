#!/usr/bin/env python3
"""Prepare the reviewed 4x P01 idle masters as a memory-bounded 2x runtime source."""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]

def prepare(frames, manifest, output):
    data = json.loads(manifest.read_text())
    entries = data['frames']
    if len(entries) != 76 or data['summary']['frames_completed'] != 76:
        raise ValueError('Expected all 76 reviewed masters')
    original = Image.open(ROOT/'art/sprites/source/player_base_idle_video_normalized.png').convert('RGBA')
    if original.size != (2048, 2560):
        raise ValueError('Original idle grid changed; review registration before preparing')
    sheet = Image.new('RGBA', (5120, 4096))
    for i, entry in enumerate(entries):
        path = frames/f'frame_{i:03}.png'
        if entry['frame'] != i or hashlib.sha256(path.read_bytes()).hexdigest() != entry['output_sha256']:
            raise ValueError(f'Master hash mismatch: {path.name}')
        hd = Image.open(path).convert('RGBA')
        if hd.size != (1024, 1024):
            raise ValueError(f'Invalid master dimensions: {path.name}')
        src = original.crop((i%8*256, i//8*256, (i%8+1)*256, (i//8+1)*256))
        expected_alpha = src.getchannel('A').resize((1024, 1024), Image.Resampling.LANCZOS)
        if expected_alpha.tobytes() != hd.getchannel('A').tobytes():
            raise ValueError(f'Master alpha differs from current original: {path.name}')
        # One fixed transform for every frame, with premultiplied RGB at transparent edges.
        cell = hd.convert('RGBa').resize((512, 512), Image.Resampling.LANCZOS).convert('RGBA')
        cell.putalpha(src.getchannel('A').resize((512, 512), Image.Resampling.LANCZOS))
        sheet.paste(cell, (i%10*512, i//10*512))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)
    print(f'Prepared 76 frames: {output}, {sheet.width}x{sheet.height}, 2x pixels; canonical root (128,238)')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--frames', required=True, type=Path)
    parser.add_argument('--manifest', type=Path, default=ROOT/'docs/art/p01-idle-sr.manifest.json')
    parser.add_argument('--output', type=Path, default=ROOT/'art/sprites/source/player_base_idle_sr_2x.png')
    args = parser.parse_args()
    prepare(args.frames, args.manifest, args.output)
