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
    """GrabCut da camada ``cam`` dentro de ``livre`` (o que ainda não foi de outra camada)."""
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


def preenche(img, conhecido, faixa_lado=120, buraco_estreito=260, nevoa=None, embaixo_espelho=0):
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
    tex, longe, (cols, rows) = cc.chao(rgb, (escala, jx, jy), horizonte, base_y, args.perto,
                                       args.unidade, args.largura)
    Image.fromarray(tex).save(os.path.join(args.saida, spec.get('chao', 'chao.jpg')), quality=88, optimize=True)

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

    print(f'chao: perto {args.perto:.0f}, longe {longe:.0f}, ate {longe:.0f}, centroX {cc.REST_CENTER_X:.0f} ({cols}x{rows} texels)')
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
            nucleo = cv2.erode(mascara.astype(np.uint8), np.ones((5, 5), np.uint8)).astype(bool)
            nevoa = cam.get('nevoa')
            if nevoa:
                nevoa = np.array([int(nevoa[5:7], 16), int(nevoa[3:5], 16), int(nevoa[1:3], 16)], np.float32)
            cor = preenche(img, nucleo, nevoa=nevoa, embaixo_espelho=int(cam.get('espelhar_embaixo', 0)))
        cor = cor[:ate]
        a = cv2.GaussianBlur(alfa[:ate].astype(np.float32), (3, 3), 0)
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
        topo = (0 - jy) / escala / cc.VH
        base = (ate - jy) / escala / cc.VH
        x = ((w / 2) - jx) / escala / cc.VW
        print(f'camada {cam["id"]}: imagem {arquivo}, profundidade {prof}, topo {topo:.4f}, base {base:.4f}, x {x:.4f}')


if __name__ == '__main__':
    main()
