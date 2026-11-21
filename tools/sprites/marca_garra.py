#!/usr/bin/env python3
"""As marcas da Garra do tigre do Kōga (S1A: os três riscos; SG: o X) viram efeitos soltos.

A marca é um poder (o dono, 21/11: "o princípio da marca é ser um poder, então tem que respeitar as
características de poder do nosso jogo"): no motor ela é um projétil parado que fica a janela da
continuação, e continua se o Kōga apanhar. Enquanto ele está na Garra, a marca é a do desenho dele
(a folha GARRA_1/GARRA_2); fora dela (apanhou, cancelou no Super), quem desenha a marca é este
efeito, tirado do mesmo quadro da folha, no mesmo lugar e tamanho.

A marca encosta na mão e o X passa por cima da perna, então não sai por pedaço solto: sai pela cor
do brilho (laranja, amarelo e branco quentes; a pele e a luva ficam de fora) à direita do corpo, e o
miolo escuro entre os riscos entra fechando os buracos.

    python3 tools/sprites/marca_garra.py            # grava as duas e mostra a caixa de cada uma
    python3 tools/sprites/marca_garra.py --previa P.png

A caixa (px do mundo, da raiz do Kōga) é a que vai em mecanica.marca do S1A e do SG no
characters/p03/character.json.
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
Image.MAX_IMAGE_PIXELS = None
SAIDA = ROOT / 'android/app/src/main/assets/efeitos'
WORLD_SCALE = 1.075          # tools/sprites/profiles/player_base.json (o perfil do Kōga)
BURACO_MAX = 2500             # px da folha
LUZ = {'cor': '#FF6A2A', 'alcance': 0.8, 'forca': 0.6}   # a do X que voa (onda_p03)

# nome do efeito, clipe, quadro da folha (a pose segurada com a marca inteira), x a partir do qual
# é marca (px da célula: à esquerda está o corpo, a camisa branca do 2º corte é clara como a marca)
# e o canto da mão (x até, y desde: a luva vermelha que encosta na marca)
MARCAS = [
    ('marca_p03', 'p03_garra_1', 13, 390, (560, 400), 'os três riscos do 1º corte (S1A), quadro 13 da folha GARRA_1'),
    ('marca_x_p03', 'p03_garra_2', 15, 370, (520, 370), 'o X do 2º corte (SG), quadro 15 da folha GARRA_2'),
]


def celula(clipe, i):
    c = json.loads((ROOT / f'tools/sprites/clips/{clipe}.json').read_text())
    im = np.array(Image.open(ROOT / c['source']).convert('RGBA'))
    fw, fh, cols = c['frameWidth'], c['frameHeight'], c['columns']
    r, q = divmod(i, cols)
    return im[r * fh:(r + 1) * fh, q * fw:(q + 1) * fw].copy(), c


def mascara(cel, x0, mao):
    rgb = cel[..., :3].astype(np.int32)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    a = cel[..., 3]
    quente = (r > 120) & (b < r * 0.45)          # laranja e amarelo do brilho (pele tem mais azul)
    # A mão (retângulo da célula: x até mao[0], y desde mao[1]): ali só entra brilho de verdade
    # (branco, ou laranja e amarelo com verde); a luva e a borda dela são do mesmo vermelho do
    # miolo da marca.
    na_mao = np.zeros(a.shape, bool)
    na_mao[mao[1]:, :mao[0]] = True
    quente &= ~(na_mao & ((g <= r * 0.4) | (a >= 240)))   # a pele dos dedos é opaca, o halo não
    branco = np.minimum(np.minimum(r, g), b) > 200
    # O miolo escuro entre os riscos, meio transparente (a luva e a calça, do mesmo vermelho, são opacas).
    vermelho = (r > 70) & (g < r * 0.6) & (b < r * 0.6) & (a < 230)
    vermelho &= ~na_mao
    m = ((quente | branco | vermelho) & (a > 40)).astype(np.uint8)
    m[:, :x0] = 0
    # Perto do corpo (opaco sem a cor do brilho: luva, pele, calça), só o brilho entra; o vermelho
    # meio transparente ali é a borda suavizada da luva e da manga.
    corpo = ((a >= 230) & ~quente & ~branco).astype(np.uint8)
    perto = cv2.dilate(corpo, np.ones((11, 11), np.uint8)) > 0
    m[perto & ~(quente | branco)] = 0
    # A maior peça: a luva só encosta na marca pela pele (que não entra) e o tênis branco é outra.
    n, lab, st, _ = cv2.connectedComponentsWithStats(m, 4)
    maior = 1 + int(np.argmax(st[1:, cv2.CC_STAT_AREA]))
    peca = (lab == maior).astype(np.uint8)
    # Os buracos pequenos (o degradê do branco para o laranja, que tem cor de pele); o grande é o
    # braço cercado pelo risco comprido do X e fica de fora.
    fundo = np.zeros((peca.shape[0] + 2, peca.shape[1] + 2), np.uint8)
    cheio = peca.copy()
    cv2.floodFill(cheio, fundo, (0, 0), 2)
    buracos = ((cheio != 2) & (peca == 0)).astype(np.uint8)
    n, lab, st, _ = cv2.connectedComponentsWithStats(buracos, 8)
    for k in range(1, n):
        if st[k, cv2.CC_STAT_AREA] <= BURACO_MAX:
            peca[lab == k] = 1
    return (peca > 0) & (a > 0)


def fazer(nome, clipe, quadro, x0, mao, nota, previa):
    cel, c = celula(clipe, quadro)
    m = mascara(cel, x0, mao)
    out = cel.copy()
    out[~m] = 0
    ys, xs = np.nonzero(m)
    x1, x2, y1, y2 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
    recorte = out[y1:y2, x1:x2]
    ps = c['pixelScale'] / WORLD_SCALE           # px da folha por px do mundo
    cx, cy = (x1 + x2) / 2, (y1 + y2) / 2
    # A caixa no mundo, da raiz (frente para a frente do Kōga, altura para cima do chão).
    frente = (cx - c['rootX']) / ps
    altura = (c['rootY'] - cy) / ps
    meia_l, meia_a = (x2 - x1) / 2 / ps, (y2 - y1) / 2 / ps
    caixa = {'frente': round(frente, 1), 'altura': round(altura, 1),
             'meiaLargura': round(meia_l, 1), 'meiaAltura': round(meia_a, 1)}
    print(nome, 'caixa', caixa)
    if previa is not None:
        previa.append(recorte)
        return
    pasta = SAIDA / nome
    pasta.mkdir(parents=True, exist_ok=True)
    Image.fromarray(recorte).save(pasta / 'folha.webp', lossless=True)
    info = {
        'nome': nome,
        'video': f'{nota}: a marca da Garra parada, para quando o Kōga sai da Garra com ela na tela '
                 '(tools/sprites/marca_garra.py; a mesma escala da folha dele)',
        'ancora': 'centro',
        'pixelScale': round(ps, 4),
        'altura': round((y2 - y1) / ps, 1),
        'quadros': [[0, 0, int(x2 - x1), int(y2 - y1), round(x1 - cx, 1), round(y1 - cy, 1)]],
        'duracoesMs': [1000],
        'caixa': caixa,
        'luz': LUZ,
    }
    (pasta / 'efeito.json').write_text(json.dumps(info, ensure_ascii=False, indent=1) + '\n')


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--previa')
    args = ap.parse_args()
    previa = [] if args.previa else None
    for item in MARCAS:
        fazer(*item, previa)
    if previa is not None:
        h = max(p.shape[0] for p in previa)
        w = sum(p.shape[1] for p in previa) + 20 * len(previa)
        tela = Image.new('RGBA', (w, h), (40, 40, 60, 255))
        x = 0
        for p in previa:
            tela.alpha_composite(Image.fromarray(p), (x, 0))
            x += p.shape[1] + 20
        tela.save(args.previa)


if __name__ == '__main__':
    main()
