#!/usr/bin/env python3
"""Monta um cenário a partir de uma arte de conceito pintada (a tela inteira, sem HUD).

O conceito é uma pintura só, em perspectiva. Para o jogo ficar igual a ele na câmera do
começo da luta, a pintura vira duas peças:

* ``fundo_conceito.jpg``: a parte de cima (céu, montanhas, arquibancada), uma camada plana
  na profundidade da arquibancada, com a base encostada no fim do chão;
* ``chao_conceito.jpg``: o chão "desentortado". Cada pixel da textura do piso (vista de
  cima, como o motor espera) é levado pela mesma câmera do jogo até a tela e copiado do
  conceito. O jogo projeta de volta e o chão aparece como na pintura, com o lótus, os
  riscos de luz e as sombras pintadas; e continua em perspectiva quando a câmera anda.
  Onde a câmera vê além da pintura (cantos), o conceito é espelhado na borda
  (dos lados e embaixo, para a câmera afastada).

As contas são as do StageCamera (stage-core) na câmera de repouso do jogo: zoom 1,24,
chão dos lutadores na linha 552 da tela de 1280x720, horizonte em ``horizonte * 720``.

    python3 tools/stage/cenario_do_conceito.py conceito_sem_hud.png \\
        android/app/src/main/assets/stages/arena_central --horizonte 0.47 --base 0.555

Imprime os números para o stage.json (camada "fundo" e "chao").
"""
import argparse
import os

import numpy as np
from PIL import Image

VW, VH = 1280, 720
GROUND_Y = 565.0          # Arena.GROUND_Y
REST_ZOOM = 1.24          # CameraRig.CAMERA_REST_ZOOM
GROUND_SCREEN_Y = 552.0   # CameraRig.CAMERA_GROUND_SCREEN_Y
BASE_DISTANCE = 1000.0    # stage.json camera.distancia
REST_CENTER_X = 1300.0    # (PLAYER_START_X + OPPONENT_START_X) / 2: o meio do mundo


def camera(horizonte):
    horizon = horizonte * VH
    eye_y = GROUND_Y - (GROUND_SCREEN_Y - horizon) / REST_ZOOM
    distance = BASE_DISTANCE / REST_ZOOM
    return horizon, eye_y, distance


def scale(z, distance):
    return BASE_DISTANCE / np.maximum(1.0, distance + z)


def chao(src, janela, horizonte, base_y, perto, unidade, largura_mundo):
    """O chão "desentortado" (textura vista de cima) e a profundidade do fim dele.

    ``src``: a pintura (array float RGB). ``janela`` = (escala, x0, y0): o pixel (sx, sy) da tela
    de repouso (1280x720) fica em (x0 + escala*sx, y0 + escala*sy) na pintura; para um conceito
    do tamanho da tela é (1, 0, 0). ``base_y``: a linha da tela onde o chão encontra o fundo.
    Fora da pintura (dos lados e embaixo) o chão continua espelhado na borda.
    """
    h_img, w_img = src.shape[:2]
    escala, x0, y0 = janela
    horizon, eye_y, distance = camera(horizonte)
    eye_h = GROUND_Y - eye_y
    longe = BASE_DISTANCE * eye_h / (base_y - horizon) - distance
    rows = int(round((longe - perto) / unidade))
    cols = int(round(largura_mundo / unidade))
    # O motor estica a largura pela proporção da imagem: largura = (longe-perto)*cols/rows.
    largura = (longe - perto) * cols / rows
    v = (np.arange(rows) + 0.5) / rows
    u = (np.arange(cols) + 0.5) / cols
    z = longe - v * (longe - perto)                       # topo da imagem = longe
    xw = REST_CENTER_X - largura / 2 + u * largura
    s = scale(z, distance)
    sy = horizon + eye_h * s                              # linha da tela de cada fileira
    sx = VW / 2 + (xw[None, :] - REST_CENTER_X) * s[:, None]
    sy = np.broadcast_to(np.maximum(sy, base_y)[:, None], sx.shape)
    # Na pintura.
    px = x0 + escala * sx
    py = y0 + escala * sy

    def espelha(a, n):
        p = 2 * (n - 1)
        a = np.mod(a, p)
        return np.where(a > n - 1, p - a, a)

    px = espelha(px, w_img)
    py = np.where(py > h_img - 1, 2 * (h_img - 1) - py, py)
    py = np.clip(py, y0 + escala * base_y, h_img - 1)
    xi = np.floor(px).astype(int); yi = np.floor(py).astype(int)
    x1 = np.clip(xi + 1, 0, w_img - 1); y1 = np.clip(yi + 1, 0, h_img - 1)
    fx = (px - xi)[..., None]; fy = (py - yi)[..., None]
    out = (src[yi, xi] * (1 - fx) * (1 - fy) + src[yi, x1] * fx * (1 - fy)
           + src[y1, xi] * (1 - fx) * fy + src[y1, x1] * fx * fy)
    return np.clip(out, 0, 255).astype(np.uint8), longe, (cols, rows)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('conceito')
    ap.add_argument('saida')
    ap.add_argument('--horizonte', type=float, default=0.47)
    ap.add_argument('--base', type=float, required=True,
                    help='onde o chão encontra a arquibancada no conceito (fração da altura)')
    ap.add_argument('--perto', type=float, default=-700.0)
    ap.add_argument('--unidade', type=float, default=2.0, help='unidades do mundo por texel do chão')
    ap.add_argument('--largura', type=float, default=6000.0, help='largura do chão no mundo')
    args = ap.parse_args()

    img = Image.open(args.conceito).convert('RGB').resize((VW, VH), Image.LANCZOS)
    src = np.asarray(img).astype(np.float32)
    base_y = args.base * VH
    tex, longe, (cols, rows) = chao(src, (1.0, 0.0, 0.0), args.horizonte, base_y, args.perto,
                                    args.unidade, args.largura)
    perto = args.perto
    # Fundo: do topo até a base, na profundidade "longe" (a do fim do chão).
    img.crop((0, 0, VW, int(round(base_y)))).save(os.path.join(args.saida, 'fundo_conceito.jpg'),
                                                  quality=90, optimize=True)
    Image.fromarray(tex).save(os.path.join(args.saida, 'chao_conceito.jpg'), quality=88, optimize=True)

    print(f'horizonte {args.horizonte}: chao perto {perto:.0f} longe {longe:.0f} ({cols}x{rows} texels), centroX {REST_CENTER_X:.0f}')
    print(f'camada fundo: profundidade {longe:.0f}, topo 0, base {args.base}, x 0.5 (na câmera de repouso)')


if __name__ == '__main__':
    main()
