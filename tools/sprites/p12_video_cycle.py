#!/usr/bin/env python3
"""Production rendering of reviewed, real P12 video frames.

Source pixels must match the original video extraction. Only alpha subtraction,
whole-frame camera-scale correction and root translation are supported. Missing
registration or unresolved motion/art findings reject the entire batch before
SR or runtime writes. New drawings and the superseded authored sheets cannot
enter this renderer by claiming video provenance in metadata.
"""
import json
import hashlib
import argparse
from pathlib import Path

import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt

from p12_video_sources import ROOT, STATE_FILES, sha, write_json
from p12_authored_cycle import CachedUpscaler, atlas, MODEL_HASH
from p12_video_selection import bind_sources

# Logical dimensions, before SR. The importer applies pixelScale=2 to its
# dimensional limits. Extended limbs must fit without changing fighter scale.
FW, FH, RX, RY = 768, 384, 320, 366


def render_pose(source, scale, root, anchor=(RX, RY)):
    """Uniform whole-source correction; no limb editing or per-pose fitting."""
    source = source.convert('RGBA')
    size = (max(1, round(source.width * scale)), max(1, round(source.height * scale)))
    picture = (source.copy() if size == source.size else
               source.convert('RGBa').resize(size, Image.Resampling.LANCZOS).convert('RGBA'))
    if (len(anchor) != 2 or any(type(n) is not int for n in anchor)
            or not 0 < anchor[0] < FW or not 0 < anchor[1] < FH):
        raise ValueError('invalid native clip anchor')
    x, y = round(anchor[0] - root[0] * scale), round(anchor[1] - root[1] * scale)
    bounds = picture.getchannel('A').point(lambda a: 255 if a > 10 else 0).getbbox()
    if bounds is None or (x + bounds[0] < 8 or y + bounds[1] < 8
                         or x + bounds[2] > FW - 8 or y + bounds[3] > FH - 8):
        raise ValueError(f'registered original-video pose is clipped: bounds={bounds}, '
                         f'placement={(x, y)}, scale={scale}, root={root}; correct registration instead of shrinking limbs')
    canvas = Image.new('RGBA', (FW, FH))
    canvas.alpha_composite(picture, (x, y))
    return canvas

REGISTRATION = ROOT / 'art/keys/p12/registration.json'
CONTRACT = ROOT / 'docs/art/p12-motion-contract.json'
MANIFEST = ROOT / 'docs/art/p12-video-render-manifest.json'


class VideoUpscaler(CachedUpscaler):
    """Same verified SR model, bounded crop plus full-frame exact alpha.

    32 native pixels of context exceed this model's convolution radius. Empty
    canvas does not consume inference time. Cache keys bind model, pixels,
    canvas size and the crop algorithm independently of the old renderer cache.
    """
    def sr(self, frame):
        key = hashlib.sha256(b'video-anime-v3-crop32-v1' + MODEL_HASH.encode()
                             + repr(frame.size).encode() + frame.tobytes()).hexdigest()
        path = self.cache / (key + '.png')
        size = (frame.width * 2, frame.height * 2)
        alpha = frame.getchannel('A').resize(size, Image.Resampling.LANCZOS)
        if path.exists():
            cached = Image.open(path).convert('RGBA')
            if cached.size == size and cached.getchannel('A').tobytes() == alpha.tobytes():
                self.hits += 1
                return cached
        if self.model is None:
            self.load_model()
        improved = refine_video_frame(self.model, frame)
        improved.save(path)
        self.misses += 1
        return improved


def refine_video_frame(model, frame):
    import torch
    box = frame.getchannel('A').getbbox()
    if box is None:
        raise ValueError('empty video pose cannot be refined')
    x0, y0, x1, y1 = box
    box = (max(0, x0 - 32), max(0, y0 - 32),
           min(frame.width, x1 + 32), min(frame.height, y1 + 32))
    crop = frame.crop(box)
    pixels = np.array(crop)
    invisible = pixels[..., 3] < 128
    _, indices = distance_transform_edt(invisible, return_indices=True)
    rgb = pixels[..., :3].copy()
    rgb[invisible] = rgb[indices[0][invisible], indices[1][invisible]]
    tensor = torch.from_numpy(np.ascontiguousarray(rgb.transpose(2, 0, 1))).float()[None] / 255
    with torch.inference_mode():
        prediction = model(tensor)[0].clamp(0, 1).permute(1, 2, 0).numpy()
    high = Image.fromarray(np.rint(prediction * 255).astype(np.uint8))
    high.putalpha(crop.getchannel('A').resize((crop.width * 4, crop.height * 4), Image.Resampling.LANCZOS))
    high = high.convert('RGBa').resize((crop.width * 2, crop.height * 2), Image.Resampling.LANCZOS).convert('RGBA')
    result = Image.new('RGBA', (frame.width * 2, frame.height * 2))
    result.paste(high, (box[0] * 2, box[1] * 2))
    result.putalpha(frame.getchannel('A').resize(result.size, Image.Resampling.LANCZOS))
    return result


def read(path):
    return json.loads(path.read_text())


def combat_unchanged(pack, contract):
    for key in ['fighter', 'moves', 'specialMoves', 'specialAnimations', 'animations']:
        if pack[key] != contract[key]:
            raise ValueError('P12 immutable combat/animation contract changed: ' + key)


def original_frame(state, index, ordinal, main, origin, record, frame_spec):
    """Read the actual pixels required by the mandatory source gate, not aliases
    merely listed in sourceReferences. Video extraction hashes bind their origin.
    """
    tag = state.lower()
    if ordinal == 0:
        relative = f'art/keys/p12/poses/{tag}_start.png'
    elif ordinal == main:
        relative = f'art/keys/p12/poses/{tag}_peak.png'
    else:
        prefix = Path(frame_spec['videoName']).stem + '_' if 'videoName' in frame_spec else ''
        relative = f'art/keys/p12/video_frames/{tag}/{prefix}{index:04d}.png'
    source = ROOT / relative
    if not source.exists() or sha(source) != record['sha256']:
        raise ValueError(state + f': frame {index} is not the recorded original-video extraction')
    picture = Image.open(source).convert('RGBA')
    box = record['sourceBox']
    if picture.size != (box[2] - box[0], box[3] - box[1]):
        raise ValueError(state + ': source crop dimensions changed')
    references = [relative]
    mask_spec = frame_spec.get('alphaMask')
    if mask_spec:
        prefix = Path(frame_spec['videoName']).stem + '_' if 'videoName' in frame_spec else ''
        mask_name = f'art/keys/p12/video_masks/{tag}/{prefix}{index:04d}.png'
        if mask_spec.get('path') != mask_name or sha(ROOT / mask_name) != mask_spec.get('sha256'):
            raise ValueError(state + ': invalid cleanup mask provenance')
        mask = Image.open(ROOT / mask_name)
        if mask.mode != 'L' or mask.size != picture.size:
            raise ValueError(state + ': alpha removal mask must be grayscale at the original crop size')
        pixels = np.array(picture)
        # This can remove embedded effects/opponents; it cannot paint new limbs,
        # restore occluded body parts or replace the original character drawing.
        pixels[..., 3] = ((pixels[..., 3].astype(np.uint16) * np.asarray(mask).astype(np.uint16) + 127) // 255).astype(np.uint8)
        pixels[pixels[..., 3] == 0, :3] = 0
        picture = Image.fromarray(pixels)
        references.append(mask_name)
    recipe = {'source': relative, 'sourceSha256': record['sha256'],
              'driveId': origin['driveId'], 'videoSha256': origin['videoSha256'],
              'videoFrame': index, 'sourceTimeSeconds': record['time'],
              'sourceBox': box, 'alphaMask': mask_spec}
    if 'videoName' in frame_spec:
        recipe['videoName'] = frame_spec['videoName']
    return picture, references, recipe


def preflight(selected_states=None):
    if not REGISTRATION.exists():
        raise ValueError('P12 original-video registration missing: scale, support, masks, motion and transitions remain unapproved')
    registration = read(REGISTRATION)
    contract = read(CONTRACT)
    combat_unchanged(read(ROOT / 'characters/p12/character.json'), contract)
    if registration.get('sourcePolicy') != 'EXISTING_DRIVE_VIDEOS_ONLY':
        raise ValueError('P12 accepts original Drive video frames only')
    states = registration.get('states', {})
    if set(states) != set(contract['animations']):
        raise ValueError('reviewed video registration must cover exactly all 42 states')
    selected_states = set(contract['animations']) if selected_states is None else set(selected_states)
    if not selected_states or not selected_states <= set(contract['animations']):
        raise ValueError('batch must select existing P12 states')
    inventory = read(ROOT / 'tools/sprites/videos/p12-drive-inventory.json')
    originals = {f['name']: f['id'] for f in inventory['files'] if f['mimeType'] == 'video/mp4'}
    rendered = {}
    for state, animation in contract['animations'].items():
        if state not in selected_states:
            continue
        spec = states[state]
        review = spec.get('review', {})
        if (review.get('status') != 'reviewed' or review.get('findings') != []
                or not review.get('movementEvidence') or not spec.get('scaleEvidence')
                or any(review.get(k) is not True for k in ('transitionsReviewed', 'supportReviewed', 'alphaReviewed'))):
            raise ValueError(state + ': real movement/scale/support/alpha/transition review incomplete')
        name = STATE_FILES[state]
        records = read(ROOT / 'docs/art/p12-video-sources' / (name.removesuffix('.mp4') + '.frames.json'))
        origin = spec.get('videoOrigin', {})
        if origin.get('driveId') != originals[name] or origin.get('videoSha256') != records['videoSha256']:
            raise ValueError(state + ': video origin differs from the recorded Drive original')
        selected = origin.get('selectedVideoFrames', [])
        count = len(animation['frames'])
        frame_specs = spec.get('frames', [])
        composite = any('videoName' in f for f in frame_specs)
        if (len(selected) != count or any(type(i) is not int for i in selected)
                or (not composite and (selected != sorted(set(selected))
                    or any(not 0 <= i < len(records['frames']) for i in selected)))):
            raise ValueError(state + ': explicit, real sequential frames must preserve the combat frame count')
        main = animation.get('impactFrame', spec.get('mainIndex'))
        if type(main) is not int or not 0 <= main < count or spec.get('mainIndex') != main:
            raise ValueError(state + ': main pose does not preserve impact index')
        if count > 1 and state not in ('IDLE', 'COMBAT') and main == 0:
            raise ValueError(state + ': independent preparation and main pose required')
        if len(frame_specs) != count:
            raise ValueError(state + ': each source frame needs explicit root and measured uniform scale')
        source_names = [f.get('videoName', name) for f in frame_specs]
        source_records, source_origins = {name: records}, {name: origin}
        for source_name in set(source_names) - {name}:
            if source_name not in originals:
                raise ValueError(state + ': bridge video is not an inventoried Drive original')
            source_record = read(ROOT / 'docs/art/p12-video-sources' / (source_name.removesuffix('.mp4') + '.frames.json'))
            source_origin = spec.get('videoOrigins', {}).get(source_name, {})
            if (source_origin.get('driveId') != originals[source_name]
                    or source_origin.get('videoSha256') != source_record['videoSha256']):
                raise ValueError(state + ': bridge origin differs from the recorded Drive original')
            source_records[source_name], source_origins[source_name] = source_record, source_origin
        bind_sources(state, source_names, selected, main, name, count, source_records)
        frames, references, recipes = [], [], []
        for ordinal, (index, frame_spec) in enumerate(zip(selected, frame_specs)):
            if frame_spec.get('videoFrame') != index:
                raise ValueError(state + ': source root/scale attached to a different video frame')
            scale, root = frame_spec.get('scale'), frame_spec.get('root')
            if (not isinstance(scale, (float, int)) or not 0 < scale <= 1
                    or not isinstance(root, list) or len(root) != 2
                    or any(not isinstance(n, (float, int)) or not np.isfinite(n) for n in root)):
                raise ValueError(state + ': invalid measured scale/root')
            source_name = source_names[ordinal]
            source, used, recipe = original_frame(state, index, ordinal, main,
                source_origins[source_name], source_records[source_name]['frames'][index], frame_spec)
            anchor = spec.get('anchor', [RX, RY])
            image = render_pose(source, scale, root, anchor)
            recipe.update(scale=scale, root=root, anchor=anchor,
                          nativePixelSha256=hashlib.sha256(image.tobytes()).hexdigest())
            frames.append(image)
            references.extend(used)
            recipes.append(recipe)
        rendered[state] = (frames, list(dict.fromkeys(references)), recipes)
    return registration, contract, rendered


def prepare(weights, selected_states=None):
    registration, contract, rendered = preflight(selected_states)
    # Model and ALL source registrations validate before the first runtime write.
    upscaler = VideoUpscaler(weights)
    manifest = {'character': 'p12', 'status': 'PARTIAL_NOT_ART_APPROVED',
        'sourcePolicy': registration['sourcePolicy'], 'modelSha256': MODEL_HASH,
        'registrationSha256': sha(REGISTRATION), 'contractSha256': sha(CONTRACT), 'states': {}}
    if MANIFEST.exists():
        prior = read(MANIFEST)
        if (prior.get('registrationSha256') == manifest['registrationSha256']
                and prior.get('modelSha256') == manifest['modelSha256']
                and prior.get('contractSha256') == manifest['contractSha256']):
            manifest['states'] = prior['states']
    for state, (frames, references, recipes) in rendered.items():
        aid = 'p12_full_' + state.lower()
        native = ROOT / f'art/sprites/source/{aid}_original.png'
        refined = ROOT / f'art/sprites/source/{aid}_sr2x.png'
        # Refine the selected real poses, using only the existing model-bound
        # per-frame cache. Native and refined alpha remain independently checked.
        high = atlas([upscaler.sr(f) for f in frames])
        atlas(frames).save(native)
        high.save(refined)
        path = ROOT / f'tools/sprites/clips/{aid}.json'
        cfg = read(path)
        cfg.update(source=refined.relative_to(ROOT).as_posix(),
                   sourceReferences=[native.relative_to(ROOT).as_posix()] + references,
                   frameWidth=FW * 2, frameHeight=FH * 2,
                   rootX=registration['states'][state].get('anchor', [RX, RY])[0] * 2,
                   rootY=registration['states'][state].get('anchor', [RX, RY])[1] * 2,
                   expectedFrames=len(frames), pixelScale=2,
                   sourceNote='Existing animations/p12 Drive video pixels; measured uniform camera correction; alpha removal; cached AnimeVideo-v3 SR')
        write_json(path, cfg)
        manifest['states'][state] = {'recipes': recipes, 'nativeSha256': sha(native), 'refinedSha256': sha(refined)}
        print(f'{state}: {len(frames)} recorded original video frames actually consumed', flush=True)
    profile_path = ROOT / 'tools/sprites/profiles/p12.json'
    profile = read(profile_path)
    profile['anatomyReference'].update(frameWidth=FW, frameHeight=FH)
    if 'IDLE' in rendered:
        # This field describes the shipped Idle's bbox, not an upright key
        # drawing. The real zombie is hunched; enlarging it to the rejected
        # pilot's 208px Idle would inflate its head relative to other videos.
        threshold = profile['validation']['alphaThreshold']
        frames = rendered['IDLE'][0]
        top = min(f.getchannel('A').resize((FW * 2, FH * 2), Image.Resampling.LANCZOS)
                  .point(lambda a: 255 if a > threshold else 0).getbbox()[1] for f in frames)
        profile['standingVisualHeight'] = round((RY * 2 - top) / 2)
    write_json(profile_path, profile)
    manifest['srCache'] = {'hits': upscaler.hits, 'misses': upscaler.misses}
    if set(manifest['states']) == set(contract['animations']):
        manifest['status'] = 'PREPARED_NOT_ART_APPROVED'
    write_json(MANIFEST, manifest)


def verify(selected_states=None):
    registration, contract, rendered = preflight(selected_states)
    manifest = read(MANIFEST)
    if (manifest.get('sourcePolicy') != 'EXISTING_DRIVE_VIDEOS_ONLY'
            or manifest['registrationSha256'] != sha(REGISTRATION)
            or manifest['contractSha256'] != sha(CONTRACT)):
        raise ValueError('P12 source/render manifest is stale')
    if selected_states is None and set(manifest['states']) != set(contract['animations']):
        raise ValueError('final video verification requires all 42 prepared states')
    for state, (frames, references, recipes) in rendered.items():
        aid = 'p12_full_' + state.lower()
        native = ROOT / f'art/sprites/source/{aid}_original.png'
        refined = ROOT / f'art/sprites/source/{aid}_sr2x.png'
        row = manifest['states'][state]
        if (row['recipes'] != recipes or sha(native) != row['nativeSha256']
                or sha(refined) != row['refinedSha256']
                or Image.open(native).convert('RGBA').tobytes() != atlas(frames).tobytes()):
            raise ValueError(state + ': atlas does not reproduce the consumed original video pixels')
        cfg = read(ROOT / f'tools/sprites/clips/{aid}.json')
        verify_clip_binding(state, cfg, registration['states'][state], len(frames))
        if not set(references) <= set(cfg.get('sourceReferences', [])):
            raise ValueError(state + ': used original source files missing from source gate binding')
        high = Image.open(refined).convert('RGBA')
        if high.size != (FW * 8, FH * 2 * ((len(frames) + 3) // 4)):
            raise ValueError(state + ': wrong refined atlas size')
        for i, frame in enumerate(frames):
            cell = high.crop((i % 4 * FW * 2, i // 4 * FH * 2, (i % 4 + 1) * FW * 2, (i // 4 + 1) * FH * 2))
            if cell.getchannel('A').tobytes() != frame.getchannel('A').resize((FW * 2, FH * 2), Image.Resampling.LANCZOS).tobytes():
                raise ValueError(state + ': refined alpha differs from original native alpha')
    print(f'{len(rendered)}/42 states: original-video pixels, native reproduction, SR alpha and combat contract verified. Mandatory art audits remain.')


def verify_clip_binding(state, cfg, spec, count):
    anchor = spec.get('anchor', [RX, RY])
    required = {'source': f'art/sprites/source/p12_full_{state.lower()}_sr2x.png',
                'frameWidth': FW * 2, 'frameHeight': FH * 2, 'columns': 4,
                'rootX': anchor[0] * 2, 'rootY': anchor[1] * 2,
                'expectedFrames': count, 'pixelScale': 2, 'rootMode': 'authored'}
    for key, value in required.items():
        if cfg.get(key) != value:
            raise ValueError(state + ': runtime clip binding differs from consumed video pixels: ' + key)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('operation', choices=('prepare', 'verify'))
    parser.add_argument('--weights', type=Path)
    parser.add_argument('--states', nargs='+', help='Reviewed batch only; omit for mandatory full 42-state verification')
    args = parser.parse_args()
    if args.operation == 'prepare':
        if args.weights is None:
            parser.error('prepare requires --weights')
        prepare(args.weights, args.states)
    else:
        verify(args.states)
