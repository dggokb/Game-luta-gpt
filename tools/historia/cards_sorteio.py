#!/usr/bin/env python3
"""Monta os cards de mangá das duplas para o sorteio do torneio (ep04).

Cada card é um quadro duplo de mangá: um lutador de cada lado, separados por uma divisa branca
inclinada, recortados das páginas prontas do porto (ep03), onde cada dupla aparece de perto. As
duplas dos irmãos usam um quadro só (os quatro lado a lado na página do pré-sorteio). Os
recortes ficam de fora dos balões da arte: a fala do card é escrita pelo jogo (traduzível).

    python3 tools/historia/cards_sorteio.py            # grava historia/torneio/cards/<dupla>.jpg
    python3 tools/historia/cards_sorteio.py --folha x.jpg   # e uma folha com todos, para conferir

Recorte: (página, centro x, centro y, altura), em frações da página; a largura sai da proporção
da metade do card. Para trocar a arte de um lutador, mude a linha dele em RECORTES.
"""
import argparse
import os

from PIL import Image, ImageDraw, ImageOps

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
HISTORIA = os.path.join(ROOT, "android", "app", "src", "main", "assets", "historia")
SAIDA = os.path.join(HISTORIA, "torneio", "cards")

W, H = 900, 570
DIVISA = 14          # divisa branca entre os dois lados
BORDA = 8            # contorno preto do card
INCLINA = 0.10       # a divisa vai de 55% da largura em cima a 45% embaixo

# Lutador -> (página, cx, cy, altura do recorte), tudo em frações da página.
RECORTES = {
    "p05": ("ep03/pag5_koga_p05.jpg", 0.43, 0.30, 0.17),
    "p06": ("ep03/pag5_ryuga_p05.jpg", 0.50, 0.69, 0.13),
    "p07": ("ep03/pag5_koga_p07.jpg", 0.27, 0.37, 0.17),
    "p08": ("ep03/pag5_ryuga_p07.jpg", 0.20, 0.48, 0.14),
    "p09": ("ep03/pag5_ryuga_p09.jpg", 0.65, 0.31, 0.12),
    "p10": ("ep03/pag5_ryuga_p09.jpg", 0.17, 0.31, 0.12),
    "p11": ("ep03/pag5_ryuga_p11.jpg", 0.62, 0.605, 0.11),
    "p12": ("ep03/pag5_ryuga_p11.jpg", 0.28, 0.29, 0.13),
    "p13": ("ep03/pag5_ryuga_p13.jpg", 0.30, 0.30, 0.24),
    "p14": ("ep03/pag5_ryuga_p13.jpg", 0.555, 0.50, 0.11),
    "p15": ("ep03/pag5_ryuga_p15.jpg", 0.38, 0.44, 0.15),
    "p16": ("ep03/pag5_koga_p15.jpg", 0.68, 0.50, 0.12),
}
# Dupla -> os dois lutadores (um de cada lado) ou um quadro só (página, cx, cy, altura).
CARDS = {
    "ryuga": ("ep04/pag1.jpg", 0.25, 0.752, 0.178),
    "koga": ("ep04/pag1.jpg", 0.75, 0.752, 0.178),
    "p05": ("p05", "p06"),
    "p07": ("p07", "p08"),
    "p09": ("p09", "p10"),
    "p11": ("p11", "p12"),
    "p13": ("p13", "p14"),
    "p15": ("p15", "p16"),
}


def recorte(pagina, cx, cy, altura, largura_px, altura_px):
    """O retângulo da página com o centro e a altura pedidos, na proporção do destino, ampliado."""
    im = Image.open(os.path.join(HISTORIA, pagina)).convert("RGB")
    pw, ph = im.size
    h = altura * ph
    w = h * largura_px / altura_px
    x0 = min(max(cx * pw - w / 2, 0), pw - w)
    y0 = min(max(cy * ph - h / 2, 0), ph - h)
    parte = im.crop((round(x0), round(y0), round(x0 + w), round(y0 + h)))
    return parte.resize((largura_px, altura_px), Image.LANCZOS)


def card(id_dupla):
    c = CARDS[id_dupla]
    out = Image.new("RGB", (W, H), (255, 255, 255))
    if len(c) == 4:
        out.paste(recorte(*c, W, H), (0, 0))
    else:
        esq, dir_ = (RECORTES[l] for l in c)
        topo, base = W * (0.5 + INCLINA / 2), W * (0.5 - INCLINA / 2)
        meia = int(max(topo, W - base)) + DIVISA
        a = recorte(*esq, meia, H)
        b = recorte(*dir_, meia, H)
        mascara = Image.new("L", (W, H), 0)
        ImageDraw.Draw(mascara).polygon([(0, 0), (topo - DIVISA / 2, 0), (base - DIVISA / 2, H), (0, H)], fill=255)
        out.paste(a, (0, 0), mascara.crop((0, 0, meia, H)))
        mascara = Image.new("L", (W, H), 0)
        ImageDraw.Draw(mascara).polygon([(topo + DIVISA / 2, 0), (W, 0), (W, H), (base + DIVISA / 2, H)], fill=255)
        out.paste(b, (W - meia, 0), mascara.crop((W - meia, 0, W, H)))
        d = ImageDraw.Draw(out)
        for dx in (-DIVISA / 2, DIVISA / 2):
            d.line([(topo + dx, 0), (base + dx, H)], fill=(10, 10, 10), width=3)
    out = ImageOps.expand(out.crop((BORDA, BORDA, W - BORDA, H - BORDA)), border=BORDA, fill=(10, 10, 10))
    return out


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--folha", help="grava também uma folha com todos os cards")
    a = p.parse_args()
    os.makedirs(SAIDA, exist_ok=True)
    feitos = []
    for id_dupla in CARDS:
        im = card(id_dupla)
        im.save(os.path.join(SAIDA, id_dupla + ".jpg"), quality=88)
        feitos.append(im)
        print(os.path.join(SAIDA, id_dupla + ".jpg"))
    if a.folha:
        folha = Image.new("RGB", (W * 2, H * 4), (30, 30, 30))
        for i, im in enumerate(feitos):
            folha.paste(im, ((i % 2) * W, (i // 2) * H))
        folha.resize((W, H * 2)).save(a.folha, quality=85)


if __name__ == "__main__":
    main()
