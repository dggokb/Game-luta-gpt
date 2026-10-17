#!/usr/bin/env python3
"""Arte leve da tela de seleção (android/app/src/main/assets/selection/).

A tela de seleção desenhava recortes de imagens grandes:
  * o retrato em destaque vinha de selection/art/hero_<id>.png (1000-1500 px) decodificado
    pela metade (o p05, de um quarto de heroes_p02_p05.png decodificado inteiro);
  * cada rostinho da grade vinha da folha de conceito inteira (art/concepts/<id>.png,
    ~1450x1090) para usar só um pedaço.

Este script grava exatamente o que a tela usava, já pronto, em WebP sem perda:
  * selection/art/tela/hero_<id>.webp: a metade do hero (média de cada 2x2, em alfa
    pré-multiplicado; o p05 é o quarto do atlas, no tamanho em que já era usado);
  * selection/rostos/<id>.webp: o recorte de cada rosto, com a mesma conta de arredondamento
    do Java (Math.round em float).

Os hero_*.png originais continuam: o Ultra sem corte próprio usa o hero inteiro na abertura.

    python3 tools/selection/gerar_selecao.py          # grava
    python3 tools/selection/gerar_selecao.py --check  # só confere (falha se estiver velho)
"""
import argparse
import io
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'android/app/src/main/assets/selection'
CONCEPTS = ROOT / 'art/concepts'
# ids da arte (player_base é p01).
HEROES = ('p01', 'p02', 'p03', 'p04', 'p05', 'p06', 'p07', 'p08')
FACES = tuple(f'p{n:02d}' for n in range(1, 17))
# Região do rosto em frações da folha de conceito (l, t, r, b); sem entrada: o padrão.
FACE_DEFAULT = (.805, .027, .991, .312)
FACE_ROI = {
    'p02': (.79, .012, .992, .252), 'p03': (.796, .014, .992, .311), 'p04': (.809, .023, .991, .323),
    'p05': (.007, .67, .202, .985), 'p06': (.552, .601, .768, .992), 'p07': (.011, .668, .231, .982),
    'p08': (.008, .594, .208, .985), 'p09': (.012, .751, .242, .99), 'p10': (.009, .709, .254, .974),
    'p11': (.601, .386, .791, .688), 'p12': (.807, .014, .991, .323), 'p13': (.008, .632, .275, .988),
    'p14': (.009, .631, .275, .99), 'p15': (.625, .472, .803, .769), 'p16': (.812, .025, .992, .326),
}


def java_round(value):
    """Math.round(float) do Java: floor(x + 0.5) em float32."""
    return int(np.floor(np.float32(value) + np.float32(0.5)))


def face_box(face_id, width, height):
    l, t, r, b = (np.float32(v) for v in FACE_ROI.get(face_id, FACE_DEFAULT))
    x, y = java_round(l * np.float32(width)), java_round(t * np.float32(height))
    w, h = java_round((r - l) * np.float32(width)), java_round((b - t) * np.float32(height))
    return x, y, max(1, w), max(1, h)


def half(image):
    """Metade exata (largura//2, altura//2): média de cada bloco 2x2 em alfa pré-multiplicado."""
    rgba = np.asarray(image.convert('RGBA'), dtype=np.float64)
    h, w = rgba.shape[0] // 2 * 2, rgba.shape[1] // 2 * 2
    rgba = rgba[:h, :w]
    alpha = rgba[..., 3:4] / 255.0
    pre = np.concatenate([rgba[..., :3] * alpha, rgba[..., 3:4]], axis=2)
    block = pre.reshape(h // 2, 2, w // 2, 2, 4).mean(axis=(1, 3))
    a = block[..., 3:4]
    rgb = np.where(a > 0, block[..., :3] / np.maximum(a / 255.0, 1e-9), 0)
    out = np.concatenate([rgb, a], axis=2)
    return Image.fromarray(np.clip(np.rint(out), 0, 255).astype(np.uint8), 'RGBA')


def webp(image):
    data = io.BytesIO()
    image.save(data, format='WEBP', lossless=True, quality=100, method=6, exact=False)
    return data.getvalue()


def outputs():
    """{caminho: bytes} de tudo o que o script grava."""
    out = {}
    for hero in HEROES:
        if hero == 'p05':
            atlas = Image.open(ASSETS / 'art/heroes_p02_p05.png').convert('RGBA')
            w, h = atlas.width // 2, atlas.height // 2
            image = atlas.crop((w, h, w + w, h + h))
        else:
            image = half(Image.open(ASSETS / f'art/hero_{hero}.png'))
        out[ASSETS / f'art/tela/hero_{hero}.webp'] = webp(image)
    for face in FACES:
        sheet = Image.open(CONCEPTS / f'{face}.png').convert('RGBA')
        x, y, w, h = face_box(face, sheet.width, sheet.height)
        out[ASSETS / f'rostos/{face}.webp'] = webp(sheet.crop((x, y, x + w, y + h)))
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    stale = []
    for path, data in outputs().items():
        if args.check:
            if not path.is_file() or path.read_bytes() != data:
                stale.append(path.relative_to(ROOT).as_posix())
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    if stale:
        sys.exit('Arte da seleção velha; rode tools/selection/gerar_selecao.py:\n' + '\n'.join(stale))
    print('Arte da seleção ' + ('em dia' if args.check else 'gravada'))


if __name__ == '__main__':
    main()
