#!/usr/bin/env python3
"""Separa em planos e anima a Arena Imperial (``stages/arena_imperial``), montada pelo
``cenario_pintura.py`` a partir da arena inteira pintada pelo Codex
(``art/stages/arena_imperial_pintura/4_panorama_4096.webp``).

    python3 tools/stage/animar_imperial.py [pasta]

* **planos**: o **céu** (pintado sem a terra, bem longe), as **montanhas** e o vale (pintados sem
  a arena, recortados pelo molde magenta) e a **arena** (``parede.png``: os pixels da pintura, com
  o alfa do molde verde). No que aparece na tela de repouso, céu e montanhas são os da pintura; o
  que só aparece com a câmera andando vem das imagens pintadas à parte, com as cores casadas;
* **telão**: a tela do telão recebe a luta ao vivo e o replay (``"telao"``);
* **plateia**: a gente dos dois prédios pula de leve e solta flashes de câmera;
* **lanternas**: as penduradas e as da mureta brilham e tremulam; as da mureta refletem no chão
  (o chão é o pintado sem os riscos de luz: o motor desenha o reflexo, sempre reto);
* **cachoeiras**: a água das duas quedas ao lado da escadaria corre;
* **árvore**: as folhas do bordo vermelho balançam com o vento (``"vento"``).

As posições são em pixels do panorama de 4096; a parede tem ``EXTRA`` linhas em cima.
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
import animar_coberta  # noqa: E402
import cachoeiras  # noqa: E402
import pontos_plateia  # noqa: E402
from montar_porto import plateia as mascara_plateia  # noqa: E402

RAIZ = AQUI.parents[1]
ART = RAIZ / "art/stages/arena_imperial_pintura"
PASTA = RAIZ / "android/app/src/main/assets/stages/arena_imperial"
PANORAMA = ART / "4_panorama_4096.webp"
EXTRA = 300                                   # --ceu-extra do cenario_pintura.py
JUNCAO = 1262                                 # --juncao do cenario_pintura.py
TELA = (1412, 970, 1598, 1081)                # a tela do telão (x0, y0, x1, y1) no panorama
PLATEIA = (800, 1175)                         # faixa dos balcões (linhas do panorama)
PALACIO = (1560, 2560)                        # o meio (palácio, escadaria): sem plateia
QUEDAS = [(1640, 1010, 190, 215), (2250, 1015, 150, 210)]   # (x, y, largura, altura) no panorama
ARVORE = (2280, 840, 2760, 1185)              # caixa do bordo vermelho (x0, y0, x1, y1)
PROF_CEU, PROF_MONTANHAS = 30000.0, 6000.0
CEU_EXTRA = 900                               # linhas de céu acima do panorama (super pulo)
MAX_LUZES = 50
VENTO_GRUPOS = 10
VENTO_PX = 7.0                                # px do panorama que a ponta das folhas anda


def casar(fonte, alvo, mf, ma):
    """Média e desvio de cada canal da fonte (onde mf) iguais aos do alvo (onde ma)."""
    out = fonte.astype(np.float32).copy()
    for c in range(3):
        a, b = fonte[..., c][mf].astype(np.float32), alvo[..., c][ma].astype(np.float32)
        out[..., c] = (out[..., c] - a.mean()) / max(a.std(), 1e-3) * b.std() + b.mean()
    return np.clip(out, 0, 255)


def ler(caminho, w, h):
    return np.asarray(Image.open(caminho).convert("RGB").resize((w, h), Image.LANCZOS)).astype(np.float32)


def planos(pasta, stage, parede):
    """Céu, montanhas e a arena recortada; devolve (camadas novas, alfa da arena na parede)."""
    pimg = np.asarray(Image.open(pasta / "parede.jpg").convert("RGB")).astype(np.float32)
    h, w = pimg.shape[:2]                                 # EXTRA + linhas do panorama
    hp = h - EXTRA
    pan = ler(PANORAMA, w, 2305)[:hp]
    arena = np.asarray(Image.open(ART / "10_mascara_arena_4096.png").convert("L").resize((w, 2305)))[:hp] > 128
    terra = np.asarray(Image.open(ART / "15_mascara_montanhas_4096.png").convert("L").resize((w, 2305)))[:hp] > 128
    arena[JUNCAO - 40:] = True                           # o chão e o pé da mureta: sempre arena
    ceu_vis = ~arena & ~terra
    monte_vis = ~arena & terra

    # Arena: os pixels da parede, transparentes onde era céu ou montanha.
    a = np.zeros((h, w), np.float32)
    a[EXTRA:] = cv2.GaussianBlur(arena.astype(np.float32), (0, 0), 0.7)
    Image.fromarray(np.dstack([pimg, a * 255]).astype(np.uint8), "RGBA").save(pasta / "parede.png", optimize=True)
    (pasta / "parede.jpg").unlink(missing_ok=True)
    parede["imagem"] = "parede.png"
    parede.pop("corAcima", None)

    t0, b0 = parede["topo"], parede["base"]
    frac = lambda r: round(t0 + (b0 - t0) * (r + EXTRA) / h, 5)

    # Céu: a pintura onde ele aparece; atrás da arena e das montanhas, o "só céu" casado.
    so = casar(ler(ART / "13_so_ceu_4096.webp", w, 2305)[:hp], pan, np.ones((hp, w), bool), ceu_vis)
    k = cv2.GaussianBlur(cv2.erode(ceu_vis.astype(np.uint8), np.ones((9, 9), np.uint8)).astype(np.float32),
                         (0, 0), 20)[..., None]
    ceu = pan * k + so * (1 - k)
    alto = ceu[:600][::-1]
    pedacos, n = [], 0
    while n < CEU_EXTRA:
        pedacos.append(alto)
        alto = alto[::-1]
        n += 600
    topo = np.concatenate(pedacos[::-1], 0)[-CEU_EXTRA:]
    escuro = np.array([58, 26, 74], np.float32)          # roxo do alto do céu
    tc = (np.linspace(1, 0, CEU_EXTRA)[:, None, None]) ** 1.3
    ceu = np.concatenate([topo * (1 - tc * 0.8) + escuro * tc * 0.8, ceu], 0)
    Image.fromarray(np.clip(ceu, 0, 255).astype(np.uint8)).save(pasta / "ceu.jpg", quality=92)

    # Montanhas: a pintura onde aparecem; atrás da arena, o "sem arena" casado.
    sem = casar(ler(ART / "12_sem_arena_4096.webp", w, 2305)[:hp], pan, terra & ~arena, monte_vis)
    k = cv2.GaussianBlur(cv2.erode(monte_vis.astype(np.uint8), np.ones((9, 9), np.uint8)).astype(np.float32),
                         (0, 0), 6)[..., None]
    monte = pan * k + sem * (1 - k)
    am = cv2.GaussianBlur(terra.astype(np.float32), (0, 0), 0.8)
    # WebP sem perda (com perda, o alfa saía em blocos no celular); longe, em meia resolução.
    Image.fromarray(np.dstack([monte, am * 255]).astype(np.uint8), "RGBA").resize((w // 2, hp // 2), Image.LANCZOS).save(
        pasta / "montanhas.webp", lossless=True, quality=100, method=6)
    (pasta / "montanhas.png").unlink(missing_ok=True)

    cor = "#%02X%02X%02X" % tuple(int(v) for v in escuro * 0.8 + topo[0].mean(0) * 0.2)
    return [
        {"id": "ceu", "imagem": "ceu.jpg", "profundidade": PROF_CEU, "topo": frac(-CEU_EXTRA),
         "base": frac(hp), "x": parede["x"], "repetir": "espelhar", "corAcima": cor},
        {"id": "montanhas", "imagem": "montanhas.webp", "profundidade": PROF_MONTANHAS, "topo": frac(0),
         "base": frac(hp), "x": parede["x"], "repetir": "espelhar"},
    ], frac


def tirar(img, m):
    """A imagem sem o que está em m: dentro, ela mesma bem borrada; na borda, inpaint fino."""
    tampa = cv2.dilate(m, np.ones((9, 9), np.uint8))
    dentro = cv2.erode(tampa, np.ones((15, 15), np.uint8))
    borda = (tampa & (1 - dentro)).astype(np.uint8)
    borrada = cv2.GaussianBlur(img.astype(np.float32), (0, 0), 8)
    sem = img.copy()
    sem[dentro > 0] = borrada[dentro > 0]
    sem = cv2.inpaint(sem.astype(np.uint8), borda, 4, cv2.INPAINT_TELEA)
    m3 = cv2.GaussianBlur(tampa.astype(np.float32), (0, 0), 1.5)[..., None]
    return (img * (1 - m3) + sem * m3).astype(np.uint8)


def arvore(pasta, parede, frac):
    """As folhas do bordo vermelho numa camada com vento (o tronco fica na parede)."""
    rgba = np.asarray(Image.open(pasta / "parede.png").convert("RGBA")).copy()
    x0, y0, x1, y1 = ARVORE
    q = rgba[y0 + EXTRA:y1 + EXTRA, x0:x1]
    r, g, b = [q[..., i].astype(np.int16) for i in range(3)]
    folha = ((r > 140) & (g < r * 0.55) & (b < r * 0.5) & (q[..., 3] > 128)).astype(np.uint8)
    folha = cv2.morphologyEx(folha, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))
    folha = cv2.morphologyEx(folha, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    n, lab, st, _ = cv2.connectedComponentsWithStats(folha, 8)
    folha = np.isin(lab, [i for i in range(1, n) if st[i, 4] > 80]).astype(np.uint8)
    hh, ww = folha.shape
    al = cv2.GaussianBlur(folha.astype(np.float32), (0, 0), 0.8)
    Image.fromarray(np.dstack([q[..., :3], al * q[..., 3] / 255 * 255]).astype(np.uint8), "RGBA").save(
        pasta / "arvore.png", optimize=True)
    # Na parede: sem as folhas (atrás, ela mesma borrada; onde era céu, transparente).
    sem = tirar(q[..., :3], folha)
    q[..., :3] = sem
    rgba[y0 + EXTRA:y1 + EXTRA, x0:x1] = q
    Image.fromarray(rgba, "RGBA").save(pasta / "parede.png", optimize=True)
    cols, rows = 32, 16
    yy, xx = np.mgrid[0:rows + 1, 0:cols + 1].astype(np.float32)
    py, px = yy * hh / rows, xx * ww / cols
    ys, xs = np.nonzero(folha)
    fundo = ys.max()
    alcance = np.clip((fundo - py) / (fundo * 0.75), 0, 1) ** 0.9            # o pé das folhas parado
    alcance *= np.clip(np.minimum(px, ww - px) / 20.0, 0, 1)
    pts = np.float32(np.column_stack([xs, ys]))[::20]
    _, _, cent = cv2.kmeans(pts, VENTO_GRUPOS, None, (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.5),
                            3, cv2.KMEANS_PP_CENTERS)
    rng = np.random.default_rng(3)
    t, b_ = parede["topo"], parede["base"]
    w = rgba.shape[1]
    alt = (b_ - t) * 720.0
    cx = parede["x"] * 1280.0 + ((x0 + x1) / 2 - w / 2) / rgba.shape[0] * alt
    return {"id": "arvore", "imagem": "arvore.png", "profundidade": round(parede["profundidade"] - 0.3, 1),
            "topo": frac(y0), "base": frac(y1), "x": round(cx / 1280.0, 5),
            "vento": {"colunas": cols, "linhas": rows, "alcance": [round(float(v), 3) for v in alcance.ravel()],
                      "grupos": [[round(float(a / ww), 4), round(float(c / hh), 4)] for a, c in cent],
                      "freq": [round(float(f), 3) for f in rng.uniform(0.45, 0.7, VENTO_GRUPOS)],
                      "amplitude": round(VENTO_PX / hh, 5), "amortecimento": 0.35, "raio": 0.12}}


def lanternas(pan):
    """As lanternas acesas (amarelo forte, pequenas), uma luz por lanterna: (x, y, raio, reflete)."""
    im = pan.astype(np.int16)
    r, g, b = im[..., 0], im[..., 1], im[..., 2]
    m = ((r > 235) & (g > 170) & (b < 210)).astype(np.uint8)
    m[:700] = 0
    m[JUNCAO + 10:] = 0
    x0, y0, x1, y1 = TELA
    m[y0 - 40:y1 + 40, x0 - 40:x1 + 40] = 0
    m = cv2.dilate(m, np.ones((5, 5), np.uint8))
    n, _, st, cen = cv2.connectedComponentsWithStats(m, 8)
    achados = sorted([(st[i, 4], cen[i], st[i]) for i in range(1, n)
                      if 30 <= st[i, 4] <= 900 and st[i, 2] <= 36 and st[i, 3] <= 48], key=lambda t: -t[0])
    lista = []
    for _, (cx, cy), s_ in achados:
        if any(np.hypot(cx - x, cy - y) < 36 for x, y, _, _ in lista):
            continue
        lista.append((float(cx), float(cy), float(max(s_[2], s_[3])), cy > JUNCAO - 110))
        if len(lista) >= MAX_LUZES:
            break
    return sorted(lista)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", nargs="?", type=Path, default=PASTA)
    pasta = ap.parse_args().pasta
    stage_path = pasta / "stage.json"
    stage = json.loads(stage_path.read_text(encoding="utf-8"))
    parede = next(c for c in stage["camadas"] if c["id"] == "parede")
    fundos, frac = planos(pasta, stage, parede)
    img = Image.open(pasta / "parede.png").convert("RGBA")
    w, h = img.size

    x0, y0, x1, y1 = TELA
    parede["telao"] = {"x": round(x0 / w, 4), "y": round((y0 + EXTRA) / h, 4),
                       "largura": round((x1 - x0) / w, 4), "altura": round((y1 - y0) / h, 4)}

    topo, base = (PLATEIA[0] + EXTRA) / h, (PLATEIA[1] + EXTRA) / h
    gente = mascara_plateia(img, [(topo, base)])
    al = np.asarray(gente)[..., 3].copy()
    al[:, PALACIO[0]:PALACIO[1]] = 0                     # o palácio, a escadaria e o telão não pulam
    al[:, x0 - 60:x1 + 60] = 0
    gente.putalpha(Image.fromarray(al))
    gente.save(pasta / "parede_plateia.webp", quality=90, alpha_quality=100, method=6)
    flashes = pontos_plateia.pontos(pasta / "parede_plateia.webp", topo, base, 200, 7)
    parede["plateia"] = [{"topo": round(topo, 4), "base": round(base, 4), "mascara": "parede_plateia.webp",
                          "pulo": 0.003, "colunas": 200, "flashes": flashes}]

    pan = np.asarray(Image.open(PANORAMA).convert("RGB"))
    lant = lanternas(pan)
    parede["luzes"] = [{"x": round(x / w, 4), "y": round((y + EXTRA) / h, 4),
                        "raio": round(min(16.0 if ref else 11.0, max(7.0, r * 0.5)) / h, 4),
                        "cor": "#FFC060" if ref else "#FFB050", "pulsar": 0.05 if ref else 0.08, "reflexo": bool(ref)}
                       for x, y, r, ref in lant]

    camada_arvore = arvore(pasta, parede, frac)

    cachoeiras.AREAS["parede.png"] = {"tela": 0.97, "areas": [
        (round(x / w, 4), round((y + EXTRA) / h, 4), round(cw / w, 4), round(ch / h, 4)) for x, y, cw, ch in QUEDAS]}

    stage["camadas"] = fundos + [parede, camada_arvore]
    stage["chao"]["reflexoLutadores"] = 0.25
    stage["chao"]["reflexoLuzes"] = 0.85
    stage["chao"]["reflexoComprimento"] = 0.45
    stage["chao"]["reflexoImagem"] = animar_coberta.risco(pasta)
    stage["poeira"] = {"quantidade": 120, "cor": "#FFD49A", "alfa": 0.6, "tamanho": 2.4, "vento": 14,
                       "perto": -260, "longe": 2000, "topo": 0.1, "base": 0.9}
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    feitas = cachoeiras.montar(pasta, "parede.png")
    if not cachoeiras.anotar(pasta, "parede.png", feitas):
        raise SystemExit("camada da parede não achada no stage.json")
    # A parede em WebP sem perda (o PNG passava de 9 MB).
    Image.open(pasta / "parede.png").save(pasta / "parede.webp", lossless=True, quality=100, method=6)
    (pasta / "parede.png").unlink()
    texto = stage_path.read_text(encoding="utf-8").replace('"imagem": "parede.png"', '"imagem": "parede.webp"')
    stage_path.write_text(texto, encoding="utf-8")
    print(f"planos céu/montanhas/arena, telão {parede['telao']}, plateia com {len(flashes)} flashes, "
          f"{len(lant)} lanternas ({sum(1 for l in lant if l[3])} com reflexo), {len(QUEDAS)} cachoeiras, árvore com vento")


if __name__ == "__main__":
    main()
