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

import numpy as np
from PIL import Image

from p12_video_sources import ROOT, STATE_FILES, sha, write_json
from p12_authored_cycle import CachedUpscaler, atlas, render_pose, FW, FH, RX, RY, MODEL_HASH

REGISTRATION = ROOT / 'art/keys/p12/registration.json'
CONTRACT = ROOT / 'docs/art/p12-motion-contract.json'
MANIFEST = ROOT / 'docs/art/p12-video-render-manifest.json'


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
        relative = f'art/keys/p12/video_frames/{tag}/{index:04d}.png'
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
        mask_name = f'art/keys/p12/video_masks/{tag}/{index:04d}.png'
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
    return picture, references, recipe


def preflight():
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
    inventory = read(ROOT / 'tools/sprites/videos/p12-drive-inventory.json')
    originals = {f['name']: f['id'] for f in inventory['files'] if f['mimeType'] == 'video/mp4'}
    rendered = {}
    for state, animation in contract['animations'].items():
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
        if (len(selected) != count or any(type(i) is not int for i in selected)
                or selected != sorted(set(selected))
                or any(not 0 <= i < len(records['frames']) for i in selected)):
            raise ValueError(state + ': explicit, real sequential frames must preserve the combat frame count')
        main = animation.get('impactFrame', spec.get('mainIndex'))
        if type(main) is not int or not 0 <= main < count or spec.get('mainIndex') != main:
            raise ValueError(state + ': main pose does not preserve impact index')
        if count > 1 and state not in ('IDLE', 'COMBAT') and main == 0:
            raise ValueError(state + ': independent preparation and main pose required')
        frame_specs = spec.get('frames', [])
        if len(frame_specs) != count:
            raise ValueError(state + ': each source frame needs explicit root and measured uniform scale')
        frames, references, recipes = [], [], []
        for ordinal, (index, frame_spec) in enumerate(zip(selected, frame_specs)):
            if frame_spec.get('videoFrame') != index:
                raise ValueError(state + ': source root/scale attached to a different video frame')
            scale, root = frame_spec.get('scale'), frame_spec.get('root')
            if (not isinstance(scale, (float, int)) or not 0 < scale <= 1
                    or not isinstance(root, list) or len(root) != 2
                    or any(not isinstance(n, (float, int)) or not np.isfinite(n) for n in root)):
                raise ValueError(state + ': invalid measured scale/root')
            source, used, recipe = original_frame(state, index, ordinal, main, origin, records['frames'][index], frame_spec)
            image = render_pose(source, scale, root)
            recipe.update(scale=scale, root=root,
                          nativePixelSha256=hashlib.sha256(image.tobytes()).hexdigest())
            frames.append(image)
            references.extend(used)
            recipes.append(recipe)
        rendered[state] = (frames, list(dict.fromkeys(references)), recipes)
    return registration, contract, rendered


def prepare(weights):
    registration, contract, rendered = preflight()
    # Model and ALL source registrations validate before the first runtime write.
    upscaler = CachedUpscaler(weights)
    manifest = {'character': 'p12', 'status': 'PREPARED_NOT_ART_APPROVED',
        'sourcePolicy': registration['sourcePolicy'], 'modelSha256': MODEL_HASH,
        'registrationSha256': sha(REGISTRATION), 'contractSha256': sha(CONTRACT), 'states': {}}
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
        cfg.update(sourceReferences=[native.relative_to(ROOT).as_posix()] + references,
                   frameWidth=FW * 2, frameHeight=FH * 2, rootX=RX * 2, rootY=RY * 2,
                   expectedFrames=len(frames), pixelScale=2,
                   sourceNote='Existing animations/p12 Drive video pixels; measured uniform camera correction; alpha removal; cached AnimeVideo-v3 SR')
        write_json(path, cfg)
        manifest['states'][state] = {'recipes': recipes, 'nativeSha256': sha(native), 'refinedSha256': sha(refined)}
        print(f'{state}: {len(frames)} recorded original video frames actually consumed', flush=True)
    profile_path = ROOT / 'tools/sprites/profiles/p12.json'
    profile = read(profile_path)
    profile['anatomyReference'].update(frameWidth=FW, frameHeight=FH)
    write_json(profile_path, profile)
    manifest['srCache'] = {'hits': upscaler.hits, 'misses': upscaler.misses}
    write_json(MANIFEST, manifest)


def verify():
    registration, contract, rendered = preflight()
    manifest = read(MANIFEST)
    if (manifest.get('sourcePolicy') != 'EXISTING_DRIVE_VIDEOS_ONLY'
            or manifest['registrationSha256'] != sha(REGISTRATION)
            or manifest['contractSha256'] != sha(CONTRACT)):
        raise ValueError('P12 source/render manifest is stale')
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
        if not set(references) <= set(cfg.get('sourceReferences', [])):
            raise ValueError(state + ': used original source files missing from source gate binding')
        high = Image.open(refined).convert('RGBA')
        if high.size != (FW * 8, FH * 2 * ((len(frames) + 3) // 4)):
            raise ValueError(state + ': wrong refined atlas size')
        for i, frame in enumerate(frames):
            cell = high.crop((i % 4 * FW * 2, i // 4 * FH * 2, (i % 4 + 1) * FW * 2, (i // 4 + 1) * FH * 2))
            if cell.getchannel('A').tobytes() != frame.getchannel('A').resize((FW * 2, FH * 2), Image.Resampling.LANCZOS).tobytes():
                raise ValueError(state + ': refined alpha differs from original native alpha')
    print('Original-video source consumption, native reproduction, SR alpha and immutable combat contract verified. Mandatory art audits remain.')
