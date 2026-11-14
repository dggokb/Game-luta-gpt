#!/usr/bin/env python3
"""Mais spray nas defesas de brilho do dono (pedido de 14/11), sem desenhar nada novo: em cada
quadro, os riscos soltos do spray (os pedaços pequenos, fora do arco) são copiados e girados em
volta do clarão, um pouco maiores ou menores e mais fracos, e entram por baixo do desenho original.
Girar em volta do clarão mantém cada risco apontando para fora, então o movimento quadro a quadro
continua o do desenho.

    python3 art/vfx/defesa_brilho_v01/mais_spray.py      (escreve spray_1, spray_2, spray_3)
"""
import numpy as np
from pathlib import Path
from PIL import Image
from scipy import ndimage as nd

AQUI = Path(__file__).resolve().parent
CLARAO = {1: (254, 247), 2: (257, 250), 3: (256, 252)}
# (giro em graus, escala, opacidade) de cada cópia do spray.
COPIAS = [(17, 0.95, 1.0), (-15, 1.1, 0.95), (34, 0.85, 0.85), (-32, 1.02, 0.85)]
PEDACO_MAX = 1500  # px: acima disso é o arco, não spray


def spray(rgba, primeiro, centro):
    """Os riscos soltos na frente do clarão (o lado para onde o spray voa); os pedaços do arco, que
    ficam na altura do clarão ou atrás dele, não entram (girados viravam anéis)."""
    a = rgba[..., 3]
    lab, n = nd.label(a > 40 / 255)
    if not n:
        return np.zeros_like(rgba)
    area = nd.sum(np.ones_like(a), lab, range(1, n + 1))
    manter = np.zeros(n + 1, bool)
    cy_, cx_ = np.array(nd.center_of_mass(a, lab, range(1, n + 1))).T
    for i, s in enumerate(area, 1):
        manter[i] = s < PEDACO_MAX and cx_[i - 1] > centro[0] + 25
    if primeiro:
        manter[int(np.argmax(area)) + 1] = False
    m = nd.binary_dilation(manter[lab], iterations=3)
    out = rgba.copy()
    out[..., 3] *= m
    return out


def girar(rgba, centro, graus, escala):
    """Gira e escala em volta do centro (pré-multiplicado, para a borda não escurecer)."""
    pre = rgba.copy()
    pre[..., :3] *= pre[..., 3:4]
    t = np.radians(graus)
    cx, cy = centro
    # Mapa inverso: para cada pixel de saída, de onde ele vem na entrada.
    c, s = np.cos(t) / escala, np.sin(t) / escala
    mat = np.array([[c, s], [-s, c]])          # em (y, x)
    off = np.array([cy, cx]) - mat @ np.array([cy, cx])
    out = np.stack([nd.affine_transform(pre[..., k], mat, offset=off, order=1) for k in range(4)], -1)
    return out


def main():
    for k, centro in CLARAO.items():
        src, dst = AQUI / f'defesa_{k}', AQUI / f'spray_{k}'
        dst.mkdir(exist_ok=True)
        for q in range(1, 7):
            im = np.asarray(Image.open(src / f'q_{q:02d}.png')).astype(np.float32) / 255.0
            if q == 1:
                Image.fromarray((im * 255).astype(np.uint8), 'RGBA').save(dst / f'q_{q:02d}.png')
                continue
            base = spray(im, q <= 4, centro)
            # Fundo: as cópias somadas em pré-multiplicado (luz sobre luz), depois o original por cima.
            fundo = np.zeros_like(im)
            for graus, escala, op in COPIAS:
                g = girar(base, centro, graus, escala) * op
                fundo = fundo + g * (1 - fundo[..., 3:4])
            orig = im.copy()
            orig[..., :3] *= orig[..., 3:4]
            out = orig + fundo * (1 - orig[..., 3:4])
            cor = np.where(out[..., 3:4] > 1e-4, out[..., :3] / np.maximum(out[..., 3:4], 1e-4), 0)
            res = np.dstack([np.clip(cor, 0, 1), np.clip(out[..., 3], 0, 1)])
            Image.fromarray((res * 255).round().astype(np.uint8), 'RGBA').save(dst / f'q_{q:02d}.png')
        print('spray', k)


if __name__ == '__main__':
    main()
