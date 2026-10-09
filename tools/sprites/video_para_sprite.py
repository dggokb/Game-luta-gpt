#!/usr/bin/env python3
"""Converte um vídeo de animação (fundo verde) em folha normalizada do pipeline.

O vídeo vem do Seedance com o personagem de lado, câmera parada e fundo verde liso. Para
cada quadro do trecho escolhido: recorta o verde (e a sombra/faixa de chão), mantém só o
personagem (maior peça conectada; poeira e efeitos soltos saem), corrige o deslizamento
lateral e escala tudo para o tamanho oficial do personagem. A raiz (meio dos pés) cai em
rootX/rootY da célula.

O tamanho nunca é chutado: --personagem mede a escala comparando o vídeo com as imagens
iniciais (art/keys/<id>/inicio_*.png, ver tools/sprites/tamanho.py). Só quando o vídeo não
bate com nada é que se passa --escala, e aí --motivo é obrigatório.

    python3 tools/sprites/video_para_sprite.py video.mp4 --personagem p03 \
        --clipe tools/sprites/clips/p03_jab.json --inicio 16 --fim 37

Com --clipe a folha vai para o `source` do clipe e a receita (vídeo, escala, quadros e
opções) fica gravada no próprio clipe, em "video"; para refazer basta
`video_para_sprite.py video.mp4 --clipe <clipe>` (opções da linha de comando mudam a receita).
`--loop A-B` procura, dentro de A..B, o trecho que melhor fecha em loop.

Precisa de opencv e numpy: pip install -r tools/sprites/requirements-animar.txt
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

import tamanho

ROOT = Path(__file__).resolve().parents[2]


def read_frames(path):
    cap = cv2.VideoCapture(path)
    frames = []
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        frames.append(frame)
    return frames, cap.get(cv2.CAP_PROP_FPS)


def best_loop(frames, lo, hi, min_len):
    """Compara os quadros recortados em volta do tronco, para funcionar mesmo se o
    personagem desliza no vídeo. Só os quadros de lo a hi são recortados."""
    small = {}
    for i in range(lo, hi + 1):
        k = key(frames[i])
        cx = int(torso_x(k))
        g = cv2.cvtColor(k[..., :3], cv2.COLOR_RGB2GRAY) * (k[..., 3] / 255)
        g = np.pad(g, ((0, 0), (400, 400)))[:, cx: cx + 800]
        small[i] = cv2.resize(g, (100, 90)).astype(np.float32)
    best = None
    for a in range(lo, hi - min_len + 1):
        for b in range(a + min_len, hi + 1):
            d = float(np.abs(small[a] - small[b]).mean())
            if best is None or d < best[0]:
                best = (d, a, b)
    return best


JOIN_GAP = 7       # px: peça grande a essa distância do corpo é do corpo (faixa verde da roupa)
JOIN_AREA = 800


def keep_largest(alpha):
    """Zera tudo fora da maior peça conectada (poeira, efeitos e restos soltos). Peça grande
    colada no corpo fica: roupa com faixa verde igual ao fundo (calça do p10) corta a perna,
    e a bota viraria uma peça solta."""
    n, labels, stats, _ = cv2.connectedComponentsWithStats((alpha > 0).astype(np.uint8))
    if n > 1:
        main = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
        near = cv2.dilate((labels == main).astype(np.uint8), np.ones((2 * JOIN_GAP + 1,) * 2, np.uint8))
        keep = {main} | {i for i in np.unique(labels[(near > 0) & (labels > 0)])
                         if stats[i, cv2.CC_STAT_AREA] >= JOIN_AREA}
        alpha[~np.isin(labels, list(keep))] = 0
    return alpha


def floor_dust(alpha, h, s, v):
    """Poeira levantada no chão: esverdeada (tingida pelo fundo) ou bege clara, só na faixa
    dos pés, onde não há cabelo nem pele. Tênis branco e roupa escura (sem essas cores) ficam."""
    rows = np.nonzero((alpha > 0).any(1))[0]
    if len(rows) == 0:
        return np.zeros_like(alpha, bool)
    top, bottom = rows.min(), rows.max()
    band = np.zeros_like(alpha, bool)
    band[bottom - (bottom - top) * 12 // 100:] = True
    tinted = (h > 35) & (h < 95) & (s >= 10) & (v >= 90)
    beige = (h >= 10) & (h <= 34) & (s >= 10) & (s < 45) & (v >= 120) & (v < 230)
    return band & (tinted | beige)


def key(frame, dust=False, effects=False, floor=False):
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    h, s, v = (hsv[..., i].astype(int) for i in range(3))
    bg = (h > 35) & (h < 95) & (s > 45)
    if dust:  # poeira bege dos pés (o jogo desenha os efeitos)
        bg |= (h >= 8) & (h <= 34) & (s >= 18) & (s <= 70) & (v >= 120)
    if effects:
        # Clarão de impacto: amarelo claro (a pele é mais alaranjada e menos clara).
        bg |= (h >= 22) & (h <= 40) & (v >= 200) & (s >= 40)
        # Anel de energia ciano-petróleo (o azul da roupa é mais anil, matiz >= 104).
        bg |= (h >= 85) & (h <= 102) & (s >= 120)
        # Poeira de impacto, brilho e arco de golpe claros, tingidos de verde pelo fundo.
        bg |= (h > 35) & (h < 95) & (s >= 14) & (v >= 140)
    alpha = np.where(bg, 0, 255).astype(np.uint8)
    if effects:
        # Rastro de golpe: faixa fina, clara e sem cor. Some na abertura morfológica,
        # enquanto calça, cabelo e faixa (largos) ficam.
        thick = cv2.morphologyEx(alpha, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (15, 15)))
        thick = cv2.dilate(thick, np.ones((5, 5), np.uint8))
        pale = (s < 60) & (v > 150)
        thin = (alpha > 0) & (thick == 0) & (pale | ((h >= 20) & (h <= 50)))
        # Rastro largo: claro e longe de qualquer parte colorida do corpo (pele, azul,
        # preto, vermelho). A calça branca fica perto da faixa azul, da faixa e do tênis.
        colored = ((alpha > 0) & ~pale).astype(np.uint8)
        body = cv2.dilate(colored, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (91, 91)))
        alpha[thin | ((alpha > 0) & pale & (body == 0))] = 0
        keep_largest(alpha)
    if floor:
        alpha[floor_dust(alpha, h, s, v)] = 0
        alpha = cv2.morphologyEx(alpha, cv2.MORPH_OPEN, np.ones((5, 5), np.uint8))
    alpha = keep_largest(cv2.morphologyEx(alpha, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8)))
    # Furinhos dentro do corpo (reflexo verde na roupa branca) voltam a ser opacos;
    # vãos grandes, como entre as pernas, continuam transparentes.
    _, labels, stats, _ = cv2.connectedComponentsWithStats((alpha == 0).astype(np.uint8))
    h_, w_ = alpha.shape
    x, y, w, h, area = stats.T
    fill = (area < 600) & (x > 0) & (y > 0) & (x + w < w_) & (y + h < h_)
    fill[0] = False
    alpha[fill[labels]] = 255
    alpha = cv2.GaussianBlur(alpha, (3, 3), 0)
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB).astype(int)
    rgb[..., 1] = np.minimum(rgb[..., 1], np.maximum(rgb[..., 0], rgb[..., 2]) + 8)  # tira o verde da borda
    return np.dstack([rgb.astype(np.uint8), alpha])


def torso_x(rgba):
    """Centro horizontal do tronco (faixa do ombro à cintura), estável durante a passada."""
    a = rgba[..., 3] > 128
    rows = np.nonzero(a.any(1))[0]
    top, bot = rows.min(), rows.max()
    band = a[top + (bot - top) * 15 // 100: top + (bot - top) * 45 // 100]
    return float(np.nonzero(band)[1].mean())


# Opções gravadas na receita do clipe (nome do argumento -> padrão).
RECIPE = dict(inicio=None, fim=None, passo=1, quadros=None, celula=256, largura=None,
              raiz="128,238", colunas=8, fixar="tronco", pe_no_chao=False, deslocar="0,0",
              sem_poeira=False, sem_efeitos=False, limpar_chao=False, ancorar_pe=False, reescala=None, limpar_quadros=None,
              aparar_borda=0)


def parser():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("video")
    p.add_argument("saida", nargs="?", help="folha de saída (padrão: o source do --clipe)")
    p.add_argument("--clipe", help="clipe do pipeline: lê e grava a receita em \"video\"")
    p.add_argument("--personagem", help="mede a escala pelas imagens iniciais (art/keys/<id>)")
    p.add_argument("--escala", type=float, help="escala manual, só quando a medida falha (exige --motivo)")
    p.add_argument("--motivo", help="por que a escala é manual")
    p.add_argument("--inicio", type=int)
    p.add_argument("--fim", type=int, help="quadro final, exclusivo")
    p.add_argument("--loop", help="A-B: escolhe o melhor loop dentro desse intervalo")
    p.add_argument("--min-loop", type=int, default=24)
    p.add_argument("--passo", type=int, default=1, help="usar 1 a cada N quadros")
    p.add_argument("--quadros", help="lista exata de quadros do vídeo, ex.: 10,14,18,22")
    p.add_argument("--celula", type=int, default=256, help="altura da célula")
    p.add_argument("--largura", type=int, help="largura da célula (padrão: igual à altura)")
    p.add_argument("--sem-poeira", action="store_true", help="remove poeira bege do chão")
    p.add_argument("--sem-efeitos", action="store_true", help="remove clarão de impacto e rastro de golpe")
    p.add_argument("--limpar-chao", action="store_true",
                   help="remove poeira cinza-esverdeada na faixa dos pés (seguro com cabelo claro e pele)")
    p.add_argument("--colunas", type=int, default=8)
    p.add_argument("--raiz", default="128,238")
    p.add_argument("--fixar", choices=["tronco", "quadro", "registro", "video"], default="tronco",
                   help="tronco: anula o deslizamento lateral; quadro: cada quadro no próprio tronco "
                        "(o jogo é que empurra, ex.: defesa); registro: idem, com o tronco medido como "
                        "na auditoria de harmonia (levantar, reações); video: mantém a posição do vídeo")
    p.add_argument("--pe-no-chao", action="store_true",
                   help="cada quadro com os pés na raiz (poses no ar: o jogo é que sobe e desce)")
    p.add_argument("--reescala", type=lambda v: [float(x) for x in v.split(",")],
                   help="fator de tamanho por quadro (tools/sprites/reescalar.py): divide a escala")
    p.add_argument("--limpar-quadros", type=lambda v: [int(x) for x in v.split(",")],
                   help="células a limpar de efeito depois de converter (tools/sprites/limpar.py)")
    p.add_argument("--ancorar-pe", action="store_true",
                   help="depois de converter, fixa o pé de apoio no lugar do idle (tools/sprites/ancorar.py)")
    p.add_argument("--aparar-borda", type=int, default=0,
                   help="apaga N px nas bordas de cima e dos lados de cada célula: o que passa da "
                        "célula máxima (chicote, capa) sai cortado limpo em vez de colado na borda")
    p.add_argument("--deslocar", default="0,0",
                   help="DX,DY em px da célula aplicado a todos os quadros (acerto fino de registro)")
    p.add_argument("--previa", help="GIF de prévia no tamanho do jogo")
    return p


def parse_args(argv=None):
    p = parser()
    known, _ = p.parse_known_args(argv)
    clip = None
    if known.clipe:
        clip = json.loads(Path(known.clipe).read_text(encoding="utf-8"))
        recipe = clip.get("video", {})
        keep = set(RECIPE) | {"personagem"} | ({"escala", "motivo"} if "motivo" in recipe else set())
        p.set_defaults(**{k: v for k, v in recipe.items() if k in keep})  # escala medida é medida de novo
    args = p.parse_args(argv)
    if args.saida is None:
        if clip is None:
            p.error("informe a saída ou --clipe")
        args.saida = str(ROOT / clip["source"])
    if args.escala is not None and not args.motivo:
        p.error("--escala manual exige --motivo (fica gravado no clipe)")
    if args.escala is None and not args.personagem:
        p.error("use --personagem <id> para medir a escala (ou --escala N --motivo '...')")
    return args, clip


def save_recipe(path, clip, args, start, end, measured, count, cw):
    recipe = {"arquivo": Path(args.video).name}
    if args.personagem:
        recipe["personagem"] = args.personagem
    recipe["escala"] = round(args.escala, 4) if args.escala is not None else round(measured["escala"], 4)
    if args.escala is not None:
        recipe["motivo"] = args.motivo
    else:
        recipe["medida"] = f"{measured['medida']} (nota {measured['nota']:.2f})"
    resolved = dict(vars(args), inicio=start, fim=end)
    if args.quadros:
        resolved.update(inicio=None, fim=None)
    recipe.update({k: resolved[k] for k, default in RECIPE.items() if resolved[k] != default})
    rx, ry = (int(v) for v in args.raiz.split(","))
    clip.update(expectedFrames=count, columns=args.colunas, video=recipe)
    for name, value, default in (("frameWidth", cw, 256), ("frameHeight", args.celula, 256),
                                 ("rootX", rx, 128), ("rootY", ry, 238)):
        if value != default:
            clip[name] = value
        else:
            clip.pop(name, None)
    Path(path).write_text(json.dumps(clip, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"receita gravada em {path}")


def registration_x(rgba):
    """Tronco como a auditoria de harmonia mede: mediana do meio de cada linha entre 30% e
    55% da altura da silhueta."""
    a = rgba[..., 3] > 10
    rows = np.nonzero(a.any(1))[0]
    top, h = rows.min(), rows.max() + 1 - rows.min()
    mids = []
    for y in range(top + int(h * 0.30), top + int(h * 0.55)):
        xs = np.nonzero(a[y])[0]
        if len(xs):
            mids.append((xs.min() + xs.max()) / 2)
    return float(np.median(mids))


def main(argv=None):
    args, clip = parse_args(argv)
    frames, fps = read_frames(args.video)
    if args.loop:
        lo, hi = (int(v) for v in args.loop.split("-"))
        d, start, end = best_loop(frames, lo, min(hi, len(frames) - 1), args.min_loop)
        print(f"loop {start}-{end} (diferença {d:.2f})")
    else:
        start, end = args.inicio or 0, args.fim or len(frames)
    picked = ([int(v) for v in args.quadros.split(",")] if args.quadros
              else list(range(start, end, args.passo)))
    cache = {}

    def keyed_frame(i, dust=False, effects=False, floor=False):
        if (i, dust, effects, floor) not in cache:
            cache[i, dust, effects, floor] = key(frames[i], dust, effects, floor)
        return cache[i, dust, effects, floor]

    measured = None
    if args.escala is not None:
        s = args.escala
        print(f"escala manual {s:.4f}: {args.motivo}")
    else:
        measured = tamanho.medir(frames, args.personagem, key)
        s = measured["escala"]
        print(f"escala {s:.4f} ({measured['medida']}, nota {measured['nota']:.2f}, zoom {measured['zoom']:.3f})")
    scale = s
    keyed = [keyed_frame(i, args.sem_poeira, args.sem_efeitos, args.limpar_chao) for i in picked]
    xs = np.array([torso_x(k) for k in keyed])
    if args.fixar == "tronco":
        # Remove só a tendência (o deslizamento); o balanço natural do corpo continua.
        t = np.arange(len(xs))
        trend = np.polyval(np.polyfit(t, xs, 1), t)
        offsets = -(trend - trend[0])
    elif args.fixar == "quadro":
        offsets = -(xs - xs[0])
    elif args.fixar == "registro":
        xs = np.array([registration_x(k) for k in keyed])
        offsets = -(xs - xs[0])
    else:
        offsets = np.zeros(len(xs))
    # Chão e posição de referência vêm do 1º quadro do vídeo (a imagem inicial): poses no
    # ar ficam acima do chão e todos os clipes do personagem se alinham entre si.
    first = keyed_frame(0, args.sem_poeira)
    foot_y = int(np.nonzero((first[..., 3] > 128).any(1))[0].max())
    ref_x = torso_x(first) if args.fixar == "video" else xs[0]
    rx, ry = (int(v) for v in args.raiz.split(","))
    dx, dy = (int(v) for v in args.deslocar.split(","))
    rx, ry = rx + dx, ry + dy
    cell, cols = args.celula, args.colunas
    cw = args.largura or cell
    rows = (len(keyed) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cw, rows * cell), (0, 0, 0, 0))
    if args.reescala and len(args.reescala) != len(keyed):
        raise SystemExit(f"--reescala tem {len(args.reescala)} fatores para {len(keyed)} quadros")
    for n, (k, off) in enumerate(zip(keyed, offsets)):
        s = scale / args.reescala[n] if args.reescala else scale
        img = Image.fromarray(k, "RGBA")
        img = img.resize((round(img.width * s), round(img.height * s)), Image.LANCZOS)
        x = rx - round((ref_x - off) * s)
        bottom = int(np.nonzero((k[..., 3] > 128).any(1))[0].max()) if args.pe_no_chao else foot_y
        y = ry - round(bottom * s)
        # Cada quadro fica na própria célula: o que passa da borda é cortado, não invade a vizinha.
        box = Image.new("RGBA", (cw, cell), (0, 0, 0, 0))
        box.paste(img, (x, y))
        if args.aparar_borda:
            a = np.array(box)
            b = args.aparar_borda
            a[:b, :, 3] = a[:, :b, 3] = a[:, -b:, 3] = 0
            box = Image.fromarray(a, "RGBA")
        sheet.paste(box, ((n % cols) * cw, (n // cols) * cell))
    sheet.save(args.saida)
    print(f"{len(keyed)} quadros, {cols}×{rows}, {fps:.1f} fps de origem → {args.saida}")
    if clip is not None:
        save_recipe(args.clipe, clip, args, start, end, measured, len(keyed), cw)
        pack_id = "player_base" if args.personagem == "p01" else args.personagem
        atlas = Path(clip["output"]).stem
        in_place = Path(args.saida).resolve() == (ROOT / clip["source"]).resolve()
        if args.ancorar_pe and in_place:
            import ancorar
            off = ancorar.anchor(pack_id, atlas)
            print("pé de apoio fixado: " + " ".join(f"{d:+d}" for d in off))
        if args.limpar_quadros and in_place:
            import limpar
            limpar.clean_atlas(pack_id, atlas, args.limpar_quadros)
        sheet = Image.open(args.saida)
        cw = json.loads(Path(args.clipe).read_text(encoding="utf-8")).get("frameWidth", cw)
    if args.previa:
        gif = []
        for n in range(len(keyed)):
            c = sheet.crop(((n % cols) * cw, (n // cols) * cell, (n % cols + 1) * cw, (n // cols + 1) * cell))
            bg = Image.new("RGBA", (cw, cell), (60, 60, 80, 255))
            bg.alpha_composite(c)
            gif.append(bg.convert("RGB").resize((cw * 2, cell * 2), Image.LANCZOS))
        gif[0].save(args.previa, save_all=True, append_images=gif[1:], duration=round(1000 * args.passo / fps), loop=0)


if __name__ == "__main__":
    main()
