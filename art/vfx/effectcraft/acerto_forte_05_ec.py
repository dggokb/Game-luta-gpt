#!/usr/bin/env python3
"""A V4 da faísca do golpe forte (art/vfx/acerto_forte_05.png, desenhada pelo dono) com o
acabamento do EffectCraft que já foi aprovado no dano_forte: halo laranja (Glow A e B) e o anel
de pressão de ar em 3D atravessando a faísca. Não leva faixas de cor nem erosão: o desenho já
tem as cores dele e já se dissipa em lascas.

    python3 art/vfx/effectcraft/acerto_forte_05_ec.py --saida PASTA [--cli CAMINHO]

No jogo (o contato é a origem; mesma escala da V4 testada):
    efeito_video.py PASTA --nome acerto_forte_05_ec --fps 24 --origem 255,483 --escala-mundo 0.43
"""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path

from PIL import Image

AQUI = Path(__file__).resolve().parent
IMAGEM = AQUI.parent / 'acerto_forte_05.png'      # 4 × 2 células de 780 × 650
PROJETO = AQUI / 'acerto_forte_05_ec.ecproj'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

FPS = 24
ORDEM = [0, 1, 2, 2, 3, 4, 5, 6, 7]                # segura o auge um desenho, como o Tōkon (126-128)
CEL = (780, 650)
FOLGA = 150                                        # px a mais em cima e embaixo para o anel caber
LARGO, ALTO = CEL[0], CEL[1] + 2 * FOLGA
MIOLO = (255.0, 333.0 + FOLGA)                     # o contato

# O anel do dano_forte (0,30 px do mundo por px) convertido para a escala da V4 (0,43).
K = 0.30 / 0.43
ANEL_GIRO = (-10, 66)
ANEL_RAIOS = [r * K for r in (100, 200, 280, 340, 385, 420, 445, 462, 470)]
ANEL_TRACOS = [t * K for t in (22, 19, 16, 13, 10, 7, 5, 3, 2)]
ANEL_OPAC = [100, 96, 88, 74, 56, 36, 18, 6, 0]
ANEL_AVANCO = [d * K for d in (0, 30, 55, 75, 90, 100, 107, 111, 113)]
ANEL_EROSAO = [300, 280, 200, 130, 70, 20, -30, -70, -100]


def t(q):
    return q / FPS


def cortar(pasta):
    """As células da folha, na ordem, com folga em cima e embaixo, numeradas para o import."""
    im = Image.open(IMAGEM).convert('RGBA')
    cel = [im.crop((i * CEL[0], j * CEL[1], (i + 1) * CEL[0], (j + 1) * CEL[1])) for j in range(2) for i in range(4)]
    for n, k in enumerate(ORDEM):
        tela = Image.new('RGBA', (LARGO, ALTO), (0, 0, 0, 0))
        tela.paste(cel[k], (0, FOLGA))
        tela.save(pasta / f'v4_{n:04d}.png')
    return pasta / 'v4_0000.png'


def chaves(camada, caminho, valores):
    return [{'command': 'prop.addKey', 'params': {'layer': camada, 'path': caminho, 'time': t(q), 'value': v}}
            for q, v in enumerate(valores) if v is not None]


def prop(camada, caminho, valor):
    return {'command': 'prop.set', 'params': {'layer': camada, 'path': caminho, 'value': valor}}


def montar(primeiro, item):
    dur = len(ORDEM) / FPS
    s = [{'command': 'comp.new', 'params': {'name': 'acerto_forte_05_ec', 'width': LARGO, 'height': ALTO,
                                            'frameRate': FPS, 'duration': dur}},
         {'command': 'engine.batch', 'params': {'steps': [
             {'command': 'layer.addItem', 'params': {'item': item}},
             {'command': 'layer.rename', 'params': {'layer': '$1.layer', 'name': 'v4'}}]}},
         # Halo laranja limpo em volta (o mesmo do dano_forte).
         {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': ['v4']}},
         prop('v4', 'effects/#1/threshold', 45), prop('v4', 'effects/#1/radius', 30),
         prop('v4', 'effects/#1/intensity', 0.8), prop('v4', 'effects/#1/colors', 1),
         prop('v4', 'effects/#1/colorA', '#ff8a1f'), prop('v4', 'effects/#1/colorB', '#c2280c'),
         # Plano 3D em z = 0, para o anel atravessá-lo (sem câmera o 3D enquadra igual ao 2D).
         {'command': 'layer.setSwitch', 'params': {'layers': ['v4'], 'switch': 'threeD', 'value': True}}]
    # O anel de pressão de ar em 3D.
    n = 'anel'
    s += [{'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': n, 'size': [200, 200],
                                                   'stroke': '#ffffff', 'strokeWidth': 20, 'fill': '#dff6ff'}},
          {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'repeater'}},
          prop(n, 'contents/repeater/copies', 2), prop(n, 'contents/repeater/transform/position', [0, 0]),
          prop(n, 'contents/repeater/transform/scale', [88, 88]), prop(n, 'contents/repeater/transform/endOpacity', 40),
          {'command': 'layer.setSwitch', 'params': {'layers': [n], 'switch': 'threeD', 'value': True}},
          prop(n, 'transform/rotationX', ANEL_GIRO[0]), prop(n, 'transform/rotationY', ANEL_GIRO[1])]
    s += chaves(n, 'transform/position', [[MIOLO[0] + d, MIOLO[1], 0] for d in ANEL_AVANCO])
    s += chaves(n, 'contents/group/contents/ellipse/size', [None] + [[2 * r, 2 * r] for r in ANEL_RAIOS])
    s += chaves(n, 'contents/group/contents/stroke/width', [None] + ANEL_TRACOS)
    s += chaves(n, 'transform/opacity', [0] + ANEL_OPAC)
    s += chaves(n, 'contents/group/contents/fill/opacity', [0, 8, 6, 0])
    s += [{'command': 'effect.apply', 'params': {'effect': 'Turbulent Displace', 'layers': [n]}},
          prop(n, 'effects/#1/amount', 4), prop(n, 'effects/#1/size', 90),
          {'command': 'effect.apply', 'params': {'effect': 'Fractal Noise', 'layers': [n]}},
          prop(n, 'effects/#2/contrast', 400), prop(n, 'effects/#2/overflow', 0),
          prop(n, 'effects/#2/transform/scale', 80), prop(n, 'effects/#2/complexity', 3),
          prop(n, 'effects/#2/evolutionOptions/seed', 5), prop(n, 'effects/#2/blendingMode', 19)]
    s += chaves(n, 'effects/#1/evolution', [None] + [25 * i for i in range(len(ANEL_RAIOS))])
    s += chaves(n, 'effects/#2/brightness', [300] + ANEL_EROSAO)
    s += [{'command': 'effect.apply', 'params': {'effect': 'Gaussian Blur', 'layers': [n]}},
          prop(n, 'effects/#3/blurriness', 1.2),
          {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': [n]}},
          prop(n, 'effects/#4/threshold', 50), prop(n, 'effects/#4/radius', 14), prop(n, 'effects/#4/intensity', 0.7),
          prop(n, 'effects/#4/colors', 1), prop(n, 'effects/#4/colorA', '#bff0ff'), prop(n, 'effects/#4/colorB', '#5cc8ff')]
    return s


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True, help='pasta da sequência PNG (fora do repositório)')
    a = p.parse_args()
    a.saida.mkdir(parents=True, exist_ok=True)
    for velho in a.saida.glob('acerto_forte_05_ec_*.png'):
        velho.unlink()
    celulas = a.saida.parent / 'acerto_forte_05_celulas'
    celulas.mkdir(parents=True, exist_ok=True)
    primeiro = cortar(celulas)
    # 1) importa a sequência e descobre o nome do item; 2) monta a composição.
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump([{'command': 'file.import', 'params': {'paths': [str(primeiro)], 'sequence': True, 'frameRate': FPS}}], f)
        lista = f.name
    r = subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(PROJETO), '--json'],
                       check=True, capture_output=True, text=True)
    resp = json.loads(r.stdout.strip().splitlines()[-1])
    resp = resp['result'] if isinstance(resp, dict) else resp      # com --save-as vem dentro de 'result'
    item = resp[0]['result']['items'][0]
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(montar(primeiro, item), f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', str(PROJETO), '--commands', lista, '--save', '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / 'acerto_forte_05_ec_[####].png'
    subprocess.run([str(a.cli), 'run', str(PROJETO),
                    'renderQueue.add', json.dumps({'comp': 'acerto_forte_05_ec', 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', PROJETO)
    print('quadros:', len(list(a.saida.glob('acerto_forte_05_ec_*.png'))))


if __name__ == '__main__':
    main()
