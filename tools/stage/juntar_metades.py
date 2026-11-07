#!/usr/bin/env python3
"""Junta duas metades de um panorama pintadas à parte pelo Codex (cada uma com uma sobra em comum).

    python3 tools/stage/juntar_metades.py esq.png dir.png saida.png --largura 4096 --altura 1265 \
        --metade 2248 [--nuvens]

As duas metades vêm ampliadas (Real-ESRGAN x2) e cada uma cobre ``--metade`` px da largura final: a
esquerda de 0 a ``metade``, a direita de ``largura - metade`` até o fim. Na sobra, a troca de uma
para a outra segue um caminho vertical de menor custo que passa pelo céu (sem cortar galho, folha
ou, com ``--nuvens``, nuvem), com 6 px de transição (20 com ``--nuvens``). Uma mistura em degradê
deixava folhas fantasmas; um corte reto, galhos cortados.
"""
import argparse

import numpy as np
from PIL import Image


def ceu(im):
    r, g, b = [im[..., i] for i in range(3)]
    return ((b > r + 35) & (b > g + 10)) | ((b > 185) & (g > 185) & (r > 175) & (abs(b - r) < 60))


def nuvem(im):
    mx, mn = im.max(2), im.min(2)
    return (mx - mn) / np.maximum(mx, 1) < 0.55


def caminho(custo):
    """A coluna da troca em cada linha (programação dinâmica, passo de no máximo 1 px por linha)."""
    h, w = custo.shape
    acc = custo.copy()
    back = np.zeros((h, w), np.int32)
    for y in range(1, h):
        p = acc[y - 1]
        cand = np.stack([np.r_[np.inf, p[:-1]], p, np.r_[p[1:], np.inf]])
        back[y] = cand.argmin(0) - 1
        acc[y] += cand.min(0)
    x = int(acc[-1].argmin())
    xs = np.zeros(h, np.int32)
    for y in range(h - 1, -1, -1):
        xs[y] = x
        x = min(w - 1, max(0, x + back[y, x]))
    return xs


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("esq")
    ap.add_argument("dir")
    ap.add_argument("saida")
    ap.add_argument("--largura", type=int, default=4096)
    ap.add_argument("--altura", type=int, required=True)
    ap.add_argument("--metade", type=int, required=True)
    ap.add_argument("--nuvens", action="store_true", help="evitar cortar nuvens (o céu sozinho)")
    a = ap.parse_args()
    W, H, M = a.largura, a.altura, a.metade
    s = 2 * M - W                                           # a sobra em comum
    abrir = lambda f: np.asarray(Image.open(f).convert("RGB").resize((M, H), Image.LANCZOS)).astype(np.float32)
    L, R = abrir(a.esq), abrir(a.dir)
    l, r = L[:, W - M:], R[:, :s]
    if a.nuvens:
        livre = nuvem(l) | nuvem(r)
        # O azul das duas, linha a linha, casado no meio (a troca não marca).
        ok = (~livre)[..., None]
        dif = (l * ok).sum(1) / np.maximum(ok.sum(1), 1) - (r * ok).sum(1) / np.maximum(ok.sum(1), 1)
        L[:, :] -= dif[:, None, :] * 0.5
        R[:, :] += dif[:, None, :] * 0.5
        l, r = L[:, W - M:], R[:, :s]
        trans = 20.0
    else:
        livre = ~ceu(l) | ~ceu(r)
        trans = 6.0
    custo = np.abs(l - r).mean(2) + 400.0 * livre + np.abs(np.linspace(-1, 1, s))[None, :] * 30
    xs = caminho(custo)
    w = np.clip((xs[:, None] - np.arange(s)[None, :]) / trans + 0.5, 0, 1)[..., None]
    out = np.zeros((H, W, 3), np.float32)
    out[:, :W - M] = L[:, :W - M]
    out[:, M:] = R[:, s:]
    out[:, W - M:M] = l * w + r * (1 - w)
    Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).save(a.saida)


if __name__ == "__main__":
    main()
