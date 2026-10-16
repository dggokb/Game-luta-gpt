#!/usr/bin/env python3
"""Prepara (e opcionalmente gera) as imagens de um ultra da "Página Final".

Uso básico, com imagens que você já gerou:

    python tools/ultra/preparar_ultra.py --id player_base --nome "Explosão Solar" \\
        --cor "#F4B73B" --entrada minhas_imagens/player_base

A pasta de entrada deve ter arquivos com o nome do painel (qualquer extensão
de imagem): olhos, carga, golpe, final, e os dois da abertura do Ultra:
corte (o personagem recortado, PNG com fundo transparente) e palco (o fundo do
Ultra). Imagem que falta continua com a arte provisória do jogo.

Nome e cor já ficam no ultra.json de cada personagem; --nome e --cor só trocam.

Gerando as 5 imagens direto pela API da OpenAI (precisa de OPENAI_API_KEY e
do pacote `openai`):

    python tools/ultra/preparar_ultra.py --id player_base --nome "Explosão Solar" \\
        --cor "#F4B73B" --gerar --descricao "lutador de cabelo espetado preto, \\
        kimono laranja e faixa azul" --referencia ficha_player_base.png

O script recorta cada imagem no formato do painel e grava tudo em
android/app/src/main/assets/ultras/<id>/, junto com o ultra.json.
"""

import argparse
import base64
import json
import os
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps
except ImportError:
    sys.exit("Instale o Pillow: pip install pillow")

RAIZ = Path(__file__).resolve().parents[2]
SAIDA_PADRAO = RAIZ / "android" / "app" / "src" / "main" / "assets" / "ultras"
PROMPTS = Path(__file__).resolve().parent / "prompts.json"
EXTENSOES = (".png", ".jpg", ".jpeg", ".webp")

# Proporção (largura / altura) de cada painel na página, e largura final.
# O jogo ainda faz um zoom lento por cima, então sobra uma margem.
PAINEIS = {
    "olhos": {"proporcao": 5.6, "largura": 1600, "tamanho_api": "1536x1024"},
    "carga": {"proporcao": 1.0, "largura": 900, "tamanho_api": "1024x1024"},
    "golpe": {"proporcao": 1.75, "largura": 1400, "tamanho_api": "1536x1024"},
    "final": {"proporcao": 16 / 9, "largura": 1600, "tamanho_api": "1536x1024"},
}
# Abertura do Ultra: o personagem recortado (mantém a transparência) e o fundo.
ABERTURA = {
    "corte": {"altura": 1400, "arquivo": "corte.png", "tamanho_api": "1024x1536"},
    "palco": {"proporcao": 16 / 9, "largura": 1600, "arquivo": "palco.jpg", "tamanho_api": "1536x1024"},
}


# --------------------------------------------------------------- imagens

def recortar(imagem, proporcao, foco):
    """Corta no formato do painel. foco = posição vertical (0 topo, 1 base)."""
    largura, altura = imagem.size
    if largura / altura > proporcao:
        nova_largura = round(altura * proporcao)
        esquerda = (largura - nova_largura) // 2
        return imagem.crop((esquerda, 0, esquerda + nova_largura, altura))
    nova_altura = round(largura / proporcao)
    topo = round((altura - nova_altura) * foco)
    return imagem.crop((0, topo, largura, topo + nova_altura))


def filtro_manga(imagem, intensidade=1.0):
    """Contraste forte, traço de tinta nas bordas e retícula nas sombras."""
    rgb = imagem.convert("RGB")
    rgb = ImageEnhance.Contrast(rgb).enhance(1.0 + 0.25 * intensidade)
    rgb = ImageEnhance.Color(rgb).enhance(1.0 + 0.20 * intensidade)

    # Tinta: bordas detectadas viram linhas escuras.
    cinza = ImageOps.grayscale(rgb)
    bordas = cinza.filter(ImageFilter.FIND_EDGES).point(lambda v: 255 if v > 60 else 0)
    bordas = bordas.filter(ImageFilter.MaxFilter(3))
    tinta = Image.new("RGB", rgb.size, (17, 17, 20))
    rgb = Image.composite(tinta, rgb, bordas.point(lambda v: int(v * 0.55 * intensidade)))

    # Retícula: pontos pretos maiores onde a imagem é mais escura.
    passo = max(6, rgb.width // 220)
    pequena = cinza.resize((max(1, rgb.width // passo), max(1, rgb.height // passo)))
    pontos = Image.new("L", rgb.size, 0)
    desenho = ImageDraw.Draw(pontos)
    for y in range(pequena.height):
        for x in range(pequena.width):
            escuro = 1.0 - pequena.getpixel((x, y)) / 255.0
            if escuro < 0.45:
                continue
            raio = passo * 0.5 * (escuro - 0.45) / 0.55 * 1.1
            cx = x * passo + passo / 2 + (passo / 2 if y % 2 else 0)
            cy = y * passo + passo / 2
            desenho.ellipse((cx - raio, cy - raio, cx + raio, cy + raio), fill=int(110 * intensidade))
    return Image.composite(tinta, rgb, pontos)


def preparar(arquivo, painel, estilo, foco, intensidade):
    config = PAINEIS[painel]
    imagem = Image.open(arquivo)
    imagem = ImageOps.exif_transpose(imagem).convert("RGB")
    imagem = recortar(imagem, config["proporcao"], foco)
    largura = min(config["largura"], imagem.width)
    altura = round(largura / config["proporcao"])
    imagem = imagem.resize((largura, altura), Image.LANCZOS)
    if estilo == "manga":
        imagem = filtro_manga(imagem, intensidade)
    return imagem


def tirar_fundo(imagem, tolerancia=38):
    """Fundo liso (branco, verde...) vira transparente: parte dos cantos e espalha pelos
    pixels parecidos. Só para quando a imagem gerada veio sem transparência."""
    rgb = imagem.convert("RGB")
    mascara = Image.new("L", rgb.size, 255)
    largura, altura = rgb.size
    for canto in ((0, 0), (largura - 1, 0), (0, altura - 1), (largura - 1, altura - 1)):
        ImageDraw.floodfill(rgb, canto, (255, 0, 255), thresh=tolerancia)
    pixels = rgb.load()
    alfa = mascara.load()
    for y in range(altura):
        for x in range(largura):
            if pixels[x, y] == (255, 0, 255):
                alfa[x, y] = 0
    mascara = mascara.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(1.2))
    saida = imagem.convert("RGBA")
    saida.putalpha(mascara)
    return saida


def preparar_corte(arquivo):
    """Personagem recortado: transparência (ou fundo removido), aparado e na altura certa."""
    imagem = ImageOps.exif_transpose(Image.open(arquivo))
    if imagem.mode != "RGBA" or imagem.getchannel("A").getextrema()[0] > 250:
        imagem = tirar_fundo(imagem)
    caixa = imagem.getchannel("A").point(lambda v: 255 if v > 16 else 0).getbbox()
    if caixa:
        imagem = imagem.crop(caixa)
    altura = min(ABERTURA["corte"]["altura"], imagem.height)
    largura = round(imagem.width * altura / imagem.height)
    return imagem.resize((largura, altura), Image.LANCZOS)


def preparar_palco(arquivo, foco):
    config = ABERTURA["palco"]
    imagem = ImageOps.exif_transpose(Image.open(arquivo)).convert("RGB")
    imagem = recortar(imagem, config["proporcao"], foco)
    largura = min(config["largura"], imagem.width)
    return imagem.resize((largura, round(largura / config["proporcao"])), Image.LANCZOS)


def achar_imagem(pasta, painel):
    for extensao in EXTENSOES:
        candidato = pasta / (painel + extensao)
        if candidato.exists():
            return candidato
    return None


# ------------------------------------------------------------------- API

def gerar_imagens(pasta, descricao, cor_nome, referencia, modelo, paineis):
    try:
        from openai import OpenAI
    except ImportError:
        sys.exit("Para --gerar instale o pacote da OpenAI: pip install openai")
    if not os.environ.get("OPENAI_API_KEY"):
        sys.exit("Defina a variável OPENAI_API_KEY para usar --gerar")

    prompts = json.loads(PROMPTS.read_text(encoding="utf-8"))
    cliente = OpenAI()
    pasta.mkdir(parents=True, exist_ok=True)

    for painel in paineis:
        prompt = (
            prompts[painel].format(descricao=descricao, cor=cor_nome)
            + " " + prompts["estilo"]
        )
        tamanho = PAINEIS[painel]["tamanho_api"]
        print(f"gerando {painel}...")
        if referencia:
            # A ficha do personagem entra como referência para manter ele igual.
            with open(referencia, "rb") as ref:
                resposta = cliente.images.edit(model=modelo, image=[ref], prompt=prompt, size=tamanho)
        else:
            resposta = cliente.images.generate(model=modelo, prompt=prompt, size=tamanho)
        dados = base64.b64decode(resposta.data[0].b64_json)
        (pasta / f"{painel}.png").write_bytes(dados)


# ------------------------------------------------------------------ main

def ler_foco(valores, definicao):
    """Foco de cada recorte: o --foco da linha de comando, senão o que ficou gravado no
    ultra.json da última vez ("foco" no painel, "focoPalco" na abertura), senão o meio."""
    paineis = definicao.get("paineis", {})
    foco = {painel: float(paineis.get(painel, {}).get("foco", 0.5)) for painel in PAINEIS}
    foco["corte"] = 0.5
    foco["palco"] = float(definicao.get("abertura", {}).get("focoPalco", 0.5))
    for valor in valores or []:
        painel, _, numero = valor.partition("=")
        if painel not in foco:
            sys.exit(f"--foco: painel desconhecido '{painel}'")
        foco[painel] = min(1.0, max(0.0, float(numero)))
    return foco


def main():
    parser = argparse.ArgumentParser(description="Prepara as imagens de um ultra da Página Final.")
    parser.add_argument("--id", required=True, help="pasta do ultra, ex.: player_base (id do personagem)")
    parser.add_argument("--nome", help="troca o nome do golpe, ex.: \"Explosão Solar\" (padrão: o do ultra.json)")
    parser.add_argument("--cor", help="troca a cor principal, ex.: #F4B73B (padrão: a do ultra.json)")
    parser.add_argument("--cor-secundaria", help="cor do brilho, ex.: #FFE7A3")
    parser.add_argument("--entrada", type=Path, help="pasta com olhos/carga/golpe/final, corte e palco")
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO, help="pasta ultras/ dos assets")
    parser.add_argument(
        "--estilo", choices=["manga", "nenhum"], default="nenhum",
        help="manga aplica contraste, tinta e retícula; só ajuda quando as imagens saem com estilos diferentes",
    )
    parser.add_argument(
        "--intensidade", type=float, default=1.0,
        help="força do filtro de mangá (0.5 = suave, 1 = padrão, 1.5 = pesado)",
    )
    parser.add_argument(
        "--foco", action="append",
        help="posição vertical do recorte por painel, ex.: --foco golpe=0.2 (0 topo, 1 base); "
             "fica gravado no ultra.json para as próximas vezes",
    )
    parser.add_argument("--gerar", action="store_true", help="gera as imagens pela API da OpenAI antes")
    parser.add_argument("--descricao", help="descrição do personagem (para --gerar)")
    parser.add_argument("--cor-nome", default="dourada", help="cor da energia em palavras (para --gerar)")
    parser.add_argument("--referencia", type=Path, help="ficha do personagem usada como referência")
    parser.add_argument("--modelo", default="gpt-image-1", help="modelo de imagem da OpenAI")
    parser.add_argument("--paineis", nargs="+", choices=list(PAINEIS), default=list(PAINEIS))
    args = parser.parse_args()

    entrada = args.entrada or (Path(__file__).resolve().parent / "geradas" / args.id)
    if args.gerar:
        if not args.descricao:
            sys.exit("--gerar precisa de --descricao")
        gerar_imagens(entrada, args.descricao, args.cor_nome, args.referencia, args.modelo, args.paineis)
    if not entrada.is_dir():
        sys.exit(f"pasta de entrada não existe: {entrada}")

    destino = args.saida / args.id
    destino.mkdir(parents=True, exist_ok=True)
    arquivo_json = destino / "ultra.json"
    definicao = json.loads(arquivo_json.read_text(encoding="utf-8")) if arquivo_json.exists() else {}
    if args.nome:
        definicao["nome"] = args.nome
    if args.cor:
        definicao["cor"] = args.cor
    if not definicao.get("nome"):
        sys.exit("o ultra.json ainda não tem nome: passe --nome")
    if args.cor_secundaria:
        definicao["corSecundaria"] = args.cor_secundaria
    paineis_json = definicao.setdefault("paineis", {})

    foco = ler_foco(args.foco, definicao)
    for painel in PAINEIS:
        arquivo = achar_imagem(entrada, painel)
        entrada_json = paineis_json.setdefault(painel, {})
        if arquivo is None:
            print(f"{painel}: sem imagem, fica a arte provisória")
            continue
        imagem = preparar(arquivo, painel, args.estilo, foco[painel], args.intensidade)
        nome_final = f"{painel}.jpg"
        imagem.save(destino / nome_final, quality=88)
        entrada_json["imagem"] = nome_final
        entrada_json["foco"] = foco[painel]
        print(f"{painel}: {arquivo.name} -> {nome_final} ({imagem.width}x{imagem.height})")

    abertura = definicao.setdefault("abertura", {})
    for parte, config in ABERTURA.items():
        arquivo = achar_imagem(entrada, parte)
        if arquivo is None:
            print(f"{parte}: sem imagem, fica o visual provisório da abertura")
            continue
        if parte == "corte":
            imagem = preparar_corte(arquivo)
            imagem.save(destino / config["arquivo"], optimize=True)
        else:
            imagem = preparar_palco(arquivo, foco[parte])
            imagem.save(destino / config["arquivo"], quality=88)
            abertura["focoPalco"] = foco[parte]
        abertura[parte] = config["arquivo"]
        print(f"{parte}: {arquivo.name} -> {config['arquivo']} ({imagem.width}x{imagem.height})")

    arquivo_json.write_text(json.dumps(definicao, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"pronto: {destino}")


if __name__ == "__main__":
    main()
