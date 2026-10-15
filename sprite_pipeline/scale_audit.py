"""Measure named anatomical segments across explicitly annotated motion phases.

Projected lengths require comparable views and careful landmarks. This is a
checkpoint audit, not automatic anatomy detection or approval of unmeasured frames.
"""
import numpy as np
from .errors import CalibrationError


def audit_scale(setting, metadata, transforms):
    if setting is None:
        return {'status': 'not_checked', 'full_frame_anatomical_validation': False,
                'note': 'Pose matching does not verify body size in other action poses.'}
    if not isinstance(setting, dict) or setting.get('source_sha256') != metadata['source_sha256']:
        raise CalibrationError('Conferência anatômica exige SHA-256 da fonte correta')
    count = metadata['frame_count']
    if len(transforms) != count:
        raise CalibrationError('Conferência anatômica exige uma matriz por frame')
    tolerance = setting.get('tolerance_fraction', .06)
    tolerances = setting.get('segment_tolerances', {})
    if not isinstance(tolerances, dict):
        raise CalibrationError('Tolerâncias anatômicas inválidas')
    for value in [tolerance, *tolerances.values()]:
        if isinstance(value, bool) or not isinstance(value, (int, float)) or not np.isfinite(value) or not 0 < value <= .15:
            raise CalibrationError('Tolerância anatômica deve ser positiva e até 15%')
    keys = setting.get('checkpoints', [])
    if not isinstance(keys, list) or len(keys) < 3 or not all(isinstance(k, dict) for k in keys):
        raise CalibrationError('Anote início, fases intermediárias e fim do movimento')
    indices = [k.get('frame') for k in keys]
    if (any(type(i) is not int or not 0 <= i < count for i in indices)
            or indices[0] != 0 or indices[-1] != count-1
            or any(a >= b for a,b in zip(indices, indices[1:]))):
        raise CalibrationError('Checkpoints devem ser crescentes e cobrir início e fim')
    reference = setting.get('reference_frame', 0)
    if type(reference) is not int or reference not in indices:
        raise CalibrationError('Referência anatômica ausente dos checkpoints')
    phases = [k.get('phase') for k in keys]
    required = setting.get('required_phases')
    if (not all(isinstance(p, str) and p for p in phases)
            or not isinstance(required, list) or not all(isinstance(p, str) and p for p in required)
            or len(set(required)) < 3
            or not set(required).issubset(phases)):
        raise CalibrationError('Anote e confira todas as fases declaradas do movimento')
    width, height = metadata['resolution']
    samples = []
    for key in keys:
        segments = key.get('segments')
        if not isinstance(segments, dict) or not segments:
            raise CalibrationError('Checkpoint sem segmento anatômico')
        matrix = np.asarray(transforms[key['frame']], dtype=float)
        if matrix.shape != (2,3) or not np.isfinite(matrix).all():
            raise CalibrationError('Matriz anatômica inválida')
        lengths = {}
        for name, points in segments.items():
            try:
                points = np.asarray(points, dtype=float)
            except (ValueError, TypeError) as error:
                raise CalibrationError('Segmento anatômico inválido') from error
            if (not isinstance(name,str) or not name or points.shape != (2,2)
                    or not np.isfinite(points).all() or np.any(points < 0)
                    or np.any(points[:,0] >= width) or np.any(points[:,1] >= height)):
                raise CalibrationError('Segmento anatômico fora do frame ou inválido')
            vector = points[1]-points[0]
            source_length = float(np.linalg.norm(vector))
            if source_length < 8:
                raise CalibrationError('Segmento anatômico curto demais para conferir escala')
            lengths[name] = {'points': points.tolist(), 'source_length_px': source_length,
                             'output_length_px': float(np.linalg.norm(matrix[:,:2]@vector))}
        samples.append({'frame':key['frame'], 'phase':key['phase'],
                        'purpose':key.get('purpose','review'), 'segments':lengths})
    baseline = samples[indices.index(reference)]['segments']
    primary = setting.get('primary_segment', next(iter(baseline)))
    if not isinstance(primary, str) or primary not in baseline or any(primary not in s['segments'] for s in samples):
        raise CalibrationError('O mesmo segmento principal deve ser conferido em todas as fases')
    if set(tolerances)-set(baseline):
        raise CalibrationError('Tolerância sem segmento na referência')
    changes = []
    for sample in samples:
        for name, measure in sample['segments'].items():
            if name not in baseline or baseline[name]['output_length_px'] <= 0:
                raise CalibrationError('Segmento sem referência anatômica válida')
            ratio = measure['output_length_px']/baseline[name]['output_length_px']
            measure.update(output_ratio_to_reference=ratio,
                           source_ratio_to_reference=measure['source_length_px']/baseline[name]['source_length_px'])
            if abs(ratio-1) > tolerances.get(name,tolerance):
                changes.append({'frame':sample['frame'], 'phase':sample['phase'], 'segment':name, 'ratio':ratio})
    return {'status':'drift_detected' if changes else 'within_annotation_tolerance',
            'method':'named_anatomical_segments_across_motion_phases',
            'primary_segment':primary,
            'source_sha256':metadata['source_sha256'], 'reference_frame':reference,
            'tolerance_fraction':tolerance, 'segment_tolerances':tolerances,
            'phases_checked':list(dict.fromkeys(phases)), 'checkpoint_count':len(keys),
            'holdout_count':sum(s['purpose']=='holdout' for s in samples),
            'frame_count':count, 'full_frame_anatomical_validation':False,
            'samples':samples, 'changes':changes,
            'note':'Manual source landmarks and projected lengths; unmeasured frames, perspective and source deformation still require visual review.'}


def require_scale_audit(report):
    if report['status'] == 'drift_detected':
        change = max(report['changes'], key=lambda c:abs(c['ratio']-1))
        raise CalibrationError(f"Tamanho muda fora da pose de referência: {change['segment']} "
                               f"{change['ratio']-1:+.1%}, frame {change['frame']} ({change['phase']})")
