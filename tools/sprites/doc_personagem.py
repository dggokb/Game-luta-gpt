#!/usr/bin/env python3
"""A parte "No jogo hoje" do documento de cada personagem, gerada dos dados do jogo.

Lê o pacote (characters/<id>/character.json), as receitas dos clipes (tools/sprites/clips), as
montagens (tools/sprites/montagens/<id>.json), as escalas das imagens iniciais
(art/keys/<pasta>/tamanho.json) e a conferência da emenda no idle (emenda_idle_excecoes.json), e
escreve no docs/personagens/<doc>.md, entre as marcas

    <!-- no-jogo:inicio -->
    <!-- no-jogo:fim -->

duas tabelas: os golpes (botão, nome, tempo, dano, o que fazem) e a arte (de onde veio cada
animação). Rode depois de mexer num golpe ou numa animação, para o documento não ficar velho:

    python3 tools/sprites/doc_personagem.py player_base --write
    python3 tools/sprites/doc_personagem.py --write          # todos
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
INICIO, FIM = '<!-- no-jogo:inicio -->', '<!-- no-jogo:fim -->'
DOC = {'player_base': 'p01'}
CHAVES = {'player_base': 'p01'}

BOTOES = [('L', 'L'), ('M', 'M'), ('H', 'H'), ('2L', '↓ + L'), ('2M', '↓ + M'), ('2H', '↓ + H'),
          ('6M', '→ + M'), ('6H', '→ + H'), ('DH', '→ → + H (correndo)'),
          ('jL', 'L no ar'), ('jM', 'M no ar'), ('jH', 'H no ar'), ('j2H', '↓ + H no ar')]
NOME_NORMAL = {'L': 'fraco', 'M': 'médio', 'H': 'forte', '2L': 'fraco agachado', '2M': 'médio agachado',
               '2H': 'forte agachado', '6M': 'golpe alto', 'jL': 'fraco no ar', 'jM': 'médio no ar',
               'jH': 'forte no ar', 'j2H': 'para baixo no ar'}
LANCA = {'knockdown': 'derruba', 'launch': 'lança para o alto', 'wallBounce': 'joga na parede',
         'groundBounce': 'crava no chão (quica)', 'slam': 'crava no chão', 'arc': 'joga em arco',
         'spin': 'derruba girando'}
POSTURAS = [('IDLE', 'parado'), ('IDLE_2', 'segundo idle'), ('WALK_FORWARD', 'andar para frente'),
            ('WALK_BACK', 'andar para trás'), ('DASH', 'corrida'), ('BACKDASH', 'backdash'),
            ('JUMP', 'pulo'), ('FALL', 'queda'), ('LAND', 'pouso'), ('CROUCH', 'agachar'),
            ('CROUCH_IDLE', 'agachado parado'), ('RISE', 'levantar'),
            ('DEFENSE_STAND', 'defesa em pé'), ('DEFENSE_STAND_2', 'defesa em pé 2'),
            ('DEFENSE_STAND_3', 'defesa em pé 3'), ('DEFENSE_CROUCH', 'defesa agachado'),
            ('DEFENSE_AIR', 'defesa no ar'), ('HIT_STAND', 'apanhar em pé'), ('HIT_HIGH', 'apanhar no rosto'),
            ('HIT_BODY', 'apanhar no corpo'), ('HIT_HEAVY', 'apanhar forte'), ('HIT_CROUCH', 'apanhar agachado'),
            ('HIT_AIR', 'apanhar no ar'), ('KNOCKDOWN', 'cair'), ('GROUNDED', 'caído'), ('GETUP', 'levantar do chão'),
            ('THROW_GRAB', 'agarrão'), ('INTRO', 'intro'), ('VICTORY', 'vitória'), ('DEFEAT', 'derrota'),
            ('TAUNT', 'provocação'), ('SHINKAI_ATIVA', 'Shinkai (ativação)')]


def ler(p):
    return json.loads(Path(p).read_text(encoding='utf-8'))


def escalas_h3(cid):
    t = ROOT / 'art/keys' / CHAVES.get(cid, cid) / 'tamanho.json'
    if not t.exists():
        return set()
    return {round(v, 4) for k, v in ler(t).get('chaves', {}).items() if k.endswith('_h3')}


def montagens(cid):
    p = ROOT / 'tools/sprites/montagens' / f'{cid}.json'
    out = {}
    if p.exists():
        for k, v in ler(p).items():
            if not k.startswith('_'):
                for a in v.get('animacoes', {}):
                    out[a] = (k, v)
    return out


def tempo(m):
    return f"{m.get('startupFrames', '?')} / {m.get('activeFrames', '?')} / {m.get('recoveryFrames', '?')}"


def efeito(m):
    partes = []
    mec = m.get('mecanica', {})
    if m.get('golpes') and len(m['golpes']) > 1:
        partes.append(f"{len(m['golpes'])} acertos")
    if m.get('low'):
        partes.append('baixo (defende agachado)')
    if m.get('overhead'):
        partes.append('por cima (defende em pé)')
    if m.get('launchType') in LANCA:
        partes.append(LANCA[m['launchType']])
    if 'arco' in mec:
        a = mec['arco']
        partes.append(f"arco de {a['altura']} × {a['distancia']} px" + (', quica na parede perto da borda' if a.get('parede') else ''))
    if 'voo' in mec:
        partes.append(f"voa {mec['voo']['altura']} px" + (' e paira' if mec['voo'].get('paira') else ''))
    if 'avanco' in mec:
        partes.append('anda no golpe')
    canc = [c for c in m.get('cancelInto', []) if c != 'JUMP']
    if 'JUMP' in m.get('cancelInto', []):
        partes.append('cancela em pulo')
    if canc:
        partes.append('emenda em ' + ', '.join(canc))
    return '; '.join(partes) or '—'


def arte(cid, nome, pack, clips_h3, monta, fora):
    a = pack['animations'].get(nome)
    if not a:
        return None
    cj = ROOT / 'tools/sprites/clips' / f"{a['atlas']}.json"
    c = ler(cj) if cj.exists() else {}
    v = c.get('video', {})
    extras = []
    if 'smear' in c:
        extras.append('smear')
    if str(c.get('source', '')).endswith('_sr_hd.png'):
        extras.append('nitidez HD')
    if (ROOT / 'android/app/src/main/res/drawable-nodpi' / f"{a['atlas']}_n.webp").exists():
        extras.append('normal map')
    if nome in monta:
        k, rec = monta[nome]
        trechos = ', '.join(sorted({t[0] for t in rec.get('trechos', [])}))
        origem = f'vídeo H3 `{trechos}` (montagem `{k}`)'
        nova = True
    elif v.get('escala') is not None and round(v['escala'], 4) in clips_h3:
        origem = f"vídeo H3 `{v.get('arquivo', '?')}`"
        nova = True
    elif v:
        origem = f"vídeo antigo `{v.get('arquivo', '?')}`"
        nova = False
    else:
        origem = 'arte antiga'
        nova = False
    marca = '✅' if nova else '⬜'
    if nome in fora:
        marca = '⚠️'
        extras.append('não emenda no idle')
    return marca, origem, ', '.join(extras)


def gerar(cid):
    pack = ler(ROOT / 'characters' / cid / 'character.json')
    mec = ler(ROOT / 'tools/sprites/mecanicas.json')['personagens'].get(cid, {})
    cmd = mec.get('comandos', {})
    h3 = escalas_h3(cid)
    monta = montagens(cid)
    exc = ler(ROOT / 'tools/sprites/emenda_idle_excecoes.json')
    fora_ids = set(exc.get(cid, [])) | set(exc.get('pose', {}).get(cid, []))
    fora = set()
    linhas = [INICIO, '', '_Gerado por `tools/sprites/doc_personagem.py` a partir dos dados do jogo; '
              'não edite à mão (rode a ferramenta de novo)._', '',
              '### Golpes', '', '| Botão | Golpe | Tempo (prep. / ativo / recup.) | Dano | O que faz |',
              '|---|---|---|---|---|']
    for b, rotulo in BOTOES:
        m = pack['moves'].get(b)
        if not m:
            continue
        if b in fora_ids and m.get('animation'):
            fora.add(m['animation'])
        nome = m.get('nome') or NOME_NORMAL.get(b, b)
        linhas.append(f"| {rotulo} | {nome} | {tempo(m)} | {m.get('damage', '?')} | {efeito(m)} |")
    energia = pack['fighter'].get('energy')
    if energia:
        en = energia.get('attack', {})
        s1 = cmd.get('S1', {})
        tipo = 'corpo a corpo' if not energia.get('range') else f"projétil ({energia.get('range')} px)"
        linhas.append(f"| {s1.get('comando', 'S1')} + golpe | S1 {s1.get('nome') or energia.get('nome', '')} | {tempo(en)} "
                      f"| {energia.get('damage', en.get('damage', '?'))} | {tipo} |")
    for sid, m in sorted(pack.get('specialMoves', {}).items()):
        if sid in fora_ids and m.get('animation'):
            fora.add(m['animation'])
        c = cmd.get(sid, {})
        linhas.append(f"| {c.get('comando', sid)} + golpe{' (no ar)' if c.get('noAr') else ''} | {sid} {m.get('nome') or c.get('nome', '')} "
                      f"| {tempo(m)} | {m.get('damage', '?')} | {efeito(m)} |")
    sup = pack['fighter'].get('super')
    if sup:
        linhas.append(f"| SUPER | Super | — | {sup.get('damage', '?')} | {sup.get('hits', 1)} acertos |")
    if pack.get('specialAnimations', {}).get('ULTRA'):
        linhas.append('| ↓ + SUPER | Ultra | — | — | — |')
    linhas += ['', '### Arte', '', '✅ arte nova (vídeo H3) · ⬜ arte antiga, falta refazer · ⚠️ fora da regra da emenda no idle · — não usado', '',
               '| Animação | Estado | De onde veio | Extras |', '|---|---|---|---|']
    vistos = set()
    golpes = [(m.get('animation'), f"golpe {b}") for b, m in pack['moves'].items() if m.get('animation')]
    golpes += [(m.get('animation'), f"especial {s}") for s, m in pack.get('specialMoves', {}).items() if m.get('animation')]
    golpes += [(a, f"{k}") for k, a in pack.get('specialAnimations', {}).items()]
    for nome, rotulo in POSTURAS + golpes:
        if not nome or nome in vistos or nome not in pack['animations']:
            continue
        vistos.add(nome)
        r = arte(cid, nome, pack, h3, monta, fora)
        if r and nome == 'HIT_STAND' and all(x in pack['animations'] for x in ('HIT_BODY', 'HIT_HIGH', 'HIT_HEAVY')):
            r = ('—', 'não usado: as três reações novas (rosto, corpo, forte) tomam o lugar', '')
        if r:
            linhas.append(f"| {rotulo} (`{nome}`) | {r[0]} | {r[1]} | {r[2]} |")
    linhas += ['', FIM]
    return '\n'.join(linhas)


def escrever(cid):
    doc = ROOT / 'docs/personagens' / f"{DOC.get(cid, cid)}.md"
    texto = doc.read_text(encoding='utf-8')
    bloco = gerar(cid)
    if INICIO in texto and FIM in texto:
        i, j = texto.index(INICIO), texto.index(FIM) + len(FIM)
        texto = texto[:i] + bloco + texto[j:]
    else:
        texto = texto.rstrip() + '\n\n## No jogo hoje\n\n' + bloco + '\n'
    doc.write_text(texto, encoding='utf-8')
    return doc


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='*')
    p.add_argument('--write', action='store_true')
    a = p.parse_args()
    ids = a.personagens or ['player_base'] + [f'p{n:02d}' for n in range(2, 17)]
    for cid in ids:
        if a.write:
            print(escrever(cid))
        else:
            print(gerar(cid))


if __name__ == '__main__':
    main()
