#!/usr/bin/env python3
"""Recorta o rosto de cada concept (art/concepts/pNN.png) em um retrato pequeno para a chave
do torneio do modo história: android/app/src/main/assets/historia/torneio/rostos/pNN.jpg.

Os recortes de p01 a p12 são os mesmos dos retratos do HUD (HudConceptPortraits); p13 a p16
foram marcados à mão no model sheet. Rode de novo quando um concept mudar:

    python3 tools/historia/rostos_torneio.py
"""
from pathlib import Path
from PIL import Image

RAIZ = Path(__file__).resolve().parents[2]
SAIDA = RAIZ / "android/app/src/main/assets/historia/torneio/rostos"
TAMANHO = 160

# {esquerda, topo, direita, base} em pixels do concept (1448x1086).
ROSTOS = {
    "p01": (1170, 30, 1430, 340), "p02": (1147, 10, 1438, 274), "p03": (1149, 14, 1435, 342),
    "p04": (1244, 24, 1525, 330), "p05": (8, 755, 289, 1065), "p06": (868, 638, 1184, 1015),
    "p07": (18, 685, 357, 1005), "p08": (12, 645, 307, 1070), "p09": (18, 758, 372, 1014),
    "p10": (13, 770, 368, 1058), "p11": (870, 419, 1145, 747), "p12": (1169, 15, 1435, 332),
    "p13": (20, 690, 390, 1070), "p14": (32, 690, 392, 1040), "p15": (910, 514, 1170, 834),
    "p16": (1162, 44, 1448, 334),
}


def quadrado(caixa, largura, altura):
    """O maior quadrado dentro da caixa, com o mesmo centro e sem sair da imagem."""
    l, t, r, b = caixa
    lado = min(r - l, b - t, largura, altura)
    cx, cy = (l + r) / 2, (t + b) / 2
    l = min(max(0, cx - lado / 2), largura - lado)
    t = min(max(0, cy - lado / 2), altura - lado)
    return int(l), int(t), int(l + lado), int(t + lado)


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    for codigo, caixa in ROSTOS.items():
        concept = Image.open(RAIZ / "art/concepts" / f"{codigo}.png").convert("RGB")
        rosto = concept.crop(quadrado(caixa, *concept.size)).resize((TAMANHO, TAMANHO), Image.LANCZOS)
        rosto.save(SAIDA / f"{codigo}.jpg", quality=88)
        print(SAIDA / f"{codigo}.jpg")


if __name__ == "__main__":
    main()
