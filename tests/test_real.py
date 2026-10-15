"""Validate artifacts from existing real footage. Set SPRITE_REAL_RESULT.

No synthetic video is created. The half-resolution case resamples the same
existing frames and is explicitly labelled as controlled, derived evidence.
"""
import json
import os
import unittest
from pathlib import Path
import cv2
import numpy as np
from sprite_pipeline.imaging import bounds

ROOT = os.environ.get('SPRITE_REAL_RESULT')


@unittest.skipUnless(ROOT, 'Defina SPRITE_REAL_RESULT para o lote de vídeos reais')
class RealFootageTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.root = Path(ROOT)
        cls.manifest = json.loads((cls.root/'manifest.json').read_text())
        cls.clips = {c['name']: c for c in cls.manifest['clips']}

    def read(self, entry, kind='normalized_file'):
        return cv2.cvtColor(cv2.imread(str(self.root/entry[kind]), -1), cv2.COLOR_BGRA2RGBA)

    def test_all_frames_timestamps_and_original_resolution(self):
        for c in self.clips.values():
            with self.subTest(c=c['name']):
                self.assertEqual(c['frame_count'], len(c['frames']))
                self.assertEqual(len(list((self.root/c['name']/'raw').glob('*.png'))), c['frame_count'])
                times = [f['timestamp_s'] for f in c['frames']]
                self.assertTrue(all(b > a for a, b in zip(times, times[1:])))
                for f in c['frames']:
                    raw = self.read(f, 'raw_file')
                    self.assertEqual([raw.shape[1], raw.shape[0]], c['resolution'])
                    self.assertGreater(f['duration_s'], 0)

    def test_raw_pixels_pts_and_original_hash_match_independent_decode(self):
        from sprite_pipeline.extraction import run, digest
        config = json.loads((self.root/'project.snapshot.json').read_text())
        source_base = Path(os.environ.get('SPRITE_REAL_INPUTS', self.root.parent))
        for c, configured in zip(self.manifest['clips'], config['clips']):
            source = source_base/configured['video']
            if not source.exists():
                self.skipTest(f'Fonte indisponível para conferência independente: {source}')
            self.assertEqual(digest(source), c['source_sha256'])
            probe = json.loads(run(['ffprobe','-v','error','-select_streams','v:0',
                                    '-show_frames','-show_entries','frame=best_effort_timestamp',
                                    '-of','json',str(source)]))
            self.assertEqual([int(f['best_effort_timestamp']) for f in probe['frames']],
                             [f['pts'] for f in c['frames']])
            decoded = run(['ffmpeg','-v','error','-noautorotate','-i',str(source),'-map','0:v:0',
                           '-frames:v','1','-f','rawvideo','-pix_fmt','rgba','pipe:1'])
            expected = np.frombuffer(decoded,np.uint8).reshape(c['resolution'][1],c['resolution'][0],4)
            np.testing.assert_array_equal(self.read(c['frames'][0], 'raw_file'), expected)

    def test_visual_scale_across_different_video_sizes(self):
        a, b = self.clips['jab'], self.clips['jab_half']
        fa, fb = a['frames'][a['reference_frame']], b['frames'][b['reference_frame']]
        self.assertGreater(fa['bbox_source'][3]-fa['bbox_source'][1], 1.9*(fb['bbox_source'][3]-fb['bbox_source'][1]))
        ba, bb = bounds(self.read(fa)), bounds(self.read(fb))
        for axis in (0, 1):
            self.assertLessEqual(abs((ba[axis+2]-ba[axis])-(bb[axis+2]-bb[axis])), 2)
        self.assertAlmostEqual(b['scale_per_video']/a['scale_per_video'], 2, delta=.02)
        # Compare interior anatomical sections too, not only PNG or bbox height.
        for f, c in ((fa, a), (fb, b)):
            im = self.read(f)
            root = c['root']
            bnd = bounds(im)
            y0, y1 = bnd[1], bnd[3]
            rows = []
            for fraction in (.12, .3, .5, .8):
                mask = im[round(y0+(y1-y0)*fraction), :, 3] > 64
                xx = np.flatnonzero(mask)
                rows.append(int(xx[-1]-xx[0]+1) if len(xx) else 0)
            if f is fa:
                widths_a = rows
            else:
                self.assertTrue(all(abs(x-y) <= 5 for x, y in zip(widths_a, rows)), (widths_a, rows))
        self.assertEqual(a['reference_frame'], 0)

    def test_pose_starting_crouched_uses_crouch_master(self):
        c = self.clips['crouch_small']
        self.assertEqual(c['reference_pose'], 'crouch')
        self.assertLessEqual(c['reference_frame'], 10)
        f = c['frames'][c['reference_frame']]
        im = self.read(f)
        target = self.manifest['references']['crouch']['bbox']
        actual = bounds(im)
        first = c['frames'][0]
        first_height = bounds(self.read(first))[3]-bounds(self.read(first))[1]
        expected_first = (first['bbox_source'][3]-first['bbox_source'][1])*c['scale_per_video']
        self.assertLessEqual(abs(first_height-expected_first), 2)
        self.assertLessEqual(abs((actual[3]-actual[1])-(target[3]-target[1])), 2)
        self.assertNotEqual(actual[3]-actual[1], self.manifest['references']['idle']['bbox'][3]-self.manifest['references']['idle']['bbox'][1])

    def test_no_per_frame_scale_or_recenter_and_movement_preserved(self):
        for name, axis, minimum_motion in [('dash', 0, 40), ('jump', 1, 60)]:
            c = self.clips[name]
            matrix = np.array(c['transform_source_to_output'])
            support = []
            for f in c['frames']:
                b = f['bbox_source']; actual = bounds(self.read(f))
                expected = np.array([b[0], b[1]])*c['scale_per_video']+matrix[:, 2]
                np.testing.assert_allclose(actual[:2], expected, atol=2)
                support.append((actual[0]+actual[2])/2 if axis == 0 else actual[3])
            self.assertGreater(max(support)-min(support), minimum_motion)

    def test_repeated_idle_has_advisory_sequence_candidates(self):
        c = self.clips['idle']
        self.assertTrue(c['loops'])
        candidate = c['loops'][0]
        self.assertGreaterEqual(candidate['confidence'], .9)
        self.assertGreaterEqual(candidate['start_frame'], 0)
        self.assertIn('seam_relative_quality', candidate)
        self.assertTrue(candidate['advisory_only'])
        self.assertEqual(len(list((self.root/'idle'/'normalized').glob('*.png'))), c['frame_count'])

    def test_problem_silhouettes_are_reported(self):
        c = self.clips['jump']
        self.assertTrue(any('source_touches_edge_possible_clipping' in f['warnings'] for f in c['frame_warnings']))
        self.assertFalse(self.manifest['review']['visual_approval'])

    def test_atlas_round_trip_and_shared_coordinates(self):
        for c in self.clips.values():
            if c['status'] == 'rejected':continue
            self.assertEqual(c['root'], self.manifest['root'])
            atlas = json.loads((self.root/c['name']/'spritesheets'/'atlas.json').read_text())
            self.assertEqual(len(atlas['frames']), c['frame_count'])
            for i in (0, c['frame_count']//2, c['frame_count']-1):
                f = atlas['frames'][i]
                sheet = cv2.imread(str(self.root/c['name']/'spritesheets'/f['page']), -1)
                x,y,w,h = f['rect']
                png = cv2.imread(str(self.root/c['frames'][i]['normalized_file']), -1)
                np.testing.assert_array_equal(sheet[y:y+h,x:x+w], png)
                self.assertEqual(f['root_in_cell'], c['root'])

    def test_switching_reference_poses_has_same_floor_and_visual_size(self):
        heights = []
        for name in ('idle','jab','jab_half','dash','jump'):
            c = self.clips[name]
            f = c['frames'][c['reference_frame']]
            b = bounds(self.read(f))
            heights.append(b[3]-b[1])
            self.assertLessEqual(abs(b[3]-c['root'][1]), 1)
        self.assertLessEqual(max(heights)-min(heights), 2)

    def test_crouch_does_not_grow_during_the_transition(self):
        for name in ('crouch', 'crouch_jab'):
            c = self.clips[name]
            with self.subTest(clip=name):
                self.assertEqual(c['scale_mode'], 'annotated_segment_curve')
                self.assertNotIn('transform_source_to_output', c)
                measured = c['scale_correction']['keyframes']
                source_lengths = [key['length_px'] for key in measured]
                corrected_lengths = []
                for key in measured:
                    f = c['frames'][key['frame']]
                    matrix = np.asarray(f['transform_source_to_output'])
                    points = np.asarray(key['points'])
                    transformed = points@matrix[:, :2].T+matrix[:, 2]
                    corrected_lengths.append(np.linalg.norm(transformed[1]-transformed[0]))
                    self.assertAlmostEqual(key['ground_y']*matrix[1, 1]+matrix[1, 2], c['root'][1])
                self.assertGreater(max(source_lengths)/min(source_lengths), 1.05)
                self.assertLess(max(corrected_lengths)-min(corrected_lengths), .001)
                floors = []
                for f in c['frames']:
                    actual = bounds(self.read(f))
                    floors.append(actual[3])
                    matrix = np.asarray(f['transform_source_to_output'])
                    expected = np.asarray(f['bbox_source'][:2])*matrix[0, 0]+matrix[:, 2]
                    np.testing.assert_allclose(actual[:2], expected, atol=2)
                self.assertLessEqual(max(floors)-min(floors), 4)

    def test_crouch_head_measurements_on_frames_not_used_for_calibration(self):
        c = self.clips['crouch']
        # Independent manual checkpoints, deliberately absent from the curve.
        held_out = [(40, [[584, 146], [618, 291]]), (80, [[584, 147], [620, 291]])]
        target = c['scale_correction']['keyframes'][0]['segment_length_output_px']
        calibrated = {key['frame'] for key in c['scale_correction']['keyframes']}
        for index, points in held_out:
            self.assertNotIn(index, calibrated)
            length = np.linalg.norm(np.subtract(points[1], points[0]))
            corrected = length*c['frames'][index]['scale_applied']
            self.assertLess(abs(corrected/target-1), .025)


if __name__ == '__main__':
    unittest.main()
