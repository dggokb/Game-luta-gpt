#!/usr/bin/env python3
"""Audit source coverage, original hashes, timestamps, rasters and phone viewers."""
import argparse
import base64
import io
import json
from pathlib import Path
import sys
import cv2
import numpy as np
from PIL import Image
from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from sprite_pipeline.extraction import digest
from sprite_pipeline.imaging import bounds
from sprite_pipeline.root_motion import torso_anchor_x
from sprite_pipeline.exporting import write_json


def check_preview_pixels(page, folder, clip):
    """Compare the displayed atlas crop with an independent full PNG draw."""
    page.evaluate("$('guides').checked=false;$('ghost').checked=false;frame=0;render()")
    stage=page.locator('#after').locator('..')
    actual=stage.screenshot()
    original=folder/clip['frames'][0]['normalized_png']
    url='data:image/png;base64,'+base64.b64encode(original.read_bytes()).decode()
    page.evaluate("""async url=>{
      const image=new Image();image.src=url;await image.decode();
      const canvas=$('after'),ctx=canvas.getContext('2d'),k=commonZoom();
      ctx.setTransform(1,0,0,1,0,0);ctx.clearRect(0,0,canvas.width,canvas.height);
      ctx.setTransform(k,0,0,k,0,0);ctx.drawImage(image,0,0,...data.canvas);
    }""",url)
    expected=stage.screenshot()
    a=np.asarray(Image.open(io.BytesIO(actual)).convert('RGB'),np.float32)
    b=np.asarray(Image.open(io.BytesIO(expected)).convert('RGB'),np.float32)
    error=float(np.mean(np.abs(a-b)))
    assert error<3,(folder.name,clip['name'],'atlas differs from independent full PNG',error)
    page.evaluate("$('guides').checked=true;render()")
    return error


def audit(inventory, sources, processed, evidence):
    evidence.mkdir(parents=True,exist_ok=True)
    expected=json.loads(inventory.read_text())
    report={'characters':[], 'page_errors':[], 'external_requests':[], 'checks':[]}
    for character in expected:
        cid=character['id'];folder=processed/cid
        summary=json.loads((folder/'complete.json').read_text())
        videos={f['id']:f for f in character['files'] if f['mime_type'].startswith('video/')}
        if not videos:
            assert summary['videos']==0
            report['characters'].append(summary)
            continue
        manifest=json.loads((folder/'assets/manifest.json').read_text())
        assert set(videos)=={c['source_drive_id'] for c in manifest['clips']}, cid
        assert len(videos)==len(manifest['clips']),cid
        dimensions={}
        normalized_count=0
        anchor_errors=[]
        for clip in manifest['clips']:
            assert clip['source_sha256']==digest(sources/cid/clip['source_name'])
            frames=clip['frames'];assert len(frames)==clip['frame_count']
            assert all(b['pts']>a['pts'] for a,b in zip(frames,frames[1:]))
            assert all(f['duration_s']>0 for f in frames)
            rejected=clip['status']=='rejected'
            assert rejected == (not any('normalized_file' in f for f in frames))
            for f in frames:
                for field in ('raw','normalized'):
                    if field+'_file' not in f:
                        continue
                    filename=f[field+'_file'];path=folder/'assets'/filename
                    if filename not in dimensions:
                        with Image.open(path) as im:
                            im.verify()
                        with Image.open(path) as im:
                            dimensions[filename]=im.size
                    x,y,w,h=f[field+'_rect'];pw,ph=dimensions[filename]
                    assert min(x,y)>=0 and min(w,h)>0 and x+w<=pw and y+h<=ph
                if rejected:
                    continue
                png=folder/f['normalized_png']
                with Image.open(png) as im:
                    assert list(im.size)==manifest['canvas']
                normalized_count+=1
                if 'normalized_destination_rect' in f:
                    dx,dy,dw,dh=f['normalized_destination_rect']
                    assert dx>=0 and dy>=0 and dx+dw<=manifest['canvas'][0] and dy+dh<=manifest['canvas'][1]
                    b=f['bbox_output']
                    assert dx<=b[0] and dy<=b[1] and dx+dw>=b[2] and dy+dh>=b[3]
                matrix=np.asarray(f['transform_source_to_output'])
                assert matrix[0,0]==matrix[1,1] and matrix[0,1]==matrix[1,0]==0
            if not rejected:
                for index in set((0,clip['reference_frame'],len(frames)//2,len(frames)-1)):
                    f=frames[index]
                    rgba=cv2.cvtColor(cv2.imread(str(folder/f['normalized_png']),-1),cv2.COLOR_BGRA2RGBA)
                    assert bounds(rgba)==f['bbox_output'],(cid,clip['name'],index)
                    assert np.any(rgba[:,:,3]>128)
                if clip.get('root_motion'):
                    motion=clip['root_motion'];xs=[];actual=[]
                    for f,x in zip(frames,motion['anchor_x_source_px']):
                        m=f['transform_source_to_output'];xs.append(x*m[0][0]+m[0][2])
                    assert max(xs)-min(xs)<1e-6,(cid,clip['name'])
                    # Raster check is separate from the recorded transform.
                    for f in (frames[0],frames[-1]):
                        rgba=cv2.cvtColor(cv2.imread(str(folder/f['normalized_png']),-1),cv2.COLOR_BGRA2RGBA)
                        actual.append(torso_anchor_x(rgba))
                    anchor_errors.append({'clip':clip['name'],'raster_first_last_torso_dx_px':actual[1]-actual[0]})
        assert normalized_count==sum(c['frame_count'] for c in manifest['clips'] if c['status']!='rejected')
        summary['normalized_pngs']=normalized_count
        summary['raster_anchor_checks']=anchor_errors
        report['characters'].append(summary)
    report['checks'].extend(['every_inventoried_video_accounted_for','original_video_hashes_unchanged',
        'all_original_frame_counts_and_increasing_timestamps','all_preview_tiles_present_and_in_bounds',
        'all_normalized_png_headers_match_common_canvas','sampled_png_pixels_match_recorded_bounds',
        'locomotion_torso_x_fixed_in_every_frame'])
    with sync_playwright() as pw:
        browser=pw.chromium.launch(headless=True,args=['--no-sandbox'])
        page=browser.new_page(viewport={'width':390,'height':844},is_mobile=True,has_touch=True)
        page.on('pageerror',lambda error:report['page_errors'].append(str(error)))
        page.on('request',lambda request:report['external_requests'].append(request.url)
                if request.url.startswith(('http://','https://')) else None)
        for summary in report['characters']:
            if not summary['videos']:
                continue
            cid=summary['character'];assets=processed/cid/'assets'
            manifest=json.loads((assets/'manifest.json').read_text())
            page.goto((assets/'index.html').as_uri())
            page.wait_for_function("document.getElementById('rawInfo').textContent.length>0")
            assert page.evaluate('document.body.scrollWidth<=innerWidth')
            assert page.locator('#play').is_visible()
            usable=[(i,c) for i,c in enumerate(manifest['clips']) if c['status']!='rejected']
            if usable:
                for i,c in usable:
                    page.select_option('#clip',str(i))
                    page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith(clips[current].name+' ·')")
                    page.evaluate('frame=clips[current].frame_count-1;render()')
                    page.wait_for_function("document.getElementById('position').textContent.startsWith(clips[current].frame_count+'/')")
                walk=next(((i,c) for i,c in usable if c.get('role')=='walk_back'),usable[0])
                page.select_option('#clip',str(walk[0]))
                page.wait_for_function("document.getElementById('normalInfo').textContent.startsWith(clips[current].name+' ·')")
                summary['preview_vs_full_png_mean_rgb_error']=check_preview_pixels(page,processed/cid,walk[1])
                page.screenshot(path=str(evidence/f'{cid}-phone.png'),full_page=True)
                page.tap('#play');page.wait_for_function('frame>1');page.tap('#play')
                page.select_option('#compare',str(usable[0][0]))
                page.wait_for_function("document.body.dataset.view==='compare'")
                page.select_option('#compareMode','fixed')
                page.locator('#compareSeek').evaluate("e=>{e.value=Math.min(10,Number(e.max));e.dispatchEvent(new Event('input',{bubbles:true}))}")
                page.wait_for_function("document.getElementById('comparePosition').textContent.includes('Frame fixo')")
                assert page.evaluate("document.getElementById('after').width===document.getElementById('comparison').width")
            rejected=next(((i,c) for i,c in enumerate(manifest['clips']) if c['status']=='rejected'),None)
            if rejected:
                page.select_option('#clip',str(rejected[0]))
                page.wait_for_function("document.getElementById('normalInfo').textContent===clips[current].rejection_reason")
                assert page.evaluate("document.body.dataset.view==='before'")
                assert page.locator('#clipNote').is_visible()
                assert page.locator('#before').is_visible()
        browser.close()
    assert not report['page_errors'],report['page_errors']
    assert not report['external_requests'],report['external_requests']
    report['checks'].extend(['every_converted_clip_opens_on_phone','every_character_play_pause_and_comparison',
        'rejected_sources_remain_accessible','portrait_has_no_horizontal_overflow','no_javascript_errors_or_network_requests'])
    report['status']='passed'
    report['totals']={key:sum(c.get(key,0) for c in report['characters']) for key in ('videos','frames','converted','normalized_pngs')}
    write_json(evidence/'collection-checks.json',report)
    return report['totals']


def main():
    p=argparse.ArgumentParser();p.add_argument('--inventory',type=Path,required=True)
    p.add_argument('--inputs',type=Path,required=True);p.add_argument('--processed',type=Path,required=True)
    p.add_argument('--evidence',type=Path,required=True);a=p.parse_args()
    print(json.dumps(audit(a.inventory,a.inputs,a.processed,a.evidence),indent=2))


if __name__=='__main__':main()
