"""Registration regressions: limb extension, per-frame scale and game carry."""
import unittest
import numpy as np
from sprite_pipeline.root_motion import torso_anchor_x, register_anchors
from sprite_pipeline.errors import CalibrationError


class RootMotionTests(unittest.TestCase):
    def test_stepping_foot_does_not_change_torso_anchor(self):
        pose = np.zeros((120, 160, 4), np.uint8)
        pose[10:100, 60:90, 3] = 255
        extended = pose.copy()
        extended[85:100, 90:150, 3] = 255
        self.assertEqual(torso_anchor_x(pose), torso_anchor_x(extended))

    def test_registers_translation_without_changing_scale_or_vertical_motion(self):
        matrices = [[[s, 0, -100*s], [0, s, -200*s]] for s in (.6, .7, .8)]
        before = np.array(matrices).copy()
        anchors = [110, 85, 50]
        result, report = register_anchors(anchors, matrices, 1, 'original-hash')
        positions = [m[0][0]*x+m[0][2] for m, x in zip(result, anchors)]
        np.testing.assert_allclose(positions, [positions[1]]*3)
        np.testing.assert_array_equal(np.array(result)[:, 1], before[:, 1])
        np.testing.assert_array_equal(np.array(result)[:, 0, :2], before[:, 0, :2])
        np.testing.assert_array_equal(matrices, before)
        self.assertEqual(report['removed_x_output_px'][1], 0)
        self.assertEqual(report['source_sha256'], 'original-hash')
        for a, b, dx in zip(before, result, report['removed_x_output_px']):
            self.assertAlmostEqual(a[0][2]-b[0][2], dx)

    def test_invalid_trajectory_is_rejected(self):
        with self.assertRaises(CalibrationError):
            register_anchors([float('nan')], [[[1, 0, 0], [0, 1, 0]]], 0, 'hash')

    def test_shared_constant_matrix_is_not_mutated_or_accumulated(self):
        matrix = [[.6, 0, -60], [0, .6, -120]]
        matrices, report = register_anchors([100, 90, 80], [matrix]*3, 0, 'hash')
        np.testing.assert_allclose([m[0][2] for m in matrices], [-60, -54, -48])
        self.assertEqual(matrix, [[.6, 0, -60], [0, .6, -120]])
        self.assertEqual(report['first_to_last_removed_x_px'], -12)
