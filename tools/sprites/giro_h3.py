#!/usr/bin/env python3
"""Folhas do giro de nocaute (HIT_SPIN e HIT_SPIN_POUSO) a partir do vídeo H3 do giro.

O vídeo (1344x768, fundo verde) vai da guarda torcida pelo soco ao corpo deitado de costas
(docs/personagens/prompts-reacoes-cena.md). O centro de massa do corpo fica em rootX; na altura, ver
folha() (o arco do voo é a física do jogo, CombatFighter.spinning). Escala: a guarda do vídeo
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
FW, FH, RX, RY = 768, 704, 400, 600

# Os quadros do voo da Bonnie: 15-63 sem a cabeça girando errado (39-47).
BONNIE = [n for n in range(15, 64) if not 39 <= n <= 47]

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
        # O 2º vídeo do giro dela (18/11). 15-63 depois do clarão do soco (9-13), o voo com o rolo
        # até ficar deitada no ar, SEM 39-47: ali a cabeça dá meia-volta sozinha, com o corpo de costas
        # (o dono: "tem uma hora que a cabeça dela gira errada, corta esse erro"); o 38 emenda no 48.
        # Do 64 ao 72 ela cai de ombros com as pernas para cima: fica no pouso (rápido), não no fim do
        # voo, que o jogo segura até o chão.
        'video': 'art/sprites/source/p16_hit_spin_video.mp4',
        'escala': 441 / 537,
        'voo': [BONNIE[round(i * (len(BONNIE) - 1) / 23)] for i in range(24)],
        'pouso': list(range(64, 77, 2)),
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


def recorte(frame):
    """O quadro sem o verde, o centro de massa do corpo (x, y) e a base, em px do vídeo."""
    k = key(frame)
    ys, xs = np.nonzero(k[..., 3] > 128)
    return k, (xs.mean(), ys.mean()), ys.max()


def celula(k, centro, acima, escala):
    """O centro de massa do corpo em rootX e `acima` px (do vídeo) acima de rootY."""
    im = Image.fromarray(k, 'RGBA')
    im = im.resize((round(im.width * escala), round(im.height * escala)), Image.LANCZOS)
    cel = Image.new('RGBA', (FW, FH), (0, 0, 0, 0))
    cel.paste(im, (round(RX - centro[0] * escala), round(RY - (centro[1] + acima) * escala)), im)
    return cel


def folha(fr, nums, escala, cols, saida, voo):
    """No voo o centro do corpo desce em linha reta da altura do 1º quadro (os pés na raiz) à do
    último (deitada, a base na raiz): o desenho segue o arco da física sem pular. Alinhar cada quadro
    pela base fazia o corpo subir e descer no pico (o dono, 18/11: "no pico ela dá uma subida e
    descida"): a ponta de uma faixa ou de um braço caído virava a base. No pouso, a base na raiz."""
    rows = (len(nums) + cols - 1) // cols
    out = Image.new('RGBA', (cols * FW, rows * FH), (0, 0, 0, 0))
    cortes = [recorte(fr[n - 1]) for n in nums]
    h0 = cortes[0][2] - cortes[0][1][1]
    h1 = cortes[-1][2] - cortes[-1][1][1]
    for i, (k, centro, base) in enumerate(cortes):
        acima = h0 + (h1 - h0) * i / (len(nums) - 1) if voo else base - centro[1]
        out.paste(celula(k, centro, acima, escala), ((i % cols) * FW, (i // cols) * FH))
    out.save(ROOT / saida)
    print(saida, len(nums), 'quadros')


def main():
    pacote = sys.argv[1]
    p = PACOTES[pacote]
    fr = quadros(p['video'])
    folha(fr, p['voo'], p['escala'], 8, f'art/sprites/source/{pacote}_hit_spin.png', True)
    folha(fr, p['pouso'], p['escala'], len(p['pouso']), f'art/sprites/source/{pacote}_hit_spin_pouso.png', False)


if __name__ == '__main__':
    main()
