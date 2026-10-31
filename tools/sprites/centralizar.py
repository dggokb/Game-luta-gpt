#!/usr/bin/env python3
"""Mantém o tronco de um golpe no eixo da pose de referência (o agachado parado, o pulo).

O vídeo às vezes anda com o corpo inteiro no golpe: o 2L da p14 avançava o tronco 63 px à
frente do agachado (os outros personagens: até 25), o 2H recuava 55, e o jL saía 60-70 px abaixo
do tronco do pulo (o pé preso na raiz com as pernas encolhidas puxa o corpo para baixo). Na
tela o lutador sai do eixo e volta.

O eixo do tronco é a coluna (e, com --vertical, a linha) com mais desenho, pesada ao quadrado:
o tronco é a parte mais grossa; a perna esticada do chute e o braço do soco pesam pouco e não
puxam o eixo (o centro da caixa puxaria). Cada quadro anda o que passa de --limite px da
referência (dentro do limite fica como está: o golpe continua com o próprio balanço), com uma
média curta entre vizinhos para não dar tranco.

    python3 tools/sprites/centralizar.py p14 CROUCH_LIGHT CROUCH_MEDIUM CROUCH_HEAVY --ref CROUCH_IDLE --limite 16
    python3 tools/sprites/centralizar.py p14 JUMP_LIGHT JUMP_MEDIUM JUMP_HEAVY --ref JUMP --limite 12 --vertical
    python3 tools/sprites/centralizar.py p14 CROUCH_LIGHT --ref CROUCH_IDLE --ver   # só mostra

Edita a folha do clipe (como o ancorar.py) e grava "centralizar" na receita do vídeo, para a
reconversão repetir. Depois rode caixas_sprites.py --write <id> (as caixas e o alcance vêm do
desenho) e build_characters.py --write.
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import auditoria as A  # noqa: E402
from ancorar import MARGIN, clip_path  # noqa: E402


def mediana_pesada(v):
    w = v.astype(float) ** 2
    cs = np.cumsum(w)
    return int(np.searchsorted(cs, cs[-1] / 2))


def eixo(rgba, rx, ry):
    """(x, y) do tronco relativo à raiz, em px lógicos; None se o quadro está vazio."""
    a = rgba[..., 3] > 128
    if not a.any():
        return None
    return mediana_pesada(a.sum(0)) - rx, mediana_pesada(a.sum(1)) - ry


def referencia(pack, clips, estado):
    """O eixo da pose de referência: o 1º quadro do agachado parado; no pulo (e em loops), a
    mediana dos quadros sem o da saída do chão."""
    anim = pack['animations'][estado]
    sheet = A.Sheet(clips[anim['atlas']])
    eixos = [eixo(sheet.frame(i), sheet.rx, sheet.ry) for i in anim['frames']]
    eixos = [e for e in eixos if e is not None]
    if estado.startswith('JUMP') and len(eixos) > 2:
        eixos = eixos[1:]
    elif not anim.get('loop') or estado.startswith('CROUCH'):
        eixos = eixos[:1]
    return float(np.median([e[0] for e in eixos])), float(np.median([e[1] for e in eixos]))


def suavizar(v):
    if len(v) < 3:
        return v
    p = np.concatenate([[v[0]], v, [v[-1]]])
    return (p[:-2] + 2 * p[1:-1] + p[2:]) / 4


def deslocamentos(sheet, n, ref, limite, vertical):
    """(dx, dy) inteiros de cada célula do atlas (px lógicos)."""
    eixos = [eixo(sheet.frame(i), sheet.rx, sheet.ry) for i in range(n)]
    dx, dy = np.zeros(n), np.zeros(n)
    for i, e in enumerate(eixos):
        if e is None:
            continue
        x, y = e
        dx[i] = ref[0] + np.clip(x - ref[0], -limite, limite) - x
        if vertical:
            dy[i] = ref[1] + np.clip(y - ref[1], -limite, limite) - y
    return np.round(suavizar(dx)).astype(int), np.round(suavizar(dy)).astype(int)


def aplicar_na_folha(atlas, clips, dx, dy):
    """Desloca cada célula da folha (física) e alarga a célula para nada encostar na borda."""
    cfg = clips[atlas]
    sheet = A.Sheet(cfg)
    n = cfg['expectedFrames']
    masks = [sheet.frame(i)[..., 3] > 0 for i in range(n)]
    lefts, rights, tops, bottoms = [], [], [], []
    for m, x, y in zip(masks, dx, dy):
        if not m.any():
            continue
        xs, ys = np.nonzero(m.any(0))[0], np.nonzero(m.any(1))[0]
        lefts.append(xs.min() + x)
        rights.append(xs.max() + x)
        tops.append(ys.min() + y)
        bottoms.append(ys.max() + y)
    pad_l = int(max(0, MARGIN - min(lefts)))
    pad_r = int(max(0, max(rights) - (sheet.w - 1 - MARGIN)))
    pad_t = int(max(0, MARGIN - min(tops)))
    pad_b = int(max(0, max(bottoms) - (sheet.h - 1 - MARGIN)))
    d = sheet.d
    w, h = sheet.pw + d * (pad_l + pad_r), sheet.ph + d * (pad_t + pad_b)
    img = Image.open(A.ROOT / cfg['source']).convert('RGBA')
    rows = (n + sheet.cols - 1) // sheet.cols
    out = Image.new('RGBA', (w * sheet.cols, h * rows), (0, 0, 0, 0))
    for i in range(n):
        x, y = i % sheet.cols * sheet.pw, i // sheet.cols * sheet.ph
        cell = img.crop((x, y, x + sheet.pw, y + sheet.ph))
        # Onde a célula cai dentro da nova (a parte que sai é só transparência da margem).
        ox, oy = d * (pad_l + int(dx[i])), d * (pad_t + int(dy[i]))
        sx0, sy0 = max(0, -ox), max(0, -oy)
        sx1, sy1 = min(sheet.pw, w - ox), min(sheet.ph, h - oy)
        if sx1 <= sx0 or sy1 <= sy0:
            continue
        out.alpha_composite(cell.crop((sx0, sy0, sx1, sy1)),
                            (i % sheet.cols * w + ox + sx0, i // sheet.cols * h + oy + sy0))
    out.save(A.ROOT / cfg['source'])
    if pad_l or pad_r or pad_t or pad_b:
        path = clip_path(atlas)
        data = json.loads(path.read_text(encoding='utf-8'))
        data['frameWidth'], data['frameHeight'] = w, h
        data['rootX'], data['rootY'] = sheet.prx + d * pad_l, sheet.pry + d * pad_t
        if 'video' in data:
            v = data['video']
            v['largura'] = w // d
            v['celula'] = h // d
            rx, ry = v.get('raiz', '128,238').split(',')
            v['raiz'] = f'{int(rx) + pad_l},{int(ry) + pad_t}'
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    A.Sheet.cache.pop(cfg['source'], None)


def centralizar(cid, atlas, spec, ver=False):
    """spec: "REFERENCIA,LIMITE" ou "REFERENCIA,LIMITE,v" (também na vertical). Devolve (dx, dy)."""
    partes = spec.split(',')
    ref_estado, limite, vertical = partes[0], float(partes[1]), len(partes) > 2 and partes[2] == 'v'
    pack, clips = A.load_pack(cid)
    ref = referencia(pack, clips, ref_estado)
    cfg = clips[atlas]
    dx, dy = deslocamentos(A.Sheet(cfg), cfg['expectedFrames'], ref, limite, vertical)
    if not ver and (dx.any() or dy.any()):
        aplicar_na_folha(atlas, clips, dx, dy)
    return dx, dy


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('estados', nargs='+')
    p.add_argument('--ref', required=True, help='estado de referência (CROUCH_IDLE, JUMP, IDLE)')
    p.add_argument('--limite', type=float, default=16, help='px que o tronco pode andar da referência')
    p.add_argument('--vertical', action='store_true', help='também na vertical (golpes no ar)')
    p.add_argument('--ver', action='store_true', help='só mostra os deslocamentos')
    args = p.parse_args()
    spec = f'{args.ref},{args.limite:g}' + (',v' if args.vertical else '')
    pack, clips = A.load_pack(args.personagem)
    feitos = set()
    for estado in args.estados:
        atlas = pack['animations'][estado]['atlas']
        if atlas in feitos:
            continue
        feitos.add(atlas)
        dx, dy = centralizar(args.personagem, atlas, spec, args.ver)
        print(f'{args.personagem} {estado}: dx ' + ' '.join(f'{v:+d}' for v in dx)
              + ('  dy ' + ' '.join(f'{v:+d}' for v in dy) if args.vertical else ''))
        if args.ver or not (dx.any() or dy.any()):
            continue
        path = clip_path(atlas)
        data = json.loads(path.read_text(encoding='utf-8'))
        if 'video' in data:
            data['video']['centralizar'] = spec
        else:
            data['centralizado'] = f'tronco no eixo de {spec} por tools/sprites/centralizar.py'
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
