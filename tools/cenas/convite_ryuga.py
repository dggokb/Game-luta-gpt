#!/usr/bin/env python3
"""A luta animada à mão do convite do Ryūga (Ryūga × Yin-lu), a coreografia do dono em quatro
sequências, escrita como quadros-chave (60 por segundo) e gravada na ação "animar" de
android/app/src/main/assets/historia/ep02/cinematica_convite_ryuga.json.

    python3 tools/cenas/convite_ryuga.py

x: distância do meio entre os dois no começo, ao longo do eixo do Ryūga para a Yin-lu (o Ryūga
começa no negativo); y: altura acima do chão. A gramática é a do vídeo de palitinho de referência:
antecipação segurando, a arrancada esticada com rastro em poucos quadros, o contato parado (o
impacto congela), o recuo, a pausa de quem se estuda e a câmera cortando seco.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CENA = ROOT / 'android/app/src/main/assets/historia/ep02/cinematica_convite_ryuga.json'


class Lutador:
    def __init__(self):
        self.chaves = []
        self.x = 0.0

    def k(self, q, pose, x=None, **kw):
        """Uma chave no quadro q; x sem valor repete o último."""
        if x is not None:
            self.x = x
        c = {'q': q, 'x': round(self.x, 1), 'pose': pose}
        for nome, v in kw.items():
            c[nome] = v
        if self.chaves and self.chaves[-1]['q'] > q:
            raise ValueError(f'chave fora de ordem no quadro {q}')
        self.chaves.append(c)


R, Y = Lutador(), Lutador()
eventos = []


def ev(q, acao, **kw):
    eventos.append({'q': q, 'acao': dict(acao=acao, **kw)})


def camera(q, zoom, foco='centro', segundos=0.0):
    ev(q, 'camera', zoom=zoom, foco=foco, segundos=segundos)


def linhas(q, estilo=None, escuro=0.0, segundos=0.08):
    if estilo:
        ev(q, 'linhas', estilo=estilo, escuro=escuro, segundos=segundos)
    else:
        ev(q, 'linhas', ligar=False, segundos=segundos)


# A pausa no contato por força (s): o golpe defendido, o forte e o IMPACTO final. Mais longas que as
# do motor (13/11, o dono: "tá tudo muito acelerado, os golpes perdem o impacto").
CONGELA = {1: 0.18, 2: 0.3, 3: 0.6}


def impacto(q, quem, forca, **kw):
    kw.setdefault('congela', CONGELA[forca])
    ev(q, 'impacto', quem=quem, forca=forca, **kw)


def lenta(q, antes=4, depois=18, valor=0.35):
    """Câmera lenta em volta do contato do quadro q (a animação anda a `valor` e volta ao normal)."""
    ev(q - antes, 'velocidade', valor=valor, segundos=0.05, semEsperar=True)
    ev(q + depois, 'velocidade', valor=1.0, segundos=0.2, semEsperar=True)


def efeito(q, quem, nome, **kw):
    ev(q, 'efeito', quem=quem, nome=nome, **kw)


def letreiro(q, texto, pos, tamanho, **kw):
    ev(q, 'onomatopeia', texto=texto, pos=pos, tamanho=tamanho, semEsperar=True, **kw)


def respirar(lut, q0, q1, pose, x, passo=16):
    """Ofegante: o peito sobe e desce (sy) na pose, de q0 a q1."""
    q, cima = q0, True
    while q < q1:
        lut.k(q, pose, x, sy=1.0 if cima else 0.965, sx=1.0 if cima else 1.02, curva='suave')
        q += passo
        cima = not cima


DASH = dict(sx=1.32, sy=0.84, rastro=6)

# ------------------------------------------------------------------ Sequência 1: retomada
# Os dois em guarda; close na Yin-lu, que sorri e chama. Enquanto ela fala, o Ryūga anda
# para perto (fora do quadro).
R.k(0, 'IDLE:0', -210, ciclo=15)
Y.k(0, 'IDLE:0', 210, ciclo=14)
camera(0, 1.15)
camera(18, 1.9, 'rival')
linhas(18, 'concentracao')
ev(22, 'fala', quem='p02', texto='Vamos ver o quanto você melhorou!', segura=0.6, lado='rival')
Y.k(24, 'TAUNT:4', ciclo=18)
R.k(40, 'WALK_FORWARD:0', -210, ciclo=30)
R.k(100, 'IDLE:0', -170, ciclo=15)
Y.k(118, 'IDLE:0', ciclo=14)
linhas(132)
camera(136, 1.25)

# Antecipação agachada e a arrancada: cruza a tela em 8 quadros.
Y.k(140, 'LAND:2', 210, curva='corte')
efeito(150, 'rival', 'fumaca_dash_v05')
linhas(150, 'velocidade', escuro=0.15)
Y.k(150, 'DASH:6', 210, curva='corte', **DASH)
Y.k(158, 'DASH:9', -40, curva='sai', **DASH)
# 1) O soco: ele joga o corpo para trás e passa no vento.
Y.k(159, 'LIGHT_JAB:3', -42)
Y.k(162, 'LIGHT_JAB:6', -50, curva='sai', rastro=2)
R.k(157, 'IDLE:0', -170)
R.k(160, 'HIT_HEAVY:1', -197, curva='sai')
Y.k(167, 'LIGHT_JAB:9', -45)
# 2) O chute alto: ele defende.
R.k(166, 'DEFENSE_STAND:1', -195)
Y.k(169, 'JOELHO_GARCA:6', -42)
Y.k(173, 'JOELHO_GARCA:9', -48, curva='sai', rastro=3)
R.k(173, 'DEFENSE_STAND:4', -195)
impacto(173, 'rival', 1)
R.k(175, 'DEFENSE_STAND:5', -195)
R.k(183, 'DEFENSE_STAND:5', -215, curva='sai')
Y.k(181, 'JOELHO_GARCA:11', -45)
# 3) A rasteira: ele pula por cima.
Y.k(185, 'CROUCH_MEDIUM:5', -50)
Y.k(189, 'CROUCH_MEDIUM:7', -62, curva='sai', rastro=3)
R.k(184, 'CROUCH:6', -215)
efeito(188, 'jogador', 'fumaca_pulo')
R.k(188, 'JUMP:3', -220, y=60, curva='sai')
R.k(194, 'JUMP:6', -235, y=150, curva='sai')
Y.k(197, 'CROUCH_MEDIUM:8', -62)
camera(194, 1.3, 'centro', 0.12)
# No alto ele gira e desce com o chute; ela cruza os braços.
R.k(202, 'JUMP:7', -232, y=170, rot=-360)
R.k(203, 'JUMP_HEAVY:7', -232, y=170, rot=28, curva='corte', rastro=5)
R.k(211, 'JUMP_HEAVY:8', -188, y=70, rot=28, curva='entra', rastro=5)
Y.k(203, 'DEFENSE_CROUCH:1', -62)
Y.k(209, 'DEFENSE_CROUCH:2', -62)
impacto(211, 'jogador', 2, letreiro='DOOOM!')
lenta(211)
efeito(212, 'rival', 'fumaca_queda_v01', escala=1.2)
linhas(212)
# O ricochete: ele volta no ar e pousa longe; ela afunda e escorrega.
R.k(213, 'BACKDASH:3', -188, y=80, rot=0, curva='corte', rastro=3)
R.k(230, 'BACKDASH:8', -290, y=40, curva='sai')
R.k(236, 'LAND:1', -310, y=0, curva='entra')
efeito(236, 'jogador', 'fumaca_pouso')
R.k(242, 'LAND:4', -318, curva='sai')
R.k(252, 'IDLE:0', -318, ciclo=15)
Y.k(213, 'DEFENSE_CROUCH:3', -62)
Y.k(228, 'DEFENSE_CROUCH:4', 20, curva='sai', rastro=2)
efeito(214, 'rival', 'fumaca_dash_v05', atras=True, escala=0.8)
Y.k(238, 'DEFENSE_CROUCH:6', 25)
Y.k(246, 'IDLE:0', 25, ciclo=14)
camera(218, 1.05, 'centro', 0.35)

# ------------------------------------------------------------------ Sequência 2: alta velocidade
# Os dois se estudam e arrancam ao mesmo tempo; a câmera corta a cada golpe.
R.k(272, 'LAND:2', -318, curva='corte')
Y.k(272, 'LAND:2', 25, curva='corte')
camera(272, 1.1)
linhas(280, 'velocidade', escuro=0.2)
for quem, lut, x0, x1 in (('jogador', R, -318, -205), ('rival', Y, 25, -75)):
    efeito(280, quem, 'fumaca_dash_v05')
    lut.k(280, 'DASH:6', x0, curva='corte', **DASH)
    lut.k(288, 'DASH:9', x1, curva='sai', **DASH)
# b1: jab dele, ela defende.
camera(288, 1.65, 'ryuga')
R.k(289, 'LIGHT_JAB:2', -205)
R.k(292, 'LIGHT_JAB:5', -198, curva='sai', rastro=2)
Y.k(289, 'DEFENSE_STAND:2', -75)
Y.k(292, 'DEFENSE_STAND:4', -75)
impacto(292, 'jogador', 1)
R.k(299, 'LIGHT_JAB:9', -206)
Y.k(298, 'DEFENSE_STAND:5', -76)
# b2: a palma dela, ele defende.
Y.k(301, 'SPECIAL_S3:9', -76)
Y.k(304, 'SPECIAL_S3:12', -86, curva='sai', rastro=2)
R.k(301, 'DEFENSE_STAND:2', -208)
R.k(304, 'DEFENSE_STAND:4', -208)
impacto(304, 'rival', 1)
Y.k(312, 'SPECIAL_S3:16', -60)
R.k(311, 'DEFENSE_STAND:6', -228)
# b3: os dois socam juntos, os punhos se chocam e os dois deslizam para trás.
camera(314, 1.3)
R.k(315, 'MEDIUM_KICK:3', -235)
Y.k(315, 'LIGHT_JAB:3', -42)
R.k(319, 'MEDIUM_KICK:6', -222, curva='sai', rastro=3)
Y.k(319, 'LIGHT_JAB:6', -52, curva='sai', rastro=3)
impacto(319, 'jogador', 2)
R.k(321, 'MEDIUM_KICK:12', -222)
Y.k(321, 'LIGHT_JAB:8', -52)
R.k(333, 'MEDIUM_KICK:15', -255, curva='sai')
Y.k(333, 'LIGHT_JAB:10', -15, curva='sai')
# b4: o chute alto dela, ele se abaixa.
Y.k(337, 'HEAVY_STRAIGHT:4', -50)
Y.k(341, 'HEAVY_STRAIGHT:7', -70, curva='sai', rastro=4)
R.k(335, 'CROUCH:3', -232)
R.k(339, 'CROUCH:7', -228)
Y.k(349, 'HEAVY_STRAIGHT:12', -70)
# O combo dele: soco no abdômen (defendido), gancho (defendido), chute giratório (ela recua).
camera(350, 1.75, 'ryuga')
R.k(350, 'SOCO_PLEXO:3', -245)
R.k(353, 'SOCO_PLEXO:4', -215, curva='sai', rastro=3)
Y.k(350, 'DEFENSE_STAND:2', -70)
Y.k(353, 'DEFENSE_STAND:4', -70)
impacto(353, 'jogador', 1)
R.k(358, 'CROUCH_HEAVY:2', -220)
R.k(361, 'CROUCH_HEAVY:6', -205, y=12, curva='sai', rastro=3)
Y.k(358, 'DEFENSE_STAND:3', -70)
Y.k(361, 'DEFENSE_STAND:4', -70)
impacto(361, 'jogador', 1)
camera(368, 1.25)
R.k(368, 'HEAVY_STRAIGHT:2', -218, y=0)
R.k(371, 'HEAVY_STRAIGHT:4', -222)
R.k(375, 'HEAVY_STRAIGHT:7', -225, curva='sai', rastro=4)
Y.k(371, 'DEFENSE_STAND:4', -70)
impacto(375, 'jogador', 2)
# Ela é empurrada e escorrega pelo chão; agachada, sorri.
Y.k(377, 'DEFENSE_STAND:5', -70, rastro=3)
Y.k(396, 'LAND:2', 125, curva='sai')
efeito(377, 'rival', 'fumaca_dash_v05', atras=True)
R.k(383, 'HEAVY_STRAIGHT:13', -215)
R.k(392, 'HEAVY_STRAIGHT:18', -215)
R.k(404, 'IDLE:0', -215, ciclo=15)
Y.k(404, 'LAND:4', 125)
linhas(398)
camera(404, 1.95, 'rival')
linhas(404, 'concentracao')
ev(406, 'fala', quem='p02', texto='Nada mal!', segura=0.4, lado='rival')
Y.k(412, 'TAUNT:8', 125, ate=12)
Y.k(452, 'TAUNT:12', 125)
linhas(468)

# ------------------------------------------------------------------ Sequência 3: a superioridade
# Ele parte para cima correndo; ela some com um dash e surge às costas dele.
camera(470, 1.1)
linhas(476, 'velocidade', escuro=0.2)
R.k(470, 'LAND:2', -215, curva='corte')
efeito(476, 'jogador', 'fumaca_dash_v05')
R.k(476, 'DASH:4', -215, ciclo=24, sx=1.15, sy=0.92, rastro=4, curva='corte')
Y.k(470, 'CROUCH:3', 125, curva='corte')
Y.k(486, 'CROUCH:4', 125)
# O sumiço: some em 3 quadros (o rastro fica) e reaparece atrás dele.
linhas(488)
letreiro(488, 'SHUN!', [0.62, 0.36], 120, rotacao=-8, segundos=0.4)
Y.k(488, 'DASH:6', 125, alfa=1.0, rastro=6, sx=1.4, sy=0.8)
Y.k(491, 'DASH:7', 110, alfa=0.0, rastro=6, sx=1.4, sy=0.8)
R.k(491, 'DASH:4', -60, ciclo=24, sx=1.15, sy=0.92, rastro=4, vira=True)
Y.k(500, 'SPECIAL_S3:8', -110, alfa=0.0)
R.k(500, 'LAND:1', 10, vira=True)
camera(500, 1.5, 'ryuga')
R.k(506, 'LAND:2', 30, curva='sai', vira=True)
Y.k(504, 'SPECIAL_S3:9', -105, alfa=0.0, curva='corte')
Y.k(507, 'SPECIAL_S3:10', -105, alfa=1.0)
# Ele se vira e leva a palma.
R.k(508, 'IDLE:0', 30)
Y.k(510, 'SPECIAL_S3:12', -95, curva='sai', rastro=2)
impacto(510, 'rival', 2, letreiro='PAH!')
lenta(510)
R.k(510, 'HIT_BODY:1', 30, rastro=2)
R.k(524, 'HIT_BODY:3', 100, curva='sai')
camera(514, 1.25, 'centro', 0.2)
Y.k(518, 'SPECIAL_S3:15', -95)
# Ela segue com o chute alto; ele defende.
Y.k(524, 'DASH:8', -95, sx=1.2, sy=0.9, rastro=3)
Y.k(530, 'DASH:9', -40, curva='sai', sx=1.2, sy=0.9, rastro=3)
R.k(527, 'DEFENSE_STAND:1', 100)
Y.k(531, 'JOELHO_GARCA:6', -40)
Y.k(535, 'JOELHO_GARCA:9', -45, curva='sai', rastro=3)
R.k(535, 'DEFENSE_STAND:4', 102)
impacto(535, 'rival', 1)
# Ele gira o corpo e tenta pegar o braço dela; ela usa o impulso dele e o projeta.
camera(542, 1.45)
R.k(542, 'HEAVY_STRAIGHT:3', 100)
R.k(547, 'THROW_GRAB:3', 72, curva='sai', sx=1.08)
Y.k(541, 'JOELHO_GARCA:12', -42)
Y.k(545, 'THROW_GRAB:1', -40)
Y.k(549, 'THROW_GRAB:2', -36)
Y.k(552, 'THROW_GRAB:3', -36)
impacto(554, 'rival', 1)
Y.k(555, 'THROW_TOSS:2', -36)
Y.k(561, 'THROW_TOSS:4', -38)
Y.k(574, 'THROW_TOSS:7', -40)
Y.k(590, 'IDLE:0', -40, ciclo=14)
R.k(552, 'KNOCKDOWN:0', 50, y=30, rastro=4)
R.k(558, 'KNOCKDOWN:1', 0, y=150, rot=-90, curva='sai', rastro=4)
R.k(566, 'KNOCKDOWN:2', -110, y=135, rot=-200, rastro=4)
R.k(574, 'JUMP:6', -185, y=60, rot=-360, rastro=3)
R.k(575, 'JUMP:6', -190, y=55, rot=0, curva='corte')
R.k(580, 'GETUP:8', -215, y=0, curva='entra')
efeito(580, 'jogador', 'fumaca_pouso')
efeito(581, 'jogador', 'fumaca_dash_v05', atras=True)
R.k(598, 'GETUP:8', -300, curva='sai')
R.k(606, 'GETUP:10', -300)
R.k(612, 'GETUP:11', -300)
R.k(620, 'IDLE:0', -300, ciclo=15)
camera(580, 1.1, 'centro', 0.3)
# As falas do monstro.
camera(624, 1.8, 'ryuga')
ev(626, 'fala', quem='ryuga', texto='Você continua sendo um monstro!', segura=0.5, expressao='bravo')
camera(722, 1.8, 'rival')
ev(724, 'fala', quem='p02', texto='E você continua falando demais!', segura=0.5, lado='rival')
Y.k(724, 'TAUNT:8', -40, ate=12)
Y.k(760, 'TAUNT:12', -40)
Y.k(800, 'IDLE:0', -40, ciclo=14)

# ------------------------------------------------------------------ Sequência 4: o último confronto
camera(818, 1.0)
letreiro(822, 'HAH... HAH...', [0.5, 0.3], 70, segundos=1.4)
respirar(R, 818, 900, 'LAND:5', -300)
respirar(Y, 818, 900, 'LAND:6', -40)
# Ele fecha os punhos.
camera(904, 1.75, 'ryuga')
camera(906, 2.0, 'ryuga', 1.6)
linhas(904, 'concentracao', escuro=0.2)
R.k(904, 'SPECIAL_S2:2', -300, curva='corte')
for i, q in enumerate(range(910, 998, 3)):
    R.k(q, 'SPECIAL_S2:2', -300 + (1.5 if i % 2 else -1.5))
ev(908, 'fala', quem='ryuga', texto='Eu não vim até aqui pra perder!', segura=0.6, expressao='bravo')
Y.k(904, 'IDLE:0', -40, ciclo=14)
linhas(1000)
# Os dois disparam.
camera(1002, 1.15)
R.k(1002, 'LAND:2', -300, curva='corte')
Y.k(1002, 'LAND:2', -40, curva='corte')
linhas(1008, 'velocidade', escuro=0.25)
for quem, lut, x0, x1 in (('jogador', R, -300, -215), ('rival', Y, -40, -85)):
    efeito(1008, quem, 'fumaca_dash_v05')
    lut.k(1008, 'DASH:6', x0, curva='corte', **DASH)
    lut.k(1016, 'DASH:9', x1, curva='sai', **DASH)
# O soco direto: ela se abaixa.
camera(1016, 1.5)
R.k(1017, 'MEDIUM_KICK:3', -215)
R.k(1020, 'MEDIUM_KICK:6', -200, curva='sai', rastro=3)
Y.k(1017, 'CROUCH:3', -85)
Y.k(1020, 'CROUCH:6', -88)
R.k(1026, 'MEDIUM_KICK:12', -205)
# O chute alto dela: ele se abaixa, gira e sobe com o gancho; ela salta para trás.
Y.k(1025, 'JOELHO_GARCA:6', -85)
Y.k(1029, 'JOELHO_GARCA:9', -90, curva='sai', rastro=4)
R.k(1027, 'CROUCH:3', -210)
R.k(1030, 'CROUCH:7', -212)
R.k(1033, 'HEAVY_STRAIGHT:3', -210)
camera(1034, 1.2)
R.k(1036, 'CROUCH_HEAVY:2', -205)
R.k(1040, 'CROUCH_HEAVY:6', -175, y=40, curva='sai', rastro=5)
R.k(1048, 'CROUCH_HEAVY:8', -160, y=70, curva='sai')
Y.k(1036, 'JOELHO_GARCA:12', -88)
Y.k(1038, 'BACKDASH:3', -75, y=30, rastro=3)
Y.k(1050, 'BACKDASH:4', 70, y=130, curva='sai', rastro=3)
# Ele acompanha com um salto e desce com o golpe; ela pousa e cruza os braços.
efeito(1048, 'jogador', 'fumaca_pulo')
R.k(1050, 'JUMP:3', -140, y=120, curva='sai')
R.k(1058, 'JUMP_HEAVY_DOWN:6', -50, y=140, curva='sai')
R.k(1062, 'JUMP_HEAVY_DOWN:10', -25, y=130, rastro=6)
R.k(1068, 'JUMP_HEAVY_DOWN:14', 5, y=60, curva='entra', rastro=6)
Y.k(1058, 'LAND:1', 125, y=0, curva='entra')
efeito(1058, 'rival', 'fumaca_pouso')
Y.k(1062, 'DEFENSE_CROUCH:1', 128)
Y.k(1066, 'DEFENSE_CROUCH:2', 128)
camera(1060, 1.25)
linhas(1062, 'concentracao', escuro=0.45, segundos=0.05)
# Os golpes se cruzam: IMPACTO!, o clarão de dois quadros e a câmera volta devagar.
impacto(1068, 'jogador', 3)
lenta(1068, antes=8, depois=24, valor=0.3)
ev(1068, 'clarao', segundos=0.05)
letreiro(1068, 'IMPACTO!', [0.5, 0.4], 190, tremor=18, cor='#FF5A3C', segundos=0.9)
efeito(1069, 'rival', 'fumaca_queda_v01', escala=1.5)
linhas(1070, segundos=0.4)
camera(1070, 1.1, 'centro', 2.2)
# Os dois são jogados para longe um do outro e ficam ofegantes.
R.k(1070, 'BACKDASH:5', 5, y=90, rastro=4)
R.k(1092, 'BACKDASH:19', -140, y=0, curva='sai')
R.k(1096, 'LAND:2', -150)
efeito(1096, 'jogador', 'fumaca_pouso')
Y.k(1070, 'DEFENSE_CROUCH:3', 128, rastro=3)
Y.k(1090, 'DEFENSE_CROUCH:4', 210, curva='sai')
efeito(1071, 'rival', 'fumaca_dash_v05', atras=True)
Y.k(1104, 'DEFENSE_CROUCH:6', 212)
respirar(R, 1110, 1290, 'LAND:5', -150)
respirar(Y, 1116, 1290, 'LAND:6', 212)
letreiro(1128, 'HAH... HAH...', [0.5, 0.3], 80, segundos=2.0)
QUADROS = 1290

# As pausas (quadro da coreografia: quadros a mais): os dois seguram a pose daquele quadro e o
# resto anda para a frente. É onde a cena respira: a antecipação, o golpe esticado depois do
# contato, a pausa de quem se estuda. Só em quadros onde os dois têm chave ou estão parados.
PAUSAS = {
    140: 10,   # ela agachada antes de arrancar
    162: 6,    # o soco esticado no vento
    173: 10,   # o chute alto na defesa dele
    189: 4,    # a rasteira esticada
    194: 6,    # ele parado no alto antes de girar
    252: 22,   # os dois se estudam depois do DOOOM
    272: 10,   # a antecipação dos dois antes de arrancar
    292: 8,    # o jab na defesa dela
    304: 8,    # a palma na defesa dele
    319: 12,   # os punhos se chocando
    341: 6,    # o chute alto passando por cima
    353: 6,    # o soco no abdômen
    361: 6,    # o gancho
    375: 12,   # o chute giratório na defesa dela
    396: 12,   # ela agachada depois de escorregar
    470: 10,   # ele agachado antes de correr
    506: 14,   # ele parado, procurando por ela (ela invisível atrás)
    535: 8,    # o chute dela na defesa dele
    547: 6,    # a mão dele no braço dela
    620: 12,   # ele de pé depois de escorregar
    1002: 12,  # a antecipação dos dois antes do último confronto
    1020: 6,   # o soco dele no vento
    1029: 6,   # o chute dela no vento
    1058: 10,  # ele no alto, ela pousando
}


def curva(nome, t):
    """A mesma curva de chegada do jogo (AnimacaoManual.curva)."""
    t = max(0.0, min(1.0, t))
    if nome == 'sai':
        return 1 - (1 - t) ** 3
    if nome == 'entra':
        return t ** 3
    if nome == 'suave':
        return t * t * (3 - 2 * t)
    if nome == 'corte':
        return 1.0 if t >= 1 else 0.0
    return t


def pausar(jogador, rival, eventos, quadros, pausas):
    """Insere as pausas: depois do quadro p, n quadros com os dois segurando a pose de p."""
    def novo(q):
        return q + sum(n for p, n in pausas.items() if q > p)

    def um(chaves):
        out = []
        for c in chaves:
            c2 = dict(c)
            c2['q'] = novo(c['q'])
            out.append(c2)
        for p, n in sorted(pausas.items()):
            antes = [c for c in chaves if c['q'] <= p]
            depois = [c for c in chaves if c['q'] > p]
            a = antes[-1]
            if 'ate' in a:
                raise ValueError(f'pausa no quadro {p}: a chave {a["q"]} anima com "ate"')
            parada = {k: v for k, v in a.items() if k not in ('curva', 'rastro')}
            if a['q'] != p and depois:
                # No meio de um movimento: a pose de p (os números interpolados como no jogo) ganha
                # uma chave em p, e a pausa segura nela.
                b = depois[0]
                t = curva(b.get('curva', 'linear'), (p - a['q']) / (b['q'] - a['q']))
                for k, padrao in (('x', 0), ('y', 0), ('sx', 1), ('sy', 1), ('rot', 0), ('alfa', 1)):
                    va, vb = a.get(k, padrao), b.get(k, padrao)
                    if va != vb or k in a:
                        parada[k] = round(va + (vb - va) * t, 2)
                chave_p = dict(parada)
                chave_p['q'] = novo(p)
                chave_p['curva'] = b.get('curva', 'linear')
                if a.get('rastro'):
                    chave_p['rastro'] = a['rastro']
                out.append(chave_p)
            copia = dict(parada)
            copia['q'] = novo(p) + n
            out.append(copia)
        out.sort(key=lambda c: c['q'])
        return out

    evs = []
    for e in eventos:
        e2 = dict(e)
        e2['q'] = novo(e['q'])
        evs.append(e2)
    return um(jogador), um(rival), evs, novo(quadros) + 0


def _linha(o):
    return json.dumps(o, ensure_ascii=False)


def escrever(cena):
    """O arquivo legível: uma ação por linha; os quadros da página e as chaves da animação, um por linha."""
    linhas = ['{', f' "_comentario": {_linha(cena["_comentario"])},', f' "gatilho": {_linha(cena["gatilho"])},', ' "acoes": [']
    acoes = cena['acoes']
    for n, a in enumerate(acoes):
        fim = ',' if n + 1 < len(acoes) else ''
        listas = [k for k, v in a.items() if isinstance(v, list) and v and isinstance(v[0], dict)]
        if not listas:
            linhas.append(f'  {_linha(a)}{fim}')
            continue
        simples = {k: v for k, v in a.items() if k not in listas}
        cab = _linha(simples)[:-1]
        partes = [f'  {cab},']
        for j, k in enumerate(listas):
            itens = ',\n'.join(f'    {_linha(i)}' for i in a[k])
            partes.append(f'   "{k}": [\n{itens}]' + (',' if j + 1 < len(listas) else ''))
        partes.append(f'  }}{fim}')
        linhas.append('\n'.join(partes))
    linhas.append(' ]')
    linhas.append('}')
    return '\n'.join(linhas) + '\n'


def main():
    # No mesmo quadro o impacto vai por último: ele congela a animação, e o que vem depois
    # dele (o clarão, o letreiro) esperaria a pausa acabar.
    eventos.sort(key=lambda e: (e['q'], e['acao']['acao'] == 'impacto'))
    jogador, rival, evs, total = pausar(R.chaves, Y.chaves, eventos, QUADROS, PAUSAS)
    anim = {'acao': 'animar', 'quadros': total, 'jogador': jogador, 'rival': rival, 'eventos': evs}
    cena = json.loads(CENA.read_text(encoding='utf-8'))
    acoes = cena['acoes']
    i = next(n for n, a in enumerate(acoes) if a.get('acao') == 'animar')
    acoes[i] = anim
    cena['_comentario'] = (
        'Cena da luta do convite do Ryūga, animada à mão (ação animar, motor de luta parado), gerada por '
        'tools/cenas/convite_ryuga.py: a coreografia do dono (13/11) em quatro sequências, na gramática do '
        'vídeo de palitinho de referência. 1) a Yin-lu chama, arranca e solta soco (no vento), chute alto '
        '(defendido) e rasteira (ele pula), ele gira no ar e desce com o chute nos braços cruzados dela; 2) os '
        'dois arrancam, trocam golpes com a câmera cortando, os punhos se chocam, o combo dele (soco no '
        'abdômen, gancho, chute giratório) a faz escorregar, "Nada mal!"; 3) ele corre, ela some e surge às '
        'costas dele com a palma, ele defende o chute, tenta pegar o braço e é projetado, gira no ar e '
        'escorrega com a mão no chão, as falas do monstro; 4) ofegantes, "Eu não vim até aqui pra perder!", os '
        'dois disparam: o soco (ela se abaixa), o chute alto (ele se abaixa), o gancho (ela salta para trás), '
        'o salto com o golpe descendo nos braços cruzados dela: IMPACTO, clarão de dois quadros, a câmera volta '
        'devagar e os dois ofegantes. A coreografia com QTE do motor de luta está em '
        'cinematica_convite_ryuga_qte.json.')
    CENA.write_text(escrever(cena), encoding='utf-8')
    print(f'{CENA.relative_to(ROOT)}: {total} quadros ({total / 60:.1f} s, sem as pausas de contato e a '
          f'câmera lenta), {len(jogador)} chaves do Ryūga, {len(rival)} da Yin-lu, {len(evs)} eventos')


if __name__ == '__main__':
    main()
