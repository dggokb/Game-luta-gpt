#!/usr/bin/env python3
"""Vídeo de fundo de cena (não verde) para verde puro: o Ultra do p14.

    python3 tools/sprites/ultra_fundo.py ultra.mp4 saida.mp4 40 346

Tira o fundo pela diferença com a placa (mediana dos quadros 42–200, em que ele anda pela tela)
e pelo verde que o fundo tem e a roupa preta não, fecha os buracos pequenos da roupa, apaga a
sombra e a luz do chão, desfaz a câmera que sobe no pulo (quadros 95–125, medida nas bordas) e
recompõe sobre verde puro (cor sem a mistura do fundo), para o ultras.py tratar como os outros.
"""
import sys, subprocess, numpy as np, cv2
src, out = sys.argv[1], sys.argv[2]
raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', src, '-f', 'rawvideo', '-pix_fmt', 'bgr24', '-'],
                     capture_output=True, check=True).stdout
a = np.frombuffer(raw, np.uint8).reshape(-1, 768, 1344, 3)
n = len(a)
plate = np.median(a[list(range(42, 200, 3))], axis=0).astype(np.float32)
pl_l = plate.mean(2)
gex_p = plate[..., 1] - np.maximum(plate[..., 0], plate[..., 2])
GREEN = np.array([0, 255, 0], np.float32)
p = subprocess.Popen(['ffmpeg', '-v', 'error', '-y', '-f', 'rawvideo', '-pix_fmt', 'bgr24', '-s', '1344x768', '-r', '24',
                      '-i', '-', '-c:v', 'libx264rgb', '-crf', '0', '-preset', 'veryfast', out], stdin=subprocess.PIPE)
first, last = int(sys.argv[3]), int(sys.argv[4])
prev = None
def camera_dy(f):
    """Quanto a câmera do vídeo desceu o fundo (o pulo: ela sobe junto), pelas bordas da tela."""
    best = []
    for x0, x1 in ((0, 120), (1224, 1344)):
        errs = [(np.abs(f[100:700, x0:x1] - np.roll(plate, dy, axis=0)[100:700, x0:x1]).mean(), dy)
                for dy in range(-90, 91, 3)]
        best.append(min(errs))
    err, dy = min(best)
    return dy if err < 4 else 0


shifts = []
for i in range(first, last):
    f = a[i].astype(np.float32)
    dy = camera_dy(f) if 95 <= i <= 125 else 0
    shifts.append(dy)
    if dy:
        # Desfaz o movimento da câmera: o fundo volta para a placa e o corpo para a altura certa.
        g = np.roll(f, -dy, axis=0)
        if dy > 0:
            g[-dy:] = plate[-dy:]
        else:
            g[:-dy] = plate[:-dy]
        f = g
    d = np.abs(f - plate).sum(2)
    alpha = np.clip((d - 28) / 42, 0, 1)
    # Onde o fundo é verde (o brilho do meio), o que perde o verde é o personagem (roupa preta).
    gex = f[..., 1] - np.maximum(f[..., 0], f[..., 2])
    ag = np.clip((gex_p - gex - 5) / np.maximum(0.6 * gex_p, 7), 0, 1) * (gex_p > 3)
    alpha = np.maximum(alpha, ag)
    m = ((d > 45) | (ag > 0.6)).astype(np.uint8)
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))
    # Buracos pequenos (roupa preta parecida com o fundo escuro) viram corpo; vãos grandes (entre os
    # anéis do giro) continuam fundo.
    inv = (1 - m).astype(np.uint8)
    k, lab, st, _ = cv2.connectedComponentsWithStats(inv, connectivity=4)
    fill = np.zeros_like(m)
    for j in range(1, k):
        x, y, w, h, area = st[j]
        if area < 6000 and x > 0 and y > 0 and x + w < 1344 and y + h < 768:
            fill[lab == j] = 1
    solid = cv2.erode(np.maximum(m, fill), np.ones((5, 5), np.uint8))
    # Fundo é verde escuro: pixel com o verde dominando e perto da placa é fundo.
    gdom = (f[..., 1] > f[..., 2] + 12) & (f[..., 1] > f[..., 0] + 4) & (d < 90)
    solid[gdom] = 0
    alpha = np.maximum(alpha, solid.astype(np.float32))
    alpha[gdom & (fill == 0)] = 0
    # Chão: a faixa de luz do chão muda; abaixo do pé só fica o que é claro (efeito) ou vermelho.
    low = np.arange(768)[:, None] > 712
    alpha[low & (f.max(2) < 120)] = 0
    # Sombra no chão: mais escura que a placa, rente ao chão.
    shadow = (f.mean(2) < pl_l - 4) & (np.arange(768)[:, None] > 690)
    alpha[shadow & (f[..., 2] < 90)] = 0
    # Sujeira solta: o que não está perto de uma peça grande (corpo, efeito) e as faixas finas
    # e compridas (o fundo que mexe no pulo).
    mm = (alpha > 0.3).astype(np.uint8)
    k, lab, st, _ = cv2.connectedComponentsWithStats(mm, connectivity=8)
    big = np.zeros_like(mm)
    for j in range(1, k):
        x, y, w, h, area = st[j]
        if area >= 2500 and not (h < 30 and w > 150):
            big[lab == j] = 1
    near = cv2.dilate(big, np.ones((41, 41), np.uint8))
    for j in range(1, k):
        x, y, w, h, area = st[j]
        if (h < 30 and w > 150) or (area < 2500 and not near[lab == j].any()):
            alpha[lab == j] = 0
    alpha[(alpha <= 0.3) & (near == 0)] = 0
    a3 = alpha[..., None]
    fg = np.where(a3 > 0.05, (f - (1 - a3) * plate) / np.maximum(a3, 0.05), 0)
    fg = np.clip(fg, 0, 255)
    o = fg * a3 + GREEN * (1 - a3)
    p.stdin.write(o.astype(np.uint8).tobytes())
p.stdin.close(); p.wait()
print('câmera', [(first + i, v) for i, v in enumerate(shifts) if v])
