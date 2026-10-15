"""Explicit, annotated correction of scale drift inside existing footage.

Pose height is never used: bending the knees must remain a change of posture.
Unannotated clips retain their original constant transform.
"""
import numpy as np
from .errors import CalibrationError


def frame_transforms(clip, metadata, calibration):
    """Return root-relative matrices and an auditable correction report."""
    base_scale = calibration['scale_per_video']
    ox, oy = calibration['source_root']
    count = metadata['frame_count']
    correction = clip.get('scale_correction')
    if correction is None:
        matrix = [[base_scale, 0, -ox*base_scale], [0, base_scale, -oy*base_scale]]
        return [matrix for _ in range(count)], None
    if not isinstance(correction, dict):
        raise CalibrationError('scale_correction deve conter segmentos anatômicos anotados')
    if correction.get('source_sha256', metadata['source_sha256']) != metadata['source_sha256']:
        raise CalibrationError('Anotações de escala pertencem a outro vídeo (SHA-256 diferente)')
    keys = correction.get('keyframes', [])
    if not isinstance(keys, list) or len(keys) < 2 or not correction.get('segment_name'):
        raise CalibrationError('Anote o mesmo segmento anatômico em ao menos dois keyframes')
    indices = [key.get('frame') for key in keys if isinstance(key, dict)]
    if (len(indices) != len(keys) or any(type(i) is not int for i in indices)
            or indices[0] != 0 or indices[-1] != count-1
            or any(b <= a for a, b in zip(indices, indices[1:]))):
        raise CalibrationError('Keyframes devem ser únicos, crescentes e cobrir o primeiro e o último frame')
    reference = calibration['reference_frame']
    if reference not in indices:
        raise CalibrationError('A pose de calibração também deve ter segmento anotado')
    width, height = metadata['resolution']
    measured = []
    for key in keys:
        try:
            points = np.asarray(key.get('points'), dtype=float)
        except (TypeError, ValueError) as error:
            raise CalibrationError('Segmento de correção inválido') from error
        if (points.shape != (2, 2) or not np.isfinite(points).all() or np.any(points < 0)
                or np.any(points[:, 0] >= width) or np.any(points[:, 1] >= height)):
            raise CalibrationError('Segmento de correção fora do frame ou inválido')
        length = float(np.linalg.norm(points[1]-points[0]))
        if length < 8:
            raise CalibrationError('Segmento de correção muito curto para medir com segurança')
        measured.append({'frame': key['frame'], 'time_s': metadata['frames'][key['frame']]['time_s'],
                         'points': points.tolist(), 'length_px': length})
    times = np.array([frame['time_s'] for frame in metadata['frames']], dtype=float)
    if not np.isfinite(times).all() or np.any(np.diff(times) <= 0):
        raise CalibrationError('Correção exige timestamps originais estritamente crescentes')
    lengths = np.array([key['length_px'] for key in measured])
    reference_length = lengths[indices.index(reference)]
    factors = reference_length/np.interp(times, times[indices], lengths)
    limit = correction.get('maximum_change_fraction', .2)
    if (isinstance(limit, bool) or not isinstance(limit, (int, float)) or not 0 < limit <= .5
            or np.max(np.abs(factors-1)) > limit):
        raise CalibrationError('Correção excede a variação máxima de escala declarada (padrão 20%)')
    ground = ['ground_y' in key for key in keys]
    if any(ground) and not all(ground):
        raise CalibrationError('Anote ground_y em todos os keyframes ou em nenhum')
    anchors_y = np.full(count, oy)
    if all(ground):
        if clip.get('animation_type', 'stationary') not in ('stationary', 'crouched'):
            raise CalibrationError('Correção de chão exige animação apoiada, nunca salto ou deslocamento')
        values = [key['ground_y'] for key in keys]
        if any(isinstance(v, bool) or not isinstance(v, (int, float))
               or not np.isfinite(v) or not 0 <= v <= height for v in values):
            raise CalibrationError('ground_y inválido ou fora do frame')
        if abs(values[indices.index(reference)]-oy) > 2:
            raise CalibrationError('Chão anotado da pose de calibração não coincide com seu root')
        anchors_y = np.interp(times, times[indices], values)
        for key, value in zip(measured, values):
            key['ground_y'] = value
    scales = base_scale*factors
    matrices = [[[float(s), 0, float(-ox*s)], [0, float(s), float(-y*s)]]
                for s, y in zip(scales, anchors_y)]
    for key, factor in zip(measured, factors[indices]):
        key['correction_factor'] = float(factor)
        key['segment_length_output_px'] = reference_length*base_scale
    return matrices, {
        'method': 'annotated_segment_length_curve', 'segment_name': correction['segment_name'],
        'source_sha256': metadata['source_sha256'], 'reference_frame': reference,
        'interpolation': 'linear_segment_length_by_original_timestamp',
        'maximum_change_fraction': limit, 'keyframes': measured,
        'factor_range': [float(factors.min()), float(factors.max())],
        'horizontal_anchor': 'fixed_calibration_root',
        'vertical_anchor': 'annotated_ground' if all(ground) else 'fixed_calibration_root',
        'note': 'Manual anatomical measurements, not automatic pose detection. No pose-height fitting, nonuniform stretching or frame removal.',
    }
