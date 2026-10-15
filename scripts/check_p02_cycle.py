#!/usr/bin/env python3
"""Check trimmed PTS, dense local residuals, shoulder controls and actual viewer."""
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
from sprite_pipeline.streaming import probe_video
from sprite_pipeline.imaging import bounds
from check_collection import check_preview_pixels


def check(folder,source,evidence):
    evidence.mkdir(parents=True,exist_ok=True)
    data=json.loads((folder/'assets/manifest.json').read_text());before,after,idle=data['clips']
    original=probe_video(source)
    assert original['source_sha256']==after['source_sha256']==before['source_sha256']
    assert before['frame_count']==after['frame_count']==24
    assert after['source_selection']['start_inclusive']==14 and after['source_selection']['stop_exclusive']==38
    for clip in (before,after):
        assert [f['pts'] for f in clip['frames']]==[f['pts'] for f in original['frames'][14:38]]
        assert [f['source_frame_index'] for f in clip['frames']]==list(range(14,38))
        np.testing.assert_allclose([f['time_s'] for f in clip['frames']],
            [f['time_s']-original['frames'][14]['time_s'] for f in original['frames'][14:38]])
        np.testing.assert_allclose([f['duration_s'] for f in clip['frames']],
                                  [f['duration_s'] for f in original['frames'][14:38]])
        assert clip['loops']==[]
    report=after['scale_correction'];fitted=report['fitted']
    assert report['reference_source_frame']==16 and report['global_factor']==.85
    assert min(f['correlation'] for f in fitted)>.8
    for clip in data['clips']:
        for frame in clip['frames']:
            im=cv2.imread(str(folder/frame['normalized_png']),-1)
            assert list(im.shape[1::-1])==data['canvas']
            assert bounds(cv2.cvtColor(im,cv2.COLOR_BGRA2RGBA))==frame['bbox_output']
            m=frame['transform_source_to_output']
            assert m[0][0]==m[1][1] and m[0][1]==m[1][0]==0
    x=[]
    for fit,frame in zip(fitted,after['frames']):
        m=np.asarray(frame['transform_source_to_output'])
        x.append(float(m[0,0]*fit['patch_center_source_px'][0]+m[0,2]))
        assert abs(m[1,1]*648+m[1,2]-data['root'][1])<1e-8
    assert np.ptp(x)<1e-7
    residuals={}
    for clip in (before,after):
        length=np.array([fit['scale_to_action_reference']*frame['scale_applied']
                         for fit,frame in zip(fitted,clip['frames'])])
        residuals[clip['name']]=float(np.ptp(length)/np.median(length))
    # The median deliberately leaves isolated patch deformations uncorrected;
    # reject >2% residual or less than a threefold reduction from the old curve.
    assert residuals[after['name']]<.02
    assert residuals[after['name']]<residuals[before['name']]/3
    shoulders=[c['output_length_px'] for c in report['independent_shoulder_controls']]
    assert np.ptp(shoulders)/np.mean(shoulders)<.03
    errors=[];requests=[];pixels={}
    with sync_playwright() as pw:
        browser=pw.chromium.launch(args=['--no-sandbox'])
        for single in (False,True):
            page=browser.new_page(viewport={'width':390,'height':844},is_mobile=True,has_touch=True)
            page.on('pageerror',lambda e:errors.append(str(e)))
            page.on('requestfailed',lambda r:requests.append(r.url))
            page.on('request',lambda r:requests.append(r.url) if r.url.startswith(('https://','http://')) else None)
            path=folder/'p02-dash-comparacao.html' if single else folder/'assets/index.html'
            page.goto(path.resolve().as_uri())
            page.wait_for_function("$('normalInfo').textContent.startsWith('dash_ciclo_ajustado ·')")
            assert page.evaluate("current===1&&frame===12&&comparisonFrame===12")
            assert page.evaluate('document.body.scrollWidth<=innerWidth')
            for i,clip in enumerate(data['clips']):
                page.select_option('#clip',str(i))
                page.wait_for_function("$('normalInfo').textContent.startsWith(clips[current].name+' ·')")
                pixels[f'{single}/{clip["name"]}']=check_preview_pixels(page,folder,clip)
            page.select_option('#clip','1')
            page.wait_for_function("$('normalInfo').textContent.startsWith('dash_ciclo_ajustado ·')")
            page.evaluate('pausePlayback();frame=12;render()')
            page.wait_for_function('comparisonFrame===12')
            page.screenshot(path=str(evidence/f'cycle-{single}-phone.png'),full_page=True)
            page.tap('#play');page.wait_for_function('frame>13');page.tap('#play')
            page.evaluate('pausePlayback();frame=23;render()');page.wait_for_function('comparisonFrame===23')
            page.tap('#next');page.wait_for_function('frame===0&&comparisonFrame===0')
            page.set_viewport_size({'width':844,'height':390})
            page.screenshot(path=str(evidence/f'cycle-{single}-landscape.png'),full_page=True)
            page.select_option('#compare','2');page.wait_for_function('comparisonFrame===0')
            page.close()
        browser.close()
    assert not errors,errors
    assert not requests,requests
    result={'source_sha256':digest(source),'source_frames':97,'selected_source_frames':[14,37],
        'dash_frames':24,'exported_pngs':49,'pts_preserved':True,'playback_times_rebased':True,
        'local_patch_peak_to_peak_relative_median':residuals,
        'local_fit_minimum_correlation':min(f['correlation'] for f in fitted),
        'independent_shoulder_control_difference_fraction':float(np.ptp(shoulders)/np.mean(shoulders)),
        'horizontal_anchor_spread_px':float(np.ptp(x)),
        'preview_vs_png_mean_rgb_errors':pixels,'page_errors':errors,'external_or_failed_requests':requests,
        'full_frame_anatomical_validation':False,'perfect_gait_loop_claimed':False}
    (evidence/'p02-cycle-checks.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('folder',type=Path);p.add_argument('--source',type=Path,required=True)
    p.add_argument('--evidence',type=Path,required=True);a=p.parse_args();check(a.folder,a.source,a.evidence)
