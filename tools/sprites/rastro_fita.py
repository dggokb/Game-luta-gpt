#!/usr/bin/env python3
"""A textura da fita do rastro do golpe (assets/efeitos/rastro_fita).

Entra a fita RETA gerada no GPT/Codex em fundo preto (art/vfx/arco_textura_01.png): nasce fina à
esquerda (a ponta velha do rastro) e chega à grossura cheia à direita (a ponta nova, onde está o
punho ou o pé), com a borda de cima nítida. O jogo dobra ela ao longo da linha de ação de cada
golpe (RastroDoGolpe, EfeitosVideo.fita) e pinta da cor do membro: por isso ela é gravada nas
cores que o jogo sabe pintar (efeito_imagem.tingivel).

    python3 tools/sprites/rastro_fita.py art/vfx/arco_textura_01.png
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import efeito_imagem as ei  # noqa: E402

LARGURA, ALTURA = 512, 96   # px da folha: o jogo estica no caminho, não precisa de mais


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('textura')
    p.add_argument('--nome', default='rastro_fita')
    a = p.parse_args()
    rgba = ei.tingivel(ei.recortar_fundo(Image.open(a.textura), 'preto'))
    ys, xs = np.nonzero(rgba[..., 3] > 0.04)
    rgba = rgba[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    fita = Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), 'RGBA').resize((LARGURA, ALTURA), Image.LANCZOS)
    pasta = ei.SAIDA / a.nome
    pasta.mkdir(parents=True, exist_ok=True)
    fita.save(pasta / 'folha.webp', lossless=True, quality=100, method=6)
    dados = {'nome': a.nome, 'origem': f'tools/sprites/rastro_fita.py ({Path(a.textura).name})', 'ancora': 'fita',
             'pixelScale': 1.0, 'altura': ALTURA, 'quadros': [[0, 0, LARGURA, ALTURA, 0.0, 0.0]], 'duracoesMs': [1000]}
    (pasta / 'efeito.json').write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(a.nome, '->', pasta.relative_to(ei.ROOT), fita.size)


if __name__ == '__main__':
    main()
