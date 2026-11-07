#!/usr/bin/env python3
"""Separa o dojo da Yin Lu (``stages/dojo_yinlu``) em planos, para a paralaxe e o super pulo.

    python3 tools/stage/planos_yinlu.py [pasta]

O ``animar_yinlu.py`` chama este antes de animar (depois do ``cenario_pintura.py``). A pintura era um plano só (o
céu, as árvores e o muro andando juntos: parecia uma foto). Fica:

* **céu** (``ceu.jpg``): só o degradê azul, bem longe;
* **nuvens** (``nuvens.png``): as nuvens do céu pintado à parte, inteiras, numa camada que anda
  devagar com o vento (``"deriva"``);
* **árvores** (``arvores.png``): as copas no meio do caminho (o vento nos galhos é do
  ``animar_yinlu.py``); atrás do muro e do portão, o que o Codex pintou sem eles;
* **mata** (``mata.png``): uma fileira de árvores mais longe e apagadas, nos vãos entre as copas;
* **pátio** (``patio.jpg``): o pátio de dentro, pintado à parte mais largo que o vão do portão,
  um pouco atrás do muro (pelo vão ele anda diferente do portão);
* **parede** (``parede.png``): o muro e o portão, recortados pelo molde verde (e o vão do portão
  pelo dele), nos mesmos pixels de antes.

Em cima, as copas e o céu continuados pelo Codex (o topo da pintura cortava as copas; no super
pulo aparecia o céu esticado). As imagens do Codex estão em ``art/stages/dojo_yinlu`` (prompts
``prompt_10_*`` a ``prompt_13_*``); as linhas são as do ``9_panorama_4096.webp``.
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

AQUI = Path(__file__).resolve().parent
RAIZ = AQUI.parents[1]
ART = RAIZ / "art/stages/dojo_yinlu"
PASTA = RAIZ / "android/app/src/main/assets/stages/dojo_yinlu"

ALTO = 565            # linhas acima da pintura (o que o Codex continuou), em px do panorama
EXTRA = 300           # o --ceu-extra do cenario_pintura.py: a linha 0 da parede é a -300 do panorama
BASE = 674            # a última linha da parede (--juncao + 10)
PROF_PATIO = 850.0    # o pátio de dentro, visto pelo vão do portão
PROF_ARVORES = 1000.0
PROF_MATA = 1800.0    # a mata ao longe, entre as árvores e as nuvens
PATIO = (1800, 650, 2290, 900)   # px da parede: o retângulo do pátio pintado à parte
MATA = (220, 560)     # linhas do panorama: o alto das copas da mata e o pé (atrás do muro)
PROF_NUVENS = 3500.0
PROF_CEU = 30000.0
DERIVA = 20.0         # unidades do mundo por segundo


def salvar(caminho, rgba, metade=False):
    """PNG com o que é transparente (longe da borda) numa cor lisa: o arquivo fica bem menor."""
    a = rgba[..., 3]
    longe = cv2.distanceTransform((a == 0).astype(np.uint8), cv2.DIST_L2, 3) > 4
    out = rgba.copy()
    out[longe, :3] = np.median(rgba[a > 200, :3], 0) if (a > 200).any() else 0
    img = Image.fromarray(out, "RGBA")
    if metade:
        img = img.resize((img.width // 2, img.height // 2), Image.LANCZOS)
    img.save(caminho, optimize=True)


def retangulo(parede, w, h, x0, y0, x1, y1):
    """topo, base e x (frações da tela de repouso) de um retângulo da parede (px)."""
    t, b = parede["topo"], parede["base"]
    alt = (b - t) * 720.0
    cx = parede["x"] * 1280.0 + ((x0 + x1) / 2 - w / 2) / h * alt
    return {"topo": round(t + (b - t) * y0 / h, 5), "base": round(t + (b - t) * y1 / h, 5), "x": round(cx / 1280.0, 5)}


def ceu_cor(img):
    """Onde é céu azul ou nuvem (pela cor)."""
    b, g, r = [img[..., i].astype(np.int16) for i in range(3)]
    azul = (b > r + 35) & (b > g + 10)
    nuvem = (b > 185) & (g > 185) & (r > 175) & (np.abs(b - r) < 60)
    return azul | nuvem


def casar(fonte, alvo, mf, ma):
    """Média e desvio de cada canal da fonte (onde mf) iguais aos do alvo (onde ma)."""
    out = fonte.astype(np.float32).copy()
    for c in range(3):
        a, b = fonte[..., c][mf].astype(np.float32), alvo[..., c][ma].astype(np.float32)
        out[..., c] = (out[..., c] - a.mean()) / max(a.std(), 1e-3) * b.std() + b.mean()
    return np.clip(out, 0, 255)


def montar(pasta):
    stage_path = pasta / "stage.json"
    stage = json.loads(stage_path.read_text(encoding="utf-8"))
    parede = next(c for c in stage["camadas"] if c["id"] == "parede")
    t0, b0 = parede["topo"], parede["base"]
    hp = EXTRA + BASE
    frac = lambda r: round(t0 + (b0 - t0) * (r + EXTRA) / hp, 5)       # linha do panorama → tela

    alto = cv2.imread(str(ART / "10_alto_4096.webp"))          # linhas -ALTO..700 do panorama
    sem = cv2.imread(str(ART / "11_sem_muro_4096.webp"))       # mesma área, sem o muro
    so_ceu = cv2.imread(str(ART / "12_so_ceu_4096.webp"))      # mesma área, só céu e nuvens
    muro = cv2.imread(str(ART / "13_mascara_muro_4096.png"), cv2.IMREAD_GRAYSCALE) > 128
    W = alto.shape[1]
    pintada = cv2.imread(str(ART / "parede_com_arvores.jpg"))  # a parede de antes (linhas -300..674)

    # --- parede: o muro com os pixels de antes, transparente no resto.
    m = np.zeros((hp, W), np.float32)
    r0 = ALTO - EXTRA                                    # linha -300 do panorama no "alto"
    m[:] = muro[r0:r0 + hp].astype(np.float32) if muro.shape[0] >= r0 + hp else 0
    m[EXTRA + 640:] = 1                                  # o pé do muro e a mureta: sempre muro
    # O vão do portão: transparente (o pátio de dentro é outra camada, um pouco mais longe).
    px0, py0, px1, py1 = PATIO
    vao = cv2.imread(str(ART / "15_mascara_vao.png"), cv2.IMREAD_GRAYSCALE) > 128
    m[py0:py1, px0:px1][vao] = 0
    patio = cv2.imread(str(ART / "14_patio.webp"))
    cv2.imwrite(str(pasta / "patio.jpg"), patio, [cv2.IMWRITE_JPEG_QUALITY, 94])
    a = cv2.GaussianBlur(m, (0, 0), 0.7)
    rgba = np.dstack([pintada[..., 2], pintada[..., 1], pintada[..., 0], (a * 255).astype(np.uint8)])
    salvar(pasta / "parede.png", rgba)
    (pasta / "parede.jpg").unlink(missing_ok=True)
    parede["imagem"] = "parede.png"
    parede.pop("corAcima", None)

    # --- árvores: a pintura onde elas aparecem; atrás do muro, as do "sem muro".
    hh = ALTO + BASE                                     # linhas -ALTO..BASE
    vis = alto[:hh].astype(np.float32)
    atras = sem[:hh].astype(np.float32)
    ceu_v, ceu_s = ceu_cor(alto[:hh]), ceu_cor(sem[:hh])
    muro_h = muro[:hh]
    atras = casar(atras, vis, ~ceu_s & ~muro_h, ~ceu_v & ~muro_h)
    km = cv2.GaussianBlur(cv2.dilate(muro_h.astype(np.uint8), np.ones((7, 7), np.uint8)).astype(np.float32),
                          (0, 0), 2)[..., None]
    arv = vis * (1 - km) + atras * km
    alfa = np.where(muro_h, ~ceu_s, ~ceu_v).astype(np.float32)
    alfa = cv2.morphologyEx(alfa, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    alfa = cv2.GaussianBlur(alfa, (0, 0), 0.8)
    alfa[:4] = 0
    # Um pouco de ar entre as árvores e o muro: névoa leve e um nada de desfoque.
    nevoa = np.array([245, 227, 207], np.float32)      # BGR do céu claro perto do horizonte
    arv = cv2.GaussianBlur(arv * 0.9 + nevoa * 0.1, (0, 0), 0.6)
    rgba = np.dstack([arv[..., 2], arv[..., 1], arv[..., 0], alfa * 255]).astype(np.uint8)
    salvar(pasta / "arvores.png", rgba)

    # --- céu e nuvens: do "só céu", com as cores do céu da pintura.
    ceu = so_ceu[:hh].astype(np.float32)
    ceu = casar(ceu, vis, np.ones(ceu.shape[:2], bool), ceu_v & ~muro_h)
    hsv = cv2.cvtColor(np.clip(ceu, 0, 255).astype(np.uint8), cv2.COLOR_BGR2HSV).astype(np.float32)
    sat = hsv[..., 1]
    # O azul de cada linha: o mais saturado (sem nuvem), liso.
    linha = np.zeros((hh, 3), np.float32)
    for y in range(hh):
        s = sat[y]
        sel = s >= np.percentile(s, 70)
        linha[y] = ceu[y][sel].mean(0)
    linha = cv2.GaussianBlur(linha[:, None, :], (0, 0), sigmaX=0.1, sigmaY=25)[:, 0, :]
    # Para cima: mais 600 linhas, o azul ficando mais fundo (o super pulo).
    mais = 600
    topo = linha[0] + (np.array([150, 70, 20], np.float32) - linha[0]) * np.linspace(1, 0, mais)[:, None] ** 1.5
    grad = np.concatenate([topo, linha], 0)
    img = np.repeat(grad[:, None, :], 2048, 1)   # largo: poucas cópias lado a lado
    cv2.imwrite(str(pasta / "ceu.jpg"), np.clip(img, 0, 255).astype(np.uint8), [cv2.IMWRITE_JPEG_QUALITY, 95])
    fundo = np.repeat(linha[:, None, :], W, 1)
    sat_f = cv2.cvtColor(np.clip(fundo, 0, 255).astype(np.uint8), cv2.COLOR_BGR2HSV)[..., 1].astype(np.float32)
    an = np.clip(1 - sat / np.maximum(sat_f, 1), 0, 1)
    an = np.clip((an - 0.08) / 0.7, 0, 1)
    an = cv2.GaussianBlur(an, (0, 0), 0.8)
    cor = (ceu - fundo * (1 - an[..., None])) / np.maximum(an[..., None], 0.05)
    cor = np.where(an[..., None] > 0.05, cor, 255)
    rgba = np.dstack([cor[..., 2], cor[..., 1], cor[..., 0], an * 255])
    salvar(pasta / "nuvens.png", np.clip(rgba, 0, 255).astype(np.uint8), metade=True)   # macias

    # A mata ao longe: recortada do magenta, com a névoa da distância (puxando para o azul do céu).
    mata = cv2.imread(str(ART / "16_mata.png"), cv2.IMREAD_UNCHANGED)
    cor_ceu = linha[min(len(linha) - 1, ALTO + MATA[0])]
    am = mata[..., 3:4].astype(np.float32) / 255
    rgb = mata[..., :3].astype(np.float32) * 0.72 + cor_ceu * 0.28
    salvar(pasta / "mata.png", np.dstack([np.clip(rgb[..., ::-1], 0, 255), am * 255]).astype(np.uint8),
           metade=True)   # BGR → RGB

    camadas = [c for c in stage["camadas"] if c["id"] not in ("ceu", "nuvens", "arvores-plano", "mata", "patio")]
    i = camadas.index(parede)
    camadas[i:i] = [
        {"id": "ceu", "imagem": "ceu.jpg", "profundidade": PROF_CEU, "topo": frac(-ALTO - mais),
         "base": frac(BASE), "x": parede["x"], "repetir": "espelhar", "corAcima": "#%02X%02X%02X" % tuple(
             int(v) for v in np.clip(topo[0][::-1], 0, 255))},
        {"id": "nuvens", "imagem": "nuvens.png", "profundidade": PROF_NUVENS, "topo": frac(-ALTO),
         "base": frac(BASE), "x": parede["x"], "repetir": "espelhar", "deriva": DERIVA},
        {"id": "mata", "imagem": "mata.png", "profundidade": PROF_MATA, "topo": frac(MATA[0]),
         "base": frac(MATA[1]), "x": parede["x"], "repetir": "espelhar", "apoio": parede["profundidade"]},
        {"id": "arvores-plano", "imagem": "arvores.png", "profundidade": PROF_ARVORES, "topo": frac(-ALTO),
         "base": frac(BASE), "x": parede["x"], "repetir": "espelhar", "apoio": parede["profundidade"]},
        {"id": "patio", "imagem": "patio.jpg", "profundidade": PROF_PATIO,
         **retangulo(parede, W, hp, *PATIO), "apoio": parede["profundidade"]},
    ]
    # Mata, árvores e pátio "apoiados" no muro: na vertical andam junto com ele (no super pulo a
    # câmera sobe e, soltos, eles subiam atrás do muro: a mata aparecia demais e, no vão do
    # portão, aparecia a grama embaixo do pátio). Na horizontal a paralaxe continua.
    stage["camadas"] = camadas
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"planos: céu {PROF_CEU:.0f}, nuvens {PROF_NUVENS:.0f}, mata {PROF_MATA:.0f}, árvores {PROF_ARVORES:.0f},"
          f" pátio {PROF_PATIO:.0f}, parede {parede['profundidade']}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", nargs="?", type=Path, default=PASTA)
    montar(ap.parse_args().pasta)


if __name__ == "__main__":
    main()
