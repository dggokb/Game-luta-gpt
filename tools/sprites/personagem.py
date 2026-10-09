#!/usr/bin/env python3
"""Monta (ou atualiza) um personagem a partir dos vídeos dele, num comando.

    python3 tools/sprites/personagem.py p05 <pasta com os vídeos>            # tudo
    python3 tools/sprites/personagem.py p05 <pasta> CROUCH_LIGHT HIT_AIR     # só esses estados

Antes, uma vez: imagens iniciais em art/keys/<id>/ e
`python3 tools/sprites/tamanho.py calibrar <id> <vídeo de agachar> --escala-de p01`.

Para cada estado o script acha no vídeo o trecho da ação e o quadro do impacto, escolhe os
quadros na mesma contagem do p01 (assim o tempo e o impacto do p01 servem direto), pula
quadros com efeito desenhado pelo vídeo (clarão, arco, poeira) e converte com
video_para_sprite.py, que mede a escala e grava a receita no clipe. Depois atualiza o
pacote em characters/<id>/:

- personagem novo: copia os dados do p01 (golpes, especiais, Super) com corpo e alcance na
  escala da altura dele e cria o perfil tools/sprites/profiles/<id>.json;
- personagem que já existe: só a animação do estado refeito muda, e o tempo dela só volta
  ao do p01 se a contagem de quadros mudou. Dano, frame data, poderes e tempos ajustados à
  mão continuam.

O nome de cada vídeo por estado fica em tools/sprites/videos/<id>.json (criado na 1ª vez a
partir dos nomes conhecidos; edite se um vídeo tiver outro nome). Escala manual, só quando
a medida falha: --manual ESTADO=0.36 --motivo "..." (fica gravada na receita e é reusada).

Precisa de opencv e numpy: pip install -r tools/sprites/requirements-animar.txt
"""
import argparse
import copy
import json
import os
import subprocess
import sys
from concurrent.futures import ProcessPoolExecutor, ThreadPoolExecutor
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
from video_para_sprite import best_loop, key, read_frames, torso_x  # noqa: E402

CACHE = ROOT / 'android/app/build/video-cache'
P01 = json.loads((ROOT / 'characters/player_base/character.json').read_text(encoding='utf-8'))

# Estados que o script monta; THROW vira o atlas de agarrar (0-3) e jogar (3-8).
STATES = ['IDLE', 'WALK_FORWARD', 'WALK_BACK', 'DASH', 'BACKDASH', 'CROUCH', 'JUMP', 'LIGHT_JAB',
          'MEDIUM_KICK', 'HEAVY_STRAIGHT', 'CROUCH_LIGHT', 'CROUCH_MEDIUM', 'CROUCH_HEAVY', 'JUMP_LIGHT',
          'JUMP_MEDIUM', 'JUMP_HEAVY', 'JUMP_HEAVY_DOWN', 'SPECIAL_ENERGY', 'SPECIAL_S2', 'SPECIAL_S3',
          'SPECIAL_S4', 'SUPER_WAVE', 'ULTRA_BEAM', 'DEFENSE_STAND', 'DEFENSE_CROUCH', 'DEFENSE_AIR',
          'HIT_STAND', 'HIT_CROUCH', 'HIT_AIR', 'KNOCKDOWN', 'INTRO', 'VICTORY', 'DEFEAT', 'TAUNT', 'THROW']
# Nomes de arquivo já usados nos vídeos (sem .mp4, sem diferença de maiúsculas).
ALIASES = {
    'IDLE': ['idle'], 'WALK_FORWARD': ['frente', 'andarfrente', 'andarparafrente'], 'WALK_BACK': ['tras', 'back', 'andartras', 'andarparatras'],
    'DASH': ['dash'], 'BACKDASH': ['backdash'], 'CROUCH': ['agachar', 'abaixar', 'agachado', 'agacha'], 'JUMP': ['pulo', 'jump'],
    'LIGHT_JAB': ['l', 'socofraco', 'll'], 'MEDIUM_KICK': ['m', 'socomedio'], 'HEAVY_STRAIGHT': ['h', 'socoforte'],
    'CROUCH_LIGHT': ['2l', 'socofracobaixo', 'chutebaixofraco'], 'CROUCH_MEDIUM': ['2m', 'rasteira', 'rasteirabaixo'],
    'CROUCH_HEAVY': ['2h', 'upper'], 'JUMP_LIGHT': ['jl', 'pulochute', 'pulochutefraco'],
    'JUMP_MEDIUM': ['jm', 'socopulo'], 'JUMP_HEAVY': ['jh', 'pulosocoforte', 'pulochuteforteparafrente'],
    'JUMP_HEAVY_DOWN': ['j2h', 'pulosocoparabaixo', 'pulosocoparabaixoforte'], 'SPECIAL_ENERGY': ['s1'],
    'SPECIAL_S2': ['s2'], 'SPECIAL_S3': ['s3'], 'SPECIAL_S4': ['s4'], 'SUPER_WAVE': ['super'], 'ULTRA_BEAM': ['ultra'],
    'DEFENSE_STAND': ['defesaempe', 'defesacima', 'defendecima'], 'DEFENSE_CROUCH': ['defesaagachado', 'defesaagaxado', 'defesabaixo', 'defesanochao', 'defesaembaixo', 'defesachao', 'defendebaixo'],
    'DEFENSE_AIR': ['defesanoar', 'defesapulo', 'defesapulando'], 'HIT_STAND': ['levargolpeempe', 'danoempe', 'damoempe', 'danocima'],
    'HIT_CROUCH': ['levargolpeagachada', 'levargolpeagachado', 'danochao', 'danoagachado', 'danobaixo'],
    'HIT_AIR': ['levargolpenoar', 'danopulando', 'danoar', 'golpenoar', 'danopulo', 'dano pulo'], 'KNOCKDOWN': ['derrubado', 'cair_levantar', 'caindo', 'queda_levanta', 'derrubar_levantar', 'derruma_levanta', 'derrumba_levanta', 'derruba_levanta'],
    'INTRO': ['intro'], 'VICTORY': ['vitoria'], 'DEFEAT': ['derrota'], 'TAUNT': ['provocacao', 'provocar', 'provoca'], 'THROW': ['agarrao', 'agarrar'],
}
UP = {'CROUCH_HEAVY', 'SPECIAL_S2'}  # impacto pela altura (golpe para cima)
AIR = {'JUMP_LIGHT', 'JUMP_MEDIUM', 'JUMP_HEAVY', 'JUMP_HEAVY_DOWN', 'DEFENSE_AIR', 'HIT_AIR'}
PUSHED = {'DEFENSE_STAND', 'DEFENSE_CROUCH', 'HIT_STAND', 'HIT_CROUCH', 'BACKDASH', 'DASH'}
FLOOR = {'WALK_FORWARD', 'WALK_BACK', 'DASH', 'DEFENSE_STAND', 'DEFENSE_CROUCH', 'HIT_STAND', 'HIT_CROUCH', 'GETUP'}
WHOLE = {'INTRO', 'VICTORY', 'DEFEAT', 'TAUNT'}
# Animação -> clipe (nome do atlas sem o id); sem vídeo próprio usa o jH do personagem.
CLIP_OF = dict(IDLE='idle', COMBAT='idle', CROUCH='crouch', RISE='rise', JUMP='jump', FALL='fall', LAND='land',
               KNOCKDOWN='fall_down', GROUNDED='fall_down', GETUP='getup', THROW_GRAB='throw', THROW_TOSS='throw')
FALLBACK = dict(JUMP_MEDIUM='jump_heavy', JUMP_HEAVY_DOWN='jump_heavy')
SHARED_FRAMES = {'KNOCKDOWN', 'GROUNDED', 'THROW_GRAB', 'THROW_TOSS', 'COMBAT'}


# ---------------------------------------------------------------- análise do vídeo

def analyse(path):
    """Por quadro: base, topo, esquerda, direita, tronco, máscara reduzida e área clara."""
    import cv2
    CACHE.mkdir(parents=True, exist_ok=True)
    stat = os.stat(path)
    name = CACHE / f'{Path(path).stem}-{stat.st_size}-{int(stat.st_mtime)}.npz'
    if name.exists():
        d = np.load(name)
        return {k: d[k] for k in d.files}
    frames, fps = read_frames(path)
    rows, masks, bright = [], [], []
    for f in frames:
        k = key(f)
        a = k[..., 3] > 128
        ys, xs = np.nonzero(a)
        rows.append((ys.max(), ys.min(), xs.min(), xs.max(), torso_x(k)))
        masks.append(a[::4, ::4])
        hsv = cv2.cvtColor(f, cv2.COLOR_BGR2HSV)
        # Clarão, estrela de impacto e arco: quase branco ou amarelo bem claro.
        bright.append(int((a & (hsv[..., 2] > 235) & (hsv[..., 1] < 110)).sum()))
    d = dict(info=np.array(rows, float), masks=np.array(masks), fps=np.array(fps), bright=np.array(bright))
    np.savez_compressed(name, **d)
    return d


def segment(d, thresh=0.12):
    """Trecho em que a silhueta se afasta da do 1º quadro (a ação)."""
    m = d['masks']
    diff = np.array([np.logical_xor(m[0], mi).sum() for mi in m]) / m[0].sum()
    moving = np.nonzero(diff > thresh)[0]
    if len(moving) == 0:
        return 0, len(diff) - 1
    return max(0, moving[0] - 1), min(len(diff) - 1, moving[-1] + 1)


def spread(a, b, n, endpoint=True):
    return [int(round(v)) for v in np.linspace(a, b, n, endpoint=endpoint)] if n > 0 else []


def airborne(d, s, e):
    info = d['info']
    lift = 0.15 * (info[0, 0] - info[0, 1])
    air = [i for i in range(s, e + 1) if info[i, 0] < info[0, 0] - lift]
    return (air[0], air[-1]) if air else (s, e)


def attack_frames(d, state, n, k):
    """n quadros com o impacto na posição k (a do p01)."""
    s, e = segment(d)
    if state in AIR:
        s, e = airborne(d, s, e)
    info = d['info']
    rng = range(s, e + 1)
    if state in UP:
        impact = min(rng, key=lambda i: info[i, 1])
    else:
        impact = max(rng, key=lambda i: info[i, 3] - info[0, 4])
    if e - s + 1 >= n:
        impact = min(max(impact, s + k), e - (n - k) + 1)
    return spread(s, impact, k, endpoint=False) + spread(impact, e, n - k)


def jump_frames(d):
    info = d['info']
    air = np.nonzero(info[:, 0] < info[0, 0] - 10)[0]
    t0, t1 = air[0], air[-1] + 1
    apex = t0 + int(np.argmin(info[t0:t1, 0]))
    return dict(jump=[0, max(0, t0 - 1)] + spread(t0, apex, 4), fall=spread(apex + 1, t1 - 1, 6),
                land=spread(t1, min(len(info) - 1, t1 + 6), 4))


def crouch_frames(d):
    h = d['info'][:, 0] - d['info'][:, 1]
    down = np.nonzero(h <= h.min() + 4)[0]
    c0, c1 = down[0], down[-1]
    s = int(np.nonzero(h < h[0] - 6)[0][0])
    up = [i for i in range(c1, len(h)) if h[i] >= h[0] - 6]
    return dict(crouch=spread(max(0, s - 1), c0, 8), rise=spread(c1, up[0] if up else len(h) - 1, 6))


def knockdown_frames(d):
    h = d['info'][:, 0] - d['info'][:, 1]
    lying = np.nonzero(h < 0.45 * h[0])[0]
    if not len(lying):  # deitado com as pernas para cima: o mais baixo do vídeo
        lying = np.nonzero(h <= h.min() + 0.15 * (h[0] - h.min()))[0]
    l0, l1 = lying[0], lying[-1]
    s, e = segment(d)
    falling = np.nonzero(h[:l0] < 0.85 * h[0])[0]  # começa já caindo, não em pé
    if len(falling):
        s = max(s, int(falling[0]))
    return spread(s, l0, 7) + spread(l0, l1, 4), spread(l1, e, 12)


def loop_frames(path, d, n_min, passo):
    frames, _ = read_frames(path)
    s, e = segment(d, 0.02)
    _, a, b = best_loop(frames, max(s, 4), min(e, len(frames) - 2), n_min)
    return list(range(a, b, passo))


def clean(d, quadros):
    """Troca quadros com efeito do vídeo (silhueta bem maior ou muito branco/amarelo claro
    que o resto do trecho) pelo vizinho limpo mais perto."""
    area = d['masks'].reshape(len(d['masks']), -1).sum(1).astype(float)
    light = d['bright']
    seg = slice(min(quadros), max(quadros) + 1)
    ref, ref_light = np.median(area[seg]), np.median(light[seg])
    dirty = lambda i: area[i] > 1.3 * ref or light[i] > 1.6 * ref_light + 300
    out = []
    for q in quadros:
        if dirty(q):
            for off in (1, -1, 2, -2, 3, -3, 4, -4):
                if 0 <= q + off < len(area) and not dirty(q + off):
                    q += off
                    break
        out.append(q)
    return out


def plan(path, state):
    """{clipe: (quadros, opções)} de um estado."""
    d = analyse(path)
    if state == 'THROW':
        s, e = segment(d)
        return {'throw': (clean(d, spread(s, e, 9)), {})}
    anim = P01['animations'][state]
    n = len(anim['frames'])
    if state == 'IDLE':
        return {'idle': (loop_frames(path, d, 40, 1), {})}
    if state in ('WALK_FORWARD', 'WALK_BACK'):
        return {state.lower(): (loop_frames(path, d, 24, 2), dict(pe_no_chao=True))}
    if state == 'DASH':  # o trecho correndo, cada quadro no próprio tronco
        s, e = segment(d)
        return {'dash': (clean(d, spread(s + 2, e - 2, 6)), dict(fixar='registro', pe_no_chao=True))}
    if state == 'JUMP':
        return {k: (q, dict(pe_no_chao=k != 'land')) for k, q in jump_frames(d).items()}
    if state == 'CROUCH':
        return {k: (q, {}) for k, q in crouch_frames(d).items()}
    if state == 'KNOCKDOWN':
        down, up = knockdown_frames(d)
        return {'fall_down': (clean(d, down), dict(fixar='quadro', pe_no_chao=True)),
                'getup': (up, dict(fixar='registro', pe_no_chao=True, sem_chao=True))}
    if state in WHOLE:
        return {state.lower(): (spread(0, len(d['info']) - 1, n), {})}
    if 'impactFrame' in anim:
        return {state.lower(): (clean(d, attack_frames(d, state, n, anim['impactFrame'])),
                                dict(pe_no_chao=state in AIR))}
    s, e = segment(d)
    if state in AIR:
        s, e = airborne(d, s, e)
    return {state.lower(): (clean(d, spread(s, e, n)), dict(pe_no_chao=state in AIR or state in FLOOR,
                                                             fixar='registro' if state in PUSHED else 'tronco'))}


# ---------------------------------------------------------------- conversão

def clip_path(char, name):
    cid = f'{char}_{name}'
    path = ROOT / f'tools/sprites/clips/{cid}.json'
    if not path.exists():
        path.write_text(json.dumps({
            'id': cid, 'javaName': cid.upper(), 'profile': f'{char}.json',
            'source': f'art/sprites/source/{cid}_video_normalized.png',
            'output': f'android/app/src/main/res/drawable-nodpi/{cid}.png',
            'report': f'tools/sprites/reports/{cid}.report.json',
            'preview': f'android/app/build/sprite-review/{cid}-import.png',
            'expectedFrames': 1, 'segmentation': 'prepared-grid', 'columns': 1, 'rootMode': 'authored',
            'sourceNote': f'{char} gerado em vídeo (Seedance), convertido por tools/sprites/personagem.py '
                          f'(receita em "video").'}, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    return path


def run(args):
    r = subprocess.run([sys.executable, str(HERE / 'video_para_sprite.py')] + args, capture_output=True, text=True)
    if r.returncode:
        raise RuntimeError((r.stderr.strip() or r.stdout.strip()).splitlines()[-1])
    return r.stdout


def convert(char, name, video, quadros, opts, manual):
    """Duas passadas: célula grande para medir o alcance dos quadros, depois a célula justa."""
    clip = clip_path(char, name)
    cols = min(len(quadros), 11)
    args = [video, '--clipe', str(clip), '--personagem', char, '--quadros', ','.join(map(str, quadros)),
            '--colunas', str(cols), '--fixar', opts.get('fixar', 'tronco')]
    if opts.get('pe_no_chao'):
        args.append('--pe-no-chao')
    if (not opts.get('pe_no_chao') or opts.get('fixar') == 'registro') and not opts.get('sem_chao'):
        args.append('--limpar-chao')  # deitado não: o cabelo fica na faixa do chão
    if manual:
        args += ['--escala', str(manual[0]), '--motivo', manual[1]]
    run(args + ['--celula', '512', '--largura', '768', '--raiz', '384,470', '--aparar-borda', '0'])
    sheet = Image.open(ROOT / f'art/sprites/source/{char}_{name}_video_normalized.png')
    boxes = [sheet.crop((i % cols * 768, i // cols * 512, i % cols * 768 + 768, i // cols * 512 + 512))
             .getchannel('A').point(lambda v: 255 if v > 10 else 0).getbbox() for i in range(len(quadros))]
    boxes = [b for b in boxes if b]
    x0, y0 = min(b[0] for b in boxes), min(b[1] for b in boxes)
    x1, y1 = max(b[2] for b in boxes), max(b[3] for b in boxes)
    m = 14
    left, right, up, down = 384 - x0 + m, x1 - 384 + m, 470 - y0 + m, max(18, y1 - 470 + m)
    w = min(768, -(-(left + right) // 32) * 32)
    h = min(512, -(-(up + down) // 32) * 32)
    wide, tall = left + right > 768 or x0 <= 1 or x1 >= 767, up + down > 512 or y0 <= 1
    if wide or tall:  # passa da célula máxima (chicote, capa): célula máxima e borda cortada limpa
        args += ['--aparar-borda', '10']
        w, h = (768 if wide else w), (512 if tall else h)
        left, right = min(left, w // 2), min(right, w // 2)
        up = min(up, h - down)
    out = run(args + ['--celula', str(h), '--largura', str(w), '--raiz', f'{left + (w - left - right) // 2},{h - down}'])
    return out.strip().splitlines()[0]


# ---------------------------------------------------------------- pacote

def ensure_profile(char, k):
    path = ROOT / f'tools/sprites/profiles/{char}.json'
    if path.exists():
        return
    p = json.loads((ROOT / 'tools/sprites/profiles/player_base.json').read_text(encoding='utf-8'))
    p['id'] = char
    p['standingVisualHeight'] = round(p['standingVisualHeight'] * k)
    p['validation']['minOpaquePixels'] = 7000  # personagem menor agachado
    path.write_text(json.dumps(p, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def new_pack(char, k):
    """Dados do p01 com corpo, alcance e pontos de saída na escala da altura do personagem."""
    pack = copy.deepcopy(P01)
    pack.update(id=char, displayName=char, profile=f'{char}.json', animations={})
    f = pack['fighter']
    for key_ in f['body']:
        f['body'][key_] = round(f['body'][key_] * k)
    for part, keys in (('energy', ('spawnX', 'spawnY', 'crouchSpawnY')), ('super', ('spawnX', 'spawnY', 'airSpawnY'))):
        for key_ in keys:
            if key_ in f[part]:
                f[part][key_] = round(f[part][key_] * k)
    for group in (pack['moves'], pack.get('specialMoves', {})):
        for m in group.values():
            for key_ in ('reach', 'hitHeight'):
                if key_ in m:
                    m[key_] = round(m[key_] * k)
    return pack


def update_pack(char, rebuilt):
    """Aponta cada animação para o clipe dela; só os estados refeitos mudam de tempo, e só
    se a contagem de quadros mudou."""
    k = json.loads((ROOT / f'art/keys/{char}/tamanho.json').read_text(encoding='utf-8'))['altura'] / 224
    ensure_profile(char, k)
    path = ROOT / f'characters/{char}/character.json'
    pack = json.loads(path.read_text(encoding='utf-8')) if path.exists() else new_pack(char, k)
    clips = {p.stem[len(char) + 1:] for p in (ROOT / 'tools/sprites/clips').glob(f'{char}_*.json')}
    for state, ref in P01['animations'].items():
        name = CLIP_OF.get(state, state.lower())
        name = name if name in clips else FALLBACK.get(state)
        if name not in clips:
            continue
        n = json.loads(clip_path(char, name).read_text(encoding='utf-8'))['expectedFrames']
        old = pack['animations'].get(state)
        anim = {'atlas': f'{char}_{name}',
                'frames': list(ref['frames']) if state in SHARED_FRAMES else list(range(n)), 'loop': ref['loop']}
        same_count = old is not None and len(old.get('durationsMs', [])) == len(anim['frames'])
        if 'distancePerFrame' in ref:
            anim['distancePerFrame'] = old['distancePerFrame'] if old else round(ref['distancePerFrame'] * k, 2)
        elif same_count:
            anim['durationsMs'] = old['durationsMs']  # tempo já ajustado continua
        else:
            anim['durationsMs'] = [41] * n if state == 'IDLE' else list(ref['durationsMs'])
        if 'impactFrame' in ref:  # a arte nova tem o impacto na posição do p01
            anim['impactFrame'] = ref['impactFrame']
        if old is None or name in rebuilt or old.get('atlas') != anim['atlas']:
            pack['animations'][state] = anim
    missing = set(P01['animations']) - set(pack['animations'])
    if missing:
        print(f'{char}: ainda sem vídeo para {sorted(missing)}; o pacote só é gravado quando tiver todos')
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    return True


# ---------------------------------------------------------------- comando

def video_map(char, folder):
    path = HERE / f'videos/{char}.json'
    known = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    files = {p.stem.lower(): p.stem for p in Path(folder).glob('*.mp4')}
    for state in STATES:
        if state not in known:
            hit = next((files[a] for a in ALIASES.get(state, []) if a in files), None)
            if hit:
                known[state] = hit
    path.parent.mkdir(exist_ok=True)
    path.write_text(json.dumps(known, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    return known


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('pasta', help='pasta com os vídeos .mp4')
    p.add_argument('estados', nargs='*', help='estados a refazer (padrão: todos com vídeo)')
    p.add_argument('--manual', action='append', default=[], help='CLIPE=ESCALA (com --motivo)')
    p.add_argument('--motivo', help='por que a escala é manual')
    args = p.parse_args()
    char = args.personagem
    if not (ROOT / f'art/keys/{char}/tamanho.json').exists():
        raise SystemExit(f'calibre antes: python3 tools/sprites/tamanho.py calibrar {char} <vídeo de agachar> --escala-de p01')
    videos = video_map(char, args.pasta)
    states = args.estados or [s for s in STATES if s in videos]
    absent = [s for s in states if s not in videos]
    if absent:
        raise SystemExit(f'sem vídeo para {absent}: ajuste tools/sprites/videos/{char}.json')
    manual = {}
    for item in args.manual:
        name, value = item.split('=')
        if not args.motivo:
            raise SystemExit('--manual exige --motivo')
        manual[name] = (float(value), args.motivo)
    paths = {s: str(Path(args.pasta) / f'{videos[s]}.mp4') for s in states}
    # A análise de cada vídeo roda em paralelo; o IDLE vem antes (outros vídeos podem medir a
    # escala pela guarda dele).
    with ProcessPoolExecutor(4) as ex:
        list(ex.map(analyse, sorted(set(paths.values()))))
    jobs = {}
    for s in states:
        for name, (quadros, opts) in plan(paths[s], s).items():
            jobs[name] = (paths[s], quadros, opts)
    order = sorted(jobs, key=lambda n: n != 'idle')
    done = []

    def work(name):
        try:
            msg = convert(char, name, *jobs[name], manual.get(name))
            done.append(name)
            return f'{name:16} {msg}'
        except Exception as e:  # noqa: BLE001 - relata e segue com os outros clipes
            return f'{name:16} ERRO {e}'
    if 'idle' in jobs:
        print(work('idle'), flush=True)
        order.remove('idle')
    with ThreadPoolExecutor(3) as ex:
        for line in ex.map(work, order):
            print(line, flush=True)
    if update_pack(char, set(done)):
        print(f'characters/{char}/character.json atualizado; rode tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
