#!/usr/bin/env python3
"""Comportamento próprio de um personagem, medido nos vídeos dele: o vídeo é a fonte da
verdade do movimento, e o tempo pode ser acelerado. Nada vem do p01.

    python3 tools/sprites/comportamento.py p15 <pasta com os vídeos> --ver   # só mostra
    python3 tools/sprites/comportamento.py p15 <pasta com os vídeos>         # grava no pacote

Rode depois do personagem.py (que converte os clipes) e antes do build_characters.py --write.

O fator de aceleração fica em tools/sprites/comportamento/<id>.json ("acelerar", com
exceções por estado em "acelerarPorEstado"); o vídeo do Omni Flash enche 4 s, então andar,
correr e golpe saem em câmera lenta e são acelerados; o pulo já sai no tempo de luta.

O que é gravado em characters/<id>/character.json:

- fighter.movimento (o motor usa no lugar do CombatConfig): velocidade de andar para frente
  e para trás e de correr = distância que o corpo anda no vídeo ÷ tempo do vídeo × fator;
  backdash = distância e duração do salto do vídeo; pulo = altura e tempo de subida e de
  descida do vídeo (velocidade de saída e gravidade de cada metade);
- distancePerFrame do andar = quanto o corpo anda por quadro na arte (o pé não desliza);
- durationsMs de corrida, backdash, pulo, queda, aterrissagem, agachar e levantar = tempo do
  vídeo entre os quadros do clipe ÷ fator;
- golpes (L, M, H, 6M): startup até o impacto, ativo e recuperação contados no vídeo ÷ fator,
  e a animação com os mesmos tempos. A vantagem no acerto e na defesa continua a mesma
  (hitstun/blockstun acompanham a recuperação) e as janelas de cancelamento são remapeadas.
  Clipe que termina parado no impacto (o vídeo vai até a pose final) volta pelos mesmos
  quadros invertidos.

"comportamento" no pacote guarda os fatores e as medidas, e a auditoria passa a conferir o
ritmo contra o vídeo do personagem (não contra o p01).
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
from personagem import analyse  # noqa: E402
from video_para_sprite import key as keyer, read_frames, registration_x  # noqa: E402

VIDEO_FPS = 24
GAME_FPS = 60
CONFIG = HERE / 'comportamento'
DEFAULT = {
    '_doc': 'Fator de aceleração dos vídeos deste personagem (tools/sprites/comportamento.py). '
            'velocidade/tempo no jogo = vídeo × fator. acelerarPorEstado: exceções.',
    'acelerar': 3.0,
    'acelerarPorEstado': {'JUMP': 1.0, 'FALL': 1.0},
}
REACH_IMPACT = 0.95   # impacto: 1º quadro com 95% do alcance máximo do golpe
HOLD_VIDEO_FRAMES = 3  # golpe que para no impacto: quanto segura a pose esticada
# Golpe -> (clipe, volta pelos mesmos quadros?)
ATTACKS = {'L': ('light_jab', True), 'M': ('medium_kick', False), 'H': ('heavy_straight', True),
           '6M': ('overhead', False)}


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))


def write(path, data):
    Path(path).write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


class Clip:
    """Receita do clipe, quadros do vídeo e a análise do vídeo (posição, base, alcance)."""

    def __init__(self, char, name, folder, world_scale):
        self.cfg = read(ROOT / f'tools/sprites/clips/{char}_{name}.json')
        v = self.cfg['video']
        self.q = [int(x) for x in str(v['quadros']).split(',')]
        self.d = analyse(str(Path(folder) / v['arquivo']))
        self.info = self.d['info']
        self.k = v['escala'] * world_scale   # px do vídeo -> unidades do mundo

    def gaps(self):
        """Quadros do vídeo que cada quadro do clipe dura (o último: o passo mais comum)."""
        g = list(np.diff(self.q))
        return [int(x) for x in g] + [int(np.median(g)) if g else 1]

    def x(self, i):
        return float(self.info[min(i, len(self.info) - 1), 4])

    def reach(self, i):
        return float(self.info[i, 3] - self.info[0, 4])


def ms(video_frames, f):
    return round(1000 * video_frames / VIDEO_FPS / f, 3)


def game(video_frames, f):
    return max(1, round(GAME_FPS * video_frames / VIDEO_FPS / f))


def settle(clip, start, tol=1.0, run=3):
    """1º quadro depois de `start` em que o corpo para de andar no vídeo."""
    xs = clip.info[:, 4]
    for i in range(start, len(xs) - run):
        if all(abs(xs[j + 1] - xs[j]) < tol for j in range(i, i + run)):
            return i
    return len(xs) - 1


def walk(clip, folder, f):
    """Andar em arrancadas, como no vídeo: a distância de cada quadro do loop é o quanto o
    tronco anda no vídeo até o quadro seguinte (o mesmo registro da conversão, que deixa o
    tronco parado na célula), e o tempo dele é o do vídeo ÷ fator. O motor anda cada quadro
    nesse ritmo, então o pé fica plantado nas paradas e nas arrancadas."""
    step = clip.q[1] - clip.q[0]
    pts = clip.q + [clip.q[-1] + step]
    frames, _ = read_frames(str(Path(folder) / clip.cfg['video']['arquivo']))
    xs = [registration_x(keyer(frames[min(i, len(frames) - 1)], contour=bool(clip.cfg['video'].get('contorno'))))
          for i in pts]
    steps = [max(0.1, round(abs(b - a) * clip.k, 2)) for a, b in zip(xs, xs[1:])]
    seconds = [round(step / VIDEO_FPS / f, 4)] * len(steps)
    return steps, seconds


def remap(t, old, new):
    """Quadro t do golpe antigo no golpe novo (startup, ativo e recuperação esticados à parte)."""
    s0, a0, _ = old
    s1, a1, r1 = new
    if t < s0:
        return round(t * s1 / max(1, s0))
    if t < s0 + a0:
        return s1 + round((t - s0) * a1 / max(1, a0))
    r0 = max(1, old[2])
    return min(s1 + a1 + r1 - 1, s1 + a1 + round((t - s0 - a0) * r1 / r0))


def attack(clip, f, reverse, impact=None):
    """(frames, durationsMs, impactFrame, startup, active, recovery) contados no vídeo."""
    n, g = len(clip.q), clip.gaps()
    reach = [clip.reach(i) for i in clip.q]
    if impact is None:
        top = max(reach)
        impact = next(i for i, r in enumerate(reach) if r >= REACH_IMPACT * top)
    if reverse:  # o vídeo para na pose do impacto: segura e volta pelos mesmos quadros
        ida = g[:-1] + [HOLD_VIDEO_FRAMES]
        frames = list(range(n)) + list(range(n - 2, -1, -1))
        dur = ida + [g[i] for i in range(n - 2, -1, -1)]
        active_end = n
    else:
        frames, dur = list(range(n)), g
        top = max(reach)
        active_end = next((i for i in range(impact + 1, n) if reach[i] < REACH_IMPACT * top), impact + 1)
        if active_end == impact + 1 and top - reach[0] < 20:  # golpe sem alcance (6M): o impacto e o seguinte
            active_end = min(n, impact + 2)
    startup = game(sum(dur[:impact]), f)
    active = game(sum(dur[impact:active_end]), f)
    recovery = game(sum(dur[active_end:]), f)
    return frames, [ms(v, f) for v in dur], impact, startup, active, recovery


def plan(char, folder):
    path = CONFIG / f'{char}.json'
    if not path.exists():
        CONFIG.mkdir(exist_ok=True)
        write(path, DEFAULT)
    cfg = read(path)
    acc = lambda state: cfg.get('acelerarPorEstado', {}).get(state, cfg['acelerar'])  # noqa: E731
    pack_path = ROOT / f'characters/{char}/character.json'
    pack = read(pack_path)
    world = read(ROOT / f"tools/sprites/profiles/{pack['profile']}")['worldScale']
    clip = lambda name: Clip(char, name, folder, world)  # noqa: E731
    anims, moves = pack['animations'], pack['moves']
    report, mv = {}, {}

    # Andar: a arte dita o passo (sem deslizar) e o vídeo × fator a velocidade.
    for state, field, curve in (('WALK_FORWARD', 'walkSpeed', 'walkStepSeconds'),
                                ('WALK_BACK', 'walkBackSpeed', 'walkBackStepSeconds')):
        steps, seconds = walk(clip(state.lower()), folder, acc(state))
        anims[state]['distancias'] = steps
        anims[state]['distancePerFrame'] = round(sum(steps) / len(steps), 2)
        mv[field] = round(sum(steps) / sum(seconds), 1)  # média (no ar e para a CPU)
        mv[curve] = seconds
        report[state] = (f'{mv[field]} u/s em média, de {min(steps) / seconds[0]:.0f} a '
                         f'{max(steps) / seconds[0]:.0f} u/s (arrancadas do vídeo; fator {acc(state)})')

    # Corrida: o loop do vídeo no tempo dele ÷ fator; a velocidade é a do corpo no vídeo.
    c, f = clip('dash'), acc('DASH')
    step = c.q[1] - c.q[0]
    end = c.q[-1] + step
    dist = abs(c.x(end) - c.x(c.q[0])) * c.k
    seconds = (end - c.q[0]) / VIDEO_FPS / f
    mv['dashSpeed'] = round(dist / seconds, 1)
    anims['DASH']['durationsMs'] = [ms(step, f)] * len(c.q)
    report['DASH'] = f"{mv['dashSpeed']} u/s, ciclo de {seconds:.2f} s (fator {f})"

    # Backdash: o salto inteiro do clipe; a recuperação é o resto até o corpo parar no vídeo.
    c, f = clip('backdash'), acc('BACKDASH')
    g = c.gaps()
    end = c.q[-1] + g[-1]
    frames = game(end - c.q[0], f)
    dist = abs(c.x(end) - c.x(c.q[0])) * c.k
    mv['backdashFrames'] = frames
    mv['backdashSpeed'] = round(dist / (frames / GAME_FPS), 1)
    mv['backdashRecoveryFrames'] = game(max(1, settle(c, end) - end), f)
    lift = float(np.max(c.info[0, 0] - c.info[c.q[0]:end + 1, 0])) * c.k
    # O salto já está desenhado na arte (o corpo sai do chão no vídeo): o motor não soma outro.
    mv['backdashHopHeight'] = 0.0 if lift > 4 else 1.0
    anims['BACKDASH']['durationsMs'] = [ms(v, f) for v in g]
    report['BACKDASH'] = (f"{dist:.0f} u em {frames} quadros ({mv['backdashSpeed']} u/s), recupera "
                          f"{mv['backdashRecoveryFrames']}, salto de {lift:.0f} u na arte (fator {f})")

    # Pulo: altura e tempos de subida e descida do vídeo.
    c = clip('jump')
    base = c.info[:, 0]
    air = np.nonzero(base < base[0] - 10)[0]
    t0, t1 = int(air[0]), int(air[-1])
    apex = t0 + int(np.argmin(base[t0:t1 + 1]))
    height = float(base[0] - base[apex]) * c.k
    f = acc('JUMP')
    rise = (apex - t0 + 1) / VIDEO_FPS / f
    f_fall = acc('FALL')
    fall = (t1 + 1 - apex) / VIDEO_FPS / f_fall
    mv['jumpSpeed'] = round(2 * height / rise, 1)
    mv['jumpRisingGravity'] = round(mv['jumpSpeed'] / rise, 1)
    mv['jumpFallingGravity'] = round(2 * height / fall ** 2, 1)
    report['JUMP'] = f'{height:.0f} u de altura, {rise:.2f} s subindo e {fall:.2f} s descendo (fator {f}/{f_fall})'
    for state, name in (('JUMP', 'jump'), ('FALL', 'fall'), ('LAND', 'land'), ('CROUCH', 'crouch'), ('RISE', 'rise')):
        c = clip(name)
        g = c.gaps()
        if state == 'JUMP':  # antes de sair do chão: o motor já pula na hora, quadros rápidos
            g = [1 if q < t0 - 1 else v for q, v in zip(c.q, g)]
        anims[state]['durationsMs'] = [ms(v, acc(state)) for v in g]

    # Golpes: tempo do vídeo ÷ fator; vantagem e cancelamentos acompanham.
    for binding, (name, reverse) in ATTACKS.items():
        if binding not in moves or 'animation' not in moves[binding]:
            continue
        c, f = clip(name), acc(binding)
        explicit = c.cfg.get('animacao', {}).get('impactFrame') if not reverse else None
        frames, dur, impact, s, a, r = attack(c, f, reverse, explicit)
        m = moves[binding]
        old = (m['startupFrames'], m['activeFrames'], m['recoveryFrames'])
        later = (a + r) - (old[1] + old[2])
        for key in ('hitstunFrames', 'blockstunFrames'):
            m[key] = max(1, m[key] + later)
        windows = m['cancelWindows']
        for key, w in windows.items():
            if w:
                lo = max(s if key != 'whiff' else 0, remap(w[0], old, (s, a, r)))
                windows[key] = [lo, max(lo, remap(w[1], old, (s, a, r)))]
        m.update(startupFrames=s, activeFrames=a, recoveryFrames=r)
        anim = anims[m['animation']]
        anim.update(frames=frames, durationsMs=dur, impactFrame=impact)
        if reverse:  # o personagem.py repete a ida e volta se refizer o pacote
            c.cfg['animacao'] = {'frames': frames, 'impactFrame': impact}
            write(ROOT / f'tools/sprites/clips/{char}_{name}.json', c.cfg)
        report[binding] = f'{s}/{a}/{r} quadros (startup/ativo/recuperação), impacto no q{c.q[impact]} (fator {f})'

    # Temporários que tocam o M: os mesmos tempos do M novo.
    for state, why in pack.get('temporario', {}).get('estados', {}).items():
        if 'M do próprio' in why and state in anims:
            anims[state]['durationsMs'] = list(anims['MEDIUM_KICK']['durationsMs'])
            anims[state]['frames'] = list(anims['MEDIUM_KICK']['frames'])
    pack['fighter']['movimento'] = mv
    pack['comportamento'] = {
        'nota': 'Movimento, tempos e frame data medidos nos vídeos do personagem '
                '(tools/sprites/comportamento.py); fatores em tools/sprites/comportamento/' + f'{char}.json.',
        'acelerar': {s: acc(s) for s in ('WALK_FORWARD', 'WALK_BACK', 'DASH', 'BACKDASH', 'JUMP', 'FALL', 'LAND',
                                         'CROUCH', 'RISE', 'L', 'M', 'H', '6M')},
        'medidas': report,
    }
    return pack_path, pack, mv, report


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('pasta', help='pasta com os vídeos .mp4')
    p.add_argument('--ver', action='store_true', help='só mostra, sem gravar')
    args = p.parse_args()
    path, pack, mv, report = plan(args.personagem, args.pasta)
    for state, text in report.items():
        print(f'{args.personagem} {state:13} {text}')
    if args.ver:
        return
    write(path, pack)
    print(f'{path.relative_to(ROOT)} atualizado; rode tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
