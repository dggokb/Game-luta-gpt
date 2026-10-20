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

Além dos estados do p01: IDLE2 (segundo idle, toca uma vez a cada duas voltas do IDLE) e 6M
(o → + M com animação própria; sem ele o motor monta o 6M do M).

Clipe com quadros escolhidos à mão grava "quadrosAMao" (o motivo) e só é refeito quando o
estado é pedido pelo nome. "animacao" no clipe dá os quadros de jogo quando não são a folha
em ordem: o L do vídeo que termina parado no impacto volta pelos mesmos quadros
({"frames": [1,3,4,4,4,4,3,2,1,0,0], "impactFrame": 2}).

Personagem incompleto (para testar no jogo): --temporario preenche os estados do p01 sem
vídeo de forma TEMPORÁRIA e marcada no pacote ("temporario"): golpes agachados e aéreos
usam a pose, especiais tocam o M do próprio personagem e reações, defesas, queda, agarrão,
intro, vitória, derrota e provocação ficam sem animação (o motor usa a guarda e a queda
dele). Nunca a arte de outro personagem. --so-pacote só atualiza o pacote, sem converter.

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
          'HIT_STAND', 'HIT_CROUCH', 'HIT_AIR', 'KNOCKDOWN', 'INTRO', 'VICTORY', 'DEFEAT', 'TAUNT', 'THROW',
          'IDLE2', '6M']
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
    'IDLE2': ['idle2'], '6M': ['6m', 'frentem', 'm_baixo', 'mbaixo'],
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
# Clipes de cada estado (o padrão é o nome do estado em minúsculas).
CLIPS_OF_STATE = dict(JUMP=('jump', 'fall', 'land'), CROUCH=('crouch', 'rise'), KNOCKDOWN=('fall_down', 'getup'),
                      THROW=('throw',), IDLE2=('idle2',), **{'6M': ('overhead',)})
# → + M próprio: o mesmo golpe que o motor monta do M (GolpeAlto.java), com a animação do vídeo.
OVERHEAD = dict(startupFrames=20, activeFrames=3, recoveryFrames=14, hitstunFrames=20, blockstunFrames=11,
                damagePermille=1150, hitHeight=110, hitFloor=40)
OVERHEAD_FRAMES = 15
# Temporários (--temporario): golpe sem vídeo que tem pose no motor, e especiais que tocam o M.
POSE_OF = dict(CROUCH_LIGHT=('2L', 'CROUCH'), CROUCH_MEDIUM=('2M', 'CROUCH'), CROUCH_HEAVY=('2H', 'CROUCH'),
               JUMP_LIGHT=('jL', 'AIR'), JUMP_MEDIUM=('jM', 'AIR'), JUMP_HEAVY=('jH', 'AIR'))
TEMP_WITH_M = ('SPECIAL_ENERGY', 'SPECIAL_S2', 'SPECIAL_S3', 'SPECIAL_S4', 'SUPER_WAVE', 'ULTRA_BEAM')
TEMP_NOTE = ('TEMPORÁRIO até ter vídeo (personagem.py --temporario): golpes agachados e aéreos usam a pose; '
             'especiais tocam o M do próprio personagem; reações, defesas, queda, agarrão, intro, vitória, '
             'derrota e provocação ficam sem animação e o motor usa a guarda e a queda dele. '
             'Nunca a arte de outro personagem.')


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


def overhead_frames(d, n=OVERHEAD_FRAMES, k=8):
    """→ + M: o punho sobe (topo mais alto) e desce; o impacto é o corpo mais baixo depois."""
    s, e = segment(d)
    top = d['info'][:, 1]
    peak = min(range(s, e + 1), key=lambda i: top[i])
    impact = max(range(peak, e + 1), key=lambda i: top[i])
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
    if state == 'IDLE2':  # toca inteiro, uma vez (do 1º quadro, na guarda, ao último)
        return {'idle2': (list(range(len(d['info']))), {})}
    if state == '6M':
        return {'overhead': (clean(d, overhead_frames(d)), {})}
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
            'output': f'android/app/src/main/res/drawable-nodpi/{cid}.webp',
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


def convert(char, name, video, quadros, opts, manual, densidade=None):
    """Duas passadas: célula grande para medir o alcance dos quadros, depois a célula justa.
    Medidas em px lógicos; com densidade 2 (HD) a folha tem o dobro de px."""
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
    if densidade:
        args += ['--densidade', str(densidade)]
    run(args + ['--celula', '512', '--largura', '768', '--raiz', '384,470', '--aparar-borda', '0'])
    d = int(json.loads(clip.read_text(encoding='utf-8')).get('pixelScale', 1))
    sheet = Image.open(ROOT / f'art/sprites/source/{char}_{name}_video_normalized.png')
    boxes = [sheet.crop((i % cols * 768 * d, i // cols * 512 * d, (i % cols + 1) * 768 * d, (i // cols + 1) * 512 * d))
             .getchannel('A').point(lambda v: 255 if v > 10 else 0).getbbox() for i in range(len(quadros))]
    boxes = [[v // d for v in b] for b in boxes if b]
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


def game_frames(cfg, ref, state, n):
    """Quadros de jogo: os do "animacao" do clipe (ida e volta pelos mesmos quadros), os do
    p01 nos estados que dividem a folha, ou a folha em ordem."""
    custom = cfg.get('animacao', {})
    if 'frames' in custom:
        return list(custom['frames'])
    return list(ref['frames']) if state in SHARED_FRAMES else list(range(n))


def timed(ref_ms, count):
    """Tempos do p01 para `count` quadros (a mesma soma quando a contagem é outra)."""
    if len(ref_ms) == count:
        return list(ref_ms)
    return [round(sum(ref_ms) / count, 3)] * count


def overhead(pack, n, k):
    """6M com animação própria: o golpe alto que o motor montaria do M (GolpeAlto.java) e os
    tempos com o impacto no começo da janela ativa."""
    m, h = pack['moves']['M'], pack['moves']['H']
    o = OVERHEAD
    total = o['startupFrames'] + o['activeFrames'] + o['recoveryFrames']
    targets = [t for t in h['cancelInto'] if t in ('S', 'SUPER') or (len(t) == 2 and t[0] == 'S' and t[1] in '23456789')]
    window = [o['startupFrames'], o['startupFrames'] + o['activeFrames'] + 8] if targets else None
    move = {'animation': 'OVERHEAD', 'damage': max(1, m['damage'] * o['damagePermille'] // 1000), 'reach': m['reach'],
            'startupFrames': o['startupFrames'], 'activeFrames': o['activeFrames'],
            'recoveryFrames': o['recoveryFrames'], 'hitstunFrames': o['hitstunFrames'],
            'blockstunFrames': o['blockstunFrames'], 'hitstopFrames': m['hitstopFrames'],
            'cancelWindows': {'hit': window, 'block': None, 'whiff': None}, 'cancelInto': targets,
            'pushbackOnHit': m['pushbackOnHit'], 'pushbackOnBlock': m['pushbackOnBlock'], 'maxUsesPerCombo': 1,
            'overhead': True, 'hitHeight': o['hitHeight'], 'hitFloor': o['hitFloor']}
    before = 1000 * o['startupFrames'] / 60 / k
    after = 1000 * (total - o['startupFrames']) / 60 / (n - k)
    return move, [round(before, 3)] * k + [round(after, 3)] * (n - k)


def extras(pack, char, clips, rebuilt):
    """IDLE2 e 6M: estados do motor novo que o p01 não tem."""
    anims = pack['animations']
    if 'idle2' in clips and ('IDLE2' not in anims or 'idle2' in rebuilt):
        n = json.loads(clip_path(char, 'idle2').read_text(encoding='utf-8'))['expectedFrames']
        anims['IDLE2'] = {'atlas': f'{char}_idle2', 'frames': list(range(n)), 'loop': False,
                          'durationsMs': [round(1000 / 24, 3)] * n}  # o tempo do vídeo
    if 'overhead' in clips and ('OVERHEAD' not in anims or 'overhead' in rebuilt or '6M' not in pack['moves']):
        cfg = json.loads(clip_path(char, 'overhead').read_text(encoding='utf-8'))
        frames = game_frames(cfg, {}, '6M', cfg['expectedFrames'])
        k = cfg.get('animacao', {}).get('impactFrame', 8)
        move, ms = overhead(pack, len(frames), k)
        anims['OVERHEAD'] = {'atlas': f'{char}_overhead', 'frames': frames, 'loop': False, 'durationsMs': ms,
                             'impactFrame': k}
        pack['moves']['6M'] = move


def temporary(pack, char, k):
    """Estados do p01 sem vídeo, preenchidos de forma TEMPORÁRIA e marcada em "temporario".
    Só a arte do próprio personagem: pose do motor, o M dele ou nenhuma animação."""
    anims, moves = pack['animations'], pack['moves']
    was = pack.get('temporario', {}).get('estados', {})
    temp = {}
    for state, (binding, pose) in POSE_OF.items():
        if state not in anims or state in was:
            anims.pop(state, None)
            moves[binding].pop('animation', None)
            moves[binding]['pose'] = pose
            temp[state] = f'{binding}: pose {pose} (sem animação própria)'
    if 'JUMP_HEAVY_DOWN' not in anims or 'JUMP_HEAVY_DOWN' in was:
        anims.pop('JUMP_HEAVY_DOWN', None)
        moves.pop('j2H', None)
        fighter = pack['fighter']
        attacks = list(moves.values()) + list(pack.get('specialMoves', {}).values()) + \
            [fighter[k]['attack'] for k in ('energy', 'super') if k in fighter]
        for mv in attacks:  # rotas que levavam ao j2H
            for field in ('cancelInto', 'rotasProprias'):
                if 'j2H' in mv.get(field, []):
                    mv[field].remove('j2H')
        temp['JUMP_HEAVY_DOWN'] = 'sem j2H: no ar com ↓ sai o jH'
    m = anims['MEDIUM_KICK']
    for state in TEMP_WITH_M:
        if state not in anims or state in was:
            anims[state] = {'atlas': m['atlas'], 'frames': list(m['frames']), 'loop': False,
                            'durationsMs': list(m['durationsMs'])}
            temp[state] = 'o M do próprio personagem'
    for state in P01['animations']:
        if state not in anims and state not in temp:
            temp[state] = 'sem animação: o motor usa a guarda/queda dele'
    pack['temporario'] = {'nota': TEMP_NOTE, 'estados': temp}


def restore(pack, char, k):
    """Estado que era temporário e ganhou vídeo volta ao golpe do p01."""
    temp = pack.get('temporario', {}).get('estados', {})
    base = None
    for state in list(temp):
        real = state in pack['animations'] and pack['animations'][state]['atlas'] != pack['animations']['MEDIUM_KICK']['atlas']
        if not real or state == 'MEDIUM_KICK':
            continue
        base = base or new_pack(char, k)
        for binding, mv in base['moves'].items():
            if mv.get('animation') == state:
                pack['moves'][binding] = mv
        del temp[state]
    if 'temporario' in pack and not temp:
        del pack['temporario']


def update_pack(char, rebuilt, temporario=False):
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
        cfg = json.loads(clip_path(char, name).read_text(encoding='utf-8'))
        old = pack['animations'].get(state)
        anim = {'atlas': f'{char}_{name}', 'frames': game_frames(cfg, ref, state, cfg['expectedFrames']),
                'loop': ref['loop']}
        same_count = old is not None and len(old.get('durationsMs', [])) == len(anim['frames'])
        if 'distancePerFrame' in ref:
            anim['distancePerFrame'] = old['distancePerFrame'] if old else round(ref['distancePerFrame'] * k, 2)
        elif same_count:
            anim['durationsMs'] = old['durationsMs']  # tempo já ajustado continua
        else:
            anim['durationsMs'] = [41] * len(anim['frames']) if state == 'IDLE' else timed(ref['durationsMs'], len(anim['frames']))
        if 'impactFrame' in ref:  # a arte nova tem o impacto na posição do p01
            anim['impactFrame'] = cfg.get('animacao', {}).get('impactFrame', ref['impactFrame'])
        if old is None or name in rebuilt or old.get('atlas') != anim['atlas']:
            pack['animations'][state] = anim
    extras(pack, char, clips, rebuilt)
    restore(pack, char, k)
    if temporario or 'temporario' in pack:
        temporary(pack, char, k)
    missing = set(P01['animations']) - set(pack['animations']) - set(pack.get('temporario', {}).get('estados', {}))
    if missing:
        print(f'{char}: ainda sem vídeo para {sorted(missing)}; o pacote só é gravado quando tiver todos '
              f'(para testar antes: --temporario)')
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    if 'temporario' in pack:
        print(f"{char}: TEMPORÁRIOS {sorted(pack['temporario']['estados'])}")
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


def hand_picked(char, state):
    """Estado com clipe de quadros escolhidos à mão: só refaz quando pedido pelo nome."""
    for name in CLIPS_OF_STATE.get(state, (state.lower(),)):
        path = ROOT / f'tools/sprites/clips/{char}_{name}.json'
        if path.exists() and 'quadrosAMao' in json.loads(path.read_text(encoding='utf-8')):
            print(f'{state}: quadros escolhidos à mão ({path.name}); peça pelo nome para refazer')
            return True
    return False


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('pasta', help='pasta com os vídeos .mp4')
    p.add_argument('estados', nargs='*', help='estados a refazer (padrão: todos com vídeo)')
    p.add_argument('--manual', action='append', default=[], help='CLIPE=ESCALA (com --motivo)')
    p.add_argument('--motivo', help='por que a escala é manual')
    p.add_argument('--densidade', type=int,
                   help='px de textura por px do jogo (2 = HD direto do vídeo); padrão: o da receita')
    p.add_argument('--temporario', action='store_true',
                   help='grava o pacote incompleto, com os estados sem vídeo marcados como temporários')
    p.add_argument('--so-pacote', action='store_true',
                   help='só atualiza o pacote, sem converter vídeo (os estados dados contam como refeitos)')
    args = p.parse_args()
    char = args.personagem
    if not (ROOT / f'art/keys/{char}/tamanho.json').exists():
        raise SystemExit(f'calibre antes: python3 tools/sprites/tamanho.py calibrar {char} <vídeo de agachar> --escala-de p01')
    if args.so_pacote:  # estados pedidos: clipes já reconvertidos à mão (video_para_sprite.py --clipe)
        redone = {n for s in args.estados for n in CLIPS_OF_STATE.get(s, (s.lower(),))}
        if update_pack(char, redone, args.temporario):
            print(f'characters/{char}/character.json atualizado; rode tools/sprites/build_characters.py --write')
        return
    videos = video_map(char, args.pasta)
    states = args.estados or [s for s in STATES if s in videos and not hand_picked(char, s)]
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
            msg = convert(char, name, *jobs[name], manual.get(name), args.densidade)
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
    if update_pack(char, set(done), args.temporario):
        print(f'characters/{char}/character.json atualizado; rode tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
