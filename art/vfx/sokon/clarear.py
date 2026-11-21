#!/usr/bin/env python3
"""Clareia a sombra das fumaças do Sōkon FX (a ferramenta faz a sombra a 65% da cor; nas fumaças de
hoje ela é um cinza bem claro): cada cor vai para 1 - (1 - cor) * K, o alfa fica igual.
    python3 art/vfx/sokon/clarear.py PASTA [K=0.5]"""
import sys
from pathlib import Path
import numpy as np
from PIL import Image
pasta, k = Path(sys.argv[1]), float(sys.argv[2]) if len(sys.argv) > 2 else 0.5
for f in sorted(pasta.glob('q_*.png')):
    a = np.asarray(Image.open(f).convert('RGBA')).astype(np.float32) / 255.0
    a[..., :3] = 1.0 - (1.0 - a[..., :3]) * k
    Image.fromarray((a * 255).round().astype(np.uint8), 'RGBA').save(f)
