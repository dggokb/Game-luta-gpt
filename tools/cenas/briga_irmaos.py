#!/usr/bin/env python3
"""A briga dos irmãos crianças (Prólogo), animada à mão (ação "animar"), gravada em
android/app/src/main/assets/historia/ep00/cinematica_briga.json.

    python3 tools/cenas/briga_irmaos.py

As chaves são por irmão ("ryuga" e "koga"), não por jogador e rival: a cena é a mesma nas duas
campanhas e cada menino faz o seu papel (o Kōga, esquentado, ataca primeiro; o Ryūga, mais calmo,
acerta o soco no fim, como na página 5). x: distância do meio do cenário ao longo do eixo do
Ryūga para o Kōga (o Ryūga começa no negativo); y: altura acima do chão; 60 quadros por segundo.

O roteiro é o da briga em vídeo de antes: o Mestre Dafu manda irem devagar, os dois trocam golpes
desajeitados, os punhos se chocam, a troca de golpes acaba com os dois se acertando e caindo, o
Kōga levanta primeiro e avança num soco que assusta o Ryūga, e o soco do Ryūga pega o Kōga na cara.
"""
from pathlib import Path

from animacao import Lutador, amostra, cortar, escrever, pausar

ROOT = Path(__file__).resolve().parents[2]
CENA = ROOT / 'android/app/src/main/assets/historia/ep00/cinematica_briga.json'

R, K = Lutador(), Lutador()
eventos = []


def ev(q, acao, apos=0, **kw):
    e = {'q': q, 'acao': dict(acao=acao, **kw)}
    if apos:
        e['apos'] = apos
    eventos.append(e)


def camera(q, zoom, foco='centro', segundos=0.0):
    ev(q, 'camera', zoom=zoom, foco=foco, segundos=segundos)


def linhas(q, estilo=None, escuro=0.0, segundos=0.08):
    if estilo:
        ev(q, 'linhas', estilo=estilo, escuro=escuro, segundos=segundos)
    else:
        ev(q, 'linhas', ligar=False, segundos=segundos)


# A pausa no contato por força (s), as mesmas do convite (o golpe precisa de peso).
CONGELA = {1: 0.16, 2: 0.3, 3: 0.8}


def impacto(q, quem, forca, **kw):
    kw.setdefault('congela', CONGELA[forca])
    ev(q, 'impacto', quem=quem, forca=forca, **kw)


def lenta(q, antes=4, depois=18, valor=0.35):
    ev(q - antes, 'velocidade', valor=valor, segundos=0.05, semEsperar=True)
    ev(q + depois, 'velocidade', valor=1.0, segundos=0.2, semEsperar=True)


def efeito(q, quem, nome, **kw):
    kw.setdefault('escala', 0.75)
    ev(q, 'efeito', quem=quem, nome=nome, **kw)


def letreiro(q, texto, pos, tamanho, **kw):
    ev(q, 'onomatopeia', texto=texto, pos=pos, tamanho=tamanho, semEsperar=True, **kw)


def respirar(lut, q0, q1, pose, x, passo=14):
    """Ofegante: o peito sobe e desce (sy) na pose, de q0 a q1."""
    q, cima = q0, True
    while q < q1:
        lut.k(q, pose, x, sy=1.0 if cima else 0.96, sx=1.0 if cima else 1.025, curva='suave')
        q += passo
        cima = not cima


DASH = dict(sx=1.25, sy=0.88, rastro=5)
# O vídeo da troca de golpes (ep00/qt1: 62 quadros a 12 por segundo, 5146 ms) e quantos quadros
# da cena ele dura no ritmo escolhido.
VIDEO_RITMO = 1.2
VIDEO_QUADROS = round(5146 / VIDEO_RITMO * 60 / 1000)

# ------------------------------------------------------------------ 1: a troca desajeitada
# Os dois em guarda; o Dafu manda irem devagar e eles nem ouvem: andam um para o outro.
R.k(0, 'IDLE:0', -175, ciclo=15)
K.k(0, 'IDLE:0', 175, ciclo=14)
camera(0, 1.2)
ev(6, 'fala', quem='dafu', expressao='normal', lado='esquerda', texto='Ei, ei! Devagar, meninos!', segura=0.8)
R.k(24, 'WALK_FORWARD:0', -175, ciclo=26)
K.k(18, 'WALK_FORWARD:0', 175, ciclo=22)
R.k(60, 'IDLE:0', -140, ciclo=15)
K.k(56, 'IDLE:0', 125, ciclo=14)
# A câmera fecha nos dois: o Kōga perde a paciência.
camera(62, 1.35)
linhas(62, 'concentracao')
K.k(76, 'CROUCH:3', 125, curva='corte')
linhas(86)
camera(86, 1.25)
# Ele arranca e solta o soco esticado; o Ryūga se abaixa atrasado, de olho fechado, e o soco passa
# por cima. O Kōga vai junto com o soco e quase cai para a frente.
efeito(86, 'koga', 'fumaca_dash_v05')
linhas(86, 'velocidade', escuro=0.12)
K.k(86, 'DASH:2', 125, curva='corte', **DASH)
K.k(94, 'DASH:8', -40, curva='sai', **DASH)
K.k(95, 'CROUCH_LIGHT:2', -40)
K.k(98, 'CROUCH_LIGHT:4', -60, curva='sai', rastro=3)
R.k(93, 'IDLE:0', -140)
R.k(96, 'CROUCH:3', -140)
R.k(98, 'CROUCH:6', -142, curva='sai')
letreiro(98, 'VUSH!', [0.5, 0.3], 80, rotacao=-6, segundos=0.5)
linhas(99)
K.k(104, 'CROUCH_LIGHT:5', -78, rot=-8, curva='sai')
K.k(110, 'CROUCH_LIGHT:7', -72, rot=4, curva='suave')
K.k(116, 'CROUCH_LIGHT:9', -66, rot=0, curva='suave')
# O Ryūga responde de baixo com o gancho; o Kōga pula para trás, o gancho passa no vento e o
# Ryūga gira junto com ele.
R.k(108, 'CROUCH_HEAVY:3', -142)
R.k(114, 'CROUCH_HEAVY:6', -132, curva='sai', rastro=3)
K.k(117, 'BACKDASH:1', -66)
K.k(121, 'BACKDASH:4', -25, y=24, curva='sai')
K.k(130, 'BACKDASH:12', 45, y=12)
K.k(136, 'BACKDASH:27', 55, y=0, curva='entra')
efeito(136, 'koga', 'fumaca_pouso')
K.k(142, 'IDLE:0', 55, ciclo=14)
letreiro(114, 'VUSH!', [0.44, 0.26], 80, rotacao=8, segundos=0.45)
R.k(120, 'CROUCH_HEAVY:9', -128, rot=10, curva='sai')
R.k(128, 'CROUCH_HEAVY:10', -130, rot=-5, curva='suave')
R.k(134, 'IDLE:0', -130, rot=0, ciclo=15, curva='suave')
# O Ryūga avança com o soco; o Kōga recua um passo e o soco sai curto. O Ryūga se estica demais e
# tropeça para a frente.
R.k(142, 'CROUCH:2', -130, curva='corte')
efeito(148, 'ryuga', 'fumaca_dash_v05')
R.k(148, 'DASH:3', -130, curva='corte', **DASH)
R.k(155, 'DASH:8', -55, curva='sai', **DASH)
R.k(156, 'MEDIUM_KICK:4', -55)
R.k(159, 'MEDIUM_KICK:6', -45, curva='sai', rastro=2)
K.k(153, 'BACKDASH:1', 55)
K.k(159, 'BACKDASH:3', 85, curva='sai')
K.k(166, 'BACKDASH:27', 92)
K.k(172, 'IDLE:0', 92, ciclo=14)
letreiro(159, 'VUSH!', [0.56, 0.32], 85, rotacao=6, segundos=0.45)
R.k(165, 'MEDIUM_KICK:7', -28, rot=-12, curva='sai')
R.k(172, 'WALK_FORWARD:6', -18, rot=-6, curva='suave')
R.k(180, 'WALK_FORWARD:12', -14, rot=0, curva='suave')
R.k(186, 'IDLE:0', -14, ciclo=15)
# O Kōga devolve com o chute de lado; o Ryūga fecha os olhos e recua, o chute passa a um palmo e o
# Kōga perde o equilíbrio, gira num pé só e quase cai.
K.k(188, 'HEAVY_STRAIGHT:2', 92, curva='corte')
K.k(196, 'HEAVY_STRAIGHT:4', 92)
K.k(200, 'HEAVY_STRAIGHT:7', 78, curva='sai', rastro=4)
R.k(194, 'HIT_STAND:1', -14)
R.k(200, 'HIT_STAND:2', -62, curva='sai')
letreiro(200, 'VUSH!', [0.47, 0.3], 95, rotacao=-8, segundos=0.5)
lenta(200, antes=3, depois=10, valor=0.45)
K.k(206, 'HEAVY_STRAIGHT:12', 70, rot=-14, y=6, curva='sai')
K.k(214, 'HEAVY_STRAIGHT:16', 76, rot=10, y=0, curva='suave')
K.k(222, 'LAND:2', 85, rot=-4, curva='suave')
K.k(230, 'IDLE:0', 85, rot=0, ciclo=14, curva='suave')
efeito(222, 'koga', 'fumaca_pouso', escala=0.6)
R.k(212, 'HIT_STAND:3', -70)
R.k(222, 'IDLE:0', -110, ciclo=15, curva='suave')
camera(214, 1.1, 'centro', 0.3)
# Ofegantes, um olhando para o outro.
respirar(R, 232, 250, 'IDLE:0', -135)
respirar(K, 232, 250, 'IDLE:0', 85, passo=15)

# ------------------------------------------------------------------ 2: os punhos se chocam (QTE)
R.k(250, 'CROUCH:2', -135, curva='corte')
K.k(250, 'CROUCH:2', 85, curva='corte')
linhas(256, 'velocidade', escuro=0.15)
for quem, lut, x0, x1 in (('ryuga', R, -135, -100), ('koga', K, 85, 50)):
    efeito(256, quem, 'fumaca_dash_v05')
    lut.k(256, 'DASH:3', x0, curva='corte', **DASH)
    lut.k(262, 'DASH:8', x1, curva='sai', **DASH)
R.k(263, 'LIGHT_JAB:3', -100)
K.k(263, 'LIGHT_JAB:2', 50)
camera(263, 1.45, 'centro', 0.1)
# QTE CHOQUE! no quadro 264: os dois com o punho armado.
R.k(267, 'LIGHT_JAB:6', -95, curva='sai', rastro=2)
K.k(267, 'LIGHT_JAB:5', 45, curva='sai', rastro=2)
impacto(267, 'ryuga', 2, letreiro='PAH!')
efeito(268, 'ryuga', 'fumaca_dash_v05', atras=True, escala=0.6)
efeito(268, 'koga', 'fumaca_dash_v05', atras=True, escala=0.6)
# Os dois quicam para trás sacudindo a mão.
R.k(269, 'HIT_STAND:2', -95)
K.k(269, 'HIT_STAND:2', 45)
R.k(282, 'HIT_STAND:3', -150, curva='sai')
K.k(282, 'HIT_STAND:3', 100, curva='sai')
camera(282, 1.15, 'centro', 0.4)
linhas(270)
R.k(290, 'HIT_STAND:3', -150, sx=1.03, sy=0.97)
K.k(290, 'HIT_STAND:3', 100, sx=1.03, sy=0.97)
R.k(298, 'IDLE:0', -150, ciclo=15)
K.k(296, 'IDLE:0', 100, ciclo=14)

# ------------------------------------------------------------------ 3: a troca de golpes (QTE)
R.k(306, 'WALK_FORWARD:0', -150, ciclo=30)
K.k(304, 'WALK_FORWARD:0', 100, ciclo=24)
R.k(326, 'IDLE:0', -80, ciclo=15)
K.k(326, 'IDLE:0', 80, ciclo=14)
camera(318, 1.3, 'centro', 0.3)
# QTE TROCA DE GOLPES! no quadro 331. Depois, o vídeo dos dois trocando golpes e errando (o qt1,
# o mesmo da briga em vídeo de antes): ele entra no lugar dos meninos, que seguram a pose escondidos
# (uma pausa do tamanho do vídeo, PAUSAS[332]). No vídeo os dois estão a 80 do meio, como aqui
# (97 px da folha com pixelScale 1,2).
ev(332, 'video', clipe='ep00/qt1', esquerda='koga', ritmo=VIDEO_RITMO, semEsperar=True)
for ms, txt, pos, rot in ((415, 'PÁ!', [0.55, 0.3], 8), (1079, 'PÁ!', [0.45, 0.32], -8),
                          (1743, 'VUSH!', [0.5, 0.6], 4), (2573, 'VUSH!', [0.55, 0.36], 6),
                          (3154, 'PÁ!', [0.5, 0.3], -6)):
    ev(332, 'onomatopeia', texto=txt, pos=pos, tamanho=90, rotacao=rot, segundos=0.45, semEsperar=True,
       apos=round(ms / VIDEO_RITMO * 60 / 1000))
ev(332, 'video', parar=True, apos=VIDEO_QUADROS)
# Os dois soltam o soco juntos e se acertam na cara: caem os dois.
R.k(366, 'IDLE:0', -80, ciclo=15)
K.k(366, 'IDLE:0', 80, ciclo=14)
R.k(372, 'CROUCH:2', -88, curva='corte')
K.k(372, 'CROUCH:2', 88, curva='corte')
R.k(379, 'MEDIUM_KICK:4', -55, curva='sai')
K.k(379, 'LIGHT_JAB:2', 45, curva='sai')
R.k(383, 'MEDIUM_KICK:6', -48, curva='sai', rastro=3)
K.k(383, 'LIGHT_JAB:5', 38, curva='sai', rastro=3)
camera(381, 1.6, 'centro', 0.06)
impacto(383, 'ryuga', 2, letreiro='POW!')
lenta(383, antes=2, depois=10, valor=0.4)
R.k(385, 'KNOCKDOWN:0', -48)
K.k(385, 'KNOCKDOWN:0', 38)
R.k(396, 'KNOCKDOWN:3', -125, y=0, curva='sai')
K.k(396, 'KNOCKDOWN:3', 105, y=0, curva='sai')
R.k(404, 'KNOCKDOWN:6', -135)
K.k(404, 'KNOCKDOWN:6', 105)
efeito(398, 'ryuga', 'fumaca_queda_v01', escala=0.8)
efeito(398, 'koga', 'fumaca_queda_v01', escala=0.8)
letreiro(398, 'TUM!', [0.5, 0.62], 110, rotacao=4, segundos=0.7)
camera(398, 1.15, 'centro', 0.5)

# ------------------------------------------------------------------ 4: o Kōga levanta e assusta
# O Kōga levanta primeiro, bravo; o Ryūga ainda está se levantando.
K.k(430, 'GETUP:4', 105, ate=11)
K.k(462, 'IDLE:0', 105, ciclo=14)
R.k(444, 'GETUP:4', -135, ate=8)
R.k(468, 'GETUP:8', -135)
camera(456, 1.35)
linhas(456, 'concentracao', escuro=0.2)
linhas(474)
camera(474, 1.3)
K.k(470, 'CROUCH:3', 105, curva='corte')
efeito(476, 'koga', 'fumaca_dash_v05')
K.k(476, 'DASH:3', 105, curva='corte', **DASH)
K.k(484, 'DASH:9', -20, curva='sai', **DASH)
K.k(485, 'CROUCH_LIGHT:2', -20)
K.k(490, 'CROUCH_LIGHT:4', -40, curva='sai', rastro=3)
# O Ryūga, ainda se erguendo, se assusta e salta para trás: o soco para a um palmo.
R.k(486, 'BACKDASH:2', -135)
R.k(494, 'BACKDASH:6', -178, y=26, curva='sai')
R.k(506, 'BACKDASH:16', -196, y=10)
R.k(512, 'BACKDASH:27', -200, y=0, curva='entra')
efeito(512, 'ryuga', 'fumaca_pouso')
R.k(518, 'IDLE:0', -200, ciclo=15)
letreiro(490, 'VUSH!', [0.42, 0.32], 100, rotacao=-8, segundos=0.6)
lenta(490, antes=6, depois=10, valor=0.3)
camera(488, 1.45, 'centro', 0.08)
K.k(502, 'CROUCH_LIGHT:8', -40)
K.k(512, 'IDLE:0', -40, ciclo=14)
camera(516, 1.15, 'centro', 0.4)
respirar(R, 520, 566, 'IDLE:0', -200)
respirar(K, 518, 566, 'IDLE:0', -40, passo=15)
# A câmera fecha de novo: o Ryūga resolve.
camera(548, 1.4)
linhas(548, 'concentracao', escuro=0.25)

# ------------------------------------------------------------------ 5: o golpe final (QTE)
linhas(570)
camera(570, 1.3)
R.k(566, 'CROUCH:3', -200, curva='corte')
efeito(572, 'ryuga', 'fumaca_dash_v05')
linhas(572, 'velocidade', escuro=0.2)
R.k(572, 'DASH:3', -200, curva='corte', **DASH)
R.k(580, 'DASH:9', -150, curva='sai', **DASH)
K.k(568, 'CROUCH_LIGHT:0', -40)
K.k(578, 'CROUCH_LIGHT:1', -35)
R.k(581, 'CROUCH_LIGHT:2', -145)
K.k(582, 'CROUCH_LIGHT:2', -38)
camera(581, 1.55, 'centro', 0.08)
# QTE GOLPE FINAL! no quadro 582. O soco do Ryūga pega primeiro, na cara do Kōga.
R.k(587, 'CROUCH_LIGHT:4', -135, curva='sai', rastro=4)
K.k(587, 'CROUCH_LIGHT:2', -38)
lenta(587, antes=5, depois=14, valor=0.3)
impacto(587, 'ryuga', 3, letreiro='POW!')
camera(587, 1.7, 'centro', 0.05)
K.k(589, 'HIT_STAND:1', -34, rot=-6)
K.k(604, 'HIT_STAND:2', 0, rot=-10, curva='sai')
K.k(620, 'HIT_STAND:2', 6, rot=-10)
R.k(596, 'CROUCH_LIGHT:5', -133)
R.k(620, 'CROUCH_LIGHT:5', -133)

QUADROS = 620

# As pausas (quadro: quadros a mais): a antecipação segurando, o golpe esticado depois do
# contato, a pausa de quem se estuda.
PAUSAS = {
    60: 10, 76: 10, 98: 8, 114: 6, 142: 6, 159: 8, 188: 8, 200: 8,
    250: 8, 290: 10, 326: 6, 332: VIDEO_QUADROS, 404: 24, 470: 10, 490: 10, 566: 8,
}

# Os QTE (quadro da coreografia, antes das pausas): a animação para na pose desse quadro e pede.
# Neutros, porque a cena é das duas campanhas; errar faz os dois se embolarem (a cabeçada sem
# querer), um clarão, e o QTE pede de novo.
QTES = [
    (264, {'tipo': 'toque', 'texto': 'CHOQUE!'}),
    (331, {'tipo': 'martelar', 'texto': 'TROCA DE GOLPES!', 'vezes': 10, 'segundos': 3.0}),
    (582, {'tipo': 'tempo', 'texto': 'GOLPE FINAL!', 'segundos': 1.3}),
]


def erro_qte(r, k):
    """Os dois avançam juntos e batem a cabeça um no outro, quicam e voltam à pose do QTE."""
    meio = (r['x'] + k['x']) / 2
    rk = [dict(r, q=0),
          {'q': 7, 'x': meio - 32, 'pose': 'DASH:6', 'curva': 'sai', 'rastro': 3, 'sx': 1.15, 'sy': 0.92},
          {'q': 9, 'x': meio - 30, 'pose': 'HIT_STAND:1'},
          {'q': 24, 'x': meio - 80, 'pose': 'HIT_STAND:3', 'curva': 'sai'},
          {'q': 40, 'x': meio - 80, 'pose': 'HIT_STAND:3'}]
    kk = [dict(k, q=0),
          {'q': 7, 'x': meio + 32, 'pose': 'DASH:6', 'curva': 'sai', 'rastro': 3, 'sx': 1.15, 'sy': 0.92},
          {'q': 9, 'x': meio + 30, 'pose': 'HIT_STAND:1'},
          {'q': 24, 'x': meio + 80, 'pose': 'HIT_STAND:3', 'curva': 'sai'},
          {'q': 40, 'x': meio + 80, 'pose': 'HIT_STAND:3'}]
    for c in rk + kk:
        c.pop('ciclo', None)
        c.pop('ate', None)
        c['y'] = 0
    eventos = [{'q': 9, 'acao': {'acao': 'impacto', 'quem': 'ryuga', 'forca': 1, 'congela': 0.2}},
               {'q': 9, 'acao': {'acao': 'onomatopeia', 'texto': 'TOC!', 'pos': [0.5, 0.3], 'tamanho': 100,
                                 'segundos': 0.5, 'semEsperar': True}},
               {'q': 36, 'acao': {'acao': 'clarao', 'segundos': 0.15}}]
    eventos.sort(key=lambda e: e['acao']['acao'] == 'impacto')
    return {'quadros': 40, 'ryuga': rk, 'koga': kk, 'eventos': eventos}


def main():
    eventos.sort(key=lambda e: (e['q'], e['acao']['acao'] == 'impacto'))
    ryuga, koga, evs, total = pausar(R.chaves, K.chaves, eventos, QUADROS, PAUSAS)

    def novo(q):
        return q + sum(n for p, n in PAUSAS.items() if q > p)

    cortes = [novo(q) for q, _ in QTES]
    limites = [0] + cortes + [total]
    trechos = []
    for n in range(len(limites) - 1):
        ini, fim = limites[n], limites[n + 1]
        anim = {'acao': 'animar', 'quadros': fim - ini}
        if n:
            anim['continua'] = True
        else:
            anim['meio'] = 'palco'
        anim['ryuga'] = cortar(ryuga, ini, fim)
        anim['koga'] = cortar(koga, ini, fim)
        anim['eventos'] = [dict(e, q=e['q'] - ini) for e in evs if ini <= e['q'] < fim]
        trechos.append(anim)
        if n < len(QTES):
            qte = {'acao': 'qte', 'golpe': 'nenhum', 'pos': [0.82, 0.6], **QTES[n][1]}
            qte['erroAnimar'] = erro_qte(amostra(ryuga, fim), amostra(koga, fim))
            trechos.append(qte)
    acoes = [
        {'acao': 'controle', 'travar': True},
        {'acao': 'velocidade', 'valor': 0.3, 'segundos': 0.2, 'semEsperar': True},
        {'acao': 'faixas', 'entrar': True, 'segundos': 1.0, 'semEsperar': True},
        {'acao': 'camera', 'zoom': 1.12, 'segundos': 1.0, 'semEsperar': True},
        {'acao': 'esperar', 'segundos': 1.0},
        {'acao': 'tinta', 'cobrir': True, 'segundos': 0.5},
        {'acao': 'velocidade', 'valor': 1.0, 'segundos': 0.0, 'semEsperar': True},
        # Os dois vão para o meio do cenário atrás da tinta (a animação começa no meio do palco).
        {'acao': 'camera', 'zoom': 1.2, 'foco': 'centro', 'segundos': 0.0, 'semEsperar': True},
        {'acao': 'tinta', 'cobrir': False, 'segundos': 0.5, 'semEsperar': True},
    ] + trechos + [
        {'acao': 'esperar', 'segundos': 0.9},
        {'acao': 'tinta', 'cobrir': True, 'segundos': 0.9},
        {'acao': 'terminarLuta', 'vencedor': 'interrompida'},
    ]
    cena = {
        '_comentario': (
            'A briga dos irmãos (Prólogo, depois do treino), animada à mão (ação animar, motor de luta '
            'parado), gerada por tools/cenas/briga_irmaos.py. As chaves são por irmão (ryuga, koga): a mesma '
            'cena nas duas campanhas. O Dafu manda irem devagar; o Kōga arranca com o soco (o Ryūga se '
            'abaixa), o gancho do Ryūga passa no vento, o soco dele é defendido, a voadora do Kōga o empurra '
            'longe; os punhos se chocam (CHOQUE!); a troca de golpes (TROCA DE GOLPES!) acaba com os dois se '
            'acertando na cara e caindo; o Kōga levanta primeiro e avança num soco que assusta o Ryūga; o soco '
            'do Ryūga pega o Kōga na cara (GOLPE FINAL!), POW, a tinta cobre e a página 5 começa nesse soco. '
            'Errar um QTE faz os dois baterem a cabeça (TOC!), clarão, e ele pede de novo.'),
        'gatilho': {'vida': 0.5},
        'acoes': acoes,
    }
    CENA.write_text(escrever(cena), encoding='utf-8')
    print(f'{CENA.relative_to(ROOT)}: {total} quadros ({total / 60:.1f} s sem as pausas de contato e a câmera '
          f'lenta), {len(ryuga)} chaves do Ryūga, {len(koga)} do Kōga, {len(evs)} eventos')


if __name__ == '__main__':
    main()
