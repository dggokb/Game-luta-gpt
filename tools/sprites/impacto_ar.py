#!/usr/bin/env python3
"""O impacto de ar do acerto, desenhado em código (docs/revisao-base/direcao.md, regras E1–E7).

Golpe normal é pancada: ninguém solta poder, então o acerto não tem faísca, fogo nem energia.
O que se vê é o ar deslocado, na mesma família dos estouros de ar da defesa (defesa,
defesa_2, defesa_3: estrelas brancas de pontas rasgadas, tiradas dos vídeos de defesa).

O traço é o do resto do jogo: borda dura e tons chapados (branco, a borda fria lilás e um fio
de nanquim em volta, que segura o estouro em cima de roupa branca e de cenário claro). Nada
borrado: a primeira versão era um clarão macio com uma elipse perfeita e sumia na tela.
- uma estrela de ar de espinhos gordos e desiguais, mais compridos para o lado de quem apanha (o
  golpe empurra o ar para dentro da vítima) e curtos para trás;
- no contato, o miolo de nanquim com um ponto branco (a mancha de tinta do City of the Wolves:
  combina com o contorno dos lutadores e marca o quadro do golpe sem piscar ninguém);
- depois a estrela esvazia pelo meio, os espinhos afinam e viram cacos, e um anel de pressão em
  pé (a onda de choque vista de lado), quebrado em pedaços, abre e some.

``impacto_ar``: o acerto (o jogo escala 1 : 1,3 : 1,6 por força; a estrela do forte tem 128 px do
mundo, a regra de tamanho da revisão, e só os cacos e o anel passam um pouco dela). ``impacto_ar_super``: o mesmo
estouro com mais espinhos e um anel a mais, para o Super e o Ultra.

Animação limitada (6 desenhos com tempos diferentes); o jogo estica a folha para a pausa do
golpe + alguns quadros. O golpe vai para a direita (+x); o jogo espelha para a esquerda. Semente
fixa: a folha sai igual em qualquer máquina.

    python3 tools/sprites/impacto_ar.py              # grava em assets/efeitos/
    python3 tools/sprites/impacto_ar.py --previa P   # só a prévia (PNG)
"""
import argparse
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SAIDA = ROOT / 'android/app/src/main/assets/efeitos'
LADO = 320          # célula em px da folha: a estrela do contato ocupa 256 (128 px do mundo no forte);
                    # a sobra é para os cacos e o anel, que passam dela
PIXEL_SCALE = 2.0   # 2 px da folha = 1 px do mundo no tamanho 1 (o forte)
SS = 3              # supersampling
BRANCO = (250, 250, 255)
LILAS = (190, 200, 255)    # a borda fria dos estouros de ar da defesa
NANQUIM = (24, 26, 52)     # o fio de fora e o miolo do contato
TEMPOS = [33, 67, 50, 50, 33, 33]

N = LADO * SS
YY, XX = np.mgrid[0:N, 0:N].astype(np.float32)
_RETICULA = np.random.default_rng(11).random((64, 64)).astype(np.float32)

# Espinhos (ângulo, comprimento, meia largura): compridos para a frente (+x), curtos para trás.
ESPINHOS = [(0.02, 1.00, 0.30), (0.66, 0.80, 0.25), (1.34, 0.62, 0.24), (2.08, 0.46, 0.25), (2.86, 0.38, 0.26),
            (3.58, 0.44, 0.24), (4.34, 0.58, 0.25), (5.04, 0.72, 0.25), (5.70, 0.88, 0.26)]
# Os do Super: finos e longos, entre os outros.
ESPINHOS_SUPER = [(0.34, 1.04, 0.12), (1.00, 0.84, 0.11), (1.72, 0.62, 0.11), (4.70, 0.74, 0.11), (5.38, 0.96, 0.12),
                  (3.20, 0.50, 0.11)]
MIOLO = 0.26   # raio do corpo da estrela sem os espinhos (fração do tamanho)


def _ruido(x, y):
    xi, yi = np.floor(x).astype(int), np.floor(y).astype(int)
    fx, fy = x - xi, y - yi
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = _RETICULA[yi % 64, xi % 64], _RETICULA[yi % 64, (xi + 1) % 64]
    c, d = _RETICULA[(yi + 1) % 64, xi % 64], _RETICULA[(yi + 1) % 64, (xi + 1) % 64]
    return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy


def _dentro(d):
    """Borda dura com 1 px de antialias (d = distância para dentro, em px da grade)."""
    return np.clip(d + 0.5, 0.0, 1.0)


def _estrela(dx, dy, espinhos, gira, fino, raio):
    """Distância para dentro da estrela (px da grade; positiva dentro): o corpo redondo mais
    espinhos de lados retos e desiguais, cada um um triângulo da base até a ponta."""
    d = raio * MIOLO - np.hypot(dx, dy)
    for a, comp, larg in espinhos:
        ux, uy = math.cos(a + gira), -math.sin(a + gira)
        ao_longo = dx * ux + dy * uy
        de_lado = np.abs(-dx * uy + dy * ux)
        ponta = comp * raio
        meia = raio * MIOLO * math.sin(min(1.2, larg * fino)) * 2.1
        tri = (meia * (1 - ao_longo / ponta) - de_lado) / math.hypot(1.0, meia / ponta)
        d = np.maximum(d, np.where(ao_longo >= 0, tri, -1e9))
    return d


def _cacos(dx, dy, espinhos, raio, de, ate, fino):
    """Distância para dentro dos cacos: de cada espinho sobra um losango fino, do trecho
    {@code de} ao {@code ate} do comprimento dele (o ar se desfazendo em lascas)."""
    d = np.full_like(dx, -1e9)
    for a, comp, larg in espinhos:
        ux, uy = math.cos(a), -math.sin(a)
        ao_longo = dx * ux + dy * uy
        de_lado = np.abs(-dx * uy + dy * ux)
        p0, p1 = de * comp * raio, ate * comp * raio
        meia = raio * MIOLO * math.sin(min(1.2, larg)) * 2.1 * fino
        quebra = p0 + (p1 - p0) * 0.28                       # o ponto mais largo, perto da base
        sobe = (ao_longo - p0) / max(1.0, quebra - p0)
        desce = (p1 - ao_longo) / max(1.0, p1 - quebra)
        d = np.maximum(d, meia * np.minimum(sobe, desce) - de_lado)
    return d


def _pinta(rgb, alfa, cor, cobertura):
    """Tinta chapada por cima (alfa reto, composição "over")."""
    cor = np.asarray(cor, np.float32)
    fora = alfa * (1 - cobertura)
    novo = cobertura + fora
    rgb[:] = (cor * cobertura[..., None] + rgb * fora[..., None]) / np.maximum(novo, 1e-6)[..., None]
    alfa[:] = novo


def estouro(semente, super_=False):
    espinhos = ESPINHOS + (ESPINHOS_SUPER if super_ else [])
    R = N * 0.40                                # raio: a estrela do contato fica com ~256 px da folha de
                                                # ponta a ponta (128 px do mundo no forte), como na review
    cx, cy = N * 0.40, N / 2.0                  # o centro um pouco para trás: os espinhos da frente cabem
    dx, dy = XX - cx, YY - cy
    r = np.hypot(dx, dy)
    fio = SS * 1.7                               # o fio de nanquim, ~1,7 px da folha
    claro = SS * 2.2                             # a borda fria que sobra em volta do branco
    # Os seis desenhos: a estrela abre num quadro, fica (com o miolo de nanquim), esvazia pelo
    # meio e se desfaz em cacos; o anel de pressão abre por trás.
    # (estrela: tamanho, miolo de nanquim, buraco | cacos: de, até, largura | anel: raio, espessura, falhas)
    roteiro = [
        ((0.62, 0.60, 0.00), None, None),
        ((1.00, 0.52, 0.00), None, None),
        ((1.06, 0.00, 0.46), None, (0.56, 0.050, 0.30)),
        (None, (0.50, 1.07, 0.44), (0.72, 0.034, 0.40)),
        (None, (0.74, 1.12, 0.30), (0.88, 0.020, 0.52)),
        (None, (0.95, 1.16, 0.20), None),
    ]
    quadros = []
    for i, (estrela, cacos, anel) in enumerate(roteiro):
        rgb = np.zeros((N, N, 3), np.float32)
        alfa = np.zeros((N, N), np.float32)
        if anel:
            # O anel em pé (a onda de choque vista de lado), mais aberto do lado da vítima e
            # quebrado em pedaços; por baixo da estrela.
            ra, ea, falha = anel
            for k, (rr, ee) in enumerate([(ra, ea)] + ([(ra * 0.72, ea * 0.8)] if super_ else [])):
                ex = np.where(dx > 0, 0.56, 0.42)
                er = np.hypot(dx / ex, dy)
                n = _ruido(np.arctan2(dy, dx / ex) * 2.4 + 7.0 + k * 3.1 + semente, np.full_like(er, i * 1.7))
                esp = R * ee * np.clip((n - falha) * 3.0, 0, 1.3)
                d = esp - np.abs(er - R * rr)
                vivo = (n > falha + 0.04)
                _pinta(rgb, alfa, NANQUIM, _dentro(d + fio * 0.7) * vivo * 0.85)
                _pinta(rgb, alfa, LILAS, _dentro(d) * vivo)
                _pinta(rgb, alfa, BRANCO, _dentro(d - R * ee * 0.42) * vivo)
        if estrela:
            tam, miolo, buraco = estrela
            fora = _estrela(dx, dy, espinhos, 0.0, 1.0, R * tam)
            # O buraco do meio também é uma estrela (girada): a estrela fica vazada.
            oco = -_estrela(dx, dy, ESPINHOS, 0.36, 1.0, R * tam * buraco) if buraco > 0 else np.full_like(r, 1e9)
        else:
            de, ate, larg = cacos
            fora, oco, miolo = _cacos(dx, dy, espinhos, R, de, ate, larg), np.full_like(r, 1e9), 0
        _pinta(rgb, alfa, NANQUIM, _dentro(fora + fio) * _dentro(oco + fio) * 0.9)
        _pinta(rgb, alfa, LILAS, _dentro(fora) * _dentro(oco))
        # O branco fica a um fio da borda: a borda fria aparece como contorno, não como degradê.
        _pinta(rgb, alfa, BRANCO, _dentro(fora - claro) * _dentro(oco - claro))
        if miolo > 0:
            # O miolo de nanquim do contato: uma estrela menor, girada, com o ponto branco.
            _pinta(rgb, alfa, NANQUIM, _dentro(_estrela(dx, dy, ESPINHOS, 0.36, 0.85, R * tam * miolo)))
            _pinta(rgb, alfa, BRANCO, _dentro(_estrela(dx, dy, ESPINHOS, 0.05, 0.7, R * tam * miolo * 0.40)))
        # Reduz com o alfa pré-multiplicado (sem franja escura na borda).
        pre = np.dstack([rgb * alfa[..., None], alfa[..., None] * 255.0])
        pre = pre.reshape(LADO, SS, LADO, SS, 4).mean(axis=(1, 3))
        a = pre[..., 3:4]
        cor = np.where(a > 0.5, pre[..., :3] / np.maximum(a / 255.0, 1e-6), 0)
        quadros.append(Image.fromarray(np.dstack([cor, a]).clip(0, 255).astype(np.uint8), 'RGBA'))
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
    # A luz que o efeito emite ("luz": cor, alcance, forca) é posta à mão no efeito.json:
    # refazer a folha não a apaga.
    antigo = pasta / 'efeito.json'
    if antigo.exists() and 'luz' not in dados:
        luz = json.loads(antigo.read_text(encoding='utf-8')).get('luz')
        if luz is not None:
            dados['luz'] = luz
    (pasta / 'efeito.json').write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(nome, '->', pasta.relative_to(ROOT), folha.size)


def todos():
    return {'impacto_ar': estouro(3), 'impacto_ar_super': estouro(3, super_=True)}


def previa(caminho):
    """Os dois estouros, em fundo escuro e em fundo claro (o fio de nanquim tem de segurar nos dois)."""
    fs = todos()
    cel = LADO + 8
    out = Image.new('RGBA', (cel * 6 + 8, cel * len(fs) * 2 + 8), (34, 28, 52, 255))
    for j, qs in enumerate(list(fs.values()) * 2):
        if j >= len(fs):
            out.paste((232, 226, 214, 255), (0, 4 + j * cel, out.width, 4 + (j + 1) * cel))
        for i, q in enumerate(qs):
            out.alpha_composite(q, (8 + i * cel, 8 + j * cel))
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
