#!/usr/bin/env python3
"""Monta folhas de sprite com poses soltas (temporárias) geradas no Codex, em fundo verde.

Cada pose é uma imagem do lutador inteiro, de lado e virado para a direita, num fundo verde liso
(art/temporarios/<conjunto>/<pose>.webp, com o prompt ao lado). O script tira o verde (a chave
do efeito_video.py), ajusta a escala pela altura do lutador parado no jogo, põe os pés na raiz do
quadro e grava a folha no formato "prepared-grid" do import_sprites.py (o clip em
tools/sprites/clips/ aponta para ela). Depois: python3 tools/sprites/build_characters.py --write.

São poses de referência: o desenho definitivo vem dos vídeos (video_para_sprite.py), que trocam
a folha sem mudar o jogo.

    python3 tools/sprites/poses_temporarias.py            # todas as folhas de CONJUNTOS
"""
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(ROOT, "tools", "sprites"))
import efeito_video as ev  # noqa: E402

# Altura do lutador parado (o 1º quadro do idle, em pixels da folha, pixelScale 2).
ALTURA_PARADO = {"player_base": 458}

# Folha -> (conjunto, quadro, raiz, poses). Cada pose: (arquivo, tamanho, apoio), com o tamanho
# como fração da altura do lutador parado: ("altura", f) mede a altura do desenho, ("largura", f)
# a largura (o voo, deitado). Apoio: "pe" (pés na raiz) ou ("ar", px) (a base do desenho px acima
# da raiz, para poses no ar).
CONJUNTOS = {
    "art/sprites/source/player_base_dragon_rush_temp.png": {
        "pack": "player_base",
        "pasta": "art/temporarios/shinkai_ryuga",
        "quadro": (768, 640), "raiz": (300, 610), "colunas": 4,
        "poses": [
            ("dr_preparo", ("altura", 0.80), "pe"),
            ("dr_voo", ("largura", 1.22), ("ar", 70)),
            ("dr_soco1", ("altura", 0.97), "pe"),
            ("dr_soco2", ("altura", 0.92), "pe"),
            ("dr_final_frente", ("altura", 0.90), "pe"),
            ("dr_final_cima", ("altura", 1.18), "pe"),
            ("dr_final_lado", ("altura", 0.97), "pe"),
        ],
    },
    "art/sprites/source/player_base_energy_shinkai_temp.png": {
        "pack": "player_base",
        "pasta": "art/temporarios/shinkai_ryuga",
        "quadro": (768, 640), "raiz": (300, 610), "colunas": 3,
        "poses": [
            ("sk_preparo", ("altura", 0.92), "pe"),
            ("sk_esq", ("altura", 0.93), "pe"),
            ("sk_dir", ("altura", 0.93), "pe"),
        ],
    },
}


def recortar(caminho):
    rgb = np.array(Image.open(caminho).convert("RGB"))
    rgba = ev.chave(rgb, ev.cor_do_fundo([rgb]))
    rgba[..., 3][rgba[..., 3] < 16] = 0
    im = Image.fromarray(rgba, "RGBA")
    return im.crop(im.getbbox())


def montar(destino, conjunto):
    qw, qh = conjunto["quadro"]
    rx, ry = conjunto["raiz"]
    cols = conjunto["colunas"]
    poses = conjunto["poses"]
    linhas = -(-len(poses) // cols)
    folha = Image.new("RGBA", (qw * cols, qh * linhas), (0, 0, 0, 0))
    parado = ALTURA_PARADO[conjunto["pack"]]
    for i, (nome, (medida, fracao), apoio) in enumerate(poses):
        im = recortar(os.path.join(ROOT, conjunto["pasta"], nome + ".webp"))
        k = parado * fracao / (im.height if medida == "altura" else im.width)
        im = im.resize((max(1, round(im.width * k)), max(1, round(im.height * k))), Image.LANCZOS)
        base = ry if apoio == "pe" else ry - apoio[1]
        # Os pés (a base do desenho) na raiz; na horizontal, o corpo começa um pouco atrás dela.
        x = rx - round(im.width * (0.40 if medida == "altura" else 0.35))
        x = max(0, min(qw - im.width, x))
        y = max(0, base - im.height)
        if im.width > qw or im.height > qh:
            raise SystemExit(f"{nome}: {im.size} não cabe no quadro {qw}x{qh}")
        cx, cy = (i % cols) * qw, (i // cols) * qh
        folha.alpha_composite(im, (cx + x, cy + y))
        print(f"  {nome}: {im.size}")
    folha.save(os.path.join(ROOT, destino), optimize=True)
    print(destino, folha.size)


def main():
    for destino, conjunto in CONJUNTOS.items():
        montar(destino, conjunto)


if __name__ == "__main__":
    main()
