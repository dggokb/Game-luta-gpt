"""Master-grounded calibration; no per-frame scaling or anatomical assumptions."""
import cv2
import numpy as np
from .errors import CalibrationError
from .imaging import image_rgba, bounds, quality, shape_feature, appearance_feature, similarity


def load_master(path, background=None):
    rgba = image_rgba(cv2.imread(str(path), cv2.IMREAD_UNCHANGED), background)
    bbox = bounds(rgba)
    return {'rgba': rgba, 'bbox': bbox, 'feature': shape_feature(rgba, bbox),
            'appearance': appearance_feature(rgba, bbox), 'warnings': quality(rgba, bbox)}


def master_coherence(masters, segments, tolerance=.08):
    if 'crouch' not in masters:
        return {'status': 'idle_only', 'reason': 'Crouch Master não fornecido'}
    if not segments:
        return {'status': 'needs_review', 'reason': 'Anote ao menos um segmento anatômico rígido equivalente nos dois mestres; alturas de poses diferentes não verificam anatomia.'}
    measurements = []
    for segment in segments:
        lengths = {}
        for pose in ('idle', 'crouch'):
            points = np.array(segment[pose], dtype=float)
            h, w = masters[pose]['rgba'].shape[:2]
            if points.shape != (2, 2) or not np.isfinite(points).all() or np.any(points < 0) or np.any(points[:, 0] >= w) or np.any(points[:, 1] >= h):
                raise CalibrationError('Segmento anatômico inválido')
            lengths[pose] = float(np.linalg.norm(points[1]-points[0]))
            if lengths[pose] < 8:
                raise CalibrationError('Segmento anatômico muito curto para medir com segurança')
        measurements.append({'name': segment['name'], 'points': {p: segment[p] for p in lengths},
                             'lengths_px': lengths, 'crouch_to_idle_ratio': lengths['crouch']/lengths['idle']})
    ratios = [m['crouch_to_idle_ratio'] for m in measurements]
    coherent = all(abs(r-1) <= tolerance for r in ratios)
    return {'status': 'coherent_within_annotation_tolerance' if coherent else 'needs_review',
            'method': 'user_annotated_rigid_anatomical_segments', 'tolerance_fraction': tolerance,
            'measurements': measurements, 'correction_applied': False,
            'reason': 'Medições anatômicas aproximadas; revisar marcações na prévia. Nenhuma correção oculta entre mestres.' if coherent else 'Mestres com segmentos anatômicos em escalas incoerentes; reveja imagens ou marcações.'}


def compare_pose(rgba, bbox, master):
    feature = shape_feature(rgba, bbox)
    shape = similarity(feature, master['feature'])
    appearance = float(np.clip(1 - np.mean(np.abs(appearance_feature(rgba, bbox)-master['appearance'])) * 3, 0, 1))
    x0, y0, x1, y1 = bbox
    a0, b0, a1, b1 = master['bbox']
    sx, sy = (a1-a0)/(x1-x0), (b1-b0)/(y1-y0)
    agreement = min(sx, sy)/max(sx, sy)
    return {'silhouette': shape, 'appearance': appearance, 'dimension_agreement': agreement,
            'score': .65 * shape + .2 * appearance + .15 * agreement,
            'feature': feature}


def scan(raw, master, background):
    frames = []
    for file in sorted(raw.glob('*.png')):
        rgba = image_rgba(cv2.imread(str(file), cv2.IMREAD_UNCHANGED), background)
        try:
            bbox = bounds(rgba)
            metrics = compare_pose(rgba, bbox, master)
            warnings = quality(rgba, bbox)
        except CalibrationError as error:
            bbox, metrics = None, {'score': 0, 'feature': np.zeros((96, 96), np.uint8)}
            warnings = [str(error)]
        frames.append({'bbox': bbox, 'warnings': warnings, **metrics})
    return frames


def calibrate(frames, master, target_height, clip, threshold=.82):
    explicit = clip.get('reference_frame')
    if explicit is not None and (not isinstance(explicit, int) or not 0 <= explicit < len(frames)):
        raise CalibrationError('Frame de referência fora do vídeo')
    options = [explicit] if explicit is not None else range(len(frames))
    viable = [i for i in options if frames[i]['bbox'] and not frames[i]['warnings']]
    if not viable:
        raise CalibrationError('Nenhuma pose de referência com silhueta íntegra')
    # Prefer equivalent poses near the start; tie only, never override confidence.
    top_score = max(frames[i]['score'] for i in viable)
    close = [i for i in viable if frames[i]['score'] >= max(threshold, top_score-.01)
             and frames[i]['silhouette'] >= .78 and frames[i]['appearance'] >= .70 and frames[i]['dimension_agreement'] >= .88]
    idx = min(close) if close else max(viable, key=lambda i: frames[i]['score'])
    best = frames[idx]
    if best['score'] < threshold or best['silhouette'] < .78 or best['appearance'] < .70 or best['dimension_agreement'] < .88:
        raise CalibrationError(f"Pose incerta: score={best['score']:.3f}, silhueta={best['silhouette']:.3f}, aparência={best['appearance']:.3f}, proporções={best['dimension_agreement']:.3f}; nenhuma escala aprovada")
    x0, y0, x1, y1 = best['bbox']
    scale = target_height/(y1-y0)
    # Several equally plausible poses with incompatible heights are ambiguous.
    peers = [f for f in frames if f['bbox'] and not f['warnings'] and f['score'] >= max(threshold, best['score']-.015)
             and f.get('dimension_agreement', 0) >= .88]
    peer_heights = [f['bbox'][3]-f['bbox'][1] for f in peers]
    spread = (max(peer_heights)-min(peer_heights))/np.median(peer_heights) if peer_heights else 0
    if spread > .06:
        raise CalibrationError(f'Poses equivalentes com variação de escala de {spread:.1%} dentro do vídeo; escala única não resolve zoom/deformação')
    mx0, my0, mx1, my1 = master['bbox']
    master_root = master.get('root', [(mx0+mx1)/2, my1])
    source_root = clip.get('source_root', [x0+(master_root[0]-mx0)*(x1-x0)/(mx1-mx0), y0+(master_root[1]-my0)*(y1-y0)/(my1-my0)])
    if len(source_root) != 2 or not np.isfinite(source_root).all():
        raise CalibrationError('Root de origem inválido')
    h, w = master['rgba'].shape[:2]
    mx0, _, mx1, my1 = master['bbox']
    master_root = master.get('root', [(mx0+mx1)/2, my1])
    if len(master_root) != 2 or not np.isfinite(master_root).all() or not (0 <= master_root[0] <= w and 0 <= master_root[1] <= h):
        raise CalibrationError('Root mestre inválido')
    # Default is a measurable support anchor, not an inferred skeleton joint.
    return {'reference_frame': idx, 'reference_similarity': best['score'],
            'metrics': {k: best[k] for k in ('silhouette', 'appearance', 'dimension_agreement')},
            'scale_per_video': scale, 'source_root': list(map(float, source_root)),
            'master_root': list(map(float, master_root)), 'peer_height_spread': float(spread),
            'peer_count': len(peers), 'target_visual_height': target_height,
            'root_method': 'explicit' if 'source_root' in clip else 'reference_silhouette_bottom_center',
            'calibration_status': 'matched_requires_visual_review'}
