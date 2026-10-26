#!/usr/bin/env python3
"""Telão provisório das arenas (semifinal e final), até a arte do GPT chegar.

Desenha a estrutura com PIL: moldura de aço escuro com friso dourado, uma faixa vermelha no
alto com o emblema do torneio (um medalhão dourado), refletores em cima e duas pernas de
treliça até o chão. A tela fica escura: o jogo desenha a luta nela. Imprime o trecho
"telao" do stage.json (onde fica a tela, em frações da imagem).

    python3 tools/stage/telao_provisorio.py android/app/src/main/assets/stages/arena_central/telao.png
"""
import math
import sys
from PIL import Image, ImageDraw, ImageFilter

W, H = 1000, 820
TELA = (70, 96, 930, 578)          # a tela (esquerda, topo, direita, base)
ACO, ACO_CLARO = (26, 28, 36, 255), (52, 56, 70, 255)
OURO, VERMELHO = (214, 168, 74, 255), (168, 30, 36, 255)


def treliça(d, x0, x1, y0, y1):
    d.rectangle((x0, y0, x0 + 10, y1), fill=ACO)
    d.rectangle((x1 - 10, y0, x1, y1), fill=ACO)
    passo = 44
    y = y0
    while y < y1:
        d.line((x0 + 5, y, x1 - 5, min(y1, y + passo)), fill=ACO_CLARO, width=5)
        d.line((x1 - 5, y, x0 + 5, min(y1, y + passo)), fill=ACO_CLARO, width=5)
        d.line((x0, y, x1, y), fill=ACO, width=4)
        y += passo
    d.rectangle((x0 - 18, y1 - 12, x1 + 18, y1), fill=ACO)


def main(saida):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    l, t, r, b = TELA
    # Pernas de treliça até o chão.
    treliça(d, 230, 290, b + 30, H - 1)
    treliça(d, 710, 770, b + 30, H - 1)
    # Moldura e caixa de trás.
    d.rounded_rectangle((l - 34, t - 58, r + 34, b + 40), radius=10, fill=ACO)
    d.rounded_rectangle((l - 22, t - 14, r + 22, b + 22), radius=6, fill=ACO_CLARO)
    d.rectangle((l - 10, t - 6, r + 10, b + 10), outline=OURO, width=4)
    # A tela (o jogo desenha por cima).
    d.rectangle((l, t, r, b), fill=(6, 10, 20, 255))
    # Faixa vermelha com o emblema no alto.
    d.rectangle((l - 34, t - 58, r + 34, t - 20), fill=VERMELHO)
    d.line((l - 34, t - 20, r + 34, t - 20), fill=OURO, width=3)
    cx, cy = (l + r) // 2, t - 39
    d.ellipse((cx - 30, cy - 30, cx + 30, cy + 30), fill=OURO)
    d.ellipse((cx - 22, cy - 22, cx + 22, cy + 22), outline=VERMELHO, width=4)
    for k in range(8):
        a = k * math.pi / 4
        d.line((cx, cy, cx + 18 * math.cos(a), cy + 18 * math.sin(a)), fill=VERMELHO, width=3)
    # Refletores em cima da moldura.
    for i in range(6):
        x = l + 40 + i * (r - l - 80) / 5
        d.rectangle((x - 16, t - 86, x + 16, t - 60), fill=ACO)
        d.ellipse((x - 11, t - 82, x + 11, t - 64), fill=(250, 244, 220, 255))
    img.save(saida, optimize=True)
    print('"telao": {"x": %.4f, "y": %.4f, "largura": %.4f, "altura": %.4f}'
          % (l / W, t / H, (r - l) / W, (b - t) / H))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "telao.png")
