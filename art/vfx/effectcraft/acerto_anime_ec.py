#!/usr/bin/env python3
"""Faísca de acerto forte em estilo anime feita do zero no EffectCraft (sem imagem de entrada),
com as técnicas que os tutoriais de After Effects usam, só com efeitos nativos.

Pedido de 10/11: "pesquise como fazer anime vfx no After Effects a partir do zero e faça um
exemplo". A pesquisa e as fontes estão em docs/vfx-continuar.md (seção "Anime VFX do zero").

Técnicas (After Effects, todas existem no EffectCraft):
- Tempo em degraus: Posterize Time em cada camada (anime "de dois em dois": 30 por segundo
  num efeito de 60 dá 8 desenhos em 16 quadros).
- Raios a partir de ruído: Fractal Noise esticado (sem escala uniforme, largura pequena e altura
  enorme, contraste alto e brilho baixo) vira listras; Polar Coordinates (retangular para polar)
  dobra as listras em raios saindo do centro. Unmult tira o preto; Levels dá a borda dura.
- Formas: estrela com muitas pontas e raio de dentro pequeno (os espinhos), Wiggle Paths para a
  ponta sair irregular, Repeater para os riscos em leque, Trim Paths para o risco sair e recolher.
- Luz: núcleo branco, Tint do laranja ao amarelo-claro, Glow em duas passadas (raio pequeno
  forte e raio grande fraco), como os tutoriais de brilho.

Números (reports/VFX de luta estilo anime.md, tabela da faísca):
- 16 quadros (267 ms). Corpo encolhe em linha reta até zero no quadro 10. Centro e riscos
  crescem um pouco nos 16. Camada de impacto atrás some no quadro 8 (opacidade 50 % caindo
  6,25 % por quadro). Clarão de 2 a 3 quadros. Giro de 30° por quadro. Camadas entrando de 0
  a 3 quadros uma depois da outra.

    python3 art/vfx/effectcraft/acerto_anime_ec.py --saida PASTA [--cli CAMINHO]

Sai: art/vfx/effectcraft/acerto_anime_ec.ecproj e PASTA/acerto_anime_ec_0000.png ... 0015.png.
"""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path

AQUI = Path(__file__).resolve().parent
PROJETO = AQUI / 'acerto_anime_ec.ecproj'
CLI_PADRAO = Path('/home/user/storytold/effectcraft/target/release/effectcraft-cli')

FPS = 60
QUADROS = 16
PASSO_ANIME = 30          # Posterize Time: desenho novo a cada 2 quadros de jogo
LADO = 512
C = (LADO / 2, LADO / 2)  # o ponto de contato; o golpe vem da esquerda e vai para a direita

BRANCO = '#ffffff'
AMARELO = '#fff2a8'
LARANJA = '#ff8a1f'
VERMELHO = '#e8401a'


def t(q):
    return q / FPS


def chaves(camada, caminho, valores):
    """Um quadro-chave por quadro de jogo (None pula), em degraus de dois em dois: o quadro
    ímpar repete o par (o Posterize Time não segura escala, giro e posição da própria camada; no
    After Effects também não, lá se pré-compõe). O número de cada desenho fica exato."""
    passo = FPS // PASSO_ANIME
    chaves_ = []
    for q, v in enumerate(valores):
        base = q - q % passo
        v = valores[base] if base < len(valores) and valores[base] is not None else v
        if v is not None:
            chaves_.append({'command': 'prop.addKey', 'params': {'layer': camada, 'path': caminho, 'time': t(q), 'value': v}})
    return chaves_


def efeito(camada, nome, valores=None):
    """Aplica o efeito e acerta os parâmetros (o caminho é o do último efeito da camada)."""
    passos = [{'command': 'effect.apply', 'params': {'effect': nome, 'layers': [camada]}}]
    for k, v in (valores or {}).items():
        passos.append({'command': 'prop.set', 'params': {'layer': camada, 'path': f'@ultimo/{k}', 'value': v}})
    return passos


def resolver(passos):
    """Troca '@ultimo' pelo índice do último efeito aplicado em cada camada."""
    conta = {}
    for p in passos:
        par = p['params']
        if p['command'] == 'effect.apply':
            for c in par['layers']:
                conta[c] = conta.get(c, 0) + 1
        caminho = par.get('path', '')
        if caminho.startswith('@ultimo/'):
            par['path'] = f"effects/#{conta[par['layer']]}/" + caminho[len('@ultimo/'):]
    return passos


def degraus(camada):
    return efeito(camada, 'Posterize Time', {'frameRate': PASSO_ANIME})


def brilho(camada, forte=1.4, largo=0.7):
    """O brilho dos tutoriais: uma passada curta e forte (as cores do próprio desenho) e uma
    larga e fraca pintada com as cores A e B do Glow (laranja para vermelho), que é o halo."""
    return (efeito(camada, 'Glow', {'threshold': 40, 'radius': 6, 'intensity': forte})
            + efeito(camada, 'Glow', {'threshold': 30, 'radius': 28, 'intensity': largo, 'colors': 1,
                                      'colorA': LARANJA, 'colorB': VERMELHO, 'colorLooping': 0}))


def forma(nome, tipo, **extra):
    p = {'kind': tipo, 'name': nome}
    p.update(extra)
    return [{'command': 'layer.newShape', 'params': p},
            {'command': 'prop.set', 'params': {'layer': nome, 'path': 'transform/position', 'value': [*C, 0]}}]


def escala(valores):
    return [None if v is None else [v, v, 100] for v in valores]


def passos():
    s = [{'command': 'comp.new', 'params': {'name': 'acerto_anime_ec', 'width': LADO, 'height': LADO,
                                            'frameRate': FPS, 'duration': QUADROS / FPS, 'background': [0, 0, 0]}}]

    # 1. Impacto (atrás de tudo): mancha laranja macia; some no quadro 8, de 50 % caindo 6,25 % por quadro.
    s += forma('impacto', 'ellipse', size=[300, 300], fill=LARANJA)
    s += efeito('impacto', 'Gaussian Blur', {'blurriness': 40})
    s += chaves('impacto', 'transform/opacity', [50 - 6.25 * q for q in range(9)])
    s += chaves('impacto', 'transform/scale', escala([60 + 8 * q for q in range(9)]))
    s += degraus('impacto')

    # 2. Estouro (o corpo): ruído esticado -> raios por coordenadas polares -> borda dura.
    #    Encolhe em linha reta até zero no quadro 10; o ruído muda a cada desenho (evolução).
    s += [{'command': 'layer.newSolid', 'params': {'name': 'estouro', 'color': '#000000', 'width': LADO, 'height': LADO}}]
    s += efeito('estouro', 'Fractal Noise', {
        'fractalType': 0, 'noiseType': 2, 'contrast': 420, 'brightness': -60, 'overflow': 0,
        'transform/uniformScaling': False, 'transform/scaleWidth': 4, 'transform/scaleHeight': 2400,  # listras em pé
        'complexity': 2})
    s += chaves('estouro', 'effects/#1/evolution', [37 * q for q in range(QUADROS)])
    # Erosão: o brilho do ruído cai e os raios afinam e somem pelas pontas.
    s += chaves('estouro', 'effects/#1/brightness', [-55, -50, -50, -55, -62, -70, -78, -86, -94, -100, -110])
    # No After Effects os tutoriais usam "Rect to Polar"; no EffectCraft 0.23.1 quem faz os raios
    # é a opção 1, que ele chama de "Polar to Rect" (os nomes parecem trocados; anotado no doc).
    s += efeito('estouro', 'Polar Coordinates', {'interpolation': 100, 'conversion': 1})
    s += efeito('estouro', 'Unmult')
    s += efeito('estouro', 'Tint', {'black': VERMELHO, 'white': AMARELO})
    s += [{'command': 'prop.set', 'params': {'layer': 'estouro', 'path': 'transform/anchor', 'value': [*C, 0]}},
          {'command': 'prop.set', 'params': {'layer': 'estouro', 'path': 'transform/position', 'value': [*C, 0]}}]
    # Gilland: o primeiro quadro é o mesmo de toda explosão; depois encolhe até zero no 10.
    s += chaves('estouro', 'transform/scale', escala([70, 115, 110, 100, 90, 78, 64, 50, 34, 18, 0]))
    s += brilho('estouro', 0.6, 0.9)
    s += degraus('estouro')
    # Os raios enchem o quadro: um disco macio por cima serve de máscara de alfa (track matte)
    # e encolhe junto com o corpo; as pontas dos raios saem apagando.
    s += forma('mascara', 'ellipse', size=[330, 330], fill=BRANCO)
    s += efeito('mascara', 'Gaussian Blur', {'blurriness': 70})
    s += chaves('mascara', 'transform/scale', escala([70, 115, 110, 100, 90, 78, 64, 50, 34, 18, 0]))
    s += degraus('mascara')
    s += [{'command': 'layer.setTrackMatte', 'params': {'layers': ['estouro'], 'matte': 'mascara', 'kind': 'alpha'}}]

    # 3. Riscos em leque: um risco fino que o Repeater copia em volta; sai rápido e freia
    #    (Ogawa), e recolhe pelo fundo com Trim Paths. Entra 1 quadro depois do estouro.
    s += forma('riscos', 'rect', size=[90, 5], fill=BRANCO)
    s += [{'command': 'layer.addShapeItem', 'params': {'layer': 'riscos', 'kind': 'repeater'}},
          {'command': 'prop.set', 'params': {'layer': 'riscos', 'path': 'contents/repeater/copies', 'value': 9}},
          {'command': 'prop.set', 'params': {'layer': 'riscos', 'path': 'contents/repeater/transform/position', 'value': [0, 0]}},
          {'command': 'prop.set', 'params': {'layer': 'riscos', 'path': 'contents/repeater/transform/rotation', 'value': 40}},
          {'command': 'prop.set', 'params': {'layer': 'riscos', 'path': 'contents/group/contents/rect/roundness', 'value': 3}}]
    afasta = [None, 60, 105, 135, 155, 168, 177, 183, 187, 190, 192, 193, 194, 195, 195, 195]
    s += chaves('riscos', 'contents/group/contents/rect/position', [None if d is None else [d, 0] for d in afasta])
    comprido = [None, 140, 120, 100, 80, 62, 46, 32, 20, 10, 4, 0, 0]
    s += chaves('riscos', 'contents/group/contents/rect/size', [None if c is None else [c, 6] for c in comprido])
    s += chaves('riscos', 'transform/opacity', [0, 0, 100])
    s += chaves('riscos', 'transform/rotation', [-12])
    s += efeito('riscos', 'Tint', {'black': LARANJA, 'white': AMARELO})
    s += brilho('riscos', 0.8, 0.4)
    s += degraus('riscos')

    # 4. Espinhos em raio (centro): estrela de 12 pontas com raio de dentro pequeno e Wiggle
    #    Paths para as pontas saírem desiguais. Gira 30° por quadro e cresce um pouco nos 16.
    #    Uma segunda cópia menor, girada, "para ficar algo no olho" (Ogawa).
    for nome, pontas, fora, gira, cor in (('espinhos', 12, 120, 0, LARANJA), ('espinhos2', 8, 80, 15, AMARELO)):
        s += forma(nome, 'star', fill=cor)
        s += [{'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/group/contents/star/points', 'value': pontas}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/group/contents/star/innerRadius', 'value': fora * 0.16}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/group/contents/star/outerRadius', 'value': fora}},
              {'command': 'layer.addShapeItem', 'params': {'layer': nome, 'kind': 'wiggle'}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/wiggle/size', 'value': fora * 0.18}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/wiggle/detail', 'value': 2}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/wiggle/points', 'value': 0}},
              {'command': 'prop.set', 'params': {'layer': nome, 'path': 'contents/wiggle/speed', 'value': 7.5}}]
        s += chaves(nome, 'transform/rotation', [gira + 30 * q for q in range(QUADROS)])
        s += chaves(nome, 'transform/scale', escala([55, 85, 92, 96, 99, 101, 103, 105, 107, 108, 109, 110, 111, 112, 113, 114]))
        # Opaca até o quadro 9; a cauda some em três desenhos.
        s += chaves(nome, 'transform/opacity', [100] * 10 + [75, 75, 45, 45, 0])
        s += brilho(nome, 0.5, 0.8)
        s += degraus(nome)

    # 5. Anel de choque: traço com Wiggle Paths (borda viva), nasce no quadro 2, abre e afina.
    s += forma('anel', 'ellipse', size=[200, 200], stroke=LARANJA, strokeWidth=10)
    s += [{'command': 'prop.set', 'params': {'layer': 'anel', 'path': 'contents/group/contents/fill/opacity', 'value': 0}},
          {'command': 'layer.addShapeItem', 'params': {'layer': 'anel', 'kind': 'wiggle'}},
          {'command': 'prop.set', 'params': {'layer': 'anel', 'path': 'contents/wiggle/size', 'value': 9}},
          {'command': 'prop.set', 'params': {'layer': 'anel', 'path': 'contents/wiggle/detail', 'value': 6}}]
    s += chaves('anel', 'transform/scale', escala([None, None, 40, 70, 95, 115, 130, 142, 150, 156, 160]))
    s += chaves('anel', 'contents/group/contents/stroke/width', [None, None, 14, 11, 8, 6, 4, 3, 2, 1, 0])
    s += chaves('anel', 'transform/opacity', [0, 0, 100, 100, 90, 75, 60, 45, 30, 15, 0])
    s += brilho('anel', 0.8, 0.4)
    s += degraus('anel')

    # 6. Clarão: branco puro no contato, 2 desenhos (4 quadros), o mais claro da tela.
    #    Toda lista termina em quadro par: o ímpar repete o par anterior.
    s += forma('clarao', 'ellipse', size=[170, 170], fill=BRANCO)
    s += efeito('clarao', 'Gaussian Blur', {'blurriness': 22})
    s += chaves('clarao', 'transform/scale', escala([100, 100, 125, 125, 0]))
    s += chaves('clarao', 'transform/opacity', [100, 100, 60, 60, 0])
    return resolver(s)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--cli', type=Path, default=CLI_PADRAO)
    p.add_argument('--saida', type=Path, required=True, help='pasta da sequência PNG (fora do repositório)')
    a = p.parse_args()
    a.saida.mkdir(parents=True, exist_ok=True)
    for velho in a.saida.glob('acerto_anime_ec_*.png'):
        velho.unlink()
    with tempfile.NamedTemporaryFile('w', suffix='.json', delete=False) as f:
        json.dump(passos(), f)
        lista = f.name
    subprocess.run([str(a.cli), 'run', '--empty', '--commands', lista, '--save-as', str(PROJETO), '--json'],
                   check=True, stdout=subprocess.DEVNULL)
    saida = a.saida / 'acerto_anime_ec_[####].png'
    subprocess.run([str(a.cli), 'run', str(PROJETO),
                    'renderQueue.add', json.dumps({'comp': 'acerto_anime_ec', 'format': 'png', 'channels': 'rgba',
                                                   'output': str(saida)}),
                    'renderQueue.render', '{"wait":true}', '--json'], check=True, stdout=subprocess.DEVNULL)
    print('projeto:', PROJETO)
    print('quadros:', len(list(a.saida.glob('acerto_anime_ec_*.png'))))


if __name__ == '__main__':
    main()
