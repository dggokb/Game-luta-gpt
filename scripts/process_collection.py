#!/usr/bin/env python3
"""Process every inventoried original, without inventing missing animations.

Two complete, timestamp-checked streaming decodes avoid duplicating raw PNGs.
Original videos, full-resolution normalized PNGs and per-frame transforms remain
available. Mobile derivatives use atlas tiles; no source frames are discarded.
"""
import argparse
import copy
import json
import math
import re
import sys
import time
from pathlib import Path
import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from sprite_pipeline.calibration import load_master, master_coherence, compare_pose, calibrate
from sprite_pipeline.corrections import frame_transforms
from sprite_pipeline.root_motion import torso_anchor_x, register_anchors
from sprite_pipeline.imaging import image_rgba, bounds, quality, affine_rgba_region as affine_rgba, save_rgba, border_rgb_color
from sprite_pipeline.extraction import digest
from sprite_pipeline.streaming import probe_video, decode_frames
from sprite_pipeline.exporting import write_json, contact_sheet
from sprite_pipeline.errors import CalibrationError
from prepare_mobile import render_mobile

BACKGROUND = {'mode': 'green', 'exclude_rectangles': [[0, 0, 64, 50]]}
MOVEMENT = {'WALK_FORWARD': 'walk_forward', 'WALK_BACK': 'walk_back',
            'DASH': 'dash', 'BACKDASH': 'back_dash'}
P01 = {'idle':'IDLE', 'walk_fward':'WALK_FORWARD', 'walk_back':'WALK_BACK',
       'dash':'DASH', 'back_dash':'BACKDASH', 'jump':'JUMP', 'L(jab)':'LIGHT_JAB',
       'crounch':'CROUCH', 'l2(jab)':'CROUCH_LIGHT', 'dano_baixo':'HIT_CROUCH',
       'M (soco médio)':'MEDIUM_KICK', 'H(chute alto)':'HEAVY_STRAIGHT',
       'm2(rasteira)':'CROUCH_MEDIUM', 'upper':'CROUCH_HEAVY', 'vitoria':'VICTORY',
       'provocacao':'TAUNT', 'derrota':'DEFEAT', 'carga':'ENERGY_CHARGE',
       'disparoCarregado':'ENERGY_CHARGED', 'potencia_2_combos_L':'LIGHT_COMBO'}
P01_NAMES = {'IDLE':'idle', 'LIGHT_JAB':'jab', 'CROUCH':'crouch',
             'CROUCH_LIGHT':'crouch_jab', 'HIT_CROUCH':'crouch_small'}


class PreviewPages:
    """Pack native-detail cropped sprites, retaining logical canvas positions."""
    def __init__(self, folder, relative, field, shape):
        self.folder, self.relative, self.field = folder, relative, field
        factor = min(320/shape[0], 512/shape[1], 1)
        self.size = tuple(max(1, round(v*factor)) for v in shape)
        self.pending, self.page = [], 0
        self.x = self.y = self.row_height = 0

    def add(self, rgba, entry):
        image = Image.fromarray(rgba)
        if self.field == 'normalized':
            crop = image.getchannel('A').getbbox()
            if crop is None:
                raise CalibrationError('Frame normalizado vazio')
            left, top, right, bottom = crop
            left, top = max(0,left-2), max(0,top-2)
            right, bottom = min(image.width,right+2), min(image.height,bottom+2)
            image = image.crop((left,top,right,bottom))
            entry['normalized_destination_rect'] = [left,top,right-left,bottom-top]
            image.thumbnail((720,720),Image.Resampling.LANCZOS)
        else:
            image = image.resize(self.size,Image.Resampling.LANCZOS)
        w,h=image.size
        if self.x+w > 2048:
            self.x=0;self.y+=self.row_height+2;self.row_height=0
        if self.y+h > 2048 or len(self.pending)>=32:
            self.flush()
        self.pending.append((image,entry,self.x,self.y))
        self.x+=w+2;self.row_height=max(self.row_height,h)

    def flush(self):
        if not self.pending:
            return
        width=max(x+image.width for image,_,x,_ in self.pending)
        height=max(y+image.height for image,_,_,y in self.pending)
        sheet=Image.new('RGBA',(width,height))
        relative=f'{self.relative}/{self.page:04d}.webp'
        dest=self.folder/relative;dest.parent.mkdir(parents=True,exist_ok=True)
        for image,entry,x,y in self.pending:
            sheet.paste(image,(x,y))
            entry[self.field+'_file']=relative
            entry[self.field+'_rect']=[x,y,image.width,image.height]
        sheet.save(dest,'WEBP',quality=85,method=0)
        self.pending.clear();self.page+=1
        self.x=self.y=self.row_height=0


def clip_spec(character, file, mapping):
    stem = Path(file['name']).stem
    state = mapping.get(stem)
    name = MOVEMENT.get(state, P01_NAMES.get(state, state.lower() if state else re.sub('[^a-zA-Z0-9_-]', '_', stem)))
    if stem.startswith('dreamina-'):
        name = 'fonte_'+stem.split('-')[4]
    if state is None and stem == 'correr':
        state, name = 'WALK_FORWARD', 'walk_forward'
    role = MOVEMENT.get(state, {'IDLE':'idle','LIGHT_JAB':'jab','CROUCH':'crouch'}.get(state))
    airborne = bool(state and ('JUMP' in state or state in ('HIT_AIR','DEFENSE_AIR','BACKDASH')))
    animation_type = 'airborne' if airborne else ('translation' if state in MOVEMENT else ('crouched' if state and 'CROUCH' in state else 'stationary'))
    spec = {'name':name, 'video':file['name'], 'state':state, 'role':role,
            'animation_type':animation_type, 'source_drive_id':file['id'],
            'note':'Nome original: '+file['name']+'. Ação ainda sem identificação confirmada.' if not state and stem.startswith(('dreamina-','gemini_generated_')) else ''}
    if state in MOVEMENT:
        spec['root_motion'] = {'mode':'in_place_x', 'anchor':'torso_band'}
        spec['note'] += ' Movimento horizontal no lugar; a posição fica a cargo do jogo.'
    if character == 'p01' and name in ('idle','jab','dash','jump','crouch','crouch_jab','walk_forward','walk_back','back_dash','crouch_small'):
        spec['reference_pose'] = 'crouch' if name == 'crouch_small' else 'idle'
    if character == 'p01' and name in ('crouch','crouch_jab'):
        spec['scale_correction'] = json.loads((ROOT/'examples/real-scale-corrections.json').read_text())[name]
    return spec


def reference_files(character, folder, files, output):
    images = [folder/f['name'] for f in files if f['mime_type'].startswith('image/')]
    idle = next((f for f in images if 'centro' in f.name or f.name in ('inicio.png','centro.png')), None)
    crouch = next((f for f in images if any(s in f.name for s in ('agacha','agaich','baixo'))), None)
    note = None
    if idle is None:
        videos = [folder/f['name'] for f in files if f['mime_type'].startswith('video/')]
        preferred = next((f for f in videos if f.stem in ('idle','parado')), videos[0] if videos else None)
        if preferred is None:
            return {}, 'Sem vídeo: somente concept disponível; nenhum movimento foi inventado.'
        meta = probe_video(preferred)
        generator = decode_frames(preferred, meta)
        original = next(generator).copy()
        generator.close()
        idle = output/'reference-from-existing-frame.png'
        cv2.imwrite(str(idle), original)
        note = {'method':'first_decoded_frame_of_existing_video', 'video':preferred.name,
                'source_sha256':meta['source_sha256'], 'frame':0,
                'note':'Derived pose reference; body size across different characters still requires visual review.'}
    return {'idle':idle, **({'crouch':crouch} if crouch else {})}, note


def process_character(character, source_root, output_root, mappings, annotations):
    cid = character['id']
    folder, output = source_root/cid, output_root/cid
    marker = output/'complete.json'
    if marker.exists():
        return json.loads(marker.read_text())
    if output.exists() and any(output.iterdir()):
        raise ValueError(f'Saída parcial existe: {output}; escolha um destino novo')
    output.mkdir(parents=True, exist_ok=True)
    files = character['files']
    originals = {f['name']:digest(folder/f['name']) for f in files
                 if f['mime_type'].startswith(('video/','image/'))}
    refs, reference_note = reference_files(cid, folder, files, output)
    if not refs:
        summary = {'character':cid, 'videos':0, 'frames':0, 'converted':0, 'rejected':[], 'note':reference_note}
        write_json(marker, summary)
        return summary
    character_background=copy.deepcopy(BACKGROUND)
    if cid in ('Dafu','Takatoshi'):
        character_background.update(edge_mode='hard',minimum_green_dominance=6)
    if cid in ('p05','p10','p12'):
        character_background.update(protect_green_art=True,green_chroma_tolerance=.12,green_art_inset=2)
    masters = {pose:load_master(path, character_background) for pose, path in refs.items()}
    for master in masters.values():
        x0,y0,x1,y1 = master['bbox']
        master['root'] = [(x0+x1)/2,y1]
    if cid == 'p01':
        mapping = P01
    elif (mappings/f'{cid}.json').exists():
        mapping = {v:k for k,v in json.loads((mappings/f'{cid}.json').read_text()).items()}
    else:
        mapping = {'idle':'IDLE','parado':'IDLE','frente':'WALK_FORWARD','tras':'WALK_BACK','walk_back':'WALK_BACK','walk_forward':'WALK_FORWARD','dash':'DASH','backDash':'BACKDASH','correr':'WALK_FORWARD'}
    annotation_doc = json.loads((annotations/f'{cid}.json').read_text()) if (annotations/f'{cid}.json').exists() else None
    segments = annotation_doc.get('segments') if isinstance(annotation_doc,dict) else annotation_doc
    if isinstance(annotation_doc,dict):
        for pose,path in refs.items():
            if annotation_doc.get('source_sha256',{}).get(pose) != digest(path):
                raise CalibrationError('Anotações de mestre pertencem a outra fonte: '+cid+'/'+pose)
    if isinstance(annotation_doc,dict):
        for pose,point in annotation_doc.get('master_roots',{}).items():
            if pose not in masters or len(point)!=2 or not np.isfinite(point).all():
                raise CalibrationError('Root mestre anotado inválido')
            h,w=masters[pose]['rgba'].shape[:2]
            if not (0<=point[0]<=w and 0<=point[1]<=h):
                raise CalibrationError('Root mestre anotado fora da imagem')
            masters[pose]['root']=point
    coherence = master_coherence(masters, segments)
    master_scales = {pose:1. for pose in masters}
    if segments and coherence['status'] == 'needs_review':
        ratios = [m['crouch_to_idle_ratio'] for m in coherence['measurements']]
        if max(ratios)/min(ratios) > 1.08:
            raise CalibrationError('Segmentos anatômicos discordam sobre a correção do mestre')
        master_scales['crouch'] = 1/float(np.median(ratios))
        coherence.update(uncorrected_status=coherence['status'], status='coherent_with_annotation_correction',
                         correction_applied=True, crouch_scale_to_output=master_scales['crouch'],
                         reason='Uniform scale from explicitly annotated equivalent head segments; never pose height.')
    manifest = {'format_version':4, 'character':cid, 'status':'needs_review', 'canvas':None, 'root':None,
                'references':{}, 'master_coherence':coherence, 'reference_provenance':reference_note,
                'review':{'visual_approval':False}, 'clips':[], 'original_sha256':originals,
                'limitations':['Vertical pose motion remains in previews; integrate engine jump physics explicitly.',
                               'Torso registration is not skeleton tracking. Source clipping and deformation remain.']}
    assets = output/'assets'
    (assets/'masters').mkdir(parents=True)
    for pose,m in masters.items():
        save_rgba(assets/'masters'/f'{pose}.png', m['rgba'])
        manifest['references'][pose] = {'file':f'masters/{pose}.png','bbox':m['bbox'],
            'root_source':m['root'],'scale_to_output':master_scales[pose],'sha256':digest(refs[pose]),'background_treatment':character_background}
    jobs, all_boxes = [], []
    ordered_files = sorted(files, key=lambda f: (mapping.get(Path(f['name']).stem) != 'IDLE', f['name'].casefold()))
    for f in ordered_files:
        if not f['mime_type'].startswith('video/'):
            continue
        spec = clip_spec(cid, f, mapping)
        source = folder/f['name']
        metadata = probe_video(source)
        info = {**spec, **metadata, 'status':'rejected', 'frame_warnings':[], 'loops':[]}
        manifest['clips'].append(info)
        background = copy.deepcopy(character_background)
        if metadata['resolution'][0] < 64 or metadata['resolution'][1] < 50:
            background = {'mode':'green'}
        if cid == 'Dafu' and source.stem in ('aprovar','rir','surpreso'):
            background={'mode':'border_color','tolerance':60 if source.stem=='rir' else 40,'exclude_rectangles':[[0,0,64,50]]}
        info['background_treatment'] = background
        scans = {pose:[] for pose in masters}
        anchors = []
        before = PreviewPages(assets, f'images/{spec["name"]}/before', 'raw', metadata['resolution'])
        for index, raw in enumerate(decode_frames(source, metadata)):
            entry = metadata['frames'][index]
            before.add(cv2.cvtColor(raw, cv2.COLOR_BGRA2RGBA), entry)
            if background.get('mode')=='border_color' and 'color' not in background:
                background['color']=border_rgb_color(cv2.cvtColor(raw,cv2.COLOR_BGRA2RGB))
            if background.get('protect_green_art') and 'green_reference_rgb' not in background:
                background['green_reference_rgb']=border_rgb_color(cv2.cvtColor(raw,cv2.COLOR_BGRA2RGB))
            rgba = image_rgba(raw, background)
            try:
                bbox = bounds(rgba)
                warnings = quality(rgba,bbox)
                anchor = torso_anchor_x(rgba) if spec.get('root_motion') else None
            except CalibrationError as error:
                bbox, warnings, anchor = None, [str(error)], None
            entry.update(bbox_source=bbox, warnings=warnings)
            if warnings:
                info['frame_warnings'].append({'index':index,'warnings':warnings})
            anchors.append(anchor)
            for pose, master in masters.items():
                metrics = compare_pose(rgba,bbox,master) if bbox else {'score':0}
                scans[pose].append({'bbox':bbox,'warnings':warnings,**metrics})
        before.flush()
        candidates, failures = [], []
        for pose, scan in scans.items():
            if spec.get('reference_pose', pose) != pose:
                continue
            try:
                if any(frame['bbox'] is None for frame in scan):
                    raise CalibrationError('Frame sem silhueta mensurável')
                if pose == 'crouch' and coherence['status'] not in ('coherent_within_annotation_tolerance','coherent_with_annotation_correction'):
                    raise CalibrationError('Referência agachada precisa de conferência anatômica')
                height = (masters[pose]['bbox'][3]-masters[pose]['bbox'][1])*master_scales[pose]
                cal = calibrate(scan, masters[pose], height, spec,
                                allow_scale_drift=spec.get('scale_correction') is not None)
                candidates.append((cal['reference_similarity'],pose,cal))
            except CalibrationError as error:
                failures.append(pose+': '+str(error))
        if not candidates:
            info['reference_pose'] = 'idle'
            info['rejection_reason'] = '; '.join(failures)
            print(json.dumps({'character':cid,'clip':spec['name'],'rejected':info['rejection_reason']},ensure_ascii=False),flush=True)
            continue
        _, pose, cal = max(candidates, key=lambda v:v[0])
        info.update(cal, reference_pose=pose)
        try:
            transforms, correction = frame_transforms(spec, metadata, cal)
            if spec.get('root_motion'):
                transforms, motion = register_anchors(anchors,transforms,cal['reference_frame'],metadata['source_sha256'])
                info['root_motion'] = motion
            if correction:
                info['scale_correction'] = correction
            else:
                info.pop('scale_correction',None)
        except CalibrationError as error:
            info['rejection_reason'] = str(error)
            continue
        info['scale_mode'] = 'annotated_segment_curve' if correction else 'constant_per_video'
        info['status'] = 'requires_visual_review'
        boxes = [[b[0]*m[0][0]+m[0][2],b[1]*m[1][1]+m[1][2],
                  b[2]*m[0][0]+m[0][2],b[3]*m[1][1]+m[1][2]]
                 for b,m in zip((e['bbox_source'] for e in metadata['frames']), transforms)]
        all_boxes.extend(boxes)
        jobs.append((source,info,transforms,boxes))
        print(json.dumps({'character':cid,'scanned':spec['name'],'reference':pose,'score':cal['reference_similarity']},ensure_ascii=False),flush=True)
    for pose,m in masters.items():
        x0,y0,x1,y1 = m['bbox'];rx,ry = m['root']
        all_boxes.append([(x0-rx)*master_scales[pose],(y0-ry)*master_scales[pose],(x1-rx)*master_scales[pose],(y1-ry)*master_scales[pose]])
    min_x,min_y = math.floor(min(b[0] for b in all_boxes)), math.floor(min(b[1] for b in all_boxes))
    max_x,max_y = math.ceil(max(b[2] for b in all_boxes)), math.ceil(max(b[3] for b in all_boxes))
    root, canvas = [24-min_x,24-min_y], [max_x-min_x+51,max_y-min_y+51]
    if canvas[0]*canvas[1] > 25000000:
        raise CalibrationError('Canvas excessivo; nenhuma escala automática para caber')
    manifest.update(root=root,canvas=canvas)
    for pose,m in masters.items():
        save_rgba(assets/'masters'/f'{pose}-aligned.png',affine_rgba(m['rgba'],master_scales[pose],np.subtract(root,np.asarray(m['root'])*master_scales[pose]),canvas))
        manifest['references'][pose]['aligned_file'] = f'masters/{pose}-aligned.png'
    evidence, labels = [], []
    for source,info,transforms,boxes in jobs:
        info.update(root=root,canvas=canvas)
        after = PreviewPages(assets,f'images/{info["name"]}/after','normalized',canvas)
        normalized = output/'normalized'/info['name']
        normalized.mkdir(parents=True)
        ref = boxes[info['reference_frame']]
        for index,raw in enumerate(decode_frames(source,info)):
            entry,m = info['frames'][index],copy.deepcopy(transforms[index])
            m[0][2] += root[0];m[1][2] += root[1]
            rgba = image_rgba(raw,info['background_treatment'])
            result = affine_rgba(rgba,m[0][0],[m[0][2],m[1][2]],canvas)
            png = normalized/f'{index:06d}.png'
            if not cv2.imwrite(str(png),cv2.cvtColor(result,cv2.COLOR_RGBA2BGRA),[cv2.IMWRITE_PNG_COMPRESSION,1]):
                raise OSError(png)
            box = bounds(result)
            entry.update(normalized_png=f'normalized/{info["name"]}/{index:06d}.png',
                transform_source_to_output=m,scale_applied=m[0][0],bbox_output=box,
                offset_from_root=[box[0]-root[0],box[1]-root[1]],
                visual_displacement_from_reference=[(boxes[index][0]+boxes[index][2]-ref[0]-ref[2])/2,boxes[index][3]-ref[3]])
            if 'root_motion' in info:
                entry['removed_root_motion_output_px'] = [info['root_motion']['removed_x_output_px'][index],0]
            after.add(result,entry)
            if index == info['reference_frame']:
                evidence.append(result);labels.append(info['name'])
        after.flush()
        write_json(output/f'{info["name"]}-metadata.json',info)
        print(json.dumps({'character':cid,'converted':info['name'],'frames':info['frame_count']},ensure_ascii=False),flush=True)
    if originals != {name:digest(folder/name) for name in originals}:
        raise CalibrationError('Fonte original foi alterada')
    manifest['originals_unchanged'] = True
    manifest['mobile_preview'] = {'frame_count':sum(c['frame_count'] for c in manifest['clips']),
        'maximum_image_width':720,'raw_maximum_image_width':320,'layout':'cropped_tiles_v1','format':'WebP quality 85 atlas tiles','all_frames_retained':True,
        'timestamps_changed':False,'full_resolution_pngs_included':False}
    write_json(assets/'manifest.json',manifest)
    write_json(assets/'calibration-report.json',{k:manifest[k] for k in ('character','status','master_coherence','reference_provenance')})
    write_json(assets/'project.snapshot.json',{'inventory':character,'mappings':mapping,'segments':segments})
    if evidence:
        contact_sheet(assets/'calibration-comparison.png',evidence,labels,max_column=260)
    render_mobile(manifest, assets)
    summary = {'character':cid,'videos':len(manifest['clips']),
        'frames':sum(c['frame_count'] for c in manifest['clips']),
        'converted':len(jobs),'rejected':[{'clip':c['name'],'reason':c['rejection_reason']} for c in manifest['clips'] if c['status']=='rejected'],
        'in_place_clips':[c['name'] for c in manifest['clips'] if c['status']!='rejected' and c.get('root_motion')],
        'source_warnings':sum(bool(c['frame_warnings']) for c in manifest['clips']),
        'originals_unchanged':True,'reference_provenance':reference_note}
    write_json(marker,summary)
    return summary


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--inventory',type=Path,required=True)
    parser.add_argument('--inputs',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--mappings',type=Path,required=True)
    parser.add_argument('--annotations',type=Path,required=True)
    parser.add_argument('--characters',nargs='*')
    args=parser.parse_args()
    inventory=json.loads(args.inventory.read_text())
    for character in inventory:
        if args.characters and character['id'] not in args.characters:
            continue
        summary=process_character(character,args.inputs,args.output,args.mappings,args.annotations)
        print(json.dumps({'complete':summary},ensure_ascii=False),flush=True)


if __name__=='__main__':
    main()
