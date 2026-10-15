"""Approximate sequence comparison; proposals never discard any frame."""
import numpy as np
from .imaging import similarity


def loop_candidates(frames, minimum_period=6, threshold=.90):
    features = [f.get('feature') for f in frames]
    n = len(features)
    if n < minimum_period*2 or any(f['warnings'] for f in frames):
        return []
    # Static holds have no identifiable cycle. Avoid claiming period=6 for stills.
    changes = [1-similarity(a, b) for a, b in zip(features, features[1:])]
    if max(changes, default=0) < .015:
        return []
    candidates = []
    for period in range(minimum_period, n//2+1):
        scores = np.array([similarity(features[i], features[i+period]) for i in range(n-period)])
        # Require two complete cycles, allow nonrepeating lead-in/out.
        for start in range(n-2*period+1):
            confidence = float(np.mean(scores[start:start+period]))
            if confidence < threshold or float(np.quantile(scores[start:start+period], .1)) < threshold-.07:
                continue
            seam = similarity(features[start+period-1], features[start])
            normal_steps = [similarity(features[i], features[i+1]) for i in range(start, start+period-1)]
            normal = float(np.median(normal_steps))
            # Shape features are translation invariant. Record positional closure too.
            a, b = frames[start]['bbox'], frames[start+period]['bbox']
            displacement = [(b[0]+b[2]-a[0]-a[2])/2, b[3]-a[3]]
            candidates.append({'start_frame': start, 'period_frames': period, 'end_frame_exclusive': start+period,
                               'repeat_frame': start+period, 'confidence': round(confidence, 5),
                               'seam_similarity': round(seam, 5), 'typical_adjacent_similarity': round(normal, 5),
                               'seam_relative_quality': round(min(1, seam/max(normal, 1e-9)), 5),
                               'cycle_displacement_source_px': displacement,
                               'method': 'all_frame_silhouette_sequence_comparison', 'advisory_only': True})
    # Keep distinct candidate periods, then order by confidence. No automatic trimming.
    best = {}
    for c in candidates:
        if c['period_frames'] not in best or c['confidence'] > best[c['period_frames']]['confidence']:
            best[c['period_frames']] = c
    return sorted(best.values(), key=lambda c: (-c['confidence'], c['period_frames']))[:8]
