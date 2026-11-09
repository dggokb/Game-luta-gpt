#!/usr/bin/env python3
"""Junta quadros gravados (gravar.sh / gradle :pc:gravarLuta) numa imagem só, para olhar de uma vez.

    folha_quadros.py <pasta> <nome> <saida.png> <i0,i1,...> [corte x0,y0,x1,y1 | -] [colunas]
    folha_quadros.py /tmp/esp esp /tmp/esp.png 30,40,50,60 250,150,910,440 4

Para vídeo: ffmpeg -framerate 60 -i <pasta>/<nome>-%03d.png -pix_fmt yuv420p saida.mp4
"""
import sys
from PIL import Image
pasta, pre, out, idx = sys.argv[1:5]
box = tuple(int(v) for v in sys.argv[5].split(',')) if len(sys.argv) > 5 and sys.argv[5] != '-' else None
cols = int(sys.argv[6]) if len(sys.argv) > 6 else 4
ims = []
for i in idx.split(','):
    im = Image.open(f'{pasta}/{pre}-{int(i):03d}.png').convert('RGB')
    ims.append(im.crop(box) if box else im)
w, h = ims[0].size
rows = (len(ims) + cols - 1) // cols
s = Image.new('RGB', (w * cols + 2 * (cols - 1), h * rows + 2 * (rows - 1)))
for k, im in enumerate(ims): s.paste(im, ((k % cols) * (w + 2), (k // cols) * (h + 2)))
s.save(out); print(s.size)
