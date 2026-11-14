#!/usr/bin/env python3
"""Separa os dois lutadores de um vídeo com os dois juntos no verde (a briga dos irmãos, o qt1 do
Drive ryuga_8) e grava uma folha de poses para cada um, no formato das folhas do personagem.py
(art/sprites/source/<id>_<clipe>_video_normalized.png e a receita em tools/sprites/clips).

    python3 tools/sprites/dupla_video.py qt1.mp4 --esquerda koga_8 --direita ryuga_8 \
        --clipe cena_briga --quadros 4-96:2

Cada quadro: o key() do video_para_sprite (com os efeitos do vídeo apagados, o jogo desenha os
dele), os dois maiores pedaços, o da esquerda para um e o da direita para o outro. Nos quadros em
que os dois se tocam (o pé de um no outro) a imagem é cortada na coluna mais vazia entre os dois.
Quem está à direita no vídeo olha para a esquerda: a folha dele sai espelhada (todas as folhas
olham para a direita).

O registro é o do "fixar: video" dos clipes dos dois: o tronco do 1º quadro do vídeo fica na raiz
e o chão é o pé do 1º quadro, na escala 0,30 dos vídeos das crianças. Então a pose sai no mesmo
lugar do idle, e o que o menino anda no vídeo ele anda na folha.
"""
import argparse
import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
from video_para_sprite import key, read_frames, torso_x  # noqa: E402


def quadros(spec):
    """'4-96:2' -> 4, 6, ..., 96; '3,5,9' -> 3, 5, 9."""
    if '-' in spec:
        faixa, _, passo = spec.partition(':')
        a, b = (int(v) for v in faixa.split('-'))
        return list(range(a, b + 1, int(passo or 1)))
    return [int(v) for v in spec.split(',')]


def separar(rgba, antes):
    """Os dois lutadores do quadro (esquerda, direita), cada um num RGBA do tamanho do vídeo.
    antes: os centros (x) dos dois no quadro anterior, para cortar quando eles se tocam."""
    alfa = (rgba[..., 3] > 128).astype(np.uint8)
    n, lab, st, cen = cv2.connectedComponentsWithStats(alfa)
    pedacos = sorted(range(1, n), key=lambda k: -st[k, 4])[:2]
    if len(pedacos) == 2 and st[pedacos[1], 4] > 20000:
        esq, dir_ = sorted(pedacos, key=lambda k: cen[k, 0])
        mascaras = [lab == esq, lab == dir_]
    else:
        # Encostados: a coluna com menos corpo entre os dois centros de antes.
        corpo = lab == pedacos[0]
        a, b = int(antes[0]) + 60, int(antes[1]) - 60
        coluna = corpo[:, a:b].sum(0)
        corte = a + int(np.argmin(coluna))
        cols = np.arange(rgba.shape[1])[None, :]
        mascaras = [corpo & (cols < corte), corpo & (cols >= corte)]
    out = []
    for m in mascaras:
        um = rgba.copy()
        um[..., 3] = np.where(m, rgba[..., 3], 0)
        out.append(um)
    return out


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('video')
    p.add_argument('--esquerda', required=True, help='id de quem está à esquerda no vídeo')
    p.add_argument('--direita', required=True, help='id de quem está à direita no vídeo')
    p.add_argument('--clipe', required=True, help='nome do clipe (o atlas fica <id>_<clipe>)')
    p.add_argument('--quadros', required=True, help="quadros do vídeo: '4-96:2' ou '3,5,9'")
    p.add_argument('--escala', type=float, default=0.3, help='mundo por px do vídeo (0,30 nas crianças)')
    p.add_argument('--celula', type=int, default=224, help='altura da célula (mundo)')
    p.add_argument('--largura', type=int, default=256, help='largura da célula (mundo)')
    p.add_argument('--raiz', default='96,206', help='raiz na célula (mundo): o tronco do 1º quadro e o chão')
    p.add_argument('--densidade', type=int, default=2)
    p.add_argument('--colunas', type=int, default=8)
    args = p.parse_args()

    frames, fps = read_frames(args.video)
    lista = quadros(args.quadros)
    primeiro = separar(key(frames[0], effects=True, objects=2), (0, frames[0].shape[1]))
    ref = [(torso_x(k), int(np.nonzero((k[..., 3] > 128).any(1))[0].max())) for k in primeiro]
    antes = [torso_x(k) for k in primeiro]
    por_lado = ([], [])
    # Todos os quadros até o último pedido: os centros de antes acompanham quem encosta.
    for i in range(max(lista) + 1):
        dois = separar(key(frames[i], effects=True, objects=2), antes)
        antes = [torso_x(k) for k in dois]
        if i in lista:
            for lado in (0, 1):
                por_lado[lado].append(dois[lado])

    d = args.densidade
    s = args.escala * d
    rx, ry = (int(v) * d for v in args.raiz.split(','))
    cw, ch = args.largura * d, args.celula * d
    cols = args.colunas
    rows = (len(lista) + cols - 1) // cols
    for lado, ident in enumerate((args.esquerda, args.direita)):
        rx0, foot = ref[lado]
        folha = Image.new('RGBA', (cols * cw, rows * ch), (0, 0, 0, 0))
        for n, k in enumerate(por_lado[lado]):
            img = Image.fromarray(k, 'RGBA')
            tronco = rx0
            if lado == 1:
                # Olha para a esquerda no vídeo: espelha (o tronco do 1º quadro espelha junto).
                img = img.transpose(Image.FLIP_LEFT_RIGHT)
                tronco = k.shape[1] - 1 - rx0
            img = img.resize((round(img.width * s), round(img.height * s)), Image.LANCZOS)
            caixa = Image.new('RGBA', (cw, ch), (0, 0, 0, 0))
            caixa.paste(img, (rx - round(tronco * s), ry - round(foot * s)))
            folha.paste(caixa, ((n % cols) * cw, (n // cols) * ch))
        atlas = f'{ident}_{args.clipe}'
        fonte = ROOT / f'art/sprites/source/{atlas}_video_normalized.png'
        folha.save(fonte)
        receita = {
            'id': atlas,
            'javaName': atlas.upper(),
            'profile': f'{ident}.json',
            'source': str(fonte.relative_to(ROOT)),
            'output': f'android/app/src/main/res/drawable-nodpi/{atlas}.webp',
            'report': f'tools/sprites/reports/{atlas}.report.json',
            'preview': f'android/app/build/sprite-review/{atlas}-import.png',
            'expectedFrames': len(lista),
            'segmentation': 'prepared-grid',
            'columns': cols,
            'rootMode': 'authored',
            'sourceNote': f'{ident} separado do vídeo com os dois juntos ({Path(args.video).name}) por '
                          'tools/sprites/dupla_video.py',
            'video': {'arquivo': Path(args.video).name, 'personagem': ident, 'escala': args.escala,
                      'lado': 'esquerda' if lado == 0 else 'direita', 'quadros': args.quadros,
                      'celula': args.celula, 'largura': args.largura, 'raiz': args.raiz,
                      'colunas': cols, 'fixar': 'video', 'densidade': d},
            'frameWidth': cw,
            'frameHeight': ch,
            'rootX': rx,
            'rootY': ry,
            'pixelScale': float(d),
        }
        (HERE / f'clips/{atlas}.json').write_text(json.dumps(receita, ensure_ascii=False, indent=2) + '\n',
                                                  encoding='utf-8')
        print(f'{atlas}: {len(lista)} quadros, {cols}×{rows} → {fonte.relative_to(ROOT)}')


if __name__ == '__main__':
    main()
