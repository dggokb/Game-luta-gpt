#!/usr/bin/env python3
"""O estouro de luz atrás do STAMP: as imagens geradas (luz branca em fundo preto, com um
retângulo vazio no meio onde o quadro encaixa) viram as imagens com transparência que a
cinemática desenha atrás do quadro, alternando entre elas.

    python3 tools/cenas/stamp_estouro.py art/vfx/stamp_estouro_01.png art/vfx/stamp_estouro_02.png

Grava android/app/src/main/assets/historia/stamp/estouro_N.webp e estouro.json, com o buraco do
meio de cada uma ({x0, y0, x1, y1} de 0 a 1): o jogo estica a imagem para o buraco cair em cima
do quadro.
"""
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/sprites'))
from efeito_imagem import recortar_fundo  # noqa: E402

DESTINO = ROOT / 'android/app/src/main/assets/historia/stamp'
LARGURA = 1600


def buraco(alfa):
    """O retângulo escuro do meio: de cada lado, até onde a luz quase não chega (linhas e colunas
    que passam pelo centro)."""
    h, w = alfa.shape
    cy, cx = h // 2, w // 2
    faixa_y = alfa[cy - h // 10:cy + h // 10, :].mean(axis=0)
    faixa_x = alfa[:, cx - w // 10:cx + w // 10].mean(axis=1)
    escuro = 0.08

    def lado(perfil, centro, passo):
        i = centro
        while 0 < i < len(perfil) - 1 and perfil[i] < escuro:
            i += passo
        return i

    x0, x1 = lado(faixa_y, cx, -1), lado(faixa_y, cx, 1)
    y0, y1 = lado(faixa_x, cy, -1), lado(faixa_x, cy, 1)
    return [round(x0 / w, 4), round(y0 / h, 4), round(x1 / w, 4), round(y1 / h, 4)]


def main():
    DESTINO.mkdir(parents=True, exist_ok=True)
    buracos = []
    for n, arquivo in enumerate(sys.argv[1:], 1):
        imagem = Image.open(arquivo)
        if imagem.width > LARGURA:
            imagem = imagem.resize((LARGURA, round(imagem.height * LARGURA / imagem.width)), Image.LANCZOS)
        rgba = recortar_fundo(imagem, 'preto')
        b = buraco(rgba[..., 3])
        buracos.append(b)
        saida = Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), 'RGBA')
        saida.save(DESTINO / f'estouro_{n}.webp', quality=88)
        print(f'estouro_{n}.webp: {saida.width}x{saida.height}, buraco {b}')
    (DESTINO / 'estouro.json').write_text(json.dumps({
        '_sobre': 'O estouro de luz atrás do STAMP (tools/cenas/stamp_estouro.py): as imagens, na ordem em '
                  'que alternam, e o buraco do meio de cada uma ({x0, y0, x1, y1} de 0 a 1), onde o quadro encaixa.',
        'imagens': [f'estouro_{n}.webp' for n in range(1, len(buracos) + 1)],
        'buracos': buracos}, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
