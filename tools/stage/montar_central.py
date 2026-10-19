#!/usr/bin/env python3
"""Monta o cenário da Arena Central (Ilha do Torneio) a partir das imagens geradas no GPT.

    python tools/stage/montar_central.py --comum pasta/torneio --entrada pasta/torneio/arena_central

`--comum` é a pasta das peças iguais em toda a ilha (aqui o céu é o ceu_lua, como no
concept) e `--entrada` a da arena. Além do que o preparar_cenario.py faz (cortar margens
transparentes, alfa opaco, JPG, 2048 px):

- Piso: o chao.png e o chao_liso.png têm a mesma grade de lajotas, mas também um círculo
  grande que sai pelas bordas e linhas vermelhas em cruz (o "liso" não veio liso). Entre
  as juntas `FAIXA` só há lajotas e o círculo do emblema inteiro: as duas imagens são
  cortadas ali, e o padrão de lajotas fecha com a cópia do lado. A ponta da linha vermelha
  da cruz, que entra pelas bordas, é coberta com as lajotas ao lado, e as poucas colunas da
  borda do chao vêm do chao_liso já limpo.
- Arquibancada: o GPT pintou um pedaço do piso do ringue embaixo dela; é cortado.
- Templo ao longe e penhascos: névoa da distância (montar_porto.afastar).
- Sem primeiro plano: o camarote.png é ignorado (objetos colados na câmera ficavam soltos
  no meio do piso nos outros cenários da ilha).

Grava em android/app/src/main/assets/stages/arena_central/ sem mexer no stage.json.
"""

import argparse
import sys
import tempfile
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    sys.exit("Instale o Pillow e o numpy: pip install pillow numpy")

sys.path.insert(0, str(Path(__file__).resolve().parent))
from preparar_cenario import SAIDA_PADRAO, preparar  # noqa: E402
from montar_porto import afastar, esfumar_ilha, imagens, peca_comum  # noqa: E402

ID = "arena_central"
COMUNS = ("ceu_lua", "mar", "penhascos")
# Juntas das lajotas que fecham o padrão, em volta do círculo do emblema.
FAIXA = (341 / 1672, 1331 / 1672)
BORDA = 14
# Faixa da altura onde a linha vermelha da cruz chega às bordas.
LINHA = (405 / 941, 495 / 941)
# Fração da altura da arquibancada.png onde começa o piso pintado.
CORTE_ARQUIBANCADA = 655 / 724


def montar_piso(chao, liso):
    w, h = liso.size
    caixa = (round(FAIXA[0] * w), 0, round(FAIXA[1] * w), h)
    sem = liso.convert("RGB").crop(caixa)
    # A ponta da linha vermelha da cruz entra na faixa pelas bordas, no meio da altura:
    # cobre com as lajotas logo ao lado (espelhadas).
    y0, y1 = round(LINHA[0] * h), round(LINHA[1] * h)
    esq = sem.crop((BORDA, y0, 2 * BORDA, y1)).transpose(Image.FLIP_LEFT_RIGHT)
    dir = sem.crop((sem.width - 2 * BORDA, y0, sem.width - BORDA, y1)).transpose(Image.FLIP_LEFT_RIGHT)
    sem.paste(esq, (0, y0))
    sem.paste(dir, (sem.width - BORDA, y0))
    com = chao.convert("RGB").resize(liso.size).crop(caixa)
    for x0 in (0, sem.width - BORDA):
        com.paste(sem.crop((x0, 0, x0 + BORDA, h)), (x0, 0))
    print(f"piso: faixa {caixa[0]}-{caixa[2]} -> {sem.width}x{h}")
    return com, sem


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--entrada", required=True, type=Path, help="pasta da arena_central")
    parser.add_argument("--comum", required=True, type=Path, help="pasta das peças comuns da ilha")
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO / ID)
    args = parser.parse_args()

    entrada = imagens(args.entrada)
    comum = imagens(args.comum)
    if "mar" not in comum and "maar" in comum:
        comum["mar"] = comum.pop("maar")
    for falta in ("chao", "chao_liso", "arquibancada", "trelica", "pilastra", "barreira", "templo_longe"):
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
            peca_comum(nome, Image.open(comum[nome])).save(tmp / f"{nome}.png")
        for nome, arquivo in entrada.items():
            if nome in ("chao", "chao_liso"):
                continue
            if nome in ("camarote", "frente_esq", "frente_dir"):
                print(f"{arquivo.name} ignorado: a arena fica sem primeiro plano")
                continue
            img = Image.open(arquivo)
            if nome == "arquibancada":
                img = img.convert("RGBA")
                img = img.crop((0, 0, img.width, round(img.height * CORTE_ARQUIBANCADA)))
            elif nome == "templo_longe":
                img = afastar(esfumar_ilha(img, lados=0.03, baixo=0.02), 0.4)
            img.save(tmp / f"{nome}.png")
        for arquivo in sorted(tmp.iterdir()):
            saida, tamanho = preparar(arquivo, args.saida)
            print(f"{arquivo.name} -> {saida.name} {tamanho[0]}x{tamanho[1]}")


if __name__ == "__main__":
    main()
