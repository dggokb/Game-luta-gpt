#!/usr/bin/env python3
"""O impacto de ar do acerto, desenhado em código (docs/revisao-base/direcao.md, regras E1–E7).

Golpe normal é pancada: ninguém solta poder, então o acerto não tem faísca, fogo nem energia.
O que se vê é o ar deslocado, na mesma família dos estouros de ar da defesa (defesa,
defesa_2, defesa_3: brancos, translúcidos, de pontas macias, tirados dos vídeos de defesa):
- um clarão curto de compressão no ponto do golpe;
- rajadas de vento saindo dele, mais compridas para o lado de quem apanha (o golpe empurra o ar
  para dentro da vítima) e mais curtas para trás;
- um anel de pressão em pé (a onda de choque vista de lado) que abre e some.

``impacto_ar``: o acerto (o jogo escala 1 : 1,3 : 1,6 por força). ``impacto_ar_super``: o mesmo
estouro com mais rajadas e um anel a mais, para o Super e o Ultra.

Animação limitada (6 desenhos com tempos diferentes); o jogo estica a folha para a pausa do
golpe + alguns quadros. O golpe vai para a direita (+x); o jogo espelha para a esquerda. Semente
fixa: a folha sai igual em qualquer máquina.

    python3 tools/sprites/impacto_ar.py              # grava em assets/efeitos/
    python3 tools/sprites/impacto_ar.py --previa P   # só a prévia (PNG)
"""
import argparse
import json
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
SAIDA = ROOT / 'android/app/src/main/assets/efeitos'
LADO = 256          # célula em px da folha
PIXEL_SCALE = 2.0   # 256 px da folha = 128 px do mundo no tamanho 1 (o forte)
SS = 3              # supersampling
BRANCO = (250, 250, 255)
LILAS = (206, 212, 255)    # a borda fria dos estouros de ar da defesa
TEMPOS = [33, 67, 50, 50, 33, 33]


def _camada():
    return Image.new('L', (LADO * SS, LADO * SS), 0)


def _pt(cx, cy, r, a, achata=1.0):
    return (cx + r * math.cos(a), cy + r * math.sin(a) * achata)


def _rajada(d, cx, cy, a, r0, r1, larg, alfa):
    """Uma rajada: lâmina fina que afina até a ponta, cheia na base."""
    if r1 <= r0:
        return
    base_l = _pt(cx, cy, r0, a - larg)
    base_r = _pt(cx, cy, r0, a + larg)
    meio_l = _pt(cx, cy, r0 + (r1 - r0) * 0.35, a - larg * 0.55)
    meio_r = _pt(cx, cy, r0 + (r1 - r0) * 0.35, a + larg * 0.55)
    d.polygon([base_l, meio_l, _pt(cx, cy, r1, a), meio_r, base_r], fill=int(255 * alfa))


def _rajadas(semente, n):
    rnd = random.Random(semente)
    out = []
    for i in range(n):
        a = (i + rnd.uniform(-0.35, 0.35)) / n * 2 * math.pi
        # Para a frente (+x, quem apanha) o ar vai longe; para trás, pouco.
        frente = max(0.0, math.cos(a))
        comp = rnd.uniform(0.55, 1.0) * (0.45 + 0.65 * frente)
        out.append((a, comp, rnd.uniform(0.7, 1.2)))
    return out


def estouro(semente, super_=False):
    rnd = random.Random(semente + 5)
    raj = _rajadas(semente, 22 if super_ else 16)
    R = LADO * SS * 0.48
    cx, cy = LADO * SS * 0.44, LADO * SS / 2    # o centro um pouco para trás: as rajadas da frente cabem
    # (rajadas: começo, fim, largura, alfa | clarão: raio, alfa | anel: raio, espessura, alfa)
    roteiro = [
        (0.04, 0.55, 0.20, 1.00, 0.24, 1.00, 0.00, 0, 0),
        (0.08, 1.00, 0.13, 0.95, 0.15, 1.00, 0.00, 0, 0),
        (0.26, 1.05, 0.085, 0.80, 0.08, 0.80, 0.50, 0.040, 0.75),
        (0.58, 1.08, 0.055, 0.55, 0.00, 0.00, 0.66, 0.028, 0.50),
        (0.82, 1.10, 0.035, 0.32, 0.00, 0.00, 0.78, 0.016, 0.28),
        (0.96, 1.12, 0.022, 0.16, 0.00, 0.00, 0.00, 0, 0),
    ]
    quadros = []
    for r0, r1, larg, ar, rc, ac, ra, ea, aa in roteiro:
        miolo, borda = _camada(), _camada()
        dm, db = ImageDraw.Draw(miolo), ImageDraw.Draw(borda)
        for a, c, w in raj:
            fim = R * (r0 + (r1 - r0) * c)
            ini = R * r0
            _rajada(db, cx, cy, a, ini, fim, larg * w * 1.6, ar)
            _rajada(dm, cx, cy, a, ini, ini + (fim - ini) * 0.8, larg * w * 0.7, ar)
        if rc > 0:
            r = R * rc
            # Compressão: achatada no eixo do golpe (o ar é empurrado de lado).
            db.ellipse([cx - r * 0.75, cy - r * 1.25, cx + r * 0.75, cy + r * 1.25], fill=int(255 * ac))
            dm.ellipse([cx - r * 0.45, cy - r * 0.85, cx + r * 0.45, cy + r * 0.85], fill=int(255 * ac))
        if ra > 0:
            aneis = [(ra, ea, aa)] + ([(ra * 0.7, ea * 0.7, aa * 0.8)] if super_ else [])
            for rr, ee, a2 in aneis:
                r = R * rr
                # O anel em pé (a onda de choque vista de lado), mais aberto do lado da vítima.
                caixa = [cx - r * 0.42, cy - r, cx + r * 0.55, cy + r]
                db.ellipse(caixa, outline=int(255 * a2), width=max(1, int(R * ee * 1.8)))
                dm.ellipse(caixa, outline=int(255 * a2), width=max(1, int(R * ee * 0.8)))
        # Ar é macio: a borda borrada, o miolo quase nítido.
        borda = borda.filter(ImageFilter.GaussianBlur(SS * 2.2))
        miolo = miolo.filter(ImageFilter.GaussianBlur(SS * 0.8))
        b = np.asarray(borda, dtype=np.float32) / 255
        m = np.asarray(miolo, dtype=np.float32) / 255
        alfa = np.clip(b * 0.75 + m, 0, 1)
        t = np.clip(m / np.maximum(alfa, 1e-6), 0, 1)[..., None]
        cor = np.array(LILAS, np.float32) * (1 - t) + np.array(BRANCO, np.float32) * t
        rgba = np.dstack([cor, alfa[..., None] * 255]).astype(np.uint8)
        im = Image.fromarray(rgba, 'RGBA').resize((LADO, LADO), Image.LANCZOS)
        quadros.append(im)
    return quadros


def gravar(nome, quadros):
    pasta = SAIDA / nome
    pasta.mkdir(parents=True, exist_ok=True)
    caixas = []
    for q in quadros:
        a = q.getchannel('A').point(lambda v: 255 if v > 3 else 0)
        caixas.append(a.getbbox() or (0, 0, 1, 1))
    largura = sum(b[2] - b[0] for b in caixas) + 2 * len(caixas)
    alto = max(b[3] - b[1] for b in caixas)
    folha = Image.new('RGBA', (largura, alto), (0, 0, 0, 0))
    lista, x = [], 0
    for q, b in zip(quadros, caixas):
        folha.paste(q.crop(b), (x, 0))
        lista.append([x, 0, b[2] - b[0], b[3] - b[1], float(b[0] - LADO / 2), float(b[1] - LADO / 2)])
        x += b[2] - b[0] + 2
    folha.save(pasta / 'folha.webp', lossless=True, quality=100, method=6)
    dados = {'nome': nome, 'origem': 'tools/sprites/impacto_ar.py (desenhado em código)', 'ancora': 'centro',
             'pixelScale': PIXEL_SCALE, 'altura': LADO / PIXEL_SCALE, 'quadros': lista, 'duracoesMs': TEMPOS}
    (pasta / 'efeito.json').write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(nome, '->', pasta.relative_to(ROOT), folha.size)


def todos():
    return {'impacto_ar': estouro(3), 'impacto_ar_super': estouro(3, super_=True)}


def previa(caminho):
    fs = todos()
    cel = LADO // 2 + 8
    out = Image.new('RGBA', (cel * 6 + 8, cel * len(fs) + 8), (34, 28, 52, 255))
    for j, qs in enumerate(fs.values()):
        for i, q in enumerate(qs):
            out.alpha_composite(q.resize((LADO // 2, LADO // 2), Image.LANCZOS), (8 + i * cel, 8 + j * cel))
    out.convert('RGB').save(caminho)


if __name__ == '__main__':
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--previa', help='só a prévia (PNG), sem gravar nos assets')
    a = ap.parse_args()
    if a.previa:
        previa(a.previa)
    else:
        for nome, qs in todos().items():
            gravar(nome, qs)
