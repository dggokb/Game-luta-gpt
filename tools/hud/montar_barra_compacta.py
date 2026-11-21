#!/usr/bin/env python3
"""Monta a barra compacta da luta (assets/ui/hud/hud-compacto.png) a partir da arte gerada no Codex
(art/hud/compacto/hud_compacto_dbfz.png, 1672x941, a escolhida pelo dono em 21/11: o time
empilhado no canto, como no Dragon Ball FighterZ). Depois de "os encaixes de imagem estão
errados", o Codex redesenhou só as molduras dos rostos, com o miolo em magenta puro
(hud_compacto_dbfz_molduras.png, a arte usada): os buracos dos rostos são o magenta, exatos, e o
script imprime os contornos para o HudRenderer.

Como no montar_barra_raios.py: abre buracos onde o jogo desenha ao vivo (rostos, vida, TAG,
SHINKAI e o miolo dos quadradinhos do Super, deixando o contorno de neon) e apaga os textos que
mudam (os nomes, ∞/TREINO e MENU). TAG, SHINKAI e SUPER ficam pintados. Fica só a faixa de cima
(linhas 0 a 132). As mesmas medidas estão no HudRenderer (COMPACTA_P1/COMPACTA_P2).

    python3 tools/hud/montar_barra_compacta.py [arte.png] [saida.png] [--previa previa.png]
"""
import os
import sys

import cv2
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ARTE = os.path.join(ROOT, "art", "hud", "compacto", "hud_compacto_dbfz_molduras.png")
SAIDA = os.path.join(ROOT, "android", "app", "src", "main", "assets", "ui", "hud", "hud-compacto.png")
TOPO, BASE = 0, 132

ROSTOS = [[(36, 57), (80, 27), (146, 27), (167, 46), (138, 75), (118, 97), (98, 92)],
          [(1504, 45), (1524, 27), (1596, 27), (1637, 57), (1570, 96), (1549, 90), (1543, 77)]]
PARCEIROS = [[(141, 77), (179, 77), (219, 109), (112, 109)],
             [(1491, 77), (1546, 77), (1560, 92), (1557, 110), (1452, 110)]]
# Vida e TAG: achados pela cor a partir de um ponto (semente, caixa).
POR_COR = [("vida", (400, 56), (150, 44, 745, 69)), ("vida", (1200, 56), (925, 44, 1525, 69)),
           ("vida", (500, 81), (245, 72, 765, 90)), ("vida", (1150, 81), (905, 72, 1430, 90)),
           ("azul", (400, 103), (306, 94, 484, 114)), ("vermelho", (1270, 103), (1192, 94, 1358, 114))]


def celula(x):
    return [(x + 9, 99), (x + 27, 99), (x + 20, 109), (x + 2, 109)]


CELULAS = [celula(569 + 27.6 * i) for i in range(5)] + [celula(1038 + 27.1 * i) for i in range(5)]
TEXTOS = [(192, 22, 292, 47), (1394, 22, 1478, 47), (798, 26, 876, 80), (802, 93, 872, 115)]


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    previa = sys.argv[sys.argv.index("--previa") + 1] if "--previa" in sys.argv else None
    if previa in args:
        args.remove(previa)
    A = np.array(Image.open(args[0] if args else ARTE).convert("RGBA"))
    saida = args[1] if len(args) > 1 else SAIDA
    H, W = A.shape[:2]
    assert (W, H) == (1672, 941), f"arte com {W}x{H}: as medidas são da de 1672x941"
    R, G, B = (A[..., i].astype(int) for i in range(3))
    mx, mn = A[..., :3].max(2).astype(int), A[..., :3].min(2).astype(int)
    cores = {"vida": (mx - mn > 80) & (mx > 140) & (B < np.maximum(R, G) - 40),
             "azul": (B > 150) & (R < 120), "vermelho": (R > 150) & (G < 90) & (B < 100)}
    buraco = np.zeros((H, W), np.uint8)
    for cor, (sx, sy), (x0, y0, x1, y1) in POR_COR:
        m = np.zeros((H, W), np.uint8)
        m[y0:y1, x0:x1] = cores[cor][y0:y1, x0:x1]
        _, lab = cv2.connectedComponents(m, connectivity=8)
        assert lab[sy, sx], f"{cor} {sx},{sy}: o ponto não tem a cor esperada"
        hull = cv2.convexHull(cv2.findNonZero((lab == lab[sy, sx]).astype(np.uint8)))
        cv2.fillPoly(buraco, [hull], 255)
    buraco = cv2.dilate(buraco, np.ones((3, 3), np.uint8))
    # Rostos: o miolo magenta da moldura, com 2 px a mais (a borda antisserrilhada do magenta).
    mag = ((R > 170) & (B > 170) & (G < 110) & (A[..., 3] > 100)).astype(np.uint8)
    mag[BASE:] = 0
    n, lab, st, _ = cv2.connectedComponentsWithStats(mag, 4)
    formas = sorted([i for i in range(1, n) if st[i][4] > 300], key=lambda i: -st[i][4])
    assert len(formas) == 4, f"{len(formas)} molduras magenta (esperava 4)"
    for i in formas:
        um = cv2.dilate((lab == i).astype(np.uint8), np.ones((5, 5), np.uint8))
        cs, _ = cv2.findContours(um, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        pol = cv2.approxPolyDP(max(cs, key=cv2.contourArea), 1.2, True).reshape(-1, 2)
        lado = "P1" if pol[:, 0].mean() < W / 2 else "P2"
        tipo = "rosto" if st[i][4] > 3000 else "parceiro"
        print(lado, tipo, "new float[]{" + ", ".join(f"{x}, {y}" for x, y in pol) + "}")
        cv2.fillPoly(buraco, [pol.astype(np.int32)], 255)
    for cor, (sx, sy), (x0, y0, x1, y1) in POR_COR:
        m = np.zeros((H, W), np.uint8)
        m[y0:y1, x0:x1] = cores[cor][y0:y1, x0:x1]
        _, lb = cv2.connectedComponents(m, connectivity=8)
        ys, xs = np.nonzero(lb == lb[sy, sx])
        print(cor, "caixa", xs.min(), ys.min(), xs.max() + 1, ys.max() + 1)
    for poly in CELULAS:
        cv2.fillPoly(buraco, [np.array(poly, np.int32)], 255)
    claro = A[..., :3].min(2) > 150
    texto = np.zeros((H, W), np.uint8)
    for x0, y0, x1, y1 in TEXTOS:
        texto[y0:y1, x0:x1] = claro[y0:y1, x0:x1] * 255
    texto = cv2.dilate(texto, np.ones((5, 5), np.uint8))
    A[..., :3] = cv2.inpaint(np.ascontiguousarray(A[..., :3]), texto, 5, cv2.INPAINT_TELEA)
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
