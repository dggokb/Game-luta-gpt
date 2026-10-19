#!/usr/bin/env python3
"""Monta o cenário do Porto Secreto (Ilha do Torneio) a partir das imagens geradas no GPT.

    python tools/stage/montar_porto.py --comum pasta/torneio --entrada pasta/torneio/porto_secreto

`--comum` é a pasta das peças iguais em toda a ilha (ceu, mar, penhascos; veja
tools/stage/ilha-do-torneio.md) e `--entrada` a do porto. Além do que o
preparar_cenario.py faz (cortar margens transparentes, alfa opaco, JPG, 2048 px):

- Piso: o chao_cais.png vira a peça que se repete, com as bordas esquerda e direita
  fundidas (a emenda da repetição some). O chao.jpg é o mesmo cais com o emblema do
  chao.png colado no centro, com borda suave: o GPT refez o concreto na edição, então só o
  emblema vem do chao.png (as faixas amarelas ficaram nas mesmas linhas nas duas).
- Grade: o GPT desenhou a lateral do cais (concreto e pneus) embaixo do guarda-corpo. Ela
  fica de costas para a câmera e é cortada; sobra o guarda-corpo em cima da borda do cais.
- Sem primeiro plano: o frente_esq.png (caixas) e o frente_dir.png (cabeço e luminária)
  apareciam soltos no meio do cais com a câmera afastada. São ignorados.
- Ilha e penhascos: recebem a névoa da distância (`afastar`: menos cor e contraste,
  puxando para o azul-arroxeado do céu); sem ela pareciam uma maquete. A base fica sólida
  (`solidificar`): a névoa do pé, meio transparente, deixava ver os rochedos do fundo. A
  ilha não é espelhada: o lado cortado dela fica sempre fora da tela (x no stage.json).
- Poste: o estandarte vira bandeira_poste.png, para balançar ao vento (separar_bandeiras.py).

Grava em android/app/src/main/assets/stages/porto_secreto/ sem mexer no stage.json.
"""

import argparse
import sys
import tempfile
from pathlib import Path

try:
    import numpy as np
    from PIL import Image, ImageDraw, ImageFilter
except ImportError:
    sys.exit("Instale o Pillow e o numpy: pip install pillow numpy")

sys.path.insert(0, str(Path(__file__).resolve().parent))
from preparar_cenario import EXTENSOES, SAIDA_PADRAO, preparar  # noqa: E402
from montar_brucy import sem_emenda  # noqa: E402
import separar_bandeiras  # noqa: E402

ID = "porto_secreto"
COMUNS = ("ceu", "mar", "penhascos")
# Emblema no chao.png: centro e raio (até as pontas da estrela) em fração da largura.
EMBLEMA_X, EMBLEMA_Y, EMBLEMA_R = 843 / 1672, 441 / 941, 272 / 1672
# Fração da altura da grade.png onde começa a lateral do cais (cortada).
CORTE_GRADE = 410 / 724


def montar_piso(chao, cais):
    w, h = cais.size
    base = sem_emenda(cais)
    bw, bh = base.size
    raio = EMBLEMA_R * w
    cx, cy = EMBLEMA_X * w, EMBLEMA_Y * h
    lado = int(raio * 2.5)
    pedaco = chao.convert("RGB").resize(cais.size).crop((int(cx - lado / 2), int(cy - lado / 2),
                                                       int(cx + lado / 2), int(cy + lado / 2)))
    mascara = Image.new("L", pedaco.size, 0)
    margem = (lado - raio * 2.1) / 2
    ImageDraw.Draw(mascara).ellipse((margem, margem, lado - margem, lado - margem), fill=255)
    mascara = mascara.filter(ImageFilter.GaussianBlur(raio * 0.08))
    com = base.copy()
    com.paste(pedaco, (int(bw / 2 - lado / 2), int(cy - lado / 2)), mascara)
    print(f"piso: {w}x{h} -> {bw}x{bh}, emblema no centro ({raio:.0f} px de raio)")
    return com, base


def esfumar_ilha(img, lados=0.12, baixo=0.14):
    """Apaga aos poucos as bordas esquerda e direita e a faixa de névoa de baixo."""
    px = np.asarray(img.convert("RGBA")).astype(np.float32)
    h, w = px.shape[:2]
    n = int(w * lados)
    if n > 0:
        rampa = np.linspace(0.0, 1.0, n) ** 1.5
        px[:, :n, 3] *= rampa
        px[:, w - n:, 3] *= rampa[::-1]
    m = int(h * baixo)
    if m > 0:
        px[h - m:, :, 3] *= np.linspace(1.0, 0.0, m)[:, None] ** 1.2
    return Image.fromarray(px.clip(0, 255).astype(np.uint8))


# Cor da distância: o azul-arroxeado do céu perto do horizonte, à noite.
NEVOA = (88, 80, 140)
# Peças comuns que ficam longe e recebem a névoa da distância (força de 0 a 1).
COMUNS_LONGE = {"penhascos": 0.45}


def afastar(img, forca, cor=NEVOA):
    """Profundidade atmosférica: o que está longe perde cor e contraste e puxa para a cor do
    céu. Sem isso, as peças do fundo da ilha (cheias de cor e de detalhe, como as de perto)
    pareciam uma maquete colada atrás do cenário."""
    px = np.asarray(img.convert("RGBA")).astype(np.float32)
    rgb = px[..., :3]
    cinza = (rgb * np.array([0.299, 0.587, 0.114], np.float32)).sum(axis=2, keepdims=True)
    rgb = rgb + (cinza - rgb) * (0.4 * forca)
    rgb = rgb + (np.array(cor, np.float32) - rgb) * forca
    px[..., :3] = rgb
    return Image.fromarray(px.clip(0, 255).astype(np.uint8))


def solidificar(img):
    """Base sólida: abaixo do contorno de cada coluna, a névoa do pé da montanha vira opaca
    (misturada com a cor da própria névoa). Meio transparente, ela deixava ver os rochedos
    do fundo por baixo da montanha da frente."""
    px = np.asarray(img.convert("RGBA")).astype(np.float32)
    h, w = px.shape[:2]
    a = px[..., 3] / 255.0
    base = px[int(h * 0.85):]
    opaco = base[..., 3] > 200
    nevoa = np.median(base[opaco][:, :3], axis=0) if opaco.any() else np.array(NEVOA, np.float32)
    dentro = np.cumsum(a > 0.5, axis=0) > 0
    rgb = px[..., :3] * a[..., None] + nevoa * (1 - a[..., None])
    px[..., :3] = np.where(dentro[..., None], rgb, px[..., :3])
    px[..., 3] = np.where(dentro, 255.0, px[..., 3])
    return Image.fromarray(px.clip(0, 255).astype(np.uint8))


def afinar_pontas(img, fracao=0.3):
    """Faixa de montanhas cortada nas duas bordas (os penhascos): nas pontas, cada coluna é
    achatada em direção à base, e as montanhas vão baixando até sumir na água, em vez de
    acabar num corte reto."""
    px = np.asarray(img.convert("RGBA"))
    h, w = px.shape[:2]
    n = int(w * fracao)
    saida = px.copy()
    for x in list(range(n)) + list(range(w - n, w)):
        k = (x + 0.5) / n if x < n else (w - x - 0.5) / n
        k = 0.12 + 0.88 * (k * k * (3 - 2 * k))
        alto = max(1, round(h * k))
        coluna = Image.fromarray(px[:, x:x + 1]).resize((1, alto), Image.BILINEAR)
        saida[:, x] = 0
        saida[h - alto:, x:x + 1] = np.asarray(coluna)
    return Image.fromarray(saida)


def textura_mar(mar):
    """Textura do mar vista de cima (mar_plano.jpg), para a água deitada em perspectiva (a
    camada "agua" com "imagem", veja docs/stage-engine.md). Provisória: as ondas de perto do
    mar.png, do lado sem o caminho do sol, esticadas na vertical (na foto elas estão
    achatadas pela perspectiva) e com as bordas fundidas nas duas direções, para repetir."""
    from montar_brucy import sem_emenda
    w, h = mar.size
    faixa = mar.convert("RGB").crop((round(w * 0.42), round(h * 0.745), w, h))
    faixa = faixa.resize((faixa.width, round(faixa.height * 2.2)), Image.BICUBIC)
    faixa = sem_emenda(faixa)
    return sem_emenda(faixa.transpose(Image.TRANSPOSE)).transpose(Image.TRANSPOSE)


def plateia(img, faixas):
    """Só a gente da arquibancada (o resto transparente), para a plateia viva do motor
    ("plateia" com "mascara" no stage.json): o pulo mexe só esses pixels, e os estandartes,
    as luzes e as grades que passam pela faixa ficam parados. Gente = textura miúda (muita
    variação de um pixel para o outro) e escura, fora do vermelho liso dos estandartes; só
    dentro das faixas (topo, base em fração da altura)."""
    from PIL import ImageFilter
    img = img.convert("RGBA")
    px = np.asarray(img).astype(np.float32)
    r, g, b, a = px[..., 0], px[..., 1], px[..., 2], px[..., 3]
    lum = 0.3 * r + 0.59 * g + 0.11 * b
    mx, mn = px[..., :3].max(axis=2), px[..., :3].min(axis=2)
    sat = (mx - mn) / np.maximum(mx, 1)
    borrar = lambda m, raio: np.asarray(Image.fromarray(m.clip(0, 255).astype(np.uint8)).filter(
        ImageFilter.BoxBlur(raio))).astype(np.float32)
    media = borrar(lum, 3)
    var = np.sqrt(np.maximum(borrar(lum * lum / 255, 3) * 255 - media * media, 0))
    estandarte = (r > 110) & (sat > 0.55) & (g < 90)
    gente = (var > 13) & (borrar(lum, 6) < 120) & ~estandarte & (a > 200)
    h = px.shape[0]
    dentro = np.zeros_like(gente)
    for topo, base in faixas:
        dentro[int(topo * h):int(base * h)] = True
    m = Image.fromarray(((gente & dentro) * 255).astype(np.uint8))
    m = m.filter(ImageFilter.MaxFilter(7)).filter(ImageFilter.MinFilter(9)).filter(ImageFilter.MedianFilter(5))
    m = m.filter(ImageFilter.GaussianBlur(0.8))
    saida = img.copy()
    saida.putalpha(Image.fromarray(np.minimum(np.asarray(m), a.astype(np.uint8))))
    return saida


def peca_comum(nome, img):
    """Uma peça comum da ilha como ela entra no cenário (as de longe com a névoa)."""
    if nome == "penhascos":
        img = solidificar(afinar_pontas(img))
    if nome in COMUNS_LONGE:
        return afastar(img, COMUNS_LONGE[nome])
    return img


def imagens(pasta):
    return {p.stem: p for p in pasta.iterdir() if p.suffix.lower() in EXTENSOES}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--entrada", required=True, type=Path, help="pasta do porto_secreto")
    parser.add_argument("--comum", required=True, type=Path, help="pasta das peças comuns da ilha")
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO / ID)
    args = parser.parse_args()

    entrada = imagens(args.entrada)
    comum = imagens(args.comum)
    # No Drive o mar veio como "maar.png".
    if "mar" not in comum and "maar" in comum:
        comum["mar"] = comum.pop("maar")
    for falta in ("chao", "chao_cais", "grade", "poste"):
        if falta not in entrada:
            sys.exit(f"faltou {falta}.png em {args.entrada}")
    for falta in COMUNS:
        if falta not in comum:
            sys.exit(f"faltou {falta}.png em {args.comum}")
    args.saida.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        com, sem = montar_piso(Image.open(entrada["chao"]), Image.open(entrada["chao_cais"]))
        com.save(tmp / "chao.png")
        sem.save(tmp / "chao_cais.png")
        for nome in COMUNS:
            peca_comum(nome, Image.open(comum[nome])).save(tmp / f"{nome}.png")
        textura_mar(Image.open(comum["mar"])).save(tmp / "mar_plano.png")
        for nome, arquivo in entrada.items():
            if nome in ("chao", "chao_cais"):
                continue
            if nome in ("frente_esq", "frente_dir"):
                print(f"{arquivo.name} ignorado: o porto fica sem primeiro plano")
                continue
            img = Image.open(arquivo)
            if nome == "ilha":
                img = afastar(solidificar(img), 0.4)
            if nome == "grade":
                img = img.convert("RGBA")
                img = img.crop((0, 0, img.width, round(img.height * CORTE_GRADE)))
            img.save(tmp / f"{nome}.png")
        for arquivo in sorted(tmp.iterdir()):
            saida, tamanho = preparar(arquivo, args.saida)
            print(f"{arquivo.name} -> {saida.name} {tamanho[0]}x{tamanho[1]}")
    separar_bandeiras.STAGES = args.saida.parent
    print("bandeira do poste", separar_bandeiras.separar(ID))


if __name__ == "__main__":
    main()
