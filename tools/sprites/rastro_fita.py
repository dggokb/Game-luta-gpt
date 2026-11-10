#!/usr/bin/env python3
"""A textura da fita do rastro do golpe (assets/efeitos/rastro_fita).

Entra a fita RETA gerada no GPT/Codex em fundo preto (art/vfx/arco_textura_04.png): nasce fina à
esquerda (a ponta velha do rastro) e chega à grossura cheia à direita (a ponta nova, onde está o
punho ou o pé). O jogo dobra ela ao longo da linha de ação de cada golpe, numa malha só
(RastroDoGolpe, EfeitosVideo.fita). Sai nas cores dela: a malha não pinta.

    python3 tools/sprites/rastro_fita.py art/vfx/arco_textura_04.png
"""
import argparse
import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import efeito_imagem as ei  # noqa: E402

LARGURA, ALTURA = 1024, 192   # px da folha: os riscos finos precisam de resolução


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('textura')
    p.add_argument('--nome', default='rastro_fita')
    p.add_argument('--veu', type=float, default=0.5,
                   help='expoente da opacidade: menor que 1 levanta o véu meio transparente sem estourar os riscos')
    a = p.parse_args()
    rgba = ei.recortar_fundo(Image.open(a.textura), 'preto')
    ys, xs = np.nonzero(rgba[..., 3] > 0.04)
    rgba = rgba[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    # Reduz por média com a cor multiplicada pela opacidade (sem repique na borda dos riscos). O
    # expoente levanta o véu meio transparente sem estourar os riscos, que já são opacos.
    menor = lambda m: cv2.resize(m.astype(np.float32), (LARGURA, ALTURA), interpolation=cv2.INTER_AREA)
    alfa = menor(rgba[..., 3])
    cor = np.dstack([menor(rgba[..., c] * rgba[..., 3]) for c in range(3)]) / np.maximum(alfa, 1e-4)[..., None]
    # Uma linha transparente em cima e embaixo: a malha repete a imagem fora dela.
    alfa[0, :] = alfa[-1, :] = 0.0
    rgba = np.dstack([np.clip(cor, 0.0, 1.0), np.clip(alfa, 0.0, 1.0) ** a.veu])
    fita = Image.fromarray((rgba * 255).round().astype(np.uint8), 'RGBA')
    pasta = ei.SAIDA / a.nome
    pasta.mkdir(parents=True, exist_ok=True)
    fita.save(pasta / 'folha.webp', lossless=True, quality=100, method=6)
    dados = {'nome': a.nome, 'origem': f'tools/sprites/rastro_fita.py ({Path(a.textura).name})', 'ancora': 'fita',
             'pixelScale': 1.0, 'altura': ALTURA, 'quadros': [[0, 0, LARGURA, ALTURA, 0.0, 0.0]], 'duracoesMs': [1000]}
    (pasta / 'efeito.json').write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(a.nome, '->', pasta.relative_to(ei.ROOT), fita.size)


if __name__ == '__main__':
    main()
