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

# ------------------------------------------------------------------ 1. A provocação
J.k(0, 'IDLE:0', 'atual', ciclo=12)
R.k(0, 'IDLE:0', 'atual', ciclo=12)
camera(0, 1.1, 'centro', 0.3)
# Os dois se encaram, o jogador ofegante.
respirar(J, 30, 330, 'HIT_STAND:0.15', 'jogador')
camera(40, 1.9, 'jogador', 0.5)
linhas(40, 'concentracao', escuro=0.25)
fala(60, 'jogador', 'Droga... tá difícil!', segura=1.0, expressao='normal')
linhas(200)
camera(204, 1.9, 'rival', 0.4)
linhas(204, 'concentracao', escuro=0.25)
fala(224, 'rival', 'Já vai desistir, é?', segura=1.0, lado='rival')
R.k(204, 'IDLE:0', 'rival', ciclo=8)
linhas(350)
# A pausa: plano aberto, os dois parados.
camera(352, 1.05, 'centro', 0.4)
J.k(352, 'DEFENSE_STAND:0.1', 'jogador')
R.k(352, 'IDLE:0', 'rival', ciclo=10)
q = 400

# ------------------------------------------------------------------ 2. O adversário investe
# Antecipação: ele abaixa, segura, e arranca.
R.k(q, 'LAND:0.4', 'rival', curva='corte')
R.k(q + 18, 'LAND:0.4', 'rival')
efeito(q + 18, 'rival', 'fumaca_dash_v05')
linhas(q + 18, 'velocidade', escuro=0.2)
R.k(q + 18, 'DASH:0.3', 'rival', curva='corte', **DASH)
R.k(q + 28, 'DASH:0.7', 'jogador+150', curva='sai', **DASH)
camera(q + 26, 1.4, 'centro', 0.1)
linhas(q + 30)
q += 30
# Dois golpes defendidos, com o contato parado e uma pausa entre eles.
for golpe, x0, x1 in (('L', 'jogador-15', 'jogador-30'), ('M', 'jogador-45', 'jogador-60')):
    R.k(q, f'@{golpe}:prepara', 'jogador+140')
    R.k(q + 8, f'@{golpe}:prepara', 'jogador+140')
    R.k(q + 12, f'@{golpe}:acerta', 'jogador+125', curva='sai', rastro=3)
    J.k(q + 12, 'DEFENSE_STAND:0.4', x0)
    impacto(q + 12, 'rival', 1, congela=0.18)
    J.k(q + 24, 'DEFENSE_STAND:0.7', x1, curva='sai')
    R.k(q + 26, f'@{golpe}:fim', 'jogador+110')
    q += 36
# Dois que pegam: o segundo em câmera lenta.
for i, (golpe, rec) in enumerate((('L', 'jogador-75'), ('M', 'jogador-100'))):
    R.k(q, f'@{golpe}:prepara', 'jogador+95')
    R.k(q + 8, f'@{golpe}:acerta', 'jogador+85', curva='sai', rastro=3)
    J.k(q + 8, 'HIT_STAND:0.2', rec, rastro=2)
    impacto(q + 8, 'rival', 2 if i else 1, congela=0.22 if i else 0.16)
    dano(q + 8, 0.02)
    if i:
        lenta(q + 8, antes=5, depois=14, valor=0.35)
    J.k(q + 24, 'HIT_STAND:0.7', rec, curva='sai')
    R.k(q + 24, f'@{golpe}:fim', 'jogador+85')
    q += 32
# O forte: ele recua o braço e segura, a tela escurece, e o golpe joga o jogador na parede.
camera(q, 1.6, 'centro', 0.3)
linhas(q, 'concentracao', escuro=0.4, segundos=0.2)
R.k(q, '@H:inicio', 'jogador+75')
R.k(q + 24, '@H:prepara', 'jogador+70', curva='suave')
J.k(q, 'HIT_STAND:0.7', 'jogador-100')
q += 24
linhas(q + 4)
R.k(q + 6, '@H:acerta', 'jogador+55', curva='sai', rastro=5)
J.k(q + 6, 'HIT_AIR:0', 'jogador-110', y=30, rastro=4)
impacto(q + 6, 'rival', 3, letreiro='BAM!', congela=0.5, stamp=stamp_dano('bam'))
lenta(q + 6, antes=2, depois=10, valor=0.3)
dano(q + 6, 0.04)
camera(q + 8, 1.15, 'centro', 0.25)
linhas(q + 8, 'velocidade', escuro=0.25)
J.k(q + 26, 'HIT_AIR:0.5', 'parede+60', y=110, rot=-25, curva='sai', rastro=7)
# Bate na parede, a parede treme, ele escorrega até o chão.
impacto(q + 27, 'rival', 2, congela=0.25)
efeito(q + 27, 'jogador', 'fumaca_queda_v01', escala=1.0)
linhas(q + 30)
J.k(q + 30, 'KNOCKDOWN:0.3', 'parede+70', y=90, rot=-10)
J.k(q + 52, 'GROUNDED:0', 'parede+80', y=0, rot=0, curva='entra')
efeito(q + 52, 'jogador', 'fumaca_pouso')
R.k(q + 30, '@H:fim', 'jogador+55')
R.k(q + 60, 'IDLE:0', 'parede+340', ciclo=10, curva='suave')
q += 90

# ------------------------------------------------------------------ 3. A troca
camera(q, 1.15, 'centro', 0.4)
fala(q + 10, 'parceiro', 'Troca comigo!', segura=0.7)
J.k(q + 90, 'GETUP:0.5', 'parede+80')
camera(q + 120, 1.7, 'jogador', 0.4)
fala(q + 130, 'jogador', 'Tá!', segura=0.5, expressao='normal')
J.k(q + 160, 'GETUP:1', 'parede+80')
q += 220
# Quem estava em campo salta para fora (por cima da tela) e o parceiro desce de lá com a voadora.
camera(q, 1.1, 'centro', 0.2)
J.k(q, 'LAND:0.4', 'parede+80', curva='corte')
J.k(q + 8, 'JUMP:0.2', 'parede+80', curva='corte')
efeito(q + 8, 'jogador', 'fumaca_pulo')
J.k(q + 20, 'JUMP:0.4', 'parede+40', y=900, curva='entra')
ev(q + 22, 'parceiro', quem='jogador')
J.k(q + 24, '@jH:prepara', 'parede+60', y=700, curva='corte')
# Ela vem descendo; a câmera vai junto e entra em câmera lenta antes do contato.
camera(q + 24, 1.3, 'jogador', 0.15)
linhas(q + 30, 'velocidade', escuro=0.25)
J.k(q + 44, '@jH:acerta', 'parede+250', y=90, curva='entra', rastro=7)
R.k(q + 24, 'DEFENSE_STAND:0.2', 'parede+340')
R.k(q + 44, 'DEFENSE_STAND:0.4', 'parede+345')
lenta(q + 44, antes=8, depois=10, valor=0.3)
impacto(q + 44, 'jogador', 2, letreiro='DOOM!', congela=0.35)
linhas(q + 46)
R.k(q + 56, 'DEFENSE_STAND:0.7', 'parede+375', curva='sai')
J.k(q + 50, 'JUMP:0.8', 'parede+230', y=50)
J.k(q + 58, 'LAND:0.2', 'parede+220', y=0, curva='entra')
efeito(q + 58, 'jogador', 'fumaca_pouso')
camera(q + 58, 1.15, 'centro', 0.3)
q += 76

# ------------------------------------------------------------------ 4. Lançada para cima
# Ele responde com a rasteira, que joga o parceiro alto; a câmera sobe com ele.
R.k(q, '@2H:prepara', 'parede+345')
R.k(q + 12, '@2H:prepara', 'parede+340')
R.k(q + 16, '@2H:acerta', 'parede+320', curva='sai', rastro=4)
J.k(q + 16, 'HIT_AIR:0', 'parede+225', y=20, rastro=3)
impacto(q + 16, 'rival', 2, congela=0.25)
dano(q + 16, 0.01)
camera(q + 18, 1.25, 'jogador', 0.2)
linhas(q + 18, 'velocidade', escuro=0.2)
J.k(q + 50, 'HIT_AIR:0.5', 'parede+200', y=380, rot=-20, curva='sai', rastro=6)
R.k(q + 30, '@2H:fim', 'parede+320')
# Ele some do chão (SHUN!) e aparece no ar, na frente do parceiro.
R.k(q + 40, 'LAND:0.4', 'parede+320')
R.k(q + 46, 'DASH:0.5', 'parede+320', alfa=1.0, rastro=5, sx=1.3, sy=0.85)
R.k(q + 48, 'DASH:0.5', 'parede+320', y=200, alfa=0.0, rastro=5, sx=1.3, sy=0.85)
ev(q + 47, 'onomatopeia', texto='SHUN!', pos=[0.7, 0.62], tamanho=100, rotacao=-8, segundos=0.5, semEsperar=True)
R.k(q + 54, '@jL:prepara', 'parede+300', y=390, alfa=0.0)
R.k(q + 58, '@jL:prepara', 'parede+290', y=392, alfa=1.0)
J.k(q + 58, 'HIT_AIR:0.5', 'parede+200', y=392, rot=-20)
q += 58
# Uma pausa no alto (os dois parados no ar) e três golpes rápidos.
for k, golpe in enumerate(('jL', 'jM', 'jL')):
    R.k(q + 8, f'@{golpe}:prepara', 'parede+290', y=394)
    R.k(q + 11, f'@{golpe}:acerta', 'parede+285', y=394, curva='sai', rastro=3)
    J.k(q + 11, 'HIT_AIR:0.2', 'parede+200', y=394, rot=-15)
    impacto(q + 11, 'rival', 1, congela=0.12)
    dano(q + 11, 0.01)
    J.k(q + 18, 'HIT_AIR:0.6', 'parede+195', y=396, rot=-18)
    q += 18
# O golpe que joga para baixo: antecipação no alto, câmera lenta, e o parceiro despenca.
linhas(q, 'concentracao', escuro=0.4, segundos=0.15)
R.k(q, '@jH:inicio', 'parede+290', y=410)
R.k(q + 20, '@jH:prepara', 'parede+290', y=420, curva='suave')
R.k(q + 26, '@jH:acerta', 'parede+275', y=405, curva='sai', rastro=5)
J.k(q + 26, 'HIT_AIR:0.8', 'parede+200', y=392, rot=-45)
lenta(q + 26, antes=3, depois=6, valor=0.3)
impacto(q + 26, 'rival', 2, congela=0.3)
linhas(q + 28, 'velocidade', escuro=0.3)
J.k(q + 44, 'KNOCKDOWN:0.4', 'parede+150', y=0, rot=-85, curva='entra', rastro=8)
impacto(q + 44, 'rival', 3, letreiro='PAH!', congela=0.55, stamp=stamp_dano('pah'))
dano(q + 44, 0.03)
efeito(q + 44, 'jogador', 'fumaca_queda_v01', escala=1.4)
linhas(q + 48)
J.k(q + 50, 'GROUNDED:0', 'parede+140', y=0, rot=0)
R.k(q + 44, 'JUMP:0.7', 'parede+330', y=220, curva='sai')
R.k(q + 70, 'LAND:0.3', 'parede+390', y=0, curva='entra')
efeito(q + 70, 'rival', 'fumaca_pouso')
R.k(q + 86, 'IDLE:0', 'parede+390', ciclo=8)
camera(q + 48, 1.1, 'centro', 0.5)
q += 110

# ------------------------------------------------------------------ 5. No chão
# Caído, o close é do centro (o foco segura o peito de quem está de pé e cortaria quem está no chão).
camera(q, 1.35, 'centro', 0.5)
linhas(q, 'concentracao', escuro=0.3)
fala(q + 20, 'jogador', 'ARGHHHH!', segura=0.7)
fala(q + 120, 'jogador', 'Droga!', segura=0.8)
linhas(q + 240)
q += 250
# De fora, o outro irmão grita; plano aberto, o adversário de pé esperando.
camera(q, 1.05, 'centro', 0.6)
fala(q + 20, 'outroIrmao', 'Heyyy! Vão desistir agora? É tudo que vocês têm?', segura=1.2)
# O STAMP do outro irmão gritando de fora da arena (a fala fica por cima dele).
ev(q + 24, 'stamp', imagem='torneio/stamp_fora_{outroIrmao}.jpg', segundos=2.2)
q += 260
# O parceiro levanta devagar, sorri e agradece.
J.k(q, 'GETUP:0.2', 'parede+140')
J.k(q + 60, 'GETUP:1', 'parede+140', curva='suave')
respirar(J, q + 70, q + 280, 'IDLE:0', 'parede+140')
camera(q + 60, 1.8, 'jogador', 0.5)
linhas(q + 70, 'concentracao', escuro=0.25)
fala(q + 80, 'jogador', 'He he... você está certo! Valeu, {irmao}!', segura=1.2, expressao='normal')
linhas(q + 270)
q += 280

# ------------------------------------------------------------------ 6. De volta à luta
# A pausa de quem se estuda, a antecipação dos dois e a arrancada.
camera(q, 1.05, 'centro', 0.4)
J.k(q, 'DEFENSE_STAND:0.1', 'parede+140')
q += 40
J.k(q, 'LAND:0.4', 'parede+140', curva='corte')
R.k(q, 'LAND:0.4', 'parede+390', curva='corte')
q += 16
# O grito da investida: o STAMP de quem está em campo avançando e gritando.
# O letreiro vai antes do STAMP no mesmo quadro: o STAMP congela a animação, e um evento depois
# dele esperaria o quadro sair.
ev(q, 'onomatopeia', texto='HAAAA!', pos=[0.29, 0.21], tamanho=112, rotacao=-10, segundos=1.0, semEsperar=True)
ev(q, 'stamp', imagem='torneio/stamp_grito_{jogador}.jpg', jogadorNaArte='esquerda', segundos=0.9)
J.k(q, 'DASH:0.3', 'parede+140', curva='corte', **DASH)
R.k(q, 'DASH:0.3', 'parede+390', curva='corte', **DASH)
efeito(q, 'jogador', 'fumaca_dash_v05')
efeito(q, 'rival', 'fumaca_dash_v05')
linhas(q, 'velocidade', escuro=0.2)
J.k(q + 10, 'DASH:0.7', 'parede+235', curva='sai', **DASH)
R.k(q + 10, 'DASH:0.7', 'parede+345', curva='sai', **DASH)
camera(q + 10, 1.35, 'centro', 0.15)
linhas(q + 12)
q += 12
# Cinco golpes trocados, cada um defendido, cada um com o contato parado.
for i, golpe in enumerate(('L', 'M', 'L', 'M', 'H')):
    bate, defende = (J, R) if i % 2 == 0 else (R, J)
    xb = 'parede+235' if bate is J else 'parede+345'
    xd = 'parede+345' if bate is J else 'parede+235'
    quem = 'jogador' if bate is J else 'rival'
    bate.k(q, f'@{golpe}:prepara', xb)
    bate.k(q + 6, f'@{golpe}:acerta', xb, curva='sai', rastro=3)
    defende.k(q, 'DEFENSE_STAND:0.2', xd)
    defende.k(q + 6, 'DEFENSE_STAND:0.4', xd)
    impacto(q + 6, quem, 2 if golpe == 'H' else 1, congela=0.25 if golpe == 'H' else 0.14)
    bate.k(q + 14, f'@{golpe}:fim', xb)
    defende.k(q + 14, 'DEFENSE_STAND:0.7', xd)
    q += 20
# Os dois pulam para trás e a luta continua.
J.k(q, 'BACKDASH:0.1', 'parede+235')
R.k(q, 'BACKDASH:0.1', 'parede+345')
J.k(q + 16, 'BACKDASH:0.4', 'parede+130', y=50, curva='sai', rastro=3)
R.k(q + 16, 'BACKDASH:0.4', 'parede+470', y=50, curva='sai', rastro=3)
J.k(q + 28, 'LAND:0.3', 'parede+110', y=0, curva='entra')
R.k(q + 28, 'LAND:0.3', 'parede+490', y=0, curva='entra')
camera(q + 16, 1.0, 'centro', 0.5)
J.k(q + 40, 'IDLE:0', 'parede+110', ciclo=12)
R.k(q + 40, 'IDLE:0', 'parede+490', ciclo=12)
QUADROS = q + 50


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
