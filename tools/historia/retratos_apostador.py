#!/usr/bin/env python3
"""Recorta os três rostos do Apostador (quem apresenta a Semifinal) do concept
art/concepts/apostador_concept.png para o quadro da narração (docs/historia/aposta.md):
android/app/src/main/assets/historia/retratos/apostador_<expressao>.png.

O model sheet tem três close-ups na coluna da direita:
- de cima, rindo com a mão aberta: "empolgado";
- do meio, a mão no queixo, sorriso de canto: "normal";
- de baixo, a gargalhada de olho arregalado: "gargalhada".
O fundo liso (cinza-azulado) sai e vira transparente, como nos outros retratos. A camisa creme
encosta na borda: por isso o fundo é a cor do canto do quadro, com pouca folga. Rode de novo
quando o concept mudar:

    python3 tools/historia/retratos_apostador.py
"""
from collections import deque
from pathlib import Path
from PIL import Image, ImageFilter

RAIZ = Path(__file__).resolve().parents[2]
CONCEPT = RAIZ / "art/concepts/apostador_concept.png"
SAIDA = RAIZ / "android/app/src/main/assets/historia/retratos"
TAMANHO = 288

# {esquerda, topo, direita, base} em pixels do concept (1536x1024), por dentro da moldura,
# com o rosto no meio.
RETRATOS = {
    "empolgado": (1070, 81, 1335, 372),
    "normal": (1040, 391, 1345, 659),
    "gargalhada": (1035, 677, 1375, 984),
}

# Folga de cada canal em volta da cor do fundo.
FOLGA = 20
# Ilhas de fundo presas no desenho (entre os fios do cabelo) saem a partir deste tamanho.
ILHA = 25


def cor_do_fundo(img):
    """A média de um quadradinho no canto de cima à esquerda e à direita (o fundo liso)."""
    w, _ = img.size
    px = img.load()
    amostras = [px[x, y] for x in list(range(2, 8)) + list(range(w - 8, w - 2)) for y in range(2, 8)]
    return tuple(sum(c[i] for c in amostras) // len(amostras) for i in range(3))


def recortar(img):
    w, h = img.size
    px = img.load()
    bg = cor_do_fundo(img)

    def fundo(rgb):
        return all(abs(rgb[i] - bg[i]) <= FOLGA for i in range(3))

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
    visto = bytearray(fora)
    for y0 in range(h):
        for x0 in range(w):
            if visto[y0 * w + x0] or not fundo(px[x0, y0][:3]):
                continue
            ilha, fila = [], deque([(x0, y0)])
            visto[y0 * w + x0] = 1
            while fila:
                x, y = fila.popleft()
                ilha.append(y * w + x)
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    if 0 <= nx < w and 0 <= ny < h and not visto[ny * w + nx] and fundo(px[nx, ny][:3]):
                        visto[ny * w + nx] = 1
                        fila.append((nx, ny))
            if len(ilha) >= ILHA:
                for i in ilha:
                    fora[i] = 1
    alfa = Image.frombytes("L", (w, h), bytes(0 if v else 255 for v in fora))
    alfa = alfa.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(0.8))
    out = img.convert("RGBA")
    out.putalpha(alfa)
    return out


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    concept = Image.open(CONCEPT).convert("RGB")
    for expressao, caixa in RETRATOS.items():
        retrato = recortar(concept.crop(caixa))
        escala = TAMANHO / retrato.height
        retrato = retrato.resize((round(retrato.width * escala), TAMANHO), Image.LANCZOS)
        destino = SAIDA / f"apostador_{expressao}.png"
        retrato.save(destino, optimize=True)
        print(destino, retrato.size)


if __name__ == "__main__":
    main()
