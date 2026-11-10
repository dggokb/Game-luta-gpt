#!/usr/bin/env python3
"""A faísca do golpe forte de hoje (impacto_ar_forte, tirada do vídeo art/vfx/dano/dano_forte.mp4)
refeita no EffectCraft, desenhada nele mesmo, para ver se o programa chega à mesma cara.

Pedido de 10/11: "não consegue fazer igual à referência do que está no jogo hoje?". As medidas
saíram da folha atual (assets/efeitos/impacto_ar_forte), quadro a quadro, em px da folha (1,5 por
px do mundo), e o tempo é o mesmo dela: 12 desenhos que o jogo estica para a pausa do golpe.

O que tem na referência, e como fica aqui:
- Estrela de espinhos irregulares no contato, em faixas chapadas de anime (borda vermelho-escura,
  laranja, amarelo, amarelo-claro e núcleo branco): o mesmo contorno em 5 cópias, cada uma menor
  em direção ao centro. Encolhe até sumir no desenho 8.
- Três jatos longos e pontudos na direção do golpe (para a direita), nas mesmas faixas; encolhem
  para o contato.
- Riscos finos vermelho-escuros em volta da estrela, só nos primeiros desenhos.
- Brasas: losangos alongados que voam para a direita e freiam; ficam até o fim e somem.
- Halo laranja (Glow com as cores A e B, do laranja ao vermelho) e uma mancha laranja atrás.

    python3 art/vfx/effectcraft/impacto_forte_ec.py --saida PASTA [--cli CAMINHO]

Sai: art/vfx/effectcraft/impacto_forte_ec.ecproj e PASTA/impacto_forte_ec_0000.png ... 0011.png.
No jogo: efeito_video.py PASTA --nome impacto_forte_ec --fps 8 --origem 245,231 --escala-mundo 0.6667
--resolucao 1.5 (o mesmo ponto, tamanho e ritmo da impacto_ar_forte).
"""
import argparse
import json
import math
import random
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
PROJETO = AQUI / 'impacto_forte_ec.ecproj'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

FPS = 30
QUADROS = 12
# A tela da folha atual era 585 x 342 com o miolo em (150, 170); cresceu 70 para a esquerda e 60
# em cima e embaixo para caber o anel de pressão de ar. O contato do jogo fica em (245, 231).
FOLGA = (70, 60)
LARGO, ALTO = 585 + FOLGA[0], 342 + 2 * FOLGA[1]
CENTRO = (150.0 + FOLGA[0], 170.0 + FOLGA[1])
CONTATO = (175 + FOLGA[0], 171 + FOLGA[1])

# As faixas, de fora para dentro: (cor, escala do contorno).
FAIXAS = [('#6e1b0e', 1.00), ('#ff6a0d', 0.93), ('#ffb21a', 0.80), ('#ffe27a', 0.60), ('#fffbe8', 0.38)]
LARANJA, VERMELHO = '#ff7a12', '#b8260c'


def t(q):
    return q / FPS


def chaves(camada, caminho, valores):
    return [{'command': 'prop.addKey', 'params': {'layer': camada, 'path': caminho, 'time': t(q), 'value': v}}
            for q, v in enumerate(valores) if v is not None]


def caminho(pontos):
    zero = [[0, 0]] * len(pontos)
    return {'closed': True, 'vertices': [[round(x, 2), round(y, 2)] for x, y in pontos],
            'in_tangents': zero, 'out_tangents': zero}


def losango(comprimento, largura, angulo, atras=0.0):
    """Ponta fina dos dois lados: base `atras` px antes da origem, ponta a `comprimento`."""
    a = math.radians(angulo)
    ux, uy, nx, ny = math.cos(a), math.sin(a), -math.sin(a), math.cos(a)
    meio = (comprimento - atras) * 0.35 - atras * 0.0
    return [(-atras * ux, -atras * uy),
            (meio * ux + nx * largura / 2, meio * uy + ny * largura / 2),
            (comprimento * ux, comprimento * uy),
            (meio * ux - nx * largura / 2, meio * uy - ny * largura / 2)]


def estrela(sorte):
    """O contorno da estrela da referência: espinhos medidos (ângulo em graus, comprimento em px),
    com vales irregulares entre eles."""
    espinhos = [(-78, 170), (-112, 132), (-138, 95), (-168, 75), (150, 92), (100, 82),
                (62, 140), (35, 132), (-20, 150), (-45, 100), (12, 70), (-60, 60), (130, 55), (-150, 45)]
    # Os espinhos menores entre os grandes (a borda serrilhada da referência).
    for _ in range(10):
        espinhos.append((sorte.uniform(-180, 180), sorte.uniform(55, 85)))
    espinhos.sort()
    pontos = []
    for i, (ang, comp) in enumerate(espinhos):
        a = math.radians(ang)
        pontos.append((comp * math.cos(a), comp * math.sin(a)))
        prox = espinhos[(i + 1) % len(espinhos)][0] + (360 if i + 1 == len(espinhos) else 0)
        v = math.radians((ang + prox) / 2)
        vale = sorte.uniform(38, 54)
        pontos.append((vale * math.cos(v), vale * math.sin(v)))
    return pontos


def grupo(camada, n, pontos, cor, escala=100, pos=(0, 0)):
    """Um grupo da camada de forma com um caminho e um preenchimento."""
    g = f'contents/g{n}'
    return [
        {'command': 'engine.batch', 'params': {'steps': [
            {'command': 'layer.addShapeItem', 'params': {'layer': camada, 'kind': 'group'}},
        ]}},
        {'command': 'layer.addShapeItem', 'params': {'layer': camada, 'kind': 'path', 'group': f'@g{n}'}},
        {'command': 'layer.addShapeItem', 'params': {'layer': camada, 'kind': 'fill', 'group': f'@g{n}'}},
        {'command': 'prop.set', 'params': {'layer': camada, 'path': f'@g{n}/contents/path/path', 'value': caminho(pontos)}},
        {'command': 'prop.set', 'params': {'layer': camada, 'path': f'@g{n}/contents/fill/color', 'value': cor}},
        {'command': 'prop.set', 'params': {'layer': camada, 'path': f'@g{n}/transform/scale', 'value': [escala, escala]}},
        {'command': 'prop.set', 'params': {'layer': camada, 'path': f'@g{n}/transform/position', 'value': list(pos)}},
    ]


def camada_forma(nome, pos=CENTRO):
    return [{'command': 'layer.newShape', 'params': {'kind': 'none', 'name': nome}},
            {'command': 'prop.set', 'params': {'layer': nome, 'path': 'transform/position', 'value': [*pos, 0]}}]


def halo(camada, raio=34, forca=0.9):
    return [{'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': [camada]}},
            {'command': 'prop.set', 'params': {'layer': camada, 'path': '@fx/threshold', 'value': 20}},
            {'command': 'prop.set', 'params': {'layer': camada, 'path': '@fx/radius', 'value': raio}},
            {'command': 'prop.set', 'params': {'layer': camada, 'path': '@fx/intensity', 'value': forca}},
            {'command': 'prop.set', 'params': {'layer': camada, 'path': '@fx/colors', 'value': 1}},
            {'command': 'prop.set', 'params': {'layer': camada, 'path': '@fx/colorA', 'value': LARANJA}},
            {'command': 'prop.set', 'params': {'layer': camada, 'path': '@fx/colorB', 'value': VERMELHO}}]


# O anel de pressão de ar visto de 3/4: o anel fica de pé, atravessado na linha do golpe, então
# na tela é uma elipse estreita (ACHATA da altura), um pouco inclinada, que avança na direção do
# golpe enquanto abre. A metade de trás passa atrás da estrela e a da frente por cima dela.
ACHATA = 0.45
INCLINA = -12                   # graus (a mesma inclinação do jato de cima)
AVANCO = [0, 10, 20, 28, 35, 40, 44, 47, 49]
RAIOS = [80, 132, 166, 190, 204, 213, 219, 223, 225]
LARGURAS = [9, 8, 7, 6, 5, 4, 3, 2, 1]
OPAC = [100, 96, 88, 74, 56, 36, 16, 0, 0]


def anel_de_ar(nome, frente):
    """Pressão de ar, não fumaça: traço fino, nítido e branco, com uma segunda linha mais fraca
    logo dentro (a linha dupla da onda de choque, pelo Repeater), uma bolha quase transparente de
    borda limpa e brilho ciano curto. Nasce no desenho 1, abre rápido e freia, afina e se parte em
    arcos de borda dura por erosão (Fractal Noise em "Stencil Luma" na própria camada). O traço muda no tamanho da elipse, não
    na escala da camada, para afinar enquanto abre."""
    antes = [None]
    s = [{'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': nome, 'size': [140, 140],
                                                  'stroke': '#ffffff', 'strokeWidth': 8, 'fill': '#dff6ff'}},
         {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/group/contents/fill/opacity', 'value': 7}},
         {'command': 'layer.addShapeItem', 'params': {'layer': nome, 'kind': 'repeater'}},
         {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/repeater/copies', 'value': 2}},
         {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/repeater/transform/position', 'value': [0, 0]}},
         {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/repeater/transform/scale', 'value': [86, 90]}},
         {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/repeater/transform/endOpacity', 'value': 40}},
         {'command': 'prop.set', 'params': {'layer': nome, 'path': 'transform/rotation', 'value': INCLINA}}]
    s += chaves(nome, 'transform/position', [[CENTRO[0] + d, CENTRO[1], 0] for d in AVANCO])
    s += chaves(nome, 'contents/group/contents/ellipse/size', antes + [[2 * r * ACHATA, 2 * r] for r in RAIOS])
    s += chaves(nome, 'contents/group/contents/stroke/width', antes + LARGURAS)
    s += chaves(nome, 'transform/opacity', [0] + OPAC)
    # A bolha só existe enquanto o anel está inteiro: na erosão ela ficaria manchada (fumaça).
    s += chaves(nome, 'contents/group/contents/fill/opacity', [0, 8, 6, 0])
    if frente:
        # Só a metade de cá (o lado direito, no espaço da camada: a máscara gira junto). Na camada
        # de forma a máscara conta a partir do centro do conteúdo (0, 0), não do canto.
        s += [{'command': 'layer.addMask', 'params': {'layer': nome, 'shape': 'rect',
                                                      'rect': [0, -ALTO, LARGO, 2 * ALTO]}},
              {'command': 'layer.mask.set', 'params': {'layer': nome, 'mask': 1, 'field': 'feather', 'value': 8}}]
    s += [{'command': 'effect.apply', 'params': {'effect': 'Turbulent Displace', 'layers': [nome]}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/amount', 'value': 3}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/size', 'value': 70}}]
    s += chaves(nome, '@fx/evolution', antes + [25 * i for i in range(9)])
    # A erosão dentro da própria camada: Fractal Noise de borda dura no modo "Stencil Luma" usa o
    # ruído como recorte só deste anel; o brilho do ruído cai a cada desenho e os buracos crescem.
    # (Com track matte o EffectCraft 0.23.1 abria o buraco também nas camadas de baixo.) A mesma
    # semente e a mesma posição nas duas metades: os buracos batem na emenda.
    s += [{'command': 'effect.apply', 'params': {'effect': 'Fractal Noise', 'layers': [nome]}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/contrast', 'value': 700}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/overflow', 'value': 0}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/transform/scale', 'value': 45}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/complexity', 'value': 3}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/evolutionOptions/seed', 'value': 5}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/blendingMode', 'value': 19}}]
    s += chaves(nome, '@fx/brightness', [100, 100, 100, 70, 40, 12, -20, -55, -90, -100])
    s += [{'command': 'effect.apply', 'params': {'effect': 'Gaussian Blur', 'layers': [nome]}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/blurriness', 'value': 0.6}},
          {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': [nome]}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/threshold', 'value': 50}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/radius', 'value': 7}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/intensity', 'value': 0.7}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/colors', 'value': 1}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/colorA', 'value': '#bff0ff'}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': '@fx/colorB', 'value': '#5cc8ff'}}]
    return s


def particulas_das_pontas(contorno):
    sorte = random.Random(11)
    s = camada_forma('faiscas_pontas')
    n = 0
    pontas = contorno[0::2]                 # o contorno alterna ponta e vale
    for px, py in pontas:
        comp_espinho = math.hypot(px, py)
        ang0 = math.degrees(math.atan2(py, px))
        quantas = 3 if comp_espinho > 120 else 2 if comp_espinho > 80 else 1
        for _ in range(quantas):
            ang = ang0 + sorte.uniform(-9, 9)
            a = math.radians(ang)
            ux, uy = math.cos(a), math.sin(a)
            nasce = sorte.choice([0, 0, 1])
            v = sorte.uniform(20, 34) * (1.15 if comp_espinho > 120 else 1.0)
            comp = sorte.uniform(20, 36)
            inicio = comp_espinho * sorte.uniform(0.85, 1.0)
            n += 1
            s += grupo('faiscas_pontas', n, losango(comp, max(3.4, comp / 6), ang, atras=comp * 0.6),
                       '#ffe27a', pos=(inicio * ux, inicio * uy))
            pos, opac, esc = [], [], []
            for q in range(QUADROS):
                k = max(0, q - nasce)
                d = inicio + v * (1 - 0.72 ** k) / 0.28
                pos.append([d * ux, d * uy])
                fim = 6 + nasce + sorte.choice([0, 1])
                opac.append(0 if q < nasce else 100 if q < fim - 1 else 55 if q < fim else 0)
                e = max(25, 100 - 13 * k)
                esc.append([e, e])
            s += chaves('faiscas_pontas', f'@g{n}/transform/position', pos)
            s += chaves('faiscas_pontas', f'@g{n}/transform/opacity', opac)
            s += chaves('faiscas_pontas', f'@g{n}/transform/scale', esc)
    s += halo('faiscas_pontas', 10, 1.4)
    return s


def passos():
    sorte = random.Random(7)
    s = [{'command': 'comp.new', 'params': {'name': 'impacto_forte_ec', 'width': LARGO, 'height': ALTO,
                                            'frameRate': FPS, 'duration': QUADROS / FPS, 'background': [0, 0, 0]}}]

    # Mancha laranja atrás (o brilho difuso em volta de tudo na referência).
    s += [{'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': 'mancha', 'size': [440, 230], 'fill': '#ff6a0d'}},
          {'command': 'prop.set', 'params': {'layer': 'mancha', 'path': 'transform/position', 'value': [CENTRO[0] + 120, CENTRO[1], 0]}},
          {'command': 'effect.apply', 'params': {'effect': 'Gaussian Blur', 'layers': ['mancha']}},
          {'command': 'prop.set', 'params': {'layer': 'mancha', 'path': 'effects/#1/blurriness', 'value': 60}}]
    s += chaves('mancha', 'transform/opacity', [50, 50, 42, 34, 26, 18, 10, 5, 0])
    s += chaves('mancha', 'transform/scale', [[v, v, 100] for v in (100, 98, 90, 78, 64, 50, 36, 22, 10)])

    # Brasas: losangos alongados espalhados sobretudo para a direita; voam para a direita e freiam.
    s += camada_forma('brasas')
    n = 0
    for _ in range(14):
        x = sorte.uniform(40, 420) if sorte.random() < 0.8 else sorte.uniform(-140, 40)
        y = sorte.uniform(-140, 140) * (0.55 if x > 200 else 1.0)
        comp = sorte.uniform(10, 46)
        ang = sorte.uniform(-18, 18) + (10 if y > 0 else -10)
        fora = losango(comp, comp / 8, ang, atras=comp * 0.5)
        n += 1
        s += grupo('brasas', n, fora, '#ffa21a', pos=(x, y))
        voo = sorte.uniform(14, 26)
        posicoes = [[x + voo * (1 - 0.8 ** q) / 0.2, y + y * 0.02 * q] for q in range(QUADROS)]
        s += chaves('brasas', f'@g{n}/transform/position', posicoes)
        nasce = 0 if x < 260 else sorte.choice([0, 1, 2])
        apaga = sorte.randint(8, 11)
        opac = [0 if q < nasce else 100 if q < apaga - 2 else 60 if q < apaga else 0 for q in range(QUADROS)]
        s += chaves('brasas', f'@g{n}/transform/opacity', opac)
    # Pedaços que se soltam da ponta dos jatos no desenho 2 e seguem voando.
    for ang, comp in ((-14, 330), (0, 360), (20, 220)):
        a = math.radians(ang)
        x, y = comp * math.cos(a), comp * math.sin(a)
        n += 1
        s += grupo('brasas', n, losango(80, 11, ang, atras=40), '#ffb21a', pos=(x, y))
        s += chaves('brasas', f'@g{n}/transform/position',
                    [[x + 30 * (1 - 0.75 ** max(0, q - 2)) / 0.25 * math.cos(a), y + 30 * (1 - 0.75 ** max(0, q - 2)) / 0.25 * math.sin(a)]
                     for q in range(QUADROS)])
        s += chaves('brasas', f'@g{n}/transform/opacity', [0, 0, 100, 100, 100, 90, 75, 55, 35, 0])
        s += chaves('brasas', f'@g{n}/transform/scale', [[v, v] for v in (100, 100, 100, 92, 82, 70, 58, 46, 34, 20)])
    s += halo('brasas', 12, 1.6)

    # Riscos escuros finos em volta da estrela (só no começo).
    s += camada_forma('riscos_escuros')
    for k in range(9):
        ang = sorte.uniform(-170, 170)
        comp = sorte.uniform(70, 130)
        s += grupo('riscos_escuros', k + 1, losango(comp, 3.2, ang, atras=-comp * 0.45), '#5a160c')
    s += chaves('riscos_escuros', 'transform/opacity', [90, 90, 70, 45, 20, 0])
    s += chaves('riscos_escuros', 'transform/scale', [[s_, s_, 100] for s_ in (100, 104, 108, 112, 116, 120)])

    s += anel_de_ar('pressao', frente=False)

    # Jatos: três pontas longas na direção do golpe, nas faixas.
    jatos = [(-14, 410, 40), (0, 425, 34), (20, 270, 32), (-32, 190, 26), (38, 165, 22)]
    s += camada_forma('jatos')
    n = 0
    # O primeiro grupo da lista fica na frente (como no After Effects): do miolo para fora.
    for cor, k in reversed(FAIXAS[:4]):
        for ang, comp, larg in jatos:
            n += 1
            s += grupo('jatos', n, losango(comp * (0.55 + 0.45 * k), larg * k, ang, atras=10), cor)
    s += chaves('jatos', 'transform/scale', [[v, 100 * (0.6 + 0.4 * v / 100), 100] for v in
                                              (100, 92, 80, 64, 46, 30, 16, 6, 0)])
    s += halo('jatos', 30, 1.2)

    # Estrela: o mesmo contorno em faixas, encolhendo até sumir no desenho 8. Em duas camadas: a
    # borda (vermelho-escuro, laranja e amarelo) sem o lado direito perto do miolo, para o miolo
    # escorrer direto para os jatos (pedido de 10/11: "tirar o contorno da direita da estrela na
    # parte do miolo"), e o miolo (amarelo-claro e branco) inteiro por cima. A máscara conta a
    # partir do centro do desenho e gira junto com a estrela.
    contorno = estrela(sorte)
    for nome, faixas in (('estrela', FAIXAS[:3]), ('estrela_miolo', FAIXAS[3:])):
        s += camada_forma(nome)
        for i, (cor, k) in enumerate(reversed(faixas)):
            s += grupo(nome, i + 1, contorno, cor, escala=100 * k)
        s += chaves(nome, 'transform/scale', [[v, v, 100] for v in (100, 97, 88, 72, 55, 40, 26, 13, 0)])
        s += chaves(nome, 'transform/rotation', [0, 3, 6, 9, 12, 15, 18, 21, 24])
        if nome == 'estrela':
            s += [{'command': 'layer.addMask', 'params': {'layer': nome, 'shape': 'rect', 'rect': [-LARGO, -ALTO, 2 * LARGO, 2 * ALTO]}},
                  {'command': 'layer.addMask', 'params': {'layer': nome, 'shape': 'rect', 'rect': [14, -48, 170, 96]}},
                  {'command': 'layer.mask.mode', 'params': {'layer': nome, 'mask': 2, 'mode': 'Subtract'}},
                  {'command': 'layer.mask.set', 'params': {'layer': nome, 'mask': 2, 'field': 'feather', 'value': 16}}]
            s += halo(nome, 45, 1.4)

    # Faíscas saindo das pontas da estrela (pedido de 10/11: "mais partícula saindo do final da
    # estrela"): de cada ponta saem faíscas finas na direção do espinho, rápidas e freando
    # (Ogawa), que encolhem e apagam. Duas ou três por espinho grande, uma por espinho pequeno.
    s += particulas_das_pontas(contorno)

    # Núcleo: luz branca macia no miolo.
    s += [{'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': 'nucleo', 'size': [44, 40], 'fill': '#ffffff'}},
          {'command': 'prop.set', 'params': {'layer': 'nucleo', 'path': 'transform/position', 'value': [*CENTRO, 0]}},
          {'command': 'effect.apply', 'params': {'effect': 'Gaussian Blur', 'layers': ['nucleo']}},
          {'command': 'prop.set', 'params': {'layer': 'nucleo', 'path': 'effects/#1/blurriness', 'value': 12}}]
    s += chaves('nucleo', 'transform/scale', [[v, v, 100] for v in (110, 100, 85, 68, 50, 35, 20, 8, 0)])

    # A metade da frente do anel passa por cima da estrela (a de trás ficou atrás dos jatos).
    s += anel_de_ar('pressao_frente', frente=True)
    return resolver(s)


def resolver(passos):
    """'@gN' vira o caminho do N-ésimo grupo da camada (contents/group, contents/group#2, ...) e '@fx' o
    do último efeito aplicado nela."""
    efeitos, grupos = {}, {}
    saida = []
    for p in passos:
        par = p['params']
        if p['command'] == 'effect.apply':
            for c in par['layers']:
                efeitos[c] = efeitos.get(c, 0) + 1
        if p['command'] == 'engine.batch':
            passo = par['steps'][0]
            c = passo['params']['layer']
            grupos[c] = grupos.get(c, 0) + 1
            saida.append({'command': 'layer.addShapeItem', 'params': passo['params']})
            continue
        camada = par.get('layer')
        for chave in ('path', 'group'):
            v = par.get(chave)
            if isinstance(v, str) and v.startswith('@g'):
                num, _, resto = v[2:].partition('/')
                nome = 'contents/group' if num == '1' else f'contents/group#{num}'
                par[chave] = nome + ('/' + resto if resto else '')
            elif isinstance(v, str) and v.startswith('@fx/'):
                par[chave] = f'effects/#{efeitos[camada]}/' + v[4:]
        saida.append(p)
    return saida


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True, help='pasta da sequência PNG (fora do repositório)')
    p.add_argument('--passos', type=Path, help='só grava a lista de comandos neste arquivo')
    a = p.parse_args()
    lista_passos = passos()
    if a.passos:
        a.passos.write_text(json.dumps(lista_passos, indent=1))
        return
    a.saida.mkdir(parents=True, exist_ok=True)
    for velho in a.saida.glob('impacto_forte_ec_*.png'):
        velho.unlink()
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(lista_passos, f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(PROJETO), '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / 'impacto_forte_ec_[####].png'
    subprocess.run([str(a.cli), 'run', str(PROJETO),
                    'renderQueue.add', json.dumps({'comp': 'impacto_forte_ec', 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', PROJETO)
    print('quadros:', len(list(a.saida.glob('impacto_forte_ec_*.png'))))


if __name__ == '__main__':
    main()
