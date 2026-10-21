#!/usr/bin/env python3
"""Quadros intermediários fora do jogo: acha os saltos grandes entre quadros e gera o do meio.

Um quadro intermediário é o desenho de antes e o de depois andando metade do caminho um até
o outro (fluxo óptico nos dois sentidos) e misturados. Sai para revisão, não entra sozinho no
pack: o fluxo erra em poses muito diferentes, então cada candidato é olhado antes de virar
quadro (docs/motor-animacao.md diz como aplicar).

    pip install -r tools/sprites/requirements-animar.txt
    # Os maiores saltos do personagem (pixels do mundo entre um quadro e o seguinte):
    python3 tools/sprites/intermediarios.py auditar
    python3 tools/sprites/intermediarios.py auditar --clipes HEAVY_STRAIGHT LIGHT_JAB --limite 10
    # O quadro do meio entre os quadros 9 e 10 da animação HEAVY_STRAIGHT (posições na lista
    # "frames" dela), com a tira de revisão e os GIFs antes/depois:
    python3 tools/sprites/intermediarios.py gerar HEAVY_STRAIGHT --entre 9

Saída padrão: android/app/build/sprite-review/intermediarios/ (fora do git).
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SAIDA = ROOT / 'android/app/build/sprite-review/intermediarios'
# Duração de um quadro sem durationsMs no pack (as caminhadas, que andam por distância).
MS_PADRAO = 42
# Salto (pixels do mundo) a partir do qual a auditoria marca o par como candidato, e a
# confiança mínima do fluxo para o quadro do meio servir (abaixo: redesenhar).
SALTO_CANDIDATO = 40.0
CONFIANCA_MINIMA = 0.6


def ler(caminho):
    return json.loads(Path(caminho).read_text(encoding='utf-8'))


class Folha:
    """Uma folha empacotada do pack: cada quadro volta para a célula dele (raiz no mesmo lugar)."""

    def __init__(self, root, atlas):
        cfg = ler(root / 'tools/sprites/clips' / f'{atlas}.json')
        report = ler(root / cfg['report'])
        self.layout = report['packed']
        # Pixels da célula para pixels do mundo (como o SpriteFighterRenderer).
        self.escala = report['worldScale'] / self.layout.get('pixelScale', cfg.get('pixelScale', 1))
        self.imagem = np.asarray(Image.open(root / cfg['output']).convert('RGBA'))

    def quadro(self, i):
        lay = self.layout
        sx, sy, w, h, cx, cy = lay['frames'][i]
        cel = np.zeros((lay['frameHeight'], lay['frameWidth'], 4), np.uint8)
        cel[cy:cy + h, cx:cx + w] = self.imagem[sy:sy + h, sx:sx + w]
        return cel


def cinza(img):
    """Cinza sobre fundo neutro: o fluxo vê o desenho, não o recorte."""
    a = img[..., 3:4].astype(np.float32) / 255
    rgb = img[..., :3].astype(np.float32) * a + 128 * (1 - a)
    return cv2.cvtColor(rgb.astype(np.uint8), cv2.COLOR_RGB2GRAY)


def fluxo(a, b):
    """Fluxo óptico de a para b (pixel p de a vai para p + fluxo[p] em b)."""
    return cv2.calcOpticalFlowFarneback(cinza(a), cinza(b), None, 0.5, 5, 21, 5, 7, 1.5, 0)


def salto(a, b):
    """Quanto o desenho anda de a para b: o percentil 95 do fluxo onde há desenho (pixels da célula)."""
    mascara = (a[..., 3] > 127) | (b[..., 3] > 127)
    if not mascara.any():
        return 0.0
    f = fluxo(a, b)
    return float(np.percentile(np.hypot(f[..., 0], f[..., 1])[mascara], 95))


def _grade(h, w):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def _erro_ida_volta(ida, volta):
    """Por pixel de origem: quanto a ida e a volta do fluxo deixam de se anular (pixels)."""
    xx, yy = _grade(*ida.shape[:2])
    volta_la = cv2.remap(volta, xx + ida[..., 0], yy + ida[..., 1], cv2.INTER_LINEAR,
                         borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    return np.hypot(ida[..., 0] + volta_la[..., 0], ida[..., 1] + volta_la[..., 1])


def confianca(a, b):
    """
    Quanto do desenho o fluxo acerta: a fração dos pixels de a que vão até b e voltam pelo
    fluxo de b para a (a menos de 2 pixels). Baixa num giro ou numa pose nova, onde o quadro
    do meio vira dupla exposição e o certo é desenhar.
    """
    mascara = a[..., 3] > 127
    if not mascara.any():
        return 1.0
    erro = _erro_ida_volta(fluxo(a, b), fluxo(b, a))
    return float((erro[mascara] < 2.0).mean())


def _puxar(img, f, t):
    xx, yy = _grade(*img.shape[:2])
    return cv2.remap(img.astype(np.float32), xx - t * f[..., 0], yy - t * f[..., 1], cv2.INTER_LINEAR,
                     borderMode=cv2.BORDER_CONSTANT, borderValue=0)


def intermediario(a, b, t=0.5):
    """O quadro em t (0 = a, 1 = b): os dois desenhos andam até lá pelo fluxo e se misturam."""
    de_a = _puxar(a, fluxo(a, b), t)
    de_b = _puxar(b, fluxo(b, a), 1 - t)
    # Mistura com alfa pré-multiplicado (a borda não escurece).
    pa, pb = de_a[..., 3:4] / 255, de_b[..., 3:4] / 255
    cor = (1 - t) * de_a[..., :3] * pa + t * de_b[..., :3] * pb
    alfa = (1 - t) * pa + t * pb
    # Borda firme: o que os dois concordam fica opaco, a sobra fraca da mistura some.
    firme = np.clip((alfa - 0.2) / 0.6, 0, 1)
    firme = firme * firme * (3 - 2 * firme)
    rgb = np.where(alfa > 1e-4, cor / np.maximum(alfa, 1e-4), 0)
    return np.dstack([np.clip(rgb, 0, 255), firme * 255]).astype(np.uint8)


def animacoes(root, personagem):
    pack = ler(root / 'characters' / personagem / 'character.json')
    return pack['animations']


def duracoes(anim):
    ms = anim.get('durationsMs') or []
    return [ms[i] if i < len(ms) else MS_PADRAO for i in range(len(anim['frames']))]


def auditar(root, personagem, clipes=None, limite=20):
    """Os pares de quadros seguidos com o maior salto, do maior para o menor."""
    folhas, linhas = {}, []
    for nome, anim in animacoes(root, personagem).items():
        if clipes and nome not in clipes:
            continue
        folha = folhas.setdefault(anim['atlas'], Folha(root, anim['atlas']))
        frames, ms = anim['frames'], duracoes(anim)
        pares = list(zip(range(len(frames) - 1), range(1, len(frames))))
        if anim.get('loop') and len(frames) > 2:
            pares.append((len(frames) - 1, 0))
        for i, j in pares:
            if frames[i] == frames[j]:
                continue
            px = salto(folha.quadro(frames[i]), folha.quadro(frames[j])) * folha.escala
            linhas.append({'clipe': nome, 'de': i, 'para': j, 'mundo': px, 'ms': ms[i]})
    linhas.sort(key=lambda l: -l['mundo'])
    linhas = linhas[:limite] if limite else linhas
    for l in linhas:
        anim = animacoes(root, personagem)[l['clipe']]
        folha = folhas[anim['atlas']]
        l['confianca'] = confianca(folha.quadro(anim['frames'][l['de']]), folha.quadro(anim['frames'][l['para']]))
    return linhas


def xadrez(h, w, lado=16):
    yy, xx = np.mgrid[0:h, 0:w]
    v = np.where(((yy // lado) + (xx // lado)) % 2 == 0, 70, 90).astype(np.uint8)
    return np.dstack([v, v, v, np.full((h, w), 255, np.uint8)])


def sobre(fundo, img):
    a = img[..., 3:4].astype(np.float32) / 255
    out = fundo.astype(np.float32)
    out[..., :3] = img[..., :3] * a + out[..., :3] * (1 - a)
    return out.astype(np.uint8)


def gif(caminho, quadros, ms):
    h, w = quadros[0].shape[:2]
    imagens = [Image.fromarray(sobre(xadrez(h, w), q)[..., :3]) for q in quadros]
    imagens[0].save(caminho, save_all=True, append_images=imagens[1:], duration=ms, loop=0)


def gerar(root, personagem, clipe, entre, saida):
    """O quadro do meio depois de cada posição em {@code entre}, a tira de revisão e os GIFs."""
    anim = animacoes(root, personagem)[clipe]
    folha = Folha(root, anim['atlas'])
    frames, ms = anim['frames'], duracoes(anim)
    saida.mkdir(parents=True, exist_ok=True)
    originais = [folha.quadro(f) for f in frames]
    novos, novos_ms, gerados = [], [], []
    for i, q in enumerate(originais):
        if i in entre:
            if i + 1 >= len(originais):
                raise ValueError(f'{clipe}: não há quadro depois da posição {i}')
            meio = intermediario(q, originais[i + 1])
            nota = confianca(q, originais[i + 1])
            print(f'{clipe} {i} > {i + 1}: confiança do fluxo {nota:.0%}'
                  + ('' if nota >= CONFIANCA_MINIMA else ' (baixa: o meio vai sair com dupla exposição)'))
            arquivo = saida / f'{clipe}_{i:02d}-{i + 1:02d}.png'
            Image.fromarray(meio).save(arquivo)
            h, w = q.shape[:2]
            tira = np.hstack([sobre(xadrez(h, w), x) for x in (q, meio, originais[i + 1])])
            Image.fromarray(tira[..., :3]).save(saida / f'{clipe}_{i:02d}-{i + 1:02d}_revisao.png')
            gerados.append(arquivo)
            # O quadro de antes divide o tempo dele com o do meio: a duração do golpe não muda.
            novos += [q, meio]
            novos_ms += [ms[i] - ms[i] // 2, ms[i] // 2]
        else:
            novos.append(q)
            novos_ms.append(ms[i])
    gif(saida / f'{clipe}_original.gif', originais, ms)
    gif(saida / f'{clipe}_com_intermediarios.gif', novos, novos_ms)
    return gerados


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--personagem', default='player_base')
    sub = parser.add_subparsers(dest='comando', required=True)
    a = sub.add_parser('auditar', help='lista os maiores saltos entre quadros seguidos')
    a.add_argument('--clipes', nargs='*')
    a.add_argument('--limite', type=int, default=20)
    g = sub.add_parser('gerar', help='gera o quadro do meio depois das posições pedidas')
    g.add_argument('clipe')
    g.add_argument('--entre', type=int, action='append', required=True,
                   help='posição na lista "frames" da animação (o meio fica entre ela e a seguinte)')
    g.add_argument('--saida', type=Path, default=SAIDA)
    args = parser.parse_args()
    if args.comando == 'auditar':
        print(f'{"clipe":<22}{"quadros":>9}{"salto (px)":>12}{"ms":>6}{"confiança":>11}')
        for l in auditar(ROOT, args.personagem, args.clipes, args.limite):
            marca = ''
            if l['mundo'] >= SALTO_CANDIDATO:
                marca = '  <- candidato' if l['confianca'] >= CONFIANCA_MINIMA else '  <- salto, mas pose nova: redesenhar'
            print(f"{l['clipe']:<22}{l['de']:>4} > {l['para']:<3}{l['mundo']:>11.0f}{l['ms']:>7}{l['confianca']:>10.0%}{marca}")
    else:
        for arquivo in gerar(ROOT, args.personagem, args.clipe, set(args.entre), args.saida):
            print(arquivo)
        print(f'Revisão em {args.saida}')


if __name__ == '__main__':
    main()
