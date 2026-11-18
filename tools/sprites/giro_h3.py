#!/usr/bin/env python3
"""Folhas do giro de nocaute (HIT_SPIN e HIT_SPIN_POUSO) a partir do vídeo H3 do giro.

O vídeo (1344x768, fundo verde) vai da guarda torcida pelo soco ao corpo deitado de costas
(docs/personagens/prompts-reacoes-cena.md). Em cada quadro o corpo fica com a base na raiz e o
meio na raiz (o arco do voo é a física do jogo, CombatFighter.spinning). Escala: a guarda do vídeo
H3 = o IDLE da folha do pacote.

    python3 tools/sprites/giro_h3.py p02

Os números de cada pacote ficam em PACOTES (quadros contados do 1, como no ffmpeg). O do Ryūga
(player_base) foi montado antes desta ferramenta, com voo 2-48 de 2 em 2, pouso 50-62 e escala
451/547; estão aqui para refazer do mesmo jeito.
"""
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from video_para_sprite import key  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
FW, FH, RX, RY = 768, 640, 400, 600

PACOTES = {
    'player_base': {
        'video': 'art/sprites/source/player_base_hit_spin_video.mp4',
        'escala': 451 / 547,
        'voo': list(range(2, 49, 2)),
        'pouso': list(range(50, 63, 2)),
    },
    'p02': {
        'video': 'art/sprites/source/p02_hit_spin_video.mp4',
        'escala': 391 / 469,
        # 5-64: o soco já torceu (o clarão dos quadros 2-4 cobre o rosto), o voo e o rolo.
        'voo': [round(5 + i * 59 / 23) for i in range(24)],
        'pouso': list(range(66, 79, 2)),
    },
    'p15': {
        'video': 'art/sprites/source/p15_hit_spin_video.mp4',
        'escala': 483 / 586,
        # 5-57: o soco torce e o voo com o rolo; bate de costas no 59.
        'voo': [round(5 + i * 52 / 23) for i in range(24)],
        'pouso': list(range(59, 72, 2)),
    },
    'p16': {
        'video': 'art/sprites/source/p16_hit_spin_video.mp4',
        'escala': 441 / 537,
        # 12-62: depois do clarão do soco (7-9), o voo com o rolo até ficar deitada no ar. Do 64 ao 76
        # ela cai de ombros com as pernas para cima: isso fica no pouso (rápido), não no fim do voo,
        # que o jogo segura até o chão (o dono, 18/11: "da Bonnie ficou estranho").
        'voo': [round(12 + i * 50 / 23) for i in range(24)],
        'pouso': [66, 70, 74, 78, 82, 86, 90],
    },
}


def quadros(video):
    cap = cv2.VideoCapture(str(ROOT / video))
    out = []
    while True:
        ok, f = cap.read()
        if not ok:
            return out
        out.append(f)


def celula(frame, escala):
    k = key(frame)
    ys, xs = np.nonzero(k[..., 3] > 128)
    im = Image.fromarray(k, 'RGBA')
    im = im.resize((round(im.width * escala), round(im.height * escala)), Image.LANCZOS)
    meio = (xs.min() + xs.max()) / 2 * escala
    base = ys.max() * escala
    cel = Image.new('RGBA', (FW, FH), (0, 0, 0, 0))
    cel.paste(im, (round(RX - meio), round(RY - base)), im)
    return cel


def folha(fr, nums, escala, cols, saida):
    rows = (len(nums) + cols - 1) // cols
    out = Image.new('RGBA', (cols * FW, rows * FH), (0, 0, 0, 0))
    for i, n in enumerate(nums):
        out.paste(celula(fr[n - 1], escala), ((i % cols) * FW, (i // cols) * FH))
    out.save(ROOT / saida)
    print(saida, len(nums), 'quadros')


def main():
    pacote = sys.argv[1]
    p = PACOTES[pacote]
    fr = quadros(p['video'])
    folha(fr, p['voo'], p['escala'], 8, f'art/sprites/source/{pacote}_hit_spin.png')
    folha(fr, p['pouso'], p['escala'], len(p['pouso']), f'art/sprites/source/{pacote}_hit_spin_pouso.png')


if __name__ == '__main__':
    main()
