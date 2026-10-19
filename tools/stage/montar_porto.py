#!/usr/bin/env python3
"""Monta o cenário do Porto Secreto (Ilha do Torneio) a partir das imagens geradas no GPT.

    python tools/stage/montar_porto.py --comum pasta/torneio --entrada pasta/torneio/porto_secreto

`--comum` é a pasta das peças iguais em toda a ilha (ceu, mar, penhascos; veja
tools/stage/ilha-do-torneio.md) e `--entrada` a do porto. Além do que o
preparar_cenario.py faz (cortar margens transparentes, alfa opaco, JPG, 2048 px):

- Piso: o chao_cais.png vira a peça que se repete, com as bordas esquerda e direita
  fundidas (a emenda da repetição some). O chao.jpg é o mesmo cais com o emblema do
  chao.png colado no centro, com borda suave: o GPT refez o concreto na edição, então só o
  emblema vem do chao.png (as faixas amarelas ficaram nas mesmas linhas nas duas).
- Grade: o GPT desenhou a lateral do cais (concreto e pneus) embaixo do guarda-corpo. Ela
  fica de costas para a câmera e é cortada; sobra o guarda-corpo em cima da borda do cais.
- Primeiro plano: frente_esq.png (caixas) e frente_dir.png (cabeço e luminária) são
  desfocados, como no Beco do Brucy.
- Ilha: as bordas dos lados e a faixa de névoa de baixo são esfumadas (a imagem não se
  repete e o corte reto aparecia contra o mar).
- Poste: o estandarte vira bandeira_poste.png, para balançar ao vento (separar_bandeiras.py).

Grava em android/app/src/main/assets/stages/porto_secreto/ sem mexer no stage.json.
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
from montar_brucy import desfocar, sem_emenda  # noqa: E402
import separar_bandeiras  # noqa: E402

ID = "porto_secreto"
COMUNS = ("ceu", "mar", "penhascos")
# Emblema no chao.png: centro e raio (até as pontas da estrela) em fração da largura.
EMBLEMA_X, EMBLEMA_Y, EMBLEMA_R = 843 / 1672, 441 / 941, 272 / 1672
# Fração da altura da grade.png onde começa a lateral do cais (cortada).
CORTE_GRADE = 410 / 724


def montar_piso(chao, cais):
    w, h = cais.size
    base = sem_emenda(cais)
    bw, bh = base.size
    raio = EMBLEMA_R * w
    cx, cy = EMBLEMA_X * w, EMBLEMA_Y * h
    lado = int(raio * 2.5)
    pedaco = chao.convert("RGB").resize(cais.size).crop((int(cx - lado / 2), int(cy - lado / 2),
                                                       int(cx + lado / 2), int(cy + lado / 2)))
    mascara = Image.new("L", pedaco.size, 0)
    margem = (lado - raio * 2.1) / 2
    ImageDraw.Draw(mascara).ellipse((margem, margem, lado - margem, lado - margem), fill=255)
    mascara = mascara.filter(ImageFilter.GaussianBlur(raio * 0.08))
    com = base.copy()
    com.paste(pedaco, (int(bw / 2 - lado / 2), int(cy - lado / 2)), mascara)
    print(f"piso: {w}x{h} -> {bw}x{bh}, emblema no centro ({raio:.0f} px de raio)")
    return com, base


def esfumar_ilha(img, lados=0.12, baixo=0.14):
    """Apaga aos poucos as bordas esquerda e direita e a faixa de névoa de baixo."""
    px = np.asarray(img.convert("RGBA")).astype(np.float32)
    h, w = px.shape[:2]
    n = max(1, int(w * lados))
    rampa = np.linspace(0.0, 1.0, n) ** 1.5
    px[:, :n, 3] *= rampa
    px[:, w - n:, 3] *= rampa[::-1]
    m = max(1, int(h * baixo))
    px[h - m:, :, 3] *= np.linspace(1.0, 0.0, m)[:, None] ** 1.2
    return Image.fromarray(px.clip(0, 255).astype(np.uint8))


def imagens(pasta):
    return {p.stem: p for p in pasta.iterdir() if p.suffix.lower() in EXTENSOES}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--entrada", required=True, type=Path, help="pasta do porto_secreto")
    parser.add_argument("--comum", required=True, type=Path, help="pasta das peças comuns da ilha")
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO / ID)
    args = parser.parse_args()

    entrada = imagens(args.entrada)
    comum = imagens(args.comum)
    # No Drive o mar veio como "maar.png".
    if "mar" not in comum and "maar" in comum:
        comum["mar"] = comum.pop("maar")
    for falta in ("chao", "chao_cais", "frente_esq", "frente_dir", "grade", "poste"):
        if falta not in entrada:
            sys.exit(f"faltou {falta}.png em {args.entrada}")
    for falta in COMUNS:
        if falta not in comum:
            sys.exit(f"faltou {falta}.png em {args.comum}")
    args.saida.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        com, sem = montar_piso(Image.open(entrada["chao"]), Image.open(entrada["chao_cais"]))
        com.save(tmp / "chao.png")
        sem.save(tmp / "chao_cais.png")
        for nome in COMUNS:
            img = Image.open(comum[nome])
            img.save(tmp / f"{nome}.png")
        for nome, arquivo in entrada.items():
            if nome in ("chao", "chao_cais"):
                continue
            if nome in ("frente_esq", "frente_dir"):
                desfocar(Image.open(arquivo), tmp, nome)
                continue
            img = Image.open(arquivo)
            if nome == "ilha":
                img = esfumar_ilha(img)
            if nome == "grade":
                img = img.convert("RGBA")
                img = img.crop((0, 0, img.width, round(img.height * CORTE_GRADE)))
            img.save(tmp / f"{nome}.png")
        for arquivo in sorted(tmp.iterdir()):
            saida, tamanho = preparar(arquivo, args.saida)
            print(f"{arquivo.name} -> {saida.name} {tamanho[0]}x{tamanho[1]}")
    separar_bandeiras.STAGES = args.saida.parent
    print("bandeira do poste", separar_bandeiras.separar(ID))


if __name__ == "__main__":
    main()
