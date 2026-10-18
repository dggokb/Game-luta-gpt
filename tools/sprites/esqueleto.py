#!/usr/bin/env python3
"""Tamanho pelo esqueleto: cada quadro na escala da imagem inicial oficial, em qualquer pose.

    python3 tools/sprites/esqueleto.py p02 IDLE DASH INTRO CROUCH            # só mede
    python3 tools/sprites/esqueleto.py p02 IDLE DASH INTRO CROUCH --corrigir # grava nas folhas
    python3 tools/sprites/esqueleto.py p02 INTRO --chao --corrigir           # + pé no chão
                                                       # (depois: build_characters.py --write)

A régua de silhueta (regua.py) erra quando a pose muda: guarda larga, corrida e agachado
mudam a silhueta sem mudar o corpo, e o mesmo vídeo pode ter a personagem maior correndo
do que parada. Aqui o corpo é medido pelos ossos: o MediaPipe acha o esqueleto do quadro
(como o jogo desenha) e estima o mesmo esqueleto em 3D. Para cada osso, px na imagem ÷
comprimento do osso no plano da imagem do 3D = px por metro; a mediana dos ossos é a escala
do quadro. O osso virado para a câmera encurta nas duas medidas, então o agachado não
parece menor. A referência é a imagem inicial (art/keys/<id>/inicio_centro.png) no tamanho
oficial do tamanho.json, a mesma de regua.py.

Fator 100% = certo. Todo clipe segue a mesma regra, sem escolha à mão: cada quadro vai ao
tamanho oficial, pela curva da medida suavizada (mediana de 7 quadros, média de 9, no máximo
1% de mudança por quadro), porque a medida tem uns 3% de ruído por quadro (no IDLE, 96-104%)
e o próprio vídeo muda o tamanho do corpo entre as fases (a corrida do dash.mp4 ~12% maior
que a guarda; a p02 em pé no intro.mp4 ~6% maior que na base). Curva com menos de 3% de
variação vira um fator só; clipe que já está a ±2% do certo não muda.
A correção reescala a folha do jogo em volta da raiz e grava na receita, como regua.py
--corrigir (reescala, reguaFatores e o motivo em regua).

Precisa de mediapipe (pip install -r tools/sprites/requirements-animar.txt); no Linux, da
libEGL (apt install libegl1). O modelo (9 MB) é baixado em android/app/build/esqueleto.
"""
import argparse
import sys
import urllib.request
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
import regua  # noqa: E402

MODEL_URL = ('https://storage.googleapis.com/mediapipe-models/pose_landmarker/'
             'pose_landmarker_full/float16/latest/pose_landmarker_full.task')
MODEL = ROOT / 'android/app/build/esqueleto/pose_landmarker_full.task'
UP = 3.0            # o mundo tem ~200 px de altura: amplia para o detector
MIN_BONE = 0.08     # m: osso quase apontado para a câmera não entra na medida
MIN_VIS = 0.5
BONES = ((11, 12), (23, 24), (11, 23), (12, 24), (11, 13), (12, 14), (13, 15), (14, 16),
         (23, 25), (24, 26), (25, 27), (26, 28))
TOLERANCE = 0.02    # clipe a ±2% do certo fica como está
RATE = 0.01         # o tamanho muda no máximo 1% por quadro (nunca pulsa)
FLAT = 0.03         # curva com menos variação que isso no clipe: um fator só


def detector():
    from mediapipe.tasks import python as mpt
    from mediapipe.tasks.python import vision
    if not MODEL.exists():
        MODEL.parent.mkdir(parents=True, exist_ok=True)
        urllib.request.urlretrieve(MODEL_URL, MODEL)
    return vision.PoseLandmarker.create_from_options(vision.PoseLandmarkerOptions(
        base_options=mpt.BaseOptions(model_asset_path=str(MODEL)), running_mode=vision.RunningMode.IMAGE))


def scale(det, rgba):
    """px do mundo por metro do esqueleto, ou None se não achou o corpo."""
    import cv2
    import mediapipe as mp
    h, w = rgba.shape[:2]
    canvas = np.zeros((int(h * UP) + 200, int(w * UP) + 200, 3), np.uint8)
    canvas[:] = (0, 200, 0)
    big = cv2.resize(rgba, (int(w * UP), int(h * UP)), interpolation=cv2.INTER_CUBIC)
    a = big[..., 3:4] / 255.0
    area = canvas[100:100 + big.shape[0], 100:100 + big.shape[1]]
    area[:] = (big[..., :3] * a + area * (1 - a)).astype(np.uint8)
    found = det.detect(mp.Image(image_format=mp.ImageFormat.SRGB, data=canvas))
    if not found.pose_landmarks:
        return None
    lm, world = found.pose_landmarks[0], found.pose_world_landmarks[0]
    px = lambda k: np.array([lm[k].x * canvas.shape[1], lm[k].y * canvas.shape[0]]) / UP
    values = []
    for a_, b_ in BONES:
        plane = np.hypot(world[a_].x - world[b_].x, world[a_].y - world[b_].y)
        if plane < MIN_BONE or min(lm[a_].visibility, lm[b_].visibility) < MIN_VIS:
            continue
        values.append(np.linalg.norm(px(a_) - px(b_)) / plane)
    return float(np.median(values)) if len(values) >= 3 else None


def reference(det, p):
    refs = regua.inicio_images(p)
    s = scale(det, refs['inicio_centro'])
    if s is None:
        raise SystemExit(f'{p.id}: esqueleto não achado na imagem inicial')
    return s


def measure(det, p, atlas, ref):
    """Fator de cada quadro da folha (1,00 = tamanho oficial), None onde não achou."""
    n = p.clips[atlas]['expectedFrames']
    out = []
    for i in range(n):
        s = scale(det, p.atlas_frame(atlas, i)[0])
        out.append(None if s is None else s / ref)
    return out


def smooth(values, loop):
    """Mediana de 7 quadros, média de 9 e no máximo 1% de mudança por quadro (ida e volta):
    acompanha o tamanho mudando entre as fases do vídeo sem passar o ruído da medida."""
    n = len(values)
    idx = np.arange(n)
    ok = np.array([v is not None for v in values])
    raw = np.interp(idx, idx[ok], np.array([v for v in values if v is not None]),
                    period=n if loop else None)
    pad = lambda a, k: np.concatenate([a[-k:], a, a[:k]]) if loop else np.pad(a, k, mode='edge')
    med = np.array([np.median(pad(raw, 3)[i:i + 7]) for i in range(n)])
    avg = np.convolve(pad(med, 4), np.ones(9) / 9, mode='valid')
    out = avg.copy()
    for _ in range(2):   # ida e volta: o limite vale para os dois lados
        for i in range(1, n):
            out[i] = np.clip(out[i], out[i - 1] - RATE, out[i - 1] + RATE)
        for i in range(n - 2, -1, -1):
            out[i] = np.clip(out[i], out[i + 1] - RATE, out[i + 1] + RATE)
    return out


def factors(values, loop):
    """Fator a aplicar em cada quadro (o que a reescala divide). Todo clipe segue a mesma
    regra: cada quadro vai ao tamanho oficial, com a curva suavizada; se a curva varia menos
    que FLAT no clipe, um fator só (a mediana)."""
    known = [v for v in values if v is not None]
    if not known:
        return None
    if len(known) < 3:
        return [float(np.median(known))] * len(values)
    curve = smooth(values, loop)
    if curve.max() - curve.min() < FLAT:
        return [float(np.median(known))] * len(values)
    return [float(v) for v in curve]


SINK = 6  # px da folha (3 do jogo): pé abaixo do chão a partir disso é corrigido


def ground(cfg, atlas, show=False):
    """Pé abaixo do chão (o quadro afunda e, na troca para o IDLE, ela pula para cima): sobe
    cada quadro até o pé encostar na raiz, como nos outros clipes. Devolve o quanto subiu cada
    quadro (px da folha)."""
    cfg = regua.resolved(cfg)
    img = Image.open(ROOT / cfg['source']).convert('RGBA')
    w, h, cols, ry = cfg['frameWidth'], cfg['frameHeight'], cfg['columns'], cfg['rootY']
    lifts = []
    for i in range(cfg['expectedFrames']):
        box = (i % cols * w, i // cols * h, i % cols * w + w, i // cols * h + h)
        cell = img.crop(box)
        rows = np.nonzero((np.array(cell)[..., 3] > 128).any(1))[0]
        sink = int(rows[-1]) - ry if len(rows) else 0
        lift = sink if sink >= SINK else 0
        lifts.append(lift)
        if lift and not show:
            moved = Image.new('RGBA', (w, h))
            moved.paste(cell, (0, -lift))
            img.paste(moved, box[:2])
    if not show and any(lifts):
        img.save(ROOT / cfg['source'])
    return lifts


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('personagem')
    ap.add_argument('estados', nargs='+')
    ap.add_argument('--corrigir', action='store_true', help='grava o tamanho nas folhas do jogo')
    ap.add_argument('--chao', action='store_true',
                    help='também sobe os quadros com o pé abaixo do chão (grava com --corrigir)')
    args = ap.parse_args()
    p = regua.Pack(args.personagem)
    det = detector()
    ref = reference(det, p)
    done = set()
    for state in args.estados:
        anim = p.anim(state)
        atlas = anim['atlas']
        if atlas in done:
            continue
        done.add(atlas)
        loop = any(a['loop'] for a in p.pack['animations'].values() if a['atlas'] == atlas)
        values = measure(det, p, atlas, ref)
        z = factors(values, loop)
        if z is None:
            print(f'{state} ({atlas}): esqueleto não achado')
            continue
        used = [values[i] for i in anim['frames'] if values[i] is not None]
        print(f"{state} ({atlas}): {np.median(used):.0%} (quadros {min(used):.0%}-{max(used):.0%}, "
              f"{len(used)}/{len(anim['frames'])} medidos); aplica {min(z):.0%}-{max(z):.0%}")
        if args.chao:
            lifts = ground(p.clips[atlas], atlas, show=not args.corrigir)
            if any(lifts):
                print('  pé abaixo do chão (px da folha): ' + ' '.join(str(v) for v in lifts)
                      + ('  -> subiu' if args.corrigir else ''))
                if args.corrigir:
                    from ancorar import clip_path
                    import json
                    path = clip_path(atlas)
                    data = json.loads(path.read_text(encoding='utf-8'))
                    (data.get('video') or data).setdefault('regua', []).append(
                        'esqueleto --chao: pé no chão, quadros subidos ' + ','.join(str(v) for v in lifts))
                    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
        if not args.corrigir:
            continue
        if all(abs(f - 1) <= TOLERANCE for f in z):
            print('  dentro de ±2%: fica como está')
            continue
        reason = (f'esqueleto: {np.median(used) * 100:.0f}% da imagem inicial '
                  f'({"mediana" if len(set(z)) == 1 else "curva suavizada, quadro a quadro"}), levado a 100%')
        regua.bake(p.clips[atlas], atlas, z, reason)
        print('  gravado')
    if args.corrigir:
        print('agora: python3 tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
