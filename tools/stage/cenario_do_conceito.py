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
REST_CENTER_X = 700.0     # (PLAYER_START_X + OPPONENT_START_X) / 2


def camera(horizonte):
    horizon = horizonte * VH
    eye_y = GROUND_Y - (GROUND_SCREEN_Y - horizon) / REST_ZOOM
    distance = BASE_DISTANCE / REST_ZOOM
    return horizon, eye_y, distance


def scale(z, distance):
    return BASE_DISTANCE / np.maximum(1.0, distance + z)


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
    horizon, eye_y, distance = camera(args.horizonte)
    eye_h = GROUND_Y - eye_y

    # Profundidade do chão na linha da base (onde a arquibancada encosta).
    base_y = args.base * VH
    longe = BASE_DISTANCE * eye_h / (base_y - horizon) - distance

    # Fundo: do topo até a base, na profundidade "longe" (a do fim do chão).
    fundo = img.crop((0, 0, VW, int(round(base_y))))
    fundo.save(os.path.join(args.saida, 'fundo_conceito.jpg'), quality=90, optimize=True)

    # Chão: textura vista de cima, texel quadrado de "unidade" no mundo.
    perto = args.perto
    rows = int(round((longe - perto) / args.unidade))
    cols = int(round(args.largura / args.unidade))
    # O motor estica a largura pela proporção da imagem: largura = (longe-perto)*cols/rows.
    largura = (longe - perto) * cols / rows
    v = (np.arange(rows) + 0.5) / rows
    u = (np.arange(cols) + 0.5) / cols
    z = longe - v * (longe - perto)                       # topo da imagem = longe
    xw = REST_CENTER_X - largura / 2 + u * largura
    s = scale(z, distance)
    sy = horizon + eye_h * s                              # linha da tela de cada fileira
    sx = VW / 2 + (xw[None, :] - REST_CENTER_X) * s[:, None]
    sy = np.broadcast_to(sy[:, None], sx.shape)

    def espelha(a, n):
        # Reflete nas bordas (o conceito repetido em espelho fora da pintura).
        p = 2 * (n - 1)
        a = np.mod(a, p)
        return np.where(a > n - 1, p - a, a)

    sx = espelha(sx, VW)
    # Abaixo da pintura (câmera afastada) o chão continua espelhado na borda de baixo.
    sy = np.where(sy > VH - 1, 2 * (VH - 1) - sy, sy)
    sy = np.clip(sy, base_y, VH - 1)
    # Amostragem bilinear.
    x0 = np.floor(sx).astype(int); y0 = np.floor(sy).astype(int)
    x1 = np.clip(x0 + 1, 0, VW - 1); y1 = np.clip(y0 + 1, 0, VH - 1)
    fx = (sx - x0)[..., None]; fy = (sy - y0)[..., None]
    out = (src[y0, x0] * (1 - fx) * (1 - fy) + src[y0, x1] * fx * (1 - fy)
           + src[y1, x0] * (1 - fx) * fy + src[y1, x1] * fx * fy)
    Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).save(
        os.path.join(args.saida, 'chao_conceito.jpg'), quality=88, optimize=True)

    print(f'horizonte {args.horizonte}: chao perto {perto:.0f} longe {longe:.0f} ({cols}x{rows} texels), centroX {REST_CENTER_X:.0f}')
    print(f'camada fundo: profundidade {longe:.0f}, topo 0, base {args.base}, x 0.5 (na câmera de repouso)')


if __name__ == '__main__':
    main()
