"""Explicit lateral registration for game-controlled locomotion.

Measure the torso band, never the whole silhouette centre (a stepping foot or
extended weapon must not drag the sprite). Vertical pose motion is retained.
"""
import copy
import cv2
import numpy as np
from .errors import CalibrationError
from .imaging import image_rgba, bounds


def torso_anchor_x(rgba, band=(.15, .45)):
    x0, y0, x1, y1 = bounds(rgba)
    mask = rgba[:, :, 3] > 128
    top, bottom = y0+int((y1-y0)*band[0]), y0+int((y1-y0)*band[1])
    ys, xs = np.nonzero(mask[top:bottom])
    if len(xs) < 16:
        raise CalibrationError('Tronco insuficiente para registrar movimento no lugar')
    return float(xs.mean())


def register_in_place(clip, metadata, calibration, transforms, raw, background):
    setting = clip.get('root_motion')
    if setting is None:
        return transforms, None
    if setting != {'mode': 'in_place_x', 'anchor': 'torso_band'}:
        raise CalibrationError('root_motion exige mode=in_place_x e anchor=torso_band')
    anchors = [torso_anchor_x(image_rgba(cv2.imread(str(raw/f['file']), -1), background))
               for f in metadata['frames']]
    return register_anchors(anchors, transforms, calibration['reference_frame'], metadata['source_sha256'])


def register_anchors(anchors, transforms, reference, source_sha256):
    if len(anchors) != len(transforms) or not len(anchors) or not np.isfinite(anchors).all():
        raise CalibrationError('Trajetória de registro inválida')
    # Constant-scale clips may deliberately share one immutable input matrix.
    # Copy each occurrence independently before changing its translation.
    matrices = [copy.deepcopy(matrix) for matrix in transforms]
    positions = [float(x*m[0][0]+m[0][2]) for x, m in zip(anchors, matrices)]
    removed = [p-positions[reference] for p in positions]
    for matrix, dx in zip(matrices, removed):
        matrix[0][2] -= dx
    return matrices, {'mode': 'in_place_x', 'anchor': 'torso_band',
        'method': 'alpha_centroid_in_15_to_45_percent_height_band',
        'source_sha256': source_sha256, 'reference_frame': reference,
        'anchor_x_source_px': anchors, 'removed_x_output_px': removed,
        'first_to_last_removed_x_px': removed[-1]-removed[0],
        'vertical_motion': 'preserved', 'game_applies_displacement': True,
        'note': 'Torso registration is a visual anchor, not skeleton tracking. Horizontal translation only; all poses, scales and timestamps retained. Removed translation is metadata, never automatically replayed.'}
