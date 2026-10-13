#!/usr/bin/env python3
"""Prepara uma página de mangá pronta para o modo história.

Acha os quadros pelas divisas claras (brancas) entre eles e os balões de fala (miolo branco
com texto), grava a página em JPG na pasta do episódio e imprime o passo do episodio.json
com o contorno de cada quadro na ordem de leitura (linhas de cima para baixo, quadros da
esquerda para a direita) e os balões de cada quadro. No jogo, o miolo de cada balão é
pintado de branco e o texto vem do episódio (traduzível); falta só preencher o "texto".
Também grava uma imagem de conferência com os quadros numerados e os balões marcados.

    pip install opencv-python-headless numpy
    python3 tools/historia/recortar_pagina.py pagina.png --episodio ep00 --nome pag1

Quadros separados só por uma linha preta, ou com o desenho passando por cima da divisa,
podem sair juntos: confira a imagem de conferência e corrija o "area" à mão no JSON.
"""
import argparse
import json
import os
import sys

import cv2
import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "android", "app", "src", "main", "assets", "historia")


def gutter_lines(img):
    """Máscara das divisas: traços claros, finos e compridos (o branco dentro do desenho fica de fora)."""
    h, w = img.shape[:2]
    longest = max(w, h)
    white = (img.min(axis=2) > 185).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(white, 8)
    lines = np.zeros_like(white)
    for i in range(1, count):
        _, _, bw, bh, area = stats[i]
        long_side = max(bw, bh)
        thin = area / float(bw * bh) < 0.35
        if long_side > 0.10 * longest and (thin or long_side > 0.5 * longest):
            lines[labels == i] = 1
    # Fecha as falhas pequenas nas linhas das bordas.
    return cv2.dilate(lines, np.ones((3, 3), np.uint8), iterations=2)


def find_panels(img, min_area=0.006):
    h, w = img.shape[:2]
    min_side = 0.05 * max(w, h)
    content = (1 - gutter_lines(img)).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(content, 4)
    panels = []
    for i in range(1, count):
        _, _, bw, bh, area = stats[i]
        # Fora: sobras pequenas e lascas finas entre divisas.
        if area < w * h * min_area or min(bw, bh) < min_side:
            continue
        mask = (labels == i).astype(np.uint8) * 255
        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        hull = cv2.convexHull(max(contours, key=cv2.contourArea))
        poly = cv2.approxPolyDP(hull, 0.012 * cv2.arcLength(hull, True), True).reshape(-1, 2)
        panels.append(poly.astype(float))
    return reading_order(panels, w * h)


def find_balloons(img):
    """Balões de fala: miolo branco e liso (quase elíptico) com texto escuro dentro. As
    onomatopeias em balão espetado ficam de fora (fazem parte da arte em qualquer idioma).
    Devolve o contorno do miolo branco (o jogo pinta de branco para apagar o texto) e a
    caixa onde o texto traduzido é escrito."""
    h, w = img.shape[:2]
    white = (img.min(axis=2) > 200).astype(np.uint8)
    filled = white.copy()
    contours, hierarchy = cv2.findContours(white, cv2.RETR_CCOMP, cv2.CHAIN_APPROX_SIMPLE)
    for i, c in enumerate(contours):
        # Buracos pequenos dentro do branco são as letras; os grandes são quadros cercados por divisas.
        if hierarchy[0][i][3] >= 0 and cv2.contourArea(c) < w * h * 0.01:
            cv2.drawContours(filled, contours, i, 1, -1)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(filled, 8)
    balloons = []
    for i in range(1, count):
        x, y, bw, bh, area = stats[i]
        if area < w * h * 0.002 or area > w * h * 0.08:
            continue
        mask = (labels == i).astype(np.uint8)
        c = max(cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)[0], key=cv2.contourArea)
        hull = cv2.convexHull(c)
        solidity = cv2.contourArea(c) / max(1.0, cv2.contourArea(hull))
        text = ((mask == 1) & (white == 0)).sum() / float(area)
        if solidity < 0.93 or text < 0.03:
            continue
        # Caixa do texto: em volta do ponto mais "fundo" do balão (longe do rabicho).
        dist = cv2.distanceTransform(mask, cv2.DIST_L2, 5)
        _, _, _, (cx, cy) = cv2.minMaxLoc(dist)
        row = np.where(mask[cy] == 1)[0]
        col = np.where(mask[:, cx] == 1)[0]
        half_w = min(cx - row.min(), row.max() - cx) * 0.74
        half_h = min(cy - col.min(), col.max() - cy) * 0.72
        poly = cv2.approxPolyDP(c, 1.5, True).reshape(-1, 2).astype(float)
        balloons.append({"poly": poly, "box": (cx - half_w, cy - half_h, cx + half_w, cy + half_h),
                         "center": (cx, cy)})
    return balloons


def inside(poly, point):
    return cv2.pointPolygonTest(poly.astype(np.float32).reshape(-1, 1, 2), point, False) >= 0


def reading_order(panels, page_area):
    """Linhas pela sobreposição vertical, da esquerda para a direita. Um quadro de fundo
    enorme (mais de 35% da página) entra na posição do centro dele."""
    def span(p):
        return p[:, 1].min(), p[:, 1].max()

    def area(p):
        return cv2.contourArea(p.astype(np.float32))

    normal = sorted([p for p in panels if area(p) <= 0.35 * page_area], key=lambda p: span(p)[0])
    big = [p for p in panels if area(p) > 0.35 * page_area]
    rows = []
    for p in normal:
        t, b = span(p)
        for row in rows:
            rt, rb = row[0]
            if min(b, rb) - max(t, rt) > 0.5 * min(b - t, rb - rt):
                row[1].append(p)
                row[0] = (min(t, rt), max(b, rb))
                break
        else:
            rows.append([(t, b), [p]])
    ordered = [p for row in rows for p in sorted(row[1], key=lambda p: p[:, 0].mean())]
    for p in big:
        cy = p[:, 1].mean()
        index = sum(1 for q in ordered if q[:, 1].mean() < cy)
        ordered.insert(index, p)
    return ordered


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("pagina", help="imagem da página (png, jpg ou webp)")
    parser.add_argument("--episodio", required=True, help="pasta do episódio em assets/historia (ex.: ep00)")
    parser.add_argument("--nome", required=True, help="nome do arquivo sem extensão (ex.: pag1)")
    parser.add_argument("--conferencia", help="onde gravar a imagem de conferência (padrão: <nome>_quadros.jpg aqui)")
    args = parser.parse_args()

    img = cv2.imread(args.pagina, cv2.IMREAD_COLOR)
    if img is None:
        sys.exit("não consegui abrir " + args.pagina)
    h, w = img.shape[:2]
    panels = find_panels(img)
    if not panels:
        sys.exit("nenhum quadro encontrado")

    out_dir = os.path.join(ASSETS, args.episodio)
    os.makedirs(out_dir, exist_ok=True)
    jpg = args.nome + ".jpg"
    cv2.imwrite(os.path.join(out_dir, jpg), img, [cv2.IMWRITE_JPEG_QUALITY, 90])

    check = img.copy()
    quadros = []
    balloons = find_balloons(img)
    for b in balloons:
        cv2.polylines(check, [b["poly"].astype(np.int32).reshape(-1, 1, 2)], True, (255, 0, 255), 3)
        l, t, r, bt = [int(v) for v in b["box"]]
        cv2.rectangle(check, (l, t), (r, bt), (0, 200, 0), 2)
    for k, p in enumerate(panels):
        cv2.polylines(check, [p.astype(np.int32).reshape(-1, 1, 2)], True, (0, 0, 255), 4)
        cx, cy = p.mean(axis=0).astype(int)
        cv2.putText(check, str(k + 1), (cx - 18, cy + 18), cv2.FONT_HERSHEY_SIMPLEX, 2.0, (0, 255, 255), 6)
        area = []
        for x, y in p:
            area += [round(min(max(x / w, 0.0), 1.0), 4), round(min(max(y / h, 0.0), 1.0), 4)]
        quadro = {"area": area}
        mine = [b for b in balloons if inside(p, (float(b["center"][0]), float(b["center"][1])))]
        # Ordem de leitura dos balões: de cima para baixo e da esquerda para a direita.
        mine.sort(key=lambda b: (round(b["center"][1] / (h * 0.06)), b["center"][0]))
        if mine:
            quadro["baloes"] = []
            for b in mine:
                outline = []
                for x, y in b["poly"]:
                    outline += [round(x / w, 4), round(y / h, 4)]
                l, t, r, bt = b["box"]
                quadro["baloes"].append({"texto": "", "caixa": [round(l / w, 4), round(t / h, 4),
                                                             round(r / w, 4), round(bt / h, 4)],
                                         "area": outline})
        quadros.append(quadro)
    check_path = args.conferencia or (args.nome + "_quadros.jpg")
    cv2.imwrite(check_path, check)

    passo = {"tipo": "pagina", "imagem": jpg, "quadros": quadros}
    print(json.dumps(passo, ensure_ascii=False, indent=1))
    total = sum(len(q.get("baloes", [])) for q in quadros)
    print("\n%d quadros e %d balões de fala (preencha o \"texto\" de cada um). Página em %s; conferência em %s"
          % (len(panels), total, os.path.join(out_dir, jpg), check_path), file=sys.stderr)


if __name__ == "__main__":
    main()
