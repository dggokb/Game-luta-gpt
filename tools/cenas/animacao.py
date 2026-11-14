#!/usr/bin/env python3
"""O que as cenas animadas à mão (ação "animar") têm em comum: as chaves de cada lutador, as
curvas, as pausas que seguram a pose, o corte em trechos nos QTE e o JSON legível.

x: distância do meio do eixo da animação; y: altura acima do chão; 60 quadros por segundo.
"""
import json


class Lutador:
    def __init__(self):
        self.chaves = []
        self.x = 0.0

    def k(self, q, pose, x=None, **kw):
        """Uma chave no quadro q; x sem valor repete o último."""
        if x is not None:
            self.x = x
        c = {'q': q, 'x': round(self.x, 1), 'pose': pose}
        for nome, v in kw.items():
            c[nome] = v
        if self.chaves and self.chaves[-1]['q'] > q:
            raise ValueError(f'chave fora de ordem no quadro {q}')
        self.chaves.append(c)



def curva(nome, t):
    """A mesma curva de chegada do jogo (AnimacaoManual.curva)."""
    t = max(0.0, min(1.0, t))
    if nome == 'sai':
        return 1 - (1 - t) ** 3
    if nome == 'entra':
        return t ** 3
    if nome == 'suave':
        return t * t * (3 - 2 * t)
    if nome == 'corte':
        return 1.0 if t >= 1 else 0.0
    return t


def pausar(jogador, rival, eventos, quadros, pausas):
    """Insere as pausas: depois do quadro p, n quadros com os dois segurando a pose de p."""
    def novo(q):
        return q + sum(n for p, n in pausas.items() if q > p)

    def um(chaves):
        out = []
        for c in chaves:
            c2 = dict(c)
            c2['q'] = novo(c['q'])
            out.append(c2)
        for p, n in sorted(pausas.items()):
            antes = [c for c in chaves if c['q'] <= p]
            depois = [c for c in chaves if c['q'] > p]
            a = antes[-1]
            if 'ate' in a:
                raise ValueError(f'pausa no quadro {p}: a chave {a["q"]} anima com "ate"')
            parada = {k: v for k, v in a.items() if k not in ('curva', 'rastro')}
            if a['q'] != p and depois:
                # No meio de um movimento: a pose de p (os números interpolados como no jogo) ganha
                # uma chave em p, e a pausa segura nela.
                b = depois[0]
                t = curva(b.get('curva', 'linear'), (p - a['q']) / (b['q'] - a['q']))
                for k, padrao in (('x', 0), ('y', 0), ('sx', 1), ('sy', 1), ('rot', 0), ('alfa', 1)):
                    va, vb = a.get(k, padrao), b.get(k, padrao)
                    if va != vb or k in a:
                        parada[k] = round(va + (vb - va) * t, 2)
                chave_p = dict(parada)
                chave_p['q'] = novo(p)
                chave_p['curva'] = b.get('curva', 'linear')
                if a.get('rastro'):
                    chave_p['rastro'] = a['rastro']
                out.append(chave_p)
            copia = dict(parada)
            copia['q'] = novo(p) + n
            out.append(copia)
        out.sort(key=lambda c: c['q'])
        return out

    evs = []
    for e in eventos:
        e2 = dict(e)
        e2['q'] = novo(e['q'])
        evs.append(e2)
    return um(jogador), um(rival), evs, novo(quadros) + 0


def _linha(o):
    return json.dumps(o, ensure_ascii=False)


def escrever(cena):
    """O arquivo legível: uma ação por linha; os quadros da página e as chaves da animação, um por linha."""
    linhas = ['{', f' "_comentario": {_linha(cena["_comentario"])},', f' "gatilho": {_linha(cena["gatilho"])},', ' "acoes": [']
    acoes = cena['acoes']
    for n, a in enumerate(acoes):
        fim = ',' if n + 1 < len(acoes) else ''
        listas = [k for k, v in a.items() if isinstance(v, list) and v and isinstance(v[0], dict)]
        if not listas:
            linhas.append(f'  {_linha(a)}{fim}')
            continue
        simples = {k: v for k, v in a.items() if k not in listas}
        cab = _linha(simples)[:-1]
        partes = [f'  {cab},']
        for j, k in enumerate(listas):
            itens = ',\n'.join(f'    {_linha(i)}' for i in a[k])
            partes.append(f'   "{k}": [\n{itens}]' + (',' if j + 1 < len(listas) else ''))
        partes.append(f'  }}{fim}')
        linhas.append('\n'.join(partes))
    linhas.append(' ]')
    linhas.append('}')
    return '\n'.join(linhas) + '\n'


def amostra(chaves, q):
    """O estado de um lutador no quadro q, como chave (os números interpolados como no jogo)."""
    a, b = chaves[0], None
    for i, c in enumerate(chaves):
        if c['q'] <= q:
            a, b = c, (chaves[i + 1] if i + 1 < len(chaves) else None)
    t = curva(b.get('curva', 'linear'), (q - a['q']) / (b['q'] - a['q'])) if b and b['q'] > a['q'] else 0.0
    out = {}
    for k, padrao in (('x', 0), ('y', 0), ('sx', 1), ('sy', 1), ('rot', 0), ('alfa', 1)):
        va = a.get(k, padrao)
        vb = b.get(k, padrao) if b else va
        v = round(va + (vb - va) * t, 2)
        if v != padrao or k == 'x':
            out[k] = v
    clip, quadro = a['pose'].split(':')
    quadro = int(quadro)
    desde = q - a['q']
    if 'ciclo' in a:
        quadro += int(desde * a['ciclo'] / 60)
        out['ciclo'] = a['ciclo']
    elif 'ate' in a and b and b['q'] > a['q']:
        quadro = round(quadro + (a['ate'] - quadro) * min(1.0, desde / (b['q'] - a['q'])))
        out['ate'] = a['ate']
    out['pose'] = f'{clip}:{quadro}'
    for k in ('rastro', 'vira'):
        if k in a:
            out[k] = a[k]
    return out


def cortar(chaves, ini, fim):
    """As chaves do trecho [ini, fim] da animação, contadas do começo dele."""
    out = []
    primeira = amostra(chaves, ini)
    primeira['q'] = 0
    out.append(primeira)
    for c in chaves:
        if ini < c['q'] < fim:
            c2 = dict(c)
            c2['q'] = c['q'] - ini
            out.append(c2)
    depois = [c for c in chaves if c['q'] >= fim]
    ultima = amostra(chaves, fim)
    ultima['q'] = fim - ini
    if depois:
        ultima['curva'] = depois[0].get('curva', 'linear')
    if out[-1]['q'] < ultima['q']:
        out.append(ultima)
    return out
