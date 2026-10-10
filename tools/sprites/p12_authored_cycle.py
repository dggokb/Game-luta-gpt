#!/usr/bin/env python3
"""Authored P12 renderer: independent drawings, uniform registration, cached SR.

No Idle rig, remap, body-part deformation or procedural animation fallback.
Registration is explicit; missing/unreviewed registration fails before writes.
The original combat contract, including every duration and impact index, stays
unchanged. Technical preparation alone never grants visual approval.
"""
import argparse
import hashlib
import importlib.util
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt

from p12_authored_sources import ROOT, POSES, sha, write_json

FW, FH, RX, RY = 384, 256, 128, 238
MODEL_HASH = 'b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d'
REGISTRATION = ROOT / 'art/keys/p12/registration.json'
CONTRACT = ROOT / 'docs/art/p12-motion-contract.json'
MANIFEST = ROOT / 'docs/art/p12-authored-render-manifest.json'
CACHE_VERSION = 'anime-v3-alpha-v2'


def read(path):
    return json.loads(path.read_text())


def frame_map(animation, source_count, peak):
    """Map independently drawn frames onto the immutable runtime event indices."""
    count = len(animation['frames'])
    if count == 1:
        return [0]
    target_peak = animation.get('impactFrame', round((count - 1) * peak / (source_count - 1)))
    if not 0 < target_peak < count - 1:
        raise ValueError('action peak must have anticipation and recovery frames')
    before = [round(i * peak / target_peak) for i in range(target_peak + 1)]
    after = [round(peak + i * (source_count - 1 - peak) / (count - 1 - target_peak))
             for i in range(1, count - target_peak)]
    return before + after


def render_pose(source, scale, root):
    """Only a uniform resize and translation of the entire independently drawn pose."""
    source = source.convert('RGBA')
    size = (max(1, round(source.width * scale)), max(1, round(source.height * scale)))
    # Avoid a lossy premultiplied round-trip when no resize is requested.
    picture = (source.copy() if size == source.size else
               source.convert('RGBa').resize(size, Image.Resampling.LANCZOS).convert('RGBA'))
    x, y = round(RX - root[0] * scale), round(RY - root[1] * scale)
    if x < 8 or y < 8 or x + size[0] > FW - 8 or y + size[1] > FH - 8:
        raise ValueError('registered pose is clipped; correct registration/art instead of shrinking this frame')
    canvas = Image.new('RGBA', (FW, FH))
    canvas.alpha_composite(picture, (x, y))
    return canvas


def atlas(frames):
    width, height = frames[0].size
    result = Image.new('RGBA', (width * 4, height * math.ceil(len(frames) / 4)))
    for i, frame in enumerate(frames):
        result.paste(frame, (i % 4 * width, i // 4 * height))
    return result


class CachedUpscaler:
    def __init__(self, weights, cache_dir=None):
        if sha(weights) != MODEL_HASH:
            raise ValueError('AnimeVideo-v3 model checksum mismatch')
        self.weights = weights
        self.cache = cache_dir or ROOT / 'android/app/build/p12-sr-cache'
        self.cache.mkdir(parents=True, exist_ok=True)
        self.model = None
        self.hits = self.misses = 0

    def load_model(self):
        import torch
        spec = importlib.util.spec_from_file_location('p12_srvgg', ROOT / 'art/redraws/p03_hd/idle_sr_4x/srvgg_arch.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        torch.set_num_threads(3)
        model = module.SRVGGNetCompact(num_in_ch=3, num_out_ch=3, num_feat=64,
                                     num_conv=16, upscale=4, act_type='prelu')
        model.load_state_dict(torch.load(self.weights, map_location='cpu', weights_only=True)['params'], strict=True)
        self.model = model.eval()

    def sr(self, frame):
        import torch
        key = hashlib.sha256(CACHE_VERSION.encode() + MODEL_HASH.encode() +
                             str(frame.size).encode() + frame.tobytes()).hexdigest()
        path = self.cache / f'{key}.png'
        size = (frame.width * 2, frame.height * 2)
        expected_alpha = frame.getchannel('A').resize(size, Image.Resampling.LANCZOS)
        if path.exists():
            cached = Image.open(path).convert('RGBA')
            if cached.size == size and cached.getchannel('A').tobytes() == expected_alpha.tobytes():
                self.hits += 1
                return cached
        if self.model is None:
            self.load_model()
        pixels = np.asarray(frame).copy()
        invisible = pixels[:, :, 3] < 128
        _, indices = distance_transform_edt(invisible, return_indices=True)
        rgb = pixels[:, :, :3].copy()
        rgb[invisible] = rgb[indices[0][invisible], indices[1][invisible]]
        tensor = torch.from_numpy(np.ascontiguousarray(rgb.transpose(2, 0, 1))).float()[None] / 255
        with torch.inference_mode():
            prediction = self.model(tensor)[0].clamp(0, 1).permute(1, 2, 0).numpy()
        improved = Image.fromarray(np.rint(prediction * 255).astype(np.uint8))
        improved.putalpha(frame.getchannel('A').resize((frame.width * 4, frame.height * 4), Image.Resampling.LANCZOS))
        improved = improved.convert('RGBa').resize(size, Image.Resampling.LANCZOS).convert('RGBA')
        improved.putalpha(expected_alpha)
        improved.save(path)
        self.misses += 1
        return improved


def preflight():
    sources, contract = read(POSES / 'manifest.json'), read(CONTRACT)
    if set(sources['states']) != set(contract['animations']) or sources['errors']:
        raise ValueError('all 42 independent source states must extract cleanly before refinement')
    if not REGISTRATION.exists():
        raise ValueError('P12 registration is missing: annotate scale, roots and movement evidence before refinement')
    registration = read(REGISTRATION)
    states = registration.get('states', {})
    if set(states) != set(contract['animations']):
        raise ValueError('registration must cover exactly all 42 states')
    rendered = {}
    for state, source in sources['states'].items():
        spec = states[state]
        if spec.get('sourceSha256') != source['sourceSha256']:
            raise ValueError(state + ': stale registration after source redraw')
        review = spec.get('review', {})
        if review.get('status') != 'reviewed' or review.get('findings') or not review.get('movementEvidence'):
            raise ValueError(state + ': visual/movement review incomplete or defects unresolved')
        if len(spec.get('roots', [])) != source['count'] or not 0 < spec.get('scale', 0) <= 1:
            raise ValueError(state + ': explicit source roots and one uniform state scale required')
        selected = frame_map(contract['animations'][state], source['count'], source['peak'])
        frames, recipes = [], []
        for i in selected:
            record = source['frames'][i]
            # The named start/peak/recover files are read, not merely mentioned.
            label = next((label for label, key in source['keys'].items() if key['frame'] == i), None)
            record = source['keys'][label] if label else record
            path = ROOT / record['path']
            if sha(path) != record['sha256']:
                raise ValueError(state + ': modified source pose')
            image = render_pose(Image.open(path), spec['scale'], spec['roots'][i])
            frames.append(image)
            recipes.append({'source': record['path'], 'sourceSha256': record['sha256'],
                            'sourceFrame': i, 'scale': spec['scale'], 'root': spec['roots'][i],
                            'nativePixelSha256': hashlib.sha256(image.tobytes()).hexdigest()})
        rendered[state] = (frames, recipes)
    return sources, contract, registration, rendered


def prepare(weights):
    sources, contract, registration, rendered = preflight()
    upscaler = CachedUpscaler(weights)
    pack = read(ROOT / 'characters/p12/character.json')
    # No combat setting or animation timing is regenerated from another fighter.
    for key in ['fighter', 'moves', 'specialMoves', 'specialAnimations', 'animations']:
        if pack[key] != contract[key]:
            raise ValueError('P12 combat/animation contract changed: ' + key)
    manifest = {'character': 'p12', 'status': 'PREPARED_NOT_ART_APPROVED',
                'generation': 'independent-authored-poses', 'modelSha256': MODEL_HASH,
                'registrationSha256': sha(REGISTRATION), 'contractSha256': sha(CONTRACT), 'states': {}}
    for state, (frames, recipes) in rendered.items():
        tag, aid = state.lower(), 'p12_full_' + state.lower()
        native = ROOT / f'art/sprites/source/{aid}_original.png'
        refined = ROOT / f'art/sprites/source/{aid}_sr2x.png'
        atlas(frames).save(native)
        atlas([upscaler.sr(frame) for frame in frames]).save(refined)
        cfg = read(ROOT / f'tools/sprites/clips/{aid}.json')
        refs = list(dict.fromkeys([native.relative_to(ROOT).as_posix(), sources['states'][state]['source']] +
                                 [r['source'] for r in recipes]))
        cfg.update(sourceReferences=refs, frameWidth=FW * 2, frameHeight=FH * 2,
                   expectedFrames=len(frames), rootX=RX * 2, rootY=RY * 2,
                   sourceNote='Independent authored P12 poses; uniform registration; AnimeVideo-v3 SR; visual audit required')
        write_json(ROOT / f'tools/sprites/clips/{aid}.json', cfg)
        manifest['states'][state] = {'recipes': recipes, 'nativeSha256': sha(native),
                                    'refinedSha256': sha(refined)}
        print(f'{state}: {len(frames)} authored frames; source pixels consumed', flush=True)
    profile = read(ROOT / 'tools/sprites/profiles/p12.json')
    profile['anatomyReference']['frameWidth'] = FW
    write_json(ROOT / 'tools/sprites/profiles/p12.json', profile)
    manifest['srCache'] = {'hits': upscaler.hits, 'misses': upscaler.misses}
    write_json(MANIFEST, manifest)
    print('Prepared authored assets. Mandatory source, harmony, visual and Android tests remain.')


def verify():
    sources, contract, registration, rendered = preflight()
    manifest = read(MANIFEST)
    pack = read(ROOT / 'characters/p12/character.json')
    for key in ['fighter', 'moves', 'specialMoves', 'specialAnimations', 'animations']:
        if pack[key] != contract[key]:
            raise ValueError('combat contract changed: ' + key)
    if manifest['registrationSha256'] != sha(REGISTRATION) or manifest['contractSha256'] != sha(CONTRACT):
        raise ValueError('render manifest is stale')
    for state, (frames, recipes) in rendered.items():
        aid = 'p12_full_' + state.lower()
        native = ROOT / f'art/sprites/source/{aid}_original.png'
        refined = ROOT / f'art/sprites/source/{aid}_sr2x.png'
        row = manifest['states'][state]
        if row['recipes'] != recipes or Image.open(native).convert('RGBA').tobytes() != atlas(frames).tobytes():
            raise ValueError(state + ': native atlas does not reproduce consumed independent sources')
        if sha(native) != row['nativeSha256'] or sha(refined) != row['refinedSha256']:
            raise ValueError(state + ': atlas changed after generation')
        high = Image.open(refined).convert('RGBA')
        if high.size != (FW * 8, FH * 2 * math.ceil(len(frames) / 4)):
            raise ValueError(state + ': refined atlas dimensions wrong')
        for i, frame in enumerate(frames):
            cell = high.crop((i % 4 * FW * 2, i // 4 * FH * 2, (i % 4 + 1) * FW * 2, (i // 4 + 1) * FH * 2))
            if cell.getchannel('A').tobytes() != frame.getchannel('A').resize((FW * 2, FH * 2), Image.Resampling.LANCZOS).tobytes():
                raise ValueError(state + ': refined alpha differs from native alpha')
    print('Authored source consumption, alpha and immutable combat contract verified. Art approval remains separate.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['prepare', 'verify'])
    parser.add_argument('--weights', type=Path)
    args = parser.parse_args()
    if args.action == 'prepare':
        if args.weights is None:
            parser.error('prepare requires --weights')
        prepare(args.weights)
    else:
        verify()
