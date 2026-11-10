#!/usr/bin/env python3
"""A textura de forma do rastro do golpe (assets/efeitos/rastro_forma), pela receita do relatório
reports/VFX de luta estilo anime.md: cinza, sem cor, em três canais de uma imagem só.

    R  a FORMA: a fita reta gerada no Codex (art/vfx/arco_textura_04.png, o véu com riscos finos),
       em tons de cinza. É o desenho; o código não desenha nada dela.
    G  o RUÍDO que come a forma (erosão): fBm de valor que repete sem emenda no comprimento (o
       jogo faz ele correr ao longo da fita), com o histograma igualado (limiar 0,1 come ~10%).
    B  a BASE ESCURA: a forma borrada e alargada, a área da base sob o corte (como a Arc System
       Works faz, para o corte não sumir em cenário claro).

A cor sai no jogo (RastroDoGolpe / ErosaoDaFita): o cinza vira índice da rampa do personagem.
Tudo linear, sem correção de gama; alfa 255 (o jogo lê os canais, não desenha a imagem).

    python3 tools/sprites/rastro_forma.py art/vfx/arco_textura_04.png
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import efeito_imagem as ei  # noqa: E402

LARGURA, ALTURA = 512, 96   # px da folha: o jogo estica no caminho


ESTICA = 7.0   # o ruído é ESTICA vezes mais fino através da fita que ao longo dela


def ruido_fbm(largura, altura, celulas=5, oitavas=4, semente=7):
    """Ruído de valor em oitavas que repete no eixo do comprimento (a grade dá a volta)."""
    rng = np.random.default_rng(semente)
    total = np.zeros((altura, largura), np.float32)
    amp, soma = 1.0, 0.0
    for o in range(oitavas):
        cx = celulas * 2 ** o
        # Mais células através que ao longo: manchas compridas, no sentido do vento.
        cy = max(2, int(round(cx * altura / largura * ESTICA)) + 1)
        g = rng.random((cy + 1, cx)).astype(np.float32)
        tx = np.arange(largura, dtype=np.float32) * cx / largura
        ty = np.arange(altura, dtype=np.float32) * (cy - 1) / max(1, altura - 1)
        x0 = np.floor(tx).astype(int); fx = tx - x0; x1 = (x0 + 1) % cx; x0 %= cx
        y0 = np.minimum(np.floor(ty).astype(int), cy - 1); fy = ty - y0; y1 = np.minimum(y0 + 1, cy)
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy)
        fx = fx[None, :]; fy = fy[:, None]
        a = g[y0][:, x0]; b = g[y0][:, x1]; c = g[y1][:, x0]; d = g[y1][:, x1]
        total += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy)
        soma += amp
        amp *= 0.5
    return total / soma


def igualar(m):
    """Histograma igualado: o valor vira a fração de pixels abaixo dele."""
    plano = m.ravel()
    ordem = np.argsort(plano, kind='stable')
    saida = np.empty_like(plano)
    saida[ordem] = np.linspace(0.0, 1.0, plano.size, dtype=np.float32)
    return saida.reshape(m.shape)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('textura')
    p.add_argument('--nome', default='rastro_forma')
    p.add_argument('--semente', type=int, default=7)
    a = p.parse_args()
    rgba = ei.recortar_fundo(Image.open(a.textura), 'preto')
    # O cinza da forma: o quanto o pixel acende (branco = 1), já com a transparência do recorte.
    cinza = rgba[..., :3].max(axis=2) * rgba[..., 3]
    # No comprimento vai até o último risco; através, só até onde o véu tem corpo (as bordas
    # esfumadas do véu deixavam a fita mais fina que a malha).
    ys, xs = np.nonzero(cinza > 0.04)
    linhas = np.nonzero((cinza > 0.12).mean(axis=1) > 0.05)[0]
    cinza = cinza[linhas.min():linhas.max() + 1, xs.min():xs.max() + 1]
    forma = np.asarray(Image.fromarray(cinza.astype(np.float32), 'F').resize((LARGURA, ALTURA), Image.BOX))
    forma = np.clip(forma / max(1e-4, np.percentile(forma, 99.7)), 0.0, 1.0)
    ruido = igualar(ruido_fbm(LARGURA, ALTURA, semente=a.semente))
    # A base: a forma alargada e borrada, cheia onde há corpo da fita.
    base = Image.fromarray((forma * 255).astype(np.uint8), 'L').filter(ImageFilter.MaxFilter(3))
    base = np.asarray(base.filter(ImageFilter.GaussianBlur(3)), dtype=np.float32) / 255.0
    base = np.clip(base / max(1e-4, np.percentile(base, 99)), 0.0, 1.0)
    rgb = np.dstack([forma, ruido, base])
    img = Image.fromarray((rgb * 255 + 0.5).astype(np.uint8), 'RGB').convert('RGBA')
    pasta = ei.SAIDA / a.nome
    pasta.mkdir(parents=True, exist_ok=True)
    img.save(pasta / 'folha.webp', lossless=True, quality=100, method=6)
    dados = {'nome': a.nome, 'origem': f'tools/sprites/rastro_forma.py ({Path(a.textura).name})',
             'ancora': 'canais', 'canais': 'R forma, G ruido (repete no comprimento), B base escura',
             'pixelScale': 1.0, 'quadros': [[0, 0, LARGURA, ALTURA, 0.0, 0.0]], 'duracoesMs': [1000]}
    (pasta / 'efeito.json').write_text(json.dumps(dados, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(a.nome, '->', pasta.relative_to(ei.ROOT), img.size,
          'forma media %.2f, p50 %.2f, p90 %.2f' % (forma.mean(), np.percentile(forma[forma > 0.05], 50),
                                                   np.percentile(forma[forma > 0.05], 90)))


if __name__ == '__main__':
    main()
