#!/usr/bin/env python3
"""Ultra de cada personagem seguindo o vídeo (tools/sprites/ultras.json).

Escolhe os quadros do vídeo (a cada 3 na ativação, a cada 2 na sequência, sempre os quadros
de golpe; pose parada vira um quadro só), converte a folha ULTRA_SEQ e grava no character.json
a animação e o "ultra" do lutador, já em quadros do jogo (60 fps):

    preparo   tempo da animação (quadros) em que a ativação termina e a investida começa
    golpes    quadros, contados do preparo, em que cada pancada acerta (o último lança)
    duracao   quadros do preparo até o fim da animação
    rival     [quadro, frente, altura] da posição do rival presa à pose (agarrão)
    some      [de, até] em que o rival não aparece; preto: [de, até] com a tela preta
    explosao  [quadro, frente, altura, ângulo] da explosão desenhada pelo jogo
    camera    planos fixos [quadro, frente, altura, zoom] da sequência (corte seco de um para o outro)
    zanzoken  [de, até] em que ela some (deixa a imagem para trás) e reaparece
    recuo     [quadro, distância] em que ela reaparece longe do rival (px do jogo para trás)
    cameraDaLuta  sem os closes do Ultra: a câmera da luta segue os dois
    voo       [quadro, frente, altura(, giro em graus)] do desenho de quem bate (o p13 sobe, gira e
              desce de meteoro)
    deCabeca  [de, até] com o rival deitado de cabeça para baixo (o abraço do p13)
    bracos    [de, até] com os braços dele (folha ULTRA_SEQ_FRENTE) por cima do rival
    quedaFinal  [subida, empurrão] do golpe final no lugar do lançamento alto

A câmera aceita um 5º número por plano: os quadros que ela leva, suave, do plano anterior até
ele (sem ele, corte seco). "velocidade" no personagem troca a do arquivo (o p13, mais lento).

"segurar" ([quadro, quadros do jogo]) segura um quadro da folha mais tempo (a tela preta da p16).

"pular" (quadros do vídeo, [de, até]) tira trechos da folha: a explosão do canhão da p16, que
passa da borda do vídeo e o jogo desenha por fora.

    python3 tools/sprites/ultras.py                         # só mostra os quadros
    python3 tools/sprites/ultras.py p02 --videos DIR --converter   # DIR/p02/ultra.mp4
    python3 tools/sprites/ultras.py --write                 # grava character.json

O vídeo roda a "velocidade" quadros do jogo por quadro do vídeo (2 = 24 fps → 48 fps, 25% mais
rápido que o vídeo). Depois: reescalar.py <id> ULTRA_SEQ (zoom da câmera) e build_characters.py --write.
"""
import argparse
import json
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
DATA = HERE / 'ultras.json'
STATE = 'ULTRA_SEQ'
HOLD_DIFF = 2.5      # diferença média (0-255) abaixo da qual a pose está parada
MAX_GAP = 6          # quadros do vídeo: pose parada ainda ganha um quadro a cada tanto

# Folha: altura e largura da célula, raiz e densidade (1: a folha do Ultra é longa e fica na memória
# a luta inteira; HD custaria ~60 MB por personagem).
SHEET = {
    'p02': dict(celula=352, largura=576, raiz='210,330', densidade=1, peNoChao=True),
    'p03': dict(celula=352, largura=576, raiz='210,330', densidade=1, peNoChao=True),
    'p04': dict(celula=352, largura=576, raiz='210,330', densidade=1, peNoChao=True),
    # p05: o sumiço (rastro borrado) é a pose da investida e tem poucos pixels.
    'p05': dict(celula=352, largura=576, raiz='210,330', densidade=1, peNoChao=True, minOpaco=400),
    'p07': dict(celula=352, largura=576, raiz='230,330', densidade=1, peNoChao=True),
    'p08': dict(celula=352, largura=768, raiz='220,330', densidade=1, peNoChao=True),
    # p09: pula com o rival acima da cabeça (célula mais alta) e avança no agarrão.
    'p09': dict(celula=416, largura=640, raiz='230,394', densidade=1, peNoChao=True),
    'p10': dict(celula=384, largura=576, raiz='230,362', densidade=1),
    # p11: o jaleco abre para trás e a estocada estica com o clarão para a frente.
    'p11': dict(celula=384, largura=640, raiz='250,362', densidade=1, peNoChao=True),
    'p12': dict(celula=352, largura=576, raiz='210,330', densidade=1, peNoChao=True),
    # p16: cartola e canhão de circo grudados nela (o tronco medido erra): posição do vídeo, e a
    # cartola caindo solta fica.
    'p16': dict(celula=352, largura=608, raiz='175,330', densidade=1, fixar='video', objetos=1500),
    # p15: os dois vídeos do Mugen Rendan colados, na câmera dos vídeos H3 (escala manual); os rastros
    # vermelhos dos socos e o clarão dos chutes ficam (o fundo escurece no meio do vídeo).
    'p15': dict(celula=352, largura=480, raiz='170,330', densidade=1, fixar='video', manterEfeitos=True,
                escala=(0.411, 'mesma câmera dos vídeos H3 de 24/10')),
    # p13: ult2 + ult3 + ult4 + o pulo colados (o ult1, o estalo do pescoço, fica de fora); a célula pega
    # o vídeo inteiro (a explosão vai de borda a borda) e o fogo do meteoro fica.
    # p14: vídeo de fundo escuro recortado por ultra_fundo.py; a célula pega o vídeo inteiro (ele
    # pula por cima do rival e chuta do outro lado) e os efeitos (rajadas, giro, tornado) ficam.
    'p14': dict(celula=384, largura=576, raiz='180,336', densidade=1, fixar='video', manterEfeitos=True,
                escala=(0.411, 'mesma câmera dos vídeos H3 do p14'), esfumar=True),
    'p13': dict(celula=368, largura=600, raiz='300,330', densidade=1, fixar='video', manterEfeitos=True,
                escala=(0.4322, 'mesma câmera dos outros vídeos do p13'), esfumar=True),
}


def pack_path(cid):
    return ROOT / 'characters' / cid / 'character.json'


def skipped(spec, v):
    return any(a <= v <= b for a, b in spec.get('pular', []))


def candidates(spec):
    pre = [v for v in range(spec['inicio'], spec['preparo'], 3) if not skipped(spec, v)]
    seq = {v for v in range(spec['preparo'], spec['fim'] + 1, 2) if not skipped(spec, v)}
    seq |= set(spec['golpes']) | {spec['preparo'], spec['fim']}
    return pre, sorted(seq)


def pick(spec, video=None):
    """Quadros do vídeo que entram na folha. Com o vídeo, pose parada vira um quadro só."""
    pre, seq = candidates(spec)
    if video is None:
        return pre + seq
    import cv2
    import numpy as np
    cap, frames = cv2.VideoCapture(str(video)), []
    while True:
        ok, f = cap.read()
        if not ok:
            break
        frames.append(cv2.resize(cv2.cvtColor(f, cv2.COLOR_BGR2GRAY), (320, 180)).astype(np.float32))
    must = set(spec['golpes']) | {spec['preparo'], spec['fim']} | {v for v, _ in spec.get('segurar', [])}
    kept = [seq[0]]
    for v in seq[1:]:
        still = float(np.abs(frames[v] - frames[kept[-1]]).mean()) < HOLD_DIFF
        if v in must or not still or v - kept[-1] >= MAX_GAP:
            kept.append(v)
    return pre + kept


def timeline(spec, picked, speed):
    """Duração de cada quadro (ms) e o "ultra" do lutador em quadros do jogo."""
    ms = 1000.0 / 60.0
    nxt = picked[1:] + [picked[-1] + 2]
    # Velocidade fracionária (o p13): os quadros do jogo arredondam.
    whole = (lambda v: int(round(v))) if speed != int(speed) else (lambda v: v)
    # "segurar": [quadro do vídeo, quadros do jogo a mais] (a tela preta da p16 dura mais).
    holds = {v: extra for v, extra in spec.get('segurar', [])}
    if set(holds) - set(picked):
        raise SystemExit(f"segurar {sorted(set(holds) - set(picked))}: quadros fora da folha")
    durations = [round(((b - a) * speed + holds.get(a, 0)) * ms) for a, b in zip(picked, nxt)]
    start = picked[0]
    at = lambda v: whole((v - spec['preparo']) * speed + sum(e for h, e in holds.items() if h < v))
    prep = whole((spec['preparo'] - start) * speed)
    ultra = {
        'preparo': prep,
        'golpes': [at(g) for g in spec['golpes']],
        'duracao': at(nxt[-1]),
    }
    if spec.get('rival'):
        ultra['rival'] = [[at(v), dx, dy] for v, dx, dy in spec['rival']]
    if spec.get('some'):
        ultra['some'] = [[at(a), at(b)] for a, b in spec['some']]
    if spec.get('preto'):
        ultra['preto'] = [at(v) for v in spec['preto']]
    if spec.get('explosao'):
        v, dx, dy, angle = spec['explosao']
        ultra['explosao'] = [at(v), dx, dy, angle]
    if spec.get('camera'):
        # 5º número: quadros do vídeo que a câmera leva do plano anterior até este (o p13).
        ultra['camera'] = [[max(0, at(k[0]))] + k[1:4] + ([whole(k[4] * speed)] if len(k) > 4 else [])
                           for k in spec['camera']]
    if spec.get('zanzoken'):
        ultra['zanzoken'] = [[at(a), at(b)] for a, b in spec['zanzoken']]
    if spec.get('recuo'):
        v, distance = spec['recuo']
        ultra['recuo'] = [at(v), distance]
    if spec.get('cameraDaLuta'):
        ultra['cameraDaLuta'] = True
    # O Ultra do p13: voo de quem bate (só o desenho), rival de cabeça para baixo, braços na frente
    # dele e a queda curta do golpe final.
    if spec.get('voo'):
        ultra['voo'] = [[at(k[0])] + k[1:] for k in spec['voo']]   # 4º número: giro (graus)
    for key in ('deCabeca', 'bracos'):
        if spec.get(key):
            ultra[key] = [at(v) for v in spec[key]]
    if spec.get('quedaFinal'):
        ultra['quedaFinal'] = spec['quedaFinal']
    return durations, ultra


def profile(cid):
    """Perfil de recorte do personagem (o p03 usa o do p01)."""
    return f'{cid}.json' if (HERE / 'profiles' / f'{cid}.json').exists() else 'player_base.json'


def clip_for(cid, count, quadros):
    path = HERE / 'clips' / f'{cid}_ultra_seq.json'
    clip = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    # A receita antiga não decide a nova conversão; o zoom medido (reescalar.py) e os quadros
    # limpos (limpar.py) ficam se os quadros do vídeo são os mesmos.
    old = clip.pop('video', {})
    if old.get('quadros') == quadros:
        clip['video'] = {k: old[k] for k in ('reescala', 'limpar_quadros', 'limpar_area') if k in old}
    clip.update({
        'id': f'{cid}_ultra_seq', 'javaName': f'{cid.upper()}_ULTRA_SEQ', 'profile': profile(cid),
        'source': f'art/sprites/source/{cid}_ultra_seq_video_normalized.png',
        'output': f'android/app/src/main/res/drawable-nodpi/{cid}_ultra_seq.webp',
        'report': f'tools/sprites/reports/{cid}_ultra_seq.report.json',
        'preview': f'android/app/build/sprite-review/{cid}_ultra_seq-import.png',
        'expectedFrames': count, 'segmentation': 'prepared-grid', 'rootMode': 'authored',
        'sourceNote': f'{cid}: Ultra em sequência, vídeo Seedance ultra.mp4 (pasta animations/{cid} do Drive), '
                      'quadros e golpes em tools/sprites/ultras.json.',
    })
    path.write_text(json.dumps(clip, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    return path


def convert(cid, video, picked):
    sheet = SHEET[cid]
    clip = clip_for(cid, len(picked), ','.join(map(str, picked)))
    data = json.loads(clip.read_text(encoding='utf-8'))
    if sheet.get('minOpaco'):
        data['minOpaquePixels'] = sheet['minOpaco']
    else:
        data.pop('minOpaquePixels', None)
    clip.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    cmd = [sys.executable, str(HERE / 'video_para_sprite.py'), str(video), '--clipe', str(clip),
           '--personagem', cid, '--quadros', ','.join(map(str, picked)),
           '--celula', str(sheet['celula']), '--largura', str(sheet['largura']), '--raiz', sheet['raiz'],
           '--colunas', '8', '--densidade', str(sheet['densidade']), '--limpar-chao']
    if sheet.get('semEfeitos'):
        cmd.append('--sem-efeitos')
    if sheet.get('fixar'):
        cmd += ['--fixar', sheet['fixar']]
    if sheet.get('objetos'):
        cmd += ['--objetos', str(sheet['objetos'])]
    if sheet.get('manterEfeitos'):
        cmd.append('--manter-efeitos')
    if sheet.get('escala'):
        cmd += ['--escala', str(sheet['escala'][0]), '--motivo', sheet['escala'][1]]
    # O que passa da célula (rastro do sumiço, chicote) sai cortado limpo, longe da margem.
    cmd += ['--aparar-borda', '10']
    if sheet.get('peNoChao'):
        cmd.append('--pe-no-chao')  # o zoom da câmera desce o chão do vídeo
    if sheet['densidade'] > 1:
        cmd.append('--nitidez')
    subprocess.run(cmd, check=True, cwd=ROOT)
    if sheet.get('esfumar'):
        esfumar(cid, picked, spec_of(cid))
    if spec_of(cid).get('bracos'):
        frente(cid, picked, spec_of(cid))


def spec_of(cid):
    return json.loads(DATA.read_text(encoding='utf-8'))['personagens'][cid]


def cells(cid):
    """A folha do Ultra (RGBA) e a geometria das células: (imagem, largura, altura, colunas, raiz x, raiz y)."""
    from PIL import Image
    clip = json.loads((HERE / 'clips' / f'{cid}_ultra_seq.json').read_text(encoding='utf-8'))
    img = Image.open(ROOT / clip['source']).convert('RGBA')
    return img, clip['frameWidth'], clip['frameHeight'], clip['columns'], clip['rootX'], clip['rootY']


def esfumar(cid, picked, spec, faixa=36):
    """Apaga aos poucos o que encosta nas bordas do vídeo (a explosão de borda a borda, o fogo
    que sobe além do quadro): sem a linha reta do corte. As bordas são as do vídeo dentro da
    célula, achadas pelo quadro mais largo; a de cima só nos quadros de "esfumarTopo"."""
    import numpy as np
    from PIL import Image
    img, cw, ch, cols, rx, ry = cells(cid)
    a = np.array(img)
    n = len(picked)
    boxes = []
    for i in range(n):
        cell = a[(i // cols) * ch:(i // cols + 1) * ch, (i % cols) * cw:(i % cols + 1) * cw, 3]
        xs = np.nonzero(cell.max(0) > 8)[0]
        boxes.append((xs.min(), xs.max()) if len(xs) else None)
    left = min(b[0] for b in boxes if b)
    right = max(b[1] for b in boxes if b)
    top_frames = spec.get('esfumarTopo', [])
    for i, v in enumerate(picked):
        y0, x0 = (i // cols) * ch, (i % cols) * cw
        cell = a[y0:y0 + ch, x0:x0 + cw].astype(np.float32)
        ramp = np.ones(cw, np.float32)
        k = np.arange(cw, dtype=np.float32)
        ramp = np.minimum(ramp, np.clip((k - left) / faixa, 0, 1))
        ramp = np.minimum(ramp, np.clip((right - k) / faixa, 0, 1))
        alpha = cell[..., 3] * ramp[None, :]
        if any(f <= v <= t for f, t in top_frames):
            ys = np.nonzero(a[y0:y0 + ch, x0:x0 + cw, 3].max(1) > 8)[0]
            if len(ys):
                r = np.arange(ch, dtype=np.float32)
                alpha *= np.clip((r - ys.min()) / (faixa * 2), 0, 1)[:, None]
        a[y0:y0 + ch, x0:x0 + cw, 3] = alpha.astype(np.uint8)
    Image.fromarray(a, 'RGBA').save(ROOT / json.loads((HERE / 'clips' / f'{cid}_ultra_seq.json')
                                                     .read_text(encoding='utf-8'))['source'])
    print(f'{cid}: bordas do vídeo esfumadas (x {left}–{right})')


def frente(cid, picked, spec):
    """Folha ULTRA_SEQ_FRENTE: só os braços dos quadros de "bracos" (vídeo), um quadro por quadro
    do abraço. O jogo desenha o rival entre o corpo e esta folha (o abraço do p13)."""
    import numpy as np
    from PIL import Image
    img, cw, ch, cols, rx, ry = cells(cid)
    a = np.array(img)
    hug = hug_cells(picked, spec)
    out = np.zeros((ch, cw * len(hug), 4), np.uint8)
    for k, i in enumerate(hug):
        y0, x0 = (i // cols) * ch, (i % cols) * cw
        out[:, k * cw:(k + 1) * cw] = bracos(a[y0:y0 + ch, x0:x0 + cw])
    src = f'art/sprites/source/{cid}_ultra_seq_frente_video_normalized.png'
    Image.fromarray(out, 'RGBA').save(ROOT / src)
    clip = json.loads((HERE / 'clips' / f'{cid}_ultra_seq.json').read_text(encoding='utf-8'))
    clip.pop('video', None)
    clip.update({
        'id': f'{cid}_ultra_seq_frente', 'javaName': f'{cid.upper()}_ULTRA_SEQ_FRENTE', 'source': src,
        'output': f'android/app/src/main/res/drawable-nodpi/{cid}_ultra_seq_frente.webp',
        'report': f'tools/sprites/reports/{cid}_ultra_seq_frente.report.json',
        'preview': f'android/app/build/sprite-review/{cid}_ultra_seq_frente-import.png',
        'expectedFrames': len(hug), 'columns': len(hug), 'minOpaquePixels': 500,
        'sourceNote': f'{cid}: os braços do abraço do Ultra, recortados da folha ULTRA_SEQ por tools/sprites/ultras.py.',
    })
    (HERE / 'clips' / f'{cid}_ultra_seq_frente.json').write_text(json.dumps(clip, indent=2, ensure_ascii=False) + '\n',
                                                                encoding='utf-8')
    print(f'{cid}: braços na frente em {len(hug)} quadros')


def hug_cells(picked, spec):
    """Posições da folha (ULTRA_SEQ) dos quadros do abraço."""
    lo, hi = spec['bracos']
    return [i for i, v in enumerate(picked) if lo <= v < hi]


def bracos(cell):
    """Os braços de uma célula do abraço: a pele (braços, ombros e cabeça) e as luvas abaixo
    dos antebraços; o tronco e a calça ficam atrás do rival."""
    import numpy as np
    import cv2
    rgb = cell[..., :3].astype(np.int16)
    alpha = cell[..., 3] > 40
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    skin = alpha & (r > 120) & (r > g + 25) & (g > b + 5)
    ys, xs = np.nonzero(alpha)
    top, bottom = ys.min(), ys.max()
    # A cabeça e os ombros também ficam na frente: as pernas do rival, de cabeça para baixo,
    # sobem por trás da cabeça dele. As luvas só a partir dos ombros.
    shoulder = top + int((bottom - top) * 0.30)
    # As luvas: o escuro logo abaixo da pele dos antebraços (só para baixo: a calça do lado fica).
    down = np.zeros((61, 9), np.uint8)
    down[:31] = 1   # o elemento estende a pele para baixo (a âncora fica no meio)
    near = cv2.dilate(skin.astype(np.uint8), down) > 0
    near |= cv2.dilate(skin.astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
    mask = skin | (near & alpha & (np.arange(cell.shape[0])[:, None] >= shoulder))
    mask = cv2.morphologyEx(mask.astype(np.uint8), cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8)) > 0
    out = np.zeros_like(cell)
    out[mask] = cell[mask]
    return out


def apply(cid, spec, picked, speed, damage):
    path = pack_path(cid)
    pack = json.loads(path.read_text(encoding='utf-8'))
    durations, ultra = timeline(spec, picked, speed)
    pack['animations'][STATE] = {'atlas': f'{cid}_ultra_seq', 'frames': list(range(len(picked))),
                                 'loop': False, 'durationsMs': durations}
    if spec.get('bracos'):
        # Mesmo tempo da ULTRA_SEQ; fora do abraço aponta para o 1º quadro (o jogo nem desenha).
        hug = hug_cells(picked, spec)
        pack['animations']['ULTRA_SEQ_FRENTE'] = dict(pack['animations'][STATE], atlas=f'{cid}_ultra_seq_frente',
                                                      frames=[hug.index(i) if i in hug else 0 for i in range(len(picked))])
    # O Ultra deixa de ser o raio: a folha do raio sai e o "ULTRA" aponta para a sequência.
    pack['animations'].pop('ULTRA_BEAM', None)
    pack.setdefault('specialAnimations', {})['ULTRA'] = STATE
    ultra['dano'] = damage
    ultra['quadrosVideo'] = picked
    pack['fighter']['ultra'] = ultra
    path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('ids', nargs='*')
    p.add_argument('--videos', help='pasta com <id>/ultra.mp4')
    p.add_argument('--converter', action='store_true', help='converte a folha (precisa de --videos)')
    p.add_argument('--write', action='store_true', help='grava animação e "ultra" no character.json')
    args = p.parse_args(argv)
    data = json.loads(DATA.read_text(encoding='utf-8'))
    speed = data['velocidade']
    mech = json.loads((HERE / 'mecanicas.json').read_text(encoding='utf-8'))['personagens']
    for cid, spec in data['personagens'].items():
        if args.ids and cid not in args.ids:
            continue
        video = Path(args.videos) / cid / 'ultra.mp4' if args.videos else None
        clip = HERE / 'clips' / f'{cid}_ultra_seq.json'
        if video is not None and video.exists():
            picked = pick(spec, video)
        elif clip.exists():
            picked = [int(v) for v in json.loads(clip.read_text(encoding='utf-8'))['video']['quadros'].split(',')]
        else:
            picked = pick(spec)
        if args.converter:
            if video is None or not video.exists():
                raise SystemExit(f'{cid}: vídeo {video} não encontrado')
            convert(cid, video, picked)
        damage = round(data['dano'] * mech.get(cid, {}).get('dano', 1.0))
        own = spec.get('velocidade', speed)   # o p13 anda mais devagar que os outros
        durations, ultra = timeline(spec, picked, own)
        print(f"{cid} {spec['nome']}: {len(picked)} quadros, preparo {ultra['preparo']}, "
              f"golpes {ultra['golpes']}, duração {ultra['duracao']}, dano {damage}")
        if args.write:
            apply(cid, spec, picked, own, damage)


if __name__ == '__main__':
    main()
