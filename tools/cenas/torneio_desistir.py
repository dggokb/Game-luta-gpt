#!/usr/bin/env python3
"""A cena do meio da luta "Já vai desistir?" (torneio), animada à mão e genérica: serve a qualquer
dupla adversária e às duas campanhas. Gravada em
android/app/src/main/assets/historia/torneio/cena_desistir.json.

    python3 tools/cenas/torneio_desistir.py

O roteiro do dono (18/11): quando os dois da dupla do jogador chegam a 50%, quem está em campo
reclama ("Droga... tá difícil!"), o adversário provoca ("Já vai desistir, é?"), investe, dois golpes
defendidos, dois que pegam e o forte que joga o jogador na parede. O parceiro pede a troca, entra
com a voadora, o adversário defende e responde com a rasteira; a câmera segue o parceiro no ar, o
adversário aparece na frente dele, três golpes rápidos e o joga no chão (PAH!). No chão: "ARGHHH!",
"Droga!"; de fora, o outro irmão grita; o parceiro levanta, agradece, os dois trocam cinco golpes,
pulam para trás e a luta continua. Tira um pouco de vida dos dois da dupla do jogador.

Genérica: as poses são as que todo lutador tem (DEFENSE_STAND, HIT_STAND, HIT_AIR, KNOCKDOWN,
GETUP...) em frações do clipe ("HIT_STAND:0.3"), e os golpes do adversário são os dele ("@H:acerta":
o golpe forte dele no primeiro quadro que pega). O x vem da luta: "jogador+150" (onde o jogador
estava, +150 para o lado do adversário), "parede+40" (a parede atrás do jogador).
"""
from pathlib import Path

from animacao import Lutador, escrever

ROOT = Path(__file__).resolve().parents[2]
CENA = ROOT / 'android/app/src/main/assets/historia/torneio/cena_desistir.json'

J, R = Lutador(), Lutador()   # o jogador (quem está em campo; depois da troca, o parceiro) e o adversário
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


def impacto(q, quem, forca, **kw):
    kw.setdefault('congela', {1: 0.12, 2: 0.25, 3: 0.45}[forca])
    ev(q, 'impacto', quem=quem, forca=forca, **kw)


def dano(q, valor, quem='jogador'):
    ev(q, 'dano', quem=quem, valor=valor)


def fala(q, quem, texto, segura=0.5, expressao='bravo', lado='jogador'):
    ev(q, 'fala', quem=quem, texto=texto, segura=segura, expressao=expressao, lado=lado)


def efeito(q, quem, nome, **kw):
    ev(q, 'efeito', quem=quem, nome=nome, **kw)


DASH = dict(sx=1.25, sy=0.88, rastro=5)

# ------------------------------------------------------------------ A provocação
J.k(0, 'IDLE:0', 'atual', ciclo=14)
R.k(0, 'IDLE:0', 'atual', ciclo=14)
camera(0, 1.7, 'jogador', 0.25)
linhas(0, 'concentracao', escuro=0.2)
fala(6, 'jogador', 'Droga... tá difícil!', expressao='normal')
camera(110, 1.7, 'rival', 0.2)
fala(114, 'rival', 'Já vai desistir, é?', lado='rival')
linhas(210)
camera(214, 1.25, 'centro', 0.2)

# ------------------------------------------------------------------ O adversário investe
J.k(214, 'IDLE:0', 'jogador', ciclo=14)
J.k(222, 'DEFENSE_STAND:0.2', 'jogador')
R.k(214, 'IDLE:0', 'rival')
R.k(222, 'DASH:0.3', 'rival', curva='corte', **DASH)
efeito(222, 'rival', 'fumaca_dash_v05')
linhas(222, 'velocidade', escuro=0.15)
R.k(230, 'DASH:0.7', 'jogador+150', curva='sai', **DASH)
linhas(232)
# Dois golpes defendidos (o jogador escorrega um pouco a cada um).
q = 232
for golpe in ('L', 'M'):
    R.k(q, f'@{golpe}:prepara', 'jogador+135')
    R.k(q + 4, f'@{golpe}:acerta', 'jogador+125', curva='sai', rastro=2)
    J.k(q + 4, 'DEFENSE_STAND:0.4', 'jogador-10' if golpe == 'L' else 'jogador-30')
    impacto(q + 4, 'rival', 1)
    J.k(q + 12, 'DEFENSE_STAND:0.7', 'jogador-25' if golpe == 'L' else 'jogador-45', curva='sai')
    R.k(q + 14, f'@{golpe}:fim', 'jogador+125')
    q += 18
# Mais dois que pegam.
for golpe, rec in (('L', 'jogador-60'), ('M', 'jogador-80')):
    R.k(q, f'@{golpe}:prepara', 'jogador+105')
    R.k(q + 4, f'@{golpe}:acerta', 'jogador+95', curva='sai', rastro=2)
    J.k(q + 4, 'HIT_STAND:0.2', rec, rastro=2)
    impacto(q + 4, 'rival', 1)
    dano(q + 4, 0.02)
    J.k(q + 14, 'HIT_STAND:0.6', rec, curva='sai')
    R.k(q + 14, f'@{golpe}:fim', 'jogador+95')
    q += 18
# O forte joga o jogador na parede.
camera(q, 1.4, 'centro', 0.1)
R.k(q, '@H:prepara', 'jogador+70')
R.k(q + 6, '@H:acerta', 'jogador+60', curva='sai', rastro=4)
J.k(q + 6, 'HIT_AIR:0', 'jogador-90', y=20, rastro=4)
impacto(q + 6, 'rival', 2, letreiro='BAM!')
dano(q + 6, 0.04)
linhas(q + 6, 'velocidade', escuro=0.2)
J.k(q + 20, 'HIT_AIR:0.5', 'parede+60', y=90, rot=-25, curva='sai', rastro=6)
# Bate na parede, escorrega e cai sentado.
impacto(q + 21, 'rival', 2, congela=0.15)
efeito(q + 21, 'jogador', 'fumaca_queda_v01', escala=0.9)
linhas(q + 24)
J.k(q + 24, 'KNOCKDOWN:0.3', 'parede+70', y=60, rot=-10)
J.k(q + 38, 'GROUNDED:0', 'parede+80', y=0, rot=0, curva='entra')
efeito(q + 38, 'jogador', 'fumaca_pouso')
R.k(q + 26, '@H:fim', 'jogador+60')
R.k(q + 40, 'IDLE:0', 'parede+330', ciclo=14, curva='suave')
q += 50

# ------------------------------------------------------------------ A troca
camera(q, 1.1, 'centro', 0.25)
fala(q + 4, 'parceiro', 'Troca comigo!', segura=0.4)
J.k(q + 60, 'GETUP:0.5', 'parede+80')
fala(q + 70, 'jogador', 'Tá!', segura=0.3, expressao='normal')
J.k(q + 100, 'GETUP:1', 'parede+80')
q += 120
# Quem estava em campo salta para fora (por cima da tela) e o parceiro entra de lá com a voadora.
J.k(q, 'JUMP:0.2', 'parede+80', curva='corte')
efeito(q, 'jogador', 'fumaca_pulo')
J.k(q + 10, 'JUMP:0.4', 'parede+20', y=700, curva='entra')
camera(q, 1.15, 'centro', 0.2)
ev(q + 11, 'parceiro', quem='jogador')
J.k(q + 12, '@jH:prepara', 'parede+60', y=520, curva='corte')
linhas(q + 14, 'velocidade', escuro=0.2)
J.k(q + 26, '@jH:acerta', 'parede+250', y=70, curva='entra', rastro=6)
R.k(q + 14, 'DEFENSE_STAND:0.2', 'parede+330')
R.k(q + 26, 'DEFENSE_STAND:0.4', 'parede+335')
impacto(q + 26, 'jogador', 1, letreiro='DOOM!')
linhas(q + 28)
R.k(q + 34, 'DEFENSE_STAND:0.7', 'parede+360', curva='sai')
J.k(q + 30, 'JUMP:0.8', 'parede+235', y=40)
J.k(q + 36, 'LAND:0.2', 'parede+225', y=0, curva='entra')
efeito(q + 36, 'jogador', 'fumaca_pouso')
# A rasteira (2H) dele joga o parceiro para cima.
R.k(q + 40, '@2H:prepara', 'parede+330')
R.k(q + 45, '@2H:acerta', 'parede+320', curva='sai', rastro=3)
J.k(q + 45, 'HIT_AIR:0', 'parede+225', y=10)
impacto(q + 45, 'rival', 1)
dano(q + 45, 0.01)
q += 45

# ------------------------------------------------------------------ No ar e no chão
# A câmera segue o parceiro no ar; o adversário aparece na frente dele.
camera(q + 2, 1.15, 'centro', 0.15)
J.k(q + 20, 'HIT_AIR:0.5', 'parede+200', y=70, rot=-15, curva='sai', rastro=3)
R.k(q + 14, 'DASH:0.5', 'parede+310', y=0, alfa=1.0, rastro=5, sx=1.3, sy=0.85)
R.k(q + 16, 'DASH:0.5', 'parede+300', y=40, alfa=0.0, rastro=5, sx=1.3, sy=0.85)
ev(q + 16, 'onomatopeia', texto='SHUN!', pos=[0.72, 0.3], tamanho=100, rotacao=-8, segundos=0.4, semEsperar=True)
R.k(q + 20, '@jL:prepara', 'parede+300', y=70, alfa=0.0)
R.k(q + 22, '@jL:prepara', 'parede+290', y=70, alfa=1.0)
q += 22
for golpe in ('jL', 'jM', 'jL'):
    R.k(q, f'@{golpe}:prepara', 'parede+290', y=72)
    R.k(q + 3, f'@{golpe}:acerta', 'parede+285', y=72, curva='sai', rastro=2)
    J.k(q + 3, 'HIT_AIR:0.2', 'parede+200', y=72, rot=-15)
    impacto(q + 3, 'rival', 1, congela=0.08)
    dano(q + 3, 0.01)
    J.k(q + 8, 'HIT_AIR:0.6', 'parede+195', y=75, rot=-18)
    q += 9
# O golpe que joga para baixo: o parceiro despenca e PAH no chão.
R.k(q, '@jH:prepara', 'parede+290', y=80)
R.k(q + 5, '@jH:acerta', 'parede+275', y=85, curva='sai', rastro=4)
J.k(q + 5, 'HIT_AIR:0.8', 'parede+200', y=70, rot=-40)
impacto(q + 5, 'rival', 2)
linhas(q + 6, 'velocidade', escuro=0.25)
J.k(q + 13, 'KNOCKDOWN:0.4', 'parede+150', y=0, rot=-80, curva='entra', rastro=6)
impacto(q + 13, 'rival', 3, letreiro='PAH!')
dano(q + 13, 0.03)
efeito(q + 13, 'jogador', 'fumaca_queda_v01', escala=1.3)
linhas(q + 16)
J.k(q + 18, 'GROUNDED:0', 'parede+140', y=0, rot=0)
R.k(q + 14, 'JUMP:0.7', 'parede+330', y=60, curva='sai')
R.k(q + 26, 'LAND:0.3', 'parede+380', y=0, curva='entra')
efeito(q + 26, 'rival', 'fumaca_pouso')
R.k(q + 36, 'IDLE:0', 'parede+380', ciclo=14)
camera(q + 20, 1.3, 'centro', 0.4)
q += 36

# ------------------------------------------------------------------ Do chão
fala(q, 'jogador', 'ARGHHHH!', segura=0.2)
fala(q + 50, 'jogador', 'Droga!', segura=0.2)
q += 95
camera(q, 1.1, 'centro', 0.3)
fala(q + 4, 'outroIrmao', 'Heyyy! Vão desistir agora? É tudo que vocês têm?', segura=0.5)
q += 150
J.k(q, 'GETUP:0.2', 'parede+140')
J.k(q + 30, 'GETUP:1', 'parede+140')
J.k(q + 40, 'IDLE:0', 'parede+140', ciclo=14)
camera(q + 40, 1.6, 'jogador', 0.3)
fala(q + 44, 'jogador', 'He he... você está certo! Valeu, {irmao}!', segura=0.6, expressao='normal')
q += 200

# ------------------------------------------------------------------ De volta à luta
camera(q, 1.25, 'centro', 0.2)
J.k(q, 'LAND:0.2', 'parede+140', curva='corte')
q += 8
J.k(q, 'DASH:0.3', 'parede+140', curva='corte', **DASH)
efeito(q, 'jogador', 'fumaca_dash_v05')
linhas(q, 'velocidade', escuro=0.15)
J.k(q + 8, 'DASH:0.7', 'parede+290', curva='sai', **DASH)
linhas(q + 10)
q += 10
# Cinco golpes trocados, cada um defendido pelo outro.
for i, golpe in enumerate(('L', 'M', 'L', 'M', 'H')):
    bate, defende = (J, R) if i % 2 == 0 else (R, J)
    xb = 'parede+290' if bate is J else 'parede+380'
    xd = 'parede+380' if bate is J else 'parede+290'
    quem = 'jogador' if bate is J else 'rival'
    bate.k(q, f'@{golpe}:prepara', xb)
    bate.k(q + 4, f'@{golpe}:acerta', xb, curva='sai', rastro=2)
    defende.k(q, 'DEFENSE_STAND:0.2', xd)
    defende.k(q + 4, 'DEFENSE_STAND:0.4', xd)
    impacto(q + 4, quem, 1)
    bate.k(q + 9, f'@{golpe}:fim', xb)
    defende.k(q + 9, 'DEFENSE_STAND:0.7', xd)
    q += 12
# Os dois pulam para trás e a luta continua.
J.k(q, 'BACKDASH:0.1', 'parede+290')
R.k(q, 'BACKDASH:0.1', 'parede+380')
J.k(q + 14, 'BACKDASH:0.4', 'parede+170', y=40, curva='sai', rastro=3)
R.k(q + 14, 'BACKDASH:0.4', 'parede+520', y=40, curva='sai', rastro=3)
J.k(q + 24, 'LAND:0.3', 'parede+150', y=0, curva='entra')
R.k(q + 24, 'LAND:0.3', 'parede+540', y=0, curva='entra')
camera(q + 14, 1.0, 'centro', 0.4)
J.k(q + 34, 'IDLE:0', 'parede+150', ciclo=14)
R.k(q + 34, 'IDLE:0', 'parede+540', ciclo=14)
QUADROS = q + 36


def main():
    eventos.sort(key=lambda e: (e['q'], e['acao']['acao'] == 'impacto'))
    for lut in (J, R):
        lut.chaves.sort(key=lambda c: c['q'])
    anim = {'acao': 'animar', 'quadros': QUADROS, 'jogador': J.chaves, 'rival': R.chaves, 'eventos': eventos}
    acoes = [
        {'acao': 'controle', 'travar': True},
        {'acao': 'velocidade', 'valor': 0.3, 'segundos': 0.15, 'semEsperar': True},
        {'acao': 'faixas', 'entrar': True, 'segundos': 0.5, 'semEsperar': True},
        {'acao': 'esperar', 'segundos': 0.4},
        {'acao': 'velocidade', 'valor': 1.0, 'segundos': 0.0, 'semEsperar': True},
        anim,
        {'acao': 'faixas', 'entrar': False, 'segundos': 0.4},
    ]
    cena = {
        '_comentario': (
            'Cena do meio da luta "Já vai desistir?" (torneio), animada à mão e genérica (qualquer dupla '
            'adversária, as duas campanhas), gerada por tools/cenas/torneio_desistir.py com o roteiro do dono '
            '(18/11). Quando os dois da dupla do jogador chegam a 50%: o adversário provoca, investe, dois golpes '
            'defendidos, dois que pegam, o forte na parede; o parceiro entra com a voadora, leva a rasteira, três '
            'golpes no ar e PAH no chão; o outro irmão grita de fora; o parceiro levanta, cinco golpes trocados, os '
            'dois pulam para trás e a luta continua. Não encerra a luta (vai em "cenasNaLuta" do passo).'),
        'gatilho': {'vidaDupla': 0.5, 'lado': 'jogador'},
        'acoes': acoes,
    }
    CENA.write_text(escrever(cena), encoding='utf-8')
    print(f'{CENA.relative_to(ROOT)}: {QUADROS} quadros ({QUADROS / 60:.1f} s), {len(J.chaves)} chaves do jogador, '
          f'{len(R.chaves)} do adversário, {len(eventos)} eventos')


if __name__ == '__main__':
    main()
