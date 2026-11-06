#!/usr/bin/env python3
"""Anima a Arena Coberta (``stages/arena_coberta``), montada pelo ``cenario_pintura.py``.

    python3 tools/stage/animar_coberta.py [pasta]

* **telão**: a tela do telão da pintura recebe a luta ao vivo e o replay (``"telao"`` na camada);
* **plateia**: a gente das arquibancadas pula de leve e solta flashes de câmera;
* **luzes**: os holofotes do teto pulsam de leve;
* **chão**: encerado, com o reflexo dos lutadores;
* **lados**: as pontas da arquibancada, que curvam vindo para perto da câmera, viram as camadas
  ``parede_esq`` e ``parede_dir`` com ``"curva"`` (cada coluna na profundidade da base da barreira
  dela), sem a parte de baixo (o chão aparece); sem isso a base curva virava chão esticado;
* **fachos**: cones de luz saindo dos spots do teto, varrendo devagar (duas imagens com os fachos
  em ângulos diferentes, cruzando uma na outra).

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
MAX_LUZES = 70
MAX_FACHOS = 11
BALANCO = 7                                   # graus: o facho vai de −7 a +7 (cruzamento das imagens)
ALFA_FACHO = 0.42
JUNCAO = 520                                  # --juncao do cenario_pintura.py (linha do panorama)
SEPARA = 38                                   # px do panorama: mais perto, é a mesma lâmpada
TETO = 300                                    # só as lâmpadas acima desta linha do panorama (o teto)
PANORAMA = AQUI.parents[1] / "art/stages/arena_coberta/2_panorama_4096.webp"


def holofotes(w, h, so_lampadas=False):
    """Os spots do teto: as lâmpadas (branco-amarelado forte) no alto do panorama original (na
    parede o alto está escurecido e as da fileira de cima sumiam). Uma luz por spot: as que ficam
    a menos de SEPARA px de uma maior são a mesma lâmpada."""
    pan = np.asarray(Image.open(PANORAMA).convert("RGB")).astype(np.int16)
    sx = w / pan.shape[1]
    mn = pan.min(axis=2)
    m = ((mn > 215) & (pan[..., 0] > 240)).astype(np.uint8)
    m[TETO:] = 0
    m[:, int(TELA[0] - 40):int(TELA[2] + 40)][TELA[1] - 10:] = 0       # o telão e as lâmpadas dele
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


D, ZOOM_QUADRO, HORIZONTE, GY = 1000.0, 0.948, 0.3786 * 720, 552.0   # do cenario_pintura.py


def base_da_barreira(pan):
    """A linha onde o chão encontra a barreira, coluna a coluna (linhas do panorama): o degrau
    claro (chão) logo abaixo do rodapé escuro, liso e nunca acima da junção do meio."""
    lum = cv2.GaussianBlur(pan.mean(axis=2).astype(np.float32), (0, 0), 2)
    g = cv2.Sobel(lum, cv2.CV_32F, 0, 1, ksize=5)
    w = pan.shape[1]
    xs = np.arange(0, w, 16)
    ys = np.array([470 + int(np.argmax(g[470:700, max(0, x - 8):x + 8].mean(1))) for x in xs], np.float32)
    ys = np.array([np.median(ys[max(0, i - 3):i + 4]) for i in range(len(ys))], np.float32)
    # O pé das lanternas e dos pilares fica na frente da barreira: a linha passa embaixo deles
    # (o máximo por perto) e fica lisa.
    ys = np.array([ys[max(0, i - 3):i + 4].max() for i in range(len(ys))], np.float32)
    ys = np.convolve(np.pad(ys, 4, mode="edge"), np.ones(9) / 9, mode="valid")
    ys = np.maximum(ys, JUNCAO)
    return np.interp(np.arange(w), xs, ys)


def lados(pasta, parede, base, h):
    """As pontas curvas: camadas com "curva" a partir da parede (o mesmo recorte de tela)."""
    img = np.asarray(Image.open(pasta / parede["imagem"]).convert("RGB"))
    w = img.shape[1]
    t, b = parede["topo"] * 720, parede["base"] * 720
    linha_y = lambda r: t + r / h * (b - t)                       # linha da parede -> y na tela de repouso
    prof = lambda y: max(60.0, ((GY - HORIZONTE) * D / max(1.0, y - HORIZONTE) - D) / ZOOM_QUADRO)
    wr = (b - t) * w / h
    esq = 0.5 * 1280 - wr / 2
    curva = base > JUNCAO + 4
    meio = np.nonzero(~curva)[0]
    xa, xb = int(meio[0]), int(meio[-1])
    camadas, pad = [], 96
    for nome, c0, c1 in (("esq", 0, xa + pad), ("dir", xb - pad, w)):
        if c1 - c0 <= pad + 8:
            continue
        rgba = np.dstack([img[:, c0:c1], np.full((h, c1 - c0), 255, np.uint8)])
        rows = np.arange(h)[:, None]
        fundo = (base[c0:c1] + EXTRA + 3)[None, :]
        a = np.clip((fundo - rows) / 3.0, 0, 1)                    # some abaixo da base (borda macia)
        borda = np.linspace(0, 1, pad)                              # junta com o meio aos poucos
        lado = np.ones(c1 - c0)
        if nome == "esq":
            lado[-pad:] = borda[::-1]
        else:
            lado[:pad] = borda
        rgba[..., 3] = (a * lado[None, :] * 255).astype(np.uint8)
        arq = f"parede_{nome}.png"
        Image.fromarray(rgba, "RGBA").save(pasta / arq, optimize=True)
        us = np.arange(c0, c1, 32).tolist() + [c1 - 1]
        pts = [[round((x - c0) / (c1 - c0), 4), round(prof(linha_y(base[x] + EXTRA)), 1)] for x in us]
        camadas.append({"id": f"parede-{nome}", "imagem": arq, "profundidade": max(p[1] for p in pts),
                        "topo": parede["topo"], "base": parede["base"],
                        "x": round((esq + (c0 + c1) / 2 / w * wr) / 1280, 4), "curva": pts})
    print(f"lados: meio reto de {xa} a {xb}; junção do meio z={prof(linha_y(JUNCAO + EXTRA)):.0f}, "
          f"pontas z={prof(linha_y(base[0] + EXTRA)):.0f} / {prof(linha_y(base[-1] + EXTRA)):.0f}")
    return camadas, xa, xb


def fachos(pasta, parede, luzes_pan, xa, xb, w, h):
    """Fachos de luz dos spots da fileira de cima do teto, mirando o chão da luta, pintados em duas
    imagens (cada facho num ângulo em cada uma: ± BALANCO graus). As duas são camadas iguais à
    parede, com "onda" em fase oposta: uma entra enquanto a outra sai e a luz parece varrer.
    Duas imagens por quadro, em vez de um cone por spot (pesava no Canvas)."""
    escala = 4                                         # as imagens em 1/4 (luz é macia)
    fw, fh = w // escala, h // escala
    yy, xx = np.mgrid[0:fh, 0:fw].astype(np.float32)
    cima = sorted(l for l in luzes_pan if l[1] < 150 and xa + 60 < l[0] < xb - 60)
    passo = max(1, int(np.ceil(len(cima) / MAX_FACHOS)))
    escolhidos = cima[::passo]
    alvo_y = 760 + EXTRA
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
        rgb = cor / np.maximum(luz[..., None], 1e-4)
        rgb = rgb + (255 - rgb) * (luz ** 3)[..., None] * 0.5          # o miolo mais branco
        a = np.clip(luz * ALFA_FACHO, 0, 1) * 255
        Image.fromarray(np.dstack([np.clip(rgb, 0, 255), a]).astype(np.uint8), "RGBA").save(
            pasta / f"fachos_{nome}.png", optimize=True)
    camadas = []
    for nome, fase in (("a", 0.0), ("b", 3.1416)):
        camadas.append({"id": f"fachos-{nome}", "imagem": f"fachos_{nome}.png",
                        "profundidade": round(parede["profundidade"] - 1, 1), "topo": parede["topo"],
                        "base": parede["base"], "x": parede["x"],
                        "onda": {"amplitude": 1, "velocidade": 0.45, "fase": fase, "leve": 0.5}})
    return camadas, len(escolhidos)


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
    pan = np.asarray(Image.open(PANORAMA).convert("RGB"))
    base = base_da_barreira(pan)
    novas, xa, xb = lados(pasta, parede, base, h)
    stage["camadas"] = [c for c in stage["camadas"] if not c["id"].startswith("parede-")] + novas
    lampadas = holofotes(w, h, so_lampadas=True)
    parede["luzes"] = [l for l in holofotes(w, h) if xa / w < l["x"] < xb / w]
    parede.pop("fachos", None)
    camadas_fachos, nfachos = fachos(pasta, parede, lampadas, xa, xb, w, h)
    stage["camadas"] = [c for c in stage["camadas"] if not c["id"].startswith("fachos-")] + camadas_fachos

    stage["chao"]["reflexoLutadores"] = 0.22
    stage_path.write_text(json.dumps(stage, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"telão {parede['telao']}, plateia com {len(flashes)} flashes, {len(parede['luzes'])} holofotes, "
          f"{nfachos} fachos, {len(novas)} pontas curvas")


if __name__ == "__main__":
    main()
