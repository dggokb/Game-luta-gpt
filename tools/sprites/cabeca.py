#!/usr/bin/env python3
"""Régua da cabeça: o tamanho de cada quadro de um vídeo, contra a imagem inicial oficial.

    python3 tools/sprites/cabeca.py p02 dash.mp4 0,14,26,60     # tamanho desses quadros

A cabeça é a parte rígida do corpo e aparece em quase todo quadro. A da imagem inicial
(art/keys/<id>/inicio_centro.png, a mesma câmera 1280x720 dos vídeos) é procurada em cada
quadro do vídeo, em resolução cheia, em várias escalas e giros: o zoom achado é o tamanho
do quadro relativo à imagem inicial. A escala do quadro no jogo é a da imagem inicial no
tamanho.json dividida pelo zoom. Isso cobre os dois problemas dos vídeos do Seedance:
tamanho mudando dentro do vídeo (dash.mp4 da p02: correndo ~15% maior que em guarda) e
tamanho diferente entre vídeos (intro.mp4 em pé ~12% maior).

A medida erra uns 4% nos quadros com a cabeça virada (corrida, cabelo voando): quadro com
nota baixa não conta, e a curva é suavizada por trecho (suavizar()), nunca quadro a quadro.
"""
import json
import sys
from pathlib import Path

import cv2
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
import video_para_sprite as vps  # noqa: E402

HEAD = 0.20          # fração de cima da silhueta que é a cabeça (cabelo e rosto)
MIN_NOTA = 0.45      # abaixo disso a cabeça não foi achada (deitada, de costas)
ZOOMS = np.exp(np.linspace(np.log(0.45), np.log(2.6), 161))
GIROS = (-30, -20, -10, 0, 10, 20, 30)
RATE = 0.006         # a curva muda no máximo 0,6% por quadro
FLAT = 0.03          # trecho com menos variação que isso: um tamanho só


def gray(rgba):
    a = rgba[..., 3:4] / 255.0
    return cv2.cvtColor((rgba[..., :3] * a + 128 * (1 - a)).astype(np.uint8), cv2.COLOR_RGB2GRAY)


def head_crop(rgba):
    """Cabeça (cinza sobre cinza neutro) e a faixa da silhueta em que ela está."""
    m = rgba[..., 3] > 128
    ys = np.nonzero(m.any(1))[0]
    top, h = int(ys[0]), int((ys[-1] - ys[0]) * HEAD)
    row = np.nonzero(m[top + h // 2])[0]
    cx = int(row.mean())
    x0 = max(0, cx - int(h * 0.8))
    return gray(rgba)[top:top + h, x0:cx + int(h * 0.8)]


class Regua:
    def __init__(self, cid):
        keys = ROOT / 'art/keys' / cid
        self.key_scale = json.loads((keys / 'tamanho.json').read_text())['chaves']['inicio_centro']
        self.head = head_crop(vps.key(cv2.imread(str(keys / 'inicio_centro.png'))))

    def zoom(self, frame, guess=None):
        """(zoom, nota): tamanho do quadro relativo à imagem inicial."""
        k = vps.key(frame)
        m = k[..., 3] > 128
        if m.sum() < 2000:
            return None, 0.0
        ys = np.nonzero(m.any(1))[0]
        xs = np.nonzero(m.any(0))[0]
        g = gray(k)
        # Só a metade de cima da silhueta (a cabeça está lá, em pé ou agachada).
        y1 = int(ys[0] + (ys[-1] - ys[0]) * 0.6) + 40
        region = g[max(0, ys[0] - 40):y1, max(0, xs[0] - 40):xs[-1] + 40]
        zs = ZOOMS if guess is None else ZOOMS[np.abs(np.log(ZOOMS / guess)) < 0.18]
        best = (-1.0, None)
        for z in zs:
            t0 = cv2.resize(self.head, None, fx=z, fy=z, interpolation=cv2.INTER_AREA if z < 1 else cv2.INTER_CUBIC)
            if t0.shape[0] >= region.shape[0] or t0.shape[1] >= region.shape[1] or min(t0.shape) < 10:
                continue
            for rot in GIROS:
                M = cv2.getRotationMatrix2D((t0.shape[1] / 2, t0.shape[0] / 2), rot, 1)
                t = cv2.warpAffine(t0, M, (t0.shape[1], t0.shape[0]), borderValue=128)
                v = float(cv2.matchTemplate(region, t, cv2.TM_CCOEFF_NORMED).max())
                if v > best[0]:
                    best = (v, float(z))
        return best[1], best[0]

    def scales(self, frames):
        """Escala de jogo de cada quadro (px da célula por px do vídeo) e as medidas cruas."""
        raw, guess = [], None
        for f in frames:
            z, nota = self.zoom(f, guess)
            raw.append((z, nota))
            if z and nota >= MIN_NOTA:
                guess = z
        zooms = suavizar([z if z and n >= MIN_NOTA else None for z, n in raw],
                         [n for _, n in raw])
        return [self.key_scale / z for z in zooms], raw


STEP = 0.08         # diferença de tamanho entre trechos menor que isso é ruído da medida
MIN_TRECHO = 6      # quadros medidos de cada lado para um degrau contar
PASSAGEM = 0.03     # no degrau, quadro a mais que isso dos dois trechos é a passagem do vídeo


def degraus(values):
    """Tamanho por trecho: um valor só (a mediana) no clipe todo, com degrau só onde o vídeo
    muda de tamanho de verdade (trechos de MIN_TRECHO quadros medidos cada, STEP ou mais de
    diferença: a corrida do dash.mp4). Nunca curva: curva suavizada transforma o erro da
    medida em zoom lento. Os poucos quadros em que o vídeo passa de um tamanho ao outro seguem a
    medida. Quadro sem medida fica no trecho em que está."""
    n = len(values)
    if not any(v is not None for v in values):
        raise SystemExit('cabeça não achada em nenhum quadro')
    x = [None if v is None else float(np.log(v)) for v in values]
    step = float(np.log(1 + STEP))

    def level(lo, hi):
        return float(np.median([v for v in x[lo:hi] if v is not None]))

    def cut(lo, hi):
        """Melhor ponto de corte de [lo, hi): o de menor erro absoluto dos dois lados."""
        best = None
        for c in range(lo + 1, hi):
            a = [v for v in x[lo:c] if v is not None]
            b = [v for v in x[c:hi] if v is not None]
            if len(a) < MIN_TRECHO or len(b) < MIN_TRECHO:
                continue
            ma, mb = np.median(a), np.median(b)
            cost = sum(abs(v - ma) for v in a) + sum(abs(v - mb) for v in b)
            if best is None or cost < best[0]:
                best = (cost, c)
        return None if best is None else best[1]

    def split(lo, hi):
        c = cut(lo, hi)
        if c is None or abs(level(lo, c) - level(c, hi)) < step:
            return [(lo, hi)]
        return split(lo, c) + split(c, hi)

    parts = split(0, n)
    merged = True
    while merged:   # vizinhos que ficaram parecidos depois dos cortes viram um trecho só
        merged = False
        for k in range(len(parts) - 1):
            if abs(level(*parts[k]) - level(*parts[k + 1])) < step:
                parts[k:k + 2] = [(parts[k][0], parts[k + 1][1])]
                merged = True
                break
    out = []
    for lo, hi in parts:
        out += [level(lo, hi)] * (hi - lo)
    # O vídeo muda de tamanho em uns quadros (intro.mp4: 12% em 4): nesses, entre os dois
    # trechos e longe dos dois, vale a medida do quadro, senão o degrau dá um tranco.
    tol = float(np.log(1 + PASSAGEM))
    for (_, b), _ in zip(parts, parts[1:]):
        la, lb = out[b - 1], out[b]
        lo, hi = min(la, lb) + tol, max(la, lb) - tol
        for side in (range(b - 1, -1, -1), range(b, n)):
            for k in side:
                if x[k] is None or not lo < x[k] < hi:
                    break
                out[k] = x[k]
    return [float(np.exp(v)) for v in out]


def suavizar(values, weights=None):
    """Curva por trecho: mediana de 9, média de 15, no máximo RATE por quadro (ida e volta).
    Quadro sem medida pega o valor dos vizinhos; curva quase plana vira um valor só."""
    n = len(values)
    idx = np.arange(n)
    ok = np.array([v is not None for v in values])
    if not ok.any():
        raise SystemExit('cabeça não achada em nenhum quadro')
    raw = np.interp(idx, idx[ok], np.array([v for v in values if v is not None]))
    pad = lambda a, k: np.pad(a, k, mode='edge')
    med = np.array([np.median(pad(raw, 4)[i:i + 9]) for i in range(n)])
    out = np.convolve(pad(med, 7), np.ones(15) / 15, mode='valid')
    for _ in range(2):
        for i in range(1, n):
            out[i] = np.clip(out[i], out[i - 1] * (1 - RATE), out[i - 1] * (1 + RATE))
        for i in range(n - 2, -1, -1):
            out[i] = np.clip(out[i], out[i + 1] * (1 - RATE), out[i + 1] * (1 + RATE))
    if out.max() / out.min() - 1 < FLAT:
        out[:] = float(np.median(raw[ok]))
    return [float(v) for v in out]


def main():
    cid, video, quadros = sys.argv[1:4]
    cap = cv2.VideoCapture(video)
    frames = []
    while True:
        ok, f = cap.read()
        if not ok:
            break
        frames.append(f)
    r = Regua(cid)
    for i in [int(q) for q in quadros.split(',')]:
        z, nota = r.zoom(frames[i])
        print(f'q{i}: cabeça {z:.3f} x a da imagem inicial (nota {nota:.2f}) -> escala {r.key_scale / z:.4f}'
              if z else f'q{i}: cabeça não achada')


if __name__ == '__main__':
    main()
