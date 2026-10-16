#!/usr/bin/env python3
"""Audit real four-character PNGs, failed gates and mobile sequence playback."""
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
from sprite_pipeline.imaging import bounds,image_rgba,affine_rgba_region
from sprite_pipeline.streaming import probe_video,decode_frames


def pixels(page,folder,entry,id):
    actual=page.locator('#'+id).screenshot()
    png=folder/entry['normalized_png'];url='data:image/png;base64,'+base64.b64encode(png.read_bytes()).decode()
    page.evaluate('''async ({url,id})=>{
      const image=new Image();image.src=url;await image.decode();
      const canvas=document.getElementById(id),rect=canvas.getBoundingClientRect(),d=devicePixelRatio||1;
      const ctx=canvas.getContext('2d'),k=Math.min(rect.width/760,rect.height/570);
      ctx.setTransform(1,0,0,1,0,0);ctx.clearRect(0,0,canvas.width,canvas.height);
      ctx.setTransform(d*k,0,0,d*k,d*(rect.width/2-1100*k),d*(rect.height/2-540*k));
      ctx.strokeStyle='#65bda7';ctx.lineWidth=1/k;ctx.beginPath();ctx.moveTo(0,760);ctx.lineTo(1800,760);ctx.stroke();
      ctx.drawImage(image,0,0);
    }''',{'url':url,'id':id})
    expected=page.locator('#'+id).screenshot()
    a=np.asarray(Image.open(io.BytesIO(actual)).convert('RGB'),float)
    b=np.asarray(Image.open(io.BytesIO(expected)).convert('RGB'),float)
    error=float(np.mean(abs(a-b)))
    assert error<3,(id,entry['normalized_png'],error)
    return error


def check(folder,sources,baseline,evidence):
    evidence.mkdir(parents=True,exist_ok=True)
    data=json.loads((folder/'assets/manifest.json').read_text())
    assert [c['id'] for c in data['characters']]==['p02','p06','p08','p11']
    clips={c['name']:c for c in data['clips']}
    checks=[];count=0
    expected={'p02':'trial_gates_passed','p06':'blocked_registration','p08':'trial_gates_failed','p11':'trial_gates_passed'}
    for char in data['characters']:
        cid=char['id'];q=char['quality'];assert q['status']==expected[cid]
        assert not q['full_frame_anatomical_validation'] and not q['approved_for_all_characters']
        old=json.loads((baseline/cid/'assets/manifest.json').read_text())
        original=probe_video(sources/cid/'dash.mp4')
        idle_source=probe_video(sources/cid/'idle.mp4')
        assert original['source_sha256']==char['source_sha256']
        assert idle_source['source_sha256']==char['global_axis_match']['idle_source_sha256']
        for role,name in char['clips'].items():
            c=clips[name];source=idle_source if role=='idle' else original
            assert c['root']==data['root'] and c['canvas']==data['canvas']
            frames=c['frames'];start=c['source_selection']['start_inclusive'];stop=c['source_selection']['stop_exclusive']
            assert [f['source_frame_index'] for f in frames]==list(range(start,stop))
            assert [f['pts'] for f in frames]==[f['pts'] for f in source['frames'][start:stop]]
            np.testing.assert_allclose([f['time_s'] for f in frames],
                [f['time_s']-source['frames'][start]['time_s'] for f in source['frames'][start:stop]])
            np.testing.assert_allclose([f['duration_s'] for f in frames],[f['duration_s'] for f in source['frames'][start:stop]])
            for frame in frames:
                count+=1;im=cv2.imread(str(folder/frame['normalized_png']),-1)
                assert list(im.shape[1::-1])==data['canvas']
                assert bounds(cv2.cvtColor(im,cv2.COLOR_BGRA2RGBA))==frame['bbox_output']
                box=frame['bbox_output'];assert frame['offset_from_root']==[box[0]-data['root'][0],box[1]-data['root'][1]]
                m=np.asarray(frame['transform_source_to_output'])
                assert m[0,0]==m[1,1] and m[0,1]==m[1,0]==0
            assert c['status']=='requires_visual_review'
            assert c['calibration_transform_source_to_output']==frames[c['reference_frame']]['transform_source_to_output']
        before=clips[char['clips']['fixed']];after=clips[char['clips']['adjusted']]
        if q['status']=='blocked_registration':
            failed=[f['source_frame_index'] for f in q['frame_diagnostics'] if 'failure' in f]
            assert failed==[44] and after['scale_correction'] is None
            for a,b in zip(before['frames'],after['frames']):
                assert digest(folder/a['normalized_png'])==digest(folder/b['normalized_png'])
            result={'character':cid,'result':q['status'],'failed_source_frames':failed,'uncertain_curve_not_applied':True}
        else:
            fits=char['correction']['fitted'];assert min(f['correlation'] for f in fits)>=.8
            residual=np.array([f['scale_to_action_reference']*a['scale_applied'] for f,a in zip(fits,after['frames'])])
            measured=float(np.ptp(residual)/np.median(residual));assert abs(measured-q['patch_after_fraction'])<1e-10
            x=[f['patch_center_source_px'][0]*a['scale_applied']+a['transform_source_to_output'][0][2]
               for f,a in zip(fits,after['frames'])]
            assert np.ptp(x)<1e-7
            body=[]
            for control in q['independent_body_controls']:
                f=next(f for f in after['frames'] if f['source_frame_index']==control['source_frame_index'])
                vector=np.diff(np.asarray(control['source_points']),axis=0)[0]
                body.append(float(np.linalg.norm(np.asarray(f['transform_source_to_output'])[:,:2]@vector)))
            body_error=float(np.ptp(body)/np.median(body));assert abs(body_error-q['body_control_difference_fraction'])<1e-10
            if q['status']=='trial_gates_passed':assert measured<=.02 and body_error<=.06
            else:assert measured>.02 and 'local_patch_residual' in q['failure_reasons']
            result={'character':cid,'result':q['status'],'patch_before_fraction':q['patch_before_fraction'],
                    'patch_after_fraction':measured,'body_control_difference_fraction':body_error}
        # Verify actual source pixels against a saved normalized frame, not only metadata.
        first=after['frames'][0];source_index=first['source_frame_index']
        dash_meta=next(c for c in old['clips'] if c['name']=='dash')
        for i,raw in enumerate(decode_frames(sources/cid/'dash.mp4',dash_meta)):
            if i!=source_index:continue
            m=np.asarray(first['transform_source_to_output'])
            expected_png=affine_rgba_region(image_rgba(raw,dash_meta['background_treatment']),m[0,0],m[:,2].tolist(),data['canvas'])
            actual=cv2.cvtColor(cv2.imread(str(folder/first['normalized_png']),-1),cv2.COLOR_BGRA2RGBA)
            assert np.array_equal(actual,expected_png)
        checks.append(result)
    assert count==228
    errors=[];requests=[];pixel_results={};sequence_results={}
    with sync_playwright() as pw:
        browser=pw.chromium.launch(args=['--no-sandbox'])
        for portable in (False,True):
            page=browser.new_page(viewport={'width':390,'height':844},is_mobile=True,has_touch=True)
            page.on('pageerror',lambda e:errors.append(str(e)))
            page.on('requestfailed',lambda r:requests.append(r.url))
            page.on('request',lambda r:requests.append(r.url) if r.url.startswith(('https://','http://')) else None)
            path=folder/'dash-quatro-personagens.html' if portable else folder/'assets/index.html'
            page.goto(path.resolve().as_uri());page.wait_for_function("$('renderState').textContent==='p02/cycle/0/0/fixed/0'")
            for index,char in enumerate(data['characters']):
                cid=char['id'];page.select_option('#character',str(index))
                page.wait_for_function(f"$('renderState').textContent==='{cid}/cycle/0/0/fixed/0'")
                assert page.evaluate('document.body.scrollWidth<=innerWidth')
                left=page.locator('#left').bounding_box();right=page.locator('#right').bounding_box()
                assert abs(left['y']-right['y'])<1 and abs(left['height']-right['height'])<1
                after=clips[char['clips']['adjusted']];before=clips[char['clips']['fixed']]
                pixel_results[f'{portable}/{cid}/left']=pixels(page,folder,after['frames'][0],'left')
                pixel_results[f'{portable}/{cid}/right']=pixels(page,folder,before['frames'][0],'right')
                page.evaluate('render()');page.wait_for_timeout(100)
                page.screenshot(path=str(evidence/f'{cid}-{portable}-phone.png'),full_page=True)
                page.select_option('#comparison','idle')
                page.wait_for_function("$('renderState').textContent.includes('/idle/')")
                idle=clips[char['clips']['idle']]
                pixel_results[f'{portable}/{cid}/idle']=pixels(page,folder,idle['frames'][idle['reference_frame']],'right')
                page.select_option('#comparison','fixed');page.wait_for_function("$('renderState').textContent.includes('/fixed/')")
                page.click('#layout');page.wait_for_timeout(100)
                assert page.locator('#rightPane').is_hidden()
                page.click('#layout')
                page.evaluate('pause();frameIndex=clip().frame_count-1;render()')
                page.wait_for_function("$('renderState').textContent.split('/')[3]===String(frameIndex)")
                page.click('#next');page.wait_for_function("$('renderState').textContent.split('/')[3]==='0'")
            page.click('#all')
            page.wait_for_function('finishedAll===true',timeout=30000)
            history=page.evaluate('stateHistory');assert page.evaluate("characterIndex===3&&phaseIndex===3&&!playing")
            for cid in ('p02','p06','p08','p11'):
                states=[row for row in history if row[0]==cid and row[1]=='all']
                assert [(row[2],row[3]) for row in states]==[(0,'idle'),(1,'adjusted'),(2,'adjusted'),(3,'idle')],states
            sequence_results[str(portable)]={'all_four_completed':True,'phase_history':history}
            page.set_viewport_size({'width':844,'height':390});page.wait_for_timeout(150)
            page.screenshot(path=str(evidence/f'all-{portable}-landscape.png'),full_page=True)
            page.close()
        browser.close()
    assert not errors,errors
    assert not requests,requests
    result={'characters':checks,'exported_pngs':count,'source_pts_preserved':True,
        'all_selected_frames_preserved':True,'sequence':sequence_results,'preview_vs_png_mean_rgb_errors':pixel_results,
        'page_errors':errors,'external_or_failed_requests':requests,'full_frame_anatomical_validation':False,
        'approved_as_universal_standard':False}
    (evidence/'dash-trial-checks.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('folder',type=Path);p.add_argument('--sources',type=Path,required=True)
    p.add_argument('--baseline',type=Path,required=True);p.add_argument('--evidence',type=Path,required=True)
    a=p.parse_args();check(a.folder,a.sources,a.baseline,a.evidence)
