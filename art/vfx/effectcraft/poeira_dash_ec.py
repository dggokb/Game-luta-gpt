#!/usr/bin/env python3
"""**NÃO APROVADO (10/11): não chegou ao nível profissional** (saiu com cara de pão, bolinhas e
depois uma faixa lisa). Fica como registro da técnica; para a fumaça do dash, voltar a imagem ou
vídeo gerado (docs/vfx-continuar.md, "Fumaça do dash").

Fumaça do pé na largada do dash, feita no EffectCraft pela técnica de fumaça cel de anime
("metaball"): tufos redondos que voam, se fundem por desfoque + corte duro do alfa e ganham duas
cores chapadas. Pedido de 10/11: a poeira de hoje (poeira_corrida_01.png) "é feia".

Regra 2 da skill vfx-luta (revista em 10/11): desenho feito no programa só vale em nível
profissional e o dono julga no vídeo. A técnica é a dos tutoriais de fumaça cartoon do After
Effects (formas + desfoque + corte duro + duas cores) e a do relatório de VFX (tempo de anime,
erosão de borda dura):
- 8 desenhos a 12 por segundo (667 ms): fumaça pede 6 a 8 desenhos e 600 a 700 ms;
- os tufos nascem no pé em quatro levas (desenhos 0 a 3), voam para trás rápidos e freiam,
  crescem e sobem um pouco; base reta no chão (máscara);
- massa: Gaussian Blur + Channel Threshold no alfa = a silhueta fundida, de borda limpa;
- luz: os mesmos tufos, menores e mais altos, no tom claro, presos à massa (Preserve Transparency);
- riscos: traços afinando (taper) saindo do pé rente ao chão, que esticam e recolhem (Trim Paths);
- some em pedaços de borda dura (Fractal Noise em Stencil Luma), a mesma semente na luz e na massa.

    python3 art/vfx/effectcraft/poeira_dash_ec.py --saida PASTA [--cli CAMINHO]

Sai: art/vfx/effectcraft/poeira_dash_ec.ecproj e PASTA/poeira_dash_ec_0000.png ... 0007.png.
No jogo (o pé no chão é a origem):
    efeito_video.py PASTA --nome poeira_dash_ec --fps 12 --origem 680,300 --escala-mundo 0.38
"""
import argparse
import json
import math
import random
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
PROJETO = AQUI / 'poeira_dash_ec.ecproj'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

FPS = 12
DESENHOS = 8
LARGO, ALTO = 760, 340
PE = (680.0, 300.0)            # o pé, na linha do chão; a poeira vai para a esquerda (para trás)
CHAO = PE[1]

SOMBRA = '#b98f6a'             # tom de sombra (a massa)
LUZ = '#f6eee0'                # tom de luz (por cima)
RISCO = '#fff8ec'

EROSAO = [750, 750, 750, 640, 470, 280, 70, -180]       # some do desenho 4 em diante


def t(q):
    return q / FPS


def tufos():
    """Os tufos: (nasce, x0, vx, sobe, r0, cresce). Quatro levas saindo do pé."""
    sorte = random.Random(21)
    lista = []
    # Velocidades bem diferentes: uns tufos ficam perto do pé, outros vão longe, e a poeira vira
    # um rastro que se espalha do pé para trás (não um bloco andando).
    # Distribuídos sem buraco ao longo do rastro: maiores longe do pé, menores perto.
    for leva, quantos in ((0, 7), (1, 6), (2, 5), (3, 4)):
        for i in range(quantos):
            f = (i + sorte.uniform(0.1, 0.9)) / quantos          # 0 = perto do pé, 1 = longe
            lista.append((leva,
                          PE[0] - sorte.uniform(0, 20),
                          (20 + 230 * f) * (1.0 - 0.18 * leva),
                          sorte.uniform(2, 7),
                          (11 + 14 * f) * sorte.uniform(0.85, 1.15),
                          sorte.uniform(1.7, 2.4)))
    return lista


def chave(camada, caminho, q, v):
    return {'command': 'prop.addKey', 'params': {'layer': camada, 'path': caminho, 'time': t(q), 'value': v}}


def grupo(n):
    return 'contents/group' if n == 1 else f'contents/group#{n}'


def camada_de_tufos(nome, cor, encolhe, sobe_extra, adiante=0.0):
    """Uma camada de forma com um grupo (elipse + preenchimento) por tufo."""
    s = [{'command': 'layer.newShape', 'params': {'kind': 'none', 'name': nome}},
         {'command': 'prop.set', 'params': {'layer': nome, 'path': 'transform/position', 'value': [0, 0, 0]}}]
    for n, (nasce, x0, vx, sobe, r0, cresce) in enumerate(tufos(), start=1):
        g = grupo(n)
        s += [{'command': 'layer.addShapeItem', 'params': {'layer': nome, 'kind': 'group'}},
              {'command': 'layer.addShapeItem', 'params': {'layer': nome, 'kind': 'ellipse', 'group': g}},
              {'command': 'layer.addShapeItem', 'params': {'layer': nome, 'kind': 'fill', 'group': g}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': f'{g}/contents/fill/color', 'value': cor}}]
        for q in range(DESENHOS):
            k = q - nasce
            if k < 0:
                s.append(chave(nome, f'{g}/contents/ellipse/size', q, [0, 0]))
                s.append(chave(nome, f'{g}/contents/ellipse/position', q, [x0, CHAO]))
                continue
            andou = vx * (1 - 0.62 ** (k + 1)) / 0.38          # rápido e freando
            r = r0 * (1 + (cresce - 1) * (1 - 0.6 ** (k + 1))) * encolhe
            y = CHAO - sobe * k - r * (0.35 + sobe_extra)
            s.append(chave(nome, f'{g}/contents/ellipse/size', q, [2 * r * 1.15, 2 * r]))
            s.append(chave(nome, f'{g}/contents/ellipse/position', q, [x0 - andou + adiante * r, y]))
    # Funde os tufos: desfoque e corte duro no alfa; depois pinta de novo com a cor chapada.
    s += [{'command': 'effect.apply', 'params': {'effect': 'Gaussian Blur', 'layers': [nome]}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#1/blurriness', 'value': 12}},
          {'command': 'effect.apply', 'params': {'effect': 'Channel Threshold', 'layers': [nome]}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#2/channel', 'value': 3}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#2/threshold', 'value': 120}},
          {'command': 'effect.apply', 'params': {'effect': 'Fill', 'layers': [nome]}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#3/color', 'value': cor}},
          # Erosão de borda dura (mesma semente nas duas camadas: os buracos batem).
          {'command': 'effect.apply', 'params': {'effect': 'Fractal Noise', 'layers': [nome]}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#4/contrast', 'value': 1000}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#4/overflow', 'value': 0}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#4/transform/scale', 'value': 32}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#4/complexity', 'value': 3}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#4/evolutionOptions/seed', 'value': 6}},
          {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#4/blendingMode', 'value': 19}}]
    s += [chave(nome, 'effects/#4/brightness', q, v) for q, v in enumerate(EROSAO)]
    # Base reta no chão (máscara em camada de forma: conta do centro do conteúdo, 0,0).
    s += [{'command': 'layer.addMask', 'params': {'layer': nome, 'shape': 'rect',
                                                  'rect': [-LARGO, -ALTO, 3 * LARGO, CHAO + ALTO]}}]
    return s


def riscos():
    """Traços afinando rentes ao chão, saindo do pé para trás: esticam e recolhem."""
    sorte = random.Random(5)
    nome = 'riscos'
    s = [{'command': 'layer.newShape', 'params': {'kind': 'none', 'name': nome}},
         {'command': 'prop.set', 'params': {'layer': nome, 'path': 'transform/position', 'value': [0, 0, 0]}}]
    for n in range(1, 5):
        g = grupo(n)
        comp = sorte.uniform(150, 280)
        ang = math.radians(180 + sorte.uniform(3, 12))       # para trás, rasante
        x1, y1 = PE[0] + comp * math.cos(ang), CHAO - 4 + comp * math.sin(ang)
        s += [{'command': 'layer.addShapeItem', 'params': {'layer': nome, 'kind': 'group'}},
              {'command': 'layer.addShapeItem', 'params': {'layer': nome, 'kind': 'path', 'group': g}},
              {'command': 'layer.addShapeItem', 'params': {'layer': nome, 'kind': 'stroke', 'group': g}},
              {'command': 'layer.addShapeItem', 'params': {'layer': nome, 'kind': 'trim', 'group': g}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': f'{g}/contents/path/path', 'value': {
                  'closed': False, 'vertices': [[PE[0] - 6, CHAO - 4], [x1, y1]],
                  'in_tangents': [[0, 0], [0, 0]], 'out_tangents': [[0, 0], [0, 0]]}}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': f'{g}/contents/stroke/color', 'value': RISCO}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': f'{g}/contents/stroke/width', 'value': sorte.uniform(6, 9)}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': f'{g}/contents/stroke/taper/units', 'value': 1}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': f'{g}/contents/stroke/taper/endLength', 'value': 100}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': f'{g}/contents/stroke/taper/endWidth', 'value': 0}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': f'{g}/contents/stroke/taper/startLength', 'value': 25}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': f'{g}/contents/stroke/taper/startWidth', 'value': 30}}]
        # Estica do pé para trás (fim) e recolhe pelo começo.
        fim = [45, 90, 100, 100, 100, 100, 100, 100]
        inicio = [0, 15, 45, 80, 100, 100, 100, 100]
        s += [chave(nome, f'{g}/contents/trim/end', q, v) for q, v in enumerate(fim)]
        s += [chave(nome, f'{g}/contents/trim/start', q, v) for q, v in enumerate(inicio)]
    return s


def passos():
    s = [{'command': 'comp.new', 'params': {'name': 'poeira_dash_ec', 'width': LARGO, 'height': ALTO,
                                            'frameRate': FPS, 'duration': DESENHOS / FPS, 'background': [0, 0, 0]}}]
    s += camada_de_tufos('massa', SOMBRA, 1.0, 0.0)
    s += camada_de_tufos('luz', LUZ, 0.88, 0.41, 0.14)     # em cima e para a frente: sombra embaixo
    s += riscos()                                              # na frente da nuvem
    # A luz fica por cima e só aparece onde já há massa embaixo.
    s += [{'command': 'layer.setSwitch', 'params': {'layers': ['luz'], 'switch': 'preserveTransparency', 'value': True}}]
    return s


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True, help='pasta da sequência PNG (fora do repositório)')
    a = p.parse_args()
    a.saida.mkdir(parents=True, exist_ok=True)
    for velho in a.saida.glob('poeira_dash_ec_*.png'):
        velho.unlink()
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(passos(), f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(PROJETO), '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / 'poeira_dash_ec_[####].png'
    subprocess.run([str(a.cli), 'run', str(PROJETO),
                    'renderQueue.add', json.dumps({'comp': 'poeira_dash_ec', 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', PROJETO)
    print('quadros:', len(list(a.saida.glob('poeira_dash_ec_*.png'))))


if __name__ == '__main__':
    main()
