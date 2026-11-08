#!/usr/bin/env python3
"""Separa em planos e anima o Terraço da Lua (``stages/terraco_lua``), montado pelo
``cenario_pintura.py`` a partir do terraço inteiro pintado pelo Codex
(``art/stages/terraco_lua_pintura/7_panorama_4096.webp``).

    python3 tools/stage/animar_terraco.py [pasta]

* **planos**: o **céu** (estrelas e a lua, pintado sem as nuvens, bem longe), as **nuvens**
  (as da pintura, mais claras que o céu limpo atrás: andam devagar com ``"deriva"``), o
  **fundo** (o mar, o mar de nuvens lá embaixo, as ilhas, o estádio e o templo; abaixo do que se
  vê, o mar de nuvens pintado sem o terraço) e o **terraço** (``parede.webp``: a mureta e os
  postes, com o alfa do molde verde);
* **lanternas**: as da mureta e as dos postes brilham e tremulam; as da mureta refletem no chão
  (o chão é o pintado sem os riscos de luz: o motor desenha o reflexo, sempre reto);
* **lua**: um brilho grande e lento em volta dela.

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

RAIZ = AQUI.parents[1]
ART = RAIZ / "art/stages/terraco_lua_pintura"
PASTA = RAIZ / "android/app/src/main/assets/stages/terraco_lua"
PANORAMA = ART / "7_panorama_4096.webp"
EXTRA = 300                                   # --ceu-extra do cenario_pintura.py
JUNCAO = 1258                                 # --juncao do cenario_pintura.py (pé da mureta)
HORIZONTE = 1068                              # a linha do mar (o ponto de fuga do piso)
FUNDO_TOPO = 1035                             # daqui para baixo é o fundo (mar, ilhas)
FUNDO_MAIS = 850                              # linhas de mar de nuvens abaixo da mureta (super pulo)
SEM_HORIZONTE = 985                           # o horizonte no 11_sem_terraco_4096.webp
PROF_CEU, PROF_NUVENS, PROF_FUNDO = 30000.0, 12000.0, 6000.0
DERIVA_NUVENS = 30.0
CEU_EXTRA = 900                               # linhas de céu acima do panorama (super pulo)
MAX_LUZES = 60


def casar(fonte, alvo, mf, ma):
    out = fonte.astype(np.float32).copy()
    for c in range(3):
        a, b = fonte[..., c][mf].astype(np.float32), alvo[..., c][ma].astype(np.float32)
        out[..., c] = (out[..., c] - a.mean()) / max(a.std(), 1e-3) * b.std() + b.mean()
    return np.clip(out, 0, 255)


def tirar_postes(img, y0, terraco, ref):
    """Tira os postes (e o brilho das lanternas) das linhas y0.. do céu: o céu ao lado, deslocado na
    horizontal, entra no lugar (as estrelas e o degradê seguem; um inpaint deixava borrões)."""
    quente = (ref[..., 0] - ref[..., 2]) > 10                  # a luz das lanternas (o céu é azul)
    m = np.zeros(img.shape[:2], np.uint8)
    m[y0:y0 + len(terraco)] = cv2.dilate((terraco | quente).astype(np.uint8), np.ones((61, 61), np.uint8))
    m[:y0 + int(terraco.any(1).argmax()) - 40] = 0              # só a faixa dos postes (a lua fica)
    out, falta = img.copy(), m.astype(bool)
    for d in (220, -220, 440, -440, 660, -660):
        ok = falta & ~np.roll(m.astype(bool), d, 1)
        out[ok] = np.roll(img, d, 1)[ok]
        falta &= ~ok
    k = cv2.GaussianBlur(m.astype(np.float32), (0, 0), 6)[..., None]
    return img * (1 - k) + out * k


def ler(caminho, w, h):
    return np.asarray(Image.open(caminho).convert("RGB").resize((w, h), Image.LANCZOS)).astype(np.float32)


def planos(pasta, parede):
    pimg = np.asarray(Image.open(pasta / "parede.jpg").convert("RGB")).astype(np.float32)
    h, w = pimg.shape[:2]
    hp = h - EXTRA
    pan = ler(PANORAMA, w, 2305)[:hp]
    terraco = np.asarray(Image.open(ART / "8_mascara_terraco_4096.png").convert("L").resize((w, 2305)))[:hp] > 128
    terraco[JUNCAO - 6:] = True                       # o pé da mureta e o chão: sempre terraço
    t0, b0 = parede["topo"], parede["base"]
    frac = lambda r: round(t0 + (b0 - t0) * (r + EXTRA) / h, 5)

    # Terraço: os pixels da parede, transparentes além da mureta.
    a = np.zeros((h, w), np.float32)
    a[EXTRA:] = cv2.GaussianBlur(terraco.astype(np.float32), (0, 0), 0.7)
    Image.fromarray(np.dstack([pimg, a * 255]).astype(np.uint8), "RGBA").save(
        pasta / "parede.webp", lossless=True, quality=100, method=6)
    (pasta / "parede.jpg").unlink(missing_ok=True)
    parede["imagem"] = "parede.webp"
    parede.pop("corAcima", None)

    yy = np.arange(hp, dtype=np.float32)[:, None]
    limpo = ler(ART / "10_ceu_limpo_4096.webp", w, 2305)[:hp]
    alem = ~terraco

    # Fundo: a pintura da linha FUNDO_TOPO para baixo; atrás da mureta (o que o super pulo mostra),
    # o mar de nuvens pintado sem o terraço, com o horizonte no mesmo lugar e as cores casadas.
    sem = ler(ART / "11_sem_terraco_4096.webp", w, 2305)
    desl = HORIZONTE - SEM_HORIZONTE
    sem = np.roll(sem, desl, 0)[:hp + FUNDO_MAIS]
    vis = alem & (yy >= FUNDO_TOPO + 30)
    mf = np.zeros(sem.shape[:2], bool)
    mf[HORIZONTE + 40:hp] = True
    sem = casar(sem, pan, mf, vis)
    k = cv2.GaussianBlur(cv2.erode(vis.astype(np.uint8), np.ones((15, 15), np.uint8)).astype(np.float32),
                         (0, 0), 4)[..., None]
    fundo = pan * k + sem[:hp] * (1 - k)
    # Embaixo, o mar de nuvens continua (no super pulo o terraço, mais perto, desce mais que o
    # fundo; sem isto aparecia uma faixa escura entre o mar e a mureta).
    fundo = np.concatenate([fundo, sem[hp:]], 0)
    yy2 = np.arange(len(fundo), dtype=np.float32)[:, None]
    af = np.clip((yy2 - FUNDO_TOPO) / 30.0, 0, 1) * np.ones((1, w), np.float32)
    Image.fromarray(np.dstack([fundo, af * 255]).astype(np.uint8), "RGBA").save(
        pasta / "fundo.webp", lossless=True, quality=100, method=6)

    # Céu: o limpo (estrelas e lua), casado em cor com o que aparece entre as nuvens; nuvens: a
    # pintura onde ela é mais clara que o céu limpo (as nuvens são prata sobre o azul-marinho).
    ceu = pan[:FUNDO_TOPO + 40]
    lp = limpo[:FUNDO_TOPO + 40]
    claro = ceu.mean(2) - lp.mean(2)
    an = np.clip((claro - 10) / 45, 0, 1)
    lum_l = cv2.GaussianBlur(lp.mean(2), (0, 0), 5)
    sy, sx = np.unravel_index(int(np.argmax(lum_l)), lum_l.shape)
    gy, gx = np.mgrid[0:len(ceu), 0:w]
    dist = np.hypot(gx - sx, gy - sy).astype(np.float32)
    an *= np.clip((dist - 140) / 60, 0, 1)                      # a lua fica no céu (parada)
    an *= np.clip((FUNDO_TOPO + 10 - gy) / 40.0, 0, 1)
    postes = cv2.dilate((terraco[:len(ceu)] | ((ceu[..., 0] - ceu[..., 2]) > 10)).astype(np.uint8),
                        np.ones((61, 61), np.uint8))
    postes[:int(terraco.any(1).argmax()) - 40] = 0
    an *= 1 - postes                                            # sem a borda dourada dos postes
    an = cv2.GaussianBlur(an.astype(np.float32), (0, 0), 1.0)
    peso = (1 - an) ** 3
    soma = lambda x: cv2.GaussianBlur((x * peso[..., None]).sum(1)[:, None, :], (0, 0), sigmaX=0.1, sigmaY=50)[:, 0, :]
    sp = cv2.GaussianBlur(peso.sum(1)[:, None, None].astype(np.float32), (0, 0), sigmaX=0.1, sigmaY=50).reshape(-1, 1)
    dif = (soma(ceu) - soma(lp)) / np.maximum(sp, 1)
    lp = np.clip(lp + dif[:, None, :] * np.clip((dist[..., None] - 180) / 200, 0, 1), 0, 255)
    alto = lp[:500][::-1]
    pedacos, n = [], 0
    while n < CEU_EXTRA:
        pedacos.append(alto)
        alto = alto[::-1]
        n += 500
    topo = np.concatenate(pedacos[::-1], 0)[-CEU_EXTRA:]
    escuro = np.array([6, 10, 30], np.float32)
    tc = (np.linspace(1, 0, CEU_EXTRA)[:, None, None]) ** 1.3
    # O céu vai até embaixo (no super pulo o fundo, mais perto, desce mais que o céu e abriria
    # uma faixa vazia entre os dois): embaixo do horizonte, o próprio céu limpo (mar e névoa).
    # (o mar sem o terraço: o céu limpo do Codex ainda tem a mureta e os postes embaixo)
    resto = sem[len(lp):hp]
    ceu_img = np.concatenate([topo * (1 - tc * 0.7) + escuro * tc * 0.7, lp, resto], 0)
    # Os postes do céu limpo saem (no super pulo o céu desce menos que o terraço, e o pedaço de
    # poste dele ficava solto no ar acima do poste de verdade).
    ceu_img = tirar_postes(ceu_img, CEU_EXTRA, terraco[:len(lp)], lp)
    Image.fromarray(np.clip(ceu_img, 0, 255).astype(np.uint8)).save(pasta / "ceu.jpg", quality=93)
    Image.fromarray(np.dstack([ceu, an * 255]).astype(np.uint8), "RGBA").resize(
        (w * 3 // 4, len(ceu) * 3 // 4), Image.LANCZOS).save(pasta / "nuvens.webp", lossless=True, quality=100, method=6)
    hc = len(ceu)
    lua = {"x": round(sx / w, 4), "y": round((sy + CEU_EXTRA) / (hp + CEU_EXTRA), 4),
           "raio": round(150 / (hp + CEU_EXTRA), 4), "cor": "#BFD8FF", "pulsar": 0.04, "reflexo": False}
    return [
        {"id": "ceu", "imagem": "ceu.jpg", "profundidade": PROF_CEU, "topo": frac(-CEU_EXTRA), "base": frac(hp),
         "x": parede["x"], "repetir": "espelhar", "corAcima": "#060A1E", "luzes": [lua]},
        {"id": "nuvens", "imagem": "nuvens.webp", "profundidade": PROF_NUVENS, "topo": frac(0), "base": frac(hc),
         "x": parede["x"], "repetir": "espelhar", "deriva": DERIVA_NUVENS},
        {"id": "fundo", "imagem": "fundo.webp", "profundidade": PROF_FUNDO, "topo": frac(0), "base": frac(hp + FUNDO_MAIS),
         "x": parede["x"], "repetir": "espelhar",
         # Na vertical anda com o céu: o horizonte está no infinito. Solto (6000), no super pulo
         # descia mais que o céu e a borda de cima (ilhas cortadas) aparecia sobre o céu.
         "apoio": PROF_CEU},
    ]


def lanternas(pan):
    """As lanternas acesas, uma luz por lanterna: (x, y, raio, reflete)."""
    im = pan.astype(np.int16)
    r, g, b = im[..., 0], im[..., 1], im[..., 2]
    m = ((r > 235) & (g > 165) & (b < 190)).astype(np.uint8)
    m[:800] = 0
    m[JUNCAO + 6:] = 0
    m = cv2.dilate(m, np.ones((5, 5), np.uint8))
    n, _, st, cen = cv2.connectedComponentsWithStats(m, 8)
    achados = sorted([(st[i, 4], cen[i], st[i]) for i in range(1, n)
                      if 30 <= st[i, 4] <= 1500 and st[i, 2] <= 50 and st[i, 3] <= 70], key=lambda t: -t[0])
    lista = []
    for _, (cx, cy), s_ in achados:
        if any(np.hypot(cx - x, cy - y) < 40 for x, y, _, _ in lista):
            continue
        lista.append((float(cx), float(cy), float(max(s_[2], s_[3])), cy > JUNCAO - 90))
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
    fundos = planos(pasta, parede)
    w, h = Image.open(pasta / "parede.webp").size
    pan = np.asarray(Image.open(PANORAMA).convert("RGB"))
    lant = lanternas(pan)
    parede["luzes"] = [{"x": round(x / w, 4), "y": round((y + EXTRA) / h, 4),
                        "raio": round(min(14.0 if ref else 12.0, max(7.0, r * 0.5)) / h, 4),
                        "cor": "#FFC060", "pulsar": 0.05 if ref else 0.08, "reflexo": bool(ref)}
                       for x, y, r, ref in lant]
    stage["camadas"] = fundos + [parede]
    stage["chao"]["reflexoLutadores"] = 0.3
    stage["chao"]["reflexoLuzes"] = 0.8
    stage["chao"]["reflexoComprimento"] = 0.4
    stage["chao"]["reflexoImagem"] = animar_coberta.risco(pasta)
    stage.pop("poeira", None)
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"planos céu/nuvens/fundo/terraço, {len(lant)} lanternas ({sum(1 for l in lant if l[3])} com reflexo)")


if __name__ == "__main__":
    main()
