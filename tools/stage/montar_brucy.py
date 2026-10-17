#!/usr/bin/env python3
"""Monta o cenário do Beco do Brucy a partir das imagens geradas no GPT.

    python tools/stage/montar_brucy.py --entrada pasta/com/as/imagens

Além do que o preparar_cenario.py faz (cortar margens transparentes, alfa opaco, JPG, 2048 px):

- Piso: o chao_asfalto.png vira a peça que se repete. As bordas esquerda e direita são
  fundidas (a imagem perde uma faixa da direita) para a emenda da repetição sumir. O
  chao.jpg é o mesmo asfalto com a tampa de bueiro do chao.png colada no centro, com
  borda suave: assim as duas imagens têm as mesmas bordas e a cópia do centro encaixa nas
  outras sem emenda (o GPT refez o asfalto todo na edição, então as bordas não batiam).
- Prédios, cerca e lixo: o GPT desenhou uma faixa de chão molhado com reflexos embaixo.
  Ela é cortada (o jogo faz o piso e o reflexo); senão a camada fica com um degrau de chão
  pintado embaixo. No predio_dir.png, o lixo encostado nas bordas é esfumado.
- Primeiro plano: frente_esq.png (sacos de lixo e caixa) e frente_dir.png (cavalete e
  barril) são desfocados, como no Shūshinkan. O frente.png antigo (um beco inteiro em
  perspectiva) é ignorado.

Grava em android/app/src/main/assets/stages/beco_brucy/ sem mexer no stage.json.
"""

import argparse
import shutil
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
from montar_dojo import separar_frente  # noqa: E402

ID = "beco_brucy"
# Fração da altura da imagem onde termina o objeto e começa o chão molhado pintado pelo GPT.
CORTE_BASE = {"predio_esq": 1446 / 1536, "predio_dir": 1336 / 1536, "lixo": 900 / 1086,
              "cerca": 634 / 724}
# Tampa de bueiro no chao.png: centro e raio em fração da largura.
BUEIRO_X, BUEIRO_Y, BUEIRO_R = 836 / 1671, 455 / 941, 176 / 1671
FUSAO = 0.12


def sem_emenda(img, fracao=FUSAO):
    """Funde a borda esquerda com a faixa da direita e corta a faixa: repete sem emenda."""
    px = np.asarray(img.convert("RGB")).astype(np.float32)
    w = px.shape[1]
    b = int(w * fracao)
    t = np.linspace(0.0, 1.0, b)[None, :, None]
    saida = px[:, :w - b].copy()
    saida[:, :b] = px[:, :b] * t + px[:, w - b:] * (1 - t)
    return Image.fromarray(saida.clip(0, 255).astype(np.uint8))


def montar_piso(chao, asfalto):
    w, h = asfalto.size
    base = sem_emenda(asfalto)
    bw, bh = base.size
    raio = BUEIRO_R * w
    cx, cy = BUEIRO_X * w, BUEIRO_Y * h
    lado = int(raio * 2.8)
    pedaco = chao.convert("RGB").crop((int(cx - lado / 2), int(cy - lado / 2),
                                       int(cx + lado / 2), int(cy + lado / 2)))
    mascara = Image.new("L", pedaco.size, 0)
    margem = (lado - raio * 2.3) / 2
    ImageDraw.Draw(mascara).ellipse((margem, margem, lado - margem, lado - margem), fill=255)
    mascara = mascara.filter(ImageFilter.GaussianBlur(raio * 0.12))
    com = base.copy()
    com.paste(pedaco, (int(bw / 2 - lado / 2), int(cy - lado / 2)), mascara)
    print(f"piso: {w}x{h} -> {bw}x{bh}, bueiro no centro ({raio:.0f} px de raio)")
    return com, base


def cortar_base(img, fracao):
    img = img.convert("RGBA")
    return img.crop((0, 0, img.width, round(img.height * fracao)))


def esfumar_bordas(img, fracao=0.05):
    px = np.asarray(img.convert("RGBA")).astype(np.float32)
    w = px.shape[1]
    n = max(1, int(w * fracao))
    rampa = np.linspace(0.0, 1.0, n) ** 1.5
    px[:, :n, 3] *= rampa
    px[:, w - n:, 3] *= rampa[::-1]
    return Image.fromarray(px.clip(0, 255).astype(np.uint8))


def desfocar(img, pasta, nome):
    """Desfoca um objeto de primeiro plano (o separar_frente desfoca as duas metades)."""
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        larga = Image.new("RGBA", (img.width * 2, img.height), (0, 0, 0, 0))
        larga.paste(img.convert("RGBA"), (0, 0))
        separar_frente(larga, tmp)
        shutil.copy(tmp / "frente_esq.png", pasta / f"{nome}.png")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--entrada", required=True, type=Path)
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO / ID)
    args = parser.parse_args()

    entrada = {p.stem: p for p in args.entrada.iterdir() if p.suffix.lower() in EXTENSOES}
    for falta in ("chao", "chao_asfalto", "frente_esq", "frente_dir"):
        if falta not in entrada:
            sys.exit(f"faltou {falta}.png em {args.entrada}")
    args.saida.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        com, sem = montar_piso(Image.open(entrada["chao"]), Image.open(entrada["chao_asfalto"]))
        com.save(tmp / "chao.png")
        sem.save(tmp / "chao_asfalto.png")
        for nome, arquivo in entrada.items():
            if nome in ("chao", "chao_asfalto", "frente"):
                continue
            if nome in ("frente_esq", "frente_dir"):
                desfocar(Image.open(arquivo), tmp, nome)
                continue
            img = Image.open(arquivo)
            if nome in CORTE_BASE:
                img = cortar_base(img, CORTE_BASE[nome])
            if nome == "predio_dir":
                img = esfumar_bordas(img)
            if img.mode in ("RGBA", "LA", "P"):
                img.save(tmp / f"{nome}.png")
            else:
                img.convert("RGB").save(tmp / f"{nome}.png")
        for arquivo in sorted(tmp.iterdir()):
            saida, tamanho = preparar(arquivo, args.saida)
            print(f"{arquivo.name} -> {saida.name} {tamanho[0]}x{tamanho[1]}")
    if "frente" in entrada:
        print("frente.png ignorado: o primeiro plano agora é frente_esq.png e frente_dir.png")


if __name__ == "__main__":
    main()
