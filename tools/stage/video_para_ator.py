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
from video_para_sprite import key, read_frames, torso_x  # noqa: E402

FOLGA = 12
COLUNAS_MAX = 8


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
    parser.add_argument("--fim", type=int, default=None, help="último quadro do vídeo")
    parser.add_argument("--fps", type=float, default=12.0, help="quadros por segundo no jogo")
    parser.add_argument("--altura", type=int, default=480, help="altura da célula em px")
    parser.add_argument("--loop", action="store_true", help="animação em loop (a parada)")
    parser.add_argument("--espelhar", action="store_true", help="o vídeo olha para o lado errado")
    parser.add_argument("--medir", action="store_true", help="mede a escala de novo neste vídeo")
    args = parser.parse_args()

    ator_json = args.ator / "ator.json"
    ator = json.loads(ator_json.read_text(encoding="utf-8"))
    frames, fps_video = read_frames(args.video)
    if not frames:
        sys.exit(f"não li nenhum quadro de {args.video}")
    fim = len(frames) - 1 if args.fim is None else min(args.fim, len(frames) - 1)
    passo = max(1.0, (fps_video or 24.0) / args.fps)
    indices = [int(round(args.inicio + k * passo)) for k in range(int((fim - args.inicio) / passo) + 1)]

    recortes = []
    for i in indices:
        # Recorte simples: o modo de efeitos dos lutadores apagaria roupa clara (camisa do
        # Dafu, casaco branco do Takatoshi), e as reações não têm efeitos.
        rgba = key(frames[i])
        if args.espelhar:
            rgba = rgba[:, ::-1]
        recortes.append(rgba)

    medida = ator.get("video")
    if medida is None or args.medir:
        medida = medir(recortes[0], args.altura)
        ator["video"] = medida
        print(f"medida nova: {medida}")

    escala, cx, chao = medida["escala"], medida["centroX"], medida["chaoY"]
    # Largura da célula: o que o ator ocupa para os lados do tronco, nesta animação.
    meia = 0
    for rgba in recortes:
        xs = np.nonzero((rgba[..., 3] > 128).any(0))[0]
        if len(xs):
            meia = max(meia, abs(xs.min() - cx), abs(xs.max() - cx))
    cw = int(math.ceil(meia * escala)) * 2 + 2 * FOLGA
    ch = args.altura
    colunas = min(COLUNAS_MAX, len(recortes))
    linhas = math.ceil(len(recortes) / colunas)
    folha = Image.new("RGBA", (cw * colunas, ch * linhas), (0, 0, 0, 0))
    for n, rgba in enumerate(recortes):
        img = Image.fromarray(rgba)
        img = img.resize((round(img.width * escala), round(img.height * escala)), Image.LANCZOS)
        # Pés na base da célula e tronco no meio, sempre no mesmo ponto do vídeo.
        celula = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
        celula.paste(img, (cw // 2 - round(cx * escala), ch - FOLGA - round(chao * escala)))
        folha.paste(celula, ((n % colunas) * cw, (n // colunas) * ch))

    nome = f"{args.animacao}.png"
    folha.save(args.ator / nome)
    anim = {"folha": nome, "quadros": len(recortes), "colunas": colunas, "fps": args.fps}
    if args.loop:
        anim["loop"] = True
    ator.setdefault("animacoes", {})[args.animacao] = anim
    ator_json.write_text(json.dumps(ator, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{args.ator / nome}: {len(recortes)} quadros de {args.video} ({indices[0]}..{indices[-1]}), célula {cw}x{ch}")


if __name__ == "__main__":
    main()
