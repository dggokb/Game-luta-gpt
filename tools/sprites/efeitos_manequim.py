#!/usr/bin/env python3
"""Efeito de dano do vídeo do manequim verde sobre fundo verde, direto para o jogo.

O manequim e o fundo são verdes; os efeitos não (branco, ciano, amarelo, vermelho...). A placa é
a mediana dos quadros sem efeito (o manequim parado, respirando). Em cada quadro, a parte verde
que sobrou diz o quanto a placa aparece por baixo:

    alfa = 1 - verde(quadro) / verde(placa)        verde(c) = (G - max(R, B)) / G

e a cor do efeito sai desfazendo a mistura com a placa (C = a·E + (1 - a)·P). A placa leva o
menor verde da vizinhança, então o manequim que se mexe um pouco não vira efeito; o contorno e o
brilho dele que mexem saem pelo escuro e pelas linhas finas dentro do corpo. Efeito verde some
junto com o manequim; efeito atrás do corpo fica com buraco. O que passa da borda do vídeo é
esfumado (sem o corte reto).

A escala vem do manequim (a altura dele vira --altura px do mundo, a de um lutador) e a origem é
o ponto do corpo que apanha (--ponto cabeca, tronco ou baixo, medidos no manequim, ou x,y do
vídeo): o jogo põe essa origem no mesmo ponto do corpo de quem apanha. O vídeo é de um golpe
para a esquerda (o spray sai para trás de quem olha para a direita): --espelhar deixa no padrão
do jogo (golpe para a direita; o jogo espelha o resto).

    python3 tools/sprites/efeitos_manequim.py cabeca.mp4 --nome dano_cabeca \\
        --quadros 80-104 --placa 30-75 --ponto cabeca --espelhar

Sai em android/app/src/main/assets/efeitos/<nome>/ (folha.webp e efeito.json, o formato do
tools/sprites/efeito_video.py) e a prévia em android/app/build/sprite-review/efeitos/<nome>.gif.
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

import efeito_video as EV

# Onde fica cada ponto no manequim: fração da altura dele, de cima.
PONTOS = {'cabeca': 0.09, 'tronco': 0.38, 'baixo': 0.55}


def faixa(texto):
    a, b = (int(v) for v in texto.split('-'))
    return range(a, b + 1)


def verde(img):
    g = img[..., 1]
    return (g - np.maximum(img[..., 0], img[..., 2])) / np.maximum(g, 1.0)


def ler(video):
    cap = cv2.VideoCapture(str(video))
    fps = cap.get(cv2.CAP_PROP_FPS) or 24.0
    frames = []
    while True:
        ok, f = cap.read()
        if not ok:
            break
        frames.append(f[..., ::-1].astype(np.float32))   # RGB
    return frames, fps


class Chave:
    """O recorte de um vídeo: placa, o verde dela e onde está o corpo do manequim."""

    def __init__(self, frames, placa, vizinhanca=9, escuro=90, minimo=0.06):
        self.placa = np.median(np.stack([frames[i] for i in placa]), 0)
        k = np.ones((vizinhanca, vizinhanca), np.uint8)
        self.vp = cv2.erode(verde(self.placa).astype(np.float32), k)
        # Onde a placa quase não é verde (contorno escuro, brilho) não dá para medir: a diferença
        # para a placa decide (efeito claro por cima do contorno ainda aparece).
        self.confiavel = self.vp > 0.25
        self.manequim = verde(self.placa) < 0.9
        # O corpo com folga para ele se mexer: ali as linhas finas são o contorno que mexeu.
        self.corpo = cv2.dilate(self.manequim.astype(np.uint8), k).astype(bool)
        self.escuro, self.minimo = escuro, minimo

    def rgba(self, c):
        alfa = np.clip(1.0 - verde(c) / np.maximum(self.vp, 1e-3), 0, 1)
        dif = np.abs(c - self.placa).max(2) / 255.0
        alfa = np.where(self.confiavel, alfa, np.clip((dif - 0.15) * 2.0, 0, 1))
        alfa[alfa < self.minimo] = 0
        aberto = cv2.morphologyEx(alfa.astype(np.float32), cv2.MORPH_OPEN, np.ones((5, 5), np.uint8))
        alfa = np.where(self.corpo, aberto, alfa)
        # Tira pontinhos soltos (ruído do vídeo).
        m = (alfa > 0.2).astype(np.uint8)
        n, lab, st, _ = cv2.connectedComponentsWithStats(m, connectivity=8)
        pequeno = st[:, cv2.CC_STAT_AREA] < 12
        pequeno[0] = False
        alfa[pequeno[lab]] = 0
        # Cor do efeito: desfaz a mistura com a placa.
        aa = np.maximum(alfa, 1e-3)[..., None]
        cor = np.clip((c - (1 - aa) * self.placa) / aa, 0, 255)
        # Sem verde sobrando na borda fina (o fundo que vaza): G no máximo o maior de R e B.
        cor[..., 1] = np.minimum(cor[..., 1], np.maximum(cor[..., 0], cor[..., 2]) + 12)
        if self.escuro > 0:
            # Efeito de dano é luz: o que sai escuro é o contorno do manequim que se mexeu.
            alfa = alfa * np.clip((cor.max(2) - self.escuro) / 40.0, 0, 1)
        return np.dstack([cor, alfa * 255]).astype(np.uint8)

    def ponto(self, nome):
        """O ponto do corpo no vídeo (x, y) e a altura do manequim."""
        ys, xs = np.nonzero(self.manequim)
        topo, pe = ys.min(), ys.max()
        y = topo + PONTOS[nome] * (pe - topo)
        linha = xs[np.abs(ys - y) < 6]
        return float(linha.mean()), float(y), float(pe - topo)


def esfumar_bordas(rgba, faixa_px):
    """O efeito que passa da borda do vídeo some aos poucos (sem a linha reta do corte)."""
    h, w = rgba.shape[:2]
    rx = np.clip(np.minimum(np.arange(w), w - 1 - np.arange(w)) / faixa_px, 0, 1)
    ry = np.clip(np.minimum(np.arange(h), h - 1 - np.arange(h)) / faixa_px, 0, 1)
    rgba[..., 3] = (rgba[..., 3] * np.minimum(ry[:, None], rx[None, :])).astype(np.uint8)
    return rgba


def converter(video, nome, quadros, placa, ponto='cabeca', altura=225.0, espelhar=False,
              passo=1, borda=70, escuro=90, resolucao=EV.PIXEL_SCALE, saida=EV.SAIDA, previa=EV.PREVIA):
    frames, fps = ler(video)
    chave = Chave(frames, faixa(placa), escuro=escuro)
    if ',' in ponto:
        ox, oy = (float(v) for v in ponto.split(','))
        ys = np.nonzero(chave.manequim)[0]
        alto = float(ys.max() - ys.min())
    else:
        ox, oy, alto = chave.ponto(ponto)
    rgba = [esfumar_bordas(chave.rgba(frames[i]), borda) for i in list(faixa(quadros))[::passo]]
    if espelhar:
        w = rgba[0].shape[1]
        rgba = [q[:, ::-1].copy() for q in rgba]
        ox = w - 1 - ox
    escala = altura * resolucao / alto
    recortados, desloc = [], []
    for q in rgba:
        c = EV.caixa(q[..., 3]) or (int(ox), int(oy), int(ox) + 1, int(oy) + 1)
        x0, y0, x1, y1 = c
        parte = q[y0:y1, x0:x1]
        w = max(1, round((x1 - x0) * escala))
        h = max(1, round((y1 - y0) * escala))
        # Redução com área e alfa pré-multiplicado (a borda não escurece).
        pre = parte.astype(np.float32)
        pre[..., :3] *= pre[..., 3:4] / 255.0
        pre = cv2.resize(pre, (w, h), interpolation=cv2.INTER_AREA)
        a = pre[..., 3:4] / 255.0
        pre[..., :3] = np.where(a > 1e-3, pre[..., :3] / np.maximum(a, 1e-3), 0)
        recortados.append(np.clip(pre, 0, 255).astype(np.uint8))
        desloc.append(((x0 - ox) * escala, (y0 - oy) * escala))
    folha, lugares = EV.empacotar(recortados)
    destino = Path(saida) / nome
    destino.mkdir(parents=True, exist_ok=True)
    Image.fromarray(folha, 'RGBA').save(destino / 'folha.webp', lossless=True, method=6)
    ms = round(1000.0 * passo / fps)
    dados = {
        'nome': nome,
        'video': Path(video).name,
        'trecho': quadros,
        'placa': placa,
        'passo': passo,
        'ancora': ponto,
        'manequim': True,
        'espelhado': espelhar,
        'pixelScale': resolucao,
        # Altura do manequim no mundo: o jogo escala pela altura de quem apanha.
        'altura': altura,
        'quadros': [[lx, ly, im.shape[1], im.shape[0], round(dx, 1), round(dy, 1)]
                    for (lx, ly), im, (dx, dy) in zip(lugares, recortados, desloc)],
        'duracoesMs': [ms] * len(recortados),
    }
    (destino / 'efeito.json').write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    EV.gif_previa(dados, recortados, Path(previa) / f'{nome}.gif')
    return destino, dados


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('video')
    p.add_argument('--nome', required=True)
    p.add_argument('--quadros', required=True, help='A-B: quadros do efeito')
    p.add_argument('--placa', required=True, help='A-B: quadros sem efeito (o manequim sozinho)')
    p.add_argument('--ponto', default='cabeca', help='cabeca, tronco, baixo ou x,y do vídeo')
    p.add_argument('--altura', type=float, default=225.0, help='altura do manequim no mundo (px)')
    p.add_argument('--espelhar', action='store_true', help='o vídeo é de um golpe para a esquerda')
    p.add_argument('--passo', type=int, default=1)
    p.add_argument('--borda', type=int, default=70, help='px do vídeo esfumados na borda')
    p.add_argument('--escuro', type=float, default=90,
                   help='cor mais escura que isso (0-255) é o contorno do manequim; 0 desliga (fumaça escura)')
    p.add_argument('--resolucao', type=float, default=EV.PIXEL_SCALE,
                   help='px da folha por px do mundo (menos: folha mais leve; o jogo desenha o dano menor)')
    a = p.parse_args()
    destino, dados = converter(a.video, a.nome, a.quadros, a.placa, a.ponto, a.altura, a.espelhar,
                               a.passo, a.borda, a.escuro, a.resolucao)
    print(f"{len(dados['quadros'])} quadros em {destino} (prévia em {EV.PREVIA / (a.nome + '.gif')})")


if __name__ == '__main__':
    main()
