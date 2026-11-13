#!/usr/bin/env python3
"""Monta a moldura da barra com raios (assets/ui/hud/hud-raios.png) a partir da arte do dono.

A arte vem pronta (1672x941, fundo transparente, com rostos, barras cheias e textos pintados).
Aqui se abrem buracos onde o jogo desenha ao vivo (rostos, vida, TAG, SHINKAI e os quadradinhos
do Super) e se apagam os textos que mudam (o número do Super, ∞/TREINO, ◀ MENU e TAG). Fica só
a faixa de cima (linhas 76 a 324). As mesmas medidas estão no HudRenderer (RAIOS_P1/RAIOS_P2):
se a arte mudar de lugar, mude os dois.

    pip install opencv-python-headless numpy pillow
    python3 tools/hud/montar_barra_raios.py arte-do-dono.png [saida.png] [--previa previa.png]
"""
import os
import sys

import cv2
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SAIDA = os.path.join(ROOT, "android", "app", "src", "main", "assets", "ui", "hud", "hud-raios.png")
TOPO, BASE = 76, 324

# Rostos e parceiros: contorno medido à mão por dentro da linha da moldura.
ROSTOS = [[(96, 127), (184, 127), (220, 163), (137, 254), (37, 182)],
          [(1575, 127), (1487, 127), (1451, 163), (1535, 254), (1631, 181)]]
PARCEIROS = [[(157, 262), (220, 263), (249, 297), (147, 297)],
             [(1449, 262), (1509, 262), (1524, 298), (1419, 298)]]
# SHINKAI: o miolo tem um brilho branco que corta a cor ao meio; vai medido à mão.
SHINKAI = [[(293, 225), (450, 225), (442, 244), (278, 244)],
           [(1236, 225), (1393, 225), (1406, 244), (1239, 244)]]
# Vida, TAG e quadradinhos do Super: achados pela cor a partir de um ponto (semente, caixa).
POR_COR = [
    ("vida", (400, 185), (190, 165, 740, 210)), ("vida", (500, 280), (310, 266, 660, 294)),
    ("vida", (1200, 185), (930, 165, 1480, 210)), ("vida", (1200, 280), (1010, 266, 1360, 294)),
] + [("azul", (int(594 + i * 29.8), 233), (int(594 + i * 29.8) - 16, 220, int(594 + i * 29.8) + 16, 248)) for i in range(5)] \
  + [("vermelho", (int(1084 + i * 29.8), 233), (int(1084 + i * 29.8) - 16, 220, int(1084 + i * 29.8) + 16, 248)) for i in range(5)]
# Textos que o jogo escreve ao vivo: número do Super (2), ∞/TREINO, ◀ MENU e TAG (2).
TEXTOS = [(543, 218, 571, 249), (1029, 218, 1058, 249), (780, 132, 890, 226),
          (770, 262, 905, 300), (252, 264, 314, 293), (1362, 264, 1414, 293)]


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    previa = sys.argv[sys.argv.index("--previa") + 1] if "--previa" in sys.argv else None
    if previa in args:
        args.remove(previa)
    if not args:
        sys.exit(__doc__)
    A = np.array(Image.open(args[0]).convert("RGBA"))
    saida = args[1] if len(args) > 1 else SAIDA
    H, W = A.shape[:2]
    assert (W, H) == (1672, 941), f"arte com {W}x{H}: as medidas são da de 1672x941"
    R, G, B = (A[..., i].astype(int) for i in range(3))
    mx, mn = A[..., :3].max(2).astype(int), A[..., :3].min(2).astype(int)
    cores = {
        "vida": (mx - mn > 80) & (mx > 140) & (B < np.maximum(R, G) - 40),
        "azul": (B > 110) & (R < 60),
        "vermelho": (R > 110) & (G < 70) & (B < 70),
    }

    buraco = np.zeros((H, W), np.uint8)
    for cor, (sx, sy), (x0, y0, x1, y1) in POR_COR:
        m = np.zeros((H, W), np.uint8)
        m[y0:y1, x0:x1] = cores[cor][y0:y1, x0:x1]
        _, lab = cv2.connectedComponents(m, connectivity=8)
        assert lab[sy, sx], f"{cor} {sx},{sy}: o ponto não tem a cor esperada"
        hull = cv2.convexHull(cv2.findNonZero((lab == lab[sy, sx]).astype(np.uint8)))
        cv2.fillPoly(buraco, [hull], 255)
    for poly in SHINKAI:
        cv2.fillPoly(buraco, [np.array(poly, np.int32)], 255)
    buraco = cv2.dilate(buraco, np.ones((3, 3), np.uint8))  # 1 px: a borda antisserrilhada da cor
    for poly in ROSTOS + PARCEIROS:
        cv2.fillPoly(buraco, [np.array(poly, np.int32)], 255)

    claro = A[..., :3].min(2) > 150
    texto = np.zeros((H, W), np.uint8)
    for x0, y0, x1, y1 in TEXTOS:
        texto[y0:y1, x0:x1] = claro[y0:y1, x0:x1] * 255
    texto = cv2.dilate(texto, np.ones((5, 5), np.uint8))
    A[..., :3] = cv2.inpaint(np.ascontiguousarray(A[..., :3]), texto, 5, cv2.INPAINT_TELEA)
    A[..., 3][texto > 0] = np.maximum(A[..., 3][texto > 0], 240)
    A[..., 3][buraco > 0] = 0
    A[..., :3][buraco > 0] = 0

    Image.fromarray(A[TOPO:BASE]).save(saida, optimize=True)
    print("salvo", saida, W, BASE - TOPO)
    if previa:
        p = Image.new("RGBA", (W, BASE - TOPO), (255, 0, 255, 255))
        p.alpha_composite(Image.fromarray(A[TOPO:BASE]))
        p.save(previa)


if __name__ == "__main__":
    main()
