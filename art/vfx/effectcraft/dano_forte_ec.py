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
import json
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
VIDEO = AQUI.parent / 'dano' / 'dano_forte.mp4'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

LARGO, ALTO, FPS_VIDEO = 1344, 768, 24
FPS = 12                                  # um desenho por quadro da composição
# Quadro do vídeo mostrado em cada desenho: o impacto (0) segurado por dois desenhos, depois o
# vídeo de dois em dois e, no fim, mais espaçado (a cauda de brasas).
DESENHOS = [0, 0, 2, 4, 6, 8, 10, 12, 14, 17, 20, 24]
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
        {'command': 'prop.set', 'params': {'layer': 'dano', 'path': 'transform/position', 'value': [LARGO / 2, ALTO / 2 + folga, 0]}},
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
    if anel:
        # A faísca vira um plano 3D em z = 0 (sem câmera, o 3D enquadra igual ao 2D) e o anel a
        # atravessa: a metade da frente passa por cima dela e a de trás por baixo, sozinhas.
        s += [{'command': 'layer.setSwitch', 'params': {'layers': ['dano'], 'switch': 'threeD', 'value': True}}]
        s += anel_de_ar((MIOLO[0], MIOLO[1] + folga))
    return s


# O anel de pressão de ar (o do impacto_forte_ec, refeito em 3D de verdade): fino, nítido e
# branco, com uma segunda linha mais fraca logo dentro (Repeater), uma bolha quase transparente só
# no começo, brilho ciano curto; de pé, atravessado na linha do golpe, visto de 3/4 (giro em Y),
# nasce no desenho 1, abre rápido e freia, avança na direção do golpe e se parte em arcos por
# erosão (Fractal Noise em Stencil Luma na própria camada). Medidas em px do vídeo (0,30 px do
# mundo cada): raio de 100 a 470 (30 a 141 no jogo).
FOLGA_ANEL = 160                    # px a mais em cima e embaixo para o anel caber
MIOLO = (427.0, 383.0)              # o miolo branco do vídeo (o contato da folha de hoje)
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
