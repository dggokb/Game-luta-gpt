#!/usr/bin/env python3
"""Sorteia os pontos dos flashes da plateia a partir da máscara e grava no stage.json.

    python3 tools/stage/pontos_plateia.py arena_central [arena_costeira ...]

Para cada faixa de "plateia" com "mascara" (a imagem só com a gente, que o
montar_porto.plateia gera), sorteia pontos onde a máscara é gente e grava em "flashes"
([[x, y], ...] em fração da imagem). O motor pisca flashes de câmera nesses pontos, então
eles nunca caem num estandarte ou numa luz. Rode de novo depois de trocar a arquibancada.
"""
import json
import random
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image

STAGES = Path(__file__).resolve().parents[2] / "android/app/src/main/assets/stages"
POR_FAIXA = 160


def pontos(mascara, topo, base, n, semente):
    a = np.asarray(Image.open(mascara).convert("RGBA"))[..., 3]
    h, w = a.shape
    ys, xs = np.nonzero(a[int(topo * h):int(base * h)] > 220)
    if len(xs) == 0:
        return []
    rnd = random.Random(semente)
    escolha = rnd.sample(range(len(xs)), min(n, len(xs)))
    return sorted([[round(xs[i] / w, 4), round((ys[i] + int(topo * h)) / h, 4)] for i in escolha])


def main():
    for cenario in sys.argv[1:]:
        arquivo = STAGES / cenario / "stage.json"
        texto = arquivo.read_text(encoding="utf-8")
        dados = json.loads(texto)
        for camada in dados["camadas"]:
            for k, faixa in enumerate(camada.get("plateia", [])):
                if "mascara" not in faixa:
                    continue
                lista = pontos(STAGES / cenario / faixa["mascara"], faixa["topo"], faixa["base"], POR_FAIXA, k)
                # Troca só a faixa no texto, para manter a formatação do resto do arquivo.
                velho = re.search(r'\{ "topo": %s, "base": %s,.*?\}(?=\s*[,\]])' % (
                    re.escape(json.dumps(faixa["topo"])), re.escape(json.dumps(faixa["base"]))), texto, re.S)
                campos = {k: v for k, v in faixa.items() if k not in ("topo", "base", "flashes")}
                novo = ('{ "topo": %s, "base": %s, %s,\n          "flashes": %s }'
                        % (json.dumps(faixa["topo"]), json.dumps(faixa["base"]),
                           ", ".join(f"{json.dumps(k)}: {json.dumps(v)}" for k, v in campos.items()),
                           json.dumps(lista, separators=(",", ":"))))
                texto = texto[:velho.start()] + novo + texto[velho.end():]
                print(f"{cenario} {camada['id']} faixa {k}: {len(lista)} pontos")
        json.loads(texto)
        arquivo.write_text(texto, encoding="utf-8")


if __name__ == "__main__":
    main()
