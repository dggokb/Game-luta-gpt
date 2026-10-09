#!/usr/bin/env python3
"""Apaga efeito que o vídeo desenhou junto do personagem (poeira, arco, risco, clarão): mancha
de cor fora da paleta do personagem. Borda e luz sobre o corpo são pontinhos soltos e ficam. Edita a folha normalizada e grava os quadros limpos
na receita, para a reconversão repetir.

    python3 tools/sprites/limpar.py p02 LIGHT_JAB:3,4,5 HIT_AIR:1 --previa pasta
    python3 tools/sprites/limpar.py p02 LIGHT_JAB:3,4,5            # grava

Os quadros são os da animação (como a auditoria mostra). Depois rode build_characters.py --write.
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

MIN_BLOB = 120      # px: mancha de cor estranha menor que isso é borda/luz do corpo, fica
PALETTE_SHARE = 1e-4


def hsv_bins(rgba):
    h = np.array(Image.fromarray(np.ascontiguousarray(rgba[..., :3])).convert('HSV')).astype(int)
    return (h[..., 0] * 24 // 256) * 256 + (h[..., 1] * 16 // 256) * 16 + h[..., 2] * 16 // 256


def palette(pack, clips):
    """Cores do personagem em HSV fino, de clipes sem efeito."""
    count = np.zeros(24 * 256)
    for state in ('IDLE', 'WALK_FORWARD', 'CROUCH', 'HIT_STAND', 'DEFENSE_STAND'):
        anim = pack['animations'][state]
        sheet = A.Sheet(clips[anim['atlas']])
        for f in anim['frames']:
            cell = sheet.frame(f)
            count += np.bincount(hsv_bins(cell)[cell[..., 3] > 128], minlength=24 * 256)
    return count >= count.sum() * PALETTE_SHARE


def clean_cell(rgba, known):
    import cv2
    a = rgba[..., 3] > 128
    foreign = (a & ~known[hsv_bins(rgba)]).astype(np.uint8)
    n, lab, stats, _ = cv2.connectedComponentsWithStats(foreign, connectivity=8)
    erase = np.isin(lab, [i for i in range(1, n) if stats[i, cv2.CC_STAT_AREA] >= MIN_BLOB])
    # Contorno da mancha: borda meio transparente ou de cor estranha em volta dela.
    ring = cv2.dilate(erase.astype(np.uint8), np.ones((7, 7), np.uint8)) > 0
    erase |= ring & ((rgba[..., 3] < 250) | (foreign > 0))
    out = rgba.copy()
    out[erase] = 0
    # Sobras soltas: fica só o maior pedaço (o corpo).
    n, lab, stats, _ = cv2.connectedComponentsWithStats((out[..., 3] > 0).astype(np.uint8), connectivity=8)
    if n > 2:
        keep = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
        out[(lab != keep) & (lab > 0)] = 0
    return out, int(erase.sum())


def clean_atlas(cid, atlas, cells, pack=None, clips=None, preview=None):
    """Limpa células de um atlas; devolve [(antes, depois)]. Com preview não grava."""
    if pack is None:
        pack, clips = A.load_pack(cid)
    known = palette(pack, clips)
    cfg = clips[atlas]
    sheet = A.Sheet(cfg)
    img = np.array(Image.open(A.ROOT / cfg['source']).convert('RGBA'))
    shots = []
    for c in cells:
        y, x = c // sheet.cols * sheet.h, c % sheet.cols * sheet.w
        before = img[y:y + sheet.h, x:x + sheet.w].copy()
        after, n = clean_cell(before, known)
        print(f'{cid} {atlas} célula {c}: {n} px apagados')
        img[y:y + sheet.h, x:x + sheet.w] = after
        shots.append((before, after))
    if not preview:
        Image.fromarray(img).save(A.ROOT / cfg['source'])
        A.Sheet.cache.pop(cfg['source'], None)
    return shots


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('clipes', nargs='+', help='ESTADO:q1,q2 (quadros da animação)')
    p.add_argument('--previa', help='pasta: grava antes/depois e não mexe na folha')
    args = p.parse_args()
    pack, clips = A.load_pack(args.personagem)
    for item in args.clipes:
        state, frames = item.split(':')
        anim = pack['animations'][state]
        cells = sorted({anim['frames'][int(i)] for i in frames.split(',')})
        shots = clean_atlas(args.personagem, anim['atlas'], cells, pack, clips, args.previa)
        if args.previa:
            out = Path(args.previa)
            out.mkdir(parents=True, exist_ok=True)
            h, w = shots[0][0].shape[:2]
            board = Image.new('RGBA', (w * len(shots), h * 2), (60, 60, 80, 255))
            for k, (b, a) in enumerate(shots):
                board.alpha_composite(Image.fromarray(b), (k * w, 0))
                board.alpha_composite(Image.fromarray(a), (k * w, h))
            board.convert('RGB').save(out / f'{args.personagem}_{state}.png')
            continue
        path = clip_path(anim['atlas'])
        data = json.loads(path.read_text(encoding='utf-8'))
        target = data['video'] if 'video' in data else data
        target['limpar_quadros'] = sorted(set(target.get('limpar_quadros', [])) | set(cells))
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
