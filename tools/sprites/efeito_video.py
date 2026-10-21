#!/usr/bin/env python3
"""Converte o vídeo de um efeito (fundo verde, sem personagem) numa folha de efeito do jogo.

O efeito vem do Seedance/Omni sozinho no verde: impacto, fumaça, onda de chão, arco de golpe.
Diferente do personagem (recorte tudo ou nada), efeito tem partes meio transparentes
(fumaça, brilho, rastro), então a chave aqui é por diferença de cor: quanto mais verde o pixel
é em relação ao fundo, mais transparente ele fica, e a cor do fundo sai da mistura (o branco a
50% sobre o verde volta a ser branco a 50%, não verde-claro).

    pip install -r tools/sprites/requirements-animar.txt
    python3 tools/sprites/efeito_video.py impacto.mp4 --nome impacto_forte \\
        --inicio 12 --fim 40 --ancora centro --altura 220

    --ancora centro   o ponto de onde o efeito sai é o meio do vídeo (ou --origem x,y)
    --ancora chao     o meio de baixo do efeito (fumaça, onda de chão): fica no chão do jogo
    --altura H        o efeito inteiro (a maior altura entre os quadros) vira H px do mundo
    --recorte x0,y0,x1,y1   usa só esse retângulo do vídeo (outra coisa no quadro)
    --passo N         um quadro a cada N do vídeo (24 fps com --passo 2 = 12 fps)

Sai em android/app/src/main/assets/efeitos/<nome>/: folha.webp (os quadros recortados e
empacotados) e efeito.json (onde cada quadro está, a origem e o tempo de cada um). A prévia
(GIF sobre xadrez) vai para android/app/build/sprite-review/efeitos/<nome>.gif.
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SAIDA = ROOT / 'android/app/src/main/assets/efeitos'
PREVIA = ROOT / 'android/app/build/sprite-review/efeitos'
# Resolução da folha: pixels da folha por pixel do mundo (nítido na tela do celular).
PIXEL_SCALE = 1.5
# Abaixo disso o pixel é fundo (ruído da compressão do vídeo no verde).
ALFA_MINIMO = 0.08
# Manchas menores que isso (px do vídeo) são ruído, não efeito.
AREA_MINIMA = 12
LARGURA_FOLHA = 2048


def ler_quadros(caminho, inicio, fim, passo):
    cap = cv2.VideoCapture(str(caminho))
    fps = cap.get(cv2.CAP_PROP_FPS) or 24.0
    quadros, i = [], 0
    while True:
        ok, q = cap.read()
        if not ok:
            break
        if i >= inicio and (fim is None or i <= fim) and (i - inicio) % passo == 0:
            quadros.append(cv2.cvtColor(q, cv2.COLOR_BGR2RGB))
        i += 1
    cap.release()
    if not quadros:
        raise SystemExit('nenhum quadro no trecho pedido')
    return quadros, fps


def cor_do_fundo(quadros):
    """A cor do verde: a mediana dos pixels bem verdes (dos primeiros quadros, quase vazios)."""
    amostra = np.concatenate([q.reshape(-1, 3) for q in quadros[:3]]).astype(np.float32)
    verde = amostra[:, 1] - np.maximum(amostra[:, 0], amostra[:, 2])
    fundo = amostra[verde > np.percentile(verde, 60)]
    return np.median(fundo, axis=0)


def chave(quadro, fundo):
    """RGBA por diferença de cor: alfa pelo quanto o pixel é menos verde que o fundo, e a cor
    do fundo tirada da mistura (despill)."""
    f = quadro.astype(np.float32)
    verde = f[..., 1] - np.maximum(f[..., 0], f[..., 2])
    verde_fundo = max(1.0, float(fundo[1] - max(fundo[0], fundo[2])))
    a = np.clip(1.0 - verde / verde_fundo, 0.0, 1.0)
    a[a < ALFA_MINIMO] = 0.0
    # Pontinhos soltos (ruído da compressão no verde) saem; cacos e gotas do efeito ficam.
    n, rotulos, stats, _ = cv2.connectedComponentsWithStats((a > 0).astype(np.uint8))
    miudos = stats[:, cv2.CC_STAT_AREA] < AREA_MINIMA
    miudos[0] = False
    a[miudos[rotulos]] = 0.0
    cor = (f - (1.0 - a[..., None]) * fundo) / np.maximum(a, 1e-3)[..., None]
    cor = np.clip(cor, 0, 255)
    # O verde que sobra na borda (cinza esverdeado) vira cinza.
    cor[..., 1] = np.minimum(cor[..., 1], np.maximum(cor[..., 0], cor[..., 2]) + 6)
    return np.dstack([cor, a * 255.0]).astype(np.uint8)


def caixa(alfa, limiar=10):
    ys, xs = np.where(alfa > limiar)
    if len(xs) == 0:
        return None
    return int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1


def uniao(caixas):
    caixas = [c for c in caixas if c]
    if not caixas:
        raise SystemExit('o trecho não tem efeito nenhum (tudo verde?)')
    return (min(c[0] for c in caixas), min(c[1] for c in caixas),
            max(c[2] for c in caixas), max(c[3] for c in caixas))


def empacotar(imagens):
    """Prateleiras: lado a lado até a largura da folha, depois a próxima linha."""
    x = y = linha = 0
    lugares = []
    for im in imagens:
        h, w = im.shape[:2]
        if x + w > LARGURA_FOLHA:
            x, y, linha = 0, y + linha + 2, 0
        lugares.append((x, y))
        x += w + 2
        linha = max(linha, h)
    largura = max((lx + im.shape[1] for (lx, _), im in zip(lugares, imagens)), default=1)
    altura = max((ly + im.shape[0] for (_, ly), im in zip(lugares, imagens)), default=1)
    folha = np.zeros((altura, largura, 4), np.uint8)
    for (lx, ly), im in zip(lugares, imagens):
        folha[ly:ly + im.shape[0], lx:lx + im.shape[1]] = im
    return folha, lugares


def converter(video, nome, inicio=0, fim=None, passo=1, ancora='centro', origem=None,
              altura=200.0, recorte=None, saida=SAIDA, previa=PREVIA):
    quadros, fps = ler_quadros(video, inicio, fim, passo)
    if recorte:
        x0, y0, x1, y1 = recorte
        quadros = [q[y0:y1, x0:x1] for q in quadros]
    fundo = cor_do_fundo(quadros)
    rgba = [chave(q, fundo) for q in quadros]
    ux0, uy0, ux1, uy1 = uniao([caixa(q[..., 3]) for q in rgba])
    # Escala: a altura do efeito inteiro vira `altura` px do mundo (a folha guarda PIXEL_SCALE).
    escala = altura * PIXEL_SCALE / (uy1 - uy0)
    if origem:
        ox, oy = origem[0] - (recorte[0] if recorte else 0), origem[1] - (recorte[1] if recorte else 0)
    elif ancora == 'chao':
        ox, oy = (ux0 + ux1) / 2.0, uy1
    else:
        h, w = quadros[0].shape[:2]
        ox, oy = w / 2.0, h / 2.0
    recortados, desloc = [], []
    for q in rgba:
        c = caixa(q[..., 3])
        if c is None:
            c = (ux0, uy0, ux0 + 1, uy0 + 1)
        x0, y0, x1, y1 = c
        parte = q[y0:y1, x0:x1]
        w = max(1, round((x1 - x0) * escala))
        h = max(1, round((y1 - y0) * escala))
        # Redução com área (sem serrilhar o contorno fino); alfa pré-multiplicado para a borda
        # não escurecer.
        pre = parte.astype(np.float32)
        pre[..., :3] *= pre[..., 3:4] / 255.0
        pre = cv2.resize(pre, (w, h), interpolation=cv2.INTER_AREA)
        a = pre[..., 3:4] / 255.0
        pre[..., :3] = np.where(a > 1e-3, pre[..., :3] / np.maximum(a, 1e-3), 0)
        recortados.append(np.clip(pre, 0, 255).astype(np.uint8))
        desloc.append(((x0 - ox) * escala, (y0 - oy) * escala))
    folha, lugares = empacotar(recortados)
    destino = Path(saida) / nome
    destino.mkdir(parents=True, exist_ok=True)
    Image.fromarray(folha, 'RGBA').save(destino / 'folha.webp', lossless=True, method=6)
    ms = round(1000.0 * passo / fps)
    dados = {
        'nome': nome,
        'video': Path(video).name,
        'trecho': [inicio, fim if fim is not None else inicio + (len(quadros) - 1) * passo],
        'passo': passo,
        'ancora': 'origem' if origem else ancora,
        'pixelScale': PIXEL_SCALE,
        'altura': altura,
        # Cada quadro: x, y, largura, altura na folha e onde o canto de cima fica em relação à
        # origem (pixels da folha).
        'quadros': [[lx, ly, im.shape[1], im.shape[0], round(dx, 1), round(dy, 1)]
                    for (lx, ly), im, (dx, dy) in zip(lugares, recortados, desloc)],
        'duracoesMs': [ms] * len(recortados),
    }
    (destino / 'efeito.json').write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    gif_previa(dados, recortados, Path(previa) / f'{nome}.gif')
    return destino, dados


def gif_previa(dados, recortados, caminho):
    caminho.parent.mkdir(parents=True, exist_ok=True)
    xs = [q[4] for q in dados['quadros']] + [q[4] + q[2] for q in dados['quadros']]
    ys = [q[5] for q in dados['quadros']] + [q[5] + q[3] for q in dados['quadros']]
    x0, y0 = int(min(xs)) - 8, int(min(ys)) - 8
    w, h = int(max(xs)) - x0 + 8, int(max(ys)) - y0 + 8
    yy, xx = np.mgrid[0:h, 0:w]
    xadrez = np.where(((yy // 16) + (xx // 16)) % 2 == 0, 60, 80).astype(np.float32)
    imagens = []
    for q, im in zip(dados['quadros'], recortados):
        tela = np.dstack([xadrez] * 3)
        px, py = int(round(q[4])) - x0, int(round(q[5])) - y0
        a = im[..., 3:4].astype(np.float32) / 255.0
        area = tela[py:py + im.shape[0], px:px + im.shape[1]]
        area[:] = im[..., :3] * a + area * (1 - a)
        # A origem em vermelho (onde o jogo põe o efeito).
        cx, cy = -x0, -y0
        tela[max(0, cy - 1):cy + 2, max(0, cx - 6):cx + 7] = (255, 40, 40)
        tela[max(0, cy - 6):cy + 7, max(0, cx - 1):cx + 2] = (255, 40, 40)
        imagens.append(Image.fromarray(tela.astype(np.uint8)))
    imagens[0].save(caminho, save_all=True, append_images=imagens[1:], duration=dados['duracoesMs'], loop=0)


def numeros(texto, n):
    v = [int(x) for x in texto.split(',')]
    if len(v) != n:
        raise argparse.ArgumentTypeError(f'esperado {n} números separados por vírgula')
    return v


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('video')
    p.add_argument('--nome', required=True)
    p.add_argument('--inicio', type=int, default=0)
    p.add_argument('--fim', type=int)
    p.add_argument('--passo', type=int, default=1)
    p.add_argument('--ancora', choices=['centro', 'chao'], default='centro')
    p.add_argument('--origem', type=lambda t: numeros(t, 2))
    p.add_argument('--altura', type=float, default=200.0, help='altura do efeito no mundo (px)')
    p.add_argument('--recorte', type=lambda t: numeros(t, 4))
    a = p.parse_args()
    destino, dados = converter(a.video, a.nome, a.inicio, a.fim, a.passo, a.ancora, a.origem,
                               a.altura, a.recorte)
    print(f"{len(dados['quadros'])} quadros em {destino} (prévia em {PREVIA / (a.nome + '.gif')})")


if __name__ == '__main__':
    main()
