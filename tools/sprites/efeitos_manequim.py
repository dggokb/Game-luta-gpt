#!/usr/bin/env python3
"""Tira só os efeitos de um vídeo do manequim verde sobre fundo verde (dano, estouro, vento).

O manequim e o fundo são verdes; os efeitos não (branco, ciano, amarelo, vermelho...). A placa é
a mediana dos quadros sem efeito (o manequim parado, respirando). Em cada quadro, a parte verde
que sobrou diz o quanto a placa aparece por baixo:

    alfa = 1 - verde(quadro) / verde(placa)        verde(c) = (G - max(R, B)) / G

e a cor do efeito sai desfazendo a mistura com a placa (C = a·E + (1 - a)·P). A placa leva o
menor verde da vizinhança, então o manequim que se mexe um pouco não vira efeito. Efeito verde
some junto com o manequim; efeito atrás do corpo fica com buraco.

    python3 tools/sprites/efeitos_manequim.py video.mp4 SAIDA --quadros 80-104 --placa 30-75

Grava SAIDA/efeito_NNN.png (RGBA, recortados na caixa de todos os quadros), SAIDA/folha.png
e SAIDA/previa.png (sobre fundo escuro) e SAIDA/efeito.json (caixa, centro no vídeo).
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np


def faixa(texto):
    a, b = (int(v) for v in texto.split('-'))
    return range(a, b + 1)


def verde(img):
    g = img[..., 1]
    return (g - np.maximum(img[..., 0], img[..., 2])) / np.maximum(g, 1.0)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('video')
    p.add_argument('saida')
    p.add_argument('--quadros', required=True, help='A-B: quadros do efeito')
    p.add_argument('--placa', required=True, help='A-B: quadros sem efeito (o manequim sozinho)')
    p.add_argument('--vizinhanca', type=int, default=9, help='px: o quanto o manequim se mexe')
    p.add_argument('--minimo', type=float, default=0.06, help='alfa abaixo disso vira 0 (ruído)')
    p.add_argument('--escuro', type=float, default=90,
                   help='cor do efeito mais escura que isso (0-255) é o contorno do manequim que mexeu; 0 desliga (fumaça escura)')
    a = p.parse_args()

    cap = cv2.VideoCapture(a.video)
    frames = []
    while True:
        ok, f = cap.read()
        if not ok:
            break
        frames.append(f[..., ::-1].astype(np.float32))   # RGB
    placa = np.median(np.stack([frames[i] for i in faixa(a.placa)]), 0)
    k = np.ones((a.vizinhanca, a.vizinhanca), np.uint8)
    vp = cv2.erode(verde(placa).astype(np.float32), k)
    # Onde a placa quase não é verde (contorno escuro, brilho) não dá para medir: a diferença
    # para a placa decide (efeito claro por cima do contorno ainda aparece).
    confiavel = vp > 0.25
    # O corpo do manequim (o que não é fundo na placa, com folga para ele se mexer): ali as linhas
    # finas são o contorno e o brilho dele que mexeram, não efeito.
    corpo = cv2.dilate((verde(placa) < 0.9).astype(np.uint8), k).astype(bool)

    out = Path(a.saida)
    out.mkdir(parents=True, exist_ok=True)
    rgba = []
    for i in faixa(a.quadros):
        c = frames[i]
        alfa = np.clip(1.0 - verde(c) / np.maximum(vp, 1e-3), 0, 1)
        dif = np.abs(c - placa).max(2) / 255.0
        alfa = np.where(confiavel, alfa, np.clip((dif - 0.15) * 2.0, 0, 1))
        alfa[alfa < a.minimo] = 0
        aberto = cv2.morphologyEx(alfa.astype(np.float32), cv2.MORPH_OPEN, np.ones((5, 5), np.uint8))
        alfa = np.where(corpo, aberto, alfa)
        # Tira pontinhos soltos (ruído do vídeo).
        m = (alfa > 0.2).astype(np.uint8)
        n, lab, st, _ = cv2.connectedComponentsWithStats(m, connectivity=8)
        pequeno = np.isin(lab, [j for j in range(1, n) if st[j, cv2.CC_STAT_AREA] < 12])
        alfa[pequeno] = 0
        # Cor do efeito: desfaz a mistura com a placa.
        aa = np.maximum(alfa, 1e-3)[..., None]
        cor = np.clip((c - (1 - aa) * placa) / aa, 0, 255)
        # Sem verde sobrando na borda fina (o fundo que vaza): G no máximo o maior de R e B.
        cor[..., 1] = np.minimum(cor[..., 1], np.maximum(cor[..., 0], cor[..., 2]) + 12)
        if a.escuro > 0:
            # Efeito de dano é luz: o que sai escuro é o contorno do manequim que se mexeu.
            alfa = alfa * np.clip((cor.max(2) - a.escuro) / 40.0, 0, 1)
        rgba.append(np.dstack([cor, alfa * 255]).astype(np.uint8))

    alfas = np.stack([f[..., 3] for f in rgba])
    ys, xs = np.nonzero(alfas.max(0) > 8)
    x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
    for n, f in enumerate(rgba):
        cv2.imwrite(str(out / f'efeito_{n:03d}.png'), cv2.cvtColor(f[y0:y1, x0:x1], cv2.COLOR_RGBA2BGRA))
    corte = [f[y0:y1, x0:x1] for f in rgba]
    cols = min(8, len(corte))
    lin = -(-len(corte) // cols)
    h, w = y1 - y0, x1 - x0
    folha = np.zeros((lin * h, cols * w, 4), np.uint8)
    for n, f in enumerate(corte):
        folha[n // cols * h:(n // cols + 1) * h, n % cols * w:(n % cols + 1) * w] = f
    cv2.imwrite(str(out / 'folha.png'), cv2.cvtColor(folha, cv2.COLOR_RGBA2BGRA))
    fundo = np.zeros_like(folha[..., :3], np.float32) + (40, 40, 60)
    aa = folha[..., 3:4] / 255.0
    previa = (folha[..., :3] * aa + fundo * (1 - aa)).astype(np.uint8)
    cv2.imwrite(str(out / 'previa.png'), cv2.cvtColor(previa, cv2.COLOR_RGB2BGR))
    (out / 'efeito.json').write_text(json.dumps({
        'video': Path(a.video).name, 'quadros': a.quadros, 'placa': a.placa, 'colunas': cols,
        'caixa': [int(x0), int(y0), int(x1), int(y1)], 'celula': [int(w), int(h)]}, indent=2) + '\n')
    print(f'{len(corte)} quadros, caixa {x0},{y0}-{x1},{y1} ({w}x{h}) em {out}')


if __name__ == '__main__':
    main()
