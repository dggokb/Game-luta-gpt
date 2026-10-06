#!/usr/bin/env python3
"""Prepara as imagens de um cenário geradas pela IA.

    python tools/stage/preparar_cenario.py --entrada minhas_imagens/ --id templo_lua

Para cada imagem da pasta de entrada:
- PNG com transparência: corta as margens transparentes (senão uma camada "noChao"
  fica flutuando acima do piso) e mantém o PNG;
- imagens sem transparência (céu, piso): grava como JPG;
- reduz para no máximo 2048 px no lado maior.

Grava em android/app/src/main/assets/stages/<id>/ sem mexer no stage.json. Depois de
trocar a arte, confira posições de luzes e fogos no visualizador (veja o README).
"""

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.exit("Instale o Pillow: pip install pillow")

RAIZ = Path(__file__).resolve().parents[2]
SAIDA_PADRAO = RAIZ / "android" / "app" / "src" / "main" / "assets" / "stages"
EXTENSOES = (".png", ".jpg", ".jpeg", ".webp")
MAX_LADO = 2048


def tem_transparencia(img):
    if img.mode not in ("RGBA", "LA", "P"):
        return False
    alpha = img.convert("RGBA").getchannel("A")
    return alpha.getextrema()[0] < 250


def preparar(arquivo, destino):
    img = Image.open(arquivo)
    nome = arquivo.stem
    if tem_transparencia(img):
        img = img.convert("RGBA")
        caixa = img.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox()
        if caixa:
            img = img.crop(caixa)
        saida = destino / f"{nome}.png"
    else:
        img = img.convert("RGB")
        saida = destino / f"{nome}.jpg"
    if max(img.size) > MAX_LADO:
        escala = MAX_LADO / max(img.size)
        img = img.resize((round(img.width * escala), round(img.height * escala)), Image.LANCZOS)
    if saida.suffix == ".jpg":
        img.save(saida, quality=88)
    else:
        img.save(saida)
    return saida, img.size


def main():
    parser = argparse.ArgumentParser(description="Prepara as imagens de um cenário.")
    parser.add_argument("--entrada", type=Path, required=True, help="pasta com as imagens geradas")
    parser.add_argument("--id", required=True, help="pasta do cenário, ex.: templo_lua")
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO)
    args = parser.parse_args()

    if not args.entrada.is_dir():
        sys.exit(f"pasta de entrada não existe: {args.entrada}")
    destino = args.saida / args.id
    destino.mkdir(parents=True, exist_ok=True)
    arquivos = sorted(p for p in args.entrada.iterdir() if p.suffix.lower() in EXTENSOES)
    if not arquivos:
        sys.exit("nenhuma imagem na pasta de entrada")
    for arquivo in arquivos:
        saida, (w, h) = preparar(arquivo, destino)
        print(f"{arquivo.name} -> {saida.name} ({w}x{h})")
    print(f"pronto: {destino}")
    print("Confira se os nomes batem com o stage.json (por exemplo, cidade.png e ceu.jpg).")


if __name__ == "__main__":
    main()
