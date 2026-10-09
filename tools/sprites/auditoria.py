#!/usr/bin/env python3
"""Auditoria automática da arte de um personagem: aponta, clipe por clipe, o que está fora
do padrão, para a revisão olhar só o que precisa.

    python3 tools/sprites/auditoria.py p03            # relatório no terminal
    python3 tools/sprites/auditoria.py p01 p02 --folhas android/app/build/auditoria

Mede nas folhas normalizadas (o mesmo que vai para o jogo):
  * tamanho      - 1º e último quadro, quando a pose bate com a guarda ou o agachado do
                   próprio personagem (silhueta parecida), têm de ter a mesma altura deles;
  * pulo         - golpe que começa ou termina em guarda tem o pé de trás onde o idle
                   deixa (senão o sprite "pula" ao trocar de animação);
  * deslize      - durante golpes no chão o pé de apoio (o que menos se mexe) não anda aos
                   poucos (3-15 px por quadro); alargar a base, giro e rasteira não contam;
  * pose         - golpe/reação agachado que fica em pé;
  * efeito       - quadro com cor fora da paleta do personagem a mais que os vizinhos
                   (clarão, arco, poeira, rastro, aura);
  * quebrado     - quadro de outro personagem, quase vazio, cortado na borda da célula ou
                   flutuando no chão;
  * receita      - clipe sem receita de vídeo ou com escala manual.
Com --folhas grava uma folha de conferência só dos clipes com aviso.
"""
import argparse
import json
import os
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(os.environ.get('AUDITORIA_RAIZ') or Path(__file__).resolve().parents[2])
SIZE_WARN, SIZE_ERROR = 0.04, 0.07      # diferença de altura numa pose igual à de referência
POSE_MATCH = 0.88                       # silhuetas (mesma altura) parecidas a partir daqui
HEAD_WARN, HEAD_ERROR = 0.14, 0.18       # cabeça maior/menor que a do idle (estimativa grossa)
JUMP_WARN, JUMP_ERROR = 12, 24          # px: pé de trás no 1º quadro x idle
SLIDE_WARN, SLIDE_ERROR = 12, 24        # px: quanto o pé de apoio anda no golpe (somado)
SLIDE_STEP = 15                         # px por quadro: acima disso é troca de pé, não deslize
CROUCH_TALL = 1.2                       # golpe/reação agachado não passa disso da altura agachada
FX_PIXELS, FX_FACTOR = 300, 2.5            # efeito: px de cor estranha a mais que os vizinhos
FOREIGN_FRAME = 0.15                    # fração de cor estranha: quadro de outro personagem
GROUND = 6                              # px: pé a até isso da raiz conta como no chão
# Animações com deslocamento de propósito (o corpo anda, cai ou é empurrado na arte).
MOVING = {'WALK_FORWARD', 'WALK_BACK', 'DASH', 'BACKDASH', 'JUMP', 'FALL', 'LAND', 'KNOCKDOWN',
          'GROUNDED', 'GETUP', 'HIT_AIR', 'DEFENSE_AIR', 'INTRO', 'VICTORY', 'DEFEAT', 'TAUNT',
          'THROW_TOSS', 'SPECIAL_S4', 'SUPER_WAVE', 'SPECIAL_S2'}
AIR = {'JUMP_LIGHT', 'JUMP_MEDIUM', 'JUMP_HEAVY', 'JUMP_HEAVY_DOWN', 'DEFENSE_AIR', 'HIT_AIR', 'JUMP', 'FALL'}
CROUCHED = {'CROUCH_LIGHT', 'CROUCH_MEDIUM', 'HIT_CROUCH', 'DEFENSE_CROUCH'}
GROUNDED = {'IDLE', 'COMBAT', 'WALK_FORWARD', 'WALK_BACK', 'CROUCH', 'RISE', 'LAND', 'DASH',
            'DEFENSE_STAND', 'DEFENSE_CROUCH', 'HIT_STAND', 'HIT_CROUCH', 'GROUNDED', 'GETUP'}


def load_pack(cid):
    pack = json.loads((ROOT / f'characters/{cid}/character.json').read_text(encoding='utf-8'))
    clips = {}
    for p in (ROOT / 'tools/sprites/clips').glob('*.json'):
        cfg = json.loads(p.read_text(encoding='utf-8'))
        clips[Path(cfg['output']).stem] = cfg
    return pack, clips


class Sheet:
    """Quadros de um atlas (RGBA) com a raiz da célula."""
    cache = {}

    def __init__(self, cfg):
        self.cfg = cfg
        self.w, self.h = cfg.get('frameWidth', 256), cfg.get('frameHeight', 256)
        self.rx, self.ry = cfg.get('rootX', 128), cfg.get('rootY', 238)
        self.cols = cfg['columns']
        src = cfg['source']
        if src not in Sheet.cache:
            Sheet.cache[src] = np.array(Image.open(ROOT / src).convert('RGBA'))
        self.img = Sheet.cache[src]

    def frame(self, i):
        y, x = i // self.cols * self.h, i % self.cols * self.w
        return self.img[y:y + self.h, x:x + self.w]


def measure(rgba, rx, ry):
    a = rgba[..., 3] > 128
    ys, xs = np.nonzero(a)
    if len(ys) == 0:
        return None
    top, bottom = ys.min(), ys.max()
    contact = []
    if bottom >= ry - GROUND:  # pés no chão: colunas opacas nas linhas de baixo, em grupos
        # 10 linhas: os dois pés entram mesmo com um deles um pouco mais alto (pivô, ponta do pé)
        cols = np.nonzero(a[max(0, bottom - 9):bottom + 1].any(0))[0]
        if len(cols):
            groups = np.split(cols, np.nonzero(np.diff(cols) > 6)[0] + 1)
            contact = [float(g.mean()) - rx for g in groups if len(g) >= 3]
    q = (rgba[..., :3][a] // 32).astype(np.int16)
    return dict(mask=a, top=top, bottom=bottom, height=ry - top, area=int(a.sum()),
                back=min(contact) if contact else None, contact=contact,
                colors=q[:, 0] * 64 + q[:, 1] * 8 + q[:, 2],
                edge=bool(a[0].any() or a[-1].any() or a[:, 0].any() or a[:, -1].any()),
                floating=bottom < ry - GROUND)


def silhouette(m, rows=96):
    a = m['mask'][m['top']:m['bottom'] + 1]
    cols = np.nonzero(a.any(0))[0]
    a = a[:, cols.min():cols.max() + 1]
    w = max(1, round(a.shape[1] * rows / a.shape[0]))
    return np.array(Image.fromarray(a.astype(np.uint8) * 255).resize((w, rows), Image.NEAREST)) > 127


def iou(a, b):
    w = max(a.shape[1], b.shape[1])
    pa = np.zeros((a.shape[0], w), bool); pb = np.zeros((b.shape[0], w), bool)
    pa[:, :a.shape[1]] = a; pb[:, :b.shape[1]] = b
    return (pa & pb).sum() / max(1, (pa | pb).sum())


def head_template(m, rgba):
    """Cabeça do idle (20% de cima da silhueta) sobre cinza, para comparar aparência."""
    h = int((m['bottom'] - m['top']) * 0.2)
    cx = int(np.nonzero(m['mask'][m['top'] + 3])[0].mean())
    crop = rgba[m['top']:m['top'] + h, max(0, cx - int(h * 0.6)):cx + int(h * 0.6)]
    return flat(crop)


def flat(rgba):
    a = rgba[..., 3:4].astype(np.float32) / 255
    return (rgba[..., :3].astype(np.float32) * a + 128 * (1 - a)).astype(np.uint8)


def head_scale(tmpl, cells):
    """(escala, nota): mediana, entre os quadros, da escala em que a cabeça de referência
    bate melhor. Tamanho grosso, independente da pose. Precisa de opencv (sem ele: None)."""
    try:
        import cv2
    except ImportError:
        return None
    small = cv2.resize(tmpl, None, fx=0.5, fy=0.5, interpolation=cv2.INTER_AREA)
    per = []
    for c in cells:
        img = cv2.resize(flat(c), None, fx=0.5, fy=0.5, interpolation=cv2.INTER_AREA)
        best = (-1.0, 1.0)
        for z in np.exp(np.linspace(np.log(0.6), np.log(1.7), 31)):
            t = cv2.resize(small, None, fx=z, fy=z, interpolation=cv2.INTER_AREA if z < 1 else cv2.INTER_CUBIC)
            if img.shape[0] < t.shape[0] or img.shape[1] < t.shape[1] or min(t.shape[:2]) < 6:
                continue
            r = float(cv2.matchTemplate(img, t, cv2.TM_CCOEFF_NORMED).max())
            best = max(best, (r, z))
        if best[0] >= 0.6:
            per.append(best)
    if len(per) < 2:
        return None
    return float(np.median([z for _, z in per])), float(np.median([r for r, _ in per]))


def frames_of(pack, clips, state):
    anim = pack['animations'][state]
    cfg = clips[anim['atlas']]
    sheet = Sheet(cfg)
    return [measure(sheet.frame(i), sheet.rx, sheet.ry) for i in anim['frames']], cfg


def references(pack, clips):
    """Poses de referência do próprio personagem: guarda (idle) e agachado (fim do CROUCH)."""
    refs = []
    idle, _ = frames_of(pack, clips, 'IDLE')
    for m in idle[::max(1, len(idle) // 8)]:
        refs.append(('guarda', m, silhouette(m)))
    crouch, _ = frames_of(pack, clips, 'CROUCH')
    refs.append(('agachado', crouch[-1], silhouette(crouch[-1])))
    return refs, idle[0]


def palette(pack, clips):
    """Cores do personagem (RGB em 8 níveis), tiradas de clipes sem efeito. Efeito (clarão,
    poeira, arco, rastro, aura) e quadro de outro personagem aparecem como cor fora dela."""
    count = np.zeros(512)
    for state in ('IDLE', 'WALK_FORWARD', 'CROUCH', 'HIT_STAND'):
        for m in frames_of(pack, clips, state)[0]:
            count += np.bincount(m['colors'], minlength=512)
    return count >= count.sum() * 0.0005


def planted_steps(ms):
    """Quanto o pé de apoio anda em cada troca de quadro. Pé de apoio é o que menos se mexe:
    alargar a base (um pé só anda) não conta, deslize (os dois andam juntos) conta. Troca de
    pé (mais de SLIDE_STEP px: giro, rasteira, passo) e quadro sem pé no chão dão 0."""
    steps = [0.0]
    for i in range(1, len(ms)):
        a, b = ms[i - 1]['contact'], ms[i]['contact']
        step = min((y - x for x in a for y in b), key=abs) if a and b else 0.0
        steps.append(step if abs(step) <= SLIDE_STEP else 0.0)
    return steps


def attack_states(pack):
    names = {m['animation'] for m in list(pack['moves'].values()) + list(pack.get('specialMoves', {}).values())
             if 'animation' in m}
    return names | set(pack.get('specialAnimations', {}).values()) | {'THROW_GRAB', 'THROW_TOSS'}


def audit(cid):
    pack, clips = load_pack(cid)
    refs, idle0 = references(pack, clips)
    idle_anim = pack['animations']['IDLE']
    idle_sheet = Sheet(clips[idle_anim['atlas']])
    tmpl = head_template(idle0, idle_sheet.frame(idle_anim['frames'][0]))
    crouch_anim = pack['animations']['CROUCH']
    crouch_sheet = Sheet(clips[crouch_anim['atlas']])
    crouch_tmpl = head_template(refs[-1][1], crouch_sheet.frame(crouch_anim['frames'][-1]))
    attacks = attack_states(pack)
    known = palette(pack, clips)
    found = []

    def add(state, kind, level, text, frames=()):
        found.append(dict(state=state, kind=kind, level=level, text=text, frames=list(frames)))

    seen_atlas, seen_borrowed = set(), set()
    for state, anim in pack['animations'].items():
        ms, cfg = frames_of(pack, clips, state)
        if any(m is None for m in ms):
            add(state, 'quebrado', 'erro', 'quadro vazio', [i for i, m in enumerate(ms) if m is None])
            continue
        # Receita (uma vez por atlas)
        if anim['atlas'] not in seen_atlas:
            seen_atlas.add(anim['atlas'])
            recipe = cfg.get('video')
            if recipe is None:
                add(state, 'receita', 'info', 'sem receita de vídeo: tamanho não garantido pela medida')
            elif 'motivo' in recipe:
                add(state, 'receita', 'info', f"escala manual {recipe['escala']}: {recipe['motivo']}")
            elif 'guarda do IDLE' in recipe.get('medida', ''):
                add(state, 'receita', 'info', f"escala pela guarda do idle (menos precisa): {recipe['medida']}")
            # Tamanho grosso pela cabeça (pega erro grande mesmo sem pose de referência)
            sheet = Sheet(cfg)
            picks = sorted(set(anim['frames'][::max(1, len(anim['frames']) // 6)] + [anim['frames'][-1]]))
            low = state in CROUCHED or state in ('CROUCH_HEAVY', 'RISE')
            hs = head_scale(crouch_tmpl if low else tmpl, [sheet.frame(i) for i in picks]) \
                if state not in ('IDLE', 'COMBAT', 'CROUCH') else None
            if hs and hs[1] >= 0.7 and abs(hs[0] - 1) >= HEAD_WARN:
                ref = 'do agachado' if low else 'do idle'
                add(state, 'tamanho', 'erro' if abs(hs[0] - 1) >= HEAD_ERROR else 'aviso',
                    f'cabeça {hs[0] - 1:+.0%} do tamanho da {ref} (nota {hs[1]:.2f})')
        # Tamanho: pontas do clipe que batem com uma pose de referência
        sizes = []
        for i in sorted({0, len(ms) - 1}):
            sil = silhouette(ms[i])
            name, ref, score = max(((n, r, iou(sil, rs)) for n, r, rs in refs), key=lambda t: t[2])
            if score >= POSE_MATCH and state not in ('IDLE', 'COMBAT'):
                sizes.append((i, name, ms[i]['height'] / ref['height'] - 1, score))
        for i, name, diff, score in sizes:
            if abs(diff) >= SIZE_WARN:
                add(state, 'tamanho', 'erro' if abs(diff) >= SIZE_ERROR else 'aviso',
                    f'quadro {i} ({name}, silhueta {score:.0%} igual) {diff:+.0%} de altura', [i])
        # Pulo de posição: golpe que começa ou termina em guarda tem de ter o pé de trás onde
        # o idle deixa, senão o sprite pula ao trocar de animação.
        if state in attacks and idle0['back'] is not None:
            for i, name, _, _ in sizes:
                if name == 'guarda' and ms[i]['back'] is not None:
                    jump = ms[i]['back'] - idle0['back']
                    if abs(jump) >= JUMP_WARN:
                        when = 'começa' if i == 0 else 'termina'
                        add(state, 'pulo', 'erro' if abs(jump) >= JUMP_ERROR else 'aviso',
                            f'{when} com o pé de trás {jump:+.0f} px do idle (o sprite pula ao trocar)', [i])
        # Deslize do pé de apoio: anda aos poucos, de 3 a 15 px por quadro.
        if state in attacks and state not in MOVING and state not in AIR:
            steps = planted_steps(ms)
            worst = [i for i, d in enumerate(steps) if abs(d) >= 3]
            moved, net = sum(abs(steps[i]) for i in worst), sum(steps[i] for i in worst)
            if moved >= SLIDE_WARN:
                where = f'termina {net:+.0f} px fora do lugar' if abs(net) >= JUMP_WARN else 'e volta'
                add(state, 'deslize', 'erro' if moved >= SLIDE_ERROR or abs(net) >= JUMP_ERROR else 'aviso',
                    f'pé de apoio anda {moved:.0f} px no golpe ({where})', worst)
        # Golpe ou reação agachado que levanta
        if state in CROUCHED:
            crouch_h = refs[-1][1]['height']
            tall = [i for i, m in enumerate(ms) if m['height'] > CROUCH_TALL * crouch_h]
            if tall:
                add(state, 'pose', 'erro', f'fica em pé (agachado tem {crouch_h} px de altura)', tall)
        # Efeitos e quadros estranhos: cor fora da paleta, comparada aos vizinhos (efeito
        # dura 1-3 quadros); quadro com muita cor estranha é de outro personagem ou recolorido.
        areas = np.array([m['area'] for m in ms], float)
        med_a = np.median(areas)
        foreign = np.array([(~known[m['colors']]).sum() for m in ms], float)
        borrowed = not anim['atlas'].startswith(cid + '_')
        if borrowed and anim['atlas'] not in seen_borrowed:
            seen_borrowed.add(anim['atlas'])
            add(state, 'receita', 'aviso', f"usa a arte de outro personagem ({anim['atlas']}): falta o vídeo")
        other = [] if borrowed else [i for i in range(len(ms)) if foreign[i] > FOREIGN_FRAME * areas[i]]
        if other:
            add(state, 'quebrado', 'erro', 'quadro com cores de outro personagem (ou recolorido)', other)
        fx = []
        for i in range(len(ms)):
            near = np.concatenate([foreign[max(0, i - 3):i], foreign[i + 1:i + 4]])
            base = np.median(near) if len(near) else np.median(foreign)
            if not borrowed and foreign[i] - base >= FX_PIXELS and foreign[i] >= FX_FACTOR * base and i not in other:
                fx.append(i)
        if fx:
            add(state, 'efeito', 'aviso', 'cor fora da paleta (clarão, poeira, arco, rastro)', fx)
        tiny = [i for i in range(len(ms)) if areas[i] < 0.45 * med_a]
        if tiny:
            add(state, 'quebrado', 'erro', 'quadro quase vazio (corpo cortado ou fora do quadro)', tiny)
        edge = [i for i, m in enumerate(ms) if m['edge']]
        if edge:
            add(state, 'quebrado', 'aviso', 'encosta na borda da célula (pode estar cortado)', edge)
        if state in GROUNDED:
            fl = [i for i, m in enumerate(ms) if m['floating']]
            if fl:
                add(state, 'quebrado', 'aviso', 'flutuando acima do chão', fl)
    return found


def contact_sheet(cid, state, issues, out):
    pack, clips = load_pack(cid)
    anim = pack['animations'][state]
    sheet = Sheet(clips[anim['atlas']])
    marked = {i for it in issues for i in it['frames']}
    n = len(anim['frames'])
    cw, ch, sc = 300, 360, 0.5
    img = Image.new('RGB', (int(cw * min(n, 12) * sc), int(ch * ((n + 11) // 12) * sc) + 16), (45, 45, 58))
    d = ImageDraw.Draw(img)
    d.text((4, 2), f'{cid} {state}: ' + ' | '.join(f"{it['kind']}: {it['text']}" for it in issues)[:180],
           fill=(255, 255, 255))
    for k, f in enumerate(anim['frames']):
        cell = Image.fromarray(sheet.frame(f))
        canvas = Image.new('RGBA', (cw, ch), (90, 50, 50, 255) if k in marked else (60, 60, 80, 255))
        canvas.paste(cell, (cw // 2 - sheet.rx, ch - 20 - sheet.ry), cell)
        cd = ImageDraw.Draw(canvas)
        cd.line([(0, ch - 20), (cw, ch - 20)], fill=(0, 255, 0, 255))
        cd.line([(cw // 2, 0), (cw // 2, ch)], fill=(255, 255, 0, 160))  # raiz
        cd.text((4, 4), str(k), fill=(255, 255, 255, 255))
        img.paste(canvas.convert('RGB').resize((int(cw * sc), int(ch * sc))),
                  (int(k % 12 * cw * sc), 16 + int(k // 12 * ch * sc)))
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / f'{cid}_{state}.png')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('personagens', nargs='+')
    p.add_argument('--folhas', help='pasta para as folhas de conferência dos clipes com aviso')
    p.add_argument('--json', help='grava o resultado em JSON')
    args = p.parse_args()
    report = {}
    order = {'erro': 0, 'aviso': 1, 'info': 2}
    for cid in args.personagens:
        found = sorted(audit(cid), key=lambda f: (order[f['level']], f['state']))
        report[cid] = found
        counts = {lv: sum(f['level'] == lv for f in found) for lv in order}
        print(f'\n== {cid}: {counts["erro"]} erros, {counts["aviso"]} avisos, {counts["info"]} notas')
        for f in found:
            frames = f" q{','.join(map(str, f['frames']))}" if f['frames'] else ''
            print(f"  {f['level']:5} {f['state']:16} {f['kind']:8} {f['text']}{frames}")
        if args.folhas:
            by_state = {}
            for f in found:
                if f['level'] != 'info':
                    by_state.setdefault(f['state'], []).append(f)
            for state, issues in by_state.items():
                contact_sheet(cid, state, issues, Path(args.folhas))
    if args.json:
        Path(args.json).write_text(json.dumps(report, indent=1, ensure_ascii=False), encoding='utf-8')


if __name__ == '__main__':
    main()
