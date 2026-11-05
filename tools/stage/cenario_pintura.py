#!/usr/bin/env python3
"""Cenário a partir de uma pintura só, sem desentortar e sem recorte automático.

A pintura é cortada numa linha reta só, onde o chão encontra a arquibancada (``--juncao``):

* **parede** (céu + arquibancada): uma camada ``imagem`` na profundidade da junção, um plano em
  pé lá no fundo. Em cima ganha céu espelhado para o super pulo. Leve névoa e desfoque, para os
  lutadores saltarem na frente (como na pintura com os personagens);
* **chão**: o resto, em perspectiva, como ``chao`` com ``"projecao"``. O motor pega para cada
  ponto do chão o pixel que a câmera de repouso vê ali: parada, a tela é a pintura; andando,
  cada faixa do chão anda na velocidade da profundidade dela e encontra a parede na junção.

A câmera do cenário (horizonte) sai do anel do chão: ele é um círculo no chão em volta dos
lutadores (centro na profundidade 0); a meia largura dele dá o raio, a borda de longe dá o
horizonte. Com isso a profundidade da junção sai sozinha.

    python3 tools/stage/cenario_pintura.py art/stages/arena_pintura/pintura_fonte_x2.png \\
        android/app/src/main/assets/stages/arena_pintura

Medidas na pintura (pixels da imagem de entrada): ``--emblema X``, ``--chao Y`` (a linha do pé
dos lutadores, no centro do anel), ``--anel-longe Y`` (a borda de longe do anel),
``--anel-meia X`` (a meia largura do anel), ``--juncao Y``.

Quando a pintura nasceu de uma tela do jogo (um concept com o HUD, limpo e estendido para os
lados), o enquadramento já é o da câmera de repouso: ``--tela-px`` (quantos pixels da imagem
cabem na largura da tela), ``--centro X`` (o x da imagem no meio da tela) e ``--horizonte F``
(a altura do horizonte, fração da tela, medida pelo anel). Aí a escala não sai da largura.
"""
import argparse
import json
import os

import numpy as np
from PIL import Image, ImageFilter

D, ZOOM, VW, VH, GY = 1000.0, 1.24, 1280.0, 720.0, 552.0   # câmera de repouso do jogo
MUNDO = 2600.0                                              # largura da arena


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('fonte')
    ap.add_argument('saida')
    ap.add_argument('--emblema', type=float, default=1970)
    ap.add_argument('--chao', type=float, default=1200)
    ap.add_argument('--anel-longe', type=float, default=980)
    ap.add_argument('--anel-meia', type=float, default=1500)
    ap.add_argument('--juncao', type=float, default=872)
    ap.add_argument('--ceu-extra', type=int, default=520)
    ap.add_argument('--nevoa', default='#6E4A78')
    ap.add_argument('--tela-px', type=float, default=None)
    ap.add_argument('--centro', type=float, default=None)
    ap.add_argument('--horizonte', type=float, default=None)
    a = ap.parse_args()

    im = Image.open(a.fonte).convert('RGB')
    W, H = im.size
    # Escala: na linha do pé, a pintura cobre o mundo todo (a câmera vai de 0 a 2600).
    lado = min(a.emblema, W - a.emblema)
    k = ZOOM * MUNDO / 2 / lado                     # px da tela de repouso por px da pintura
    if a.tela_px:
        k = VW / a.tela_px
    centro = a.centro if a.centro is not None else a.emblema

    def tela_y(py):
        return GY + (py - a.chao) * k

    x_frac = 0.5 + (W / 2 - centro) * k / VW

    # Horizonte pelo anel: raio R (mundo) pela meia largura; a borda de longe em z = R.
    dz = D / ZOOM
    R = a.anel_meia * k / ZOOM
    yl = tela_y(a.anel_longe)
    f = dz / (dz + R)
    hz = (yl - GY * f) / (1 - f)                    # y do horizonte na tela
    if a.horizonte is not None:
        hz = a.horizonte * VH
    yj = tela_y(a.juncao)
    if yj <= hz:
        raise SystemExit(f'junção ({yj:.0f}) acima do horizonte ({hz:.0f}): confira as medidas')
    zj = dz * (GY - hz) / (yj - hz) - dz            # profundidade da junção
    print(f'escala {k:.4f} px/px  raio {R:.0f}  horizonte {hz:.1f} ({hz / VH:.4f})  junção z={zj:.0f}')

    nevoa = np.array([int(a.nevoa[i:i + 2], 16) for i in (1, 3, 5)], np.float32)
    os.makedirs(a.saida, exist_ok=True)

    # Parede: do topo até um pouco abaixo da junção (o chão cobre), com céu espelhado em cima.
    base_px = int(a.juncao) + 10
    parede = np.asarray(im.crop((0, 0, W, base_px))).astype(np.float32)
    ex = a.ceu_extra
    topo = parede[:ex][::-1].copy()
    t = (np.linspace(1, 0, ex)[:, None, None]) ** 1.2
    topo = topo * (1 - t * 0.85) + np.array([40, 20, 56], np.float32) * (t * 0.85)
    parede = np.concatenate([topo, parede], 0)
    # Névoa de profundidade: mais forte embaixo (o pé da arquibancada, junto do chão), leve no céu.
    hp = parede.shape[0]
    rampa = np.clip((np.arange(hp) - ex) / max(1, base_px), 0, 1)[:, None, None]
    parede = parede * (1 - 0.10 - 0.08 * rampa) + nevoa * (0.10 + 0.08 * rampa)
    pimg = Image.fromarray(np.clip(parede, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.1))
    pimg.save(os.path.join(a.saida, 'parede.jpg'), quality=92)

    # Chão: da junção para baixo; névoa só perto da junção, sumindo até o pé dos lutadores.
    cy0 = int(a.juncao) - 2
    chao = np.asarray(im.crop((0, cy0, W, H))).astype(np.float32)
    hc = chao.shape[0]
    rows = np.arange(hc) + cy0
    n = np.clip((a.chao - rows) / max(1, a.chao - a.juncao), 0, 1)[:, None, None] ** 1.5 * 0.16
    chao = chao * (1 - n) + nevoa * n
    Image.fromarray(np.clip(chao, 0, 255).astype(np.uint8)).save(os.path.join(a.saida, 'chao.jpg'), quality=93)

    stage = {
        "nome": "Arena Central (pintura)",
        "camera": {"distancia": D, "horizonte": round(hz / VH, 4)},
        "corFundo": "#2A1438",
        "luzLutadores": {"cor": "#FF9A70", "forca": 0.28, "brilho": 0.95, "pes": "#D8B8A8",
                         "contraluz": {"cor": "#FFB060", "forca": 1.0, "direcao": [-1, -1]}},
        "sombraLutadores": {"alfa": 0.72, "cor": "#1A0A12", "suave": 0.5, "x": 0.03, "y": 0.05},
        "camadas": [
            {"id": "parede", "imagem": "parede.jpg", "profundidade": round(zj + 1, 1),
             "topo": round(tela_y(-ex) / VH, 4), "base": round(tela_y(base_px) / VH, 4),
             "x": round(x_frac, 4), "repetir": "espelhar", "corAcima": "#281234"}
        ],
        "chao": {"imagem": "chao.jpg", "perto": -3000, "longe": round(zj, 1), "ate": round(zj, 1),
                 "reflexoLuzes": 0,
                 "projecao": {"topo": round(tela_y(cy0) / VH, 4), "base": round(tela_y(H) / VH, 4),
                              "x": round(x_frac, 4)}},
    }
    with open(os.path.join(a.saida, 'stage.json'), 'w') as fh:
        json.dump(stage, fh, ensure_ascii=False, indent=2)
        fh.write('\n')


if __name__ == '__main__':
    main()
