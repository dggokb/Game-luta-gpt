#!/usr/bin/env python3
"""A source-bound, manually reviewed P04 running cycle; no automatic rollout."""
import argparse,base64,copy,json,shutil,sys
from pathlib import Path
import cv2
import numpy as np
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from sprite_pipeline.action_cycle import extract_cycle,register_patch,axis_length
from sprite_pipeline.imaging import image_rgba,affine_rgba_region,bounds
from sprite_pipeline.streaming import decode_frames
from sprite_pipeline.extraction import digest
from sprite_pipeline.exporting import write_json


def generate(sources,baseline,previous,output,settings):
    if output.exists() and any(output.iterdir()):raise ValueError('Use saída nova ou vazia')
    cfg=json.loads(settings.read_text());old=json.loads((baseline/'p04/assets/manifest.json').read_text())
    dash=next(c for c in old['clips'] if c['name']=='dash');idle=next(c for c in old['clips'] if c['name']=='idle')
    assert digest(sources/'p04/dash.mp4')==cfg['source_sha256']==dash['source_sha256']
    assert digest(sources/'p04/idle.mp4')==cfg['idle_source_sha256']==idle['source_sha256']
    original_manifest_sha=digest(baseline/'p04/assets/manifest.json');previous_manifest_sha=digest(previous/'assets/manifest.json')
    start,stop=cfg['start'],cfg['stop'];raw=[im for i,im in enumerate(decode_frames(sources/'p04/dash.mp4',dash)) if start<=i<stop]
    fits=register_patch(raw,cfg['reference_source_frame']-start,cfg['polygon'],search_radius_px=600)
    scale=dash['scale_per_video']*cfg['global_factor'];root=old['root'];canvas=old['canvas'];assets=output/'assets';assets.mkdir(parents=True)
    d={'status':'single_case_manual_candidate','character':'p04','source_sha256':cfg['source_sha256'],'configuration_sha256':digest(settings),
       'baseline_manifest_sha256':original_manifest_sha,'previous_preview_manifest_sha256':previous_manifest_sha,
       'root':root,'canvas':canvas,'automatic_rollout':False,'visual_approval':False,'clips':[],
       'notes':cfg['notes'],'internal_scale_method':cfg['internal_scale_method'],'previous_apk_version':'1.8-triagem'}
    selected=extract_cycle(dash,start,stop);reference=cfg['reference_source_frame']-start;anchor_ref=fits[reference]['patch_center_source_px'][0]
    for kind in ('body','full','effects'):
        c=copy.deepcopy(selected);c.update(name=kind,role=kind,root=root,canvas=canvas,reference_frame=reference,scale_mode='constant_running_cycle',scale_per_video=scale,
          scale_correction=None,reference_pose='dash_running',source_root=[cfg['source_hip_x'],cfg['source_ground_y']],
          root_method='manual_hip_offset_and_registered_head_x',status='requires_visual_review',loops=[],
          root_motion={'mode':'in_place_x','anchor':'registered_head_patch_center','reference_frame':reference,'reference_source_frame':cfg['reference_source_frame'],
             'source_sha256':cfg['source_sha256'],'anchor_x_source_px':[f['patch_center_source_px'][0] for f in fits],
             'removed_x_output_px':[(f['patch_center_source_px'][0]-anchor_ref)*scale for f in fits],
             'game_applies_displacement':True,'vertical_motion':'preserved'})
        d['clips'].append(c)
    partition_counts=[]
    for j,(im,fit) in enumerate(zip(raw,fits)):
        rgba=image_rgba(im,dash['background_treatment']);mask=np.zeros(rgba.shape[:2],np.uint8)
        cv2.fillPoly(mask,[np.asarray(cfg['body_keep_polygons'][str(start+j)],np.int32)],255)
        body=rgba.copy();body[:,:,3]=np.minimum(rgba[:,:,3],mask)
        effect=rgba.copy();effect[:,:,3]=np.where(mask==0,rgba[:,:,3],0)
        assert np.array_equal(body[:,:,3].astype(int)+effect[:,:,3],rgba[:,:,3])
        partition_counts.append(int(np.count_nonzero(effect[:,:,3])))
        dx=root[0]-cfg['source_hip_x']*scale-(fit['patch_center_source_px'][0]-anchor_ref)*scale;dy=root[1]-cfg['source_ground_y']*scale
        matrix=[[scale,0,dx],[0,scale,dy]]
        for kind,source_rgba in [('body',body),('full',rgba),('effects',effect)]:
            dest=output/'normalized'/kind/f'{j:03d}.png';dest.parent.mkdir(parents=True,exist_ok=True)
            out=affine_rgba_region(source_rgba,scale,[dx,dy],canvas);assert cv2.imwrite(str(dest),cv2.cvtColor(out,cv2.COLOR_RGBA2BGRA))
            x0,y0,cw,ch=cv2.boundingRect(out[:,:,3]);x1,y1=x0+cw,y0+ch;preview=assets/'images'/kind/dest.name;preview.parent.mkdir(parents=True,exist_ok=True)
            assert cv2.imwrite(str(preview),cv2.cvtColor(out[y0:y1,x0:x1],cv2.COLOR_RGBA2BGRA))
            f=next(c for c in d['clips'] if c['name']==kind)['frames'][j]
            f.update(normalized_file=preview.relative_to(assets).as_posix(),normalized_png=dest.relative_to(output).as_posix(),normalized_destination_rect=[x0,y0,x1-x0,y1-y0],transform_source_to_output=matrix,
               scale_applied=scale,bbox_source=bounds(source_rgba),bbox_output=bounds(out),removed_root_motion_output_px=[(fit['patch_center_source_px'][0]-anchor_ref)*scale,0],
               output_png_sha256=digest(dest))
            for key in ('raw_file','raw_rect','normalized_rect'):f.pop(key,None)
    # Preserve the actual predecessor PNGs, including its problematic loop.
    prev=json.loads((previous/'assets/manifest.json').read_text());before=copy.deepcopy(next(c for c in prev['clips'] if c['name']=='p04_dash_after'));before['name']='before'
    for j,f in enumerate(before['frames']):
        src=previous/f['normalized_png'];dest=output/'normalized/before'/f'{j:03d}.png';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dest)
        im=cv2.imread(str(src),-1);x0,y0,cw,ch=cv2.boundingRect(im[:,:,3]);x1,y1=x0+cw,y0+ch;preview=assets/'images/before'/dest.name;preview.parent.mkdir(parents=True,exist_ok=True);assert cv2.imwrite(str(preview),im[y0:y1,x0:x1])
        f.update(normalized_file=preview.relative_to(assets).as_posix(),normalized_png=dest.relative_to(output).as_posix(),normalized_destination_rect=[x0,y0,x1-x0,y1-y0],output_png_sha256=digest(dest))
        for key in ('normalized_rect',):f.pop(key,None)
    d['clips'].append(before)
    idle_clip=extract_cycle(idle,idle['reference_frame'],min(idle['reference_frame']+3,idle['frame_count']))
    # One static reference, no new pose or rescaling.
    idle_clip.update(name='idle',frame_count=1,frames=idle_clip['frames'][:1],reference_frame=0,root=root,canvas=canvas)
    f=idle_clip['frames'][0];src=baseline/'p04'/f['normalized_png'];dest=output/'normalized/idle/000.png';dest.parent.mkdir(parents=True);shutil.copy2(src,dest)
    im=cv2.imread(str(src),-1);x0,y0,cw,ch=cv2.boundingRect(im[:,:,3]);x1,y1=x0+cw,y0+ch;preview=assets/'images/idle/000.png';preview.parent.mkdir(parents=True);assert cv2.imwrite(str(preview),im[y0:y1,x0:x1])
    f.update(normalized_file=preview.relative_to(assets).as_posix(),normalized_png=dest.relative_to(output).as_posix(),normalized_destination_rect=[x0,y0,x1-x0,y1-y0],output_png_sha256=digest(dest),source_frame_index=idle['reference_frame'])
    idle_clip['duration_s']=f['duration_s'];idle_clip['source_selection']['stop_exclusive']=idle['reference_frame']+1
    for key in ('normalized_rect','raw_file','raw_rect'):f.pop(key,None)
    d['clips'].append(idle_clip)
    for c in d['clips']:
        c['calibration_transform_source_to_output']=c['frames'][c['reference_frame']]['transform_source_to_output']
        first=c['frames'][0]['bbox_output']
        for f in c['frames']:
            b=f['bbox_output'];f['offset_from_root']=[b[0]-root[0],b[1]-root[1]]
            f['visual_displacement_from_reference']=[(b[0]+b[2]-first[0]-first[2])/2,b[3]-first[3]]
    lengths=[axis_length(c['points']) for c in cfg['body_controls']];head=[f['scale_to_action_reference'] for f in fits]
    proposed=np.median([np.roll(head,1),head,np.roll(head,-1)],axis=0);head_only_torso=np.array(lengths)/proposed
    d['measurements']={'body_controls':cfg['body_controls'],'torso_spread_fraction':float(np.ptp(lengths)/np.median(lengths)),
       'head_patch_spread_fraction':float(np.ptp(head)/np.median(head)),
       'rejected_head_only_curve_torso_spread_fraction':float(np.ptp(head_only_torso)/np.median(head_only_torso)),
       'head_tracking':fits,'global_factor':cfg['global_factor'],'global_axis_match':cfg['global_axis_match'],
       'manual_effect_partition_nonzero_pixels':partition_counts,'constant_output_scale':scale,
       'note':'Projected, manually annotated torso control; not a full-body anatomy guarantee. Head-only scale correction was intentionally not applied.'}
    d['mobile_preview']={'frame_count':sum(c['frame_count'] for c in d['clips']),'format':'Lossless PNG crops, original output coordinates','source_pts_preserved':True,'playback_times_rebased':True,'frames_synthesized':False}
    assert digest(baseline/'p04/assets/manifest.json')==original_manifest_sha
    assert digest(previous/'assets/manifest.json')==previous_manifest_sha
    write_json(assets/'manifest.json',d);write_json(assets/'project.snapshot.json',cfg)
    page=(ROOT/'sprite_pipeline/p04_repair.html').read_text().replace('__MANIFEST__',json.dumps(d,ensure_ascii=False).replace('<','\\u003c'))
    (assets/'index.html').write_text(page)
    encoded={p.relative_to(assets).as_posix():'data:image/png;base64,'+base64.b64encode(p.read_bytes()).decode() for p in assets.rglob('*.png')}
    portable=page.replace('const embeddedImages={};','const embeddedImages='+json.dumps(encoded)+';')
    (output/'p04-corrida.html').write_text(portable)
    (output/'LEIA-ME.txt').write_text('P04 — candidato manual de corrida\n\nCorpo: normalized/body (6 PNGs). Versão com os efeitos originais: normalized/full. Efeitos externos recortados: normalized/effects; são uma camada derivada, não um sistema de partículas pronto.\nAntes conserva 12 PNGs da triagem anterior. Idle é uma referência estática já existente. Todos usam o mesmo canvas/root.\nA passada nova contém a fonte 37–42 consecutiva (numeração a partir de zero), com PTS e durações originais. Não há inversão, espelhamento nem novas poses.\nA escala geral anterior foi preservada; o tronco foi conferido nos seis frames. A curva da cabeça não foi aplicada porque aumentava a variação do tronco. As marcações têm incerteza e a cabeça ainda muda com a pose.\nO recorte manual da poeira pode deixar resíduo nas bordas dos calçados, onde arte e efeito estão misturados. O original permanece inteiro.\nEste caso exige avaliação visual; não autoriza processamento automático da coleção.\n')
    return d

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--sources',type=Path,required=True);p.add_argument('--baseline',type=Path,required=True);p.add_argument('--previous',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--settings',type=Path,default=ROOT/'examples/collection/p04-dash-repair.json');a=p.parse_args();generate(a.sources,a.baseline,a.previous,a.output,a.settings)
