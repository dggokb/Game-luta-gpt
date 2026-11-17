#!/usr/bin/env python3
"""O treino do Episódio 1 (Ryūga × Kōga no dojo), animado à mão (ação "animar") com os STAMP,
gravado em android/app/src/main/assets/historia/ep01/cinematica_treino.json.

    python3 tools/cenas/treino_ep01.py

O roteiro é o do dono (16/11): os dois se distanciam e provocam; troca 1, os punhos se chocam
(STAMP) e os dois são jogados para trás, "Hehehe"; troca 2, o Kōga solta duas sequências e o
Ryūga desvia, o Ryūga responde com soco e chute e o Kōga desvia, soco por soco e os chutes se
cruzam (STAMP); troca 3, o Ryūga pressiona, o Kōga recua e encaixa um soco, a voadora do Kōga na
defesa do Ryūga (STAMP), ele cai na frente e leva o chute (STAMP), "Maldito..."; troca 4, os socos
em alta velocidade, cada vez mais rápidos, um STAMP a cada aceleração; o grito, os dois pulam para
trás, gritam o nome do outro e correm: o último STAMP é o primeiro quadro da página 7 (os dois
correndo um para o outro), e a história segue lendo a página dali (o DING DONG é o quadro seguinte).

As chaves são por irmão ("ryuga" e "koga"): a mesma cena nas duas campanhas (na do Kōga os lados
trocam e os STAMP batem espelhados). x: distância do meio do cenário ao longo do eixo do Ryūga para
o Kōga (o Ryūga no negativo); "atual" é onde cada um está quando a cena começa; y: altura acima
do chão; 60 quadros por segundo.
"""
from pathlib import Path

from animacao import Lutador, escrever, pausar

ROOT = Path(__file__).resolve().parents[2]
CENA = ROOT / 'android/app/src/main/assets/historia/ep01/cinematica_treino.json'

R, K = Lutador(), Lutador()
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


CONGELA = {1: 0.16, 2: 0.3, 3: 0.6}


def impacto(q, quem, forca, **kw):
    kw.setdefault('congela', CONGELA[forca])
    ev(q, 'impacto', quem=quem, forca=forca, **kw)


def lenta(q, antes=4, depois=14, valor=0.4):
    ev(q - antes, 'velocidade', valor=valor, segundos=0.05, semEsperar=True)
    ev(q + depois, 'velocidade', valor=1.0, segundos=0.2, semEsperar=True)


def efeito(q, quem, nome, **kw):
    ev(q, 'efeito', quem=quem, nome=nome, **kw)


def fala(q, quem, texto, segura=0.5, expressao='bravo'):
    ev(q, 'fala', quem=quem, texto=texto, segura=segura, expressao=expressao)


def stamp(n, **kw):
    """O STAMP n (a arte do dono, ep01/stamp_treinoN.jpg): o Ryūga está à esquerda nela."""
    return dict(imagem=f'ep01/stamp_treino{n}.jpg', ryugaNaArte='esquerda', **kw)


DASH_R = dict(sx=1.3, sy=0.86, rastro=6)
DASH_K = dict(sx=1.3, sy=0.86, rastro=6)


def arrancar(q, ate, xr0, xr1, xk0, xk1):
    """Os dois arrancam juntos em q e chegam em ate."""
    linhas(q, 'velocidade', escuro=0.2)
    for quem in ('ryuga', 'koga'):
        efeito(q, quem, 'fumaca_dash_v05')
    R.k(q, 'DASH:6', xr0, curva='corte', **DASH_R)
    R.k(ate, 'DASH:9', xr1, curva='sai', **DASH_R)
    K.k(q, 'DASH:1', xk0, curva='corte', **DASH_K)
    K.k(ate, 'DASH:4', xk1, curva='sai', **DASH_K)


# ------------------------------------------------------------------ Início
# A luta para onde estava; os dois saltam para trás e ficam em guarda, longe.
R.k(0, 'IDLE:0', 'atual', ciclo=15)
K.k(0, 'IDLE:0', 'atual', ciclo=14)
camera(0, 1.0, 'centro', 0.3)
R.k(6, 'BACKDASH:2', 'atual')
K.k(6, 'JUMP:1', 'atual')
R.k(20, 'BACKDASH:9', -265, y=45, curva='sai', rastro=3)
K.k(20, 'JUMP:3', 265, y=45, curva='sai', rastro=3)
R.k(32, 'BACKDASH:20', -300, y=0, curva='entra')
K.k(32, 'LAND:1', 300, y=0, curva='entra')
efeito(32, 'ryuga', 'fumaca_pouso')
efeito(32, 'koga', 'fumaca_pouso')
R.k(40, 'IDLE:0', -300, ciclo=15)
K.k(40, 'IDLE:0', 300, ciclo=14)
camera(44, 1.8, 'ryuga', 0.25)
linhas(44, 'concentracao')
fala(48, 'ryuga', 'Que foi, Kōga? Já tá cansado...?', expressao='normal')
linhas(190)
camera(194, 1.8, 'koga', 0.2)
linhas(194, 'concentracao')
fala(198, 'koga', 'Eu, maninho? Só tô começando!')
linhas(340)
camera(344, 1.12, 'centro', 0.3)

# ------------------------------------------------------------------ Troca 1: o soco mútuo
R.k(360, 'LAND:3', -300, curva='corte')
K.k(360, 'LAND:2', 300, curva='corte')
arrancar(370, 378, -300, -95, 300, 95)
camera(378, 1.35)
R.k(379, 'MEDIUM_KICK:3', -90)
K.k(379, 'MEDIUM_KICK:3', 90)
R.k(383, 'MEDIUM_KICK:6', -72, curva='sai', rastro=3)
K.k(383, 'MEDIUM_KICK:6', 72, curva='sai', rastro=3)
impacto(383, 'ryuga', 2, letreiro='BAKOOM!', stamp=stamp(1))
# O choque joga os dois para trás.
R.k(385, 'HIT_STAND:1', -72, rastro=3)
K.k(385, 'HIT_STAND:1', 72, rastro=3)
efeito(386, 'ryuga', 'fumaca_dash_v05', atras=True, escala=0.8)
efeito(386, 'koga', 'fumaca_dash_v05', atras=True, escala=0.8)
linhas(388)
R.k(404, 'HIT_STAND:3', -250, curva='sai')
K.k(404, 'HIT_STAND:3', 250, curva='sai')
R.k(414, 'IDLE:0', -250, ciclo=15)
K.k(414, 'IDLE:0', 250, ciclo=14)
camera(392, 1.1, 'centro', 0.3)
fala(420, 'ryuga', 'Hehehe.', segura=0.3, expressao='normal')
fala(470, 'koga', 'Hehehe.', segura=0.3, expressao='normal')

# ------------------------------------------------------------------ Troca 2
R.k(540, 'LAND:3', -250, curva='corte')
K.k(540, 'LAND:2', 250, curva='corte')
arrancar(548, 556, -250, -125, 250, 105)
camera(556, 1.45)
linhas(558)
# O Kōga solta duas sequências; o Ryūga se abaixa na primeira e salta para trás na segunda.
K.k(557, 'LIGHT_JAB:2', 105)
K.k(560, 'LIGHT_JAB:5', 92, curva='sai', rastro=2)
K.k(563, 'LIGHT_JAB:2', 100)
K.k(566, 'LIGHT_JAB:6', 90, curva='sai', rastro=2)
R.k(557, 'CROUCH:2', -125)
R.k(560, 'CROUCH:6', -130, curva='sai')
R.k(568, 'CROUCH:6', -130)
K.k(571, 'MEDIUM_KICK:3', 100)
K.k(575, 'MEDIUM_KICK:6', 82, curva='sai', rastro=3)
R.k(571, 'BACKDASH:3', -140, y=10)
R.k(576, 'BACKDASH:6', -190, y=28, curva='sai', rastro=2)
R.k(584, 'LAND:3', -200, y=0, curva='entra')
efeito(584, 'ryuga', 'fumaca_pouso')
K.k(584, 'MEDIUM_KICK:10', 95)
# O Ryūga avança com soco e chute; o Kōga se abaixa e salta para trás.
R.k(590, 'DASH:6', -200, curva='corte', rastro=3)
R.k(596, 'MEDIUM_KICK:3', -95, curva='sai')
R.k(600, 'MEDIUM_KICK:6', -80, curva='sai', rastro=3)
K.k(590, 'IDLE:0', 95)
K.k(597, 'CROUCH:3', 100)
K.k(600, 'CROUCH:5', 105, curva='sai')
R.k(606, 'HEAVY_STRAIGHT:4', -85)
R.k(611, 'HEAVY_STRAIGHT:7', -80, curva='sai', rastro=4)
K.k(606, 'CROUCH:5', 105)
K.k(608, 'JUMP:1', 115)
K.k(613, 'JUMP:3', 175, y=35, curva='sai', rastro=2)
K.k(620, 'LAND:1', 185, y=0, curva='entra')
efeito(620, 'koga', 'fumaca_pouso')
R.k(620, 'HEAVY_STRAIGHT:14', -80)
R.k(626, 'IDLE:0', -90, ciclo=15)
camera(626, 1.3)
# Soco por soco: cada um defende o do outro.
R.k(630, 'DASH:6', -90, curva='corte')
R.k(636, 'MEDIUM_KICK:3', -40, curva='sai')
R.k(640, 'MEDIUM_KICK:6', -30, curva='sai', rastro=3)
K.k(630, 'IDLE:0', 185)
K.k(634, 'DASH:2', 150, curva='sai')
K.k(638, 'DEFENSE_STAND:2', 95)
K.k(640, 'DEFENSE_STAND:3', 100)
impacto(640, 'ryuga', 1)
K.k(646, 'MEDIUM_KICK:3', 95)
K.k(650, 'MEDIUM_KICK:6', 80, curva='sai', rastro=3)
R.k(646, 'DEFENSE_STAND:2', -35)
R.k(650, 'DEFENSE_STAND:4', -40)
impacto(650, 'koga', 1)
# Chute com chute: as pernas se cruzam.
R.k(656, 'HEAVY_STRAIGHT:4', -50)
K.k(656, 'HEAVY_STRAIGHT:8', 92)
camera(658, 1.55, 'centro', 0.1)
R.k(662, 'HEAVY_STRAIGHT:12', -40, curva='sai', rastro=4)
K.k(662, 'HEAVY_STRAIGHT:12', 75, curva='sai', rastro=4)
impacto(662, 'ryuga', 2, letreiro='KRAKOOM!', stamp=stamp(2))
R.k(664, 'HEAVY_STRAIGHT:12', -40, rastro=2)
K.k(664, 'HEAVY_STRAIGHT:12', 75, rastro=2)
efeito(665, 'ryuga', 'fumaca_dash_v05', atras=True, escala=0.8)
efeito(665, 'koga', 'fumaca_dash_v05', atras=True, escala=0.8)
R.k(680, 'HEAVY_STRAIGHT:16', -170, curva='sai')
K.k(680, 'HEAVY_STRAIGHT:17', 200, curva='sai')
R.k(690, 'IDLE:0', -180, ciclo=15)
K.k(690, 'IDLE:0', 210, ciclo=14)
camera(690, 1.1, 'centro', 0.3)

# ------------------------------------------------------------------ Troca 3
# O Ryūga pressiona; o Kōga recua e encaixa um soco.
R.k(720, 'LAND:3', -180, curva='corte')
linhas(728, 'velocidade', escuro=0.2)
efeito(728, 'ryuga', 'fumaca_dash_v05')
R.k(728, 'DASH:6', -180, curva='corte', **DASH_R)
R.k(738, 'DASH:9', 20, curva='sai', **DASH_R)
K.k(726, 'IDLE:0', 210)
K.k(728, 'JUMP:1', 210)
K.k(736, 'JUMP:3', 300, y=30, curva='sai', rastro=2)
K.k(744, 'LAND:1', 320, y=0, curva='entra')
efeito(744, 'koga', 'fumaca_pouso')
R.k(742, 'DASH:6', 60, **DASH_R)
R.k(749, 'MEDIUM_KICK:3', 185, curva='sai')
K.k(746, 'MEDIUM_KICK:3', 320)
K.k(750, 'MEDIUM_KICK:6', 300, curva='sai', rastro=3)
camera(750, 1.35, 'ryuga', 0.08)
linhas(750)
impacto(750, 'koga', 2)
R.k(751, 'HIT_STAND:2', 185, rastro=3)
R.k(768, 'HIT_STAND:5', 30, curva='sai')
R.k(778, 'IDLE:0', 20, ciclo=15)
efeito(752, 'ryuga', 'fumaca_dash_v05', atras=True, escala=0.8)
K.k(760, 'MEDIUM_KICK:10', 305)
K.k(770, 'IDLE:0', 300, ciclo=14)
camera(780, 1.15, 'centro', 0.3)
# A voadora do Kōga na defesa do Ryūga.
K.k(790, 'JUMP:1', 300, curva='corte')
efeito(794, 'koga', 'fumaca_pulo')
K.k(796, 'SPECIAL_S4:5', 250, y=90, curva='sai')
K.k(804, 'SPECIAL_S4:6', 160, y=110, curva='sai', rastro=4)
K.k(812, 'SPECIAL_S4:7', 75, y=60, curva='entra', rastro=6)
R.k(800, 'DEFENSE_STAND:1', 20)
R.k(810, 'DEFENSE_STAND:3', 20)
linhas(804, 'concentracao', escuro=0.3)
camera(806, 1.45, 'centro', 0.1)
impacto(812, 'koga', 2, letreiro='DOOM!', stamp=stamp(3))
R.k(814, 'DEFENSE_STAND:5', 5, rastro=2)
R.k(824, 'DEFENSE_STAND:5', -30, curva='sai')
efeito(815, 'ryuga', 'fumaca_dash_v05', atras=True, escala=0.7)
# Ele cai na frente do Ryūga, que aproveita a abertura com o chute.
K.k(814, 'JUMP:4', 110, y=60)
K.k(826, 'LAND:1', 115, y=0, curva='entra')
efeito(826, 'koga', 'fumaca_pouso')
linhas(816)
K.k(832, 'LAND:3', 115)
R.k(830, 'HEAVY_STRAIGHT:3', -30)
R.k(836, 'HEAVY_STRAIGHT:5', -20)
R.k(840, 'HEAVY_STRAIGHT:12', -10, curva='sai', rastro=5)
camera(838, 1.4, 'koga', 0.08)
impacto(840, 'ryuga', 2, letreiro='KRAKOOM!', stamp=stamp(4))
K.k(842, 'HIT_STAND:2', 120, rastro=4)
K.k(862, 'HIT_STAND:4', 300, curva='sai')
K.k(872, 'LAND:3', 310)
efeito(843, 'koga', 'fumaca_dash_v05', atras=True)
R.k(856, 'HEAVY_STRAIGHT:16', -10)
R.k(870, 'IDLE:0', -20, ciclo=15)
camera(876, 1.8, 'koga', 0.25)
linhas(876, 'concentracao')
K.k(880, 'IDLE:0', 310, ciclo=14)
fala(880, 'koga', 'Maldito...')
linhas(972)
camera(972, 1.15, 'centro', 0.2)

# ------------------------------------------------------------------ Troca 4: alta velocidade
# Socos fracos e médios trocados cada vez mais rápido; a cada aceleração os dois socam juntos e
# um STAMP (os socos cruzando, cada vez mais de perto) bate.
R.k(980, 'LAND:3', -20, curva='corte')
K.k(980, 'LAND:2', 310, curva='corte')
arrancar(990, 998, -20, -70, 310, 70)
RECORTES = [
    [0.0, 0.0, 1.0, 1.0],
    [0.02, 0.08, 0.66, 0.72],
    [0.38, 0.05, 1.0, 0.67],
    [0.22, 0.28, 0.78, 0.84],
    [0.3, 0.36, 0.7, 0.76],
]
SEGURA = [0.55, 0.45, 0.38, 0.32, 0.3]
q = 1000
for bloco, periodo in enumerate([14, 11, 8, 6, 5]):
    camera(q, 1.3 + 0.1 * bloco, 'centro', 0.1)
    linhas(q, 'velocidade', escuro=0.15 + 0.05 * bloco)
    for i in range(4):
        golpe, prepara, estica = ('LIGHT_JAB', 2, 5) if i % 2 == 0 else ('MEDIUM_KICK', 3, 6)
        bate = max(1, periodo // 4)
        R.k(q, f'{golpe}:{prepara}', -70)
        R.k(q + bate, f'{golpe}:{estica}', -60, curva='sai', rastro=2)
        K.k(q, 'DEFENSE_STAND:2', 70)
        K.k(q + bate, 'DEFENSE_STAND:3', 72)
        meio = q + periodo // 2
        K.k(meio, f'{golpe}:{prepara}', 70)
        K.k(meio + bate, f'{golpe}:{estica}', 60, curva='sai', rastro=2)
        R.k(meio, 'DEFENSE_STAND:2', -70)
        R.k(meio + bate, 'DEFENSE_STAND:4', -72)
        q += periodo
    # Os dois socam juntos: os punhos cruzam e o STAMP bate.
    R.k(q, 'MEDIUM_KICK:3', -70)
    K.k(q, 'MEDIUM_KICK:3', 70)
    R.k(q + 3, 'MEDIUM_KICK:6', -58, curva='sai', rastro=3)
    K.k(q + 3, 'MEDIUM_KICK:6', 58, curva='sai', rastro=3)
    impacto(q + 3, 'ryuga', 1, stamp=stamp(5, recorte=RECORTES[bloco], segundos=SEGURA[bloco]))
    q += 8

# ------------------------------------------------------------------ O grito
# Os dois gritam no meio da troca, a mais rápida de todas, e pulam para trás.
camera(q, 1.5, 'centro', 0.15)
linhas(q, 'concentracao', escuro=0.35)
fala(q + 4, 'ryuga', 'AHHHHHHHHHHHHHH!', segura=0.2)
fala(q + 44, 'koga', 'AHHHHHHHHHHHHHH!', segura=0.2)
for i in range(18):
    golpe, prepara, estica = ('LIGHT_JAB', 2, 5) if i % 2 == 0 else ('MEDIUM_KICK', 3, 6)
    R.k(q, f'{golpe}:{prepara}', -70)
    K.k(q, f'{golpe}:{prepara}', 70)
    R.k(q + 2, f'{golpe}:{estica}', -60, curva='sai', rastro=2)
    K.k(q + 2, f'{golpe}:{estica}', 60, curva='sai', rastro=2)
    q += 5
linhas(q)
camera(q, 1.1, 'centro', 0.3)
R.k(q, 'BACKDASH:2', -70)
K.k(q, 'JUMP:1', 70)
R.k(q + 14, 'BACKDASH:9', -270, y=50, curva='sai', rastro=3)
K.k(q + 14, 'JUMP:3', 270, y=50, curva='sai', rastro=3)
R.k(q + 26, 'BACKDASH:20', -320, y=0, curva='entra')
K.k(q + 26, 'LAND:1', 320, y=0, curva='entra')
efeito(q + 26, 'ryuga', 'fumaca_pouso')
efeito(q + 26, 'koga', 'fumaca_pouso')
R.k(q + 34, 'IDLE:0', -320, ciclo=15)
K.k(q + 34, 'IDLE:0', 320, ciclo=14)
q += 40

# ------------------------------------------------------------------ O choque final
camera(q, 1.85, 'ryuga', 0.2)
linhas(q, 'concentracao', escuro=0.3)
fala(q + 4, 'ryuga', 'KOGAAAAAAAAAAAAA!', segura=0.4)
q += 110
camera(q, 1.85, 'koga', 0.2)
fala(q + 4, 'koga', 'RYŪGAAAAAAAAAAAAAAA!', segura=0.4)
q += 110
linhas(q)
camera(q, 1.1, 'centro', 0.2)
R.k(q, 'LAND:3', -320, curva='corte')
K.k(q, 'LAND:2', 320, curva='corte')
q += 10
# Os dois correm um para o outro, em câmera lenta no fim; o primeiro quadro da página bate antes do choque.
linhas(q, 'velocidade', escuro=0.25)
efeito(q, 'ryuga', 'fumaca_dash_v05')
efeito(q, 'koga', 'fumaca_dash_v05')
R.k(q, 'DASH:0', -320, ciclo=24, sx=1.15, sy=0.92, rastro=4, curva='corte')
K.k(q, 'DASH:0', 320, ciclo=18, sx=1.15, sy=0.92, rastro=4, curva='corte')
camera(q + 10, 1.3, 'centro', 0.4)
ev(q + 14, 'velocidade', valor=0.35, segundos=0.15, semEsperar=True)
R.k(q + 40, 'DASH:0', -110, ciclo=24, sx=1.15, sy=0.92, rastro=4)
K.k(q + 40, 'DASH:0', 110, ciclo=18, sx=1.15, sy=0.92, rastro=4)
QUADROS = q + 40

# As pausas (quadro: quadros a mais): a antecipação antes de cada arrancada.
PAUSAS = {360: 10, 540: 8, 720: 8, 980: 10}

# O último STAMP: o primeiro quadro da página 7 (os dois correndo um para o outro) vira a página.
STAMP_FINAL = {'acao': 'stamp', 'imagem': 'ep01/pag7.jpg', 'quadro': 0, 'segundos': 1.2,
               'viraPagina': True, 'vencedor': 'interrompida'}


def main():
    eventos.sort(key=lambda e: (e['q'], e['acao']['acao'] == 'impacto'))
    ryuga, koga, evs, total = pausar(R.chaves, K.chaves, eventos, QUADROS, PAUSAS)
    anim = {'acao': 'animar', 'quadros': total, 'meio': 'palco', 'ryuga': ryuga, 'koga': koga, 'eventos': evs}
    acoes = [
        {'acao': 'controle', 'travar': True},
        {'acao': 'velocidade', 'valor': 0.3, 'segundos': 0.2, 'semEsperar': True},
        {'acao': 'faixas', 'entrar': True, 'segundos': 0.8, 'semEsperar': True},
        {'acao': 'esperar', 'segundos': 0.6},
        {'acao': 'velocidade', 'valor': 1.0, 'segundos': 0.2, 'semEsperar': True},
        anim,
        STAMP_FINAL,
    ]
    cena = {
        '_comentario': (
            'O treino do Episódio 1 (Ryūga × Kōga no dojo), animado à mão (ação animar, motor de luta parado) '
            'com os STAMP, gerado por tools/cenas/treino_ep01.py com o roteiro do dono (16/11). Quando um dos '
            'dois chega a 25%: os dois saltam para trás e provocam; os punhos se chocam (STAMP); o Kōga ataca e '
            'o Ryūga desvia, o Ryūga ataca e o Kōga desvia, soco por soco e os chutes se cruzam (STAMP); o Kōga '
            'recua e encaixa um soco, a voadora dele na defesa do Ryūga (STAMP), ele cai na frente e leva o '
            'chute (STAMP); os socos em alta velocidade, um STAMP a cada aceleração; o grito, os nomes, a '
            'corrida, e o primeiro quadro da página 7 (os dois correndo) bate como STAMP e vira a página (o DING DONG vem no toque seguinte). As chaves '
            'são por irmão: a mesma cena nas duas campanhas. A cena antiga (a corrida com a campainha, no '
            'motor de luta) está em cinematica_treino_corrida.json.'),
        'gatilho': {'vida': 0.25},
        'acoes': acoes,
    }
    CENA.write_text(escrever(cena), encoding='utf-8')
    print(f'{CENA.relative_to(ROOT)}: {total} quadros ({total / 60:.1f} s sem as pausas de contato e a câmera '
          f'lenta), {len(ryuga)} chaves do Ryūga, {len(koga)} do Kōga, {len(evs)} eventos')


if __name__ == '__main__':
    main()
