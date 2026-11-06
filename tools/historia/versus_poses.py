#!/usr/bin/env python3
"""Grava a pose de cada lutador para a tela de versus do torneio (historia/torneio/versus/pNN.png).

Por enquanto a pose vem dos sprites do jogo: o último quadro da animação de entrada (intro), que
é a guarda de luta virada para a direita; quem ainda não tem intro (p13 a p16) usa o primeiro
quadro do idle. O recorte vai justo no desenho, com os pés na base da imagem, em PNG transparente, todos na mesma
escala (2 pixels por pixel do mundo): o tamanho relativo dos lutadores é o do jogo.

Para trocar por uma ilustração (a arte gerada da tela de versus), grave a imagem no mesmo nome:
PNG transparente, o lutador inteiro, virado para a direita, com os pés na borda de baixo e na
mesma escala (um lutador de altura comum com uns 470 pixels). O jogo não precisa de mudança.

    python3 tools/historia/versus_poses.py
"""
import os
import re

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FONTE = os.path.join(ROOT, "android", "app", "src", "main", "java", "com", "gamelutagpt", "GeneratedCharacters.java")
RES = os.path.join(ROOT, "android", "app", "src", "main", "res", "drawable-nodpi")
SAIDA = os.path.join(ROOT, "android", "app", "src", "main", "assets", "historia", "torneio", "versus")

# Pixels da imagem por pixel do mundo (o pixelScale dos sprites grandes); os de 1.0 dobram.
DENSIDADE = 2.0
# Código do retrato (pNN) -> pack de luta.
PACKS = {"p01": "player_base", **{"p%02d" % i: "p%02d" % i for i in range(2, 17)}}


def atlas(texto, nome):
    m = re.search(r'new CharacterDefinition\.Atlas\("%s",(\d+),(\d+),(\d+),(\d+),(\d+),(\d+),([\d.]+)f,(\d+),(\d+),"([0-9,]+)"'
                  % re.escape(nome), texto)
    if not m:
        return None
    cw, ch, ps = int(m.group(1)), int(m.group(2)), float(m.group(7))
    nums = list(map(int, m.group(10).split(",")))
    return cw, ch, ps, [nums[i:i + 6] for i in range(0, len(nums), 6)]


def pose(texto, pack):
    for anim, qual in (("intro", -1), ("idle", 0)):
        a = atlas(texto, pack + "_" + anim)
        if a:
            cw, ch, ps, quadros = a
            x, y, w, h, ox, oy = quadros[qual]
            folha = Image.open(os.path.join(RES, pack + "_" + anim + ".webp")).convert("RGBA")
            cela = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
            cela.paste(folha.crop((x, y, x + w, y + h)), (ox, oy))
            im = cela.crop(cela.getbbox())
            if ps != DENSIDADE:   # todos na mesma escala: o tamanho relativo dos lutadores fica certo
                im = im.resize((round(im.width * DENSIDADE / ps), round(im.height * DENSIDADE / ps)), Image.LANCZOS)
            return im, anim
    raise SystemExit("sem intro nem idle: " + pack)


def main():
    texto = open(FONTE, encoding="utf-8").read()
    os.makedirs(SAIDA, exist_ok=True)
    for codigo, pack in PACKS.items():
        im, anim = pose(texto, pack)
        destino = os.path.join(SAIDA, codigo + ".png")
        im.save(destino, optimize=True)
        print(destino, im.size, anim)


if __name__ == "__main__":
    main()
