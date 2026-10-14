#!/usr/bin/env python3
"""Aplica as mecânicas de cada personagem (tools/sprites/mecanicas.json) no character.json:

  * vida, dano e projétil (velocidade e alcance) de cada um;
  * frame data dos golpes: o tempo base vezes o ritmo do personagem (golpe a golpe se
    houver "ritmoGolpe"). A vantagem no hit e na defesa é a mesma para todos (hitstun e
    blockstun acompanham); janelas de cancelamento e caixas são remapeadas fase a fase
    (startup, ativo, recuperação), então continuam no mesmo trecho do golpe;
  * cadência da animação dos golpes: o motor estica a animação sobre o golpe inteiro, então
    só a proporção entre os quadros importa. Cada quadro ganha o tempo que tinha no vídeo
    (receita do clipe, saltos acima de 2x a mediana limitados) e o quadro do impacto é
    encaixado no começo da janela ativa.

Com "manterTempo" (p01, arte desenhada quadro a quadro) só vida, dano e projétil mudam.
Alcance e caixas de acerto não mudam aqui (vêm do DEBUG > AJUSTAR CAIXAS). O Super mantém
o tempo e a animação (só o dano muda). Depois rode build_characters.py --write.

    python3 tools/sprites/mecanicas.py            # só mostra
    python3 tools/sprites/mecanicas.py --write    # grava
    python3 tools/sprites/mecanicas.py p09 p10    # só alguns
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import auditoria as A  # noqa: E402

DATA = Path(__file__).with_name('mecanicas.json')
GAP_CAP = 2.0   # um quadro fica no máximo 2x o tempo mediano do clipe


def remap(t, old, new):
    """Frame t do golpe antigo no golpe novo, fase a fase."""
    (s, a, r), (s2, a2, r2) = old, new
    if t < s:
        return t * s2 / s
    if t < s + a:
        return s2 + (t - s) * a2 / a
    return s2 + a2 + ((t - s - a) * r2 / r if r else 0)


def retime(m, new, advantage=None):
    old = (m['startupFrames'], m['activeFrames'], m['recoveryFrames'])
    s, a, r = new
    m['startupFrames'], m['activeFrames'], m['recoveryFrames'] = s, a, r
    total, last_active = s + a + r, s + a - 1
    if advantage:
        m['hitstunFrames'] = max(1, a - 1 + r + advantage[0])
        m['blockstunFrames'] = max(1, a - 1 + r + advantage[1])
    for key, w in m['cancelWindows'].items():
        if w:
            first = 0 if key == 'whiff' else s
            lo = min(max(first, round(remap(w[0], old, new))), total - 1)
            m['cancelWindows'][key] = [lo, min(max(lo, round(remap(w[1], old, new))), total - 1)]
    for key, first, last in (('hitboxes', s, last_active), ('hurtboxes', 0, total - 1)):
        for box in m.get(key, []):
            lo = min(max(first, round(remap(box['frames'][0], old, new))), last)
            box['frames'] = [lo, min(max(lo, round(remap(box['frames'][1], old, new))), last)]


def video_cadence(anim, cfg, total_ms):
    """durationsMs na proporção do vídeo; None se o clipe não tem receita de vídeo."""
    v = A.video_seconds(cfg, anim)
    if v is None or len(v) != len(anim['frames']) or v.sum() <= 0:
        return None
    v = np.minimum(v, GAP_CAP * np.median(v))
    return [max(1, round(x)) for x in v / v.sum() * total_ms]


def fit_impact(anim, m):
    """Quadro do impacto começando junto com o primeiro frame ativo (o golpe encosta quando
    acerta): redistribui o tempo antes e depois dele, mesmo total."""
    i, d = anim.get('impactFrame'), anim['durationsMs']
    if i is None or not 0 < i < len(d):
        return
    total = m['startupFrames'] + m['activeFrames'] + m['recoveryFrames']
    start = m['startupFrames'] / total
    whole, before = sum(d), sum(d[:i])
    if start - 0.75 / total <= before / whole <= start < (before + d[i]) / whole:
        return
    # começa meio frame antes do acerto e dura pelo menos um frame de jogo
    lead, hold = start - 0.5 / total, max(d[i] / whole, 1 / total)
    after = sum(d[i + 1:])
    head = [x * lead * whole / before for x in d[:i]]
    tail = [x * (1 - lead - hold) * whole / after for x in d[i + 1:]] if after else []
    anim['durationsMs'] = [max(1, round(x)) for x in head + [hold * whole] + tail]


def scaled(base, f):
    return [max(1, round(x * f)) for x in base]


def apply(cid, spec, base):
    path = A.ROOT / f'characters/{cid}/character.json'
    pack = json.loads(path.read_text(encoding='utf-8'))
    _, clips = A.load_pack(cid)
    fighter, report = pack['fighter'], []
    fighter['maxLife'] = spec['vida']
    keep = spec.get('manterTempo', False)   # arte própria calibrada golpe a golpe (p01)

    def pace(key):
        return spec.get('ritmoGolpe', {}).get(key, spec['ritmo'])

    def damage(key):
        return int(round(base['dano'][key] * spec['dano'] / 10) * 10)

    attacks = []
    for key, m in list(pack['moves'].items()) + list(pack.get('specialMoves', {}).items()):
        if key not in base['frames']:
            continue
        if not keep:
            retime(m, scaled(base['frames'][key], pace(key)), base['vantagem'][key])
        m['damage'] = damage(key)
        attacks.append((key, m.get('animation'), m))
    energy = fighter['energy']
    if not keep:
        retime(energy['attack'], scaled(base['frames']['S'], pace('S')))
    energy['damage'] = damage('S')
    energy['speed'] = spec['projetil']['velocidade']
    energy['range'] = spec['projetil']['alcance']
    attacks.append(('S', pack.get('specialAnimations', {}).get('S'), energy['attack']))
    fighter['super']['damage'] = damage('SUPER')

    for key, anim_id, m in attacks:
        total = m['startupFrames'] + m['activeFrames'] + m['recoveryFrames']
        anim = pack['animations'].get(anim_id) if anim_id else None
        cadence = 'sem animação' if not keep else 'tempo mantido'
        if anim and not keep and not anim.get('loop') and 'durationsMs' in anim:
            d = video_cadence(anim, clips.get(anim['atlas'], {}), total * 1000 / 60)
            if d:
                anim['durationsMs'], cadence = d, 'vídeo'
            else:
                cadence = 'mantida'
            fit_impact(anim, m)
        dmg = energy['damage'] if key == 'S' else m['damage']
        report.append(f"  {key:4} {dmg:5}  {m['startupFrames']:2}/{m['activeFrames']:2}/{m['recoveryFrames']:2}"
                      f"  {total / 60:.2f}s  cadência {cadence}")
    return pack, path, report


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='*')
    p.add_argument('--write', action='store_true', help='grava os character.json')
    args = p.parse_args()
    data = json.loads(DATA.read_text(encoding='utf-8'))
    for cid in args.personagens or list(data['personagens']):
        spec = data['personagens'][cid]
        pack, path, report = apply(cid, spec, data['base'])
        print(f"{cid} ({spec['molde']}): vida {spec['vida']}, dano {spec['dano']}x, ritmo {spec['ritmo']}x, "
              f"Super {pack['fighter']['super']['damage']}")
        print('\n'.join(report))
        if args.write:
            path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
