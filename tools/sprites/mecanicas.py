#!/usr/bin/env python3
"""Aplica as mecânicas de cada personagem (tools/sprites/mecanicas.json) no character.json:

  * vida, dano e projétil (velocidade e alcance) de cada um;
  * frame data dos golpes: o tempo base vezes o ritmo do personagem (golpe a golpe se
    houver "ritmoGolpe"). A vantagem no hit e na defesa é a mesma para todos (hitstun e
    blockstun acompanham); janelas de cancelamento e caixas são remapeadas fase a fase
    (startup, ativo, recuperação), então continuam no mesmo trecho do golpe;
  * cadência da animação dos golpes: o motor estica a animação sobre o golpe inteiro, então
    só a proporção entre os quadros importa. Cada quadro ganha o tempo que tinha no vídeo
    (receita do clipe, saltos acima de 2x a mediana limitados) e o quadro do impacto é
    encaixado no começo da janela ativa.

  * comandos de S1 a S4 (bloco "comandos": nome e comando em setas, "[←] →" para segurar,
    noAr para só no ar), que a pausa mostra em COMANDOS (docs/personagens/comandos.md).

Com "manterTempo" (p01, arte desenhada quadro a quadro) só vida, dano e projétil mudam.
Alcance e caixas de acerto não mudam aqui (vêm do DEBUG > AJUSTAR CAIXAS). O Super mantém
o tempo e a animação (só o dano muda). Depois rode build_characters.py --write.

    python3 tools/sprites/mecanicas.py            # só mostra
    python3 tools/sprites/mecanicas.py --write    # grava
    python3 tools/sprites/mecanicas.py p09 p10    # só alguns
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import auditoria as A  # noqa: E402

DATA = Path(__file__).with_name('mecanicas.json')
PANCADAS = Path(__file__).with_name('pancadas.json')
STRIKE_WINDOW = 3   # quadros em que cada pancada pode acertar (AttackDefinition.STRIKE_WINDOW)
GAP_CAP = 2.0   # um quadro fica no máximo 2x o tempo mediano do clipe


def remap(t, old, new):
    """Frame t do golpe antigo no golpe novo, fase a fase."""
    (s, a, r), (s2, a2, r2) = old, new
    if t < s:
        return t * s2 / s
    if t < s + a:
        return s2 + (t - s) * a2 / a
    return s2 + a2 + ((t - s - a) * r2 / r if r else 0)


def retime(m, new, advantage=None):
    old = (m['startupFrames'], m['activeFrames'], m['recoveryFrames'])
    s, a, r = new
    m['startupFrames'], m['activeFrames'], m['recoveryFrames'] = s, a, r
    total, last_active = s + a + r, s + a - 1
    if advantage:
        m['hitstunFrames'] = max(1, a - 1 + r + advantage[0])
        m['blockstunFrames'] = max(1, a - 1 + r + advantage[1])
    for key, w in m['cancelWindows'].items():
        if w:
            first = 0 if key == 'whiff' else s
            lo = min(max(first, round(remap(w[0], old, new))), total - 1)
            m['cancelWindows'][key] = [lo, min(max(lo, round(remap(w[1], old, new))), total - 1)]
    for key, first, last in (('hitboxes', s, last_active), ('hurtboxes', 0, total - 1)):
        for box in m.get(key, []):
            lo = min(max(first, round(remap(box['frames'][0], old, new))), last)
            box['frames'] = [lo, min(max(lo, round(remap(box['frames'][1], old, new))), last)]


def video_cadence(anim, cfg, total_ms):
    """durationsMs na proporção do vídeo; None se o clipe não tem receita de vídeo."""
    v = A.video_seconds(cfg, anim)
    if v is None or len(v) != len(anim['frames']) or v.sum() <= 0:
        return None
    v = np.minimum(v, GAP_CAP * np.median(v))
    return [max(1, round(x)) for x in v / v.sum() * total_ms]


def fit_impact(anim, m):
    """Quadro do impacto começando junto com o primeiro frame ativo (o golpe encosta quando
    acerta): redistribui o tempo antes e depois dele, mesmo total."""
    i, d = anim.get('impactFrame'), anim['durationsMs']
    if i is None or not 0 < i < len(d):
        return
    total = m['startupFrames'] + m['activeFrames'] + m['recoveryFrames']
    start = m['startupFrames'] / total
    whole, before = sum(d), sum(d[:i])
    if start - 0.75 / total <= before / whole <= start < (before + d[i]) / whole:
        return
    # começa meio frame antes do acerto e dura pelo menos um frame de jogo
    lead, hold = start - 0.5 / total, max(d[i] / whole, 1 / total)
    after = sum(d[i + 1:])
    head = [x * lead * whole / before for x in d[:i]]
    tail = [x * (1 - lead - hold) * whole / after for x in d[i + 1:]] if after else []
    anim['durationsMs'] = [max(1, round(x)) for x in head + [hold * whole] + tail]


def window(name, m):
    """Janela nomeada em frames do golpe: startup = antes do golpe sair, ate_ativo = do início
    ao último frame ativo."""
    s, a = m['startupFrames'], m['activeFrames']
    if isinstance(name, list):
        return name
    return {'startup': [1, s - 1], 'ate_ativo': [0, s + a - 1]}[name]


def unique(pack, spec):
    """Mecânica própria do personagem (bloco "unica"): grava "mecanica" nos golpes e no lutador."""
    fighter = pack['fighter']
    targets = dict(pack['moves'])
    targets.update(pack.get('specialMoves', {}))
    targets['S'] = fighter['energy']['attack']
    for m in targets.values():
        m.pop('mecanica', None)
    fighter.pop('mecanica', None)
    u = spec.get('unica', {})
    for key, wanted in u.get('golpes', {}).items():
        m, mech = targets[key], {}
        for kind, value in wanted.items():
            if kind == 'teleporte':
                mech[kind] = m['startupFrames'] - 2 if value == 'fimStartup' else value
            elif isinstance(value, dict) and 'janela' in value:
                mech[kind] = {k: v for k, v in value.items() if k != 'janela'}
                mech[kind]['frames'] = window(value['janela'], m)
            else:
                mech[kind] = value
        m['mecanica'] = mech
    for key, route in u.get('rotas', {}).items():
        m = targets[key]
        total = m['startupFrames'] + m['activeFrames'] + m['recoveryFrames']
        m['cancelInto'] = list(dict.fromkeys(m['cancelInto'] + route['cancelaEm']))
        m['cancelWindows']['hit'] = m['cancelWindows']['block'] = [m['startupFrames'], total - 1]
        if route.get('noVazio'):
            m['cancelWindows']['whiff'] = [m['startupFrames'], total - 1]
        if 'maxUsos' in route:
            m['maxUsesPerCombo'] = route['maxUsos']
    for key, reach in u.get('alcance', {}).items():
        targets[key]['reach'] = reach
    for key, animation in u.get('animacao', {}).items():
        targets[key]['animation'] = animation
    if u.get('lutador'):
        fighter['mecanica'] = dict(u['lutador'])


ARROWS = {'→': 1, '↘': 2, '↓': 3, '↙': 4, '←': 5, '↖': 6, '↑': 7, '↗': 8}


def parse_command(text):
    """"↓↘→" → ([3, 2, 1], False); "[←] →" → ([5, 1], True): segura a primeira direção e aperta
    a segunda. Direções relativas ao personagem virado para a direita (1 = frente, 5 = trás)."""
    hold = text.lstrip().startswith('[')
    steps = [ARROWS[ch] for ch in text if ch in ARROWS]
    if any(ch not in ARROWS and ch not in '[] ' for ch in text) or not steps:
        raise SystemExit(f'comando "{text}": use só setas, espaços e [ ]')
    if hold and len(steps) != 2:
        raise SystemExit(f'comando "{text}": segurar é [direção] direção')
    return steps, hold


def commands(pack, spec):
    """Comandos próprios (bloco "comandos"): S1 é a energia, S2 a S4 os especiais. Qualquer
    botão (L, M ou H) solta o especial; o botão dá a força da energia."""
    fighter, specials = pack['fighter'], pack.get('specialMoves', {})
    for key, c in spec.get('comandos', {}).items():
        target = fighter['energy'] if key == 'S1' else specials[key]
        # Sempre na mesma ordem no fim do golpe: refazer dá o mesmo arquivo.
        for field in ('buttons', 'nome', 'segurar', 'noAr'):
            target.pop(field, None)
        target['command'], hold = parse_command(c['comando'])
        if key != 'S1':
            target['buttons'] = ['L', 'M', 'H']
        target['nome'] = c['nome']
        if hold:
            target['segurar'] = True
        if c.get('noAr'):
            if key == 'S1':
                raise SystemExit('S1 sai no chão e no ar; noAr é para S2 a S4')
            target['noAr'] = True


def strike_frames(anim, total, startup, pancadas):
    """Quadro do golpe em que cada pancada acerta: o tempo da animação até o quadro dela
    (a animação estica sobre o golpe). A primeira é o fim da preparação."""
    d = anim['durationsMs']
    whole, frames = sum(d), [startup]
    for i in pancadas[1:]:
        frames.append(max(round(sum(d[:i]) / whole * total), frames[-1] + 2))
    return frames


BASE_KEYS = ('startupFrames', 'activeFrames', 'recoveryFrames', 'hitstunFrames', 'blockstunFrames')


def restore(m):
    """Desfaz as pancadas gravadas antes (refazer dá sempre o mesmo resultado)."""
    m.update(m.pop('golpesBase', {}))
    m.pop('golpes', None)


def strike(m, anim, pancadas):
    """Golpe de várias pancadas: janela ativa cobre todas, mesma duração total. Guarda o
    tempo de antes em "golpesBase"."""
    m.setdefault('golpesBase', {k: m[k] for k in BASE_KEYS})
    total = m['startupFrames'] + m['activeFrames'] + m['recoveryFrames']
    frames = strike_frames(anim, total, m['startupFrames'], pancadas)
    if frames[-1] > total - 2:
        raise SystemExit(f"pancadas {pancadas}: a última cai no quadro {frames[-1]} de {total}")
    m['activeFrames'] = min(frames[-1] - m['startupFrames'] + STRIKE_WINDOW, total - 1 - m['startupFrames'])
    m['recoveryFrames'] = total - m['startupFrames'] - m['activeFrames']
    m['golpes'] = frames
    # A última pancada vem depois do impacto: o atordoamento dela desconta esse atraso, e o
    # golpe fica com a mesma vantagem na defesa e no acerto que tinha com uma pancada só.
    late = frames[-1] - m['startupFrames']
    m['blockstunFrames'] = max(4, m['blockstunFrames'] - late)
    m['hitstunFrames'] = max(10, m['hitstunFrames'] - late)


def advance(m, speed):
    m.setdefault('mecanica', {})['avanco'] = {'velocidade': speed, 'frames': window('ate_ativo', m)}


def striking_super(pack, spec):
    """Super que bate corpo a corpo seguindo a animação (em vez de soltar a onda)."""
    a = pack['fighter']['super']['attack']
    anim = pack['animations'][pack['specialAnimations']['SUPER']]
    a['golpesBase'] = {k: a[k] for k in BASE_KEYS}
    total = a['startupFrames'] + a['activeFrames'] + a['recoveryFrames']
    d, first = anim['durationsMs'], spec['pancadas'][0]
    a['startupFrames'] = int(sum(d[:first]) / sum(d) * total)
    a['activeFrames'], a['recoveryFrames'] = 1, total - a['startupFrames'] - 1
    strike(a, anim, spec['pancadas'])
    anim['impactFrame'] = first
    heavy = pack['moves']['H']
    a['reach'] = spec.get('alcance', heavy['reach'] + 30)
    a['hitHeight'] = spec.get('altura', heavy['hitHeight'])
    a['launchType'], a['knockback'] = 'knockdown', spec.get('empurrao', 120)
    # Defendido, o Super corpo a corpo fica -10 (a onda era segura por ser de longe).
    a['blockstunFrames'] = max(4, total - a['golpes'][-1] - 10)
    if spec.get('avanco'):
        advance(a, spec['avanco'])


def scaled(base, f, air=False):
    """Startup e recuperação seguem o ritmo; no ar a janela ativa fica a da base (golpe
    aéreo precisa de tempo ativo para encostar na descida)."""
    s, a, r = (max(1, round(x * f)) for x in base)
    return [s, base[1] if air else a, r]


def apply(cid, spec, base):
    path = A.ROOT / f'characters/{cid}/character.json'
    pack = json.loads(path.read_text(encoding='utf-8'))
    _, clips = A.load_pack(cid)
    fighter, report = pack['fighter'], []
    for m in list(pack['moves'].values()) + list(pack.get('specialMoves', {}).values()):
        restore(m)
    sup = fighter['super']['attack']
    restore(sup)
    for k in ('reach', 'hitHeight', 'launchType', 'knockback', 'mecanica'):
        sup.pop(k, None)
    fighter['maxLife'] = spec['vida']
    keep = spec.get('manterTempo', False)   # arte própria calibrada golpe a golpe (p01)

    def pace(key):
        return spec.get('ritmoGolpe', {}).get(key, spec['ritmo'])

    def damage(key):
        return int(round(base['dano'][key] * spec['dano'] / 10) * 10)

    attacks = []
    for key, m in list(pack['moves'].items()) + list(pack.get('specialMoves', {}).items()):
        if key not in base['frames']:
            continue
        if not keep:
            retime(m, scaled(base['frames'][key], pace(key), key.startswith('j')), base['vantagem'][key])
        m['damage'] = damage(key)
        attacks.append((key, m.get('animation'), m))
    energy = fighter['energy']
    if not keep:
        retime(energy['attack'], scaled(base['frames']['S'], pace('S')))
    energy['damage'] = damage('S')
    energy['speed'] = spec['projetil']['velocidade']
    energy['range'] = spec['projetil']['alcance']
    attacks.append(('S', pack.get('specialAnimations', {}).get('S'), energy['attack']))
    fighter['super']['damage'] = damage('SUPER')
    unique(pack, spec)
    commands(pack, spec)
    hits = json.loads(PANCADAS.read_text(encoding='utf-8'))['personagens'].get(cid, {})
    specials = pack.get('specialMoves', {})
    for key, h in hits.items():
        if key != 'SUPER' and 'pancadas' in h:
            pack['animations'][specials[key]['animation']]['impactFrame'] = h['pancadas'][0]

    for key, anim_id, m in attacks:
        anim_id = m.get('animation', anim_id)   # a mecânica pode trocar a animação do golpe
        total = m['startupFrames'] + m['activeFrames'] + m['recoveryFrames']
        anim = pack['animations'].get(anim_id) if anim_id else None
        cadence = 'sem animação' if not keep else 'tempo mantido'
        if anim and not keep and not anim.get('loop') and 'durationsMs' in anim:
            d = video_cadence(anim, clips.get(anim['atlas'], {}), total * 1000 / 60)
            if d:
                anim['durationsMs'], cadence = d, 'vídeo'
            else:
                cadence = 'mantida'
            fit_impact(anim, m)
        h = hits.get(key, {})
        if 'pancadas' in h:
            strike(m, anim, h['pancadas'])
            cadence += f", {len(h['pancadas'])} pancadas {m['golpes']}"
            if 'avanco' in m.get('mecanica', {}):
                m['mecanica']['avanco']['frames'] = window('ate_ativo', m)   # a janela ativa cresceu
        if 'avanco' in h:
            advance(m, h['avanco'])
        dmg = energy['damage'] if key == 'S' else m['damage']
        report.append(f"  {key:4} {dmg:5}  {m['startupFrames']:2}/{m['activeFrames']:2}/{m['recoveryFrames']:2}"
                      f"  {total / 60:.2f}s  cadência {cadence}")
    if 'SUPER' in hits:
        striking_super(pack, hits['SUPER'])
        a = fighter['super']['attack']
        report.append(f"  SUPER corpo a corpo: {len(a['golpes'])} pancadas {a['golpes']}, "
                      f"{a['startupFrames']}/{a['activeFrames']}/{a['recoveryFrames']}, alcance {a['reach']}")
    return pack, path, report


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='*')
    p.add_argument('--write', action='store_true', help='grava os character.json')
    args = p.parse_args()
    data = json.loads(DATA.read_text(encoding='utf-8'))
    for cid in args.personagens or list(data['personagens']):
        spec = data['personagens'][cid]
        pack, path, report = apply(cid, spec, data['base'])
        print(f"{cid} ({spec['molde']}): vida {spec['vida']}, dano {spec['dano']}x, ritmo {spec['ritmo']}x, "
              f"Super {pack['fighter']['super']['damage']}")
        print('\n'.join(report))
        if args.write:
            path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
