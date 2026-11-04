#!/usr/bin/env python3
"""Exporta as duas artes aprovadas da Arena Central para o motor do jogo.

As artes nao sao espelhadas nem repetidas. A segunda arte constitui a lateral
esquerda, a primeira a direita; as bordas que mostram o camarote central se
encontram numa transicao curta. O piso e reprojetado para a camera real do motor.

Uso: python3 tools/stage/arena_central_final.py [--preview /tmp/arena.png]
"""

import argparse
from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "art/stages/arena_central"
DEST = ROOT / "android/app/src/main/assets/stages/arena_central"
LEFT = ART / "panorama_esquerda.webp"
RIGHT = ART / "panorama_direita.webp"
OVERLAP = 180
BASE = 0.555
HORIZON = 0.47
VW, VH = 1280, 720
CAMERA_DISTANCE = 1000.0
REST_ZOOM = 1.24
GROUND_Y = 565.0
SCREEN_GROUND_Y = 552.0
REST_CENTER_X = 700.0
FLOOR_NEAR = -700.0
FLOOR_FAR = 2008.0


def panorama(left: Image.Image, right: Image.Image) -> Image.Image:
    if left.size != right.size or left.size != (1905, 825):
        raise ValueError("esperadas as duas artes aprovadas de 1905x825")
    a = np.asarray(left.convert("RGB"), dtype=np.uint8)
    b = np.asarray(right.convert("RGB"), dtype=np.uint8)
    h, w, _ = a.shape
    out = np.empty((h, 2 * w - OVERLAP, 3), dtype=np.uint8)
    out[:, :w - OVERLAP] = a[:, :w - OVERLAP]
    out[:, w:] = b[:, OVERLAP:]
    t = np.linspace(0, 1, OVERLAP, dtype=np.float32)[None, :, None]
    out[:, w - OVERLAP:w] = np.round(
        a[:, w - OVERLAP:].astype(np.float32) * (1 - t)
        + b[:, :OVERLAP].astype(np.float32) * t
    ).astype(np.uint8)
    return Image.fromarray(out, "RGB")


def layers(pan: Image.Image) -> None:
    limit = round(pan.height * BASE)
    background = pan.crop((0, 0, pan.width, limit))
    background.save(DEST / "fundo_panorama.webp", "WEBP", quality=94, method=6)

    # Mesmo desenho na camera de repouso. A mascara deixa o ceu/montanhas
    # mais distantes e prende as arquibancadas ao plano do limite do piso.
    rgba = np.asarray(background.convert("RGBA")).copy()
    y = np.arange(limit, dtype=np.float32)
    alpha = np.clip((y - 180) / 85, 0, 1)
    rgba[:, :, 3] = np.round(alpha[:, None] * 255).astype(np.uint8)
    Image.fromarray(rgba, "RGBA").save(
        DEST / "arquibancadas_panorama.webp", "WEBP", quality=96, method=6
    )


def floor_texture(pan: Image.Image) -> None:
    # Coordenadas do StageCamera em repouso; sem espelhar bordas nem emblemas.
    pan = pan.resize((round(pan.width * VH / pan.height), VH), Image.Resampling.LANCZOS)
    src = np.asarray(pan, dtype=np.uint8)
    panorama_center = src.shape[1] / 2
    horizon = HORIZON * VH
    rest_distance = CAMERA_DISTANCE / REST_ZOOM
    eye_height = (SCREEN_GROUND_Y - horizon) / REST_ZOOM
    rows, columns = 1354, 3500
    floor_width = (FLOOR_FAR - FLOOR_NEAR) * columns / rows
    z = FLOOR_FAR - (np.arange(rows, dtype=np.float32) + 0.5) / rows * (FLOOR_FAR - FLOOR_NEAR)
    x = REST_CENTER_X + ((np.arange(columns, dtype=np.float32) + 0.5) / columns - 0.5) * floor_width
    scale = CAMERA_DISTANCE / (rest_distance + z)
    sx = panorama_center + (x[None, :] - REST_CENTER_X) * scale[:, None]
    sy = horizon + eye_height * scale
    # Fora do panorama original o motor usa uma textura de lajotas neutras.
    sx = np.clip(sx, 0, src.shape[1] - 1)
    sy = np.clip(sy, BASE * VH, VH - 1)
    x0 = np.floor(sx).astype(np.int32)
    y0 = np.floor(sy).astype(np.int32)[:, None]
    x1 = np.minimum(x0 + 1, src.shape[1] - 1)
    y1 = np.minimum(y0 + 1, VH - 1)
    fx = (sx - x0)[..., None]
    fy = (sy[:, None] - y0)[..., None]
    pixels = (src[y0, x0] * (1 - fx) * (1 - fy)
              + src[y0, x1] * fx * (1 - fy)
              + src[y1, x0] * (1 - fx) * fy
              + src[y1, x1] * fx * fy)
    Image.fromarray(np.clip(pixels, 0, 255).astype(np.uint8), "RGB").save(
        DEST / "piso_panorama.webp", "WEBP", quality=94, method=6
    )
    # Amostra de pedras sem o grande simbolo dourado, usada fora da textura central.
    pan.crop((200, 575, 800, 715)).save(
        DEST / "piso_lajotas.webp", "WEBP", quality=90, method=6
    )


def render_rest_preview(pan: Image.Image, target: Path) -> float:
    """Previa da tela de repouso usando a mesma projecao do piso no StageScene."""
    pan = pan.resize((round(pan.width * VH / pan.height), VH), Image.Resampling.LANCZOS)
    src = np.asarray(pan, dtype=np.uint8)
    center = src.shape[1] // 2
    expected = src[:, center - VW // 2:center + VW // 2]
    with Image.open(DEST / "piso_panorama.webp") as floor:
        tex = np.asarray(floor.convert("RGB"), dtype=np.uint8)
    frame = expected.copy()
    horizon = HORIZON * VH
    eye_height = (SCREEN_GROUND_Y - horizon) / REST_ZOOM
    distance = CAMERA_DISTANCE / REST_ZOOM
    floor_width = (FLOOR_FAR - FLOOR_NEAR) * tex.shape[1] / tex.shape[0]
    ys = np.arange(round(BASE * VH) + 2, VH, dtype=np.float32) + 0.5
    z = CAMERA_DISTANCE * eye_height / (ys - horizon) - distance
    scale = CAMERA_DISTANCE / (distance + z)
    worldx = REST_CENTER_X + (np.arange(VW, dtype=np.float32)[None, :] - VW / 2) / scale[:, None]
    tx = np.clip(((worldx - REST_CENTER_X) / floor_width + 0.5) * tex.shape[1], 0, tex.shape[1] - 1)
    ty = np.clip((FLOOR_FAR - z) / (FLOOR_FAR - FLOOR_NEAR) * tex.shape[0], 0, tex.shape[0] - 1)
    floor_pixels = tex[np.floor(ty).astype(int)[:, None], np.floor(tx).astype(int)]
    frame[round(BASE * VH) + 2:] = floor_pixels
    Image.fromarray(frame, "RGB").save(target)
    return float(np.abs(frame[420:].astype(np.int16) - expected[420:].astype(np.int16)).mean())


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", type=Path)
    parser.add_argument("--rest-preview", type=Path)
    args = parser.parse_args()
    DEST.mkdir(parents=True, exist_ok=True)
    with Image.open(LEFT) as l, Image.open(RIGHT) as r:
        pan = panorama(l, r)
    layers(pan)
    floor_texture(pan)
    if args.preview:
        pan.save(args.preview)
    if args.rest_preview:
        difference = render_rest_preview(pan, args.rest_preview)
        print(f"Camera de repouso: erro medio RGB {difference:.2f}/255 contra a pintura")
    print(f"Arena Central: {pan.width}x{pan.height}, quatro assets gerados sem repeticao/espelho")


if __name__ == "__main__":
    main()
