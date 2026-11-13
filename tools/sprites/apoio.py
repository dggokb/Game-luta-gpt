#!/usr/bin/env python3
"""Pé de apoio de cada quadro, medido no desenho (as folhas do jogo): o atrito entre o pé e o chão.

Em cada quadro de uma animação, acha os pés (o que a silhueta tem de mais baixo, perto da linha
do chão), segue cada um de um quadro para o seguinte e decide qual está apoiado. O pé apoiado
fica cravado no mundo: o quanto ele anda para trás no desenho, de um quadro para o outro, é o
quanto o corpo anda para a frente no jogo (o "avanço" do quadro).

Regras (valem para todo quadro, sem número por personagem; as medidas são frações da altura
em pé do lutador):
    pé          trecho de colunas com desenho na faixa de FAIXA × altura acima do chão, com
                perna em cima (o que está largado no chão não é pé). Cada pé é seguido inteiro e
                em duas metades (calcanhar e ponta, ou dois pés juntos num trecho só), pelo
                desenho dele, do quadro para o seguinte
    sentido     na locomoção (andar para a frente e para trás), para que lado o corpo anda no
                desenho: cada pé fica mais tempo apoiado (andando devagar para trás em relação ao
                corpo) do que no ar, então o lado para onde os pés andam na maior parte dos
                quadros é o contrário do lado do corpo
    de apoio    do que toca o chão, o que mais anda contra o sentido do corpo (o pé que dá o
                passo vai a favor; o que o corpo carrega anda com ele), no caminho em que a
                velocidade do corpo menos muda de um quadro para o outro (o corpo tem peso e não
                dá tranco). Casar mal, subir, descer e estar acima do chão pesam contra a parte
    sem apoio   nem o pé mais baixo chega perto do chão (NO_AR × altura): o corpo segue como
                vinha (o avanço dos quadros medidos em volta)

Saída, por animação (character.json, "apoio"):
    folha       a marca da folha e dos quadros medidos (se a animação mudar, a medida não vale)
    avanco      px do mundo que o corpo anda, para a frente (negativo: para trás), do quadro i
                para o seguinte (o último, para o primeiro, nas que dão a volta)
    semApoio    quadros sem pé no chão
    duvida      quadros em que o pé de apoio casou mal com o quadro seguinte (medida fraca)
    aoContrario a locomoção é desenhada para o lado contrário (andar para trás com o passo de
                quem anda para a frente): o motor não usa
    inseguro    por que o motor não deve usar a medida: o desenho quase não anda (marca passo), o
                corpo anda para o lado errado em quadros demais, quadros demais em dúvida, ou o
                apoio "volta para trás" (o apoio seguinte tem de pisar à frente do anterior)

Quem mede é o build (build_characters.py --write): animação de andar nova ou trocada sai medida
sozinha, e com --check a medida de uma animação que mudou é erro. O motor usa o avanço do andar
medido com segurança (CombatEngine.andarPeloPe): a passada corre no tempo, no ritmo que dá a
velocidade de andar do lutador, e o corpo anda o que o pé de apoio empurra. Sem medida segura,
fica o andar de velocidade fixa. A corrida não é medida (o pé fica dois quadros no chão por
passada e anda mais do que dá para seguir); as animações no ar saem com medida falsa, porque a
folha encosta o pé na linha do chão (quem sabe que é ar é o motor).

    python3 tools/sprites/apoio.py p02                    # mostra a medida de cada animação
    python3 tools/sprites/apoio.py p02 WALK_FORWARD -v    # quadro a quadro
    python3 tools/sprites/apoio.py todos                  # o andar do elenco
    python3 tools/sprites/apoio.py p02 --write            # grava a do andar (o build também grava)
"""
import argparse
import hashlib
import json
from pathlib import Path

try:   # a medida precisa de OpenCV e numpy; conferir a marca de uma medida já gravada, não
    import cv2
    import numpy as np
except ImportError:
    cv2 = np = None
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
OPACO = 160        # alfa mínimo de um pixel que conta (0-255), como no caixas_sprites.py
FAIXA = 0.20       # da altura em pé: a faixa acima do chão em que se procuram os pés
BUSCA = (0.16, 0.03)  # da altura em pé: até onde se procura o mesmo pé no quadro seguinte (x, y)
MOLDE = 0.09       # da altura em pé: a altura do desenho do pé que se segue (a sola e o peito do pé)
MEIO_PE = 0.06     # da altura em pé: trecho mais largo que isto também é seguido em duas metades
PERNA = (0.25, 0.36, 0.05)  # pé tem perna em cima: esta fração das colunas dele com desenho
                            # entre a faixa dos pés e esta altura (× altura em pé), com esta folga para os lados
MIUDO = 0.02       # da altura em pé: trecho mais estreito que isto não é pé
RUIM = 0.55        # casamento abaixo disto (0-1): não é o mesmo pé
DUVIDA = 0.80      # apoio escolhido com casamento abaixo disto: o quadro fica marcado em dúvida
NO_AR = 0.04       # da altura em pé: o pé mais baixo acima disto, ninguém está no chão
PESO_SUBIDA = 0.5  # px por px: quanto subir ou descer pesa contra a parte na escolha do apoio
PESO_NOTA = 6.0    # px do mundo: quanto um casamento ruim pesa contra a parte na escolha do apoio
PESO_ALTURA = 0.6  # px por px: quanto pesa a parte estar acima da linha do chão
PASSO_MINIMO = 0.15  # da altura em pé: volta que anda menos que isto, o desenho marca passo
CONTRA_MAXIMO = 0.15 # dos quadros: mais que isto andando contra o sentido, a medida não é segura
FRACOS_MAXIMO = 0.5  # dos quadros: mais que isto em dúvida ou sem apoio, a medida não é segura
JUNTOS = 2.5         # px do mundo por quadro: pés que andam com esta diferença estão os dois apoiados
RECUO = 0.3          # da altura em pé: o apoio seguinte pisar mais que isto atrás do anterior não é passo
PARADO = 0.5       # px do mundo por quadro: pé que anda menos que isto não conta no sentido do desenho
CLARO = 1.5        # o desenho anda ao contrário da animação se os pés dizem isso por esta margem
PESO_APOIO = 1.0   # px por px: na locomoção, quanto pesa a parte andar menos para trás que a que mais anda
# O sentido em que o corpo anda nas animações de locomoção. Nelas o apoio é, do que toca o chão, o
# que mais anda para trás no desenho: o pé que dá o passo vai para a frente, e o que o corpo
# carrega (o chicote no chão, a barra da saia) anda junto com ele, parado no desenho.
SENTIDO = {'WALK_FORWARD': 1, 'DASH': 1, 'WALK_BACK': -1, 'BACKDASH': -1}
_folhas = {}


def usar_raiz(raiz):
    """Lê as folhas, os clipes e os relatórios de outra raiz (o build monta tudo numa pasta à
    parte antes de trocar os arquivos do repositório)."""
    global ROOT
    ROOT = Path(raiz)
    _folhas.clear()


def ler(atlas):
    """(RGBA da folha, layout, escala px da folha -> px do mundo)."""
    if atlas not in _folhas:
        clip = json.loads((ROOT / 'tools/sprites/clips' / f'{atlas}.json').read_text(encoding='utf-8'))
        rep = json.loads((ROOT / 'tools/sprites/reports' / f'{atlas}.report.json').read_text(encoding='utf-8'))
        im = np.array(Image.open(ROOT / clip['source']).convert('RGBA'))
        _folhas[atlas] = (im, rep['layout'], rep['worldScale'] / rep.get('pixelScale', 1))
    return _folhas[atlas]


def celula(atlas, i):
    im, lay, _ = ler(atlas)
    fw, fh, cols = lay['frameWidth'], lay['frameHeight'], lay['columns']
    r, c = divmod(i, cols)
    return im[r * fh:(r + 1) * fh, c * fw:(c + 1) * fw]


def tem_perna(atlas, i, pe, faixa, altura):
    """O trecho tem perna em cima? Fração das colunas dele (com uma folga para a perna na
    diagonal) que têm desenho logo acima da faixa dos pés. O que está largado no chão ao lado do
    corpo (o chicote enrolado, uma pedra) não tem."""
    _, lay, _ = ler(atlas)
    m = celula(atlas, i)[..., 3] >= OPACO
    chao = lay['rootY']
    y0, y1 = max(0, chao - round(PERNA[1] * altura)), max(1, chao - faixa)
    folga = round(PERNA[2] * altura)
    x0, x1 = max(0, pe['x0'] - folga), min(m.shape[1], pe['x1'] + folga + 1)
    cheio = m[y0:y1, x0:x1].any(0)
    return float(cheio.sum()) / max(1, pe['x1'] - pe['x0'] + 1)


def pes(atlas, i, faixa, miudo=4):
    """Os pés do quadro: [{x0, x1, base, perfil}] em px da célula. base = a linha mais baixa do
    pé; perfil = a linha mais baixa de cada coluna dele."""
    _, lay, _ = ler(atlas)
    chao = lay['rootY']
    m = celula(atlas, i)[..., 3] >= OPACO
    y0 = max(0, chao - faixa)
    banda = m[y0:chao + 3]
    tem = banda.any(0)
    baixo = np.where(tem, banda.shape[0] - 1 - np.argmax(banda[::-1], axis=0), -1) + y0
    achados, x, largura = [], 0, m.shape[1]
    while x < largura:
        if not tem[x]:
            x += 1
            continue
        x0 = x
        while x < largura and tem[x]:
            x += 1
        achados.append({'x0': x0, 'x1': x - 1, 'base': int(baixo[x0:x].max()), 'perfil': baixo[x0:x].copy()})
    return [p for p in achados if p['x1'] - p['x0'] >= miudo]


def casar(atlas, i, j, pe, alto, busca):
    """Onde o pé do quadro i está no quadro j: (dx, dy, nota 0-1), pelo desenho do pé (a faixa
    de baixo dele), com fração de pixel em x."""
    _, lay, _ = ler(atlas)
    a, b = celula(atlas, i), celula(atlas, j)
    y0, y1 = max(0, pe['base'] - alto), pe['base'] + 2
    x0, x1 = max(0, pe['x0'] - 1), min(a.shape[1], pe['x1'] + 2)
    molde = a[y0:y1, x0:x1].astype(np.float32)
    molde = molde[..., :3].mean(-1) * (molde[..., 3] / 255) + (255 - molde[..., 3])  # fundo claro
    bx, by = busca
    sx0, sy0 = max(0, x0 - bx), max(0, y0 - by)
    sx1, sy1 = min(b.shape[1], x1 + bx), min(b.shape[0], y1 + by)
    area = b[sy0:sy1, sx0:sx1].astype(np.float32)
    area = area[..., :3].mean(-1) * (area[..., 3] / 255) + (255 - area[..., 3])
    if area.shape[0] < molde.shape[0] or area.shape[1] < molde.shape[1] or molde.std() < 1:
        return 0.0, 0.0, 0.0
    r = cv2.matchTemplate(area, molde, cv2.TM_CCOEFF_NORMED)
    _, nota, _, (px, py) = cv2.minMaxLoc(r)
    fino = 0.0
    if 0 < px < r.shape[1] - 1:   # parábola pelos três pontos em volta do pico
        e, c, d = r[py, px - 1], r[py, px], r[py, px + 1]
        if e + d - 2 * c < -1e-6:
            fino = 0.5 * (e - d) / (e + d - 2 * c)
    return px + fino + sx0 - x0, py + sy0 - y0, float(nota)


def partes(pe, meio_pe=12):
    """O pé inteiro e as duas metades dele (calcanhar e ponta, ou dois pés juntos num trecho
    só): cada parte é seguida à parte, e a que está apoiada é a que anda como o corpo pede."""
    x0, x1, perfil = pe['x0'], pe['x1'], pe['perfil']
    lista = [{'x0': x0, 'x1': x1, 'base': pe['base'], 'parte': 'inteiro'}]
    meio = (x0 + x1) // 2
    if x1 - x0 >= meio_pe:
        # cada metade com a própria base: num trecho com dois pés, um pode estar no ar
        lista.append({'x0': x0, 'x1': meio, 'base': int(perfil[:meio - x0 + 1].max()), 'parte': 'trás'})
        lista.append({'x0': meio + 1, 'x1': x1, 'base': int(perfil[meio - x0 + 1:].max()), 'parte': 'frente'})
    return lista


def caminho(candidatos, volta):
    """Em cada quadro, a parte de pé que apoia: o caminho em que a velocidade do corpo menos
    muda de um quadro para o outro (o corpo tem peso e não dá tranco; o pé que dá o passo, sim).
    candidatos[k] = [(dx, custo)], vazio nos quadros sem pé no chão. Devolve o índice escolhido
    por quadro (None nos vazios)."""
    com = [k for k, c in enumerate(candidatos) if c]
    if not com:
        return [None] * len(candidatos)

    def melhor(fixo):
        # fixo: o candidato do primeiro quadro (a volta fecha nele), ou None (animação que não dá a volta)
        custo = [[c[1] for c in candidatos[k]] for k in com]
        de = [[0] * len(candidatos[k]) for k in com]
        if fixo is not None:
            custo[0] = [v if i == fixo else 1e9 for i, v in enumerate(custo[0])]
        for n in range(1, len(com)):
            antes, agora = candidatos[com[n - 1]], candidatos[com[n]]
            for i, (dx, _) in enumerate(agora):
                passo = [custo[n - 1][j] + abs(dx - antes[j][0]) for j in range(len(antes))]
                j = int(np.argmin(passo))
                custo[n][i] += passo[j]
                de[n][i] = j
        fim = list(custo[-1])
        if fixo is not None:   # fecha a volta: do último para o primeiro
            fim = [v + abs(candidatos[com[-1]][i][0] - candidatos[com[0]][fixo][0]) for i, v in enumerate(fim)]
        i = int(np.argmin(fim))
        total, escolha = fim[i], [0] * len(com)
        for n in range(len(com) - 1, -1, -1):
            escolha[n] = i
            i = de[n][i]
        return total, escolha

    if volta:
        total, escolha = min((melhor(f) for f in range(len(candidatos[com[0]]))), key=lambda t: t[0])
    else:
        total, escolha = melhor(None)
    saida = [None] * len(candidatos)
    for n, k in enumerate(com):
        saida[k] = escolha[n]
    return saida


def candidatos_de(atlas, quadros, volta, altura, escala):
    """Para cada passagem de quadro, as partes de pé seguidas até o quadro seguinte:
    [(dx, dy, altura acima do chão, nota, parte)] em px do mundo; None no quadro sem ninguém no
    chão; [] se nada casou."""
    _, lay, _ = ler(atlas)
    faixa = round(FAIXA * altura)
    alto, busca = max(6, round(MOLDE * altura)), (round(BUSCA[0] * altura), max(3, round(BUSCA[1] * altura)))
    n = len(quadros)
    achados = [pes(atlas, q, faixa, max(3, round(MIUDO * altura))) for q in quadros]
    # O chão da animação: o mais baixo que um pé chega nela (a corrida é desenhada um pouco
    # acima da linha do chão da célula; no andar é a própria linha).
    bases = [max(p['base'] for p in f) for f in achados if f]
    chao = min(lay['rootY'], max(bases)) if bases else lay['rootY']
    linhas = []
    for k in range(n if volta else n - 1):
        i, j = quadros[k], quadros[(k + 1) % n]
        if i == j:   # o mesmo desenho segurado: o corpo não anda
            linhas.append([(0.0, 0.0, 0.0, 1.0, {'x0': 0, 'x1': 0, 'base': chao, 'parte': 'parado'})])
            continue
        # No ar: nem o pé mais baixo chega perto da linha do chão.
        if not achados[k] or max(p['base'] for p in achados[k]) < chao - NO_AR * altura:
            linhas.append(None)
            continue
        linha = []
        for p in achados[k]:
            if tem_perna(atlas, i, p, faixa, altura) < PERNA[0]:
                continue   # largado no chão, sem perna em cima: não é pé
            for parte in partes(p, round(MEIO_PE * altura)):
                dx, dy, nota = casar(atlas, i, j, parte, alto, busca)
                if nota >= RUIM:
                    linha.append((dx * escala, dy * escala, max(0, chao - parte['base']) * escala, nota, parte))
        linhas.append(linha)
    return linhas


def sentido_do_desenho(linhas):
    """Para que lado o corpo anda no desenho. No passo, cada pé fica mais tempo apoiado (andando
    devagar para trás em relação ao corpo) do que no ar (voltando depressa para a frente): o
    lado para onde os pés andam na maior parte dos quadros é o contrário do lado do corpo.
    Devolve (quadros-pé que levam o corpo para a frente, para trás)."""
    frente = tras = 0
    for linha in linhas:
        for dx, _, _, nota, parte in linha or []:
            if nota >= DUVIDA and parte['parte'] == 'inteiro' and abs(dx) >= PARADO:
                if dx < 0:
                    frente += 1
                else:
                    tras += 1
    return frente, tras


def medir(pack, nome, verbose=False, detalhes=None, sentido=None):
    """A medida do pé de apoio de uma animação: {'avanco', 'semApoio', 'duvida'} e, na locomoção
    desenhada para o lado contrário ao que ela anda, 'aoContrario'. detalhes: uma lista que
    recebe a parte de pé escolhida em cada quadro ({x0, x1, base} na célula, ou None). sentido:
    força o lado para que o corpo anda no desenho (1, -1; 0 sem lado), no lugar do da animação."""
    anim = pack['animations'][nome]
    atlas = anim['atlas']
    _, lay, escala = ler(atlas)
    quadros = anim['frames']
    volta = bool(anim.get('loop'))
    altura_mundo = pack['fighter']['body']['standHeight']
    linhas = candidatos_de(atlas, quadros, volta, altura_mundo / escala, escala)
    pares = len(linhas)
    esperado = SENTIDO.get(nome, 0)
    contrario = False
    if sentido is None:
        sentido = esperado
        if esperado:
            # O desenho anda para o lado da animação? (Andar para trás desenhado com o passo
            # do andar para a frente: o pé de apoio leva o corpo para o lado errado.)
            frente, tras = sentido_do_desenho(linhas)
            contra, favor = (tras, frente) if esperado > 0 else (frente, tras)
            if contra >= CLARO * max(1, favor):
                sentido, contrario = -esperado, True
    candidatos = []
    for linha in linhas:
        if linha is None:
            candidatos.append([])
            continue
        # Pesa contra a parte: casar mal; subir ou descer (está saindo do chão; sem proibir: o
        # pé de apoio também sobe na ponta no fim do passo); estar acima da linha do chão; e, na
        # locomoção, andar menos para trás que a parte que mais anda.
        custos = [PESO_NOTA * (1 - nota) + PESO_SUBIDA * abs(dy) + PESO_ALTURA * alt for dx, dy, alt, nota, _ in linha]
        if sentido and linha:
            firmes = [c for c in linha if c[3] >= DUVIDA] or linha
            frente = max(-sentido * c[0] for c in firmes)
            custos = [c + PESO_APOIO * max(0.0, frente + sentido * l[0]) for c, l in zip(custos, linha)]
        candidatos.append([(l[0], c) for l, c in zip(linha, custos)])
    escolha = caminho(candidatos, volta)
    avanco, sem, duvida = [None] * pares, [], []
    for k in range(pares):
        if escolha[k] is None:
            sem.append(k)
            if detalhes is not None:
                detalhes.append(None)
            continue
        dx, _, _, nota, parte = linhas[k][escolha[k]]
        if detalhes is not None:
            detalhes.append(parte)
        avanco[k] = round(float(-dx), 2)
        if nota < DUVIDA:
            duvida.append(k)
    if verbose:
        for k in range(pares):
            marca = 'sem apoio' if escolha[k] is None else f'corpo {avanco[k]:+6.2f}'
            desc = ' '.join(f"{'*' if escolha[k] == c else ' '}[{p['x0'] - lay['rootX']:+4d}..{p['x1'] - lay['rootX']:+4d} "
                            f"{p['parte'][:3]} dx {dx:+5.1f} alt {alt:2.0f} {nota:.2f}]"
                            for c, (dx, _, alt, nota, p) in enumerate(linhas[k] or []))
            print(f'  {k:2d} {marca:13} {desc}')
    if sentido and volta and sem and len(sem) < pares:
        # Sem pé no chão o corpo segue como vinha (inércia): o avanço dos quadros medidos em volta.
        medidos = [k for k in range(pares) if avanco[k] is not None]
        for k in sem:
            perto = sorted(medidos, key=lambda m: min(abs(m - k), pares - abs(m - k)))[:2]
            avanco[k] = round(float(np.mean([avanco[m] for m in perto])), 2)
    medida = {'folha': marca_da_folha(anim), 'avanco': avanco, 'semApoio': sem, 'duvida': duvida}
    if contrario:
        medida['aoContrario'] = True
    if esperado:
        motivo = inseguro(medida, linhas, escolha, sentido, altura_mundo, lay['rootX'], escala, volta)
        if motivo:
            medida['inseguro'] = motivo
    return medida


def inseguro(medida, linhas, escolha, sentido, altura, raiz, escala, volta):
    """Por que a medida de uma locomoção não é segura para o motor usar (ou None): o passo é
    curto demais (o desenho marca passo), o corpo anda contra o sentido em quadros demais, há
    quadros demais em dúvida ou sem apoio, ou o apoio "volta para trás" (o apoio seguinte tem de
    pisar à frente do anterior; se pisa bem atrás, o que foi seguido não era o pé)."""
    avanco = medida['avanco']
    pares = len(avanco)
    if any(v is None for v in avanco):
        return 'quadros sem medida'
    passo = sum(avanco)
    if passo * sentido < PASSO_MINIMO * altura:
        return f'o desenho quase não anda ({passo:+.0f} px por volta)'
    contra = sum(1 for v in avanco if v * sentido < -PARADO)
    if contra > CONTRA_MAXIMO * pares:
        return f'o corpo anda para o lado errado em {contra} de {pares} quadros'
    fracos = len(set(medida['duvida']) | set(medida['semApoio']))
    if fracos > FRACOS_MAXIMO * pares:
        return f'{fracos} de {pares} quadros em dúvida ou sem apoio'
    # Onde cada apoio está no mundo (o corpo no avanço acumulado + o lugar da parte no desenho).
    acum, antes, anterior, recuos = 0.0, None, None, []
    for k in range(pares):
        if escolha[k] is not None and linhas[k][escolha[k]][4]['parte'] != 'parado':
            parte = linhas[k][escolha[k]][4]
            centro = (parte['x0'] + parte['x1']) / 2
            mundo = acum + (centro - raiz) * escala
            if antes is not None and (mundo - antes) * sentido < -RECUO * altura:
                # Os dois pés no chão andando juntos: trocar de um para o outro não é recuo. Só
                # é se, no quadro anterior, o que está aqui não andava como o apoio de então.
                dx_antes = linhas[anterior][escolha[anterior]][0]
                junto = any(abs((c[4]['x0'] + c[4]['x1']) / 2 + c[0] / escala - centro) <= (parte['x1'] - parte['x0']) / 2 + 3
                            and abs(c[0] - dx_antes) <= JUNTOS for c in linhas[anterior])
                if not junto:
                    recuos.append(k)
            antes, anterior = mundo, k
        acum += avanco[k]
    if recuos:
        return 'o apoio volta para trás nos quadros ' + ', '.join(map(str, recuos))
    return None


def marca_da_folha(anim, root=None):
    """A marca da folha e dos quadros que a medida olhou: se a animação mudar (outra folha,
    outros quadros), a medida gravada não vale mais e o build recusa (build_characters.py)."""
    root = root or ROOT
    clip = json.loads((root / 'tools/sprites/clips' / f"{anim['atlas']}.json").read_text(encoding='utf-8'))
    h = hashlib.sha1((root / clip['source']).read_bytes())
    h.update(json.dumps([anim['frames'], bool(anim.get('loop'))]).encode())
    return h.hexdigest()[:12]


LOCOMOCAO = ('WALK_FORWARD', 'WALK_BACK')   # as animações que o motor anda pelo pé de apoio


def atualizar(pack, nomes=LOCOMOCAO, avisar=print):
    """Mede o pé de apoio das animações de locomoção do pacote que ainda não têm medida ou
    mudaram depois dela (outra folha, outros quadros), e grava em animations.<X>.apoio. É o build
    (build_characters.py) que chama: animação nova ou trocada sai medida sozinha. Devolve True
    se alguma medida mudou."""
    mudou = False
    for nome in nomes:
        anim = pack['animations'].get(nome)
        if not anim:
            continue
        try:
            if anim.get('apoio', {}).get('folha') == marca_da_folha(anim):
                continue
        except (OSError, KeyError, ValueError):
            continue   # sem clipe ou sem folha: o build acusa isso por conta própria
        if cv2 is None:
            if 'apoio' in anim:
                raise ValueError(f"{pack.get('id', '?')}.{nome}.apoio: a animação mudou depois da medida do pé de "
                                 'apoio, e medir de novo precisa de OpenCV e numpy (pip install opencv-python-headless numpy)')
            continue   # sem medida e sem como medir aqui: fica o andar de velocidade fixa
        try:
            medida = medir(pack, nome)
        except (OSError, KeyError, ValueError, IndexError, cv2.error) as erro:
            # Sem folha ou sem medida possível: fica sem apoio (o andar de velocidade fixa).
            if anim.pop('apoio', None) is not None:
                mudou = True
            if avisar:
                avisar(f"{pack.get('id', '?'):12} {nome:18} pé de apoio sem medida ({type(erro).__name__})")
            continue
        anim['apoio'] = medida
        mudou = True
        if avisar:
            avisar(f"{pack.get('id', '?'):12} " + resumo(pack, nome, medida))
    return mudou


def resumo(pack, nome, m):
    anim = pack['animations'][nome]
    medidos = [v for v in m['avanco'] if v is not None]
    total = sum(medidos)
    n = len(m['avanco'])
    return (f"{nome:18} {len(anim['frames']):3d} quadros  apoio em {len(medidos):3d}/{n:<3d} "
            f"sem apoio {len(m['semApoio']):2d}  dúvida {len(m['duvida']):2d}  corpo anda {total:+7.1f} px"
            + (' por volta' if anim.get('loop') else '')
            + (' | DESENHO AO CONTRÁRIO' if m.get('aoContrario') else '')
            + (' | INSEGURO: ' + m['inseguro'] if m.get('inseguro') else ''))


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('personagem', help='o personagem, ou "todos"')
    ap.add_argument('animacoes', nargs='*')
    ap.add_argument('--write', action='store_true',
                    help='grava "apoio" nas animações pedidas (sem nomes: as de locomoção)')
    ap.add_argument('-v', action='store_true', help='quadro a quadro')
    args = ap.parse_args()
    quem = sorted(p.parent.name for p in (ROOT / 'characters').glob('*/character.json')) \
        if args.personagem == 'todos' else [args.personagem]
    for personagem in quem:
        caminho_do_pack = ROOT / f'characters/{personagem}/character.json'
        pack = json.loads(caminho_do_pack.read_text(encoding='utf-8'))
        nomes = args.animacoes or (list(LOCOMOCAO) if args.write or len(quem) > 1 else list(pack['animations']))
        mudou = False
        for nome in nomes:
            if nome not in pack['animations']:
                continue
            try:
                m = medir(pack, nome, args.v)
            except FileNotFoundError as e:
                print(f'{personagem:12} {nome:18} sem folha ({e.filename})')
                continue
            print(f'{personagem:12} ' + resumo(pack, nome, m))
            if args.write and pack['animations'][nome].get('apoio') != m:
                pack['animations'][nome]['apoio'] = m
                mudou = True
        if mudou:
            caminho_do_pack.write_text(json.dumps(pack, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
