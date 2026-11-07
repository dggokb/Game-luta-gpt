#!/usr/bin/env python3
"""Anima o dojo da Yin Lu (``stages/dojo_yinlu``), montado pelo ``cenario_pintura.py`` a partir do
concept sem os pilares (``art/stages/dojo_yinlu/9_panorama_4096.webp``).

    python3 tools/stage/animar_yinlu.py [pasta]

* **lanternas**: as quatro lanternas de pedra e as duas do portão brilham e tremulam de leve;
* **raios de sol**: fachos de luz entre as árvores, em duas imagens que cruzam uma na outra
  (``"onda"`` em fase oposta), como o sol passando pelas folhas que o vento mexe;
* **poeira**: grãos dourados de pó flutuando na luz;
* as folhas caindo e a luz do sol nos lutadores vêm do ``stage_extra.json``.

As posições são em pixels da ``parede.jpg`` (4096 de largura).
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image

AQUI = Path(__file__).resolve().parent
PASTA = AQUI.parents[1] / "android/app/src/main/assets/stages/dojo_yinlu"
# (x, y, raio) em px da parede: as janelas das lanternas de pedra e as lanternas do portão.
LANTERNAS = [(506, 836, 26), (1290, 836, 26), (2809, 836, 26), (3568, 836, 26),
             (1802, 773, 34), (2294, 773, 34)]
RAIOS = [(330, 4), (900, -2), (1500, 5), (2620, -4), (3150, 3), (3720, -3)]   # (x no alto, graus)
ANGULO = 24          # graus: o sol vem do alto à esquerda
ALFA_RAIO = 0.5


def raios(pasta, w, h, chao_y):
    """Os fachos de sol, pintados em 1/4 da resolução, em duas versões (o feixe um pouco deslocado
    e mais ou menos forte); as duas camadas com "onda" em fase oposta cruzam e a luz respira."""
    e = 4
    fw, fh = w // e, h // e
    yy, xx = np.mgrid[0:fh, 0:fw].astype(np.float32)
    for nome, desvio in (("a", -1.0), ("b", 1.0)):
        luz = np.zeros((fh, fw), np.float32)
        for i, (x0, ang) in enumerate(RAIOS):
            a = np.radians(ANGULO + ang * desvio * 0.6)
            dx, dy = np.sin(a), np.cos(a)
            px, py = xx * e - (x0 + desvio * 40 * (1 if i % 2 else -1)), yy * e - 250
            ao_longo = px * dx + py * dy
            lado = px * dy - py * dx
            meia = 55 + ao_longo * 0.12
            f = np.exp(-0.5 * (lado / np.maximum(meia, 1)) ** 2) * (ao_longo > 0)
            f *= np.clip(ao_longo / 200, 0, 1) * np.clip((chao_y - yy * e) / 120, 0, 1)
            luz = np.maximum(luz, f * (0.7 + 0.3 * ((i + (desvio > 0)) % 2)))
        rgb = np.dstack([np.full_like(luz, 255), np.full_like(luz, 240), np.full_like(luz, 196)])
        img = np.dstack([rgb, np.clip(luz * ALFA_RAIO, 0, 1) * 255]).astype(np.uint8)
        Image.fromarray(img, "RGBA").save(pasta / f"raios_{nome}.png", optimize=True)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", nargs="?", type=Path, default=PASTA)
    pasta = ap.parse_args().pasta
    stage_path = pasta / "stage.json"
    stage = json.loads(stage_path.read_text(encoding="utf-8"))
    parede = next(c for c in stage["camadas"] if c["id"] == "parede")
    w, h = Image.open(pasta / parede["imagem"]).size
    parede["luzes"] = [{"x": round(x / w, 4), "y": round(y / h, 4), "raio": round(r / h, 4),
                        "cor": "#FFC860", "pulsar": 0.12, "reflexo": False} for x, y, r in LANTERNAS]
    raios(pasta, w, h, 960)
    camadas = [c for c in stage["camadas"] if not c["id"].startswith("raios-")]
    for nome, fase in (("a", 0.0), ("b", 3.1416)):
        camadas.append({"id": f"raios-{nome}", "imagem": f"raios_{nome}.png",
                        "profundidade": round(parede["profundidade"] - 0.5, 1), "topo": parede["topo"],
                        "base": parede["base"], "x": parede["x"], "repetir": "espelhar",
                        "onda": {"amplitude": 0.8, "velocidade": 0.35, "fase": fase, "leve": 0.5}})
    stage["camadas"] = camadas
    stage["poeira"] = {"quantidade": 120, "cor": "#FFF2C8", "alfa": 0.45, "tamanho": 2.2, "vento": 10,
                       "perto": -200, "longe": 1500, "topo": 0.15, "base": 0.8}
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(LANTERNAS)} lanternas, {len(RAIOS)} raios de sol, poeira")


if __name__ == "__main__":
    main()
