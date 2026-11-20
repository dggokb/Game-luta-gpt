#!/usr/bin/env python3
"""Monta a arte da Bolsa de Apostas (docs/historia/aposta.md) a partir das imagens do Codex:

- art/hud/apostas/letreiro_original.png (o letreiro do rodapé, o concept M1 aprovado em 20/11,
  fundo verde; o medalhão e a barra em magenta) vira duas peças em assets/ui/hud/apostas/:
  - faixa.png: só a faixa fina da borda de baixo, de ponta a ponta (o meio, onde o placar
    sobe, é preenchido com a faixa lisa);
  - letreiro.png: o placar que sobe no meio (com o pé da faixa embaixo), para o jogo pôr no vão
    entre o direcional e os botões; o verde e o magenta ficam transparentes;
  - letreiro.json: as medidas no letreiro.png: o hexágono do rosto, a barra, as duas placas do
    dinheiro, as duas plaquinhas da odd e onde a faixa começa (no letreiro e na faixa.png).
- art/vfx/aposta/{rastro,chegada,moeda_luz}.png (luz em fundo preto): o rastro da moeda que
  vai até a barra de Super, o estouro na chegada e a moeda com brilho; o preto vira
  transparente pelo brilho.

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
sys.path.insert(0, str(RAIZ / "tools/historia"))
import efeito_video as ev  # noqa: E402
from versus_arte import brilho_sobre_preto  # noqa: E402

HUD = RAIZ / "art/hud/apostas"
VFX = RAIZ / "art/vfx/aposta"
SAIDA = RAIZ / "android/app/src/main/assets/ui/hud/apostas"

# O placar no letreiro_original.png (2560x240): de onde o pé esquerdo sai da faixa até o direito.
PLACAR = (760, 1800)
# Coluna da faixa lisa (sem o brilho das pontas) que preenche o meio da faixa.
FAIXA_LISA = 600
# Escala de cada peça (a original é o dobro da tela de 1280).
ESCALA_LETREIRO, ESCALA_FAIXA = 0.75, 0.625
# Maior lado das peças do efeito.
LADO = {"rastro": 1024, "chegada": 512, "moeda_luz": 256}


def poligono(mascara, folga=0.02):
    cs, _ = cv2.findContours(mascara.astype(np.uint8), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    c = max(cs, key=cv2.contourArea)
    return cv2.approxPolyDP(c, folga * cv2.arcLength(c, True), True).reshape(-1, 2).astype(float)


def pedacos(mascara, area_minima):
    n, rot, st, _ = cv2.connectedComponentsWithStats(mascara.astype(np.uint8))
    return [(rot == i, st[i]) for i in range(1, n) if st[i, cv2.CC_STAT_AREA] >= area_minima]


def letreiro():
    rgb = np.array(Image.open(HUD / "letreiro_original.png").convert("RGB"))
    rgba = ev.chave(rgb, ev.cor_do_fundo([rgb]))
    r, g, b = (rgb[..., i].astype(int) for i in range(3))
    magenta = (r > 170) & (b > 170) & (g < 110)
    rgba[cv2.dilate(magenta.astype(np.uint8), np.ones((5, 5), np.uint8)) > 0, 3] = 0
    verde = rgba[..., 3] == 0
    # Onde a faixa começa (a primeira linha com desenho na coluna lisa).
    topo_faixa = int(np.argmax(~verde[:, FAIXA_LISA]))

    # A faixa: o meio vira a coluna lisa, e em cima dela tudo transparente.
    faixa = rgba.copy()
    x0, x1 = PLACAR
    faixa[:, x0:x1] = faixa[:, FAIXA_LISA:FAIXA_LISA + 1]
    faixa = faixa[topo_faixa - 4:]
    img = Image.fromarray(faixa)
    img = img.resize((round(img.width * ESCALA_FAIXA), round(img.height * ESCALA_FAIXA)), Image.LANCZOS)
    img.save(SAIDA / "faixa.png", optimize=True)

    # O placar, com o pé da faixa.
    placar = rgba[:, x0:x1]
    rr, gg, bb = r[:, x0:x1], g[:, x0:x1], b[:, x0:x1]
    mag = magenta[:, x0:x1]
    s = ESCALA_LETREIRO

    def em(pts):
        return [[round(x * s, 1), round(y * s, 1)] for x, y in pts]

    buracos = sorted(pedacos(mag, 300), key=lambda p: p[1][cv2.CC_STAT_TOP])
    if len(buracos) != 2:
        raise SystemExit(f"letreiro: achei {len(buracos)} áreas magenta, esperava 2 (o rosto e a barra)")
    escuro = (rr < 45) & (gg < 55) & (bb < 90)
    dinheiro, odd = [], []
    for _, st in pedacos(escuro, 800):
        x, y, w, h, _ = st
        caixa = [round(x * s, 1), round(y * s, 1), round((x + w) * s, 1), round((y + h) * s, 1)]
        if 50 <= h <= 90 and 250 <= w <= 450:
            dinheiro.append(caixa)
        elif 20 <= h <= 45 and 80 <= w <= 160:
            odd.append(caixa)
    if len(dinheiro) != 2 or len(odd) != 2:
        raise SystemExit(f"letreiro: {len(dinheiro)} placas de dinheiro e {len(odd)} de odd (esperava 2 e 2)")
    medidas = {
        "largura": round(placar.shape[1] * s), "altura": round(placar.shape[0] * s),
        "hexagono": em(poligono(buracos[0][0])), "barra": em(poligono(buracos[1][0], 0.004)),
        "dinheiro": sorted(dinheiro), "odd": sorted(odd),
        "topoFaixa": round(topo_faixa * s, 1),
        "faixaAltura": img.height, "faixaTopo": round(4 * ESCALA_FAIXA, 1),
    }
    Image.fromarray(placar).resize((medidas["largura"], medidas["altura"]), Image.LANCZOS).save(
        SAIDA / "letreiro.png", optimize=True)
    (SAIDA / "letreiro.json").write_text(json.dumps(medidas, indent=1) + "\n", encoding="utf-8")
    print("faixa.png", img.size, "letreiro.png", (medidas["largura"], medidas["altura"]))


def efeitos():
    for nome, lado in LADO.items():
        img = brilho_sobre_preto(Image.open(VFX / f"{nome}.png"))
        escala = lado / max(img.size)
        img = img.resize((round(img.width * escala), round(img.height * escala)), Image.LANCZOS)
        img.save(SAIDA / f"{nome}.png", optimize=True)
        print(f"{nome}.png", img.size)


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    letreiro()
    efeitos()


if __name__ == "__main__":
    main()
