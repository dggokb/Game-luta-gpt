#!/usr/bin/env python3
"""Corta um panorama pintado (o cenário inteiro, mais largo que a tela) em camadas com paralaxe.

O panorama é uma pintura só: céu, montanhas, estádio e chão. Para a câmera andar e cada coisa
se mexer na sua velocidade, ele vira:

* camadas (por exemplo céu, montanhas, estádio), cada uma uma imagem com
  transparência onde a da frente fica. O que a da frente tapa é preenchido (inpaint), para não
  abrir buraco quando a câmera anda e as camadas escorregam umas sobre as outras; a de mais trás
  é opaca e as do meio são cheias da borda de cima delas para baixo;
* o chão "desentortado" (a mesma conta do ``cenario_do_conceito.py``), agora com a pintura
  inteira: os lados do chão vêm do panorama em vez do espelho.

Quem separa as camadas é o GrabCut do OpenCV, a partir de retângulos de "semente" escritos à mão
num JSON (``recorte.json``, ao lado do panorama). Na ordem do JSON cada camada é separada do
que sobrou (a primeira pode ser o chão, com ``"descartar": true``); a última leva o resto. Depois
elas são empilhadas pela profundidade.

    pip install opencv-python numpy pillow
    python3 tools/stage/cenario_panorama.py art/stages/arena_central/recorte.json \\
        android/app/src/main/assets/stages/arena_central

O JSON (caminhos relativos a ele):

    { "panorama": "panorama.webp",
      "janela": { "escala": 0.903, "x": 409, "y": 54 },   // a tela de repouso dentro do panorama
      "juncao": 424,          // linha do panorama onde o chão encontra o fundo (no meio)
      "horizonte": 0.47,
      "camadas": [            // na ordem do recorte
        { "id": "chao", "descartar": true, "padrao": "nao", "sim": [[x0, y0, x1, y1], ...] },
        { "id": "ceu", "profundidade": 60000, "arquivo": "ceu.jpg", "ate": 360,
          "espelhar_embaixo": 140,   // o que fica atrás do resto: as nuvens espelhadas (sem névoa)
          "padrao": "nao",    // fora das sementes: provável que não seja dela ("sim" o contrário)
          "talvez_sim": [[...]], "talvez_nao": [[...]],   // prováveis
          "sim": [[...]], "nao": [[...]] },                 // certeza
        { "id": "montanhas", "profundidade": 5000, "nevoa": "#7E6280", ... },   // cor da névoa no pé
        { "id": "estadio", "arquivo": "estadio.png", "ate": 480 } ] }   // o resto

Outros campos: ``"juntar_com": "ceu"`` (a camada recortada à parte vai para a imagem de outra);
``"preencher": "paisagem"`` (o fundo de longe: pedaços do lado colados por Poisson atrás das
torres e um lago com névoa embaixo do pé das montanhas; ``"suave"``: só o degradê);
``"acima": 320`` e ``"corZenite"`` (mais céu em cima, espelhado e escurecendo, para a camada
com ``"apoio"`` no stage.json); no topo, ``"limpar_chao": true`` (o fundo do chão que caiu fora
do piso vira piso do lado) e ``"nitidez_longe": 1.2`` (realça o chão de longe).

``janela``: o pixel (sx, sy) da tela de repouso (1280x720) fica em (x + escala*sx, y + escala*sy)
no panorama. Dá para achar com o conceito 16:9 (``--referencia conceito.png``: SIFT + RANSAC).
Sem ``profundidade`` a camada fica no fim do chão. Imprime as camadas e o chão para o stage.json.
"""
import argparse
import json
import os
import sys

import cv2
import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cenario_do_conceito as cc  # noqa: E402


def janela_por_referencia(panorama, referencia):
    """Onde o conceito 16:9 (a tela de repouso) está dentro do panorama: escala e deslocamento."""
    c = cv2.resize(cv2.imread(referencia), (cc.VW, cc.VH), interpolation=cv2.INTER_AREA)
    sift = cv2.SIFT_create(4000)
    k1, d1 = sift.detectAndCompute(cv2.cvtColor(c, cv2.COLOR_BGR2GRAY), None)
    k2, d2 = sift.detectAndCompute(cv2.cvtColor(panorama, cv2.COLOR_BGR2GRAY), None)
    pares = [a for a, b in cv2.BFMatcher().knnMatch(d1, d2, k=2) if a.distance < 0.7 * b.distance]
    src = np.float32([k1[p.queryIdx].pt for p in pares])
    dst = np.float32([k2[p.trainIdx].pt for p in pares])
    a, _ = cv2.estimateAffinePartial2D(src, dst, ransacReprojThreshold=3)
    return float(np.hypot(a[0, 0], a[1, 0])), float(a[0, 2]), float(a[1, 2])


def separa(img, livre, cam):
    """GrabCut da camada ``cam`` dentro de ``livre`` (o que ainda não foi de outra camada).

    Num panorama grande o corte roda numa cópia de ~2000 px (as sementes foram acertadas nesse
    tamanho, e o GrabCut separa melhor sem o detalhe miúdo) e a máscara volta ampliada, com a
    borda suave."""
    H, W = img.shape[:2]
    f = max(1, round(W / 2000))
    if f > 1:
        peq = cv2.resize(img, (W // f, H // f), interpolation=cv2.INTER_AREA)
        livre_p = cv2.resize(livre.astype(np.uint8), (W // f, H // f), interpolation=cv2.INTER_NEAREST).astype(bool)
        cam_p = dict(cam)
        for chave in ('sim', 'nao', 'talvez_sim', 'talvez_nao'):
            cam_p[chave] = [[v // f for v in r] for r in cam.get(chave, [])]
        cam_p['provavel'] = cam.get('provavel', 0) // f
        m = separa_direto(peq, livre_p, cam_p)
        m = cv2.resize(m.astype(np.float32), (W, H), interpolation=cv2.INTER_LINEAR) > 0.5
        return m & livre
    return separa_direto(img, livre, cam)


def separa_direto(img, livre, cam):
    h, w = img.shape[:2]
    m = np.full((h, w), cv2.GC_PR_BGD if cam.get('padrao') == 'nao' else cv2.GC_PR_FGD, np.uint8)
    m[:int(cam.get('provavel', 0)), :] = cv2.GC_PR_BGD
    # Na sobreposição vale a última: o "nao" certo ganha do "sim".
    for chave, valor in (('talvez_nao', cv2.GC_PR_BGD), ('talvez_sim', cv2.GC_PR_FGD),
                         ('sim', cv2.GC_FGD), ('nao', cv2.GC_BGD)):
        for x0, y0, x1, y1 in cam.get(chave, []):
            m[y0:y1, x0:x1] = valor
    m[~livre] = cv2.GC_BGD
    bg = np.zeros((1, 65), np.float64)
    fg = np.zeros((1, 65), np.float64)
    cv2.grabCut(img, m, None, bg, fg, 6, cv2.GC_INIT_WITH_MASK)
    return ((m == cv2.GC_FGD) | (m == cv2.GC_PR_FGD)) & livre


def _vai_e_volta(n, k):
    """Índices 0..k-1 indo e voltando (0 1 .. k-1 k-1 .. 1 0 0 1 ..) para n posições."""
    i = np.arange(n) % (2 * k)
    return np.where(i < k, i, 2 * k - 1 - i)


def preenche(img, conhecido, faixa_lado=120, buraco_estreito=260, nevoa=None, embaixo_espelho=0, suave=False):
    """Inventa o que não é ``conhecido`` (o que a camada da frente tapa) espelhando o que está
    em volta, para manter a textura (nuvem, montanha) quando a câmera anda e destapa um pedaço:

    * buracos estreitos numa linha (torres, postes, o pavilhão): o lado esquerdo espelhado para
      dentro (ou o direito, na borda da imagem), indo e voltando numa faixa de ``faixa_lado`` px;
    * o resto (embaixo das arquibancadas): névoa, a cor de cima escorrendo e virando a cor
      média do pé da camada.
    """
    h, w = img.shape[:2]
    out = img.copy()
    feito = conhecido.copy()
    for y in range(h):
        linha = feito[y]
        if linha.all() or not linha.any():
            continue
        d = np.diff(np.concatenate(([1], linha.astype(np.int8), [1])))
        for x0, x1 in zip(np.where(d == -1)[0], np.where(d == 1)[0]):
            n = x1 - x0
            if n > buraco_estreito:
                continue
            if x0 > 0:
                k = min(faixa_lado, x0)
                fonte = x0 - 1 - _vai_e_volta(n, k)
            elif x1 < w:
                k = min(faixa_lado, w - x1)
                fonte = x1 + _vai_e_volta(n, k)[::-1]
            else:
                continue
            # Só copia do que é mesmo desta camada (ao lado pode haver outro buraco, ou a borda de
            # uma bandeira): senão o buraco fica para o preenchimento de cima para baixo.
            if not feito[y, fonte].all():
                continue
            out[y, x0:x1] = out[y, fonte]
            feito[y, x0:x1] = True
    if suave:
        # O resto (atrás das arquibancadas, embaixo do céu): o degradê suave.
        return preenche_suave(out, feito)
    # Embaixo (o que fica atrás das arquibancadas): a última cor de cada coluna escorrendo para
    # baixo e virando névoa (a cor média do pé da camada) em ~30 px, borrada de lado para não
    # riscar. Como o pé das montanhas na neblina, e o céu clareando no horizonte.
    embaixo = np.zeros((h, w), bool)
    ultimas = []
    for x in range(w):
        col = feito[:, x]
        if col.all() or not col.any():
            continue
        d = np.diff(np.concatenate(([1], col.astype(np.int8), [1])))
        for y0, y1 in zip(np.where(d == -1)[0], np.where(d == 1)[0]):
            if y0 == 0:
                continue  # nada em cima: fica para o inpaint
            if embaixo_espelho:
                # O céu: as nuvens de cima espelhadas para baixo (indo e voltando numa faixa).
                k = min(embaixo_espelho, y0)
                out[y0:y1, x] = out[y0 - 1 - _vai_e_volta(y1 - y0, k), x]
                feito[y0:y1, x] = True
                continue
            out[y0:y1, x] = out[y0 - 1, x]
            ultimas.append(out[y0 - 1, x].astype(np.float32))
            embaixo[y0:y1, x] = True
            feito[y0:y1, x] = True
    if ultimas:
        if nevoa is None:
            nevoa = np.median(np.array(ultimas), axis=0)
        # Distância (linhas) desde o começo do buraco em cada coluna.
        prof = np.zeros((h, w), np.float32)
        for y in range(1, h):
            prof[y] = np.where(embaixo[y], prof[y - 1] + 1, 0)
        t = np.clip(prof / 30.0, 0, 1)[..., None]
        borrado = cv2.blur(out, (41, 3))
        mistura = borrado.astype(np.float32) * (1 - t) + nevoa[None, None, :] * t
        out[embaixo] = np.clip(mistura[embaixo], 0, 255).astype(np.uint8)
    if not feito.all():
        out = cv2.inpaint(out, (~feito).astype(np.uint8) * 255, 5, cv2.INPAINT_TELEA)
    out[conhecido] = img[conhecido]
    return out


def preenche_suave(img, conhecido, reducao=16):
    """Inventa o que não é ``conhecido`` como uma continuação suave das cores em volta (o céu
    do pôr do sol escorrendo para baixo, sem textura): o inpaint numa cópia bem pequena,
    ampliada e desfocada. Atrás do estádio, quando a câmera anda ou sobe no super pulo, aparece
    um degradê do céu e não pedaços espelhados (que viravam blocos)."""
    h, w = img.shape[:2]
    pw, ph = max(8, w // reducao), max(8, h // reducao)
    peq = cv2.resize(img, (pw, ph), interpolation=cv2.INTER_AREA)
    # Pequeno: só o que é bem conhecido conta (a borda mistura o que estava na frente).
    k = cv2.resize(conhecido.astype(np.float32), (pw, ph), interpolation=cv2.INTER_AREA) > 0.9
    cheio = cv2.inpaint(peq, (~k).astype(np.uint8) * 255, 6, cv2.INPAINT_TELEA)
    cheio = cv2.GaussianBlur(cheio, (0, 0), 1.5)
    grande = cv2.resize(cheio, (w, h), interpolation=cv2.INTER_CUBIC)
    # Junta com o original numa borda macia.
    peso = cv2.GaussianBlur(conhecido.astype(np.float32), (0, 0), max(2.0, w / 1000.0))[..., None]
    peso = np.where(conhecido[..., None], 1.0, np.minimum(peso, 0.5))
    out = img.astype(np.float32) * peso + grande.astype(np.float32) * (1 - peso)
    return np.clip(out, 0, 255).astype(np.uint8)


def _fecha_vales(linha, k):
    """Fechamento 1D (máximo, depois mínimo, numa janela de ``k``): some com os vales estreitos."""
    k = max(1, int(k)) | 1
    v = linha.astype(np.float32)[None, :]
    el = np.ones((1, k), np.uint8)
    return cv2.erode(cv2.dilate(v, el), el)[0]


def pe_da_paisagem(conhecido, torre, suave):
    """A linha de baixo da paisagem em cada coluna, por baixo dela só o lago: a última linha
    conhecida, sem os vales estreitos (até ``torre`` px) que as torres e os postes abrem nela
    (atrás de uma torre a montanha continua), e sem degrau onde um buraco largo (o pavilhão) a
    corta: ela desce macia (``suave`` px) sem nunca passar da linha de verdade."""
    h, w = conhecido.shape
    tem = conhecido.any(0)
    xs = np.arange(w)
    if not tem.any():
        return np.zeros(w, np.float32)
    ultimo = (h - 1 - np.argmax(conhecido[::-1], 0)).astype(np.float32)
    pe = np.interp(xs, xs[tem], ultimo[tem]).astype(np.float32)
    pe = _fecha_vales(pe, torre)
    k = max(1, int(suave)) | 1
    baixo = cv2.erode(pe[None, :], np.ones((1, k), np.uint8), borderType=cv2.BORDER_REPLICATE)
    baixo = cv2.GaussianBlur(baixo, (0, 0), sigmaX=suave / 3.0, borderType=cv2.BORDER_REPLICATE)[0]
    return np.minimum(pe, baixo)


def remenda_com_vizinho(out, feito, buraco, largura_max, borda, faixa=64, base=None, minimo=0.85):
    """Cada buraco (componente de ``buraco``) até ``largura_max`` de largura vira pedaços do
    lado, em faixas de ``faixa`` linhas de cima para baixo: cada faixa deslocada na horizontal
    toda de uma vez (a nuvem não quebra em degraus de linha em linha) e colada por Poisson
    (``seamlessClone``) no que já está feito (sem costura de cor). A faixa que não acha um
    vizinho conhecido (``minimo`` da faixa) fica para depois; no vizinho, o que não é conhecido
    vem de ``base`` (o degradê suave), que só dá o tom. Muda ``out`` e ``feito``."""
    h, w = feito.shape
    n, rot, caixas, _ = cv2.connectedComponentsWithStats(buraco.astype(np.uint8), connectivity=8)
    pad = borda + 4
    el = np.ones((2 * borda + 1, 2 * borda + 1), np.uint8)
    for i in range(1, n):
        x, y, bw, bh, _ = caixas[i]
        if bw > largura_max:
            continue
        for fy in range(y, y + bh, faixa):
            comp = (rot[fy:fy + faixa] == i)
            if not comp.any():
                continue
            comp_t = np.zeros((h, w), bool)
            comp_t[fy:fy + faixa] = comp
            m = cv2.dilate(comp_t.astype(np.uint8), el).astype(bool)
            ys, xs = np.nonzero(m)
            y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
            largura = x1 - x0
            for k in range(1, 7):
                feitos = False
                for sinal in (-1, 1):
                    dx = sinal * k * (largura + max(8, largura // 6))
                    if x0 + dx < 0 or x1 + dx > w:
                        continue
                    conhecidos = feito[ys, xs + dx]
                    if conhecidos.mean() < minimo or (base is None and not conhecidos.all()):
                        continue
                    # A cena com uma margem espelhada: o Poisson não aceita máscara na borda.
                    dst = cv2.copyMakeBorder(out[max(0, y0 - pad):y1 + pad], pad, pad, pad, pad, cv2.BORDER_REFLECT)
                    oy = pad + (y0 - max(0, y0 - pad))
                    src = out[y0:y1, x0 + dx:x1 + dx]
                    if base is not None:
                        src = np.where(feito[y0:y1, x0 + dx:x1 + dx, None], src, base[y0:y1, x0 + dx:x1 + dx])
                    mk = m[y0:y1, x0:x1].astype(np.uint8) * 255
                    mk[0, :] = mk[-1, :] = 0
                    mk[:, 0] = mk[:, -1] = 0
                    centro = (pad + x0 + largura // 2, oy + (y1 - y0) // 2)
                    colado = cv2.seamlessClone(src, dst, mk, centro, cv2.NORMAL_CLONE)
                    colado = colado[oy:oy + (y1 - y0), pad:pad + w]
                    mm = m[y0:y1]
                    out[y0:y1][mm] = colado[mm]
                    feito |= comp_t
                    feitos = True
                    break
                if feitos:
                    break


def preenche_paisagem(img, conhecido, buraco_estreito, px=1.0, nevoa=None):
    """O fundo de longe (céu e montanhas numa imagem só) inventado sem bloco nem faixa lisa:

    * em cima do pé da paisagem (o céu atrás das torres, dos postes, do pavilhão): um pedaço
      vizinho colado por Poisson (:func:`remenda_com_vizinho`); o que não achar vizinho, o
      degradê suave;
    * embaixo do pé (o que aparece atrás das arquibancadas quando a câmera sobe no super pulo):
      um lago com névoa, o pé das montanhas refletido, borrado de lado e tingido, sumindo na
      névoa em ~80 px. Antes era um degradê liso, que no jogo virava uma faixa roxa chapada.
    """
    h, w = img.shape[:2]
    out = img.copy()
    feito = conhecido.copy()
    pe = pe_da_paisagem(conhecido, round(400 * px), round(40 * px))
    linhas = np.arange(h, dtype=np.float32)[:, None]
    acima = linhas < pe[None, :]
    base = preenche_suave(img, conhecido)
    remenda_com_vizinho(out, feito, acima & ~feito, 2.5 * buraco_estreito, max(3, round(6 * px)), round(48 * px),
                        base=base, minimo=0.6)
    if not feito[acima].all():
        sobra = feito | ~acima
        suave = preenche_suave(out, sobra)
        out[~sobra] = suave[~sobra]
        feito |= acima
    # A névoa: a cor do pé da paisagem (as montanhas já somem na bruma do pôr do sol).
    if nevoa is None:
        xs = np.arange(w)
        ys = np.clip(pe.astype(int) - round(4 * px), 0, h - 1)
        nevoa = np.median(out[ys, xs].astype(np.float32), axis=0)
        nevoa = nevoa * 0.75 + np.array([150, 110, 120], np.float32) * 0.25   # BGR: lilás claro
    nevoa = np.asarray(nevoa, np.float32)
    # O reflexo: a linha y embaixo do pé vem de pe - 1 - 0.85 (y - pe), com uma ondinha de lado.
    d = np.maximum(0.0, linhas - pe[None, :])
    mapa_y = np.clip(pe[None, :] - 1 - 0.85 * d, 0, h - 1).astype(np.float32)
    mapa_x = (np.arange(w, dtype=np.float32)[None, :] + 1.6 * px * np.sin(linhas * (0.9 / px))).astype(np.float32)
    mapa_x = np.broadcast_to(mapa_x, (h, w)).copy()
    reflexo = cv2.remap(out, mapa_x, mapa_y, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REFLECT)
    reflexo = cv2.GaussianBlur(reflexo, (0, 0), sigmaX=6 * px, sigmaY=1.2 * px).astype(np.float32)
    reflexo = reflexo * 0.55 + nevoa * 0.45
    t = np.clip(d / (80.0 * px), 0, 1)[..., None] ** 0.8
    lago = reflexo * (1 - t) + nevoa * t
    # Uma linha de bruma mais clara na água logo embaixo do pé (a margem).
    margem = np.exp(-((d - 3 * px) / (5 * px)) ** 2)[..., None]
    lago = lago * (1 - 0.35 * margem) + (nevoa * 1.12) * 0.35 * margem
    abaixo = ~acima & ~conhecido
    out[abaixo] = np.clip(lago[abaixo], 0, 255).astype(np.uint8)
    return out


def limpa_chao(tex, piso):
    """Troca na textura do chão o que não é ``piso`` por pedaços de piso do lado (as fileiras
    do chão desentortado são parecidas na horizontal), colados por Poisson."""
    out = cv2.cvtColor(tex, cv2.COLOR_RGB2BGR)
    nucleo = cv2.erode(piso.astype(np.uint8), np.ones((5, 5), np.uint8)).astype(bool)
    base = preenche_suave(out, nucleo)
    feito = nucleo.copy()
    remenda_com_vizinho(out, feito, ~nucleo, out.shape[1], 4, 40, base=base, minimo=0.9)
    if not feito.all():
        out[~feito] = base[~feito]
    return cv2.cvtColor(out, cv2.COLOR_BGR2RGB)


def nitido_longe(tex, forca, ate=0.35):
    """Realça o chão de longe (as linhas de cima da textura, até ``ate`` da altura): na pintura
    o fundo da arena é macio e espremido em poucas linhas; no super pulo a câmera sobe, ele
    cresce na tela e virava um borrão. Máscara de nitidez, ``forca`` no topo indo a zero."""
    h = tex.shape[0]
    f = tex.astype(np.float32)
    borrado = cv2.GaussianBlur(f, (0, 0), 2.2)
    k = (forca * np.clip(1 - np.arange(h, dtype=np.float32) / (ate * h), 0, 1))[:, None, None]
    return np.clip(f + k * (f - borrado), 0, 255).astype(np.uint8)


def estende_ceu(img, linhas, zenite):
    """Mais ``linhas`` de céu em cima da imagem: as de cima espelhadas para o alto, cada vez
    mais borradas e escurecendo para a cor do zênite (``#RRGGBB``). Com a camada apoiada no
    estádio (``"apoio"``), no super pulo o céu desce junto e o que aparece em cima é isto, não
    uma faixa de cor chapada."""
    h, w = img.shape[:2]
    n = min(linhas, h)
    espelho = img[:n][::-1].astype(np.float32)
    if n < linhas:
        espelho = np.vstack([np.repeat(espelho[:1], linhas - n, axis=0), espelho])
    borrado = cv2.GaussianBlur(espelho, (0, 0), sigmaX=max(2.0, linhas / 30.0))
    # t: 0 na emenda (embaixo) e 1 no alto.
    t = (1.0 - (np.arange(linhas, dtype=np.float32) + 0.5) / linhas)[:, None, None]
    z = np.array([int(zenite[5:7], 16), int(zenite[3:5], 16), int(zenite[1:3], 16)], np.float32)
    ceu = espelho * (1 - np.clip(t * 2.5, 0, 1)) + borrado * np.clip(t * 2.5, 0, 1)
    ceu = ceu * (1 - 0.75 * t ** 1.3) + z * 0.75 * t ** 1.3
    return np.vstack([np.clip(ceu, 0, 255).astype(np.uint8), img])


def cheio_para_baixo(mascara):
    """Cada coluna cheia da primeira linha da camada até embaixo; colunas sem ela pegam a borda
    das vizinhas (atrás de uma torre a montanha continua)."""
    h, w = mascara.shape
    topo = np.where(mascara.any(0), mascara.argmax(0), -1).astype(float)
    tem = topo >= 0
    if not tem.any():
        return mascara
    xs = np.arange(w)
    cheio = np.interp(xs, xs[tem], topo[tem])
    # Só entre colunas que têm a camada: para fora da primeira e da última, nada.
    fora = (xs < xs[tem][0]) | (xs > xs[tem][-1])
    return (np.arange(h)[:, None] >= cheio[None, :]) & ~fora[None, :]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('recorte', help='o JSON com o panorama e as sementes')
    ap.add_argument('saida', help='a pasta do cenário')
    ap.add_argument('--referencia', help='o conceito 16:9 para achar a janela (troca a do JSON)')
    ap.add_argument('--perto', type=float, default=-700.0)
    ap.add_argument('--unidade', type=float, default=2.0)
    ap.add_argument('--largura', type=float, default=6000.0)
    ap.add_argument('--mascaras', help='grava as máscaras de cada camada nesta pasta (para conferir)')
    ap.add_argument('--linhas', type=int, help='chão em perspectiva: altura da imagem (linhas iguais na tela)')
    ap.add_argument('--colunas', type=int, help='chão em perspectiva: largura da imagem (cobre --largura do mundo)')
    args = ap.parse_args()

    pasta = os.path.dirname(os.path.abspath(args.recorte))
    spec = json.load(open(args.recorte, encoding='utf-8'))
    img = cv2.imread(os.path.join(pasta, spec['panorama']))
    h, w = img.shape[:2]
    if args.referencia:
        escala, jx, jy = janela_por_referencia(img, args.referencia)
        print(f'janela: escala {escala:.4f}, x {jx:.1f}, y {jy:.1f}')
    else:
        j = spec['janela']
        escala, jx, jy = j['escala'], j['x'], j['y']
    horizonte = spec.get('horizonte', 0.47)
    juncao = spec['juncao']
    base_y = (juncao - jy) / escala           # a linha da tela de repouso onde o chão acaba

    # O chão.
    rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB).astype(np.float32)
    tex, longe, (cols, rows), dist = cc.chao(rgb, (escala, jx, jy), horizonte, base_y, args.perto,
                                             args.unidade, args.largura, args.linhas, args.colunas)

    # O recorte, na ordem do JSON (cada uma tira a sua parte do que sobrou; a última leva o
    # resto acima do "ate" dela). "descartar": a parte sai, mas não vira camada (o chão).
    livre = np.ones((h, w), bool)
    feitas = []
    camadas = spec['camadas']
    for i, cam in enumerate(camadas):
        if i == len(camadas) - 1:
            mascara = livre & (np.arange(h)[:, None] < int(cam.get('ate', juncao)))
        else:
            mascara = separa(img, livre, cam)
        # Tira os pixels soltos e fecha furinhos.
        mascara = cv2.morphologyEx(mascara.astype(np.uint8), cv2.MORPH_OPEN, np.ones((3, 3), np.uint8)).astype(bool)
        mascara &= livre
        livre &= ~mascara
        if args.mascaras:
            os.makedirs(args.mascaras, exist_ok=True)
            cv2.imwrite(os.path.join(args.mascaras, cam['id'] + '.png'), mascara.astype(np.uint8) * 255)
        if not cam.get('descartar'):
            feitas.append((cam, mascara))
        elif cam['id'] == 'chao' and spec.get('limpar_chao'):
            # O que no chão desentortado veio de fora do piso (escadas, pé das paredes dos
            # lados): no super pulo a câmera sobe e isso aparecia como degraus borrados.
            m3 = np.repeat(mascara[..., None].astype(np.float32) * 255, 3, axis=2)
            mtex, *_, fora = cc.chao(m3, (escala, jx, jy), horizonte, base_y, args.perto, args.unidade,
                                     args.largura, args.linhas, args.colunas, com_fora=True,
                                     margem=float(spec.get('margem_chao', 0)))
            # Só no quinto de longe do chão: perto, o espelho da pintura vai bem (é o
            # piso com as faixas), longe ele espreme a parede dos lados em riscos.
            longe_ = np.arange(mtex.shape[0])[:, None] < 0.2 * mtex.shape[0]
            mtex = (mtex[..., 0] > 160) & ~(fora & longe_) | ~longe_
            tex = limpa_chao(tex, mtex)
    if spec.get('nitidez_longe'):
        tex = nitido_longe(tex, float(spec['nitidez_longe']))
    Image.fromarray(tex).save(os.path.join(args.saida, spec.get('chao', 'chao.jpg')), quality=88, optimize=True)

    # "juntar_com": a camada é recortada à parte (as sementes ficam mais simples), mas vai para
    # a imagem de outra (as montanhas no céu: uma camada só, sem pedaço solto entre as duas).
    for cam, mascara in list(feitas):
        alvo = cam.get('juntar_com')
        if alvo:
            for i, (outra, m) in enumerate(feitas):
                if outra['id'] == alvo:
                    feitas[i] = (outra, m | mascara)
            feitas.remove((cam, mascara))

    extra = f', largura {args.largura:.0f}, linhasPerspectiva {dist:.2f}' if args.linhas else ''
    print(f'chao: perto {args.perto:.0f}, longe {longe:.0f}, ate {longe:.0f}, centroX {cc.REST_CENTER_X:.0f}{extra} ({cols}x{rows} texels)')
    # Da frente para trás (sem "profundidade" = no fim do chão): a da frente só com o que é dela,
    # as do meio cheias da borda de cima para baixo, a de mais trás opaca; o que a camada não
    # tem (tapado pelas da frente) é inventado a partir das bordas dela.
    feitas.sort(key=lambda cm: cm[0].get('profundidade', longe))
    for i, (cam, mascara) in enumerate(feitas):
        ultima = i == len(feitas) - 1
        ate = int(cam.get('ate', juncao))
        if ultima:
            alfa = np.ones((h, w), bool)
        elif i == 0:
            alfa = mascara
        else:
            alfa = cheio_para_baixo(mascara)
        if i == 0:
            cor = img.copy()
        else:
            # A borda de 2 px da camada ainda tem um pouco do que estava na frente (meio pixel de
            # torre, de bandeira): não serve de fonte para o espelho.
            e = max(5, round(5 * w / 1983.0)) | 1
            nucleo = cv2.erode(mascara.astype(np.uint8), np.ones((e, e), np.uint8)).astype(bool)
            nevoa = cam.get('nevoa')
            if nevoa:
                nevoa = np.array([int(nevoa[5:7], 16), int(nevoa[3:5], 16), int(nevoa[1:3], 16)], np.float32)
            px = w / 1983.0   # os tamanhos do preenchimento foram acertados num panorama de 1983 px
            if cam.get('preencher') == 'paisagem':
                cor = preenche_paisagem(img, nucleo, round(260 * px), px)
            elif cam.get('preencher') == 'suave':
                cor = preenche(img, nucleo, faixa_lado=round(120 * px), buraco_estreito=round(260 * px), suave=True)
            else:
                cor = preenche(img, nucleo, faixa_lado=round(120 * px), buraco_estreito=round(260 * px), nevoa=nevoa,
                               embaixo_espelho=int(cam.get('espelhar_embaixo', 0)))
        cor = cor[:ate]
        a = cv2.GaussianBlur(alfa[:ate].astype(np.float32), (3, 3), 0)
        acima = int(cam.get('acima', 0))
        if acima:
            cor = estende_ceu(cor, acima, cam.get('corZenite', '#4E3070'))
            a = np.vstack([np.ones((acima, w), np.float32), a])
        arquivo = cam.get('arquivo', cam['id'] + '.png')
        destino = os.path.join(args.saida, arquivo)
        rgb_cam = cv2.cvtColor(cor, cv2.COLOR_BGR2RGB)
        if ultima or arquivo.endswith('.jpg'):
            Image.fromarray(rgb_cam).save(destino, quality=90, optimize=True)
        else:
            rgba = np.dstack([rgb_cam, np.clip(a * 255, 0, 255).astype(np.uint8)])
            if arquivo.endswith('.webp'):
                Image.fromarray(rgba).save(destino, quality=90, alpha_quality=100, method=6)
            else:
                Image.fromarray(rgba).save(destino, optimize=True)
        prof = cam.get('profundidade', round(longe))
        topo = (0 - acima - jy) / escala / cc.VH
        base = (ate - jy) / escala / cc.VH
        x = ((w / 2) - jx) / escala / cc.VW
        print(f'camada {cam["id"]}: imagem {arquivo}, profundidade {prof}, topo {topo:.4f}, base {base:.4f}, x {x:.4f}')


if __name__ == '__main__':
    main()
