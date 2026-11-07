#!/usr/bin/env python3
"""Anima o dojo da Yin Lu (``stages/dojo_yinlu``), montado pelo ``cenario_pintura.py`` a partir do
concept sem os pilares (``art/stages/dojo_yinlu/9_panorama_4096.webp``).

    python3 tools/stage/animar_yinlu.py [pasta]

* **lanternas**: as quatro lanternas de pedra e as duas do portão brilham e tremulam de leve;
* **raios de sol**: fachos de luz entre as árvores, em duas imagens que cruzam uma na outra
  (``"onda"`` em fase oposta), como o sol passando pelas folhas que o vento mexe;
* **poeira**: grãos dourados de pó flutuando na luz;
* **planos** (``planos_yinlu.py``, rodado antes): céu, nuvens andando, árvores e muro separados;
* **vento nas árvores**: o plano das árvores ganha ``"vento"``: o tronco parado, os galhos como
  molas que o vento empurra para um lado e que voltam devagar, e as folhas indo junto;
* **borlas**: as lanternas do portão ficam num poste (paradas); as borlas vermelhas penduradas
  embaixo delas balançam com o vento (``"pendulos"`` no ``"vento"``), presas pelo cordão;
* **árvores do pátio de dentro**: as copas vistas pelo portão, com o mesmo vento, mais fraco;
* as folhas caindo e a luz do sol nos lutadores vêm do ``stage_extra.json``.

As posições são em pixels da ``parede.jpg`` (4096 de largura).
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

import planos_yinlu

AQUI = Path(__file__).resolve().parent
PASTA = AQUI.parents[1] / "android/app/src/main/assets/stages/dojo_yinlu"
# (x, y, raio) em px da parede: as janelas das lanternas de pedra e as lanternas do portão.
LANTERNAS = [(506, 836, 26), (1290, 836, 26), (2809, 836, 26), (3568, 836, 26),
             (1802, 773, 34), (2294, 773, 34)]
BORLAS = [(1801, 792, 1814, 834), (2286, 792, 2299, 835)]   # embaixo das lanternas do portão
BORLA_GRAUS = 16.0
TEMPLO = (1974, 750, 2120, 872)   # o templo do fundo e a escada: não mexem
DENTRO_AMP = 0.035
RAIOS = [(330, 4), (900, -2), (1500, 5), (2620, -4), (3150, 3), (3720, -3)]   # (x no alto, graus)
ANGULO = 24          # graus: o sol vem do alto à esquerda
ALFA_RAIO = 0.5
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


def tirar(img, arv):
    """A parede sem o que está na máscara. Atrás (aparece só nos poucos pixels que a peça anda):
    dentro, ela mesma borrada; na borda, uma faixa fina continuada (inpaint só na faixa)."""
    tampa = cv2.dilate(arv, np.ones((9, 9), np.uint8))
    dentro = cv2.erode(tampa, np.ones((15, 15), np.uint8))
    borda = (tampa & (1 - dentro)).astype(np.uint8)
    borrada = cv2.GaussianBlur(img.astype(np.float32), (0, 0), 6)
    sem = img.copy()
    sem[dentro > 0] = borrada[dentro > 0].astype(np.uint8)
    sem = cv2.inpaint(sem, borda, 4, cv2.INPAINT_TELEA)
    m3 = cv2.GaussianBlur(tampa.astype(np.float32), (0, 0), 1.5)[..., None]
    return (img * (1 - m3) + sem * m3).astype(np.uint8)


def posicao(parede, w, h, x0, y0, x1, y1):
    """topo, base e x (frações da tela de repouso) de um retângulo da parede."""
    t, b = parede["topo"], parede["base"]
    alt = (b - t) * 720.0                       # altura da parede na tela (720 de altura)
    cx = parede["x"] * 1280.0 + ((x0 + x1) / 2 - w / 2) / h * alt
    return {"topo": round(t + (b - t) * y0 / h, 5), "base": round(t + (b - t) * y1 / h, 5),
            "x": round(cx / 1280.0, 5)}


def vento_arvores(pasta, camada, h):
    """O vento no plano das árvores (``arvores.png`` do ``planos_yinlu.py``): os troncos (a
    madeira grossa da metade de baixo) não mexem, os galhos são molas e as folhas vão junto."""
    rgba = cv2.imread(str(pasta / camada["imagem"]), cv2.IMREAD_UNCHANGED)
    hh, w = rgba.shape[:2]
    b, g, r = [rgba[..., i].astype(np.int16) for i in range(3)]
    arv = (rgba[..., 3] > 128).astype(np.uint8)
    folha = ((g > b + 25) & (g > r * 0.75)).astype(np.uint8) & arv
    madeira = arv & (1 - folha)
    tronco = cv2.morphologyEx(madeira, cv2.MORPH_OPEN, np.ones((15, 15), np.uint8))
    n, lab, st, _ = cv2.connectedComponentsWithStats(tronco, 8)
    tronco = np.isin(lab, [i for i in range(1, n) if st[i, 4] > 1200]).astype(np.uint8)
    tronco[: hh // 2] = 0
    tronco[hh - 60:] = 1                                  # o pé (atrás do muro) fica parado
    alcance = np.clip(cv2.distanceTransform(1 - tronco, cv2.DIST_L2, 5) / 380.0, 0, 1) ** 1.2
    cols, rows = 96, 30
    al = cv2.GaussianBlur(alcance, (0, 0), 20)
    reach = [round(float(al[min(hh - 1, int(j * hh / rows)), min(w - 1, int(i * w / cols))]), 3)
             for j in range(rows + 1) for i in range(cols + 1)]
    ys, xs = np.nonzero(arv & (alcance > 0.2).astype(np.uint8))
    pts = np.float32(np.column_stack([xs, ys]))[::40]
    _, _, cent = cv2.kmeans(pts, VENTO_GRUPOS, None, (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.5),
                            3, cv2.KMEANS_PP_CENTERS)
    rng = np.random.default_rng(5)
    camada["vento"] = {"colunas": cols, "linhas": rows, "alcance": reach,
                       "grupos": [[round(float(cx / w), 4), round(float(cy / hh), 4)] for cx, cy in cent],
                       "freq": [round(float(f), 3) for f in rng.uniform(0.45, 0.7, VENTO_GRUPOS)],
                       # o mesmo deslocamento de antes (8 px da parede na ponta), nesta altura
                       "amplitude": round(VENTO_AMP * h / hh, 5), "amortecimento": 0.35, "raio": 0.018}


def arvores_dentro(pasta, parede, w, h):
    """As copas do pátio de dentro (na camada ``patio`` do ``planos_yinlu.py``): a mesma mola dos
    galhos, mais fraca (estão longe); o templo e o chão parados."""
    x0, y0, x1, y1 = planos_yinlu.PATIO
    img = cv2.imread(str(pasta / "patio.jpg"))
    hh, ww = img.shape[:2]
    b, g, r = [img[..., i].astype(np.int16) for i in range(3)]
    ceu = (b > r + 35) & (b > g + 10)
    nuvem = (b > 190) & (g > 190) & (r > 190) & (np.abs(b - r) < 45)
    folha = (~ceu & ~nuvem).astype(np.uint8)
    tx0, ty0, tx1, ty1 = TEMPLO
    folha[ty0 - y0:, tx0 - x0:tx1 - x0] = 0                         # o templo e o caminho
    folha[hh - 22:] = 0                                              # o chão
    folha = cv2.morphologyEx(folha, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    sem = tirar(img, folha)
    cv2.imwrite(str(pasta / "patio.jpg"), sem, [cv2.IMWRITE_JPEG_QUALITY, 94])
    a = cv2.GaussianBlur(folha.astype(np.float32), (0, 0), 0.8)
    rgba = np.dstack([img[..., 2], img[..., 1], img[..., 0], (a * 255).astype(np.uint8)])
    Image.fromarray(rgba, "RGBA").save(pasta / "arvores_dentro.png", optimize=True)
    cols, rows = 24, 12
    yy, xx = np.mgrid[0:rows + 1, 0:cols + 1].astype(np.float32)
    py, px = yy * hh / rows, xx * ww / cols
    alcance = np.clip((hh - 30 - py) / 90.0, 0, 1)                     # o pé das copas parado
    alcance *= np.clip(np.minimum(px, ww - px) / 10.0, 0, 1) * np.clip(py / 10.0, 0, 1)   # as bordas
    gx, gy = px + x0, py + y0                                          # o templo
    fora = np.hypot(np.maximum(np.maximum(tx0 - gx, gx - tx1), 0), np.maximum(ty0 - gy, 0))
    alcance *= np.clip(fora / 12.0, 0, 1)
    reach = [round(float(v), 3) for v in alcance.ravel()]
    ys, xs = np.nonzero(folha)
    pts = np.float32(np.column_stack([xs, ys]))[::5]
    _, _, cent = cv2.kmeans(pts, 8, None, (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.5),
                            3, cv2.KMEANS_PP_CENTERS)
    rng = np.random.default_rng(9)
    grupos = [[round(float(cx / ww), 4), round(float(cy / hh), 4)] for cx, cy in cent]
    freq = [round(float(f), 3) for f in rng.uniform(0.5, 0.75, len(grupos))]
    return {"id": "arvores-dentro", "imagem": "arvores_dentro.png",
            "profundidade": planos_yinlu.PROF_PATIO - 0.1, **posicao(parede, w, h, x0, y0, x1, y1),
            "apoio": parede["profundidade"],
            "vento": {"colunas": cols, "linhas": rows, "alcance": reach, "grupos": grupos, "freq": freq,
                      "amplitude": DENTRO_AMP, "amortecimento": 0.35, "raio": 0.1}}


def borlas(img, pasta, parede, w, h):
    """As borlas vermelhas embaixo das lanternas do portão: recortadas pela cor (o corpo vermelho
    e o cordão até a lanterna), cada uma um pêndulo preso no alto do cordão. Atrás delas o poste
    escuro continua de cima para baixo (cada coluna ligando o que está acima e abaixo)."""
    camadas = []
    for k, (bx0, by0, bx1, by1) in enumerate(BORLAS):
        x0, y0, x1, y1 = bx0 - 8, by0 - 2, bx1 + 8, by1 + 6
        q = img[y0:y1, x0:x1].astype(np.int16)
        b, g, r = q[..., 0], q[..., 1], q[..., 2]
        red = ((r > g + 40) & (g < r * 0.5) & (b < r * 0.6) & (r > 70)).astype(np.uint8)
        red[:, :8] = 0                                       # só entre as bordas da borla
        red[:, -8:] = 0
        red = cv2.morphologyEx(red, cv2.MORPH_OPEN, np.ones((2, 2), np.uint8))
        n, lab, st, _ = cv2.connectedComponentsWithStats(red, 8)
        corpo = (lab == 1 + int(np.argmax(st[1:, 4]))).astype(np.uint8)
        corpo = cv2.morphologyEx(corpo, cv2.MORPH_CLOSE, np.ones((3, 3), np.uint8))
        ys, xs = np.nonzero(corpo)
        cx = int(round(xs.mean()))
        mask = corpo.copy()
        mask[0:ys.min() + 1, cx - 1:cx + 2] = 1             # o cordão até a lanterna
        hh, ww = y1 - y0, x1 - x0
        # A parede sem a borla: cada coluna liga o que está logo acima e logo abaixo dela.
        tampa = cv2.dilate(mask, np.ones((5, 5), np.uint8))
        bloco = img[y0:y1, x0:x1].astype(np.float32)
        for c in range(ww):
            rows = np.nonzero(tampa[:, c])[0]
            if len(rows) == 0:
                continue
            a, z = max(rows.min() - 1, 0), min(rows.max() + 1, hh - 1)
            t = ((np.arange(a, z + 1) - a) / max(1, z - a))[:, None]
            bloco[a:z + 1, c] = bloco[a, c] * (1 - t) + bloco[z, c] * t
        bloco = cv2.GaussianBlur(bloco, (0, 0), 0.6) * tampa[..., None] + bloco * (1 - tampa[..., None])
        sem = img.copy()
        sem[y0:y1, x0:x1] = bloco.astype(np.uint8)
        img = sem
        alfa = cv2.GaussianBlur(mask.astype(np.float32), (0, 0), 0.6)
        qq = q.astype(np.uint8)
        rgba = np.dstack([qq[..., 2], qq[..., 1], qq[..., 0], (np.clip(alfa * 1.3, 0, 1) * 255).astype(np.uint8)])
        nome = f"borla_{k + 1}.png"
        Image.fromarray(rgba, "RGBA").save(pasta / nome, optimize=True)
        cols, rows = 4, 10
        camadas.append({
            "id": f"borla-{k + 1}", "imagem": nome, "profundidade": round(parede["profundidade"] - 0.3, 1),
            **posicao(parede, w, h, x0, y0, x1, y1),
            "vento": {"colunas": cols, "linhas": rows, "amortecimento": 0.25, "pendulos": [
                {"pivo": [round((cx + 0.5) / ww, 4), 0.0], "graus": BORLA_GRAUS, "freq": 1.1,
                 "atraso": round(cx / w * 1.5, 3), "peso": [1.0] * ((cols + 1) * (rows + 1))}]}})
    return img, camadas


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", nargs="?", type=Path, default=PASTA)
    pasta = ap.parse_args().pasta
    planos_yinlu.montar(pasta)       # céu, nuvens, árvores e muro, sempre da pintura inteira
    stage_path = pasta / "stage.json"
    stage = json.loads(stage_path.read_text(encoding="utf-8"))
    parede = next(c for c in stage["camadas"] if c["id"] == "parede")
    w, h = Image.open(pasta / parede["imagem"]).size
    parede["luzes"] = [{"x": round(x / w, 4), "y": round(y / h, 4), "raio": round(r / h, 4),
                        "cor": "#FFC860", "pulsar": 0.12, "reflexo": False} for x, y, r in LANTERNAS]
    raios(pasta, w, h, 960)
    camadas = [c for c in stage["camadas"] if c["id"] not in ("arvores", "arvores-dentro")
               and not c["id"].startswith(("raios-", "lanterna-", "borla-"))]
    vento_arvores(pasta, next(c for c in camadas if c["id"] == "arvores-plano"), h)
    rgba = cv2.imread(str(pasta / parede["imagem"]), cv2.IMREAD_UNCHANGED)
    img = rgba[..., :3].copy()
    camada = arvores_dentro(pasta, parede, w, h)
    camadas.append(camada)
    img, novas = borlas(img, pasta, parede, w, h)
    camadas += novas
    cv2.imwrite(str(pasta / parede["imagem"]), np.dstack([img, rgba[..., 3]]))
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
