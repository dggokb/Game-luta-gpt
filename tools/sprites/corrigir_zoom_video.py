#!/usr/bin/env python3
"""Desfaz o zoom da câmera de um vídeo no verde, quadro a quadro, antes do efeito_video.py.

O vídeo gerado às vezes aproxima e afasta a câmera (a ativação do SHINKAI da Yin-lu: ela cresce ao
erguer os braços e fica com ~78% do tamanho enquanto a garça abre as asas). Com a escala medida de
alguns quadros (o tamanho dela em relação à guarda), cada quadro é reescalado por 1/escala em volta
de um ponto (os pés), numa tela maior, para o que cresce não sair do quadro. A beira do vídeo
original some aos poucos antes de reescalar (o mesmo que a --borda do efeito_video.py, que depois
não vê mais a beira de verdade), e abaixo do chão também.

    python3 tools/sprites/corrigir_zoom_video.py video.mp4 saida.mp4 \\
        --escala "1:1,5:1.05,6:1.2,9:1.2,12:0.95,20:0.78,104:0.78,117:1" \\
        --ancora 670,733 --folga 320,300,320,40 --borda 120 --chao-y 733

    --escala   quadro:escala (quadros do vídeo contados de 1), interpolado entre as marcas
    --ancora   o ponto que fica parado (os pés)
    --folga    esquerda,cima,direita,baixo: px de tela a mais (verde) em volta do vídeo

Sai um mp4 sem perda (libx264rgb, crf 0) que o efeito_video.py lê normalmente; a origem dele muda
pela folga (--origem x+esquerda,y+cima).
"""
import argparse
import subprocess
import tempfile
from pathlib import Path

import cv2
import numpy as np


def curva(texto, n):
    marcas = sorted((int(a), float(b)) for a, b in (p.split(':') for p in texto.split(',')))
    xs = [m[0] for m in marcas]
    ys = [m[1] for m in marcas]
    return np.interp(np.arange(1, n + 1), xs, ys)


def mascara(w, h, borda, chao_y):
    """Multiplicador de 1 (dentro) a 0 (na beira): superelipse de cantos redondos, sem apagar
    embaixo até o chão; abaixo do chão some até a beira."""
    m = np.ones((h, w), np.float32)
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    if borda:
        nx = np.abs(xs - (w - 1) / 2.0) / ((w - 1) / 2.0)
        ny = np.abs(ys - (h - 1) / 2.0) / ((h - 1) / 2.0)
        if chao_y is not None:
            ny = np.where(ys > (h - 1) / 2.0, 0.0, ny)
        r = (nx ** 4 + ny ** 4) ** 0.25
        dentro = 1.0 - 2.0 * borda / float(min(w, h))
        t = np.clip((1.0 - r) / max(1e-3, 1.0 - dentro), 0.0, 1.0)
        m *= t * t * (3.0 - 2.0 * t)
    if chao_y is not None and chao_y < h - 1:
        m *= np.clip((h - 1 - ys) / float(h - 1 - chao_y), 0.0, 1.0)
    return m


def main():
    p = argparse.ArgumentParser()
    p.add_argument('video')
    p.add_argument('saida')
    p.add_argument('--escala', required=True)
    p.add_argument('--ancora', required=True, type=lambda t: [float(v) for v in t.split(',')])
    p.add_argument('--folga', default='0,0,0,0', type=lambda t: [int(v) for v in t.split(',')])
    p.add_argument('--borda', type=int, default=0)
    p.add_argument('--chao-y', type=int)
    a = p.parse_args()

    cap = cv2.VideoCapture(a.video)
    fps = cap.get(cv2.CAP_PROP_FPS) or 24.0
    quadros = []
    while True:
        ok, q = cap.read()
        if not ok:
            break
        quadros.append(q)
    cap.release()
    h, w = quadros[0].shape[:2]
    # O verde do fundo: a mediana dos cantos do primeiro quadro.
    cantos = np.concatenate([quadros[0][:20, :20].reshape(-1, 3), quadros[0][:20, -20:].reshape(-1, 3)])
    fundo = np.median(cantos, axis=0)
    m = mascara(w, h, a.borda, a.chao_y)[..., None]
    esc = curva(a.escala, len(quadros))
    fe, fc, fd, fb = a.folga
    W, H = w + fe + fd, h + fc + fb
    ax, ay = a.ancora[0] + fe, a.ancora[1] + fc
    with tempfile.TemporaryDirectory() as tmp:
        for i, q in enumerate(quadros):
            q = (q.astype(np.float32) * m + fundo * (1.0 - m)).astype(np.uint8)
            tela = np.empty((H, W, 3), np.uint8)
            tela[:] = fundo.astype(np.uint8)
            tela[fc:fc + h, fe:fe + w] = q
            k = 1.0 / esc[i]
            M = np.float32([[k, 0, ax - k * ax], [0, k, ay - k * ay]])
            tela = cv2.warpAffine(tela, M, (W, H), flags=cv2.INTER_CUBIC if k > 1 else cv2.INTER_AREA,
                                  borderMode=cv2.BORDER_CONSTANT, borderValue=fundo.tolist())
            cv2.imwrite(str(Path(tmp) / f'q{i:04d}.png'), tela)
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-framerate', str(fps), '-i', str(Path(tmp) / 'q%04d.png'),
                        '-c:v', 'libx264rgb', '-crf', '0', a.saida], check=True)
    print(f'{len(quadros)} quadros, escala de {esc.min():.2f} a {esc.max():.2f}; tela {W}x{H}, '
          f'origem nova {ax:.0f},{ay:.0f}')


if __name__ == '__main__':
    main()
