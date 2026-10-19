#!/usr/bin/env python3
"""Anima as cachoeiras pintadas nas imagens dos cenários.

    python tools/stage/cachoeiras.py android/app/src/main/assets/stages/arena_imperial ...

Para cada imagem conhecida (`AREAS`) que estiver na pasta do cenário, grava
`<imagem>_agua.png` e põe o campo "cachoeiras" na camada dela no stage.json
(docs/stage-engine.md). Os montar_*.py da ilha refazem só as folhas (`atualizar`), sem
mexer no stage.json; rode este script quando mudar as áreas. A imagem é uma grade de quadros: cada linha é uma área (um
retângulo em volta de uma cachoeira) e cada coluna, um quadro da animação.

Em cada quadro, só a água muda: a máscara pega os pixels claros e pouco saturados (a
água branca-azulada; lanternas e estandartes têm cor e ficam de fora) e com risco vertical
(a névoa, lisa, fica de fora). Sobre a água corre um ruído esticado na vertical (faixas
claras e escuras descendo), que repete a cada `QUADROS` passos: o último quadro emenda no
primeiro. As pedras em volta ficam como estão, então nada escorre para fora da cachoeira.
"""

import argparse
import json
import re
import sys
from pathlib import Path

try:
    import numpy as np
    from PIL import Image, ImageFilter
except ImportError:
    sys.exit("Instale o Pillow e o numpy: pip install pillow numpy")

QUADROS = 8
FPS = 12

# Áreas por imagem (x, y, largura, altura em fração da imagem) e o tamanho de um pixel da
# imagem na tela (aproximado, com a câmera de repouso): o ruído e a queda são medidos em
# pixels da tela, para a água ter o mesmo grão de perto e de longe.
AREAS = {
    "palacio.png": {
        "tela": 0.39,
        "areas": [(0.095, 0.56, 0.185, 0.43), (0.72, 0.56, 0.185, 0.43)],
    },
    "abismo.png": {
        "tela": 1.0,
        "sat": 0.5,
        "areas": [(0.425, 0.28, 0.085, 0.43), (0.525, 0.35, 0.045, 0.15), (0.655, 0.29, 0.065, 0.24)],
    },
    "encosta.png": {
        "luz": 0.3,
        "tela": 0.41,
        "areas": [
            (0.755, 0.16, 0.05, 0.07), (0.87, 0.14, 0.03, 0.08), (0.57, 0.3, 0.065, 0.11),
            (0.815, 0.38, 0.05, 0.13), (0.515, 0.51, 0.05, 0.13), (0.355, 0.65, 0.045, 0.14),
            (0.515, 0.68, 0.035, 0.12), (0.335, 0.75, 0.035, 0.12), (0.765, 0.81, 0.07, 0.15),
        ],
    },
    "penhascos.png": {
        "luz": 0.3,
        "tela": 0.3,
        "areas": [
            (0.43, 0.42, 0.03, 0.2), (0.623, 0.47, 0.027, 0.24), (0.75, 0.56, 0.035, 0.33),
            (0.105, 0.8, 0.03, 0.15), (0.615, 0.82, 0.035, 0.14),
        ],
    },
    "templo_longe.png": {
        "luz": 0.3,
        "tela": 0.39,
        "areas": [
            (0.485, 0.29, 0.025, 0.1), (0.68, 0.3, 0.03, 0.17), (0.8, 0.32, 0.03, 0.15),
            (0.685, 0.49, 0.025, 0.12), (0.26, 0.58, 0.025, 0.12), (0.405, 0.66, 0.045, 0.24),
            (0.6, 0.85, 0.035, 0.12), (0.675, 0.68, 0.02, 0.06), (0.825, 0.74, 0.03, 0.18),
        ],
    },
}

# Em pixels da tela: queda por quadro e o grão do ruído (fino na horizontal, comprido na
# vertical, como os fios da água).
QUEDA = 7.0
GRAO_X, GRAO_Y = 1.2, 7.0


def suave(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def mascara(rgb, luz_min=0.38, sat_max=0.28):
    """0..1 por pixel: o quanto é água (clara, sem cor, riscada na vertical). As imagens
    ao longe (com a névoa da distância) têm a água mais escura; a do abismo, iluminada
    pelas tochas, tem cor."""
    mx, mn = rgb.max(axis=2), rgb.min(axis=2)
    luz = rgb.mean(axis=2)
    sat = (mx - mn) / np.maximum(mx, 1e-3)
    clara = suave(luz_min, luz_min + 0.24, luz) * (1.0 - suave(sat_max, sat_max + 0.17, sat))
    # Risco vertical: diferença entre colunas vizinhas, média num trecho vertical.
    dx = np.abs(np.diff(luz, axis=1, append=luz[:, -1:]))
    risco = np.asarray(Image.fromarray((dx * 255).clip(0, 255).astype(np.uint8))
                       .filter(ImageFilter.BoxBlur(3))).astype(np.float32) / 255
    m = clara * suave(0.012, 0.04, risco)
    m = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.2)))
    return m.astype(np.float32) / 255


def borda(h, w, f=0.12):
    """Some nas bordas da área, para a emenda com a imagem não aparecer."""
    y = suave(0, f, np.linspace(0, 1, h)) * suave(0, f, np.linspace(1, 0, h))
    x = suave(0, f * 2, np.linspace(0, 1, w)) * suave(0, f * 2, np.linspace(1, 0, w))
    return y[:, None] * x[None, :]


def ruido(w, periodo, gx, gy, semente):
    """Ruído (0..1) que repete na vertical a cada `periodo` linhas."""
    rng = np.random.default_rng(semente)
    n = rng.random((periodo, w)).astype(np.float32)
    # Desfoque com volta (o ruído emenda em cima e embaixo).
    pad = int(gy * 3) + 1
    alto = np.concatenate([n[-pad:], n, n[:pad]], axis=0) if pad < periodo else np.tile(n, (3, 1))
    img = Image.fromarray((alto * 255).astype(np.uint8))
    # GaussianBlur do PIL é isotrópico: estica, desfoca e volta.
    sx = max(1, round(w * 4 / max(gx, 0.5)))
    tmp = img.resize((sx, img.height), Image.BILINEAR).filter(ImageFilter.GaussianBlur(gy))
    tmp = np.asarray(tmp.resize((w, img.height), Image.BILINEAR)).astype(np.float32) / 255
    off = pad if pad < periodo else periodo
    tmp = tmp[off:off + periodo]
    tmp = (tmp - tmp.mean()) / max(tmp.std(), 1e-3)
    return 1 / (1 + np.exp(-tmp * 1.6))


def quadros(img, area, conf, semente):
    tela = conf["tela"]
    W, H = img.size
    x0, y0 = round(area[0] * W), round(area[1] * H)
    x1, y1 = round((area[0] + area[2]) * W), round((area[1] + area[3]) * H)
    pedaco = np.asarray(img.crop((x0, y0, x1, y1)).convert("RGBA")).astype(np.float32) / 255
    rgb, alfa = pedaco[..., :3], pedaco[..., 3]
    h, w = alfa.shape
    m = mascara(rgb, conf.get("luz", 0.38), conf.get("sat", 0.28)) * borda(h, w)
    passo = max(1, round(QUEDA / tela))
    periodo = passo * QUADROS
    n = ruido(w, periodo, GRAO_X / tela, GRAO_Y / tela, semente)
    n = np.tile(n, (h // periodo + 2, 1))
    saida = []
    for q in range(QUADROS):
        desl = periodo - q * passo  # o ruído desce `passo` linhas por quadro
        nq = n[desl:desl + h]
        # Faixas claras e escuras correndo; nas claras, um pouco de espuma branca.
        fator = 0.72 + 0.56 * nq
        novo = rgb * fator[..., None] + suave(0.65, 0.95, nq)[..., None] * 0.25
        mix = rgb + (novo.clip(0, 1) - rgb) * m[..., None]
        saida.append(np.dstack([mix, alfa]))
    return saida, (x1 - x0, y1 - y0)


def montar(pasta, nome):
    conf = AREAS[nome]
    img = Image.open(pasta / nome).convert("RGBA")
    linhas = []
    for i, area in enumerate(conf["areas"]):
        linhas.append(quadros(img, area, conf, sum(nome.encode()) * 31 + i)[0])
    largura = max(q[0].shape[1] for q in linhas)
    altura = sum(q[0].shape[0] for q in linhas)
    folha = np.zeros((altura, largura * QUADROS, 4), np.float32)
    y = 0
    for qs in linhas:
        h, w = qs[0].shape[:2]
        for c, q in enumerate(qs):
            folha[y:y + h, c * largura:c * largura + w] = q
        y += h
    saida = pasta / (Path(nome).stem + "_agua.png")
    Image.fromarray((folha * 255).round().astype(np.uint8)).save(saida, optimize=True)
    return saida, folha.shape


def anotar(pasta, nome, arquivo):
    """Põe (ou troca) o campo "cachoeiras" logo depois do "imagem" da camada."""
    json_path = pasta / "stage.json"
    texto = json_path.read_text(encoding="utf-8")
    areas = ", ".join("[" + ", ".join(f"{v:g}" for v in a) + "]" for a in AREAS[nome]["areas"])
    campo = (f'"cachoeiras": {{ "imagem": "{arquivo}", "quadros": {QUADROS}, "fps": {FPS}, '
             f'"areas": [{areas}] }},')
    linha = re.search(rf'\n(\s*)"imagem": "{re.escape(nome)}",(\n\s*"cachoeiras": \{{[^\n]*\}},)?', texto)
    if not linha:
        return False
    texto = texto[:linha.end(1) + len(f'"imagem": "{nome}",')] + "\n" + linha.group(1) + campo + texto[linha.end():]
    json.loads(texto)
    json_path.write_text(texto, encoding="utf-8")
    return True


def atualizar(pasta):
    """Refaz as folhas de água das imagens da pasta (os montar_*.py chamam no fim)."""
    for nome in AREAS:
        if (pasta / nome).exists():
            saida, forma = montar(pasta, nome)
            print(f"cachoeiras de {nome} -> {saida.name} {forma[1]}x{forma[0]}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("pastas", nargs="+", type=Path, help="pastas de cenário (com o stage.json)")
    args = parser.parse_args()
    for pasta in args.pastas:
        for nome in AREAS:
            if not (pasta / nome).exists():
                continue
            saida, forma = montar(pasta, nome)
            anotado = anotar(pasta, nome, saida.name)
            print(f"{pasta.name}/{saida.name} {forma[1]}x{forma[0]}" + ("" if anotado else " (camada não achada)"))


if __name__ == "__main__":
    main()
