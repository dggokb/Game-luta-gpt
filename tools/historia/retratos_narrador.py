#!/usr/bin/env python3
"""Recorta os três rostos do narrador do torneio do concept art/concepts/narrador_concept.png
para o quadro da narração (docs/historia/narrador.md):
android/app/src/main/assets/historia/retratos/narrador_<expressao>.png.

O model sheet tem três close-ups na coluna da direita:
- de cima, sorrindo de canto: "normal";
- do meio, gritando no microfone: "empolgado";
- de baixo, de cara fechada: "serio".
O fundo liso (cinza ou bege) sai e vira transparente, como nos outros retratos. Rode de novo
quando o concept mudar:

    python3 tools/historia/retratos_narrador.py
"""
from collections import deque
from pathlib import Path
from PIL import Image, ImageFilter

RAIZ = Path(__file__).resolve().parents[2]
CONCEPT = RAIZ / "art/concepts/narrador_concept.png"
SAIDA = RAIZ / "android/app/src/main/assets/historia/retratos"
TAMANHO = 288

# {esquerda, topo, direita, base} em pixels do concept (1448x1086), por dentro da moldura,
# na proporção do quadro da narração (um pouco mais alto que largo), com o rosto no meio.
RETRATOS = {
    "normal": (1121, 8, 1356, 266),
    "empolgado": (1150, 281, 1400, 505),
    "serio": (1128, 515, 1342, 750),
}

# A boca aberta (dentes claros) fica, em pixels do recorte: {esquerda, topo, direita, base}.
BOCA = {
    "normal": (118, 130, 185, 190),
    "empolgado": (105, 95, 158, 145),
}

# Ilha de fundo presa no desenho (entre os fios do cabelo, atrás do microfone): sai a partir
# deste tamanho. Na faixa de cima (cabelo) qualquer pedacinho; embaixo, só pedaço grande (o
# brilho do dourado do microfone parece papel).
ILHA_CABELO, ILHA = 12, 150
CABELO = 0.4


def fundo(rgb):
    """Fundo do close-up: claro e quase sem cor (a pele, o cabelo e o terno ficam)."""
    r, g, b = rgb
    return min(r, g, b) >= 110 and max(r, g, b) - min(r, g, b) <= 50


def papel(rgb):
    """Fundo das ilhas: só o bege/branco do papel e da moldura, quase sem cor."""
    r, g, b = rgb
    return min(r, g, b) >= 170 and max(r, g, b) - min(r, g, b) <= 30


def recortar(img, boca=None):
    """Tira o fundo ligado às bordas de cima e dos lados (o terno encosta embaixo) e as ilhas de
    fundo presas no cabelo, menos a boca."""
    w, h = img.size
    px = img.load()
    fora = bytearray(w * h)
    fila = deque()
    for x in range(w):
        fila.append((x, 0))
    for y in range(h):
        fila.append((0, y))
        fila.append((w - 1, y))
    while fila:
        x, y = fila.popleft()
        if x < 0 or y < 0 or x >= w or y >= h or fora[y * w + x] or not fundo(px[x, y][:3]):
            continue
        fora[y * w + x] = 1
        fila.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))
    # Ilhas: pedaços de fundo cercados pelo desenho.
    visto = bytearray(fora)
    for y0 in range(h):
        for x0 in range(w):
            if visto[y0 * w + x0] or not papel(px[x0, y0][:3]):
                continue
            ilha, fila, na_boca = [], deque([(x0, y0)]), False
            visto[y0 * w + x0] = 1
            while fila:
                x, y = fila.popleft()
                ilha.append(y * w + x)
                if boca and boca[0] <= x < boca[2] and boca[1] <= y < boca[3]:
                    na_boca = True
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    if 0 <= nx < w and 0 <= ny < h and not visto[ny * w + nx] and papel(px[nx, ny][:3]):
                        visto[ny * w + nx] = 1
                        fila.append((nx, ny))
            minimo = ILHA_CABELO if y0 < h * CABELO else ILHA
            if len(ilha) >= minimo and not na_boca:
                for i in ilha:
                    fora[i] = 1
    alfa = Image.frombytes("L", (w, h), bytes(0 if v else 255 for v in fora))
    # Borda macia de 1 px, sem halo claro.
    alfa = alfa.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(0.8))
    out = img.convert("RGBA")
    out.putalpha(alfa)
    return out


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    concept = Image.open(CONCEPT).convert("RGB")
    for expressao, caixa in RETRATOS.items():
        retrato = recortar(concept.crop(caixa), BOCA.get(expressao))
        escala = TAMANHO / retrato.height
        retrato = retrato.resize((round(retrato.width * escala), TAMANHO), Image.LANCZOS)
        destino = SAIDA / f"narrador_{expressao}.png"
        retrato.save(destino, optimize=True)
        print(destino, retrato.size)


if __name__ == "__main__":
    main()
