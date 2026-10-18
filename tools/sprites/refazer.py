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
4. Tamanho: régua da cabeça (cabeca.py), por trecho: um tamanho só no clipe, com degrau só
   onde o vídeo muda de tamanho de verdade (cabeca.degraus), nunca curva. Nível pela imagem
   inicial do vídeo ou, sem ela, pela média da cabeça com o esqueleto. Deitado (KNOCKDOWN,
   GROUNDED, GETUP) fica na escala da receita: a cabeça deitada não é achada direito.
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
SKIP = {'ULTRA_SEQ', 'ULTRA_BEAM'}
LYING = {'KNOCKDOWN', 'GROUNDED', 'GETUP'}


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))


def write(path, data):
    Path(path).write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def old_frames(recipe, total):
    if recipe.get('quadros'):
        return [int(v) for v in str(recipe['quadros']).split(',')]
    return list(range(recipe.get('inicio') or 0, recipe.get('fim') or total, recipe.get('passo', 1)))


def effect_frames(video, lo, hi):
    """Quadros do trecho com efeito desenhado pelo vídeo (clarão, estrela, arco, poeira; o jogo
    desenha os dele), como o personagem.py decide: silhueta bem maior ou mais larga que a do
    resto do trecho, ou muito mais branco/amarelo claro."""
    import personagem
    d = personagem.analyse(str(video))
    seg = slice(lo, hi + 1)
    area = d['masks'].reshape(len(d['masks']), -1).sum(1).astype(float)
    width = d['info'][:, 3] - d['info'][:, 2]
    light = d['bright'].astype(float)
    ref_a, ref_w, ref_l = np.median(area[seg]), np.median(width[seg]), np.median(light[seg])
    return {q for q in range(lo, hi + 1)
            if area[q] > 1.3 * ref_a or width[q] > 1.6 * ref_w or light[q] > 1.6 * ref_l + 300}


def new_frames(old, cap, bad=frozenset()):
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
            picks = [a + round(j * (b - a) / (n + 1)) for j in range(1, n + 1)]
            out += [f for f in dict.fromkeys(picks) if f not in bad]
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
    é a posição do quadro antigo k na folha nova; os novos entre k e k+1 vão com o k.
    `starts[j]`: onde o quadro j da animação antiga ficou na nova (impacto, pancadas)."""
    nxt_pos = lambda k: where[k + 1] if k + 1 < len(where) else total
    frames, durations, starts = [], [], []
    old = anim['frames']
    durs = anim.get('durationsMs')
    for j, f in enumerate(old):
        nxt = old[j + 1] if j + 1 < len(old) else None
        # Os novos depois deste quadro entram se o próximo da animação é o vizinho na folha.
        group = list(range(where[f], nxt_pos(f))) if nxt == f + 1 else [where[f]]
        starts.append(len(frames))
        frames += group
        if durs is not None:
            durations += split(int(round(durs[j])), len(group)) if len(group) > 1 else [durs[j]]
    out = dict(anim, frames=frames)
    if durs is not None:
        out['durationsMs'] = durations
    if anim.get('impactFrame') is not None:
        out['impactFrame'] = starts[anim['impactFrame']]
    if 'distancePerFrame' in anim:
        out['distancePerFrame'] = round(anim['distancePerFrame'] * len(old) / len(frames), 2)
    return out, starts


def remap_pancadas(cid, pack, state, starts):
    """pancadas.json (mecanicas.py) guarda índices da animação: vão junto com ela."""
    path = HERE / 'pancadas.json'
    data = read(path)
    for key, h in data['personagens'].get(cid, {}).items():
        anim = (pack['specialAnimations'].get('SUPER') if key == 'SUPER'
                else pack.get('specialMoves', {}).get(key, {}).get('animation'))
        if anim == state and 'pancadas' in h:
            h['pancadas'] = [starts[j] for j in h['pancadas']]
            write(path, data)


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


def redone(cid, videos, states):
    """Folhas já refeitas por este script (para retomar só a etapa de tamanho)."""
    pack = read(ROOT / f'characters/{cid}/character.json')
    out = []
    for p in (HERE / 'clips').glob(f'{cid}_*.json'):
        cfg = read(p)
        if 'refazer.py' not in cfg.get('sourceNote', ''):
            continue
        atlas = Path(cfg['output']).stem
        users = [s for s, a in pack['animations'].items() if a['atlas'] == atlas]
        if states and not set(users) & states:
            continue
        stem = Path(cfg['source']).name.replace('_video_hd.png', '').replace('_video_normalized.png', '')
        out.append((atlas, p, Path(videos) / cfg['video']['arquivo'], None, [],
                    f'art/sprites/source/{stem}_video_normalized.png', None))
    return out


def redo(cid, videos, states, scratch, only_size=False):
    pack_path = ROOT / f'characters/{cid}/character.json'
    pack = read(pack_path)
    anims = pack['animations']
    clips = {}
    for p in (HERE / 'clips').glob(f'{cid}_*.json'):
        cfg = read(p)
        clips[Path(cfg['output']).stem] = (p, cfg)
    if only_size:
        # Só o tamanho, de novo, nos quadros que a folha já tem.
        done = []
        for atlas, path, video, _, olds, ref, _ in redone(cid, videos, states):
            recipe = read(path)['video']
            lying = any(s in LYING for s, a in anims.items() if a['atlas'] == atlas)
            quadros = old_frames(recipe, video_length(video))
            base, reescala, anchored, note = head_scales(cid, video, recipe, quadros, lying, scratch)
            print(f'{atlas}: {note}', flush=True)
            convert_pair(video, path, ['--escala', f'{base:.4f}', '--motivo', note, '--reescala', reescala],
                         ref, scratch)
            done.append((atlas, path, video, lying or anchored, olds, ref, None))
        return size_pass(cid, done, scratch)
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
        bad = effect_frames(video, min(old_q), max(old_q))
        new_q, where = new_frames(old_q, cap, bad)
        lying = any(s in LYING for s in users)
        print(f'{atlas}: {len(old_q)} -> {len(new_q)} quadros ({", ".join(users)}); '
              f'{len(bad)} do trecho com efeito, pulados', flush=True)
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
                              'todos os quadros que o jogo mostra, HD direto do vídeo, tamanho pela régua da cabeça.')
        cfg.pop('minOpaquePixels', None)
        write(path, cfg)
        for state, anim in users.items():
            anims[state], starts = remap_animation(anim, where, len(new_q))
            remap_pancadas(cid, pack, state, starts)
        write(pack_path, pack)
        base, reescala, anchored, note = head_scales(cid, video, recipe, new_q, lying, scratch)
        extra = ['--escala', f'{base:.4f}', '--motivo', note, '--reescala', reescala]
        columns = convert_pair(video, path, extra, ref, scratch)
        done.append((atlas, path, video, lying or anchored, old_sources, ref, columns))
    for _, _, _, _, olds, ref, _ in done:
        for src in olds:
            p = ROOT / src
            keep = {read(c).get('source') for c in (HERE / 'clips').glob('*.json')}
            keep |= {r for c in (HERE / 'clips').glob('*.json') for r in read(c).get('sourceReferences', [])}
            if src not in keep and p.exists():
                p.unlink()
    return size_pass(cid, done, scratch)


_FRAMES = {}


def video_frames(video):
    if video not in _FRAMES:
        import cv2
        cap = cv2.VideoCapture(str(video))
        frames = []
        while True:
            ok, f = cap.read()
            if not ok:
                break
            frames.append(f)
        _FRAMES.clear()
        _FRAMES[video] = frames
    return _FRAMES[video]


ANCHOR_SPAN = 7  # quadros do começo (ou do fim) do vídeo que medem a imagem inicial


def head_zooms(regua_cabeca, video, quadros, scratch=None):
    """Zoom cru da cabeça em cada quadro (None onde não achou), guardado no rascunho: medir é
    o lento (uns segundos por quadro)."""
    import cabeca
    cache = Path(scratch) / 'cabeca.json' if scratch else None
    store = read(cache) if cache and cache.exists() else {}
    key = f'{Path(video).name}:{",".join(map(str, quadros))}'
    if key not in store:
        frames = video_frames(video)
        raw, guess = [], None
        for q in quadros:
            z, nota = regua_cabeca.zoom(frames[q], guess)
            raw.append(z if z and nota >= cabeca.MIN_NOTA else None)
            if raw[-1]:
                guess = raw[-1]
        store[key] = raw
        if cache:
            write(cache, store)
    return store[key]


def anchor(recipe):
    """'começo' ou 'fim': a ponta do vídeo que é a imagem inicial, onde a escala da receita foi
    medida (medida com nota >= 0,9). Depois da primeira conversão a medida sai da receita (a
    escala vira manual) e fica no motivo."""
    import re
    medida = recipe.get('medida', '')
    m = re.search(r'nota ([0-9.]+)', medida)
    if 'inicio_' in medida and m and float(m.group(1)) >= 0.9:
        return 'começo' if medida.startswith('1º') else 'fim'
    m = re.search(r'relativo ao (começo|fim|quadro (\d+)) do vídeo', recipe.get('motivo', ''))
    if m:
        return m.group(1) if not m.group(2) else ('começo' if m.group(2) == '0' else 'fim')
    return None


def head_scales(cid, video, recipe, quadros, lying=False, scratch=None):
    """(escala base, reescala por quadro, ancorado, motivo) pela régua da cabeça.
    Dentro do vídeo: um tamanho só, com degrau onde o vídeo muda de tamanho de verdade
    (cabeca.degraus; o Seedance aumenta a corrida do dash). Nível: o começo (ou o fim) do vídeo,
    quando a receita achou nele a imagem inicial (nota >= 0,9); senão fica para o esqueleto
    (média do clipe), depois da conversão."""
    import cabeca
    if lying:
        # Deitada a cabeça não é achada direito: escala única, a da receita.
        return float(recipe['escala']), ','.join(['1.0000'] * len(quadros)), True, \
            'deitada: escala da receita, única (a régua da cabeça não acha a cabeça deitada)'
    regua_cabeca = cabeca.Regua(cid)
    raw = head_zooms(regua_cabeca, video, quadros, scratch)
    found = [z for z in raw if z]
    if len(found) < max(2, len(raw) // 4):
        # Cabeça quase nunca achada (deitada): tamanho único, o da receita.
        return float(recipe['escala']), ','.join(['1.0000'] * len(quadros)), True, \
            'cabeça não achada no clipe: escala da receita, única'
    # Trava: a cabeça achada no lugar errado dá escala absurda; a receita nunca erra 30%.
    mid = float(np.median(found))
    level = regua_cabeca.key_scale / mid
    if not 0.7 < level / float(recipe['escala']) < 1.3:
        return float(recipe['escala']), ','.join(['1.0000'] * len(quadros)), True, \
            f'régua da cabeça discordou da receita ({level:.4f} x {recipe["escala"]}): escala da receita, única'
    zooms = list(np.clip(cabeca.degraus([z and float(np.clip(z, mid / 1.33, mid * 1.33)) for z in raw]),
                         mid / 1.33, mid * 1.33))
    phases = len(set(zooms))
    inside = 'um tamanho só' if phases == 1 else f'{phases} trechos de tamanho (degrau onde o vídeo muda)'
    end = anchor(recipe)
    if end:
        frames = video_frames(video)
        span = range(ANCHOR_SPAN) if end == 'começo' else range(len(frames) - ANCHOR_SPAN, len(frames))
        seen = [regua_cabeca.zoom(frames[i]) for i in span]
        seen = [z for z, nota in seen if z and nota >= 0.8]
        if seen:
            za = float(np.median(seen))
            base = float(recipe['escala'])
            note = (f'régua da cabeça: {inside}, relativo ao {end} do vídeo (a imagem inicial, escala '
                    f'{base:.4f} medida pela receita)')
            return base, ','.join(f'{z / za:.4f}' for z in zooms), True, note
    note = (f'régua da cabeça: {inside}; nível pelo esqueleto, média do clipe (o vídeo não começa nem '
            'termina na imagem inicial)')
    return level, ','.join(f'{z / mid:.4f}' for z in zooms), False, note


def size_pass(cid, done, scratch):
    build()
    # Nível dos clipes sem imagem inicial: esqueleto, mediana de todos os quadros (a variação
    # dentro do vídeo já saiu pela cabeça, então a mediana é de quadros do mesmo tamanho).
    import esqueleto
    import regua
    p = regua.Pack(cid)
    det = esqueleto.detector()
    reference = esqueleto.reference(det, p)
    changed = False
    for atlas, path, video, fixed, _, ref, _ in done:
        if fixed:
            continue
        values = [v for v in esqueleto.measure(det, p, atlas, reference) if v is not None]
        if not values:
            continue
        level = float(np.median(values))
        print(f'{atlas}: nível pelo esqueleto {level:.0%}', flush=True)
        if abs(level - 1) <= 2 * esqueleto.TOLERANCE:
            continue
        cfg = read(path)
        v = cfg['video']
        # Duas réguas de nível, com erros para lados opostos (p02 intro: cabeça -9%, esqueleto +5%
        # contra o ajuste no olho): a conversão saiu no nível da cabeça; vai à média geométrica.
        base = float(v['escala']) / level ** 0.5
        note = v.get('motivo', '') + (f'; esqueleto {level * 100:.0f}%: escala base na média das duas réguas '
                                      f'{base:.4f}')
        reescala = ','.join(f'{x:.4f}' for x in v['reescala'])
        convert_pair(video, path, ['--escala', f'{base:.4f}', '--motivo', note, '--reescala', reescala], ref, scratch)
        changed = True
    if changed:
        build()
    return [a for a, *_ in done]


MAX_SIDE = 4096  # lado máximo de textura que todo aparelho aceita


def fits_hd(cfg):
    """A folha HD (densidade 2) cabe em MAX_SIDE nos dois lados?"""
    v = cfg['video']
    w = v.get('largura', v.get('celula', 256)) * 2
    h = v.get('celula', 256) * 2
    n = len(str(v['quadros']).split(','))
    cols = max(1, MAX_SIDE // w)
    return -(-n // cols) * h <= MAX_SIDE


def convert_pair(video, path, extra, ref, scratch, grow=3):
    """1x de referência e HD com a mesma receita; a HD guarda a 1x em referencia. Clipe com
    quadros demais para caber em HD (intro, vitória...) sai em 1x direto, sem referência."""
    cfg = read(path)
    if not fits_hd(cfg):
        v = cfg['video']
        v.pop('referencia', None)
        v['colunas'] = max(1, MAX_SIDE // v.get('largura', v.get('celula', 256)))
        cfg['sourceReferences'] = []
        write(path, cfg)
        convert(video, path, extra + ['--densidade', '1'])
        if (ROOT / ref).exists():
            (ROOT / ref).unlink()
        import regua
        pad = regua.margin_pad(regua.resolved(read(path)))
        if any(pad) and grow:
            cfg = read(path)
            v = cfg['video']
            left, top, right, bottom = (x + 24 if x else 0 for x in pad)
            rx, ry = (int(x) for x in v.get('raiz', '128,238').split(','))
            v['raiz'] = f'{rx + left},{ry + top}'
            v['largura'] = v.get('largura', v.get('celula', 256)) + left + right
            v['celula'] = v.get('celula', 256) + top + bottom
            write(path, cfg)
            return convert_pair(video, path, extra, ref, scratch, grow - 1)
        return read(path)['columns']
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
    # Quadro novo que passa da célula (perna esticada, chute): célula maior e de novo.
    import regua
    pad = regua.margin_pad(regua.resolved(read(path)))
    if any(pad) and grow:
        cfg = read(path)
        v = cfg['video']
        # px lógicos (HD = 2x); o quadro cortado na borda esconde quanto falta: folga de 24 px.
        left, top, right, bottom = (int(np.ceil(x / 2)) + 24 if x else 0 for x in pad)
        rx, ry = (int(x) for x in v.get('raiz', '128,238').split(','))
        v['raiz'] = f'{rx + left},{ry + top}'
        v['largura'] = v.get('largura', v.get('celula', 256)) + left + right
        v['celula'] = v.get('celula', 256) + top + bottom
        write(path, cfg)
        print(f'{path.stem}: célula aumentada em {pad} px (quadro passava da borda)', flush=True)
        return convert_pair(video, path, extra, ref, scratch, grow - 1)
    return columns


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('personagem')
    ap.add_argument('videos', help='pasta com os vídeos do personagem (nomes da receita)')
    ap.add_argument('estados', nargs='*')
    ap.add_argument('--rascunho', default=str(ROOT / 'android/app/build/refazer'))
    ap.add_argument('--so-tamanho', action='store_true',
                    help='só o tamanho de novo (régua da cabeça e esqueleto) nas folhas já refeitas')
    args = ap.parse_args()
    Path(args.rascunho).mkdir(parents=True, exist_ok=True)
    done = redo(args.personagem, args.videos, set(args.estados), args.rascunho, args.so_tamanho)
    print(f'{len(done)} folhas refeitas: ' + ', '.join(done))


if __name__ == '__main__':
    main()
