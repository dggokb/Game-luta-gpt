#!/usr/bin/env python3
"""Fixa o pé de apoio de um golpe no lugar: tira o deslize do vídeo (o pé andando aos poucos)
e o pulo ao trocar de animação (golpe que começa ou termina em guarda com o pé fora do lugar
do idle). Edita a folha normalizada do clipe e marca a receita, para a reconversão repetir.

    python3 tools/sprites/ancorar.py p04 HEAVY_STRAIGHT SPECIAL_ENERGY
    python3 tools/sprites/ancorar.py player_base SPECIAL_S3 --ver   # só mostra os deslocamentos

Troca de pé de apoio (giro, rasteira, passo de verdade: mais de 15 px num quadro) é mantida.
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

MARGIN = 10  # px livres na borda da célula (o build exige 8)


def clip_path(atlas):
    for p in (A.ROOT / 'tools/sprites/clips').glob('*.json'):
        if Path(json.loads(p.read_text(encoding='utf-8'))['output']).stem == atlas:
            return p
    raise SystemExit(f'clipe do atlas {atlas} não encontrado')


def reference_feet(pack, clips):
    """Pé de trás e silhueta das poses de referência (guarda do idle, agachado)."""
    refs, idle0 = A.references(pack, clips)
    return [(name, m['back'], sil) for name, m, sil in refs], idle0['back']


def offsets(ms, refs):
    """Deslocamento horizontal (px da célula) de cada quadro do clipe."""
    n = len(ms)
    off = -np.cumsum(A.planted_steps(ms))

    def target(i):  # pé de trás da pose de referência, se o quadro for uma delas
        if ms[i]['back'] is None:
            return None
        sil = A.silhouette(ms[i])
        name, foot, score = max(((nm, f, A.iou(sil, s)) for nm, f, s in refs), key=lambda t: t[2])
        return foot if score >= A.POSE_MATCH and foot is not None else None

    start, end = target(0), target(n - 1)
    if start is not None:
        off += start - ms[0]['back']
    elif end is not None:
        off += end - (ms[-1]['back'] + off[-1])
    if end is not None and start is not None:
        miss = end - (ms[-1]['back'] + off[-1])
        if abs(miss) >= 3:  # sobra no fim (troca de pé no meio): corrige aos poucos na volta
            k = np.arange(n)
            first = n // 2
            off += miss * np.clip((k - first) / max(1, n - 1 - first), 0, 1)
    return np.round(off).astype(int)


def anchor(cid, atlas, show=False, pack=None, clips=None):
    """Fixa o pé de apoio de um atlas do personagem; devolve os deslocamentos."""
    if pack is None:
        pack, clips = A.load_pack(cid)
    refs, _ = reference_feet(pack, clips)
    cfg = clips[atlas]
    sheet = A.Sheet(cfg)
    ms = [A.measure(sheet.frame(i), sheet.rx, sheet.ry) for i in range(cfg['expectedFrames'])]
    off = offsets(ms, refs)
    if show or not off.any():
        return off
    # Célula alarga para o lado que precisar, para nada encostar na borda (margem do build).
    lefts = [int(np.nonzero(m['mask'].any(0))[0].min()) + d for m, d in zip(ms, off)]
    rights = [int(np.nonzero(m['mask'].any(0))[0].max()) + d for m, d in zip(ms, off)]
    pad_l = int(max(0, MARGIN - min(lefts)))
    pad_r = int(max(0, max(rights) - (sheet.w - 1 - MARGIN)))
    w = sheet.w + pad_l + pad_r
    img = Image.open(A.ROOT / cfg['source']).convert('RGBA')
    rows = (cfg['expectedFrames'] + sheet.cols - 1) // sheet.cols
    out = Image.new('RGBA', (w * sheet.cols, sheet.h * rows), (0, 0, 0, 0))
    for i, d in enumerate(off):
        x, y = i % sheet.cols * sheet.w, i // sheet.cols * sheet.h
        cell = img.crop((x, y, x + sheet.w, y + sheet.h))
        out.alpha_composite(cell, (i % sheet.cols * w + pad_l + d, y))
    out.save(A.ROOT / cfg['source'])
    if pad_l or pad_r:
        path = clip_path(atlas)
        data = json.loads(path.read_text(encoding='utf-8'))
        data['frameWidth'], data['rootX'] = w, sheet.rx + pad_l
        if 'video' in data:
            data['video']['largura'] = w
            rx, ry = data['video'].get('raiz', '128,238').split(',')
            data['video']['raiz'] = f'{int(rx) + pad_l},{ry}'
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    A.Sheet.cache.pop(cfg['source'], None)
    return off


def apply(cid, states, show=False):
    pack, clips = A.load_pack(cid)
    done = set()
    for state in states:
        atlas = pack['animations'][state]['atlas']
        if atlas in done:
            continue
        done.add(atlas)
        off = anchor(cid, atlas, show, pack, clips)
        print(f'{cid} {state} ({atlas}): {" ".join(f"{d:+d}" for d in off)}')
        if show or not off.any():
            continue
        path = clip_path(atlas)
        data = json.loads(path.read_text(encoding='utf-8'))
        if 'video' in data:
            data['video']['ancorar_pe'] = True
        else:
            data['ancoradoPe'] = 'pé de apoio fixado por tools/sprites/ancorar.py'
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('estados', nargs='+')
    p.add_argument('--ver', action='store_true', help='só mostra os deslocamentos')
    args = p.parse_args()
    apply(args.personagem, args.estados, args.ver)


if __name__ == '__main__':
    main()
