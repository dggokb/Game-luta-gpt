#!/usr/bin/env python3
"""Máscara de uma pintura a partir de uma cópia dela com uma parte trocada por verde chapado.

    python3 tools/stage/alinhar_mascara.py original.png verde.png saida.png --largura 4096 [--inverter]

O Codex (como os geradores de imagem em geral) repinta a imagem inteira quando troca uma parte
por verde: o que sobra sai um pouco deslocado. Então o verde serve só de molde. Ele é alinhado
à original (pontos SIFT em comum, polinômio de 3º grau de um para o outro e um acerto fino por
fluxo óptico) e a máscara sai na geometria da original. Branco = o que NÃO é verde (com
``--inverter``, o verde). Saída na largura pedida, sem pedacinhos soltos.
"""
import argparse

import cv2
import numpy as np
from PIL import Image


def verde(img):
    return ((img[:, :, 1].astype(int) > 180) & (img[:, :, 0] < 110) & (img[:, :, 2] < 110)).astype(np.uint8)


def alinhar(original, molde):
    """Mapas (mx, my) que levam cada pixel da original ao pixel correspondente do molde."""
    h, w = original.shape[:2]
    g = verde(molde)
    ga = cv2.cvtColor(original, cv2.COLOR_RGB2GRAY)
    gm = cv2.cvtColor(molde, cv2.COLOR_RGB2GRAY)
    sift = cv2.SIFT_create(8000)
    ka, da = sift.detectAndCompute(ga, None)
    km, dm = sift.detectAndCompute(gm, (1 - g) * 255)
    bons = [x for x, y in cv2.BFMatcher().knnMatch(da, dm, k=2) if x.distance < 0.75 * y.distance]
    pa = np.float32([ka[x.queryIdx].pt for x in bons])
    pm = np.float32([km[x.trainIdx].pt for x in bons])

    def termos(p):
        x, y = p[:, 0] / w, p[:, 1] / h
        return np.stack([np.ones_like(x), x, y, x * x, x * y, y * y, x ** 3, x * x * y, x * y * y, y ** 3], 1)

    usa = np.ones(len(pa), bool)
    for _ in range(5):
        cx = np.linalg.lstsq(termos(pa[usa]), pm[usa, 0], rcond=None)[0]
        cy = np.linalg.lstsq(termos(pa[usa]), pm[usa, 1], rcond=None)[0]
        t = termos(pa)
        erro = np.hypot(t @ cx - pm[:, 0], t @ cy - pm[:, 1])
        usa = erro < max(3, np.percentile(erro[usa], 80))
    print(f"{len(bons)} pontos, {usa.sum()} usados, erro mediano {np.median(erro[usa]):.2f} px")
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = termos(np.stack([xx.ravel(), yy.ravel()], 1))
    mx = (t @ cx).reshape(h, w).astype(np.float32)
    my = (t @ cy).reshape(h, w).astype(np.float32)
    quase = cv2.remap(molde, mx, my, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE)
    f = cv2.calcOpticalFlowFarneback(ga, cv2.cvtColor(quase, cv2.COLOR_RGB2GRAY), None, 0.5, 5, 25, 5, 7, 1.5, 0)
    mx = cv2.remap(mx, xx + f[:, :, 0], yy + f[:, :, 1], cv2.INTER_LINEAR)
    my = cv2.remap(my, xx + f[:, :, 0], yy + f[:, :, 1], cv2.INTER_LINEAR)
    return mx, my


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("original")
    ap.add_argument("verde")
    ap.add_argument("saida")
    ap.add_argument("--largura", type=int, default=None)
    ap.add_argument("--inverter", action="store_true")
    a = ap.parse_args()
    molde = np.asarray(Image.open(a.verde).convert("RGB"))
    original = np.asarray(Image.open(a.original).convert("RGB").resize((molde.shape[1], molde.shape[0]), Image.LANCZOS))
    mx, my = alinhar(original, molde)
    g = cv2.remap(verde(molde).astype(np.float32), mx, my, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE)
    m = g if a.inverter else 1 - g
    m[:14] = m[14:15]                       # a borda de cima do alinhamento
    W = a.largura or original.shape[1]
    H = round(m.shape[0] * W / m.shape[1])
    m = cv2.resize(m, (W, H), interpolation=cv2.INTER_CUBIC)
    b = (m > 0.5).astype(np.uint8)
    for alvo in (b, None):
        x = b if alvo is not None else 1 - b
        n, lab, st, _ = cv2.connectedComponentsWithStats(x, 8)
        for i in range(1, n):
            if st[i, cv2.CC_STAT_AREA] < 400:
                b[lab == i] = 0 if alvo is not None else 1
    b = cv2.erode(b, np.ones((3, 3), np.uint8))
    Image.fromarray((cv2.GaussianBlur(b.astype(np.float32), (0, 0), 1.2) * 255).astype(np.uint8)).save(a.saida)


if __name__ == "__main__":
    main()
