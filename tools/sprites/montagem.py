#!/usr/bin/env python3
"""Clipes feitos de vários vídeos (golpe com complementos) ou com quadros escolhidos à mão.

    python3 tools/sprites/montagem.py p15 <pasta com os vídeos>                  # todos
    python3 tools/sprites/montagem.py p15 <pasta> special_s3 special_s4          # só esses

A receita fica em tools/sprites/montagens/<id>.json, um bloco por clipe:

    "special_s4": {
      "trechos": [["s4", 0, 41, "fixo"], ["s4_apoio1", 1, 96], ["s4_apoio2", 1, 96]],
      "quadros": "0,4,8-40/4,41,...",
      "opcoes": {"fixar": "video"},
      "animacoes": {"SPECIAL_S4": {"frames": "0-12,12-0", "impacto": 6}}
    }

- trechos: [vídeo sem .mp4, primeiro quadro, fim exclusivo]. Cada vídeo de complemento
  começa no último quadro do anterior (o quadro final de um é o inicial do outro no Flow),
  então os trechos colados formam um vídeo contínuo, na mesma câmera. "fixo" anula o
  deslocamento lateral do trecho (a voadora atravessa a tela; quem anda no jogo é o motor,
  com o avanço do golpe) e os trechos seguintes ficam no mesmo lugar; "alinha" leva o trecho,
  aos poucos, a terminar com o tronco onde o vídeo colado começou (a guarda do fim volta para
  a raiz, sem pulo ao trocar para o IDLE).
- quadros: os quadros do vídeo colado que viram o clipe ("A-B/N" = de A a B de N em N).
- opcoes: as de conversão do personagem.py (fixar, pe_no_chao, sem_efeitos, recorte_suave,
  deslocar, e "escala" com "motivo" para a escala manual); "_opcoes" no topo da receita vale para
  todos os clipes.
- animacoes: as animações do pacote que usam o clipe. frames são índices do clipe ("9-0" =
  de trás para a frente: a volta pelo inverso do vídeo que só vai até a pose); impacto é a
  posição em frames do quadro em que o golpe encosta; "distancia": "video" (andar) troca de
  quadro pela distância andada, com a distância medida no vídeo (pé plantado); "distancia": "pe"
  mede pelo pé de apoio (vídeo em que a câmera acompanha, como a corrida do dash); "devagar": 1.6
  deixa o ciclo 1,6x mais lento que o pé plantado (o pé escorrega um pouco); "velocidade": "video"
  faz o lutador andar no chão na velocidade do vídeo (fighter.walkSpeed / walkBackSpeed, no lugar
  da do motor): o ciclo toca no tempo do vídeo com o pé plantado; "tempo": "video" deixa o andar
  do motor e acerta a distância por quadro para o ciclo tocar no tempo do vídeo (o pé escorrega
  se o passo do vídeo for curto; "acelerar": 2 toca o ciclo 2x mais rápido). A mesma folha
  pode servir a mais de uma animação (o Super e o Ultra do p15 usam a do S1).

Um vídeo só, sem "fixo", é convertido direto do arquivo. O vídeo colado vai para
android/app/build/video-cache/montagem/<id>_<clipe>.mp4 (sem perda) e a receita do clipe grava esse
nome; para refazer, rode este script de novo. O tempo de cada quadro sai do vídeo (o de cada
quadro do vídeo colado); depois tools/sprites/mecanicas.py encaixa o impacto na janela ativa.
"""
import argparse
import json
import subprocess
import sys
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import personagem  # noqa: E402
from mecanicas import fit_impact  # noqa: E402
from video_para_sprite import key, read_frames, torso_x  # noqa: E402

OUT = ROOT / 'android/app/build/video-cache/montagem'
FPS = 24
ENGINE_WALK = dict(walkSpeed=300, walkBackSpeed=220)   # CombatConfig (px/s)


def numbers(spec):
    """"0,4,8-20/4,30-27" → [0, 4, 8, 12, 16, 20, 30, 29, 28, 27]."""
    if isinstance(spec, list):
        return spec
    out = []
    for part in str(spec).replace(' ', '').split(','):
        rng, _, step = part.partition('/')
        a, sep, b = rng.partition('-')
        if not sep:
            out.append(int(a))
            continue
        a, b, n = int(a), int(b), int(step or 1)
        out += list(range(a, b + 1, n) if b >= a else range(a, b - 1, -n))
    return out


def shift(frame, dx):
    """Desloca o quadro dx px na horizontal; o que entra é o verde do fundo."""
    if not dx:
        return frame
    out = np.empty_like(frame)
    out[:] = frame[:, :1] if dx > 0 else frame[:, -1:]
    if dx > 0:
        out[:, dx:] = frame[:, :-dx]
    else:
        out[:, :dx] = frame[:, -dx:]
    return out


def glue(folder, trechos):
    """Quadros dos trechos colados (BGR), com o deslocamento dos trechos fixos."""
    frames, offset, start = [], 0, None
    for item in trechos:
        name, a, b = item[:3]
        src, _ = read_frames(str(Path(folder) / f'{name}.mp4'))
        part = src[a:b]
        if start is None:
            start = torso_x(key(part[0]))
        if 'fixo' in item[3:]:
            xs = np.array([torso_x(key(f)) for f in part])
            t = np.arange(len(xs))
            trend = np.polyval(np.polyfit(t, xs, 1), t)
            moves = [int(round(offset - (v - trend[0]))) for v in trend]
        elif 'alinha' in item[3:]:
            # Termina com o tronco onde o vídeo colado começou (a guarda volta ao lugar da raiz).
            end = start - (torso_x(key(part[-1])) + offset)
            moves = [int(round(offset + end * i / max(1, len(part) - 1))) for i in range(len(part))]
        else:
            moves = [offset] * len(part)
        frames += [shift(f, m) for f, m in zip(part, moves)]
        offset = moves[-1]
    return frames


def write_video(frames, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    h, w = frames[0].shape[:2]
    cmd = ['ffmpeg', '-y', '-loglevel', 'error', '-f', 'rawvideo', '-pix_fmt', 'bgr24', '-s', f'{w}x{h}',
           '-r', str(FPS), '-i', '-', '-c:v', 'libx264rgb', '-qp', '0', '-preset', 'veryfast', str(path)]
    subprocess.run(cmd, input=b''.join(f.tobytes() for f in frames), check=True)


def source(char, folder, name, spec):
    """O vídeo de onde o clipe sai: o próprio arquivo ou o colado."""
    trechos = spec['trechos']
    if len(trechos) == 1 and 'fixo' not in trechos[0][3:]:
        video, a, _ = trechos[0][:3]
        return str(Path(folder) / f'{video}.mp4'), a
    path = OUT / f'{char}_{name}.mp4'
    write_video(glue(folder, trechos), path)
    return str(path), 0


def durations(quadros, frames):
    """ms de cada quadro da animação: o tempo até o próximo quadro escolhido no vídeo."""
    gaps = list(np.diff(quadros)) + [float(np.median(np.abs(np.diff(quadros)))) if len(quadros) > 1 else 1.0]
    return [max(1, round(abs(gaps[i]) * 1000 / FPS)) for i in frames]


def stride(char, name, video, quadros, foot=False):
    """Quanto o corpo anda no jogo a cada quadro do clipe (px do mundo): o avanço do tronco na
    volta inteira do loop (ou a reta dele, se o vídeo acaba no último quadro), na escala da receita
    e no worldScale do perfil. Com essa distância por quadro o pé do andar fica plantado (o motor
    anda, a arte acompanha). foot: pela velocidade do pé de apoio em relação ao tronco, para
    vídeo em que a câmera acompanha o lutador (corrida: o tronco quase não anda na imagem)."""
    frames, _ = read_frames(video)
    nxt = quadros[-1] + (quadros[-1] - quadros[-2] if len(quadros) > 1 else 1)
    if foot:
        slope = support_speed(frames, quadros)
    elif nxt < len(frames):
        # Loop: quanto o tronco avança do 1º quadro até o que viria depois do último (a volta
        # inteira); a reta dos quadros erra num passo só, em que o tronco anda aos trancos.
        slope = abs(torso_x(key(frames[nxt])) - torso_x(key(frames[quadros[0]]))) / len(quadros)
    else:
        xs = np.array([torso_x(key(frames[q])) for q in quadros])
        slope = abs(np.polyfit(np.arange(len(xs)), xs, 1)[0])
    clip = json.loads((HERE / f'clips/{char}_{name}.json').read_text(encoding='utf-8'))
    profile = json.loads((HERE / f"profiles/{clip['profile']}").read_text(encoding='utf-8'))
    return round(float(slope * clip['video']['escala'] * profile['worldScale']), 2)


def support_speed(frames, quadros):
    """px de vídeo por quadro escolhido que o pé no chão corre para trás em relação ao tronco:
    a mediana entre quadros seguidos em que o mesmo pé continua embaixo."""
    low = []
    for q in quadros:
        k = key(frames[q])
        ys, xs = np.nonzero(k[..., 3] > 40)
        bottom = ys.max()
        low.append((xs[ys >= bottom - 6].mean() - torso_x(k), bottom))
    moves = [x1 - x0 for (x0, b0), (x1, b1) in zip(low, low[1:]) if abs(x1 - x0) < 80 and abs(b1 - b0) <= 6]
    if not moves:
        raise SystemExit('distancia "pe": nenhum par de quadros com o mesmo pé no chão')
    return abs(float(np.median(moves)))


def update_pack(char, name, spec, distance=None):
    path = ROOT / f'characters/{char}/character.json'
    if not path.exists():
        personagem.update_pack(char, set())
    pack = json.loads(path.read_text(encoding='utf-8'))
    quadros = numbers(spec['quadros'])
    for state, a in spec.get('animacoes', {}).items():
        frames = numbers(a.get('frames', f'0-{len(quadros) - 1}'))
        if max(frames) >= len(quadros):
            raise SystemExit(f'{name}/{state}: quadro {max(frames)} além dos {len(quadros)} do clipe')
        old = pack['animations'].get(state, {})
        anim = {'atlas': f'{char}_{name}', 'frames': frames, 'loop': bool(a.get('loop', False))}
        if 'distancia' in a:   # andar: o quadro troca pela distância andada, não pelo tempo
            anim['loop'] = True
            anim['distancePerFrame'] = distance if a['distancia'] in ('video', 'pe') else a['distancia']
            if 'devagar' in a:   # anima mais devagar que o pé plantado (o pé escorrega um pouco)
                anim['distancePerFrame'] = round(anim['distancePerFrame'] * a['devagar'], 2)
            step = float(np.median(np.abs(np.diff(quadros)))) if len(quadros) > 1 else 1.0
            key = 'walkBackSpeed' if state == 'WALK_BACK' else 'walkSpeed'
            if a.get('velocidade') == 'video':
                # Anda no chão na velocidade do vídeo: o ciclo toca no tempo do vídeo, pé plantado.
                pack['fighter'][key] = round(anim['distancePerFrame'] * FPS / step, 1)
            elif a.get('tempo') == 'video':
                # Anda na velocidade do motor e o ciclo toca no tempo do vídeo (o pé escorrega
                # se o passo do vídeo for mais curto que o andar do motor).
                pack['fighter'].pop(key, None)
                # "acelerar": 2 toca o ciclo 2x mais rápido que o vídeo (o pé escorrega menos).
                anim['distancePerFrame'] = round(ENGINE_WALK[key] * step / FPS / a.get('acelerar', 1), 2)
            pack['animations'][state] = anim
            continue
        same = old.get('atlas') == anim['atlas'] and old.get('frames') == frames \
            and len(old.get('durationsMs', [])) == len(frames)
        anim['durationsMs'] = old['durationsMs'] if same and 'durationsMs' in old else durations(quadros, frames)
        if 'impacto' in a:
            anim['impactFrame'] = a['impacto']
        if a.get('golpe') == 'SUPER':
            # O Super não passa pelo mecanicas.py (mantém o tempo do p01): o impacto vai para
            # o começo da janela ativa dele aqui.
            fit_impact(anim, pack['fighter']['super']['attack'])
        pack['animations'][state] = anim
    path.write_text(json.dumps(pack, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagem')
    p.add_argument('pasta', help='pasta com os vídeos .mp4')
    p.add_argument('clipes', nargs='*', help='clipes a refazer (padrão: todos da receita)')
    args = p.parse_args()
    char = args.personagem
    recipe = json.loads((HERE / f'montagens/{char}.json').read_text(encoding='utf-8'))
    names = args.clipes or [k for k in recipe if not k.startswith('_')]
    distances = {}
    for name in names:
        spec = recipe[name]
        video, first = source(char, args.pasta, name, spec)
        quadros = [q + first for q in numbers(spec['quadros'])]
        opts = {**recipe.get('_opcoes', {}), **spec.get('opcoes', {})}
        # "escala" (com "motivo"): manual, para o vídeo em que a medida não acha a guarda (pose no ar).
        manual = (opts['escala'], opts.get('motivo', 'escala manual da receita')) if 'escala' in opts else None
        msg = personagem.convert(char, name, video, quadros, opts, manual)
        print(f'{name:16} {msg}', flush=True)
        how = {a.get('distancia') for a in spec.get('animacoes', {}).values()}
        if how & {'video', 'pe'}:
            distances[name] = stride(char, name, video, quadros, foot='pe' in how)
            print(f'{name:16} {distances[name]} px por quadro (pé plantado)', flush=True)
    for name in names:
        update_pack(char, name, recipe[name], distances.get(name))
    print(f'characters/{char}/character.json atualizado; rode tools/sprites/mecanicas.py --write {char} '
          f'e tools/sprites/build_characters.py --write')


if __name__ == '__main__':
    main()
