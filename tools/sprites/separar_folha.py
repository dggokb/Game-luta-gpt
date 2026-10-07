#!/usr/bin/env python3
"""Turns a GPT sprite sheet drawn on a tight grid into a strip the importer can read.

Image generators pack the poses close together: a hand or a strap crosses into the
neighbouring cell and column projection merges two frames. Each connected piece of art
(8-connectivity) goes to the grid cell holding most of its pixels, then the frames are laid
out left to right, in reading order, with transparent space between them and the lowest
pixel of every frame on the same line.

    python3 tools/sprites/separar_folha.py folha_gpt.png --colunas 3 --linhas 3 \
        art/sprites/source/player_base_ultra_beam_source.png
"""
import argparse

from PIL import Image

ALPHA = 8
PAD = 40


def components(image):
    alpha = image.getchannel("A").load()
    w, h = image.size
    seen = bytearray(w * h)
    for y in range(h):
        for x in range(w):
            if alpha[x, y] <= ALPHA or seen[y * w + x]:
                continue
            seen[y * w + x] = 1
            stack, points = [(x, y)], []
            while stack:
                cx, cy = stack.pop()
                points.append((cx, cy))
                for nx in (cx - 1, cx, cx + 1):
                    for ny in (cy - 1, cy, cy + 1):
                        if 0 <= nx < w and 0 <= ny < h and not seen[ny * w + nx] and alpha[nx, ny] > ALPHA:
                            seen[ny * w + nx] = 1
                            stack.append((nx, ny))
            yield points


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("entrada")
    p.add_argument("saida")
    p.add_argument("--colunas", type=int, required=True)
    p.add_argument("--linhas", type=int, required=True)
    args = p.parse_args()

    sheet = Image.open(args.entrada).convert("RGBA")
    cell_w = sheet.width / args.colunas
    cell_h = sheet.height / args.linhas
    count = args.colunas * args.linhas
    owned = [[] for _ in range(count)]
    for points in components(sheet):
        votes = {}
        for x, y in points:
            cell = min(int(x // cell_w), args.colunas - 1) + min(int(y // cell_h), args.linhas - 1) * args.colunas
            votes[cell] = votes.get(cell, 0) + 1
        owned[max(votes, key=votes.get)].extend(points)

    src = sheet.load()
    frames = []
    for index, points in enumerate(owned):
        if not points:
            raise SystemExit(f"quadro {index} vazio")
        xs = [x for x, _ in points]
        ys = [y for _, y in points]
        x0, y0 = min(xs), min(ys)
        frame = Image.new("RGBA", (max(xs) - x0 + 1, max(ys) - y0 + 1))
        out = frame.load()
        for x, y in points:
            out[x - x0, y - y0] = src[x, y]
        frames.append(frame)

    slot_w = max(f.width for f in frames) + 2 * PAD
    slot_h = max(f.height for f in frames) + 2 * PAD
    strip = Image.new("RGBA", (slot_w * count, slot_h))
    for index, frame in enumerate(frames):
        strip.paste(frame, (index * slot_w + PAD, slot_h - PAD - frame.height))
    strip.save(args.saida, optimize=True)
    print(f"{count} quadros, faixa {strip.width}x{strip.height} em {args.saida}")


if __name__ == "__main__":
    main()
