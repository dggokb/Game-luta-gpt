#!/usr/bin/env python3
"""A inclinação do membro que bate em cada quadro dos golpes, para a faísca de acerto seguir a
linha do golpe (o chute que sobe joga a faísca para cima, o chute baixo para baixo).

Mede no desenho o mesmo membro das caixas do caixas_sprites.py (o que mexeu desde o primeiro
quadro do golpe, ligado à ponta mais à frente) e tira a linha dele na parte da frente (perto do
corpo entram a cabeça e o ombro que mexeram). Grava só isto, em graus (positivo para cima), na
silhueta de cada animação de golpe (`silhueta.angulo`, por quadro da folha): não mexe nas caixas,
no alcance nem no corpo, que o caixas_sprites.py --write mediria de novo.

    python3 tools/sprites/angulo_golpe.py                 # mostra
    python3 tools/sprites/angulo_golpe.py player_base --write
    python3 tools/sprites/build_characters.py --write     # leva para o jogo
"""
import argparse
import json
import math
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import caixas_sprites as cx  # noqa: E402

ROOT = HERE.parents[1]
FRENTE = 0.6         # a parte do membro que dá a linha: os 60% da frente, na horizontal
MAO = 0.15           # ... menos a ponta (a mão aberta, o pé), que tem forma própria
MAXIMO = 40          # graus: a faísca não deita mais que isto
SALTO = 8            # px do mundo: o vão que separa dois pedaços na mesma coluna


def membro_da_caixa(pack, a, i, nome, lancador):
    """(dx, h) dos pixels do membro que a caixa do golpe cobre (a mesma escolha do caixas_sprites.ponta)."""
    meia = pack['fighter']['body']['halfWidth']
    alto = pack['fighter']['body']['standHeight']
    antes = a['frames'][0]
    if nome == lancador:
        _, rep, k = cx.ler(pack['id'], a['atlas'])
        lay = rep['layout']
        folga = max(2, int(round(6 / k)))
        perto = cx.cv2.dilate(cx.celula(a['atlas'], antes).astype(np.uint8), np.ones((2 * folga + 1,) * 2, np.uint8)) > 0
        ys, xs = np.nonzero(cx.celula(a['atlas'], i) & ~perto)
        dx = (xs - lay['rootX']) * k
        if pack.get('artFacing') == 'left':
            dx = -dx
        h = (lay['rootY'] - ys) * k
        sobe = (dx > 0) & (h >= alto * 0.5)
        return dx[sobe], h[sobe]
    dx, h = cx.membro(pack, a['atlas'], i, antes, meia)
    if not len(dx):
        return dx, h
    frente = dx >= meia * 0.5
    return dx[frente], h[frente]


def angulo(dx, h, cintura):
    """Graus da linha do membro na parte da frente (positivo para cima), ou None.

    Só a parte da frente (FRENTE da largura): perto do corpo o que mexeu inclui a cabeça e o ombro
    (o corpo que inclina para trás no chute). A linha é seguida da ponta para trás, coluna por
    coluna, sempre no mesmo pedaço (o joelho embaixo do braço que soca não entra), pelo meio dele
    (a grossura do membro não pesa), sem a mão ou o pé da ponta. Membro em pé (o gancho que sobe, o pisão): o máximo, para
    cima se está acima da {@code cintura} de quem bate."""
    if len(dx) < 30:
        return None
    x0, x1 = dx.min(), dx.max()
    largo = x1 - x0
    if largo < 10:
        return None
    frente = dx >= x1 - FRENTE * largo
    xs, hs = np.round(dx[frente]).astype(int), h[frente]
    pontos, alturas, meio = [], [], None
    for c in sorted(np.unique(xs), reverse=True):
        col = np.sort(hs[xs == c])
        # Os pedaços desta coluna (um vão de mais de SALTO px separa).
        cortes = np.nonzero(np.diff(col) > SALTO)[0] + 1
        pedacos = np.split(col, cortes)
        p = min(pedacos, key=lambda q: abs(q.mean() - meio)) if meio is not None else max(pedacos, key=lambda q: q.mean())
        if meio is not None and abs(p.mean() - meio) > SALTO * 3:
            break
        pontos.append((c, p.mean()))
        alturas.append(np.ptp(p))
        meio = p.mean()
    if len(pontos) < 5:
        return None
    cs, ms = np.array(pontos, dtype=float).T
    if np.median(alturas) > 1.5 * np.ptp(cs):
        # O pedaço seguido é mais alto que largo: o membro em pé.
        return MAXIMO if ms.mean() > cintura else -MAXIMO
    # A linha sem a mão ou o pé da ponta (os dedos abertos para cima entortavam o soco reto).
    braco = cs <= x1 - MAO * largo
    if braco.sum() >= 5:
        cs, ms = cs[braco], ms[braco]
    g = math.degrees(math.atan(np.polyfit(cs, ms, 1)[0]))
    return int(round(max(-MAXIMO, min(MAXIMO, g))))


def medir(pack):
    """{animação: {quadro: graus}} dos quadros que têm caixa de golpe."""
    lancador = pack['moves'].get('2H', {}).get('animation')
    # O golpe para baixo no ar (j2H): a ponta mais à frente do desenho é o joelho, não o pé que
    # desce; a faísca vai para baixo de qualquer jeito.
    para_baixo = pack['moves'].get('j2H', {}).get('animation')
    saida = {}
    for nome in cx.animacoes_de_golpe(pack):
        a = pack['animations'][nome]
        golpe = a.get('silhueta', {}).get('golpe', {})
        angulos = {}
        for q in golpe:
            if nome == para_baixo:
                angulos[q] = -MAXIMO
                continue
            g = angulo(*membro_da_caixa(pack, a, int(q), nome, lancador), pack['fighter']['body']['standHeight'] * 0.5)
            if g:
                angulos[q] = g
        saida[nome] = angulos
    return saida


def gravar(pack, medidos=None):
    """Põe as inclinações na silhueta das animações (o caixas_sprites.py --write chama depois de
    refazer as caixas). Sem caixas medidas não há nada a inclinar: a animação fica como está."""
    for nome, angulos in (medidos if medidos is not None else medir(pack)).items():
        sil = pack['animations'][nome].get('silhueta')
        if sil is None:
            continue
        if angulos:
            sil['angulo'] = angulos
        else:
            sil.pop('angulo', None)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='*')
    p.add_argument('--write', action='store_true')
    a = p.parse_args()
    for path in sorted((ROOT / 'characters').glob('*/character.json')):
        pack = json.loads(path.read_text(encoding='utf-8'))
        if pack['id'] in cx.FORA or (a.personagens and pack['id'] not in a.personagens):
            continue
        medidos = medir(pack)
        por_anim = {m['animation']: b for b, m in pack['moves'].items() if 'animation' in m}
        print(pack['id'])
        for nome, angulos in medidos.items():
            if angulos:
                print(f"    {por_anim.get(nome, nome):6} {nome:28} " + ' '.join(f'{q}:{g:+d}' for q, g in sorted(angulos.items(), key=lambda kv: int(kv[0]))))
        if a.write:
            gravar(pack, medidos)
            path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    if a.write:
        print('agora: python3 tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
