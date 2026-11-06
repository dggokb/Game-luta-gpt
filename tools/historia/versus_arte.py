#!/usr/bin/env python3
"""Prepara as peças da tela de versus (historia/torneio/versus/) a partir das imagens do Codex.

A tela é uma montagem (TelaVersus): o templo de fundo, o espírito de cada irmão (o dragão azul do
Ryūga, o tigre vermelho do Kōga), os lutadores da cintura para cima, o "VS" e o logo. Os prompts
estão em docs/historia/prompts-versus.md.

- Lutadores (pNN.png) e o VS: fundo verde liso, tirado com a chave (despill) do efeito_video.py,
  recortados justos. Os lutadores ficam com 1100 px de altura.
- Dragão e tigre: energia brilhando sobre o preto. O brilho vira a transparência (o preto some,
  como numa mistura "tela").
- Templo: o fundo, cortado em 16:9 (1600×900, JPG).
- Logo: o logo do jogo (historia/logo.jpg), com o preto virando transparência.

    python3 tools/historia/versus_arte.py art/versus     # os originais do Codex guardados no repo
    python3 tools/historia/versus_arte.py <pasta>        # ou a pasta do Codex (p01/p01.png, ...)
"""
import os
import sys

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(AQUI))
sys.path.insert(0, os.path.join(ROOT, "tools", "sprites"))
import efeito_video as ev  # noqa: E402

HISTORIA = os.path.join(ROOT, "android", "app", "src", "main", "assets", "historia")
SAIDA = os.path.join(HISTORIA, "torneio", "versus")
LUTADORES = ["p%02d" % i for i in range(1, 17)]
ALTURA_LUTADOR = 1100


def chave_verde(caminho):
    rgb = np.array(Image.open(caminho).convert("RGB"))
    rgba = ev.chave(rgb, ev.cor_do_fundo([rgb]))
    rgba[..., 3][rgba[..., 3] < 12] = 0
    im = Image.fromarray(rgba, "RGBA")
    return im.crop(im.getbbox())


def brilho_sobre_preto(im, preto=14, cheio=190):
    """O preto vira transparente e o brilho fica: alfa pelo canal mais claro, cor desmisturada."""
    f = np.array(im.convert("RGB")).astype(np.float32)
    a = np.clip((f.max(-1) - preto) / (cheio - preto), 0, 1) ** 0.85
    cor = np.clip(f / np.maximum(a, 1e-3)[..., None], 0, 255)
    out = Image.fromarray(np.dstack([cor, a * 255]).astype(np.uint8), "RGBA")
    return out.crop(out.getbbox())


def main():
    pasta = sys.argv[1]
    os.makedirs(SAIDA, exist_ok=True)

    def origem(nome):
        # A pasta do Codex (uma pasta por peça) ou os originais guardados em art/versus/.
        for p in (os.path.join(pasta, nome, nome + ".png"), os.path.join(pasta, nome + ".webp"),
                  os.path.join(pasta, nome + ".png")):
            if os.path.exists(p):
                return p
        return None

    for n in LUTADORES:
        if not origem(n):
            print("sem", n)
            continue
        im = chave_verde(origem(n))
        k = ALTURA_LUTADOR / im.height
        im = im.resize((round(im.width * k), ALTURA_LUTADOR), Image.LANCZOS)
        im.save(os.path.join(SAIDA, n + ".png"), optimize=True)
        print(n, im.size)
    if origem("vs"):
        im = chave_verde(origem("vs"))
        im.thumbnail((640, 640), Image.LANCZOS)
        im.save(os.path.join(SAIDA, "vs.png"), optimize=True)
        print("vs", im.size)
    for n in ("dragao", "tigre"):
        if origem(n):
            im = brilho_sobre_preto(Image.open(origem(n)))
            im.thumbnail((1100, 1100), Image.LANCZOS)
            im.save(os.path.join(SAIDA, n + ".png"), optimize=True)
            print(n, im.size)
    if origem("templo"):
        im = Image.open(origem("templo")).convert("RGB")
        w, h = im.size
        alvo_h = round(w * 9 / 16)
        topo = max(0, (h - alvo_h) // 2)
        im = im.crop((0, topo, w, topo + alvo_h)).resize((1600, 900), Image.LANCZOS)
        im.save(os.path.join(SAIDA, "templo.jpg"), quality=88)
        print("templo", im.size)
    logo = brilho_sobre_preto(Image.open(os.path.join(HISTORIA, "logo.jpg")), preto=18, cheio=120)
    logo.thumbnail((700, 700), Image.LANCZOS)
    logo.save(os.path.join(SAIDA, "logo.png"), optimize=True)
    print("logo", logo.size)


if __name__ == "__main__":
    main()
