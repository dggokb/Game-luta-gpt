#!/usr/bin/env python3
"""Faísca de defesa pela técnica do Marvel Tōkon: um disco de energia 3D em pé, visto de 3/4,
que vira um anel fino e se abre. Estudo da referência do dono (11/11, quadros 77 a 83, ~250 ms,
~24 por segundo):
- q0: clarão pequeno ciano e branco no ponto do golpe;
- q1: o clarão abre num disco oval em pé (de frente para quem bate), azul com miolo branco e
  riscos de energia;
- q2-q3: o disco inteiro, a borda ciano clara;
- q4-q6: o disco some e fica só o anel fino, que abre e apaga.

Técnica (sem forma desenhada à mão): sólido com máscara oval + Gradient Ramp radial (miolo claro)
multiplicado por Fractal Noise (os riscos), cortado em faixas de azul a branco pelo Colorama, em
3D girado 62° em Y; anel = elipse 3D com traço, na mesma rotação; Glow ciano por cima.

    python3 art/vfx/effectcraft/defesa3d_ec.py --saida PASTA [--cli CAMINHO]

No jogo (o contato é a origem; o escudo fica de frente para quem bate):
    efeito_video.py PASTA --nome defesa3d_ec --fps 24 --origem 300,300 --escala-mundo 0.55
"""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
PROJETO = AQUI / 'defesa3d_ec.ecproj'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

FPS = 24
DESENHOS = 8
LADO = 600
C = (300.0, 300.0)
GIRO_Y = 68
PALETA = '0:0.10,0.40,0.95 0.18:0.25,0.65,1 0.38:0.55,0.90,1 0.62:0.90,0.99,1'

# Por desenho.
DISCO_ESC = [0, 55, 100, 106, 110, 0, 0, 0]
DISCO_OPAC = [0, 100, 100, 85, 45, 0, 0, 0]
ANEL_ESC = [0, 55, 100, 108, 118, 128, 136, 142]
ANEL_TRACO = [0, 8, 9, 8, 6, 5, 3, 2]
ANEL_OPAC = [0, 100, 100, 100, 100, 85, 55, 0]
CLARAO_ESC = [70, 120, 60, 0, 0, 0, 0, 0]


def t(q):
    return q / FPS


def chaves(camada, caminho, valores):
    return [{'command': 'prop.addKey', 'params': {'layer': camada, 'path': caminho, 'time': t(q), 'value': v}}
            for q, v in enumerate(valores)]


def prop(camada, caminho, valor):
    return {'command': 'prop.set', 'params': {'layer': camada, 'path': caminho, 'value': valor}}


def passos():
    s = [{'command': 'comp.new', 'params': {'name': 'defesa3d_ec', 'width': LADO, 'height': LADO,
                                            'frameRate': FPS, 'duration': DESENHOS / FPS}}]

    # O disco de energia.
    s += [{'command': 'layer.newSolid', 'params': {'name': 'disco', 'color': '#ffffff', 'width': LADO, 'height': LADO}},
          {'command': 'layer.addMask', 'params': {'layer': 'disco', 'shape': 'ellipse', 'rect': [150, 150, 300, 300]}},
          {'command': 'layer.mask.set', 'params': {'layer': 'disco', 'mask': 1, 'field': 'feather', 'value': 4}},
          {'command': 'effect.apply', 'params': {'effect': 'Gradient Ramp', 'layers': ['disco']}},
          prop('disco', 'effects/#1/shape', 1), prop('disco', 'effects/#1/start', list(C)),
          prop('disco', 'effects/#1/startColor', '#ffffff'), prop('disco', 'effects/#1/end', [C[0] + 165, C[1]]),
          prop('disco', 'effects/#1/endColor', '#202020'),
          {'command': 'effect.apply', 'params': {'effect': 'Fractal Noise', 'layers': ['disco']}},
          prop('disco', 'effects/#2/fractalType', 3), prop('disco', 'effects/#2/contrast', 150),
          prop('disco', 'effects/#2/brightness', 30), prop('disco', 'effects/#2/transform/scale', 30),
          prop('disco', 'effects/#2/blendingMode', 3),
          {'command': 'effect.apply', 'params': {'effect': 'Colorama', 'layers': ['disco']}},
          prop('disco', 'effects/#3/outputCycle/usePresetPalette', 8), prop('disco', 'effects/#3/outputCycle/palette', PALETA),
          prop('disco', 'effects/#3/outputCycle/interpolatePalette', False), prop('disco', 'effects/#3/outputCycle/cycles', 0.999),
          {'command': 'layer.setSwitch', 'params': {'layers': ['disco'], 'switch': 'threeD', 'value': True}},
          prop('disco', 'transform/rotationY', GIRO_Y)]
    s += chaves('disco', 'effects/#2/evolution', [40 * q for q in range(DESENHOS)])     # a energia ferve
    s += chaves('disco', 'transform/scale', [[e, e, 100] for e in DISCO_ESC])
    s += chaves('disco', 'transform/opacity', DISCO_OPAC)

    # O anel: a borda clara do disco, que fica sozinha no fim e se abre.
    s += [{'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': 'anel', 'size': [300, 300],
                                                   'stroke': '#bff4ff', 'strokeWidth': 10}},
          prop('anel', 'contents/group/contents/fill/opacity', 0),
          prop('anel', 'transform/position', [*C, 0]),
          {'command': 'layer.setSwitch', 'params': {'layers': ['anel'], 'switch': 'threeD', 'value': True}},
          prop('anel', 'transform/rotationY', GIRO_Y)]
    s += chaves('anel', 'transform/scale', [[e, e, 100] for e in ANEL_ESC])
    s += chaves('anel', 'contents/group/contents/stroke/width', ANEL_TRACO)
    s += chaves('anel', 'transform/opacity', ANEL_OPAC)

    # O clarão do contato.
    s += [{'command': 'layer.newShape', 'params': {'kind': 'star', 'name': 'clarao', 'fill': '#e8fbff'}},
          prop('clarao', 'contents/group/contents/star/points', 8),
          prop('clarao', 'contents/group/contents/star/innerRadius', 14),
          prop('clarao', 'contents/group/contents/star/outerRadius', 60),
          prop('clarao', 'transform/position', [*C, 0])]
    s += chaves('clarao', 'transform/scale', [[e, e, 100] for e in CLARAO_ESC])

    # Brilho ciano em tudo (uma camada de ajuste por cima).
    s += [{'command': 'layer.newAdjustment', 'params': {'name': 'brilho'}},
          {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': ['brilho']}},
          prop('brilho', 'effects/#1/threshold', 45), prop('brilho', 'effects/#1/radius', 22),
          prop('brilho', 'effects/#1/intensity', 1.0), prop('brilho', 'effects/#1/colors', 1),
          prop('brilho', 'effects/#1/colorA', '#9ff0ff'), prop('brilho', 'effects/#1/colorB', '#1f7bff')]
    return s


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True)
    a = p.parse_args()
    a.saida.mkdir(parents=True, exist_ok=True)
    for velho in a.saida.glob('defesa3d_ec_*.png'):
        velho.unlink()
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(passos(), f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(PROJETO), '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / 'defesa3d_ec_[####].png'
    subprocess.run([str(a.cli), 'run', str(PROJETO),
                    'renderQueue.add', json.dumps({'comp': 'defesa3d_ec', 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', PROJETO)
    print('quadros:', len(list(a.saida.glob('defesa3d_ec_*.png'))))


if __name__ == '__main__':
    main()
