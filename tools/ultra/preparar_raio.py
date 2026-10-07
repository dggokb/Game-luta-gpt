#!/usr/bin/env python3
"""Prepara as imagens do raio final do ultra (geradas com fundo preto).

O GPT desenha brilho com fundo transparente mal; com fundo preto fica bom. Este script
transforma o preto em transparência mantendo o brilho suave (o pixel vira a cor "cheia"
com a transparência proporcional à luz), recorta, centraliza e reduz:

- corpo:   feixe horizontal. Fica centrado no núcleo e sem emenda nas bordas (repete).
- ponta:   frente do feixe (opcional). Use --espelhar-ponta se a frente aponta para a esquerda.
- inicio:  esfera nas mãos. O centro da esfera vai para o ultra.json.
- impacto: explosão no alvo.

Uso:
    python tools/ultra/preparar_raio.py --id player_base --corpo corpo.png \
        --ponta ponta.png --espelhar-ponta --inicio inicio.png --impacto impacto.png
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
    p.add_argument("--corpo", required=True)
    p.add_argument("--ponta")
    p.add_argument("--espelhar-ponta", action="store_true")
    p.add_argument("--inicio")
    p.add_argument("--impacto")
    args = p.parse_args()

    pasta = os.path.join(ULTRAS, args.id)
    os.makedirs(pasta, exist_ok=True)
    raio = {"corpo": "raio_corpo.png"}

    bruto = Image.open(args.corpo)
    centro, _ = centro_do_nucleo(bruto)
    corpo = recorte_vertical_centrado(para_alfa(bruto), centro)
    corpo = reduzir_largura(sem_emenda(corpo), LARGURA_CORPO)
    corpo.save(os.path.join(pasta, "raio_corpo.png"), optimize=True)

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

    caminho = os.path.join(pasta, "ultra.json")
    dados = {}
    if os.path.exists(caminho):
        with open(caminho, encoding="utf-8") as f:
            dados = json.load(f)
    anterior = dados.get("raio", {})
    raio["hits"] = anterior.get("hits", 20)
    raio["espessura"] = anterior.get("espessura", 150)
    dados["raio"] = raio
    with open(caminho, "w", encoding="utf-8") as f:
        json.dump(dados, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("raio gravado em", pasta, json.dumps(raio, ensure_ascii=False))


if __name__ == "__main__":
    main()
