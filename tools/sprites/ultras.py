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
    must = set(spec['golpes']) | {spec['preparo'], spec['fim']}
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
    # "segurar": [quadro do vídeo, quadros do jogo a mais] (a tela preta da p16 dura mais).
    holds = {v: extra for v, extra in spec.get('segurar', [])}
    if set(holds) - set(picked):
        raise SystemExit(f"segurar {sorted(set(holds) - set(picked))}: quadros fora da folha")
    durations = [round(((b - a) * speed + holds.get(a, 0)) * ms) for a, b in zip(picked, nxt)]
    start = picked[0]
    at = lambda v: (v - spec['preparo']) * speed + sum(e for h, e in holds.items() if h < v)
    prep = (spec['preparo'] - start) * speed
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
        ultra['camera'] = [[max(0, at(v)), dx, dy, zoom] for v, dx, dy, zoom in spec['camera']]
    if spec.get('zanzoken'):
        ultra['zanzoken'] = [[at(a), at(b)] for a, b in spec['zanzoken']]
    if spec.get('recuo'):
        v, distance = spec['recuo']
        ultra['recuo'] = [at(v), distance]
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


def apply(cid, spec, picked, speed, damage):
    path = pack_path(cid)
    pack = json.loads(path.read_text(encoding='utf-8'))
    durations, ultra = timeline(spec, picked, speed)
    pack['animations'][STATE] = {'atlas': f'{cid}_ultra_seq', 'frames': list(range(len(picked))),
                                 'loop': False, 'durationsMs': durations}
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
        durations, ultra = timeline(spec, picked, speed)
        print(f"{cid} {spec['nome']}: {len(picked)} quadros, preparo {ultra['preparo']}, "
              f"golpes {ultra['golpes']}, duração {ultra['duracao']}, dano {damage}")
        if args.write:
            apply(cid, spec, picked, speed, damage)


if __name__ == '__main__':
    main()
