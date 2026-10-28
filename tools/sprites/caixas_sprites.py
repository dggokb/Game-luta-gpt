#!/usr/bin/env python3
"""Caixas de colisão de cada personagem medidas no desenho (as folhas do jogo).

Hurt (o corpo, fighter.body):
    standHeight   altura do IDLE (do chão ao topo da cabeça, mediana dos quadros)
    crouchHeight  altura do agachado parado (CROUCH_IDLE, ou o fim do CROUCH)
    halfWidth     meia largura do tronco (do peito ao quadril, sem os pés abertos nem a cabeça)
    pushHalfWidth 0,9 da meia largura do tronco desenhado parado (a faixa do meio da silhueta
                  do IDLE, com a guarda): dois lutadores encostam sem um entrar no outro
    pushHeight    mantém a proporção que tinha com standHeight (no máximo 60 acima do
                  agachado, como o build pede).

Hit (cada golpe normal: L, M, H, 2L, 2M, 2H, jL, jM, jH, j2H, 6M), no quadro mais esticado
entre o do impacto (um antes) e três depois:
    reach         até onde vai a ponta do membro que bate (px à frente da raiz); no j2H (golpe
                  para baixo no ar), a parte mais baixa à frente da raiz; no 2H (anti-aéreo,
                  lançador), o ponto mais alto, com a caixa descendo pelo caminho da perna
    hitHeight / hitFloor  a faixa de altura da ponta (os últimos PONTA px do golpe; em pé, a
                  base desce pelo membro inteiro à frente do corpo: o chute alto na diagonal). A caixa
                  do motor vai de hitFloor a hitHeight + 19,5 (CombatRules.VERTICAL_SLACK).

Ultra (ultras.json "alcance", nos que não têm o lugar do rival feito à mão): a mediana da ponta
das pancadas desenhadas + 15 px; o rival sai de onde o avanço encostou e vai até ela.

Px do mundo: px da folha × worldScale do perfil ÷ pixelScale da folha, contados da raiz da
célula, como o SpriteFighterRenderer desenha. Só os pixels bem opacos contam (rastro e brilho
meio transparentes do vídeo não alongam o golpe). Especiais, Super e Ultra ficam como estão
(alcance de projeto, em tools/sprites/mecanicas.json).

    python3 tools/sprites/caixas_sprites.py             # mostra antes -> depois
    python3 tools/sprites/caixas_sprites.py --write p14 # grava (todos sem nomes)
    python3 tools/sprites/build_characters.py --write
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
NORMAIS = ('L', 'M', 'H', '2L', '2M', '2H', 'jL', 'jM', 'jH', 'j2H', '6M')
# Fora: o boneco do treino e o monstro (não estão na seleção). O p01 (player_base) entra; a
# calibração de desenho dele (P01SpriteCalibration, ~2% de escala e poucos px) fica de fora da conta.
FORA = {'player_two', 'monster_npc'}
# Agachar desenhado já calibrado (o jogo redimensiona ao mostrar): a folha crua não serve de medida.
AGACHAR_CALIBRADO = {'player_base'}
OPACO = 160          # alfa mínimo de um pixel que conta (0-255)
PONTA = 45           # px do mundo: o pedaço da ponta do golpe que dá a altura da caixa
FOLGA = 19.5         # CombatRules.VERTICAL_SLACK
FAIXAS = 3           # caixas do corpo por quadro do golpe (cabeça, tronco, pernas)
LADO = 60            # px do mundo: o quanto a ponta vai para os lados do ponto que bate
_folhas = {}
ESCOLHIDO = {}   # o quadro medido de cada golpe (a folha de conferência mostra ele)


def ler(pack_id, atlas):
    """(alfa da folha HxW, relatório, escala px da folha -> px do mundo)."""
    if atlas not in _folhas:
        clip = json.loads((HERE / 'clips' / f'{atlas}.json').read_text(encoding='utf-8'))
        rep = json.loads((HERE / 'reports' / f'{atlas}.report.json').read_text(encoding='utf-8'))
        im = Image.open(ROOT / clip['source'])
        a = np.array(im.getchannel('A')) if im.mode in ('RGBA', 'LA') else np.array(im.convert('RGBA'))[..., 3]
        _folhas[atlas] = (a, rep, rep['worldScale'] / rep.get('pixelScale', 1))
    return _folhas[atlas]


def celula(atlas, i):
    """Máscara dos pixels opacos do quadro i na célula da folha."""
    a, rep, _ = _folhas[atlas]
    lay = rep['layout']
    fw, fh, cols = lay['frameWidth'], lay['frameHeight'], lay['columns']
    r, c = divmod(i, cols)
    return a[r * fh:(r + 1) * fh, c * fw:(c + 1) * fw] >= OPACO


def quadro(pack, atlas, i):
    """Pixels opacos do quadro i: (dx, h) em px do mundo — dx à frente da raiz, h acima do chão."""
    _, rep, k = ler(pack['id'], atlas)
    lay = rep['layout']
    mask = celula(atlas, i)
    ys, xs = np.nonzero(mask)
    dx = (xs - lay['rootX']) * k
    if pack.get('artFacing') == 'left':
        dx = -dx
    return dx, (lay['rootY'] - ys) * k


def membro(pack, atlas, i, antes, meia, alto=None):
    """O braço ou a perna que bate no quadro i: o que mudou desde o quadro {@code antes} (o corpo
    que já estava lá fica de fora), só o pedaço ligado à ponta mais à frente (as pernas que
    trocam de base também mudam). (dx, h) em px do mundo."""
    _, rep, k = ler(pack['id'], atlas)
    lay = rep['layout']
    folga = max(2, int(round(6 / k)))   # o corpo que mexe um pouco não vira golpe
    perto = cv2.dilate(celula(atlas, antes).astype(np.uint8), np.ones((2 * folga + 1,) * 2, np.uint8)) > 0
    mexeu = (celula(atlas, i) & ~perto).astype(np.uint8)
    n, rotulos = cv2.connectedComponents(mexeu, connectivity=8)
    ys, xs = np.nonzero(mexeu)
    if n <= 1 or not len(xs):
        return np.zeros(0), np.zeros(0)
    dx = (xs - lay['rootX']) * k
    if pack.get('artFacing') == 'left':
        dx = -dx
    alvo = rotulos[ys, xs]
    # A ponta: o pixel mais à frente entre os pedaços que passam do corpo. Com {@code alto} (a
    # altura de quem bate, no anti-aéreo) e algo acima de 80% dela: tudo que mexeu à frente.
    h = (lay['rootY'] - ys) * k
    j = int(np.argmax(dx))
    if alto is not None:
        # Anti-aéreo (2H): a perna que sobe sai em pedaços soltos; vale tudo que mexeu à frente.
        frente = dx > 0
        if frente.sum() >= 20 and h[frente].max() > alto * 0.8:
            return dx[frente], h[frente]
    parte = alvo == alvo[j]
    if parte.sum() < 20:
        return np.zeros(0), np.zeros(0)
    return dx[parte], h[parte]


def anim(pack, nome):
    a = pack['animations'].get(nome)
    return (a['atlas'], a['frames']) if a else (None, [])


def cabeca(h, k):
    """Topo da cabeça: a linha mais alta com corpo de verdade (8 px do mundo), não um fio de cabelo."""
    linhas = np.round(h).astype(int)
    largura = np.bincount(linhas - linhas.min())
    cheias = np.nonzero(largura * k >= 8)[0]
    return float(cheias.max() + linhas.min()) if len(cheias) else float(h.max())


def corpo(pack):
    atlas, frames = anim(pack, 'IDLE')
    alturas, meias = [], []
    for i in sorted(set(frames)):
        dx, h = quadro(pack, atlas, i)
        if not len(h):
            continue
        k = ler(pack['id'], atlas)[2]
        topo = cabeca(h, k)
        alturas.append(topo)
        # Tronco: da barriga ao peito (45% a 65% da altura), o trecho contínuo que passa pela
        # raiz (sem o braço de trás nem o punho da guarda soltos do corpo).
        for faixa in np.linspace(0.45, 0.65, 6):
            linha = np.sort(dx[np.abs(h - topo * faixa) < 1.5])
            if not len(linha):
                continue
            quebra = np.nonzero(np.diff(linha) > 3 * k)[0]
            ini = np.concatenate([[0], quebra + 1])
            fim = np.concatenate([quebra, [len(linha) - 1]])
            j = int(np.argmin([0 if linha[a] <= 0 <= linha[b] else min(abs(linha[a]), abs(linha[b]))
                               for a, b in zip(ini, fim)]))
            meias.append((linha[fim[j]] - linha[ini[j]]) / 2)
    nome = 'CROUCH_IDLE' if 'CROUCH_IDLE' in pack['animations'] else 'CROUCH'
    atlas_c, frames_c = anim(pack, nome)
    if nome == 'CROUCH':
        frames_c = frames_c[-1:]
    agachado = [cabeca(quadro(pack, atlas_c, i)[1], ler(pack['id'], atlas_c)[2]) for i in sorted(set(frames_c))]
    return float(np.median(alturas)), float(np.median(agachado)), float(np.median(meias))


def golpe(pack, binding, meia, alto):
    """(reach, hitHeight, hitFloor ou None) do golpe, ou None se não dá para medir."""
    m = pack['moves'].get(binding)
    if not m or 'reach' not in m or 'animation' not in m:
        return None
    a = pack['animations'].get(m['animation'])
    if not a or 'impactFrame' not in a:
        return None
    i = a['impactFrame']
    # O quadro mais esticado do impacto ao ativo (o impacto às vezes cai um quadro antes).
    vizinhos = sorted({a['frames'][j] for j in range(max(0, i - 1), min(len(a['frames']), i + 4))})
    melhor = None
    for f in vizinhos:
        dx, h = quadro(pack, a['atlas'], f)
        frente = dx > meia
        if frente.sum() < 20:
            continue
        # O anti-aéreo (2H) pelo quadro em que chega mais alto; o resto pelo mais à frente.
        nota = float(h[dx > 0].max()) if binding == '2H' else float(dx[frente].max())
        if melhor is None or nota > melhor[0]:
            melhor = (nota, f)
    if melhor is None:
        return None
    f = melhor[1]
    ESCOLHIDO[binding] = f
    # O membro que bate: o que mudou desde o primeiro quadro do golpe (o corpo que já estava lá
    # fica de fora: sem ele o soco curto virava uma coluna do chão ao punho).
    dx, h = membro(pack, a['atlas'], f, a['frames'][0], meia,
                   alto if binding == '2H' else None)
    if len(dx) < 20:
        dx, h = quadro(pack, a['atlas'], f)
        mexeu = dx > meia
        dx, h = dx[mexeu], h[mexeu]
    largura, altura = dx.max() - max(dx.min(), 0.0), h.max() - h.min()
    if binding == 'j2H' and h.min() < h.max() * 0.4:
        # Golpe para baixo no ar: bate o que está mais embaixo à frente da raiz.
        j = int(np.argmin(h))
        ponta = (h <= h[j] + 1.5 * PONTA) & (np.abs(dx - dx[j]) <= LADO)
    elif altura > 1.3 * largura and binding == '2H':
        # Golpe que sobe (o chute alto, o anti-aéreo): bate o topo.
        j = int(np.argmax(h))
        ponta = (h >= h[j] - 1.5 * PONTA) & (np.abs(dx - dx[j]) <= LADO)
    else:
        # Para a frente: a ponta mais à frente, só na altura dela (sem o joelho embaixo).
        j = int(np.argmax(dx))
        ponta = (dx >= dx[j] - 1.5 * PONTA) & (np.abs(h - h[j]) <= LADO)
    reach = float(dx[ponta].max())
    lo, hi = float(h[ponta].min()), float(h[ponta].max())
    if reach < meia and binding != '2H':
        return None   # o que mexeu não passa da frente do corpo: não dá para medir, fica como está
    if binding in ('L', 'M', 'H', '6M'):
        # Em pé: o chute ou soco na diagonal (o chute alto) pega no caminho do membro inteiro à
        # frente do corpo, não só na ponta lá em cima.
        lo = min(lo, float(h[dx > meia].min(initial=lo)))
    if binding == '2H':
        # O lançador pega no caminho da perna (ou do punho) que sobe, não só lá no alto: tem que
        # acertar quem está em pé, mesmo os baixos (o golpe dos combos).
        lo = max(20.0, min(float(h[dx > meia].min(initial=lo)), alto * 0.5))
        reach = max(reach, lancador_alcance(pack, meia))
    if hi - lo <= 2 * FOLGA:
        return reach, (lo + hi) / 2, None
    return reach, hi - FOLGA, lo


def medir(pack):
    alto, agachado, meia = corpo(pack)
    b = pack['fighter']['body']
    if pack['id'] in AGACHAR_CALIBRADO:
        agachado = b['crouchHeight']
    novo = {
        'halfWidth': round(meia),
        'standHeight': round(alto),
        'crouchHeight': round(agachado),
        'pushHalfWidth': empurrao(pack) or round(meia * b['pushHalfWidth'] / b['halfWidth']),
        # A caixa de empurrão fica perto da altura agachada (o build pede no máximo 60 acima).
        'pushHeight': min(round(alto * b['pushHeight'] / b['standHeight']), round(agachado) + 60),
    }
    # Os projéteis nascem dentro do corpo (o build confere): o corpo não fica mais baixo que eles.
    f = pack['fighter']
    lancados = [d for d in (f.get('energy'), f.get('super')) if d] + \
        [sp['projectile'] for sp in pack.get('specialMoves', {}).values() if sp.get('projectile')]
    for d in lancados:
        for chave, campo in (('spawnY', 'standHeight'), ('airSpawnY', 'standHeight'), ('crouchSpawnY', 'crouchHeight')):
            if chave in d:
                novo[campo] = max(novo[campo], int(np.ceil(d[chave])))
    novo['pushHeight'] = min(novo['pushHeight'], novo['crouchHeight'] + 60)
    golpes = {}
    for binding in NORMAIS:
        g = golpe(pack, binding, novo['halfWidth'], novo['standHeight'])
        if g:
            golpes[binding] = g
    return novo, golpes


def faixas(pack, atlas, i):
    """O corpo no quadro i em FAIXAS caixas (cabeça, tronco, pernas): [x0, x1, y0, y1] cada,
    da silhueta principal (o maior pedaço ligado: o efeito solto do vídeo não apanha)."""
    _, rep, k = ler(pack['id'], atlas)
    lay = rep['layout']
    mask = celula(atlas, i).astype(np.uint8)
    n, rotulos, st, _ = cv2.connectedComponentsWithStats(mask, connectivity=8)
    if n <= 1:
        return []
    maior = 1 + int(np.argmax(st[1:, cv2.CC_STAT_AREA]))
    ys, xs = np.nonzero(rotulos == maior)
    dx = (xs - lay['rootX']) * k
    if pack.get('artFacing') == 'left':
        dx = -dx
    h = (lay['rootY'] - ys) * k
    base, topo = max(0.0, float(h.min())), float(h.max())
    caixas = []
    for j in range(FAIXAS):
        y0 = base + (topo - base) * j / FAIXAS
        y1 = base + (topo - base) * (j + 1) / FAIXAS
        dentro = (h >= y0) & (h <= y1)
        if dentro.sum() < 10:
            continue
        x0, x1 = np.percentile(dx[dentro], [2, 98])
        caixas.append([round(float(x0)), round(float(x1)), round(y0), round(y1)])
    return caixas


def ponta(pack, atlas, i, antes, meia, alto=None):
    """A caixa do membro que bate no quadro i: o que mudou desde o primeiro quadro do golpe
    (ligado à ponta mais à frente), do corpo para a frente. [x0, x1, y0, y1] ou None. Com
    {@code alto} (o lançador): o que mexeu à frente da cintura para cima (o punho ou a perna que
    sobe, não o pé que desliza no chão)."""
    if alto is not None:
        dx, h = quadro(pack, atlas, i)
        _, rep, k = ler(pack['id'], atlas)
        lay = rep['layout']
        folga = max(2, int(round(6 / k)))
        perto = cv2.dilate(celula(atlas, antes).astype(np.uint8), np.ones((2 * folga + 1,) * 2, np.uint8)) > 0
        ys, xs = np.nonzero(celula(atlas, i) & ~perto)
        dx = (xs - lay['rootX']) * k
        if pack.get('artFacing') == 'left':
            dx = -dx
        h = (lay['rootY'] - ys) * k
        sobe = (dx > 0) & (h >= alto * 0.5)
        if sobe.sum() < 30:
            return None
        return [round(float(dx[sobe].min())), round(float(dx[sobe].max())),
                round(float(h[sobe].min())), round(float(h[sobe].max()))]
    dx, h = membro(pack, atlas, i, antes, meia)
    if len(dx) < 20 or dx.max() < meia:
        return None
    frente = dx >= meia * 0.5
    if frente.sum() < 30 or np.ptp(dx[frente]) < 10:
        return None   # pedacinho que mexeu (dobra da roupa), não um braço ou perna
    return [round(float(dx[frente].min())), round(float(dx[frente].max())),
            round(float(h[frente].min())), round(float(h[frente].max()))]


def animacoes_de_golpe(pack):
    """As animações dos golpes que batem: normais, especiais, a S e o Super (o Ultra e o
    agarrão prendem o rival e têm regra própria)."""
    nomes = [m['animation'] for m in pack['moves'].values() if 'animation' in m]
    nomes += [m['animation'] for m in pack.get('specialMoves', {}).values() if 'animation' in m]
    nomes += [pack.get('specialAnimations', {}).get(k) for k in ('S', 'SUPER')]
    return [n for n in dict.fromkeys(nomes) if n and n in pack['animations']]


# Posturas fora dos golpes: o corpo que apanha parado, andando, agachado, no ar, apanhando e
# defendendo (o motor usa a mediana dos quadros de cada uma). Do agachar só o fim.
POSTURAS = ('IDLE', 'WALK_FORWARD', 'WALK_BACK', 'CROUCH_IDLE', 'CROUCH', 'JUMP', 'FALL', 'DASH', 'BACKDASH',
            'HIT_STAND', 'HIT_CROUCH', 'HIT_AIR', 'DEFENSE_STAND', 'DEFENSE_CROUCH', 'DEFENSE_AIR')


def silhuetas(pack):
    """Caixas de cada quadro do desenho: nos golpes o corpo (faixas) e o membro que bate; nas
    posturas só o corpo."""
    meia = pack['fighter']['body']['halfWidth']
    alto = pack['fighter']['body']['standHeight']
    lancador = pack['moves'].get('2H', {}).get('animation')
    for nome in animacoes_de_golpe(pack):
        a = pack['animations'][nome]
        corpo, golpe = {}, {}
        for i in sorted(set(a['frames'])):
            corpo[str(i)] = faixas(pack, a['atlas'], i)
            g = ponta(pack, a['atlas'], i, a['frames'][0], meia, alto if nome == lancador else None)
            if g and nome == lancador:
                # O lançador (2H) sobe colado ao corpo: alcança um pouco além dele e desce até a
                # cintura, como a caixa do golpe (tem que pegar quem está em pé na frente).
                g = [g[0], max(g[1], round(lancador_alcance(pack, meia))), min(g[2], round(alto * 0.5)), g[3]]
            if g:
                golpe[str(i)] = g
        a['silhueta'] = {'corpo': corpo, 'golpe': golpe}
    for nome in POSTURAS:
        a = pack['animations'].get(nome)
        if not a or (nome.startswith('CROUCH') and pack['id'] in AGACHAR_CALIBRADO):
            continue
        quadros = a['frames'][-1:] if nome == 'CROUCH' else sorted(set(a['frames']))
        a['silhueta'] = {'corpo': {str(i): faixas(pack, a['atlas'], i) for i in quadros}, 'golpe': {}}


# Ultras com o lugar do rival feito à mão (agarrão, voo, câmera própria, recuo do zanzoken).
ULTRA_A_MAO = {'p09', 'p13', 'p14', 'p15', 'p16'}


def alcance_do_ultra(pack, spec):
    """Até onde a ponta das pancadas desenhadas do Ultra chega (px à frente da raiz), mediana
    das pancadas, + 15 para a ponta entrar um pouco no rival. None sem folha do Ultra."""
    a = pack['animations'].get('ULTRA_SEQ')
    ult = pack['fighter'].get('ultra')
    if not a or not ult or not ult.get('quadrosVideo'):
        return None
    qv = ult['quadrosVideo']
    pontas = []
    for g in spec['golpes']:
        i = max([j for j, v in enumerate(qv) if v <= g] or [0])
        # Só o personagem (o maior pedaço ligado): a estrela de impacto solta do vídeo não conta.
        corpo = faixas(pack, a['atlas'], a['frames'][min(i, len(a['frames']) - 1)])
        if corpo:
            pontas.append(float(max(c[1] for c in corpo)))
    return round(float(np.median(pontas))) + 15 if pontas else None


EMPURRAO_DO_TRONCO = 0.9


def lancador_alcance(pack, meia):
    """O mínimo que o lançador (2H) alcança à frente da raiz: passa da caixa de empurrão (os
    corpos ficam a duas delas) o bastante para pegar depois do empurrão do L > M."""
    push = empurrao(pack) or pack['fighter']['body']['pushHalfWidth']
    return max(meia + 30.0, push + 50.0)


def empurrao(pack):
    """Meia largura do empurrão: EMPURRAO_DO_TRONCO da meia largura do tronco desenhado parado
    (a faixa do meio da silhueta do IDLE, mediana dos quadros). None sem silhueta."""
    corpo = pack['animations'].get('IDLE', {}).get('silhueta', {}).get('corpo')
    meios = [c[1] for c in (corpo or {}).values() if len(c) == 3]
    if not meios:
        return None
    return round(EMPURRAO_DO_TRONCO * float(np.median([(m[1] - m[0]) / 2 for m in meios])))


def aplicar(pack, novo, golpes):
    pack['fighter']['body'].update(novo)
    for binding, (reach, altura, base) in golpes.items():
        m = pack['moves'][binding]
        # Limite de balanço (docs/balanco/ajustes.md): a medida do desenho não passa dele.
        m['reach'] = round(min(reach, m.get('alcanceMaximo', reach)))
        m['hitHeight'] = round(altura)
        if base is None or round(base) >= m['hitHeight'] - FOLGA:
            m.pop('hitFloor', None)
        else:
            m['hitFloor'] = max(1, round(base))


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='*')
    p.add_argument('--write', action='store_true')
    a = p.parse_args()
    caminhos = sorted((ROOT / 'characters').glob('*/character.json'))
    for path in caminhos:
        pack = json.loads(path.read_text(encoding='utf-8'))
        if pack['id'] in FORA or (a.personagens and pack['id'] not in a.personagens):
            continue
        novo, golpes = medir(pack)
        b = pack['fighter']['body']
        print(f"{pack['id']}  corpo: " + ', '.join(f'{k} {b[k]}→{v}' for k, v in novo.items()))
        for binding, (reach, altura, base) in golpes.items():
            m = pack['moves'][binding]
            antes = f"{m['reach']}/{m.get('hitHeight', '-')}/{m.get('hitFloor', '-')}"
            depois = f"{round(reach)}/{round(altura)}/{round(base) if base is not None else '-'}"
            print(f'    {binding:4} alcance/altura/base {antes:>14} → {depois}')
        if a.write:
            aplicar(pack, novo, golpes)
            silhuetas(pack)
            meia = empurrao(pack)
            if meia:
                pack['fighter']['body']['pushHalfWidth'] = meia
            path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    # Ultras: o rival preso onde a ponta das pancadas encosta.
    ultras_path = HERE / 'ultras.json'
    ultras = json.loads(ultras_path.read_text(encoding='utf-8'))
    for cid, spec in ultras['personagens'].items():
        if cid in ULTRA_A_MAO or spec.get('rival') or (a.personagens and cid not in a.personagens):
            continue
        pack = json.loads((ROOT / f'characters/{cid}/character.json').read_text(encoding='utf-8'))
        d = alcance_do_ultra(pack, spec)
        if d:
            print(f"{cid}  Ultra: rival preso a {spec.get('alcance', '-')}→{d} px (onde as pancadas encostam)")
            spec['alcance'] = d
    if a.write:
        ultras_path.write_text(json.dumps(ultras, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
        print('agora: python3 tools/sprites/ultras.py --write e python3 tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
