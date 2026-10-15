#!/usr/bin/env python3
"""Check the actual exported P02 preview, PNGs, source hash and timestamps."""
import argparse
import json
from pathlib import Path
import sys
import cv2
import numpy as np
from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from sprite_pipeline.extraction import digest
from sprite_pipeline.imaging import bounds
from check_collection import check_preview_pixels


def check(folder,source,evidence):
    evidence.mkdir(parents=True,exist_ok=True)
    data=json.loads((folder/'assets/manifest.json').read_text());before,after=data['clips']
    assert digest(source)==before['source_sha256']==after['source_sha256']
    assert len(before['frames'])==len(after['frames'])==97
    assert [f['pts'] for f in before['frames']]==[f['pts'] for f in after['frames']]
    assert before['scale_audit']['status']=='drift_detected'
    assert after['scale_audit']['status']=='within_annotation_tolerance'
    assert after['scale_audit']['holdout_count']==8
    for index in (0,96):
        assert np.array_equal(cv2.imread(str(folder/before['frames'][index]['normalized_png']),-1),
                              cv2.imread(str(folder/after['frames'][index]['normalized_png']),-1))
    for clip in data['clips']:
        for f in clip['frames']:
            im=cv2.imread(str(folder/f['normalized_png']),-1)
            assert list(im.shape[1::-1])==data['canvas']
            assert bounds(cv2.cvtColor(im,cv2.COLOR_BGRA2RGBA))==f['bbox_output']
            m=f['transform_source_to_output'];assert m[0][0]==m[1][1] and m[0][1]==m[1][0]==0
    errors=[];requests=[];pixel_checks={}
    with sync_playwright() as pw:
        browser=pw.chromium.launch(args=['--no-sandbox'])
        for single in (False,True):
            page=browser.new_page(viewport={'width':390,'height':844},is_mobile=True,has_touch=True)
            page.on('pageerror',lambda error:errors.append(str(error)))
            page.on('requestfailed',lambda request:requests.append(request.url))
            page.on('request',lambda request:requests.append(request.url) if request.url.startswith(('https://','http://')) else None)
            path=folder/'p02-dash-comparacao.html' if single else folder/'assets/index.html'
            page.goto(path.resolve().as_uri())
            page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith('dash_corrigido ·')")
            assert page.evaluate("document.body.dataset.view==='compare' && frame===26 && comparisonFrame===26")
            assert 'experimental' in page.locator('#clipNote').inner_text()
            assert page.evaluate('document.body.scrollWidth<=innerWidth')
            for i,c in enumerate(data['clips']):
                page.select_option('#clip',str(i))
                page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith(clips[current].name+' ·')")
                pixel_checks[f'{"single" if single else "folder"}/{c["name"]}']=check_preview_pixels(page,folder,c)
            page.select_option('#clip','1');page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith('dash_corrigido ·')")
            page.evaluate('frame=26;render()')
            page.wait_for_function('comparisonFrame===26')
            page.screenshot(path=str(evidence/f'p02-dash-{ "single" if single else "folder"}-phone.png'),full_page=True)
            page.tap('#play');page.wait_for_function('frame>27');page.tap('#play')
            page.evaluate('pausePlayback();frame=96;render()');page.wait_for_function('comparisonFrame===96')
            page.set_viewport_size({'width':844,'height':390})
            page.screenshot(path=str(evidence/f'p02-dash-{ "single" if single else "folder"}-landscape.png'),full_page=True)
            page.close()
        browser.close()
    assert not errors,errors
    assert not requests,requests
    samples=after['scale_audit']['samples'];controls=[s for s in samples if s['purpose']=='holdout']
    metrics={'source_sha256':digest(source),'original_frames':97,'exported_pngs':194,'pts_equal':True,
        'before_maximum_head_growth_fraction':max(s['segments']['head_axis']['output_ratio_to_reference']-1 for s in before['scale_audit']['samples']),
        'after_maximum_absolute_heldout_head_error_fraction':max(abs(s['segments']['head_axis']['output_ratio_to_reference']-1) for s in controls),
        'checkpoint_count':len(samples),'head_holdouts':len(controls),'shoulder_controls':sum('shoulder_span' in s['segments'] for s in samples),
        'first_and_last_png_pixels_unchanged':True,
        'preview_vs_png_mean_rgb_errors':pixel_checks,'page_errors':errors,'external_or_failed_requests':requests,
        'full_frame_anatomical_validation':False}
    (evidence/'p02-dash-checks.json').write_text(json.dumps(metrics,indent=2)+'\n');print(json.dumps(metrics,indent=2))


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('folder',type=Path);p.add_argument('--source',type=Path,required=True)
    p.add_argument('--evidence',type=Path,required=True);a=p.parse_args();check(a.folder,a.source,a.evidence)
