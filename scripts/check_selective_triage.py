#!/usr/bin/env python3
"""Verify selective delivery pixels, source timing, preservation and offline UI."""
import argparse,base64,io,json,sys
from pathlib import Path
import cv2
import numpy as np
from PIL import Image
from playwright.sync_api import sync_playwright
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from sprite_pipeline.extraction import digest
from sprite_pipeline.imaging import bounds,image_rgba,affine_rgba_region
from sprite_pipeline.streaming import decode_frames


def check(folder,baseline,sources,evidence):
    evidence.mkdir(parents=True,exist_ok=True)
    d=json.loads((folder/'assets/manifest.json').read_text());clips={c['name']:c for c in d['clips']}
    assert len(d['cases'])==13 and not d['full_frame_anatomical_validation']
    assert {s:sum(c['decision']==s for c in d['cases']) for s in ('corrected','preserved','pending')}=={'corrected':8,'preserved':2,'pending':3}
    png_count=0;source_pixels=0
    for case in d['cases']:
        old=json.loads((baseline/case['character']/'assets/manifest.json').read_text())
        original=next(c for c in old['clips'] if c['name']==case['movement'])
        source=sources/case['character']/original['source_name'];assert digest(source)==case['source_sha256']
        s=case['selection'];start,stop=s['start_inclusive'],s['stop_exclusive'];before=clips[case['clips']['before']];after=clips[case['clips']['after']]
        for clip in (before,after):
            assert clip['frame_count']==stop-start
            for i,f in enumerate(clip['frames']):
                o=original['frames'][start+i]
                assert f['source_frame_index']==start+i and f['pts']==o['pts'] and f['duration_s']==o['duration_s']
                assert abs(f['time_s']-(o['time_s']-original['frames'][start]['time_s']))<1e-10
            for key in ('anchor_x_source_px','removed_x_output_px'):
                values=clip.get('root_motion',{}).get(key)
                if values is not None:assert len(values)==clip['frame_count']
        if case['decision']=='pending':
            assert before is after and not (folder/'normalized'/case['id']).exists()
            assert original['status']!='converted';continue
        for bf,af in zip(before['frames'],after['frames']):
            o=original['frames'][af['source_frame_index']]
            old_png=baseline/case['character']/bf['baseline_png'];assert digest(old_png)==bf['baseline_png_sha256']==af['baseline_png_sha256']
            a=np.asarray(af['transform_source_to_output']);b=np.asarray(o['transform_source_to_output'])
            np.testing.assert_array_equal(a[:,:2],b[:,:2]);assert a[1,2]==b[1,2]
            assert af['scale_applied']==o['scale_applied']
            if not case['root_changed']:assert a[0,2]==b[0,2]+case['padding'][0]
            if case['decision']=='preserved':
                assert af==bf and 'normalized_png' not in af;continue
            png_count+=1;rgba=cv2.cvtColor(cv2.imread(str(folder/af['normalized_png']),-1),cv2.COLOR_BGRA2RGBA)
            assert list(rgba.shape[1::-1])==case['canvas'] and bounds(rgba)==af['bbox_output']
            box=af['bbox_output'];assert af['offset_from_root']==[box[0]-case['root'][0],box[1]-case['root'][1]]
        if case['root_changed']:
            anchors=after['root_motion']['anchor_x_source_px'];xs=[x*f['scale_applied']+f['transform_source_to_output'][0][2] for x,f in zip(anchors,after['frames'])]
            assert np.ptp(xs)<1e-7 and case['before_anchor_spread_px']>32
        if case['decision']=='corrected':
            wanted={start,(start+stop-1)//2,stop-1}
            for i,raw in enumerate(decode_frames(source,original)):
                if i in wanted:
                    f=after['frames'][i-start];a=np.asarray(f['transform_source_to_output'])
                    original_rgba=image_rgba(raw,original['background_treatment']);rgba=image_rgba(raw,after['background_treatment'])
                    np.testing.assert_array_equal(original_rgba[:,:,:3],rgba[:,:,:3]);assert np.all(rgba[:,:,3]<=original_rgba[:,:,3])
                    expected=affine_rgba_region(rgba,a[0,0],a[:,2].tolist(),case['canvas'])
                    actual=cv2.cvtColor(cv2.imread(str(folder/f['normalized_png']),-1),cv2.COLOR_BGRA2RGBA)
                    np.testing.assert_array_equal(actual,expected);source_pixels+=1
                if i>=stop-1:break
    assert png_count==531 and source_pixels==24
    for cid,sha in d['structural_triage']['baseline_manifest_sha256'].items():assert digest(baseline/cid/'assets/manifest.json')==sha
    for path,sha in d['approved_assets_sha256'].items():assert digest(folder/'assets/approved'/path)==sha
    errors=[];external=[];pixel_errors={}
    with sync_playwright() as pw:
        browser=pw.chromium.launch(args=['--no-sandbox'])
        page=browser.new_page(viewport={'width':390,'height':844},is_mobile=True,has_touch=True)
        page.on('pageerror',lambda e:errors.append(str(e)))
        page.on('requestfailed',lambda r:errors.append(r.url))
        page.on('request',lambda r:external.append(r.url) if r.url.startswith(('http://','https://')) else None)
        page.goto((folder/'assets/index.html').resolve().as_uri())
        for i,case in enumerate(d['cases']):
            page.select_option('#case',str(i));page.wait_for_function("$('renderState').textContent===selected().id+'/0'")
            assert page.evaluate('document.body.scrollWidth<=innerWidth')
            boxes=[page.locator('#'+side).bounding_box() for side in ('after','before')]
            assert abs(boxes[0]['y']-boxes[1]['y'])<1 and abs(boxes[0]['height']-boxes[1]['height'])<1
            if case['render_kind']=='normalized':
                for side in ('after','before'):
                    f=clips[case['clips'][side]]['frames'][0]
                    if side=='after' and case['decision']=='corrected':png=Image.open(folder/f['normalized_png']).convert('RGBA')
                    else:
                        original=Image.open(baseline/case['character']/f['baseline_png']).convert('RGBA')
                        png=Image.new('RGBA',case['canvas']);png.paste(original,(case['padding'][0],0))
                    buf=io.BytesIO();png.save(buf,format='PNG');url='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode()
                    actual=page.locator('#'+side).screenshot()
                    page.evaluate('''async ({url,id})=>{const im=new Image();im.src=url;await im.decode();const c=selected(),can=$(id),b=can.getBoundingClientRect(),den=devicePixelRatio||1,k=Math.min(b.width/760,b.height/570),ctx=can.getContext('2d');ctx.setTransform(1,0,0,1,0,0);ctx.clearRect(0,0,can.width,can.height);ctx.setTransform(den*k,0,0,den*k,den*(b.width/2-c.root[0]*k),den*(b.height/2-(c.root[1]-220)*k));ctx.strokeStyle='#64bdab';ctx.lineWidth=1/k;ctx.beginPath();ctx.moveTo(0,c.root[1]);ctx.lineTo(c.canvas[0],c.root[1]);ctx.stroke();ctx.drawImage(im,0,0)}''',{'url':url,'id':side})
                    expected=page.locator('#'+side).screenshot();a=np.asarray(Image.open(io.BytesIO(actual)).convert('RGB'),float);b=np.asarray(Image.open(io.BytesIO(expected)).convert('RGB'),float)
                    error=float(abs(a-b).mean());assert error<3,(case['id'],side,error);pixel_errors[case['id']+'/'+side]=error
                page.evaluate('render()');page.wait_for_timeout(60)
            page.screenshot(path=str(evidence/(case['id']+'-phone.png')),full_page=True)
            page.click('#layout');assert page.locator('#beforePane').is_hidden();page.click('#layout')
            page.evaluate('pause();frame=clip().frame_count-1;render()');page.wait_for_function("$('renderState').textContent===selected().id+'/'+frame")
            page.click('#next');page.wait_for_function("$('renderState').textContent===selected().id+'/0'")
        page.set_viewport_size({'width':844,'height':390});page.wait_for_timeout(100)
        assert page.evaluate('document.body.scrollWidth<=innerWidth');page.screenshot(path=str(evidence/'landscape.png'),full_page=True)
        page.click('#runAll');page.wait_for_function('finished',timeout=60000)
        history=page.evaluate('history');assert history==[c['id'] for c in d['cases']]
        assert page.evaluate('!playing && frame===clip().frame_count-1')
        page.click('#approved');page.wait_for_selector('#character');assert page.locator('#character option').count()==4
        browser.close()
    assert not errors and not external,(errors,external)
    report={'status':'passed','normalized_pngs_checked':png_count,'source_pixel_reconstructions':source_pixels,'cases':len(d['cases']),'approved_assets_unchanged':len(d['approved_assets_sha256']),'baseline_manifests_unchanged':len(d['structural_triage']['baseline_manifest_sha256']),'pixel_mean_absolute_errors':pixel_errors,'play_all_order':history,'browser_errors':errors,'external_requests':external}
    (evidence/'validation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--folder',type=Path,required=True);p.add_argument('--baseline',type=Path,required=True);p.add_argument('--sources',type=Path,required=True);p.add_argument('--evidence',type=Path,required=True);a=p.parse_args();check(a.folder,a.baseline,a.sources,a.evidence)
