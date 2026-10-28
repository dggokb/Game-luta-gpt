#!/usr/bin/env python3
"""Converte um vídeo de animação (fundo verde) em folha normalizada do pipeline.

O vídeo vem do Seedance com o personagem de lado, câmera parada e fundo verde liso. Para
cada quadro do trecho escolhido: recorta o verde (e a sombra/faixa de chão), mantém só o
personagem (maior peça conectada; poeira e efeitos soltos saem), corrige o deslizamento
lateral e escala tudo para o tamanho oficial do personagem. A raiz (meio dos pés) cai em
rootX/rootY da célula.

O tamanho nunca é chutado: --personagem mede a escala comparando o vídeo com as imagens
iniciais (art/keys/<id>/inicio_*.png, ver tools/sprites/tamanho.py). Só quando o vídeo não
bate com nada é que se passa --escala, e aí --motivo é obrigatório.

    python3 tools/sprites/video_para_sprite.py video.mp4 --personagem p03 \
        --clipe tools/sprites/clips/p03_jab.json --inicio 16 --fim 37

Com --clipe a folha vai para o `source` do clipe e a receita (vídeo, escala, quadros e
opções) fica gravada no próprio clipe, em "video"; para refazer basta
`video_para_sprite.py video.mp4 --clipe <clipe>` (opções da linha de comando mudam a receita).
`--loop A-B` procura, dentro de A..B, o trecho que melhor fecha em loop.

Precisa de opencv e numpy: pip install -r tools/sprites/requirements-animar.txt
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

import tamanho

ROOT = Path(__file__).resolve().parents[2]


def read_frames(path):
    cap = cv2.VideoCapture(path)
    frames = []
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        frames.append(frame)
    return frames, cap.get(cv2.CAP_PROP_FPS)


def best_loop(frames, lo, hi, min_len):
    """Compara os quadros recortados em volta do tronco, para funcionar mesmo se o
    personagem desliza no vídeo. Só os quadros de lo a hi são recortados."""
    small = {}
    for i in range(lo, hi + 1):
        k = key(frames[i])
        cx = int(torso_x(k))
        g = cv2.cvtColor(k[..., :3], cv2.COLOR_RGB2GRAY) * (k[..., 3] / 255)
        g = np.pad(g, ((0, 0), (400, 400)))[:, cx: cx + 800]
        small[i] = cv2.resize(g, (100, 90)).astype(np.float32)
    best = None
    for a in range(lo, hi - min_len + 1):
        for b in range(a + min_len, hi + 1):
            d = float(np.abs(small[a] - small[b]).mean())
            if best is None or d < best[0]:
                best = (d, a, b)
    return best


# Fundo é o verde da tela, que é claro (v ~240): preto com reflexo verde (ponta da faixa, luva,
# cabelo) tem matiz e saturação de verde, mas é escuro, e é do personagem.
SCREEN_MIN_V = 100
# Vídeo que escurece o fundo (o Ultra da p15 apaga a luz do verde): o mínimo cai com o verde da
# borda do quadro, até este piso; o personagem não escurece junto.
SCREEN_MIN_V_FLOOR = 36


def screen_min_v(hsv):
    """Brilho mínimo do verde da tela neste quadro: SCREEN_MIN_V no fundo de sempre, menos no
    fundo escurecido (55% do verde medido na borda do quadro)."""
    b = 8
    border = np.concatenate([hsv[:b].reshape(-1, 3), hsv[-b:].reshape(-1, 3),
                             hsv[:, :b].reshape(-1, 3), hsv[:, -b:].reshape(-1, 3)])
    green = border[(border[:, 0] > 35) & (border[:, 0] < 95) & (border[:, 1] > 45)]
    if len(green) < 100:
        return SCREEN_MIN_V
    return int(min(SCREEN_MIN_V, max(SCREEN_MIN_V_FLOOR, 0.55 * float(np.median(green[:, 2])))))
JOIN_GAP = 7       # px: peça grande a essa distância do corpo é do corpo (faixa verde da roupa)
JOIN_AREA = 800


def keep_largest(alpha, objects=0):
    """Zera tudo fora da maior peça conectada (poeira, efeitos e restos soltos). Peça grande
    colada no corpo fica: roupa com faixa verde igual ao fundo (calça do p10) corta a perna,
    e a bota viraria uma peça solta. Com objects > 0, peça solta com pelo menos essa área
    (px do vídeo) também fica: objeto do golpe longe do corpo (a casca de banana da p16)."""
    n, labels, stats, _ = cv2.connectedComponentsWithStats((alpha > 0).astype(np.uint8))
    if n > 1:
        main = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
        near = cv2.dilate((labels == main).astype(np.uint8), np.ones((2 * JOIN_GAP + 1,) * 2, np.uint8))
        keep = {main} | {i for i in np.unique(labels[(near > 0) & (labels > 0)])
                         if stats[i, cv2.CC_STAT_AREA] >= JOIN_AREA}
        if objects > 0:
            keep |= {i for i in range(1, n) if stats[i, cv2.CC_STAT_AREA] >= objects}
        alpha[~np.isin(labels, list(keep))] = 0
    return alpha


def floor_dust(alpha, h, s, v):
    """Poeira levantada no chão: esverdeada (tingida pelo fundo) ou bege clara, só na faixa
    dos pés, onde não há cabelo nem pele. Tênis branco e roupa escura (sem essas cores) ficam."""
    rows = np.nonzero((alpha > 0).any(1))[0]
    if len(rows) == 0:
        return np.zeros_like(alpha, bool)
    top, bottom = rows.min(), rows.max()
    band = np.zeros_like(alpha, bool)
    band[bottom - (bottom - top) * 12 // 100:] = True
    tinted = (h > 35) & (h < 95) & (s >= 10) & (v >= 90)
    beige = (h >= 10) & (h <= 34) & (s >= 10) & (s < 45) & (v >= 120) & (v < 230)
    return band & (tinted | beige)


def key(frame, dust=False, effects=False, floor=False, soft=False, objects=0, glow=False):
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    h, s, v = (hsv[..., i].astype(int) for i in range(3))
    bg = (h > 35) & (h < 95) & (s > 45) & (v >= screen_min_v(hsv))
    if dust:  # poeira bege dos pés (o jogo desenha os efeitos)
        bg |= (h >= 8) & (h <= 34) & (s >= 18) & (s <= 70) & (v >= 120)
    if effects:
        # Clarão de impacto: amarelo claro (a pele é mais alaranjada e menos clara).
        bg |= (h >= 22) & (h <= 40) & (v >= 200) & (s >= 40)
        # Anel de energia ciano-petróleo (o azul da roupa é mais anil, matiz >= 104).
        bg |= (h >= 85) & (h <= 102) & (s >= 120)
        # Poeira de impacto, brilho e arco de golpe claros, tingidos de verde pelo fundo.
        bg |= (h > 35) & (h < 95) & (s >= 14) & (v >= 140)
    alpha = np.where(bg, 0, 255).astype(np.uint8)
    if effects:
        # Rastro de golpe: faixa fina, clara e sem cor. Some na abertura morfológica,
        # enquanto calça, cabelo e faixa (largos) ficam.
        thick = cv2.morphologyEx(alpha, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (15, 15)))
        thick = cv2.dilate(thick, np.ones((5, 5), np.uint8))
        pale = (s < 60) & (v > 150)
        thin = (alpha > 0) & (thick == 0) & (pale | ((h >= 20) & (h <= 50)))
        # Rastro largo: claro e longe de qualquer parte colorida do corpo (pele, azul,
        # preto, vermelho). A calça branca fica perto da faixa azul, da faixa e do tênis.
        colored = ((alpha > 0) & ~pale).astype(np.uint8)
        body = cv2.dilate(colored, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (91, 91)))
        alpha[thin | ((alpha > 0) & pale & (body == 0))] = 0
        keep_largest(alpha, objects)
    if floor:
        alpha[floor_dust(alpha, h, s, v)] = 0
        alpha = cv2.morphologyEx(alpha, cv2.MORPH_OPEN, np.ones((5, 5), np.uint8))
    alpha = keep_largest(cv2.morphologyEx(alpha, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8)), objects)
    # Furinhos dentro do corpo (reflexo verde na roupa branca) voltam a ser opacos;
    # vãos grandes, como entre as pernas, continuam transparentes.
    _, labels, stats, _ = cv2.connectedComponentsWithStats((alpha == 0).astype(np.uint8))
    h_, w_ = alpha.shape
    x, y, w, h, area = stats.T
    fill = (area < 600) & (x > 0) & (y > 0) & (x + w < w_) & (y + h < h_)
    fill[0] = False
    alpha[fill[labels]] = 255
    alpha = cv2.GaussianBlur(alpha, (3, 3), 0)
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB).astype(int)
    rgb[..., 1] = np.minimum(rgb[..., 1], np.maximum(rgb[..., 0], rgb[..., 2]) + 8)  # tira o verde da borda
    out = np.dstack([rgb.astype(np.uint8), alpha])
    out = soft_edges(frame, out) if soft else out
    return keep_glow(frame, out, glow if isinstance(glow, (tuple, list)) else None) if glow else out


GLOW_MIN = 0.1        # alfa mínimo do halo (abaixo é ruído da compressão no verde)
GLOW_CORE = 0.85      # alfa do miolo que faz uma mancha clara ser efeito, e não ruído
GLOW_CORE_AREA = 12   # px do vídeo de miolo para a mancha contar
GLOW_LIGHT = 150      # brilho mínimo da cor tirada do verde (sombra no chão sai escura)
GLOW_REACH = 31       # px do vídeo além da mancha onde o halo ainda se espalha
GLOW_EDGE = 60        # px do vídeo junto da borda em que o efeito apaga
GLOW_NO_BLUE = 25     # azul abaixo do qual o pixel do corpo é mistura do efeito com o verde
GLOW_TINTED = 0.6     # verde do miolo (fração do verde do fundo) abaixo do qual o efeito é colorido
GLOW_BLUR = 31        # px do vídeo da suavização do halo (some com os blocos da compressão)


def effect_in_body(frame, rgba):
    """Pixels que o key() deu como corpo e são o efeito misturado ao verde: sem azul (vermelho,
    laranja, amarelo); roupa, pele e cabelo têm."""
    f = frame.astype(np.float32)
    return (rgba[..., 3] >= 128) & (f[..., 0] < GLOW_NO_BLUE) & (np.maximum(f[..., 1], f[..., 2]) > 120)


def keep_glow(frame, rgba, tint=None):
    """Efeito de luz desenhado pelo vídeo (bola de energia, brilho na mão, raio), mantido com o
    halo meio transparente: o key() corta o halo, que é amarelo misturado com o verde (matiz de
    fundo), e o keep_largest apaga o raio, solto do corpo. Aqui cada pixel ganha o alfa pelo quanto
    o verde do fundo sumiu dele (alfa = 1 - verde / verde do fundo) e a cor sem o fundo; ficam as
    manchas claras com miolo forte (o ruído do verde tem alfa baixo e nenhum miolo). O corpo
    recortado continua por cima.

    tint (R, G, B): a cor do efeito, quando ela é conhecida (a aura vermelha do S2 da p15): o alfa
    de cada pixel sai do quanto o verde do fundo caiu em direção ao verde dessa cor, em todos os
    pixels claros ou misturados, e a aura que o key() juntou ao corpo sai do corpo."""
    f = frame.astype(np.float32)
    green = f[..., 1] - np.maximum(f[..., 0], f[..., 2])
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    screen = (hsv[..., 0] > 35) & (hsv[..., 0] < 95) & (hsv[..., 1] > 45) & (hsv[..., 2] >= screen_min_v(hsv))
    if screen.sum() < 100:
        return rgba
    full = float(np.median(green[screen]))
    color = np.median(f[screen], axis=0)
    a = np.clip(1 - green / max(1.0, full), 0, 1)
    unmixed = np.clip((f - (1 - a[..., None]) * color) / np.maximum(a, 1e-3)[..., None], 0, 255)
    # Efeito colado no corpo, que o key() juntou ao corpo: a mistura dele com o verde não tem azul
    # (vermelho, laranja, amarelo); roupa, pele e cabelo têm. Fora disso o corpo fica de fora.
    mixed = effect_in_body(frame, rgba)
    light = (a >= GLOW_MIN) & (unmixed.max(axis=2) >= GLOW_LIGHT) & ((rgba[..., 3] < 128) | mixed)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(light.astype(np.uint8))
    core = np.bincount(labels[light & (a >= GLOW_CORE)], minlength=n)
    kept = [i for i in range(1, n) if core[i] >= GLOW_CORE_AREA]
    keep = np.isin(labels, kept)
    # Efeito de cor com pouco verde (aura vermelha): a medida pelo verde que sobra exagera o alfa
    # (vermelho misturado ao verde perde o "verde a mais" antes da metade) e o halo sai
    # verde-amarelado. Com a cor do miolo conhecida, o alfa sai do quanto o verde do fundo caiu.
    bg_g = float(color[1])
    override = np.zeros(a.shape, bool)
    if tint is not None:
        # Cor conhecida: a mesma conta em todos os pixels que podem ser do efeito.
        c_g = float(tint[1])
        region = (rgba[..., 3] < 128) | mixed
        ai = np.clip((bg_g - f[..., 1]) / max(1.0, bg_g - c_g), 0, 1)
        a = np.where(region, ai, a)
        unmixed = np.where(region[..., None], np.clip((f - (1 - a[..., None]) * color) / np.maximum(a, 1e-3)[..., None], 0, 255), unmixed)
        light = region & (a >= GLOW_MIN) & (unmixed.max(axis=2) >= GLOW_LIGHT)
        n, labels, stats, _ = cv2.connectedComponentsWithStats(light.astype(np.uint8))
        core = np.bincount(labels[light & (a >= GLOW_CORE)], minlength=n)
        kept = [i for i in range(1, n) if core[i] >= GLOW_CORE_AREA]
        keep = np.isin(labels, kept)
        override = mixed.copy()
        kept = []
    for i in kept:
        blob = labels == i
        # O verde da cor do efeito: o do miolo mais puro (a mistura com o fundo só soma verde).
        c_g = float(np.percentile(f[..., 1][blob & (a >= GLOW_CORE)], 10))
        if c_g >= GLOW_TINTED * bg_g:
            continue   # amarelo e branco: o verde do efeito confunde com o do fundo; fica a medida de cima
        ai = np.clip((bg_g - f[..., 1][blob]) / max(1.0, bg_g - c_g), 0, 1)
        a[blob] = ai
        unmixed[blob] = np.clip((f[blob] - (1 - ai[:, None]) * color) / np.maximum(ai, 1e-3)[:, None], 0, 255)
        override |= blob & mixed   # a aura que o key() deu como corpo sai com o alfa dela
    # O halo fraco vem em blocos da compressão: a borda da mancha e o alfa são suavizados, e a
    # cor de cada pixel é a média ponderada pelo alfa em volta (no halo fraco a cor tirada do
    # verde é só ruído).
    near = cv2.dilate(keep.astype(np.uint8), cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (GLOW_REACH,) * 2)) > 0
    glow = cv2.GaussianBlur(np.where(near, a, 0).astype(np.float32), (GLOW_BLUR, GLOW_BLUR), 0)
    glow = np.where(keep & (a >= 0.5), a, glow)
    weight = cv2.GaussianBlur(np.where(keep, a, 0).astype(np.float32), (GLOW_BLUR, GLOW_BLUR), 0)
    tint = cv2.GaussianBlur(unmixed * np.where(keep, a, 0)[..., None], (GLOW_BLUR, GLOW_BLUR), 0)
    # Longe do miolo o peso some: a cor tende à do miolo (sem ela, o halo de fora sai escuro).
    base = np.median(unmixed[keep & (a >= GLOW_CORE)], axis=0) if (keep & (a >= GLOW_CORE)).any() else color
    tint = (tint + 0.05 * base) / (weight + 0.05)[..., None]
    rgb = np.where(keep[..., None], unmixed, tint)[..., ::-1]
    out = rgba.copy()
    # O efeito que passa da borda do vídeo (a aura alta) apaga aos poucos, em vez de cortar reto.
    h_, w_ = glow.shape
    ramp = np.minimum(1.0, np.minimum.outer(np.arange(h_), np.minimum(np.arange(w_), w_ - 1 - np.arange(w_))) / GLOW_EDGE)
    glow = glow * np.where(keep | (glow > 0), ramp, 1.0)
    alpha = (np.clip(glow, 0, 1) * 255).astype(np.uint8)
    more = ((alpha >= 10) & (alpha > out[..., 3])) | override
    out[..., 3][more] = alpha[more]
    out[..., :3][more] = np.clip(rgb[more], 0, 255).astype(np.uint8)
    return out


SOFT_FROM = 14      # "verde" (G - max(R, B)) até onde o pixel é todo do personagem
SOFT_REACH = 7      # px além do contorno onde a mecha meio transparente ainda conta
SOFT_DARK = 110     # só cor escura vira mecha (cabelo, faixa, luva): o resto seria franja


def soft_edges(frame, rgba):
    """Recorte suave (cabelo comprido de fios finos, p15): o key() acima é binário e a abertura
    apaga as mechas finas, deixando o cabelo em bloco serrilhado; e o fechamento de furinhos
    tapa vãos de fundo entre as mechas, que viram mancha clara depois de tirar o verde.

    Aqui: vão de fundo verde saturado que tinha sido tapado volta a ser transparente, e em
    volta do contorno a mistura de cor escura com o fundo ganha alfa pelo quanto ela é verde
    (alfa = 1 - verde / verde do fundo), com a cor do fundo tirada da mistura."""
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    h, s, v = (hsv[..., i].astype(int) for i in range(3))
    screen = (h > 35) & (h < 95) & (s > 45) & (v >= screen_min_v(hsv))
    alpha = rgba[..., 3].copy()
    n, labels, stats, _ = cv2.connectedComponentsWithStats(screen.astype(np.uint8))
    for i in range(1, n):
        if stats[i, cv2.CC_STAT_AREA] >= 4:
            m = labels == i
            if (alpha[m] > 128).mean() > 0.5:
                alpha[m] = 0
    f = frame.astype(np.float32)
    green = f[..., 1] - np.maximum(f[..., 0], f[..., 2])
    back = screen & (alpha < 10)
    if back.sum() < 100:
        return np.dstack([rgba[..., :3], alpha])
    full = np.percentile(green[back], 5)   # o fundo mais fraco ainda é fundo
    color = np.median(f[back], axis=0)
    a = np.clip((full - green) / max(1.0, full - SOFT_FROM), 0, 1)
    unmixed = np.clip((f - (1 - a[..., None]) * color) / np.maximum(a, 1e-3)[..., None], 0, 255)
    body = alpha > 128
    near = cv2.dilate(body.astype(np.uint8), np.ones((2 * SOFT_REACH + 1,) * 2, np.uint8)) > 0
    strand = near & ~body & (unmixed.max(axis=2) < SOFT_DARK) & (a >= 0.08)
    out = rgba.copy()
    out[..., 3] = alpha
    out[..., 3][strand] = np.maximum(alpha[strand], (a[strand] * 255).astype(np.uint8))
    rgb = unmixed[..., ::-1].copy()
    rgb[..., 1] = np.minimum(rgb[..., 1], np.maximum(rgb[..., 0], rgb[..., 2]))   # ponta da mecha sem verde
    out[..., :3][strand] = rgb[strand].astype(np.uint8)
    hair_shine(out)
    return out


SHINE_AREA = (12, 400)   # px do vídeo: menor é o branco do olho; maior é a roupa
SHINE_TOP = 0.18         # só cabeça e pescoço (a gola no ombro, mais abaixo, é roupa de verdade)
SHINE_DARK = 0.35        # parte da borda da ilha que tem de ser cabelo escuro


def hair_shine(rgba):
    """Ilha clara pequena entre as mechas (a gola branca aparecendo atrás do pescoço, p15): no
    jogo parece fundo que não foi recortado. Vira a cor do cabelo em volta."""
    a = rgba[..., 3] > 128
    rows = np.nonzero(a.any(1))[0]
    if not len(rows):
        return
    top = rows.min() + SHINE_TOP * (rows.max() - rows.min())
    hsv = cv2.cvtColor(rgba[..., :3], cv2.COLOR_RGB2HSV)
    s, v = hsv[..., 1].astype(int), hsv[..., 2].astype(int)
    pale = a & (s < 60) & (v > 160)
    dark = a & (v < 70)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(pale.astype(np.uint8))
    for i in range(1, n):
        x, y, w, h, area = stats[i]
        if not SHINE_AREA[0] <= area <= SHINE_AREA[1] or y + h / 2 > top:
            continue
        island = labels == i
        # A borda suavizada da ilha (meio clara) vai junto, senão fica um contorno pontilhado.
        edge = cv2.dilate(island.astype(np.uint8), np.ones((5, 5), np.uint8)).astype(bool) & a & (v > 90) & (s < 90)
        island |= edge
        ring = cv2.dilate(island.astype(np.uint8), np.ones((5, 5), np.uint8)).astype(bool) & ~island & a
        if ring.sum() == 0 or dark[ring].mean() < SHINE_DARK:
            continue
        rgba[..., :3][island] = np.median(rgba[..., :3][ring & dark], axis=0).astype(np.uint8)


def torso_x(rgba):
    """Centro horizontal do tronco (faixa do ombro à cintura), estável durante a passada."""
    a = rgba[..., 3] > 128
    rows = np.nonzero(a.any(1))[0]
    top, bot = rows.min(), rows.max()
    band = a[top + (bot - top) * 15 // 100: top + (bot - top) * 45 // 100]
    return float(np.nonzero(band)[1].mean())


# Opções gravadas na receita do clipe (nome do argumento -> padrão).
RECIPE = dict(inicio=None, fim=None, passo=1, quadros=None, celula=256, largura=None,
              raiz="128,238", colunas=8, fixar="tronco", pe_no_chao=False, deslocar="0,0",
              sem_poeira=False, sem_efeitos=False, manter_efeitos=False, cor_efeito=None, limpar_chao=False, ancorar_pe=False, reescala=None, limpar_quadros=None,
              limpar_area=None, aparar_borda=0, densidade=1, nitidez=False, referencia=None, recorte_suave=False,
              balanco=1.0, objetos=0, rampa=None, cabeca_em=None)


def parser():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("video")
    p.add_argument("saida", nargs="?", help="folha de saída (padrão: o source do --clipe)")
    p.add_argument("--clipe", help="clipe do pipeline: lê e grava a receita em \"video\"")
    p.add_argument("--personagem", help="mede a escala pelas imagens iniciais (art/keys/<id>)")
    p.add_argument("--escala", type=float, help="escala manual, só quando a medida falha (exige --motivo)")
    p.add_argument("--motivo", help="por que a escala é manual")
    p.add_argument("--inicio", type=int)
    p.add_argument("--fim", type=int, help="quadro final, exclusivo")
    p.add_argument("--loop", help="A-B: escolhe o melhor loop dentro desse intervalo")
    p.add_argument("--min-loop", type=int, default=24)
    p.add_argument("--passo", type=int, default=1, help="usar 1 a cada N quadros")
    p.add_argument("--quadros", help="lista exata de quadros do vídeo, ex.: 10,14,18,22")
    p.add_argument("--celula", type=int, default=256, help="altura da célula")
    p.add_argument("--largura", type=int, help="largura da célula (padrão: igual à altura)")
    p.add_argument("--sem-poeira", action="store_true", help="remove poeira bege do chão")
    p.add_argument("--sem-efeitos", action="store_true", help="remove clarão de impacto e rastro de golpe")
    p.add_argument("--manter-efeitos", action="store_true",
                   help="mantém o efeito de luz do vídeo com o halo meio transparente (keep_glow): "
                        "bola de energia, brilho na mão, raio solto do corpo")
    p.add_argument("--cor-efeito", help="R,G,B: a cor do efeito mantido (com --manter-efeitos), quando ela é "
                   "conhecida: a aura vermelha sai sem o verde misturado e solta do corpo")
    p.add_argument("--recorte-suave", action="store_true",
                   help="mechas de cabelo meio transparentes e vãos de fundo entre elas abertos (soft_edges)")
    p.add_argument("--limpar-chao", action="store_true",
                   help="remove poeira cinza-esverdeada na faixa dos pés (seguro com cabelo claro e pele)")
    p.add_argument("--colunas", type=int, default=8)
    p.add_argument("--raiz", default="128,238")
    p.add_argument("--fixar", choices=["tronco", "volta", "quadro", "registro", "video"], default="tronco",
                   help="tronco: anula o deslizamento lateral; quadro: cada quadro no próprio tronco "
                        "(o jogo é que empurra, ex.: defesa); registro: idem, com o tronco medido como "
                        "na auditoria de harmonia (levantar, reações); video: mantém a posição do vídeo; "
                        "volta: como tronco, mas tira o avanço da volta inteira do loop (andar de um "
                        "passo só: a folha e o motor andam o mesmo e o loop emenda sem solavanco)")
    p.add_argument("--balanco", type=float, default=1.0,
                   help="com --fixar volta: quanto do vaivém do tronco no passo fica (1 = o do vídeo, "
                        "0 = recua/avança constante; 0.5 = uma pausa do vídeo vira só uma freada)")
    p.add_argument("--cabeca-em", type=float,
                   help="cada quadro com o topo da cabeça a N px (do jogo) acima da raiz: golpe no ar "
                        "na altura do pulo (o pé no chão afundava o corpo quando as pernas encolhem)")
    p.add_argument("--pe-no-chao", action=argparse.BooleanOptionalAction, default=False,
                   help="cada quadro com os pés na raiz (poses no ar: o jogo é que sobe e desce); "
                        "--no-pe-no-chao desliga o que a receita gravada tiver")
    p.add_argument("--reescala", type=lambda v: [float(x) for x in v.split(",")],
                   help="fator de tamanho por quadro (tools/sprites/reescalar.py): divide a escala")
    p.add_argument("--objetos", type=int, default=0,
                   help="mantém peça solta com pelo menos essa área em px do vídeo (objeto do golpe: "
                        "casca de banana, balão), em vez de só o corpo")
    p.add_argument("--limpar-quadros", type=lambda v: [int(x) for x in v.split(",")],
                   help="células a limpar de efeito depois de converter (tools/sprites/limpar.py)")
    p.add_argument("--limpar-area", type=json.loads,
                   help='faixa de altura onde limpar cada célula, {"4": [0, 0.3]} (fração do corpo, 0 = topo)')
    p.add_argument("--ancorar-pe", action="store_true",
                   help="depois de converter, fixa o pé de apoio no lugar do idle (tools/sprites/ancorar.py)")
    p.add_argument("--aparar-borda", type=int, default=0,
                   help="apaga N px nas bordas de cima e dos lados de cada célula: o que passa da "
                        "célula máxima (chicote, capa) sai cortado limpo em vez de colado na borda")
    p.add_argument("--densidade", type=int, default=1,
                   help="px de textura por px do jogo (2 = HD tirado direto do vídeo): célula, raiz e "
                        "medidas da receita continuam lógicas; o clipe ganha \"pixelScale\"")
    p.add_argument("--referencia", type=json.loads,
                   help='folha 1x revisada cujos retoques à mão a folha HD repete (tools/sprites/retoque_hd.py): '
                        '{"folha": "art/...png", "celula": [W, H], "raiz": [X, Y], "colunas": N}')
    p.add_argument("--escala-da-receita", action="store_true",
                   help="reusa a escala medida gravada no clipe em vez de medir de novo (refazer em "
                        "outra densidade sem mudar o tamanho)")
    p.add_argument("--nitidez", action="store_true",
                   help="unsharp mask leve e borda sem verde (franja do recorte) depois de escalar")
    p.add_argument("--deslocar", default="0,0",
                   help="DX,DY em px da célula aplicado a todos os quadros (acerto fino de registro)")
    p.add_argument("--rampa", default=None,
                   help="DX,ATE: desloca os quadros aos poucos, de 0 no primeiro a DX px da célula no "
                        "quadro ATE (e DX dali em diante); emenda clipes de vídeos diferentes (a queda "
                        "deitada no lugar em que o vídeo do levantar começa)")
    p.add_argument("--previa", help="GIF de prévia no tamanho do jogo")
    return p


def parse_args(argv=None):
    p = parser()
    known, _ = p.parse_known_args(argv)
    clip = None
    if known.clipe:
        clip = json.loads(Path(known.clipe).read_text(encoding="utf-8"))
        recipe = clip.get("video", {})
        keep = set(RECIPE) | {"personagem"} | ({"escala", "motivo"} if "motivo" in recipe else set())
        p.set_defaults(**{k: v for k, v in recipe.items() if k in keep})  # escala medida é medida de novo
        if "medida" in recipe:
            p.set_defaults(_medida=recipe)
    args = p.parse_args(argv)
    if args.saida is None:
        if clip is None:
            p.error("informe a saída ou --clipe")
        args.saida = str(ROOT / clip["source"])
    if args.escala is not None and not args.motivo:
        p.error("--escala manual exige --motivo (fica gravado no clipe)")
    if args.escala is None and not args.personagem:
        p.error("use --personagem <id> para medir a escala (ou --escala N --motivo '...')")
    return args, clip


def save_recipe(path, clip, args, start, end, measured, count, cw):
    recipe = {"arquivo": Path(args.video).name}
    if args.personagem:
        recipe["personagem"] = args.personagem
    recipe["escala"] = round(args.escala, 4) if args.escala is not None else round(measured["escala"], 4)
    if args.escala is not None:
        recipe["motivo"] = args.motivo
    elif "gravada" in measured:
        recipe["medida"] = measured["gravada"]
    else:
        recipe["medida"] = f"{measured['medida']} (nota {measured['nota']:.2f})"
    resolved = dict(vars(args), inicio=start, fim=end)
    if args.quadros:
        resolved.update(inicio=None, fim=None)
    recipe.update({k: resolved[k] for k, default in RECIPE.items() if resolved[k] != default})
    rx, ry = (int(v) for v in args.raiz.split(","))
    clip.update(expectedFrames=count, columns=args.colunas, video=recipe)
    d = args.densidade
    for name, value, default in (("frameWidth", cw * d, 256), ("frameHeight", args.celula * d, 256),
                                 ("rootX", rx * d, 128), ("rootY", ry * d, 238)):
        if value != default:
            clip[name] = value
        else:
            clip.pop(name, None)
    if d != 1:
        clip["pixelScale"] = float(d)
    else:
        clip.pop("pixelScale", None)
    Path(path).write_text(json.dumps(clip, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"receita gravada em {path}")


def registration_x(rgba):
    """Tronco como a auditoria de harmonia mede: mediana do meio de cada linha entre 30% e
    55% da altura da silhueta."""
    a = rgba[..., 3] > 10
    rows = np.nonzero(a.any(1))[0]
    top, h = rows.min(), rows.max() + 1 - rows.min()
    mids = []
    for y in range(top + int(h * 0.30), top + int(h * 0.55)):
        xs = np.nonzero(a[y])[0]
        if len(xs):
            mids.append((xs.min() + xs.max()) / 2)
    return float(np.median(mids))


MAX_SIDE = 4096  # px: lado máximo de textura que a GPU de qualquer aparelho aceita (folha HD)
FRINGE = 2      # px físicos da borda onde o verde do fundo ainda tinge a cor
SHARP_RADIUS, SHARP_AMOUNT = 1.2, 0.6


def sharpen(img, d):
    """Versão b do HD: nitidez leve (unsharp mask) e borda sem franja verde.

    A cor é afiada sobre fundo opaco (sangrando a cor do corpo para fora, para o fundo
    transparente não escurecer a borda) e a franja dos FRINGE*d/2 px da borda perde o verde que
    ainda passa da média de vermelho e azul."""
    import cv2
    a = np.array(img)
    alpha = a[..., 3]
    if not alpha.any():
        return img
    rgb = a[..., :3].astype(np.float32)
    solid = alpha > 200
    # Cor do corpo sangrada para fora: o borrão do unsharp não puxa preto da transparência.
    _, idx = cv2.distanceTransformWithLabels((~solid).astype(np.uint8), cv2.DIST_L2, 5,
                                             labelType=cv2.DIST_LABEL_PIXEL)
    ys, xs = np.nonzero(solid)
    lut = np.zeros((idx.max() + 1, 3), np.float32)
    lut[idx[ys, xs]] = rgb[ys, xs]
    bled = np.where(solid[..., None], rgb, lut[idx])
    blur = cv2.GaussianBlur(bled, (0, 0), SHARP_RADIUS * d / 2)
    rgb = np.clip(bled + SHARP_AMOUNT * (bled - blur), 0, 255)
    # Franja: perto da transparência, o verde não passa da média de vermelho e azul.
    inner = cv2.erode((alpha > 128).astype(np.uint8), np.ones((2 * FRINGE * d // 2 + 1,) * 2, np.uint8))
    edge = (alpha > 0) & (inner == 0)
    g_max = (rgb[..., 0] + rgb[..., 2]) / 2
    rgb[..., 1] = np.where(edge, np.minimum(rgb[..., 1], g_max), rgb[..., 1])
    # A borda meio transparente mais externa (só verde misturado ao fundo) sai.
    a[..., 3] = np.where(alpha < 48, 0, alpha)
    a[..., :3] = rgb.astype(np.uint8)
    return Image.fromarray(a, "RGBA")


def main(argv=None):
    args, clip = parse_args(argv)
    frames, fps = read_frames(args.video)
    if args.loop:
        lo, hi = (int(v) for v in args.loop.split("-"))
        d, start, end = best_loop(frames, lo, min(hi, len(frames) - 1), args.min_loop)
        print(f"loop {start}-{end} (diferença {d:.2f})")
    else:
        start, end = args.inicio or 0, args.fim or len(frames)
    picked = ([int(v) for v in args.quadros.split(",")] if args.quadros
              else list(range(start, end, args.passo)))
    cache = {}

    def keyed_frame(i, dust=False, effects=False, floor=False):
        soft, objects = args.recorte_suave, args.objetos
        glow = (tuple(int(v) for v in args.cor_efeito.split(",")) if args.cor_efeito else True) if args.manter_efeitos else False
        if (i, dust, effects, floor, soft) not in cache:
            cache[i, dust, effects, floor, soft] = key(frames[i], dust, effects, floor, soft, objects, glow)
        return cache[i, dust, effects, floor, soft]

    measured = None
    if args.escala is not None:
        s = args.escala
        print(f"escala manual {s:.4f}: {args.motivo}")
    else:
        gravada = getattr(args, "_medida", None)
        if args.escala_da_receita and gravada:
            measured = dict(escala=gravada["escala"], gravada=gravada["medida"], medida=gravada["medida"], nota=0, zoom=0)
        else:
            measured = tamanho.medir(frames, args.personagem, key)
        s = measured["escala"]
        print(f"escala {s:.4f} ({measured['medida']}, nota {measured['nota']:.2f}, zoom {measured['zoom']:.3f})")
    scale = s
    keyed = [keyed_frame(i, args.sem_poeira, args.sem_efeitos, args.limpar_chao) for i in picked]
    xs = np.array([torso_x(k) for k in keyed])
    if args.fixar == "tronco":
        # Remove só a tendência (o deslizamento); o balanço natural do corpo continua.
        t = np.arange(len(xs))
        trend = np.polyval(np.polyfit(t, xs, 1), t)
        offsets = -(trend - trend[0])
    elif args.fixar == "volta":
        # Avanço por quadro = do 1º quadro até o que viria depois do último, dividido pela volta.
        # É a mesma conta da distância do andar (montagem.py): o motor anda isso por quadro e a
        # folha desconta isso, então na emenda do loop o corpo não volta para trás.
        nxt = picked[-1] + (picked[-1] - picked[-2] if len(picked) > 1 else 1)
        if nxt >= len(frames):
            raise SystemExit(f"--fixar volta: o vídeo acaba antes do quadro {nxt}")
        step = (torso_x(keyed_frame(nxt)) - xs[0]) / len(xs)
        t = np.arange(len(xs))
        sway = xs - xs[0] - step * t           # o vaivém do tronco em torno do avanço constante
        offsets = -step * t - (1 - args.balanco) * sway
    elif args.fixar == "quadro":
        offsets = -(xs - xs[0])
    elif args.fixar == "registro":
        xs = np.array([registration_x(k) for k in keyed])
        offsets = -(xs - xs[0])
    else:
        offsets = np.zeros(len(xs))
    # Chão e posição de referência vêm do 1º quadro do vídeo (a imagem inicial): poses no
    # ar ficam acima do chão e todos os clipes do personagem se alinham entre si.
    first = keyed_frame(0, args.sem_poeira)
    foot_y = int(np.nonzero((first[..., 3] > 128).any(1))[0].max())
    ref_x = torso_x(first) if args.fixar == "video" else xs[0]
    # Densidade d: a folha sai d vezes maior em px (célula, raiz, borda), mesma geometria lógica.
    d = args.densidade
    rx, ry = (int(v) for v in args.raiz.split(","))
    dx, dy = (int(v) for v in args.deslocar.split(","))
    rx, ry = (rx + dx) * d, (ry + dy) * d
    lcell, cols = args.celula, args.colunas
    lcw = args.largura or lcell
    cell, cw = lcell * d, lcw * d
    if d > 1 and cols * cw > MAX_SIDE:
        # Folha HD em mais linhas, para nenhum lado passar do limite de textura.
        cols = max(1, MAX_SIDE // cw)
        args.colunas = cols
    rows = (len(keyed) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cw, rows * cell), (0, 0, 0, 0))
    if args.reescala and len(args.reescala) != len(keyed):
        raise SystemExit(f"--reescala tem {len(args.reescala)} fatores para {len(keyed)} quadros")
    for n, (k, off) in enumerate(zip(keyed, offsets)):
        s = (scale / args.reescala[n] if args.reescala else scale) * d
        img = Image.fromarray(k, "RGBA")
        size = (round(img.width * s), round(img.height * s))
        img = img.resize(size, Image.LANCZOS)
        if args.nitidez and args.manter_efeitos:
            # A nitidez sangra a cor do corpo na borda meio transparente e tira o verde dela: o
            # halo do efeito ficaria marrom e laranja. Só o corpo (recortado sem o efeito) é
            # afiado, por cima do efeito.
            body = key(frames[picked[n]], args.sem_poeira, True, args.limpar_chao,
                       args.recorte_suave, args.objetos)
            if args.cor_efeito:   # a aura que o key() juntou ao corpo fica só na camada do efeito
                body[..., 3][effect_in_body(frames[picked[n]], body)] = 0
            img.alpha_composite(sharpen(Image.fromarray(body, "RGBA").resize(size, Image.LANCZOS), d))
        elif args.nitidez:
            img = sharpen(img, d)
        x = rx - round((ref_x - off) * s)
        if args.rampa:
            rdx, ate = (float(v) for v in str(args.rampa).split(","))
            x += round(rdx * min(1.0, n / max(1.0, ate)) * d)
        bottom = int(np.nonzero((k[..., 3] > 128).any(1))[0].max()) if args.pe_no_chao else foot_y
        y = ry - round(bottom * s)
        if args.cabeca_em is not None:
            top = int(np.nonzero((k[..., 3] > 128).any(1))[0].min())
            y = ry - round(top * s) - round(args.cabeca_em * d)
        # Cada quadro fica na própria célula: o que passa da borda é cortado, não invade a vizinha.
        box = Image.new("RGBA", (cw, cell), (0, 0, 0, 0))
        box.paste(img, (x, y))
        if args.aparar_borda:
            a = np.array(box)
            b = args.aparar_borda * d
            a[:b, :, 3] = a[:, :b, 3] = a[:, -b:, 3] = 0
            box = Image.fromarray(a, "RGBA")
        sheet.paste(box, ((n % cols) * cw, (n // cols) * cell))
    sheet.save(args.saida)
    print(f"{len(keyed)} quadros, {cols}×{rows}, {fps:.1f} fps de origem → {args.saida}")
    if clip is not None:
        save_recipe(args.clipe, clip, args, start, end, measured, len(keyed), lcw)
        pack_id = "player_base" if args.personagem == "p01" else args.personagem
        atlas = Path(clip["output"]).stem
        in_place = Path(args.saida).resolve() == (ROOT / clip["source"]).resolve()
        if args.ancorar_pe and in_place:
            import ancorar
            off = ancorar.anchor(pack_id, atlas)
            print("pé de apoio fixado: " + " ".join(f"{d:+d}" for d in off))
        if args.limpar_quadros and in_place:
            import limpar
            limpar.clean_atlas(pack_id, atlas, args.limpar_quadros, areas=args.limpar_area)
        if args.referencia and in_place and args.densidade > 1:
            import retoque_hd
            done = json.loads(Path(args.clipe).read_text(encoding="utf-8"))
            for line in retoque_hd.transfer(Path(args.saida), done, args.referencia, ROOT):
                print("retoque da referência: " + line)
        sheet = Image.open(args.saida)
        cw = json.loads(Path(args.clipe).read_text(encoding="utf-8")).get("frameWidth", cw)
    if args.previa:
        gif = []
        for n in range(len(keyed)):
            c = sheet.crop(((n % cols) * cw, (n // cols) * cell, (n % cols + 1) * cw, (n // cols + 1) * cell))
            bg = Image.new("RGBA", (cw, cell), (60, 60, 80, 255))
            bg.alpha_composite(c)
            gif.append(bg.convert("RGB").resize((cw * 2, cell * 2), Image.LANCZOS))
        gif[0].save(args.previa, save_all=True, append_images=gif[1:], duration=round(1000 * args.passo / fps), loop=0)


if __name__ == "__main__":
    main()
