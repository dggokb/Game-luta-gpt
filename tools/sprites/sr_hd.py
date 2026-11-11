#!/usr/bin/env python3
"""Melhoria de nitidez dos sprites de um clipe com super-resolução por IA (Real-ESRGAN
AnimeVideo-v3), pelo procedimento do dono (SOKON_Processo_Melhoria_HD_Sprites, 11/11/2026):

- só a COR de cada quadro passa pelo modelo; a transparência (alfa) é a da fonte, byte a byte;
- a mesma quantidade, ordem e tamanho de quadros, a mesma raiz e o mesmo pixelScale: a folha
  nova tem a geometria da antiga e o personagem não muda de tamanho nem de lugar;
- o RGB invisível é preenchido com a cor do pixel visível mais próximo antes do modelo (sem halo);
- o modelo amplia 4x e a cor volta ao tamanho da célula reduzida com o alfa pré-multiplicado.

A fonte de hoje já está em 2x (pixelScale 2), então o modelo trabalha em cima dela e a saída volta
para 2x: é a mesma resolução com o detalhe refeito, não uma folha maior (memória igual).

    python3 tools/sprites/sr_hd.py tools/sprites/clips/player_base_idle.json --pesos realesr-animevideov3.pth

Escreve a fonte nova ao lado da antiga (<fonte>_sr_hd.png), um manifesto com hashes
(docs/art/sr-hd/<fonte>_sr_hd.json) e troca o "source" do clipe (a fonte antiga fica em sourceReferences). Depois:
python3 tools/sprites/build_characters.py --write.
"""
import argparse
import hashlib
import json
import math
import time
from pathlib import Path

import numpy as np
import torch
from PIL import Image
from scipy.ndimage import distance_transform_edt
from torch import nn
from torch.nn import functional as F

RAIZ = Path(__file__).resolve().parents[2]
SHA_PESOS = 'b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d'
URL_PESOS = 'https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesr-animevideov3.pth'
AMPLIA = 4


class SRVGGNetCompact(nn.Module):
    """A rede do realesr-animevideov3 (Real-ESRGAN, BSD-3): convoluções 3x3 com PReLU e PixelShuffle."""

    def __init__(self, num_in_ch=3, num_out_ch=3, num_feat=64, num_conv=16, upscale=4):
        super().__init__()
        self.upscale = upscale
        self.body = nn.ModuleList([nn.Conv2d(num_in_ch, num_feat, 3, 1, 1), nn.PReLU(num_parameters=num_feat)])
        for _ in range(num_conv):
            self.body.append(nn.Conv2d(num_feat, num_feat, 3, 1, 1))
            self.body.append(nn.PReLU(num_parameters=num_feat))
        self.body.append(nn.Conv2d(num_feat, num_out_ch * upscale * upscale, 3, 1, 1))
        self.upsampler = nn.PixelShuffle(upscale)

    def forward(self, x):
        out = x
        for camada in self.body:
            out = camada(out)
        return self.upsampler(out) + F.interpolate(x, scale_factor=self.upscale, mode='nearest')


def sha(caminho):
    return hashlib.sha256(Path(caminho).read_bytes()).hexdigest()


def modelo(pesos):
    if sha(pesos) != SHA_PESOS:
        raise SystemExit(f'pesos com SHA-256 diferente do esperado ({SHA_PESOS}); baixe de {URL_PESOS}')
    rede = SRVGGNetCompact()
    estado = torch.load(pesos, map_location='cpu')
    rede.load_state_dict(estado.get('params', estado), strict=True)
    return rede.eval()


def preencher_invisivel(rgba):
    """O RGB onde o alfa é zero vira a cor do pixel visível mais próximo (sem halo no modelo)."""
    alfa = rgba[..., 3]
    if not (alfa > 0).any():
        return rgba[..., :3].copy()
    _, (iy, ix) = distance_transform_edt(alfa == 0, return_indices=True)
    return rgba[..., :3][iy, ix]


def melhorar(rede, celula):
    """Uma célula RGBA (numpy uint8) → a mesma célula com a cor refeita e o alfa intacto."""
    alfa = celula[..., 3]
    if not (alfa > 0).any():
        return celula.copy()
    ys, xs = np.nonzero(alfa)
    m = 4
    y0, y1 = max(0, ys.min() - m), min(alfa.shape[0], ys.max() + 1 + m)
    x0, x1 = max(0, xs.min() - m), min(alfa.shape[1], xs.max() + 1 + m)
    parte = celula[y0:y1, x0:x1]
    rgb = preencher_invisivel(parte).astype(np.float32) / 255.0
    with torch.inference_mode():
        t = torch.from_numpy(rgb).permute(2, 0, 1).unsqueeze(0)
        grande = rede(t).clamp_(0, 1).squeeze(0).permute(1, 2, 0).numpy()
    h, w = parte.shape[:2]
    # O alfa na escala do modelo (Lanczos do original) só para pré-multiplicar a redução.
    a4 = np.asarray(Image.fromarray(parte[..., 3]).resize((w * AMPLIA, h * AMPLIA), Image.Resampling.LANCZOS),
                    dtype=np.float32) / 255.0
    pre = np.dstack([grande * a4[..., None], a4])
    reduz = []
    for c in range(4):
        canal = Image.fromarray(pre[..., c].astype(np.float32), mode='F')
        reduz.append(np.asarray(canal.resize((w, h), Image.Resampling.BOX), dtype=np.float32))
    cor, ar = np.dstack(reduz[:3]), reduz[3]
    cor = np.where(ar[..., None] > 1e-4, cor / np.maximum(ar[..., None], 1e-4), rgb)
    saida = celula.copy()
    saida[y0:y1, x0:x1, :3] = np.clip(np.round(cor * 255.0), 0, 255).astype(np.uint8)
    saida[y0:y1, x0:x1, 3] = parte[..., 3]
    return saida


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('clipe', type=Path)
    p.add_argument('--pesos', type=Path, required=True)
    p.add_argument('--threads', type=int, default=0)
    a = p.parse_args()
    if a.threads:
        torch.set_num_threads(a.threads)
    clipe = json.loads(a.clipe.read_text(encoding='utf-8'))
    fonte = RAIZ / clipe['source']
    if fonte.stem.endswith('_sr_hd'):
        raise SystemExit(f'{fonte.name} já é uma fonte melhorada')
    n, col = clipe['expectedFrames'], clipe['columns']
    cw, ch = clipe['frameWidth'], clipe['frameHeight']
    folha = np.asarray(Image.open(fonte).convert('RGBA'))
    linhas = math.ceil(n / col)
    if folha.shape[1] != col * cw or folha.shape[0] != linhas * ch:
        raise SystemExit(f'geometria não bate: {folha.shape[1]}x{folha.shape[0]} contra {col}x{cw} por {linhas}x{ch}')
    rede = modelo(a.pesos)
    nova = folha.copy()
    inicio = time.time()
    for i in range(n):
        y, x = (i // col) * ch, (i % col) * cw
        nova[y:y + ch, x:x + cw] = melhorar(rede, folha[y:y + ch, x:x + cw])
        print(f'  quadro {i + 1}/{n} ({time.time() - inicio:.0f} s)', flush=True)
    # Conferência obrigatória: o alfa de cada quadro é o da fonte, byte a byte.
    if not np.array_equal(nova[..., 3], folha[..., 3]):
        raise SystemExit('o alfa mudou: abortado')
    saida = fonte.with_name(fonte.stem + '_sr_hd.png')
    Image.fromarray(nova, 'RGBA').save(saida)
    manifesto = {
        'clipe': clipe['id'], 'quadros': n, 'colunas': col, 'celula': [cw, ch],
        'raiz': [clipe.get('rootX'), clipe.get('rootY')], 'pixelScale': clipe.get('pixelScale'),
        'modelo': 'realesr-animevideov3 (SRVGGNetCompact 4x)', 'pesos_sha256': SHA_PESOS,
        'fonte': clipe['source'], 'fonte_sha256': sha(fonte),
        'saida': str(saida.relative_to(RAIZ)), 'saida_sha256': sha(saida),
        'alfa': 'idêntico à fonte (conferido byte a byte)',
        'processo': 'cor 4x pelo modelo sobre a fonte 2x, reduzida de volta à célula com alfa pré-multiplicado',
    }
    # O manifesto fica em docs/art/sr-hd (a pasta das fontes só aceita arte referenciada por um clipe).
    pasta = RAIZ / 'docs' / 'art' / 'sr-hd'
    pasta.mkdir(parents=True, exist_ok=True)
    (pasta / (saida.stem + '.json')).write_text(json.dumps(manifesto, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    refs = clipe.get('sourceReferences') or []
    if clipe['source'] not in refs:
        refs = [clipe['source']] + refs
    clipe['sourceReferences'] = refs
    clipe['source'] = str(saida.relative_to(RAIZ))
    a.clipe.write_text(json.dumps(clipe, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'{n} quadros em {saida} ({time.time() - inicio:.0f} s); clipe atualizado')


if __name__ == '__main__':
    main()
