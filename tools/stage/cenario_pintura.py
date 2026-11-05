#!/usr/bin/env python3
"""Cenário de uma pintura só: a imagem inteira numa camada, sem recorte e sem chão desentortado.

O chão é o da pintura ("chaoNaPintura"), na profundidade 1 (anda com os lutadores). A escala
faz a pintura cobrir o mundo inteiro (2600) na câmera; em cima, céu espelhado para o super pulo.
EMB_X/CHAO_Y: o centro do emblema e a linha do pé dos lutadores na pintura.

    python3 tools/stage/cenario_pintura.py art/stages/arena_pintura/pintura_fonte.webp \\
        android/app/src/main/assets/stages/arena_pintura [profundidade]
"""
import json, sys, os
from PIL import Image, ImageFilter
import numpy as np

fonte, saida = sys.argv[1], sys.argv[2]
prof = float(sys.argv[3]) if len(sys.argv) > 3 else 1.0
EMB_X, CHAO_Y = 985.0, 600.0     # centro do emblema e linha do pé dos lutadores na pintura
EXTRA_CEU = 260                  # céu espelhado acima (super pulo)
im = Image.open(fonte).convert('RGB')
W, H = im.size
# Céu a mais em cima: o topo espelhado, escurecendo para a cor de cima.
topo = np.asarray(im.crop((0, 0, W, EXTRA_CEU)).transpose(Image.FLIP_TOP_BOTTOM)).astype(np.float32)
cor = np.array([46, 22, 60], np.float32)
t = np.linspace(1, 0, EXTRA_CEU)[:, None, None] ** 1.3   # 1 lá em cima (cor), 0 na emenda
topo = topo * (1 - t) + cor * t
grande = Image.new('RGB', (W, H + EXTRA_CEU))
grande.paste(Image.fromarray(topo.astype(np.uint8)), (0, 0))
grande.paste(im, (0, EXTRA_CEU))
H2 = H + EXTRA_CEU
# 2x com Lanczos + nitidez leve (provisório: um upscaler bom faria melhor).
g2 = grande.resize((W * 2, H2 * 2), Image.LANCZOS).filter(ImageFilter.UnsharpMask(radius=1.6, percent=70, threshold=2))
os.makedirs(saida, exist_ok=True)
g2.save(os.path.join(saida, 'pintura.jpg'), quality=92)

# Escala: a pintura cobre o mundo todo (2600) na profundidade 0; mais funda, anda menos e pode ser menor.
D, ZOOM, VW, VH, GY = 1000.0, 1.24, 1280.0, 720.0, 552.0
f = (D / ZOOM) / (D / ZOOM + prof)              # quanto a camada anda em relação aos lutadores
visivel = VW / ZOOM                             # mundo visível na câmera de repouso
cobrir = visivel + 2 * (1300 - visivel / 2) * f  # largura (em mundo de repouso) que a pintura precisa
# pixels da pintura por unidade de mundo na tela de repouso
lado = min(EMB_X, W - EMB_X)
s = (VW / ZOOM) / visivel * 1.0
px_por_mundo = lado / (cobrir / 2)
tela_por_px = ZOOM / px_por_mundo               # px de tela por px da pintura (repouso)
largura_tela = W * tela_por_px
x_centro = 0.5 + ((W / 2) - EMB_X) * tela_por_px / VW
topo_f = (GY - (CHAO_Y + EXTRA_CEU) * tela_por_px) / VH
base_f = (GY + (H - CHAO_Y) * tela_por_px) / VH
print(f'prof={prof} f={f:.3f} tela_por_px={tela_por_px:.3f} largura={largura_tela/VW:.2f} telas topo={topo_f:.4f} base={base_f:.4f} x={x_centro:.4f}')
torre = lambda px: 1300 + (px - EMB_X) / px_por_mundo
stage = {
  "nome": "Arena Central (pintura)",
  "camera": {"distancia": 1000, "horizonte": 0.47},
  "corFundo": "#2E163C",
  "chaoNaPintura": True,
  "luzLutadores": {"cor": "#FFA090", "forca": 0.2, "brilho": 0.97, "pes": "#E8D0C8",
                   "contraluz": {"cor": "#FF9A50", "forca": 0.9, "direcao": [-1, -1]}},
  "sombraLutadores": {"alfa": 0.6, "cor": "#1C0A14", "suave": 0.55, "x": 0.05, "y": 0.05},
  "camadas": [
    {"id": "pintura", "imagem": "pintura.jpg", "profundidade": prof,
     "topo": round(topo_f, 4), "base": round(base_f, 4), "x": round(x_centro, 4),
     "repetir": "espelhar", "corAcima": "#2E163C"}
  ]
}
json.dump(stage, open(os.path.join(saida, 'stage.json'), 'w'), ensure_ascii=False, indent=2)
