"""Checks on the existing walk/backdash footage; no generated video fixtures."""
import json
import os
from pathlib import Path
import unittest
import cv2
import numpy as np
from sprite_pipeline.imaging import bounds, image_rgba

ROOT = os.environ.get('SPRITE_REAL_RESULT')
NAMES = ('walk_forward', 'walk_back', 'back_dash')


@unittest.skipUnless(ROOT, 'Defina SPRITE_REAL_RESULT para o lote de movimentação real')
class MovementTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.root = Path(ROOT)
        cls.manifest = json.loads((cls.root/'manifest.json').read_text())
        cls.clips = {clip['name']: clip for clip in cls.manifest['clips']}
        if not all(name in cls.clips for name in NAMES):
            raise unittest.SkipTest('Lote sem a extensão de movimentação')

    def read(self, frame):
        return cv2.cvtColor(cv2.imread(str(self.root/frame['normalized_file']), -1), cv2.COLOR_BGRA2RGBA)

    def test_new_clips_have_all_363_frames_and_common_reference_scale(self):
        self.assertEqual(sum(self.clips[name]['frame_count'] for name in NAMES), 363)
        idle_height = self.manifest['references']['idle']['bbox'][3]-self.manifest['references']['idle']['bbox'][1]
        for name in NAMES:
            c = self.clips[name]
            with self.subTest(name=name):
                self.assertEqual(c['source_name'], {'walk_forward':'walk_fward.mp4','walk_back':'walk_back.mp4','back_dash':'back_dash.mp4'}[name])
                self.assertNotEqual(c['status'], 'rejected')
                self.assertEqual(c['role'], name)
                self.assertEqual(c['scale_mode'], 'constant_per_video')
                self.assertEqual(len(list((self.root/name/'normalized').glob('*.png'))), c['frame_count'])
                self.assertEqual(c['root'], self.manifest['root'])
                self.assertEqual(c['canvas'], self.manifest['canvas'])
                for index in (0, c['reference_frame']):
                    b = bounds(self.read(c['frames'][index]))
                    self.assertLessEqual(abs(b[3]-b[1]-idle_height), 2)
                    self.assertLessEqual(abs(b[3]-c['root'][1]), 1)

    def test_recorded_transforms_match_actual_pngs_and_extract_only_horizontal_motion(self):
        for name in NAMES:
            c = self.clips[name]
            source_centroids, actual_centroids = [], []
            removed = c.get('root_motion', {}).get('removed_x_output_px', [0]*c['frame_count'])
            for f in c['frames']:
                with self.subTest(name=name, frame=f['index']):
                    matrix = np.asarray(f['transform_source_to_output'])
                    self.assertEqual(matrix[0, 0], c['scale_per_video'])
                    self.assertEqual(matrix[1, 2], c['frames'][0]['transform_source_to_output'][1][2])
                    raw = image_rgba(cv2.imread(str(self.root/f['raw_file']), -1), c['background_treatment'])
                    actual = self.read(f)[:, :, 3]
                    # Sparse extremities can disappear between downsampled pixels;
                    # bbox corners are not affine invariants. Check the complete
                    # rasterized alpha instead, allowing one alpha quantization step.
                    expected = cv2.warpAffine(raw[:, :, 3].astype(np.float32)/255, matrix,
                                              tuple(c['canvas']), flags=cv2.INTER_LINEAR,
                                              borderMode=cv2.BORDER_CONSTANT, borderValue=0)
                    expected = np.clip(np.rint(expected*255), 0, 255).astype(np.uint8)
                    self.assertLessEqual(int(np.abs(actual.astype(np.int16)-expected).max()), 1)
                    source_moments, actual_moments = cv2.moments(raw[:, :, 3]), cv2.moments(actual)
                    a = np.array([source_moments['m10'], source_moments['m01']])/source_moments['m00']
                    b = np.array([actual_moments['m10'], actual_moments['m01']])/actual_moments['m00']
                    np.testing.assert_allclose(b, a*matrix[0, 0]+matrix[:, 2], atol=.5)
                    source_centroids.append(a);actual_centroids.append(b)
            expected_motion = (np.asarray(source_centroids)-source_centroids[0])*matrix[0, 0]
            expected_motion[:, 0] -= np.asarray(removed)-removed[0]
            observed_motion = np.asarray(actual_centroids)-actual_centroids[0]
            np.testing.assert_allclose(observed_motion, expected_motion, atol=.5)

    def test_backward_walk_is_in_place_and_backdash_keeps_its_vertical_pose(self):
        walk = self.clips['walk_back']
        first, last = [bounds(self.read(walk['frames'][index])) for index in (0, -1)]
        self.assertLess(abs((last[0]+last[2]-first[0]-first[2])/2), 10)
        self.assertLess(walk['root_motion']['first_to_last_removed_x_px'], -100)
        from sprite_pipeline.root_motion import torso_anchor_x
        anchors = [torso_anchor_x(self.read(f)) for f in walk['frames']]
        self.assertLess(max(anchors)-min(anchors), 1)
        backdash = self.clips['back_dash']
        self.assertEqual(backdash['animation_type'], 'airborne')
        supports = [bounds(self.read(f))[3] for f in backdash['frames']]
        self.assertGreater(max(supports)-min(supports), 100)
        self.assertTrue(any('source_touches_edge_possible_clipping' in f['warnings'] for f in backdash['frame_warnings']))
        self.assertEqual([f['index'] for f in backdash['frame_warnings']], list(range(22, 31)))
        self.assertFalse(backdash.get('scale_correction'))

    def test_walking_loop_candidates_do_not_claim_a_static_hold(self):
        for name in ('walk_forward', 'walk_back'):
            c = self.clips[name]
            for candidate in c['loops']:
                start, period = candidate['start_frame'], candidate['period_frames']
                if name == 'walk_forward':self.assertGreaterEqual(start, 85)
                for offset in (start, start+period):
                    sequence = c['frames'][offset:offset+period]
                    first = self.read(sequence[0])
                    self.assertTrue(any(not np.array_equal(first, self.read(f)) for f in sequence[1:]))
                self.assertTrue(candidate['advisory_only'])

    def test_approved_nonlocomotion_geometry_and_timestamps_are_preserved(self):
        baseline = os.environ.get('SPRITE_MOVEMENT_BASELINE')
        if not baseline:
            self.skipTest('Defina SPRITE_MOVEMENT_BASELINE para comparar o lote aprovado')
        baseline = Path(baseline)
        old = json.loads((baseline/'manifest.json').read_text())
        delta = np.subtract(self.manifest['root'], old['root'])
        for previous in old['clips']:
            c = self.clips[previous['name']]
            self.assertEqual([f['pts'] for f in previous['frames']], [f['pts'] for f in c['frames']])
            if c.get('root_motion'):continue
            for a, b in zip(previous['frames'], c['frames']):
                expected = cv2.imread(str(baseline/a['normalized_file']), -1)
                actual = cv2.imread(str(self.root/b['normalized_file']), -1)
                expected = cv2.warpAffine(expected, np.array([[1.,0.,delta[0]],[0.,1.,delta[1]]]), tuple(self.manifest['canvas']))
                matrix = np.asarray(a['transform_source_to_output']).copy()
                matrix[:, 2] += delta
                np.testing.assert_allclose(b['transform_source_to_output'], matrix, atol=1e-12)
                # Moving the shared canvas origin can change OpenCV's global
                # fixed-point interpolation rounding by 1/32 source pixel.
                # Geometry stays identical; compare visible premultiplied pixels.
                def premultiplied(im):
                    im = im.astype(np.float32)/255
                    im[:, :, :3] *= im[:, :, 3:]
                    return im
                visible = (actual[:, :, 3] > 0) | (expected[:, :, 3] > 0)
                error = premultiplied(actual)-premultiplied(expected)
                self.assertLess(float(np.sqrt(np.mean(error[visible]**2))), 1/255)


if __name__ == '__main__':
    unittest.main()
