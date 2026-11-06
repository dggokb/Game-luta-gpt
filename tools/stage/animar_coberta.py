#!/usr/bin/env python3
"""Anima a Arena Coberta (``stages/arena_coberta``), montada pelo ``cenario_pintura.py``.

    python3 tools/stage/animar_coberta.py [pasta]

* **telão**: a tela do telão da pintura recebe a luta ao vivo e o replay (``"telao"`` na camada);
* **plateia**: a gente das arquibancadas pula de leve e solta flashes de câmera;
* **luzes**: os holofotes do teto pulsam de leve;
* **chão**: encerado, com o reflexo dos lutadores.

As posições são em pixels do panorama de 4096 (``art/stages/arena_coberta/2_panorama_4096.webp``);
a parede tem ``EXTRA`` linhas de teto escuro em cima.
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
import pontos_plateia  # noqa: E402
from montar_porto import plateia as mascara_plateia  # noqa: E402

PASTA = AQUI.parents[1] / "android/app/src/main/assets/stages/arena_coberta"
EXTRA = 500                                   # --ceu-extra do cenario_pintura.py
TELA = (1785, 227, 2238, 400)                 # a tela azul do telão (x0, y0, x1, y1) no panorama
PLATEIA = (250, 455)                          # faixa das arquibancadas (linhas do panorama)
MAX_LUZES = 40


def holofotes(img):
    """Os holofotes do teto: pontos brancos-amarelados no alto, os maiores primeiro."""
    h, w = img.shape[:2]
    mn = img[..., :3].min(axis=2)
    m = (mn > 225).astype(np.uint8)
    m[:EXTRA] = 0
    m[EXTRA + 300:] = 0
    m = cv2.dilate(m, np.ones((5, 5), np.uint8))
    n, _, st, cen = cv2.connectedComponentsWithStats(m, 8)
    achados = sorted([(st[i, cv2.CC_STAT_AREA], st[i], cen[i]) for i in range(1, n) if st[i, cv2.CC_STAT_AREA] >= 40],
                     key=lambda t: -t[0])[:MAX_LUZES]
    return [{"x": round(float(c[0] / w), 4), "y": round(float(c[1] / h), 4),
             "raio": round(float(max(s[2], s[3]) * 1.6 / h), 4), "cor": "#FFE8B8", "pulsar": 0.05,
             "reflexo": False} for _, s, c in achados]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", nargs="?", type=Path, default=PASTA)
    pasta = ap.parse_args().pasta
    stage_path = pasta / "stage.json"
    stage = json.loads(stage_path.read_text(encoding="utf-8"))
    parede = next(c for c in stage["camadas"] if c["id"] == "parede")
    imagem = Image.open(pasta / parede["imagem"]).convert("RGBA")
    w, h = imagem.size
    img = np.asarray(imagem)

    x0, y0, x1, y1 = TELA
    parede["telao"] = {"x": round(x0 / w, 4), "y": round((y0 + EXTRA) / h, 4),
                       "largura": round((x1 - x0) / w, 4), "altura": round((y1 - y0) / h, 4)}

    topo, base = (PLATEIA[0] + EXTRA) / h, (PLATEIA[1] + EXTRA) / h
    gente = mascara_plateia(imagem, [(topo, base)])
    al = np.asarray(gente)[..., 3].copy()
    tx0, tx1 = int(x0 - 0.02 * w), int(x1 + 0.02 * w)     # o telão e a treliça não pulam
    al[:, tx0:tx1] = 0
    gente.putalpha(Image.fromarray(al))
    gente.save(pasta / "parede_plateia.webp", quality=90, alpha_quality=100, method=6)
    flashes = pontos_plateia.pontos(pasta / "parede_plateia.webp", topo, base, 220, 11)
    parede["plateia"] = [{"topo": round(topo, 4), "base": round(base, 4), "mascara": "parede_plateia.webp",
                          "pulo": 0.003, "colunas": 200, "flashes": flashes}]
    parede["luzes"] = holofotes(img)

    stage["chao"]["reflexoLutadores"] = 0.22
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"telão {parede['telao']}, plateia com {len(flashes)} flashes, {len(parede['luzes'])} holofotes")


if __name__ == "__main__":
    main()
