#!/usr/bin/env python3
"""Converte o vídeo de uma reação (fundo verde) na folha de um ator de fundo do cenário.

    python tools/stage/video_para_ator.py aplaudir.mp4 \
        --ator android/app/src/main/assets/stages/dojo_shinshinkan/atores/dafu --animacao aplaudir

O vídeo vem do Seedance com o ator de frente (3/4), câmera parada e fundo verde, começando
e terminando na imagem inicial dele (docs/tutorial-plateia.md). Para cada quadro: recorta o
verde com o mesmo recorte dos lutadores (tools/sprites/video_para_sprite.py), reduz para a
altura da célula e põe os pés no fundo da célula, no mesmo lugar em todas as animações.

A escala e o lugar dos pés são medidos no primeiro vídeo convertido do ator (de preferência
o "parado") e ficam gravados no ator.json, em "video". Os outros vídeos usam a mesma medida,
então o ator não muda de tamanho nem anda quando troca de animação. --medir mede de novo.

Grava <animacao>.png na pasta do ator e preenche "folha", "quadros", "colunas" e "fps" da
animação no ator.json. --loop marca a animação como loop (o "parado").

Precisa de opencv e numpy: pip install -r tools/sprites/requirements-animar.txt
"""

import argparse
import json
import math
import sys
from pathlib import Path

try:
    import cv2
    import numpy as np
    from PIL import Image
except ImportError:
    sys.exit("Instale: pip install -r tools/sprites/requirements-animar.txt")

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "sprites"))
from video_para_sprite import best_loop, keep_largest, key, read_frames, torso_x  # noqa: E402

FOLGA = 12
COLUNAS_MAX = 8


def fundo_verde(frame):
    """A borda do quadro é verde (o fundo pedido)? O Seedance às vezes entrega fundo claro."""
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    borda = np.concatenate([hsv[0], hsv[-1], hsv[:, 0], hsv[:, -1]])
    h, sat = np.median(borda[:, 0]), np.median(borda[:, 1])
    return 35 < h < 95 and sat > 45


def key_claro(frame, tol=24):
    """Fundo claro liso (bege, cinza, branco): o fundo é o que a cor da borda alcança sem
    atravessar o contorno do desenho. Roupa clara dentro do contorno (a camisa creme do Dafu)
    fica, o que um recorte só pela cor apagaria. A sombra cinza do chão embaixo dos pés sai."""
    img = frame.astype(np.int16)
    h, w = img.shape[:2]
    borda = np.concatenate([img[0], img[-1], img[:, 0], img[:, -1]])
    fundo = np.median(borda, axis=0)
    parecido = (np.abs(img - fundo).max(2) < tol).astype(np.uint8)
    mascara = np.zeros((h + 2, w + 2), np.uint8)
    for x, y in ([(x, 0) for x in range(0, w, 4)] + [(x, h - 1) for x in range(0, w, 4)] +
                 [(0, y) for y in range(0, h, 4)] + [(w - 1, y) for y in range(0, h, 4)]):
        if parecido[y, x] == 1:
            cv2.floodFill(parecido, mascara, (x, y), 2)
    alpha = np.where(parecido == 2, 0, 255).astype(np.uint8)
    alpha = keep_largest(cv2.morphologyEx(alpha, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8)))
    # Sombra no chão: cinza sem cor, um pouco mais escura que o fundo, só na faixa dos pés.
    rows = np.nonzero((alpha > 0).any(1))[0]
    if len(rows):
        hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        luz = img.mean(2)
        faixa = np.zeros_like(alpha, bool)
        faixa[rows.max() - (rows.max() - rows.min()) * 8 // 100:] = True
        sombra = faixa & (hsv[..., 1] < 22) & (luz > fundo.mean() - 70) & (luz < fundo.mean() + 6)
        alpha[sombra] = 0
        # Fundo preso entre as pernas (cercado pelas pernas e pela sombra, a borda não chega
        # lá): cor do fundo, só na parte de baixo do corpo. A camisa clara fica mais acima.
        pernas = np.zeros_like(alpha, bool)
        pernas[rows.max() - (rows.max() - rows.min()) * 22 // 100:] = True
        vao = ((np.abs(img - fundo).max(2) < tol) & pernas & (alpha > 0)).astype(np.uint8)
        n, rotulos, stats, _ = cv2.connectedComponentsWithStats(vao, 8)
        for i in range(1, n):
            if stats[i, cv2.CC_STAT_AREA] >= 120:
                alpha[rotulos == i] = 0
        alpha = keep_largest(cv2.morphologyEx(alpha, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8)))
    alpha = cv2.GaussianBlur(alpha, (3, 3), 0)
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    return np.dstack([rgb, alpha])


def trecho_com_movimento(recortes, inicio, fim, fps):
    """Corta a pose parada do começo e do fim (o vídeo segura a pose depois da reação):
    do primeiro quadro que mexe até o último, com um respiro de ~0,15 s de cada lado."""
    def mini(r):
        g = cv2.cvtColor(r[..., :3], cv2.COLOR_RGB2GRAY).astype(np.float32) * (r[..., 3] / 255.0)
        return cv2.resize(g, (160, 90))
    pequenos = [mini(r) for r in recortes[inicio:fim + 1]]
    primeiro, ultimo = pequenos[0], pequenos[-1]
    ida = np.array([np.abs(p - primeiro).mean() for p in pequenos])
    volta = np.array([np.abs(p - ultimo).mean() for p in pequenos])
    limite = max(1.0, 0.12 * max(ida.max(), volta.max()))
    folga = int(round(0.15 * fps))
    mexe = np.nonzero(ida > limite)[0]
    para = np.nonzero(volta > limite)[0]
    a = inicio + max(0, (mexe[0] if len(mexe) else 0) - folga)
    b = inicio + min(len(pequenos) - 1, (para[-1] if len(para) else len(pequenos) - 1) + folga)
    return a, max(a, b)


def medir(rgba, altura):
    """Escala, x do tronco e y dos pés do ator na imagem inicial (o primeiro quadro)."""
    a = rgba[..., 3] > 128
    rows = np.nonzero(a.any(1))[0]
    if len(rows) == 0:
        sys.exit("o primeiro quadro não tem ninguém: o vídeo começa na imagem inicial?")
    top, bottom = int(rows.min()), int(rows.max())
    escala = (altura - 2 * FOLGA) / (bottom - top + 1)
    return {"escala": round(escala, 5), "centroX": round(torso_x(rgba), 1), "chaoY": bottom}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("video")
    parser.add_argument("--ator", type=Path, required=True, help="pasta do ator (com o ator.json)")
    parser.add_argument("--animacao", required=True)
    parser.add_argument("--inicio", type=int, default=0, help="primeiro quadro do vídeo")
    parser.add_argument("--fim", type=int, default=None,
                        help="último quadro do vídeo (sem ele: corta a pose parada, ou acha o loop com --loop)")
    parser.add_argument("--fps", type=float, default=10.0, help="quadros por segundo no jogo")
    parser.add_argument("--altura", type=int, default=320, help="altura da célula em px")
    parser.add_argument("--loop", action="store_true", help="animação em loop (a parada)")
    parser.add_argument("--espelhar", action="store_true", help="o vídeo olha para o lado errado")
    parser.add_argument("--fundo", choices=["auto", "verde", "claro"], default="auto",
                        help="cor do fundo do vídeo (auto: verde, ou claro se a borda não for verde)")
    parser.add_argument("--cores", type=int, default=256, help="cores da paleta (0 = PNG cheio)")
    parser.add_argument("--medir", action="store_true", help="mede a escala de novo neste vídeo")
    args = parser.parse_args()

    ator_json = args.ator / "ator.json"
    ator = json.loads(ator_json.read_text(encoding="utf-8"))
    frames, fps_video = read_frames(args.video)
    if not frames:
        sys.exit(f"não li nenhum quadro de {args.video}")
    fps_video = fps_video or 24.0
    claro = args.fundo == "claro" or (args.fundo == "auto" and not fundo_verde(frames[0]))
    if claro:
        print("fundo claro (não verde): recorte a partir das bordas, parando no contorno")
        todos = [key_claro(f) for f in frames]
    else:
        # Recorte simples: o modo de efeitos dos lutadores apagaria roupa clara (camisa do
        # Dafu, casaco branco do Takatoshi), e as reações não têm efeitos.
        todos = [key(f) for f in frames]
    if args.espelhar:
        todos = [r[:, ::-1] for r in todos]

    inicio, fim = args.inicio, len(frames) - 1 if args.fim is None else min(args.fim, len(frames) - 1)
    if args.loop and args.fim is None:
        # O trecho que melhor fecha em loop (o último quadro emenda no primeiro).
        _, inicio, fim = best_loop(frames, inicio, fim, int(fps_video * 1.6))
        fim -= 1
    elif not args.loop and args.fim is None:
        inicio, fim = trecho_com_movimento(todos, inicio, fim, fps_video)
    passo = max(1.0, fps_video / args.fps)
    indices = [int(round(inicio + k * passo)) for k in range(int((fim - inicio) / passo) + 1)]
    recortes = [todos[i] for i in indices]

    medida = ator.get("video")
    if medida is None or args.medir:
        medida = medir(todos[0], args.altura)
        ator["video"] = medida
        print(f"medida nova: {medida}")
    escala, cx, chao = medida["escala"], medida["centroX"], medida["chaoY"]
    # Cada vídeo deixa o ator uns pixels diferente: alinha os pés e o tronco do primeiro
    # quadro deste vídeo com os da medida (a escala é a mesma para todos).
    proprio = medir(todos[0], args.altura)
    dx, dy = cx - proprio["centroX"], chao - proprio["chaoY"]

    # Largura da célula: o que o ator ocupa para os lados do tronco, nesta animação.
    meia = 0
    for rgba in recortes:
        xs = np.nonzero((rgba[..., 3] > 128).any(0))[0]
        if len(xs):
            meia = max(meia, abs(xs.min() + dx - cx), abs(xs.max() + dx - cx))
    cw = int(math.ceil(meia * escala)) * 2 + 2 * FOLGA
    ch = args.altura
    colunas = min(COLUNAS_MAX, len(recortes))
    linhas = math.ceil(len(recortes) / colunas)
    folha = Image.new("RGBA", (cw * colunas, ch * linhas), (0, 0, 0, 0))
    for n, rgba in enumerate(recortes):
        img = Image.fromarray(rgba)
        img = img.resize((round(img.width * escala), round(img.height * escala)), Image.LANCZOS)
        # Pés na base da célula e tronco no meio, sempre no mesmo ponto.
        celula = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
        celula.paste(img, (cw // 2 - round((cx - dx) * escala), ch - FOLGA - round((chao - dy) * escala)))
        folha.paste(celula, ((n % colunas) * cw, (n // colunas) * ch))

    nome = f"{args.animacao}.png"
    if args.cores:
        # Paleta de 256 cores: ~5x menor no APK, sem diferença visível na arte chapada.
        folha = folha.quantize(colors=args.cores, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
    folha.save(args.ator / nome, optimize=True)
    anim = {"folha": nome, "quadros": len(recortes), "colunas": colunas, "fps": args.fps}
    if args.loop:
        anim["loop"] = True
    ator.setdefault("animacoes", {})[args.animacao] = anim
    ator_json.write_text(json.dumps(ator, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{args.ator / nome}: {len(recortes)} quadros de {Path(args.video).name} "
          f"(quadros {indices[0]}..{indices[-1]} do vídeo), célula {cw}x{ch}")


if __name__ == "__main__":
    main()
