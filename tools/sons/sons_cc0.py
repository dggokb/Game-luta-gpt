#!/usr/bin/env python3
"""Monta os sons de acerto e defesa a partir de pacotes CC0 do OpenGameArt.

Regras (docs/revisao-base/direcao.md, seção 7): uma biblioteca base de impactos para os golpes
normais, com o tamanho do som acompanhando a força (fraco, médio, forte), a defesa com timbre
próprio e o ataque instantâneo (o som começa no 1º quadro da pausa). Os golpes no ar (ar_*),
queda, super e trovão continuam os de tools/sons/gerar_sons.py.

Fontes (todas CC0 1.0, domínio público; créditos em tools/sons/CREDITOS_CC0.md):
- qubodup, "Punch": https://opengameart.org/content/punch (qubodupPunch.7z)
- qubodup, "Impact": https://opengameart.org/content/impact (qubodupImpact.7z)
- AntumDeluge, "Thwack Sounds": https://opengameart.org/content/thwack-sounds (thwack-1.0.zip)

Uso: baixar e extrair os três pacotes numa pasta e rodar
    python3 tools/sons/sons_cc0.py PASTA
(precisa do ffmpeg). Escolha por medição: ataque até o pico < 10 ms, duração e brilho
(centroide espectral) crescendo do fraco para o forte.
"""
import subprocess
import sys
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / 'android/app/src/main/assets/sons/luta'
RATE = 22050


def achar(base, nome):
    hits = sorted(Path(base).rglob(nome))
    if not hits:
        sys.exit(f'não achei {nome} em {base}')
    return str(hits[0])


def montar(saida, camadas, dur, fade, pico):
    """camadas: (arquivo, ganho, tom) — tom < 1 deixa mais grave (e mais longo)."""
    args = ['ffmpeg', '-loglevel', 'error', '-y']
    filtros = []
    for i, (arq, ganho, tom) in enumerate(camadas):
        args += ['-i', arq]
        f = (f'[{i}:a]aformat=channel_layouts=mono,silenceremove=start_periods=1:start_threshold=0.02,'
             f'asetrate={int(44100 * tom)},aresample={RATE},volume={ganho}[c{i}]')
        filtros.append(f)
    mix = ''.join(f'[c{i}]' for i in range(len(camadas)))
    filtros.append(f'{mix}amix=inputs={len(camadas)}:normalize=0,atrim=0:{dur},'
                   f'afade=t=out:st={dur - fade}:d={fade},alimiter=limit={pico}:level=disabled[o]')
    args += ['-filter_complex', ';'.join(filtros), '-map', '[o]', '-ac', '1', '-ar', str(RATE),
             '-c:a', 'pcm_s16le', str(OUT / saida)]
    subprocess.run(args, check=True)


def main(base):
    punch = {n: achar(base, f'qubodupPunch0{n}.flac') for n in range(1, 6)}
    meat = achar(base, 'qubodupImpactMeat02.flac')
    stone = achar(base, 'qubodupImpactStone.flac')
    wood = achar(base, 'qubodupImpactWood.flac')
    thump = achar(base, 'thwack-03.wav')
    # Fraco: soco curto e claro. Médio: soco cheio. Forte: soco grave + pancada surda embaixo.
    montar('acerto_l.wav', [(punch[1], 0.9, 1.08)], 0.16, 0.08, 0.55)
    montar('acerto_m.wav', [(punch[4], 1.0, 1.0), (thump, 0.35, 1.0)], 0.26, 0.12, 0.75)
    montar('acerto_h.wav', [(punch[2], 1.0, 0.92), (meat, 0.6, 0.85), (thump, 0.8, 0.8)], 0.42, 0.2, 0.95)
    # Especial e Super: o forte mais grave e mais longo.
    montar('acerto_s.wav', [(punch[2], 1.0, 0.82), (meat, 0.7, 0.75), (thump, 0.9, 0.7)], 0.55, 0.28, 0.98)
    # Punição e KO: o forte com mais corpo embaixo.
    montar('punish.wav', [(punch[5], 1.0, 0.9), (meat, 0.7, 0.8), (thump, 1.0, 0.72)], 0.5, 0.25, 0.98)
    montar('ko.wav', [(punch[2], 1.0, 0.78), (meat, 0.8, 0.7), (thump, 1.0, 0.62)], 0.8, 0.45, 0.98)
    # Defesa: timbre duro e seco (pedra e madeira), diferente do acerto, curto.
    montar('defesa.wav', [(stone, 0.8, 1.1), (wood, 0.5, 1.2)], 0.14, 0.07, 0.6)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
