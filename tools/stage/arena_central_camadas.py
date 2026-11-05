#!/usr/bin/env python3
"""Monta a Arena Central a partir das pinturas independentes (cada camada pintada inteira).

As artes ficam em ``art/stages/arena_central/camadas/`` (do pacote "pinturas independentes v2"
do Drive, convertidas para WebP): todas no mesmo quadro de referência de 4000 x 1596 px (o
concept aprovado), menos o céu (4000 x 2355, começando 1449 px acima do quadro) e o piso (a
textura vista de cima, 4000 x 4000, com o emblema no meio).

* ``ceu.webp``: o céu inteiro, opaco, bem mais alto que a tela (o super pulo mostra em cima);
* ``montanhas.webp``: montanhas e cachoeiras, e embaixo delas o vale pintado (o que aparece
  atrás do muro quando a câmera sobe);
* ``estrutura.webp``: muro, pavilhão e arquibancadas sem as torres (o público e o muro pintados
  atrás delas);
* ``torre_*.webp``: as três torres soltas;
* ``piso.webp``: o chão de cima.

O quadro de referência é a tela de repouso do jogo ampliada: o pixel (sx, sy) da tela de
1280 x 720 fica em (x0 + escala*sx, y0 + escala*sy) (a mesma ``janela`` do panorama antigo,
passada para 4000 px). A profundidade de cada camada vem da linha onde ela toca o chão (o muro)
ou é escolhida (céu e montanhas); o motor faz a paralaxe.

O piso: a textura de cima vira duas imagens do mesmo tamanho no mundo (``largura`` x de
``perto`` a ``longe``): ``chao_lajotas.jpg`` só com pedras, que o motor repete para os lados e
para o fundo, e ``chao.jpg``, a mesma com o emblema no meio, desenhada uma vez no centro. As
pedras são as fiadas da textura que não passam pelo emblema, empilhadas (algumas viradas e
deslocadas de lado); cada fiada é a original seguida dela espelhada, então a imagem emenda
sozinha nos lados.

    python3 tools/stage/arena_central_camadas.py
"""
import argparse
import json
import os
import sys
from pathlib import Path

import numpy as np
from PIL import Image

AQUI = Path(__file__).resolve().parent
sys.path.insert(0, str(AQUI))
import cenario_do_conceito as cc  # noqa: E402

ARTE = AQUI.parents[1] / "art/stages/arena_central/camadas"
PASTA = AQUI.parents[1] / "android/app/src/main/assets/stages/arena_central"

# A tela de repouso no quadro de 4000 x 1596 (a janela do panorama de 3966 x 1586 ampliada).
ESCALA, X0, Y0 = 1.8188, 825.2, 109.5
CEU_Y = -1449                 # onde o céu começa no quadro
MURO_Y = 866                  # o pé do muro no meio: onde o chão acaba (como no concept)
# A pintura do muro tem embaixo uma base de pedra (até a linha ~903) que no concept fica
# coberta pelo chão: sai nas colunas em que a estrutura acaba antes de BASE_ATE (o muro do meio;
# os prédios dos lados descem mais e ficam inteiros).
BASE_ATE = 915
PROF_CEU, PROF_MONTANHAS = 20000, 5000
TORRES = ["torre_bandeira", "torre_refletor_esquerda", "torre_refletor_direita"]

# O piso: o anel vermelho do emblema (borda de fora) mede RAIO_ANEL no mundo; o emblema fica
# em z = Z_EMBLEMA, no meio da arena (x = REST_CENTER_X). O piso vai de PERTO até o muro.
RAIO_ANEL = 380.0
Z_EMBLEMA = 0.0
PERTO = -500.0
LARGURA_TEX = 4096
# Na textura de cima (4000 px): centro do emblema, raio do vermelho de fora e as juntas das
# fiadas que não passam pelo emblema.
EMBLEMA_C = (1992, 1978)
ANEL_PX = 1050
FIADAS = [(154, 550), (3107, 3482), (3482, 3861)]


# Focos da sombra dos lutadores (pontos do quadro): os holofotes das torres (o da esquerda,
# do lado do sol, mais forte) e as chamas dos cinco braseiros do muro (sombra comprida e fraca).
HOLOFOTES = [(1440, 127, 0.72), (2533, 127, 0.35)]
BRASEIROS = [(462, 807), (1085, 771), (1755, 769), (2164, 769), (2775, 765)]
TOCHA_ALFA, TOCHA_ALCANCE = 0.55, 3000


def no_mundo(px, py, z):
    """Um ponto do quadro na profundidade z: x e altura acima do chão no mundo."""
    horizon, eye_y, distance = cc.camera(0.47)
    s = cc.BASE_DISTANCE / (distance + z)
    chao_y = horizon + (cc.GROUND_Y - eye_y) * s
    sx, sy = (px - X0) / ESCALA, (py - Y0) / ESCALA
    return round(cc.REST_CENTER_X + (sx - cc.VW / 2) / s), round((chao_y - sy) / s)


def tela(y):
    return (y - Y0) / ESCALA


def profundidade(y_quadro):
    """A profundidade do chão na linha y do quadro (pela câmera de repouso)."""
    horizon, eye_y, distance = cc.camera(0.47)
    eye_h = cc.GROUND_Y - eye_y
    s = (tela(y_quadro) - horizon) / eye_h
    return cc.BASE_DISTANCE / s - distance


def caixa(x0, y0, x1, y1):
    """topo, base e x (centro) em fração da tela de repouso de um retângulo do quadro."""
    return (round(tela(y0) / cc.VH, 4), round(tela(y1) / cc.VH, 4),
            round(((x0 + x1) / 2 - X0) / ESCALA / cc.VW, 4))


def borda_do_emblema(img):
    """Raio da borda dourada de fora do anel em cada ângulo (para colar o emblema sem pegar
    pedra: a emenda fica em cima do dourado liso)."""
    cx, cy = EMBLEMA_C
    angs = np.linspace(0, 2 * np.pi, 720, endpoint=False)
    raios = np.arange(ANEL_PX - 20, ANEL_PX + 90)
    r_ = np.empty(len(angs))
    for i, a in enumerate(angs):
        xs = np.clip((cx + raios * np.cos(a)).astype(int), 0, img.shape[1] - 1)
        ys = np.clip((cy + raios * np.sin(a)).astype(int), 0, img.shape[0] - 1)
        p = img[ys, xs].astype(int)
        ouro = (p[:, 0] > 165) & (p[:, 2] < 75)
        r_[i] = raios[np.nonzero(ouro)[0].max()] if ouro.any() else ANEL_PX + 30
    # Suaviza (pinceladas soltas) e entra 3 px no dourado.
    k = np.ones(15) / 15
    r_ = np.convolve(np.concatenate([r_[-7:], r_, r_[:7]]), k, mode="valid")
    return angs, r_ - 3


def piso(saida, longe):
    img = np.array(Image.open(ARTE / "piso.webp").convert("RGB"))
    k = RAIO_ANEL / ANEL_PX                     # mundo por pixel da textura de cima
    # A cor das pedras em volta do emblema (as fiadas de cima e de baixo saem nesse tom).
    cx, cy = EMBLEMA_C
    yy, xx = np.mgrid[0:img.shape[0]:4, 0:img.shape[1]:4]
    d = np.hypot(xx - cx, yy - cy)
    media = img[::4, ::4][(d > ANEL_PX + 80) & (d < ANEL_PX + 400)].reshape(-1, 3).mean(0)
    fiadas = []
    for a, b in FIADAS:
        f = img[a:b].astype(np.float32)
        f *= media / f.reshape(-1, 3).mean(0)   # mesma luz em todas
        fiadas.append(np.concatenate([f, f[:, ::-1]], axis=1))   # emenda nos lados
    periodo = fiadas[0].shape[1]
    linhas = int(round((longe - PERTO) / k))
    rng = np.random.default_rng(7)
    pedras = []
    ultima = -1
    while sum(p.shape[0] for p in pedras) < linhas:
        i = rng.integers(len(fiadas))
        if i == ultima:
            i = (i + 1) % len(fiadas)
        ultima = i
        f = np.roll(fiadas[i], int(rng.integers(periodo)), axis=1)
        pedras.append(f[:, ::-1] if rng.random() < 0.5 else f)
    lajotas = np.concatenate(pedras)[:linhas]
    # O emblema: o círculo até a borda dourada, no meio da largura e em z = Z_EMBLEMA.
    com = lajotas.copy()
    angs, borda = borda_do_emblema(img)
    rmax = int(borda.max()) + 2
    ex, ey = periodo // 2, int(round((longe - Z_EMBLEMA) / k))
    yy, xx = np.mgrid[-rmax:rmax + 1, -rmax:rmax + 1]
    ang = np.mod(np.arctan2(yy, xx), 2 * np.pi)
    lim = np.interp(ang, np.concatenate([angs, [2 * np.pi]]), np.concatenate([borda, borda[:1]]))
    d = np.hypot(xx, yy)
    alfa = np.clip(lim - d + 0.5, 0, 1)[..., None]
    fonte = img[cy - rmax:cy + rmax + 1, cx - rmax:cx + rmax + 1].astype(np.float32)
    alvo = com[ey - rmax:ey + rmax + 1, ex - rmax:ex + rmax + 1]
    com[ey - rmax:ey + rmax + 1, ex - rmax:ex + rmax + 1] = fonte * alfa + alvo * (1 - alfa)
    # A luz do concept: o meio da arena claro, o pé do muro um pouco mais escuro e a parte
    # de perto bem mais (como na pintura achatada, que ia de 163 perto a 200 no meio).
    z = longe - (np.arange(linhas) + 0.5) * k
    luz = np.interp(z, [PERTO, -150.0, 100.0, 450.0, longe], [0.80, 0.95, 1.0, 0.98, 0.90])[:, None, None]
    # Lados mais escuros que o meio (periódico na largura: emenda igual).
    x = np.arange(periodo)
    lado = (1 - 0.14 * (1 - np.cos(2 * np.pi * (x - periodo / 2) / periodo)) / 2)[None, :, None]
    lajotas = lajotas * luz * lado
    com = com * luz * lado
    # O tom e o contraste do chão do concept (pedra mais bege e mais marcada que a textura).
    alvo = np.array([168.0, 120.0, 86.0])
    base_cor = lajotas.reshape(-1, 3).mean(0)
    for arr in (lajotas, com):
        arr *= alvo / base_cor
        arr[:] = alvo + (arr - alvo) * 1.35
    largura = periodo * k
    altura_tex = int(round(LARGURA_TEX * (longe - PERTO) / largura))
    for arr, nome in ((lajotas, "chao_lajotas.jpg"), (com, "chao.jpg")):
        Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).resize(
            (LARGURA_TEX, altura_tex), Image.LANCZOS).save(saida / nome, quality=90, optimize=True)
    return largura


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", nargs="?", type=Path, default=PASTA)
    pasta = ap.parse_args().pasta
    longe = round(profundidade(MURO_Y), 1)
    camadas = []

    ceu = Image.open(ARTE / "ceu.webp").convert("RGB")
    ceu.save(pasta / "ceu.jpg", quality=92, optimize=True)
    t, b, x = caixa(0, CEU_Y, ceu.width, CEU_Y + ceu.height)
    camadas.append({"id": "ceu", "imagem": "ceu.jpg", "profundidade": PROF_CEU, "topo": t, "base": b, "x": x,
                    "repetir": "espelhar", "corAcima": "#2A1450"})

    mont = Image.open(ARTE / "montanhas.webp")
    mont.save(pasta / "montanhas.webp", quality=92, alpha_quality=100, method=6)
    t, b, x = caixa(0, 0, mont.width, mont.height)
    camadas.append({"id": "montanhas", "imagem": "montanhas.webp", "profundidade": PROF_MONTANHAS,
                    "topo": t, "base": b, "x": x, "repetir": "espelhar"})

    est = Image.open(ARTE / "estrutura.webp")
    a = np.array(est)
    alfa = a[..., 3]
    fundo_col = np.where(alfa.any(0), alfa.shape[0] - 1 - np.argmax(alfa[::-1] > 0, 0), 0)
    cols = fundo_col < BASE_ATE
    a[MURO_Y + 2:, cols, 3] = 0
    est = Image.fromarray(a)
    bb = est.getbbox()
    est.crop(bb).save(pasta / "estrutura.webp", quality=92, alpha_quality=100, method=6)
    t, b, x = caixa(*bb)
    camadas.append({"id": "estrutura", "imagem": "estrutura.webp", "profundidade": longe,
                    "topo": t, "base": b, "x": x})

    for nome in TORRES:
        im = Image.open(ARTE / f"{nome}.webp")
        bb = im.getbbox()
        im.crop(bb).save(pasta / f"{nome}.webp", quality=92, alpha_quality=100, method=6)
        t, b, x = caixa(*bb)
        # Um pouco na frente do muro (elas ficam no pé dele).
        camadas.append({"id": nome, "imagem": f"{nome}.webp", "profundidade": round(longe - 30, 1),
                        "topo": t, "base": b, "x": x})

    largura = piso(pasta, longe)

    stage_path = pasta / "stage.json"
    stage = json.loads(stage_path.read_text(encoding="utf-8"))
    stage["camadas"] = camadas
    focos = []
    for px, py, alfa in HOLOFOTES:
        x, altura = no_mundo(px, py, longe - 30)
        focos.append({"x": x, "altura": altura, "profundidade": round(longe - 30), "alfa": alfa})
    for px, py in BRASEIROS:
        x, altura = no_mundo(px, py, longe)
        focos.append({"x": x, "altura": altura, "profundidade": round(longe), "alfa": TOCHA_ALFA,
                      "alcance": TOCHA_ALCANCE})
    stage.setdefault("sombraLutadores", {})["focos"] = focos
    chao = stage.setdefault("chao", {})
    for chave in ("linhasPerspectiva",):
        chao.pop(chave, None)
    chao.update({"imagem": "chao.jpg", "imagemLajotas": "chao_lajotas.jpg", "perto": PERTO, "longe": longe,
                 "ate": longe, "centroX": round(cc.REST_CENTER_X), "largura": round(largura, 1),
                 "reflexoLuzes": 0})
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"muro em z {longe}, piso de {largura:.0f} de largura; {len(camadas)} camadas")


if __name__ == "__main__":
    main()
