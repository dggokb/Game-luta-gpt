#!/usr/bin/env python3
"""Real-ESRGAN HD para os clipes ativos P01/P02 da Fable, sem recriar poses.
P01: refina RGB mantendo pixelScale=2 e alpha idêntico.
P02: refina RGB e sobe pixelScale=1 para 2, com alpha Lanczos.
"""
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import sys

import numpy as np
import torch
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/sprites'))
from sr_hd import modelo, preencher_invisivel, SHA_PESOS

TILE, HALO = 96, 24


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def targets():
    clips = []
    for path in sorted((ROOT / 'tools/sprites/clips').glob('*.json')):
        if not (path.name.startswith('player_base_') or path.name.startswith('p02_')):
            continue
        clip = json.loads(path.read_text(encoding='utf-8'))
        source = clip.get('source', '')
        if not source.endswith('.png'):
            continue
        density = float(clip.get('pixelScale') or 1)
        if path.name.startswith('player_base_'):
            if '_sr_hd.' in source or source.endswith('_sr_hd_2x.png'):
                continue
            if density != 2:
                raise ValueError(f'{path.name}: P01 com pixelScale inesperado {density}')
        else:
            if density >= 2:
                continue
            if density != 1:
                raise ValueError(f'{path.name}: P02 com pixelScale inesperado {density}')
        if not (ROOT / source).is_file():
            raise FileNotFoundError(source)
        clips.append((path, clip))
    return clips


def shards(clips, count):
    ordered = sorted(
        clips,
        key=lambda pc: pc[1]['expectedFrames'] * pc[1]['frameWidth'] * pc[1]['frameHeight'],
        reverse=True,
    )
    groups = [[] for _ in range(count)]
    weight = [0] * count
    for path, clip in ordered:
        index = min(range(count), key=lambda k: weight[k])
        groups[index].append((path, clip))
        weight[index] += clip['expectedFrames'] * clip['frameWidth'] * clip['frameHeight']
    return groups


def render_rgb4(net, cell):
    height, width = cell.shape[:2]
    filled = preencher_invisivel(cell)
    output = np.zeros((height * 4, width * 4, 3), dtype=np.uint8)
    for y in range(0, height, TILE):
        for x in range(0, width, TILE):
            y_end, x_end = min(height, y + TILE), min(width, x + TILE)
            ya, xa = max(0, y - HALO), max(0, x - HALO)
            yb, xb = min(height, y_end + HALO), min(width, x_end + HALO)
            rgb = filled[ya:yb, xa:xb].astype(np.float32) / 255.
            tensor = torch.from_numpy(rgb).permute(2, 0, 1).unsqueeze(0)
            with torch.inference_mode():
                upscale = net(tensor).clamp_(0, 1).squeeze(0).permute(1, 2, 0).numpy()
            crop = upscale[
                (y - ya) * 4:(y_end - ya) * 4,
                (x - xa) * 4:(x_end - xa) * 4,
            ]
            output[y*4:y_end*4, x*4:x_end*4] = np.rint(crop * 255).astype(np.uint8)
    return output


def enhance(net, cell, scale):
    height, width = cell.shape[:2]
    if not cell[..., 3].any():
        if scale == 1:
            return cell.copy()
        return np.asarray(Image.fromarray(cell).resize(
            (width*scale, height*scale), Image.Resampling.NEAREST))
    rgb4 = render_rgb4(net, cell).astype(np.float32) / 255.
    alpha4 = np.asarray(Image.fromarray(cell[..., 3]).resize(
        (width*4, height*4), Image.Resampling.LANCZOS), dtype=np.float32) / 255.
    out_w, out_h = width * scale, height * scale
    alpha_final = (
        np.asarray(Image.fromarray(cell[..., 3]).resize(
            (out_w, out_h), Image.Resampling.LANCZOS), dtype=np.uint8)
        if scale == 2 else cell[..., 3].copy()
    )
    premultiplied = rgb4 * alpha4[..., None]
    channels = []
    for i in range(3):
        reduced = Image.fromarray(premultiplied[..., i], mode='F').resize(
            (out_w, out_h), Image.Resampling.BOX)
        channels.append(np.asarray(reduced, dtype=np.float32))
    alpha_reduced = np.asarray(
        Image.fromarray(alpha4, mode='F').resize(
            (out_w, out_h), Image.Resampling.BOX), dtype=np.float32)
    result = np.zeros((out_h, out_w, 4), dtype=np.uint8)
    for i in range(3):
        color = channels[i] / np.maximum(alpha_reduced, 1e-5)
        result[..., i] = np.clip(np.rint(color*255), 0, 255).astype(np.uint8)
    result[..., 3] = alpha_final
    return result


def process(net, path, clip):
    original = clip['source']
    source = ROOT / original
    num, cols = clip['expectedFrames'], clip['columns']
    width, height = clip['frameWidth'], clip['frameHeight']
    rows = math.ceil(num / cols)
    density = float(clip.get('pixelScale') or 1)
    scale = 2 if density == 1 else 1
    with Image.open(source) as original_img:
        sheet = np.asarray(original_img.convert('RGBA'))
    if sheet.shape[:2] != (rows*height, cols*width):
        raise ValueError(
            f'{path.name}: grade {sheet.shape[:2]} diferente de {(rows*height, cols*width)}')
    output = np.zeros((rows*height*scale, cols*width*scale, 4), np.uint8)
    for frame in range(num):
        row, col = divmod(frame, cols)
        cell = sheet[row*height:(row+1)*height, col*width:(col+1)*width]
        result = enhance(net, cell, scale)
        expected_alpha = (
            np.asarray(Image.fromarray(cell[..., 3]).resize(
                (width*scale, height*scale), Image.Resampling.LANCZOS))
            if scale == 2 else cell[..., 3]
        )
        if not np.array_equal(result[..., 3], expected_alpha):
            raise AssertionError(f'{path.name}: alpha alterado no quadro {frame}')
        output[
            row*height*scale:(row+1)*height*scale,
            col*width*scale:(col+1)*width*scale,
        ] = result
        if (frame+1) % 5 == 0 or frame+1 == num:
            print(f'{clip["id"]}: {frame+1}/{num}', flush=True)
    destination = source.with_name(
        source.stem + ('_2x.png' if scale == 2 else '_sr_hd.png'))
    if destination.exists():
        raise FileExistsError(f'Não sobrescrever fonte existente: {destination}')
    Image.fromarray(output, 'RGBA').save(destination, optimize=True)
    clip.setdefault('sourceReferences', [])
    if original not in clip['sourceReferences']:
        clip['sourceReferences'].insert(0, original)
    clip['source'] = str(destination.relative_to(ROOT))
    if scale == 2:
        clip.update(pixelScale=2, frameWidth=width*2, frameHeight=height*2)
        for key in ('rootX', 'rootY'):
            if key in clip:
                clip[key] *= 2
    path.write_text(json.dumps(clip, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    manifest = {
        'clip': clip['id'],
        'oldSource': original,
        'newSource': clip['source'],
        'sourceSha256': digest(source),
        'outputSha256': digest(destination),
        'frames': num,
        'columns': cols,
        'originalCell': [width, height],
        'outputCell': [width*scale, height*scale],
        'pixelScaleBefore': density,
        'pixelScaleAfter': 2,
        'rgb': 'Real-ESRGAN AnimeVideo-v3, 4x, premultiplied downsample',
        'weightsSha256': SHA_PESOS,
        'alpha': 'P01 original; P02 Lanczos 2x; verified frame by frame',
        'preserved': 'pose, frame count/order, durations, root logical position, game scale',
    }
    out_manifest = ROOT / 'docs/art/hd-fable' / f'{clip["id"]}.json'
    out_manifest.parent.mkdir(parents=True, exist_ok=True)
    out_manifest.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print(f'PRONTO {clip["id"]}: {num} quadros -> {clip["source"]}', flush=True)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--weights', type=Path, required=True)
    p.add_argument('--shard', type=int, required=True)
    p.add_argument('--shards', type=int, default=12)
    args = p.parse_args()
    if not 0 <= args.shard < args.shards:
        p.error('shard fora dos limites')
    torch.set_num_threads(min(2, os.cpu_count() or 2))
    all_clips = targets()
    batch = shards(all_clips, args.shards)[args.shard]
    print(
        f'Fable HD: {len(all_clips)} clipes pendentes; '
        f'shard {args.shard}/{args.shards}: {len(batch)} clipes',
        flush=True)
    for _, clip in batch:
        print(
            f'  {clip["id"]}: {clip["expectedFrames"]} quadros '
            f'{clip["frameWidth"]}x{clip["frameHeight"]}', flush=True)
    if not batch:
        return
    network = modelo(args.weights)
    for path, clip in batch:
        process(network, path, clip)


if __name__ == '__main__':
    main()
