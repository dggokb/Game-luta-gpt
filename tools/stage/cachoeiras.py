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

Mar agitado pintado (`"tipo": "ondas"`, o fundo do abismo) vira outra folha da mesma
imagem (`<imagem>_ondas.png`): a máscara pega o azul e a espuma crespa (a névoa lisa fica
de fora) e por cima dela vêm, devagar, manchas largas e baixas (veja `quadros_ondas`).

`"espuma"` ([x, y, largura] em fração da imagem, no ponto em que a água bate) não entra na
folha: vai para o stage.json, e o motor solta ali borrifos que sobem e somem.
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
        # O palácio é simétrico: os pés da direita são os da esquerda espelhados.
        "espuma": [(0.162, 0.79, 0.06), (0.137, 0.96, 0.07), (0.199, 0.745, 0.04),
                   (0.838, 0.79, 0.06), (0.863, 0.96, 0.07), (0.801, 0.745, 0.04)],
    },
    "abismo.png": [
        {
            "tela": 1.0,
            "sat": 0.5,
            "areas": [(0.425, 0.28, 0.085, 0.43), (0.525, 0.35, 0.045, 0.15), (0.655, 0.29, 0.065, 0.24)],
            "espuma": [(0.464, 0.696, 0.08), (0.43, 0.9, 0.06), (0.551, 0.463, 0.05), (0.693, 0.52, 0.07)],
            # A névoa do abismo é rosada (as tochas).
            "corEspuma": "#F6DCEA",
        },
        # O mar batendo no fundo do abismo, embaixo das quedas.
        {"tipo": "ondas", "tela": 0.78, "areas": [(0.35, 0.66, 0.4, 0.27)]},
    ],
    "encosta.png": {
        "luz": 0.3,
        "tela": 0.41,
        "areas": [
            (0.755, 0.16, 0.05, 0.07), (0.87, 0.14, 0.03, 0.08), (0.57, 0.3, 0.065, 0.11),
            (0.815, 0.38, 0.05, 0.13), (0.515, 0.51, 0.05, 0.13), (0.355, 0.65, 0.045, 0.14),
            (0.515, 0.68, 0.035, 0.12), (0.335, 0.75, 0.035, 0.12), (0.765, 0.81, 0.07, 0.15),
        ],
        "espuma": [(0.599, 0.38, 0.04), (0.841, 0.482, 0.04), (0.535, 0.61, 0.035), (0.37, 0.728, 0.03),
                   (0.529, 0.754, 0.03), (0.351, 0.83, 0.03), (0.809, 0.915, 0.06)],
        "corEspuma": "#D4CCEC",
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

# Mar agitado ("tipo": "ondas"): mais quadros, mais devagar, em blocos de 4 por fileira
# (a folha não passa de uns 2500 px de largura). As manchas são largas e baixas (cristas)
# e vêm devagar na direção da câmera, `ONDA` pixels da tela por ciclo.
ONDAS_QUADROS, ONDAS_FPS, ONDAS_COLUNAS = 16, 8, 4
ONDA = 10.0
GRAO_ONDA_X, GRAO_ONDA_Y = 6.0, 1.6


def grupos(nome):
    """As folhas de uma imagem: uma por tipo de água (queda, ondas)."""
    conf = AREAS[nome]
    lista = conf if isinstance(conf, list) else [conf]
    for g in lista:
        ondas = g.get("tipo") == "ondas"
        g.setdefault("quadros", ONDAS_QUADROS if ondas else QUADROS)
        g.setdefault("fps", ONDAS_FPS if ondas else FPS)
        g.setdefault("colunas", ONDAS_COLUNAS if ondas else g["quadros"])
        g.setdefault("sufixo", "_ondas" if ondas else "_agua")
    return lista


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


def mascara_ondas(rgb):
    """0..1 por pixel: mar (azul) e espuma (clara, sem cor, crespa). A pedra escura e a névoa
    (clara mas lisa, ficaria listrada) ficam de fora."""
    mx, mn = rgb.max(axis=2), rgb.min(axis=2)
    luz = rgb.mean(axis=2)
    sat = (mx - mn) / np.maximum(mx, 1e-3)
    azul = suave(-0.02, 0.06, rgb[..., 2] - rgb[..., 0]) * suave(0.14, 0.26, luz)
    grad = np.abs(np.diff(luz, axis=1, append=luz[:, -1:])) + np.abs(np.diff(luz, axis=0, append=luz[-1:]))
    crespa = np.asarray(Image.fromarray((grad * 255).clip(0, 255).astype(np.uint8))
                        .filter(ImageFilter.BoxBlur(4))).astype(np.float32) / 255
    espuma = suave(0.45, 0.65, luz) * (1.0 - suave(0.3, 0.45, sat)) * suave(0.03, 0.07, crespa)
    m = np.maximum(azul, espuma)
    m = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.5)))
    return m.astype(np.float32) / 255


def ruido_solto(h, w, gx, gy, semente):
    """Ruído (0..1) sem repetir, com grão `gx` na horizontal e `gy` na vertical."""
    rng = np.random.default_rng(semente)
    img = Image.fromarray((rng.random((h, w)) * 255).astype(np.uint8))
    sx = max(1, round(w * gy / max(gx, 0.5)))
    tmp = img.resize((sx, h), Image.BILINEAR).filter(ImageFilter.GaussianBlur(gy))
    tmp = np.asarray(tmp.resize((w, h), Image.BILINEAR)).astype(np.float32) / 255
    tmp = (tmp - tmp.mean()) / max(tmp.std(), 1e-3)
    return 1 / (1 + np.exp(-tmp * 1.6))


def quadros_ondas(rgb, alfa, conf, semente):
    """Mar agitado: dois ruídos descem devagar, cada um vai sumindo enquanto o outro
    aparece (meio ciclo de diferença), então o ciclo fecha sem a imagem pular e sem o
    ruído precisar se repetir no espaço."""
    h, w = alfa.shape
    tela = conf["tela"]
    m = mascara_ondas(rgb) * borda(h, w, 0.08)
    queda = max(2, round(ONDA / tela))
    fontes = [ruido_solto(h + queda, w, GRAO_ONDA_X / tela, GRAO_ONDA_Y / tela, semente + k) for k in (0, 7)]
    espuma = suave(0.4, 0.7, rgb.mean(axis=2))
    saida = []
    total = conf["quadros"]
    for q in range(total):
        fases = (q / total, (q / total + 0.5) % 1.0)
        pesos = [1 - abs(1 - 2 * f) for f in fases]
        n = np.zeros((h, w), np.float32)
        for fonte, f, peso in zip(fontes, fases, pesos):
            desl = round(f * queda)  # desce `queda` linhas por ciclo
            n += peso * fonte[queda - desl:queda - desl + h]
        # Misturar dois ruídos tira contraste no meio do caminho: devolve.
        n = 0.5 + (n - 0.5) / max(np.hypot(*pesos), 1e-3)
        novo = rgb * (0.8 + 0.4 * n)[..., None] + (suave(0.62, 0.9, n) * espuma * 0.22)[..., None]
        mix = rgb + (novo.clip(0, 1) - rgb) * m[..., None]
        saida.append(np.dstack([mix, alfa]))
    return saida


def quadros(img, area, conf, semente):
    tela = conf["tela"]
    W, H = img.size
    x0, y0 = round(area[0] * W), round(area[1] * H)
    x1, y1 = round((area[0] + area[2]) * W), round((area[1] + area[3]) * H)
    pedaco = np.asarray(img.crop((x0, y0, x1, y1)).convert("RGBA")).astype(np.float32) / 255
    rgb, alfa = pedaco[..., :3], pedaco[..., 3]
    h, w = alfa.shape
    if conf.get("tipo") == "ondas":
        return quadros_ondas(rgb, alfa, conf, semente), (x1 - x0, y1 - y0)
    m = mascara(rgb, conf.get("luz", 0.38), conf.get("sat", 0.28)) * borda(h, w)
    passo = max(1, round(QUEDA / tela))
    periodo = passo * conf["quadros"]
    n = ruido(w, periodo, GRAO_X / tela, GRAO_Y / tela, semente)
    n = np.tile(n, (h // periodo + 2, 1))
    saida = []
    for q in range(conf["quadros"]):
        desl = periodo - q * passo  # o ruído desce `passo` linhas por quadro
        nq = n[desl:desl + h]
        # Faixas claras e escuras correndo; nas claras, um pouco de espuma branca.
        fator = 0.72 + 0.56 * nq
        novo = rgb * fator[..., None] + suave(0.65, 0.95, nq)[..., None] * 0.25
        mix = rgb + (novo.clip(0, 1) - rgb) * m[..., None]
        saida.append(np.dstack([mix, alfa]))
    return saida, (x1 - x0, y1 - y0)


def montar(pasta, nome):
    """Grava as folhas da imagem; devolve [(arquivo, (altura, largura), grupo)]."""
    img = Image.open(pasta / nome).convert("RGBA")
    feitas = []
    for g_i, conf in enumerate(grupos(nome)):
        linhas = []
        for i, area in enumerate(conf["areas"]):
            linhas.append(quadros(img, area, conf, sum(nome.encode()) * 31 + g_i * 1000 + i)[0])
        largura = max(q[0].shape[1] for q in linhas)
        altura = sum(q[0].shape[0] for q in linhas)
        colunas = conf["colunas"]
        blocos = -(-conf["quadros"] // colunas)
        # Cada bloco é um quadro (as áreas empilhadas); `colunas` blocos por fileira.
        folha = np.zeros((altura * blocos, largura * colunas, 4), np.float32)
        y = 0
        for qs in linhas:
            h, w = qs[0].shape[:2]
            for c, q in enumerate(qs):
                bx, by = (c % colunas) * largura, (c // colunas) * altura
                folha[by + y:by + y + h, bx:bx + w] = q
            y += h
        saida = pasta / (Path(nome).stem + conf["sufixo"] + ".png")
        Image.fromarray((folha * 255).round().astype(np.uint8)).save(saida, optimize=True)
        feitas.append((saida, folha.shape, conf))
    return feitas


def anotar(pasta, nome, feitas):
    """Põe (ou troca) o campo "cachoeiras" logo depois do "imagem" da camada."""
    json_path = pasta / "stage.json"
    texto = json_path.read_text(encoding="utf-8")
    itens = []
    for arquivo, _, conf in feitas:
        areas = ", ".join("[" + ", ".join(f"{v:g}" for v in a) + "]" for a in conf["areas"])
        colunas = f'"colunas": {conf["colunas"]}, ' if conf["colunas"] != conf["quadros"] else ""
        espuma = ""
        if conf.get("espuma"):
            pes = ", ".join("[" + ", ".join(f"{v:g}" for v in pe) + "]" for pe in conf["espuma"])
            espuma = f', "espuma": [{pes}]'
            if conf.get("corEspuma"):
                espuma += f', "corEspuma": "{conf["corEspuma"]}"'
        itens.append(f'{{ "imagem": "{arquivo.name}", "quadros": {conf["quadros"]}, {colunas}"fps": {conf["fps"]}, '
                     f'"areas": [{areas}]{espuma} }}')
    campo = '"cachoeiras": ' + (itens[0] if len(itens) == 1 else "[" + ", ".join(itens) + "]") + ","
    linha = re.search(rf'\n(\s*)"imagem": "{re.escape(nome)}",(\n\s*"cachoeiras": [^\n]*,)?', texto)
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
            for saida, forma, _ in montar(pasta, nome):
                print(f"água de {nome} -> {saida.name} {forma[1]}x{forma[0]}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("pastas", nargs="+", type=Path, help="pastas de cenário (com o stage.json)")
    args = parser.parse_args()
    for pasta in args.pastas:
        for nome in AREAS:
            if not (pasta / nome).exists():
                continue
            feitas = montar(pasta, nome)
            anotado = anotar(pasta, nome, feitas)
            for saida, forma, _ in feitas:
                print(f"{pasta.name}/{saida.name} {forma[1]}x{forma[0]}" + ("" if anotado else " (camada não achada)"))


if __name__ == "__main__":
    main()
