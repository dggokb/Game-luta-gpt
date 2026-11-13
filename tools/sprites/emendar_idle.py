#!/usr/bin/env python3
"""Todo golpe de chão termina emendado na postura parada (regra do dono, 13/11).

O último quadro de um golpe em pé tem de cair onde o IDLE começa (o 1º quadro), e o de um golpe
agachado onde o CROUCH_IDLE começa: senão, na troca, o lutador pula de lugar (a Ombrada do Ryūga
terminava 60 px à frente do idle). O vídeo de cada golpe começa e termina na guarda, mas a montagem
prende o corpo pelo começo do vídeo (a corrida inclinada, o passo), e a guarda do fim sai fora do
lugar.

Medida (px lógicos, da raiz): o eixo do tronco (centralizar.eixo, a coluna com mais desenho) do
último quadro contra o do 1º quadro da postura, e a altura dos pés (o pixel mais baixo): o fim do
Carrinho da Yin-lu terminava 4 px afundado no chão e subia na troca. Correção: na volta para a
guarda (do fim da janela ativa ao último quadro) o desenho anda aos poucos até o lugar da
postura, sem tranco.

`assentar` (--assentar CROUCH CROUCH_IDLE RISE): posturas que são sempre no chão ficam com os pés
na altura do IDLE em todos os quadros (a Yin-lu agachada flutuava 7 px). Mede também quanto as duas poses batem depois de alinhadas
(IoU das silhuetas): pose diferente (outra base, outro braço na frente) não se resolve andando, e
a conferência acusa para escolher quadros ou refazer o vídeo.

    python3 tools/sprites/emendar_idle.py                       # confere o elenco (mostra)
    python3 tools/sprites/emendar_idle.py p02 --write           # corrige o que passar do limite
    python3 tools/sprites/emendar_idle.py player_base --write --golpes DH H

Edita a folha do clipe (a fonte e as referências do mesmo tamanho, como a nitidez HD) e grava
"emendar_idle" na receita do vídeo, para a reconversão repetir. Depois rode caixas_sprites.py e
build_characters.py --write (e o normal_map.py das folhas mexidas).
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
from centralizar import eixo  # noqa: E402

LIMITE = 6          # px lógicos de diferença tolerada entre o fim do golpe e a postura
LIMITE_PE = 2       # px de diferença tolerada na altura dos pés
POSE_MINIMA = 0.80  # IoU mínimo das silhuetas alinhadas (abaixo: pose diferente da postura)
EM_PE = ('L', 'M', 'H', '6M', '6H', 'DH')
AGACHADO = ('2L', '2M', '2H')


def golpes(pack):
    """[(id, animação, postura de referência, golpe)] dos golpes de chão que voltam à postura."""
    out = []
    for gid in EM_PE + AGACHADO:
        m = pack['moves'].get(gid)
        if m and 'animation' in m:
            ref = 'CROUCH_IDLE' if gid in AGACHADO else 'IDLE'
            if ref not in pack['animations']:
                ref = 'CROUCH' if gid in AGACHADO else None
            if ref:
                out.append((gid, m['animation'], ref, m))
    for gid, m in pack.get('specialMoves', {}).items():
        if 'animation' in m and not m.get('air'):
            out.append((gid, m['animation'], 'IDLE', m))
    return [g for g in out if g[1] in pack['animations']]


def quadro_ref(pack, ref):
    a = pack['animations'][ref]
    return a['atlas'], (a['frames'][-1] if ref == 'CROUCH' else a['frames'][0])


def silhueta(sheet, i):
    """Máscara do quadro com a raiz no centro de uma tela fixa (para comparar folhas diferentes)."""
    f = sheet.frame(i)[..., 3] > 128
    tela = np.zeros((400, 800), bool)
    oy, ox = 380 - sheet.ry, 400 - sheet.rx
    h, w = f.shape
    y0, x0 = max(0, oy), max(0, ox)
    y1, x1 = min(400, oy + h), min(800, ox + w)
    tela[y0:y1, x0:x1] = f[y0 - oy:y1 - oy, x0 - ox:x1 - ox]
    return tela


def pes(sheet, i):
    """Linha do pixel mais baixo do quadro, da raiz (positivo: abaixo do chão)."""
    f = sheet.frame(i)[..., 3] > 128
    ys = np.nonzero(f.any(1))[0]
    return int(ys.max()) - sheet.ry if len(ys) else None


def iou(a, b, dx):
    b = np.roll(b, int(round(dx)), axis=1)
    u = (a | b).sum()
    return float((a & b).sum() / u) if u else 1.0


def medir(pack, clips, golpe):
    """{dx, pose, atlas, ...} de um golpe: dx = quanto o último quadro tem de andar (px lógicos)."""
    gid, anim, ref, m = golpe
    a = pack['animations'][anim]
    sheet = A.Sheet(clips[a['atlas']])
    ratlas, ri = quadro_ref(pack, ref)
    rsheet = A.Sheet(clips[ratlas])
    fim = a['frames'][-1]
    e, er = eixo(sheet.frame(fim), sheet.rx, sheet.ry), eixo(rsheet.frame(ri), rsheet.rx, rsheet.ry)
    if e is None or er is None:
        return None
    dx = er[0] - e[0]
    dy = pes(rsheet, ri) - pes(sheet, fim)
    pose = iou(silhueta(rsheet, ri), silhueta(sheet, fim), dx)
    return {'golpe': gid, 'anim': anim, 'ref': ref, 'atlas': a['atlas'], 'dx': dx, 'dy': dy, 'pose': pose}


def rampa(pack, anim, m, dx):
    """{célula: dx} na volta para a guarda: do fim da janela ativa (na proporção do tempo da
    animação) ao último quadro, de 0 a dx."""
    a = pack['animations'][anim]
    d = a['durationsMs']
    total = m.get('startupFrames', 0) + m.get('activeFrames', 0) + m.get('recoveryFrames', 0)
    fim_ativo = m.get('startupFrames', 0) + m.get('activeFrames', 0)
    corte = sum(d) * fim_ativo / total if total else 0
    acum, k0 = 0, len(d) - 1
    for j, dj in enumerate(d):
        if acum >= corte:
            k0 = j
            break
        acum += dj
    k0 = min(k0, len(d) - 3) if len(d) >= 3 else 0
    n = len(d) - 1 - k0
    por_celula = {}
    for j in range(k0, len(d)):
        por_celula[a['frames'][j]] = dx * (j - k0) / n if n else dx
    return por_celula


def deslocar_folhas(atlas, clips, dxs, dys=None):
    """Desloca as células (px lógicos) na fonte do clipe e nas referências do mesmo tamanho,
    alargando a célula se o desenho encostar na borda dos lados."""
    dys = np.zeros(len(dxs), int) if dys is None else dys
    cfg = clips[atlas]
    sheet = A.Sheet(cfg)
    n = cfg['expectedFrames']
    d = sheet.d
    lefts, rights = [], []
    for i in range(n):
        msk = sheet.frame(i)[..., 3] > 0
        if not msk.any():
            continue
        xs = np.nonzero(msk.any(0))[0]
        lefts.append(xs.min() + dxs[i])
        rights.append(xs.max() + dxs[i])
    pad_l = int(max(0, MARGIN - min(lefts)))
    pad_r = int(max(0, max(rights) - (sheet.w - 1 - MARGIN)))
    w = sheet.pw + d * (pad_l + pad_r)
    rows = (n + sheet.cols - 1) // sheet.cols
    tamanho = Image.open(A.ROOT / cfg['source']).size
    fontes = [cfg['source']] + [s for s in cfg.get('sourceReferences', [])
                                if (A.ROOT / s).exists() and Image.open(A.ROOT / s).size == tamanho]
    for fonte in fontes:
        img = Image.open(A.ROOT / fonte).convert('RGBA')
        out = Image.new('RGBA', (w * sheet.cols, sheet.ph * rows), (0, 0, 0, 0))
        for i in range(n):
            x, y = i % sheet.cols * sheet.pw, i // sheet.cols * sheet.ph
            cell = img.crop((x, y, x + sheet.pw, y + sheet.ph))
            ox, oy = d * (pad_l + int(dxs[i])), d * int(dys[i])
            sx0, sx1 = max(0, -ox), min(sheet.pw, w - ox)
            sy0, sy1 = max(0, -oy), min(sheet.ph, sheet.ph - oy)
            if sx1 > sx0 and sy1 > sy0:
                out.alpha_composite(cell.crop((sx0, sy0, sx1, sy1)), (i % sheet.cols * w + ox + sx0, y + oy + sy0))
        out.save(A.ROOT / fonte)
        A.Sheet.cache.pop(fonte, None)
    path = clip_path(atlas)
    data = json.loads(path.read_text(encoding='utf-8'))
    if pad_l or pad_r:
        data['frameWidth'] = w
        data['rootX'] = sheet.prx + d * pad_l
        if 'video' in data:
            data['video']['largura'] = w // d
            rx, ry = data['video'].get('raiz', '128,238').split(',')
            data['video']['raiz'] = f'{int(rx) + pad_l},{ry}'
    if 'video' in data:
        data['video']['emendar_idle'] = True
    else:
        data['emendadoIdle'] = 'fim do golpe no lugar da postura por tools/sprites/emendar_idle.py'
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def emendar(cid, atlas=None, so=None, limite=LIMITE, write=True):
    """Corrige os golpes do personagem (ou só os da folha `atlas`, ou só os ids `so`). Devolve as
    medidas de antes."""
    pack, clips = A.load_pack(cid)
    feitos = []
    usos = {}
    for a in pack['animations'].values():
        usos[a['atlas']] = usos.get(a['atlas'], 0) + 1
    for g in golpes(pack):
        if so and g[0] not in so:
            continue
        r = medir(pack, clips, g)
        if r is None or (atlas and r['atlas'] != atlas):
            continue
        feitos.append(r)
        if not write or (abs(r['dx']) <= limite and abs(r['dy']) <= LIMITE_PE):
            continue
        if usos[r['atlas']] > 1:
            r['aviso'] = 'folha usada por outra animação: não mexi'
            continue
        cfg = clips[r['atlas']]
        dxs, dys = np.zeros(cfg['expectedFrames']), np.zeros(cfg['expectedFrames'])
        for c, v in rampa(pack, g[1], g[3], r['dx']).items():
            dxs[c] = v
        for c, v in rampa(pack, g[1], g[3], r['dy']).items():
            dys[c] = v
        deslocar_folhas(r['atlas'], clips, np.round(dxs).astype(int), np.round(dys).astype(int))
        r['feito'] = True
    return feitos


def assentar(cid, estados, write=True):
    """Posturas sempre no chão (agachar, agachado parado, levantar): cada quadro com os pés na
    altura dos pés do 1º quadro do IDLE. Devolve {estado: [dy por quadro]}."""
    pack, clips = A.load_pack(cid)
    ra = pack['animations']['IDLE']
    alvo = pes(A.Sheet(clips[ra['atlas']]), ra['frames'][0])
    out = {}
    for estado in estados:
        a = pack['animations'][estado]
        cfg = clips[a['atlas']]
        sheet = A.Sheet(cfg)
        dys = np.zeros(cfg['expectedFrames'], int)
        for i in sorted(set(a['frames'])):
            p = pes(sheet, i)
            if p is not None:
                dys[i] = alvo - p
        out[estado] = list(dys)
        if write and np.abs(dys).max() > 0:
            deslocar_folhas(a['atlas'], clips, np.zeros(len(dys), int), dys)
            clips = A.load_pack(cid)[1]
    return out


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='*')
    p.add_argument('--golpes', nargs='*', help='só estes golpes (L, DH, S2...)')
    p.add_argument('--limite', type=float, default=LIMITE)
    p.add_argument('--write', action='store_true')
    p.add_argument('--assentar', nargs='*', help='posturas sempre no chão: os pés na altura do IDLE em todo quadro')
    a = p.parse_args()
    if a.assentar:
        for cid in a.personagens:
            for estado, dys in assentar(cid, a.assentar, write=a.write).items():
                print(f'{cid} {estado}: dy ' + ' '.join(f'{v:+d}' for v in dys))
        return
    ids = a.personagens or sorted(x.parent.name for x in (A.ROOT / 'characters').glob('*/character.json'))
    ruins = 0
    for cid in ids:
        try:
            rs = emendar(cid, so=a.golpes, limite=a.limite, write=a.write)
        except (KeyError, FileNotFoundError) as e:
            print(f'{cid}: sem folhas ({e})')
            continue
        for r in rs:
            fora = abs(r['dx']) > a.limite or abs(r['dy']) > LIMITE_PE
            pose = r['pose'] < POSE_MINIMA
            ruins += fora or pose
            marca = ('corrigido' if r.get('feito') else 'FORA') if fora else 'ok'
            print(f"{cid:12s} {r['golpe']:5s} {r['anim']:22s} → {r['ref']:11s} dx {r['dx']:+6.1f} dy {r['dy']:+3d}  pose {r['pose']:.2f}"
                  f"  {marca}{'  POSE DIFERENTE' if pose else ''}{'  ' + r['aviso'] if r.get('aviso') else ''}")
    if not a.write:
        print(f'{ruins} golpe(s) fora da regra')


if __name__ == '__main__':
    main()
