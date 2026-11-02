#!/usr/bin/env python3
"""Troca a cor de um efeito de vídeo pronto (assets/efeitos/<nome>/folha.webp) sem refazer o vídeo.

Só os pixels na faixa de matiz pedida, com cor e brilho bastante (a aura, os raios, o dragão), mudam
de cor; o personagem dentro do efeito (pele, roupa, colete escuro) fica como está. A cor nova vem de
um degradê pelo brilho de cada pixel (o escuro vira o tom fundo, o médio o cheio, o claro quase
branco), então a profundidade do efeito continua. Rodar de novo na folha já trocada não muda nada
(o azul já não está lá); para outra cor, refaça a folha do vídeo (efeito_video.py) antes.

    python3 tools/sprites/recolorir_efeito.py shinkai_player_base --para 46

    --de A-B    matiz de origem em graus (ciano, azul e o violeta e rosa do brilho: 150–355)
    --para H    matiz novo em graus (amarelo dourado: ~46)
    --sat S     saturação mínima para mudar (0–1, padrão 0.10)
    --val V     brilho mínimo para mudar (0–1, padrão 0.32)
    --brilho B  multiplica o brilho antes do degradê (padrão 1.25)
    --saida F   grava a folha em outro arquivo (prévia), sem trocar a do jogo
"""
import argparse
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
EFEITOS = ROOT / 'android/app/src/main/assets/efeitos'


def rgb_hsv(rgb):
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    mx, mn = rgb.max(axis=-1), rgb.min(axis=-1)
    d = mx - mn
    h = np.zeros_like(mx)
    m = d > 1e-6
    rm = m & (mx == r)
    gm = m & (mx == g) & ~rm
    bm = m & ~rm & ~gm
    h[rm] = ((g - b)[rm] / d[rm]) % 6
    h[gm] = (b - r)[gm] / d[gm] + 2
    h[bm] = (r - g)[bm] / d[bm] + 4
    h = h * 60.0
    s = np.where(mx > 1e-6, d / np.maximum(mx, 1e-6), 0)
    return h, s, mx


def hsv_rgb(h, s, v):
    c = v * s
    hp = (h % 360) / 60.0
    x = c * (1 - np.abs(hp % 2 - 1))
    z = np.zeros_like(h)
    conds = [(hp < 1), (hp < 2), (hp < 3), (hp < 4), (hp < 5), (hp >= 5)]
    rs = [c, x, z, z, x, c]
    gs = [x, c, c, x, z, z]
    bs = [z, z, x, c, c, x]
    r = np.select(conds, rs)
    g = np.select(conds, gs)
    b = np.select(conds, bs)
    m = v - c
    return np.stack([r + m, g + m, b + m], axis=-1)


def degrade(lum, matiz):
    """A cor do matiz pedido pelo brilho: escuro, cheio e claro (perto do branco no fim)."""
    pontos = [0.0, 0.22, 0.5, 0.78, 1.0]
    escuro = hsv_rgb(np.array([matiz - 18.0]), np.array([1.0]), np.array([0.32]))[0]
    fundo = hsv_rgb(np.array([matiz - 10.0]), np.array([0.98]), np.array([0.72]))[0]
    cheio = hsv_rgb(np.array([matiz]), np.array([0.92]), np.array([1.0]))[0]
    claro = hsv_rgb(np.array([matiz + 6.0]), np.array([0.45]), np.array([1.0]))[0]
    cores = [np.zeros(3), escuro, fundo, cheio, claro]
    cores[0] = escuro * 0.3
    cores = [cores[0], escuro, fundo, cheio, claro]
    out = np.zeros(lum.shape + (3,))
    for c in range(3):
        out[..., c] = np.interp(lum, pontos, [k[c] for k in cores])
    return out


def recolorir(folha, de, para, sat_min, val_min, brilho=1.25):
    a = np.array(folha.convert('RGBA')).astype(np.float32) / 255.0
    h, s, v = rgb_hsv(a[..., :3])
    lo, hi = de
    dentro = (h >= lo) & (h <= hi) & (s >= sat_min) & (v >= val_min) & (a[..., 3] > 0.01)
    # Inteiro dentro da faixa (misturar azul com amarelo dá verde); só o escuro entra aos poucos
    # (o colete e o cabelo, escuros, ficam), e a beirada da matiz tem 12 graus de suavidade.
    borda = np.minimum(h - lo, hi - h)
    peso = np.clip(borda / 12.0, 0, 1) * np.clip((v - val_min) / 0.12, 0, 1)
    peso = np.where(dentro, peso, 0)[..., None]
    # Mapa de degradê pelo brilho de cada pixel (o azul escuro vira laranja fundo, o médio
    # dourado e o claro amarelo quase branco): a profundidade do efeito continua.
    rgb = a[..., :3]
    lum = (0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2])
    lum = np.clip(lum * brilho, 0, 1)
    novo = degrade(lum, para)
    # Pouca cor no original (o branco do brilho): fica mais perto do branco.
    sat = np.clip(s / 0.5, 0, 1)[..., None]
    branco = np.repeat(lum[..., None], 3, axis=-1)
    novo = branco * (1 - sat) + novo * sat
    a[..., :3] = rgb * (1 - peso) + novo * peso
    return Image.fromarray(np.clip(a * 255 + 0.5, 0, 255).astype(np.uint8), 'RGBA'), int(dentro.sum())


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('nome')
    p.add_argument('--de', default='150-355')
    p.add_argument('--brilho', type=float, default=1.25, help='multiplica o brilho antes do degradê')
    p.add_argument('--para', type=float, default=46.0)
    p.add_argument('--sat', type=float, default=0.10)
    p.add_argument('--val', type=float, default=0.32)
    p.add_argument('--saida', help='outro arquivo para a folha (prévia), no lugar de sobrescrever')
    a = p.parse_args()
    pasta = EFEITOS / a.nome
    folha = Image.open(pasta / 'folha.webp')
    lo, hi = (float(x) for x in a.de.split('-'))
    nova, n = recolorir(folha, (lo, hi), a.para, a.sat, a.val, a.brilho)
    destino = Path(a.saida) if a.saida else pasta / 'folha.webp'
    nova.save(destino, lossless=True, method=6)
    print(f'{n} pixels recoloridos em {destino}')


if __name__ == '__main__':
    main()
