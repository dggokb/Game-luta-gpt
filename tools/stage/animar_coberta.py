#!/usr/bin/env python3
"""Anima a Arena Coberta (``stages/arena_coberta``), montada pelo ``cenario_pintura.py`` a partir do
desenho inteiro da arena oval (``art/stages/arena_coberta/7_panorama_oval_4096.webp``).

    python3 tools/stage/animar_coberta.py [pasta]

* **telão**: a tela do telão da pintura recebe a luta ao vivo e o replay (``"telao"`` na camada);
* **plateia**: a gente das arquibancadas pula de leve e solta flashes de câmera;
* **luzes**: os spots do teto pulsam de leve;
* **fachos**: cones de luz saindo dos spots do teto, varrendo devagar (duas imagens com os fachos
  em ângulos diferentes, cruzando uma na outra);
* **chão**: o encerado pintado, com o reflexo dos lutadores.

As posições são em pixels do panorama de 4096; a parede tem ``EXTRA`` linhas de escuro em cima.
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
PANORAMA = AQUI.parents[1] / "art/stages/arena_coberta/7_panorama_oval_4096.webp"
EXTRA = 300                                   # --ceu-extra do cenario_pintura.py
TELA = (1786, 723, 2234, 892)                 # a tela azul do telão (x0, y0, x1, y1) no panorama
PLATEIA = (740, 965)                          # faixa das arquibancadas (linhas do panorama)
JUNCAO = 1010                                 # --juncao do cenario_pintura.py (linha do panorama)
TETO = 760                                    # só as lâmpadas acima desta linha (o anel do teto)
CIMA = 640                                    # os fachos saem das lâmpadas acima desta linha
ALVO_Y = 1250                                 # a linha do chão para onde os fachos miram
MAX_LUZES = 90
MAX_FACHOS = 11
SEPARA = 38                                   # px do panorama: mais perto, é a mesma lâmpada
BALANCO = 7                                   # graus: o facho vai de −7 a +7 (cruzamento das imagens)
ALFA_FACHO = 0.42
REFLEXO_FORCA = 0.85                          # o risco de luz de cada lanterna no chão (motor)
REFLEXO_COMPRIMENTO = 0.45
RISCO_ESTILO = "curto"                        # "curto" (brilho macio) ou "poca" (gota de luz)


def holofotes(w, h, so_lampadas=False):
    """Os spots do teto: as lâmpadas (branco-amarelado forte) no alto do panorama original (na
    parede o alto está escurecido e as da fileira de cima sumiam). Uma luz por spot: as que ficam
    a menos de SEPARA px de uma maior são a mesma lâmpada."""
    pan = np.asarray(Image.open(PANORAMA).convert("RGB")).astype(np.int16)
    sx = w / pan.shape[1]
    mn = pan.min(axis=2)
    m = ((mn > 215) & (pan[..., 0] > 240)).astype(np.uint8)
    m[TETO:] = 0
    m[TELA[1] - 140:, int(TELA[0] - 40):int(TELA[2] + 40)] = 0       # o telão e as lâmpadas dele
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    n, _, st, cen = cv2.connectedComponentsWithStats(m, 8)
    achados = sorted([(st[i, cv2.CC_STAT_AREA], st[i], cen[i]) for i in range(1, n)
                      if st[i, cv2.CC_STAT_AREA] >= 25 and st[i, 2] < 4 * st[i, 3] + 8],
                     key=lambda t: -t[0])
    luzes = []
    for _, s_, c in achados:
        if any(np.hypot(c[0] - x, c[1] - y) < SEPARA for x, y, _ in luzes):
            continue
        luzes.append((c[0], c[1], max(s_[2], s_[3])))
        if len(luzes) >= MAX_LUZES:
            break
    if so_lampadas:
        return luzes
    return [{"x": round(float(x * sx / w), 4), "y": round(float((y + EXTRA) / h), 4),
             "raio": round(float(max(10, r * 1.5) / h), 4), "cor": "#FFE8B8", "pulsar": 0.05,
             "reflexo": False} for x, y, r in sorted(luzes)]


def fachos(pasta, parede, luzes_pan, xa, xb, w, h):
    """Fachos de luz dos spots da fileira de cima do teto, mirando o chão da luta, pintados em duas
    imagens (cada facho num ângulo em cada uma: ± BALANCO graus). As duas são camadas iguais à
    parede, com "onda" em fase oposta: uma entra enquanto a outra sai e a luz parece varrer.
    Duas imagens por quadro, em vez de um cone por spot (pesava no Canvas)."""
    escala = 4                                         # as imagens em 1/4 (luz é macia)
    fw, fh = w // escala, h // escala
    yy, xx = np.mgrid[0:fh, 0:fw].astype(np.float32)
    cima = sorted(l for l in luzes_pan if l[1] < CIMA and xa + 60 < l[0] < xb - 60)
    passo = max(1, int(np.ceil(len(cima) / MAX_FACHOS)))
    escolhidos = cima[::passo]
    alvo_y = ALVO_Y + EXTRA
    corte = JUNCAO - 8 + EXTRA                          # os fachos param na barreira (o chão fica limpo)
    for nome, sinal in (("a", -1), ("b", 1)):
        luz = np.zeros((fh, fw), np.float32)
        cor = np.zeros((fh, fw, 3), np.float32)
        for i, (x, y, _) in enumerate(escolhidos):
            y0 = y + EXTRA
            tx = 2048 + (x - 2048) * 0.35
            ang = np.arctan2(tx - x, alvo_y - y0) + np.radians(BALANCO * sinal * (1 if i % 2 else -1))
            dx, dy = np.sin(ang), np.cos(ang)
            px, py = (xx * escala - x), (yy * escala - y0)
            ao_longo = px * dx + py * dy
            lado = px * dy - py * dx
            comp = (alvo_y - y0) * 1.1 / max(0.3, dy)
            t = np.clip(ao_longo / comp, 0, 1)
            meia = 6 + t * comp * 0.17
            cone = np.exp(-0.5 * (lado / meia) ** 2) * (ao_longo > 0) * np.clip(ao_longo / 30, 0, 1)
            cone *= (1 - t) ** 1.3
            c = np.array((190, 150, 255) if i % 4 == 2 else (255, 226, 170), np.float32)
            luz = np.maximum(luz, cone)
            cor += cone[..., None] * c
        luz *= np.clip((corte - yy * escala) / 40.0, 0, 1)    # some antes do chão
        rgb = cor / np.maximum(luz[..., None], 1e-4)
        rgb = rgb + (255 - rgb) * (luz ** 3)[..., None] * 0.5          # o miolo mais branco
        a = np.clip(luz * ALFA_FACHO, 0, 1) * 255
        Image.fromarray(np.dstack([np.clip(rgb, 0, 255), a]).astype(np.uint8), "RGBA").save(
            pasta / f"fachos_{nome}.png", optimize=True)
    camadas = []
    for nome, fase in (("a", 0.0), ("b", 3.1416)):
        camadas.append({"id": f"fachos-{nome}", "imagem": f"fachos_{nome}.png",
                        "profundidade": round(parede["profundidade"] + 1, 1), "topo": parede["topo"],
                        "base": parede["base"], "x": parede["x"],
                        "onda": {"amplitude": 1, "velocidade": 0.45, "fase": fase, "leve": 0.5}})
    return camadas, len(escolhidos)


def base_da_barreira(pan):
    """A linha onde o chão encontra a barreira, coluna a coluna (linhas do panorama): o degrau
    claro (chão) logo abaixo do rodapé escuro, lisa."""
    lum = cv2.GaussianBlur(pan.mean(axis=2).astype(np.float32), (0, 0), 2)
    g = cv2.Sobel(lum, cv2.CV_32F, 0, 1, ksize=5)
    w = pan.shape[1]
    xs = np.arange(0, w, 16)
    ys = np.array([JUNCAO - 60 + int(np.argmax(g[JUNCAO - 60:JUNCAO + 140, max(0, x - 8):x + 8].mean(1)))
                   for x in xs], np.float32)
    ys = np.array([np.median(ys[max(0, i - 3):i + 4]) for i in range(len(ys))], np.float32)
    ys = np.array([ys[max(0, i - 3):i + 4].max() for i in range(len(ys))], np.float32)
    ys = np.convolve(np.pad(ys, 4, mode="edge"), np.ones(9) / 9, mode="valid")
    return np.interp(np.arange(w), xs, ys)


def lanternas(pan, base):
    """As lanternas da barreira (amarelo forte, logo acima do pé dela), uma por lanterna, em
    pixels do panorama: viram luzes com reflexo, e o motor desenha o risco no chão encerado,
    sempre vertical (o chão é o pintado sem os riscos: pintados, abriam em leque com a câmera
    andando)."""
    im = pan.astype(np.int16)
    r, g, b = im[..., 0], im[..., 1], im[..., 2]
    m = ((r > 235) & (g > 170) & (b < 200)).astype(np.uint8)
    yy = np.arange(pan.shape[0])[:, None]
    m[(yy < base[None, :] - 60) | (yy > base[None, :] - 6)] = 0
    m = cv2.dilate(m, np.ones((5, 5), np.uint8))
    n, _, st, cen = cv2.connectedComponentsWithStats(m, 8)
    achados = sorted([(st[i, 4], cen[i], st[i]) for i in range(1, n)
                      if 40 <= st[i, 4] <= 2500 and st[i, 2] <= 50 and st[i, 3] <= 70], key=lambda t: -t[0])
    lista = []
    for _, (cx, cy), s_ in achados:
        if any(abs(cx - x) < 40 for x, _, _ in lista):
            continue
        lista.append((float(cx), float(cy), float(max(s_[2], s_[3]))))
    return sorted(lista)


def luz_reflexo(u, v, raio_frac):
    return {"x": round(u, 4), "y": round(v, 4), "raio": round(raio_frac, 4), "cor": "#FFC060",
            "pulsar": 0.04, "reflexo": True}


def risco(pasta):
    """O reflexo de uma luz no chão encerado (``reflexoImagem``), que o motor estica do pé de cada
    lanterna para baixo, sempre reto. RISCO_ESTILO "curto": um brilho macio e largo que some
    logo (nada de fio comprido); "poca": uma poça de luz em gota, ainda mais curta."""
    w, h = 64, 256
    x = np.linspace(-1, 1, w)[None, :]
    v = np.linspace(0, 1, h)[:, None]
    if RISCO_ESTILO == "poca":
        a = np.exp(-(x / (0.55 - 0.25 * v)) ** 2) * np.exp(-(v / 0.45) ** 2) * np.clip(v / 0.04, 0, 1)
        miolo = np.exp(-(x / 0.18) ** 2) * np.exp(-(v / 0.2) ** 2)
    else:
        a = np.exp(-(x / (0.45 - 0.15 * v)) ** 2) * (1 - v) ** 1.6 * np.clip(v / 0.04, 0, 1)
        miolo = np.exp(-(x / 0.14) ** 2) * (1 - v) ** 3
    a = np.clip(a * 0.85 + miolo * 0.3, 0, 1)
    t = np.clip(miolo, 0, 1)[..., None]
    rgb = np.array([255, 176, 80], np.float32) * (1 - t) + np.array([255, 240, 205], np.float32) * t
    Image.fromarray(np.dstack([rgb, a * 255]).astype(np.uint8), "RGBA").save(pasta / "risco.png", optimize=True)
    return "risco.png"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pasta", nargs="?", type=Path, default=PASTA)
    pasta = ap.parse_args().pasta
    stage_path = pasta / "stage.json"
    stage = json.loads(stage_path.read_text(encoding="utf-8"))
    parede = next(c for c in stage["camadas"] if c["id"] == "parede")
    imagem = Image.open(pasta / parede["imagem"]).convert("RGBA")
    w, h = imagem.size

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
    pan = np.asarray(Image.open(PANORAMA).convert("RGB"))
    lant = lanternas(pan, base_da_barreira(pan))
    sx = w / pan.shape[1]
    # O chão é o pintado sem os riscos de luz (CHAO_FOSCO); o motor desenha o reflexo de cada
    # lanterna, sempre reto embaixo dela, com a câmera em qualquer lugar.
    parede["luzes"] = holofotes(w, h) + [luz_reflexo(x * sx / w, (y + EXTRA) / h, max(10.0, r * 0.9) / h)
                                         for x, y, r in lant]
    parede.pop("fachos", None)
    camadas_fachos, nfachos = fachos(pasta, parede, holofotes(w, h, so_lampadas=True), 0, w, w, h)
    stage["camadas"] = [c for c in stage["camadas"] if c["id"] == "parede"] + camadas_fachos

    stage["chao"]["reflexoLutadores"] = 0.22
    stage["chao"]["reflexoLuzes"] = REFLEXO_FORCA
    stage["chao"]["reflexoComprimento"] = REFLEXO_COMPRIMENTO
    stage["chao"]["reflexoImagem"] = risco(pasta)
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"telão {parede['telao']}, plateia com {len(flashes)} flashes, {len(parede['luzes'])} holofotes, "
          f"{nfachos} fachos, {len(lant)} lanternas com reflexo")


if __name__ == "__main__":
    main()
