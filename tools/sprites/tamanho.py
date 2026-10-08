#!/usr/bin/env python3
"""Tamanho oficial do personagem nos vídeos: a escala de cada vídeo é medida, não chutada.

Cada personagem tem art/keys/<id>/tamanho.json com a escala (px da célula do jogo por px
da imagem 1280x720) de cada imagem inicial:

    {"pacote": "p03", "altura": 224,
     "chaves": {"inicio_centro": 0.5385, "inicio_agachado": 0.5264}}

A do inicio_centro é `altura` / altura da guarda nessa imagem. As outras são medidas uma
vez: esquerda/direita contra o centro; o agachado dentro do vídeo de onde foi tirado (o de
agachar, que começa no centro). Gerar ou refazer:

    python3 tools/sprites/tamanho.py calibrar p03 abaixar.mp4

Para um vídeo, `medir` acha a imagem inicial no 1º ou no último quadro (o Seedance começa
na imagem dada) comparando a aparência em várias escalas, e devolve escala = escala da
chave / zoom. Vídeo que não começa nem termina numa imagem inicial é comparado com a
guarda do IDLE do pacote. Se nada bater, para com erro em vez de adivinhar.

Precisa de opencv e numpy: pip install -r tools/sprites/requirements-animar.txt
"""
import argparse
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
KEYS = ROOT / 'art/keys'
MIN_SCORE = 0.8   # semelhança mínima (TM_CCOEFF_NORMED) para aceitar a medida
GUARD_MIN_SCORE = 0.6   # guarda do IDLE achada no meio do vídeo (pose parecida, não igual)
GUARD_SPREAD = 1.03     # ... e os 3 melhores quadros concordam na escala


def flat(rgba):
    """Personagem sobre cinza neutro, para comparar só a aparência."""
    a = rgba[..., 3:4].astype(np.float32) / 255
    return (rgba[..., :3].astype(np.float32) * a + 128 * (1 - a)).astype(np.uint8)


def crop(rgba, pad=0):
    m = rgba[..., 3] > 128
    ys, xs = np.nonzero(m)
    y0, x0 = max(0, ys.min() - pad), max(0, xs.min() - pad)
    return rgba[y0:ys.max() + 1 + pad, x0:xs.max() + 1 + pad]


def match_zoom(ref, frame, lo=0.5, hi=2.2):
    """(nota, zoom): o zoom que faz `ref` (RGBA) bater melhor com `frame` (RGBA).
    Busca grossa em meia resolução e ajuste fino em resolução cheia."""
    t_full, img_full = flat(crop(ref)), flat(crop(frame, 40))

    def score(t0, img, z):
        t = cv2.resize(t0, None, fx=z, fy=z, interpolation=cv2.INTER_AREA if z < 1 else cv2.INTER_CUBIC)
        if t.shape[0] > img.shape[0] or t.shape[1] > img.shape[1] or min(t.shape[:2]) < 8:
            return -1.0
        return float(cv2.matchTemplate(img, t, cv2.TM_CCOEFF_NORMED).max())

    t_half = cv2.resize(t_full, None, fx=0.5, fy=0.5, interpolation=cv2.INTER_AREA)
    img_half = cv2.resize(img_full, None, fx=0.5, fy=0.5, interpolation=cv2.INTER_AREA)
    zs = np.exp(np.linspace(np.log(lo), np.log(hi), 61))
    z = zs[int(np.argmax([score(t_half, img_half, v) for v in zs]))]
    fine = np.linspace(z * 0.97, z * 1.03, 19)
    notes = [score(t_full, img_full, v) for v in fine]
    best = int(np.argmax(notes))
    return notes[best], float(fine[best])


def rgba_png(path):
    return np.array(Image.open(path).convert('RGBA'))


def config(personagem):
    path = KEYS / personagem / 'tamanho.json'
    if not path.exists():
        raise SystemExit(f'{path.relative_to(ROOT)} não existe: rode  python3 tools/sprites/tamanho.py calibrar {personagem}')
    return json.loads(path.read_text(encoding='utf-8'))


def key_image(personagem, name, keyer):
    return keyer(cv2.imread(str(KEYS / personagem / f'{name}.png')))


def pack_frames(pacote, state, picks):
    """Quadros (RGBA, no tamanho oficial) de um estado do pacote do personagem."""
    char = json.loads((ROOT / 'characters' / pacote / 'character.json').read_text(encoding='utf-8'))
    atlas = char['animations'][state]['atlas']
    for clip in (ROOT / 'tools/sprites/clips').glob('*.json'):
        cfg = json.loads(clip.read_text(encoding='utf-8'))
        if Path(cfg['output']).stem == atlas:
            break
    else:
        raise SystemExit(f'{pacote}/{state}: atlas {atlas} sem clipe')
    layout = json.loads((ROOT / cfg['report']).read_text(encoding='utf-8'))['layout']
    sheet = rgba_png(ROOT / cfg['source'])
    w, h, cols, n = layout['frameWidth'], layout['frameHeight'], layout['columns'], layout['frameCount']
    frames = char['animations'][state]['frames']
    idx = [frames[i] for i in picks(len(frames))]
    return [sheet[i // cols * h:(i // cols + 1) * h, i % cols * w:(i % cols + 1) * w] for i in idx if i < n]


def medir(frames, personagem, keyer):
    """Escala do vídeo (lista de quadros BGR) para o tamanho oficial do personagem."""
    cfg = config(personagem)
    ends = {}
    for where, frame in (('1º quadro', keyer(frames[0])), ('último quadro', keyer(frames[-1]))):
        for name, escala in cfg['chaves'].items():
            nota, zoom = match_zoom(key_image(personagem, name, keyer), frame)
            if where not in ends or nota > ends[where]['nota']:
                ends[where] = dict(escala=escala / zoom, nota=nota, zoom=zoom, medida=f'{where} = {name}')
    good = [e for e in ends.values() if e['nota'] >= MIN_SCORE]
    if len(good) == 2 and max(e['escala'] for e in good) / min(e['escala'] for e in good) > 1.03:
        raise SystemExit('a câmera mudou o zoom durante o vídeo (' + ', '.join(
            f"{e['medida']}: {e['escala']:.4f}" for e in good) + '); gere o vídeo de novo com a câmera parada.')
    best = max(ends.values(), key=lambda e: e['nota'])
    if best['nota'] >= MIN_SCORE:
        return best
    # Não começa nem termina numa imagem inicial: procura a guarda do IDLE no vídeo. A nota
    # fica mais baixa (a pose nunca é idêntica), então 3 quadros diferentes têm de concordar.
    guards = pack_frames(cfg['pacote'], 'IDLE', lambda n: [0, n // 3, 2 * n // 3])
    step = max(1, len(frames) // 16)
    found = []
    for i in sorted(set(range(0, len(frames), step)) | {len(frames) - 1}):
        frame = keyer(frames[i])
        nota, zoom = max(match_zoom(g, frame, 0.8, 4.0) for g in guards)
        found.append((nota, 1 / zoom, i))
    top = sorted(found, reverse=True)[:3]
    escalas = [e for _, e, _ in top]
    if top[-1][0] >= GUARD_MIN_SCORE and max(escalas) / min(escalas) <= GUARD_SPREAD:
        return dict(escala=float(np.median(escalas)), nota=top[0][0], zoom=1 / float(np.median(escalas)),
                    medida='quadros ' + ', '.join(str(i) for _, _, i in top) + ' = guarda do IDLE')
    if top[0][0] > best['nota']:
        best = dict(nota=top[0][0], medida='guarda do IDLE sem concordância: ' + ', '.join(
            f'quadro {i} {e:.3f}' for _, e, i in top))
    raise SystemExit(
        f"não achei o personagem no tamanho conhecido (melhor: {best['medida']}, nota {best['nota']:.2f}).\n"
        f"Gere o vídeo de novo a partir de art/keys/{personagem}/inicio_*.png, sem mudar o enquadramento,\n"
        f"ou passe --escala N --motivo '...' (fica gravado no clipe).")


def calibrar(personagem, videos=(), altura=224):
    """Escala de cada imagem inicial. A do centro vem da altura da guarda; outra chave com a
    mesma pose (esquerda/direita) é comparada com o centro; uma pose diferente (agachado) é
    achada dentro do vídeo de onde saiu, que começa no centro."""
    from video_para_sprite import key as keyer, read_frames
    path = KEYS / personagem / 'tamanho.json'
    old = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    pacote = old.get('pacote') or ('player_base' if personagem == 'p01' else personagem)
    centro = key_image(personagem, 'inicio_centro', keyer)
    base = altura / crop(centro).shape[0]
    chaves = {'inicio_centro': round(base, 4)}
    clips = [read_frames(v)[0] for v in videos]
    for f in sorted((KEYS / personagem).glob('inicio_*.png')):
        if f.stem == 'inicio_centro':
            continue
        # A referência é redimensionada pelo zoom até bater: zoom = px do alvo / px da referência.
        ref = keyer(cv2.imread(str(f)))
        nota, zoom = match_zoom(centro, ref, 0.8, 1.25)
        if nota >= 0.9:
            chaves[f.stem] = round(base / zoom, 4)
            print(f'{f.stem}: mesma pose do centro, escala {base / zoom:.4f} (nota {nota:.2f})')
            continue
        for frames in clips:
            start_nota, start_zoom = match_zoom(centro, keyer(frames[0]), 0.8, 1.25)
            if start_nota < MIN_SCORE:
                continue
            for i in range(len(frames)):
                n, z = match_zoom(ref, keyer(frames[i]), 0.9, 1.1)
                if n >= 0.97:
                    escala = base / start_zoom / z
                    chaves[f.stem] = round(escala, 4)
                    print(f'{f.stem}: quadro {i} de um vídeo que começa no centro, escala {escala:.4f}')
                    break
            if f.stem in chaves:
                break
        if f.stem not in chaves:
            if f.stem in old.get('chaves', {}):
                chaves[f.stem] = old['chaves'][f.stem]
                print(f'{f.stem}: mantida a escala anterior {chaves[f.stem]}')
            else:
                print(f'{f.stem}: fica de fora; passe o vídeo (que começa no centro) de onde ela saiu')
    data = {'pacote': pacote, 'altura': altura, 'chaves': chaves}
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'{path.relative_to(ROOT)}: {data}')


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest='cmd', required=True)
    c = sub.add_parser('calibrar', help='mede a escala de cada imagem inicial do personagem')
    c.add_argument('personagem')
    c.add_argument('videos', nargs='*', help='vídeo de onde saiu uma imagem inicial de outra pose')
    m = sub.add_parser('medir', help='mostra a escala que um vídeo teria')
    m.add_argument('personagem')
    m.add_argument('videos', nargs='+')
    args = p.parse_args()
    if args.cmd == 'calibrar':
        calibrar(args.personagem, args.videos)
    else:
        from video_para_sprite import key as keyer, read_frames
        for v in args.videos:
            try:
                r = medir(read_frames(v)[0], args.personagem, keyer)
            except SystemExit as e:
                print(f'{Path(v).name}: {e}')
                continue
            print(f"{Path(v).name}: escala {r['escala']:.4f}  ({r['medida']}, nota {r['nota']:.2f}, zoom {r['zoom']:.3f})")
