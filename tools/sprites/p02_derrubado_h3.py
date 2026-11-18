#!/usr/bin/env python3
"""Folhas da queda, do caído e do levantar da Yin-lu (p02) a partir do vídeo H3 "Derrubado".

O vídeo (art/sprites/source/p02_derrubado_h3_video.mp4, 1344x768, 24 fps) começa e termina na
imagem art/keys/p02/inicio_centro_h3.png, sem zoom. Escala: a guarda do vídeo (469 px) = o IDLE
da folha (391 px). A raiz fica fixa no meio dos pés do 1º quadro (x 672, chão y 733), então o
deslocamento do vídeo fica no desenho: ela voa para trás, cai e levanta de volta no lugar. Deitada,
a base do corpo desce até 16 px abaixo dos pés (perspectiva); essa diferença é tirada para o corpo
deitado ficar no chão.

    python3 tools/sprites/p02_derrubado_h3.py

Quadros (contados do 1, como no ffmpeg): a queda 10-28 (o quique fica para o jogo, que já sobe o
corpo no começo do caído), o caído 31-45 e o levantar 81-99.
"""
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from video_para_sprite import key  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
VIDEO = ROOT / 'art/sprites/source/p02_derrubado_h3_video.mp4'
ESC = 391 / 469
RX, CHAO, DEITADO = 672, 733, 749
FW, FH, ROOT_X, ROOT_Y = 640, 512, 320, 470

FOLHAS = [
    ('art/sprites/source/p02_fall_down_h3.png', [10, 13, 16, 19, 22, 25, 28, 31, 33, 36, 45], 6),
    ('art/sprites/source/p02_getup_h3.png', [81, 83, 85, 87, 89, 91, 93, 95, 97, 99], 5),
]


def quadros():
    cap = cv2.VideoCapture(str(VIDEO))
    out = []
    while True:
        ok, f = cap.read()
        if not ok:
            return out
        out.append(f)


def celula(frame):
    k = key(frame)
    ys, xs = np.nonzero(k[..., 3] > 128)
    chao = min(max(ys.max(), CHAO), DEITADO)
    im = Image.fromarray(k, 'RGBA')
    im = im.resize((round(im.width * ESC), round(im.height * ESC)), Image.LANCZOS)
    cel = Image.new('RGBA', (FW, FH), (0, 0, 0, 0))
    cel.paste(im, (round(ROOT_X - RX * ESC), round(ROOT_Y - chao * ESC)), im)
    return cel


def main():
    fr = quadros()
    for saida, nums, cols in FOLHAS:
        rows = (len(nums) + cols - 1) // cols
        folha = Image.new('RGBA', (cols * FW, rows * FH), (0, 0, 0, 0))
        for i, n in enumerate(nums):
            folha.paste(celula(fr[n - 1]), ((i % cols) * FW, (i // cols) * FH))
        folha.save(ROOT / saida)
        print(saida, len(nums), 'quadros')


if __name__ == '__main__':
    main()
