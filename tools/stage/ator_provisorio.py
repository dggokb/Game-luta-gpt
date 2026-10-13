#!/usr/bin/env python3
"""Folha provisória da animação parada de um ator de fundo, a partir do concept.

    python tools/stage/ator_provisorio.py art/concepts/dafu.png --caixa 0 0 310 700 \
        --saida android/app/src/main/assets/stages/dojo_shinshinkan/atores/dafu

Recorta a figura de corpo inteiro que está dentro de --caixa (tira o fundo cinza claro do
model sheet), e grava parado.png: alguns quadros com uma respiração leve (o corpo cresce um
pouco e volta, com os pés parados). Serve até o vídeo da animação parada chegar; aí o
video_para_ator.py troca a folha. O ator.json não é mexido.
"""

import argparse
import math
import sys
from pathlib import Path

try:
    import cv2
    import numpy as np
    from PIL import Image
except ImportError:
    sys.exit("Instale: pip install pillow numpy opencv-python-headless")

QUADROS = 8
COLUNAS = 4
FOLGA = 12


def recortar(caminho, caixa):
    img = np.asarray(Image.open(caminho).convert("RGB")).astype(np.int16)
    x0, y0, x1, y1 = caixa
    parte = img[y0:y1, x0:x1]
    borda = np.concatenate([parte[0], parte[-1], parte[:, 0], parte[:, -1]])
    fundo = np.median(borda, axis=0)
    dist = np.abs(parte - fundo).sum(2)
    cinza = (parte.max(2) - parte.min(2)) < 14
    # Fundo e a sombra clara no chão: cinza, perto da cor do fundo e ligado à borda.
    parecido = ((dist < 30) | (cinza & (dist < 70))).astype(np.uint8)
    h, w = parecido.shape
    mascara = np.zeros((h + 2, w + 2), np.uint8)
    cheio = parecido.copy()
    for x in range(w):
        for y in (0, h - 1):
            if cheio[y, x] == 1:
                cv2.floodFill(cheio, mascara, (x, y), 2)
    for y in range(h):
        for x in (0, w - 1):
            if cheio[y, x] == 1:
                cv2.floodFill(cheio, mascara, (x, y), 2)
    figura = (cheio != 2).astype(np.uint8)
    # Só a maior peça (a figura); pedaços de outras poses que entraram na caixa saem.
    n, rotulos, stats, _ = cv2.connectedComponentsWithStats(figura, 8)
    maior = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    figura = (rotulos == maior).astype(np.uint8)
    figura = cv2.morphologyEx(figura, cv2.MORPH_CLOSE, np.ones((3, 3), np.uint8))
    # A sombra do model sheet embaixo dos pés: cinza, um pouco mais escura que o fundo. O
    # branco dos tênis é mais claro que o fundo e fica.
    ys, xs = np.nonzero(figura)
    pe = ys.max() - int((ys.max() - ys.min()) * 0.10)
    luz = parte.mean(2)
    sombra = cinza & (luz < fundo.mean() + 8) & (luz > fundo.mean() - 70)
    sombra[:pe] = False
    figura[sombra] = 0
    n, rotulos, stats, _ = cv2.connectedComponentsWithStats(figura, 8)
    maior = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    figura = (rotulos == maior).astype(np.uint8)
    alfa = cv2.GaussianBlur(figura.astype(np.float32), (3, 3), 0)
    rgba = np.dstack([parte.astype(np.uint8), (alfa * 255).astype(np.uint8)])
    ys, xs = np.nonzero(figura)
    return Image.fromarray(rgba[ys.min():ys.max() + 1, xs.min():xs.max() + 1])


def folha_parada(figura, altura):
    escala = (altura - 2 * FOLGA) / figura.height
    base = figura.resize((round(figura.width * escala), round(figura.height * escala)), Image.LANCZOS)
    cw = base.width + 2 * FOLGA
    ch = altura
    folha = Image.new("RGBA", (cw * COLUNAS, ch * math.ceil(QUADROS / COLUNAS)), (0, 0, 0, 0))
    for i in range(QUADROS):
        k = math.sin(i / QUADROS * 2 * math.pi)
        # Respiração: o tronco sobe um pouco e alarga um tiquinho; os pés ficam no lugar.
        sx, sy = 1 + 0.004 * k, 1 + 0.010 * k
        q = base.resize((round(base.width * sx), round(base.height * sy)), Image.LANCZOS)
        cx = (i % COLUNAS) * cw + (cw - q.width) // 2
        cy = (i // COLUNAS) * ch + ch - FOLGA - q.height
        folha.alpha_composite(q, (cx, cy))
    return folha, cw, ch


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("concept", type=Path)
    parser.add_argument("--caixa", type=int, nargs=4, required=True, metavar=("X0", "Y0", "X1", "Y1"))
    parser.add_argument("--saida", type=Path, required=True)
    parser.add_argument("--altura", type=int, default=480, help="altura da célula em px")
    args = parser.parse_args()
    args.saida.mkdir(parents=True, exist_ok=True)
    figura = recortar(args.concept, args.caixa)
    folha, cw, ch = folha_parada(figura, args.altura)
    folha.save(args.saida / "parado.png")
    print(f"{args.saida / 'parado.png'}: {QUADROS} quadros, {COLUNAS} colunas, célula {cw}x{ch}")


if __name__ == "__main__":
    main()
