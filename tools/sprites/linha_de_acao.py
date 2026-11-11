#!/usr/bin/env python3
"""O motor que detecta o movimento de cada golpe e cria a linha de ação dele.

Para o rastro do golpe ficar certo é preciso saber DE ONDE o braço ou a perna veio e PARA ONDE
foi: a garra que desce em arco, o chute que sai do chão, o soco que sai da guarda. Isso está na
animação. Aqui cada desenho do golpe, da guarda até o membro esticado, passa por um modelo de pose
(MediaPipe Pose: ombros, cotovelos, punhos, joelhos, pés), e o caminho da ponta do membro que
bate, desenho por desenho, é a linha de ação. O jogo desenha a fita do rastro por ela.

    python3 tools/sprites/linha_de_acao.py                 # mostra o caminho de cada golpe
    python3 tools/sprites/linha_de_acao.py p04 --write     # grava no personagem
    python3 tools/sprites/linha_de_acao.py --folha /tmp/linhas   # folhas para conferir no olho
    python3 tools/sprites/build_characters.py --write      # leva para o jogo

Grava em cada animação de golpe normal `silhueta.caminho`:
    {"cor": 0xRRGGBB, "pontos": {quadro: [x, y, jx, jy]}}
(x, y) é a ponta do membro (a mão, o pé) e (jx, jy) a junta de trás (o cotovelo, o joelho), em px
à frente da raiz e acima do chão, do desenho em que o membro sai ao desenho em que está esticado.
A cor é a do membro na ponta (a luva, o tênis): o rastro sai dessa cor.

Só golpes normais, e só onde a arte não traz um efeito desenhado (ARTE_COM_EFEITO, no
angulo_golpe.py). Especiais e Super têm os efeitos deles.

Precisa do mediapipe (`pip install mediapipe`; no Linux, as bibliotecas libegl1 e libgles2) e do
modelo `pose_landmarker_heavy.task`, que é baixado na primeira vez para ~/.cache/sokon (ou aponte
SOKON_POSE para o arquivo). Só esta ferramenta usa: o build e os testes leem o que ficou gravado.
"""
import argparse
import colorsys
import json
import math
import os
import sys
import urllib.request
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import angulo_golpe  # noqa: E402
import caixas_sprites as cx  # noqa: E402

ROOT = HERE.parents[1]
MODELO_URL = ('https://storage.googleapis.com/mediapipe-models/pose_landmarker/'
              'pose_landmarker_heavy/float16/latest/pose_landmarker_heavy.task')
# Os membros que batem: (ponta, junta de trás, raiz do membro) nos pontos do MediaPipe Pose.
MEMBROS = {'mao_e': (19, 13, 11), 'mao_d': (20, 14, 12), 'pe_e': (31, 25, 23), 'pe_d': (32, 26, 24)}
PAR = {'mao_e': 'mao_d', 'mao_d': 'mao_e', 'pe_e': 'pe_d', 'pe_d': 'pe_e'}
PARADO = 12.0        # px do mundo: menos que isto de um desenho para o outro, a ponta não saiu do lugar
VOLTA = 100.0        # graus: uma virada maior que esta no caminho é outro movimento (a preparação)
CAMINHO_MIN = 50.0   # px: caminho mais curto que isto não vira rastro
CAMINHO_PONTOS = 6   # no máximo, do fim para trás
ESTENDE = 110.0      # px: até onde a ponta de verdade pode estar além do ponto do modelo
NA_CAIXA = 30.0      # px: a ponta do membro que bate fica a no máximo isto da caixa do golpe
ANDOU = 45.0         # px: a ponta do membro que bate anda pelo menos isto da guarda ao impacto
# Golpes em que o caminho detectado sai errado (o modelo troca os membros, perde a pose na capa ou
# no cabelo). Conferido no olho nas folhas (--folha): ficam sem rastro.
# Golpes em que o modelo não acha o membro (a perna esticada some para ele): o caminho medido no
# desenho (a ponta mais à frente da perna e um ponto da canela atrás dela), {quadro: [x, y, jx, jy]}.
CAMINHO_MANUAL = {
    # p14 a p16 (11/11): o membro no acerto e na preparação, medidos com pose() para o smear.
    'p14': {
        'MEDIUM_KICK': {"8": [8, 81, 18, 175], "20": [182, 169, 82, 161]},
        'HEAVY_STRAIGHT': {"4": [-72, 9, -15, 83], "10": [241, 194, 127, 171]},
        'CROUCH_MEDIUM': {"12": [-18, 6, 38, 40], "14": [218, 40, 112, 64]},
        'CROUCH_HEAVY': {"6": [90, 18, 71, 97], "14": [101, 245, 59, 209]},
        'JUMP_MEDIUM': {"0": [8, 97, 54, 172], "2": [167, 248, 35, 207]},
        'JUMP_HEAVY': {"0": [162, 151, 68, 198], "2": [210, 204, 97, 182]},
        'JUMP_HEAVY_DOWN': {"3": [13, 89, 15, 162], "5": [63, 1, -2, 139]},
        'GOLPE_ALTO': {"6": [45, 18, 54, 89], "9": [159, 79, 75, 165]},
    },
    'p15': {
        'MEDIUM_KICK': {"4": [-13, 199, -45, 177], "6": [123, 224, 68, 213]},   # a mão de trás (o soco é duplo)
        'GOLPE_ALTO': {"6": [56, 73, 87, 154], "9": [232, 70, 119, 130]},
        'HEAVY_STRAIGHT': {"10": [-62, 19, -27, 96], "21": [225, 252, 106, 211]},
        'CROUCH_MEDIUM': {"33": [25, 40, 12, 95], "36": [249, 12, 140, 51]},
        'CROUCH_HEAVY': {"0": [51, 130, 58, 90], "5": [128, 295, 86, 253]},
        'JUMP_MEDIUM': {"6": [-3, 65, 9, 141], "10": [166, 219, 52, 168]},
        'JUMP_HEAVY': {"3": [16, 36, 39, 120], "5": [46, 65, 62, 115]},
        'JUMP_HEAVY_DOWN': {"4": [14, 28, 59, 112], "6": [51, 74, 62, 115]},
        'SPECIAL_S3': {"0": [81, 206, 45, 180], "4": [146, 217, 85, 210]},
    },
    'p16': {
        'JUMP_MEDIUM': {"3": [105, 128, 31, 164], "5": [40, 140, 26, 165]},
        'MEDIUM_KICK': {"5": [8, 158, 28, 154], "9": [150, 195, 99, 173]},
        'HEAVY_STRAIGHT': {"6": [35, 134, 66, 144], "9": [187, 173, 119, 162]},
        'SPECIAL_S2': {"6": [1, 146, -19, 138], "9": [117, 255, 57, 211]},
    },
    'player_base': {
        'JUMP_MEDIUM': {'5': [39, 30, 64, 105], '6': [159, 81, 99, 85], '8': [189, 118, 128, 92]},
        # Especiais (o detector só vê os normais): medidos com pose() quadro a quadro em 11/11. O
        # S3 tem seis chutes: cada par é a perna recolhida e a esticada.
        'SPECIAL_S2': {"4": [7, 180, 66, 105], "5": [49, 167, 75, 136], "6": [94, 215, 64, 209], "7": [83, 312, 51, 267], "8": [76, 323, 47, 274]},
        'SPECIAL_S3': {"8": [7, 75, 58, 144], "9": [191, 238, 87, 210], "13": [8, 65, 43, 146], "14": [164, 208, 86, 207], "15": [12, 97, 71, 167], "16": [194, 222, 81, 210], "19": [-52, 65, 18, 144], "20": [206, 238, 85, 212], "22": [21, 105, 62, 184], "23": [185, 243, 79, 209], "28": [7, 71, 39, 154], "29": [178, 240, 74, 211]},
    },
}
CAMINHO_ERRADO = {
    'p03': {'H', 'j2H'},
    'p04': {'jM', 'jH'},
    'p05': {'H', 'jL'},
    'p06': {'H', '2M'},
    'p07': {'2L', '2H'},
    'p08': {'L', 'H', '2M', '2H'},
    'p09': {'H', '2M'},
    'p10': {'M', 'H', 'jH', 'j2H'},
    'p11': {'2H'},
    'p12': {'2H'},
    'p13': {'M', 'j2H'},
    'p14': {'2M'},
    'p15': {'2H'},
    'p16': {'2L'},
}

AR = 0xCFE4FF        # a cor do ar: o rastro do punho de pele nua (branco puro apaga os riscos da fita)

_detector = None
_folhas = {}


def detector():
    global _detector
    if _detector is None:
        import mediapipe as mp
        from mediapipe.tasks import python as mpp
        from mediapipe.tasks.python import vision
        modelo = Path(os.environ.get('SOKON_POSE', Path.home() / '.cache/sokon/pose_landmarker_heavy.task'))
        if not modelo.is_file():
            modelo.parent.mkdir(parents=True, exist_ok=True)
            print('baixando o modelo de pose para', modelo, file=sys.stderr)
            urllib.request.urlretrieve(MODELO_URL, modelo)
        _detector = (mp, vision.PoseLandmarker.create_from_options(vision.PoseLandmarkerOptions(
            base_options=mpp.BaseOptions(model_asset_path=str(modelo)), num_poses=1,
            min_pose_detection_confidence=0.3, min_pose_presence_confidence=0.3)))
    return _detector


def celula(pack, a, q):
    """(RGBA do quadro q da folha, layout, px do mundo por px da folha)."""
    if a['atlas'] not in _folhas:
        clip = json.loads((HERE / 'clips' / f"{a['atlas']}.json").read_text(encoding='utf-8'))
        _folhas.clear()                         # uma folha de cada vez na memória
        _folhas[a['atlas']] = Image.open(ROOT / clip['source']).convert('RGBA')
    _, rep, k = cx.ler(pack['id'], a['atlas'])
    lay = rep['layout']
    fw, fh, cols = lay['frameWidth'], lay['frameHeight'], lay['columns']
    return _folhas[a['atlas']].crop(((q % cols) * fw, (q // cols) * fh, (q % cols + 1) * fw, (q // cols + 1) * fh)), lay, k


def pose(pack, a, q):
    """{membro: (ponta, junta, raiz)} e o meio do tronco, em px do mundo (x à frente da raiz, y
    acima do chão), do desenho q; None se o modelo não achou o corpo."""
    mp, det = detector()
    cel, lay, k = celula(pack, a, q)
    # Fundo cinza e folga em volta: o modelo acha melhor o corpo inteiro e solto na imagem.
    margem = max(cel.size) // 4
    lona = Image.new('RGBA', (cel.width + 2 * margem, cel.height + 2 * margem), (200, 200, 200, 255))
    lona.alpha_composite(cel, (margem, margem))
    r = det.detect(mp.Image(image_format=mp.ImageFormat.SRGB, data=np.ascontiguousarray(np.asarray(lona.convert('RGB')))))
    if not r.pose_landmarks:
        return None
    pontos = r.pose_landmarks[0]
    lado = -1 if pack.get('artFacing') == 'left' else 1

    def mundo(i):
        u, v = pontos[i].x * lona.width - margem, pontos[i].y * lona.height - margem
        return lado * (u - lay['rootX']) * k, (lay['rootY'] - v) * k

    corpo = {nome: tuple(mundo(i) for i in indices) for nome, indices in MEMBROS.items()}
    tronco = np.mean([mundo(i) for i in (11, 12, 23, 24)], axis=0)
    return corpo, (float(tronco[0]), float(tronco[1]))


def detectar(pack, nome):
    """[(quadro, (x, y), (jx, jy))] do membro que bate, do primeiro desenho do golpe ao desenho em
    que ele está mais esticado; [] se não deu para detectar.

    O membro que bate é o que o jogo já conhece: a ponta dele está na caixa do golpe do desenho
    (`silhueta.golpe`, do caixas_sprites.py). Entre os que estão, o que mais andou desde a guarda. O modelo às
    vezes troca esquerda e direita de um desenho para o outro: a ponta é seguida para trás pelo
    lado que fica mais perto de onde ela estava (a ponta e a junta juntas)."""
    a = pack['animations'][nome]
    if 'impactFrame' not in a:
        return []
    ordem = list(dict.fromkeys(a['frames']))
    i = ordem.index(a['frames'][a['impactFrame']])
    poses = {q: pose(pack, a, q) for q in ordem[:i + 3]}
    inicio = next((q for q in ordem[:i + 1] if poses[q]), None)
    if inicio is None:
        return []
    caixas = a.get('silhueta', {}).get('golpe', {})
    melhor = None
    for q in ordem[i:i + 3]:
        if not poses.get(q) or str(q) not in caixas:
            continue
        for m in MEMBROS:
            # Só o membro que o jogo sabe que bate: a ponta dele está na caixa do golpe do desenho.
            if fora_da_caixa(poses[q][0][m][0], caixas[str(q)]) > NA_CAIXA:
                continue
            # O quanto a ponta andou desde a guarda, contra o mesmo lado e o outro (a troca de
            # lado do modelo não inventa um movimento): o pé de apoio e a mão da guarda ficam.
            andou = min(math.dist(poses[q][0][m][0], poses[inicio][0][lado][0]) for lado in (m, PAR[m]))
            if melhor is None or andou > melhor[0]:
                melhor = (andou, q, m)
    if melhor is None or melhor[0] < ANDOU:
        return []
    _, fim, m = melhor
    ponta, junta, _ = poses[fim][0][m]
    caminho = [(fim, ate_a_borda(pack, a, fim, ponta, junta), junta)]
    for q in reversed(ordem[:ordem.index(fim)]):
        if not poses[q]:
            continue
        candidatos = [poses[q][0][m], poses[q][0][PAR[m]]]
        custo = lambda c: math.dist(c[0], ponta) + math.dist(c[1], junta)
        if custo(candidatos[1]) + 40.0 < custo(candidatos[0]):
            m = PAR[m]
        ponta, junta, _ = poses[q][0][m]
        caminho.append((q, ate_a_borda(pack, a, q, ponta, junta), junta))
    return caminho[::-1]


def ate_a_borda(pack, a, q, ponta, junta):
    """A ponta de verdade do membro: o ponto do modelo fica no punho ou no meio do pé; daqui segue
    na direção junta -> ponta até o desenho acabar (a ponta do tênis, os dedos, a garra)."""
    cel, lay, k = celula(pack, a, q)
    alfa = np.asarray(cel.getchannel('A'))
    lado = -1 if pack.get('artFacing') == 'left' else 1
    vx, vy = ponta[0] - junta[0], ponta[1] - junta[1]
    n = math.hypot(vx, vy)
    if n < 1:
        return ponta
    vx, vy = vx / n, vy / n
    melhor, vazio = 0.0, 0.0
    for passo in np.arange(0.0, ESTENDE, 2.0):
        u = int(round(lay['rootX'] + lado * (ponta[0] + vx * passo) / k))
        v = int(round(lay['rootY'] - (ponta[1] + vy * passo) / k))
        if 0 <= v < alfa.shape[0] and 0 <= u < alfa.shape[1] and alfa[v, u] >= 128:
            melhor, vazio = passo, 0.0
        else:
            vazio += 2.0
            if vazio > 10.0:
                break
    return ponta[0] + vx * melhor, ponta[1] + vy * melhor


def fora_da_caixa(ponto, caixas):
    """A distância (px) do ponto à caixa do golpe mais perto ([x0, x1, y0, y1], uma ou duas); 0 dentro."""
    x, y = ponto
    return min(math.hypot(max(c[0] - x, 0, x - c[1]), max(c[2] - y, 0, y - c[3]))
               for c in (caixas[j:j + 4] for j in range(0, len(caixas), 4)))


def linha_de_acao(pontos):
    """O trecho final do caminho, que é o golpe: da saída até o membro esticado.

    Do fim para trás, até a primeira virada brusca (a preparação: o braço que arma antes do soco)
    ou até a ponta parar (a guarda). Pontos quase no mesmo lugar contam como um só."""
    if not pontos:
        return []
    limpo = [pontos[-1]]
    for p in reversed(pontos[:-1]):
        (x, y), (bx, by) = p[1], limpo[-1][1]
        if math.hypot(x - bx, y - by) < PARADO:
            if len(limpo) > 1:
                break                           # parou: é de onde o golpe saiu
            continue
        if len(limpo) > 1:
            cx_, cy_ = limpo[-2][1]
            antes, depois = math.atan2(by - y, bx - x), math.atan2(cy_ - by, cx_ - bx)
            if abs((math.degrees(depois - antes) + 180) % 360 - 180) > VOLTA:
                break
        limpo.append(p)
        if len(limpo) == CAMINHO_PONTOS:
            break
    limpo.reverse()
    comprimento = sum(math.dist(a[1], b[1]) for a, b in zip(limpo, limpo[1:]))
    return limpo if len(limpo) >= 2 and comprimento >= CAMINHO_MIN else []


def cor_da_ponta(pack, a, q, x, y):
    """A cor do membro na ponta (0xRRGGBB): a luva, a faixa, o tênis. O rastro sai dessa cor.

    A cor mais comum nos 30 px em volta da ponta, sem o contorno escuro; clareada para ler como
    ar (o sapato preto dá um rastro cinza). A pele nua não tinge: fica a cor do ar."""
    cel, lay, k = celula(pack, a, q)
    lado = -1 if pack.get('artFacing') == 'left' else 1
    u, v, r = lay['rootX'] + lado * x / k, lay['rootY'] - y / k, 30.0 / k
    dados = np.asarray(cel)
    yy, xx = np.mgrid[0:dados.shape[0], 0:dados.shape[1]]
    perto = ((xx - u) ** 2 + (yy - v) ** 2 <= r * r) & (dados[..., 3] >= 200)
    if perto.sum() < 20:
        return AR
    rgb = dados[perto][:, :3].astype(float) / 255.0
    claros = rgb[rgb.max(axis=1) >= 0.25]
    if len(claros) >= 0.3 * len(rgb):
        rgb = claros
    caixas = (rgb * 5.999).astype(int)
    chave = caixas[:, 0] * 36 + caixas[:, 1] * 6 + caixas[:, 2]
    cor = rgb[chave == np.bincount(chave).argmax()].mean(axis=0)
    hh, ss, vv = colorsys.rgb_to_hsv(*cor)
    if 0.02 <= hh <= 0.12 and 0.2 <= ss <= 0.65 and vv >= 0.5:
        return AR   # pele
    # O escuro clareado não tem cor de verdade (o matiz do preto é ruído): vira cinza.
    cor = colorsys.hsv_to_rgb(hh, min(ss, 0.8) * min(1.0, vv / 0.35), max(vv, 0.78))
    return (int(round(cor[0] * 255)) << 16) | (int(round(cor[1] * 255)) << 8) | int(round(cor[2] * 255))


def golpes(pack):
    """[(golpe, animação)] dos golpes normais que podem ter rastro."""
    fora = angulo_golpe.ARTE_COM_EFEITO.get(pack['id'], set()) | CAMINHO_ERRADO.get(pack['id'], set())
    vistos, saida = set(), []
    for golpe, m in pack['moves'].items():
        nome = m.get('animation')
        if nome and nome not in vistos and golpe not in fora:
            vistos.add(nome)
            saida.append((golpe, nome))
    return saida


def caminhos(pack):
    """{animação: {"cor": cor, "pontos": {quadro: [x, y, jx, jy]}}} dos golpes com linha de ação."""
    saida = {}
    for _, nome in golpes(pack):
        manual = CAMINHO_MANUAL.get(pack['id'], {}).get(nome)
        if manual:
            a = pack['animations'][nome]
            q = max(manual, key=int)
            saida[nome] = {'cor': cor_da_ponta(pack, a, int(q), *manual[q][:2]), 'pontos': manual}
            continue
        linha = linha_de_acao(detectar(pack, nome))
        if not linha:
            continue
        a = pack['animations'][nome]
        q, (x, y), _ = linha[-1]
        saida[nome] = {'cor': cor_da_ponta(pack, a, q, x, y),
                       'pontos': {str(q): [int(round(v)) for v in (*ponta, *junta)] for q, ponta, junta in linha}}
    return saida


def manuais(pack, saida):
    """Os caminhos de CAMINHO_MANUAL das animações que não são golpe normal (os especiais)."""
    for nome, pontos in CAMINHO_MANUAL.get(pack['id'], {}).items():
        if nome not in saida and nome in pack['animations']:
            q = max(pontos, key=int)
            saida[nome] = {'cor': cor_da_ponta(pack, pack['animations'][nome], int(q), *pontos[q][:2]), 'pontos': pontos}
    return saida


def gravar(pack, medidos):
    for nome, a in pack['animations'].items():
        sil = a.get('silhueta')
        if sil is None:
            continue
        if nome in medidos:
            sil['caminho'] = medidos[nome]
        else:
            sil.pop('caminho', None)


def folha(prefixo, tudo):
    """Folhas para conferir no olho: o desenho esticado (e o da saída, apagado) com a linha de ação."""
    quadrados = []
    for pack, medidos in tudo:
        for golpe, nome in golpes(pack):
            a = pack['animations'][nome]
            cam = medidos.get(nome)
            ordem = list(dict.fromkeys(a['frames']))
            q = int(list(cam['pontos'])[-1]) if cam else a['frames'][a.get('impactFrame', 0)]
            cel, lay, k = celula(pack, a, q)
            fundo = Image.new('RGBA', cel.size, (70, 60, 110, 255))
            if cam:
                saida, _, _ = celula(pack, a, int(list(cam['pontos'])[0]))
                saida.putalpha(saida.getchannel('A').point(lambda v: v // 3))
                fundo.alpha_composite(saida)
            fundo.alpha_composite(cel)
            fundo = fundo.convert('RGB')
            if cam:
                lado = -1 if pack.get('artFacing') == 'left' else 1
                ponto = lambda x, y: (lay['rootX'] + lado * x / k, lay['rootY'] - y / k)
                d = ImageDraw.Draw(fundo)
                pontas = [ponto(*v[:2]) for v in cam['pontos'].values()]
                juntas = [ponto(*v[2:]) for v in cam['pontos'].values()]
                d.line(juntas, fill=(0, 160, 255), width=max(2, int(3 / k)))
                d.line(pontas, fill=(0, 255, 0), width=max(2, int(4 / k)))
                for n, p in enumerate(pontas):
                    r = 5 / k
                    d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=(255, 0, 0) if n == len(pontas) - 1 else (255, 255, 0))
            fundo.thumbnail((260, 200))
            quadrado = Image.new('RGB', (260, 216), (0, 0, 0))
            quadrado.paste(fundo, (0, 16))
            texto = f"{pack['id']} {golpe} " + (f"{list(cam['pontos'])} {cam['cor']:06X}" if cam else 'SEM')
            ImageDraw.Draw(quadrado).text((3, 2), texto, fill=(255, 255, 255) if cam else (255, 120, 120))
            quadrados.append(quadrado)
    colunas, por_folha = 7, 42
    for n in range(0, len(quadrados), por_folha):
        parte = quadrados[n:n + por_folha]
        o = Image.new('RGB', (260 * colunas, 216 * ((len(parte) + colunas - 1) // colunas)))
        for i, t in enumerate(parte):
            o.paste(t, ((i % colunas) * 260, (i // colunas) * 216))
        o.save(f'{prefixo}_{n // por_folha}.png')
        print(f'{prefixo}_{n // por_folha}.png')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='*')
    p.add_argument('--write', action='store_true')
    p.add_argument('--folha')
    a = p.parse_args()
    tudo = []
    for path in sorted((ROOT / 'characters').glob('*/character.json')):
        pack = json.loads(path.read_text(encoding='utf-8'))
        if pack['id'] in cx.FORA or (a.personagens and pack['id'] not in a.personagens):
            continue
        medidos = manuais(pack, caminhos(pack))
        tudo.append((pack, medidos))
        nomes = dict((nome, golpe) for golpe, nome in golpes(pack))
        print(pack['id'], ' '.join(f"{nomes[n]}:{len(c['pontos'])}" for n, c in medidos.items()),
              '| sem:', ' '.join(g for g, n in golpes(pack) if n not in medidos))
        if a.write:
            gravar(pack, medidos)
            path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    if a.folha:
        folha(a.folha, tudo)
    if a.write:
        print('agora: python3 tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
