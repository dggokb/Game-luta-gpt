#!/usr/bin/env python3
"""Compare an uncorrected real batch with its annotated scale correction."""
import argparse
import json
from pathlib import Path
import sys
import cv2
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from sprite_pipeline.exporting import contact_sheet, write_json
from sprite_pipeline.imaging import bounds


def read(root, frame):
    return cv2.cvtColor(cv2.imread(str(root/frame['normalized_file']), -1), cv2.COLOR_BGRA2RGBA)


def check(before, after, output):
    before, after, output = map(Path, (before, after, output))
    output.mkdir(parents=True, exist_ok=True)
    old = json.loads((before/'manifest.json').read_text())
    new = json.loads((after/'manifest.json').read_text())
    assert old['canvas'] == new['canvas'] and old['root'] == new['root']
    old_clips = {c['name']: c for c in old['clips']}
    new_clips = {c['name']: c for c in new['clips']}
    images, labels, landmarks, landmark_labels = [], [], [], []
    report = {'source_batch_frames': sum(c['frame_count'] for c in old['clips']),
              'corrected_batch_frames': sum(c['frame_count'] for c in new['clips']),
              'canvas': new['canvas'], 'root': new['root'], 'clips': {}, 'unchanged_clips': []}
    assert report['source_batch_frames'] == report['corrected_batch_frames']
    for name, c in new_clips.items():
        previous = old_clips[name]
        assert c['source_sha256'] == previous['source_sha256']
        assert [f['pts'] for f in c['frames']] == [f['pts'] for f in previous['frames']]
        if 'scale_correction' not in c:
            for a, b in zip(previous['frames'], c['frames']):
                np.testing.assert_array_equal(read(before, a), read(after, b))
            report['unchanged_clips'].append(name)
            continue
        keys = c['scale_correction']['keyframes']
        before_lengths = [k['length_px']*previous['scale_per_video'] for k in keys]
        after_lengths = [k['length_px']*c['frames'][k['frame']]['scale_applied'] for k in keys]
        floors = [bounds(read(after, f))[3] for f in c['frames']]
        assert max(after_lengths)-min(after_lengths) < .001
        assert max(floors)-min(floors) <= 4
        report['clips'][name] = {
            'manual_segment': c['scale_correction']['segment_name'],
            'measurement_scope': 'annotated keyframes, not an automatic anatomical proof',
            'before_segment_range_px': [min(before_lengths), max(before_lengths)],
            'before_growth_fraction': max(before_lengths)/before_lengths[0]-1,
            'after_segment_range_px': [min(after_lengths), max(after_lengths)],
            'factor_range': c['scale_correction']['factor_range'],
            'actual_output_floor_range_px': [min(floors), max(floors)],
        }
        idle = new_clips['idle']
        for im, label in (
                (read(after, idle['frames'][0]), 'Idle: reference scale'),
                (read(before, previous['frames'][60]), f'{name}: BEFORE, frame 60'),
                (read(after, c['frames'][60]), f'{name}: AFTER, frame 60')):
            images.append(im); labels.append(label)
        for key in keys:
            if key['frame'] not in (0, 20, 60, 120):
                continue
            raw = cv2.cvtColor(cv2.imread(str(after/c['frames'][key['frame']]['raw_file']), -1), cv2.COLOR_BGRA2RGBA)
            a, b = [tuple(round(v) for v in p) for p in key['points']]
            cv2.line(raw, a, b, (255, 200, 60, 255), 3, cv2.LINE_AA)
            for point in (a, b):cv2.circle(raw, point, 5, (80, 230, 220, 255), -1)
            cv2.line(raw, (0, round(key['ground_y'])), (raw.shape[1]-1, round(key['ground_y'])), (80, 230, 220, 255), 2)
            landmarks.append(raw);landmark_labels.append(f'{name}: frame {key["frame"]}, head {key["length_px"]:.1f}px')
    c = new_clips['crouch']
    target = c['scale_correction']['keyframes'][0]['segment_length_output_px']
    held_out = []
    for index, points in [(40, [[584, 146], [618, 291]]), (80, [[584, 147], [620, 291]])]:
        length = float(np.linalg.norm(np.subtract(points[1], points[0])))
        actual = length*c['frames'][index]['scale_applied']
        error = abs(actual/target-1)
        assert error < .025
        held_out.append({'frame': index, 'manual_points': points, 'length_output_px': actual, 'relative_error': error})
    report['held_out_crouch_checkpoints'] = held_out
    report['visual_approval'] = False
    report['status'] = 'checks_passed_requires_visual_review'
    contact_sheet(output/'crouch-scale-before-after.png', images, labels)
    contact_sheet(output/'crouch-scale-landmarks.png', landmarks, landmark_labels)
    write_json(output/'crouch-scale-checks.json', report)
    print(json.dumps(report, indent=2))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--before', type=Path, required=True)
    parser.add_argument('--after', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    check(args.before, args.after, args.output)
