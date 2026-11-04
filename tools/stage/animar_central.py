#!/usr/bin/env python3
"""Anima a Arena Central montada do panorama (depois do ``cenario_panorama.py``).

    python3 tools/stage/cenario_panorama.py art/stages/arena_central/recorte.json \\
        android/app/src/main/assets/stages/arena_central
    python3 tools/stage/animar_central.py

O panorama é uma pintura parada. Este script põe nela o que o motor sabe mexer, com as
posições medidas à mão no panorama (pixels do ``panorama.webp``; as camadas têm a mesma
largura e começam na linha 0 dele):

* **bandeiras**: cada estandarte vermelho sai da imagem do estádio (o ``soltar_bandeiras``
  acha o pano pela cor dentro do retângulo) e balança; o lugar dele é tapado com a parede;
* **fogos**: as chamas dos braseiros do muro, que também são focos da sombra dos lutadores
  (sombra comprida e fraca para a frente, junto com a dos holofotes);
* **luzes**: os holofotes das torres e as lanternas, pulsando (as baixas refletem no chão);
* **plateia**: flashes de câmera na gente das arquibancadas (máscara pela textura, dentro
  dos retângulos das arquibancadas); sem pulo, que no panorama borrava a gente miúda;
* **cachoeiras**: a água das três quedas da camada das montanhas corre.

Grava as imagens novas na pasta do cenário e os campos nas camadas do stage.json. Rode de
novo depois de refazer as camadas (o estádio volta com as bandeiras pintadas).
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

AQUI = Path(__file__).resolve().parent
sys.path.insert(0, str(AQUI))
import cachoeiras  # noqa: E402
import cenario_do_conceito as cc  # noqa: E402
import pontos_plateia  # noqa: E402
import soltar_bandeiras as sb  # noqa: E402
from montar_porto import plateia as mascara_plateia  # noqa: E402

PASTA = AQUI.parents[1] / "android/app/src/main/assets/stages/arena_central"
ESTADIO, MONTANHAS = "estadio.webp", "montanhas.webp"

# Estandartes: (x0, y0, x1, y1) com folga em volta do pano. Nas torres e postes eles pendem
# soltos (balançam mais); no muro e nas sacadas, presos na parede.
TORRES = [(444, 165, 496, 327), (696, 196, 740, 312), (748, 232, 774, 308), (1228, 198, 1280, 355),
          (1298, 240, 1325, 320)]
PAREDE = [(25, 135, 105, 392), (8, 120, 34, 180), (102, 88, 123, 133), (105, 165, 130, 220),
          (172, 120, 196, 170), (267, 210, 290, 262), (212, 275, 255, 386), (334, 310, 366, 400),
          (648, 338, 673, 380), (809, 341, 841, 420), (892, 342, 913, 380), (925, 297, 949, 342),
          (1026, 297, 1050, 342), (1095, 341, 1127, 415), (1142, 342, 1164, 382), (1329, 342, 1355, 382),
          (1511, 250, 1540, 340), (1636, 222, 1671, 315), (1698, 204, 1719, 252), (1842, 144, 1864, 205),
          (1873, 150, 1900, 205), (1926, 225, 1967, 324)]

# Braseiros: o ponto onde a chama encosta na bacia e a altura dela (pixels).
BRASEIROS = [(229, 410, 26), (538, 392, 24), (870, 391, 24), (1073, 391, 24), (1376, 389, 24)]

# Focos da sombra dos lutadores: os holofotes (o da esquerda é o sol do conceito, o mais forte)
# e as chamas dos braseiros (baixas: sombra comprida e fraca, que some longe da tocha).
HOLOFOTES_SOMBRA = [(713, 63, 0.72), (1256, 63, 0.35)]
TOCHA_ALFA, TOCHA_ALCANCE, TOCHA_CHAMA = 0.55, 3000, 10

# Luzes: (x, y, raio em pixels, cor, reflete no chão).
HOLOFOTE, LANTERNA = "#FFE6B0", "#FFB45A"
LUZES = [
    (713, 63, 34, HOLOFOTE, False), (1256, 63, 34, HOLOFOTE, False),
    (139, 346, 9, LANTERNA, True), (289, 368, 8, LANTERNA, True), (381, 381, 7, LANTERNA, True),
    (291, 156, 8, LANTERNA, False), (575, 342, 7, LANTERNA, True), (759, 343, 6, LANTERNA, True),
    (832, 296, 6, LANTERNA, False), (912, 281, 7, LANTERNA, False), (987, 271, 7, LANTERNA, False),
    (1059, 281, 7, LANTERNA, False), (1132, 296, 6, LANTERNA, False), (1208, 343, 6, LANTERNA, True),
    (1416, 346, 7, LANTERNA, True), (1592, 343, 7, LANTERNA, True), (1682, 190, 8, LANTERNA, False),
    (1916, 327, 9, LANTERNA, True), (1959, 346, 9, LANTERNA, True),
]

# Arquibancadas (onde a gente pode pular) e a faixa de linhas em que elas ficam.
GENTE = [(95, 200, 300, 300), (300, 230, 480, 335), (480, 270, 1505, 345), (1300, 285, 1500, 330),
         (1540, 250, 1705, 335), (1700, 165, 1983, 300)]

# Dentro das arquibancadas, o que não é gente (pavilhão, escadas, muretas de pedra).
NAO_GENTE = [(885, 235, 1085, 345), (750, 285, 805, 350), (1160, 285, 1215, 350), (1700, 215, 1840, 305),
             (1600, 280, 1700, 340), (1830, 230, 1983, 260), (480, 330, 1505, 345)]

# Quedas d'água na camada das montanhas (x, y, largura, altura em pixels).
QUEDAS = [(1410, 185, 75, 95), (1685, 110, 35, 68), (1798, 70, 26, 35)]


def pano(img, caixa):
    """O pano do estandarte dentro da caixa: o vermelho forte e a borda dourada encostada nele,
    numa peça só, com o emblema (os buracos) preenchido. Mais estrito que o soltar_bandeiras:
    no panorama ao pôr do sol a pedra, a treliça e a grade também puxam para o laranja, e
    vinham junto (balançavam com o pano)."""
    import cv2
    x0, y0, x1, y1 = caixa
    r, g, b, a = (img[y0:y1, x0:x1, k].astype(np.float32) for k in range(4))
    mx = np.maximum(np.maximum(r, g), b)
    sat = (mx - np.minimum(np.minimum(r, g), b)) / np.maximum(mx, 1)
    vermelho = (r > 95) & (r > g * 1.8) & (r > b * 1.5) & (sat > 0.5) & (a > 200)
    dourado = (r > 150) & (g > 95) & (b < g * 0.7) & (sat > 0.45) & (a > 200)
    v = vermelho.astype(np.uint8)
    n, lab, st, _ = cv2.connectedComponentsWithStats(cv2.morphologyEx(v, cv2.MORPH_CLOSE, np.ones((3, 3), np.uint8)))
    if n < 2:
        return None
    maior = 1 + int(np.argmax(st[1:, cv2.CC_STAT_AREA]))
    if st[maior, cv2.CC_STAT_AREA] < 60:
        return None
    corpo = (lab == maior).astype(np.uint8)
    perto = cv2.dilate(corpo, np.ones((5, 5), np.uint8)).astype(bool)
    m = (corpo.astype(bool) | (dourado & perto)).astype(np.uint8)
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))
    # Buracos (o emblema, que não é vermelho nem dourado vivo) preenchidos.
    # (Com uma margem vazia em volta: o canto da caixa pode ser pano.)
    cheio = np.pad(m, 1)
    borda = np.zeros((cheio.shape[0] + 2, cheio.shape[1] + 2), np.uint8)
    cv2.floodFill(cheio, borda, (0, 0), 2)
    m = ((m == 1) | (cheio[1:-1, 1:-1] == 0)).astype(bool)
    out = np.zeros(img.shape[:2], bool)
    out[y0:y1, x0:x1] = m
    return out


def soltar(img, caixas, balanco, pasta, lista):
    """Solta os estandartes das caixas (o corte do soltar_bandeiras, com o pano daqui)."""
    h, w = img.shape[:2]
    for x0, y0, x1, y1 in caixas:
        m = pano(img, (x0, y0, x1, y1))
        if m is None:
            raise SystemExit(f"sem estandarte em {(x0, y0, x1, y1)} (o estádio já foi animado? refaça as camadas)")
        ys = np.nonzero(m.any(axis=1))[0]
        corte = ys[0] + max(3, round(sb.PRESO * (ys[-1] - ys[0])))
        solta = m.copy()
        solta[:corte] = False
        leva = m.copy()
        leva[:corte - sb.SOBRA] = False
        yy, xx = np.nonzero(leva)
        bx0, bx1, by0, by1 = xx.min(), xx.max() + 1, yy.min(), yy.max() + 1
        pano_img = np.zeros((by1 - by0, bx1 - bx0, 4), np.uint8)
        recorte = leva[by0:by1, bx0:bx1]
        pano_img[recorte] = img[by0:by1, bx0:bx1][recorte]
        nome = f"estadio_bandeira{len(lista) + 1}.png"
        Image.fromarray(pano_img).save(pasta / nome, optimize=True)
        img = sb.tapar(img, solta, "parede")
        lista.append({"imagem": nome, "x": round(float(bx0 / w), 4), "y": round(float(by0 / h), 4),
                      "largura": round(float((bx1 - bx0) / w), 4), "altura": round(float((by1 - by0) / h), 4),
                      "balanco": balanco})
    return img


def no_mundo(px, py, janela, stage):
    """Um ponto do panorama no estádio (camada no fim do chão) em coordenadas do mundo:
    x, altura acima do chão e profundidade, pela câmera de repouso do cenario_do_conceito."""
    escala, jx, jy = janela["escala"], janela["x"], janela["y"]
    z = stage["chao"]["longe"]
    horizon, eye_y, distance = cc.camera(stage["camera"]["horizonte"])
    s = cc.BASE_DISTANCE / (distance + z)
    chao_y = horizon + (cc.GROUND_Y - eye_y) * s
    sx, sy = (px - jx) / escala, (py - jy) / escala
    return round(cc.REST_CENTER_X + (sx - cc.VW / 2) / s), round((chao_y - sy) / s), z


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", nargs="?", type=Path, default=PASTA)
    pasta = ap.parse_args().pasta

    estadio = Image.open(pasta / ESTADIO).convert("RGBA")
    w, h = estadio.size
    img = np.array(estadio)

    bandeiras = []
    img = soltar(img, TORRES, sb.SOLTA, pasta, bandeiras)
    img = soltar(img, PAREDE, sb.NA_PAREDE, pasta, bandeiras)
    Image.fromarray(img).save(pasta / ESTADIO, quality=90, alpha_quality=100, method=6)

    # A gente: a máscara do montar_porto (textura miúda e escura, fora do vermelho), só dentro
    # das arquibancadas.
    topo = min(y0 for _, y0, _, _ in GENTE) / h
    base = max(y1 for _, _, _, y1 in GENTE) / h
    gente = mascara_plateia(Image.fromarray(img), [(topo, base)])
    dentro = np.zeros((h, w), bool)
    for x0, y0, x1, y1 in GENTE:
        dentro[y0:y1, x0:x1] = True
    for x0, y0, x1, y1 in NAO_GENTE:
        dentro[y0:y1, x0:x1] = False
    a = np.asarray(gente)[..., 3].copy()
    a[~dentro] = 0
    gente.putalpha(Image.fromarray(a))
    gente.save(pasta / "estadio_plateia.webp", quality=90, alpha_quality=100, method=6)
    flashes = pontos_plateia.pontos(pasta / "estadio_plateia.webp", topo, base, 220, 7)

    fogos = [{"x": round(x / w, 4), "y": round(y / h, 4), "tamanho": round(t / h, 4)} for x, y, t in BRASEIROS]
    luzes = [{"x": round(x / w, 4), "y": round(y / h, 4), "raio": round(r / h, 4), "cor": cor,
              "pulsar": 0.04 if cor == HOLOFOTE else 0.08, "reflexo": reflete}
             for x, y, r, cor, reflete in LUZES]

    stage_path = pasta / "stage.json"
    stage = json.loads(stage_path.read_text(encoding="utf-8"))
    janela = json.loads((AQUI.parents[1] / "art/stages/arena_central/recorte.json").read_text(encoding="utf-8"))["janela"]
    focos = []
    for x, y, alfa in HOLOFOTES_SOMBRA:
        fx, fy, fz = no_mundo(x, y, janela, stage)
        focos.append({"x": fx, "altura": fy, "profundidade": fz, "alfa": alfa})
    for x, y, t in BRASEIROS:
        fx, fy, fz = no_mundo(x, y - TOCHA_CHAMA, janela, stage)
        focos.append({"x": fx, "altura": fy, "profundidade": fz, "alfa": TOCHA_ALFA, "alcance": TOCHA_ALCANCE})
    stage["sombraLutadores"]["focos"] = focos
    # Piso de pedra fosca: as luzes e o fogo não refletem nele.
    stage["chao"]["reflexoLuzes"] = 0
    stage["chao"]["centroX"] = round(cc.REST_CENTER_X)
    for camada in stage["camadas"]:
        if camada.get("imagem") == ESTADIO:
            camada["bandeiras"] = bandeiras
            camada["fogos"] = fogos
            camada["luzes"] = luzes
            # Sem pulo: no panorama a gente é miúda e a coluna pulando borrava (parecia meleca);
            # ficam os flashes de câmera.
            camada["plateia"] = [{"topo": round(topo, 4), "base": round(base, 4), "mascara": "estadio_plateia.webp",
                                  "pulo": 0, "colunas": 160, "flashes": flashes}]
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    # A água: a folha do cachoeiras.py para a camada das montanhas.
    mw, mh = Image.open(pasta / MONTANHAS).size
    cachoeiras.AREAS[MONTANHAS] = {
        "tela": 1.1,
        "areas": [(round(x / mw, 4), round(y / mh, 4), round(cw / mw, 4), round(ch / mh, 4)) for x, y, cw, ch in QUEDAS],
    }
    feitas = cachoeiras.montar(pasta, MONTANHAS)
    if not cachoeiras.anotar(pasta, MONTANHAS, feitas):
        raise SystemExit("camada das montanhas não achada no stage.json")
    print(f"{len(bandeiras)} bandeiras, {len(fogos)} fogos, {len(luzes)} luzes, plateia com {len(flashes)} flashes, "
          f"{len(QUEDAS)} quedas d'água")


if __name__ == "__main__":
    main()
