#!/usr/bin/env python3
"""Um clipe a partir de vários vídeos emendados: o golpe e os vídeos de apoio que o continuam.

    python3 tools/sprites/emendar.py p15 <pasta> special_s4 s4:15-38 s4_apoio1:0-40:2 s4_apoio2:0-30:2 s4_apoio3:0-30:2
    python3 tools/sprites/emendar.py p15 <pasta> special_s4          # refaz pela receita ("partes")

Cada parte é VÍDEO:INÍCIO-FIM[:PASSO] (quadros do vídeo, FIM incluído). A escala é medida na 1ª
parte (que começa numa imagem inicial); as outras continuam o mesmo enquadramento (o 1º quadro de
um vídeo de apoio é o último do anterior), então usam a mesma escala e a mesma linha do chão — um
apoio que começa no ar não vira chão. Todos os quadros ficam com o tronco na raiz (--fixar
registro): o avanço do golpe é do motor (comportamento.py mede no vídeo).

Converte cada parte numa célula grande, junta os quadros, corta na célula justa (até 768×512) e
grava a folha e a receita do clipe tools/sprites/clips/<id>_<clipe>.json, com "partes".
Não refazer esse clipe com video_para_sprite.py --clipe (só refaria a 1ª parte).
"""
import argparse
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
from personagem import analyse, clip_path  # noqa: E402

Image.MAX_IMAGE_PIXELS = None
BIG = dict(celula=512, largura=768, raiz=(384, 470))
MARGIN = 14
MAX_SIDE = 4096


def medida_padrao(line):
    """'escala 0.55 (1º quadro = inicio_centro, nota 0.97, zoom 0.998)' -> como a receita normal grava."""
    m = re.search(r'\((.*), nota ([\d.]+), zoom [\d.]+\)', line)
    return f'{m.group(1)} (nota {m.group(2)})' if m else line


def parse_part(text):
    m = re.fullmatch(r'(.+?):(\d+)-(\d+)(?::(\d+))?', text)
    if not m:
        raise SystemExit(f'parte inválida: {text} (use VÍDEO:INÍCIO-FIM[:PASSO])')
    name, a, b, step = m.group(1), int(m.group(2)), int(m.group(3)), int(m.group(4) or 1)
    return {'arquivo': name if name.endswith('.mp4') else name + '.mp4', 'quadros': list(range(a, b + 1, step))}


def convert_part(char, folder, part, out, density, escala=None, chao=None, extra=()):
    args = [sys.executable, str(HERE / 'video_para_sprite.py'), str(Path(folder) / part['arquivo']), str(out),
            '--quadros', ','.join(map(str, part['quadros'])), '--celula', str(BIG['celula']),
            '--largura', str(BIG['largura']), '--raiz', '%d,%d' % BIG['raiz'], '--colunas', '8',
            '--fixar', 'registro', '--densidade', str(density), *extra]
    if escala is None:
        args += ['--personagem', char]
    else:
        args += ['--escala', str(escala), '--motivo', 'continuação do vídeo anterior (mesmo enquadramento)']
    if chao is not None:
        args += ['--chao', str(chao)]
    r = subprocess.run(args, capture_output=True, text=True)
    if r.returncode:
        raise SystemExit(f"{part['arquivo']}: {(r.stderr or r.stdout).strip().splitlines()[-1]}")
    first = next(line for line in r.stdout.splitlines() if re.match(r'escala ', line))
    return float(re.search(r'escala (?:manual )?([\d.]+)', first).group(1)), first


def cells(path, n, density):
    sheet = Image.open(path)
    w, h = BIG['largura'] * density, BIG['celula'] * density
    cols = sheet.width // w  # o conversor põe menos colunas para a folha HD caber em 4096 px
    return [sheet.crop((i % cols * w, i // cols * h, (i % cols + 1) * w, (i // cols + 1) * h)) for i in range(n)]


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('pasta')
    p.add_argument('clipe', help='nome do clipe (ex.: special_s4)')
    p.add_argument('partes', nargs='*', help='VÍDEO:INÍCIO-FIM[:PASSO], na ordem')
    p.add_argument('--densidade', type=int, help='padrão: a da receita (ou 2)')
    p.add_argument('--sem-contorno', action='store_true')
    p.add_argument('--sem-nitidez', action='store_true', help='sem o unsharp mask (padrão: o da receita)')
    p.add_argument('--suave', action='store_true', help='recorte suave (padrão: o da receita)')
    args = p.parse_args()
    char = args.personagem
    path = clip_path(char, args.clipe)
    cfg = json.loads(path.read_text(encoding='utf-8'))
    parts = [parse_part(t) for t in args.partes] if args.partes else cfg.get('partes')
    if not parts:
        raise SystemExit('informe as partes (VÍDEO:INÍCIO-FIM)')
    old = cfg.get('video', {})
    d = args.densidade or old.get('densidade', 2)
    nitidez = not args.sem_nitidez and old.get('nitidez', True)
    contorno = not args.sem_contorno and old.get('contorno', True)
    suave = args.suave or old.get('suave', False)
    extra = (['--nitidez'] if nitidez else []) + (['--contorno'] if contorno else []) + (['--suave'] if suave else [])
    # Linha do chão: o pé no 1º quadro da 1ª parte (no vídeo); as outras usam a mesma.
    chao = int(analyse(str(Path(args.pasta) / parts[0]['arquivo']))['info'][0, 0])
    frames, escala, medida = [], None, None
    with tempfile.TemporaryDirectory() as tmp:
        for i, part in enumerate(parts):
            out = Path(tmp) / f'parte{i}.png'
            s, msg = convert_part(char, args.pasta, part, out, d, escala, None if i == 0 else chao, extra)
            if i == 0:
                escala, medida = s, msg
            frames += cells(out, len(part['quadros']), d)
            print(f"{part['arquivo']}: {len(part['quadros'])} quadros ({msg})", flush=True)
    # Célula justa: a caixa de todos os quadros em volta da raiz, com margem.
    rx, ry = BIG['raiz']
    boxes = [f.getchannel('A').point(lambda v: 255 if v > 10 else 0).getbbox() for f in frames]
    boxes = [[v // d for v in b] for b in boxes if b]
    x0, y0 = min(b[0] for b in boxes), min(b[1] for b in boxes)
    x1, y1 = max(b[2] for b in boxes), max(b[3] for b in boxes)
    left, right, up, down = rx - x0 + MARGIN, x1 - rx + MARGIN, ry - y0 + MARGIN, max(18, y1 - ry + MARGIN)
    w = min(BIG['largura'], -(-(left + right) // 32) * 32)
    h = min(BIG['celula'], -(-(up + down) // 32) * 32)
    left = min(left + (w - left - right) // 2, w)
    root = (left, min(h, up + (h - up - down) // 2))
    # Folga de 8 px na largura: o ancorar.py (pé de apoio) alarga um pouco a célula depois.
    cols = max(1, min(len(frames), MAX_SIDE // ((w + 8) * d)))
    rows = -(-len(frames) // cols)
    if rows * h * d > MAX_SIDE:
        raise SystemExit(f'{len(frames)} quadros não cabem numa folha de {MAX_SIDE} px: use passo maior')
    sheet = Image.new('RGBA', (cols * w * d, rows * h * d), (0, 0, 0, 0))
    for n, f in enumerate(frames):
        cell = f.crop(((rx - root[0]) * d, (ry - root[1]) * d, (rx - root[0] + w) * d, (ry - root[1] + h) * d))
        sheet.paste(cell, (n % cols * w * d, n // cols * h * d))
    sheet.save(ROOT / cfg['source'])
    first = parts[0]
    cfg.update(expectedFrames=len(frames), columns=cols, frameWidth=w * d, frameHeight=h * d,
               rootX=root[0] * d, rootY=root[1] * d, pixelScale=float(d),
               sourceNote=f'{char} emendado de {len(parts)} vídeos (Omni Flash 1.1) por tools/sprites/emendar.py; '
                          f'refazer com emendar.py (não com video_para_sprite.py --clipe).')
    if d == 1:  # 1x: sem pixelScale, como o video_para_sprite.py
        cfg.pop('pixelScale', None)
    cfg['video'] = {'arquivo': first['arquivo'], 'personagem': char, 'escala': round(escala, 4),
                    'medida': medida_padrao(medida),
                    'quadros': ','.join(map(str, first['quadros'])), 'celula': h, 'largura': w,
                    'raiz': f'{root[0]},{root[1]}', 'colunas': cols, 'fixar': 'registro', 'densidade': d,
                    'nitidez': nitidez, 'contorno': contorno, 'chao': chao}
    if suave:
        cfg['video']['suave'] = True
    cfg['partes'] = [{'arquivo': part['arquivo'], 'quadros': part['quadros']} for part in parts]
    cfg['partesInicio'] = [sum(len(q['quadros']) for q in parts[:i]) for i in range(len(parts))]
    keep_anchor = json.loads(path.read_text(encoding='utf-8')).get('video', {}).get('ancorar_pe')
    if keep_anchor:
        cfg['video']['ancorar_pe'] = True
    path.write_text(json.dumps(cfg, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    if keep_anchor:  # o pé fixado pelo ancorar.py na conversão anterior: repete
        import ancorar
        print('pé de apoio fixado: ' + ' '.join(f'{v:+d}' for v in ancorar.anchor(char, Path(cfg['output']).stem)))
    print(f'{path.relative_to(ROOT)}: {len(frames)} quadros, célula {w}×{h}, raiz {root}, {cols} colunas')


if __name__ == '__main__':
    main()
