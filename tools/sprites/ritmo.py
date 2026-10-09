#!/usr/bin/env python3
"""Acerta o tempo das animações que a arte comanda, para cada personagem ter o ritmo da
própria arte em vez de copiar o tempo do p01:

  * movimento (andar, dash, pulo, queda, aterrissagem, agachar, levantar): o mesmo ritmo
    visual do p01 no mesmo estado. Arte com mais movimento por quadro (passo maior, corrida
    de verdade) ganha mais tempo por quadro ou mais distância por quadro, e o pé deixa de
    patinar;
  * livres (idle, intro, vitória, derrota, provocação): o tempo do próprio vídeo.

    python3 tools/sprites/ritmo.py p02 --ver     # só mostra
    python3 tools/sprites/ritmo.py p02           # grava no character.json

  * golpes, especiais e super: frame data próprio do personagem, esticado (ou encurtado)
    pelo ritmo da arte contra o p01, até 2x. Hitstun e blockstun sobem junto com o ativo e a
    recuperação, então a vantagem do golpe não muda; janelas de cancelamento e caixas de
    acerto acompanham;
  * levantar do chão: wakeupFrames próprio do personagem, no tempo do clipe GETUP.

Reações ao golpe (esticadas pelo hitstun de quem bate), defesa e backdash ficam no motor.
Depois rode build_characters.py --write.
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import auditoria as A  # noqa: E402

VISUAL = ['WALK_FORWARD', 'WALK_BACK', 'DASH', 'JUMP', 'FALL', 'LAND', 'CROUCH', 'RISE']
TOLERANCE = 0.25   # diferença de ritmo menor que isso fica como está
LIMIT = 3.0        # nunca estica/encurta mais que isso de uma vez
MOVE_LIMIT = (0.67, 2.0)   # frame data: no máximo 2x mais lento ou 1,5x mais rápido


def plan(cid):
    """{estado: (fator, motivo)}: fator multiplica o tempo de cada quadro."""
    pack, clips = A.load_pack(cid)
    out = {}
    if cid != A.REFERENCE:
        for state in VISUAL:
            mine, ref = A.pace(cid, state), A.pace(A.REFERENCE, state)
            if mine and ref and abs(mine / ref - 1) >= TOLERANCE:
                out[state] = (float(np.clip(mine / ref, 1 / LIMIT, LIMIT)), f'ritmo {mine / ref:.2f}x o do p01')
    if cid != A.REFERENCE:
        # GETUP: o clipe no ritmo do p01; o motor espera o clipe (wakeupFrames do personagem).
        mine, ref = A.pace(cid, 'GETUP'), A.pace(A.REFERENCE, 'GETUP')
        if mine and ref and abs(mine / ref - 1) >= TOLERANCE:
            out['GETUP'] = (float(np.clip(mine / ref, 1 / LIMIT, LIMIT)), f'ritmo {mine / ref:.2f}x o do p01')
    timing = A.attack_seconds(pack)
    for state in sorted(A.FREE):
        if state not in pack['animations']:
            continue
        anim = pack['animations'][state]
        video = A.video_seconds(clips[anim['atlas']], anim)
        game = A.game_seconds(pack, state, timing)
        if video is not None and game.sum() > 0:
            f = video.sum() / game.sum()
            if abs(f - 1) >= TOLERANCE:
                out[state] = (float(np.clip(f, 1 / LIMIT, LIMIT)), f'{f:.2f}x a velocidade do vídeo')
    return out


def attacks(pack):
    """(nome, animação, frame data, projétil) de cada golpe com animação."""
    out = [(k, m['animation'], m, False) for k, m in list(pack['moves'].items()) +
           list(pack.get('specialMoves', {}).items()) if 'animation' in m]
    fighter = pack['fighter']
    for key, anim in pack.get('specialAnimations', {}).items():
        src = {'S': fighter.get('energy'), 'SUPER': fighter.get('super')}.get(key)
        if src and 'attack' in src:
            out.append((key, anim, src['attack'], True))
    return out


def plan_moves(cid):
    """{golpe: (fator, motivo)} sobre o frame data atual. O alvo é o tempo do p01 vezes o
    ritmo da arte (limitado), então rodar de novo não estica outra vez."""
    if cid == A.REFERENCE:
        return {}
    pack, _ = A.load_pack(cid)
    ref_pack, _ = A.load_pack(A.REFERENCE)
    mine_t, ref_t = A.attack_seconds(pack)[0], A.attack_seconds(ref_pack)[0]
    out = {}
    for name, anim, _, _ in attacks(pack):
        mine, ref = A.pace(cid, anim), A.pace(A.REFERENCE, anim)
        if not (mine and ref and anim in mine_t and anim in ref_t):
            continue
        now = mine_t[anim] / ref_t[anim]             # quanto o golpe já é mais longo que o do p01
        want = mine * now / ref                        # ritmo da arte se tivesse o tempo do p01
        target = float(np.clip(want, *MOVE_LIMIT)) if abs(want - 1) >= TOLERANCE else 1.0
        if abs(target / now - 1) >= 0.1:
            out[name] = (target / now, f'{anim}: ritmo {want:.2f}x o do p01, tempo {target:.2f}x o do p01')
    return out


def scale_move(m, f, projectile):
    """Estica o frame data mantendo a vantagem (hitstun/blockstun acompanham)."""
    old = {k: m[k] for k in ('startupFrames', 'activeFrames', 'recoveryFrames')}
    m['startupFrames'] = max(1, round(old['startupFrames'] * f))
    if not projectile:  # projétil sai num quadro ativo só
        m['activeFrames'] = max(1, round(old['activeFrames'] * f))
    m['recoveryFrames'] = max(1, round(old['recoveryFrames'] * f))
    later = (m['activeFrames'] - old['activeFrames']) + (m['recoveryFrames'] - old['recoveryFrames'])
    for key in ('hitstunFrames', 'blockstunFrames'):
        m[key] = max(1, m[key] + later)
    total = m['startupFrames'] + m['activeFrames'] + m['recoveryFrames']
    last_active = m['startupFrames'] + m['activeFrames'] - 1
    for key, w in m['cancelWindows'].items():
        if w:
            first = 0 if key == 'whiff' else m['startupFrames']
            a = min(max(first, round(w[0] * f)), total - 1)
            m['cancelWindows'][key] = [a, min(max(a, round(w[1] * f)), total - 1)]
    for key, first, last in (('hitboxes', m['startupFrames'], last_active), ('hurtboxes', 0, total - 1)):
        for box in m.get(key, []):
            a = min(max(first, round(box['frames'][0] * f)), last)
            box['frames'] = [a, min(max(a, round(box['frames'][1] * f)), last)]


def fit_impact(anim, m):
    """Quadro de impacto no início da janela ativa: redistribui o tempo antes e depois dele
    (mesmo total), como o build exige."""
    i, d = anim.get('impactFrame'), anim['durationsMs']
    if i is None or not 0 < i < len(d):
        return
    total = m['startupFrames'] + m['activeFrames'] + m['recoveryFrames']
    start, end = m['startupFrames'] / total, (m['startupFrames'] + m['activeFrames']) / total
    t = sum(d[:i]) / sum(d)
    if start - 1 / total <= t <= end:
        return
    want, whole = (start + min(end, start + 0.5 / total)) / 2, sum(d)
    before, after = sum(d[:i]), whole - sum(d[:i])
    a, b = want * whole / before, (1 - want) * whole / after
    anim['durationsMs'] = [max(1, round(x * a)) for x in d[:i]] + [max(1, round(x * b)) for x in d[i:]]


def apply(cid, changes, moves=None):
    path = A.ROOT / f'characters/{cid}/character.json'
    pack = json.loads(path.read_text(encoding='utf-8'))
    for state, (f, _) in changes.items():
        anim = pack['animations'][state]
        if 'distancePerFrame' in anim:
            anim['distancePerFrame'] = round(anim['distancePerFrame'] * f, 2)
        else:
            anim['durationsMs'] = [max(1, round(d * f)) for d in anim['durationsMs']]
    if 'GETUP' in changes:
        pack['fighter']['wakeupFrames'] = max(1, round(sum(pack['animations']['GETUP']['durationsMs']) * 60 / 1000))
    for name, anim, m, projectile in attacks(pack):
        if moves and name in moves:
            scale_move(m, moves[name][0], projectile)
        fit_impact(pack['animations'][anim], m)
    path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='+')
    p.add_argument('--ver', action='store_true', help='só mostra')
    args = p.parse_args()
    for cid in args.personagens:
        changes, moves = plan(cid), plan_moves(cid)
        for state, (f, why) in changes.items():
            print(f'{cid} {state:13} tempo x{f:.2f} ({why})')
        for name, (f, why) in moves.items():
            print(f'{cid} golpe {name:7} frame data x{f:.2f} ({why})')
        if not changes and not moves:
            print(f'{cid}: nada a acertar')
        if not args.ver and (changes or moves):
            apply(cid, changes, moves)


if __name__ == '__main__':
    main()
