"""Scale-drift regressions with annotated segments, independent of pose height."""
import copy
import unittest
import numpy as np
from sprite_pipeline.corrections import frame_transforms
from sprite_pipeline.errors import CalibrationError
from sprite_pipeline.calibration import calibrate


class ScaleCorrectionTests(unittest.TestCase):
    def setUp(self):
        self.calibration = {'scale_per_video': .6, 'source_root': [100, 200], 'reference_frame': 0}
        self.metadata = {'source_sha256': 'known-original', 'resolution': [300, 300],
                         'frame_count': 3, 'frames': [{'time_s': t} for t in (0, .1, 1)]}
        self.clip = {'animation_type': 'crouched', 'scale_correction': {
            'segment_name': 'rigid', 'source_sha256': 'known-original', 'keyframes': [
                {'frame': 0, 'points': [[50, 50], [50, 100]], 'ground_y': 200},
                {'frame': 2, 'points': [[60, 80], [60, 140]], 'ground_y': 210}]}}

    def test_unannotated_clips_keep_the_original_constant_transform(self):
        matrices, report = frame_transforms({}, self.metadata, self.calibration)
        self.assertIsNone(report)
        for matrix in matrices:
            np.testing.assert_allclose(matrix, [[.6, 0, -60], [0, .6, -120]])

    def test_annotated_scale_drift_does_not_bypass_pose_quality(self):
        master = {'bbox': [0, 0, 60, 95], 'rgba': np.zeros((160, 140, 4), np.uint8)}
        a = {'bbox': [0, 0, 60, 95], 'warnings': [], 'score': .99,
             'silhouette': .99, 'appearance': .99, 'dimension_agreement': .99}
        b = {**a, 'bbox': [0, 0, 66, 105]}
        with self.assertRaisesRegex(CalibrationError, 'variação de escala'):
            calibrate([a, b], master, 95, {})
        result = calibrate([a, b], master, 95, {}, allow_scale_drift=True)
        self.assertGreater(result['peer_height_spread'], .06)
        with self.assertRaisesRegex(CalibrationError, 'Pose incerta'):
            calibrate([{**a, 'silhouette': .2}], master, 95, {}, allow_scale_drift=True)

    def test_segment_output_size_is_constant_even_when_pose_height_changes(self):
        matrices, report = frame_transforms(self.clip, self.metadata, self.calibration)
        # Use original timestamps (0.1 s), not the middle frame index (0.5).
        self.assertAlmostEqual(matrices[1][0][0], 30/51)
        for length, matrix in zip((50, 51, 60), matrices):
            self.assertAlmostEqual(length*matrix[0][0], 30)
            self.assertEqual(matrix[0][0], matrix[1][1])
            self.assertEqual(matrix[0][1], 0)
            self.assertEqual(matrix[1][0], 0)
        self.assertEqual(report['vertical_anchor'], 'annotated_ground')
        self.assertEqual(report['source_sha256'], 'known-original')

    def test_annotated_floor_stays_fixed_without_recentering_horizontal_motion(self):
        matrices, _ = frame_transforms(self.clip, self.metadata, self.calibration)
        for y, matrix in zip((200, 201, 210), matrices):
            s = matrix[0][0]
            self.assertAlmostEqual(matrix[1][1]*y+matrix[1][2], 0)
            # A point 20 px right of the fixed root remains 20*s right of it.
            self.assertAlmostEqual(matrix[0][0]*120+matrix[0][2], 20*s)

    def test_scale_only_correction_preserves_vertical_motion(self):
        for key in self.clip['scale_correction']['keyframes']:
            del key['ground_y']
        matrices, report = frame_transforms(self.clip, self.metadata, self.calibration)
        self.assertEqual(report['vertical_anchor'], 'fixed_calibration_root')
        for matrix in matrices:
            self.assertAlmostEqual(matrix[1][1]*180+matrix[1][2], -20*matrix[0][0])

    def test_another_source_video_is_rejected(self):
        self.metadata['source_sha256'] = 'another-video'
        with self.assertRaisesRegex(CalibrationError, 'SHA-256'):
            frame_transforms(self.clip, self.metadata, self.calibration)

    def test_no_silent_extrapolation_or_duplicate_keyframes(self):
        for indices in ([1, 2], [0, 1], [0, 2, 2], [0, -1, 2], [False, 2], [0, 8, 2]):
            with self.subTest(indices=indices):
                clip = copy.deepcopy(self.clip)
                clip['scale_correction']['keyframes'] = [
                    {'frame': i, 'points': [[50, 50], [50, 100]]} for i in indices]
                with self.assertRaises(CalibrationError):
                    frame_transforms(clip, self.metadata, self.calibration)

    def test_reference_pose_must_be_measured(self):
        self.calibration['reference_frame'] = 1
        with self.assertRaisesRegex(CalibrationError, 'pose de calibração'):
            frame_transforms(self.clip, self.metadata, self.calibration)

    def test_invalid_or_tiny_landmarks_are_rejected(self):
        for points in ([[[-1, 50], [50, 100]]], [[50, 50], [50, 50]],
                       [[300, 50], [50, 100]], [[50, float('nan')], [50, 100]],
                       'invalid', None):
            with self.subTest(points=points):
                clip = copy.deepcopy(self.clip)
                clip['scale_correction']['keyframes'][0]['points'] = points
                with self.assertRaises(CalibrationError):
                    frame_transforms(clip, self.metadata, self.calibration)

    def test_excessive_correction_is_rejected(self):
        self.clip['scale_correction']['keyframes'][1]['points'] = [[50, 20], [50, 200]]
        with self.assertRaisesRegex(CalibrationError, 'variação máxima'):
            frame_transforms(self.clip, self.metadata, self.calibration)

    def test_floor_correction_cannot_flatten_a_jump_or_dash(self):
        for animation_type in ('airborne', 'translation'):
            self.clip['animation_type'] = animation_type
            with self.assertRaisesRegex(CalibrationError, 'salto ou deslocamento'):
                frame_transforms(self.clip, self.metadata, self.calibration)

    def test_partial_invalid_or_incompatible_floor_annotations_are_rejected(self):
        for value in (None, -1, 301, float('nan'), 250):
            clip = copy.deepcopy(self.clip)
            if value is None:
                del clip['scale_correction']['keyframes'][0]['ground_y']
            else:
                clip['scale_correction']['keyframes'][0]['ground_y'] = value
            with self.subTest(value=value), self.assertRaises(CalibrationError):
                frame_transforms(clip, self.metadata, self.calibration)

    def test_inputs_are_preserved(self):
        originals = copy.deepcopy((self.clip, self.metadata, self.calibration))
        frame_transforms(self.clip, self.metadata, self.calibration)
        self.assertEqual((self.clip, self.metadata, self.calibration), originals)


if __name__ == '__main__':
    unittest.main()
