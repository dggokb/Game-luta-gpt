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
    efeito_video.py PASTA --nome fumaca3d_dash_ec --fps 24 --origem 600,392 --escala-mundo 0.85
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
LARGO, ALTO = 700, 420
PE = (600.0, 380.0)                      # o pé, no chão; a fumaça vai para trás (esquerda)
LUZ, SOMBRA = (0.953, 0.953, 0.953), (0.784, 0.784, 0.800)
# Corte da luminância entre sombra e luz (0 a 1): abaixo vira sombra.
CORTE = 0.50
EROSAO = [320, 320, 320, 320, 320, 300, 250, 190, 120, 50, -30, -120]
OPACIDADE = [100, 100, 100, 100, 100, 100, 100, 95, 80, 60, 35, 0]


def t(q):
    return q / FPS


def bolas():
    """Cachos tipo couve-flor: em cada leva, cachos de uma bola grande com bolas menores grudadas
    por cima e dos lados; cada bola anda com o cacho dela.
    Volta (nasce, x0, y0, z, raio, vx, sobe)."""
    sorte = random.Random(17)
    lista = []
    for leva, cachos in ((0, 2), (1, 2), (2, 2), (3, 1)):
        for c in range(cachos):
            f = min(1.0, (c + leva * 0.6 + sorte.uniform(0.1, 0.9)) / 3.2)   # 0 = no pé, 1 = longe
            R = (38 + 34 * f) * sorte.uniform(0.85, 1.15)
            vx = (30 + 430 * f) * (1 - 0.1 * leva)
            sobe = (40 + 110 * f) * sorte.uniform(0.7, 1.2)
            x0, y0, z0 = PE[0] - 15 - 30 * f, PE[1] - 6, sorte.uniform(-30, 30)
            lista.append((leva, x0, y0, z0, R, vx, sobe))
            for _ in range(sorte.randint(2, 3)):
                ang = sorte.uniform(-135, -45)              # em cima: a fumaça cresce para cima
                import math
                a = math.radians(ang)
                r = R * sorte.uniform(0.55, 0.8)
                d = R * sorte.uniform(0.55, 0.85)
                lista.append((leva, x0 + d * math.cos(a), y0 + d * math.sin(a), z0 + sorte.uniform(-R, R) * 0.5,
                              r, vx * sorte.uniform(0.92, 1.08), sobe * sorte.uniform(0.95, 1.25)))
    return lista


def passos():
    s = [{'command': 'comp.new', 'params': {'name': 'bolas', 'width': LARGO, 'height': ALTO,
                                            'frameRate': FPS, 'duration': DESENHOS / FPS}}]
    for n, (nasce, x0, y0, z, raio, vx, sobe) in enumerate(bolas()):
        nome = f'b{n}'
        s.append({'command': 'layer.new3dPrimitive', 'params': {
            'kind': 'sphere', 'name': nome, 'radius': raio, 'position': [x0, y0, z],
            'baseColor': '#ffffff', 'roughness': 1.0, 'metallic': 0.0, 'castsShadows': True, 'acceptsShadows': True}})
        for q in range(DESENHOS):
            k = q - nasce
            if k < 0:
                esc, x, y = 0, x0, y0
            else:
                anda = 1 - 0.55 ** (k + 1)                      # rápido e freando
                esc = 100 * min(1.0, 0.35 + 0.65 * (1 - 0.45 ** (k + 1)))
                x = x0 - vx * anda
                y = y0 - sobe * anda - raio * esc / 100 * 0.6   # apoia no chão e sobe
            s.append({'command': 'prop.addKey', 'params': {'layer': nome, 'path': 'transform/scale',
                                                          'time': t(q), 'value': [esc, esc, esc]}})
            s.append({'command': 'prop.addKey', 'params': {'layer': nome, 'path': 'transform/position',
                                                          'time': t(q), 'value': [x, y, z]}})
    # Luz de cima e um pouco da frente, com sombra projetada: uma bola sombreia a outra e as dobras
    # escurecem, como na referência.
    s += [{'command': 'layer.newLight', 'params': {'kind': 'Parallel', 'name': 'luz'}},
          {'command': 'prop.set', 'params': {'layer': 'luz', 'path': 'transform/position', 'value': [LARGO / 2 - 60, -500, -700]}},
          {'command': 'prop.set', 'params': {'layer': 'luz', 'path': 'transform/poi', 'value': [LARGO / 2, ALTO / 2, 0]}},
          {'command': 'prop.set', 'params': {'layer': 'luz', 'path': 'lightOptions/castsShadows', 'value': True}},
          {'command': 'prop.set', 'params': {'layer': 'luz', 'path': 'lightOptions/shadowDarkness', 'value': 45}}]

    # A composição final: a fumaça 3D vira uma camada 2D com o cel, a erosão e o sumiço.
    s += [{'command': 'comp.new', 'params': {'name': 'fumaca3d_dash_ec', 'width': LARGO, 'height': ALTO,
                                             'frameRate': FPS, 'duration': DESENHOS / FPS}},
          {'command': 'engine.batch', 'params': {'steps': [
              {'command': 'layer.addItem', 'params': {'item': 'bolas'}},
              {'command': 'layer.rename', 'params': {'layer': '$1.layer', 'name': 'fumaca'}}]}},
          # Cel: duas cores chapadas pela luminância (Colorama Custom sem interpolar).
          {'command': 'effect.apply', 'params': {'effect': 'Colorama', 'layers': ['fumaca']}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#1/outputCycle/usePresetPalette', 'value': 8}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#1/outputCycle/palette', 'value':
              '0:{0},{1},{2} {3}:{4},{5},{6}'.format(*SOMBRA, CORTE, *LUZ)}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#1/outputCycle/interpolatePalette', 'value': False}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#1/outputCycle/cycles', 'value': 0.999}},
          # Erosão de borda dura: se parte em tufos no fim.
          {'command': 'effect.apply', 'params': {'effect': 'Fractal Noise', 'layers': ['fumaca']}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#2/contrast', 'value': 700}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#2/overflow', 'value': 0}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#2/transform/scale', 'value': 45}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#2/complexity', 'value': 3}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#2/evolutionOptions/seed', 'value': 4}},
          {'command': 'prop.set', 'params': {'layer': 'fumaca', 'path': 'effects/#2/blendingMode', 'value': 19}}]
    s += [{'command': 'prop.addKey', 'params': {'layer': 'fumaca', 'path': 'effects/#2/brightness', 'time': t(q), 'value': v}}
          for q, v in enumerate(EROSAO)]
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
