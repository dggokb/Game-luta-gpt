#!/usr/bin/env python3
"""Fumaça de pouso (e de queda pesada) pela técnica do Marvel Tōkon: volume 3D com sombreamento
de anime, duas colunas que saem dos pés, uma para cada lado. Estudo da referência do dono (11/11):
na queda do Hulk (quadros 165-175) cada lado nasce como um bico alto e fino, vira um volume
encaroçado em 2 ou 3 quadros, rola para fora, se rasga em tufos que sobem e se enrolam, e fica
ralo até sumir (~450 ms). Branco ~#f3f3f3 e uma sombra ~#c8c8c8, sem contorno.

Técnica (a mesma da fumaça do dash): esferas 3D iluminadas de cima com sombra projetada, numa
comp aninhada; Colorama corta a luz em duas cores chapadas; no fim as bolas se afastam, sobem e
continuam crescendo (dissipar, não sumir), Turbulent Displace em Twist e transparência.

    python3 art/vfx/effectcraft/fumaca3d_pouso_ec.py --saida PASTA [--cli CAMINHO]

No jogo (o meio dos pés no chão é a origem):
    efeito_video.py PASTA --nome fumaca3d_pouso_ec --fps 24 --origem 1200,952 --escala-mundo 0.6
"""
import argparse
import json
import random
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
PROJETO = AQUI / 'fumaca3d_pouso_ec.ecproj'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

FPS = 24
DESENHOS = 12
LARGO, ALTO = 2400, 1000
PE = (1200.0, 940.0)                      # o meio dos pés, no chão; uma coluna para cada lado
LUZ, SOMBRA = (0.953, 0.953, 0.953), (0.784, 0.784, 0.800)
# Corte da luminância entre sombra e luz (0 a 1): abaixo vira sombra.
CORTE = 0.42
TORCE = [0, 0, 0, 0, 0, 8, 20, 34, 48, 62, 76, 90]                 # os tufos se enrolam
RALO = [0, 0, 0, 0, 0, 0, 0, 0.5, 1.5, 3, 4.5, 6]                      # borda amolecendo
OPACIDADE = [100, 100, 100, 100, 100, 100, 92, 80, 64, 46, 26, 0]


def t(q):
    return q / FPS


def tufos():
    """Duas colunas, uma para cada lado dos pés: cada uma com 2 cachos tipo couve-flor (uma bola
    grande e bolas menores por cima), o de fora maior e mais alto. Volta
    (nasce, cacho, x0, y0, z, raio, vx, sobe), com vx negativo para a direita."""
    import math
    sorte = random.Random(31)
    lista, cacho = [], 0
    for lado in (-1, 1):
        for c, (leva, f) in enumerate(((0, 0.6), (1, 0.3))):
            R = (80 + 60 * f) * sorte.uniform(0.92, 1.08)
            vx = lado * (300 + 700 * f)                 # voa para fora em 2 ou 3 quadros (medido: ~700 px)
            sobe = (30 + 60 * f) * sorte.uniform(0.9, 1.1)
            x0, y0, z0 = PE[0] - lado * (40 + 30 * f), PE[1] - R * 0.9, sorte.uniform(-25, 25)
            lista.append((leva, cacho, x0, y0, z0, R, vx, sobe))
            for _ in range(sorte.randint(9, 11)):         # empilhadas: coluna alta (medido: ~400 px)
                a = math.radians(sorte.uniform(-130, -50))
                r = R * sorte.uniform(0.6, 0.9)
                d = R * sorte.uniform(0.4, 1.5)
                lista.append((leva, cacho, x0 + d * math.cos(a) * 1.2, y0 + d * math.sin(a) * 1.25, z0 + sorte.uniform(-0.4, 0.4) * R,
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
                if k == 0:
                    esc = [45 * cresce, 210 * cresce, 45 * cresce]      # nasce como um bico alto e fino
                afasta = 1 + 0.18 * d                                    # as bolas se separam do cacho
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
    s += [{'command': 'comp.new', 'params': {'name': 'fumaca3d_pouso_ec', 'width': LARGO, 'height': ALTO,
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
    for velho in a.saida.glob('fumaca3d_pouso_ec_*.png'):
        velho.unlink()
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(passos(), f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(PROJETO), '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / 'fumaca3d_pouso_ec_[####].png'
    subprocess.run([str(a.cli), 'run', str(PROJETO),
                    'renderQueue.add', json.dumps({'comp': 'fumaca3d_pouso_ec', 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', PROJETO)
    print('quadros:', len(list(a.saida.glob('fumaca3d_pouso_ec_*.png'))))


if __name__ == '__main__':
    main()
