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
from PIL import Image, ImageFilter


ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "art/stages/arena_central"
DEST = ROOT / "android/app/src/main/assets/stages/arena_central"
LEFT = ART / "panorama_esquerda.webp"
RIGHT = ART / "panorama_direita.webp"
OVERLAP = 180
BASE = 0.555
HORIZON = 0.47
SKY_OVERSCAN = 0.18
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
    source = np.asarray(background.convert("RGB"), dtype=np.uint8)
    # A pintura arquitetonica e UMA estrutura. Separar a mesma imagem inteira
    # em dois planos criou um segundo camarote no pulo e fendas nas torres.
    # Ate termos mascara real dos elementos arquitetonicos, ela fica num plano
    # unico. O chao continua independente com perspectiva/parallax de verdade.
    overscan = round(pan.height * SKY_OVERSCAN)
    # Extensao acima da tela contra a faixa roxa chapada nos saltos. Refletir
    # apenas as linhas do alto (nunca o edificio), suavizando o trecho que so
    # aparece fora da camera de repouso; nao esticar uma linha em listras.
    reflected = np.flip(source[:overscan], axis=0).copy()
    soft = np.asarray(Image.fromarray(reflected).filter(
        ImageFilter.GaussianBlur(radius=12)), dtype=np.float32)
    t = np.linspace(0, 1, overscan, dtype=np.float32)[:, None, None]
    extra = np.clip(soft * (1 - t) + reflected * t, 0, 255).astype(np.uint8)
    whole = Image.fromarray(np.concatenate([extra, source], axis=0), "RGB")
    whole.save(DEST / "fundo_panorama.webp", "WEBP", quality=95, method=6)


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
    raw_sx = panorama_center + (x[None, :] - REST_CENTER_X) * scale[:, None]
    raw_sy = horizon + eye_height * scale
    # Fora do panorama NAO esticar a coluna final de pixels: ela virava uma
    # mancha enorme quando a camera girava/abria o campo de visao.
    outside_x = np.maximum(np.maximum(-raw_sx, raw_sx - (src.shape[1] - 1)), 0)
    outside_y = np.maximum(raw_sy - (VH - 1), 0)[:, None]
    outside = np.maximum(outside_x, outside_y)
    sx = np.clip(raw_sx, 0, src.shape[1] - 1)
    sy = np.clip(raw_sy, BASE * VH, VH - 1)
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
    # A faixa neutra nunca altera o que se ve na camera de repouso. Os texels
    # de fora recebem pedra ortografica, e a transicao evita uma costura dura.
    with Image.open(ART / "piso_neutro_fonte.webp") as neutral:
        neutral = neutral.convert("RGB").resize((columns, rows), Image.Resampling.LANCZOS)
    stones = np.asarray(neutral, dtype=np.float32)
    blend = np.clip(outside / 70.0, 0, 1)[..., None]
    pixels = pixels * (1 - blend) + stones * blend
    Image.fromarray(np.clip(pixels, 0, 255).astype(np.uint8), "RGB").save(
        DEST / "piso_panorama.webp", "WEBP", quality=94, method=6
    )
    # Nao ampliar um recorte de 600x140 a dimensao de um campo inteiro: isso
    # causava faixas desfocadas quando o zoom/pulo revelava lajotas fora do centro.
    # A textura ortografica nao contem o emblema e tem resolucao equivalente
    # a imagem principal do piso, evitando diferenca grotesca entre as copias.
    neutral.save(DEST / "piso_lajotas.webp", "WEBP", quality=94, method=6)


def render_rest_preview(pan: Image.Image, target: Path) -> float:
    """Previa da tela de repouso usando a mesma projecao do piso no StageScene."""
    pan = pan.resize((round(pan.width * VH / pan.height), VH), Image.Resampling.LANCZOS)
    src = np.asarray(pan, dtype=np.uint8)
    center = src.shape[1] // 2
    expected = src[:, center - VW // 2:center + VW // 2]
    with Image.open(DEST / "piso_panorama.webp") as floor:
        tex = np.asarray(floor.convert("RGB"), dtype=np.uint8)
    frame = expected.copy()
    display_width = src.shape[1]
    with Image.open(DEST / "fundo_panorama.webp") as sky_source:
        sky = sky_source.convert("RGB").resize(
            (display_width, round((BASE + SKY_OVERSCAN) * VH)), Image.Resampling.LANCZOS
        )
    sky_y = round(SKY_OVERSCAN * VH)
    left = center - VW // 2
    far = sky.crop((left, sky_y, left + VW, sky_y + round(BASE * VH))).convert("RGBA")
    foreground = far.convert("RGB")
    frame[:foreground.height] = np.asarray(foreground)
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
    return float(np.abs(frame.astype(np.int16) - expected.astype(np.int16)).mean())


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
    print(f"Arena Central: {pan.width}x{pan.height}, fundo unico + piso, sem repeticao/espelho")


if __name__ == "__main__":
    main()
