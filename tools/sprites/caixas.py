#!/usr/bin/env python3
"""Grava no character.json as caixas ajustadas no app (DEBUG > AJUSTAR CAIXAS > SALVAR NO DRIVE).

    python3 tools/sprites/caixas.py --aplicar ajustes-caixas-20261014-1200.json
    python3 tools/sprites/build_characters.py --write

Cada ajuste diz o personagem, o campo (caminho no character.json, por exemplo
fighter.body.halfWidth, moves.L.reach, specialMoves.S2.hitHeight, moves.2H.hitFloor,
fighter.energy.spawnX),
o valor em que foi feito ("antes") e o novo. Se o pacote mudou desde então (o valor atual não
é o "antes"), o ajuste é recusado em vez de passar por cima. Sem imagens: só números.
"""
import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def default(path, node=None):
    """Valor que o build usa quando o campo não está no JSON (hitHeight e hitFloor dos golpes)."""
    parts = path.split('.')
    if parts[0] not in ('moves', 'specialMoves'):
        return None
    height = 42 if parts[1].startswith('2') else 78
    if parts[-1] == 'hitHeight':
        return height
    if parts[-1] == 'hitFloor':
        # Sem base própria a caixa começa na altura menos a folga (CombatRules.VERTICAL_SLACK).
        return (node or {}).get('hitHeight', height) - 19.5
    return None


def apply(data, root=ROOT):
    """Aplica os ajustes; devolve [(personagem, campo, antes, valor)]. Para no primeiro conflito."""
    if data.get('tipo') != 'ajuste-caixas':
        raise SystemExit('não é um arquivo de ajuste de caixas do app')
    packs, done = {}, []
    for item in data['ajustes']:
        cid, path = item['personagem'], item['campo']
        before, value = item['antes'], int(item['valor'])
        if cid not in packs:
            packs[cid] = json.loads((root / f'characters/{cid}/character.json').read_text(encoding='utf-8'))
        node = packs[cid]
        *parents, leaf = path.split('.')
        for part in parents:
            if part not in node:
                raise SystemExit(f'{cid}: campo {path} não existe no character.json')
            node = node[part]
        current = node.get(leaf, default(path, node))
        if current is None:
            raise SystemExit(f'{cid}: campo {path} não existe no character.json')
        if abs(current - before) > .5:  # o app arredonda a base (245.5 vira 246)
            raise SystemExit(f'{cid}: {path} é {current} no pacote, o ajuste foi feito sobre {before}; '
                             'ajuste de novo no app novo')
        if not 1 <= value <= 600:
            raise SystemExit(f'{cid}: {path} = {value} fora de 1-600')
        node[leaf] = value
        done.append((cid, path, before, value))
    for cid, pack in packs.items():
        (root / f'characters/{cid}/character.json').write_text(
            json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    return done


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--aplicar', metavar='ARQUIVO', required=True)
    args = ap.parse_args()
    data = json.loads(Path(args.aplicar).read_text(encoding='utf-8'))
    for cid, path, before, value in apply(data):
        print(f'{cid} {path}: {before} -> {value}')
    print('agora: python3 tools/sprites/build_characters.py --write  (no app o ajuste deixa de valer sozinho)')


if __name__ == '__main__':
    sys.exit(main())
