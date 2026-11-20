#!/usr/bin/env python3
"""Monta a arte da Bolsa de Apostas (docs/historia/aposta.md) a partir das imagens do Codex em
art/hud/apostas/:

- placa_original.png (fundo verde; o medalhão e os dois medidores em magenta) →
  assets/ui/hud/apostas/placa.png, com o verde e o magenta transparentes, recortada no desenho,
  e placa.json com as medidas (em pixels da placa.png): o hexágono do rosto, os dois medidores,
  as duas placas do dinheiro e as duas plaquinhas da odd. O jogo desenha o rosto e os medidores
  por baixo da arte, e os números por cima.
- fichas.png (fundo verde, 8 peças em duas linhas) → uma imagem por peça:
  pilha_azul, pilha_vermelha, ficha_azul, ficha_vermelha, moeda, voa_azul, voa_vermelha, brilho.

    python3 tools/hud/montar_apostas.py
"""
import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

RAIZ = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(RAIZ / "tools/sprites"))
import efeito_video as ev  # noqa: E402

ORIGEM = RAIZ / "art/hud/apostas"
SAIDA = RAIZ / "android/app/src/main/assets/ui/hud/apostas"
# A placa no jogo tem ~460 de largura (tela virtual 1280): a imagem fica com o dobro.
LARGURA_PLACA = 1000
# Maior lado de cada ficha.
LADO_FICHA = 160
FICHAS = ["pilha_azul", "pilha_vermelha", "ficha_azul", "ficha_vermelha",
          "moeda", "voa_azul", "voa_vermelha", "brilho"]


def sem_verde(rgb):
    return ev.chave(rgb, ev.cor_do_fundo([rgb]))


def poligono(mascara):
    cs, _ = cv2.findContours(mascara.astype(np.uint8), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    c = max(cs, key=cv2.contourArea)
    return cv2.approxPolyDP(c, 0.02 * cv2.arcLength(c, True), True).reshape(-1, 2).astype(float)


def pedacos(mascara, area_minima):
    n, rot, st, _ = cv2.connectedComponentsWithStats(mascara.astype(np.uint8))
    return [(rot == i, st[i]) for i in range(1, n) if st[i, cv2.CC_STAT_AREA] >= area_minima]


def placa():
    rgb = np.array(Image.open(ORIGEM / "placa_original.png").convert("RGB"))
    r, g, b = (rgb[..., i].astype(int) for i in range(3))
    magenta = (r > 170) & (b > 170) & (g < 110)
    # Os buracos crescem 2 px: some a franja rosa, e o que o jogo desenha encosta na tinta.
    furo = cv2.dilate(magenta.astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
    rgba = sem_verde(rgb)
    rgba[furo, 3] = 0
    ys, xs = np.where(rgba[..., 3] > 0)
    x0, x1, y0, y1 = xs.min() - 4, xs.max() + 5, ys.min() - 4, ys.max() + 5
    escala = LARGURA_PLACA / (x1 - x0)

    def em(p):
        return [[round((x - x0) * escala, 1), round((y - y0) * escala, 1)] for x, y in p]

    buracos = sorted(pedacos(magenta, 2000), key=lambda p: p[1][cv2.CC_STAT_AREA], reverse=True)
    hexagono = poligono(buracos[0][0])
    medidores = sorted((poligono(m) for m, _ in buracos[1:3]), key=lambda p: p[:, 0].min())
    # As placas escuras (azul-marinho) por dentro das asas: a do dinheiro (grande) e a da odd.
    marinho = (r < 40) & (g < 50) & (b > 30) & (b < 90)
    placas = []
    for m, st in pedacos(marinho, 3000):
        x, y, w, h, _ = st
        if 60 <= h <= 90 and w > 500:
            placas.append(("dinheiro", [x, y, x + w, y + h]))
        elif 30 <= h <= 55 and 120 <= w <= 220:
            placas.append(("odd", [x, y, x + w, y + h]))
    medidas = {"largura": LARGURA_PLACA, "altura": round((y1 - y0) * escala),
               "hexagono": em(hexagono), "medidor": [em(medidores[0]), em(medidores[1])]}
    for nome in ("dinheiro", "odd"):
        caixas = sorted((c for n, c in placas if n == nome), key=lambda c: c[0])
        if len(caixas) != 2:
            raise SystemExit(f"placa: achei {len(caixas)} placas de {nome}, esperava 2")
        medidas[nome] = [sum(em([c[:2], c[2:]]), []) for c in caixas]
    img = Image.fromarray(rgba[y0:y1, x0:x1]).resize((medidas["largura"], medidas["altura"]), Image.LANCZOS)
    img.save(SAIDA / "placa.png", optimize=True)
    (SAIDA / "placa.json").write_text(json.dumps(medidas, indent=1) + "\n", encoding="utf-8")
    print("placa.png", img.size)


def fichas():
    rgb = np.array(Image.open(ORIGEM / "fichas.png").convert("RGB"))
    rgba = sem_verde(rgb)
    # Peças juntas: os brilhos soltos viram uma peça só.
    junto = cv2.dilate((rgba[..., 3] > 0).astype(np.uint8), np.ones((41, 41), np.uint8))
    achados = [st for _, st in pedacos(junto, 4000)]
    if len(achados) != len(FICHAS):
        raise SystemExit(f"fichas: achei {len(achados)} peças, esperava {len(FICHAS)}")
    meio = rgb.shape[0] / 2
    achados.sort(key=lambda s: (s[1] + s[3] / 2 > meio, s[0]))
    for nome, (x, y, w, h, _) in zip(FICHAS, achados):
        peca = rgba[y:y + h, x:x + w]
        ys, xs = np.where(peca[..., 3] > 0)
        peca = peca[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
        img = Image.fromarray(peca)
        escala = LADO_FICHA / max(img.size)
        img = img.resize((max(1, round(img.width * escala)), max(1, round(img.height * escala))), Image.LANCZOS)
        img.save(SAIDA / f"{nome}.png", optimize=True)
        print(f"{nome}.png", img.size)


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    placa()
    fichas()


if __name__ == "__main__":
    main()
