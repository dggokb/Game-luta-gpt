#!/usr/bin/env python3
"""Review candidate P12 segments from the existing Drive videos only.

This produces source previews, never runtime atlases or art approval. Every
displayed frame must match the recorded extraction hash. Combat durations and
impact indices are read from the immutable contract; source PTS remain separate.
One fixed video scale/camera area is used throughout each preview so a source
zoom is visible. No interpolation, limb warp or new pose is permitted here.
"""
import argparse
import html
import json
from pathlib import Path

from PIL import Image, ImageDraw

from p12_video_sources import ROOT, STATE_FILES, sha, write_json
from p12_video_selection import bind_sources

PLAN = ROOT / 'tools/sprites/videos/p12-segments-draft.json'
CONTRACT = ROOT / 'docs/art/p12-motion-contract.json'
CW, CH = 320, 256


def validate_selection(state, spec, animation, record, folder):
    """Reject a draft with stale pixels, wrong impact placement or invented PTS."""
    selected = spec['selectedVideoFrames']
    count = len(animation['frames'])
    if len(selected) != count or any(type(i) is not int for i in selected):
        raise ValueError(state + ': selection count differs from combat contract')
    if selected != sorted(selected) or len(set(selected)) != len(selected):
        raise ValueError(state + ': select real sequential frames, without duplicates')
    if 'durationsMs' in animation and len(animation['durationsMs']) != count:
        raise ValueError(state + ': invalid immutable duration count')
    if 'durationsMs' not in animation and animation.get('distancePerFrame', 0) <= 0:
        raise ValueError(state + ': missing immutable timing/distance contract')
    peak = animation.get('impactFrame', spec['mainIndex'])
    if spec['mainIndex'] != peak or not 0 <= peak < count:
        raise ValueError(state + ': main pose does not respect impact index')
    for i in selected:
        if not 0 <= i < len(record['frames']):
            raise ValueError(state + ': selected source frame does not exist')
        frame = record['frames'][i]
        if frame['index'] != i or sha(folder / frame['file']) != frame['sha256']:
            raise ValueError(state + ': selected pixels differ from original extraction')
    times = [record['frames'][i]['time'] for i in selected]
    if any(b <= a for a, b in zip(times, times[1:])):
        raise ValueError(state + ': source timestamps are not strictly increasing')
    return peak


def resolve_selection(state, spec, animation, record, folder):
    """Return exact original sources; neutral bridges are never new drawings."""
    primary = STATE_FILES[state]
    entries = spec.get('frameSources')
    if entries is None:
        peak = validate_selection(state, spec, animation, record, folder)
        return [(primary, i, record, folder) for i in spec['selectedVideoFrames']], peak
    count = len(animation['frames'])
    if len(entries) != count:
        raise ValueError(state + ': source bindings differ from combat frame count')
    indices = [e.get('videoFrame') for e in entries]
    names = [e.get('videoName') for e in entries]
    if spec['selectedVideoFrames'] != indices:
        raise ValueError(state + ': frame source indices and explicit selection differ')
    if 'durationsMs' not in animation and len(set(names)) > 1:
        raise ValueError(state + ': distance-driven preview needs one continuous original video clock')
    peak = animation.get('impactFrame', spec['mainIndex'])
    if spec['mainIndex'] != peak:
        raise ValueError(state + ': main pose does not respect impact index')
    if ('durationsMs' in animation and len(animation['durationsMs']) != count
            or 'durationsMs' not in animation and animation.get('distancePerFrame', 0) <= 0):
        raise ValueError(state + ': invalid immutable animation timing')
    records, folders = {primary: record}, {primary: folder}
    for name in set(names) - {primary}:
        if name not in set(STATE_FILES.values()):
            raise ValueError(state + ': transition source is not an existing P12 Drive video')
        source_folder = folder.parent / Path(name).stem
        source_record = json.loads((source_folder / 'frames.json').read_text())
        durable = json.loads((ROOT / 'docs/art/p12-video-sources' / (Path(name).stem + '.frames.json')).read_text())
        if source_record['videoSha256'] != durable['videoSha256'] or source_record['frames'] != durable['frames']:
            raise ValueError(state + ': transition extraction differs from original provenance')
        records[name], folders[name] = source_record, source_folder
    bindings = bind_sources(state, names, indices, peak, primary, count, records)
    result = []
    for name, i in bindings:
        entry = records[name]['frames'][i]
        if sha(folders[name] / entry['file']) != entry['sha256']:
            raise ValueError(state + ': transition pixels differ from original extraction')
        result.append((name, i, records[name], folders[name]))
    return result, peak


def source_cells(state, spec, animation, record, folder):
    sources, peak = resolve_selection(state, spec, animation, record, folder)
    # Use the union of ALL original frames, not per-pose fitting. Preserve zoom
    # and source movement in the review instead of hiding them in a contact sheet.
    unions = [r['sourceBoundsUnion'] for _, _, r, _ in sources]
    bounds = [min(b[0] for b in unions), min(b[1] for b in unions),
              max(b[2] for b in unions), max(b[3] for b in unions)]
    scale = min((CW - 28) / (bounds[2] - bounds[0]),
                (CH - 62) / (bounds[3] - bounds[1]))
    cells, provenance = [], []
    for ordinal, (name, i, source_record, source_folder) in enumerate(sources):
        frame = source_record['frames'][i]
        picture = Image.open(source_folder / frame['file']).convert('RGBA')
        picture = picture.convert('RGBa').resize(
            (max(1, round(picture.width * scale)), max(1, round(picture.height * scale))),
            Image.Resampling.LANCZOS).convert('RGBA')
        canvas = Image.new('RGBA', (CW, CH), '#20242d')
        x = 14 + round((frame['sourceBox'][0] - bounds[0]) * scale)
        y = 34 + round((frame['sourceBox'][1] - bounds[1]) * scale)
        canvas.alpha_composite(picture, (x, y))
        draw = ImageDraw.Draw(canvas)
        label = (' / IMPACT' if 'impactFrame' in animation else ' / MAIN') if ordinal == peak else ''
        draw.text((8, 5), state + label, fill='white')
        draw.text((8, 20), f"{name} / frame {i} / {frame['time']:.3f}s", fill='#b8c7d8')
        draw.text((8, CH - 20), 'SOURCE CANDIDATE / NOT APPROVED', fill='#ffc078')
        cells.append(canvas.convert('RGB'))
        provenance.append({'videoFrame': i, 'sourceTimeSeconds': frame['time'],
                           'sourcePixelSha256': frame['sha256'], 'sourceBox': frame['sourceBox'],
                           'videoName': name, 'driveId': source_record['driveId'],
                           'videoSha256': source_record['videoSha256']})
    return cells, provenance, peak


def build_review(extraction, output):
    plan = json.loads(PLAN.read_text())
    contract = json.loads(CONTRACT.read_text())
    if plan['sourcePolicy'] != 'EXISTING_DRIVE_VIDEOS_ONLY' or plan['status'] != 'DRAFT_NOT_APPROVED':
        raise ValueError('review input must remain an existing-video candidate draft')
    if set(plan['states']) != set(contract['animations']):
        raise ValueError('all 42 states must have explicit candidate selections')
    output.mkdir(parents=True, exist_ok=True)
    report = {'status': 'DRAFT_NOT_APPROVED', 'sourcePolicy': plan['sourcePolicy'],
              'contractSha256': sha(CONTRACT), 'planSha256': sha(PLAN), 'states': {},
              'note': 'Source segment previews only. Not normalized runtime animations; no SR, pack binding or approval.'}
    cards, sections = [], []
    for state, animation in contract['animations'].items():
        spec = plan['states'][state]
        name = STATE_FILES[state]
        folder = extraction / Path(name).stem
        record = json.loads((folder / 'frames.json').read_text())
        durable = json.loads((ROOT / 'docs/art/p12-video-sources' / (Path(name).stem + '.frames.json')).read_text())
        if record['videoSha256'] != durable['videoSha256'] or record['frames'] != durable['frames']:
            raise ValueError(state + ': extraction differs from recorded original video')
        if record['name'] != name:
            raise ValueError(state + ': wrong source video')
        cells, frames, peak = source_cells(state, spec, animation, record, folder)
        tag = state.lower()
        preview = output / (tag + '.gif')
        times = [f['sourceTimeSeconds'] for f in frames]
        durations = animation.get('durationsMs')
        if durations is None:
            # Walking is distance-driven in the engine. A source-time preview
            # does not replace that gameplay contract with invented fixed ms.
            deltas = [(b - a) * 1000 for a, b in zip(times, times[1:])]
            durations = deltas + [deltas[-1]]
        cells[0].save(preview, save_all=True, append_images=cells[1:],
                      duration=[max(10, round(t / 10) * 10) for t in durations],
                      loop=0, disposal=2)
        card = Image.new('RGB', (CW * 3, CH + 50), '#20242d')
        for x, index in enumerate([0, peak, len(cells) - 1]):
            card.paste(cells[index], (x * CW, 0))
        draw = ImageDraw.Draw(card)
        phases = spec.get('phaseLabels', ['preparation', 'main', 'recovery/end'])
        for x, label in enumerate(phases):
            draw.text((x * CW + 8, CH + 3), label, fill='white')
        draw.text((8, CH + 20), spec['observedMotion'][:145], fill='#b8c7d8')
        card.save(output / (tag + '.jpg'), quality=89)
        cards.append(card)
        report['states'][state] = {'status': 'DRAFT_NOT_APPROVED',
            'source': {'file': name, 'driveId': record['driveId'], 'driveUrl': record['driveUrl'],
                       'videoSha256': record['videoSha256']},
            'frames': frames, 'runtimeDurationsMs': animation.get('durationsMs'),
            'runtimeDistancePerFrame': animation.get('distancePerFrame'),
            'previewTiming': 'runtime-ms-rounded-to-10ms' if 'durationsMs' in animation else 'source-PTS-distance-driven-runtime',
            'runtimeImpactIndex': animation.get('impactFrame'), 'mainIndex': peak,
            'observedMotion': spec['observedMotion'], 'findings': spec['findings'],
            'pending': ['uniform anatomy scale', 'support/root registration', 'alpha/effect masks',
                        'state transitions', 'runtime visual audit'],
            'previewSha256': sha(preview)}
        sections.append(f'<section><h2>{html.escape(state)}</h2>'
            f'<img src="{tag}.gif" width="320" height="256" alt="Original video candidate for {state}">'
            f'<p>{html.escape(spec["observedMotion"])}</p>'
            '<ul>' + ''.join('<li>' + html.escape(f) + '</li>' for f in spec['findings']) + '</ul>'
            f'<p><a href="{html.escape(record["driveUrl"])}">Original Drive video</a> · '
            f'<a href="{tag}.jpg">Three source phases</a></p></section>')
    for start in range(0, len(cards), 7):
        page = Image.new('RGB', (CW * 3, (CH + 50) * min(7, len(cards) - start)), '#20242d')
        for i, card in enumerate(cards[start:start + 7]):
            page.paste(card, (0, i * (CH + 50)))
        page.save(output / f'page-{start // 7 + 1:02d}.jpg', quality=90)
    write_json(output / 'review.json', report)
    (output / 'index.html').write_text('<!doctype html><html lang="pt-BR"><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1"><title>P12 original video review</title>'
        '<style>body{margin:24px;background:#151821;color:#eee;font:16px system-ui}a{color:#a8d4ff}'
        'main{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:20px}'
        'section{background:#20242d;padding:16px;border-radius:8px}h2{font-size:18px}'
        'img{max-width:100%;height:auto}</style><h1>P12: 42 trechos candidatos dos vídeos originais</h1>'
        '<p>Não aprovado. Frames reais, sem novas poses. Escala, transparência, efeitos e transições pendentes. '
        'GIF arredonda a 10 ms. Golpes usam os tempos do contrato; caminhada usa PTS do vídeo, '
        'pois o jogo avança seus frames por distância. PTS originais estão em review.json.</p>'
        '<main>' + ''.join(sections) + '</main></html>', encoding='utf-8')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--extraction', type=Path, default=ROOT / 'android/app/build/p12-video-extraction')
    parser.add_argument('--output', type=Path, default=ROOT / 'docs/art/p12-video-state-review')
    args = parser.parse_args()
    report = build_review(args.extraction, args.output)
    print(f"{len(report['states'])} original-video candidates previewed; NOT APPROVED")
