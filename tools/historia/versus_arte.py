#!/usr/bin/env python3
"""Põe as ilustrações geradas (fundo verde) na tela de versus: historia/torneio/versus/pNN.png.

Cada ilustração (do Codex, com os prompts de docs/historia/prompts-versus.md) vem num fundo verde
liso. O script tira o verde (a chave com despill do efeito_video.py), recorta justo no desenho e
ajusta a escala pela altura do lutador no jogo (o quadro parado do idle): o Krüger continua mais
alto que a Yin-lu, como na luta. A densidade é a da tela de versus (TelaVersus): 4 pixels da
imagem por pixel do mundo.

    python3 tools/historia/versus_arte.py <pasta com pNN.png> [p01,p02,...]
"""
import os
import sys

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(AQUI))
sys.path.insert(0, os.path.join(ROOT, "tools", "sprites"))
sys.path.insert(0, AQUI)
import efeito_video as ev  # noqa: E402
import versus_poses as vp  # noqa: E402

DENSIDADE = 4.0
# A ilustração tem a pose de luta (mais baixa que o idle, em pé): a altura dela é esta fração da
# altura do idle.
GUARDA = 0.97


def altura_no_mundo(texto, pack):
    """Altura do lutador parado (idle, 1º quadro) em pixels do mundo."""
    cw, ch, ps, quadros = vp.atlas(texto, pack + "_idle")
    x, y, w, h, ox, oy = quadros[0]
    folha = Image.open(os.path.join(vp.RES, pack + "_idle.webp")).convert("RGBA")
    caixa = folha.crop((x, y, x + w, y + h)).getbbox()
    return (caixa[3] - caixa[1]) / ps


def recortar(caminho):
    rgb = np.array(Image.open(caminho).convert("RGB"))
    fundo = ev.cor_do_fundo([rgb])
    rgba = ev.chave(rgb, fundo)
    a = rgba[..., 3]
    a[a < 12] = 0
    im = Image.fromarray(rgba, "RGBA")
    return im.crop(im.getbbox())


def main():
    pasta = sys.argv[1]
    alvos = sys.argv[2].split(",") if len(sys.argv) > 2 else sorted(vp.PACKS)
    texto = open(vp.FONTE, encoding="utf-8").read()
    os.makedirs(vp.SAIDA, exist_ok=True)
    for codigo in alvos:
        origem = os.path.join(pasta, codigo + ".png")
        if not os.path.exists(origem):
            print("sem", origem)
            continue
        im = recortar(origem)
        alvo = altura_no_mundo(texto, vp.PACKS[codigo]) * GUARDA * DENSIDADE
        k = alvo / im.height
        im = im.resize((round(im.width * k), round(alvo)), Image.LANCZOS)
        destino = os.path.join(vp.SAIDA, codigo + ".png")
        im.save(destino, optimize=True)
        print(destino, im.size)


if __name__ == "__main__":
    main()
