#!/usr/bin/env python3
"""Monta a faísca de defesa no EffectCraft (https://github.com/storytold/effectcraft), só pela
linha de comando, e exporta a sequência PNG com alfa.

Teste de ferramenta (11/11): ver se o EffectCraft serve para fabricar os efeitos de ponto. O
desenho vem da imagem gerada que já está no jogo (art/vfx/defesa_01.png, regra 2 da skill
vfx-luta: forma não se desenha em código); o EffectCraft corta, recompõe e anima. Só o clarão e
o anel são formas dele (degradê e onda, que o relatório diz que saem bem de código).

Receita e números: reports/VFX de luta estilo anime.md, a tabela da faísca (P.o.t.S, Atif, Ogawa).
- 8 quadros a 60 por segundo (133 ms). Opaca nos quadros 0 a 3, depois perde um quarto por quadro
  (75, 50, 25, 0 %), crescendo sempre.
- Camadas, de baixo para cima, com início em degrau de 0 a 3 quadros:
  anel (onda de choque, nasce no quadro 1), escudo (o crescente), faíscas (os estilhaços que
  ricocheteiam: saem rápido e freiam), estrela (o espinho em raio do centro, gira 30° por
  quadro), clarão (2 a 3 quadros, no ponto de contato).
- A escala não vai em todas as camadas (Ogawa: "com cara de barato"): o escudo cresce, as faíscas
  andam e esticam, a estrela gira, o clarão estoura e some.

    python3 art/vfx/effectcraft/defesa_ec.py [--cli CAMINHO] [--saida PASTA]

Sai: art/vfx/effectcraft/defesa_ec.ecproj (o projeto) e PASTA/defesa_ec_0000.png ... 0007.png.
"""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
IMAGEM = AQUI.parent / 'defesa_01.png'        # 1024 x 1024, luz em fundo preto
PROJETO = AQUI / 'defesa_ec.ecproj'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

FPS = 60
QUADROS = 8
LADO = 1024
# O ponto de contato na imagem (o mesmo --origem 0.40,0.48 da defesa de hoje).
CONTATO = (410.0, 492.0)

# Opacidade da faísca de defesa: opaca de 0 a 3, depois um quarto a menos por quadro.
OPAC_DEFESA = [100, 100, 100, 100, 75, 50, 25, 0]


def t(q):
    return q / FPS


def chaves(camada, caminho, valores):
    """Um quadro-chave por quadro desenhado: o número de cada quadro fica exato."""
    return [{'command': 'prop.addKey', 'params': {'layer': camada, 'path': caminho, 'time': t(q), 'value': v}}
            for q, v in enumerate(valores) if v is not None]


def ponto(camada, imagem=True):
    """Âncora e posição no contato: tudo cresce e gira a partir do ponto onde o golpe bateu. Na
    imagem a âncora é o pixel do contato; na forma é o meio dela (0, 0)."""
    return [
        {'command': 'prop.set', 'params': {'layer': camada, 'path': 'transform/anchor',
                                           'value': [*CONTATO, 0] if imagem else [0, 0, 0]}},
        {'command': 'prop.set', 'params': {'layer': camada, 'path': 'transform/position', 'value': [*CONTATO, 0]}},
    ]


def pedaco_da_imagem(nome, mascara, forma, pena):
    """Uma cópia da imagem, com o preto virado transparência (Unmult) e só um pedaço (máscara)."""
    return [
        {'command': 'engine.batch', 'params': {'steps': [
            {'command': 'layer.addItem', 'params': {'item': IMAGEM.name, 'index': 1}},
            {'command': 'layer.rename', 'params': {'layer': '$1.layer', 'name': nome}},
        ]}},
        {'command': 'effect.apply', 'params': {'effect': 'Unmult', 'layers': [nome]}},
        # O "quase preto" do gerador não vira névoa (o efeito_imagem.py corta 3 %).
        {'command': 'prop.set', 'params': {'layer': nome, 'path': 'effects/#1/blackLevel', 'value': 3}},
        {'command': 'layer.addMask', 'params': {'layer': nome, 'shape': forma, 'rect': mascara}},
        {'command': 'layer.mask.set', 'params': {'layer': nome, 'mask': 1, 'field': 'feather', 'value': pena}},
    ] + ponto(nome)


def passos():
    s = [
        {'command': 'comp.new', 'params': {'name': 'defesa_ec', 'width': LADO, 'height': LADO, 'frameRate': FPS,
                                           'duration': QUADROS / FPS, 'background': [0, 0, 0]}},
        {'command': 'file.import', 'params': {'paths': [str(IMAGEM)]}},
    ]

    # 1. Anel: onda de choque achatada no plano da guarda, nasce no quadro 1 (Atif: 2 a 3 quadros
    #    entre uma camada e a seguinte; a inferência do relatório: anel de 0 a 3 depois).
    s += [
        {'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': 'anel', 'size': [300, 620],
                                                 'stroke': '#3fb8ff', 'strokeWidth': 12}},
        # A forma nova vem com preenchimento: o anel é só o traço.
        {'command': 'prop.set', 'params': {'layer': 'anel', 'path': 'contents/group/contents/fill/opacity', 'value': 0}},
    ] + ponto('anel', imagem=False) + [
        {'command': 'prop.set', 'params': {'layer': 'anel', 'path': 'transform/position', 'value': [CONTATO[0] + 40, CONTATO[1], 0]}},
        {'command': 'effect.apply', 'params': {'effect': 'Glow', 'layers': ['anel']}},
        {'command': 'prop.set', 'params': {'layer': 'anel', 'path': 'effects/#1/radius', 'value': 18}},
        {'command': 'prop.set', 'params': {'layer': 'anel', 'path': 'effects/#1/threshold', 'value': 30}},
        {'command': 'prop.set', 'params': {'layer': 'anel', 'path': 'effects/#1/intensity', 'value': 0.6}},
    ]
    s += chaves('anel', 'transform/scale', [None] + [[40 + 13 * i, 40 + 13 * i, 100] for i in range(7)])
    s += chaves('anel', 'transform/opacity', [0, 90, 80, 60, 40, 25, 10, 0])

    # 2. Escudo: o crescente da imagem. Cresce sempre (92 -> 113 %), curva da defesa.
    s += pedaco_da_imagem('escudo', [372, 100, 340, 780], 'rect', 18)
    s += chaves('escudo', 'transform/scale', [[92 + 3 * q, 92 + 3 * q, 100] for q in range(QUADROS)])
    s += chaves('escudo', 'transform/opacity', OPAC_DEFESA)

    # 3. Faíscas: os estilhaços que voltam para o lado do golpe. Saem rápido e freiam (resistência
    #    do ar, Ogawa), esticados na direção em que voam nos primeiros quadros.
    s += pedaco_da_imagem('faiscas', [160, 260, 220, 470], 'rect', 22)
    recuo = [0, -22, -38, -50, -58, -63, -66, -68]
    s += chaves('faiscas', 'transform/position', [[CONTATO[0] + d, CONTATO[1], 0] for d in recuo])
    s += chaves('faiscas', 'transform/scale', [[85 + 5 * q, 85 + 5 * q, 100] for q in range(QUADROS)])
    s += chaves('faiscas', 'transform/opacity', OPAC_DEFESA)
    s += [{'command': 'effect.apply', 'params': {'effect': 'Directional Blur', 'layers': ['faiscas']}},
          {'command': 'prop.set', 'params': {'layer': 'faiscas', 'path': 'effects/#2/direction', 'value': 90}}]
    s += chaves('faiscas', 'effects/#2/length', [14, 10, 6, 3, 0, 0, 0, 0])

    # 4. Estrela: o espinho em raio do centro. Giro de 30° por quadro (P.o.t.S); começa em 0
    #    porque a folha é fixa (o sorteio do ângulo inicial seria do jogo).
    s += pedaco_da_imagem('estrela', [CONTATO[0] - 95, CONTATO[1] - 95, 190, 190], 'ellipse', 30)
    s += chaves('estrela', 'transform/rotation', [30 * q for q in range(QUADROS)])
    s += chaves('estrela', 'transform/scale', [[80 + 8 * q, 80 + 8 * q, 100] for q in range(QUADROS)])
    s += chaves('estrela', 'transform/opacity', OPAC_DEFESA)

    # 5. Clarão: branco no contato, 3 quadros (Atif: "uns 2 ou 3"), o mais claro da tela.
    s += [
        {'command': 'layer.newShape', 'params': {'kind': 'ellipse', 'name': 'clarao', 'size': [150, 150],
                                                 'fill': '#ffffff'}},
    ] + ponto('clarao', imagem=False) + [
        {'command': 'effect.apply', 'params': {'effect': 'Gaussian Blur', 'layers': ['clarao']}},
        {'command': 'prop.set', 'params': {'layer': 'clarao', 'path': 'effects/#1/blurriness', 'value': 28}},
    ]
    s += chaves('clarao', 'transform/scale', [[70, 70, 100], [130, 130, 100], [150, 150, 100], [150, 150, 100]])
    s += chaves('clarao', 'transform/opacity', [100, 90, 40, 0])
    return s


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True, help='pasta da sequência PNG (fora do repositório)')
    a = p.parse_args()
    a.saida.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(passos(), f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(PROJETO), '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / 'defesa_ec_[####].png'
    subprocess.run([str(a.cli), 'run', str(PROJETO),
                    'renderQueue.add', json.dumps({'comp': 'defesa_ec', 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', PROJETO)
    print('quadros:', sorted(x.name for x in a.saida.glob('defesa_ec_*.png')))


if __name__ == '__main__':
    main()
