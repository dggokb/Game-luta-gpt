#!/usr/bin/env python3
"""As defesas de brilho do dono (art/vfx/defesa_brilho_v01) com dois anéis de pressão de ar
(pedido de 14/11: "uns 2 anéis da mesma cor que abrem em momento separado na ponta, expandindo e
dissipando em estilhaços").

Os anéis seguem o anel de pressão de ar em 3D já aprovado nos hits fortes (impactos_v06_ec.py,
anel): elipse em 3D virada para o golpe (o plano do anel de frente para quem bate), linha dupla
(Repeater), brilho frio, e a erosão de anime (Fractal Noise em Stencil Luma com o brilho caindo)
que parte o anel em estilhaços de borda dura enquanto ele abre. O segundo nasce dois quadros depois
e menor. A cor é o branco frio da defesa (a "luz" #E6F0FF do efeito.json).

O desenho do dono entra a 2× o ritmo dele (cada desenho segura 2 quadros) para os anéis abrirem
liso: 12 quadros a 16,7 (as defesas 1 e 2) e 25 por segundo (a 3), o mesmo tempo total.

    python3 art/vfx/effectcraft/defesa_aneis_ec.py --saida PASTA
    efeito_video.py PASTA/defesa_1 --nome defesa_brilho_aneis   --fps 16.67 --origem 254,247 --escala-mundo 0.56
    efeito_video.py PASTA/defesa_2 --nome defesa_brilho_aneis_2 --fps 16.67 --origem 257,250 --escala-mundo 0.72
    efeito_video.py PASTA/defesa_3 --nome defesa_brilho_aneis_3 --fps 25    --origem 256,252 --escala-mundo 0.85
"""
import argparse
import json
import subprocess
import sys
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
sys.path.insert(0, str(AQUI))
from impactos_v06_ec import CLI_PADRAO, prop, chaves  # noqa: E402

ARTE = AQUI.parent / 'defesa_brilho_v01'
LADO = 506
# (clarão na tela, escala da folha no jogo, fps do desenho)
DEFESAS = {1: ((254, 247), 0.56, 8.33), 2: ((257, 250), 0.72, 8.33), 3: ((256, 252), 0.85, 12.5)}
QUADROS = 12
GIRO = (-10, 66)
# Cada anel, por quadro de render (12): raio e traço em px do MUNDO, opacidade, erosão.
ANEIS = [
    # anel 1: abre na batida
    dict(raio=[None, 14, 40, 66, 86, 100, 110, 116, 120, None, None, None],
         traco=[None, 9, 8, 7, 5.5, 4, 3, 2, 1.5, None, None, None],
         opac=[0, 100, 100, 95, 85, 70, 50, 28, 10, 0, None, None],
         erosao=[300, 300, 300, 220, 140, 70, 10, -40, -100, -100, None, None]),
    # anel 2: dois quadros depois, menor
    dict(raio=[None, None, None, 10, 30, 50, 66, 78, 86, 90, None, None],
         traco=[None, None, None, 7, 6.5, 5.5, 4, 3, 2, 1.5, None, None],
         opac=[0, 0, 0, 100, 100, 90, 70, 48, 26, 8, 0, None],
         erosao=[300, 300, 300, 300, 240, 160, 90, 20, -40, -100, -100, None]),
]


def montar(nome, item, centro, escala, fps):
    s = [{'command': 'comp.new', 'params': {'name': nome, 'width': LADO, 'height': LADO, 'frameRate': fps,
                                            'duration': QUADROS / fps}},
         {'command': 'engine.batch', 'params': {'steps': [
             {'command': 'layer.addItem', 'params': {'item': item}},
             {'command': 'layer.rename', 'params': {'layer': '$1.layer', 'name': 'arte'}}]}},
         {'command': 'layer.setSwitch', 'params': {'layers': ['arte'], 'switch': 'threeD', 'value': True}}]
    k = 1.0 / escala   # px da tela por px do mundo
    for i, a in enumerate(ANEIS, start=1):
        n = f'anel{i}'
        s += [{'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': n, 'size': [100, 100],
                                                       'stroke': '#ffffff', 'strokeWidth': 6, 'fill': '#e6f0ff'}},
              {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'repeater'}},
              prop(n, 'contents/repeater/copies', 2), prop(n, 'contents/repeater/transform/position', [0, 0]),
              prop(n, 'contents/repeater/transform/scale', [86, 86]),
              prop(n, 'contents/repeater/transform/endOpacity', 45),
              prop(n, 'contents/group/contents/fill/opacity', 0),
              {'command': 'layer.setSwitch', 'params': {'layers': [n], 'switch': 'threeD', 'value': True}},
              prop(n, 'transform/rotationX', GIRO[0]), prop(n, 'transform/rotationY', GIRO[1]),
              prop(n, 'transform/position', [centro[0], centro[1], 0])]
        s += chaves(n, 'contents/group/contents/ellipse/size',
                    [None if r is None else [2 * r * k, 2 * r * k] for r in a['raio']], fps)
        s += chaves(n, 'contents/group/contents/stroke/width', [None if v is None else v * k for v in a['traco']], fps)
        s += chaves(n, 'transform/opacity', a['opac'], fps)
        s += [{'command': 'effect.apply', 'params': {'effect': 'Turbulent Displace', 'layers': [n]}},
              prop(n, 'effects/#1/amount', 4), prop(n, 'effects/#1/size', 60),
              {'command': 'effect.apply', 'params': {'effect': 'Fractal Noise', 'layers': [n]}},
              prop(n, 'effects/#2/contrast', 400), prop(n, 'effects/#2/overflow', 0),
              prop(n, 'effects/#2/transform/scale', 35), prop(n, 'effects/#2/complexity', 3),
              prop(n, 'effects/#2/evolutionOptions/seed', 5 + 7 * i), prop(n, 'effects/#2/blendingMode', 19),
              {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': [n]}},
              prop(n, 'effects/#3/threshold', 50), prop(n, 'effects/#3/radius', 10), prop(n, 'effects/#3/intensity', 0.7),
              prop(n, 'effects/#3/colors', 1), prop(n, 'effects/#3/colorA', '#f2f7ff'), prop(n, 'effects/#3/colorB', '#9fc8ff')]
        s += chaves(n, 'effects/#2/brightness', a['erosao'], fps)
    return s


def rodar(cli, k, saida):
    centro, escala, fps_arte = DEFESAS[k]
    fps = fps_arte * 2
    nome = f'defesa_{k}'
    destino = saida / nome
    destino.mkdir(parents=True, exist_ok=True)
    for velho in destino.glob('*.png'):
        velho.unlink()
    projeto = Path(tempfile.gettempdir()) / f'defesa_aneis_{k}.ecproj'
    primeiro = sorted((ARTE / nome).glob('*.png'))[0]
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump([{'command': 'file.import', 'params': {'paths': [str(primeiro)], 'sequence': True,
                                                         'frameRate': fps_arte}}], f)
        lista = f.name
    r = subprocess.run([str(cli), 'run', '--empty', '--commands', lista, '--save-as', str(projeto), '--json'],
                       check=True, capture_output=True, text=True)
    resp = json.loads(r.stdout.strip().splitlines()[-1])
    resp = resp['result'] if isinstance(resp, dict) else resp
    item = resp[0]['result']['items'][0]
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(montar(nome, item, centro, escala, fps), f)
        lista = f.name
    subprocess.run([str(cli), 'run', str(projeto), '--commands', lista, '--save', '--json'], check=True,
                   stdout=subprocess.DEVNULL)
    subprocess.run([str(cli), 'run', str(projeto),
                    'renderQueue.add', json.dumps({'comp': nome, 'format': 'png', 'channels': 'rgba',
                                                   'output': str(destino / f'{nome}_[####].png')}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print(nome, len(list(destino.glob('*.png'))), 'quadros')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True)
    a = p.parse_args()
    for k in DEFESAS:
        rodar(a.cli, k, a.saida)


if __name__ == '__main__':
    main()
