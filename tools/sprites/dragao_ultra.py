#!/usr/bin/env python3
"""Dragão azul do Ultra SHINKAI do Ryūga, desenhado por código (pedido do jogador, 03/11).

Gera a folha de efeito `assets/efeitos/ultra_shinkai_player_base` (folha.webp + efeito.json) no formato
do `EfeitosVideo`: um ciclo do dragão ondulando, com a ponta do focinho na origem e o corpo atrás. O jogo
toca o ciclo repetindo e leva a origem pela tela (`EfeitosVideo.tocarAndando`) na velocidade do motor
(`CombatConfig.dragaoVelocidade`, px por quadro de 60 Hz): ele entra de fora da tela, por trás do Ryūga,
atravessa e sai do outro lado (o jogo espelha quando ele olha para a esquerda). O comprimento de onda é o
quanto a cabeça anda num ciclo, então o corpo segue o caminho da cabeça e o ciclo emenda sem pulo.

    python3 tools/sprites/dragao_ultra.py [--previa arquivo.png]
"""
import argparse
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SAIDA = os.path.join(RAIZ, "android", "app", "src", "main", "assets", "efeitos", "ultra_shinkai_player_base")

# Mundo (px do jogo; o Ryūga tem 224 de altura). y para baixo, origem no punho.
VELOCIDADE = 24.0          # px por quadro de 60 Hz (CombatConfig.dragaoVelocidade)
PASSO = 2                  # quadros de 60 Hz por quadro da folha (30 qps)
QUADROS = 16               # um ciclo da onda (o jogo repete)
CABECA = 360.0             # comprimento da cabeça
CORPO_INICIO, CORPO_FIM = 300.0, 1360.0   # do focinho para trás (CombatConfig.dragaoComprimento)
ONDA_A = 85.0
ONDA_L = VELOCIDADE * PASSO * QUADROS   # o quanto a cabeça anda num ciclo (768)
CENTRO_Y = -70.0
ESCALA = 0.35              # px da folha por px do mundo (pixelScale)
SS = 2                     # supersample

AZUL_ESCURO = (16, 60, 170)
AZUL = (44, 140, 250)
CIANO = (110, 220, 255)
BRANCO = (235, 252, 255)


def raio(s):
    if s < 600.0:
        return 62.0 + (s - CORPO_INICIO) / (600.0 - CORPO_INICIO) * 33.0
    u = (s - 600.0) / (CORPO_FIM - 600.0)
    return max(9.0, 95.0 * (1.0 - u ** 1.35))


def espinha(s, cabeca_x, fase):
    """Ponto da espinha a s px do focinho (para trás) e o ângulo da tangente."""
    x = cabeca_x - s
    y = CENTRO_Y + ONDA_A * math.sin(2 * math.pi * s / ONDA_L - fase)
    dy = ONDA_A * math.cos(2 * math.pi * s / ONDA_L - fase) * 2 * math.pi / ONDA_L
    # Andando para trás em s, x diminui: a direção do corpo para a frente é (+1, -dy).
    ang = math.atan2(-dy, 1.0)
    return x, y, ang


def disco_sprite(r):
    """Um disco sombreado (luz de cima): borda escura, miolo azul, brilho ciano em cima, barriga clara."""
    n = max(4, int(2 * r) + 2)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    c = (n - 1) / 2.0
    dx, dy = (xx - c) / r, (yy - c) / r
    d = np.sqrt(dx * dx + dy * dy)
    alfa = np.clip((1.0 - d) * r * 0.9, 0, 1)
    luz = np.clip(-dy * 0.8 + 0.2, 0, 1)               # em cima
    barriga = np.clip(dy * 1.4 - 0.55, 0, 1)            # em baixo (as placas claras)
    borda = np.clip((d - 0.86) / 0.14, 0, 1) * 0.7
    cor = np.zeros((n, n, 3), np.float32)
    for k in range(3):
        base = AZUL[k] * (1 - luz) + CIANO[k] * luz
        base = base * (1 - barriga) + (BRANCO[k] * 0.85 + CIANO[k] * 0.15) * barriga
        cor[..., k] = base * (1 - borda) + AZUL_ESCURO[k] * borda
    img = np.dstack([cor, alfa * 255]).astype(np.uint8)
    return Image.fromarray(img, "RGBA")


_DISCOS = {}


def disco(r):
    k = int(round(r))
    if k not in _DISCOS:
        _DISCOS[k] = disco_sprite(k)
    return _DISCOS[k]


def desenhar_corpo(tela, ox, oy, cabeca_x, fase, sc):
    """Do rabo para o pescoço: discos em sequência, depois as escamas e as barbatanas."""
    s = CORPO_FIM
    pontos = []
    while s >= CORPO_INICIO:
        x, y, ang = espinha(s, cabeca_x, fase)
        r = raio(s) * sc
        px, py = (x - ox) * sc, (y - oy) * sc
        d = disco(r)
        tela.alpha_composite(d, (int(px - d.width / 2), int(py - d.height / 2)))
        pontos.append((s, px, py, ang, r))
        s -= 4.0
    det = Image.new("RGBA", tela.size, (0, 0, 0, 0))
    dr = ImageDraw.Draw(det)
    # Escamas: crescentes escuros em fileiras ao longo do corpo, abertos para trás.
    for s, px, py, ang, r in pontos:
        if int(s) % 26 >= 4 or s > CORPO_FIM - 60:
            continue
        nx, ny = math.sin(ang), -math.cos(ang)    # normal (para cima do corpo)
        for f in (-0.55, -0.2, 0.15, 0.5):
            cx, cy = px + nx * f * r, py + ny * f * r
            e = r * 0.28
            dr.arc([cx - e, cy - e, cx + e, cy + e], start=100 - math.degrees(ang), end=260 - math.degrees(ang),
                   fill=AZUL_ESCURO + (150,), width=max(1, int(r * 0.07)))
    # Barbatanas nas costas: triângulos claros apontando para trás e para cima.
    for s, px, py, ang, r in pontos:
        if int(s) % 46 >= 4 or s < 380 or s > CORPO_FIM - 90:
            continue
        nx, ny = math.sin(ang), -math.cos(ang)
        tx, ty = -math.cos(ang), -math.sin(ang)   # tangente para trás
        bx, by = px + nx * r * 0.85, py + ny * r * 0.85
        h = r * 0.75
        tri = [(bx + tx * r * 0.35, by + ty * r * 0.35), (bx - tx * r * 0.35, by - ty * r * 0.35),
               (bx + nx * h + tx * h * 0.8, by + ny * h + ty * h * 0.8)]
        dr.polygon(tri, fill=CIANO + (210,), outline=BRANCO + (230,))
    tela.alpha_composite(det)


def cabeca_img(sc):
    """A cabeça em coordenadas próprias (focinho em (0, 0), olhando para +x), já na escala."""
    W, H = int(560 * sc), int(420 * sc)
    ox, oy = int(470 * sc), int(230 * sc)    # onde fica o focinho na imagem
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def P(pts):
        return [(ox + x * sc, oy + y * sc) for x, y in pts]

    lw = max(2, int(6 * sc))
    # Bigodes (atrás de tudo): duas linhas longas ondulando para trás.
    for k, (y0, amp) in enumerate(((-18, 40), (-8, 55))):
        pts = []
        for i in range(40):
            t = i / 39.0
            x = -40 - t * 440
            y = y0 - 30 * t + amp * math.sin(t * 5.5 + k) * t
            pts.append((x, y))
        d.line(P(pts), fill=BRANCO + (220,), width=max(2, int(7 * sc * (1.2 - 0.5 * k))))
    # Chifres.
    for base, ponta, larg in (((-215, -78), (-440, -190), 26), ((-195, -70), (-400, -120), 20)):
        bx, by = base
        tx, ty = ponta
        mx, my = (bx + tx) / 2 + 10, (by + ty) / 2 - 35
        pts = []
        for i in range(12):
            t = i / 11.0
            x = (1 - t) ** 2 * bx + 2 * (1 - t) * t * mx + t * t * tx
            y = (1 - t) ** 2 * by + 2 * (1 - t) * t * my + t * t * ty
            pts.append((x, y, larg * (1 - t) + 2))
        cima = [(x, y - w / 2) for x, y, w in pts]
        baixo = [(x, y + w / 2) for x, y, w in reversed(pts)]
        d.polygon(P(cima + baixo), fill=CIANO + (255,), outline=AZUL_ESCURO + (255,))
    # Juba de chamas atrás da cabeça.
    for i in range(9):
        t = i / 8.0
        bx, by = -250 - t * 110, -60 + t * 40
        h = 120 - t * 40
        ang = math.radians(150 + t * 25)
        tx, ty = bx + math.cos(ang) * h, by - math.sin(ang) * h * 0.9
        larg = 28
        pts = [(bx, by - larg), (tx, ty), (bx + 10, by + larg)]
        d.polygon(P(pts), fill=(CIANO if i % 2 else BRANCO) + (220,))
    # Mandíbula de baixo (aberta).
    d.polygon(P([(-10, 22), (-80, 58), (-180, 74), (-262, 48), (-245, 22), (-150, 30), (-60, 20)]),
              fill=AZUL + (255,), outline=AZUL_ESCURO + (255,))
    # Boca: energia branca por dentro.
    d.polygon(P([(-8, 12), (-60, 16), (-150, 24), (-240, 20), (-170, 40), (-80, 44)]), fill=BRANCO + (255,))
    # Cabeça de cima.
    cima = [(0, -12), (-40, -40), (-120, -58), (-170, -74), (-232, -88), (-300, -72), (-342, -32),
            (-346, 12), (-252, 22), (-150, 8), (-60, 4), (2, 6)]
    d.polygon(P(cima), fill=AZUL + (255,), outline=AZUL_ESCURO + (255,))
    # Brilho em cima da cabeça.
    d.polygon(P([(-30, -34), (-118, -52), (-170, -66), (-230, -80), (-290, -64), (-240, -56), (-160, -44),
                 (-80, -30)]), fill=CIANO + (230,))
    # Dentes.
    for i in range(8):
        x = -18 - i * 16
        d.polygon(P([(x, 4), (x - 8, 4), (x - 4, 20)]), fill=BRANCO + (255,))
        d.polygon(P([(x - 6, 30 + i * 2.5), (x - 14, 30 + i * 2.5), (x - 10, 16 + i * 2.5)]), fill=BRANCO + (255,))
    # Narina, sobrancelha e olho.
    d.ellipse(P([(-44, -30), (-28, -22)]), fill=AZUL_ESCURO + (255,))
    d.line(P([(-150, -70), (-185, -78), (-222, -72)]), fill=AZUL_ESCURO + (255,), width=max(2, int(9 * sc)))
    d.ellipse(P([(-206, -64), (-166, -44)]), fill=(255, 248, 190, 255), outline=AZUL_ESCURO + (255,), width=lw // 2 or 1)
    d.ellipse(P([(-190, -62), (-182, -46)]), fill=AZUL_ESCURO + (255,))
    return img, (ox, oy)


def brilho(img, raio_px, cor, forca):
    a = img.split()[3].filter(ImageFilter.GaussianBlur(raio_px))
    a = a.point(lambda v: min(255, int(v * forca)))
    g = Image.new("RGBA", img.size, cor + (0,))
    g.putalpha(a)
    return g


def quadro(i, cab):
    """O quadro i do ciclo: o focinho na origem, o corpo atrás, a onda adiantada de i/QUADROS."""
    sc = ESCALA * SS
    cabeca_x = 0.0
    fase = 2 * math.pi * i / QUADROS
    x0, x1 = -(CORPO_FIM + 260.0), 60.0
    y0, y1 = -460.0, 300.0
    W, H = int((x1 - x0) * sc) + 2, int((y1 - y0) * sc) + 2
    tela = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    desenhar_corpo(tela, x0, y0, cabeca_x, fase, sc)
    # Cabeça na ponta, girada pela tangente do pescoço.
    hx, hy, _ = espinha(0.0, cabeca_x, fase)
    _, _, ang = espinha(160.0, cabeca_x, fase)
    img, (ox, oy) = cab
    # O focinho no centro de uma tela quadrada; gira em volta dele (o PIL gira no sentido anti-horário
    # da tela, e o ângulo da espinha está com o y para baixo).
    lado = 2 * max(img.width, img.height)
    q = Image.new("RGBA", (lado, lado), (0, 0, 0, 0))
    q.alpha_composite(img, (lado // 2 - ox, lado // 2 - oy))
    q = q.rotate(-math.degrees(ang), resample=Image.BICUBIC, center=(lado // 2, lado // 2))
    tela.alpha_composite(q, (int((hx - x0) * sc) - lado // 2, int((hy - y0) * sc) - lado // 2))
    # Faíscas em volta do corpo (as mesmas em cada quadro, andando junto).
    rnd = random.Random(7 + i)
    fx = Image.new("RGBA", tela.size, (0, 0, 0, 0))
    fd = ImageDraw.Draw(fx)
    for _ in range(70):
        s = rnd.uniform(CORPO_INICIO, CORPO_FIM + 200)
        x, y, _ = espinha(min(s, CORPO_FIM), cabeca_x, fase)
        x -= max(0.0, s - CORPO_FIM)
        y += rnd.uniform(-1, 1) * raio(min(s, CORPO_FIM)) * 1.8
        r = rnd.uniform(2, 6) * sc
        px, py = (x - x0) * sc, (y - y0) * sc
        fd.ellipse([px - r, py - r, px + r, py + r], fill=BRANCO + (rnd.randint(140, 255),))
    tela.alpha_composite(fx)
    # Brilho de energia por baixo de tudo.
    final = Image.new("RGBA", tela.size, (0, 0, 0, 0))
    final.alpha_composite(brilho(tela, 26 * sc, (64, 200, 255), 1.4))
    final.alpha_composite(brilho(tela, 8 * sc, (150, 230, 255), 1.1))
    final.alpha_composite(tela)
    final = final.resize((max(1, W // SS), max(1, H // SS)), Image.LANCZOS)
    bbox = final.getbbox()
    if bbox is None:
        return None, 0, 0
    final = final.crop(bbox)
    # Deslocamento até a origem, em px da folha.
    dx = x0 * ESCALA + bbox[0]
    dy = y0 * ESCALA + bbox[1]
    return final, dx, dy


def empacotar(quadros, largura=2048):
    x = y = linha = 0
    pos = []
    for q in quadros:
        if x + q.width > largura:
            x, y, linha = 0, y + linha + 2, 0
        pos.append((x, y))
        x += q.width + 2
        linha = max(linha, q.height)
    folha = Image.new("RGBA", (largura, y + linha), (0, 0, 0, 0))
    for q, (px, py) in zip(quadros, pos):
        folha.alpha_composite(q, (px, py))
    return folha, pos


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--previa", help="salva uma prévia com alguns quadros sobre um fundo")
    args = ap.parse_args()
    cab = cabeca_img(ESCALA * SS)
    imgs, desloc = [], []
    for i in range(QUADROS):
        q, dx, dy = quadro(i, cab)
        if q is None:
            q, dx, dy = Image.new("RGBA", (1, 1), (0, 0, 0, 0)), 0, 0
        imgs.append(q)
        desloc.append((round(dx, 1), round(dy, 1)))
    folha, pos = empacotar(imgs)
    os.makedirs(SAIDA, exist_ok=True)
    folha.save(os.path.join(SAIDA, "folha.webp"), "WEBP", quality=88, method=6)
    dados = {
        "nome": "ultra_shinkai_player_base",
        "gerado": "tools/sprites/dragao_ultra.py",
        "ancora": "origem",
        "pixelScale": ESCALA,
        "quadros": [[px, py, q.width, q.height, dx, dy] for q, (px, py), (dx, dy) in zip(imgs, pos, desloc)],
        "duracoesMs": [round(1000.0 * PASSO / 60.0, 3)] * QUADROS,
    }
    # A luz que o efeito emite ("luz") é posta à mão no efeito.json: refazer não a apaga.
    antigo = os.path.join(SAIDA, "efeito.json")
    if os.path.exists(antigo):
        with open(antigo) as f:
            luz = json.load(f).get("luz")
        if luz is not None:
            dados["luz"] = luz
    with open(os.path.join(SAIDA, "efeito.json"), "w") as f:
        json.dump(dados, f, indent=1)
    print("folha", folha.size, "quadros", len(imgs))
    if args.previa:
        fundo = Image.new("RGBA", (int(1700 * ESCALA * 2), int(760 * ESCALA * 2) * 4), (30, 30, 46, 255))
        for k, i in enumerate((0, 4, 8, 12)):
            q = imgs[i].resize((imgs[i].width * 2, imgs[i].height * 2))
            dx, dy = desloc[i]
            oy = int(760 * ESCALA * 2) * k + int(460 * ESCALA * 2)
            fundo.alpha_composite(q, (int(1620 * ESCALA * 2 + dx * 2), int(oy + dy * 2)))
        fundo.convert("RGB").save(args.previa)


if __name__ == "__main__":
    main()
