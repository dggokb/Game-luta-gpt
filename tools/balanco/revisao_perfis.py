#!/usr/bin/env python3
"""Revisão da identidade (docs/balanco/identidade.md): compara as rodadas da BateriaPerfis feitas
em cada estado do jogo (S0 = antes, S1..Sn = depois de cada passo).

Para cada passo e cada lutador: o quanto a ficha de luta mudou do estado anterior (diferença
média das notas, na escala congelada) e o ruído do próprio lutador (as duas rodadas de sementes
do mesmo estado). Mudou de verdade quem passa de 2x o ruído.

    python3 tools/balanco/revisao_perfis.py <pasta com S0, S1...> docs/balanco/perfis/escala-base.csv
"""
import csv
import math
import sys
from pathlib import Path


def ficha(p):
    rows = list(csv.reader(open(p, encoding='utf-8'), delimiter=';'))
    nomes = rows[0][1:]
    return nomes, {r[0]: [float(x) for x in r[1:]] for r in rows[1:]}


def escala(p):
    out = {}
    for linha in open(p, encoding='utf-8'):
        c = linha.rstrip('\n').split(';')
        if len(c) == 3:
            out[c[0]] = (float(c[1]), float(c[2]))
    return out


def dist(a, b, esc, i):
    s = n = 0
    for k, (m, dp) in esc.items():
        if k in a and k in b:
            s += ((a[k][i] - b[k][i]) / dp) ** 2
            n += 1
    return math.sqrt(s / n) if n else 0


def resumo(p):
    out, dentro = {}, False
    for linha in open(p, encoding='utf-8'):
        if linha.startswith('## '):
            dentro = linha.startswith('## RESUMO')
        if dentro and linha.startswith('| ') and ('distância média' in linha or 'núcleo' in linha or 'pares com' in linha
                                       or 'par mais' in linha or 'vitórias: desvio' in linha or 'normal no ar' in linha):
            c = [x.strip() for x in linha.strip().strip('|').split('|')]
            out[c[0]] = c[1]
    return out


def main():
    pasta, esc = Path(sys.argv[1]), escala(sys.argv[2])
    estados = sorted(d.name for d in pasta.iterdir() if d.is_dir() and d.name.startswith('S'))
    print('## Resumo por estado\n')
    rs = {s: resumo(pasta / s / 'saida.md') for s in estados}
    chaves = list(rs[estados[0]].keys())
    print('| medida | ' + ' | '.join(estados) + ' |')
    print('|---' * (len(estados) + 1) + '|')
    for k in chaves:
        print('| ' + k + ' | ' + ' | '.join(rs[s].get(k, '—') for s in estados) + ' |')
    print('\n## O que cada passo mudou no jeito de lutar\n')
    print('Mudança da ficha de luta de cada lutador em relação ao estado anterior, na escala congelada; entre '
          'parênteses, o ruído do próprio lutador. **Negrito** = passou de 2× o ruído.\n')
    nomes, _ = ficha(pasta / estados[0] / 'ficha_luta.csv')
    print('| passo | ' + ' | '.join(nomes) + ' | quantos mudaram |')
    print('|---' * (len(nomes) + 2) + '|')
    for a, b in zip(estados, estados[1:]):
        _, la = ficha(pasta / a / 'ficha_luta.csv')
        _, lb = ficha(pasta / b / 'ficha_luta.csv')
        _, lb2 = ficha(pasta / b / 'ficha_luta2.csv')
        cel, mudou = [], 0
        for i in range(len(nomes)):
            d, r = dist(la, lb, esc, i), dist(lb, lb2, esc, i)
            forte = d > 2 * r
            mudou += forte
            cel.append(('**%.2f**' if forte else '%.2f') % d + ' (%.2f)' % r)
        print(f'| {a} → {b} | ' + ' | '.join(cel) + f' | {mudou} de 16 |')
    print('\n## Vitórias por estado (%)\n')
    print('| estado | ' + ' | '.join(nomes) + ' |')
    print('|---' * (len(nomes) + 1) + '|')
    for s in estados:
        _, l = ficha(pasta / s / 'ficha_luta.csv')
        print(f'| {s} | ' + ' | '.join('%.0f' % v for v in l['vitórias (%)']) + ' |')


if __name__ == '__main__':
    main()
