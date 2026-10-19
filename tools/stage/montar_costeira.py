#!/usr/bin/env python3
"""Monta o cenário da Arena Costeira (Ilha do Torneio) a partir das imagens geradas no GPT.

    python tools/stage/montar_costeira.py --comum pasta/torneio --entrada pasta/torneio/arena_costeira

`--comum` é a pasta das peças iguais em toda a ilha (ceu, mar, penhascos; veja
tools/stage/ilha-do-torneio.md) e `--entrada` a da arena. Além do que o
preparar_cenario.py faz (cortar margens transparentes, alfa opaco, JPG, 2048 px):

- Piso: o GPT desenhou a arena inteira vista de cima, com a mureta curva e os estandartes
  nas bordas esquerda e direita. O jogo repete o piso para os lados, então a mureta sairia
  pintada no chão. Fica só a faixa de lajotas do meio do chao_liso.png (entre duas juntas,
  `FAIXA`), repetida três vezes com o brilho nivelado (o GPT iluminou mais o centro; sem
  isso, cada emenda ficava com um degrau de luz): é o chao_liso.jpg. O chao.jpg é o mesmo, com o círculo vermelho e o emblema do
  chao.png colados no centro (as duas imagens têm a mesma grade de lajotas).
- Encosta: a borda da direita e a névoa de baixo são esfumadas, como a ilha do porto.
- Primeiro plano: frente_esq.png (grua de câmera) e frente_dir.png (vaso e corrimão de
  vidro) são desfocados, como no Beco do Brucy.

Grava em android/app/src/main/assets/stages/arena_costeira/ sem mexer no stage.json.
"""

import argparse
import sys
import tempfile
from pathlib import Path

try:
    import numpy as np
    from PIL import Image, ImageDraw, ImageFilter
except ImportError:
    sys.exit("Instale o Pillow e o numpy: pip install pillow numpy")

sys.path.insert(0, str(Path(__file__).resolve().parent))
from preparar_cenario import EXTENSOES, SAIDA_PADRAO, preparar  # noqa: E402
from montar_brucy import desfocar  # noqa: E402
from montar_porto import COMUNS, esfumar_ilha, imagens  # noqa: E402

ID = "arena_costeira"
# Faixa de lajotas do chao_liso.png que se repete: da coluna depois de uma junta até a
# próxima junta com as mesmas fileiras (em fração da largura).
FAIXA = (480 / 1672, 1193 / 1672)
COPIAS = 3
# Círculo vermelho no chao.png: centro e raio (com uma folga de lajotas em volta).
CIRCULO_X, CIRCULO_Y, CIRCULO_R = 836 / 1672, 456 / 940, 480 / 1672


def correcao_de_luz(img, sigma=70):
    """Quanto multiplicar cada pixel para tirar a iluminação do GPT (mais clara no centro).

    Medida no chao_liso (sem o emblema, que entraria na conta) e aplicada nas duas imagens,
    que têm a mesma luz: divide pela luz bem desfocada e, depois, acerta coluna a coluna
    pela mediana móvel (faixas estreitas de reflexo perto da mureta escapam do desfoque; a
    mediana ignora as juntas finas)."""
    px = np.asarray(img.convert("RGB")).astype(np.float32)
    h, w = px.shape[:2]
    m = sigma * 3
    luz = np.pad(px.mean(axis=2).astype(np.uint8), m, mode="reflect")
    desfocada = np.asarray(Image.fromarray(luz).filter(ImageFilter.GaussianBlur(sigma))).astype(np.float32)
    fator = desfocada.mean() / np.maximum(desfocada[m:m + h, m:m + w], 1.0)
    coluna = np.median(px.mean(axis=2) * fator, axis=0)
    r = 20
    borda = np.pad(coluna, r, mode="reflect")
    movel = np.array([np.median(borda[i:i + 2 * r + 1]) for i in range(w)])
    return fator * (np.median(coluna) / np.maximum(movel, 1.0))[None, :]


def aplicar(img, fator):
    px = np.asarray(img.convert("RGB")).astype(np.float32) * fator[:, :, None]
    return Image.fromarray(px.clip(0, 255).astype(np.uint8))


def montar_piso(chao, liso):
    w, h = liso.size
    x0, x1 = round(FAIXA[0] * w), round(FAIXA[1] * w)
    fator = correcao_de_luz(liso)
    # Mantém o brilho das lajotas do centro (a média da imagem inteira tem a mureta escura).
    lum = np.asarray(liso.convert("L")).astype(np.float32)[:, x0:x1]
    fator *= np.median(lum) / np.median(lum * fator[:, x0:x1])
    faixa = np.asarray(aplicar(liso, fator).crop((x0, 0, x1, h)))
    base = Image.fromarray(np.concatenate([faixa] * COPIAS, axis=1))
    bw = base.width
    # O centro da cópia do meio cai no mesmo lugar da imagem original.
    cx_base = (x1 - x0) * (COPIAS // 2) + (CIRCULO_X * w - x0)
    raio = CIRCULO_R * w
    cx, cy = CIRCULO_X * w, CIRCULO_Y * h
    lado = int(raio * 2)
    plano = aplicar(chao.convert("RGB").resize(liso.size), fator)
    pedaco = plano.crop((int(cx - raio), 0, int(cx - raio) + lado, h))
    mascara = Image.new("L", pedaco.size, 0)
    ImageDraw.Draw(mascara).ellipse((raio * 0.06, cy - raio * 0.94, lado - raio * 0.06, cy + raio * 0.94), fill=255)
    mascara = mascara.filter(ImageFilter.GaussianBlur(raio * 0.05))
    com = base.copy()
    com.paste(pedaco, (int(cx_base - raio), 0), mascara)
    print(f"piso: faixa {x0}-{x1} x{COPIAS} -> {bw}x{h}, círculo no centro ({raio:.0f} px de raio)")
    return com, base


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--entrada", required=True, type=Path, help="pasta da arena_costeira")
    parser.add_argument("--comum", required=True, type=Path, help="pasta das peças comuns da ilha")
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO / ID)
    args = parser.parse_args()

    entrada = imagens(args.entrada)
    comum = imagens(args.comum)
    # Nomes como vieram do Drive.
    if "mar" not in comum and "maar" in comum:
        comum["mar"] = comum.pop("maar")
    if "frente_dir" not in entrada and "frent_dir" in entrada:
        entrada["frente_dir"] = entrada.pop("frent_dir")
    for falta in ("chao", "chao_liso", "frente_esq", "frente_dir", "arquibancada", "barreira", "holofote", "encosta"):
        if falta not in entrada:
            sys.exit(f"faltou {falta}.png em {args.entrada}")
    for falta in COMUNS:
        if falta not in comum:
            sys.exit(f"faltou {falta}.png em {args.comum}")
    args.saida.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        com, sem = montar_piso(Image.open(entrada["chao"]), Image.open(entrada["chao_liso"]))
        com.save(tmp / "chao.png")
        sem.save(tmp / "chao_liso.png")
        for nome in COMUNS:
            Image.open(comum[nome]).save(tmp / f"{nome}.png")
        for nome, arquivo in entrada.items():
            if nome in ("chao", "chao_liso"):
                continue
            if nome in ("frente_esq", "frente_dir"):
                desfocar(Image.open(arquivo), tmp, nome)
                continue
            img = Image.open(arquivo)
            if nome == "encosta":
                img = esfumar_ilha(img, lados=0.04, baixo=0.12)
            img.save(tmp / f"{nome}.png")
        for arquivo in sorted(tmp.iterdir()):
            saida, tamanho = preparar(arquivo, args.saida)
            print(f"{arquivo.name} -> {saida.name} {tamanho[0]}x{tamanho[1]}")


if __name__ == "__main__":
    main()
