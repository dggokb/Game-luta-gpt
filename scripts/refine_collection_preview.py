#!/usr/bin/env python3
"""Repack early mobile derivatives without changing normalized PNGs or timing."""
import argparse
import json
from pathlib import Path
import sys
import cv2

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from sprite_pipeline.exporting import write_json
from process_collection import PreviewPages
from prepare_mobile import render_mobile


def refine(folder):
    assets=folder/'assets';path=assets/'manifest.json'
    if not path.exists():return
    manifest=json.loads(path.read_text())
    if manifest['mobile_preview'].get('layout')=='cropped_tiles_v1':
        render_mobile(manifest,assets)
        return
    obsolete=set()
    for clip in manifest['clips']:
        if clip['status']=='rejected':continue
        pages=PreviewPages(assets,'images/'+clip['name']+'/after-detail','normalized',manifest['canvas'])
        for entry in clip['frames']:
            obsolete.add(entry['normalized_file'])
            rgba=cv2.cvtColor(cv2.imread(str(folder/entry['normalized_png']),-1),cv2.COLOR_BGRA2RGBA)
            pages.add(rgba,entry)
        pages.flush()
        write_json(folder/(clip['name']+'-metadata.json'),clip)
    manifest['mobile_preview'].update(maximum_image_width=720,raw_maximum_image_width=320,layout='cropped_tiles_v1')
    write_json(path,manifest)
    render_mobile(manifest,assets)
    retained={f['normalized_file'] for c in manifest['clips'] for f in c['frames'] if 'normalized_file' in f}
    for relative in obsolete-retained:
        (assets/relative).unlink()
    print(json.dumps({'refined':manifest['character'],'frames':manifest['mobile_preview']['frame_count']}),flush=True)


def main():
    p=argparse.ArgumentParser();p.add_argument('processed',type=Path);p.add_argument('--characters',nargs='*');a=p.parse_args()
    for marker in sorted(a.processed.glob('*/complete.json')):
        if not a.characters or marker.parent.name in a.characters:refine(marker.parent)


if __name__=='__main__':main()
