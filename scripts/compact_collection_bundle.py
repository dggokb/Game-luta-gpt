#!/usr/bin/env python3
"""Reduce only packaged preview textures, keeping logical canvas geometry."""
import argparse
import json
from pathlib import Path
import shutil
import sys
from concurrent.futures import ThreadPoolExecutor
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_mobile import render_mobile


def compact(source, output, factor=.6, resume_from=None):
    if not .25 <= factor <= 1:
        raise ValueError('Escala de prévia inválida')
    if resume_from is None:
        if output.exists():
            raise ValueError('Use uma pasta nova')
        shutil.copytree(source, output)
    def process(path):
        if resume_from is not None:
            try:
                completed = json.loads(path.read_text())['mobile_preview'].get('packed_texture_scale') == factor
            except (ValueError, KeyError):
                completed = False
            if completed:
                return
            cid = path.parent.name
            shutil.rmtree(path.parent)
            shutil.copytree(resume_from/cid/'assets', path.parent)
        data = json.loads(path.read_text())
        sizes = {}
        for clip in data['clips']:
            for frame in clip['frames']:
                for field in ('raw', 'normalized'):
                    if field+'_file' not in frame:
                        continue
                    relative = frame[field+'_file']
                    if relative not in sizes:
                        image_path = path.parent/relative
                        with Image.open(image_path) as image:
                            old = image.size
                            new = tuple(max(1, round(n*factor)) for n in old)
                            image.resize(new, Image.Resampling.LANCZOS).save(image_path, 'WEBP', quality=80, method=0)
                        sizes[relative] = [new[0]/old[0], new[1]/old[1]]
                    sx, sy = sizes[relative]
                    x,y,w,h = frame[field+'_rect']
                    frame[field+'_rect'] = [x*sx,y*sy,w*sx,h*sy]
        data['mobile_preview'].update(maximum_image_width=round(720*factor),
            raw_maximum_image_width=round(320*factor), packed_texture_scale=factor,
            format='WebP quality 80 atlas tiles')
        path.write_text(json.dumps(data, ensure_ascii=False))
        render_mobile(data, path.parent)
        page = path.parent/'index.html'
        page.write_text(page.read_text().replace('<h1>Sprites — Visualizador</h1>',
            f'<p><a href="../../index.html">← Personagens</a></p><h1>Sprites — {data["character"]}</h1>'))
        print(data['character'], flush=True)
    paths = list(output.glob('characters/*/manifest.json'))
    with ThreadPoolExecutor(max_workers=2) as pool:
        list(pool.map(process, paths))
    path = output/'manifest.json'
    data = json.loads(path.read_text())
    data['mobile_preview'].update(maximum_image_width=round(720*factor),
        raw_maximum_image_width=round(320*factor), packed_texture_scale=factor,
        format='WebP quality 80 atlas tiles')
    path.write_text(json.dumps(data, ensure_ascii=False))


if __name__ == '__main__':
    p=argparse.ArgumentParser();p.add_argument('source', type=Path);p.add_argument('--output', type=Path, required=True)
    p.add_argument('--factor', type=float, default=.6)
    p.add_argument('--resume-from-processed',type=Path)
    a=p.parse_args();compact(a.source, a.output, a.factor, a.resume_from_processed)
