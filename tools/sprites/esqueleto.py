#!/usr/bin/env python3
"""Tamanho pelo esqueleto: cada quadro na escala da imagem inicial oficial, em qualquer pose.

    python3 tools/sprites/esqueleto.py p02 IDLE DASH INTRO CROUCH            # só mede
    python3 tools/sprites/esqueleto.py p02 IDLE DASH INTRO CROUCH --corrigir # grava nas folhas
                                                       # (depois: build_characters.py --write)

A régua de silhueta (regua.py) erra quando a pose muda: guarda larga, corrida e agachado
mudam a silhueta sem mudar o corpo, e o mesmo vídeo pode ter a personagem maior correndo
do que parada. Aqui o corpo é medido pelos ossos: o MediaPipe acha o esqueleto do quadro
(como o jogo desenha) e estima o mesmo esqueleto em 3D. Para cada osso, px na imagem ÷
comprimento do osso no plano da imagem do 3D = px por metro; a mediana dos ossos é a escala
do quadro. O osso virado para a câmera encurta nas duas medidas, então o agachado não
parece menor. A referência é a imagem inicial (art/keys/<id>/inicio_centro.png) no tamanho
oficial do tamanho.json, a mesma de regua.py.

Fator 100% = certo. O ruído é de uns 3% por quadro (no IDLE, 96-104%), por isso:
loop (IDLE, corrida) e clipe curto recebem um fator só, a mediana; clipe longo recebe a
mediana móvel de 7 quadros (zoom de câmera no meio do vídeo). Clipe dentro de ±2% não muda.
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
WINDOW = 7          # mediana móvel nos clipes longos
LONG = 16           # a partir de quantos quadros um clipe (não loop) acompanha o zoom


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


def factors(values, loop):
    """Fator a aplicar em cada quadro (o que a reescala divide): suavizado contra o ruído."""
    known = [v for v in values if v is not None]
    if not known:
        return None
    if loop or len(values) < LONG:
        return [float(np.median(known))] * len(values)
    idx = np.arange(len(values))
    ok = np.array([v is not None for v in values])
    filled = np.interp(idx, idx[ok], np.array([v for v in values if v is not None]))
    half = WINDOW // 2
    return [float(np.median(filled[max(0, i - half):i + half + 1])) for i in idx]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('personagem')
    ap.add_argument('estados', nargs='+')
    ap.add_argument('--corrigir', action='store_true', help='grava o tamanho nas folhas do jogo')
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
        if not args.corrigir:
            continue
        if all(abs(f - 1) <= TOLERANCE for f in z):
            print('  dentro de ±2%: fica como está')
            continue
        reason = (f'esqueleto: {np.median(used) * 100:.0f}% da imagem inicial '
                  f'({"mediana" if len(set(z)) == 1 else "mediana móvel"}), levado a 100%')
        regua.bake(p.clips[atlas], atlas, z, reason)
        print('  gravado')
    if args.corrigir:
        print('agora: python3 tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
