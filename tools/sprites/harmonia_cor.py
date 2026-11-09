#!/usr/bin/env python3
"""Harmonia de cor entre os clipes de um personagem.

Cada clipe vem de um vídeo gerado à parte, e a pele e a roupa mudam de tom de um vídeo para
o outro (medido no Ryūga: a pele do dano em pé fica a ΔE 7 da guarda, a do dano agachado a
ΔE 12; acima de ~5 a troca de tom se vê quando o golpe troca de clipe). A direção do jogo pede
uma paleta fixa por personagem (docs/revisao-base/direcao.md, regras A10 e L7), mas
posterizar o vídeo estraga o traço (testado: rosto e linhas somem). Aqui só a cor média de
cada material anda:

- as âncoras são os materiais do clipe de referência (a guarda): k-means em Lab, gravadas
  no JSON de cada clipe em "harmonia" (``--gravar``);
- ``--aplicar`` corrige a arte-fonte de cada clipe (o PNG de "source"): cada pixel vai para a
  âncora mais próxima, a média de cada material do clipe é medida e cada pixel anda o
  deslocamento do material dele (peso suave pela distância), com teto; o traço escuro não
  anda. Aplicar de novo não muda nada (os materiais já batem com as âncoras).

É ferramenta de autoria (numpy), como as outras: o gerador das folhas (build_characters.py)
continua só com o Pillow e monta a folha a partir da fonte já corrigida.
Sem aleatório nem BLAS: o resultado é o mesmo em qualquer máquina.

    python3 tools/sprites/harmonia_cor.py --gravar player_base_idle CLIPE...
    python3 tools/sprites/harmonia_cor.py --aplicar CLIPE...
"""
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
CLIPS = ROOT / 'tools/sprites/clips'
WHITE = (0.9505, 1.0, 1.089)


def lab(rgb):
    c = rgb / 255.0
    c = np.where(c > 0.04045, ((c + 0.055) / 1.055) ** 2.4, c / 12.92)
    r, g, b = c[:, 0], c[:, 1], c[:, 2]
    x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / WHITE[0]
    y = (0.2126 * r + 0.7152 * g + 0.0722 * b) / WHITE[1]
    z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / WHITE[2]
    f = lambda t: np.where(t > 0.008856, np.cbrt(t), 7.787 * t + 16 / 116)
    fx, fy, fz = f(x), f(y), f(z)
    return np.stack([116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)], 1)


def rgb_de_lab(L):
    fy = (L[:, 0] + 16) / 116
    fx = fy + L[:, 1] / 500
    fz = fy - L[:, 2] / 200
    inv = lambda t: np.where(t ** 3 > 0.008856, t ** 3, (t - 16 / 116) / 7.787)
    x, y, z = inv(fx) * WHITE[0], inv(fy) * WHITE[1], inv(fz) * WHITE[2]
    r = 3.2406 * x - 1.5372 * y - 0.4986 * z
    g = -0.9689 * x + 1.8758 * y + 0.0415 * z
    b = 0.0557 * x - 0.2040 * y + 1.0570 * z
    c = np.stack([r, g, b], 1)
    c = np.where(c > 0.0031308, 1.055 * np.power(np.clip(c, 0, None), 1 / 2.4) - 0.055, 12.92 * c)
    return np.clip(c * 255, 0, 255)


def _amostra(arr, n=80000):
    px = arr[arr[..., 3] > 230][:, :3].astype(float)
    passo = max(1, len(px) // n)
    return lab(px[::passo])


def _mais_proxima(L, C):
    d = np.zeros((len(L), len(C)))
    for j in range(len(C)):
        d[:, j] = ((L - C[j]) ** 2).sum(1)
    return d


def ancoras(ref_arr, k=14, voltas=25):
    """Os materiais do clipe de referência (centros em Lab)."""
    X = _amostra(ref_arr)
    # começo determinístico: pontos espaçados na ordem do brilho
    ordem = np.argsort(X[:, 0], kind='stable')
    C = X[ordem[np.linspace(0, len(X) - 1, k).astype(int)]].copy()
    for _ in range(voltas):
        rot = _mais_proxima(X, C).argmin(1)
        for j in range(k):
            m = rot == j
            if m.sum() > 50:
                C[j] = X[m].mean(0)
    return C


def aplicar(image, cfg):
    """O clipe (PIL RGBA) com a cor de cada material levada à da referência."""
    A = np.array(cfg['ancoras'], dtype=float)
    teto = float(cfg.get('teto', 14.0))
    sigma = float(cfg.get('sigma', 12.0))
    arr = np.array(image.convert('RGBA'))
    X = _amostra(arr)
    rot = _mais_proxima(X, A).argmin(1)
    C = A.copy()
    desl = np.zeros_like(A)
    for j in range(len(A)):
        m = rot == j
        # material raro neste clipe (um efeito, outra coisa) não puxa nada
        if m.sum() >= 0.01 * len(X):
            C[j] = X[m].mean(0)
            desl[j] = A[j] - C[j]
    n = np.sqrt((desl ** 2).sum(1))
    desl *= np.minimum(1.0, teto / np.maximum(n, 1e-6))[:, None]
    vis = arr[..., 3] > 0
    px = arr[vis][:, :3].astype(float)
    novo = np.empty((len(px), 3))
    for s in range(0, len(px), 200000):
        L = lab(px[s:s + 200000])
        d = _mais_proxima(L, C)
        w = np.exp(-(d - d.min(1, keepdims=True)) / (2 * sigma ** 2))
        w /= w.sum(1, keepdims=True)
        sh = np.zeros_like(L)
        for j in range(len(C)):
            sh += w[:, j:j + 1] * desl[j]
        escuro = np.clip((L[:, 0] - 15) / 20, 0, 1)[:, None]   # linhas (L < 15) ficam
        novo[s:s + 200000] = rgb_de_lab(L + sh * escuro)
    out = arr.copy()
    out[vis, :3] = np.floor(novo + 0.5).astype(np.uint8)
    return Image.fromarray(out, 'RGBA')


def gravar(referencia, clipes):
    ref_cfg = json.loads((CLIPS / f'{referencia}.json').read_text(encoding='utf-8'))
    ref = np.array(Image.open(ROOT / ref_cfg['output']).convert('RGBA'))
    A = ancoras(ref)
    for nome in clipes:
        p = CLIPS / f'{nome}.json'
        cfg = json.loads(p.read_text(encoding='utf-8'))
        cfg['harmonia'] = {'referencia': referencia, 'teto': 14.0, 'sigma': 12.0,
                           'ancoras': [[round(float(v), 2) for v in c] for c in A]}
        p.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print('harmonia ->', nome)


def aplicar_fontes(clipes):
    for nome in clipes:
        cfg = json.loads((CLIPS / f'{nome}.json').read_text(encoding='utf-8'))
        fonte = ROOT / cfg['source']
        img = Image.open(fonte)
        img.load()
        aplicar(img, cfg['harmonia']).save(fonte, format='PNG', optimize=True)
        print('fonte corrigida ->', cfg['source'])


if __name__ == '__main__':
    if len(sys.argv) >= 4 and sys.argv[1] == '--gravar':
        gravar(sys.argv[2], sys.argv[3:])
    elif len(sys.argv) >= 3 and sys.argv[1] == '--aplicar':
        aplicar_fontes(sys.argv[2:])
    else:
        sys.exit(__doc__)
