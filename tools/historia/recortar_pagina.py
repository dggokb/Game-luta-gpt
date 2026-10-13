#!/usr/bin/env python3
"""Prepara uma página de mangá pronta para o modo história.

Acha os quadros pelas divisas claras (brancas) entre eles, grava a página em JPG na pasta
do episódio e imprime o passo do episodio.json com o contorno de cada quadro, na ordem de
leitura (linhas de cima para baixo, quadros da esquerda para a direita). Também grava uma
imagem de conferência com os quadros numerados.

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
    for k, p in enumerate(panels):
        cv2.polylines(check, [p.astype(np.int32).reshape(-1, 1, 2)], True, (0, 0, 255), 4)
        cx, cy = p.mean(axis=0).astype(int)
        cv2.putText(check, str(k + 1), (cx - 18, cy + 18), cv2.FONT_HERSHEY_SIMPLEX, 2.0, (0, 255, 255), 6)
        area = []
        for x, y in p:
            area += [round(min(max(x / w, 0.0), 1.0), 4), round(min(max(y / h, 0.0), 1.0), 4)]
        quadros.append({"area": area})
    check_path = args.conferencia or (args.nome + "_quadros.jpg")
    cv2.imwrite(check_path, check)

    passo = {"tipo": "pagina", "imagem": jpg, "quadros": quadros}
    text = json.dumps(passo, ensure_ascii=False)
    print(text.replace('{"area"', '\n  {"area"').replace("]}]}", "]}\n]}"))
    print("\n%d quadros. Página em %s; conferência em %s" % (len(panels), os.path.join(out_dir, jpg), check_path),
          file=sys.stderr)


if __name__ == "__main__":
    main()
