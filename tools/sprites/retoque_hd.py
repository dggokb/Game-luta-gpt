#!/usr/bin/env python3
"""Repete na folha HD os retoques feitos à mão na folha 1x revisada.

Personagem que já estava no jogo em 1x tem retoques fora da receita (efeito apagado num
quadro, célula deslocada 1 px para a harmonia). Ao refazer em HD (`video_para_sprite.py
--densidade 2`), a folha 1x revisada fica como referência e, célula a célula:

  * deslocamento: a célula HD vai para onde a silhueta da referência está (até 4 px lógicos);
  * apagado: o que a referência não tem e a HD tem (mancha, não só a borda) sai da HD.

Quadro trocado por outro do vídeo não se transfere: vai na lista `quadros` da receita.
Célula que mesmo deslocada não bate com a referência é apontada (quadro trocado à mão).

A receita guarda a referência em "video" -> "referencia" (folha, célula, raiz e colunas da
folha 1x); a reconversão repete tudo.
"""
import cv2
import numpy as np
from PIL import Image

MAX_SHIFT = 4         # px lógicos procurados em cada direção
MATCH_GAIN = 0.004    # ganho mínimo de IoU para deslocar (ruído de arredondamento fica)
POOR_MATCH = 0.85     # IoU abaixo disso: a referência é outro quadro (avisa)
GONE_ALPHA, KEEP_ALPHA = 16, 128
MIN_ERASE = 30        # px lógicos²: mancha menor é diferença de borda, fica


def ref_cell(ref, i, geo, shape, root, d):
    """Célula i da referência 1x, ampliada d vezes e posta na célula HD pela raiz."""
    w, h = geo['celula']
    rx, ry = geo['raiz']
    cols = geo['colunas']
    c = ref.crop((i % cols * w, i // cols * h, (i % cols + 1) * w, (i // cols + 1) * h))
    up = c.resize((w * d, h * d), Image.BILINEAR)
    canvas = Image.new('RGBA', (shape[1], shape[0]), (0, 0, 0, 0))
    canvas.paste(up, (root[0] - rx * d, root[1] - ry * d))
    return np.array(canvas)


def shift(a, dx, dy):
    """Desloca a imagem (o que sai da célula some)."""
    out = np.zeros_like(a)
    h, w = a.shape[:2]
    xs, xd = (slice(0, w - dx), slice(dx, w)) if dx >= 0 else (slice(-dx, w), slice(0, w + dx))
    ys, yd = (slice(0, h - dy), slice(dy, h)) if dy >= 0 else (slice(-dy, h), slice(0, h + dy))
    out[yd, xd] = a[ys, xs]
    return out


def iou(a, b):
    return (a & b).sum() / max(1, (a | b).sum())


def transfer_cell(hd, ref, d):
    """(célula HD retocada, deslocamento, px apagados, IoU final)."""
    rm = ref[..., 3] > KEEP_ALPHA
    hm = hd[..., 3] > KEEP_ALPHA
    m = MAX_SHIFT * d
    # Interseção para todos os deslocamentos de uma vez (correlação de máscaras 0/1).
    pad = np.pad(rm.astype(np.float32), m)
    inter = cv2.matchTemplate(pad, hm.astype(np.float32), cv2.TM_CCORR)
    scores = inter / np.maximum(1, rm.sum() + hm.sum() - inter)
    yy, xx = np.unravel_index(int(np.argmax(scores)), scores.shape)
    best = (float(scores[yy, xx]), int(xx) - m, int(yy) - m)
    full_base = iou(hm, rm)
    if best[1] or best[2]:
        if best[0] - full_base >= MATCH_GAIN:
            hd = shift(hd, best[1], best[2])
        else:
            best = (full_base, 0, 0)
    else:
        best = (full_base, 0, 0)
    # Apagado na referência: mancha (não só borda) que a HD tem e a referência não.
    gone = (ref[..., 3] < GONE_ALPHA) & (hd[..., 3] > KEEP_ALPHA)
    k = np.ones((2 * d + 1,) * 2, np.uint8)
    gone = cv2.morphologyEx(gone.astype(np.uint8), cv2.MORPH_OPEN, k)
    n, lab, stats, _ = cv2.connectedComponentsWithStats(gone, connectivity=8)
    keep = [j for j in range(1, n) if stats[j, cv2.CC_STAT_AREA] >= MIN_ERASE * d * d]
    erased = 0
    if keep:
        mask = cv2.dilate(np.isin(lab, keep).astype(np.uint8), k) > 0
        mask &= ref[..., 3] < 64
        erased = int(mask.sum())
        hd = hd.copy()
        hd[mask] = 0
    return hd, (best[1], best[2]), erased, float(iou(hd[..., 3] > KEEP_ALPHA, rm))


def transfer(sheet_path, clip, referencia, root_dir):
    """Aplica os retoques da referência à folha HD do clipe (no lugar)."""
    d = int(clip.get('pixelScale', 1))
    w, h = clip['frameWidth'], clip['frameHeight']
    root = (clip['rootX'], clip['rootY'])
    cols, n = clip['columns'], clip['expectedFrames']
    sheet = np.array(Image.open(sheet_path).convert('RGBA'))
    ref = Image.open(root_dir / referencia['folha']).convert('RGBA')
    report = []
    for i in range(n):
        y, x = i // cols * h, i % cols * w
        hd = sheet[y:y + h, x:x + w]
        new, off, erased, score = transfer_cell(hd, ref_cell(ref, i, referencia, hd.shape, root, d), d)
        sheet[y:y + h, x:x + w] = new
        if off != (0, 0) or erased or score < POOR_MATCH:
            report.append(f"célula {i}: desloca {off[0]:+d},{off[1]:+d} px, apaga {erased} px"
                          + (f", NÃO BATE (IoU {score:.2f}: quadro trocado à mão?)" if score < POOR_MATCH else ''))
    Image.fromarray(sheet, 'RGBA').save(sheet_path)
    return report
