#!/usr/bin/env python3
"""A inclinação do membro que bate em cada quadro dos golpes, para a faísca de acerto seguir a
linha do golpe (o chute que sobe joga a faísca para cima, o chute baixo para baixo).

Mede no desenho o mesmo membro das caixas do caixas_sprites.py (o que mexeu desde o primeiro
quadro do golpe, ligado à ponta mais à frente) e tira a linha dele na parte da frente (perto do
corpo entram a cabeça e o ombro que mexeram). Grava só isto, em graus (positivo para cima), na
silhueta de cada animação de golpe (`silhueta.angulo`, por quadro da folha): não mexe nas caixas,
no alcance nem no corpo, que o caixas_sprites.py --write mediria de novo.

O rastro do golpe (a fita que segue o caminho do braço ou da perna) é do linha_de_acao.py.

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


def linha(dx, h, cintura):
    """Graus da linha do membro na parte da frente (positivo para cima; 90 em pé), ou None.

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
        return 90.0 if ms.mean() > cintura else -90.0
    # A linha sem a mão ou o pé da ponta (os dedos abertos para cima entortavam o soco reto).
    braco = cs <= x1 - MAO * largo
    if braco.sum() >= 5:
        cs, ms = cs[braco], ms[braco]
    return math.degrees(math.atan(np.polyfit(cs, ms, 1)[0]))


def semitransparente(pack, a, q):
    """Quantos pixels meio transparentes o quadro q tem (o brilho de um efeito desenhado na arte)."""
    alfa, rep, _ = cx.ler(pack['id'], a['atlas'])
    lay = rep['layout']
    fw, fh, cols = lay['frameWidth'], lay['frameHeight'], lay['columns']
    r, c = divmod(q, cols)
    cel = alfa[r * fh:(r + 1) * fh, c * fw:(c + 1) * fw]
    return int(((cel >= 24) & (cel < cx.OPACO)).sum())


CONFERE_RAIO = 110      # px do mundo: o pedaço do membro perto da ponta que dá a segunda medida
CONFERE_GRAUS = 25      # as duas medidas da direção têm de bater até aqui
CONFERE_PONTA = 40      # ... e as duas pontas, até aqui (px)


def ponta_do_golpe(dx, h, graus):
    """(x, y) da ponta do membro: o fim dele na linha de {@code graus}."""
    ao_longo = dx * math.cos(math.radians(graus)) + h * math.sin(math.radians(graus))
    fim = ao_longo >= np.percentile(ao_longo, 98)
    return float(dx[fim].mean()), float(h[fim].mean())


def ponta_pelo_corpo(dx, h, corpo_dx, corpo_h):
    """(x, y, graus) por outro caminho: a ponta é o pixel do membro mais longe do meio do corpo, e
    a direção é o eixo mais comprido dos pixels do membro em volta dela (apontando para a ponta)."""
    d2 = (dx - corpo_dx.mean()) ** 2 + (h - corpo_h.mean()) ** 2
    longe = d2 >= np.percentile(d2, 99)
    tx, ty = dx[longe].mean(), h[longe].mean()
    perto = (dx - tx) ** 2 + (h - ty) ** 2 <= CONFERE_RAIO ** 2
    if perto.sum() < 30:
        return None
    px, py = dx[perto] - dx[perto].mean(), h[perto] - h[perto].mean()
    _, eixos = np.linalg.eigh(np.cov(np.vstack([px, py])))
    ex, ey = eixos[:, 1]
    if ex * (tx - dx[perto].mean()) + ey * (ty - h[perto].mean()) < 0:
        ex, ey = -ex, -ey
    return float(tx), float(ty), math.degrees(math.atan2(ey, ex))


def direcao_conferida(pack, a, q, nome, lancador, cintura):
    """(x, y, graus) da ponta do golpe no quadro q, ou None quando as duas medidas não batem.

    O membro medido às vezes não é o que bate (a perna que dá o passo vai mais à frente que o
    punho; a mão aberta fica em pé na ponta do braço). A linha do membro e a medida pelo corpo
    erram em lugares diferentes: só vale quando as duas concordam na ponta e na direção. Sem
    isso o golpe fica reto e sem rastro, como era antes."""
    dx, h = membro_da_caixa(pack, a, q, nome, lancador)
    if len(dx) < 30:
        return None
    g = linha(dx, h, cintura)
    if g is None:
        return None
    x, y = ponta_do_golpe(dx, h, g)
    outra = ponta_pelo_corpo(dx, h, *cx.quadro(pack, a['atlas'], q))
    if outra is None:
        return None
    desvio = abs((outra[2] - g + 180) % 360 - 180)
    if desvio > CONFERE_GRAUS or math.hypot(outra[0] - x, outra[1] - y) > CONFERE_PONTA:
        return None
    return x, y, g


def medir(pack):
    """{animação: {quadro: graus}} dos quadros que têm caixa de golpe."""
    lancador = pack['moves'].get('2H', {}).get('animation')
    # O golpe para baixo no ar (j2H): a ponta mais à frente do desenho é o joelho, não o pé que
    # desce; a faísca vai para baixo de qualquer jeito.
    para_baixo = pack['moves'].get('j2H', {}).get('animation')
    errada = {pack['moves'][g]['animation'] for g in MEDIDA_ERRADA.get(pack['id'], ()) if g in pack['moves']}
    saida = {}
    for nome in cx.animacoes_de_golpe(pack):
        a = pack['animations'][nome]
        golpe = {} if nome in errada else a.get('silhueta', {}).get('golpe', {})
        angulos = {}
        for q in golpe:
            if nome == para_baixo:
                angulos[q] = -MAXIMO
                continue
            d = direcao_conferida(pack, a, int(q), nome, lancador, pack['fighter']['body']['standHeight'] * 0.5)
            g = int(round(max(-MAXIMO, min(MAXIMO, d[2])))) if d else 0
            if g:
                angulos[q] = g
        saida[nome] = angulos
    return saida


# A arte destes golpes já traz o efeito desenhado (onda de ar em meia-lua, estouro, arco no chão,
# borrão): o rastro do jogo não entra por cima. Conferido no olho, quadro por quadro; os outros
# personagens não têm efeito na arte. Golpe novo com efeito desenhado entra aqui.
ARTE_COM_EFEITO = {
    # p01: o efeito do vídeo saiu dos golpes M e H em 11/11 (smear no lugar; tools/sprites/smear.py).
    'player_base': {'2L', 'jL'},
    'ryuga_8': {'H', '2L', '2M', '2H', 'j2H', '6M'},
    'p10': {'jM'},
    'p13': {'2L'},
    'p15': {'jL'},
}
# Golpes em que a medida sai errada mesmo com as duas contas batendo (o pé de apoio no lugar do
# punho, a barra do jaleco, o efeito desenhado puxando a linha): ficam retos e sem rastro.
# Conferido no olho nas folhas do quadro do impacto.
MEDIDA_ERRADA = {
    'p02': {'2L'},
    'p09': {'jH'},
    'p10': {'jH'},
    'p11': {'jH'},
    'p13': {'jM'},
    'p15': {'jL'},
    'p16': {'2H', 'jH'},
}
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
        sil.pop('rastro', None)   # o rastro antigo (a onda na ponta): hoje é o linha_de_acao.py


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
