#!/usr/bin/env python3
"""Pé de apoio de cada quadro, medido no desenho (as folhas do jogo): o atrito entre o pé e o chão.

Em cada quadro de uma animação, acha os pés (o que a silhueta tem de mais baixo, perto da linha
do chão), segue cada um de um quadro para o seguinte e decide qual está apoiado. O pé apoiado
fica cravado no mundo: o quanto ele anda para trás no desenho, de um quadro para o outro, é o
quanto o corpo anda para a frente no jogo (o "avanço" do quadro).

Regras (valem para todo quadro, sem número por personagem):
    pé          trecho de colunas com desenho na faixa de FAIXA × altura em pé acima do chão. Cada
                pé é seguido inteiro e em duas metades (calcanhar e ponta, ou dois pés juntos num
                trecho só), pelo desenho dele, do quadro para o seguinte
    de apoio    em cada quadro, a parte de pé que deixa a velocidade do corpo mudar menos de um
                quadro para o outro, na animação inteira (o corpo tem peso e não dá tranco; o pé
                que dá o passo corre e para). Subir, descer e casar mal pesam contra a parte
    sem apoio   nem o pé mais baixo chega perto da linha do chão (NO_AR × altura: corrida, pulo):
                o quadro fica sem avanço medido e o corpo segue como vinha (o motor)

Saída, por animação (character.json, "apoio"):
    avanco      px do mundo que o corpo anda, para a frente (negativo: para trás), do quadro i
                para o seguinte (o último, para o primeiro, nas que dão a volta); null sem apoio
    semApoio    quadros sem pé no chão
    duvida      quadros em que o pé de apoio casou mal com o quadro seguinte (medida fraca)

O motor usa o avanço das animações de andar que dão a volta com todos os quadros medidos
(CombatEngine.andarPeloPe): a passada corre no tempo, no ritmo que dá a velocidade de andar do
lutador, e o corpo anda o que o pé de apoio empurra. Só o andar da p02 está gravado (teste de
13/11); a corrida não é medida (o pé passa de BUSCA entre dois quadros) e as animações no ar saem
com medida falsa, porque a folha encosta o pé na linha do chão (quem sabe que é ar é o motor).

    python3 tools/sprites/apoio.py p02                    # mostra a medida de cada animação
    python3 tools/sprites/apoio.py p02 WALK_FORWARD -v    # quadro a quadro
    python3 tools/sprites/apoio.py p02 --write WALK_FORWARD WALK_BACK
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
OPACO = 160        # alfa mínimo de um pixel que conta (0-255), como no caixas_sprites.py
FAIXA = 0.20       # da altura em pé: a faixa acima do chão em que se procuram os pés
BUSCA = (22, 6)    # px da folha: até onde se procura o mesmo pé no quadro seguinte (x, y)
RUIM = 0.55        # casamento abaixo disto (0-1): não é o mesmo pé
DUVIDA = 0.80      # apoio escolhido com casamento abaixo disto: o quadro fica marcado em dúvida
NO_AR = 0.04       # da altura em pé: o pé mais baixo acima disto, ninguém está no chão
PESO_SUBIDA = 0.5  # px por px: quanto subir ou descer pesa contra a parte na escolha do apoio
PESO_NOTA = 6.0    # px: quanto um casamento ruim pesa contra a parte na escolha do apoio
_folhas = {}


def ler(atlas):
    """(RGBA da folha, layout, escala px da folha -> px do mundo)."""
    if atlas not in _folhas:
        clip = json.loads((HERE / 'clips' / f'{atlas}.json').read_text(encoding='utf-8'))
        rep = json.loads((HERE / 'reports' / f'{atlas}.report.json').read_text(encoding='utf-8'))
        im = np.array(Image.open(ROOT / clip['source']).convert('RGBA'))
        _folhas[atlas] = (im, rep['layout'], rep['worldScale'] / rep.get('pixelScale', 1))
    return _folhas[atlas]


def celula(atlas, i):
    im, lay, _ = ler(atlas)
    fw, fh, cols = lay['frameWidth'], lay['frameHeight'], lay['columns']
    r, c = divmod(i, cols)
    return im[r * fh:(r + 1) * fh, c * fw:(c + 1) * fw]


def pes(atlas, i, faixa):
    """Os pés do quadro: [{x0, x1, base, sola}] em px da célula. base = a linha mais baixa do
    pé; sola = quantas colunas chegam a ela (2 px de folga)."""
    _, lay, _ = ler(atlas)
    chao = lay['rootY']
    m = celula(atlas, i)[..., 3] >= OPACO
    y0 = max(0, chao - faixa)
    banda = m[y0:chao + 3]
    tem = banda.any(0)
    baixo = np.where(tem, banda.shape[0] - 1 - np.argmax(banda[::-1], axis=0), -1) + y0
    achados, x, largura = [], 0, m.shape[1]
    while x < largura:
        if not tem[x]:
            x += 1
            continue
        x0 = x
        while x < largura and tem[x]:
            x += 1
        base = int(baixo[x0:x].max())
        achados.append({'x0': x0, 'x1': x - 1, 'base': base,
                        'sola': int((baixo[x0:x] >= base - 2).sum())})
    return [p for p in achados if p['x1'] - p['x0'] >= 4]


def casar(atlas, i, j, pe, faixa):
    """Onde o pé do quadro i está no quadro j: (dx, dy, nota 0-1), pelo desenho do pé (a faixa
    de baixo dele), com fração de pixel em x."""
    _, lay, _ = ler(atlas)
    a, b = celula(atlas, i), celula(atlas, j)
    alto = min(faixa, 18)
    y0, y1 = max(0, pe['base'] - alto), pe['base'] + 2
    x0, x1 = max(0, pe['x0'] - 1), min(a.shape[1], pe['x1'] + 2)
    molde = a[y0:y1, x0:x1].astype(np.float32)
    molde = molde[..., :3].mean(-1) * (molde[..., 3] / 255) + (255 - molde[..., 3])  # fundo claro
    bx, by = BUSCA
    sx0, sy0 = max(0, x0 - bx), max(0, y0 - by)
    sx1, sy1 = min(b.shape[1], x1 + bx), min(b.shape[0], y1 + by)
    area = b[sy0:sy1, sx0:sx1].astype(np.float32)
    area = area[..., :3].mean(-1) * (area[..., 3] / 255) + (255 - area[..., 3])
    if area.shape[0] < molde.shape[0] or area.shape[1] < molde.shape[1] or molde.std() < 1:
        return 0.0, 0.0, 0.0
    r = cv2.matchTemplate(area, molde, cv2.TM_CCOEFF_NORMED)
    _, nota, _, (px, py) = cv2.minMaxLoc(r)
    fino = 0.0
    if 0 < px < r.shape[1] - 1:   # parábola pelos três pontos em volta do pico
        e, c, d = r[py, px - 1], r[py, px], r[py, px + 1]
        if e + d - 2 * c < -1e-6:
            fino = 0.5 * (e - d) / (e + d - 2 * c)
    return px + fino + sx0 - x0, py + sy0 - y0, float(nota)


def partes(pe):
    """O pé inteiro e as duas metades dele (calcanhar e ponta, ou dois pés juntos num trecho
    só): cada parte é seguida à parte, e a que está apoiada é a que anda como o corpo pede."""
    lista = [dict(pe, parte='inteiro')]
    meio = (pe['x0'] + pe['x1']) // 2
    if pe['x1'] - pe['x0'] >= 12:
        lista.append(dict(pe, x1=meio, parte='trás'))
        lista.append(dict(pe, x0=meio + 1, parte='frente'))
    return lista


def caminho(candidatos, volta):
    """Em cada quadro, a parte de pé que apoia: o caminho em que a velocidade do corpo menos
    muda de um quadro para o outro (o corpo tem peso e não dá tranco; o pé que dá o passo, sim).
    candidatos[k] = [(dx, custo)], vazio nos quadros sem pé no chão. Devolve o índice escolhido
    por quadro (None nos vazios)."""
    com = [k for k, c in enumerate(candidatos) if c]
    if not com:
        return [None] * len(candidatos)

    def melhor(fixo):
        # fixo: o candidato do primeiro quadro (a volta fecha nele), ou None (animação que não dá a volta)
        custo = [[c[1] for c in candidatos[k]] for k in com]
        de = [[0] * len(candidatos[k]) for k in com]
        if fixo is not None:
            custo[0] = [v if i == fixo else 1e9 for i, v in enumerate(custo[0])]
        for n in range(1, len(com)):
            antes, agora = candidatos[com[n - 1]], candidatos[com[n]]
            for i, (dx, _) in enumerate(agora):
                passo = [custo[n - 1][j] + abs(dx - antes[j][0]) for j in range(len(antes))]
                j = int(np.argmin(passo))
                custo[n][i] += passo[j]
                de[n][i] = j
        fim = list(custo[-1])
        if fixo is not None:   # fecha a volta: do último para o primeiro
            fim = [v + abs(candidatos[com[-1]][i][0] - candidatos[com[0]][fixo][0]) for i, v in enumerate(fim)]
        i = int(np.argmin(fim))
        total, escolha = fim[i], [0] * len(com)
        for n in range(len(com) - 1, -1, -1):
            escolha[n] = i
            i = de[n][i]
        return total, escolha

    if volta:
        total, escolha = min((melhor(f) for f in range(len(candidatos[com[0]]))), key=lambda t: t[0])
    else:
        total, escolha = melhor(None)
    saida = [None] * len(candidatos)
    for n, k in enumerate(com):
        saida[k] = escolha[n]
    return saida


def medir(pack, nome, verbose=False, detalhes=None):
    """A medida do pé de apoio de uma animação: {'avanco', 'semApoio', 'duvida'}. detalhes: uma
    lista que recebe a parte de pé escolhida em cada quadro ({x0, x1, base} na célula, ou None)."""
    anim = pack['animations'][nome]
    atlas = anim['atlas']
    _, lay, escala = ler(atlas)
    quadros = anim['frames']
    volta = bool(anim.get('loop'))
    altura = pack['fighter']['body']['standHeight'] / escala   # px da folha
    faixa = round(FAIXA * altura)
    chao = lay['rootY']
    n = len(quadros)
    pares = n if volta else n - 1
    achados = [pes(atlas, q, faixa) for q in quadros]
    candidatos, detalhe = [], []
    for k in range(pares):
        i, j = quadros[k], quadros[(k + 1) % n]
        linha = []
        # No ar: nem o pé mais baixo chega perto da linha do chão.
        if achados[k] and max(p['base'] for p in achados[k]) >= chao - NO_AR * altura and i != j:
            for p in achados[k]:
                for parte in partes(p):
                    dx, dy, nota = casar(atlas, i, j, parte, faixa)
                    if nota >= RUIM:
                        # Pé que sobe ou desce pesa contra (está saindo do chão), sem proibir:
                        # o pé de apoio também sobe na ponta no fim do passo.
                        linha.append((dx, PESO_NOTA * (1 - nota) + PESO_SUBIDA * abs(dy), parte, nota))
        elif i == j:   # o mesmo desenho segurado: o corpo não anda
            linha.append((0.0, 0.0, {'x0': 0, 'x1': 0, 'base': chao, 'parte': 'parado'}, 1.0))
        candidatos.append([(c[0], c[1]) for c in linha])
        detalhe.append(linha)
    escolha = caminho(candidatos, volta)
    avanco, sem, duvida = [None] * pares, [], []
    for k in range(pares):
        if escolha[k] is None:
            sem.append(k)
            if detalhes is not None:
                detalhes.append(None)
            continue
        dx, _, parte, nota = detalhe[k][escolha[k]]
        if detalhes is not None:
            detalhes.append(parte)
        avanco[k] = round(float(-dx * escala), 2)
        if nota < DUVIDA:
            duvida.append(k)
    if verbose:
        for k in range(pares):
            marca = 'sem apoio' if escolha[k] is None else f'corpo {avanco[k]:+6.2f}'
            desc = ' '.join(f"{'*' if escolha[k] == c else ' '}[{p['x0'] - lay['rootX']:+4d}..{p['x1'] - lay['rootX']:+4d} "
                            f"{p['parte'][:3]} dx {dx:+5.1f} {nota:.2f}]"
                            for c, (dx, _, p, nota) in enumerate(detalhe[k]))
            print(f'  {k:2d} {marca:13} {desc}')
    return {'avanco': avanco, 'semApoio': sem, 'duvida': duvida}


def resumo(pack, nome, m):
    anim = pack['animations'][nome]
    medidos = [v for v in m['avanco'] if v is not None]
    total = sum(medidos)
    n = len(m['avanco'])
    return (f"{nome:18} {len(anim['frames']):3d} quadros  apoio em {len(medidos):3d}/{n:<3d} "
            f"sem apoio {len(m['semApoio']):2d}  dúvida {len(m['duvida']):2d}  corpo anda {total:+7.1f} px"
            + (' por volta' if anim.get('loop') else ''))


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('personagem')
    ap.add_argument('animacoes', nargs='*')
    ap.add_argument('--write', action='store_true', help='grava "apoio" nas animações pedidas')
    ap.add_argument('-v', action='store_true', help='quadro a quadro')
    args = ap.parse_args()
    caminho = ROOT / f'characters/{args.personagem}/character.json'
    pack = json.loads(caminho.read_text(encoding='utf-8'))
    nomes = args.animacoes or list(pack['animations'])
    for nome in nomes:
        try:
            m = medir(pack, nome, args.v)
        except FileNotFoundError as e:
            print(f'{nome:18} sem folha ({e.filename})')
            continue
        print(resumo(pack, nome, m))
        if args.write:
            if args.animacoes == []:
                raise SystemExit('--write pede o nome das animações')
            pack['animations'][nome]['apoio'] = m
    if args.write:
        caminho.write_text(json.dumps(pack, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
