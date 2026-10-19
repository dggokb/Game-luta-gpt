#!/usr/bin/env python3
"""Monta o cenário do Caminho dos Penhascos (Ilha do Torneio) a partir das imagens do GPT.

    python tools/stage/montar_caminho.py --comum pasta/torneio --entrada pasta/torneio/caminho_penhascos

`--comum` é a pasta das peças iguais em toda a ilha (veja tools/stage/ilha-do-torneio.md;
aqui o céu é o ceu_lua, a emboscada é à noite) e `--entrada` a do caminho. Além do que o
preparar_cenario.py faz (cortar margens transparentes, alfa opaco, JPG, 2048 px):

- Piso: a grade da passarela é simétrica em volta da placa do emblema. Do centro de uma
  viga vertical (`FAIXA`) ao centro da viga do mesmo tipo do outro lado ela se repete: a
  emenda cai no meio de uma viga e não aparece. O chao.png e o chao_grade.png têm a mesma
  grade e são cortados no mesmo lugar (sem fundir nada).
- Ruínas e arena ao longe: as bordas cortadas são esfumadas (as duas não se repetem). A
  arena ao longe e os penhascos recebem a névoa da distância (montar_porto.afastar).
- Andaime: o estandarte vira bandeira_andaime.png, para balançar ao vento
  (separar_bandeiras.py).
- Sem primeiro plano: frente_esq.png e frente_dir.png são ignorados (nos cenários abertos
  da ilha eles apareciam soltos no meio do piso).

Grava em android/app/src/main/assets/stages/caminho_penhascos/ sem mexer no stage.json.
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
import separar_bandeiras  # noqa: E402

ID = "caminho_penhascos"
COMUNS = ("ceu_lua", "mar", "penhascos")
# Centro das duas vigas verticais com placa de rebites que fecham o padrão do piso.
FAIXA = (189 / 1672, 1484 / 1672)


def cortar_piso(img):
    w, h = img.size
    return img.convert("RGB").crop((round(FAIXA[0] * w), 0, round(FAIXA[1] * w), h))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--entrada", required=True, type=Path, help="pasta do caminho_penhascos")
    parser.add_argument("--comum", required=True, type=Path, help="pasta das peças comuns da ilha")
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO / ID)
    args = parser.parse_args()

    entrada = imagens(args.entrada)
    comum = imagens(args.comum)
    if "mar" not in comum and "maar" in comum:
        comum["mar"] = comum.pop("maar")
    for falta in ("chao", "chao_grade", "abismo", "ponte", "ruinas", "arena_longe", "andaime", "guarda_corpo"):
        if falta not in entrada:
            sys.exit(f"faltou {falta}.png em {args.entrada}")
    for falta in COMUNS:
        if falta not in comum:
            sys.exit(f"faltou {falta}.png em {args.comum}")
    args.saida.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        for nome in COMUNS:
            peca_comum(nome, Image.open(comum[nome])).save(tmp / f"{nome}.png")
        for nome, arquivo in entrada.items():
            img = Image.open(arquivo)
            if nome in ("frente_esq", "frente_dir"):
                print(f"{arquivo.name} ignorado: o caminho fica sem primeiro plano")
                continue
            if nome in ("chao", "chao_grade"):
                img = cortar_piso(img)
            elif nome == "ruinas":
                img = esfumar_ilha(img, lados=0.06, baixo=0.0)
            elif nome == "arena_longe":
                img = afastar(esfumar_ilha(img, lados=0.04, baixo=0.02), 0.4)
            img.save(tmp / f"{nome}.png")
        for arquivo in sorted(tmp.iterdir()):
            saida, tamanho = preparar(arquivo, args.saida)
            print(f"{arquivo.name} -> {saida.name} {tamanho[0]}x{tamanho[1]}")
    separar_bandeiras.STAGES = args.saida.parent
    print("bandeira do andaime", separar_bandeiras.separar(ID))


if __name__ == "__main__":
    main()
