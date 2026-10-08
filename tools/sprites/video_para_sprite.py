#!/usr/bin/env python3
"""Converte um vídeo de animação (fundo verde) em folha normalizada do pipeline.

O vídeo vem do Seedance/Veo com o personagem de lado, câmera parada e fundo verde
liso. Para cada quadro do trecho escolhido: recorta o verde (e a sombra/faixa de chão),
mantém só o personagem (maior peça conectada; poeira e efeitos soltos saem), corrige o
deslizamento lateral e escala tudo com a MESMA escala do personagem, para que todos os
clipes fiquem do mesmo tamanho: o 1º quadro do vídeo é sempre a guarda em pé (a imagem
inicial), e a altura dele vira --altura px. A raiz (meio dos pés) cai em rootX/rootY da célula.

    python3 tools/sprites/video_para_sprite.py video.mp4 saida.png --inicio 29 --fim 105
    python3 tools/sprites/video_para_sprite.py video.mp4 saida.png --loop 20-110

`--loop A-B` procura, dentro de A..B, o trecho que melhor fecha em loop.
"""
import argparse

import cv2
import numpy as np
from PIL import Image


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
    personagem desliza no vídeo."""
    small = []
    for f in frames:
        k = key(f)
        cx = int(torso_x(k))
        g = cv2.cvtColor(k[..., :3], cv2.COLOR_RGB2GRAY) * (k[..., 3] / 255)
        g = np.pad(g, ((0, 0), (400, 400)))[:, cx: cx + 800]
        small.append(cv2.resize(g, (100, 90)).astype(np.float32))
    best = None
    for a in range(lo, hi - min_len):
        for b in range(a + min_len, hi + 1):
            d = float(np.abs(small[a] - small[b]).mean())
            if best is None or d < best[0]:
                best = (d, a, b)
    return best


def key(frame, dust=False, effects=False):
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    h, s, v = (hsv[..., i].astype(int) for i in range(3))
    bg = (h > 35) & (h < 95) & (s > 45)
    if dust:  # poeira bege dos pés (o jogo desenha os efeitos)
        bg |= (h >= 8) & (h <= 34) & (s >= 18) & (s <= 70) & (v >= 120)
    if effects:
        # Clarão de impacto: amarelo claro (a pele é mais alaranjada e menos clara).
        bg |= (h >= 22) & (h <= 40) & (v >= 200) & (s >= 40)
        # Anel de energia ciano-petróleo (o azul da roupa é mais anil, matiz >= 104).
        bg |= (h >= 85) & (h <= 102) & (s >= 120)
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
        n, labels, stats, _ = cv2.connectedComponentsWithStats((alpha > 0).astype(np.uint8))
        if n > 1:
            keep = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
            alpha[labels != keep] = 0
    alpha = cv2.morphologyEx(alpha, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    n, labels, stats, _ = cv2.connectedComponentsWithStats(alpha)
    if n > 1:
        keep = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
        alpha = np.where(labels == keep, alpha, 0).astype(np.uint8)
    # Furinhos dentro do corpo (reflexo verde na roupa branca) voltam a ser opacos;
    # vãos grandes, como entre as pernas, continuam transparentes.
    holes = (alpha == 0).astype(np.uint8)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(holes)
    h_, w_ = alpha.shape
    for i in range(1, n):
        x, y, w, h, area = stats[i]
        if area < 600 and x > 0 and y > 0 and x + w < w_ and y + h < h_:
            alpha[labels == i] = 255
    alpha = cv2.GaussianBlur(alpha, (3, 3), 0)
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB).astype(int)
    rgb[..., 1] = np.minimum(rgb[..., 1], np.maximum(rgb[..., 0], rgb[..., 2]) + 8)  # tira o verde da borda
    return np.dstack([rgb.astype(np.uint8), alpha])


def torso_x(rgba):
    """Centro horizontal do tronco (faixa do ombro à cintura), estável durante a passada."""
    a = rgba[..., 3] > 128
    rows = np.nonzero(a.any(1))[0]
    top, bot = rows.min(), rows.max()
    band = a[top + (bot - top) * 15 // 100: top + (bot - top) * 45 // 100]
    return float(np.nonzero(band)[1].mean())


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("video")
    p.add_argument("saida")
    p.add_argument("--inicio", type=int)
    p.add_argument("--fim", type=int, help="quadro final, exclusivo")
    p.add_argument("--loop", help="A-B: escolhe o melhor loop dentro desse intervalo")
    p.add_argument("--min-loop", type=int, default=24)
    p.add_argument("--passo", type=int, default=1, help="usar 1 a cada N quadros")
    p.add_argument("--quadros", help="lista exata de quadros do vídeo, ex.: 10,14,18,22")
    p.add_argument("--escala", default="auto",
                   help="auto: o 1º quadro do vídeo (guarda em pé) vira --altura px; ou um número")
    p.add_argument("--altura", type=int, default=224, help="altura do personagem em pé na célula")
    p.add_argument("--celula", type=int, default=256, help="altura da célula")
    p.add_argument("--largura", type=int, help="largura da célula (padrão: igual à altura)")
    p.add_argument("--sem-poeira", action="store_true", help="remove poeira bege do chão")
    p.add_argument("--sem-efeitos", action="store_true", help="remove clarão de impacto e rastro de golpe")
    p.add_argument("--colunas", type=int, default=8)
    p.add_argument("--raiz", default="128,238")
    p.add_argument("--fixar", choices=["tronco", "quadro", "video"], default="tronco",
                   help="tronco: anula o deslizamento lateral; quadro: cada quadro no próprio tronco "
                        "(o jogo é que empurra, ex.: defesa); video: mantém a posição do vídeo")
    p.add_argument("--pe-no-chao", action="store_true",
                   help="cada quadro com os pés na raiz (poses no ar: o jogo é que sobe e desce)")
    p.add_argument("--deslocar", default="0,0",
                   help="DX,DY em px da célula aplicado a todos os quadros (acerto fino de registro)")
    p.add_argument("--previa", help="GIF de prévia no tamanho do jogo")
    args = p.parse_args()

    frames, fps = read_frames(args.video)
    if args.loop:
        lo, hi = (int(v) for v in args.loop.split("-"))
        d, start, end = best_loop(frames, lo, min(hi, len(frames) - 1), args.min_loop)
        print(f"loop {start}-{end} (diferença {d:.2f})")
    else:
        start, end = args.inicio or 0, args.fim or len(frames)
    picked = ([int(v) for v in args.quadros.split(",")] if args.quadros
              else list(range(start, end, args.passo)))
    keyed = [key(frames[i], args.sem_poeira, args.sem_efeitos) for i in picked]
    xs = np.array([torso_x(k) for k in keyed])
    if args.fixar == "tronco":
        # Remove só a tendência (o deslizamento); o balanço natural do corpo continua.
        t = np.arange(len(xs))
        trend = np.polyval(np.polyfit(t, xs, 1), t)
        offsets = -(trend - trend[0])
    elif args.fixar == "quadro":
        offsets = -(xs - xs[0])
    else:
        offsets = np.zeros(len(xs))
    # Chão e posição de referência vêm do 1º quadro do vídeo (guarda em pé): poses no ar
    # ficam acima do chão e todos os clipes do personagem se alinham entre si.
    first = key(frames[0], args.sem_poeira)
    foot_y = int(np.nonzero((first[..., 3] > 128).any(1))[0].max())
    ref_x = torso_x(first) if args.fixar == "video" else xs[0]
    rx, ry = (int(v) for v in args.raiz.split(","))
    dx, dy = (int(v) for v in args.deslocar.split(","))
    rx, ry = rx + dx, ry + dy
    if args.escala == "auto":
        rows0 = np.nonzero((key(frames[0])[..., 3] > 128).any(1))[0]
        s = args.altura / float(rows0.max() - rows0.min())
        print(f"escala automática {s:.4f} (personagem em pé com {rows0.max() - rows0.min()} px no vídeo)")
    else:
        s = float(args.escala)
    cell, cols = args.celula, args.colunas
    cw = args.largura or cell
    rows = (len(keyed) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cw, rows * cell), (0, 0, 0, 0))
    for n, (k, off) in enumerate(zip(keyed, offsets)):
        img = Image.fromarray(k, "RGBA")
        img = img.resize((round(img.width * s), round(img.height * s)), Image.LANCZOS)
        x = (n % cols) * cw + rx - round((ref_x - off) * s)
        bottom = int(np.nonzero((k[..., 3] > 128).any(1))[0].max()) if args.pe_no_chao else foot_y
        y = (n // cols) * cell + ry - round(bottom * s)
        sheet.alpha_composite(img, (x, y))
    sheet.save(args.saida)
    print(f"{len(keyed)} quadros, {cols}×{rows}, {fps:.1f} fps de origem → {args.saida}")
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
