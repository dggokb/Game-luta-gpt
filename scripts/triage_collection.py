#!/usr/bin/env python3
"""Preserve consistent assets and export only confirmed position/matte changes."""
import argparse
import copy
import csv
import json
from pathlib import Path
import shutil
import sys
import cv2
import numpy as np
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from sprite_pipeline.triage import audit_metadata,translate_registered_x,required_horizontal_padding
from sprite_pipeline.action_cycle import extract_cycle,register_patch
from sprite_pipeline.streaming import decode_frames
from sprite_pipeline.imaging import image_rgba,affine_rgba_region,bounds
from sprite_pipeline.exporting import write_json
from sprite_pipeline.extraction import digest
from process_collection import PreviewPages


def select_root_metadata(clip, start, stop):
    """Keep inherited root arrays aligned with a selected source interval."""
    motion=clip.get('root_motion',{})
    for key in ('anchor_x_source_px','removed_x_output_px'):
        if isinstance(motion.get(key),list):motion[key]=motion[key][start:stop]
    reference=motion.get('reference_frame')
    if reference is not None:
        motion['reference_source_frame']=reference
        motion['reference_frame']=reference-start if start<=reference<stop else None
    removed=motion.get('removed_x_output_px')
    if removed:motion['first_to_last_removed_x_px']=removed[-1]-removed[0]


def update_geometry(clip,root,canvas,reference):
    clip.update(root=root,canvas=canvas,reference_frame=reference,loops=[])
    first=clip['frames'][0]['bbox_output']
    for f in clip['frames']:
        b=f['bbox_output'];f['offset_from_root']=[b[0]-root[0],b[1]-root[1]]
        f['visual_displacement_from_reference']=[(b[0]+b[2]-first[0]-first[2])/2,b[3]-first[3]]
    clip['calibration_transform_source_to_output']=clip['frames'][reference]['transform_source_to_output']


def generate(sources,baseline,approved,settings,output):
    if output.exists() and any(output.iterdir()):raise ValueError('Use saída nova ou vazia')
    config=json.loads(settings.read_text());assets=output/'assets';assets.mkdir(parents=True)
    audit=audit_metadata(baseline);assert audit['metadata_error_count']==0
    manifest={'status':'selective_triage_review','clips':[],'cases':[],
        'configuration_sha256':digest(settings),'structural_triage':{k:v for k,v in audit.items() if k!='rows'},
        'approved_visual_reference':config['approved_visual_reference'],'full_frame_anatomical_validation':False}
    for cfg in config['cases']:
        cid=cfg['character'];folder=baseline/cid;d=json.loads((folder/'assets/manifest.json').read_text())
        source_clip=next(c for c in d['clips'] if c['name']==cfg['clip']);source=sources/cid/source_clip['source_name']
        assert digest(source)==cfg['source_sha256']==source_clip['source_sha256']
        start,stop=cfg.get('start',0),cfg.get('stop',source_clip['frame_count'])
        selected=extract_cycle(source_clip,start,stop)
        select_root_metadata(selected,start,stop)
        raw=[im for i,im in enumerate(decode_frames(source,source_clip)) if start<=i<stop]
        case={'id':cfg['id'],'label':cfg['label'],'character':cid,'movement':cfg['clip'],
            'decision':cfg['decision'],'source_sha256':digest(source),'source_drive_id':source_clip.get('source_drive_id'),
            'selection':selected['source_selection'],'baseline_status':source_clip['status'],
            'scale_changed':False,'vertical_translation_changed':False,
            'matte_changed':bool(cfg.get('matte_enclosed_only')),'root_changed':bool(cfg.get('root_correction')),
            'clips':{},'original_root':d['root'],'original_canvas':d['canvas']}
        if cfg['decision']=='pending':
            case.update(render_kind='raw',root=[640,680],canvas=source_clip['resolution'],reason=source_clip.get('rejection_reason'))
            clip=copy.deepcopy(selected);clip.update(name=cfg['id']+'_source',case=cfg['id'])
            pages=PreviewPages(assets,f'images/{cfg["id"]}/source','raw',source_clip['resolution'])
            for im,f in zip(raw,clip['frames']):pages.add(cv2.cvtColor(im,cv2.COLOR_BGRA2RGBA),f)
            pages.flush();case['clips']={'before':clip['name'],'after':clip['name']};manifest['clips'].append(clip)
            manifest['cases'].append(case);print(cfg['id'],'pending',flush=True);continue
        reference_source=cfg.get('reference_source_frame',source_clip['reference_frame'])
        reference=reference_source-start
        if not 0<=reference<len(raw):reference=0
        anchors=None;deltas=[0.]*len(raw);matrices=[copy.deepcopy(f['transform_source_to_output']) for f in selected['frames']]
        if cfg.get('root_correction'):
            if cfg['root_correction']=='registered_patch_x':
                fits=register_patch(raw,reference,cfg['polygon'],minimum_correlation=config['minimum_patch_correlation'],search_radius_px=config['search_radius_px'])
                anchors=[f['patch_center_source_px'][0] for f in fits];case['tracking']=fits
            else:
                anchors=cfg['source_eye_x'];assert len(anchors)==len(raw);case['annotation_note']=cfg['annotation_note']
            scale=selected['frames'][reference]['scale_applied']
            target=d['root'][0]+(anchors[reference]-cfg['source_hip_x'])*scale
            old_x=[x*f['scale_applied']+f['transform_source_to_output'][0][2] for x,f in zip(anchors,selected['frames'])]
            matrices,deltas=translate_registered_x(selected['frames'],anchors,target)
            case['before_anchor_spread_px']=float(np.ptp(old_x));case['target_x_original_canvas']=target
        left,right=required_horizontal_padding(selected['frames'],deltas,d['canvas'][0])
        canvas=[d['canvas'][0]+left+right,d['canvas'][1]];root=[d['root'][0]+left,d['root'][1]]
        case.update(render_kind='normalized',canvas=canvas,root=root,padding=[left,right])
        after=copy.deepcopy(selected);before=copy.deepcopy(selected)
        before.update(name=cfg['id']+'_before',case=cfg['id']);after.update(name=cfg['id']+'_after',case=cfg['id'])
        pages_before=PreviewPages(assets,f'images/{cfg["id"]}/before','normalized',canvas)
        pages_after=PreviewPages(assets,f'images/{cfg["id"]}/after','normalized',canvas) if cfg['decision']=='corrected' else None
        pngdir=output/'normalized'/cfg['id']
        if pages_after:pngdir.mkdir(parents=True)
        options=copy.deepcopy(source_clip['background_treatment'])
        if cfg.get('matte_enclosed_only'):options['green_art_enclosed_only']=True
        changed_alpha=0
        for i,(im,old_frame,bf,af,m) in enumerate(zip(raw,selected['frames'],before['frames'],after['frames'],matrices)):
            old_png=folder/old_frame['normalized_png'];old_rgba=cv2.cvtColor(cv2.imread(str(old_png),-1),cv2.COLOR_BGRA2RGBA)
            padded=np.zeros((canvas[1],canvas[0],4),np.uint8);padded[:,left:left+old_rgba.shape[1]]=old_rgba
            bf.pop('normalized_png',None);bf.update(baseline_png=old_frame['normalized_png'],baseline_png_sha256=digest(old_png),baseline_padding_left=left,
                bbox_output=bounds(padded))
            bf['transform_source_to_output'][0][2]+=left
            for key in ('raw_file','raw_rect'):bf.pop(key,None);af.pop(key,None)
            pages_before.add(padded,bf)
            if pages_after:
                source_old=image_rgba(im,source_clip['background_treatment']);rgba=image_rgba(im,options)
                assert np.array_equal(source_old[:,:,:3],rgba[:,:,:3])
                assert not np.any(rgba[:,:,3]>source_old[:,:,3])
                changed=int(np.sum(rgba[:,:,3]!=source_old[:,:,3]));changed_alpha+=changed;af['changed_source_alpha_pixels']=changed
                m=copy.deepcopy(m);m[0][2]+=left
                result=affine_rgba_region(rgba,m[0][0],[m[0][2],m[1][2]],canvas)
                path=pngdir/f'{i:06d}.png';assert cv2.imwrite(str(path),cv2.cvtColor(result,cv2.COLOR_RGBA2BGRA))
                af.update(normalized_png=path.relative_to(output).as_posix(),transform_source_to_output=m,bbox_source=bounds(rgba),bbox_output=bounds(result),
                          scale_applied=m[0][0],baseline_png=old_frame['normalized_png'],baseline_png_sha256=bf['baseline_png_sha256'])
                if anchors:
                    af['removed_root_motion_output_px']=[(anchors[i]-anchors[reference])*m[0][0],0]
                pages_after.add(result,af)
            else:
                af.clear();af.update(copy.deepcopy(bf))
        pages_before.flush()
        if pages_after:pages_after.flush()
        if cfg['decision']=='preserved':
            # Sharing the before atlas preserves the exact displayed reference.
            after['frames']=copy.deepcopy(before['frames'])
        after['background_treatment']=options
        if anchors:
            after.update(source_root=[cfg['source_hip_x'],source_clip['source_root'][1]],root_method='annotated_hip_with_registered_x')
            after['root_motion']={'mode':'in_place_x','anchor':cfg['root_correction'],'source_sha256':digest(source),
                'anchor_x_source_px':anchors,'removed_x_output_px':[f['removed_root_motion_output_px'][0] for f in after['frames']],
                'reference_frame':reference,'game_applies_displacement':True,'vertical_motion':'preserved'}
            xs=[x*f['scale_applied']+f['transform_source_to_output'][0][2] for x,f in zip(anchors,after['frames'])]
            case['after_anchor_spread_px']=float(np.ptp(xs));assert np.ptp(xs)<1e-7
        for c in (before,after):update_geometry(c,root,canvas,reference)
        case.update(changed_source_alpha_pixels=changed_alpha,clips={'before':before['name'],'after':after['name']})
        manifest['clips'].extend([before,after]);manifest['cases'].append(case)
        assert digest(source)==cfg['source_sha256'];print(cfg['id'],cfg['decision'],case.get('before_anchor_spread_px'),changed_alpha,flush=True)
    # Keep the user's four accepted previews byte-for-byte, outside this triage.
    assert digest(approved/'index.html')
    shutil.copytree(approved,assets/'approved')
    approved_manifest=json.loads((approved/'manifest.json').read_text())
    manifest['approved_assets_sha256']={p.relative_to(approved).as_posix():digest(p) for p in approved.rglob('*') if p.is_file()}
    for old in approved_manifest['clips']:
        c=copy.deepcopy(old);c['name']='approved/'+c['name']
        for f in c['frames']:
            for key in ('raw_file','normalized_file'):
                if key in f:f[key]='approved/'+f[key]
        manifest['clips'].append(c)
    for cid,sha in audit['baseline_manifest_sha256'].items():assert digest(baseline/cid/'assets/manifest.json')==sha
    manifest['mobile_preview']={'frame_count':sum(c['frame_count'] for c in manifest['clips']),
        'format':'WebP quality 85 atlas tiles','original_pts_preserved':True,'playback_times_rebased':True,
        'new_normalized_pngs':len(list((output/'normalized').rglob('*.png')))}
    write_json(assets/'manifest.json',manifest);write_json(assets/'triage-report.json',audit)
    write_json(assets/'project.snapshot.json',config)
    page=(ROOT/'sprite_pipeline/triage_viewer.html').read_text().replace('__MANIFEST__',json.dumps(manifest,ensure_ascii=False).replace('<','\\u003c'))
    (assets/'index.html').write_text(page)
    write_json(output/'triage-report.json',{'cases':manifest['cases'],'structural':audit})
    with (output/'triage.csv').open('w',newline='',encoding='utf-8-sig') as f:
        writer=csv.DictWriter(f,fieldnames=['character','clip','status','frames','metadata_errors','anatomical_validation']);writer.writeheader()
        for row in audit['rows']:writer.writerow({k:row[k] for k in writer.fieldnames})
    (output/'LEIA-ME.txt').write_text('Triagem seletiva de sprites\n\nSomente os PNGs novos em normalized/ são candidatos de substituição. Os casos de dash corrigidos são variantes de ciclo, com intervalo de fonte no manifesto. Não substituem silenciosamente a animação completa.\nOs casos mantidos referenciam os PNGs da coleção anterior por caminho e hash. Os casos pendentes só mostram a fonte; não há PNG normalizado novo para eles.\nA escala e o eixo Y das matrizes permanecem iguais aos da saída anterior. Só mudam a translação X anotada e/ou o recorte verde exterior nos casos declarados.\nA prévia dos quatro personagens aprovados acompanha o APK sem alteração. O APK completo da coleção e os originais não foram modificados.\n')
    return manifest


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--sources',type=Path,required=True);p.add_argument('--baseline',type=Path,required=True)
    p.add_argument('--approved',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    p.add_argument('--settings',type=Path,default=ROOT/'examples/collection/selective-triage.json')
    a=p.parse_args();generate(a.sources,a.baseline,a.approved,a.settings,a.output)
