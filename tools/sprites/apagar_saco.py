#!/usr/bin/env python3
"""Apaga do vídeo (fundo verde) um objeto que o Seedance desenhou no lugar do rival: o saco do
Ultra do p09. O saco vira o verde do fundo e o vídeo segue normal para o ultras.py, que recorta
o verde. No jogo, o rival fica preso onde o saco estava ("rival" em ultras.json).

O saco é bege acinzentado: o mesmo tom da pele, mas bem menos saturado e menos claro. Sai a
mancha grande dessa cor (com as linhas e a textura de dentro) e o contorno escuro em volta,
menos o que for vermelho, faixa amarela ou pele clara do lutador. Pedaços soltos que sobram
longe do corpo o video_para_sprite.py já descarta.

    python3 tools/sprites/apagar_saco.py ultra_original.mp4 ultra.mp4
"""
import argparse
import subprocess
import tempfile
from pathlib import Path

import cv2
import numpy as np

# Cor do saco em HSV do OpenCV (H 0-180): bege acinzentado.
HUE = (9, 23)
SAT = (50, 125)
VAL = (90, 190)
MIN_AREA = 1500   # px: manchas menores dessa cor são do lutador (sombra da pele)
CLOSE = 15        # px: fecha as linhas e a textura de dentro do saco
EDGE = 13         # px: contorno escuro em volta do saco


def sack_mask(img):
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    h, s, v = (hsv[..., i].astype(int) for i in range(3))
    color = ((h >= HUE[0]) & (h <= HUE[1]) & (s >= SAT[0]) & (s <= SAT[1]) &
             (v >= VAL[0]) & (v <= VAL[1])).astype(np.uint8)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(color, 8)
    sack = np.zeros_like(color)
    for i in range(1, n):
        if stats[i, cv2.CC_STAT_AREA] >= MIN_AREA:
            sack[labels == i] = 1
    sack = cv2.morphologyEx(sack, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (CLOSE, CLOSE)))
    # Buracos dentro do saco (linhas, nó) também saem.
    outside = sack.copy()
    hh, ww = outside.shape
    cv2.floodFill(outside, np.zeros((hh + 2, ww + 2), np.uint8), (0, 0), 1)
    sack = sack | (1 - outside)
    ring = cv2.dilate(sack, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (EDGE, EDGE)))
    red = ((h < 8) | (h > 160)) & (s > 100) & (v > 60)
    yellow = (h >= 22) & (h <= 40) & (s > 140) & (v > 120)
    skin = (h >= 4) & (h <= 24) & (s > 110) & (v > 140)
    edge = (ring == 1) & (sack == 0) & ~(red | yellow | skin)
    return sack.astype(bool) | edge


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('entrada')
    p.add_argument('saida')
    args = p.parse_args(argv)
    cap = cv2.VideoCapture(args.entrada)
    fps = cap.get(cv2.CAP_PROP_FPS) or 24.0
    with tempfile.TemporaryDirectory() as tmp:
        count = touched = 0
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            mask = sack_mask(frame)
            if mask.any():
                touched += 1
                # O verde do fundo no canto (o vídeo tem vinheta leve; o recorte usa o tom).
                frame[mask] = np.median(frame[:40, :40].reshape(-1, 3), axis=0).astype(np.uint8)
            cv2.imwrite(str(Path(tmp) / f'{count:04d}.png'), frame)
            count += 1
        # Sem perda (qp 0, 4:4:4): o recorte do verde vê as mesmas cores.
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-framerate', f'{fps:g}', '-i', str(Path(tmp) / '%04d.png'),
                        '-c:v', 'libx264', '-qp', '0', '-pix_fmt', 'yuv444p', args.saida], check=True)
    print(f'{args.saida}: {count} quadros, saco apagado em {touched}')


if __name__ == '__main__':
    main()
