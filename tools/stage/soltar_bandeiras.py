#!/usr/bin/env python3
"""Solta os estandartes vermelhos pintados nas imagens da Ilha do Torneio, para balançarem.

    python tools/stage/soltar_bandeiras.py android/app/src/main/assets/stages/arena_imperial ...

O separar_bandeiras.py corta uma bandeira por cenário, com o corte medido à mão. Aqui
são dezenas (pórticos, pilastras, holofotes, arquibancadas, ponte, palácio...), então só
o retângulo em volta de cada uma é dado (`BANDEIRAS`, em fração da imagem, com folga) e
o resto sai da cor:

- O pano é o vermelho dentro do retângulo; em cada linha ele vai do primeiro ao último
  pixel vermelho (o emblema dourado no meio vem junto) e se estende pela borda dourada.
  Fica a maior sequência de linhas seguidas (a vara de cima, dourada, fica de fora).
- As primeiras linhas do pano (`PRESO`) continuam na imagem: ali ela está presa e quase
  não se mexe, então a emenda não aparece. A bandeira leva `SOBRA` linhas acima do corte.
- O buraco na imagem é tapado (`tapar`): transparente quando ela pendia no vão (pórtico,
  holofote, ponte, ruínas), ou com a parede dos lados, lisa e na sombra: quando a ponta
  balança, aparece o fundo e não um recorte.

Grava `<imagem>_bandeira<N>.png` e põe "bandeiras" nas camadas da imagem no stage.json.
Os montar_*.py da ilha chamam `soltar` no fim (antes das cachoeiras). A imagem já solta
é recusada (não tem mais vermelho nos retângulos).
"""

import argparse
import json
import re
import sys
from pathlib import Path

try:
    import numpy as np
    from PIL import Image
except ImportError:
    sys.exit("Instale o Pillow e o numpy: pip install pillow numpy")

SOBRA = 3
PRESO = 0.03
# Bandeira presa numa parede balança menos que uma solta no vão.
SOLTA, NA_PAREDE = 0.12, 0.06


def px(w, h, *caixas):
    """Retângulos medidos em pixels de uma imagem w x h, em fração."""
    return [(x0 / w, y0 / h, x1 / w, y1 / h) for x0, y0, x1, y1 in caixas]


# Por imagem: balanço, o que fica atrás ("vao" ou "parede", veja `tapar`) e os retângulos (x0, y0, x1, y1 em fração), com folga em volta do pano.
BANDEIRAS = {
    "portico.png": (SOLTA, "vao", px(766, 1512, (222, 315, 545, 1295))),
    "pilastra.png": (NA_PAREDE, "parede", px(490, 1527, (185, 180, 368, 1100))),
    "holofote.png": (SOLTA, "vao", px(492, 1536, (155, 410, 330, 1170))),
    "ponte.png": (SOLTA, "vao", px(1672, 465, (240, 45, 332, 302), (1343, 42, 1432, 302))),
    "ruinas.png": (SOLTA, "vao", px(1428, 1068, (398, 362, 470, 645), (1283, 258, 1372, 620))),
    "terminal.png": (NA_PAREDE, "parede", px(1420, 867, (665, 52, 755, 392), (18, 322, 104, 610), (1318, 322, 1402, 610))),
    "palacio.png": (NA_PAREDE, "parede", px(
        1448, 928, (42, 48, 133, 408), (1315, 48, 1408, 408), (143, 242, 180, 333), (1270, 242, 1307, 333))),
    "arquibancada_lateral.png": (NA_PAREDE, "parede", px(
        1433, 840, (165, 178, 268, 525), (1165, 178, 1265, 525), (55, 412, 115, 571), (1317, 412, 1376, 571),
        (500, 342, 556, 488), (875, 342, 931, 488), (482, 629, 540, 773), (892, 629, 950, 773))),
    "abismo.png": (NA_PAREDE, "parede", px(
        1672, 941, (60, 38, 135, 308), (270, 82, 305, 175), (510, 192, 543, 293), (1572, 28, 1643, 313),
        (1375, 85, 1411, 175), (1319, 287, 1353, 388))),
    "encosta.png": (NA_PAREDE, "parede", px(
        1448, 1086, (1010, 225, 1048, 338), (1377, 192, 1423, 341), (825, 422, 865, 528), (1257, 502, 1328, 823),
        (649, 612, 703, 843))),
}

# A arquibancada tem o mesmo nome na Central e na Costeira: as duas vão por cenário.
POR_CENARIO = {
    ("arena_central", "arquibancada.png"): (NA_PAREDE, "parede", [
        (x0 * 1.024 / 2048, y0 * 1.024 / 425, x1 * 1.024 / 2048, y1 * 1.024 / 425) for x0, y0, x1, y1 in (
            (70, 103, 138, 334), (600, 186, 666, 336), (1335, 186, 1402, 336), (1863, 103, 1932, 334),
            (390, 56, 430, 124), (460, 60, 500, 129), (655, 76, 694, 129), (1305, 76, 1349, 129),
            (1503, 60, 1543, 129), (1573, 56, 1614, 124), (256, 36, 300, 104), (1700, 36, 1745, 104),
            (758, 80, 805, 170), (1195, 80, 1245, 170), (543, 76, 590, 164), (1413, 76, 1460, 164))]),
    ("arena_costeira", "arquibancada.png"): (NA_PAREDE, "parede", [
        (x0 * 1.024 / 2048, y0 * 1.024 / 509, x1 * 1.024 / 2048, y1 * 1.024 / 509) for x0, y0, x1, y1 in (
            (35, 23, 105, 222), (678, 53, 744, 200), (1253, 53, 1320, 200), (1890, 23, 1962, 222),
            (353, 153, 402, 247), (978, 153, 1024, 247), (1598, 153, 1650, 247),
            (33, 278, 87, 442), (648, 283, 707, 444), (1295, 283, 1354, 444), (1915, 278, 1970, 442),
            (198, 278, 229, 347), (485, 278, 516, 347), (841, 278, 872, 347), (1128, 278, 1160, 347),
            (1488, 278, 1520, 347), (1773, 278, 1805, 347))]),
}


def config(pasta, nome):
    return POR_CENARIO.get((pasta.name, nome)) or BANDEIRAS.get(nome)


def pano(img, caixa):
    """Máscara do pano dentro do retângulo (ou None se não tem bandeira ali)."""
    h, w = img.shape[:2]
    x0, y0 = int(caixa[0] * w), int(caixa[1] * h)
    x1, y1 = int(np.ceil(caixa[2] * w)), int(np.ceil(caixa[3] * h))
    r, g, b, a = (img[y0:y1, x0:x1, k].astype(np.float32) for k in range(4))
    vermelho = (r > 60) & (r > g * 1.45) & (r > b * 1.3) & (a > 200)
    quente = (r > 70) & (r > b * 1.5) & (a > 200)
    m = np.zeros((h, w), bool)
    linhas = []
    for y in range(y1 - y0):
        xs = np.nonzero(vermelho[y])[0]
        if len(xs) < 2:
            linhas.append(False)
            continue
        a0, a1 = xs[0], xs[-1]
        for _ in range(6):
            if a0 > 0 and quente[y, a0 - 1]:
                a0 -= 1
            if a1 < x1 - x0 - 1 and quente[y, a1 + 1]:
                a1 += 1
        m[y0 + y, x0 + a0:x0 + a1 + 1] = True
        linhas.append(True)
    # A maior sequência de linhas seguidas é o pano.
    melhor, ini = (0, 0), None
    for y, tem in enumerate(linhas + [False]):
        if tem and ini is None:
            ini = y
        elif not tem and ini is not None:
            if y - ini > melhor[1] - melhor[0]:
                melhor = (ini, y)
            ini = None
    if melhor[1] - melhor[0] < 12:
        return None
    m[:y0 + melhor[0]] = False
    m[y0 + melhor[1]:] = False
    return m


def tapar(img, m, fundo):
    """Tapa o lugar do pano. "vao": a bandeira pendia no vão, fica transparente. "parede":
    em cada linha, a mediana da parede dos dois lados do pano (sem pegar a borda dele nem
    uma lanterna acesa), misturada de um lado ao outro, suavizada na vertical e escurecida
    (a sombra da bandeira)."""
    out = img.copy()
    linhas = np.nonzero(m.any(axis=1))[0]
    if fundo == "vao":
        out[m] = 0
        return out
    h, w = img.shape[:2]
    lados = []
    for y in linhas:
        xs = np.nonzero(m[y])[0]
        a0, a1 = xs[0], xs[-1]
        cores = []
        for faixa in (img[y, max(a0 - 16, 0):max(a0 - 3, 0)], img[y, min(a1 + 4, w):min(a1 + 17, w)]):
            faixa = faixa[faixa[:, 3] > 200].astype(np.float32)
            if len(faixa):
                luz = faixa[:, :3].mean(axis=1)
                faixa = faixa[luz <= np.percentile(luz, 60)]
            cores.append(np.median(faixa, axis=0) if len(faixa) else None)
        esq, dir_ = cores
        if esq is None and dir_ is None:
            esq = dir_ = np.zeros(4, np.float32)
        lados.append((esq if esq is not None else dir_, dir_ if dir_ is not None else esq))
    esq = np.array([e for e, _ in lados])
    dir_ = np.array([d for _, d in lados])
    raio = max(4, int(len(linhas) * 0.08))
    nucleo = np.ones(raio * 2 + 1) / (raio * 2 + 1)
    suave = lambda serie: np.stack([np.convolve(np.pad(serie[:, k], raio, mode="edge"), nucleo, "valid")
                                    for k in range(4)], axis=1)
    esq, dir_ = suave(esq), suave(dir_)
    for i, y in enumerate(linhas):
        xs = np.nonzero(m[y])[0]
        a0, a1 = xs[0], xs[-1]
        t = np.linspace(0, 1, a1 - a0 + 1)[:, None]
        cor = esq[i] * (1 - t) + dir_[i] * t
        cor[:, :3] *= 0.8
        out[y, a0:a1 + 1] = cor.round().clip(0, 255).astype(np.uint8)
    return out


def soltar_imagem(pasta, nome):
    balanco, fundo, caixas = config(pasta, nome)
    arquivo = pasta / nome
    img = np.array(Image.open(arquivo).convert("RGBA"))
    h, w = img.shape[:2]
    stem = Path(nome).stem
    lista = []
    for caixa in caixas:
        m = pano(img, caixa)
        if m is None:
            raise SystemExit(f"{pasta.name}/{nome}: sem bandeira em {caixa} (já foi solta?)")
        ys = np.nonzero(m.any(axis=1))[0]
        corte = ys[0] + max(3, round(PRESO * (ys[-1] - ys[0])))
        solta = m.copy()
        solta[:corte] = False
        leva = m.copy()
        leva[:corte - SOBRA] = False
        yy, xx = np.nonzero(leva)
        bx0, bx1, by0, by1 = xx.min(), xx.max() + 1, yy.min(), yy.max() + 1
        bandeira = np.zeros((by1 - by0, bx1 - bx0, 4), np.uint8)
        recorte = leva[by0:by1, bx0:bx1]
        bandeira[recorte] = img[by0:by1, bx0:bx1][recorte]
        saida = f"{stem}_bandeira{len(lista) + 1}.png"
        Image.fromarray(bandeira).save(pasta / saida, optimize=True)
        img = tapar(img, solta, fundo)
        lista.append({
            "imagem": saida,
            "x": round(float(bx0 / w), 4), "y": round(float(by0 / h), 4),
            "largura": round(float((bx1 - bx0) / w), 4), "altura": round(float((by1 - by0) / h), 4),
            "balanco": balanco,
        })
    Image.fromarray(img).save(arquivo, optimize=True)
    return lista


def anotar(pasta, nome, lista):
    """Põe (ou troca) "bandeiras" numa linha só, nas camadas da imagem."""
    json_path = pasta / "stage.json"
    texto = json_path.read_text(encoding="utf-8")
    campo = '"bandeiras": [' + ", ".join(json.dumps(b, ensure_ascii=False) for b in lista) + "],"
    padrao = re.compile(rf'\n(\s*)"imagem": "{re.escape(nome)}",((?:\n\s*"cachoeiras": [^\n]*,)?)'
                        r'(\n\s*"bandeiras": \[[^\n]*\],)?')
    texto, n = padrao.subn(lambda m: f'\n{m.group(1)}"imagem": "{nome}",{m.group(2)}\n{m.group(1)}{campo}', texto)
    json.loads(texto)
    json_path.write_text(texto, encoding="utf-8")
    return n


def soltar(pasta, anotar_json=False):
    for nome in sorted(set(BANDEIRAS) | {n for c, n in POR_CENARIO if c == pasta.name}):
        if not (pasta / nome).exists() or config(pasta, nome) is None:
            continue
        lista = soltar_imagem(pasta, nome)
        camadas = anotar(pasta, nome, lista) if anotar_json else 0
        print(f"{pasta.name}/{nome}: {len(lista)} bandeiras" + (f" ({camadas} camadas)" if anotar_json else ""))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("pastas", nargs="+", type=Path, help="pastas de cenário (com o stage.json)")
    args = parser.parse_args()
    for pasta in args.pastas:
        soltar(pasta, anotar_json=True)


if __name__ == "__main__":
    main()
