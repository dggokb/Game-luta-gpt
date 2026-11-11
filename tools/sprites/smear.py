#!/usr/bin/env python3
"""Smear: o membro que bate esticado para trás, na direção do golpe, nos quadros mais rápidos.

O quadro esticado do anime (smear): a ponta (o pé, o punho) fica desenhada e nítida, e as cores
do próprio membro (a calça, a faixa, o tênis, a luva) são arrastadas para trás pelo caminho que ele
acabou de fazer, em línguas de comprimentos diferentes que terminam em ponta, de borda dura, sem
desfoque. Não é efeito por cima: é a própria arte do personagem, e entra na folha do golpe.

Como sabe o que esticar: o caminho do golpe em `silhueta.caminho` do character.json (a ponta e a
junta de trás em cada desenho, medidas por tools/sprites/linha_de_acao.py). O membro é a parte do
desenho perto da reta junta -> ponta; a direção e o comprimento do arrasto são o quanto a ponta
andou desde o desenho anterior.

Escreve uma fonte nova ao lado da original (`art/sprites/source/<atlas>_smear.png`), aponta o clip
para ela e guarda no clip os parâmetros (a original fica em `sourceReferences`, para refazer). Depois:

    python3 tools/sprites/smear.py player_base HEAVY_STRAIGHT [--quadros 4,5] [--previa /tmp/s.png]
    python3 tools/sprites/build_characters.py --write

Sem --quadros, estica o desenho em que a ponta mais andou e o anterior a ele. --desfazer volta o clip à fonte
original.
"""
import argparse
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]

# Parâmetros de partida (px da folha; a folha do p01 tem 2 px por px do mundo).
COMPRIMENTO = 1.0     # o arrasto cobre esta fração do quanto a ponta andou
LARGURA_MEMBRO = 0.42  # meia largura do membro em volta da reta junta -> ponta, fração dela
PONTA_LIVRE = 0.18    # o trecho da ponta (o pé, o punho) que não estica, fração do membro
LINGUA_MIN, LINGUA_MAX = 7, 18   # largura de cada língua (px da folha)
RETO = 0.7            # cosseno entre o movimento da ponta e o membro acima do qual o golpe é reto


def ler(personagem, golpe):
    ch = json.loads((ROOT / 'characters' / personagem / 'character.json').read_text(encoding='utf-8'))
    a = ch['animations'][golpe]
    clip_path = HERE / 'clips' / f"{a['atlas']}.json"
    clip = json.loads(clip_path.read_text(encoding='utf-8'))
    rep = json.loads((HERE / 'reports' / f"{a['atlas']}.report.json").read_text(encoding='utf-8'))
    lado = -1 if ch.get('artFacing') == 'left' else 1
    return a, clip_path, clip, rep, lado


def linguas(n, rng):
    """Comprimento de cada linha (0 a 1) num feixe de n linhas: línguas de larguras e tamanhos
    diferentes, cada uma mais comprida no meio e pontuda nas beiradas."""
    comp = np.zeros(n, np.float32)
    i = 0
    while i < n:
        w = int(rng.integers(LINGUA_MIN, LINGUA_MAX + 1))
        topo = float(rng.uniform(0.45, 1.0))
        s = np.arange(w, dtype=np.float32)
        perfil = 1.0 - np.abs((s - (w - 1) / 2) / max(1.0, w / 2)) ** 1.3
        comp[i:i + w] = (topo * np.clip(perfil, 0.12, 1.0))[: max(0, n - i)]
        i += w
    # O feixe inteiro afina nas beiradas do membro.
    t = np.linspace(-1, 1, n)
    return comp * (0.35 + 0.65 * np.sqrt(np.clip(1 - t * t, 0, 1)))


def esticar(cel, ponta, junta, ponta_antes, junta_antes, rng):
    """O quadro com o membro esticado para trás: cada ponto do membro deixa uma língua até onde ele
    estava no desenho anterior (o pé, que andou muito, uma cauda longa; o joelho, quase nada), e o
    rastro vira a área que o membro varreu, em giro (chute em arco) ou reto (soco). Coordenadas em
    px da célula; devolve a célula nova (RGBA)."""
    px = np.asarray(cel).astype(np.float32)
    h, w = px.shape[:2]
    P, J, P0, J0 = (np.array(p, np.float32) for p in (ponta, junta, ponta_antes, junta_antes))
    seg, seg0 = P - J, P0 - J0
    L, L0 = float(np.hypot(*seg)) or 1.0, float(np.hypot(*seg0)) or 1.0
    n, n0 = np.array([-seg[1], seg[0]]) / L, np.array([-seg0[1], seg0[0]]) / L0
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    rx, ry = xx - J[0], yy - J[1]
    t = (rx * seg[0] + ry * seg[1]) / L ** 2          # 0 na junta, 1 na ponta
    o = rx * n[0] + ry * n[1]                          # px para o lado da reta
    tc = np.clip(t, 0.0, 1.08)
    perto = np.hypot(rx - tc * seg[0], ry - tc * seg[1]) <= LARGURA_MEMBRO * L
    membro = perto & (px[..., 3] > 128) & (t > 0.05) & (t < 1.0 - PONTA_LIVRE * 0.5)
    if membro.sum() < 50:
        return cel
    # Onde cada ponto do membro estava: o mesmo t e o mesmo o, no membro do desenho anterior.
    tm, om = t[membro], o[membro]
    ax = J0[0] + tm * seg0[0] + om * n0[0]
    ay = J0[1] + tm * seg0[1] + om * n0[1]
    bx, by = xx[membro], yy[membro]
    vx, vy = ax - bx, ay - by                          # para trás, até onde estava
    # Golpe reto (a ponta anda na direção do próprio membro: o soco, o chute que estica do
    # joelho dobrado): o lugar de onde cada ponto veio fica dentro do corpo e a língua some nele.
    # Aí o membro inteiro é arrastado reto, o quanto a ponta andou, e as línguas saem atrás.
    mov = P - P0
    reto = abs(float(mov @ seg)) / ((float(np.hypot(*mov)) or 1.0) * L) > RETO
    if reto:
        vx = np.full_like(bx, -mov[0])
        vy = np.full_like(by, -mov[1])
    dist = np.hypot(vx, vy)
    if dist.max() < 8:
        return cel
    # As línguas: faixas atravessadas ao movimento médio, cada uma com o seu comprimento.
    md = np.array([-(vx * dist).sum(), -(vy * dist).sum()])
    md /= np.hypot(*md) or 1.0
    lane = np.round(bx * -md[1] + by * md[0]).astype(int)
    l0 = lane.min()
    comp = linguas(int(lane.max() - l0 + 1), rng)[lane - l0]
    alcance = COMPRIMENTO * dist * comp
    cores = px[membro]
    camada = np.zeros_like(px)
    # Do mais longe para o mais perto: a parte de perto da perna fica por cima.
    passos = np.linspace(1.0, 0.0, int(dist.max() * 2) + 2)
    for f in passos:
        vivo = f * dist <= alcance
        if not vivo.any():
            continue
        qx = np.round(bx[vivo] + vx[vivo] * f).astype(int)
        qy = np.round(by[vivo] + vy[vivo] * f).astype(int)
        ok = (qx >= 0) & (qx < w) & (qy >= 0) & (qy < h)
        camada[qy[ok], qx[ok], :3] = cores[vivo][ok, :3]
        camada[qy[ok], qx[ok], 3] = 255
    # As línguas tomam o lugar do membro (como no smear desenhado: o braço vira as línguas) e a
    # ponta (o pé, o punho) volta inteira por cima.
    out = cel.copy()
    out.alpha_composite(Image.fromarray(camada.astype(np.uint8), 'RGBA'))
    ponta_m = perto & (px[..., 3] > 0) & (t >= 1.0 - PONTA_LIVRE)
    final = np.asarray(out).copy()
    final[ponta_m] = np.asarray(cel)[ponta_m]
    return Image.fromarray(final, 'RGBA')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('golpe')
    p.add_argument('--quadros', help='desenhos a esticar (ex.: 4,5); sem isto, o de maior salto e o anterior')
    p.add_argument('--semente', type=int, default=11)
    p.add_argument('--previa', help='só grava uma prévia dos quadros (não mexe no clip)')
    p.add_argument('--desfazer', action='store_true', help='volta o clip para a fonte original')
    a = p.parse_args()
    anim, clip_path, clip, rep, lado = ler(a.personagem, a.golpe)
    smear = clip.get('smear')
    original = smear['fonte'] if smear else clip['source']
    if a.desfazer:
        if smear:
            clip['source'] = original
            clip['sourceReferences'] = [r for r in clip.get('sourceReferences', []) if r != original]
            if not clip['sourceReferences']:
                del clip['sourceReferences']
            del clip['smear']
            clip_path.write_text(json.dumps(clip, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print('clip na fonte original:', original)
        return
    cam = anim['silhueta']['caminho']['pontos']
    lay = rep['layout']
    fw, fh, cols = lay['frameWidth'], lay['frameHeight'], lay['columns']
    k = rep['worldScale'] / rep.get('pixelScale', 1)          # px do mundo por px da folha

    def folha_xy(x, y):
        return lay['rootX'] + lado * x / k, lay['rootY'] - y / k

    ordem = sorted(int(q) for q in cam)
    andou = {q: math.hypot(cam[str(q)][0] - cam[str(p)][0], cam[str(q)][1] - cam[str(p)][1])
             for p, q in zip(ordem, ordem[1:])}
    maior = max(andou, key=andou.get)
    antes_dele = ordem[ordem.index(maior) - 1]
    quadros = ([int(x) for x in a.quadros.split(',')] if a.quadros
               else sorted({maior} | ({antes_dele} if antes_dele in andou else set())))
    folha = Image.open(ROOT / original).convert('RGBA')
    rng = np.random.default_rng(a.semente)
    feitos = []
    for q in quadros:
        if str(q) not in cam or q == ordem[0]:
            raise SystemExit(f'o desenho {q} não tem caminho antes dele (caminho: {ordem})')
        ant = ordem[ordem.index(q) - 1]
        x0, y0 = (q % cols) * fw, (q // cols) * fh
        cel = folha.crop((x0, y0, x0 + fw, y0 + fh))
        v = cam[str(q)]
        u = cam[str(ant)]
        nova = esticar(cel, folha_xy(v[0], v[1]), folha_xy(v[2], v[3]), folha_xy(u[0], u[1]), folha_xy(u[2], u[3]), rng)
        folha.paste(nova, (x0, y0))
        feitos.append((q, round(andou[q])))
    print('esticados (desenho, px do mundo que a ponta andou):', feitos)
    if a.previa:
        cels = [folha.crop(((q % cols) * fw, (q // cols) * fh, (q % cols + 1) * fw, (q // cols + 1) * fh))
                for q in range(min(ordem) - 1 if min(ordem) > 0 else 0, max(ordem) + 2)]
        prev = Image.new('RGBA', (fw * len(cels), fh), (70, 70, 80, 255))
        for i, c in enumerate(cels):
            prev.alpha_composite(c, (i * fw, 0))
        prev.save(a.previa)
        print('prévia:', a.previa)
        return
    destino = Path(original).with_name(Path(original).stem + '_smear.png')
    folha.save(ROOT / destino, optimize=True)
    clip['source'] = destino.as_posix()
    refs = clip.get('sourceReferences', [])
    if original not in refs:
        clip['sourceReferences'] = refs + [original]
    clip['smear'] = {'fonte': original, 'quadros': quadros, 'semente': a.semente,
                     'ferramenta': 'tools/sprites/smear.py'}
    clip_path.write_text(json.dumps(clip, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('fonte nova:', destino, '| clip:', clip_path.relative_to(ROOT))
    print('agora: python3 tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
