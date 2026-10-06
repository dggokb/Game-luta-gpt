#!/usr/bin/env python3
"""Gera uma arte provisória no formato do cenário "Templo da Lua".

Cada arquivo tem o mesmo nome, formato e transparência que a arte de verdade, então
serve para testar o motor de cenário sem arte (ou como modelo de um cenário novo).
Grava em tools/stage/provisorio/ por padrão, para não sobrescrever a arte final:

    python tools/stage/gerar_provisorio.py [--saida pasta]
"""

import argparse
import math
import random
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFilter
except ImportError:
    sys.exit("Instale o Pillow: pip install pillow")

SAIDA = Path(__file__).resolve().parent / "provisorio"


def gradiente(w, h, topo, base):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(topo, base)))
    return img


def ceu():
    w, h = 1536, 864
    img = gradiente(w, h, (8, 10, 32), (58, 40, 96)).convert("RGBA")
    d = ImageDraw.Draw(img)
    rnd = random.Random(1)
    for _ in range(260):
        x, y = rnd.randrange(w), rnd.randrange(int(h * 0.75))
        r = rnd.choice([1, 1, 1, 2])
        a = rnd.randrange(90, 230)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, a))
    # Halo e lua.
    halo = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    mx, my = int(w * 0.62), int(h * 0.30)
    for r in range(170, 60, -6):
        a = int(70 * (1 - (r - 60) / 110) ** 2)
        hd.ellipse([mx - r, my - r, mx + r, my + r], fill=(210, 220, 255, a))
    img = Image.alpha_composite(img, halo.filter(ImageFilter.GaussianBlur(8)))
    d = ImageDraw.Draw(img)
    d.ellipse([mx - 58, my - 58, mx + 58, my + 58], fill=(238, 240, 250, 255))
    for cx, cy, r in [(-18, -10, 14), (16, 18, 10), (10, -22, 7)]:
        d.ellipse([mx + cx - r, my + cy - r, mx + cx + r, my + cy + r], fill=(214, 218, 232, 255))
    # Nuvens escuras.
    nuvens = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    nd = ImageDraw.Draw(nuvens)
    for _ in range(26):
        x, y = rnd.randrange(-100, w), rnd.randrange(int(h * 0.15), int(h * 0.62))
        rw, rh = rnd.randrange(120, 300), rnd.randrange(24, 52)
        nd.ellipse([x, y, x + rw, y + rh], fill=(30, 26, 62, 170))
    img = Image.alpha_composite(img, nuvens.filter(ImageFilter.GaussianBlur(10)))
    return img.convert("RGB")


def montanhas():
    w, h = 1536, 420
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(2)
    for camada, cor in [(0, (70, 62, 120, 255)), (1, (48, 42, 92, 255))]:
        pts = [(0, h)]
        x = 0
        while x <= w:
            pico = h * (0.15 + 0.45 * rnd.random()) + camada * 60
            pts.append((x, pico))
            x += rnd.randrange(80, 200)
        pts.append((w, pts[1][1]))
        pts.append((w, h))
        d.polygon(pts, fill=cor)
    return img


def cidade():
    w, h = 1536, 640
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(3)
    # Prédios ao fundo com janelas acesas e alguns neons.
    x = 520
    while x < w:
        bw = rnd.randrange(40, 90)
        bh = rnd.randrange(160, 520)
        top = h - bh
        d.rectangle([x, top, x + bw, h], fill=(22, 24, 52, 255))
        for wy in range(top + 8, h - 10, 14):
            for wx in range(x + 6, x + bw - 6, 11):
                if rnd.random() < 0.35:
                    d.rectangle([wx, wy, wx + 4, wy + 6], fill=(255, 214, 140, 220))
        if rnd.random() < 0.35:
            neon = rnd.choice([(255, 60, 200), (60, 220, 255), (170, 90, 255)])
            d.rectangle([x + bw // 2 - 3, top + 10, x + bw // 2 + 3, top + bh // 2], fill=neon + (255,))
        x += bw + rnd.randrange(4, 18)
    # Morro com pagode à esquerda.
    d.ellipse([-120, h - 300, 640, h + 260], fill=(16, 22, 30, 255))
    px, base = 230, h - 250
    for i in range(5):
        lw = 150 - i * 24
        ly = base - i * 46
        d.polygon([(px - lw, ly), (px + lw, ly), (px + lw - 26, ly - 18), (px - lw + 26, ly - 18)], fill=(46, 20, 26, 255))
        d.rectangle([px - lw + 34, ly, px + lw - 34, ly + 28], fill=(30, 18, 22, 255))
        for wx in range(px - lw + 44, px + lw - 44, 16):
            d.rectangle([wx, ly + 8, wx + 7, ly + 20], fill=(255, 190, 110, 255))
    d.line([(px, base - 5 * 46), (px, base - 5 * 46 - 50)], fill=(46, 20, 26, 255), width=6)
    # Cerejeiras no morro.
    for _ in range(18):
        cx, cy = rnd.randrange(20, 600), rnd.randrange(h - 230, h - 60)
        r = rnd.randrange(22, 46)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(222, 120, 180, 235))
    return img


def portal():
    w, h = 1536, 560
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rnd = random.Random(4)
    # Cerejeiras dos lados.
    for cx in list(range(60, 520, 70)) + list(range(1020, 1500, 70)):
        tronco = rnd.randrange(200, 320)
        d.line([(cx, h), (cx + rnd.randrange(-20, 20), h - tronco)], fill=(40, 22, 30, 255), width=10)
        for _ in range(9):
            r = rnd.randrange(30, 60)
            ox, oy = cx + rnd.randrange(-70, 70), h - tronco + rnd.randrange(-60, 40)
            d.ellipse([ox - r, oy - r, ox + r, oy + r], fill=(236, 140, 196, 240))
    # Torii vermelho no centro.
    cx = w // 2
    vermelho = (176, 34, 30, 255)
    for dx in (-200, 200):
        d.rectangle([cx + dx - 22, h - 380, cx + dx + 22, h], fill=vermelho)
    d.polygon([(cx - 330, h - 430), (cx + 330, h - 430), (cx + 290, h - 400), (cx - 290, h - 400)], fill=(30, 14, 14, 255))
    d.rectangle([cx - 290, h - 400, cx + 290, h - 372], fill=vermelho)
    d.rectangle([cx - 250, h - 320, cx + 250, h - 298], fill=vermelho)
    # Ponte baixa na frente.
    d.rectangle([0, h - 40, w, h - 26], fill=(70, 30, 24, 255))
    for x in range(0, w, 48):
        d.rectangle([x, h - 60, x + 8, h], fill=(70, 30, 24, 255))
    return img


def grade():
    w, h = 1536, 300
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    madeira, escura, ouro = (120, 38, 28, 255), (64, 20, 16, 255), (214, 164, 64, 255)
    # Degraus.
    for i, y in enumerate(range(h - 70, h, 14)):
        d.rectangle([0, y, w, y + 14], fill=(62 - i * 4, 48 - i * 3, 52 - i * 3, 255))
    topo, base = 70, h - 80
    d.rectangle([0, topo, w, topo + 18], fill=madeira)
    d.rectangle([0, topo + 18, w, topo + 24], fill=escura)
    d.rectangle([0, base - 30, w, base - 16], fill=madeira)
    for x in range(0, w, 32):
        d.rectangle([x + 8, topo + 24, x + 16, base - 30], fill=escura)
    for x in range(0, w, 256):
        d.rectangle([x + 110, topo - 30, x + 146, base], fill=madeira)
        d.ellipse([x + 110, topo - 62, x + 146, topo - 26], fill=ouro)
        d.rectangle([x + 112, topo - 34, x + 144, topo - 28], fill=ouro)
    return img


def pilar():
    w, h = 720, 1440
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    vermelho, escuro, ouro = (150, 30, 26, 255), (70, 14, 12, 255), (214, 164, 64, 255)
    # Viga do teto e pilar.
    d.rectangle([0, 60, w, 130], fill=escuro)
    d.rectangle([0, 130, w, 150], fill=ouro)
    d.rectangle([60, 130, 180, h], fill=vermelho)
    d.rectangle([60, 130, 82, h], fill=(190, 52, 40, 255))
    d.rectangle([50, h - 90, 190, h], fill=escuro)
    # Estandarte com um traço de pincel (sem texto real).
    d.rectangle([260, 170, 560, 1040], fill=(110, 18, 22, 255))
    d.rectangle([250, 160, 570, 182], fill=ouro)
    d.polygon([(260, 1040), (410, 1110), (560, 1040)], fill=(110, 18, 22, 255))
    pincel = (214, 176, 120, 255)
    d.line([(320, 380), (500, 340)], fill=pincel, width=34)
    d.line([(410, 300), (400, 760)], fill=pincel, width=38)
    d.line([(330, 560), (480, 640)], fill=pincel, width=30)
    d.line([(340, 820), (500, 780)], fill=pincel, width=30)
    # Lanterna pendurada.
    d.line([(640, 150), (640, 330)], fill=escuro, width=4)
    d.rectangle([606, 330, 674, 340], fill=escuro)
    d.ellipse([590, 335, 690, 470], fill=(255, 196, 110, 255))
    for y in range(350, 470, 22):
        d.line([(596, y), (684, y)], fill=(200, 110, 50, 255), width=3)
    d.rectangle([606, 465, 674, 475], fill=escuro)
    return img


def braseiro():
    w, h = 360, 420
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bronze, escuro, ouro = (150, 104, 48, 255), (64, 40, 22, 255), (220, 172, 70, 255)
    d.rectangle([150, 110, 210, h - 40], fill=escuro)
    d.rectangle([110, h - 40, 250, h], fill=bronze)
    d.pieslice([40, -40, 320, 140], 0, 180, fill=bronze)
    d.rectangle([34, 40, 326, 56], fill=ouro)
    d.ellipse([60, 32, 300, 64], fill=(255, 140, 50, 255))
    return img


def chao():
    w, h = 1536, 1024
    img = gradiente(w, h, (38, 34, 44), (22, 20, 28)).convert("RGBA")
    d = ImageDraw.Draw(img)
    rnd = random.Random(5)
    tam = 96
    for ty in range(0, h, tam):
        for tx in range(0, w, tam):
            v = rnd.randrange(-10, 10)
            d.rectangle([tx + 2, ty + 2, tx + tam - 2, ty + tam - 2], fill=(46 + v, 42 + v, 54 + v, 255))
    for ty in range(0, h + 1, tam):
        d.line([(0, ty), (w, ty)], fill=(16, 14, 20, 255), width=3)
    for tx in range(0, w + 1, tam):
        d.line([(tx, 0), (tx, h)], fill=(16, 14, 20, 255), width=3)
    # Emblema dourado no centro.
    cx, cy = w // 2, h // 2
    ouro = (196, 150, 60, 255)
    for r, largura in [(430, 14), (380, 8), (240, 10)]:
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=ouro, width=largura)
    for i in range(3):
        a0 = i * 120
        pontos = []
        for k in range(30):
            a = math.radians(a0 + k * 4)
            r = 60 + k * 6
            pontos.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
        d.line(pontos, fill=ouro, width=18)
    # Brilho molhado.
    brilho = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bd = ImageDraw.Draw(brilho)
    for _ in range(40):
        x, y = rnd.randrange(w), rnd.randrange(h)
        bd.ellipse([x - 60, y - 8, x + 60, y + 8], fill=(150, 140, 190, 40))
    img = Image.alpha_composite(img, brilho.filter(ImageFilter.GaussianBlur(6)))
    return img.convert("RGB")


def main():
    parser = argparse.ArgumentParser(description="Gera arte provisória de cenário.")
    parser.add_argument("--saida", type=Path, default=SAIDA)
    saida = parser.parse_args().saida
    saida.mkdir(parents=True, exist_ok=True)
    ceu().save(saida / "ceu.jpg", quality=88)
    montanhas().save(saida / "montanhas.png")
    cidade().save(saida / "cidade.png")
    portal().save(saida / "portal.png")
    grade().save(saida / "grade.png")
    pilar().save(saida / "pilar.png")
    braseiro().save(saida / "braseiro.png")
    chao().save(saida / "chao.jpg", quality=88)
    print(f"arte provisória em {saida}")


if __name__ == "__main__":
    main()
