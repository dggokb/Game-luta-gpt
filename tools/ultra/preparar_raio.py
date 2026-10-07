#!/usr/bin/env python3
"""Prepara as imagens do raio final do ultra (geradas com fundo preto).

O GPT desenha brilho com fundo transparente mal; com fundo preto fica bom. Este script
transforma o preto em transparência mantendo o brilho suave (o pixel vira a cor "cheia"
com a transparência proporcional à luz), recorta, centraliza e reduz:

- corpo:   feixe horizontal. Fica centrado no núcleo e sem emenda nas bordas (repete).
- ponta:   frente do feixe (opcional). Use --espelhar-ponta se a frente aponta para a esquerda.
- inicio:  esfera nas mãos. O centro da esfera vai para o ultra.json.
- impacto: explosão no alvo.
- aura, vento, poeira: folhas de animação (quadros lado a lado). Aura e vento com fundo
  preto; poeira com fundo transparente (fumaça escura sumiria no preto).

Uso (cada imagem é opcional; o que não for passado continua como está no ultra.json):
    python tools/ultra/preparar_raio.py --id player_base --corpo corpo.png \
        --ponta ponta.png --espelhar-ponta --inicio inicio.png --impacto impacto.png \
        --aura aura.png --vento vento.png --poeira poeira.png
"""
import argparse
import json
import os

import numpy as np
from PIL import Image

RAIZ = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
ULTRAS = os.path.join(RAIZ, "android", "app", "src", "main", "assets", "ultras")

PRETO = 14          # abaixo disso é fundo (ruído do JPG)
LARGURA_CORPO = 1024
LARGURA_PONTA = 1024
LADO_INICIO = 1024
LADO_IMPACTO = 768
EMENDA = 0.12       # fração da largura usada para fundir as bordas do corpo


def para_alfa(im):
    """Preto vira transparente; o brilho mantém a cor (desfaz a mistura com o preto)."""
    rgb = np.asarray(im.convert("RGB")).astype(np.float32)
    luz = rgb.max(axis=2)
    alfa = np.clip((luz - PRETO) / (255.0 - PRETO), 0.0, 1.0)
    cor = rgb / np.maximum(luz, 1.0)[..., None] * 255.0
    rgba = np.dstack([np.clip(cor, 0, 255), alfa * 255.0]).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


def centro_do_nucleo(im):
    """Linha central da faixa mais clara (o núcleo branco do feixe)."""
    lum = np.asarray(im.convert("L")).astype(np.float32)
    meio = lum[:, lum.shape[1] // 4: lum.shape[1] * 3 // 4].mean(axis=1)
    linhas = np.where(meio >= meio.max() - 12)[0]
    return float(linhas.mean()), float(linhas.max() - linhas.min() + 1)


def recorte_vertical_centrado(im, centro):
    """Corta em volta do núcleo, simétrico, até onde ainda há brilho."""
    a = np.asarray(im)[..., 3]
    linhas = np.where(a.max(axis=1) > 8)[0]
    meia = max(centro - linhas.min(), linhas.max() - centro) + 4
    topo = int(max(0, round(centro - meia)))
    base = int(min(im.height, round(centro + meia)))
    return im.crop((0, topo, im.width, base))


def sem_emenda(im):
    """Funde o fim da imagem no começo: a última coluna continua na primeira."""
    a = np.asarray(im).astype(np.float32)
    w = a.shape[1]
    k = int(w * EMENDA)
    saida = a[:, : w - k].copy()
    peso = np.linspace(0.0, 1.0, k)[None, :, None]
    saida[:, :k] = a[:, w - k:] * (1.0 - peso) + a[:, :k] * peso
    return Image.fromarray(np.clip(saida, 0, 255).astype(np.uint8), "RGBA")


def reduzir_largura(im, largura):
    if im.width <= largura:
        return im
    return im.resize((largura, round(im.height * largura / im.width)), Image.LANCZOS)


def reduzir_lado(im, lado):
    escala = lado / max(im.size)
    if escala >= 1:
        return im
    return im.resize((round(im.width * escala), round(im.height * escala)), Image.LANCZOS)


def apagar_direita(im, inicio=0.62):
    """Some com a borda direita aos poucos: o feixe da imagem se funde no corpo do raio."""
    a = np.asarray(im).astype(np.float32)
    w = a.shape[1]
    x0 = int(w * inicio)
    rampa = np.ones(w, dtype=np.float32)
    rampa[x0:] = np.linspace(1.0, 0.0, w - x0) ** 1.5
    a[..., 3] *= rampa[None, :]
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def apagar_esquerda(im, fim=0.30):
    """Borda esquerda da ponta suave, para ela nascer de dentro do corpo sem degrau."""
    return apagar_direita(im.transpose(Image.FLIP_LEFT_RIGHT), 1.0 - fim).transpose(Image.FLIP_LEFT_RIGHT)


def limpar_veu(im, corte=18):
    """Zera o véu quase invisível que o gerador deixa no fundo "transparente".

    Alfa abaixo de {corte} vira 0 e o resto é reescalado, sem degrau na borda; num cenário
    escuro esse véu aparecia como uma caixa clara em volta do efeito.
    """
    a = np.asarray(im).astype(np.float32)
    a[..., 3] = np.clip((a[..., 3] - corte) * 255.0 / (255.0 - corte), 0, 255)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def colunas_dos_quadros(im, quadros):
    """Faixas de x de cada quadro, pelos vãos vazios entre eles.

    O gerador desenha quadros de larguras diferentes e fora do centro de cada quarto da
    imagem; cortar em colunas iguais levava pedaços de um quadro para o vizinho.
    """
    a = np.asarray(im)[..., 3] > 8
    ocupada = a.any(axis=0)
    faixas, inicio = [], None
    for x, cheia in enumerate(ocupada):
        if cheia and inicio is None:
            inicio = x
        elif not cheia and inicio is not None:
            faixas.append([inicio, x])
            inicio = None
    if inicio is not None:
        faixas.append([inicio, len(ocupada)])
    # Junta pedaços soltos (faíscas, pedrinhas) ao vizinho pelo menor vão até sobrar um por quadro.
    while len(faixas) > quadros:
        vaos = [faixas[i + 1][0] - faixas[i][1] for i in range(len(faixas) - 1)]
        i = vaos.index(min(vaos))
        faixas[i:i + 2] = [[faixas[i][0], faixas[i + 1][1]]]
    if len(faixas) != quadros:
        raise SystemExit(f"achei {len(faixas)} quadros separados, esperava {quadros}")
    return faixas


def folha_efeito(im, quadros, alinhar, altura):
    """Separa os quadros, alinha cada um e monta uma faixa de células iguais.

    alinhar: "base" (pé no chão, centrado em x) ou "centro". A célula tem o tamanho do maior
    quadro, então o efeito não pula de lugar entre os quadros.
    """
    partes = []
    for x0, x1 in colunas_dos_quadros(im, quadros):
        q = im.crop((x0, 0, x1, im.height))
        caixa = q.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox()
        partes.append(q.crop(caixa))
    w = max(p.width for p in partes) + 8
    h = max(p.height for p in partes) + 8
    escala = min(1.0, altura / h)
    w, h = round(w * escala), round(h * escala)
    faixa = Image.new("RGBA", (w * quadros, h))
    for i, p in enumerate(partes):
        p = p.resize((max(1, round(p.width * escala)), max(1, round(p.height * escala))), Image.LANCZOS)
        x = i * w + (w - p.width) // 2
        y = h - p.height - 2 if alinhar == "base" else (h - p.height) // 2
        faixa.alpha_composite(p, (x, y))
    return faixa


def centro_da_esfera(im):
    """Coluna onde o clarão é mais alto (a esfera, não o feixe que sai dela)."""
    lum = np.asarray(im.convert("L")).astype(np.float32)
    claro = lum >= 250
    altura = np.convolve(claro.sum(axis=0), np.ones(15) / 15, mode="same")
    x = int(altura.argmax())
    ys = np.where(claro[:, max(0, x - 7): x + 8].any(axis=1))[0]
    return float(x / im.width), float(ys.mean() / im.height)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--id", required=True, help="pasta do ultra em assets/ultras")
    p.add_argument("--corpo")
    p.add_argument("--ponta")
    p.add_argument("--espelhar-ponta", action="store_true")
    p.add_argument("--inicio")
    p.add_argument("--impacto")
    p.add_argument("--aura")
    p.add_argument("--vento")
    p.add_argument("--poeira")
    p.add_argument("--quadros", type=int, default=4, help="quadros lado a lado nas folhas de efeito")
    args = p.parse_args()

    pasta = os.path.join(ULTRAS, args.id)
    os.makedirs(pasta, exist_ok=True)
    caminho = os.path.join(pasta, "ultra.json")
    dados = {}
    if os.path.exists(caminho):
        with open(caminho, encoding="utf-8") as f:
            dados = json.load(f)
    raio = dict(dados.get("raio", {}))

    if args.corpo:
        bruto = Image.open(args.corpo)
        centro, _ = centro_do_nucleo(bruto)
        corpo = recorte_vertical_centrado(para_alfa(bruto), centro)
        corpo = reduzir_largura(sem_emenda(corpo), LARGURA_CORPO)
        corpo.save(os.path.join(pasta, "raio_corpo.png"), optimize=True)
        raio["corpo"] = "raio_corpo.png"

    if args.ponta:
        bruto = Image.open(args.ponta)
        if args.espelhar_ponta:
            bruto = bruto.transpose(Image.FLIP_LEFT_RIGHT)
        c, _ = centro_do_nucleo(bruto)
        ponta = recorte_vertical_centrado(para_alfa(bruto), c)
        ponta = reduzir_largura(apagar_esquerda(ponta), LARGURA_PONTA)
        ponta.save(os.path.join(pasta, "raio_ponta.png"), optimize=True)
        raio["ponta"] = "raio_ponta.png"

    if args.inicio:
        bruto = Image.open(args.inicio)
        cx, cy = centro_da_esfera(bruto)
        reduzir_lado(apagar_direita(para_alfa(bruto)), LADO_INICIO).save(os.path.join(pasta, "raio_inicio.png"), optimize=True)
        raio["inicio"] = "raio_inicio.png"
        raio["centroInicio"] = [round(cx, 3), round(cy, 3)]

    if args.impacto:
        bruto = Image.open(args.impacto)
        reduzir_lado(para_alfa(bruto), LADO_IMPACTO).save(os.path.join(pasta, "raio_impacto.png"), optimize=True)
        raio["impacto"] = "raio_impacto.png"

    efeitos = (("aura", args.aura, True, "base", 512), ("vento", args.vento, True, "centro", 256),
               ("poeira", args.poeira, False, "base", 384))
    for nome, arquivo, preto, alinhar, altura in efeitos:
        if not arquivo:
            continue
        bruto = Image.open(arquivo)
        bruto = para_alfa(bruto) if preto else limpar_veu(bruto.convert("RGBA"))
        folha_efeito(bruto, args.quadros, alinhar, altura).save(
            os.path.join(pasta, f"raio_{nome}.png"), optimize=True)
        raio[nome] = {"imagem": f"raio_{nome}.png", "quadros": args.quadros}

    raio.setdefault("hits", 20)
    raio.setdefault("espessura", 150)
    dados["raio"] = raio
    with open(caminho, "w", encoding="utf-8") as f:
        json.dump(dados, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("raio gravado em", pasta, json.dumps(raio, ensure_ascii=False))


if __name__ == "__main__":
    main()
