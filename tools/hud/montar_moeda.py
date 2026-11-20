#!/usr/bin/env python3
"""O símbolo da moeda do SOKON, o KON (o K com a chama, escolhido pelo dono em 20/11 entre as
propostas do Codex em art/hud/moeda/propostas_original.png): art/hud/moeda/kon.png (branco,
recortado da proposta 1) vira assets/ui/hud/moeda/kon_<cor>.png, uma por cor que os números
usam, com o contorno escuro em volta (como o texto do HUD). A fonte não tem o símbolo: o jogo
desenha a imagem antes do número (BolsaApostas.desenharValor).

    python3 tools/hud/montar_moeda.py
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

RAIZ = Path(__file__).resolve().parents[2]
ORIGEM = RAIZ / "art/hud/moeda/kon.png"
SAIDA = RAIZ / "android/app/src/main/assets/ui/hud/moeda"
ALTURA = 96
CONTORNO = 7
TINTA = (11, 14, 22)
# As cores do HUD (BolsaApostas: AZUL_LUZ, VERMELHO_LUZ, FRIO, CAI_COR, OURO e o branco).
CORES = {"branco": (255, 255, 255), "azul": (111, 195, 255), "vermelho": (255, 106, 106),
         "frio": (143, 227, 255), "cai": (255, 90, 90), "ouro": (255, 213, 74)}


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    kon = Image.open(ORIGEM).convert("RGBA")
    escala = (ALTURA - 2 * CONTORNO) / kon.height
    kon = kon.resize((round(kon.width * escala), ALTURA - 2 * CONTORNO), Image.LANCZOS)
    alfa = Image.new("L", (kon.width + 2 * CONTORNO, ALTURA), 0)
    alfa.paste(kon.getchannel("A"), (CONTORNO, CONTORNO))
    borda = alfa.filter(ImageFilter.MaxFilter(2 * (CONTORNO // 2) + 1)).filter(ImageFilter.MaxFilter(5))
    borda = borda.filter(ImageFilter.GaussianBlur(0.8))
    for nome, cor in CORES.items():
        out = Image.new("RGBA", alfa.size, TINTA + (0,))
        out.putalpha(borda)
        corpo = Image.new("RGBA", alfa.size, cor + (0,))
        corpo.putalpha(alfa)
        out.alpha_composite(corpo)
        out.save(SAIDA / f"kon_{nome}.png", optimize=True)
    print("kon_*.png", alfa.size)


if __name__ == "__main__":
    main()
