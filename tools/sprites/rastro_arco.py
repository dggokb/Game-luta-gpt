#!/usr/bin/env python3
"""O arco do golpe: a fita de ar que acompanha o balanço do braço ou da perna.

Entra uma textura RETA (gerada no GPT/Codex, fundo preto): uma fita horizontal que nasce fina à
esquerda (a ponta velha do rastro) e chega à grossura cheia à direita (a ponta nova, onde está o
punho ou o pé), com a borda de cima nítida. Aqui ela é curvada num arco de círculo e animada: a
ponta nova varre o arco em dois quadros e a fita se desfaz a partir da ponta velha.

    python3 tools/sprites/rastro_arco.py art/vfx/arco_textura_01.png --nome rastro_arco
    python3 tools/sprites/rastro_arco.py art/vfx/arco_textura_01.png --nome rastro_arco_desce --desce
    python3 tools/sprites/rastro_arco.py art/vfx/arco_textura_01.png --nome rastro_arco_forte --abertura 130 --grossura 0.36

A folha sai com a origem no CENTRO do círculo e a ponta nova no eixo para a direita, a RAIO px do
mundo: o jogo põe a origem na raiz do membro (o quadril, o ombro), escala pelo comprimento dele e
gira para onde ele aponta. O arco fica do lado de onde o membro veio: embaixo (o chute que sobe)
ou, com --desce, em cima (o golpe que desce).

    --abertura G   graus que o arco cobre (95)
    --grossura F   a grossura da fita na ponta nova, em fração do raio (0.28)
    --previa P     só mostra os quadros
"""
import argparse
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import efeito_imagem as ei  # noqa: E402

RAIO = 128.0   # px do mundo: o comprimento de membro para o qual a folha é desenhada (escala 1)
# (ponta velha, ponta nova) do pedaço visível, em fração do arco; opacidade; ms.
VARRIDA = [(0.0, 0.55, 1.0, 17), (0.0, 1.0, 1.0, 33), (0.18, 1.0, 0.9, 33), (0.42, 1.0, 0.65, 33),
           (0.62, 1.0, 0.4, 33), (0.78, 1.0, 0.18, 17)]


def fita(caminho):
    """A textura recortada: RGBA reto (float), a ponta nova à direita e a borda nítida em cima."""
    rgba = ei.recortar_fundo(Image.open(caminho), 'preto')
    ys, xs = np.nonzero(rgba[..., 3] > 0.04)
    return rgba[ys.min():ys.max() + 1, xs.min():xs.max() + 1]


def amostrar(tex, u, v):
    """A textura em (u, v) de 0 a 1 (bilinear); fora dela, transparente."""
    alto, largo = tex.shape[:2]
    x, y = u * (largo - 1), v * (alto - 1)
    dentro = (u >= 0) & (u <= 1) & (v >= 0) & (v <= 1)
    x0, y0 = np.clip(np.floor(x).astype(int), 0, largo - 2), np.clip(np.floor(y).astype(int), 0, alto - 2)
    fx, fy = (x - x0)[..., None], (y - y0)[..., None]
    c = (tex[y0, x0] * (1 - fx) * (1 - fy) + tex[y0, x0 + 1] * fx * (1 - fy)
         + tex[y0 + 1, x0] * (1 - fx) * fy + tex[y0 + 1, x0 + 1] * fx * fy)
    c[~dentro] = 0
    return c


def quadros(tex, abertura, grossura, desce):
    """Os quadros da varrida (PIL RGBA, telas quadradas com o centro do círculo no meio) e os tempos."""
    raio = RAIO * ei.PIXEL_SCALE
    faixa = raio * grossura
    lado = int(np.ceil((raio + faixa) * 2)) + 4
    py, px = np.mgrid[0:lado, 0:lado].astype(np.float32)
    dx, dy = px - lado / 2, lado / 2 - py          # y para cima
    if desce:
        dy = -dy
    r = np.hypot(dx, dy)
    # O arco fica embaixo do eixo (de -abertura a 0 graus): s vai de 0 (ponta velha) a 1 (ponta nova).
    s = (np.degrees(np.arctan2(dy, dx)) + abertura) / abertura
    # A borda nítida da textura (em cima) é a de fora do arco; ela passa pela ponta do membro.
    v = (raio - r) / faixa
    saida, tempos = [], []
    for velha, nova, opacidade, ms in VARRIDA:
        cor = amostrar(tex, (s - velha) / (nova - velha), v)
        cor[..., 3] *= opacidade
        saida.append(Image.fromarray((np.clip(cor, 0, 1) * 255).astype(np.uint8), 'RGBA'))
        tempos.append(ms)
    return saida, tempos, lado


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('textura')
    p.add_argument('--nome')
    p.add_argument("--abertura", type=float, default=95.0)
    p.add_argument("--grossura", type=float, default=0.28)
    p.add_argument('--desce', action='store_true')
    p.add_argument('--previa')
    a = p.parse_args()
    if not a.nome and not a.previa:
        p.error('diga --nome (grava no jogo) ou --previa (só mostra)')
    qs, tempos, lado = quadros(fita(a.textura), a.abertura, a.grossura, a.desce)
    if a.previa:
        ei.previa(qs, lado, a.previa)
    if a.nome:
        ei.gravar(a.nome, qs, tempos, lado, Path(a.textura).name)


if __name__ == '__main__':
    main()
