"""Monta os assets de um APK de teste comparando o pipeline atual com a escala automática.

Lê as saídas de `autoscale/standardize.py` (report.json e _frames/) de um ou mais personagens,
renderiza os frames antes/depois como WebP e gera um visualizador offline simples.

Uso: python autoscale/make_mobile_test.py --output /pasta/assets p02=/saida/p02 p10=/saida/p10
"""
import sys, json, argparse, pathlib
import numpy as np, cv2
from PIL import Image
ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from sprite_pipeline.imaging import image_rgba
from autoscale.standardize import render

CANVAS, ROOTPT = (1100, 800), (550, 740)
CROP = (ROOTPT[0]-240, ROOTPT[1]-560, ROOTPT[0]+240, ROOTPT[1]+40)   # x0, y0, x1, y1


def frames_for(outdir, clip_name, rep, background, target_ppm):
    files = sorted((outdir/'_frames'/clip_name).glob('*.png'))
    rgbas = [image_rgba(cv2.imread(str(f)), background) for f in files]
    per = rep['per_frame']
    base = {'n': len(files), 'rgba': rgbas, 'hipx': np.array([p['hip_x'] for p in per]), 'ground': np.array([p['ground_y'] for p in per])}
    after = render({**base, 'scale': np.array([p['scale'] for p in per])}, ROOTPT, CANVAS)
    before = render({**base, 'scale': np.full(len(files), rep['current_pipeline_constant_scale'])}, ROOTPT, CANVAS)
    return before, after


def save_webp(frame, path):
    x0, y0, x1, y1 = CROP
    Image.fromarray(frame[y0:y1, x0:x1]).save(path, format='WEBP', quality=85, method=4)


def main():
    ap = argparse.ArgumentParser(); ap.add_argument('--output', required=True); ap.add_argument('sources', nargs='+', help='nome=/pasta/da/saida')
    a = ap.parse_args(); out = pathlib.Path(a.output)
    if out.exists() and any(out.iterdir()): raise SystemExit('Use uma pasta de assets nova ou vazia')
    data, manifest_clips, total = {}, [], 0
    for spec in a.sources:
        name, folder = spec.split('=', 1); folder = pathlib.Path(folder)
        report = json.loads((folder/'report.json').read_text())
        background = report.get('background', {'mode': 'green'})
        data[name] = {'master': report['master'], 'clips': {}}
        for clip_name, rep in report['clips'].items():
            before, after = frames_for(folder, clip_name, rep, background, report['master']['px_per_metre'])
            entries = []
            for k, (b, f) in enumerate(zip(before, after)):
                rel = pathlib.Path('images')/name/clip_name
                (out/rel/'before').mkdir(parents=True, exist_ok=True); (out/rel/'after').mkdir(parents=True, exist_ok=True)
                save_webp(b, out/rel/'before'/f'{k:03d}.webp'); save_webp(f, out/rel/'after'/f'{k:03d}.webp')
                p = rep['per_frame'][k]
                entries.append({'index': k, 'raw_file': (rel/'before'/f'{k:03d}.webp').as_posix(), 'normalized_file': (rel/'after'/f'{k:03d}.webp').as_posix(),
                                'size_before': p['size_before'], 'size_after': round(p['size_before']*p['scale'], 4), 'scale': p['scale']})
            total += len(entries)
            data[name]['clips'][clip_name] = {'frames': entries, 'cycle': rep['cycle'], 'cuts': rep['cuts_after_frame'],
                                               'before_minmax': rep['size_with_current_pipeline_minmax'], 'after_p5_p95': rep['size_after_rel_to_master_p5_p95']}
            manifest_clips.append({'name': f'{name}/{clip_name}', 'frame_count': len(entries), 'frames': entries})
    manifest = {'clips': manifest_clips, 'characters': data, 'crop': CROP, 'fps': 60,
                'mobile_preview': {'frame_count': total, 'maximum_image_width': CROP[2]-CROP[0], 'format': 'WebP quality 85',
                                   'all_frames_retained': True, 'timestamps_changed': False, 'full_resolution_pngs_included': False,
                                   'note': 'Teste visual: pipeline atual (escala fixa pela silhueta) contra escala automática por frame.'}}
    (out/'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False))
    html = (ROOT/'autoscale/mobile_test.html').read_text()
    (out/'index.html').write_text(html.replace('__MANIFEST__', json.dumps(manifest, ensure_ascii=False).replace('<', '\\u003c')), encoding='utf-8')
    print(json.dumps({'frames': total, 'images': len(list(out.rglob('*.webp'))), 'bytes': sum(f.stat().st_size for f in out.rglob('*') if f.is_file())}))


if __name__ == '__main__':
    main()
