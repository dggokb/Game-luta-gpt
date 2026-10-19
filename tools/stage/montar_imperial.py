#!/usr/bin/env python3
"""Monta o cenário da Arena Imperial (Ilha do Torneio) a partir das imagens geradas no GPT.

    python tools/stage/montar_imperial.py --comum pasta/torneio --entrada pasta/torneio/arena_imperial

`--comum` é a pasta das peças iguais em toda a ilha e `--entrada` a da arena. Além do que
o preparar_cenario.py faz (cortar margens transparentes, alfa opaco, JPG, 2048 px):

- Piso: o chao.png e o chao_placas.png vieram levemente em perspectiva (as linhas
  verticais abrem 6,4% de cima para baixo, em volta do centro). As duas imagens são
  endireitadas (transformação bilinear, `ABRE`) e cortadas entre duas linhas com losango
  (`FAIXA`): a mandala cabe inteira e o padrão das placas fecha com a cópia do lado (os
  losangos cortados ao meio se completam na emenda).
- Sem primeiro plano: frente_esq.png e frente_dir.png, se houver, são ignorados.

Grava em android/app/src/main/assets/stages/arena_imperial/ sem mexer no stage.json.
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
from montar_porto import COMUNS, imagens, peca_comum, textura_mar, plateia, tirar_plateia  # noqa: E402

# Faixas de plateia (topo, base em fração da altura), as mesmas do stage.json.
PLATEIA = [(0.58, 0.76)]
ID = "arena_imperial"
# Centro da perspectiva (fração da largura) e quanto as linhas abrem do topo à base.
CENTRO, ABRE = 835 / 1672, 0.0835
# Linhas com losango que fecham o padrão (na imagem já endireitada).
FAIXA = (240 / 1672, 1431 / 1672)


def endireitar(img):
    w, h = img.size
    c = CENTRO * w
    topo, base = 1 / (1 + ABRE / 2), (1 + ABRE) / (1 + ABRE / 2)
    quad = (c - c * topo, 0, c - c * base, h, c + (w - c) * base, h, c + (w - c) * topo, 0)
    return img.convert("RGB").transform((w, h), Image.QUAD, quad, Image.BICUBIC)


def montar_piso(chao, placas):
    w, h = placas.size
    caixa = (round(FAIXA[0] * w), 0, round(FAIXA[1] * w), h)
    sem = endireitar(placas).crop(caixa)
    com = endireitar(chao.resize(placas.size)).crop(caixa)
    print(f"piso: endireitado, faixa {caixa[0]}-{caixa[2]} -> {sem.width}x{h}")
    return com, sem


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--entrada", required=True, type=Path, help="pasta da arena_imperial")
    parser.add_argument("--comum", required=True, type=Path, help="pasta das peças comuns da ilha")
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO / ID)
    parser.add_argument("--sem-plateia", action="store_true", help="não recorta a plateia (para medir as faixas)")
    args = parser.parse_args()

    entrada = imagens(args.entrada)
    comum = imagens(args.comum)
    if "mar" not in comum and "maar" in comum:
        comum["mar"] = comum.pop("maar")
    for falta in ("chao", "chao_placas", "palacio", "arquibancada_lateral", "portico", "arvore_vermelha", "lanterna"):
        if falta not in entrada:
            sys.exit(f"faltou {falta}.png em {args.entrada}")
    for falta in COMUNS:
        if falta not in comum:
            sys.exit(f"faltou {falta}.png em {args.comum}")
    args.saida.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        com, sem = montar_piso(Image.open(entrada["chao"]), Image.open(entrada["chao_placas"]))
        com.save(tmp / "chao.png")
        sem.save(tmp / "chao_placas.png")
        for nome in COMUNS:
            peca_comum(nome, Image.open(comum[nome])).save(tmp / f"{nome}.png")
        textura_mar(Image.open(comum["mar"])).save(tmp / "mar_plano.png")
        for nome, arquivo in entrada.items():
            if nome in ("chao", "chao_placas"):
                continue
            if nome in ("frente_esq", "frente_dir"):
                print(f"{arquivo.name} ignorado: a arena fica sem primeiro plano")
                continue
            Image.open(arquivo).save(tmp / f"{nome}.png")
        for arquivo in sorted(tmp.iterdir()):
            saida, tamanho = preparar(arquivo, args.saida)
            print(f"{arquivo.name} -> {saida.name} {tamanho[0]}x{tamanho[1]}")
    # Plateia recortada: a gente vai para uma imagem só dela (do mesmo tamanho, sem cortar
    # margens) e a arquibancada fica com os assentos escuros no lugar.
    if not args.sem_plateia:
        base = Image.open(args.saida / "arquibancada_lateral.png")
        gente = plateia(base, PLATEIA)
        gente.save(args.saida / "arquibancada_lateral_plateia.png")
        tirar_plateia(base, gente).save(args.saida / "arquibancada_lateral.png")


if __name__ == "__main__":
    main()
