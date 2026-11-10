#!/usr/bin/env python3
"""Vídeo de efeito luminoso em FUNDO PRETO (faísca, explosão) -> folha de efeito do jogo.

O efeito_video.py é para vídeo no verde. Efeito de luz gerado em fundo preto (os vídeos de dano de
art/vfx/dano) passa primeiro por aqui: os quadros escolhidos viram PNGs transparentes (o brilho
vira transparência, como no efeito_imagem.py) e daí seguem para o efeito_video.py, que empacota.

    python3 tools/sprites/efeito_video_preto.py art/vfx/dano/dano_medio.mp4 --nome impacto_ar \\
        --quadros 0-32:3 --origem 606,384 --escala-mundo 0.25

    --quadros   quais quadros do vídeo, na ordem: "0-32:3" (de 0 a 32, um a cada 3) e/ou soltos,
                separados por vírgula: "0,1,2,6,48-84:6"
    --borda N   o efeito some aos poucos nos N px da beira do quadro (brilho cortado pelo vídeo)
    O resto (--origem, --escala-mundo, --altura, --ancora, --fps) vai direto para o efeito_video.py.
"""
import argparse
import subprocess
import sys
import tempfile
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import efeito_imagem as ei  # noqa: E402


def lista(texto):
    quadros = []
    for parte in texto.split(','):
        if '-' in parte:
            faixa, _, passo = parte.partition(':')
            a, b = faixa.split('-')
            quadros += list(range(int(a), int(b) + 1, int(passo or 1)))
        else:
            quadros.append(int(parte))
    return quadros


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('video')
    p.add_argument('--nome', required=True)
    p.add_argument('--quadros', required=True)
    p.add_argument('--borda', type=int, default=0)
    a, resto = p.parse_known_args()
    cap = cv2.VideoCapture(a.video)
    todos = []
    while True:
        ok, q = cap.read()
        if not ok:
            break
        todos.append(cv2.cvtColor(q, cv2.COLOR_BGR2RGB))
    with tempfile.TemporaryDirectory(prefix='efeito-preto-') as pasta:
        for n, i in enumerate(lista(a.quadros)):
            rgba = ei.recortar_fundo(Image.fromarray(todos[min(i, len(todos) - 1)]), 'preto')
            if a.borda:
                alto, largo = rgba.shape[:2]
                yy, xx = np.mgrid[0:alto, 0:largo]
                beira = np.minimum.reduce([xx, yy, largo - 1 - xx, alto - 1 - yy]) / float(a.borda)
                rgba[..., 3] *= np.clip(beira, 0.0, 1.0)
            Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), 'RGBA').save(Path(pasta) / f'q{n:03d}.png')
        subprocess.run([sys.executable, str(HERE / 'efeito_video.py'), pasta, '--nome', a.nome, *resto], check=True)


if __name__ == '__main__':
    main()
