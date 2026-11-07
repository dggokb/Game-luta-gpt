#!/usr/bin/env python3
"""Anima o dojo da Yin Lu (``stages/dojo_yinlu``), montado pelo ``cenario_pintura.py`` a partir do
concept sem os pilares (``art/stages/dojo_yinlu/9_panorama_4096.webp``).

    python3 tools/stage/animar_yinlu.py [pasta]

* **lanternas**: as quatro lanternas de pedra e as duas do portão brilham e tremulam de leve;
* **raios de sol**: fachos de luz entre as árvores, em duas imagens que cruzam uma na outra
  (``"onda"`` em fase oposta), como o sol passando pelas folhas que o vento mexe;
* **poeira**: grãos dourados de pó flutuando na luz;
* **vento nas árvores**: as copas viram a camada ``arvores`` (recortadas da parede, que fica com
  o céu no lugar delas) com ``"vento"``: o tronco parado, os galhos como molas que o vento empurra
  para um lado e que voltam devagar, e as folhas indo junto com o galho;
* as folhas caindo e a luz do sol nos lutadores vêm do ``stage_extra.json``.

As posições são em pixels da ``parede.jpg`` (4096 de largura).
"""
import argparse
import json
from pathlib import Path

import cv2
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
TELHADO = 708        # linha da parede onde fica o telhado do muro: as árvores ficam acima dela
ARVORE_TOPO = 250    # acima disso é o céu esticado (borrado), que não mexe
PORTAO = (1620, 500, 2470, TELHADO)   # o telhado do portão não balança
VENTO_GRUPOS = 28    # galhos (molas)
VENTO_AMP = 0.0085   # deslocamento na ponta, em fração da altura da imagem das árvores


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


def arvores(pasta, parede, w, h):
    """Recorta as copas da parede numa camada com vento e põe o céu no lugar delas."""
    img = cv2.imread(str(pasta / parede["imagem"]))
    b, g, r = [img[..., i].astype(np.int16) for i in range(3)]
    ceu = (b > r + 35) & (b > g + 10)
    nuvem = (b > 200) & (g > 200) & (r > 200) & (np.abs(b - r) < 40)
    arv = (~ceu & ~nuvem).astype(np.uint8)
    arv[:ARVORE_TOPO] = 0
    arv[TELHADO - 3:] = 0
    x0, y0, x1, y1 = PORTAO
    arv[y0:y1, x0:x1] = 0
    arv = cv2.morphologyEx(arv, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    folha = ((g > b + 25) & (g > r * 0.75)).astype(np.uint8) & arv
    madeira = arv & (1 - folha)
    tronco = cv2.morphologyEx(madeira, cv2.MORPH_OPEN, np.ones((15, 15), np.uint8))
    n, lab, st, _ = cv2.connectedComponentsWithStats(tronco, 8)
    tronco = np.isin(lab, [i for i in range(1, n) if st[i, 4] > 1200]).astype(np.uint8)
    tronco[: (ARVORE_TOPO + TELHADO) // 2] = 0
    # Alcance: 0 no tronco, 1 na ponta dos galhos; some perto do portão, do telhado e do céu borrado.
    alcance = np.clip(cv2.distanceTransform(1 - tronco, cv2.DIST_L2, 5) / 380.0, 0, 1) ** 1.2
    portao = np.zeros_like(arv); portao[y0:y1, x0:x1] = 1
    alcance *= np.clip(cv2.distanceTransform(1 - portao, cv2.DIST_L2, 5) / 100.0, 0, 1)
    yy = np.arange(h, dtype=np.float32)[:, None]
    alcance *= np.clip((yy - ARVORE_TOPO) / 80.0, 0, 1) * np.clip((TELHADO - yy) / 40.0, 0, 1)
    # Atrás das copas (aparece só nos poucos pixels que elas andam): dentro, a própria copa
    # borrada; na borda com o céu, uma faixa fina com o céu continuado (inpaint só na faixa).
    tampa = cv2.dilate(arv, np.ones((9, 9), np.uint8))
    dentro = cv2.erode(tampa, np.ones((15, 15), np.uint8))
    borda = (tampa & (1 - dentro)).astype(np.uint8)
    borrada = cv2.GaussianBlur(img.astype(np.float32), (0, 0), 6)
    sem = img.copy()
    sem[dentro > 0] = borrada[dentro > 0].astype(np.uint8)
    sem = cv2.inpaint(sem, borda, 4, cv2.INPAINT_TELEA)
    m3 = cv2.GaussianBlur(tampa.astype(np.float32), (0, 0), 1.5)[..., None]
    sem = (img * (1 - m3) + sem * m3).astype(np.uint8)
    cv2.imwrite(str(pasta / parede["imagem"]), sem, [cv2.IMWRITE_JPEG_QUALITY, 92])
    # A camada: só a faixa das árvores, com o alfa macio.
    a = cv2.GaussianBlur(arv.astype(np.float32), (0, 0), 0.8)
    rgba = np.dstack([img[..., 2], img[..., 1], img[..., 0], (a * 255).astype(np.uint8)])[ARVORE_TOPO:TELHADO]
    Image.fromarray(rgba, "RGBA").save(pasta / "arvores.png", optimize=True)
    hh = TELHADO - ARVORE_TOPO
    cols, rows = 96, 12
    al = alcance[ARVORE_TOPO:TELHADO]
    al = cv2.GaussianBlur(al, (0, 0), 20)
    reach = [round(float(al[min(hh - 1, int(j * hh / rows)), min(w - 1, int(i * w / cols))]), 3)
             for j in range(rows + 1) for i in range(cols + 1)]
    ys, xs = np.nonzero(arv[ARVORE_TOPO:TELHADO])
    pts = np.float32(np.column_stack([xs, ys]))[::25]
    _, _, cent = cv2.kmeans(pts, VENTO_GRUPOS, None, (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.5),
                            3, cv2.KMEANS_PP_CENTERS)
    rng = np.random.default_rng(5)
    grupos = [[round(float(cx / w), 4), round(float(cy / hh), 4)] for cx, cy in cent]
    freq = [round(float(f), 3) for f in rng.uniform(0.45, 0.7, VENTO_GRUPOS)]
    t, b_ = parede["topo"], parede["base"]
    return {"id": "arvores", "imagem": "arvores.png", "profundidade": round(parede["profundidade"] - 0.3, 1),
            "topo": round(t + (b_ - t) * ARVORE_TOPO / h, 4), "base": round(t + (b_ - t) * TELHADO / h, 4),
            "x": parede["x"], "repetir": "espelhar",
            "vento": {"colunas": cols, "linhas": rows, "alcance": reach, "grupos": grupos, "freq": freq,
                      "amplitude": VENTO_AMP * h / hh, "amortecimento": 0.35, "raio": 0.018}}


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
    # A parede com as árvores pintadas: recém-saída do cenario_pintura.py (sem a camada "arvores"
    # ainda), vai para a pasta de arte; senão volta de lá (para recortar sempre da pintura inteira).
    inteira = AQUI.parents[1] / "art/stages/dojo_yinlu/parede_com_arvores.jpg"
    if any(c["id"] == "arvores" for c in stage["camadas"]):
        (pasta / parede["imagem"]).write_bytes(inteira.read_bytes())
    else:
        inteira.write_bytes((pasta / parede["imagem"]).read_bytes())
    camadas = [c for c in stage["camadas"] if not c["id"].startswith(("raios-", "arvores"))]
    camadas.append(arvores(pasta, parede, w, h))
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
