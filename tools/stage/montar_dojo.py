#!/usr/bin/env python3
"""Monta o cenário do Dojo Shinshinkan a partir das imagens geradas no GPT.

    python tools/stage/montar_dojo.py --entrada pasta/com/as/imagens

Além do que o preparar_cenario.py faz (cortar margens transparentes, JPG, 2048 px):

- Piso: as tábuas do GPT vieram levemente em perspectiva (abrem em leque para baixo).
  Cada linha é esticada para as tábuas ficarem paralelas e da mesma largura; o assoalho
  é montado tábua por tábua (sorteadas entre as do GPT), com ~70 unidades do mundo cada,
  e o brasão do chao.png entra embaixo dos lutadores (profundidade 0). Gera chao.jpg (com
  brasão) e chao_tabuas.jpg (sem), com as tábuas nas mesmas posições.
- Primeiro plano: frente.png é separado em frente_esq.png (cerca) e frente_dir.png
  (suporte de bokken) e desfocado, como na página do mangá.

Grava em android/app/src/main/assets/stages/dojo_shinshinkan/ sem mexer no stage.json.
"""

import argparse
import shutil
import sys
import tempfile
from pathlib import Path

try:
    import numpy as np
    from PIL import Image, ImageFilter
except ImportError:
    sys.exit("Instale o Pillow e o numpy: pip install pillow numpy")

sys.path.insert(0, str(Path(__file__).resolve().parent))
from preparar_cenario import EXTENSOES, SAIDA_PADRAO, preparar  # noqa: E402

ID = "dojo_shinshinkan"

# Piso no stage.json: profundidades das bordas e x do centro.
PISO_PERTO = -600.0
PISO_LONGE = 1100.0
# Piso final: TABUAS tábuas de TABUA_PX px (900 px = 1700 unidades de profundidade,
# então cada tábua tem ~68 unidades do mundo). TABUAS_POR_COPIA é quantas tábuas
# inteiras sobram na imagem do GPT depois de endireitar.
PISO_ALTURA = 900
TABUA_PX = 36
TABUAS = 56
TABUAS_POR_COPIA = 14


def juntas(cinza, y, meio=20):
    """X das juntas escuras entre tábuas na faixa da linha y."""
    perfil = cinza[max(0, y - meio):y + meio].mean(0)
    achadas = []
    for x in range(3, len(perfil) - 3):
        vizinhos = perfil[max(0, x - 20):x + 20]
        if perfil[x] == perfil[x - 3:x + 4].min() and perfil[x] < np.median(vizinhos) - 20:
            achadas.append(x)
    return np.array(achadas)


def medir_leque(img):
    """Junta central e largura da tábua (em px) no topo e na base da imagem."""
    cinza = np.asarray(img.convert("L")).astype(float)
    h, w = cinza.shape
    meio = juntas(cinza, h // 2)
    centro = meio[np.argmin(np.abs(meio - w / 2))]
    larguras = []
    for y in (40, h - 40):
        j = juntas(cinza, y)
        perto = j[np.argmin(np.abs(j - centro))]
        lados = j[(np.abs(j - perto) > 50) & (np.abs(j - perto) < 120)]
        larguras.append(float(np.median(np.abs(lados - perto))))
    return float(centro), larguras[0], larguras[1]


def endireitar(img, centro, tabua_topo, tabua_base):
    """Estica cada linha para todas as tábuas ficarem paralelas, com a largura da base."""
    src = np.asarray(img.convert("RGB")).astype(np.float32)
    h, w, _ = src.shape
    alvo = tabua_base
    largura = int(round(TABUAS_POR_COPIA * alvo))
    novo_centro = largura / 2.0
    xs = np.arange(largura, dtype=np.float32)
    saida = np.empty((h, largura, 3), np.float32)
    for y in range(h):
        tabua = tabua_topo + (tabua_base - tabua_topo) * y / (h - 1)
        sx = np.clip(centro + (xs - novo_centro) * tabua / alvo, 0, w - 1)
        x0 = np.floor(sx).astype(int)
        x1 = np.minimum(x0 + 1, w - 1)
        t = (sx - x0)[:, None]
        saida[y] = src[y, x0] * (1 - t) + src[y, x1] * t
    return Image.fromarray(np.clip(saida, 0, 255).astype(np.uint8))


def mascara_brasao(com, sem):
    """Círculo (esfumado) que cobre o brasão: onde as duas imagens diferem."""
    a = np.asarray(com.convert("RGB")).astype(float)
    b = np.asarray(sem.convert("RGB")).astype(float)
    dif = Image.fromarray(np.clip(np.abs(a - b).sum(2), 0, 255).astype(np.uint8))
    dif = np.asarray(dif.filter(ImageFilter.GaussianBlur(6)))
    ys, xs = np.nonzero(dif > 60)
    cx, cy = (xs.min() + xs.max()) / 2, (ys.min() + ys.max()) / 2
    raio = max(xs.max() - xs.min(), ys.max() - ys.min()) / 2 + 14
    h, w = dif.shape
    yy, xx = np.mgrid[0:h, 0:w]
    dist = np.hypot(xx - cx, yy - cy)
    alfa = np.clip((raio - dist) / 18.0, 0, 1) * 255
    return Image.fromarray(alfa.astype(np.uint8)), (cx, cy)


def montar_piso(chao, tabuas, destino):
    centro, topo, base = medir_leque(tabuas)
    print(f"piso: junta central x={centro:.0f}, tábua {topo:.1f} px no topo e {base:.1f} px na base")
    reta_com = endireitar(chao, centro, topo, base)
    reta_sem = endireitar(tabuas, centro, topo, base)
    mascara, (bx, by) = mascara_brasao(reta_com, reta_sem)

    # Assoalho tábua por tábua: cada tábua é sorteada entre as do GPT, com deslocamento
    # vertical (a emenda vira o topo de uma tábua), às vezes de ponta-cabeça e com um
    # tom um pouco diferente. Repetir a imagem inteira deixava brilhos e veios em grade.
    escala = TABUA_PX / base
    largura = TABUAS * TABUA_PX
    fonte = np.asarray(reta_sem.resize((TABUAS_POR_COPIA * TABUA_PX, PISO_ALTURA), Image.LANCZOS)).astype(np.float32)
    sorteio = np.random.default_rng(7)
    piso = np.empty((PISO_ALTURA, largura, 3), np.float32)
    anterior = -1
    for i in range(TABUAS):
        k = sorteio.integers(TABUAS_POR_COPIA)
        while k == anterior:
            k = sorteio.integers(TABUAS_POR_COPIA)
        anterior = k
        tabua = fonte[:, k * TABUA_PX:(k + 1) * TABUA_PX]
        if sorteio.random() < 0.5:
            tabua = tabua[::-1]
        corte = int(sorteio.integers(PISO_ALTURA // 5, PISO_ALTURA * 4 // 5))
        tabua = np.roll(tabua, corte, axis=0) * sorteio.uniform(0.9, 1.06)
        tabua[max(0, corte - 1):corte + 1, 1:-1] *= 0.6
        piso[:, i * TABUA_PX:(i + 1) * TABUA_PX] = tabua
    sem = Image.fromarray(np.clip(piso, 0, 255).astype(np.uint8))

    # Brasão embaixo dos lutadores (centro da imagem, profundidade 0), com as juntas
    # alinhadas às do assoalho.
    tw, th = round(reta_com.width * escala), round(reta_com.height * escala)
    peca = reta_com.resize((tw, th), Image.LANCZOS)
    peca_mascara = mascara.resize((tw, th), Image.LANCZOS)
    alvo_y = PISO_LONGE / (PISO_LONGE - PISO_PERTO) * PISO_ALTURA
    ox = round((largura / 2 - bx * escala) / TABUA_PX) * TABUA_PX
    oy = round(alvo_y - by * escala)
    com = sem.copy()
    com.paste(peca, (ox, oy), peca_mascara)

    com.save(destino / "chao.jpg", quality=88)
    sem.save(destino / "chao_tabuas.jpg", quality=88)
    unidades = PISO_LONGE - PISO_PERTO
    caixa = peca_mascara.getbbox()
    print(f"piso: {largura}x{PISO_ALTURA}, tábua ≈ {TABUA_PX * unidades / PISO_ALTURA:.0f} e "
          f"brasão ≈ {(caixa[2] - caixa[0]) * unidades / PISO_ALTURA:.0f} unidades do mundo, "
          f"centro do brasão em x = {(ox + bx * escala) / largura:.3f} da imagem")


def separar_frente(img, pasta):
    """Separa a cerca (esquerda) e o suporte (direita) e desfoca."""
    img = img.convert("RGBA")
    w, h = img.size
    for nome, caixa in (("frente_esq", (0, 0, w // 2, h)), ("frente_dir", (w // 2, 0, w, h))):
        parte = img.crop(caixa)
        parte = parte.crop(parte.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox())
        borda = 24
        fundo = Image.new("RGBA", (parte.width + 2 * borda, parte.height + 2 * borda), (0, 0, 0, 0))
        fundo.paste(parte, (borda, borda))
        # Desfoque com alfa pré-multiplicado, para não aparecer halo claro em volta.
        rgb = np.asarray(fundo).astype(np.float32)
        alfa = rgb[..., 3:] / 255.0
        pre = Image.fromarray(np.clip(np.concatenate([rgb[..., :3] * alfa, rgb[..., 3:]], 2), 0, 255).astype(np.uint8))
        pre = np.asarray(pre.filter(ImageFilter.GaussianBlur(7))).astype(np.float32)
        a = np.maximum(pre[..., 3:], 1) / 255.0
        out = np.concatenate([np.clip(pre[..., :3] / a, 0, 255), pre[..., 3:]], 2)
        Image.fromarray(out.astype(np.uint8)).save(pasta / f"{nome}.png")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--entrada", required=True, type=Path)
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO / ID)
    args = parser.parse_args()

    entrada = {p.stem: p for p in args.entrada.iterdir() if p.suffix.lower() in EXTENSOES}
    for falta in ("chao", "chao_tabuas", "frente"):
        if falta not in entrada:
            sys.exit(f"faltou {falta}.png em {args.entrada}")
    args.saida.mkdir(parents=True, exist_ok=True)

    montar_piso(Image.open(entrada["chao"]), Image.open(entrada["chao_tabuas"]), args.saida)

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        separar_frente(Image.open(entrada["frente"]), tmp)
        for nome, arquivo in entrada.items():
            if nome not in ("chao", "chao_tabuas", "frente"):
                shutil.copy(arquivo, tmp / arquivo.name)
        for arquivo in sorted(tmp.iterdir()):
            saida, tamanho = preparar(arquivo, args.saida)
            print(f"{arquivo.name} -> {saida.name} {tamanho[0]}x{tamanho[1]}")


if __name__ == "__main__":
    main()
