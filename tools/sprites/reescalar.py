#!/usr/bin/env python3
"""Acerta o tamanho de um clipe quadro a quadro pela cabeça do idle (ou do agachado): corrige
escala errada e zoom de câmera no meio do vídeo. Edita a folha normalizada (escala em torno da
raiz, pés no chão) e grava os fatores na receita, para a reconversão repetir.

    python3 tools/sprites/reescalar.py p02 MEDIUM_KICK TAUNT --ver   # só mede
    python3 tools/sprites/reescalar.py p02 MEDIUM_KICK TAUNT

Quadro em que a cabeça não é achada (virada, coberta) usa o fator dos vizinhos.
Depois rode build_characters.py --write.
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import auditoria as A  # noqa: E402
from ancorar import clip_path  # noqa: E402

MIN_SCORE = 0.7    # nota mínima da cabeça para valer
TOLERANCE = 0.04   # fator mais perto de 1 que isso fica 1


def head_zoom(tmpl, rgba):
    import cv2
    img, best = A.flat(rgba), (-1.0, 1.0)
    for z in np.exp(np.linspace(np.log(0.65), np.log(1.5), 61)):
        t = cv2.resize(tmpl, None, fx=z, fy=z, interpolation=cv2.INTER_AREA if z < 1 else cv2.INTER_CUBIC)
        if img.shape[0] >= t.shape[0] and img.shape[1] >= t.shape[1]:
            best = max(best, (float(cv2.matchTemplate(img, t, cv2.TM_CCOEFF_NORMED).max()), float(z)))
    return best


def factors(cid, state, pack, clips):
    low = state in A.CROUCHED or state in ('CROUCH_HEAVY', 'RISE')
    ref = pack['animations']['CROUCH' if low else 'IDLE']
    rs = A.Sheet(clips[ref['atlas']])
    f = ref['frames'][-1 if low else 0]
    tmpl = A.head_template(A.measure(rs.frame(f), rs.rx, rs.ry), rs.frame(f))
    sheet = A.Sheet(clips[pack['animations'][state]['atlas']])
    count = sheet.cfg['expectedFrames']
    found = [head_zoom(tmpl, sheet.frame(i)) for i in range(count)]
    good = [i for i, (r, _) in enumerate(found) if r >= MIN_SCORE]
    if len(good) < 2:
        raise SystemExit(f'{cid} {state}: cabeça achada em menos de 2 quadros, nada feito')
    # Sem cabeça no quadro: o fator do vizinho achado mais perto (zoom de vídeo costuma ser corte).
    z = np.array([found[min(good, key=lambda g: (abs(g - i), g))][1] for i in range(count)])
    z[np.abs(z - 1) < TOLERANCE] = 1.0
    return np.round(z, 3), found


def rescale(cfg, z):
    sheet = A.Sheet(cfg)
    img = Image.open(A.ROOT / cfg['source']).convert('RGBA')
    out = Image.new('RGBA', img.size, (0, 0, 0, 0))
    for i, f in enumerate(z):
        x, y = i % sheet.cols * sheet.w, i // sheet.cols * sheet.h
        cell = img.crop((x, y, x + sheet.w, y + sheet.h))
        if f != 1:
            small = cell.resize((round(sheet.w / f), round(sheet.h / f)), Image.LANCZOS)
            cell = Image.new('RGBA', cell.size, (0, 0, 0, 0))
            cell.alpha_composite(small, (sheet.rx - round(sheet.rx / f), sheet.ry - round(sheet.ry / f)))
        out.alpha_composite(cell, (x, y))
    out.save(A.ROOT / cfg['source'])
    A.Sheet.cache.pop(cfg['source'], None)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('estados', nargs='+')
    p.add_argument('--ver', action='store_true', help='só mostra os fatores')
    p.add_argument('--fatores', help='fatores à mão, um por quadro (ex.: 1,1.3,1.3), quando a medida erra')
    args = p.parse_args()
    pack, clips = A.load_pack(args.personagem)
    for state in args.estados:
        atlas = pack['animations'][state]['atlas']
        z, found = factors(args.personagem, state, pack, clips)
        if args.fatores:
            z = np.array([float(v) for v in args.fatores.split(',')])
            if len(z) != len(found):
                raise SystemExit(f'--fatores tem {len(z)} valores; o clipe tem {len(found)} quadros')
        print(f'{args.personagem} {state}: ' + ' '.join(f'{f:.2f}' for f in z))
        print('   cabeça: ' + ' '.join(f'{zz:.2f}/{r:.2f}' for r, zz in found))
        if args.ver or not (z != 1).any():
            continue
        rescale(clips[atlas], z)
        path = clip_path(atlas)
        data = json.loads(path.read_text(encoding='utf-8'))
        target = data['video'] if 'video' in data else data
        target['reescala'] = [float(f) for f in z]
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
