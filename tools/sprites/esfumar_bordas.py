#!/usr/bin/env python3
"""Esfuma as bordas do vídeo num clipe de golpe (o tornado, a lâmina de vento e a poeira dos
especiais do p14 passam da borda do quadro do vídeo e ficavam cortados em linha reta, "quadrados").

Só para clipes com fixar "video" (o quadro do vídeo fica no mesmo lugar da célula em todos os
quadros): a borda do vídeo dentro da célula é o ponto mais extremo que algum quadro alcança
daquele lado; dali para dentro o alfa sobe aos poucos numa faixa. Lados: L, R, T (o chão, B,
não esfuma). Vale para a folha do clipe (HD) e para a 1x de referência.

    python3 tools/sprites/esfumar_bordas.py p14_special_s2 R,T p14_special_s4 L,R@100

"@N" troca a largura da faixa (px do jogo; a poeira densa do S4 pede mais).
"""
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
FAIXA = 40   # px do jogo


def esfumar(path, cw, ch, cols, n, lados, faixa):
    img = Image.open(path).convert('RGBA')
    a = np.array(img)
    cells = [(i // cols * ch, i % cols * cw) for i in range(n)]
    ext = {'L': cw, 'R': -1, 'T': ch}
    for y0, x0 in cells:
        alpha = a[y0:y0 + ch, x0:x0 + cw, 3]
        xs = np.nonzero(alpha.max(0) > 8)[0]
        ys = np.nonzero(alpha.max(1) > 8)[0]
        if len(xs):
            ext['L'] = min(ext['L'], xs.min())
            ext['R'] = max(ext['R'], xs.max())
            ext['T'] = min(ext['T'], ys.min())
    kx = np.arange(cw, dtype=np.float32)
    ky = np.arange(ch, dtype=np.float32)
    ramp = np.ones((ch, cw), np.float32)
    if 'L' in lados:
        ramp *= np.clip((kx - ext['L']) / faixa, 0, 1)[None, :]
    if 'R' in lados:
        ramp *= np.clip((ext['R'] - kx) / faixa, 0, 1)[None, :]
    if 'T' in lados:
        ramp *= np.clip((ky - ext['T']) / faixa, 0, 1)[:, None]
    for y0, x0 in cells:
        cell = a[y0:y0 + ch, x0:x0 + cw, 3].astype(np.float32)
        a[y0:y0 + ch, x0:x0 + cw, 3] = (cell * ramp).astype(np.uint8)
    Image.fromarray(a, 'RGBA').save(path)
    return ext


def main(argv):
    if len(argv) < 2 or len(argv) % 2:
        raise SystemExit(__doc__)
    for clip_id, lados in zip(argv[::2], argv[1::2]):
        lados, _, larg = lados.partition('@')
        faixa = int(larg) if larg else FAIXA
        lados = set(lados.split(','))
        if not lados <= {'L', 'R', 'T'}:
            raise SystemExit(f'{clip_id}: lados L, R, T')
        clip = json.loads((HERE / 'clips' / f'{clip_id}.json').read_text(encoding='utf-8'))
        if clip.get('video', {}).get('fixar') != 'video':
            raise SystemExit(f'{clip_id}: só clipe com fixar "video"')
        d = int(clip.get('pixelScale', 1))
        n, cols = clip['expectedFrames'], clip['columns']
        ext = esfumar(ROOT / clip['source'], clip['frameWidth'], clip['frameHeight'], cols, n, lados, faixa * d)
        print(f'{clip_id}: {",".join(sorted(lados))} esfumados ({ext})')
        ref = clip.get('video', {}).get('referencia')
        if ref and d > 1:
            w, h = ref['celula']
            esfumar(ROOT / ref['folha'], w, h, ref['colunas'], n, lados, faixa)


if __name__ == '__main__':
    main(sys.argv[1:])
