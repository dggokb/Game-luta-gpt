"""Traceable reference calibration and optional annotated scale-drift correction."""
import json
import math
import re
from pathlib import Path
import cv2
import numpy as np
from .errors import CalibrationError
from .extraction import extract_video, digest
from .imaging import image_rgba, bounds, affine_rgba, save_rgba
from .calibration import load_master, master_coherence, scan, calibrate
from .corrections import frame_transforms
from .root_motion import register_in_place
from .loops import loop_candidates
from .motion import motion_summary
from .exporting import write_json, export_sheets, contact_sheet
from .viewer import write_viewer


def process(config_path, output):
    config_path, output = Path(config_path).resolve(), Path(output).resolve()
    cfg = json.loads(config_path.read_text(encoding='utf-8'))
    base = config_path.parent
    references = cfg.get('references', {})
    if 'idle' not in references:
        raise CalibrationError('É necessária uma imagem mestre idle')
    if set(references)-{'idle', 'crouch'}:
        raise CalibrationError('Mestres suportados: idle e crouch')
    clips = cfg.get('clips') or []
    if not clips:
        raise CalibrationError('Nenhum vídeo configurado')
    names = [c['name'] for c in clips]
    if len(set(names)) != len(names) or 'masters' in names or any(not re.fullmatch(r'[a-zA-Z0-9_-]+', n) for n in names):
        raise CalibrationError('Nomes de clips devem ser únicos, com letras, números, _ ou -; masters é reservado')
    inputs = [base/p for p in references.values()] + [base/c['video'] for c in clips]
    if any(p.resolve().is_relative_to(output) for p in inputs) or config_path.is_relative_to(output):
        raise CalibrationError('Saída não pode conter fontes nem configuração')
    if output.exists() and any(output.iterdir()):
        raise CalibrationError('Escolha uma pasta de saída nova ou vazia; nenhum arquivo existente será sobrescrito')
    threshold = float(cfg.get('minimum_similarity', .82))
    padding = int(cfg.get('padding', 24))
    atlas_size = int(cfg.get('atlas_max_size', 4096))
    tolerance = float(cfg.get('master_scale_tolerance', .08))
    if not .5 <= threshold <= 1 or padding < 3 or not 256 <= atlas_size <= 4096 or not 0 < tolerance <= .15:
        raise CalibrationError('Limite, margem (>=3), tamanho do atlas ou tolerância inválidos')
    masters = {key: load_master(base/path, cfg.get('master_background', {}).get(key)) for key, path in references.items()}
    for pose, m in masters.items():
        x0, _, x1, y1 = m['bbox']
        root = cfg.get('master_roots', {}).get(pose, [(x0+x1)/2, y1])
        if len(root) != 2 or not np.isfinite(root).all() or not (0 <= root[0] <= m['rgba'].shape[1] and 0 <= root[1] <= m['rgba'].shape[0]):
            raise CalibrationError('Root mestre inválido')
        m['root'] = root
    if any(m['warnings'] for m in masters.values()):
        raise CalibrationError('Mestre com fundo residual ou silhueta cortada; revise antes da calibração')
    idle_h = masters['idle']['bbox'][3]-masters['idle']['bbox'][1]
    target_idle = float(cfg.get('idle_target_height', idle_h))
    if not math.isfinite(target_idle) or target_idle <= 0:
        raise CalibrationError('Altura alvo inválida')
    if 'target_heights' in cfg:
        raise CalibrationError('target_heights foi removido: alturas independentes não comprovam escala anatômica dos mestres')
    master_scale = target_idle/idle_h
    coherence = master_coherence(masters, cfg.get('anatomical_segments'), tolerance)
    output.mkdir(parents=True, exist_ok=True)
    (output/'masters').mkdir()
    reference_meta = {}
    for pose, master in masters.items():
        path = base/references[pose]
        save_rgba(output/'masters'/f'{pose}.png', master['rgba'])
        reference_meta[pose] = {'file': f'masters/{pose}.png', 'source_name': path.name,
                                'sha256': digest(path), 'bbox': master['bbox'], 'root_source': master['root'], 'scale_to_output': master_scale}
    for pose, master in masters.items():
        annotated = master['rgba'].copy()
        for segment in cfg.get('anatomical_segments', []):
            a, b = [tuple(round(v) for v in point) for point in segment[pose]]
            cv2.line(annotated, a, b, (255, 195, 70, 255), 2, cv2.LINE_AA)
            for pt in (a, b):
                cv2.circle(annotated, pt, 4, (80, 230, 220, 255), -1)
        save_rgba(output/'masters'/f'{pose}-landmarks.png', annotated)
        reference_meta[pose]['landmarks_file'] = f'masters/{pose}-landmarks.png'
    manifest = {'format_version': 3, 'status': 'requires_visual_review', 'canvas': None, 'root': None,
                'coordinate_system': {'x': 'right', 'y': 'down', 'unit': 'output_pixel',
                                      'motion': 'each frame records its matrix; explicit scale curves and in_place_x registration may vary the transform; root_motion records removed translation for the engine, never automatically replay it'},
                'references': reference_meta, 'master_coherence': coherence, 'clips': [],
                'limitations': ['Matching is measured, not proof of anatomical identity.',
                               'Support anchors are silhouette measurements, not tracked skeleton roots.',
                               'Source deformation, camera motion and clipping are not corrected.',
                               'Green key may remove green artwork; use existing alpha for such characters.']}
    jobs, all_boxes = [], []
    for clip in clips:
        name = clip['name']
        dest = output/name
        dest.mkdir()
        info = {'name': name, 'status': 'rejected', 'animation_type': clip.get('animation_type', 'stationary'),
                'role': clip.get('role'), 'note': clip.get('note', ''),
                'reference_pose': clip.get('reference_pose', 'idle'), 'frames': []}
        manifest['clips'].append(info)
        if info['animation_type'] not in ('stationary', 'translation', 'airborne', 'crouched'):
            raise CalibrationError('animation_type inválido')
        pose = info['reference_pose']
        if pose not in masters:
            raise CalibrationError(f'Mestre ausente: {pose}')
        metadata = extract_video(base/clip['video'], dest/'raw')
        info.update({k: v for k, v in metadata.items() if k != 'frames'})
        info['frames'] = [{**f, 'raw_file': f"{name}/raw/{f['file']}"} for f in metadata['frames']]
        background = clip.get('background', cfg.get('background', {}))
        info['background_treatment'] = background or {'mode': 'green'}
        frames = scan(dest/'raw', masters[pose], background)
        info['frame_warnings'] = [{'index': i, 'warnings': f['warnings']} for i, f in enumerate(frames) if f['warnings']]
        info['pose_candidates'] = [{'index': i, 'score': f['score'], 'bbox': f['bbox'], 'warnings': f['warnings']}
                                   for i, f in sorted(enumerate(frames), key=lambda it: -it[1]['score'])[:12]]
        try:
            if pose == 'crouch' and coherence['status'] != 'coherent_within_annotation_tolerance':
                raise CalibrationError(coherence['reason'])
            if any(f['bbox'] is None for f in frames):
                raise CalibrationError('Frame sem silhueta mensurável; extração bruta preservada para revisão')
            height = (masters[pose]['bbox'][3]-masters[pose]['bbox'][1])*master_scale
            cal = calibrate(frames, masters[pose], height, clip, threshold,
                            allow_scale_drift=clip.get('scale_correction') is not None)
            transforms, correction = frame_transforms(clip, metadata, cal)
            transforms, root_motion = register_in_place(clip, metadata, cal, transforms, dest/'raw', background)
        except CalibrationError as error:
            info['rejection_reason'] = str(error)
            write_json(dest/'metadata.json', info)
            continue
        info.update(cal)
        info['scale_mode'] = 'annotated_segment_curve' if correction else 'constant_per_video'
        if correction:
            info['scale_correction'] = correction
        if root_motion:
            info['root_motion'] = root_motion
        info['status'] = 'requires_visual_review'
        info['loops'] = loop_candidates(frames, int(clip.get('loop_minimum_period', 6)),
                                       start_frame=clip.get('loop_start_frame', 0)) if clip.get('detect_loop') else []
        boxes = [[b[0]*m[0][0]+m[0][2], b[1]*m[1][1]+m[1][2],
                  b[2]*m[0][0]+m[0][2], b[3]*m[1][1]+m[1][2]]
                 for b, m in zip((f['bbox'] for f in frames), transforms)]
        info['motion'] = motion_summary([{'bbox': b} for b in boxes], 1, cal['reference_frame'], info['animation_type'])
        if root_motion:
            info['motion'].update(automatic_motion_removal=True, removal_mode='in_place_x', anchor='torso_band')
        all_boxes.extend(boxes)
        jobs.append((clip, info, frames, boxes, transforms))
    # A shared project extent, expanded by actual motion and attacks, never a
    # per-animation fit. Include both master reference extents as well.
    for m in masters.values():
        x0, y0, x1, y1 = m['bbox']
        all_boxes.append([(x0-m['root'][0])*master_scale, (y0-m['root'][1])*master_scale,
                          (x1-m['root'][0])*master_scale, (y1-m['root'][1])*master_scale])
    if jobs:
        min_x, min_y = math.floor(min(b[0] for b in all_boxes)), math.floor(min(b[1] for b in all_boxes))
        max_x, max_y = math.ceil(max(b[2] for b in all_boxes)), math.ceil(max(b[3] for b in all_boxes))
        root = [padding-min_x, padding-min_y]
        canvas = [max_x-min_x+2*padding+3, max_y-min_y+2*padding+3]
        if canvas[0]*canvas[1] > 25000000:
            raise CalibrationError('Canvas excessivo; reveja fontes, fundos e deslocamento. Nenhuma redução automática.')
        manifest.update(canvas=canvas, root=root)
        for pose, m in masters.items():
            x0, _, x1, y1 = m['bbox']
            translation = [root[i]-m['root'][i]*master_scale for i in (0, 1)]
            save_rgba(output/'masters'/f'{pose}-aligned.png', affine_rgba(m['rgba'], master_scale, translation, canvas))
            reference_meta[pose]['aligned_file'] = f'masters/{pose}-aligned.png'
        for clip, info, frames, boxes, transforms in jobs:
            dest = output/info['name']
            normalized = dest/'normalized'
            normalized.mkdir()
            absolute = [[[m[0][0], 0, m[0][2]+root[0]], [0, m[1][1], m[1][2]+root[1]]] for m in transforms]
            key = 'calibration_transform_source_to_output' if 'scale_correction' in info or 'root_motion' in info else 'transform_source_to_output'
            info[key] = absolute[info['reference_frame']]
            info['root'] = root
            info['canvas'] = canvas
            ref_box = boxes[info['reference_frame']]
            for i, (frame, entry) in enumerate(zip(frames, info['frames'])):
                rgba = image_rgba(cv2.imread(str(dest/'raw'/entry['file']), cv2.IMREAD_UNCHANGED), info['background_treatment'])
                matrix = absolute[i]
                scale, translation = matrix[0][0], [matrix[0][2], matrix[1][2]]
                result = affine_rgba(rgba, scale, translation, canvas)
                file = f'{i:06d}.png'
                save_rgba(normalized/file, result)
                b = frame['bbox']
                observed = bounds(result)
                entry.update(normalized_file=f"{info['name']}/normalized/{file}", bbox_source=b,
                             scale_applied=scale, transform_source_to_output=matrix,
                             bbox_output=observed, offset_from_root=[observed[0]-root[0], observed[1]-root[1]],
                             support_proxy_source=[(b[0]+b[2])/2, b[3]],
                             visual_displacement_from_reference=[(boxes[i][0]+boxes[i][2]-ref_box[0]-ref_box[2])/2, boxes[i][3]-ref_box[3]],
                             warnings=frame['warnings'])
                if 'root_motion' in info:
                    entry['removed_root_motion_output_px'] = [info['root_motion']['removed_x_output_px'][i], 0]
            info['visual_displacement_note'] = 'Output bbox support proxy, not skeleton root. Includes pose changes. Explicit anatomical scale and ground corrections, if configured, are recorded in each frame matrix.'
            info['spritesheets'] = export_sheets(dest, info['frame_count'], canvas, root, atlas_size)
            write_json(dest/'metadata.json', info)
        evidence = [cv2.cvtColor(cv2.imread(str(output/'masters'/'idle-aligned.png'), -1), cv2.COLOR_BGRA2RGBA)]
        labels = ['Idle Master | target anatomical scale']
        for _, info, _, _, _ in jobs:
            p = output/info['frames'][info['reference_frame']]['normalized_file']
            evidence.append(cv2.cvtColor(cv2.imread(str(p), -1), cv2.COLOR_BGRA2RGBA))
            labels.append(f"{info['name']} | scale {info['scale_per_video']:.5f}")
        contact_sheet(output/'calibration-comparison.png', evidence, labels)
    if any(c['status'] == 'rejected' or c['frame_warnings'] for c in manifest['clips']) or coherence['status'] == 'needs_review':
        manifest['status'] = 'needs_review'
    manifest['review'] = {'visual_approval': False, 'reason': 'Aprovação visual externa obrigatória; execução de testes não aprova arte.'}
    write_json(output/'manifest.json', manifest)
    write_json(output/'calibration-report.json', {k: manifest[k] for k in ('status', 'references', 'master_coherence', 'review')} | {
        'clips': [{k: c.get(k) for k in ('name', 'status', 'reference_frame', 'reference_similarity', 'metrics', 'scale_per_video', 'scale_mode', 'scale_correction', 'root_motion', 'source_root', 'peer_height_spread', 'rejection_reason', 'frame_warnings')} for c in manifest['clips']]})
    write_json(output/'project.snapshot.json', cfg)
    write_viewer(output, manifest)
    return manifest
