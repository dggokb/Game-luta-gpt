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

Paralaxe do fundo: ``--fundo IMG`` (céu e montanhas sem a arena, cobrindo da linha 0 até a altura
dela, na largura da pintura) e ``--mascara IMG`` (branco onde é arena, na mesma escala). A parede
vira PNG com transparência onde era céu e o fundo vai para ``--prof-fundo`` (bem mais longe),
com o céu espelhado em cima para o super pulo.

Fundo em dois planos: com ``--ceu IMG`` (só o céu, sem terra) e ``--mascara-ceu IMG`` (branco no
céu da pintura), o fundo vira o **céu** (``--prof-ceu``) e as **montanhas** (``--prof-fundo``, com
transparência no céu). Os dois usam os pixels da própria pintura no que aparece; o ``--fundo`` e
o ``--ceu`` (pintados à parte) só entram no que fica escondido atrás da arena e das montanhas,
com as cores ajustadas às da pintura. Os dois ficam apoiados na arena (``apoio``): no super pulo
descem com ela (não abre o que ninguém pintou) e para os lados cada um anda na sua velocidade.
``--zoom`` vai para ``camera.zoom`` (menor que 1 abre a câmera da luta neste cenário).
"""
import argparse
import json
import os

import numpy as np
from PIL import Image, ImageFilter

D, ZOOM, VW, VH, GY = 1000.0, 1.24, 1280.0, 720.0, 552.0   # câmera de repouso do jogo
MUNDO = 2600.0                                              # largura da arena


def casar_cores(fonte, alvo, m_fonte, m_alvo):
    """As cores de ``fonte`` com a média e o desvio de ``alvo`` (cada um medido na sua máscara)."""
    out = fonte.copy()
    for c in range(3):
        f, g = fonte[..., c][m_fonte], alvo[..., c][m_alvo]
        if f.size > 100 and g.size > 100:
            out[..., c] = (fonte[..., c] - f.mean()) * (g.std() / max(f.std(), 1e-3)) + g.mean()
    return out


def ceu_e_montanhas(a, im, W, base_px, ex, t, nevoa, tela_y, x_frac, zj):
    """O fundo em dois planos (céu e montanhas), com a pintura no que aparece e o pintado à parte
    só no que fica escondido. Devolve as duas camadas do stage.json."""
    from PIL import ImageFilter as F
    ceu_m = np.asarray(Image.open(a.mascara_ceu).convert('L')).astype(np.float32) / 255
    hm = ceu_m.shape[0]
    ceu_m = np.asarray(Image.fromarray((ceu_m * 255).astype(np.uint8)).resize((W, round(hm * W / ceu_m.shape[1])))).astype(np.float32) / 255
    hm = ceu_m.shape[0]
    arena = np.asarray(Image.open(a.mascara).convert('L').resize((W, hm))).astype(np.float32) / 255
    orig = np.asarray(im.crop((0, 0, W, hm))).astype(np.float32)
    so_ceu = np.asarray(Image.open(a.ceu).convert('RGB').resize((W, hm), Image.LANCZOS)).astype(np.float32)
    terra = np.asarray(Image.open(a.fundo).convert('RGB').resize((W, hm), Image.LANCZOS)).astype(np.float32)
    terra[:6] = terra[6]
    so_ceu[:6] = so_ceu[6]
    ceu_b, arena_b = ceu_m > 0.5, arena > 0.5
    monte_b = ~ceu_b & ~arena_b
    so_ceu = casar_cores(so_ceu, orig, np.ones_like(ceu_b), ceu_b)
    terra = casar_cores(terra, orig, ~ceu_b, monte_b)
    # Céu: a pintura no céu; atrás das montanhas e da arena, o céu pintado à parte (borda suave).
    # O que fica escondido atrás das montanhas e da arena: o céu pintado à parte (sem torres nem
    # morros), alinhado ao da pintura (o sol no mesmo lugar) e com as cores dela: a diferença de
    # cor, lisa, medida onde há céu e continuada para o resto. A máscara encolhe antes, para a
    # borda das torres e das montanhas não vir junto (ficava um fantasma delas no céu).
    import cv2
    duro = np.asarray(Image.fromarray((ceu_m * 255).astype(np.uint8)).filter(F.MinFilter(9))) > 128
    q = 8
    pequeno = lambda x: cv2.resize(x, (W // q, hm // q), interpolation=cv2.INTER_AREA)
    go, gc = pequeno(orig.mean(2)), pequeno(so_ceu.mean(2))
    mq = pequeno(duro.astype(np.float32)) > 0.99
    melhor = (0, 0, 1e18)
    for dy in range(-12, 13):
        for dx in range(-24, 25):
            m = np.roll(np.roll(gc, dy, 0), dx, 1)
            e = np.mean(((go - m) ** 2)[mq])
            if e < melhor[2]:
                melhor = (dx, dy, e)
    so_ceu = cv2.warpAffine(so_ceu, np.float32([[1, 0, melhor[0] * q], [0, 1, melhor[1] * q]]), (W, hm),
                            borderMode=cv2.BORDER_REFLECT)
    dif = pequeno(orig - so_ceu)
    dif[~mq] = 0
    # Duas escalas: perto do céu, a correção fina; longe dele, a larga (sem anel na emenda).
    lisa = np.zeros_like(dif)
    for sigma in (40, 6):
        peso = cv2.GaussianBlur(mq.astype(np.float32), (0, 0), sigma, borderType=cv2.BORDER_REFLECT)[..., None]
        est = cv2.GaussianBlur(dif, (0, 0), sigma, borderType=cv2.BORDER_REFLECT) / np.maximum(peso, 1e-4)
        conf = np.clip(peso / 0.15, 0, 1)
        lisa = est * conf + lisa * (1 - conf) if sigma == 6 else np.where(peso > 1e-4, est, 0)
    lisa = cv2.resize(lisa, (W, hm), interpolation=cv2.INTER_CUBIC)
    cheio = so_ceu + lisa
    k = cv2.GaussianBlur(duro.astype(np.float32), (0, 0), 3)[..., None]
    ceu = orig * k + cheio * (1 - k)
    # O céu de cima (super pulo) espelha o céu sem torres, não o da pintura (as lâmpadas das torres
    # tocam a borda de cima e viravam riscos).
    alto = min(ex, int(0.18 * hm))      # só a faixa de cima (o sol fica mais embaixo)
    topo = cv2.resize(np.ascontiguousarray(cheio[:alto][::-1]), (W, ex), interpolation=cv2.INTER_CUBIC)
    ceu = np.concatenate([topo * (1 - t * 0.85) + np.array([40, 20, 56], np.float32) * (t * 0.85), ceu], 0)
    Image.fromarray(np.clip(ceu, 0, 255).astype(np.uint8)).save(os.path.join(a.saida, 'ceu.jpg'), quality=90)
    # Montanhas: transparentes no céu; a pintura nelas; atrás da arena, a terra pintada à parte.
    ka = np.asarray(Image.fromarray((arena * 255).astype(np.uint8)).filter(F.MaxFilter(15)).filter(
        F.GaussianBlur(12))).astype(np.float32)[..., None] / 255
    monte = orig * (1 - ka) + terra * ka
    monte = monte * 0.93 + nevoa * 0.07
    rgba = np.dstack([np.clip(monte, 0, 255), (1 - ceu_m) * 255]).astype(np.uint8)
    Image.fromarray(rgba, 'RGBA').save(os.path.join(a.saida, 'montanhas.webp'), quality=90, alpha_quality=100, method=6)
    apoio = round(zj + 1, 1)
    return [
        {"id": "ceu", "imagem": "ceu.jpg", "profundidade": a.prof_ceu, "apoio": apoio,
         "topo": round(tela_y(-ex) / VH, 4), "base": round(tela_y(hm) / VH, 4),
         "x": round(x_frac, 4), "repetir": "espelhar", "corAcima": "#281234"},
        {"id": "montanhas", "imagem": "montanhas.webp", "profundidade": a.prof_fundo, "apoio": apoio,
         "topo": round(tela_y(0) / VH, 4), "base": round(tela_y(hm) / VH, 4),
         "x": round(x_frac, 4), "repetir": "espelhar"},
    ]


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
    ap.add_argument('--fundo', default=None)
    ap.add_argument('--mascara', default=None)
    ap.add_argument('--prof-fundo', type=float, default=9000.0)
    ap.add_argument('--ceu', default=None)
    ap.add_argument('--mascara-ceu', default=None)
    ap.add_argument('--prof-ceu', type=float, default=30000.0)
    ap.add_argument('--zoom', type=float, default=1.0)
    ap.add_argument('--chao-largo', default=None)
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
    camadas = []
    if a.fundo:
        # Com o fundo separado, a arena fica nítida (como o chão): sem desfoque e quase sem névoa;
        # quem dá a distância é o fundo, mais apagado.
        nitida = np.asarray(im.crop((0, 0, W, base_px))).astype(np.float32)
        nitida = nitida * 0.96 + nevoa * 0.04
        pimg = Image.new('RGB', (W, ex + base_px))
        pimg.paste(Image.fromarray(np.clip(nitida, 0, 255).astype(np.uint8)).filter(
            ImageFilter.UnsharpMask(radius=1.4, percent=35, threshold=2)), (0, ex))
        # Arena recortada (a máscara é o alfa) e o fundo numa camada própria, bem mais longe.
        masc = Image.open(a.mascara).convert('L')
        masc = masc.resize((W, round(masc.height * W / masc.width)), Image.LANCZOS)
        alfa = Image.new('L', (W, base_px), 255)
        alfa.paste(masc.crop((0, 0, W, min(base_px, masc.height))), (0, 0))
        par = pimg.crop((0, ex, W, ex + base_px)).convert('RGBA')
        par.putalpha(alfa)
        # PNG (sem perda): a WebP com perda e transparência saía com blocos de céu no celular.
        par.save(os.path.join(a.saida, 'parede.png'), optimize=True)
        if a.ceu:
            camadas.extend(ceu_e_montanhas(a, im, W, base_px, ex, t, nevoa, tela_y, x_frac, zj))
        else:
            fim = Image.open(a.fundo).convert('RGB')
            fh = round(fim.height * W / fim.width)
            fundo = np.asarray(fim.resize((W, fh), Image.LANCZOS)).astype(np.float32).copy()
            fundo[:6] = fundo[6]   # a imagem gerada vem com a 1ª linha clara: espelhada, vira um risco
            ftopo = fundo[:ex][::-1].copy()
            fundo = np.concatenate([ftopo * (1 - t * 0.85) + np.array([40, 20, 56], np.float32) * (t * 0.85), fundo], 0)
            fundo = fundo * 0.88 + nevoa * 0.12
            Image.fromarray(np.clip(fundo, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.8)).save(
                os.path.join(a.saida, 'fundo.jpg'), quality=90)
            camadas.append({"id": "fundo", "imagem": "fundo.jpg", "profundidade": a.prof_fundo,
                            "topo": round(tela_y(-ex) / VH, 4), "base": round(tela_y(fh) / VH, 4),
                            "x": round(x_frac, 4), "repetir": "espelhar", "corAcima": "#281234"})
        camadas.append({"id": "parede", "imagem": "parede.png", "profundidade": round(zj + 1, 1),
                        "topo": round(tela_y(0) / VH, 4), "base": round(tela_y(base_px) / VH, 4),
                        "x": round(x_frac, 4), "repetir": "espelhar"})
    else:
        pimg.save(os.path.join(a.saida, 'parede.jpg'), quality=92)
        camadas.append({"id": "parede", "imagem": "parede.jpg", "profundidade": round(zj + 1, 1),
                        "topo": round(tela_y(-ex) / VH, 4), "base": round(tela_y(base_px) / VH, 4),
                        "x": round(x_frac, 4), "repetir": "espelhar", "corAcima": "#281234"})

    # Chão: da junção para baixo; névoa só perto da junção, sumindo até o pé dos lutadores.
    # Com --chao-largo (a mesma pintura com mais chão dos dois lados, por igual, e a mesma altura),
    # o chão sai dela: com a câmera aberta o chão perto da tela passava da borda da pintura e
    # aparecia a cópia espelhada (o degrau da beira dobrado). Fica no máximo 4096 de largura.
    cy0 = int(a.juncao) - 2
    fonte_chao = im
    if a.chao_largo:
        larga = Image.open(a.chao_largo).convert('RGB')
        fonte_chao = larga.resize((round(larga.width * H / larga.height), H), Image.LANCZOS)
    chao = np.asarray(fonte_chao.crop((0, cy0, fonte_chao.width, H))).astype(np.float32)
    hc = chao.shape[0]
    rows = np.arange(hc) + cy0
    n = np.clip((a.chao - rows) / max(1, a.chao - a.juncao), 0, 1)[:, None, None] ** 1.5 * 0.16
    chao = chao * (1 - n) + nevoa * n
    chao_img = Image.fromarray(np.clip(chao, 0, 255).astype(np.uint8))
    if chao_img.width > 4096:
        chao_img = chao_img.resize((4096, round(chao_img.height * 4096 / chao_img.width)), Image.LANCZOS)
    chao_img.save(os.path.join(a.saida, 'chao.jpg'), quality=93)

    stage = {
        "nome": "Arena Central (pintura)",
        "camera": {"distancia": D, "horizonte": round(hz / VH, 4), "zoom": a.zoom},
        "corFundo": "#2A1438",
        "luzLutadores": {"cor": "#FF9A70", "forca": 0.34, "brilho": 0.95, "pes": "#D8B8A8",
                         "contraluz": {"cor": "#FFB060", "forca": 1.0, "direcao": [-1, -1]}},
        # O sol do conceito, baixo, atrás à esquerda: sombra comprida para a direita e para a frente.
        "sombraLutadores": {"alfa": 0.62, "cor": "#1A0A12", "suave": 0.55, "x": 1.05, "y": 0.2},
        "camadas": camadas,
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
