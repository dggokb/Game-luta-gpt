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
PROJETO = AQUI / 'dano_forte_ec.ecproj'
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


def passos():
    s = [
        {'command': 'comp.new', 'params': {'name': 'dano_forte_ec', 'width': LARGO, 'height': ALTO,
                                           'frameRate': FPS, 'duration': len(DESENHOS) / FPS, 'background': [0, 0, 0]}},
        {'command': 'file.import', 'params': {'paths': [str(VIDEO)]}},
        {'command': 'engine.batch', 'params': {'steps': [
            {'command': 'layer.addItem', 'params': {'item': VIDEO.name}},
            {'command': 'layer.rename', 'params': {'layer': '$1.layer', 'name': 'dano'}},
        ]}},
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
    return s


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True, help='pasta da sequência PNG (fora do repositório)')
    a = p.parse_args()
    a.saida.mkdir(parents=True, exist_ok=True)
    for velho in a.saida.glob('dano_forte_ec_*.png'):
        velho.unlink()
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(passos(), f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(PROJETO), '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / 'dano_forte_ec_[####].png'
    subprocess.run([str(a.cli), 'run', str(PROJETO),
                    'renderQueue.add', json.dumps({'comp': 'dano_forte_ec', 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', PROJETO)
    print('quadros:', len(list(a.saida.glob('dano_forte_ec_*.png'))))


if __name__ == '__main__':
    main()
