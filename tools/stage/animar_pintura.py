#!/usr/bin/env python3
"""Anima a Arena Central (pintura), montada pelo ``cenario_pintura.py`` com ``--fundo``.

    python3 tools/stage/cenario_pintura.py ... --fundo ... --mascara ...
    python3 tools/stage/animar_pintura.py

A pintura é parada, com a luz pintada. Este script põe nela o que o motor sabe mexer, como o
``animar_central.py`` faz na Arena antiga, mas sem posições medidas à mão: tudo é achado pela
cor na ``parede.png`` (a arena recortada) e no ``fundo.jpg`` (céu e montanhas).

* **bandeiras**: cada pano vermelho da arena é solto e balança (nas torres, pendurado e
  solto; na parede, preso); o lugar dele é tapado com a parede;
* **fogos**: as chamas dos braseiros (laranja forte no muro) ganham a chama animada;
* **luzes**: os holofotes das torres (branco no alto) e as lanternas (pontos amarelos no muro),
  pulsando; as baixas refletem no chão;
* **plateia**: a gente das arquibancadas pula de leve e solta flashes de câmera;
* **cachoeiras**: a água das quedas do fundo corre.

Rode sempre depois do ``cenario_pintura.py`` (ele refaz a parede com as bandeiras pintadas).
"""
import argparse
import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

AQUI = Path(__file__).resolve().parent
sys.path.insert(0, str(AQUI))
import animar_central as ac  # noqa: E402
import cachoeiras  # noqa: E402
import pontos_plateia  # noqa: E402
import soltar_bandeiras as sb  # noqa: E402
from montar_porto import plateia as mascara_plateia  # noqa: E402

PASTA = AQUI.parents[1] / "android/app/src/main/assets/stages/arena_pintura"
PAREDE, FUNDO = "parede.png", "fundo.jpg"
MAX_BANDEIRAS = 24


def componentes(m, area_min):
    n, _, st, cen = cv2.connectedComponentsWithStats(m.astype(np.uint8), 8)
    return [(st[i], cen[i]) for i in range(1, n) if st[i, cv2.CC_STAT_AREA] >= area_min]


def bandeiras(img, h, w):
    """Caixas em volta de cada pano vermelho (com folga), separando torre (solto) e parede."""
    r, g, b, a = (img[..., k].astype(np.float32) for k in range(4))
    mx = np.maximum(np.maximum(r, g), b)
    sat = (mx - np.minimum(np.minimum(r, g), b)) / np.maximum(mx, 1)
    vermelho = (r > 95) & (r > g * 1.8) & (r > b * 1.5) & (sat > 0.5) & (a > 200)
    # Os painéis vermelhos do muro (embaixo, largos) não são bandeiras.
    vermelho[int(0.80 * h):] = False
    v = cv2.morphologyEx(vermelho.astype(np.uint8), cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))
    torres, parede = [], []
    # Só os estandartes grandes (os pendões miúdos da plateia ficam pintados): até MAX_BANDEIRAS.
    achados = sorted(componentes(v, 700), key=lambda c: -c[0][cv2.CC_STAT_AREA])[:MAX_BANDEIRAS]
    for st, _ in achados:
        x, y, cw, ch = st[:4]
        if cw > 3.2 * ch or cw > 0.04 * w or ch < 30:      # faixa larga: painel, não estandarte
            continue
        f = 6
        caixa = (max(0, x - f), max(0, y - f), min(w, x + cw + f), min(h, y + ch + f))
        if ac.pano(img, caixa) is None:
            continue
        (torres if y < 0.45 * h and ch > 0.12 * h else parede).append(caixa)
    return torres, parede


def fogos(img, h, w):
    r, g, b, a = (img[..., k].astype(np.float32) for k in range(4))
    chama = (r > 225) & (g > 110) & (b < 120) & (r - b > 130) & (a > 200)
    chama[:int(0.72 * h)] = False
    m = cv2.morphologyEx(chama.astype(np.uint8), cv2.MORPH_CLOSE, np.ones((7, 7), np.uint8))
    lista = []
    for st, _ in componentes(m, 120):
        x, y, cw, ch = st[:4]
        if ch < 12:
            continue
        lista.append({"x": round(float((x + cw / 2) / w), 4), "y": round(float((y + ch * 0.92) / h), 4),
                      "tamanho": round(float(ch * 0.9 / h), 4)})
    return sorted(lista, key=lambda f: f["x"])


def luzes(img, h, w, fogo):
    r, g, b, a = (img[..., k].astype(np.float32) for k in range(4))
    lista = []
    # Holofotes: branco quase puro no alto das torres (as lâmpadas de uma torre viram uma luz).
    branco = (np.minimum(np.minimum(r, g), b) > 205) & (a > 200)
    branco[int(0.30 * h):] = False
    m = cv2.dilate(branco.astype(np.uint8), np.ones((25, 25), np.uint8))
    for st, cen in componentes(m, 400):
        raio = max(st[2], st[3]) * 0.75
        lista.append({"x": round(float(cen[0] / w), 4), "y": round(float(cen[1] / h), 4),
                      "raio": round(float(raio / h), 4), "cor": ac.HOLOFOTE, "pulsar": 0.04, "reflexo": False})
    # Lanternas: pontos amarelos pequenos no muro e nas sacadas, longe das chamas.
    amarelo = (r > 235) & (g > 175) & (b < 170) & (b > 40) & (a > 200)
    amarelo[:int(0.40 * h)] = False
    for st, cen in componentes(amarelo, 18):
        if st[cv2.CC_STAT_AREA] > 500:
            continue
        cx, cy = cen
        if any(abs(cx / w - f["x"]) < 0.015 and cy / h > f["y"] - 2.5 * f["tamanho"] for f in fogo):
            continue
        raio = 3.0 * np.sqrt(st[cv2.CC_STAT_AREA])
        lista.append({"x": round(float(cx / w), 4), "y": round(float(cy / h), 4), "raio": round(float(raio / h), 4),
                      "cor": ac.LANTERNA, "pulsar": 0.08, "reflexo": bool(cy > 0.62 * h)})
    return lista


def quedas(fundo, extra):
    """Caixas (x, y, largura, altura em pixels) das quedas d'água: claro, pouco saturado e alto."""
    px = np.asarray(fundo).astype(np.float32)
    mx, mn = px.max(axis=2), px.min(axis=2)
    agua = (mn > 165) & ((mx - mn) / np.maximum(mx, 1) < 0.22)
    agua[:extra] = False
    m = cv2.morphologyEx(agua.astype(np.uint8), cv2.MORPH_OPEN, np.ones((9, 2), np.uint8))
    m = cv2.dilate(m, np.ones((15, 9), np.uint8))
    caixas = []
    for st, _ in componentes(m, 250):
        x, y, cw, ch = st[:4]
        if ch > 1.4 * cw and ch > 30:
            caixas.append((int(x), int(y), int(cw), int(ch)))
    return caixas


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", nargs="?", type=Path, default=PASTA)
    pasta = ap.parse_args().pasta
    stage_path = pasta / "stage.json"
    stage = json.loads(stage_path.read_text(encoding="utf-8"))
    camada = {c["id"]: c for c in stage["camadas"]}

    img = np.array(Image.open(pasta / PAREDE).convert("RGBA"))
    h, w = img.shape[:2]
    ac.LARGURA_MEDIDA = 1983                 # o pano() escala os filtros pela largura
    torres, parede = bandeiras(img, h, w)
    fogo = fogos(img, h, w)
    luz = luzes(img, h, w, fogo)

    lista = []
    for caixas, balanco in ((torres, sb.SOLTA), (parede, sb.NA_PAREDE)):
        for caixa in caixas:
            if ac.pano(img, caixa) is not None:   # a de antes pode ter tapado esta
                img = ac.soltar(img, [caixa], balanco, pasta, lista)
    for b in lista:                          # o soltar dá o nome do estádio antigo
        novo = b["imagem"].replace("estadio_", "parede_")
        (pasta / b["imagem"]).rename(pasta / novo)
        b["imagem"] = novo
    Image.fromarray(img).save(pasta / PAREDE, optimize=True)

    # A gente das arquibancadas (fora do pavilhão do meio e do muro de baixo).
    topo, base = 0.36, 0.78
    gente = mascara_plateia(Image.fromarray(img), [(topo, base)])
    al = np.asarray(gente)[..., 3].copy()
    al[:, int(0.455 * w):int(0.545 * w)] = 0
    gente.putalpha(Image.fromarray(al))
    gente.save(pasta / "parede_plateia.webp", quality=90, alpha_quality=100, method=6)
    flashes = pontos_plateia.pontos(pasta / "parede_plateia.webp", topo, base, 260, 7)

    p = camada["parede"]
    p["bandeiras"] = lista
    p["fogos"] = fogo
    p["luzes"] = luz
    p["plateia"] = [{"topo": topo, "base": base, "mascara": "parede_plateia.webp", "pulo": 0.004,
                     "colunas": 200, "flashes": flashes}]
    stage["chao"]["reflexoLuzes"] = 0.3
    camada["fundo"].pop("cachoeiras", None)
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    fundo = Image.open(pasta / FUNDO).convert("RGB")
    fw, fh = fundo.size
    # As linhas de céu espelhado em cima (o cenario_pintura põe --ceu-extra na escala da pintura).
    extra = int(round(-camada["fundo"]["topo"] / (camada["fundo"]["base"] - camada["fundo"]["topo"]) * fh))
    caixas = quedas(fundo, extra)
    if caixas:
        cachoeiras.AREAS[FUNDO] = {"tela": 1.1 / (fw / 1983),
                                   "areas": [(round(x / fw, 4), round(y / fh, 4), round(cw / fw, 4), round(ch / fh, 4))
                                             for x, y, cw, ch in caixas]}
        feitas = cachoeiras.montar(pasta, FUNDO)
        if not cachoeiras.anotar(pasta, FUNDO, feitas):
            raise SystemExit("camada do fundo não achada no stage.json")
    print(f"{len(lista)} bandeiras ({len(torres)} nas torres), {len(fogo)} fogos, {len(luz)} luzes, "
          f"plateia com {len(flashes)} flashes, {len(caixas)} quedas d'água")


if __name__ == "__main__":
    main()
