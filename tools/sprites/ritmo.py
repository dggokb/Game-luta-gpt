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

Golpes, reações, defesa, queda/levantar do chão e backdash têm o tempo dado pelo motor
(frame data, hitstun, wakeup): a auditoria aponta o ritmo deles, mas o acerto é na arte ou
no frame data do personagem, não aqui. Depois rode build_characters.py --write.
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


def plan(cid):
    """{estado: (fator, motivo)}: fator multiplica o tempo de cada quadro."""
    pack, clips = A.load_pack(cid)
    out = {}
    if cid != A.REFERENCE:
        for state in VISUAL:
            mine, ref = A.pace(cid, state), A.pace(A.REFERENCE, state)
            if mine and ref and abs(mine / ref - 1) >= TOLERANCE:
                out[state] = (float(np.clip(mine / ref, 1 / LIMIT, LIMIT)), f'ritmo {mine / ref:.2f}x o do p01')
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


def apply(cid, changes):
    path = A.ROOT / f'characters/{cid}/character.json'
    pack = json.loads(path.read_text(encoding='utf-8'))
    for state, (f, _) in changes.items():
        anim = pack['animations'][state]
        if 'distancePerFrame' in anim:
            anim['distancePerFrame'] = round(anim['distancePerFrame'] * f, 2)
        else:
            anim['durationsMs'] = [max(1, round(d * f)) for d in anim['durationsMs']]
    path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='+')
    p.add_argument('--ver', action='store_true', help='só mostra')
    args = p.parse_args()
    for cid in args.personagens:
        changes = plan(cid)
        for state, (f, why) in changes.items():
            print(f'{cid} {state:13} tempo x{f:.2f} ({why})')
        if not changes:
            print(f'{cid}: nada a acertar')
        if not args.ver and changes:
            apply(cid, changes)


if __name__ == '__main__':
    main()
