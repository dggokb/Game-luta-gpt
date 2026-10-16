#!/usr/bin/env python3
"""Audit the single P04 candidate against real source pixels and its predecessor."""
import argparse,base64,io,json,sys
from pathlib import Path
import cv2
import numpy as np
from PIL import Image
from playwright.sync_api import sync_playwright
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from sprite_pipeline.extraction import digest
from sprite_pipeline.imaging import image_rgba,affine_rgba_region,bounds
from sprite_pipeline.streaming import decode_frames


def check(folder,sources,baseline,previous,evidence):
    evidence.mkdir(parents=True,exist_ok=True);d=json.loads((folder/'assets/manifest.json').read_text());cfg=json.loads((folder/'assets/project.snapshot.json').read_text());clips={c['name']:c for c in d['clips']}
    old=json.loads((baseline/'p04/assets/manifest.json').read_text());dash=next(c for c in old['clips'] if c['name']=='dash');idle=next(c for c in old['clips'] if c['name']=='idle')
    assert digest(sources/'p04/dash.mp4')==d['source_sha256'] and digest(sources/'p04/idle.mp4')==cfg['idle_source_sha256']
    assert digest(baseline/'p04/assets/manifest.json')==d['baseline_manifest_sha256'];assert digest(previous/'assets/manifest.json')==d['previous_preview_manifest_sha256']
    previous_clip=next(c for c in json.loads((previous/'assets/manifest.json').read_text())['clips'] if c['name']=='p04_dash_after')
    for f,b in zip(clips['before']['frames'],previous_clip['frames']):assert digest(folder/f['normalized_png'])==digest(previous/b['normalized_png'])
    assert digest(folder/clips['idle']['frames'][0]['normalized_png'])==digest(baseline/'p04'/idle['frames'][idle['reference_frame']]['normalized_png'])
    count=0;decoded=0
    for c in d['clips']:
        assert c['canvas']==d['canvas'] and c['root']==d['root']
        for f in c['frames']:
            full=cv2.imread(str(folder/f['normalized_png']),-1);preview=cv2.imread(str(folder/'assets'/f['normalized_file']),-1);assert digest(folder/f['normalized_png'])==f['output_png_sha256']
            assert list(full.shape[1::-1])==d['canvas'];x,y,w,h=f['normalized_destination_rect'];np.testing.assert_array_equal(preview,full[y:y+h,x:x+w]);decoded+=w*h*4
            assert bounds(cv2.cvtColor(full,cv2.COLOR_BGRA2RGBA))==f['bbox_output'];count+=1
    assert count==31 and decoded<60_000_000
    for name in ('body','full','effects'):
        c=clips[name];assert [f['source_frame_index'] for f in c['frames']]==list(range(37,43))
        for f in c['frames']:
            s=dash['frames'][f['source_frame_index']];assert f['pts']==s['pts'] and f['duration_s']==s['duration_s'];assert abs(f['time_s']-(s['time_s']-dash['frames'][37]['time_s']))<1e-9
            m=np.asarray(f['transform_source_to_output']);assert m[0,0]==m[1,1]==dash['scale_per_video'];assert m[0,1]==m[1,0]==0
    n=0
    for i,raw in enumerate(decode_frames(sources/'p04/dash.mp4',dash)):
        if 37<=i<43:
            rgba=image_rgba(raw,dash['background_treatment']);mask=np.zeros(rgba.shape[:2],np.uint8);cv2.fillPoly(mask,[np.asarray(cfg['body_keep_polygons'][str(i)])],255)
            body=rgba.copy();body[:,:,3]=np.minimum(rgba[:,:,3],mask);effect=rgba.copy();effect[:,:,3]=np.where(mask==0,rgba[:,:,3],0);np.testing.assert_array_equal(body[:,:,3].astype(int)+effect[:,:,3],rgba[:,:,3])
            for name,im in [('body',body),('full',rgba),('effects',effect)]:
                f=clips[name]['frames'][i-37];m=np.asarray(f['transform_source_to_output']);expected=affine_rgba_region(im,m[0,0],m[:,2].tolist(),d['canvas']);actual=cv2.cvtColor(cv2.imread(str(folder/f['normalized_png']),-1),cv2.COLOR_BGRA2RGBA);np.testing.assert_array_equal(actual,expected);n+=1
        if i==42:break
    assert n==18
    controls=cfg['body_controls'];lengths=np.array([np.linalg.norm(np.diff(c['points'],axis=0)) for c in controls]);assert abs(np.ptp(lengths)/np.median(lengths)-d['measurements']['torso_spread_fraction'])<1e-12
    xs=[fit['patch_center_source_px'][0]*f['scale_applied']+f['transform_source_to_output'][0][2] for fit,f in zip(d['measurements']['head_tracking'],clips['body']['frames'])];assert np.ptp(xs)<1e-7
    order=[4,5,0,1];errors=[];external=[];pixel_errors=[];trace=[];seam_trace=[]
    with sync_playwright() as pw:
        b=pw.chromium.launch(args=['--no-sandbox'])
        for portable in (False,True):
            page=b.new_page(viewport={'width':390,'height':844},is_mobile=True,has_touch=True);page.on('pageerror',lambda e:errors.append(str(e)));page.on('requestfailed',lambda r:errors.append(r.url));page.on('request',lambda r:external.append(r.url) if r.url.startswith(('http://','https://')) else None)
            page.goto((folder/('p04-corrida.html' if portable else 'assets/index.html')).resolve().as_uri());page.wait_for_function('ready');page.evaluate('step(0)');page.wait_for_function("$('renderState').textContent==='run/body/0/before/2'")
            for variant in ('body','full','effects'):
                page.select_option('#variant',variant)
                for j in range(6):
                    page.evaluate('step('+str(j)+')');page.wait_for_function("$('renderState').textContent==='run/"+variant+"/"+str(j)+"/before/"+str(j+2)+"'")
                    f=clips[variant]['frames'][j];actual=page.locator('#after').screenshot();url='data:image/png;base64,'+base64.b64encode((folder/f['normalized_png']).read_bytes()).decode()
                    page.evaluate('''async url=>{const im=new Image();im.src=url;await im.decode();const can=$('after'),box=can.getBoundingClientRect(),den=devicePixelRatio||1,k=Math.min(box.width/760,box.height/570)*zoom,ctx=can.getContext('2d');ctx.setTransform(1,0,0,1,0,0);ctx.clearRect(0,0,can.width,can.height);ctx.setTransform(den*k,0,0,den*k,den*(box.width/2-data.root[0]*k),den*(box.height/2-(data.root[1]-200)*k));ctx.strokeStyle='#6cbdac';ctx.lineWidth=1/k;ctx.beginPath();ctx.moveTo(0,data.root[1]);ctx.lineTo(data.canvas[0],data.root[1]);ctx.stroke();ctx.drawImage(im,0,0)}''',url)
                    expected=page.locator('#after').screenshot();err=float(np.mean(abs(np.asarray(Image.open(io.BytesIO(actual)).convert('RGB'),float)-np.asarray(Image.open(io.BytesIO(expected)).convert('RGB'),float))));assert err<.1,(portable,variant,j,err);pixel_errors.append(err)
                page.evaluate('render()');page.wait_for_timeout(50)
            page.select_option('#variant','body');page.evaluate('step(0)');page.wait_for_function("$('renderState').textContent==='run/body/0/before/2'");assert page.evaluate('document.body.scrollWidth<=innerWidth');page.screenshot(path=str(evidence/f'phone-{portable}.png'),full_page=True)
            page.click('#layout');page.wait_for_timeout(60);boxes=[page.locator('#'+s).bounding_box() for s in ('before','after')];assert abs(boxes[0]['y']-boxes[1]['y'])<1;page.screenshot(path=str(evidence/f'phone-comparison-{portable}.png'),full_page=True)
            page.select_option('#reference','idle');page.wait_for_function("$('renderState').textContent==='run/body/0/idle/0'");page.set_viewport_size({'width':844,'height':390});page.wait_for_timeout(80);assert page.evaluate('document.body.scrollWidth<=innerWidth');page.screenshot(path=str(evidence/f'idle-landscape-{portable}.png'),full_page=True)
            page.select_option('#reference','before');page.evaluate("window.trace=[];window.addEventListener('repairframe',()=>{let i=currentIndex();if(window.trace.at(-1)!==i)window.trace.push(i)})");page.click('#play');page.wait_for_timeout(1700);page.evaluate('pause()');live=page.evaluate('window.trace');assert len(live)>=24 and all(v==(u+1)%6 for u,v in zip(live,live[1:])),live;trace.append(live)
            page.evaluate('window.trace=[]');page.click('#seam');page.wait_for_timeout(1700);page.evaluate('pause()');live=page.evaluate('window.trace');assert len(live)>=8 and live[:4]==[4,5,0,1] and all(v==order[(order.index(u)+1)%4] for u,v in zip(live,live[1:])),live;seam_trace.append(live)
            page.close()
        b.close()
    assert not errors and not external,(errors,external)
    report={'status':'artifact_checks_passed_visual_review_still_required','pngs_checked':count,'source_pixel_reconstructions':n,'preview_decoded_bytes':decoded,'maximum_canvas_pixel_mean_error':max(pixel_errors),'live_normal_frame_traces':trace,'live_seam_frame_traces':seam_trace,'browser_errors':errors,'external_requests':external,'visual_approval':False,'automatic_rollout':False,'torso_spread_fraction':d['measurements']['torso_spread_fraction'],'note':'Frame order, pixels and measured segments are verified; these checks do not certify perceived animation quality.'}
    (evidence/'validation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--folder',type=Path,required=True);p.add_argument('--sources',type=Path,required=True);p.add_argument('--baseline',type=Path,required=True);p.add_argument('--previous',type=Path,required=True);p.add_argument('--evidence',type=Path,required=True);a=p.parse_args();check(a.folder,a.sources,a.baseline,a.previous,a.evidence)
