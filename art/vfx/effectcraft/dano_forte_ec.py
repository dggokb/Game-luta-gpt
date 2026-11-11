#!/usr/bin/env python3
"""A faísca do golpe forte pela receita "quadros gerados + acabamento" (relatório
reports/Pipeline profissional de VFX anime.md, recomendação 1): nada é desenhado no programa.

O desenho é o vídeo do dono (art/vfx/dano/dano_forte.mp4, 1344 x 768, 24 por segundo, já "de dois
em dois"). O EffectCraft faz só o que o compositor de anime faz:
- **Tempo**: remapeia o vídeo em degraus de 12 por segundo e segura o desenho do impacto por dois
  desenhos (expressão no Time Remap), em vez de tocar o vídeo inteiro;
- **Faixas chapadas**: Colorama com paleta própria e sem interpolar troca o degradê macio do vídeo
  por faixas de anime (núcleo branco, amarelo-claro, amarelo, laranja, laranja-avermelhado); o
  halo marrom e fraco do vídeo vira transparente;
- **Fundo**: Unmult tira o preto (a luz vira alfa);
- **Brilho**: Glow em cores A e B (laranja -> vermelho) refaz o halo, limpo;
- **Erosão**: Fractal Noise de borda dura em Stencil Luma come o corpo nos últimos desenhos, em
  pedaços, em vez de o efeito só apagar.

    python3 art/vfx/effectcraft/dano_forte_ec.py --saida PASTA [--cli CAMINHO]

Sai: art/vfx/effectcraft/dano_forte_ec.ecproj e PASTA/dano_forte_ec_0000.png ... (12 desenhos).
Com --anel: o anel de pressão de ar em 3D atravessando a faísca (dano_forte_anel_ec; a tela cresce
160 px em cima e embaixo, então o contato vai para 427,543).
No jogo (mesmo ponto e escala da impacto_ar_forte de hoje):
    efeito_video.py PASTA --nome dano_forte_ec --fps 8 --origem 427,384 --escala-mundo 0.30 --borda 50
"""
import argparse
import random
import json
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
VIDEO = AQUI.parent / 'dano' / 'dano_forte.mp4'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

LARGO, ALTO, FPS_VIDEO = 1344, 768, 24
MIOLO = (427.0, 383.0)              # o miolo branco do vídeo (o contato da folha de hoje)
FPS = 12                                  # um desenho por quadro da composição
# Quadro do vídeo mostrado em cada desenho: o impacto (0) segurado por dois desenhos, depois o
# vídeo de dois em dois e, no fim, mais espaçado (a cauda de brasas).
DESENHOS = [0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10]   # segura o auge até a metade
# Erosão por desenho (brilho do ruído com contraste 400: de 250 para cima não come nada; quanto
# menor, maiores os buracos).
EROSAO = [300, 300, 300, 300, 260, 210, 160, 115, 75, 35, -5, -50]
# Faixas por intensidade (0 a 1), medidas no vídeo: o halo macio vai até ~0,31 e o desenho
# (jatos, espinhos) fica de 0,4 para cima. Abaixo de 0,36 vira preto (transparente: o Glow refaz
# um halo limpo); depois, do laranja-avermelhado da borda ao branco do núcleo. Sem interpolar =
# faixas chapadas.
PALETA = ('0:0,0,0 0.36:0.86,0.24,0.04 0.48:1,0.46,0.06 0.62:1,0.70,0.14 '
          '0.76:1,0.88,0.40 0.90:1,0.98,0.85')


def chaves(camada, caminho, valores):
    return [{'command': 'prop.addKey', 'params': {'layer': camada, 'path': caminho, 'time': q / FPS, 'value': v}}
            for q, v in enumerate(valores)]


def passos(anel=False):
    nome = 'dano_forte_anel_ec' if anel else 'dano_forte_ec'
    folga = FOLGA_ANEL if anel else 0
    s = [
        {'command': 'comp.new', 'params': {'name': nome, 'width': LARGO, 'height': ALTO + 2 * folga,
                                           'frameRate': FPS, 'duration': len(DESENHOS) / FPS, 'background': [0, 0, 0]}},
        {'command': 'file.import', 'params': {'paths': [str(VIDEO)]}},
        {'command': 'engine.batch', 'params': {'steps': [
            {'command': 'layer.addItem', 'params': {'item': VIDEO.name}},
            {'command': 'layer.rename', 'params': {'layer': '$1.layer', 'name': 'dano'}},
        ]}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'transform/anchor', 'value': [*MIOLO, 0]}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'transform/position', 'value': [MIOLO[0], MIOLO[1] + folga, 0]}},
        {'command': 'layer.enableTimeRemap', 'params': {'layers': ['dano']}},
        {'command': 'prop.setExpression', 'params': {'layer': 'dano', 'path': 'timeRemap', 'expression':
            f'var d = {json.dumps(DESENHOS)}; '
            f'var i = Math.min(Math.floor(time * {FPS} + 0.001), d.length - 1); '
            f'd[i] / {FPS_VIDEO} + 0.001;'}},
        # Faixas chapadas sobre a luz do vídeo (ainda no fundo preto).
        {'command': 'effect.apply', 'params': {'effect': 'Colorama', 'layers': ['dano']}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#1/outputCycle/usePresetPalette', 'value': 8}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#1/outputCycle/palette', 'value': PALETA}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#1/outputCycle/interpolatePalette', 'value': False}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#1/outputCycle/cycles', 'value': 0.999}},
        # O preto vira transparência.
        {'command': 'effect.apply', 'params': {'effect': 'Unmult', 'layers': ['dano']}},
        # Halo limpo, do laranja ao vermelho.
        {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': ['dano']}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#3/threshold', 'value': 45}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#3/radius', 'value': 36}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#3/intensity', 'value': 0.9}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#3/colors', 'value': 1}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#3/colorA', 'value': '#ff8a1f'}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#3/colorB', 'value': '#c2280c'}},
        # Erosão de borda dura no fim (o ruído fica parado: os buracos só crescem).
        {'command': 'effect.apply', 'params': {'effect': 'Fractal Noise', 'layers': ['dano']}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#4/contrast', 'value': 400}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#4/overflow', 'value': 0}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#4/transform/scale', 'value': 70}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#4/complexity', 'value': 3}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#4/evolutionOptions/seed', 'value': 9}},
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'effects/#4/blendingMode', 'value': 19}},
    ]
    s += chaves('dano', 'effects/#4/brightness', EROSAO)
    # O miolo apaga quando os braços se soltam em lascas (desenho 4): quem leva o resto são elas.
    s += chaves('dano', 'transform/opacity', [100, 100, 100, 100, 100, 100, 70, 20, 0, 0, 0, 0])
    # Cresce em 2 desenhos a partir do contato (no Tōkon nasce pequena no 125 e enche no 127).
    s += chaves('dano', 'transform/scale', [[e, e, 100] for e in (45, 75, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100)])
    if anel:
        # A faísca vira um plano 3D em z = 0 (sem câmera, o 3D enquadra igual ao 2D) e o anel a
        # atravessa: a metade da frente passa por cima dela e a de trás por baixo, sozinhas.
        s += [{'command': 'layer.setSwitch', 'params': {'layers': ['dano'], 'switch': 'threeD', 'value': True}}]
        s += anel_de_ar((MIOLO[0], MIOLO[1] + folga))
        s += lascas((MIOLO[0], MIOLO[1] + folga))
    return s


# A dissipação (pedido de 10/11, vendo o Tōkon: "lá ele se expande dissipando em partículas"):
# enquanto a faísca do vídeo se apaga, ela solta um estouro de fagulhas que se abre para fora.
# Técnica de faísca de impacto (Ogawa: partículas esticadas em fio, rápidas e freando): Particle
# Emitter 2D com partículas tipo linha (já nascem esticadas na direção em que voam), sem gravidade,
# com resistência, do amarelo-claro ao laranja, em modo Add, e Glow A/B por cima. A área que emite
# é a do corpo da faísca, deslocada para o lado do golpe; o estouro vai para todos os lados (como na
# referência do Tōkon). Mistura normal, não Add: em Add as fagulhas somavam numa bola branca.
FAGULHAS_INICIO, FAGULHAS_FIM = 3, 6          # desenhos em que o emissor solta (o resto só voa)


def lascas(centro):
    """A dissipação medida no Tōkon (quadros 127-131 da referência do dono): quando o miolo apaga,
    os braços pontudos da explosão se soltam e viram raios em zigue-zague (amarelo com a ponta
    laranja) que continuam voando na direção em que apontavam, afinam e somem em 3 ou 4 quadros.
    Aqui: um traço reto por braço, quebrado em raio pelo operador Zig Zag, afinando nas duas
    pontas (taper), nascendo na ponta do braço do vídeo e voando para fora, freando."""
    import math
    n = 'lascas'
    # Os braços do vídeo dano_forte (ângulo, comprimento em px do vídeo), medidos na folha de hoje.
    # Só os braços principais (no Tōkon são 6 ou 7 lascas grandes, onde estavam as pontas).
    bracos = [(-14, 900), (0, 940), (20, 600), (-32, 420), (38, 370), (-78, 380), (62, 310)]
    sorte = random.Random(13)
    s = [{'command': 'layer.newShape', 'params': {'kind': 'none', 'name': n}},
         {'command': 'prop.set', 'params': {'layer': n, 'path': 'transform/position', 'value': [0, 0, 0]}}]
    for i, (ang, comp) in enumerate(bracos, start=1):
        g = 'contents/group' if i == 1 else f'contents/group#{i}'
        a = math.radians(ang)
        ux, uy = math.cos(a), math.sin(a)
        tam = min(380, 90 + comp * 0.32) * sorte.uniform(0.85, 1.15)
        larg = 16 + tam * 0.08
        x0, y0 = centro[0] + ux * comp * 0.72, centro[1] + uy * comp * 0.72
        s += [{'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'group'}},
              {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'path', 'group': g}},
              {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'zigzag', 'group': g}},
              {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'stroke', 'group': g}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/path/path', 'value': {
                  'closed': False, 'vertices': [[-tam / 2 * ux, -tam / 2 * uy], [tam / 2 * ux, tam / 2 * uy]],
                  'in_tangents': [[0, 0], [0, 0]], 'out_tangents': [[0, 0], [0, 0]]}}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/zigzag/size', 'value': larg * 0.9}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/zigzag/ridges', 'value': sorte.choice([2, 3, 3, 4])}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/zigzag/points', 'value': 0}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke/color', 'value': '#ffd43b'}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke/width', 'value': larg}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke/join', 'value': 0}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke/taper/units', 'value': 1}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke/taper/startLength', 'value': 45}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke/taper/endLength', 'value': 30}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke/taper/startWidth', 'value': 0}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke/taper/endWidth', 'value': 0}},
              # A borda laranja: um segundo traço, mais largo, por baixo (o primeiro da lista fica na frente).
              {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'stroke', 'group': g}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke#2/color', 'value': '#ff7a14'}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke#2/width', 'value': larg * 1.55}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke#2/join', 'value': 0}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke#2/taper/units', 'value': 1}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke#2/taper/startLength', 'value': 20}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke#2/taper/endLength', 'value': 55}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke#2/taper/startWidth', 'value': 0}},
              {'command': 'prop.set', 'params': {'layer': n, 'path': f'{g}/contents/stroke#2/taper/endWidth', 'value': 0}}]
        voo = comp * 0.30 + 60
        pos, esc, opac = [], [], []
        for q in range(len(DESENHOS)):
            k = q - LASCAS_INICIO
            d = 0 if k < 0 else voo * (1 - 0.55 ** (k + 1))
            pos.append([x0 + ux * d, y0 + uy * d])
            e = 0 if k < 0 else [100, 85, 60, 32, 0][min(k, 4)]
            esc.append([100, e])                              # afina (a largura cai, o comprimento fica)
            opac.append(0 if k < 0 or k >= 4 else 100)
        s += [{'command': 'prop.addKey', 'params': {'layer': n, 'path': f'{g}/transform/position', 'time': q / FPS, 'value': v}}
              for q, v in enumerate(pos)]
        s += [{'command': 'prop.addKey', 'params': {'layer': n, 'path': f'{g}/transform/scale', 'time': q / FPS, 'value': v}}
              for q, v in enumerate(esc)]
        s += [{'command': 'prop.addKey', 'params': {'layer': n, 'path': f'{g}/transform/opacity', 'time': q / FPS, 'value': v}}
              for q, v in enumerate(opac)]
    # Ponta laranja e brilho: Glow com cores A e B.
    s += [{'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': [n]}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#1/threshold', 'value': 40}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#1/radius', 'value': 14}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#1/intensity', 'value': 1.1}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#1/colors', 'value': 1}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#1/colorA', 'value': '#ff9a1f'}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#1/colorB', 'value': '#e0400c'}}]
    return s


LASCAS_INICIO = 6         # metade do efeito, como no Tōkon (quadro 129 de 125-132)


def fagulhas(centro):
    n = 'fagulhas'
    P = 'effects/#1/'

    def ps(k, v):
        return {'command': 'prop.set', 'params': {'layer': n, 'path': P + k, 'value': v}}
    s = [{'command': 'layer.newSolid', 'params': {'name': n, 'color': '#000000', 'width': LARGO, 'height': ALTO + 2 * FOLGA_ANEL}},
         {'command': 'effect.apply', 'params': {'effect': 'Particle Emitter 2D', 'layers': [n]}},
         ps('producer/position', [centro[0] + 90, centro[1]]), ps('producer/radiusX', 110), ps('producer/radiusY', 30),
         ps('physics/animation', 0), ps('physics/velocity', 6), ps('physics/gravity', 0), ps('physics/resistance', 1.5),
         ps('particle/particleType', 0), ps('particle/birthSize', 3.0), ps('particle/deathSize', 0.8),
         ps('particle/birthColor', '#fff2a8'), ps('particle/deathColor', '#ff5a10'), ps('particle/maxOpacity', 90),
         ps('particle/transferMode', 0), ps('longevity', 0.45), ps('randomSeed', 3)]
    taxa = [0] * FAGULHAS_INICIO + [12] * (FAGULHAS_FIM - FAGULHAS_INICIO) + [0] * (len(DESENHOS) - FAGULHAS_FIM)
    s += [{'command': 'prop.addKey', 'params': {'layer': n, 'path': P + 'birthRate', 'time': q / FPS, 'value': v}}
          for q, v in enumerate(taxa)]
    s += [{'command': 'keys.select', 'params': {'keys': [{'layer': n, 'path': P + 'birthRate', 'time': q / FPS} for q in range(len(taxa))]}},
          {'command': 'keys.interpolation', 'params': {'interpolation': 'hold'}},
          {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': [n]}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/threshold', 'value': 35}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/radius', 'value': 12}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/colors', 'value': 1}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/colorA', 'value': '#ff8a1f'}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/colorB', 'value': '#c2280c'}}]
    return s


# O anel de pressão de ar (o do impacto_forte_ec, refeito em 3D de verdade): fino, nítido e
# branco, com uma segunda linha mais fraca logo dentro (Repeater), uma bolha quase transparente só
# no começo, brilho ciano curto; de pé, atravessado na linha do golpe, visto de 3/4 (giro em Y),
# nasce no desenho 1, abre rápido e freia, avança na direção do golpe e se parte em arcos por
# erosão (Fractal Noise em Stencil Luma na própria camada). Medidas em px do vídeo (0,30 px do
# mundo cada): raio de 100 a 470 (30 a 141 no jogo).
FOLGA_ANEL = 160                    # px a mais em cima e embaixo para o anel caber
ANEL_GIRO = (-10, 66)               # rotação X e Y, em graus (3/4)
ANEL_RAIOS = [100, 200, 280, 340, 385, 420, 445, 462, 470]
ANEL_TRACOS = [22, 19, 16, 13, 10, 7, 5, 3, 2]
ANEL_OPAC = [100, 96, 88, 74, 56, 36, 18, 6, 0]
ANEL_AVANCO = [0, 30, 55, 75, 90, 100, 107, 111, 113]
ANEL_EROSAO = [300, 280, 200, 130, 70, 20, -30, -70, -100]


def anel_de_ar(centro):
    n = 'anel'
    antes = [None]

    def ch(caminho, valores):
        return [{'command': 'prop.addKey', 'params': {'layer': n, 'path': caminho, 'time': q / FPS, 'value': v}}
                for q, v in enumerate(valores) if v is not None]

    s = [{'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': n, 'size': [200, 200],
                                                  'stroke': '#ffffff', 'strokeWidth': 20, 'fill': '#dff6ff'}},
         {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'repeater'}},
         {'command': 'prop.set', 'params': {'layer': n, 'path': 'contents/repeater/copies', 'value': 2}},
         {'command': 'prop.set', 'params': {'layer': n, 'path': 'contents/repeater/transform/position', 'value': [0, 0]}},
         {'command': 'prop.set', 'params': {'layer': n, 'path': 'contents/repeater/transform/scale', 'value': [88, 88]}},
         {'command': 'prop.set', 'params': {'layer': n, 'path': 'contents/repeater/transform/endOpacity', 'value': 40}},
         {'command': 'layer.setSwitch', 'params': {'layers': [n], 'switch': 'threeD', 'value': True}},
         {'command': 'prop.set', 'params': {'layer': n, 'path': 'transform/rotationX', 'value': ANEL_GIRO[0]}},
         {'command': 'prop.set', 'params': {'layer': n, 'path': 'transform/rotationY', 'value': ANEL_GIRO[1]}}]
    s += ch('transform/position', [[centro[0] + d, centro[1], 0] for d in ANEL_AVANCO])
    s += ch('contents/group/contents/ellipse/size', antes + [[2 * r, 2 * r] for r in ANEL_RAIOS])
    s += ch('contents/group/contents/stroke/width', antes + ANEL_TRACOS)
    s += ch('transform/opacity', [0] + ANEL_OPAC)
    # A bolha só enquanto o anel está inteiro (na erosão ela ficaria manchada).
    s += ch('contents/group/contents/fill/opacity', [0, 8, 6, 0])
    s += [{'command': 'effect.apply', 'params': {'effect': 'Turbulent Displace', 'layers': [n]}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#1/amount', 'value': 4}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#1/size', 'value': 90}},
          {'command': 'effect.apply', 'params': {'effect': 'Fractal Noise', 'layers': [n]}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/contrast', 'value': 400}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/overflow', 'value': 0}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/transform/scale', 'value': 80}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/complexity', 'value': 3}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/evolutionOptions/seed', 'value': 5}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#2/blendingMode', 'value': 19}}]
    s += ch('effects/#1/evolution', antes + [25 * i for i in range(len(ANEL_RAIOS))])
    s += ch('effects/#2/brightness', [300] + ANEL_EROSAO)
    s += [{'command': 'effect.apply', 'params': {'effect': 'Gaussian Blur', 'layers': [n]}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#3/blurriness', 'value': 1.2}},
          {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': [n]}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#4/threshold', 'value': 50}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#4/radius', 'value': 18}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#4/intensity', 'value': 0.7}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#4/colors', 'value': 1}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#4/colorA', 'value': '#bff0ff'}},
          {'command': 'prop.set', 'params': {'layer': n, 'path': 'effects/#4/colorB', 'value': '#5cc8ff'}}]
    return s


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True, help='pasta da sequência PNG (fora do repositório)')
    p.add_argument('--anel', action='store_true', help='com o anel de pressão de ar em 3D (dano_forte_anel_ec)')
    a = p.parse_args()
    nome = 'dano_forte_anel_ec' if a.anel else 'dano_forte_ec'
    projeto = AQUI / f'{nome}.ecproj'
    a.saida.mkdir(parents=True, exist_ok=True)
    for velho in a.saida.glob(f'{nome}_*.png'):
        velho.unlink()
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(passos(a.anel), f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(projeto), '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / f'{nome}_[####].png'
    subprocess.run([str(a.cli), 'run', str(projeto),
                    'renderQueue.add', json.dumps({'comp': nome, 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', projeto)
    print('quadros:', len(list(a.saida.glob(f'{nome}_*.png'))))


if __name__ == '__main__':
    main()
