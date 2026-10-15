"""Observable support motion, with conservative advisory classification."""
import numpy as np


def motion_summary(frames, scale, reference_index, declared):
    boxes = np.array([f['bbox'] for f in frames], float)
    support = np.column_stack(((boxes[:, 0]+boxes[:, 2])/2, boxes[:, 3]))
    reference = support[reference_index]
    dx, dy = (support-reference).T*scale
    height = float(np.median(boxes[:, 3]-boxes[:, 1])*scale)
    threshold = max(5, height*.08)
    upward = float(-min(dy))
    horizontal = float(max(dx)-min(dx))
    vertical = float(max(dy)-min(dy))
    if upward > threshold:
        proposed = 'airborne_or_vertical_camera_motion'
    elif horizontal > threshold:
        proposed = 'translation_or_pose_extension'
    else:
        proposed = 'stationary_or_crouched'
    return {'declared_type': declared, 'advisory_type': proposed,
            'horizontal_range_px': horizontal, 'vertical_range_px': vertical,
            'upward_from_reference_px': upward,
            'first_to_last_support_displacement_px': ((support[-1]-support[0])*scale).tolist(),
            'method': 'silhouette_bottom_center_proxy', 'automatic_motion_removal': False,
            'caveat': 'Extending limbs, deformation and camera motion can move the proxy; this is not skeleton tracking or proof of locomotion.'}
