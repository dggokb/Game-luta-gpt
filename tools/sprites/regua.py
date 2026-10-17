#!/usr/bin/env python3
"""Régua de tamanho: diz se uma animação de um personagem está maior ou menor que as outras.

    python3 tools/sprites/regua.py p02               # tabela no terminal (~1 min)
    python3 tools/sprites/regua.py p02 --relatorio   # + docs/personagens/regua/<id>.md, .json e .png
    python3 tools/sprites/regua.py p02 --corrigir VICTORY DASH   # leva esses clipes a 100%
    python3 tools/sprites/regua.py --aplicar ajustes-tamanho-20261014-1000.json   # do app
                                                 # (depois: build_characters.py --write)

A referência é a imagem inicial do personagem (art/keys/<id>/inicio_*.png): a mesma arte de
onde saem todos os vídeos, no tamanho oficial do tamanho.json. Tudo é medido no que o jogo
desenha: atlas empacotado x worldScale / pixelScale (com o sprite-autofix.json), raiz no chão.

Altura de silhueta não é régua: guarda aberta, corrida, pulo e agachado mudam a altura sem
mudar o tamanho do corpo. O fator de tamanho (1,00 = certo) vem, nesta ordem:
  1. inicio  - a imagem inicial inteira achada num quadro do clipe (quadro na pose dela);
  2. receita - a escala que a conversão mediu na câmera do vídeo pela imagem inicial (nota
               >= 0,9), vezes o que mudou o tamanho depois (fator óptico, folha do vídeo x
               folha do jogo). Escala achada pela guarda do IDLE, pela cabeça ou à mão é fraca;
  3. cabeca  - a cabeça da imagem inicial achada nos quadros (normalizada pela do IDLE): não
               depende da pose do corpo; também mostra zoom de câmera no meio do clipe.
A emenda confere a troca de animação como o jogo faz: a ponta do clipe sobre o IDLE q0 (ou o
fim do CROUCH nos agachados), raiz no mesmo ponto. Mesma pose (silhuetas encaixam): mede
tamanho e pé fora do lugar. Pose diferente: o salto é de pose, não de tamanho (a altura
muda na troca mesmo com o corpo certo) - corrige escolhendo outros quadros ou outro vídeo.

Precisa de opencv: pip install -r tools/sprites/requirements-animar.txt
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
import folhas  # noqa: E402

SIZE_WARN, SIZE_ERROR = 0.03, 0.06   # diferença de tamanho: aviso / erro
ZOOM_SPREAD = 0.08                   # cabeça cresce/diminui mais que isso no clipe: zoom de câmera
ZOOM_TREND = 0.8                     # ... de forma contínua (correlação com o número do quadro)
FOOT_WARN, FOOT_ERROR = 4, 12        # px do mundo: corpo fora do lugar na emenda
POSE_IOU = 0.85                      # silhuetas encaixadas a partir daqui: mesma pose
POSE_HEIGHT = 0.05                   # pose diferente que muda a altura mais que isso: salto
INICIO_SCORE = 0.80                  # imagem inicial achada no quadro (TM_CCOEFF_NORMED)
HEAD_SCORE = 0.60                    # cabeça achada no quadro
HEAD_FRAMES = 2                      # quadros com cabeça achada para valer a medida
STRONG_SCORE = 0.90                  # receita medida pela imagem inicial com essa nota
CHAIN_TOL = 0.02                     # conversão vídeo -> jogo não muda o tamanho
FIX_SCORE = 0.50                     # correção: cabeça achada com essa nota entra na linha de zoom
FIX_ZOOM = 0.04                      # correção: linha da cabeça varia mais que isso -> por quadro
FIX_MARGIN = 8                       # px da folha: o quadro aumentado não chega mais perto da borda
CANVAS, CX, CY = 900, 450, 760       # tela comum das emendas, raiz em (CX, CY)
AIR = {'JUMP', 'FALL', 'JUMP_LIGHT', 'JUMP_MEDIUM', 'JUMP_HEAVY', 'JUMP_HEAVY_DOWN',
       'DEFENSE_AIR', 'HIT_AIR'}
CROUCHED = {'CROUCH_LIGHT', 'CROUCH_MEDIUM', 'CROUCH_HEAVY', 'DEFENSE_CROUCH', 'HIT_CROUCH'}
LOCOMOTION = {'WALK_FORWARD', 'WALK_BACK', 'DASH', 'BACKDASH', 'CROUCH'}  # pose muda na troca
# Pontas que nunca encostam numa pose de guarda/agachado no jogo.
NO_SEAM = {('LAND', 'ini'), ('KNOCKDOWN', 'ini'), ('KNOCKDOWN', 'fim'), ('GROUNDED', 'ini'),
           ('GROUNDED', 'fim'), ('GETUP', 'ini'), ('THROW_GRAB', 'fim'), ('THROW_TOSS', 'ini'),
           ('VICTORY', 'fim'), ('DEFEAT', 'fim'), ('INTRO', 'ini'), ('CROUCH', 'fim'),
           ('RISE', 'ini'), ('WALK_FORWARD', 'fim'), ('WALK_BACK', 'fim'), ('DASH', 'fim'),
           ('HIT_STAND', 'fim'), ('HIT_CROUCH', 'fim'), ('DASH_STOP', 'ini')}


def read_json(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))


class Pack:
    """Clipes de um personagem no espaço do mundo, como o SpriteFighterRenderer desenha."""

    def __init__(self, cid):
        self.id = cid
        self.pack = read_json(ROOT / f'characters/{cid}/character.json')
        self.profile = read_json(ROOT / 'tools/sprites/profiles' / self.pack['profile'])
        self.keydir = ROOT / 'art/keys' / ('p01' if cid == 'player_base' else cid)
        keys = self.keydir / 'tamanho.json'
        self.keys = read_json(keys)['chaves'] if keys.exists() else {}
        self.clips = {}
        for p in (ROOT / 'tools/sprites/clips').glob('*.json'):
            cfg = read_json(p)
            self.clips[Path(cfg['output']).stem] = cfg
        self.fix = {}
        autofix = ROOT / 'android/app/src/main/assets/sprite-autofix.json'
        if autofix.exists():
            for e in read_json(autofix)['corrections']:
                for frame, size, dx, dy in e['frames']:
                    self.fix[(e['character'], e['atlas'], frame)] = (size, dx, dy)
        self.sheets = {}
        self.frames = {}

    def anim(self, state):
        return self.pack['animations'][state]

    def cfg(self, state):
        return self.clips[self.anim(state)['atlas']]

    def count(self, state):
        return len(self.anim(state)['frames'])

    def world(self, state, i):
        """Quadro i do clipe (índice dentro do clipe) em px do mundo: (rgba, raiz_x, raiz_y)."""
        return self.atlas_frame(self.anim(state)['atlas'], self.anim(state)['frames'][i])

    def atlas_frame(self, atlas, frame):
        if (atlas, frame) not in self.frames:
            self.frames[(atlas, frame)] = self._atlas_frame(atlas, frame)
        return self.frames[(atlas, frame)]

    def _atlas_frame(self, atlas, frame):
        cfg = self.clips[atlas]
        l = read_json(ROOT / cfg['report'])['packed']
        if atlas not in self.sheets:
            self.sheets[atlas] = Image.open(ROOT / cfg['output']).convert('RGBA')
        cell = folhas.cell(self.sheets[atlas], l, frame)
        size, dx, dy = self.fix.get((self.id, atlas, frame), (1.0, 0.0, 0.0))
        base = self.profile['worldScale'] / cfg.get('pixelScale', 1)
        k = base * size
        cell = cell.resize((max(1, round(cell.width * k)), max(1, round(cell.height * k))), Image.LANCZOS)
        return np.array(cell), l['rootX'] * k - dx * base, l['rootY'] * k - dy * base


def height(frame):
    rgba, _, ry = frame
    return ry - np.nonzero((rgba[..., 3] > 128).any(1))[0][0]


# ---------------------------------------------------------------- emenda

def place(mask, rx, ry, s=1.0):
    """Máscara escalada por s em volta da raiz, na tela comum com a raiz em (CX, CY)."""
    img = Image.fromarray(mask.astype(np.uint8) * 255)
    if s != 1.0:
        img = img.resize((max(1, round(img.width * s)), max(1, round(img.height * s))), Image.NEAREST)
    out = Image.new('L', (CANVAS, CANVAS))
    out.paste(img, (round(CX - rx * s), round(CY - ry * s)))
    return np.array(out) > 127


def shifted_iou(a, b, dx):
    """IoU de a com b deslocado dx px para a direita."""
    if dx > 0:
        inter = (a[:, dx:] & b[:, :-dx]).sum()
    elif dx < 0:
        inter = (a[:, :dx] & b[:, -dx:]).sum()
    else:
        inter = (a & b).sum()
    return inter / max(1, a.sum() + b.sum() - inter)


def fit(ref, frame):
    """(s, dx, iou): escala do quadro (em volta da raiz) e deslocamento que melhor encaixam a
    silhueta dele na de referência. Tamanho do quadro relativo à referência = 1 / s.
    Busca em 1/4 da resolução, refina em 1/2 e termina na resolução cheia."""
    (ra, rx, ry), (fa, fx, fy) = ref, frame
    r = place(ra[..., 3] > 128, rx, ry)
    fm = fa[..., 3] > 128
    s0, d0 = 1.0, 0
    for step, scales, shifts in ((4, np.arange(0.76, 1.31, 0.02), range(-16, 17)),
                                 (2, np.arange(-0.02, 0.0201, 0.005), range(-4, 5)),
                                 (1, np.arange(-0.005, 0.0051, 0.0025), range(-2, 3))):
        base = (s0, d0)
        best = (-1.0, s0, d0)
        a = r[::step, ::step]
        for s in scales if step == 4 else base[0] + scales:
            f = place(fm, fx, fy, s)[::step, ::step]
            for k in shifts:
                dx = k if step == 4 else base[1] // step + k
                v = shifted_iou(a, f, dx)
                if v > best[0]:
                    best = (v, float(s), dx * step)
        _, s0, d0 = best
    return s0, d0, best[0]


def seams(p):
    """Emendas com a guarda (IDLE q0) ou o agachado (fim do CROUCH), como o jogo troca. pe: px
    que o corpo está deslocado em relação à referência (+ = para onde o personagem olha)."""
    idle = p.world('IDLE', 0)
    crouch = p.world('CROUCH', p.count('CROUCH') - 1)
    out = []
    for state, a in p.pack['animations'].items():
        if state in AIR or not a['frames'] or state == 'IDLE':
            continue
        for end, i in (('ini', 0), ('fim', len(a['frames']) - 1)):
            if (state, end) in NO_SEAM or (end == 'fim' and len(a['frames']) == 1):
                continue
            name, ref = ('agachado', crouch) if state in CROUCHED or (state, end) == ('RISE', 'ini') \
                else ('guarda', idle)
            frame = p.world(state, i)
            s, dx, v = fit(ref, frame)
            out.append(dict(estado=state, ponta=end, quadro=i, ref=name, fator=1 / s, pe=-dx, iou=v,
                            altura=height(frame) / height(ref), mesma_pose=v >= POSE_IOU))
    return out


# ---------------------------------------------------------------- imagem inicial e cabeça

def inicio_images(p):
    """Imagens iniciais recortadas (RGBA) no tamanho oficial do mundo."""
    import cv2
    from video_para_sprite import key as keyer
    from tamanho import crop
    out = {}
    for name, escala in p.keys.items():
        rgba = crop(keyer(cv2.imread(str(p.keydir / f'{name}.png'))))
        k = escala * p.profile['worldScale']
        img = Image.fromarray(rgba).resize((round(rgba.shape[1] * k), round(rgba.shape[0] * k)), Image.LANCZOS)
        out[name] = np.array(img)
    return out


def head_of(rgba):
    """Cabeça (22% de cima da silhueta) sobre cinza, para comparar aparência."""
    from tamanho import flat
    m = rgba[..., 3] > 128
    ys = np.nonzero(m.any(1))[0]
    top, h = ys[0], int((ys[-1] - ys[0]) * 0.22)
    cx = int(np.nonzero(m[top + h // 2])[0].mean())
    return flat(rgba[top:top + h, max(0, cx - int(h * 0.7)):cx + int(h * 0.7)])


def zoom_search(tmpl, img, lo, hi, steps):
    """(nota, zoom) do template que bate melhor com a imagem (ambos já sobre cinza)."""
    import cv2
    best = (-1.0, 1.0)
    for z in np.exp(np.linspace(np.log(lo), np.log(hi), steps)):
        t = cv2.resize(tmpl, None, fx=z, fy=z, interpolation=cv2.INTER_AREA if z < 1 else cv2.INTER_CUBIC)
        if t.shape[0] > img.shape[0] or t.shape[1] > img.shape[1] or min(t.shape[:2]) < 8:
            continue
        best = max(best, (float(cv2.matchTemplate(img, t, cv2.TM_CCOEFF_NORMED).max()), float(z)))
    return best


def match_inicio(ref, frame):
    """(nota, zoom) da imagem inicial inteira no quadro: busca em meia resolução, ajuste fino
    em resolução cheia."""
    import cv2
    from tamanho import crop, flat
    t, img = flat(crop(ref)), flat(crop(frame, 40))
    half = lambda a: cv2.resize(a, None, fx=0.5, fy=0.5, interpolation=cv2.INTER_AREA)
    _, z = zoom_search(half(t), half(img), 0.72, 1.38, 31)
    return zoom_search(t, img, z * 0.98, z * 1.02, 9)


def picks(n):
    return sorted({0, n - 1} | set(range(0, n, max(1, n // 8))))


def inicio(p, refs, state):
    """Melhor encontro da imagem inicial nos quadros do clipe: dict(fator, nota, quadro, ref)."""
    names = (['inicio_agachado'] if state in CROUCHED | {'CROUCH', 'RISE'} else []) + ['inicio_centro']
    best = None
    for i in picks(p.count(state)):
        frame = p.world(state, i)[0]
        for name in (n for n in names if n in refs):
            nota, zoom = match_inicio(refs[name], frame)
            if best is None or nota > best['nota']:
                best = dict(fator=zoom, nota=nota, quadro=i, ref=name)
    return best


def head_zooms(p, head, state):
    """[(quadro, nota, zoom)] da cabeça de referência nos quadros do clipe."""
    from tamanho import crop, flat
    out = []
    for i in picks(p.count(state)):
        nota, z = zoom_search(head, flat(crop(p.world(state, i)[0], 30)), 0.7, 1.45, 38)
        out.append((i, nota, z))
    return out


def cabeca(p, head, state, idle_head):
    """Fator pela cabeça, relativo à cabeça do IDLE: dict(fator, min, max, zoom, por_quadro)
    ou None. zoom: o tamanho cresce ou diminui de forma contínua ao longo do clipe (a cabeça
    girando dá ruído de uns 4% para os dois lados, não uma tendência)."""
    good = [(i, z / idle_head) for i, nota, z in head_zooms(p, head, state) if nota >= HEAD_SCORE]
    if len(good) < min(HEAD_FRAMES, p.count(state)):
        return None
    zs = [z for _, z in good]
    trend = 0.0
    if len(good) >= 4 and state not in AIR | LOCOMOTION:
        trend = abs(float(np.corrcoef([i for i, _ in good], zs)[0, 1])) if np.std(zs) > 0 else 0.0
    return dict(fator=float(np.median(zs)), min=min(zs), max=max(zs),
                zoom=max(zs) / min(zs) - 1 > ZOOM_SPREAD and trend >= ZOOM_TREND,
                por_quadro={i: round(z, 3) for i, z in good})


# ---------------------------------------------------------------- receita

def recipe(p, state):
    """Como a receita mediu a escala do vídeo: (tipo, texto, fator aplicado depois)."""
    v = p.cfg(state).get('video')
    if not v:
        return 'manual', 'sem receita de vídeo', 1.0
    after = float(v.get('normalizedOpticalScale', 1.0))
    if 'motivo' in v or not v.get('medida'):
        return 'manual', v.get('motivo') or 'escala sem medida gravada', after
    medida = v['medida']
    nota = float(medida.rsplit('nota ', 1)[1].rstrip(')')) if 'nota ' in medida else 0.0
    if 'reescala' in v:
        return 'fraca', f"{medida}; reescala {min(v['reescala']):.2f}-{max(v['reescala']):.2f}", after
    kind = 'forte' if 'inicio_' in medida and nota >= STRONG_SCORE else 'fraca'
    return kind, medida, after


def chain(p, state):
    """Folha do vídeo (*_video_normalized) x atlas do jogo, quadro a quadro: (fator, motivo).
    1,00 = a folha do jogo saiu da folha do vídeo sem mudar de tamanho."""
    cfg = p.cfg(state)
    v = cfg.get('video') or {}
    src = ROOT / cfg['source']
    norm = src.with_name(src.name.replace('_sr_hd', '_video_normalized').replace('_sr_2x', '_video_normalized'))
    if norm == src or not norm.exists() or 'largura' not in v:
        return None, 'sem folha do vídeo'
    sheet = np.array(Image.open(norm).convert('RGBA'))[..., 3] > 128
    w = v['largura']
    h = round(cfg.get('frameHeight', 256) / cfg.get('pixelScale', 1))
    cols = sheet.shape[1] // w
    count = cols * (sheet.shape[0] // h)
    if count < cfg['expectedFrames'] or cols != v.get('colunas', cols):
        return None, f'folha do vídeo com {count} quadros, a do jogo com {cfg["expectedFrames"]}: ' \
                     'a receita não reproduz a folha do jogo'
    ratios = []
    fixed = v.get('reguaFatores', [1.0] * cfg['expectedFrames'])
    for i in p.anim(state)['frames']:
        cell = sheet[i // cols * h:(i // cols + 1) * h, i % cols * w:(i % cols + 1) * w]
        ys = np.nonzero(cell.any(1))[0]
        ys2 = np.nonzero((p.atlas_frame(p.anim(state)['atlas'], i)[0][..., 3] > 128).any(1))[0]
        if len(ys) >= 20 and len(ys2) >= 20:
            ratios.append((ys2[-1] - ys2[0] + 1) * fixed[i] / ((ys[-1] - ys[0] + 1) * p.profile['worldScale']))
    if not ratios:
        return None, 'quadros vazios'
    return float(np.median(ratios)), ''


# ---------------------------------------------------------------- veredito

def grade(diff):
    return 'erro' if diff > SIZE_ERROR else 'aviso' if diff > SIZE_WARN else 'ok'


def audit(cid, progress=None):
    p = Pack(cid)
    refs = inicio_images(p)
    head = head_of(refs['inicio_centro'])
    idle_head = float(np.median([z for _, n, z in head_zooms(p, head, 'IDLE') if n >= HEAD_SCORE]))
    seam = seams(p)
    rows = []
    for state, a in p.pack['animations'].items():
        if not a['frames']:
            continue
        if progress:
            progress(state)
        kind, text, after = recipe(p, state)
        ch, ch_note = chain(p, state)
        ini = inicio(p, refs, state)
        cab = cabeca(p, head, state, idle_head)
        if ini['nota'] >= INICIO_SCORE:
            factor, source = ini['fator'], f"imagem inicial no q{ini['quadro']} (nota {ini['nota']:.2f})"
        elif kind == 'forte':
            factor = after * (ch if ch else 1.0)
            source = 'receita forte' + (f' x fator óptico {after:.2f}' if after != 1.0 else '') + \
                     (f' x cadeia {ch:.2f}' if ch and abs(ch - 1) > CHAIN_TOL else '')
        elif cab:
            factor, source = cab['fator'], f"cabeça em {len(cab['por_quadro'])} quadros"
        else:
            factor, source = None, 'sem medida'
        problems = []
        if factor is None:
            problems.append(('aviso', 'tamanho não medido (pose nunca bate e a receita é fraca)'))
        elif grade(abs(factor - 1)) != 'ok':
            problems.append((grade(abs(factor - 1)), f'corpo {factor * 100:.0f}% do tamanho certo'))
        if cab and cab['zoom']:
            problems.append(('aviso', f"tamanho muda dentro do clipe ({cab['min'] * 100:.0f}-{cab['max'] * 100:.0f}% "
                                      'pela cabeça): zoom de câmera no vídeo'))
        if factor is not None and cab and abs(cab['fator'] / factor - 1) > SIZE_ERROR and source != 'receita forte':
            problems.append(('aviso', f"medidas discordam: cabeça dá {cab['fator'] * 100:.0f}%"))
        if ch is None and 'não reproduz' in ch_note:
            problems.append(('aviso', ch_note))
        elif ch and abs(ch - 1) > CHAIN_TOL:
            problems.append((grade(abs(ch - 1)), f'tamanho mudou {ch * 100 - 100:+.0f}% depois da conversão'))
        if after != 1.0:
            problems.append((grade(abs(after - 1)), f'fator óptico {after:.2f} aplicado depois da medida da câmera'))
        mine = [s for s in seam if s['estado'] == state]
        for s in mine:
            if s['mesma_pose']:
                g = grade(abs(s['fator'] - 1))
                if g != 'ok':
                    problems.append((g, f"troca ({s['ponta']}): {s['fator'] * 100:.0f}% do tamanho da {s['ref']}"))
                if abs(s['pe']) > FOOT_WARN:
                    problems.append(('erro' if abs(s['pe']) > FOOT_ERROR else 'aviso',
                                     f"troca ({s['ponta']}): corpo {s['pe']:+d} px fora do lugar da {s['ref']}"))
            elif state not in LOCOMOTION and abs(s['altura'] - 1) > POSE_HEIGHT:
                problems.append(('aviso', f"troca ({s['ponta']}): pose diferente da {s['ref']}, altura "
                                          f"{s['altura'] * 100:.0f}% (salto de pose, não de tamanho)"))
        level = 'erro' if any(g == 'erro' for g, _ in problems) else 'aviso' if problems else 'ok'
        rows.append(dict(estado=state, atlas=a['atlas'], fator=factor, fonte=source, receita=kind,
                         medida=text, fator_optico=after, cadeia=ch, cadeia_nota=ch_note, inicio=ini,
                         cabeca=cab, emendas=mine, problemas=problems, nivel=level))
    return p, rows


# ---------------------------------------------------------------- correção

def correction(p, row, head, idle_head):
    """(fatores por quadro da folha, motivo) que levam o clipe a 100%. Fator óptico aplicado
    depois da medida da câmera é desfeito. Zoom de câmera (a cabeça cresce ou diminui ao longo
    do clipe) vira um fator por quadro: a reta da cabeça pelos quadros, sem passar dos valores
    medidos. Senão, o fator do clipe em todos os quadros."""
    from tamanho import crop, flat
    atlas = row['atlas']
    n = p.clips[atlas]['expectedFrames']
    if row['fator_optico'] != 1.0 and row['fonte'].startswith('receita'):
        return [row['fator_optico']] * n, f"desfaz o fator óptico {row['fator_optico']:.3f}"
    good = []
    for i in range(n):
        nota, z = zoom_search(head, flat(crop(p.atlas_frame(atlas, i)[0], 30)), 0.7, 1.45, 38)
        if nota >= FIX_SCORE:
            good.append((i, z / idle_head, nota))
    if len(good) >= 4:
        idx, zs, w = (np.array(v) for v in zip(*good))
        b, a = np.polyfit(idx, zs, 1, w=w)
        line = np.clip(a + b * np.arange(n), zs.min(), zs.max())
        if line.max() / line.min() - 1 > FIX_ZOOM:
            return [round(float(v), 4) for v in line], \
                f'zoom de câmera: {line[0] * 100:.0f}% no 1º quadro, {line[-1] * 100:.0f}% no último'
    if row['fator'] is None:
        raise SystemExit(f"{row['estado']}: sem medida de tamanho, nada a corrigir")
    return [round(row['fator'], 4)] * n, f"fator {row['fator'] * 100:.0f}% em todos os quadros"


def resolved(cfg):
    """Receita com a célula explícita: o que a receita não declara vem do perfil, como no
    import (import_sprites.prepared-grid)."""
    profile = read_json(ROOT / 'tools/sprites/profiles' / cfg['profile'])
    out = dict(cfg)
    for key, default in (('frameWidth', 'baseFrameWidth'), ('frameHeight', 'baseFrameHeight'),
                         ('rootX', 'preferredRootX'), ('rootY', 'preferredRootY')):
        out.setdefault(key, int(profile[default]))
    return out


def overflow(cfg, z):
    """(esquerda, cima, direita, baixo): px que faltam na célula para os quadros aumentados
    ficarem a FIX_MARGIN px da borda. Tudo 0 = cabe."""
    img = Image.open(ROOT / cfg['source']).convert('RGBA')
    w, h, cols, rx, ry = cfg['frameWidth'], cfg['frameHeight'], cfg['columns'], cfg['rootX'], cfg['rootY']
    need = [0, 0, 0, 0]
    for i, f in enumerate(z):
        box = img.crop((i % cols * w, i // cols * h, i % cols * w + w, i // cols * h + h)).getchannel('A') \
            .point(lambda v: 255 if v > 10 else 0).getbbox()
        if not box or f >= 1:
            continue
        x0, y0, x1, y1 = rx + (box[0] - rx) / f, ry + (box[1] - ry) / f, rx + (box[2] - rx) / f, ry + (box[3] - ry) / f
        for k, v in enumerate((FIX_MARGIN - x0, FIX_MARGIN - y0, x1 - (w - FIX_MARGIN), y1 - (h - FIX_MARGIN))):
            need[k] = max(need[k], v)
    # par: a raiz da folha HD (pixelScale 2) continua num pixel inteiro do tamanho lógico
    return [int(np.ceil(max(0.0, v) / 2)) * 2 for v in need]


def grow_cell(cfg, pad):
    """Aumenta a célula de todos os quadros (borda transparente) e move a raiz junto."""
    left, top, right, bottom = pad
    img = Image.open(ROOT / cfg['source']).convert('RGBA')
    w, h, cols = cfg['frameWidth'], cfg['frameHeight'], cfg['columns']
    n = cfg['expectedFrames']
    rows = -(-n // cols)
    nw, nh = w + left + right, h + top + bottom
    out = Image.new('RGBA', (nw * cols, nh * rows), (0, 0, 0, 0))
    for i in range(n):
        cell = img.crop((i % cols * w, i // cols * h, i % cols * w + w, i // cols * h + h))
        out.paste(cell, (i % cols * nw + left, i // cols * nh + top))
    out.save(ROOT / cfg['source'])
    import auditoria
    auditoria.Sheet.cache.pop(cfg['source'], None)
    cfg.update(frameWidth=nw, frameHeight=nh, rootX=cfg['rootX'] + left, rootY=cfg['rootY'] + top)


def margin_pad(cfg):
    """(esquerda, cima, direita, baixo) que falta na célula para cada quadro ficar a minMargin
    px da borda, medido como o import mede (alfa acima de alphaThreshold, depois da suavização)."""
    validation = read_json(ROOT / 'tools/sprites/profiles' / cfg['profile'])['validation']
    limit, threshold = validation['minMargin'], validation['alphaThreshold']
    img = Image.open(ROOT / cfg['source']).convert('RGBA')
    w, h, cols = cfg['frameWidth'], cfg['frameHeight'], cfg['columns']
    need = [0, 0, 0, 0]
    for i in range(cfg['expectedFrames']):
        box = img.crop((i % cols * w, i // cols * h, i % cols * w + w, i // cols * h + h)).getchannel('A') \
            .point(lambda v: 255 if v > threshold else 0).getbbox()
        if box:
            for k, m in enumerate((box[0], box[1], w - box[2], h - box[3])):
                need[k] = max(need[k], limit - m)
    return [int(np.ceil(max(0, v) / 2)) * 2 for v in need]


def min_opaque(cfg):
    """Pixels visíveis do quadro mais vazio, contados como o import conta."""
    threshold = read_json(ROOT / 'tools/sprites/profiles' / cfg['profile'])['validation']['alphaThreshold']
    img = Image.open(ROOT / cfg['source']).convert('RGBA')
    w, h, cols = cfg['frameWidth'], cfg['frameHeight'], cfg['columns']
    return min(img.crop((i % cols * w, i // cols * h, i % cols * w + w, i // cols * h + h)).getchannel('A')
               .point(lambda v: 255 if v > threshold else 0).histogram()[255] for i in range(cfg['expectedFrames']))


def bake(cfg, atlas, z, reason, undo_optical=False):
    """Reescala a folha do jogo (source da receita) em volta da raiz e grava na receita:
    reescala (a reconversão do vídeo repete), reguaFatores (o que mudou depois da folha do
    vídeo; a cadeia desconta) e o motivo em regua."""
    import reescalar
    from ancorar import clip_path
    path = clip_path(atlas)
    data = read_json(path)
    cfg = resolved(cfg)
    pad = overflow(cfg, z)
    if any(pad):
        grow_cell(cfg, pad)
        for key in ('frameWidth', 'frameHeight', 'rootX', 'rootY'):
            data[key] = cfg[key]
        print(f'{atlas}: célula aumentada para {cfg["frameWidth"]}x{cfg["frameHeight"]} (borda {pad})')
    reescalar.rescale(cfg, np.array(z))
    pad = margin_pad(cfg)  # a suavização da reescala espalha a borda 1-2 px
    if any(pad):
        grow_cell(cfg, pad)
        for key in ('frameWidth', 'frameHeight', 'rootX', 'rootY'):
            data[key] = cfg[key]
        print(f'{atlas}: célula aumentada para {cfg["frameWidth"]}x{cfg["frameHeight"]} (margem {pad})')
    low = min_opaque(cfg)
    validation = read_json(ROOT / 'tools/sprites/profiles' / cfg['profile'])['validation']
    required = int(data.get('minOpaquePixels', validation['minOpaquePixels']))
    if low < required:
        # Figura menor ocupa menos pixels: o mínimo do import acompanha a área (fator²).
        data['minOpaquePixels'] = min(int(required / max(z) ** 2), int(low * 0.95))
    target = data.get('video', data)
    if undo_optical:
        target.pop('normalizedOpticalScale')
    else:
        for key in ('reescala', 'reguaFatores'):
            old = target.get(key, [1.0] * len(z))
            target[key] = [round(a * b, 4) for a, b in zip(old, z)]
    target.setdefault('regua', []).append(reason)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def fix(cid, states):
    """Leva os clipes a 100% pela medida da régua."""
    p, rows = audit(cid)
    refs = inicio_images(p)
    head = head_of(refs['inicio_centro'])
    idle_head = float(np.median([z for _, n, z in head_zooms(p, head, 'IDLE') if n >= HEAD_SCORE]))
    by_state = {r['estado']: r for r in rows}
    done = set()
    for state in states:
        row = by_state[state]
        if row['atlas'] in done:
            continue
        done.add(row['atlas'])
        z, reason = correction(p, row, head, idle_head)
        bake(p.clips[row['atlas']], row['atlas'], z, reason, undo_optical=reason.startswith('desfaz'))
        print(f"{state} ({row['atlas']}): {reason}")
    print('agora: python3 tools/sprites/build_characters.py --write  e rode a régua de novo')


# Folhas do P01 que o renderer desenha com escala fixa própria, sem a calibração gerada.
P01_UNCALIBRATED = {'player_base_crouch', 'player_base_rise'}


def apply_tuning(path):
    """Grava nas folhas os tamanhos ajustados no app (DEBUG > AJUSTAR TAMANHOS > SALVAR NO
    DRIVE). fator 1.05 = folha 5% maior; na receita fica como reescala 1/fator."""
    data = read_json(path)
    if data.get('tipo') != 'ajuste-tamanho':
        raise SystemExit(f'{path}: não é um arquivo de ajuste de tamanho do app')
    clips = {}
    for c in (ROOT / 'tools/sprites/clips').glob('*.json'):
        cfg = read_json(c)
        clips[Path(cfg['output']).stem] = cfg
    for item in data['ajustes']:
        atlas, f = item['folha'], float(item['fator'])
        if atlas not in clips:
            raise SystemExit(f'{atlas}: folha sem receita em tools/sprites/clips')
        if not 0.7 <= f <= 1.4:
            raise SystemExit(f'{atlas}: fator {f} fora de 0,70-1,40')
        if abs(f - 1) < 0.005:
            continue
        cfg = clips[atlas]
        reason = f"ajuste no app {data.get('data', '')}: {f * 100:.0f}% ({' / '.join(item.get('animacoes', []))})"
        if atlas.startswith('player_base_') and atlas not in P01_UNCALIBRATED:
            # A calibração do P01 (generate_p01_calibration.py) normaliza a folha pela altura
            # do idle e desfaria a folha reescalada: o fator vai para a receita e ela multiplica.
            from ancorar import clip_path
            path = clip_path(atlas)
            clip = read_json(path)
            clip['ajusteTamanho'] = round(clip.get('ajusteTamanho', 1.0) * f, 4)
            clip.setdefault('regua', []).append(reason)
            path.write_text(json.dumps(clip, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
        else:
            bake(cfg, atlas, [1 / f] * cfg['expectedFrames'], reason)
        print(f"{item.get('personagem', '?')} {atlas}: {f * 100:.0f}%")
    print('agora: python3 tools/sprites/build_characters.py --write  (o APK novo nasce com esses tamanhos;'
          ' no app o ajuste volta a 100% sozinho)')


# ---------------------------------------------------------------- saída

def pct(v):
    return '-' if v is None else f'{v * 100:.0f}%'


def table(rows):
    lines = [f"{'':<5} {'clipe':<16} {'fator':>5}  {'medido por':<44} {'cabeça':>7}  trocas"]
    for r in rows:
        ends = ' '.join(f"{s['ponta']}:{pct(s['fator'])}/{s['pe']:+d}px" if s['mesma_pose']
                        else f"{s['ponta']}:pose({pct(s['altura'])})" for s in r['emendas'])
        cab = pct(r['cabeca']['fator']) if r['cabeca'] else '-'
        lines.append(f"{r['nivel']:<5} {r['estado']:<16} {pct(r['fator']):>5}  {r['fonte'][:44]:<44} {cab:>7}  {ends}")
    return '\n'.join(lines)


def tint(rgba, color, alpha):
    out = np.zeros_like(rgba)
    out[rgba[..., 3] > 128] = (*color, alpha)
    return Image.fromarray(out)


def seam_sheet(p, rows, path):
    """Folha das trocas: referência (ciano) e a ponta do clipe (laranja) com a raiz no mesmo
    ponto, como na troca de animação. O que sobra de uma cor é diferença de tamanho/lugar/pose."""
    idle = p.world('IDLE', 0)
    crouch = p.world('CROUCH', p.count('CROUCH') - 1)
    items = [(r, s) for r in rows for s in r['emendas']]
    thumb, cols, text = 200, 7, 44
    sheet = Image.new('RGB', (cols * thumb, -(-len(items) // cols) * (thumb + text)), (24, 24, 28))
    draw = ImageDraw.Draw(sheet)
    for n, (r, s) in enumerate(items):
        ref = idle if s['ref'] == 'guarda' else crouch
        tile = Image.new('RGBA', (CANVAS, CANVAS), (24, 24, 28, 255))
        for (rgba, rx, ry), color in ((ref, (40, 200, 230)), (p.world(r['estado'], s['quadro']), (255, 140, 40))):
            tile.alpha_composite(tint(rgba, color, 140), (round(CX - rx), round(CY - ry)))
        tile = tile.crop((CX - 190, CY - 330, CX + 190, CY + 20)).convert('RGB')
        tile.thumbnail((thumb, thumb))
        x, y = n % cols * thumb, n // cols * (thumb + text)
        sheet.paste(tile, (x, y))
        if s['mesma_pose']:
            g = 'erro' if abs(s['pe']) > FOOT_ERROR else grade(abs(s['fator'] - 1))
            line = f"{s['fator'] * 100:.0f}%  corpo {s['pe']:+d}px"
        else:
            g = 'ok' if r['estado'] in LOCOMOTION or abs(s['altura'] - 1) <= POSE_HEIGHT else 'aviso'
            line = f"outra pose, altura {s['altura'] * 100:.0f}%"
        color = {'ok': (120, 220, 120), 'aviso': (240, 200, 60), 'erro': (250, 90, 80)}[g]
        draw.text((x + 4, y + thumb + 4), f"{r['estado']} {s['ponta']}", fill=(230, 230, 230))
        draw.text((x + 4, y + thumb + 20), line, fill=color)
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)


def markdown(p, rows, png):
    order = {'erro': 0, 'aviso': 1, 'ok': 2}
    out = [f'# Régua de tamanho — {p.id}', '',
           f'Gerado por `python3 tools/sprites/regua.py {p.id} --relatorio` (explicação no topo do script). '
           f'Referência: `art/keys/{p.id}/inicio_*.png` no tamanho oficial (`tamanho.json`). '
           '**Fator** = tamanho do corpo no jogo ÷ tamanho da referência: 100% = certo, '
           f'aviso fora de ±{SIZE_WARN:.0%}, erro fora de ±{SIZE_ERROR:.0%}. **Cabeça** é a medida pela '
           'cabeça (não depende da pose), mostrada para conferir.', '',
           '| | Clipe | Fator | Medido por | Cabeça | Receita do vídeo | Problemas |',
           '|---|---|---|---|---|---|---|']
    for r in sorted(rows, key=lambda r: (order[r['nivel']], r['estado'])):
        icon = {'erro': '🔴', 'aviso': '🟡', 'ok': '🟢'}[r['nivel']]
        cab = r['cabeca']
        cab = '-' if not cab else pct(cab['fator']) + (f" ({pct(cab['min'])}–{pct(cab['max'])})"
                                                        if cab['zoom'] else '')
        probs = '<br>'.join(t for _, t in r['problemas']) or '—'
        out.append(f"| {icon} | {r['estado']} | {pct(r['fator'])} | {r['fonte']} | {cab} | "
                   f"{r['receita']}: {r['medida'].replace('|', '/')} | {probs} |")
    out += ['', f'Trocas de animação (ciano = IDLE q0 ou agachado, laranja = ponta do clipe, raiz no '
                f'mesmo ponto): [{png.name}]({png.name})', '', f'![trocas]({png.name})', '']
    return '\n'.join(out)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('personagens', nargs='*')
    ap.add_argument('--relatorio', action='store_true', help='grava docs/personagens/regua/<id>.md, .json e .png')
    ap.add_argument('--corrigir', nargs='+', metavar='ESTADO', help='leva esses clipes do personagem a 100%%')
    ap.add_argument('--aplicar', metavar='ARQUIVO', help='grava nas folhas o arquivo salvo pelo app (AJUSTAR TAMANHOS)')
    args = ap.parse_args()
    if args.aplicar:
        apply_tuning(args.aplicar)
        return
    if not args.personagens:
        ap.error('diga o personagem (ex.: p02) ou use --aplicar ARQUIVO')
    if args.corrigir:
        if len(args.personagens) != 1:
            ap.error('--corrigir é para um personagem por vez')
        fix(args.personagens[0], args.corrigir)
        return
    for cid in args.personagens:
        p, rows = audit(cid)
        print(f'== {cid}')
        print(table(rows))
        if args.relatorio:
            folder = ROOT / 'docs/personagens/regua'
            png = folder / f'{cid}-trocas.png'
            seam_sheet(p, rows, png)
            (folder / f'{cid}.md').write_text(markdown(p, rows, png), encoding='utf-8')
            (folder / f'{cid}.json').write_text(
                json.dumps(rows, indent=1, ensure_ascii=False, default=lambda o: round(float(o), 4)) + '\n',
                encoding='utf-8')
            print(f'relatório: {(folder / f"{cid}.md").relative_to(ROOT)}')


if __name__ == '__main__':
    main()
