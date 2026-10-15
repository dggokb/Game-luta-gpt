#!/usr/bin/env python3
"""Evidence of real movement, reference scale and source limitations."""
import argparse
import json
from pathlib import Path
import sys
import cv2
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from sprite_pipeline.exporting import contact_sheet, write_json
from sprite_pipeline.imaging import bounds


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('result', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    manifest = json.loads((args.result/'manifest.json').read_text())
    clips = {c['name']: c for c in manifest['clips']}
    args.output.mkdir(parents=True, exist_ok=True)

    def read(frame):
        return cv2.cvtColor(cv2.imread(str(args.result/frame['normalized_file']), -1), cv2.COLOR_BGRA2RGBA)

    images, labels, report = [], [], {}
    references, reference_labels = [], []
    for name in ('walk_forward', 'walk_back', 'back_dash'):
        c = clips[name]
        assert c['status'] != 'rejected'
        ref = c['frames'][c['reference_frame']]
        visual = bounds(read(ref))
        support = np.asarray([[(f['bbox_output'][0]+f['bbox_output'][2])/2,
                               f['bbox_output'][3]] for f in c['frames']])
        axis = 1 if name == 'back_dash' else 0
        peak = int(np.argmax(np.abs(support[:, axis]-support[0, axis])))
        action = 100 if name == 'walk_forward' else 60 if name == 'walk_back' else peak
        for index in (0, action, c['frame_count']-1):
            images.append(read(c['frames'][index]))
            labels.append(f'{name}: frame {index}, t={c["frames"][index]["time_s"]:.3f}s')
        references += [read(clips['idle']['frames'][0]), read(ref), read(c['frames'][action])]
        reference_labels += ['Idle: common scale', f'{name}: reference {c["reference_frame"]}', f'{name}: action {action}']
        report[name] = {
            'source_name': c['source_name'], 'source_sha256': c['source_sha256'],
            'frame_count': c['frame_count'], 'reference_frame': c['reference_frame'],
            'reference_similarity': c['reference_similarity'],
            'reference_height_source_px': ref['bbox_source'][3]-ref['bbox_source'][1],
            'reference_height_output_px': visual[3]-visual[1],
            'constant_scale': c['scale_per_video'], 'peer_height_spread': c['peer_height_spread'],
            'animation_type': c['animation_type'], 'loops': c['loops'],
            'output_support_range_px': (support.max(axis=0)-support.min(axis=0)).tolist(),
            'output_first_to_last_support_delta_px': (support[-1]-support[0]).tolist(),
            'frame_warnings': c['frame_warnings'], 'note': c.get('note', ''),
        }
    contact_sheet(args.output/'movement-reference-scale.png', references, reference_labels)
    contact_sheet(args.output/'movement-displacement.png', images, labels)
    provenance = json.loads((args.result.parent/'provenance.json').read_text())
    write_json(args.output/'movement-checks.json', {
        'clips_in_viewer': len(clips), 'frames_in_viewer': sum(c['frame_count'] for c in clips.values()),
        'new_source_frames': sum(report[name]['frame_count'] for name in report),
        'canvas': manifest['canvas'], 'root': manifest['root'], 'clips': report,
        'source_files_unchanged_at_processing': provenance['originals_unchanged'],
        'support_note': 'Foreground bbox support proxy; changes in pose and feet can move it without locomotion. Not skeletal root motion.',
        'visual_approval': False,
    })
    print(json.dumps({name: {k: c[k] for k in ('frame_count', 'reference_height_output_px', 'output_support_range_px')} for name, c in report.items()}, indent=2))


if __name__ == '__main__':
    main()
