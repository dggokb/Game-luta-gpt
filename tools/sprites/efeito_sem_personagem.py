"""Separa só o efeito (branco/branco-azulado) de um vídeo de personagem no verde.

O personagem é escuro (roupa preta, cabelo, pele, faixa vermelha). Fica o pixel claro e pouco
saturado que faz parte de uma mancha encostada no fundo verde (o clarão e os riscos sempre tocam
o fundo; o branco do olho fica preso no rosto e sai), e que não puxa para pêssego/rosa (pele e
faixa vistas através do clarão). Os tênis (embaixo, e caixas pedidas com --excluir) ficam de fora.
A transparência sobre o verde vem da chave do verde do efeito_video.py.

    python3 tools/sprites/efeito_sem_personagem.py . <pasta PNG do vídeo> <saída> --quadros 11-18 [--espelhar]
        [--raio cx,cy,r]                 some suave do raio 0,7r até r (corta o que vai longe)
        [--excluir q0,x0,y0,x1,y1 ...]   a partir do quadro q0 do vídeo, apaga essa caixa
        [--chao-tenis 640]               abaixo disso são os tênis (some suave nos 50 px de cima)
        [--borda 80]                     some suave perto da borda do vídeo (ponta cortada vira ponta)
        [--placa 19-30]                  personagem PARADO nesses quadros (sem efeito): o efeito é o
                                         que clareou em relação a eles. Guarda o brilho macio inteiro,
                                         até por cima do personagem (sem placa: chave do verde)
        [--brilho]                       sem placa: guarda o brilho meio transparente sobre o verde
        [--cores]                        guarda efeito colorido (amarelo, laranja, vermelho), não só o branco
        [--sem-corpo]                    tira manchas brancas grudadas no corpo (tênis, olho), mesmo
                                         encostando no verde: as que têm >= 30% da volta no corpo
"""
import argparse, sys
from pathlib import Path
import numpy as np
import cv2
from PIL import Image

p = argparse.ArgumentParser()
p.add_argument('repo'); p.add_argument('entrada'); p.add_argument('saida')
p.add_argument('--quadros', required=True)
p.add_argument('--espelhar', action='store_true')
p.add_argument('--raio')
p.add_argument('--excluir', nargs='*', default=[])
p.add_argument('--chao-tenis', type=int, default=640)
p.add_argument('--borda', type=int, default=80)
p.add_argument('--placa')
p.add_argument('--brilho', action='store_true')
p.add_argument('--sem-corpo', action='store_true')
p.add_argument('--cores', action='store_true')
a = p.parse_args()
sys.path.insert(0, str(Path(a.repo) / 'tools/sprites'))
import efeito_video as ev

Q0, Q1 = (int(v) for v in a.quadros.split('-'))
arquivos = sorted(Path(a.entrada).glob('*.png'))
quadros = [np.array(Image.open(f).convert('RGB')) for f in arquivos]
fundo = ev.cor_do_fundo(quadros[:3])
saida = Path(a.saida); saida.mkdir(parents=True, exist_ok=True)
for f in saida.glob('*.png'): f.unlink()
H, W = quadros[0].shape[:2]
fade = np.ones((H, W), np.float32)
if a.raio:
    cx, cy, r = (float(v) for v in a.raio.split(','))
    yy, xx = np.mgrid[0:H, 0:W]
    d = np.hypot(xx - cx, yy - cy)
    fade = np.clip((r - d) / (0.3 * r), 0, 1).astype(np.float32)
caixas = [tuple(int(v) for v in e.split(',')) for e in a.excluir]
placa = None
if a.placa:
    p0, p1 = (int(v) for v in a.placa.split('-'))
    placa = np.median(np.stack(quadros[p0:p1 + 1]).astype(np.float32), axis=0)


def pela_placa(rgb):
    """Efeito claro sobre a placa: F = a*E + (1-a)*P com E perto do branco. a é o quanto o pixel
    clareou (o canal que mais subiu, pelo espaço que tinha até o branco); a cor sai da mistura."""
    f = rgb.astype(np.float32)
    espaco = np.maximum(255.0 - placa, 1.0)
    sobe = (f - placa) / espaco
    sobe[espaco < 25] = 0                     # onde a placa já é clara (olho, tênis) não dá para ler
    alfa = np.clip(sobe.max(-1), 0, 1)
    luz = f.mean(-1) - placa.mean(-1)
    alfa[luz < 10] = 0                        # mexida do personagem/ruído, não é luz
    alfa[alfa < 0.08] = 0
    cor = (f - (1 - alfa[..., None]) * placa) / np.maximum(alfa, 1e-3)[..., None]
    cor = np.clip(cor, 0, 255)
    # Por cima do personagem a pose mexe um pouco em relação à placa e o corpo "vaza" (pele, faixa,
    # borda da roupa). Lá só fica o que saiu quase branco; sobre o verde da placa fica tudo.
    pv = placa[..., 1] - np.maximum(placa[..., 0], placa[..., 2])
    sobre_verde = cv2.erode((pv > 100).astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
    quase_branco = (cor.min(-1) > 205) & (cor.max(-1) - cor.min(-1) < 45) & (alfa > 0.3)
    if a.cores:   # luz colorida: fica o que clareou bastante (o corpo que mexeu clareia pouco)
        alfa[(alfa < 0.25) & ~sobre_verde] = 0
        alfa[cor.max(-1) < 170] = 0
    else:
        alfa[~(sobre_verde | quase_branco)] = 0
        # e em qualquer lugar, o que saiu escuro ou colorido é o corpo que mexeu, não luz
        alfa[(cor.min(-1) < 165) | (cor.max(-1) - cor.min(-1) > 75)] = 0
    branco = np.full_like(cor, 255.0)
    cor = branco + (cor - branco) * np.clip(alfa, 0, 1)[..., None]    # pouco alfa: puxa para o branco
    cor[..., 1] = np.minimum(cor[..., 1], np.maximum(cor[..., 0], cor[..., 2]) + 6)  # nada verde
    return cor, alfa * 255.0
# Sem corte reto: a borda do vídeo e a linha dos tênis apagam aos poucos (a ponta do risco que
# sairia do quadro termina fina, não quadrada).
yy, xx = np.mgrid[0:H, 0:W]
dist_borda = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy)).astype(np.float32)
fade *= np.clip(dist_borda / max(1, a.borda), 0, 1)
fade *= np.clip((a.chao_tenis - yy) / 50.0, 0, 1).astype(np.float32)

for n, q in enumerate(range(Q0, Q1 + 1)):
    rgb = quadros[q]
    if placa is not None:
        cor, alfa = pela_placa(rgb)
        for (q0, x0, y0, x1, y1) in caixas:
            if q >= q0: alfa[y0:y1, x0:x1] = 0
        alfa = cv2.GaussianBlur(alfa, (3, 3), 0.7)
        out = np.dstack([cor, np.clip(alfa * fade, 0, 255)]).astype(np.uint8)
        img = Image.fromarray(out, 'RGBA')
        if a.espelhar: img = img.transpose(Image.FLIP_LEFT_RIGHT)
        img.save(saida / f'q{n}.png')
        continue
    rgba = ev.chave(rgb, fundo)
    c = rgb.astype(np.int32)
    verde = c[..., 1] - np.maximum(c[..., 0], c[..., 2])
    claro = (c.min(-1) > 140) & (verde < 45)
    if a.cores:
        claro = (c.max(-1) > 200) & (verde < 20)
    perto_do_fundo = cv2.dilate((verde > 120).astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
    corpo = (c.max(-1) < 110) | ((c[..., 0] > c[..., 2] + 40) & (verde < 0))   # escuro, pele/vermelho
    perto_do_corpo = cv2.dilate(corpo.astype(np.uint8), np.ones((7, 7), np.uint8)) > 0
    cand = ((rgba[..., 3] > 0) & (claro | (rgba[..., 3] < 250))).astype(np.uint8)
    cand[a.chao_tenis + 1:] = 0
    for (q0, x0, y0, x1, y1) in caixas:
        if q >= q0: cand[y0:y1, x0:x1] = 0
    nlab, lab = cv2.connectedComponents(cand, connectivity=8)
    encosta = np.zeros(nlab, bool)
    encosta[np.unique(lab[perto_do_fundo & (cand > 0)])] = True
    encosta[0] = False
    manter = encosta[lab] & (claro | (rgba[..., 3] < 250))
    if a.brilho:   # o brilho meio transparente sobre o verde fica, mesmo solto e meio rosado
        cb = rgba[..., :3].astype(np.int32)
        if a.cores:
            manter |= (rgba[..., 3] > 0) & (cb.max(-1) > 170) & ~perto_do_corpo
        else:
            manter |= (rgba[..., 3] > 0) & (cb.min(-1) > 150) & (cb.max(-1) - cb.min(-1) < 70) & ~perto_do_corpo
        manter[a.chao_tenis + 1:] = False
        # o vento verde-claro do vídeo sai amarelado depois da chave (pouco azul): não é o brilho,
        # que é branco ou rosado
        if not a.cores:
            manter &= ~((cb[..., 2] < cb[..., 0] - 22) & (cb[..., 2] < cb[..., 1] - 12) & (rgba[..., 3] < 235))
    cor = rgba[..., :3].astype(np.int32)
    if a.cores:
        manter &= (cor.max(-1) > 170) & ~perto_do_corpo
    else:
        manter &= (cor.min(-1) > 160) & (cor[..., 0] - cor[..., 2] < (45 if a.brilho else 20))
    # quadradinhos da compressão do vídeo nas partes meio transparentes: o fraco sai, e as
    # manchinhas soltas (pequenas e sem miolo claro) também
    manter &= claro | (rgba[..., 3] >= (40 if a.brilho else 120))
    n2, lab2, st2, _ = cv2.connectedComponentsWithStats(manter.astype(np.uint8), connectivity=8)
    forte = np.zeros(n2, bool)
    forte[np.unique(lab2[manter & claro])] = True
    pequeno = st2[:, cv2.CC_STAT_AREA] < 90
    tirar = pequeno & ~forte
    tirar[0] = False
    manter &= ~tirar[lab2]
    if a.sem_corpo:
        n3, lab3, st3, _ = cv2.connectedComponentsWithStats(manter.astype(np.uint8), connectivity=8)
        corpo_u8 = corpo.astype(np.uint8)
        for i in range(1, n3):
            x, y, w, h, area = st3[i]
            if area > 6000: continue
            if area < 60:   # pontinho branco solto (brilho do tênis/olho)
                manter[lab3 == i] = False; continue
            x0, y0, x1, y1 = max(0, x - 4), max(0, y - 4), min(W, x + w + 4), min(H, y + h + 4)
            m = (lab3[y0:y1, x0:x1] == i).astype(np.uint8)
            volta = cv2.dilate(m, np.ones((7, 7), np.uint8)) - m
            if volta.sum() and (volta & corpo_u8[y0:y1, x0:x1]).sum() / volta.sum() >= 0.3:
                manter[y0:y1, x0:x1][m > 0] = False
    # nada rosado/lilás (o corpo borrado visto através do brilho): o efeito é branco
    cz = rgba[..., :3].astype(np.int32)
    if not a.cores:
        manter &= ~((cz[..., 0] - cz[..., 1] > 28) & (cz[..., 2] - cz[..., 1] > 8))
    if a.cores:   # o halo verde-claro que o vídeo põe em volta do efeito não é luz do efeito
        hv = rgba[..., :3].astype(np.int32)
        manter &= ~((rgba[..., 3] < 170) & (hv[..., 2] < hv[..., 0] - 30))
    out = rgba.copy()
    alfa = np.where(manter, rgba[..., 3], 0).astype(np.float32)
    alfa[manter & claro] = np.maximum(alfa[manter & claro], 235)
    # contorno macio: a mediana apaga a grade da compressão do vídeo e o desfoque leve tira o
    # degrau da borda (o jogo ainda reduz a folha, então não se nota a maciez)
    alfa = cv2.medianBlur(alfa.astype(np.uint8), 5).astype(np.float32)
    alfa = cv2.GaussianBlur(alfa, (5, 5), 1.3)
    out[..., 3] = np.clip(alfa * fade, 0, 255).astype(np.uint8)
    if a.cores:   # amarelo misturado com o verde sai oliva: volta ao dourado quente do vídeo
        oc = out[..., :3].astype(np.float32)
        amarelo = (oc[..., 2] < oc[..., 0] - 30) & (out[..., 3] > 0)
        lum = oc.max(-1, keepdims=True) / 255.0
        ouro = np.array([255, 222, 118], np.float32) * (0.75 + 0.25 * lum)
        oc[amarelo] = ouro[amarelo]
        al = out[..., 3].astype(np.float32); al[amarelo] = np.minimum(255, al[amarelo] * 1.5); out[..., 3] = al.astype(np.uint8)
        out[..., :3] = np.clip(oc, 0, 255).astype(np.uint8)
    if a.brilho and not a.cores:   # o brilho meio transparente fica com o tom oliva do verde tirado: puxa para o branco
        k = (out[..., 3:4].astype(np.float32) / 255.0) ** 0.5
        out[..., :3] = (np.array([252, 248, 252], np.float32) * (1 - k) + out[..., :3] * k).astype(np.uint8)
        out[..., 1] = np.minimum(out[..., 1], np.maximum(out[..., 0], out[..., 2]))
    img = Image.fromarray(out, 'RGBA')
    if a.espelhar: img = img.transpose(Image.FLIP_LEFT_RIGHT)
    img.save(saida / f'q{n}.png')
print('quadros', Q1 - Q0 + 1, '->', saida)
