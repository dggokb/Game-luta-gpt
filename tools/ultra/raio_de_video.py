#!/usr/bin/env python3
"""Corpo e ponta do raio do Ultra, animados, a partir de um vídeo de feixe em fundo preto.

O raio do Ultra (RaioFinal) aceita corpo, ponta e esfera em tiras de N quadros, um embaixo do
outro (ultra.json: "quadros", "qps"). Daqui saem as tiras do corpo e da ponta: em cada quadro
escolhido do vídeo, a frente do feixe é achada sozinha e o quadro é alinhado por ela (o feixe do
vídeo anda e o do jogo não); a janela fica centrada no núcleo. A ponta é a cabeça do feixe (a
borda esquerda some para nascer de dentro do corpo); o corpo é uma fatia da cauda, atrás da
ponta, com o fim fundido no começo (repete sem emenda). A esfera (inicio) e o impacto ficam.

    python3 tools/ultra/raio_de_video.py SUPER_PODER.mp4 --id player_base --quadros 40-62:2

    --quadros      quadros do vídeo (contados de 0): "40-62:2" ou soltos "40,42,44"
    --janela       altura da janela em px do vídeo (o feixe com as chamas; padrão 520)
    --altura-tira  altura de cada quadro na tira (padrão 340, a do raio de antes)
    --ponta        largura da ponta em px do vídeo, contada da frente para trás (padrão 520)
    --folga        px do vídeo à frente da frente do feixe, dentro da ponta (padrão 90)
    --corpo        largura da fatia do corpo em px do vídeo (padrão 490)
    --atras        onde a fatia do corpo começa, em px do vídeo atrás da frente (padrão 1050)
"""
import argparse
import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

AQUI = Path(__file__).resolve().parent
sys.path.insert(0, str(AQUI))
import preparar_raio as pr  # noqa: E402

RAIZ = AQUI.parents[1]
ULTRAS = RAIZ / 'android/app/src/main/assets/ultras'


def lista(texto):
    saida = []
    for parte in texto.split(','):
        if '-' in parte:
            faixa, _, passo = parte.partition(':')
            a, b = (int(v) for v in faixa.split('-'))
            saida.extend(range(a, b + 1, int(passo or 1)))
        else:
            saida.append(int(parte))
    return saida


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('video')
    p.add_argument('--id', required=True)
    p.add_argument('--quadros', required=True)
    p.add_argument('--janela', type=int, default=520)
    p.add_argument('--altura-tira', type=int, default=340)
    p.add_argument('--ponta', type=int, default=520)
    p.add_argument('--folga', type=int, default=90)
    p.add_argument('--corpo', type=int, default=490)
    p.add_argument('--atras', type=int, default=1050)
    p.add_argument('--qps', type=float, default=12.0)
    a = p.parse_args()

    cap = cv2.VideoCapture(a.video)
    todos = []
    while True:
        ok, q = cap.read()
        if not ok:
            break
        todos.append(cv2.cvtColor(q, cv2.COLOR_BGR2RGB))
    escolhidos = [todos[i] for i in lista(a.quadros)]
    k = a.altura_tira / float(a.janela)
    pontas, corpos = [], []
    for q in escolhidos:
        luz = q.max(axis=2)
        xs = np.nonzero((luz > 60).any(axis=0))[0]
        frente = int(xs.max())
        quente = np.nonzero((luz > 240).any(axis=1))[0]
        cy = int(round(quente.mean()))
        y0, y1 = cy - a.janela // 2, cy + a.janela // 2
        im = pr.para_alfa(Image.fromarray(q))
        # Fora do vídeo é transparente (a janela pode passar da borda).
        tela = Image.new('RGBA', (q.shape[1] + 2 * a.atras, a.janela), (0, 0, 0, 0))
        tela.paste(im.crop((0, max(0, y0), q.shape[1], min(q.shape[0], y1))), (a.atras, max(0, -y0)))
        fx = frente + a.atras
        ponta = tela.crop((fx + a.folga - a.ponta, 0, fx + a.folga, a.janela))
        ponta = pr.apagar_esquerda(ponta, 0.30)
        corpo = tela.crop((fx - a.atras, 0, fx - a.atras + a.corpo, a.janela))
        corpo = pr.sem_emenda(corpo)
        pontas.append(ponta.resize((round(ponta.width * k), a.altura_tira), Image.LANCZOS))
        corpos.append(corpo.resize((round(corpo.width * k), a.altura_tira), Image.LANCZOS))

    destino = ULTRAS / a.id
    for nome, partes in (('raio_ponta.png', pontas), ('raio_corpo.png', corpos)):
        w = max(p_.width for p_ in partes)
        tira = Image.new('RGBA', (w, a.altura_tira * len(partes)), (0, 0, 0, 0))
        for i, p_ in enumerate(partes):
            tira.paste(p_, (0, i * a.altura_tira))
        tira.save(destino / nome, optimize=True)
    # A esfera (inicio) precisa do mesmo número de quadros: se o número mudou, ela repete os dela.
    cfg = destino / 'ultra.json'
    dados = json.loads(cfg.read_text(encoding='utf-8'))
    raio = dados.setdefault('raio', {})
    antes = int(raio.get('quadros', 1))
    if antes != len(escolhidos):
        inicio = destino / raio.get('inicio', 'raio_inicio.png')
        if inicio.exists():
            img = Image.open(inicio)
            fh = img.height // antes
            nova = Image.new('RGBA', (img.width, fh * len(escolhidos)), (0, 0, 0, 0))
            for i in range(len(escolhidos)):
                j = i % antes
                nova.paste(img.crop((0, j * fh, img.width, (j + 1) * fh)), (0, i * fh))
            nova.save(inicio, optimize=True)
    raio['quadros'] = len(escolhidos)
    raio['qps'] = a.qps
    cfg.write_text(json.dumps(dados, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'{len(escolhidos)} quadros: ponta {pontas[0].size}, corpo {corpos[0].size} cada')


if __name__ == '__main__':
    main()
