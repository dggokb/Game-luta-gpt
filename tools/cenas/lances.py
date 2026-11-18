#!/usr/bin/env python3
"""Lances prontos das cenas animadas à mão, aprovados pelo dono para usar de novo.

LANÇAMENTO NO FUNDO PRETO (o dono, 18/11, cena "Já vai desistir?": "salva esses lançamentos como
padrão, porque vai ter bastante"): quem é lançado (para a parede, para cima) ou cai fica sozinho no
meio da tela, o cenário some num fundo preto e as linhas de ação brancas correm atrás dele; o outro
lutador some. No fim o cenário volta de uma vez, no contato (a parede, o chão).

    from lances import voo_no_fundo_preto, sumir

    # O jogador voa até a parede de q+8 a q+30 (as chaves do voo dele são da cena):
    voo_no_fundo_preto(ev, q + 8, q + 30, 'velocidade')
    sumir(R, q + 8, q + 30, '@H:fim', 'jogador+0')
    J.k(q + 30, 'HIT_AIR:0.5', 'parede+60', y=110, rot=-25, curva='sai', rastro=7)

Estilos: 'velocidade' (riscos de lado: voando para a parede), 'subir' (lançado para cima: o mundo
passa para baixo), 'descer' (caindo: as linhas sobem). Junte uma câmera lenta (ação "velocidade")
no voo para dar peso. O motor (CinematicaLuta, linhas com "fundo") prende a câmera em quem está em
foco: ele fica no meio da tela, na altura do centro do giro da pose, e ela não para na borda do mundo.
"""


def voo_no_fundo_preto(ev, q0, q1, estilo, foco='jogador', zoom=1.3, depois_zoom=1.15, depois_foco='centro'):
    """Fundo preto com as linhas `estilo` de q0 a q1, a câmera presa em `foco` ('jogador' ou
    'rival'). Em q1 o cenário volta e a câmera corta para `depois_foco` no `depois_zoom`.
    `ev(q, acao, **campos)` é o que a cena usa para pôr um evento no quadro q."""
    if estilo not in ('velocidade', 'subir', 'descer', 'concentracao'):
        raise ValueError(f'estilo {estilo}')
    ev(q0, 'camera', zoom=zoom, foco=foco, segundos=0.0)
    ev(q0, 'linhas', estilo=estilo, fundo=True, segundos=0.06)
    ev(q1, 'linhas', ligar=False, segundos=0.05)
    ev(q1, 'camera', zoom=depois_zoom, foco=depois_foco, segundos=0.0)


def sumir(lutador, q0, q1, pose, x, y=0, volta=None):
    """O outro lutador some de q0 a q1 (segurando `pose` em x, y) e reaparece em q1 + 1: na mesma
    pose, ou em `volta` = (pose, x, y), onde ele deve estar quando o cenário volta. Entre q0 e q1 a
    cena não põe outras chaves nele (a transparência seria interpolada)."""
    lutador.k(q0, pose, x, y=y, alfa=0.0, curva='corte')
    lutador.k(q1, pose, x, y=y, alfa=0.0)
    vp, vx, vy = volta if volta else (pose, x, y)
    lutador.k(q1 + 1, vp, vx, y=vy, alfa=1.0, curva='corte')
