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
guarda do IDLE do pacote e, por último, com a cabeça do IDLE (a maioria dos quadros tem de
concordar). Se nada bater, para com erro em vez de adivinhar.

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
ENDS_SCORE = 0.6    # as duas pontas batem com a imagem inicial um pouco abaixo de MIN_SCORE...
ENDS_SPREAD = 1.01  # ... e concordam no zoom: vale (vídeo gerado maior, traço muda um pouco)
GUARD_MIN_SCORE = 0.6   # guarda do IDLE achada no meio do vídeo (pose parecida, não igual)
GUARD_SPREAD = 1.03     # ... e os 3 melhores quadros concordam na escala
HEAD_SCORE = 0.7        # última tentativa: a cabeça do IDLE convertido achada no vídeo...
HEAD_AGREE = 3          # ... em pelo menos 3 quadros...
HEAD_SPREAD = 1.05      # ... que concordam na escala


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
    """Quadros (RGBA, no tamanho oficial) de um estado do pacote do personagem. Sem pacote
    ainda (personagem novo), lê o clipe <pacote>_<estado> direto: basta o IDLE convertido."""
    path = ROOT / 'characters' / pacote / 'character.json'
    char = json.loads(path.read_text(encoding='utf-8')) if path.exists() else None
    atlas = char['animations'][state]['atlas'] if char else f'{pacote}_{state.lower()}'
    for clip in (ROOT / 'tools/sprites/clips').glob('*.json'):
        cfg = json.loads(clip.read_text(encoding='utf-8'))
        if Path(cfg['output']).stem == atlas:
            break
    else:
        raise SystemExit(f'{pacote}/{state}: sem clipe {atlas}; converta primeiro o IDLE '
                         f'(vídeo que começa no inicio_centro)')
    sheet = rgba_png(ROOT / cfg['source'])
    w, h = cfg.get('frameWidth', 256), cfg.get('frameHeight', 256)
    cols, n = cfg['columns'], cfg['expectedFrames']
    frames = char['animations'][state]['frames'] if char else list(range(n))
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
    a, b = ends['1º quadro'], ends['último quadro']
    if min(a['nota'], b['nota']) >= ENDS_SCORE and max(a['escala'], b['escala']) / min(a['escala'], b['escala']) <= ENDS_SPREAD \
            and a['medida'].split(' = ')[1] == b['medida'].split(' = ')[1]:
        return dict(best, escala=(a['escala'] + b['escala']) / 2,
                    medida=f"1º e último quadro = {a['medida'].split(' = ')[1]}, mesmo zoom")
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
    # Pose que não passa pela guarda (corrida, golpe do começo ao fim): a cabeça do IDLE já
    # convertido (no tamanho do jogo) achada nos quadros do vídeo. Cabeça muda pouco com a pose.
    heads = head_scales(pack_frames(cfg['pacote'], 'IDLE', lambda n: [0]), frames, keyer)
    if len(heads) >= HEAD_AGREE:
        heads.sort(key=lambda h: h[1])
        for k in range(len(heads) - HEAD_AGREE + 1):
            group = heads[k:k + HEAD_AGREE]
            if group[-1][1] / group[0][1] <= HEAD_SPREAD:
                more = [h for h in heads if group[0][1] / HEAD_SPREAD <= h[1] <= group[-1][1] * HEAD_SPREAD]
                if len(more) * 2 >= len(heads):  # a maioria dos quadros com cabeça concorda
                    escala = float(np.median([h[1] for h in more]))
                    return dict(escala=escala, nota=float(np.median([h[0] for h in more])), zoom=1 / escala,
                                medida=f'cabeça do IDLE em {len(more)} quadros')
                break
    raise SystemExit(
        f"não achei o personagem no tamanho conhecido (melhor: {best['medida']}, nota {best['nota']:.2f}).\n"
        f"Gere o vídeo de novo a partir de art/keys/{personagem}/inicio_*.png, sem mudar o enquadramento,\n"
        f"ou passe --escala N --motivo '...' (fica gravado no clipe).")


def head_scales(idle, frames, keyer):
    """[(nota, escala)]: em quadros espalhados do vídeo, a escala em que a cabeça do IDLE
    (20% de cima, já no tamanho do jogo) bate melhor; só os quadros com nota boa."""
    ref = crop(idle[0])
    h = max(8, int(ref.shape[0] * 0.2))
    cx = int(np.nonzero(ref[3, :, 3] > 128)[0].mean()) if (ref[3, :, 3] > 128).any() else ref.shape[1] // 2
    head = flat(ref[:h, max(0, cx - int(h * 0.6)):cx + int(h * 0.6)])
    out = []
    for i in range(0, len(frames), max(1, len(frames) // 12)):
        img = flat(crop(keyer(frames[i]), 40))
        best = (-1.0, 1.0)
        for z in np.exp(np.linspace(np.log(1.0), np.log(4.0), 70)):
            t = cv2.resize(head, None, fx=z, fy=z, interpolation=cv2.INTER_CUBIC)
            if t.shape[0] > img.shape[0] or t.shape[1] > img.shape[1]:
                break
            best = max(best, (float(cv2.matchTemplate(img, t, cv2.TM_CCOEFF_NORMED).max()), float(z)))
        if best[0] >= HEAD_SCORE:
            out.append((best[0], 1 / best[1]))
    return out


def calibrar(personagem, videos=(), altura=224, escala_de=None):
    """Escala de cada imagem inicial. A do centro vem da altura da guarda (ou, com
    escala_de, é a mesma do outro personagem: imagens iniciais geradas na mesma escala, e a
    altura sai proporcional); outra chave com a mesma pose (esquerda/direita) é comparada com
    o centro; uma pose diferente (agachado) é achada dentro do vídeo de onde saiu, que começa
    no centro (mesmo reduzida depois)."""
    from video_para_sprite import key as keyer, read_frames
    path = KEYS / personagem / 'tamanho.json'
    old = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    pacote = old.get('pacote') or ('player_base' if personagem == 'p01' else personagem)
    centro = key_image(personagem, 'inicio_centro', keyer)
    if escala_de:
        base = config(escala_de)['chaves']['inicio_centro']
        altura = round(crop(centro).shape[0] * base)
    else:
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
            # Escala do vídeo de onde a chave saiu: pelo inicio_centro ou, se não começa
            # nele, pela medida normal (guarda do IDLE já convertido).
            path.write_text(json.dumps({'pacote': pacote, 'altura': altura, 'chaves': chaves}), encoding='utf-8')
            try:
                video_scale = medir(frames, personagem, keyer)['escala']
            except SystemExit as e:
                print(f'{f.stem}: não deu para medir o vídeo ({e})')
                continue
            # A chave é um quadro do vídeo (às vezes reduzido): o quadro que mais se parece.
            n, z, i = max(match_zoom(ref, keyer(frames[i]), 0.5, 2.2) + (i,) for i in range(0, len(frames), 2))
            if n >= 0.9:
                escala = video_scale * z
                chaves[f.stem] = round(escala, 4)
                print(f'{f.stem}: quadro {i} do vídeo (nota {n:.2f}, zoom {z:.3f}), escala {escala:.4f}')
                break
            print(f'{f.stem}: não achei no vídeo (melhor nota {n:.2f}, quadro {i})')
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
    c.add_argument('--escala-de', help='imagens iniciais na mesma escala deste personagem (altura proporcional)')
    m = sub.add_parser('medir', help='mostra a escala que um vídeo teria')
    m.add_argument('personagem')
    m.add_argument('videos', nargs='+')
    args = p.parse_args()
    if args.cmd == 'calibrar':
        calibrar(args.personagem, args.videos, escala_de=args.escala_de)
    else:
        from video_para_sprite import key as keyer, read_frames
        for v in args.videos:
            try:
                r = medir(read_frames(v)[0], args.personagem, keyer)
            except SystemExit as e:
                print(f'{Path(v).name}: {e}')
                continue
            print(f"{Path(v).name}: escala {r['escala']:.4f}  ({r['medida']}, nota {r['nota']:.2f}, zoom {r['zoom']:.3f})")
