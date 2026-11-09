#!/usr/bin/env python3
"""Uma imagem parada (gerada no GPT/Codex) vira um efeito do jogo, para testar estilo de VFX.

A imagem entra sozinha em fundo preto (efeito luminoso), em verde ou já transparente. O estouro
(nasce pequena, passa do tamanho, assenta e some) vai desenhado nos quadros da folha, então o
jogo toca como qualquer outro efeito e o motor não muda. O jogo estica a folha para a pausa do
golpe + alguns quadros (EfeitosVideo.tocarDurando).

    python3 tools/sprites/efeito_imagem.py art/vfx/acerto_01.png --nome impacto_ar
    python3 tools/sprites/efeito_imagem.py art/vfx/defesa_01.png --nome defesa --tamanho 150
    python3 tools/sprites/efeito_imagem.py art/vfx/acerto.png --nome impacto_ar --quadros 4

Com --quadros N a imagem é uma FOLHA do docs/efeitos-gpt.md (N quadros lado a lado, na ordem do
tempo): cada faixa vira um quadro e o estouro por código não entra (a animação já está desenhada).

    --nome N        a pasta em assets/efeitos/. Os nomes que a luta toca hoje:
                      impacto_ar         acerto de golpe e de projétil (fraco/médio/forte por escala)
                      impacto_ar_super   acerto de Super e de Ultra (1,25x)
                      defesa, defesa_2, defesa_3   golpe defendido (um de cada vez)
                      defesa_poder       poder defendido
                    Usar um desses nomes troca o efeito na hora (voltar: git checkout na pasta).
    --tamanho T     o lado maior do efeito em px do mundo no tamanho 1 (128: o do acerto forte)
    --fundo F       preto (padrão: o brilho vira transparência), verde, ou transparente (PNG com alfa)
    --origem x,y    o ponto de contato na imagem, de 0 a 1 (padrão 0.5,0.5: o meio)
    --parado        sem o estouro: a imagem inteira some aos poucos (fumaça, brilho que fica)
    --quadros N     a imagem tem N quadros lado a lado (faixas iguais); --origem vale dentro de cada faixa
    --previa P.png  só mostra os quadros (fundo escuro e claro), não grava no jogo

A imagem aponta para a direita (o sentido do golpe); o jogo espelha para a esquerda.
"""
import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SAIDA = ROOT / 'android/app/src/main/assets/efeitos'
PIXEL_SCALE = 2.0   # 2 px da folha = 1 px do mundo (o mesmo dos outros efeitos)
# O estouro: (escala, opacidade, ms). O pico passa um pouco do tamanho e assenta; a cauda some.
ESTOURO = [(0.55, 1.0, 33), (1.12, 1.0, 50), (1.0, 1.0, 67), (1.0, 0.8, 50), (1.03, 0.5, 33), (1.06, 0.22, 33)]
PARADO = [(1.0, 1.0, 67), (1.0, 1.0, 67), (1.0, 0.8, 50), (1.0, 0.55, 50), (1.0, 0.3, 33), (1.0, 0.12, 33)]


def recortar_fundo(imagem, fundo):
    """RGBA reto (cor sem o fundo misturado) a partir da imagem em preto, verde ou já com alfa."""
    if fundo == 'transparente':
        return np.asarray(imagem.convert('RGBA')).astype(np.float32) / 255.0
    rgb = np.asarray(imagem.convert('RGB')).astype(np.float32) / 255.0
    if fundo == 'preto':
        # Luz sobre o preto: o canal mais forte é o quanto ela cobre; a cor volta dividindo por ele.
        alfa = rgb.max(axis=2)
        alfa = np.clip((alfa - 0.03) / 0.97, 0.0, 1.0)   # o "quase preto" do gerador não vira névoa
        cor = rgb / np.maximum(rgb.max(axis=2, keepdims=True), 1e-4)
        # Miolo claro e opaco continua com a cor dele (branco não vira cor pura).
        cor = np.where(alfa[..., None] > 0.999, rgb, cor)
    else:
        # Verde: quanto o verde passa dos outros dois canais é o quanto do fundo aparece.
        sobra = np.clip(rgb[..., 1] - np.maximum(rgb[..., 0], rgb[..., 2]), 0.0, 1.0)
        alfa = np.clip(1.0 - sobra * 2.2, 0.0, 1.0)
        cor = rgb.copy()
        cor[..., 1] = np.minimum(cor[..., 1], np.maximum(cor[..., 0], cor[..., 2]))
    return np.dstack([cor, alfa])


def quadros_de(rgba, tamanho, origem, roteiro):
    """Os quadros (PIL RGBA, todos numa tela do mesmo lado, com o contato no meio) e os tempos."""
    alto, largo = rgba.shape[:2]
    ys, xs = np.nonzero(rgba[..., 3] > 0.02)
    if len(xs) == 0:
        raise SystemExit('a imagem ficou toda transparente: confira o --fundo')
    x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
    ox, oy = origem[0] * largo, origem[1] * alto
    k = tamanho * PIXEL_SCALE / max(x1 - x0, y1 - y0)
    base = Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), 'RGBA').crop((x0, y0, x1, y1))
    # A tela: o contato no centro e folga para o quadro maior do estouro.
    maior = max(e for e, _, _ in roteiro)
    alcance = max(ox - x0, x1 - ox, oy - y0, y1 - oy) * k * maior
    lado = int(np.ceil(alcance * 2)) + 4
    quadros, tempos = [], []
    for escala, opacidade, ms in roteiro:
        w, h = max(1, round((x1 - x0) * k * escala)), max(1, round((y1 - y0) * k * escala))
        img = base.resize((w, h), Image.LANCZOS)
        if opacidade < 1.0:
            a = img.getchannel('A').point(lambda v, o=opacidade: int(v * o))
            img.putalpha(a)
        tela = Image.new('RGBA', (lado, lado), (0, 0, 0, 0))
        tela.paste(img, (round(lado / 2 - (ox - x0) * k * escala), round(lado / 2 - (oy - y0) * k * escala)))
        quadros.append(tela)
        tempos.append(ms)
    return quadros, tempos, lado


def quadros_da_folha(rgba, n, tamanho, origem):
    """N faixas iguais lado a lado: cada uma é um quadro, todas na mesma escala e com a mesma origem."""
    alto, largo = rgba.shape[:2]
    passo = largo / n
    faixas = [rgba[:, round(i * passo):round((i + 1) * passo)] for i in range(n)]
    maior = 1
    for f in faixas:
        ys, xs = np.nonzero(f[..., 3] > 0.02)
        if len(xs):
            maior = max(maior, xs.max() + 1 - xs.min(), ys.max() + 1 - ys.min())
    k = tamanho * PIXEL_SCALE / maior
    lado = int(np.ceil(max(passo, alto) * k)) + 4
    quadros = []
    for f in faixas:
        img = Image.fromarray((np.clip(f, 0, 1) * 255).astype(np.uint8), 'RGBA')
        img = img.resize((max(1, round(img.width * k)), max(1, round(img.height * k))), Image.LANCZOS)
        tela = Image.new('RGBA', (lado, lado), (0, 0, 0, 0))
        tela.paste(img, (round(lado / 2 - origem[0] * img.width), round(lado / 2 - origem[1] * img.height)))
        quadros.append(tela)
    # O primeiro quadro (o clarão nascendo) passa rápido, o segundo (o máximo) segura mais.
    tempos = [33, 67] + [50] * (n - 2) if n >= 2 else [100]
    return quadros, tempos[:n], lado


def gravar(nome, quadros, tempos, lado, origem_do_arquivo):
    pasta = SAIDA / nome
    pasta.mkdir(parents=True, exist_ok=True)
    caixas = [q.getchannel('A').point(lambda v: 255 if v > 3 else 0).getbbox() or (0, 0, 1, 1) for q in quadros]
    largura = sum(b[2] - b[0] for b in caixas) + 2 * len(caixas)
    alto = max(b[3] - b[1] for b in caixas)
    folha = Image.new('RGBA', (largura, alto), (0, 0, 0, 0))
    lista, x = [], 0
    for q, b in zip(quadros, caixas):
        folha.paste(q.crop(b), (x, 0))
        lista.append([x, 0, b[2] - b[0], b[3] - b[1], float(b[0] - lado / 2), float(b[1] - lado / 2)])
        x += b[2] - b[0] + 2
    folha.save(pasta / 'folha.webp', lossless=True, quality=100, method=6)
    dados = {'nome': nome, 'origem': f'tools/sprites/efeito_imagem.py ({origem_do_arquivo})', 'ancora': 'centro',
             'pixelScale': PIXEL_SCALE, 'altura': lado / PIXEL_SCALE, 'quadros': lista, 'duracoesMs': tempos}
    (pasta / 'efeito.json').write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(nome, '->', pasta.relative_to(ROOT), folha.size)


def previa(quadros, lado, destino):
    """Os quadros lado a lado, em fundo escuro e em fundo claro."""
    tela = Image.new('RGB', (lado * len(quadros), lado * 2), (0, 0, 0))
    for linha, fundo in enumerate(((38, 30, 62), (226, 222, 214))):
        for i, q in enumerate(quadros):
            celula = Image.new('RGBA', (lado, lado), fundo + (255,))
            celula.alpha_composite(q)
            tela.paste(celula.convert('RGB'), (i * lado, linha * lado))
    tela.save(destino)
    print('prévia ->', destino, tela.size)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('imagem')
    p.add_argument('--nome')
    p.add_argument('--tamanho', type=float, default=128.0)
    p.add_argument('--fundo', choices=('preto', 'verde', 'transparente'), default='preto')
    p.add_argument('--origem', default='0.5,0.5')
    p.add_argument('--parado', action='store_true')
    p.add_argument('--quadros', type=int, default=1)
    p.add_argument('--previa')
    a = p.parse_args()
    if not a.nome and not a.previa:
        p.error('diga --nome (grava no jogo) ou --previa (só mostra)')
    origem = tuple(float(v) for v in a.origem.split(','))
    rgba = recortar_fundo(Image.open(a.imagem), a.fundo)
    if a.quadros > 1:
        quadros, tempos, lado = quadros_da_folha(rgba, a.quadros, a.tamanho, origem)
    else:
        quadros, tempos, lado = quadros_de(rgba, a.tamanho, origem, PARADO if a.parado else ESTOURO)
    if a.previa:
        previa(quadros, lado, a.previa)
    if a.nome:
        gravar(a.nome, quadros, tempos, lado, Path(a.imagem).name)


if __name__ == '__main__':
    main()
