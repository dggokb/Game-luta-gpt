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

Sem --quadros, estica o desenho em que a ponta mais andou e o anterior a ele.

Desenho com efeito do vídeo por cima do corpo (clarão, braço borrado, arco): --trocar "4=8,5=8"
põe no lugar a cópia do desenho limpo da mesma pose (o smear cobre a passagem). Golpe sem caminho:
--sem-smear (só a troca), linha_de_acao.py --write, e de novo sem --sem-smear. --desfazer volta o clip à fonte
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
COMPRIMENTO = (1.0, 1.0)  # padrão de --comprimento (meio, bordas): fração do quanto a ponta andou
LARGURA_MEMBRO = 0.42  # meia largura do membro em volta da reta junta -> ponta, fração dela
PONTA_LIVRE = 0.18    # o trecho da ponta (o pé, o punho) que não estica, fração do membro
LINGUA_MIN, LINGUA_MAX = 7, 18   # largura de cada língua (px da folha)
MARGEM = 12           # px da borda do quadro que as línguas não passam (o build exige 8)
ESCURO = 90            # --sem-escuro: pixel com o canal mais claro abaixo disto não vira língua
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


def so_corpo(cel, abre=5, borda=3):
    """O desenho sem o efeito do vídeo (faísca, anel, poeira, brilho): o corpo é opaco e o efeito,
    quase todo, meio transparente ou fino. Fica o maior pedaço opaco (aberto para soltar os
    espinhos finos da faísca), com o opaco grudado nele de volta (dedos, bico do tênis) e uma
    borda de {@code borda} px para o contorno suave."""
    import cv2
    px = np.asarray(cel).copy()
    a = px[..., 3]
    opaco = (a >= 250).astype(np.uint8)
    k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * abre + 1,) * 2)
    aberto = cv2.morphologyEx(opaco, cv2.MORPH_OPEN, k)
    n, lab, st, _ = cv2.connectedComponentsWithStats(aberto, connectivity=8)
    if n < 2:
        return cel
    corpo = (lab == 1 + int(np.argmax(st[1:, cv2.CC_STAT_AREA]))).astype(np.uint8)
    corpo = (cv2.dilate(corpo, k) & opaco) | corpo
    m = cv2.dilate(corpo, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * borda + 1,) * 2)) > 0
    px[~m] = 0
    return Image.fromarray(px, 'RGBA')


def esticar(cel, ponta, junta, ponta_antes, junta_antes, rng, comprimento=COMPRIMENTO, sem_escuro=False, clarear=0.0, alfa=1.0):
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
    # Só o desenho opaco vira língua: o halo meio transparente do vídeo daria línguas sujas.
    membro = perto & (px[..., 3] >= 250) & (t > 0.05) & (t < 1.0 - PONTA_LIVRE * 0.5)
    if sem_escuro:
        # O membro passa na frente de roupa escura (o braço do S2 diante da regata): o escuro fica.
        membro &= px[..., :3].max(axis=2) > ESCURO
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
    n_lane = int(lane.max() - l0 + 1)
    comp = linguas(n_lane, rng)[lane - l0]
    # O quanto estica vai do meio do feixe (comprimento[0]) às bordas (comprimento[1]), em curva.
    tl = np.linspace(-1, 1, n_lane)[lane - l0]
    meio, bordas = comprimento
    fator = bordas + (meio - bordas) * np.sqrt(np.clip(1 - tl * tl, 0, 1))
    alcance = fator * dist * comp
    # Nada passa da margem do quadro (o build recusa): se a língua mais longa sairia, o feixe todo
    # encolhe na mesma proporção, e as pontas continuam pontudas (cortar reto na borda, não).
    with np.errstate(divide='ignore', invalid='ignore'):
        ux, uy = vx / np.maximum(dist, 1e-6), vy / np.maximum(dist, 1e-6)
        lim_x = np.where(ux > 0, (w - 1 - MARGEM - bx) / ux, np.where(ux < 0, (MARGEM - bx) / ux, np.inf))
        lim_y = np.where(uy > 0, (h - 1 - MARGEM - by) / uy, np.where(uy < 0, (MARGEM - by) / uy, np.inf))
    cabe = np.maximum(np.minimum(lim_x, lim_y), 0.0)
    if (alcance > cabe).any():
        alcance = alcance * float(np.min(np.where(alcance > 0, cabe / np.maximum(alcance, 1e-6), np.inf)).clip(0.0, 1.0))
    cores = px[membro].copy()
    if clarear > 0:
        # Roupa preta: a língua escura vira um bloco sólido. Puxa o escuro para um cinza-azulado de
        # ar, tanto mais quanto mais escuro; o que já é claro (pele, faixa vermelha) quase não muda.
        escuro = 1.0 - cores[:, :3].max(axis=1, keepdims=True) / 255.0
        cores[:, :3] += (np.array([150.0, 156.0, 175.0]) - cores[:, :3]) * clarear * escuro
    camada = np.zeros_like(px)
    # Do mais longe para o mais perto: a parte de perto da perna fica por cima.
    topo = max(1.0, *comprimento)
    passos = np.linspace(topo, 0.0, int(dist.max() * topo * 2) + 2)
    for f in passos:
        vivo = f * dist <= alcance
        if not vivo.any():
            continue
        qx = np.round(bx[vivo] + vx[vivo] * f).astype(int)
        qy = np.round(by[vivo] + vy[vivo] * f).astype(int)
        ok = (qx >= 0) & (qx < w) & (qy >= 0) & (qy < h)
        camada[qy[ok], qx[ok], :3] = cores[vivo][ok, :3]
        camada[qy[ok], qx[ok], 3] = 255 * alfa
    # As línguas tomam o lugar do membro (como no smear desenhado: o braço vira as línguas) e a
    # ponta (o pé, o punho) volta inteira por cima.
    if clarear > 0:
        # Roupa escura: as línguas claras ficam atrás e o membro desenhado continua por cima.
        out = Image.fromarray(camada.astype(np.uint8), 'RGBA')
        out.alpha_composite(cel)
        return out
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
    p.add_argument('--comprimento', default=','.join(map(str, COMPRIMENTO)),
                   help='o quanto estica, "meio,bordas" do feixe (ou um número para os dois): '
                        '1 = até onde o membro estava no desenho anterior')
    p.add_argument('--trocar', help='antes de esticar, troca desenhos com efeito do vídeo por cima do corpo '
                                    'pelo desenho limpo da mesma pose: "4=8,5=8" (o 4 e o 5 viram cópia do 8)')
    p.add_argument('--opacidade', help='opacidade das línguas em cada desenho de --quadros, na ordem: "1,0.5" '
                                       '(sem isto: com 2 desenhos, o segundo, o do acerto, a 50%%)')
    p.add_argument('--clarear', type=float, help='0 a 1: puxa as línguas escuras (roupa preta) para um cinza-azulado')
    p.add_argument('--sem-escuro', action='store_true', help='o escuro (roupa preta atrás do membro) não vira língua')
    p.add_argument('--antes', help='de que desenho cada um estica: "7=5" (sem isto, o último do caminho '
                                   'com a ponta noutro lugar)')
    p.add_argument('--limpar', help='desenhos de onde sai o efeito do vídeo (fica só o corpo opaco): '
                                    '"5,6,9" ou "todos"')
    p.add_argument('--sem-smear', action='store_true', help='só a troca (para medir o caminho na folha limpa)')
    p.add_argument('--previa', help='só grava uma prévia dos quadros (não mexe no clip)')
    p.add_argument('--desfazer', action='store_true', help='volta o clip para a fonte original')
    a = p.parse_args()
    v_ = [float(x) for x in str(a.comprimento).split(',')]
    a.comprimento = (v_[0], v_[-1])
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
    if a.trocar is None and smear:
        a.trocar = smear.get('trocar')
    trocas = {int(d): int(o) for d, o in (t.split('=') for t in a.trocar.split(','))} if a.trocar else {}
    a.sem_escuro = a.sem_escuro or bool(smear and smear.get('semEscuro'))
    if a.opacidade is None and smear and 'opacidade' in smear:
        a.opacidade = ','.join(map(str, smear['opacidade']))
    if a.clarear is None:
        a.clarear = float(smear.get('clarear', 0.0)) if smear else 0.0
    if a.antes is None and smear:
        a.antes = smear.get('antes')
    antes_de = {int(d): int(o) for d, o in (t.split('=') for t in a.antes.split(','))} if a.antes else {}
    if a.limpar is None and smear:
        a.limpar = smear.get('limpar')
    lay = rep['layout']
    fw, fh, cols = lay['frameWidth'], lay['frameHeight'], lay['columns']
    folha = Image.open(ROOT / original).convert('RGBA')
    if a.limpar:
        n = lay['frameCount']
        for q in (range(n) if a.limpar == 'todos' else [int(x) for x in a.limpar.split(',')]):
            caixa = ((q % cols) * fw, (q // cols) * fh, (q % cols + 1) * fw, (q // cols + 1) * fh)
            folha.paste(so_corpo(folha.crop(caixa)), caixa[:2])
    limpa = folha.copy()
    for d, o in trocas.items():
        folha.paste(limpa.crop(((o % cols) * fw, (o // cols) * fh, (o % cols + 1) * fw, (o // cols + 1) * fh)),
                    ((d % cols) * fw, (d // cols) * fh))
    caminho = anim.get('silhueta', {}).get('caminho')
    if a.sem_smear or not caminho:
        if not a.sem_smear:
            raise SystemExit('o golpe não tem caminho medido: rode com --sem-smear, meça (linha_de_acao.py) e rode de novo')
        cam, quadros = {}, []
    else:
        cam = caminho['pontos']
    k = rep['worldScale'] / rep.get('pixelScale', 1)          # px do mundo por px da folha

    def folha_xy(x, y):
        return lay['rootX'] + lado * x / k, lay['rootY'] - y / k

    ordem = sorted(int(q) for q in cam)
    andou = {q: math.hypot(cam[str(q)][0] - cam[str(p)][0], cam[str(q)][1] - cam[str(p)][1])
             for p, q in zip(ordem, ordem[1:])}
    if cam:
        maior = max(andou, key=andou.get)
        antes_dele = ordem[ordem.index(maior) - 1]
        quadros = ([int(x) for x in a.quadros.split(',')] if a.quadros
                   else sorted({maior} | ({antes_dele} if antes_dele in andou else set())))
    rng = np.random.default_rng(a.semente)
    feitos = []
    def ponto(q):
        """O ponto do caminho no desenho q; num desenho trocado, o de outro com a mesma pose."""
        if str(q) in cam and q not in trocas:
            return cam[str(q)]
        fonte = trocas.get(q, q)
        for d in [fonte] + [d for d, o in trocas.items() if o == fonte]:
            if str(d) in cam:
                return cam[str(d)]
        return None

    if a.opacidade:
        opac = [float(x) for x in a.opacidade.split(',')]
    else:
        # O desenho do acerto fica parado na pausa: as línguas dele a meio caminho de sumir.
        opac = [1.0, 0.5] if len(quadros) == 2 else [1.0] * len(quadros)
    opac = (opac + [opac[-1]] * len(quadros))[:len(quadros)]
    for q, alfa in zip(quadros, opac):
        v = ponto(q)
        # O antes: o último desenho do caminho com a ponta noutro lugar (uma cópia da mesma pose não conta).
        antes = [int(d) for d in cam if int(d) < q and v is not None
                 and math.hypot(cam[d][0] - v[0], cam[d][1] - v[1]) > 8]
        if v is None or not antes:
            raise SystemExit(f'o desenho {q} não tem caminho antes dele (caminho: {ordem})')
        ant = antes_de.get(q, max(antes))
        andou[q] = math.hypot(v[0] - cam[str(ant)][0], v[1] - cam[str(ant)][1])
        x0, y0 = (q % cols) * fw, (q // cols) * fh
        cel = folha.crop((x0, y0, x0 + fw, y0 + fh))
        u = cam[str(ant)]
        nova = esticar(cel, folha_xy(v[0], v[1]), folha_xy(v[2], v[3]), folha_xy(u[0], u[1]), folha_xy(u[2], u[3]), rng, a.comprimento, a.sem_escuro, a.clarear, alfa)
        folha.paste(nova, (x0, y0))
        feitos.append((q, round(andou[q])))
    print('esticados (desenho, px do mundo que a ponta andou):', feitos)
    if a.previa and cam:
        cels = [folha.crop(((q % cols) * fw, (q // cols) * fh, (q % cols + 1) * fw, (q // cols + 1) * fh))
                for q in range(min(ordem) - 1 if min(ordem) > 0 else 0, max(ordem) + 2)]
        prev = Image.new('RGBA', (fw * len(cels), fh), (70, 70, 80, 255))
        for i, c in enumerate(cels):
            prev.alpha_composite(c, (i * fw, 0))
        prev.save(a.previa)
        print('prévia:', a.previa)
        return
    if clip['source'].endswith('_sr_hd.png'):
        # O clipe passou pela melhoria de nitidez (tools/sprites/sr_hd.py) depois do smear: refazer o
        # smear volta para a fonte sem nitidez. Não roda sem querer: a outra fonte ficaria solta.
        raise SystemExit(f"{clip['source']} é a versão com nitidez (sr_hd.py). Refaça o smear e depois "
                         'rode sr_hd.py de novo neste clipe; para seguir assim mesmo, volte o clipe '
                         'para a fonte _smear.png primeiro.')
    destino = Path(original).with_name(Path(original).stem + '_smear.png')
    folha.save(ROOT / destino, optimize=True)
    clip['source'] = destino.as_posix()
    refs = clip.get('sourceReferences', [])
    if original not in refs:
        clip['sourceReferences'] = refs + [original]
    clip['smear'] = {'fonte': original, 'quadros': quadros, 'semente': a.semente, 'comprimento': list(a.comprimento),
                     'ferramenta': 'tools/sprites/smear.py'}
    if a.trocar:
        clip['smear']['trocar'] = a.trocar
    if a.limpar:
        clip['smear']['limpar'] = a.limpar
    if a.antes:
        clip['smear']['antes'] = a.antes
    if a.sem_escuro:
        clip['smear']['semEscuro'] = True
    if a.clarear:
        clip['smear']['clarear'] = a.clarear
    clip['smear']['opacidade'] = opac
    clip_path.write_text(json.dumps(clip, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('fonte nova:', destino, '| clip:', clip_path.relative_to(ROOT))
    print('agora: python3 tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
