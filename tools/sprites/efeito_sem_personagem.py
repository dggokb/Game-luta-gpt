"""Separa só o efeito (branco/branco-azulado) de um vídeo de personagem no verde.

O personagem é escuro (roupa preta, cabelo, pele, faixa vermelha). Fica o pixel claro e pouco
saturado que faz parte de uma mancha encostada no fundo verde (o clarão e os riscos sempre tocam
o fundo; o branco do olho fica preso no rosto e sai), e que não puxa para pêssego/rosa (pele e
faixa vistas através do clarão). Os tênis (embaixo, e caixas pedidas com --excluir) ficam de fora.
A transparência sobre o verde vem da chave do verde do efeito_video.py.

    python3 tools/sprites/efeito_sem_personagem.py . <pasta PNG do vídeo> <saída> --quadros 11-18 [--espelhar]
        [--raio cx,cy,r]                 some suave do raio 0,7r até r (corta o que vai longe)
        [--excluir q0,x0,y0,x1,y1 ...]   a partir do quadro q0 do vídeo, apaga essa caixa
        [--chao-tenis 640]
"""
import argparse, sys
from pathlib import Path
import numpy as np
import cv2
from PIL import Image

p = argparse.ArgumentParser()
p.add_argument('repo'); p.add_argument('entrada'); p.add_argument('saida')
p.add_argument('--quadros', required=True)
p.add_argument('--espelhar', action='store_true')
p.add_argument('--raio')
p.add_argument('--excluir', nargs='*', default=[])
p.add_argument('--chao-tenis', type=int, default=640)
a = p.parse_args()
sys.path.insert(0, str(Path(a.repo) / 'tools/sprites'))
import efeito_video as ev

Q0, Q1 = (int(v) for v in a.quadros.split('-'))
arquivos = sorted(Path(a.entrada).glob('*.png'))
quadros = [np.array(Image.open(f).convert('RGB')) for f in arquivos]
fundo = ev.cor_do_fundo(quadros[:3])
saida = Path(a.saida); saida.mkdir(parents=True, exist_ok=True)
for f in saida.glob('*.png'): f.unlink()
H, W = quadros[0].shape[:2]
fade = np.ones((H, W), np.float32)
if a.raio:
    cx, cy, r = (float(v) for v in a.raio.split(','))
    yy, xx = np.mgrid[0:H, 0:W]
    d = np.hypot(xx - cx, yy - cy)
    fade = np.clip((r - d) / (0.3 * r), 0, 1).astype(np.float32)
caixas = [tuple(int(v) for v in e.split(',')) for e in a.excluir]

for n, q in enumerate(range(Q0, Q1 + 1)):
    rgb = quadros[q]
    rgba = ev.chave(rgb, fundo)
    c = rgb.astype(np.int32)
    verde = c[..., 1] - np.maximum(c[..., 0], c[..., 2])
    claro = (c.min(-1) > 140) & (verde < 45)
    perto_do_fundo = cv2.dilate((verde > 120).astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
    cand = ((rgba[..., 3] > 0) & (claro | (rgba[..., 3] < 250))).astype(np.uint8)
    cand[a.chao_tenis:] = 0
    for (q0, x0, y0, x1, y1) in caixas:
        if q >= q0: cand[y0:y1, x0:x1] = 0
    nlab, lab = cv2.connectedComponents(cand, connectivity=8)
    encosta = np.zeros(nlab, bool)
    encosta[np.unique(lab[perto_do_fundo & (cand > 0)])] = True
    encosta[0] = False
    manter = encosta[lab] & (claro | (rgba[..., 3] < 250))
    cor = rgba[..., :3].astype(np.int32)
    manter &= (cor.min(-1) > 160) & (cor[..., 0] - cor[..., 2] < 20)
    out = rgba.copy()
    alfa = np.where(manter, rgba[..., 3], 0).astype(np.float32)
    alfa[manter & claro] = np.maximum(alfa[manter & claro], 235)
    out[..., 3] = np.clip(alfa * fade, 0, 255).astype(np.uint8)
    img = Image.fromarray(out, 'RGBA')
    if a.espelhar: img = img.transpose(Image.FLIP_LEFT_RIGHT)
    img.save(saida / f'q{n}.png')
print('quadros', Q1 - Q0 + 1, '->', saida)
