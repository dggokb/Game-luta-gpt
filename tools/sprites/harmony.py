#!/usr/bin/env python3
"""Visual harmony audit of the shipped character packs.

Checks what makes animation look jerky even when every atlas passes import:
  * registration - the torso of non-attack poses stays over the root, so changing
    state never makes the body jump sideways;
  * grounding    - poses on the ground touch the ground (no floating/sinking);
  * scale        - reaction poses keep the body size of the canonical Idle.
Run `python3 tools/sprites/harmony.py` for a per-frame report.
"""
import json
import statistics
import sys
from pathlib import Path
from PIL import Image, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
import folhas  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
# Poses that should keep the torso over the root. Attacks lunge on purpose.
REGISTERED = ('IDLE', 'COMBAT', 'WALK_FORWARD', 'WALK_BACK', 'CROUCH', 'RISE', 'LAND',
              'DASH', 'BACKDASH', 'JUMP', 'FALL', 'DEFENSE_STAND', 'DEFENSE_CROUCH',
              'DEFENSE_AIR', 'HIT_STAND', 'HIT_CROUCH', 'GETUP')
# BACKDASH is not here: the backdash is drawn as a hop (the body leaves the ground in the
# art while the engine slides the fighter), like the classic fighting-game backstep.
GROUNDED = ('IDLE', 'COMBAT', 'WALK_FORWARD', 'WALK_BACK', 'CROUCH', 'RISE', 'LAND',
            'DASH', 'DEFENSE_STAND', 'DEFENSE_CROUCH', 'HIT_STAND',
            'HIT_CROUCH', 'KNOCKDOWN', 'GROUNDED', 'GETUP')
TORSO_TOLERANCE = 20      # px, world units at worldScale 1
SPIKE_TOLERANCE = 40      # one attack frame jumping out and back by more than this
# Hit reactions recoil or fold on purpose (feet stay planted); they get a wider band.
REACTION_TORSO_TOLERANCE = 28
REACTIONS = ('HIT_STAND', 'HIT_CROUCH')
GROUND_TOLERANCE = 4      # px above/below the root row
AIRBORNE_CLEARANCE = 30   # feet this far above the root: an airborne pose drawn in the art
REACTION_MIN_RATIO = 0.85 # standing defense height vs Idle height
THIN = 9                  # px: lines thinner than this are props, not the body
# Frames checked by eye and kept as drawn: the measure misreads them, the art is right.
# Each entry says why; anything else that drifts still fails.
ACCEPTED = {
    # Handspring: the torso band crosses the legs in the air (hands on the ground). At the
    # 94% size tuned in the app (8565e3e9) the band moved onto the legs; at 100% it did not.
    'p03/GETUP[5]',
    # Small offsets that grew with the size tuned in the app (8565e3e9): 117%, 123% and 113%
    # of the sheet, the same pose before (-19 px, +32 px spike, -42 px spike).
    'p07/DEFENSE_AIR[3]', 'p07/SPECIAL_S3[14]', 'p08/DEFENSE_AIR[2]',
    # Impact of the final finger thrust (Autópsia): the flash and the beam drawn in front of
    # the hand fill the torso band and pull its middle forward. The body lines up with
    # frames 23 and 25 (silhouettes overlaid); only the effect is new in this frame.
    'p11/ULTRA_SEQ[24]',
    # Corrida de verdade no dash da p15: na passada os dois pés saem do chão (quadros 45-46 e
    # 54-55 do vídeo, e o 38 na ponta do pé). Com os pés colados no chão ela afundava 14 px
    # nesses quadros (solavanco); os quadros de apoio continuam no chão.
    'p15/DASH[0]', 'p15/DASH[7]', 'p15/DASH[8]', 'p15/DASH[16]', 'p15/DASH[17]',
}


def _read(path):
    return json.loads(Path(path).read_text())


def frame_cell(root, atlas_id, frame, cache):
    """One packed frame of an atlas; `cache` keeps each atlas image and layout."""
    if atlas_id not in cache:
        clip = _read(root / f'tools/sprites/clips/{atlas_id}.json')
        packed = _read(root / f'tools/sprites/reports/{atlas_id}.report.json')['packed']
        cache[atlas_id] = (Image.open(root / clip['output']).convert('RGBA'), packed)
    image, packed = cache[atlas_id]
    return folhas.cell(image, packed, frame), packed


def measure(cell, packed, scale):
    # HD atlas (pixelScale 2): measured at the logical size, like the 1x atlases.
    density = int(packed.get('pixelScale', 1))
    if density != 1:
        cell = cell.resize((cell.width // density, cell.height // density), Image.LANCZOS)
        packed = dict(packed, rootX=packed['rootX'] / density, rootY=packed['rootY'] / density)
    alpha = cell.getchannel('A').point(lambda v: 255 if v > 10 else 0)
    x0, y0, x1, y1 = alpha.getbbox()
    height = y1 - y0

    def torso(mask):
        px, mids = mask.load(), []
        for y in range(y0 + int(height * 0.30), y0 + int(height * 0.55)):
            row = [x for x in range(cell.width) if px[x, y]]
            if row:
                mids.append((min(row) + max(row)) / 2)
        return (statistics.median(mids) - packed['rootX']) * scale if mids else None
    # Thin lines (a whip, a ribbon) swing across the torso band inside a move: the spike
    # check uses the torso with them wiped out by an opening (the body survives it).
    body = alpha.filter(ImageFilter.MinFilter(THIN)).filter(ImageFilter.MaxFilter(THIN))
    whole = torso(alpha)
    core = torso(body) if body.getbbox() else None
    return {
        'torso': whole,
        'core': whole if core is None else core,
        'height': (packed['rootY'] - y0) * scale,
        'ground': (y1 - 1 - packed['rootY']) * scale,
    }


def audit(root=ROOT):
    """Returns (rows, problems) for every frame of every pack."""
    rows, problems, cache = [], [], {}
    for path in sorted((root / 'characters').glob('*/character.json')):
        pack = _read(path)
        scale = _read(root / 'tools/sprites/profiles' / pack['profile'])['worldScale']
        metrics = {}
        for name, animation in pack['animations'].items():
            metrics[name] = [measure(*frame_cell(root, animation['atlas'], f, cache), scale)
                             for f in animation['frames']]
        idle_torso = statistics.mean(m['torso'] for m in metrics['IDLE'])
        idle_height = statistics.mean(m['height'] for m in metrics['IDLE'])
        for name, frames in metrics.items():
            animation = pack['animations'][name]
            for f, m in zip(animation['frames'], frames):
                row = {'character': pack['id'], 'state': name, 'frame': f, **m}
                rows.append(row)
                where = f"{pack['id']}/{name}[{f}]"
                # Tolerances are art pixels (worldScale 1); measures are world units.
                limit = (REACTION_TORSO_TOLERANCE if name in REACTIONS else TORSO_TOLERANCE) * scale
                if name in REGISTERED and abs(m['torso'] - idle_torso) > limit:
                    problems.append(f"{where}: torso {m['torso'] - idle_torso:+.0f}px off the Idle registration")
                if name in GROUNDED and abs(m['ground']) > GROUND_TOLERANCE * scale:
                    problems.append(f"{where}: feet {m['ground']:+.0f}px from the ground")
            # Out-and-back spike inside a move: neighbours agree, the middle frame does not.
            # Frames drawn in the air (feet well above the root, e.g. a spinning kick) are
            # skipped: a leg stretched across the torso band moves the measure, not the body.
            torsos = [m['core'] for m in frames]
            for i in range(1, len(torsos) - 1):
                before, here, after = torsos[i - 1], torsos[i], torsos[i + 1]
                if frames[i]['ground'] < -AIRBORNE_CLEARANCE * scale:
                    continue
                if (abs(before - after) <= 15 * scale and abs(here - before) > SPIKE_TOLERANCE * scale
                        and abs(here - after) > SPIKE_TOLERANCE * scale):
                    problems.append(f"{pack['id']}/{name}[{animation['frames'][i]}]: "
                                    f"torso spikes {here - before:+.0f}px and back")
        if 'DEFENSE_STAND' in metrics:
            ratio = metrics['DEFENSE_STAND'][0]['height'] / idle_height
            if ratio < REACTION_MIN_RATIO:
                problems.append(f"{pack['id']}/DEFENSE_STAND: body at {ratio:.0%} of Idle height")
    return rows, [p for p in problems if p.split(':')[0] not in ACCEPTED]


if __name__ == '__main__':
    rows, problems = audit()
    for r in rows:
        print(f"{r['character']:12s} {r['state']:15s} f{r['frame']:<3} torso={r['torso']:+6.1f} "
              f"height={r['height']:6.1f} ground={r['ground']:+5.1f}")
    print('\n'.join(problems) if problems else 'Harmony PASS')
