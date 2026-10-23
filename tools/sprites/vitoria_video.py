#!/usr/bin/env python3
"""Converte o vídeo de vitória (o retrato no verde) nos quadros da tela de vitória, e a arte do
cenário no fundo borrado dela.

A tela de vitória cobre a luta: o cenário borrado atrás, o lutador (o vídeo, do quadril para
cima) entra pela esquerda e faz a cena, "VITÓRIA" bate à direita e um quadro de mangá traz a
frase dele. No jogo o quadro do vídeo inteiro fica com `--altura` da tela, grudado embaixo, e o
desenho encostado na borda esquerda.

    pip install -r tools/sprites/requirements-animar.txt
    python3 tools/sprites/vitoria_video.py Max_Ajusta_Gola_5_Segundos.mp4 --personagem player_base \\
        --marca 8,8,150,50 --frase "Ainda tô só aquecendo."
    python3 tools/sprites/vitoria_video.py --fundo porto.webp --cenario padrao

    --inicio/--fim    trecho do vídeo (quadros, inclusive)
    --passo N         um quadro a cada N do vídeo
    --fps F           velocidade no jogo (sem isso, a do vídeo)
    --segurar MS      o último quadro parado antes da tela sair
    --marca x0,y0,x1,y1   apaga a marca d'água clara nesse retângulo do vídeo
    --altura A        o quadro do vídeo com A da altura da tela
    --resolucao R     pixels guardados por pixel da tela (o jogo lê os quadros aos poucos,
                      enquanto toca: o tamanho pesa no APK, não na memória)
    --frase TEXTO     frase do quadro de mangá (repita para várias; o jogo sorteia uma). Sem
                      --frase, as do vitoria.json que já existe ficam.
    --fundo IMAGEM --cenario ID   só o fundo: a arte do cenário borrada e reduzida em
                      vitorias/fundos/<ID>.webp ("padrao" vale para cenário sem fundo próprio)

Sai em android/app/src/main/assets/vitorias/<personagem>/: q000.webp... (cada quadro recortado no
desenho) e vitoria.json (onde cada um fica, o tempo e as frases).
"""
import argparse
import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent))
from efeito_video import caixa, chave, cor_do_fundo, ler_quadros  # noqa: E402
from super_video import TELA_A, TELA_L, apagar_marca, numeros, tirar_manchas  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
SAIDA = ROOT / 'android/app/src/main/assets/vitorias'
PREVIA = ROOT / 'android/app/build/sprite-review/vitorias'
# O fundo é guardado pequeno (está borrado) e esticado na tela.
FUNDO_L, FUNDO_A = 640, 360
FUNDO_BORRAO = 7


def converter(video, personagem, inicio=0, fim=None, passo=1, fps=None, segurar_ms=1000, marca=None,
              resolucao=0.6, altura=1.0, frases=None, saida=SAIDA, previa=PREVIA):
    quadros, fps_video = ler_quadros(video, inicio, fim, passo)
    fundo = cor_do_fundo(quadros)
    rgba = [chave(q, fundo) for q in quadros]
    for q in rgba:
        if marca:
            apagar_marca(q, marca)
        tirar_manchas(q)
    h_video, w_video = quadros[0].shape[:2]
    tela_por_video = altura * TELA_A / h_video
    escala = tela_por_video * resolucao
    destino = Path(saida) / personagem
    destino.mkdir(parents=True, exist_ok=True)
    antigo = destino / 'vitoria.json'
    if not frases and antigo.is_file():
        frases = json.loads(antigo.read_text(encoding='utf-8')).get('frases')
    for velho in destino.glob('q*.webp'):
        velho.unlink()
    lista, imagens = [], []
    for i, q in enumerate(rgba):
        x0, y0, x1, y1 = caixa(q[..., 3]) or (0, h_video - 1, 1, h_video)
        parte = q[y0:y1, x0:x1].astype(np.float32)
        w = max(1, round((x1 - x0) * escala))
        h = max(1, round((y1 - y0) * escala))
        # Alfa pré-multiplicado na redução, para a borda não escurecer.
        parte[..., :3] *= parte[..., 3:4] / 255.0
        parte = cv2.resize(parte, (w, h), interpolation=cv2.INTER_AREA)
        a = parte[..., 3:4] / 255.0
        parte[..., :3] = np.where(a > 1e-3, parte[..., :3] / np.maximum(a, 1e-3), 0)
        im = np.clip(parte, 0, 255).astype(np.uint8)
        nome = f'q{i:03d}.webp'
        Image.fromarray(im, 'RGBA').save(destino / nome, quality=85, method=6)
        imagens.append(im)
        # Onde o canto de cima do quadro fica, em px da tela, a partir do canto de baixo à
        # esquerda do vídeo.
        lista.append({'imagem': nome, 'x': round(x0 * tela_por_video, 1),
                      'y': round((y0 - h_video) * tela_por_video, 1),
                      'largura': round((x1 - x0) * tela_por_video, 1),
                      'altura': round((y1 - y0) * tela_por_video, 1)})
    dados = {
        'video': Path(video).name,
        'trecho': [inicio, fim if fim is not None else inicio + (len(quadros) - 1) * passo],
        'passo': passo,
        'alturaTela': altura,
        'larguraVideo': round(w_video * tela_por_video, 1),
        'quadroMs': round(1000.0 / (fps or fps_video / passo), 1),
        'segurarMs': segurar_ms,
        'frases': frases or [],
        'quadros': lista,
    }
    antigo.write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    previa_png(dados, imagens, Path(previa) / f'{personagem}.png')
    return destino, dados


def previa_png(dados, imagens, caminho):
    """Alguns quadros na tela do jogo (escura), lado a lado, para conferir corte e marca d'água."""
    caminho.parent.mkdir(parents=True, exist_ok=True)
    escolhidos = list(range(0, len(imagens), max(1, len(imagens) // 6)))[:6] + [len(imagens) - 1]
    lado = 320
    k = lado / TELA_L
    esquerda = min(q['x'] for q in dados['quadros'])
    folha = Image.new('RGB', (lado * len(escolhidos), int(TELA_A * k)), (30, 30, 40))
    for n, i in enumerate(escolhidos):
        q = dados['quadros'][i]
        im = Image.fromarray(imagens[i], 'RGBA')
        im = im.resize((max(1, round(q['largura'] * k)), max(1, round(q['altura'] * k))))
        folha.paste(im, (n * lado + round((q['x'] - esquerda) * k), round((TELA_A + q['y']) * k)), im)
    folha.save(caminho)


def fundo(imagem, cenario, saida=SAIDA):
    """A arte do cenário cortada em 16:9, reduzida e borrada: o fundo da tela de vitória."""
    im = Image.open(imagem).convert('RGB')
    w, h = im.size
    alvo = TELA_L / TELA_A
    if w / h > alvo:
        corte = round(h * alvo)
        im = im.crop(((w - corte) // 2, 0, (w - corte) // 2 + corte, h))
    else:
        corte = round(w / alvo)
        im = im.crop((0, (h - corte) // 2, w, (h - corte) // 2 + corte))
    im = im.resize((FUNDO_L, FUNDO_A), Image.LANCZOS).filter(ImageFilter.GaussianBlur(FUNDO_BORRAO))
    destino = Path(saida) / 'fundos' / f'{cenario}.webp'
    destino.parent.mkdir(parents=True, exist_ok=True)
    im.save(destino, quality=82, method=6)
    return destino


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('video', nargs='?')
    p.add_argument('--personagem')
    p.add_argument('--inicio', type=int, default=0)
    p.add_argument('--fim', type=int)
    p.add_argument('--passo', type=int, default=1)
    p.add_argument('--fps', type=float)
    p.add_argument('--segurar', type=int, default=1000)
    p.add_argument('--marca', type=numeros)
    p.add_argument('--resolucao', type=float, default=0.6)
    p.add_argument('--altura', type=float, default=1.0)
    p.add_argument('--frase', action='append')
    p.add_argument('--fundo')
    p.add_argument('--cenario', default='padrao')
    a = p.parse_args()
    if a.fundo:
        print(fundo(a.fundo, a.cenario))
        return
    if not a.video or not a.personagem:
        p.error('o vídeo e --personagem (ou --fundo)')
    destino, dados = converter(a.video, a.personagem, a.inicio, a.fim, a.passo, a.fps, a.segurar, a.marca,
                               a.resolucao, a.altura, a.frase)
    total = len(dados['quadros']) * dados['quadroMs'] + dados['segurarMs']
    print(f'{destino}: {len(dados["quadros"])} quadros, {total:.0f} ms de cena')


if __name__ == '__main__':
    main()
