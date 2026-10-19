#!/usr/bin/env python3
"""Tira a bandeira pintada na imagem do pilar e salva ela separada, para o cenário balançar.

O GPT desenha o estandarte junto com o pilar. Para ele balançar ao vento (`bandeiras` no
stage.json, ver docs/stage-engine.md), o pano sai do pilar.png e vira bandeira.png. O
motor desenha a bandeira de volta no mesmo lugar, em faixas que ondulam.

Corte: os pixels visíveis dentro do retângulo, abaixo de uma linha logo embaixo da vara
(o topo do pano fica no pilar: ali a bandeira está presa e quase não se mexe, então a
emenda não aparece) e fora das áreas `fora` (a lanterna ao lado, por exemplo). A
bandeira leva `SOBRA` pixels acima do corte, que continuam no pilar: as duas bordas
filtradas se sobrepõem em vez de deixar um risco do fundo aparecer na emenda.

Rode uma vez depois de trocar o pilar.png de um cenário. O script recusa um pilar que
já foi separado (a área está vazia). No fim ele imprime o trecho de "bandeiras" para o
stage.json.

    python3 tools/stage/separar_bandeiras.py [cenario ...]
"""
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

SOBRA = 6
RAIZ = Path(__file__).resolve().parents[2]
STAGES = RAIZ / "android/app/src/main/assets/stages"

# Em pixels do pilar.png (ou do "arquivo"). corte: dois pontos da linha embaixo da vara.
BANDEIRAS = {
    "templo_lua": {
        "retangulo": (290, 380, 594, 1200),
        "corte": ((300, 425), (580, 500)),
        "fora": [],
    },
    "dojo_yinlu": {
        "retangulo": (380, 440, 652, 1125),
        "corte": ((390, 512), (600, 555)),
        # A lanterna à direita (com o brilho do papel) entra no retângulo até o pingente;
        # embaixo dela só as pontas da bandeira chegam tão longe.
        "fora": [(582, 440, 652, 830)],
    },
    # O estandarte do poste do cais (o poste.png já cortado pelo montar_porto.py).
    "porto_secreto": {
        "arquivo": "poste.png",
        "bandeira": "bandeira_poste.png",
        "retangulo": (185, 430, 356, 1092),
        "corte": ((190, 466), (350, 466)),
        "fora": [],
    },
}


def mascara(img, cfg, sobra=0):
    h, w = img.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    x0, y0, x1, y1 = cfg["retangulo"]
    (xa, ya), (xb, yb) = cfg["corte"]
    linha = ya + (xx - xa) * (yb - ya) / float(xb - xa) - sobra
    m = (xx >= x0) & (xx < x1) & (yy >= y0) & (yy < y1) & (yy >= linha) & (img[..., 3] > 0)
    for fx0, fy0, fx1, fy1 in cfg["fora"]:
        m &= ~((xx >= fx0) & (xx < fx1) & (yy >= fy0) & (yy < fy1))
    return m


def separar(cenario):
    cfg = BANDEIRAS[cenario]
    pasta = STAGES / cenario
    pilar = pasta / cfg.get("arquivo", "pilar.png")
    nome_bandeira = cfg.get("bandeira", "bandeira.png")
    img = np.array(Image.open(pilar).convert("RGBA"))
    m = mascara(img, cfg)
    if m.sum() < 2000:
        raise SystemExit(f"{cenario}: o {pilar.name} já está sem bandeira (rode só uma vez)")
    pano = mascara(img, cfg, SOBRA)
    ys, xs = np.nonzero(pano)
    x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1

    bandeira = np.zeros((y1 - y0, x1 - x0, 4), np.uint8)
    recorte = pano[y0:y1, x0:x1]
    bandeira[recorte] = img[y0:y1, x0:x1][recorte]
    Image.fromarray(bandeira).save(pasta / nome_bandeira, optimize=True)

    sem = img.copy()
    sem[m] = 0
    Image.fromarray(sem).save(pilar, optimize=True)

    h, w = img.shape[:2]
    return {
        "imagem": nome_bandeira,
        "x": round(float(x0 / w), 4),
        "y": round(float(y0 / h), 4),
        "largura": round(float((x1 - x0) / w), 4),
        "altura": round(float((y1 - y0) / h), 4),
    }


def main():
    cenarios = sys.argv[1:] or sorted(BANDEIRAS)
    for cenario in cenarios:
        print(cenario, json.dumps(separar(cenario), ensure_ascii=False))


if __name__ == "__main__":
    main()
