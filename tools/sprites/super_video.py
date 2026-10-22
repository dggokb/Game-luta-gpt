#!/usr/bin/env python3
"""Converte o vídeo da entrada do Super (o retrato no verde) nos quadros que o jogo mostra ao
fundo enquanto o lutador carrega o Super.

O vídeo é o lutador grande, de perto, entrando por um lado do quadro (o padrão: pela
ESQUERDA, olhando para a direita). No jogo o quadro inteiro do vídeo fica com a altura da tela,
grudado no canto de baixo do lado em que o lutador está; do lado direito ele é espelhado.

    pip install -r tools/sprites/requirements-animar.txt
    python3 tools/sprites/super_video.py Entrada_Esquerda_Direita.mp4 --personagem p15 \\
        --inicio 4 --fim 44 --passo 2 --fps 18 --marca 8,8,150,50 --altura 0.85

    --inicio/--fim    trecho do vídeo (quadros, inclusive)
    --passo N         um quadro a cada N do vídeo (vídeo de 24 fps com quadros repetidos aos
                      pares: --passo 2 pega cada desenho uma vez)
    --fps F           velocidade no jogo (18 com --passo 2 = o vídeo 1,5x mais rápido)
    --segurar MS      o último quadro fica parado esse tempo antes de sair
    --saida MS        o retrato volta para a borda e some nesse tempo; aí o Super sai
    --marca x0,y0,x1,y1   apaga a marca d'água clara nesse retângulo do vídeo (o cabelo escuro
                      que passa por cima fica)
    --altura A        o quadro do vídeo com A da altura da tela (no jogo o painel de mangá decide
                      o tamanho; isto é a resolução guardada: a p15 usa 0,85)
    --resolucao R     pixels guardados por pixel da tela (0,65: um quadro do tamanho da tela
                      ocupa 832x468; menos memória, ainda nítido ao fundo)

Sai em android/app/src/main/assets/supers/<personagem>/: q00.webp, q01.webp... (cada quadro
recortado no que tem desenho) e super.json (onde cada um fica e o tempo). O motor congela a
luta pelo tempo total (quadros + segurar + saída) e só então o Super continua.
"""
import argparse
import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from efeito_video import caixa, chave, cor_do_fundo, ler_quadros  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
SAIDA = ROOT / 'android/app/src/main/assets/supers'
PREVIA = ROOT / 'android/app/build/sprite-review/supers'
# A tela do jogo (Arena.VW x Arena.VH).
TELA_L, TELA_A = 1280, 720
# Na marca d'água, o que for mais claro que isso some (o cabelo preto que passa por cima fica).
MARCA_CLARO = 110
# Manchas soltas menores que isso (px do vídeo), ou sem ponto firme, são ruído do verde.
MANCHA_MINIMA = 150


def apagar_marca(rgba, marca):
    x0, y0, x1, y1 = marca
    area = rgba[y0:y1, x0:x1]
    claro = area[..., :3].astype(np.int32).min(axis=-1) > MARCA_CLARO
    # A borda das letras (misturada com o verde) também sai.
    claro = cv2.dilate(claro.astype(np.uint8), np.ones((3, 3), np.uint8)) > 0
    area[claro, 3] = 0


def tirar_manchas(rgba):
    n, rotulos, stats, _ = cv2.connectedComponentsWithStats((rgba[..., 3] > 0).astype(np.uint8))
    # Pequena, ou sem nenhum ponto firme (a mancha clara da compressão no verde): ruído.
    pico = np.zeros(n, np.int32)
    np.maximum.at(pico, rotulos.ravel(), rgba[..., 3].ravel().astype(np.int32))
    miudos = (stats[:, cv2.CC_STAT_AREA] < MANCHA_MINIMA) | (pico < 128)
    miudos[0] = False
    rgba[miudos[rotulos], 3] = 0


def converter(video, personagem, inicio=0, fim=None, passo=1, fps=None, segurar_ms=0, saida_ms=150,
              marca=None, resolucao=0.65, altura=1.0, saida=SAIDA, previa=PREVIA):
    quadros, fps_video = ler_quadros(video, inicio, fim, passo)
    fundo = cor_do_fundo(quadros)
    rgba = [chave(q, fundo) for q in quadros]
    if marca:
        for q in rgba:
            apagar_marca(q, marca)
    for q in rgba:
        tirar_manchas(q)
    h_video, w_video = quadros[0].shape[:2]
    # O quadro do vídeo inteiro com `altura` da tela; a folha guarda `resolucao` px por px da tela.
    tela_por_video = altura * TELA_A / h_video
    escala = tela_por_video * resolucao
    destino = Path(saida) / personagem
    destino.mkdir(parents=True, exist_ok=True)
    for velho in destino.glob('q*.webp'):
        velho.unlink()
    lista, imagens = [], []
    for i, q in enumerate(rgba):
        c = caixa(q[..., 3]) or (0, h_video - 1, 1, h_video)
        x0, y0, x1, y1 = c
        parte = q[y0:y1, x0:x1].astype(np.float32)
        w = max(1, round((x1 - x0) * escala))
        h = max(1, round((y1 - y0) * escala))
        # Alfa pré-multiplicado na redução, para a borda não escurecer.
        parte[..., :3] *= parte[..., 3:4] / 255.0
        parte = cv2.resize(parte, (w, h), interpolation=cv2.INTER_AREA)
        a = parte[..., 3:4] / 255.0
        parte[..., :3] = np.where(a > 1e-3, parte[..., :3] / np.maximum(a, 1e-3), 0)
        im = np.clip(parte, 0, 255).astype(np.uint8)
        nome = f'q{i:02d}.webp'
        Image.fromarray(im, 'RGBA').save(destino / nome, quality=90, method=6)
        imagens.append(im)
        # Onde o canto de cima do quadro fica, em px da tela, a partir do canto de baixo à
        # esquerda do vídeo (o lado da borda).
        lista.append({'imagem': nome, 'x': round(x0 * tela_por_video, 1),
                      'y': round((y0 - h_video) * tela_por_video, 1),
                      'largura': round((x1 - x0) * tela_por_video, 1),
                      'altura': round((y1 - y0) * tela_por_video, 1)})
    quadro_ms = round(1000.0 / (fps or fps_video / passo), 1)
    dados = {
        'video': Path(video).name,
        'trecho': [inicio, fim if fim is not None else inicio + (len(quadros) - 1) * passo],
        'passo': passo,
        # O vídeo entra pela esquerda; com o lutador à direita da tela o jogo espelha.
        'entraPor': 'esquerda',
        'alturaTela': altura,
        'larguraVideo': round(w_video * tela_por_video, 1),
        'quadroMs': quadro_ms,
        'segurarMs': segurar_ms,
        'saidaMs': saida_ms,
        'quadros': lista,
    }
    (destino / 'super.json').write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    previa_png(dados, imagens, Path(previa) / f'{personagem}.png', resolucao)
    return destino, dados


def previa_png(dados, imagens, caminho, resolucao):
    """Os quadros na tela do jogo (escura), lado a lado, para conferir corte e marca d'água."""
    caminho.parent.mkdir(parents=True, exist_ok=True)
    escolhidos = list(range(0, len(imagens), max(1, len(imagens) // 6)))[:6] + [len(imagens) - 1]
    lado = 320
    k = lado / TELA_L
    folha = Image.new('RGB', (lado * len(escolhidos), int(TELA_A * k)), (30, 30, 40))
    for n, i in enumerate(escolhidos):
        q = dados['quadros'][i]
        im = Image.fromarray(imagens[i], 'RGBA')
        w, h = max(1, round(q['largura'] * k)), max(1, round(q['altura'] * k))
        im = im.resize((w, h))
        folha.paste(im, (n * lado + round(q['x'] * k), round((TELA_A + q['y']) * k)), im)
    folha.save(caminho)


def numeros(texto):
    return [int(v) for v in texto.split(',')]


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('video')
    p.add_argument('--personagem', required=True)
    p.add_argument('--inicio', type=int, default=0)
    p.add_argument('--fim', type=int)
    p.add_argument('--passo', type=int, default=1)
    p.add_argument('--fps', type=float)
    p.add_argument('--segurar', type=int, default=0)
    p.add_argument('--saida', type=int, default=150)
    p.add_argument('--marca', type=numeros)
    p.add_argument('--resolucao', type=float, default=0.65)
    p.add_argument('--altura', type=float, default=1.0)
    a = p.parse_args()
    destino, dados = converter(a.video, a.personagem, a.inicio, a.fim, a.passo, a.fps, a.segurar, a.saida,
                               a.marca, a.resolucao, a.altura)
    total = len(dados['quadros']) * dados['quadroMs'] + dados['segurarMs'] + dados['saidaMs']
    print(f'{destino}: {len(dados["quadros"])} quadros, {total:.0f} ms de entrada')


if __name__ == '__main__':
    main()
