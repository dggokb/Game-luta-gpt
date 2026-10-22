#!/usr/bin/env python3
"""Monta as imagens iniciais (centro, esquerda, direita) a partir da Base do concept: recorta a
personagem do fundo liso, põe sobre verde puro no tamanho do vídeo e grava em art/keys/<id>/.

    python3 tools/sprites/inicio_da_base.py p15 base.png --previa pasta
    python3 tools/sprites/inicio_da_base.py p15 base.png --sufixo _h3 --bolso 0,0,300,620 --bolso 0,0,440,270

Personagem que já tem vídeos convertidos na escala antiga: grave com --sufixo (inicio_centro_h3...)
e ponha a escala nova em tamanho.json (a antiga × altura antiga / altura nova da guarda); a
conversão mede cada vídeo contra todas as chaves e usa a que bate.

A Base pode ter fundo transparente ou de uma cor só (o cinza do gerador de imagem). O fundo sai
por inundação a partir da borda; a sombra cinza embaixo dos pés sai junto. Fundo preso dentro
do desenho (entre mechas de cabelo) não é alcançado pela borda: --bolso X0,Y0,X1,Y1 (em px da
Base) apaga a cor do fundo dentro do retângulo. Conferir a prévia antes de gerar vídeo.

Padrão do Flick/H3 (flick-h3.md): 1344×768, corpo com 77% da altura, pés em y 733, laterais a
20,3% da largura do centro (a mesma proporção do p01 em 1280×720).
"""
import argparse
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
VERDE = (0, 255, 0, 255)
TOL_FUNDO = 18      # diferença máxima (por canal) para a cor do fundo
TOL_BOLSO = 16
TOL_FRANJA = 60     # na borda, diferença do fundo a partir da qual o pixel é todo desenho
LADO = 260 / 1280   # deslocamento das imagens laterais, fração da largura (p01)


def recortar(base, bolsos, sombra_sat=12, sombra_y=0.88, desfranja=0):
    """RGBA da Base sem o fundo."""
    rgba = base.convert('RGBA')
    a = np.asarray(rgba).astype(int)
    if (a[..., 3] < 255).mean() > 0.2:   # já transparente
        return rgba
    rgb = a[..., :3]
    h, w = rgb.shape[:2]
    borda = np.concatenate([rgb[:10].reshape(-1, 3), rgb[:, :10].reshape(-1, 3), rgb[:, -10:].reshape(-1, 3)])
    fundo = np.median(borda, axis=0)
    dif = np.abs(rgb - fundo).max(-1)
    sat = rgb.max(-1) - rgb.min(-1)
    lum = rgb.mean(-1)
    yy = np.arange(h)[:, None].repeat(w, 1)
    # cor do fundo, ou cinza mais escuro no rodapé (sombra dos pés; atadura preta fica)
    cand = (dif < TOL_FUNDO) | ((sat < sombra_sat) & (lum > 110) & (lum < fundo.mean() + 8) & (yy > h * sombra_y))
    m = Image.fromarray((cand * 255).astype(np.uint8)).copy()
    for seed in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1), (w // 2, 0), (w // 2, h - 1), (0, h // 2), (w - 1, h // 2)]:
        if m.getpixel(seed) == 255:
            ImageDraw.floodfill(m, seed, 128)
    fora = np.asarray(m) == 128
    for x0, y0, x1, y1 in bolsos:
        caixa = np.zeros_like(fora)
        caixa[y0:y1, x0:x1] = True
        fora |= caixa & (dif < TOL_BOLSO)
    alfa = Image.fromarray(((~fora) * 255).astype(np.uint8))
    alfa = alfa.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(0.6))
    if desfranja:
        # Franja clara em volta das mechas: o pixel da borda é mistura do desenho com o fundo.
        # Perto do fundo, quanto mais parecido com ele, mais transparente, e a cor do fundo sai
        # da mistura (o pixel fica com a cor da mecha).
        borda = np.asarray(Image.fromarray((fora * 255).astype(np.uint8)).filter(
            ImageFilter.MaxFilter(2 * desfranja + 1))) > 0
        a = np.clip(dif / TOL_FRANJA, 0, 1)
        a = np.where(borda, a, 1.0)
        alfa = Image.fromarray(np.minimum(np.asarray(alfa), (a * 255).astype(np.uint8)))
        cor = np.clip((rgb - (1 - a[..., None]) * fundo) / np.maximum(a, 1e-3)[..., None], 0, 255)
        px = np.asarray(rgba).copy()
        px[..., :3] = np.where(borda[..., None], cor, rgb).astype(np.uint8)
        rgba = Image.fromarray(px)
    rgba.putalpha(alfa)
    return rgba


def montar(corpo, tela, altura, chao, dx):
    al = np.asarray(corpo.getchannel('A')) > 128
    ys, xs = np.nonzero(al)
    corte = corpo.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))
    s = altura / corte.height
    corte = corte.resize((round(corte.width * s), round(corte.height * s)), Image.LANCZOS)
    w, h = tela
    img = Image.new('RGBA', tela, VERDE)
    img.alpha_composite(corte, (round(w / 2 - corte.width / 2 + dx), chao - corte.height + 1))
    return img.convert('RGB'), s


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('pacote')
    ap.add_argument('base', type=Path)
    ap.add_argument('--tela', default='1344x768', help='largura x altura do vídeo')
    ap.add_argument('--altura', type=float, default=0.77, help='altura do corpo, fração da tela')
    ap.add_argument('--chao', type=float, default=733 / 768, help='y do pé, fração da tela')
    ap.add_argument('--bolso', action='append', default=[], help='X0,Y0,X1,Y1 na Base')
    ap.add_argument('--sombra-sat', type=int, default=12,
                    help='saturação máxima da sombra do chão (sombra rabiscada e quente: ~26)')
    ap.add_argument('--sombra-y', type=float, default=0.88, help='a sombra começa nesta fração da altura')
    ap.add_argument('--desfranja', type=int, default=0,
                    help='px da borda onde a franja clara do fundo antigo sai (cabelo de mechas finas: 2)')
    ap.add_argument('--sufixo', default='', help='nome inicio_<lado><sufixo>.png, ex.: _h3')
    ap.add_argument('--previa', type=Path, help='grava aqui em vez de art/keys/<pacote>')
    args = ap.parse_args()

    w, h = (int(v) for v in args.tela.lower().split('x'))
    bolsos = [tuple(int(v) for v in b.split(',')) for b in args.bolso]
    corpo = recortar(Image.open(args.base), bolsos, args.sombra_sat, args.sombra_y, args.desfranja)
    destino = args.previa or ROOT / 'art' / 'keys' / args.pacote
    destino.mkdir(parents=True, exist_ok=True)
    for nome, lado in (('centro', 0), ('esquerda', -1), ('direita', 1)):
        img, s = montar(corpo, (w, h), round(h * args.altura), round(h * args.chao), lado * round(w * LADO))
        img.save(destino / f'inicio_{nome}{args.sufixo}.png')
    print(f'{destino}: inicio_centro/esquerda/direita{args.sufixo} {w}x{h}, escala da Base {s:.4f}')


if __name__ == '__main__':
    main()
