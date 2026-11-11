#!/usr/bin/env python3
"""Fumaça do pé na largada do dash pela técnica do Marvel Tōkon: volume 3D com sombreamento de
anime (toon). Pedido de 11/11: estudar o vídeo de referência do dono quadro a quadro e fazer os
efeitos com base nele.

Medido na referência (só estudo, nenhum quadro entra no repositório): a fumaça é branca
(~#f3f3f3) com UMA sombra cinza (~#c8c8c8), sem contorno; nasce como um bico no chão, cresce em 2
ou 3 quadros até um volume encaroçado, se parte em tufos que sobem e se enrolam, afina e some
ficando transparente; tudo em ~11 quadros (~455 ms).

Como é feito aqui (a mesma técnica, não desenho à mão nem forma chapada):
- esferas 3D de verdade (Advanced 3D), iluminadas por uma luz paralela: o volume e as dobras
  entre as bolas saem da luz, não de um desenho;
- o cel: Colorama corta a luminância em duas cores chapadas (luz e sombra), sem interpolar;
- tempo da referência: 12 desenhos a 24 por segundo (500 ms); as bolas nascem do pé em levas,
  crescem rápido, voam para trás freando e sobem; no fim se partem (erosão de borda dura, Fractal
  Noise em Stencil Luma) e ficam transparentes.

    python3 art/vfx/effectcraft/fumaca3d_dash_ec.py --saida PASTA [--cli CAMINHO]

No jogo (o pé no chão é a origem):
    efeito_video.py PASTA --nome fumaca3d_dash_ec --fps 24 --origem 780,772 --escala-mundo 0.75
"""
import argparse
import json
import random
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
PROJETO = AQUI / 'fumaca3d_dash_ec.ecproj'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

FPS = 24
DESENHOS = 12
LARGO, ALTO = 900, 820
PE = (780.0, 760.0)                      # o pé, no chão; a fumaça vai para trás (esquerda)
LUZ, SOMBRA = (0.953, 0.953, 0.953), (0.784, 0.784, 0.800)
# Corte da luminância entre sombra e luz (0 a 1): abaixo vira sombra.
CORTE = 0.42
TORCE = [0, 0, 0, 0, 0, 8, 20, 34, 48, 62, 76, 90]                 # os tufos se enrolam
RALO = [0, 0, 0, 0, 0, 0, 0, 0.5, 1.5, 3, 4.5, 6]                      # borda amolecendo
OPACIDADE = [100, 100, 100, 100, 100, 100, 92, 80, 64, 46, 26, 0]


def t(q):
    return q / FPS


def tufos():
    """Cachos tipo couve-flor, mais altos que largos: cada cacho é uma bola grande com bolas
    menores grudadas por cima e dos lados, bem sobrepostas (fundem num volume só).
    Volta (nasce, cacho, x0, y0, z, raio, vx, sobe)."""
    import math
    sorte = random.Random(17)
    lista, cacho = [], 0
    for leva, cachos in ((0, 2), (1, 2), (2, 1)):
        for c in range(cachos):
            f = min(1.0, (c + 1.2 * leva + sorte.uniform(0.2, 0.8)) / 3.6)   # 0 = no pé, 1 = longe
            R = (40 + 26 * f) * sorte.uniform(0.9, 1.1)
            vx = 40 + 360 * f
            sobe = (60 + 90 * f) * sorte.uniform(0.85, 1.15)
            x0, y0, z0 = PE[0] - 15 - 30 * f, PE[1] - R * 0.9, sorte.uniform(-25, 25)
            lista.append((leva, cacho, x0, y0, z0, R, vx, sobe))
            for _ in range(sorte.randint(3, 4)):
                a = math.radians(sorte.uniform(-150, -30))   # em cima e dos lados
                r = R * sorte.uniform(0.55, 0.8)
                d = R * sorte.uniform(0.5, 0.75)
                lista.append((leva, cacho, x0 + d * math.cos(a), y0 + d * math.sin(a) * 1.3, z0 + sorte.uniform(-0.4, 0.4) * R,
                              r, vx * sorte.uniform(0.95, 1.05), sobe * sorte.uniform(1.0, 1.3)))
            cacho += 1
    return lista


DISSIPA = 5          # desenho em que começa a dissipar


def passos():
    s = [{'command': 'comp.new', 'params': {'name': 'bolas', 'width': LARGO, 'height': ALTO,
                                            'frameRate': FPS, 'duration': DESENHOS / FPS}}]
    lista = tufos()
    for n, (nasce, cacho, x0, y0, z, raio, vx, sobe) in enumerate(lista):
        nome = f'b{n}'
        # o centro do cacho (a bola grande, a primeira dele): na dissipação as bolas se afastam dele
        cx, cy = next((b[2], b[3]) for b in lista if b[1] == cacho)
        s.append({'command': 'layer.new3dPrimitive', 'params': {
            'kind': 'sphere', 'name': nome, 'radius': raio, 'position': [x0, y0, z],
            'baseColor': '#ffffff', 'roughness': 1.0, 'metallic': 0.0, 'castsShadows': True, 'acceptsShadows': True}})
        for q in range(DESENHOS):
            k = q - nasce
            if k < 0:
                esc, x, y = [0, 0, 0], x0, y0
            else:
                anda = 1 - 0.55 ** (k + 1)
                g = min(1.0, 0.45 + 0.55 * (1 - 0.45 ** (k + 1)))       # nasce e engorda rápido
                d = max(0, q - DISSIPA)                                 # dissipação
                cresce = g * (1 + 0.04 * d)                              # continua crescendo
                esc = [100 * cresce, 118 * cresce, 100 * cresce]       # um pouco mais alta que larga
                afasta = 1 + 0.55 * d                                    # as bolas se separam do cacho
                x = cx + (x0 - cx) * afasta - vx * anda
                alto = max(0.0, (cy - y0) / raio + 0.5)                  # as de cima sobem mais: o volume se rasga para cima
                y = cy + (y0 - cy) * afasta - sobe * anda - (10 + 16 * alto) * d ** 1.25
            s.append({'command': 'prop.addKey', 'params': {'layer': nome, 'path': 'transform/scale',
                                                          'time': t(q), 'value': esc}})
            s.append({'command': 'prop.addKey', 'params': {'layer': nome, 'path': 'transform/position',
                                                          'time': t(q), 'value': [x, y, z]}})
    # Luz de cima, da frente e da esquerda (a sombra cai embaixo e à direita, em manchas); sombra
    # projetada fraca, só para escurecer as dobras.
    s += [{'command': 'layer.newLight', 'params': {'kind': 'Parallel', 'name': 'luz'}},
          {'command': 'prop.set', 'params': {'layer': 'luz', 'path': 'transform/position', 'value': [LARGO / 2 - 350, -450, -800]}},
          {'command': 'prop.set', 'params': {'layer': 'luz', 'path': 'transform/poi', 'value': [LARGO / 2, ALTO / 2, 0]}},
          {'command': 'prop.set', 'params': {'layer': 'luz', 'path': 'lightOptions/castsShadows', 'value': True}},
          {'command': 'prop.set', 'params': {'layer': 'luz', 'path': 'lightOptions/shadowDarkness', 'value': 30}}]

    # A composição final: cel, torção e afinamento no fim, transparência.
    s += [{'command': 'comp.new', 'params': {'name': 'fumaca3d_dash_ec', 'width': LARGO, 'height': ALTO,
                                             'frameRate': FPS, 'duration': DESENHOS / FPS}},
          {'command': 'engine.batch', 'params': {'steps': [
              {'command': 'layer.addItem', 'params': {'item': 'bolas'}},
              {'command': 'layer.rename', 'params': {'layer': '$1.layer', 'name': 'fumaca'}}]}},
          {'command': 'effect.apply', 'params': {'effect': 'Colorama', 'layers': ['fumaca']}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#1/outputCycle/usePresetPalette', 'value': 8}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#1/outputCycle/palette', 'value':
              '0:{0},{1},{2} {3}:{4},{5},{6}'.format(*SOMBRA, CORTE, *LUZ)}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#1/outputCycle/interpolatePalette', 'value': False}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#1/outputCycle/cycles', 'value': 0.999}},
          # Torção: os tufos se enrolam no fim (Turbulent Displace em Twist, crescendo).
          {'command': 'effect.apply', 'params': {'effect': 'Turbulent Displace', 'layers': ['fumaca']}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#2/displacement', 'value': 2}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#2/size', 'value': 70}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#2/pinning', 'value': 0}},
          # Ralo: a borda amolece aos poucos enquanto fica transparente (dissipar, não apagar).
          {'command': 'effect.apply', 'params': {'effect': 'Gaussian Blur', 'layers': ['fumaca']}}]
    s += [{'command': 'prop.addKey', 'params': {'layer': 'fumaca', 'path': 'effects/#2/amount', 'time': t(q), 'value': v}}
          for q, v in enumerate(TORCE)]
    s += [{'command': 'prop.addKey', 'params': {'layer': 'fumaca', 'path': 'effects/#3/blurriness', 'time': t(q), 'value': v}}
          for q, v in enumerate(RALO)]
    s += [{'command': 'prop.addKey', 'params': {'layer': 'fumaca', 'path': 'transform/opacity', 'time': t(q), 'value': v}}
          for q, v in enumerate(OPACIDADE)]
    return s


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True)
    a = p.parse_args()
    a.saida.mkdir(parents=True, exist_ok=True)
    for velho in a.saida.glob('fumaca3d_dash_ec_*.png'):
        velho.unlink()
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(passos(), f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(PROJETO), '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / 'fumaca3d_dash_ec_[####].png'
    subprocess.run([str(a.cli), 'run', str(PROJETO),
                    'renderQueue.add', json.dumps({'comp': 'fumaca3d_dash_ec', 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', PROJETO)
    print('quadros:', len(list(a.saida.glob('fumaca3d_dash_ec_*.png'))))


if __name__ == '__main__':
    main()
