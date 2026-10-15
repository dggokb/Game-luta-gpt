#!/usr/bin/env python3
"""Build a separately inspectable dash-only P02 cycle, never an Idle-driven curve."""
import argparse
import copy
import json
from pathlib import Path
import shutil
import sys
import cv2

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from sprite_pipeline.action_cycle import extract_cycle,register_patch,cycle_transforms
from sprite_pipeline.extraction import digest
from sprite_pipeline.exporting import write_json,contact_sheet
from sprite_pipeline.imaging import image_rgba,affine_rgba_region,bounds
from sprite_pipeline.streaming import decode_frames
from process_collection import PreviewPages
from review_p02_dash import write_review_viewer


def review(source,baseline,previous,setting_file,output):
    if output.exists() and any(output.iterdir()):raise ValueError('Use pasta nova ou vazia')
    cfg=json.loads(setting_file.read_text())
    data=json.loads((baseline/'assets/manifest.json').read_text())
    original=next(c for c in data['clips'] if c['name']=='dash')
    prior=json.loads((previous/'assets/manifest.json').read_text())['clips'][1]
    if not digest(source)==cfg['source_sha256']==original['source_sha256']==prior['source_sha256']:
        raise ValueError('Vídeo diferente das anotações ou da comparação')
    start,stop=cfg['start_inclusive'],cfg['stop_exclusive']
    cycle=extract_cycle(original,start,stop)
    raw=[im for i,im in enumerate(decode_frames(source,original)) if start<=i<stop]
    reference=cfg['reference_source_frame']-start
    fits=register_patch(raw,reference,cfg['face_polygon_source'],minimum_correlation=cfg['minimum_correlation'])
    transforms,report=cycle_transforms(fits,original['scale_per_video'],cfg['global_scale_factor'],original['source_root'][1])
    report.update(source_sha256=digest(source),reference_source_frame=cfg['reference_source_frame'],
                  global_scale_note=cfg['global_scale_note'],configuration_sha256=digest(setting_file))
    before=extract_cycle(prior,start,stop)
    before.update(name='dash_teste_anterior',role=None,scale_audit={'status':'not_checked'},
                  loops=[],note='Recorte do teste anterior, com sua curva antiga preservada.')
    for key in ('anchor_x_source_px','removed_x_output_px'):
        if key in before.get('root_motion',{}):before['root_motion'][key]=before['root_motion'][key][start:stop]
    before['root_motion'].update(reference_frame=None,reference_source_frame=prior['reference_frame'])
    after=copy.deepcopy(cycle)
    after.update(name='dash_ciclo_ajustado',role=None,status='requires_visual_review',
        reference_frame=reference,loops=[],global_calibration_reference_source_frame=original['reference_frame'],
        scale_per_video=original['scale_per_video']*cfg['global_scale_factor'],
        scale_mode='action_patch_similarity_registration',scale_correction=report,
        scale_audit={'status':'not_checked','full_frame_anatomical_validation':False},
        note='Somente corrida. Tamanho estabilizado no próprio dash; escala geral única para comparar com outros movimentos.')
    motion={'mode':'in_place_x','anchor':'registered_face_patch_center',
        'anchor_x_source_px':[f['patch_center_source_px'][0] for f in fits],
        'source_sha256':digest(source),'reference_frame':reference,
        'game_applies_displacement':True,'vertical_motion':'preserved',
        'note':'Horizontal patch anchor excludes particles; no vertical pose flattening.'}
    after['root_motion']=motion
    idle=copy.deepcopy(next(c for c in data['clips'] if c['name']=='idle'))
    idle.update(name='idle_escala',role=None,frames=idle['frames'][:1],frame_count=1,loops=[],reference_frame=0,
                note='Pose parada para comparar a escala geral. Não determina a curva interna do dash.')
    idle['duration_s']=idle['frames'][0]['duration_s']
    assets=output/'assets';assets.mkdir(parents=True)
    shutil.copytree(baseline/'assets/masters',assets/'masters')
    for clip,folder in [(before,previous),(idle,baseline)]:
        dest=output/'normalized'/clip['name'];dest.mkdir(parents=True)
        for i,frame in enumerate(clip['frames']):
            for key in ('raw_file','normalized_file'):
                path=assets/frame[key];path.parent.mkdir(parents=True,exist_ok=True)
                shutil.copy2(folder/'assets'/frame[key],path)
            shutil.copy2(folder/frame['normalized_png'],dest/f'{i:06d}.png')
            frame['normalized_png']=f'normalized/{clip["name"]}/{i:06d}.png'
    dest=output/'normalized/dash_ciclo_ajustado';dest.mkdir(parents=True)
    pages=PreviewPages(assets,'images/dash-cycle/after','normalized',data['canvas'])
    for i,(im,matrix,frame) in enumerate(zip(raw,transforms,after['frames'])):
        m=copy.deepcopy(matrix);m[0][2]+=data['root'][0];m[1][2]+=data['root'][1]
        result=affine_rgba_region(image_rgba(im,original['background_treatment']),m[0][0],
                                 [m[0][2],m[1][2]],data['canvas'])
        assert cv2.imwrite(str(dest/f'{i:06d}.png'),cv2.cvtColor(result,cv2.COLOR_RGBA2BGRA))
        box=bounds(result)
        frame.update(normalized_png=f'normalized/dash_ciclo_ajustado/{i:06d}.png',
            transform_source_to_output=m,scale_applied=m[0][0],bbox_output=box,
            offset_from_root=[box[0]-data['root'][0],box[1]-data['root'][1]],
            removed_root_motion_output_px=[(fits[i]['patch_center_source_px'][0]-fits[reference]['patch_center_source_px'][0])*m[0][0],0],
            visual_displacement_from_reference=[0,0])
        pages.add(result,frame)
    pages.flush()
    reference_box=after['frames'][0]['bbox_output']
    for frame in after['frames']:
        b=frame['bbox_output']
        frame['visual_displacement_from_reference']=[(b[0]+b[2]-reference_box[0]-reference_box[2])/2,b[3]-reference_box[3]]
    motion['removed_x_output_px']=[f['removed_root_motion_output_px'][0] for f in after['frames']]
    motion['first_to_last_removed_x_px']=motion['removed_x_output_px'][-1]-motion['removed_x_output_px'][0]
    after['calibration_transform_source_to_output']=after['frames'][reference]['transform_source_to_output']
    # Independent shoulder controls: same arm phase, never used in face registration.
    controls={16:[[699,337],[776,362]],28:[[704,335],[780,358]]}
    shoulder_lengths=[]
    for source_index,points in controls.items():
        a,b=points;length=((a[0]-b[0])**2+(a[1]-b[1])**2)**.5
        shoulder_lengths.append({'source_frame_index':source_index,'source_length_px':length,
            'output_length_px':length*after['frames'][source_index-start]['scale_applied']})
    report['independent_shoulder_controls']=shoulder_lengths
    data.update(clips=[before,after,idle],status='action_only_experimental_review',
        inspection_start_frame=12,inspection_width=640,inspection_zoom=1.1,
        inspection_camera_center=[data['root'][0]-110,data['root'][1]-195],
        inspection_note='Só corrida (24 frames). Esquerda: novo ajuste. Direita: teste anterior. Selecione idle_escala para comparar a pose parada, no mesmo zoom.',
        mobile_preview={'frame_count':49,'maximum_image_width':720,'layout':'cropped_tiles_v1',
            'format':'WebP quality 85 atlas tiles','all_frames_retained':False,
            'selection':cycle['source_selection'],'source_pts_preserved':True,'playback_times_rebased':True})
    write_json(assets/'manifest.json',data)
    write_json(assets/'project.snapshot.json',cfg)
    write_json(assets/'calibration-report.json',report)
    comparison=[];labels=[]
    for i in (0,6,12):
        for clip,label in ((before,'Teste anterior'),(after,'Novo ciclo')):
            im=cv2.cvtColor(cv2.imread(str(output/clip['frames'][i]['normalized_png']),-1),cv2.COLOR_BGRA2RGBA)
            comparison.append(im[210:730,357:997]);labels.append(f'{label} — fonte {start+i}')
    contact_sheet(assets/'calibration-comparison.png',comparison,labels,max_column=480)
    write_json(output/'source-verification.json',{'source_sha256':digest(source),
        'originals_unchanged':True,'selection':cycle['source_selection'],
        'selected_source_indices':[f['source_frame_index'] for f in after['frames']],
        'source_pts_equal':[f['pts'] for f in after['frames']]==[f['pts'] for f in original['frames'][start:stop]],
        'full_frame_anatomical_validation':False})
    write_review_viewer(data,assets,output)
    (output/'LEIA-ME.txt').write_text('P02 — ciclo de dash, revisão 2\n\n'+cfg['selection_note']+'\n\n'+cfg['global_scale_note']+'\n\nEsquerda: novo ajuste; direita: teste anterior. O seletor inclui Idle para comparar escala.\nO APK completo e os originais permanecem sem alteração. Revisão visual ainda necessária.\n')
    print(json.dumps({'frames':len(raw),'minimum_correlation':min(f['correlation'] for f in fits),
        'local_correction_range':[min(report['local_correction_factors']),max(report['local_correction_factors'])],
        'shoulder_controls':shoulder_lengths,'output':str(output)},indent=2))


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True)
    p.add_argument('--baseline',type=Path,required=True);p.add_argument('--previous',type=Path,required=True)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--settings',type=Path,default=ROOT/'examples/collection/motion-landmarks/p02-dash-cycle.json')
    a=p.parse_args();review(a.source,a.baseline,a.previous,a.settings,a.output)
