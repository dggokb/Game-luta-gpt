#!/usr/bin/env python3
"""Recorta os rostos de Ryūga (p01) e Kōga (p03) adultos dos concepts para os quadros da
conversa na luta (docs/historia/motor-conversas.md):
android/app/src/main/assets/historia/retratos/<quem>_<expressao>.png.

Cada concept tem dois close-ups no canto de cima à direita: de frente (vira a expressão
"normal") e de lado, de cara fechada ("bravo"). O fundo cinza liso sai (vira transparente),
como nos retratos do Dafu e do Takatoshi. Rode de novo quando um concept mudar:

    python3 tools/historia/retratos_irmaos.py
"""
from collections import deque
from pathlib import Path
from PIL import Image, ImageFilter

RAIZ = Path(__file__).resolve().parents[2]
SAIDA = RAIZ / "android/app/src/main/assets/historia/retratos"
TAMANHO = 288

# {esquerda, topo, direita, base} em pixels do concept (1448x1086), por dentro da moldura do close-up.
RETRATOS = {
    ("ryuga", "normal"): ("p01", (1170, 31, 1431, 337)),
    ("ryuga", "bravo"): ("p01", (1174, 356, 1431, 632)),
    ("koga", "normal"): ("p03", (1150, 15, 1433, 337)),
    ("koga", "bravo"): ("p03", (1150, 355, 1433, 664)),
}


def fundo(rgb):
    """Cinza claro do fundo do model sheet (sem cor: a pele e o branco da roupa ficam)."""
    r, g, b = rgb
    return min(r, g, b) >= 188 and max(r, g, b) - min(r, g, b) <= 14 and max(r, g, b) <= 236


def recortar(img):
    """Tira o fundo ligado às bordas de cima e dos lados (a roupa encosta embaixo)."""
    w, h = img.size
    px = img.load()
    fora = [[False] * h for _ in range(w)]
    fila = deque()
    for x in range(w):
        fila.append((x, 0))
    for y in range(h):
        fila.append((0, y))
        fila.append((w - 1, y))
    while fila:
        x, y = fila.popleft()
        if x < 0 or y < 0 or x >= w or y >= h or fora[x][y] or not fundo(px[x, y][:3]):
            continue
        fora[x][y] = True
        fila.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))
    alfa = Image.new("L", (w, h), 255)
    a = alfa.load()
    for x in range(w):
        for y in range(h):
            if fora[x][y]:
                a[x, y] = 0
    # Borda macia de 1 px, sem halo cinza.
    alfa = alfa.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(0.8))
    out = img.convert("RGBA")
    out.putalpha(alfa)
    return out


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    for (quem, expressao), (concept, caixa) in RETRATOS.items():
        img = Image.open(RAIZ / "art/concepts" / f"{concept}.png").convert("RGB").crop(caixa)
        retrato = recortar(img)
        escala = TAMANHO / max(retrato.size)
        retrato = retrato.resize((round(retrato.width * escala), round(retrato.height * escala)), Image.LANCZOS)
        destino = SAIDA / f"{quem}_{expressao}.png"
        retrato.save(destino, optimize=True)
        print(destino, retrato.size)


if __name__ == "__main__":
    main()
