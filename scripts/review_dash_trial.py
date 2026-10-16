#!/usr/bin/env python3
"""Apply one dash-cycle algorithm and the same gates to four explicit cases."""
import argparse
import base64
import copy
import json
from pathlib import Path
import shutil
import sys
import cv2
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from sprite_pipeline.action_cycle import (extract_cycle,register_patch,cycle_transforms,
                                         global_axis_factor,axis_length,assess_cycle)
from sprite_pipeline.errors import CalibrationError
from sprite_pipeline.extraction import digest
from sprite_pipeline.exporting import write_json,contact_sheet
from sprite_pipeline.imaging import image_rgba,affine_rgba_region,bounds
from sprite_pipeline.streaming import decode_frames
from process_collection import PreviewPages

CANVAS=[1800,900]
ROOT_POINT=[1100,760]


def finalize_geometry(manifest,settings):
    """Replace inherited collection coordinates with the trial's actual geometry."""
    configs={c['id']:c for c in settings['characters']}
    characters={c['id']:c for c in manifest['characters']}
    for clip in manifest['clips']:
        case=characters[clip['character']];cfg=configs[clip['character']]
        clip.update(root=ROOT_POINT,canvas=CANVAS)
        boxes=[f['bbox_output'] for f in clip['frames']];first=boxes[0]
        for frame,box in zip(clip['frames'],boxes):
            frame['offset_from_root']=[box[0]-ROOT_POINT[0],box[1]-ROOT_POINT[1]]
            frame['visual_displacement_from_reference']=[(box[0]+box[2]-first[0]-first[2])/2,box[3]-first[3]]
        clip['calibration_transform_source_to_output']=clip['frames'][clip['reference_frame']]['transform_source_to_output']
        if clip['role']=='idle':continue
        clip['reference_pose']='dash_running'
        clip['metrics']={'scope':'experimental_trial_only','quality_status':case['quality']['status']}
        if case['correction']:
            fits=case['correction']['fitted'];offset=case['correction']['horizontal_offset_output_px']
            clip.update(source_root=[cfg['source_hip_x'],cfg['source_ground_y']],
                        root_method='annotated_hip_offset_and_registered_action_patch_x')
            removed=[]
            for f,fit in zip(clip['frames'],fits):
                dx=(fit['patch_center_source_px'][0]-cfg['source_hip_x'])*f['scale_applied']-offset
                f['removed_root_motion_output_px']=[dx,0];removed.append(dx)
            clip['root_motion'].update(anchor='registered_face_patch_center',
                anchor_x_source_px=[f['patch_center_source_px'][0] for f in fits],removed_x_output_px=removed,
                reference_frame=clip['reference_frame'],source_sha256=clip['source_sha256'],
                vertical_motion='preserved',horizontal_offset_output_px=offset)
        else:
            clip['root_method']='legacy_torso_band_blocked_trial'
            for frame in clip['frames']:
                original=frame.setdefault('original_removed_root_motion_output_px',frame.get('removed_root_motion_output_px',[0,0]))
                frame['removed_root_motion_output_px']=[original[0]*case['global_axis_match']['factor'],original[1]*case['global_axis_match']['factor']]
            clip['root_motion'].update(anchor='legacy_torso_band',method='blocked_trial_fixed_scale_fallback',
                removed_x_output_px=[f['removed_root_motion_output_px'][0] for f in clip['frames']])
    manifest['geometry_metadata_version']=1


def generate(sources,baseline,settings_path,output):
    if output.exists() and any(output.iterdir()):raise ValueError('Use uma saída nova ou vazia')
    cfg=json.loads(settings_path.read_text());assets=output/'assets';assets.mkdir(parents=True)
    manifest={'status':'four_character_dash_trial','canvas':CANVAS,'root':ROOT_POINT,
        'method':cfg['method'],'configuration_sha256':digest(settings_path),
        'thresholds':{k:cfg[k] for k in ('minimum_correlation','maximum_patch_residual_fraction',
                                        'maximum_body_control_difference_fraction')},
        'characters':[],'clips':[],
        'note':'Experimental action-only cycles; no full-body or collection-wide approval.'}
    evidence_images=[];evidence_labels=[]
    for char in cfg['characters']:
        cid=char['id'];base=baseline/cid
        old=json.loads((base/'assets/manifest.json').read_text())
        dash=next(c for c in old['clips'] if c['name']=='dash')
        idle=next(c for c in old['clips'] if c['name']=='idle')
        source=sources/cid/'dash.mp4'
        assert digest(source)==char['source_sha256']==dash['source_sha256']
        start,stop=char['start'],char['stop'];reference=char['reference_source_frame']-start
        selected=extract_cycle(dash,start,stop)
        raw=[im for i,im in enumerate(decode_frames(source,dash)) if start<=i<stop]
        base_scale=dash.get('scale_per_video') or 1.
        factor=global_axis_factor(char['idle_head_axis'],idle['scale_per_video'],char['dash_head_axis'],base_scale)
        fits=None;registration_failure=None
        try:
            fits=register_patch(raw,reference,char['polygon'],minimum_correlation=cfg['minimum_correlation'],
                                search_radius_px=cfg['search_radius_px'])
        except CalibrationError as error:
            registration_failure=str(error)
        if fits:
            offset=(fits[reference]['patch_center_source_px'][0]-char['source_hip_x'])*base_scale*factor
            transforms,correction=cycle_transforms(fits,base_scale,factor,char['source_ground_y'],
                                                    horizontal_offset_output_px=offset)
            fixed=[[[base_scale*factor,0,-f['patch_center_source_px'][0]*base_scale*factor+offset],
                    [0,base_scale*factor,-char['source_ground_y']*base_scale*factor]] for f in fits]
        else:
            # Do not export a low-confidence estimate or interpolate over a failed
            # source frame. Show the existing fixed-scale baseline on both sides.
            fixed=[]
            for frame in dash['frames'][start:stop]:
                m=np.asarray(frame['transform_source_to_output'],float).copy()
                m[:,2]-=old['root'];m*=factor
                fixed.append(m.tolist())
            transforms=copy.deepcopy(fixed);correction=None
        controls=[]
        for control in char['body_controls']:
            i=control['frame']-start
            if not 0<=i<len(raw):raise ValueError('Controle corporal fora do ciclo')
            points=np.asarray(control['points'],float)
            if np.any(points<0) or np.any(points[:,0]>=dash['resolution'][0]) or np.any(points[:,1]>=dash['resolution'][1]):
                raise ValueError('Controle corporal fora da fonte')
            controls.append({'source_frame_index':control['frame'],'segment':control['segment'],
                'source_points':control['points'],'output_length_px':axis_length(points)*transforms[i][0][0]})
        if fits:
            quality=assess_cycle(fits,transforms,controls,minimum_correlation=cfg['minimum_correlation'],
                maximum_patch_residual=cfg['maximum_patch_residual_fraction'],
                maximum_body_difference=cfg['maximum_body_control_difference_fraction'])
            quality['minimum_correlation']=min(f['correlation'] for f in fits)
        else:
            diagnostics=[]
            for i,im in enumerate(raw):
                try:
                    pair=register_patch([raw[reference],im],0,char['polygon'],
                        minimum_correlation=cfg['minimum_correlation'],search_radius_px=cfg['search_radius_px'])
                    diagnostics.append({'source_frame_index':start+i,'correlation':pair[1]['correlation']})
                except CalibrationError as error:
                    diagnostics.append({'source_frame_index':start+i,'failure':str(error)})
            quality={'status':'blocked_registration','failure_reasons':['registration_confidence'],
                'reason':registration_failure,'frame_diagnostics':diagnostics,
                'independent_body_controls':controls,'full_frame_anatomical_validation':False,
                'approved_for_all_characters':False,'fallback':'unchanged_fixed_scale_baseline_on_both_sides'}
        character={'id':cid,'source_sha256':digest(source),'selection':selected['source_selection'],
            'baseline_collection_status':dash['status'],'baseline_rejection_reason':dash.get('rejection_reason'),
            'global_axis_match':{'idle_source_sha256':idle['source_sha256'],
                'idle_reference_source_frame':idle['reference_frame'],
                'idle_points':char['idle_head_axis'],'dash_points':char['dash_head_axis'],
                'base_scale':base_scale,'factor':factor,'output_scale':base_scale*factor,
                'idle_axis_output_px':axis_length(char['idle_head_axis'])*idle['scale_per_video'],
                'note':'Approximate manual projected head axis, used only for one overall factor. Not independent anatomy validation.'},
            'quality':quality,'correction':correction}
        # Two action versions share source selection, fixed world coordinates and
        # overall scale; only the internal scale curve differs.
        for role,matrices in [('fixed',fixed),('adjusted',transforms)]:
            clip=copy.deepcopy(selected);clip.update(name=f'{cid}_{role}',character=cid,role=role,
                loops=[],reference_frame=reference,status='requires_visual_review',
                scale_per_video=base_scale*factor,scale_correction=correction if role=='adjusted' else None,
                root_motion={'mode':'in_place_x','game_applies_displacement':True},
                root=ROOT_POINT,canvas=CANVAS)
            pngs=output/'normalized'/cid/role;pngs.mkdir(parents=True)
            pages=PreviewPages(assets,f'images/{cid}/{role}','normalized',CANVAS)
            for i,(im,matrix,frame) in enumerate(zip(raw,matrices,clip['frames'])):
                m=np.asarray(matrix,float).copy();m[:,2]+=ROOT_POINT
                rgba=affine_rgba_region(image_rgba(im,dash['background_treatment']),float(m[0,0]),m[:,2].tolist(),CANVAS)
                path=pngs/f'{i:06d}.png';assert cv2.imwrite(str(path),cv2.cvtColor(rgba,cv2.COLOR_RGBA2BGRA))
                frame.update(normalized_png=path.relative_to(output).as_posix(),
                    transform_source_to_output=m.tolist(),scale_applied=float(m[0,0]),bbox_output=bounds(rgba))
                pages.add(rgba,frame)
                if i==reference:
                    evidence_images.append(rgba[230:830,720:1480]);evidence_labels.append(f'{cid.upper()} {role}')
            pages.flush()
            for frame in clip['frames']:
                frame.pop('raw_file',None);frame.pop('raw_rect',None)
            manifest['clips'].append(clip)
        idle_ref=idle['reference_frame'];idle_start=max(0,idle_ref-4);idle_stop=min(idle['frame_count'],idle_start+12)
        idle_clip=extract_cycle(idle,idle_start,idle_stop)
        idle_clip.update(name=f'{cid}_idle',character=cid,role='idle',loops=[],reference_frame=idle_ref-idle_start)
        pngs=output/'normalized'/cid/'idle';pngs.mkdir(parents=True)
        pages=PreviewPages(assets,f'images/{cid}/idle','normalized',CANVAS)
        # Pure integer translation reuses the accepted Idle raster without another
        # scale/interpolation. Its original transform remains authoritative.
        translation=np.array(ROOT_POINT)-np.array(old['root'])
        for i,frame in enumerate(idle_clip['frames']):
            rgba=cv2.cvtColor(cv2.imread(str(base/frame['normalized_png']),-1),cv2.COLOR_BGRA2RGBA)
            shifted=np.zeros((CANVAS[1],CANVAS[0],4),np.uint8)
            dx,dy=map(int,translation);h,w=rgba.shape[:2]
            sx,sy=max(0,-dx),max(0,-dy);tx,ty=max(0,dx),max(0,dy)
            cw,ch=min(w-sx,CANVAS[0]-tx),min(h-sy,CANVAS[1]-ty)
            shifted[ty:ty+ch,tx:tx+cw]=rgba[sy:sy+ch,sx:sx+cw]
            assert int(shifted[:,:,3].sum())==int(rgba[:,:,3].sum()),'Idle recortado pelo novo canvas'
            path=pngs/f'{i:06d}.png';assert cv2.imwrite(str(path),cv2.cvtColor(shifted,cv2.COLOR_RGBA2BGRA))
            m=np.asarray(frame['transform_source_to_output'],float);m[:,2]+=translation
            frame.update(normalized_png=path.relative_to(output).as_posix(),bbox_output=bounds(shifted),transform_source_to_output=m.tolist())
            pages.add(shifted,frame)
        pages.flush()
        for frame in idle_clip['frames']:
            frame.pop('raw_file',None);frame.pop('raw_rect',None)
        manifest['clips'].append(idle_clip)
        character['clips']={role:f'{cid}_{role}' for role in ('fixed','adjusted','idle')}
        manifest['characters'].append(character)
        print(json.dumps({'character':cid,'quality':quality['status'],'global_factor':factor,
            'patch_after_fraction':quality.get('patch_after_fraction')}),flush=True)
        assert digest(source)==char['source_sha256']
    manifest['mobile_preview']={'frame_count':sum(c['frame_count'] for c in manifest['clips']),
        'format':'WebP quality 85 atlas tiles','original_pts_preserved':True,'playback_times_rebased':True,
        'all_source_frames_included':False,'frames_synthesized':False}
    finalize_geometry(manifest,cfg)
    write_json(assets/'manifest.json',manifest);write_json(assets/'project.snapshot.json',cfg)
    write_json(assets/'trial-report.json',manifest['characters'])
    contact_sheet(assets/'comparison.png',evidence_images,evidence_labels,max_column=380)
    write_viewer(assets,output,manifest)
    (output/'LEIA-ME.txt').write_text('Ensaio de dash — P02, P06, P08 e P11\n\nO botão Rodar os 4 mostra Idle → dash → Idle para cada personagem.\nEsquerda: tentativa de estabilização. Direita: mesma escala geral sem curva interna.\nNa P06 o registro falhou: os dois lados ficam iguais, sem aplicar uma curva incerta.\nP08 tem oscilação residual acima do limite. Aprovação dos demais vale apenas para os controles do ensaio.\nO encaixe com Idle usa marcações aproximadas e exige revisão visual. Não há aprovação anatômica integral ou de todos os personagens.\nOs PNGs usam canvas e root comuns. O jogo continua responsável pelo deslocamento.\n')
    return manifest


def write_viewer(assets,output,manifest):
    template=(ROOT/'sprite_pipeline/dash_trial.html').read_text()
    data=json.dumps(manifest,ensure_ascii=False).replace('<','\\u003c')
    page=template.replace('__MANIFEST__',data)
    (assets/'index.html').write_text(page)
    images={}
    for p in assets.rglob('*.webp'):
        images[p.relative_to(assets).as_posix()]='data:image/webp;base64,'+base64.b64encode(p.read_bytes()).decode()
    prefix='<script>const embeddedImages='+json.dumps(images)+';</script>'
    portable=page.replace('<script>',prefix+'<script>',1).replace('image.src=src','image.src=embeddedImages[src]||src')
    # Portable diagnostics are embedded, so this file makes no relative requests.
    portable=portable.replace('href="manifest.json"','href="data:application/json;base64,'+base64.b64encode((assets/'manifest.json').read_bytes()).decode()+'"')
    portable=portable.replace('href="trial-report.json"','href="data:application/json;base64,'+base64.b64encode((assets/'trial-report.json').read_bytes()).decode()+'"')
    (output/'dash-quatro-personagens.html').write_text(portable)


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--sources',type=Path,required=True)
    p.add_argument('--baseline',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    p.add_argument('--settings',type=Path,default=ROOT/'examples/collection/dash-standard-trial.json')
    a=p.parse_args();generate(a.sources,a.baseline,a.settings,a.output)
