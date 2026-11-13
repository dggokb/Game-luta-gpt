#!/usr/bin/env python3
"""Confere se o texto dos balões das páginas prontas cabe no miolo branco, com folga.

Simula o que o jogo faz (PaginaGuiada.fit: maior letra até 40 px que cabe na "caixa", texto
centrado nela) em português, inglês e espanhol, e mede a menor distância da letra até a borda
do balão ("area"), em frações do tamanho da letra. Abaixo de 0,15 o texto encosta ou vaza
(balão espetado, cuja caixa pega as pontas, é o caso típico).

    pip install opencv-python-headless numpy pillow
    python3 tools/historia/conferir_baloes.py             # lista os que encostam
    python3 tools/historia/conferir_baloes.py --corrigir  # encolhe a caixa deles nos episodio.json

A correção só encolhe a "caixa" pelos lados onde a letra encosta, até a folga passar de 0,22.
"""
import glob
import json
import os
import sys

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
H = os.path.join(ROOT, "android", "app", "src", "main", "assets", "historia")
# Negrito parecido com o do celular (Roboto); o DejaVu é a reserva.
FONTES = ["/usr/share/fonts/opentype/inter/Inter-Bold.otf",
          "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"]
FONTE = next((f for f in FONTES if os.path.exists(f)), None)
LIMITE, ALVO = 0.15, 0.22
IDIOMAS = {lang: json.load(open(os.path.join(H, "idiomas", lang + ".json"))) for lang in ("en", "es")}
_fontes = {}


def fonte(size):
    if size not in _fontes:
        _fontes[size] = ImageFont.truetype(FONTE, size)
    return _fontes[size]


def wrap(text, size, maxw):
    lines = []
    for par in text.split("\n"):
        line = ""
        for word in par.split(" "):
            if not word:
                continue
            cand = word if not line else line + " " + word
            if line and fonte(size).getlength(cand) > maxw:
                lines.append(line)
                line = word
            else:
                line = cand
        lines.append(line)
    return lines


def fit(text, w, h):
    size = int(min(40, h))
    lines = wrap(text, size, w)
    while size > 9:
        lines = wrap(text, size, w)
        widest = max(fonte(size).getlength(l) for l in lines)
        if widest <= w * 1.04 and len(lines) * size * 1.12 <= h * 1.04:
            break
        size -= 1
    return size, lines


def letras(shape, text, caixa, W, Hh):
    size, lines = fit(text, (caixa[2] - caixa[0]) * W, (caixa[3] - caixa[1]) * Hh)
    lh = size * 1.12
    cx = (caixa[0] + caixa[2]) * 0.5 * W
    y = (caixa[1] + caixa[3]) * 0.5 * Hh - len(lines) * lh * 0.5 + size * 0.86
    im = Image.new("L", (shape[1], shape[0]), 0)
    d = ImageDraw.Draw(im)
    for l in lines:
        d.text((cx, y), l, font=fonte(size), fill=255, anchor="ms")
        y += lh
    return np.array(im) > 0, size


def textos(b):
    return [b["texto"]] + [d.get(b["texto"], b["texto"]) for d in IDIOMAS.values()]


def distancia(b, W, Hh):
    im = Image.new("L", (W, Hh), 0)
    a = b["area"]
    ImageDraw.Draw(im).polygon([(a[i] * W, a[i + 1] * Hh) for i in range(0, len(a), 2)], fill=255)
    m = (np.array(im) > 0).astype(np.uint8)
    return m, cv2.distanceTransform(m, cv2.DIST_L2, 5)


def folga(b, W, Hh, caixa, m, dist):
    """Menor distância letra-borda / tamanho da letra (negativo: pixels fora do balão)."""
    pior, menor = 1e9, 99
    for t in textos(b):
        tm, size = letras((Hh, W), t, caixa, W, Hh)
        if not tm.any():
            continue
        fora = int((tm & (m == 0)).sum())
        val = float(dist[tm].min()) / size if fora == 0 else -fora / size
        pior, menor = min(pior, val), min(menor, size)
    return pior, menor


def lados(b, W, Hh, caixa, dist):
    s = set()
    cx, cy = (caixa[0] + caixa[2]) / 2 * W, (caixa[1] + caixa[3]) / 2 * Hh
    w, h = (caixa[2] - caixa[0]) * W, (caixa[3] - caixa[1]) * Hh
    for t in textos(b):
        tm, size = letras((Hh, W), t, caixa, W, Hh)
        yy, xx = np.where(tm & (dist < ALVO * size))
        if (xx < cx - 0.2 * w).any(): s.add("L")
        if (xx > cx + 0.2 * w).any(): s.add("R")
        if (yy < cy - 0.2 * h).any(): s.add("T")
        if (yy > cy + 0.2 * h).any(): s.add("B")
    return s or {"L", "R", "T", "B"}


def corrige(b, W, Hh, m, dist):
    c = list(b["caixa"])
    for _ in range(60):
        f, _ = folga(b, W, Hh, c, m, dist)
        if f >= ALVO:
            break
        s = lados(b, W, Hh, c, dist)
        dx, dy = (c[2] - c[0]) * 0.025, (c[3] - c[1]) * 0.025
        if "L" in s: c[0] += dx
        if "R" in s: c[2] -= dx
        if "T" in s: c[1] += dy
        if "B" in s: c[3] -= dy
    return [round(v, 4) for v in c]


def main():
    corrigir = "--corrigir" in sys.argv
    total = 0
    # Os episódios e as páginas das cinemáticas (a "imagem" delas já traz a pasta).
    arquivos = sorted(glob.glob(os.path.join(H, "ep*", "episodio.json"))) + \
        sorted(glob.glob(os.path.join(H, "**", "cinematica*.json"), recursive=True))
    for ep in arquivos:
        epn = os.path.relpath(os.path.dirname(ep), H)
        src = open(ep, encoding="utf-8").read()
        out = src
        dados = json.loads(src)
        cena = "acoes" in dados
        for p in dados["acoes"] if cena else dados["passos"]:
            if "quadros" not in p or "imagem" not in p:
                continue
            W, Hh = Image.open(os.path.join(H, p["imagem"] if cena else os.path.join(epn, p["imagem"]))).size
            for qi, q in enumerate(p["quadros"]):
                for b in q.get("baloes", []):
                    m, dist = distancia(b, W, Hh)
                    f, size = folga(b, W, Hh, b["caixa"], m, dist)
                    if f >= LIMITE:
                        continue
                    total += 1
                    linha = f"{epn} {p['imagem']} quadro {qi + 1}: {b['texto'][:40]!r} folga {f:.2f} letra {size}"
                    if corrigir:
                        nova = corrige(b, W, Hh, m, dist)
                        velho = '"caixa": ' + json.dumps(b["caixa"])
                        if out.count(velho) == 1:
                            out = out.replace(velho, '"caixa": ' + json.dumps(nova))
                            f2, s2 = folga(b, W, Hh, nova, m, dist)
                            linha += f" -> folga {f2:.2f} letra {s2}"
                        else:
                            linha += " (caixa repetida no arquivo: corrija à mão)"
                    print(linha)
        if out != src:
            json.loads(out)
            open(ep, "w", encoding="utf-8").write(out)
    print(f"{total} balões com o texto encostando na borda")


if __name__ == "__main__":
    main()
