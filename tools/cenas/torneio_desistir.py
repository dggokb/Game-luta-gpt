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


def linhas(q, estilo=None, escuro=0.0, segundos=0.08, fundo=False):
    if estilo and fundo:
        # No fundo preto, atrás dos dois (o cenário some).
        ev(q, 'linhas', estilo=estilo, fundo=True, segundos=segundos)
    elif estilo:
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


def stamp_dano(nome):
    """O STAMP do dano: só quem apanha, sem o adversário (serve contra qualquer um). A arte é a de
    quem está em campo do lado do jogador (torneio/stamp_<nome>_<id do lutador>.jpg); nela ele está à
    esquerda (bate espelhado se na luta está à direita)."""
    return {'imagem': 'torneio/stamp_' + nome + '_{jogador}.jpg', 'jogadorNaArte': 'esquerda', 'segundos': 1.0}


def lenta(q, antes=4, depois=16, valor=0.35):
    """Câmera lenta em volta do contato do quadro q (a animação anda a `valor` e volta ao normal)."""
    ev(q - antes, 'velocidade', valor=valor, segundos=0.05, semEsperar=True)
    ev(q + depois, 'velocidade', valor=1.0, segundos=0.2, semEsperar=True)


def respirar(lut, q0, q1, pose, x, passo=18, **kw):
    """Ofegante: o peito sobe e desce na pose, de q0 a q1."""
    q, cima = q0, True
    while q < q1:
        lut.k(q, pose, x, sy=1.0 if cima else 0.965, sx=1.0 if cima else 1.02, curva='suave', **kw)
        q += passo
        cima = not cima


# O ritmo é o do anime (o dono, 18/11: "tá tudo muito rápido, sem narrativa"): cada fala tem o close
# de quem fala e tempo para ser lida; o golpe tem antecipação segurando, o contato congelado e a
# câmera lenta; entre um lance e outro, a pausa de quem se estuda.

# ------------------------------------------------------------------ 1. O golpe que abre e a provocação
# O dono (18/11, v3): "se a cinemática começa com um dano do nada, alguém precisa ter dado o dano".
# Abre com o adversário entrando com o golpe médio; o jogador recua e fica ofegante.
J.k(0, 'IDLE:0', 'atual', ciclo=12)
R.k(0, 'IDLE:0', 'atual', ciclo=12)
camera(0, 1.25, 'centro', 0.3)
R.k(4, 'LAND:0.4', 'atual', curva='corte')
efeito(10, 'rival', 'fumaca_dash_v05')
R.k(10, 'DASH:0.3', 'atual', curva='corte', **DASH)
R.k(20, '@M:prepara', 'jogador+175', curva='sai', **DASH)
R.k(24, '@M:acerta', 'jogador+170', curva='sai', rastro=3)
J.k(24, 'HIT_STAND:0.2', 'jogador-25', rastro=2)
impacto(24, 'rival', 2, congela=0.22)
dano(24, 0.01)
J.k(40, 'HIT_STAND:0.7', 'jogador-45', curva='sai')
R.k(40, '@M:fim', 'jogador+170')
# Ele pula para trás e espera.
R.k(54, 'BACKDASH:0.2', 'jogador+170')
R.k(66, 'BACKDASH:0.5', 'jogador+330', y=40, curva='sai', rastro=3)
R.k(76, 'LAND:0.3', 'jogador+340', y=0, curva='entra')
R.k(88, 'IDLE:0', 'jogador+340', ciclo=10)
J.k(56, 'IDLE:0', 'jogador-45')
respirar(J, 70, 400, 'IDLE:0', 'jogador-45')
camera(90, 1.9, 'jogador', 0.5)
linhas(90, 'concentracao', escuro=0.25)
fala(110, 'jogador', 'Droga... tá difícil!', segura=1.0, expressao='normal')
linhas(250)
camera(254, 1.9, 'rival', 0.4)
linhas(254, 'concentracao', escuro=0.25)
fala(274, 'rival', 'Já vai desistir, é?', segura=1.0, lado='rival')
R.k(254, 'IDLE:0', 'jogador+340', ciclo=8)
linhas(400)
# A pausa: plano aberto, os dois parados.
camera(402, 1.05, 'centro', 0.4)
J.k(402, 'DEFENSE_STAND:0.1', 'jogador-45')
R.k(402, 'IDLE:0', 'jogador+340', ciclo=10)
q = 440

# ------------------------------------------------------------------ 2. O adversário investe
# Antecipação: ele abaixa, segura, e arranca.
R.k(q, 'LAND:0.4', 'jogador+340', curva='corte')
R.k(q + 14, 'LAND:0.4', 'jogador+340')
efeito(q + 14, 'rival', 'fumaca_dash_v05')
linhas(q + 14, 'velocidade', escuro=0.2)
R.k(q + 14, 'DASH:0.3', 'jogador+340', curva='corte', **DASH)
R.k(q + 24, 'DASH:0.7', 'jogador+110', curva='sai', **DASH)
camera(q + 22, 1.4, 'centro', 0.1)
linhas(q + 26)
q += 24
# Os quatro rápidos (o dono: "podem ser mais rápidos"): o primeiro defendido, o segundo quebra a
# guarda (GUARD BREAK, o efeito da luta), e dois que pegam no jogador sem guarda.
R.k(q, '@L:prepara', 'jogador+110')
R.k(q + 4, '@L:acerta', 'jogador+105', curva='sai', rastro=3)
J.k(q + 4, 'DEFENSE_STAND:0.4', 'jogador-55')
impacto(q + 4, 'rival', 1, congela=0.1)
J.k(q + 12, 'DEFENSE_STAND:0.7', 'jogador-65', curva='sai')
R.k(q + 10, '@L:fim', 'jogador+100')
q += 14
R.k(q, '@M:prepara', 'jogador+125')
R.k(q + 6, '@M:prepara', 'jogador+120')
R.k(q + 9, '@M:acerta', 'jogador+110', curva='sai', rastro=4)
J.k(q, 'DEFENSE_STAND:0.4', 'jogador-65')
J.k(q + 9, 'HIT_STAND:0.1', 'jogador-80', rastro=3)
impacto(q + 9, 'rival', 2, tipo='quebra', letreiro='GUARD BREAK!', congela=0.35)
lenta(q + 9, antes=3, depois=8, valor=0.4)
J.k(q + 24, 'HIT_STAND:0.6', 'jogador-95', curva='sai')
R.k(q + 20, '@M:fim', 'jogador+100')
q += 24
for golpe, rec in (('L', 'jogador-110'), ('M', 'jogador-130')):
    R.k(q, f'@{golpe}:prepara', 'jogador+40')
    R.k(q + 4, f'@{golpe}:acerta', 'jogador+35', curva='sai', rastro=3)
    J.k(q + 4, 'HIT_STAND:0.2', rec, rastro=2)
    impacto(q + 4, 'rival', 1, tipo='acerto', congela=0.1)
    dano(q + 4, 0.01)
    J.k(q + 12, 'HIT_STAND:0.6', rec, curva='sai')
    R.k(q + 11, f'@{golpe}:fim', 'jogador+35')
    q += 14
# O forte: ele recua o braço e segura, a tela escurece, e o golpe joga o jogador na parede.
camera(q, 1.6, 'centro', 0.3)
linhas(q, 'concentracao', escuro=0.4, segundos=0.2)
R.k(q, '@H:inicio', 'jogador+20')
R.k(q + 18, '@H:prepara', 'jogador+15', curva='suave')
J.k(q, 'HIT_STAND:0.7', 'jogador-130')
q += 18
linhas(q + 4)
R.k(q + 6, '@H:acerta', 'jogador+0', curva='sai', rastro=5)
J.k(q + 6, 'HIT_AIR:0', 'jogador-140', y=30, rastro=4)
impacto(q + 6, 'rival', 3, letreiro='BAM!', congela=0.5, stamp=stamp_dano('bam'))
dano(q + 6, 0.04)
# O voo até a parede no fundo preto, com as linhas de velocidade (o dono: "mais dramaticidade").
camera(q + 8, 1.3, 'jogador', 0.15)
linhas(q + 8, 'velocidade', fundo=True, segundos=0.06)
lenta(q + 8, antes=0, depois=22, valor=0.45)
J.k(q + 30, 'HIT_AIR:0.5', 'parede+60', y=110, rot=-25, curva='sai', rastro=7)
R.k(q + 30, '@H:fim', 'jogador+0')
R.k(q + 50, 'IDLE:0', 'jogador+0', ciclo=10)
# Bate na parede: o cenário volta, a parede treme e o efeito nasce nele (não no meio dos dois).
linhas(q + 30, segundos=0.05)
impacto(q + 30, 'rival', 2, onde='alvo', congela=0.25)
efeito(q + 30, 'jogador', 'fumaca_queda_v01', escala=1.0)
camera(q + 32, 1.15, 'centro', 0.3)
J.k(q + 33, 'KNOCKDOWN:0.3', 'parede+70', y=90, rot=-10)
J.k(q + 54, 'GROUNDED:0', 'parede+80', y=0, rot=0, curva='entra')
efeito(q + 54, 'jogador', 'fumaca_pouso')
# Ele se segura no chão (a pose que o dono aprovou) e fica ali, ofegante.
J.k(q + 80, 'GETUP:0.3', 'parede+80')
J.k(q + 100, 'GETUP:0.62', 'parede+80')
respirar(J, q + 110, q + 150, 'GETUP:0.62', 'parede+80', passo=20)
q += 120

# ------------------------------------------------------------------ 3. A troca, durante a investida
# O adversário vem acabar com quem está no chão; no meio da investida (em câmera lenta) o parceiro
# pede a troca, quem está no chão salta para fora e o parceiro chega antes, com a voadora na guarda
# dele (o dono: "antes do adversário chegar para dar o golpe com a gente caído, a tag aparece").
camera(q, 1.1, 'centro', 0.4)
R.k(q, 'LAND:0.4', 'jogador+0', curva='corte')
R.k(q + 14, 'LAND:0.4', 'jogador+0')
efeito(q + 14, 'rival', 'fumaca_dash_v05')
R.k(q + 14, 'DASH:0.3', 'jogador+0', curva='corte', **DASH)
linhas(q + 16, 'velocidade', escuro=0.25)
ev(q + 18, 'velocidade', valor=0.3, segundos=0.15, semEsperar=True)
fala(q + 20, 'parceiro', 'Troca comigo!', segura=0.6)
R.k(q + 50, 'DASH:0.6', 'parede+330', **DASH)
fala(q + 34, 'jogador', 'Tá!', segura=0.4, expressao='normal')
# Quem estava no chão salta para fora (por cima da tela).
J.k(q + 36, 'LAND:0.4', 'parede+80', curva='corte')
efeito(q + 38, 'jogador', 'fumaca_pulo')
J.k(q + 38, 'JUMP:0.2', 'parede+80', curva='corte')
J.k(q + 46, 'JUMP:0.4', 'parede+30', y=900, curva='entra')
ev(q + 47, 'parceiro', quem='jogador')
J.k(q + 48, '@jH:prepara', 'parede+40', y=650, curva='corte')
camera(q + 48, 1.25, 'centro', 0.2)
# A voadora desce e pega na guarda dele, que trava a investida.
R.k(q + 56, 'DEFENSE_STAND:0.2', 'parede+300', curva='sai')
J.k(q + 60, '@jH:acerta', 'parede+180', y=90, curva='entra', rastro=7)
R.k(q + 60, 'DEFENSE_STAND:0.4', 'parede+300')
ev(q + 58, 'velocidade', valor=1.0, segundos=0.1, semEsperar=True)
linhas(q + 58)
impacto(q + 60, 'jogador', 2, letreiro='DOOM!', congela=0.4,
        stamp={'imagem': 'torneio/stamp_voadora_{jogador}.jpg', 'jogadorNaArte': 'esquerda', 'segundos': 1.0})
R.k(q + 72, 'DEFENSE_STAND:0.7', 'parede+345', curva='sai')
J.k(q + 66, 'JUMP:0.8', 'parede+170', y=50)
J.k(q + 74, 'LAND:0.2', 'parede+160', y=0, curva='entra')
efeito(q + 74, 'jogador', 'fumaca_pouso')
camera(q + 74, 1.15, 'centro', 0.3)
R.k(q + 84, 'IDLE:0', 'parede+345', ciclo=10)
J.k(q + 84, 'IDLE:0', 'parede+160', ciclo=12)
q += 100

# ------------------------------------------------------------------ 4. Lançada para cima
# Ele responde com a rasteira, que joga o parceiro alto; a câmera sobe com ele e as linhas correm
# na vertical (o dono: "as linhas de ação estão horizontais e não verticais").
R.k(q, '@2H:prepara', 'parede+345')
R.k(q + 10, '@2H:prepara', 'parede+340')
R.k(q + 14, '@2H:acerta', 'parede+320', curva='sai', rastro=4)
J.k(q + 14, 'HIT_AIR:0', 'parede+165', y=20, rastro=3)
impacto(q + 14, 'rival', 2, congela=0.25)
dano(q + 14, 0.01)
camera(q + 16, 1.25, 'jogador', 0.2)
linhas(q + 16, 'subir', escuro=0.3)
J.k(q + 48, 'HIT_AIR:0.5', 'parede+170', y=380, rot=-20, curva='sai', rastro=6)
R.k(q + 28, '@2H:fim', 'parede+320')
# Ele some do chão (SHUN!) e aparece no ar, na frente do parceiro, a uma distância de golpe.
R.k(q + 38, 'LAND:0.4', 'parede+320')
R.k(q + 44, 'DASH:0.5', 'parede+320', alfa=1.0, rastro=5, sx=1.3, sy=0.85)
R.k(q + 46, 'DASH:0.5', 'parede+320', y=200, alfa=0.0, rastro=5, sx=1.3, sy=0.85)
ev(q + 45, 'onomatopeia', texto='SHUN!', pos=[0.7, 0.62], tamanho=100, rotacao=-8, segundos=0.5, semEsperar=True)
R.k(q + 52, '@jM:inicio', 'parede+340', y=395, alfa=0.0)
R.k(q + 56, '@jM:inicio', 'parede+335', y=398, alfa=1.0)
J.k(q + 56, 'HIT_AIR:0.5', 'parede+170', y=392, rot=-20)
linhas(q + 56)
q += 56
# Três chutes no ar, cada um pegando (o efeito de acerto, não o de defesa); o parceiro sobe um
# pouco a cada um, como num combo aéreo.
for k, golpe in enumerate(('jL', 'jM', 'jL')):
    xr = 335 - 4 * k
    R.k(q + 4, f'@{golpe}:prepara', f'parede+{xr}', y=398 + 6 * k)
    R.k(q + 7, f'@{golpe}:acerta', f'parede+{xr - 8}', y=398 + 6 * k, curva='sai', rastro=3)
    J.k(q + 7, 'HIT_AIR:0.1' if k % 2 == 0 else 'HIT_AIR:0.4', f'parede+{165 - 4 * k}', y=394 + 6 * k,
        rot=-12 - 6 * k, rastro=2)
    impacto(q + 7, 'rival', 1, tipo='acerto', congela=0.1)
    dano(q + 7, 0.01)
    J.k(q + 14, 'HIT_AIR:0.6', f'parede+{160 - 4 * k}', y=398 + 6 * k, rot=-16 - 6 * k)
    q += 15
# O golpe que joga para baixo: ele sobe acima do parceiro, segura, e crava.
linhas(q, 'concentracao', escuro=0.4, segundos=0.15)
R.k(q, '@jH:inicio', 'parede+300', y=500)
R.k(q + 18, '@jH:prepara', 'parede+295', y=510, curva='suave')
R.k(q + 24, '@jH:acerta', 'parede+275', y=485, curva='sai', rastro=5)
J.k(q + 24, 'HIT_AIR:0.8', 'parede+160', y=410, rot=-45)
lenta(q + 24, antes=3, depois=6, valor=0.3)
impacto(q + 24, 'rival', 2, tipo='acerto', congela=0.3)
dano(q + 24, 0.01)
# A queda de verdade: fundo preto, as linhas correndo para cima, a câmera caindo junto.
linhas(q + 27, 'descer', fundo=True, segundos=0.06)
camera(q + 27, 1.35, 'jogador', 0.1)
ev(q + 27, 'velocidade', valor=0.5, segundos=0.1, semEsperar=True)
J.k(q + 30, 'KNOCKDOWN:0.2', 'parede+150', y=360, rot=-70)
J.k(q + 60, 'KNOCKDOWN:0.4', 'parede+130', y=0, rot=-85, curva='entra', rastro=8)
ev(q + 58, 'velocidade', valor=1.0, segundos=0.05, semEsperar=True)
# PAH no chão da arena (o cenário volta no contato) com o STAMP.
linhas(q + 60, segundos=0.04)
camera(q + 60, 1.2, 'centro', 0.2)
impacto(q + 60, 'rival', 3, letreiro='PAH!', congela=0.55, onde='alvo', stamp=stamp_dano('pah'))
dano(q + 60, 0.02)
efeito(q + 60, 'jogador', 'fumaca_queda_v01', escala=1.4)
# Saiu do STAMP: o corpo quica no chão com o grito.
fala(q + 62, 'jogador', 'ARGHHHH!', segura=0.8)
J.k(q + 62, 'KNOCKDOWN:0.5', 'parede+125', y=0, rot=-80)
J.k(q + 74, 'KNOCKDOWN:0.6', 'parede+115', y=70, rot=-40, curva='sai')
J.k(q + 88, 'GROUNDED:0', 'parede+110', y=0, rot=0, curva='entra')
efeito(q + 88, 'jogador', 'fumaca_pouso')
# O adversário desce longe (o dono: "muito perto da gente quando desceu do combo aéreo").
R.k(q + 30, '@jH:fim', 'parede+290', y=470)
R.k(q + 36, 'JUMP:0.7', 'parede+300', y=420)
R.k(q + 80, 'JUMP:0.8', 'parede+560', y=120)
R.k(q + 92, 'LAND:0.3', 'parede+580', y=0, curva='entra')
efeito(q + 92, 'rival', 'fumaca_pouso')
R.k(q + 106, 'IDLE:0', 'parede+580', ciclo=8)
q += 110

# ------------------------------------------------------------------ 5. No chão
camera(q, 1.35, 'centro', 0.5)
linhas(q, 'concentracao', escuro=0.3)
fala(q + 20, 'jogador', 'Droga!', segura=0.8)
linhas(q + 130)
q += 140
# De fora, o outro irmão grita; plano aberto, o adversário de pé esperando.
camera(q, 1.05, 'centro', 0.6)
fala(q + 20, 'outroIrmao', 'Heyyy! Vão desistir agora? É tudo que vocês têm?', segura=1.2)
# O STAMP do outro irmão gritando de fora da arena (a fala fica por cima dele).
ev(q + 24, 'stamp', imagem='torneio/stamp_fora_{outroIrmao}.jpg', segundos=2.2)
q += 200
# O parceiro levanta logo (o dono: "muito demorado para levantar"), sorri e agradece.
J.k(q, 'GETUP:0.2', 'parede+110')
J.k(q + 28, 'GETUP:1', 'parede+110', curva='suave')
respirar(J, q + 32, q + 180, 'IDLE:0', 'parede+110')
camera(q + 24, 1.8, 'jogador', 0.4)
linhas(q + 30, 'concentracao', escuro=0.25)
fala(q + 36, 'jogador', 'He he... você está certo! Valeu, {irmao}!', segura=1.1, expressao='normal')
linhas(q + 176)
q += 180

# ------------------------------------------------------------------ 6. De volta à luta
camera(q, 1.05, 'centro', 0.4)
J.k(q, 'DEFENSE_STAND:0.1', 'parede+110')
q += 24
J.k(q, 'LAND:0.4', 'parede+110', curva='corte')
R.k(q, 'LAND:0.4', 'parede+580', curva='corte')
q += 12
# O grito da investida: o STAMP de quem está em campo avançando e gritando.
# O letreiro vai antes do STAMP no mesmo quadro: o STAMP congela a animação, e um evento depois
# dele esperaria o quadro sair.
ev(q, 'onomatopeia', texto='HAAAA!', pos=[0.29, 0.21], tamanho=112, rotacao=-10, segundos=1.0, semEsperar=True)
ev(q, 'stamp', imagem='torneio/stamp_grito_{jogador}.jpg', jogadorNaArte='esquerda', segundos=0.9)
J.k(q, 'DASH:0.3', 'parede+110', curva='corte', **DASH)
R.k(q, 'DASH:0.3', 'parede+580', curva='corte', **DASH)
efeito(q, 'jogador', 'fumaca_dash_v05')
efeito(q, 'rival', 'fumaca_dash_v05')
linhas(q, 'velocidade', escuro=0.2)
# Param à distância do golpe (o dono: "muito perto").
XJ, XR = 'parede+250', 'parede+465'
J.k(q + 12, 'DASH:0.7', XJ, curva='sai', **DASH)
R.k(q + 12, 'DASH:0.7', XR, curva='sai', **DASH)
camera(q + 12, 1.3, 'centro', 0.15)
linhas(q + 14)
q += 14
# Cinco golpes trocados, rápidos (o dono: "muito lerdo"), cada um defendido.
for i, golpe in enumerate(('L', 'M', 'L', 'M', 'H')):
    bate, defende = (J, R) if i % 2 == 0 else (R, J)
    xb = XJ if bate is J else XR
    xd = XR if bate is J else XJ
    quem = 'jogador' if bate is J else 'rival'
    forte = golpe == 'H'
    bate.k(q, f'@{golpe}:prepara', xb)
    bate.k(q + 4, f'@{golpe}:acerta', xb, curva='sai', rastro=3)
    defende.k(q, 'DEFENSE_STAND:0.2', xd)
    defende.k(q + 4, 'DEFENSE_STAND:0.4', xd)
    impacto(q + 4, quem, 2 if forte else 1, tipo='defesa', congela=0.2 if forte else 0.06)
    bate.k(q + 9, f'@{golpe}:fim', xb)
    defende.k(q + 9, 'DEFENSE_STAND:0.7', xd)
    q += 16 if forte else 11
# Os dois pulam para trás, bem longe um do outro (o dono: "ainda estão muito perto").
J.k(q, 'BACKDASH:0.1', XJ)
R.k(q, 'BACKDASH:0.1', XR)
J.k(q + 18, 'BACKDASH:0.4', 'parede+120', y=60, curva='sai', rastro=3)
R.k(q + 18, 'BACKDASH:0.4', 'parede+600', y=60, curva='sai', rastro=3)
J.k(q + 30, 'LAND:0.3', 'parede+70', y=0, curva='entra')
R.k(q + 30, 'LAND:0.3', 'parede+680', y=0, curva='entra')
camera(q + 18, 1.0, 'centro', 0.5)
J.k(q + 42, 'IDLE:0', 'parede+70', ciclo=12)
R.k(q + 42, 'IDLE:0', 'parede+680', ciclo=12)
q += 50
# O locutor, antes de a luta continuar.
fala(q, 'narrador', 'QUE SEQUÊNCIA FOI ESSA, MEUS AMIGOS!', segura=1.4, expressao='empolgado', lado='direita')
ev(q + 4, 'stamp', imagem='torneio/stamp_locutor.jpg', segundos=2.4)
q += 30
QUADROS = q + 20


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
