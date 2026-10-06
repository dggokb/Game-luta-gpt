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
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
# Poses that should keep the torso over the root. Attacks lunge on purpose.
REGISTERED = ('IDLE', 'COMBAT', 'WALK_FORWARD', 'WALK_BACK', 'CROUCH', 'RISE', 'LAND',
              'DASH', 'BACKDASH', 'JUMP', 'FALL', 'DEFENSE_STAND', 'DEFENSE_CROUCH',
              'DEFENSE_AIR', 'HIT_STAND', 'HIT_CROUCH', 'GETUP')
GROUNDED = ('IDLE', 'COMBAT', 'WALK_FORWARD', 'WALK_BACK', 'CROUCH', 'RISE', 'LAND',
            'DASH', 'BACKDASH', 'DEFENSE_STAND', 'DEFENSE_CROUCH', 'HIT_STAND',
            'HIT_CROUCH', 'KNOCKDOWN', 'GROUNDED', 'GETUP')
TORSO_TOLERANCE = 20      # px, world units at worldScale 1
SPIKE_TOLERANCE = 40      # one attack frame jumping out and back by more than this
GROUND_TOLERANCE = 4      # px above/below the root row
REACTION_MIN_RATIO = 0.85 # standing defense height vs Idle height


def _read(path):
    return json.loads(Path(path).read_text())


def frame_cell(root, atlas_id, frame, cache):
    clip = _read(root / f'tools/sprites/clips/{atlas_id}.json')
    packed = _read(root / f'tools/sprites/reports/{atlas_id}.report.json')['packed']
    if atlas_id not in cache:
        cache[atlas_id] = Image.open(root / clip['output']).convert('RGBA')
    col, row = frame % packed['columns'], frame // packed['columns']
    w, h = packed['frameWidth'], packed['frameHeight']
    return cache[atlas_id].crop((col * w, row * h, col * w + w, row * h + h)), packed


def measure(cell, packed, scale):
    alpha = cell.getchannel('A').point(lambda v: 255 if v > 10 else 0)
    x0, y0, x1, y1 = alpha.getbbox()
    px = alpha.load()
    mids = []
    height = y1 - y0
    for y in range(y0 + int(height * 0.30), y0 + int(height * 0.55)):
        row = [x for x in range(cell.width) if px[x, y]]
        if row:
            mids.append((min(row) + max(row)) / 2)
    return {
        'torso': (statistics.median(mids) - packed['rootX']) * scale,
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
                if name in REGISTERED and abs(m['torso'] - idle_torso) > TORSO_TOLERANCE:
                    problems.append(f"{where}: torso {m['torso'] - idle_torso:+.0f}px off the Idle registration")
                if name in GROUNDED and abs(m['ground']) > GROUND_TOLERANCE:
                    problems.append(f"{where}: feet {m['ground']:+.0f}px from the ground")
            # Out-and-back spike inside a move: neighbours agree, the middle frame does not.
            torsos = [m['torso'] for m in frames]
            for i in range(1, len(torsos) - 1):
                before, here, after = torsos[i - 1], torsos[i], torsos[i + 1]
                if (abs(before - after) <= 15 and abs(here - before) > SPIKE_TOLERANCE
                        and abs(here - after) > SPIKE_TOLERANCE):
                    problems.append(f"{pack['id']}/{name}[{animation['frames'][i]}]: "
                                    f"torso spikes {here - before:+.0f}px and back")
        if 'DEFENSE_STAND' in metrics:
            ratio = metrics['DEFENSE_STAND'][0]['height'] / idle_height
            if ratio < REACTION_MIN_RATIO:
                problems.append(f"{pack['id']}/DEFENSE_STAND: body at {ratio:.0%} of Idle height")
    return rows, problems


if __name__ == '__main__':
    rows, problems = audit()
    for r in rows:
        print(f"{r['character']:12s} {r['state']:15s} f{r['frame']:<3} torso={r['torso']:+6.1f} "
              f"height={r['height']:6.1f} ground={r['ground']:+5.1f}")
    print('\n'.join(problems) if problems else 'Harmony PASS')
