#!/usr/bin/env python3
"""Refaz os clipes de um personagem do zero a partir dos vídeos, com o máximo de quadros.

    python3 tools/sprites/refazer.py p02 <pasta dos vídeos>              # todos os clipes
    python3 tools/sprites/refazer.py p02 <pasta dos vídeos> INTRO TAUNT  # só esses
    python3 tools/sprites/build_characters.py --write

Para cada folha (atlas):
1. Quadros: todos os do trecho do vídeo que o clipe usa, até o que o jogo consegue mostrar
   (60 por segundo no tempo do clipe; andar e loops por distância: todos). Os quadros de
   antes ficam (âncoras: impacto, ponto do loop); os novos entram entre eles.
2. Tempo: a duração de cada quadro antigo é dividida entre ele e os novos que vêm depois
   dele, então frame data, impacto e ritmo não mudam. impactFrame e distancePerFrame
   acompanham.
3. Conversão do vídeo em HD direto (densidade 2), com a escala da receita e as opções dela
   (recorte, fixar, chão...). As correções de tamanho antigas (reescala da régua) saem: o
   tamanho é refeito pelo esqueleto.
4. Tamanho: tools/sprites/esqueleto.py, quadro a quadro com a curva suavizada, numa
   reconversão só (uma reamostragem). Deitado (KNOCKDOWN, GROUNDED, GETUP) fica na escala
   da receita: o esqueleto não é confiável.
A folha 1x da mesma receita fica como referência da HD (sourceReferences).

Os retoques à mão das folhas antigas se perdem (é do zero). Ultras ficam de fora.
"""
import argparse
import json
import subprocess
import sys
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

FPS_JOGO = 60.0
SKIP = {'IDLE', 'COMBAT', 'ULTRA_SEQ', 'ULTRA_BEAM'}
LYING = {'KNOCKDOWN', 'GROUNDED', 'GETUP'}


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))


def write(path, data):
    Path(path).write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def old_frames(recipe, total):
    if recipe.get('quadros'):
        return [int(v) for v in str(recipe['quadros']).split(',')]
    return list(range(recipe.get('inicio') or 0, recipe.get('fim') or total, recipe.get('passo', 1)))


def new_frames(old, cap):
    """(quadros novos, posição de cada quadro antigo na lista nova): os antigos mais os do
    meio, espalhados, até `cap`. Quadro repetido (pausa) continua repetido; lista fora de
    ordem (loop girado) fica como está."""
    if any(b < a for a, b in zip(old, old[1:])):
        return list(old), list(range(len(old)))
    gaps = [(a, b) for a, b in zip(old, old[1:]) if b - a > 1]
    room = sum(b - a - 1 for a, b in gaps)
    extra = max(0, cap - len(old))
    out, where = [], []
    for k, q in enumerate(old):
        where.append(len(out))
        out.append(q)
        if k + 1 < len(old) and old[k + 1] - q > 1 and room:
            a, b = q, old[k + 1]
            n = b - a - 1 if extra >= room else round(extra * (b - a - 1) / room)
            out += [a + round(j * (b - a) / (n + 1)) for j in range(1, n + 1)]
    return out, where


def video_length(path):
    import cv2
    cap = cv2.VideoCapture(str(path))
    n = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    cap.release()
    return n


def split(total, parts):
    """Divide `total` ms em `parts` inteiros que somam o mesmo."""
    base = [total // parts] * parts
    for i in range(total - sum(base)):
        base[i] += 1
    return base


def remap_animation(anim, where, total):
    """Animação na folha nova: quadros, durações, impacto e distância por quadro. `where[k]`
    é a posição do quadro antigo k na folha nova; os novos entre k e k+1 vão com o k."""
    nxt_pos = lambda k: where[k + 1] if k + 1 < len(where) else total
    frames, durations, impact = [], [], None
    old = anim['frames']
    durs = anim.get('durationsMs')
    for j, f in enumerate(old):
        nxt = old[j + 1] if j + 1 < len(old) else None
        # Os novos depois deste quadro entram se o próximo da animação é o vizinho na folha.
        group = list(range(where[f], nxt_pos(f))) if nxt == f + 1 else [where[f]]
        if anim.get('impactFrame') == j:
            impact = len(frames)
        frames += group
        if durs is not None:
            durations += split(int(round(durs[j])), len(group)) if len(group) > 1 else [durs[j]]
    out = dict(anim, frames=frames)
    if durs is not None:
        out['durationsMs'] = durations
    if impact is not None:
        out['impactFrame'] = impact
    if 'distancePerFrame' in anim:
        out['distancePerFrame'] = round(anim['distancePerFrame'] * len(old) / len(frames), 2)
    return out


def remap_cells(cells, where, total):
    """Células a limpar (efeito): as antigas e as novas entre duas antigas marcadas."""
    marked = sorted(cells)
    out = {where[c] for c in marked}
    for a, b in zip(marked, marked[1:]):
        if b == a + 1:
            out |= set(range(where[a], where[b]))
    return sorted(out)


def convert(video, clip_path, extra, out=None):
    cmd = [sys.executable, str(HERE / 'video_para_sprite.py'), str(video)]
    if out:
        cmd.append(str(out))
    cmd += ['--clipe', str(clip_path)] + extra
    r = subprocess.run(cmd, capture_output=True, text=True, cwd=ROOT)
    if r.returncode:
        raise SystemExit(f'{clip_path}: conversão falhou\n{r.stdout}\n{r.stderr}')
    return r.stdout


def build():
    r = subprocess.run([sys.executable, str(HERE / 'build_characters.py'), '--write'],
                       capture_output=True, text=True, cwd=ROOT)
    if r.returncode:
        raise SystemExit('build falhou:\n' + r.stdout[-3000:] + r.stderr[-3000:])


def scale_args(recipe):
    if 'motivo' in recipe:
        return ['--escala', str(recipe['escala']), '--motivo', recipe['motivo']]
    return ['--escala-da-receita']


def redo(cid, videos, states, scratch):
    pack_path = ROOT / f'characters/{cid}/character.json'
    pack = read(pack_path)
    anims = pack['animations']
    clips = {}
    for p in (HERE / 'clips').glob(f'{cid}_*.json'):
        cfg = read(p)
        clips[Path(cfg['output']).stem] = (p, cfg)
    atlases = []
    for state, anim in anims.items():
        if state in SKIP or (states and state not in states) or anim['atlas'] in atlases:
            continue
        if anim['atlas'] in clips and clips[anim['atlas']][1].get('video'):
            atlases.append(anim['atlas'])
    done = []
    for atlas in atlases:
        path, cfg = clips[atlas]
        recipe = cfg['video']
        video = Path(videos) / recipe['arquivo']
        if not video.exists():
            print(f'{atlas}: sem o vídeo {video.name}, fica como está')
            continue
        users = {s: a for s, a in anims.items() if a['atlas'] == atlas}
        old_q = old_frames(recipe, video_length(video))
        if len(old_q) != cfg['expectedFrames']:
            print(f'{atlas}: receita com {len(old_q)} quadros e folha com {cfg["expectedFrames"]}, fica como está')
            continue
        timed = [sum(a['durationsMs']) for a in users.values() if 'durationsMs' in a]
        cap = max(len(old_q), int(max(timed) / 1000 * FPS_JOGO)) if timed else 10 ** 6
        new_q, where = new_frames(old_q, cap)
        lying = any(s in LYING for s in users)
        print(f'{atlas}: {len(old_q)} -> {len(new_q)} quadros ({", ".join(users)})', flush=True)
        # Receita do zero: sem as correções de tamanho antigas.
        stem = Path(cfg['source']).name
        for tag in ('_sr_hd', '_sr_2x', '_video_hd', '_video_normalized'):
            stem = stem.replace(tag, '')
        stem = stem.replace('.png', '')
        hd = f'art/sprites/source/{stem}_video_hd.png'
        ref = f'art/sprites/source/{stem}_video_normalized.png'
        old_sources = [cfg['source']] + cfg.get('sourceReferences', [])
        for key in ('reescala', 'reguaFatores', 'regua', 'referencia', 'inicio', 'fim', 'passo', 'densidade',
                    'ancorar_pe'):
            recipe.pop(key, None)
        recipe['quadros'] = ','.join(map(str, new_q))
        if recipe.get('limpar_quadros'):
            recipe['limpar_quadros'] = remap_cells(recipe['limpar_quadros'], where, len(new_q))
        if recipe.get('limpar_area'):
            recipe['limpar_area'] = {str(where[int(k)]): v for k, v in recipe['limpar_area'].items()}
        cfg.update(source=hd, sourceReferences=[ref], video=recipe,
                   sourceNote=f'{cid} em vídeo (Seedance), refeito do zero por tools/sprites/refazer.py: '
                              'todos os quadros que o jogo mostra, HD direto do vídeo, tamanho pelo esqueleto.')
        cfg.pop('minOpaquePixels', None)
        write(path, cfg)
        for state, anim in users.items():
            anims[state] = remap_animation(anim, where, len(new_q))
        write(pack_path, pack)
        columns = convert_pair(video, path, scale_args(recipe), ref, scratch)
        done.append((atlas, path, video, lying, old_sources, ref, columns))
    for _, _, _, _, olds, ref, _ in done:
        for src in olds:
            p = ROOT / src
            keep = {read(c).get('source') for c in (HERE / 'clips').glob('*.json')}
            keep |= {r for c in (HERE / 'clips').glob('*.json') for r in read(c).get('sourceReferences', [])}
            if src not in keep and p.exists():
                p.unlink()
    build()
    # Tamanho pelo esqueleto, quadro a quadro, numa reconversão só.
    import esqueleto
    import regua
    p = regua.Pack(cid)
    det = esqueleto.detector()
    reference = esqueleto.reference(det, p)
    for atlas, path, video, lying, _, ref, _ in done:
        if lying:
            continue
        loop = any(a['loop'] for a in p.pack['animations'].values() if a['atlas'] == atlas)
        values = esqueleto.measure(det, p, atlas, reference)
        z = esqueleto.factors(values, loop)
        if z is None or all(abs(f - 1) <= esqueleto.TOLERANCE for f in z):
            print(f'{atlas}: tamanho certo pelo esqueleto')
            continue
        known = [v for v in values if v is not None]
        print(f'{atlas}: esqueleto {np.median(known):.0%} ({min(known):.0%}-{max(known):.0%}) -> 100%', flush=True)
        cfg = read(path)
        cfg['video']['regua'] = [f'esqueleto: {np.median(known) * 100:.0f}% da imagem inicial, '
                                 'cada quadro levado a 100% pela curva suavizada']
        cfg['video'].pop('referencia', None)
        write(path, cfg)
        reescala = ','.join(f'{f:.4f}' for f in z)
        convert_pair(video, path, scale_args(cfg['video']) + ['--reescala', reescala], ref, scratch)
    build()
    return [a for a, *_ in done]


def convert_pair(video, path, extra, ref, scratch):
    """1x de referência e HD com a mesma receita; a HD guarda a 1x em referencia."""
    one = Path(scratch) / f'{path.stem}_1x.json'
    cfg = read(path)
    cfg['video'].pop('referencia', None)
    write(one, cfg)
    convert(video, one, extra + ['--densidade', '1'], out=ROOT / ref)
    columns = read(one)['columns']
    cell = read(one)['video']
    width = cell.get('largura', cell.get('celula', 256))
    height = cell.get('celula', 256)
    root = [int(v) for v in cell.get('raiz', '128,238').split(',')]
    referencia = json.dumps({'folha': ref, 'celula': [width, height], 'raiz': root, 'colunas': columns})
    convert(video, path, extra + ['--densidade', '2', '--referencia', referencia])
    return columns


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('personagem')
    ap.add_argument('videos', help='pasta com os vídeos do personagem (nomes da receita)')
    ap.add_argument('estados', nargs='*')
    ap.add_argument('--rascunho', default=str(ROOT / 'android/app/build/refazer'))
    args = ap.parse_args()
    Path(args.rascunho).mkdir(parents=True, exist_ok=True)
    done = redo(args.personagem, args.videos, set(args.estados), args.rascunho)
    print(f'{len(done)} folhas refeitas: ' + ', '.join(done))


if __name__ == '__main__':
    main()
