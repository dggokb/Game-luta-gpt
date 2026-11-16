#!/usr/bin/env python3
"""STAMP provisório: um quadro da luta passado para preto e branco de mangá (traço + retícula).

    python3 tools/cenas/stamp_provisorio.py impacto_1_1.png saida.jpg x0 y0 x1 y1

A entrada é o quadro logo antes do STAMP que o `gradle :pc:testeQte` grava (impacto_N_1.png); o
recorte (x0, y0, x1, y1, em px da tela 1280x720) pega os dois lutadores. Serve até a arte de
verdade do golpe chegar (o mesmo enquadramento e as mesmas poses, desenhados em mangá).
"""
import sys

import cv2
import numpy as np

src, dst, x0, y0, x1, y1 = sys.argv[1], sys.argv[2], *map(int, sys.argv[3:7])
img = cv2.imread(src)[y0:y1, x0:x1]
img = cv2.resize(img, None, fx=1.25, fy=1.25, interpolation=cv2.INTER_CUBIC)
g = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
g = cv2.bilateralFilter(g, 9, 60, 9)
# O traço: limiar adaptativo (as bordas viram tinta preta).
linhas = cv2.medianBlur(cv2.adaptiveThreshold(g, 255, cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY, 11, 6), 3)
# Os tons: branco, retícula de pontos e preto.
h, w = g.shape
yy, xx = np.mgrid[0:h, 0:w]
pontos = (((xx % 7) - 3) ** 2 + ((yy % 7) - 3) ** 2) < 5
tom = np.full_like(g, 255)
tom[(g < 150) & (g >= 70) & pontos] = 0
tom[g < 70] = 20
cv2.imwrite(dst, np.minimum(linhas, tom), [cv2.IMWRITE_JPEG_QUALITY, 82])
