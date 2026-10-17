#!/usr/bin/env python3
"""Monta o cenário do Dojo da Yin-Lu (連武館) a partir das imagens geradas no GPT.

    python tools/stage/montar_yinlu.py --entrada pasta/com/as/imagens

Além do que o preparar_cenario.py faz (cortar margens transparentes, JPG, 2048 px):

- Piso: chao.png e chao_lajotas.png vieram de cima, com as colunas de lajotas retas, mas a
  largura da imagem não é um número inteiro de colunas (a emenda da repetição ficava com
  uma coluna larga demais). As duas são cortadas nas juntas das colunas, simétricas em
  volta do emblema, e gravadas como chao.jpg e chao_lajotas.jpg.
- Primeiro plano: frente.png é separado em frente_esq.png (degraus quebrados) e
  frente_dir.png (pedra com arbusto) e desfocado, como no Shūshinkan. O lado que o GPT
  cortou na borda da imagem é esfumado, porque com a câmera afastada essas pedras
  aparecem no meio do piso.
- Lanterna de pedra: o nicho da luz veio vazado (transparente) e mostrava o cenário de
  trás. O vão é preenchido com o interior escuro da pedra; a luz é do jogo.
- Portão e pilar: o GPT gravou o alfa em 254 (ficavam um pouco transparentes); o
  preparar_cenario.py agora deixa opaco.
- Nomes: "lanterna de pedra.png" vira lanterna_pedra.png.

Grava em android/app/src/main/assets/stages/dojo_yinlu/ sem mexer no stage.json.
"""

import argparse
import shutil
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
from montar_dojo import separar_frente  # noqa: E402

ID = "dojo_yinlu"
NOMES = {"lanterna de pedra": "lanterna_pedra", "lanterna_de_pedra": "lanterna_pedra"}
COR_NICHO = (58, 50, 44)


def juntas_colunas(img):
    """X das juntas verticais (escuras) entre as colunas de lajotas, iguais no topo e na base."""
    cinza = np.asarray(img.convert("L")).astype(np.float32)
    h, w = cinza.shape
    achadas = None
    for y in (h // 16, h - h // 16):
        perfil = cinza[max(0, y - 40):y + 40].mean(0)
        xs = {x for x in range(4, w - 4)
              if perfil[x] == perfil[x - 4:x + 5].min()
              and perfil[x] < np.median(perfil[max(0, x - 40):x + 40]) - 20}
        achadas = xs if achadas is None else {x for x in achadas if any(abs(x - a) <= 2 for a in xs)}
    return sorted(achadas)


def cortar_piso(chao, lajotas):
    """Corta as duas imagens na primeira e na última junta, mantendo o emblema no meio."""
    juntas = juntas_colunas(lajotas)
    # Junta de 1-2 px: descarta as que caem grudadas na anterior.
    limpas = [x for i, x in enumerate(juntas) if i == 0 or x - juntas[i - 1] > 20]
    if len(limpas) < 4:
        sys.exit("não achei as juntas das colunas de lajotas no chao_lajotas")
    # O par de juntas mais afastado com o meio no centro do emblema (meio da imagem).
    centro = chao.width / 2
    pares = [(b - a, a, b) for a in limpas for b in limpas if b > a and abs((a + b) / 2 - centro) <= 8]
    if not pares:
        sys.exit("não achei duas juntas simétricas em volta do emblema")
    _, x0, x1 = max(pares)
    print(f"piso: colunas de ~{np.diff(limpas).mean():.0f} px, corte x {x0}..{x1} ({x1 - x0} px)")
    return chao.crop((x0, 0, x1, chao.height)), lajotas.crop((x0, 0, x1, lajotas.height))


def preencher_vaos(img, cor):
    """Pinta os buracos transparentes cercados pela imagem (não ligados à borda)."""
    img = img.convert("RGBA")
    alfa = img.getchannel("A")
    vazio = alfa.point(lambda a: 255 if a <= 8 else 0)
    w, h = img.size
    fora = Image.new("L", (w + 2, h + 2), 255)
    fora.paste(vazio, (1, 1))
    ImageDraw.floodfill(fora, (0, 0), 128)
    fora = np.asarray(fora)[1:-1, 1:-1]
    vaos = (np.asarray(vazio) == 255) & (fora != 128)
    # Só o maior vão (o nicho da luz); frestas entre pedras e folhas continuam vazadas.
    vao = maior_regiao(vaos)
    if vao is None or vao.sum() < 0.002 * w * h:
        return img
    px = np.asarray(img).copy()
    ys, xs = np.where(vao)
    y0, y1 = ys.min(), ys.max()
    # Degradê: mais escuro em cima (sombra do telhado), um pouco mais quente embaixo.
    t = ((ys - y0) / max(1, y1 - y0))[:, None]
    base = np.array(cor, dtype=np.float32)
    px[ys, xs, :3] = np.clip(base * (0.7 + 0.45 * t), 0, 255).astype(np.uint8)
    px[ys, xs, 3] = 255
    # A borda semitransparente em volta do vão também fica opaca, sem halo.
    perto = vao.copy()
    for dy in (-2, -1, 1, 2):
        perto |= np.roll(vao, dy, 0)
    for dx in (-2, -1, 1, 2):
        perto |= np.roll(perto, dx, 1)
    px[perto, 3] = 255
    print(f"vão preenchido: {len(xs)} px (y {y0}..{y1})")
    return Image.fromarray(px)


def maior_regiao(mascara):
    """Maior região ligada (4 vizinhos) de uma máscara booleana, por preenchimento."""
    marcas = Image.fromarray(np.where(mascara, 255, 0).astype(np.uint8))
    # As frestas pequenas (entre pedras e folhas) somem numa erosão de 21 px; o que
    # sobra são sementes dos vãos grandes, preenchidos depois na máscara original.
    sementes = np.asarray(marcas.filter(ImageFilter.MinFilter(21))) == 255
    melhor, tamanho = None, 0
    while True:
        ys, xs = np.where(sementes)
        if len(xs) == 0:
            return melhor
        regiao_img = marcas.copy()
        ImageDraw.floodfill(regiao_img, (int(xs[0]), int(ys[0])), 128)
        regiao = np.asarray(regiao_img) == 128
        if regiao.sum() > tamanho:
            melhor, tamanho = regiao, regiao.sum()
        sementes &= ~regiao


def esfumar_borda(arquivo, lado, fracao=0.18):
    """Esfuma o lado em que o GPT cortou as pedras na borda da imagem: quando a câmera
    afasta, o primeiro plano aparece no meio do piso e o corte reto ficava à mostra."""
    img = Image.open(arquivo).convert("RGBA")
    px = np.asarray(img).astype(np.float32)
    w = px.shape[1]
    largura = max(1, int(w * fracao))
    rampa = np.linspace(0.0, 1.0, largura) ** 1.5
    if lado == "direita":
        rampa = rampa[::-1]
        px[:, w - largura:, 3] *= rampa
    else:
        px[:, :largura, 3] *= rampa
    Image.fromarray(px.clip(0, 255).astype(np.uint8)).save(arquivo)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--entrada", required=True, type=Path)
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO / ID)
    args = parser.parse_args()

    entrada = {}
    for p in args.entrada.iterdir():
        if p.suffix.lower() in EXTENSOES:
            entrada[NOMES.get(p.stem, p.stem)] = p
    for falta in ("chao", "chao_lajotas", "frente"):
        if falta not in entrada:
            sys.exit(f"faltou {falta}.png em {args.entrada}")
    args.saida.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        chao, lajotas = cortar_piso(Image.open(entrada["chao"]).convert("RGB"),
                                    Image.open(entrada["chao_lajotas"]).convert("RGB"))
        chao.save(tmp / "chao.png")
        lajotas.save(tmp / "chao_lajotas.png")
        separar_frente(Image.open(entrada["frente"]), tmp)
        esfumar_borda(tmp / "frente_esq.png", "esquerda")
        esfumar_borda(tmp / "frente_dir.png", "direita")
        for nome, arquivo in entrada.items():
            if nome in ("chao", "chao_lajotas", "frente"):
                continue
            if nome == "lanterna_pedra":
                preencher_vaos(Image.open(arquivo), COR_NICHO).save(tmp / f"{nome}.png")
            else:
                shutil.copy(arquivo, tmp / f"{nome}{arquivo.suffix.lower()}")
        for arquivo in sorted(tmp.iterdir()):
            saida, tamanho = preparar(arquivo, args.saida)
            print(f"{arquivo.name} -> {saida.name} {tamanho[0]}x{tamanho[1]}")


if __name__ == "__main__":
    main()
