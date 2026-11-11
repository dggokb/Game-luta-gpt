#!/usr/bin/env python3
"""Os impactos forte, médio e fraco redesenhados pelo dono (art/vfx/impactos_v06, 6 quadros de
512 × 512 cada, pivô de contato em 210,260) com o acabamento do EffectCraft pedido em 11/11:
- forte: o desenho + halo laranja + o anel de pressão de ar em 3D (o mesmo da V4 no jogo);
- médio: o desenho + um spray de ar em círculo: riscos brancos de ar em volta do contato que
  nascem no meio na batida e correm para as pontas, sumindo nelas (técnica de After Effects:
  traço afinando nas pontas, Repeater em volta do centro, Trim Paths revelando do meio para fora
  e recolhendo atrás; duas voltas de riscos com tamanhos e atrasos diferentes);
- fraco: só o desenho (nada a acrescentar; vai direto para o efeito_video.py).

    python3 art/vfx/effectcraft/impactos_v06_ec.py --saida PASTA [--cli CAMINHO]

Sai PASTA/forte e PASTA/medio (sequências PNG). No jogo os três nascem na ponta desenhada do golpe
de quem bate (HitEvent.pontaX/Y, GameView.efeitoDeGolpe) e seguem a inclinação do golpe; o forte no
pivô da folha (210,260; centrado no meio do desenho ele nascia no meio da perna), médio e fraco no
meio do desenho, a poucos px do pivô:
    (o forte pontudo saiu do jogo em 11/11; era: efeito_video.py PASTA/forte --nome impacto_v06_forte_ec
     --fps 20 --origem 210,260 --escala-mundo 0.48)
    efeito_video.py PASTA/medio --nome impacto_v06_medio_ec --fps 24 --origem 200,248 --escala-mundo 0.48
    efeito_video.py art/vfx/impactos_v06/leve --nome impacto_v06_fraco --fps 30 --origem 214,250 --escala-mundo 0.60
O forte do jogo (o desenho do médio sem o spray, com o anel):
    efeito_video.py PASTA/forte_medio --nome impacto_v06_forte_medio --fps 24 --origem 200,248 --escala-mundo 0.60
O segundo hit forte do dono (art/vfx/impacto_forte_v08) com o anel:
    efeito_video.py PASTA/forte_v08 --nome impacto_v08_forte --fps 24 --origem 258,252 --escala-mundo 0.50
(o v07, art/vfx/impacto_forte_v07, foi reprovado). Desde 11/11 o alfa de todas as folhas é
multiplicado por 0,8 antes do efeito_video.py (pedido "tira mais 10% de opacidade").
"""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
ARTE = AQUI.parent / 'impactos_v06'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')
LADO = 512
PIVO = (210.0, 260.0)
FPS = {'forte': 20, 'medio': 24, 'forte_medio': 24, 'forte_v07': 24, 'forte_v08': 24}

# O anel do dano_forte (0,30 px do mundo por px) na escala destes desenhos (0,70); 6 quadros.
K = 0.30 / 0.70
ANEL_GIRO = (-10, 66)
ANEL_RAIOS = [None] + [r * K for r in (100, 280, 385, 445, 470)]
ANEL_TRACOS = [None] + [t * K for t in (22, 16, 10, 5, 2)]
ANEL_OPAC = [0, 100, 88, 56, 18, 0]
ANEL_AVANCO = [d * K for d in (0, 30, 75, 100, 111, 113)]
ANEL_EROSAO = [300, 300, 200, 70, -30, -100]

# O spray do médio, por quadro (6 a 24 por segundo, 250 ms): o fim do risco corre do meio para
# fora e o começo vem atrás, então cada risco nasce no contato e some na ponta.
SPRAY = [
    # (raio de dentro, raio de fora, cópias, giro inicial, largura, atraso em quadros, fim %, começo %)
    # Pedidos de 11/11: "mais riscos e menores", depois "menos pontiagudos e mais finos" —
    # três voltas de riscos curtos e finos, de ponta redonda, afinando pouco.
    (40, 150, 22, 0, 3, 0, [0, 60, 90, 100, 100, 100], [0, 0, 35, 70, 92, 100]),
    (60, 190, 20, 8.2, 2.5, 1, [0, 0, 55, 88, 100, 100], [0, 0, 0, 40, 75, 100]),
    (30, 120, 18, 4.1, 2.5, 1, [0, 0, 65, 95, 100, 100], [0, 0, 10, 55, 85, 100]),
]


def t(q, fps):
    return q / fps


def prop(camada, caminho, valor):
    return {'command': 'prop.set', 'params': {'layer': camada, 'path': caminho, 'value': valor}}


def chaves(camada, caminho, valores, fps):
    return [{'command': 'prop.addKey', 'params': {'layer': camada, 'path': caminho, 'time': t(q, fps), 'value': v}}
            for q, v in enumerate(valores) if v is not None]


def base(nome, item, fps, lado=LADO):
    return [{'command': 'comp.new', 'params': {'name': nome, 'width': lado, 'height': lado, 'frameRate': fps,
                                               'duration': 6 / fps}},
            {'command': 'engine.batch', 'params': {'steps': [
                {'command': 'layer.addItem', 'params': {'item': item}},
                {'command': 'layer.rename', 'params': {'layer': '$1.layer', 'name': 'arte'}}]}}]


def forte(item):
    fps = FPS['forte']
    s = base('forte', item, fps)
    s += [{'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': ['arte']}},
          prop('arte', 'effects/#1/threshold', 45), prop('arte', 'effects/#1/radius', 22),
          prop('arte', 'effects/#1/intensity', 0.8), prop('arte', 'effects/#1/colors', 1),
          prop('arte', 'effects/#1/colorA', '#ff8a1f'), prop('arte', 'effects/#1/colorB', '#c2280c'),
          {'command': 'layer.setSwitch', 'params': {'layers': ['arte'], 'switch': 'threeD', 'value': True}}]
    return s + anel(fps)


def anel(fps, pivo=PIVO, fator=1.0):
    """O anel de pressão de ar em 3D atravessando o desenho (o mesmo da V4 no jogo). fator: px da
    folha a mais por px destes desenhos, para o anel ficar do mesmo tamanho no mundo noutra escala."""
    s = []
    n = 'anel'
    s += [{'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': n, 'size': [200, 200],
                                                   'stroke': '#ffffff', 'strokeWidth': 20, 'fill': '#dff6ff'}},
          {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'repeater'}},
          prop(n, 'contents/repeater/copies', 2), prop(n, 'contents/repeater/transform/position', [0, 0]),
          prop(n, 'contents/repeater/transform/scale', [88, 88]), prop(n, 'contents/repeater/transform/endOpacity', 40),
          {'command': 'layer.setSwitch', 'params': {'layers': [n], 'switch': 'threeD', 'value': True}},
          prop(n, 'transform/rotationX', ANEL_GIRO[0]), prop(n, 'transform/rotationY', ANEL_GIRO[1])]
    s += chaves(n, 'transform/position', [[pivo[0] + d * fator, pivo[1], 0] for d in ANEL_AVANCO], fps)
    s += chaves(n, 'contents/group/contents/ellipse/size',
                [None if r is None else [2 * r * fator, 2 * r * fator] for r in ANEL_RAIOS], fps)
    s += chaves(n, 'contents/group/contents/stroke/width', [None if v is None else v * fator for v in ANEL_TRACOS], fps)
    s += chaves(n, 'transform/opacity', ANEL_OPAC, fps)
    s += chaves(n, 'contents/group/contents/fill/opacity', [0, 8, 0], fps)
    s += [{'command': 'effect.apply', 'params': {'effect': 'Turbulent Displace', 'layers': [n]}},
          prop(n, 'effects/#1/amount', 4), prop(n, 'effects/#1/size', 90),
          {'command': 'effect.apply', 'params': {'effect': 'Fractal Noise', 'layers': [n]}},
          prop(n, 'effects/#2/contrast', 400), prop(n, 'effects/#2/overflow', 0),
          prop(n, 'effects/#2/transform/scale', 60), prop(n, 'effects/#2/complexity', 3),
          prop(n, 'effects/#2/evolutionOptions/seed', 5), prop(n, 'effects/#2/blendingMode', 19),
          {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': [n]}},
          prop(n, 'effects/#3/threshold', 50), prop(n, 'effects/#3/radius', 10), prop(n, 'effects/#3/intensity', 0.7),
          prop(n, 'effects/#3/colors', 1), prop(n, 'effects/#3/colorA', '#bff0ff'), prop(n, 'effects/#3/colorB', '#5cc8ff')]
    s += chaves(n, 'effects/#2/brightness', ANEL_EROSAO, fps)
    return s


def forte_medio(item):
    """Pedido de 11/11: o forte como o desenho do médio, um pouco maior (na escala da folha), sem
    o spray de ar e com o anel de pressão de ar em 3D do forte."""
    fps = FPS['forte_medio']
    s = base('forte_medio', item, fps)
    s += [{'command': 'layer.setSwitch', 'params': {'layers': ['arte'], 'switch': 'threeD', 'value': True}}]
    return s + anel(fps)


def forte_v07(item):
    """O hit forte novo desenhado pelo dono (art/vfx/impacto_forte_v07, 6 quadros de 640 × 640,
    centro em 320,320) com o anel de pressão de ar em 3D ("não esquece do círculo de pressão").
    Na folha a 0,54 (o forte_medio está a 0,60), então o anel cresce 0,60/0,54 para ficar do
    mesmo tamanho no mundo."""
    fps = FPS['forte_v07']
    s = base('forte_v07', item, fps, lado=640)
    s += [{'command': 'layer.setSwitch', 'params': {'layers': ['arte'], 'switch': 'threeD', 'value': True}}]
    return s + anel(fps, pivo=(320.0, 320.0), fator=0.60 / 0.54)


def forte_v08(item):
    """O segundo hit forte novo do dono (11/11, "aquele não tava bom"): art/vfx/impacto_forte_v08,
    desenhado em fundo preto com brilho; o recorte (o preto e o brilho fraco viram transparência,
    alfa = brilho de 150 a 235) está nos quadros de frames/. Centro em 258,252, 6 quadros de
    512 × 512, na folha a 0,50: o anel cresce 0,60/0,50 para ficar do tamanho do de hoje."""
    fps = FPS['forte_v08']
    s = base('forte_v08', item, fps)
    s += [{'command': 'layer.setSwitch', 'params': {'layers': ['arte'], 'switch': 'threeD', 'value': True}}]
    return s + anel(fps, pivo=(258.0, 252.0), fator=0.60 / 0.50)


def medio(item):
    fps = FPS['medio']
    s = base('medio', item, fps)
    n = 'spray'
    s += [{'command': 'layer.newShape', 'params': {'kind': 'none', 'name': n}},
          prop(n, 'transform/position', [*PIVO, 0])]
    for g_i, (r0, r1, copias, giro, larg, atraso, fim, comeco) in enumerate(SPRAY, start=1):
        g = 'contents/group' if g_i == 1 else f'contents/group#{g_i}'
        s += [{'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'group'}},
              {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'path', 'group': g}},
              {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'stroke', 'group': g}},
              {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'trim', 'group': g}},
              {'command': 'layer.addShapeItem', 'params': {'layer': n, 'kind': 'repeater', 'group': g}},
              prop(n, f'{g}/contents/path/path', {'closed': False, 'vertices': [[r0, 0], [r1, 0]],
                                                  'in_tangents': [[0, 0], [0, 0]], 'out_tangents': [[0, 0], [0, 0]]}),
              prop(n, f'{g}/contents/stroke/color', '#f4fbff'), prop(n, f'{g}/contents/stroke/width', larg),
              prop(n, f'{g}/contents/stroke/cap', 1),
              prop(n, f'{g}/contents/stroke/taper/units', 1),
              prop(n, f'{g}/contents/stroke/taper/startLength', 20), prop(n, f'{g}/contents/stroke/taper/startWidth', 45),
              prop(n, f'{g}/contents/stroke/taper/endLength', 30), prop(n, f'{g}/contents/stroke/taper/endWidth', 40),
              prop(n, f'{g}/contents/repeater/copies', copias),
              prop(n, f'{g}/contents/repeater/transform/position', [0, 0]),
              prop(n, f'{g}/contents/repeater/transform/rotation', 360 / copias),
              prop(n, f'{g}/transform/rotation', giro)]
        s += chaves(n, f'{g}/contents/trim/end', fim, fps)
        s += chaves(n, f'{g}/contents/trim/start', comeco, fps)
    s += [{'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': [n]}},
          prop(n, 'effects/#1/threshold', 50), prop(n, 'effects/#1/radius', 8), prop(n, 'effects/#1/intensity', 0.6),
          prop(n, 'effects/#1/colors', 1), prop(n, 'effects/#1/colorA', '#e6fbff'), prop(n, 'effects/#1/colorB', '#7fd6ff')]
    s += chaves(n, 'transform/opacity', [100, 100, 95, 85, 65, 0], fps)
    return s


def rodar(cli, nome, pasta_arte, montar, saida):
    import shutil
    saida.mkdir(parents=True, exist_ok=True)
    for velho in saida.glob('*.png'):
        velho.unlink()
    projeto = AQUI / f'impactos_v06_{nome}_ec.ecproj'
    primeiro = sorted(pasta_arte.glob('*.png'))[0]
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump([{'command': 'file.import', 'params': {'paths': [str(primeiro)], 'sequence': True, 'frameRate': FPS[nome]}}], f)
        lista = f.name
    r = subprocess.run([str(cli), 'run', '--empty', '--commands', lista, '--save-as', str(projeto), '--json'],
                       check=True, capture_output=True, text=True)
    resp = json.loads(r.stdout.strip().splitlines()[-1])
    resp = resp['result'] if isinstance(resp, dict) else resp
    item = resp[0]['result']['items'][0]
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(montar(item), f)
        lista = f.name
    subprocess.run([str(cli), 'run', str(projeto), '--commands', lista, '--save', '--json'], check=True, stdout=subprocess.DEVNULL)
    subprocess.run([str(cli), 'run', str(projeto),
                    'renderQueue.add', json.dumps({'comp': nome, 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida / f'{nome}_[####].png')}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    trava = AQUI / f'.{projeto.name}.lock'
    if trava.exists():
        trava.unlink()
    print(nome, len(list(saida.glob('*.png'))), 'quadros;', projeto)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True)
    a = p.parse_args()
    rodar(a.cli, 'forte', ARTE / 'forte', forte, a.saida / 'forte')
    rodar(a.cli, 'medio', ARTE / 'medio', medio, a.saida / 'medio')
    rodar(a.cli, 'forte_medio', ARTE / 'medio', forte_medio, a.saida / 'forte_medio')
    rodar(a.cli, 'forte_v07', AQUI.parent / 'impacto_forte_v07' / 'frames', forte_v07, a.saida / 'forte_v07')
    rodar(a.cli, 'forte_v08', AQUI.parent / 'impacto_forte_v08' / 'frames', forte_v08, a.saida / 'forte_v08')


if __name__ == '__main__':
    main()
