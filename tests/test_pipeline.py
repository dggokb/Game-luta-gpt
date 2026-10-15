"""Pure raster tests. Integration uses existing real footage, never mock videos."""
import json
import tempfile
import unittest
from pathlib import Path
import cv2
import numpy as np
from sprite_standardizer import CalibrationError, process, similarity
from sprite_pipeline.imaging import image_rgba, bounds, affine_rgba, shape_feature
from sprite_pipeline.calibration import load_master, master_coherence, compare_pose, calibrate
from sprite_pipeline.loops import loop_candidates
from sprite_pipeline.viewer import write_viewer


def raster():
    # A generic asymmetric test mask, not an exported character or animation.
    im = np.zeros((160, 140, 4), np.uint8)
    im[35:130, 40:70] = [160, 80, 30, 255]
    im[60:75, 70:100] = [160, 80, 30, 255]
    return im


def master(im):
    b = bounds(im)
    from sprite_pipeline.imaging import appearance_feature
    return {'rgba': im, 'bbox': b, 'feature': shape_feature(im, b),
            'appearance': appearance_feature(im, b)}


class Tests(unittest.TestCase):
    def test_similarity_self(self):
        m = shape_feature(raster(), bounds(raster()))
        self.assertEqual(similarity(m, m), 1)

    def test_master_coherence_requires_anatomical_evidence(self):
        m = master(raster())
        self.assertEqual(master_coherence({'idle': m, 'crouch': m}, [])['status'], 'needs_review')

    def test_master_scale_mismatch_is_not_repaired_by_pose_height(self):
        m = master(raster())
        segments = [{'name': 'rigid', 'idle': [[40, 40], [60, 40]], 'crouch': [[40, 40], [70, 40]]}]
        report = master_coherence({'idle': m, 'crouch': m}, segments)
        self.assertEqual(report['status'], 'needs_review')
        self.assertFalse(report['correction_applied'])

    def test_negative_landmarks_rejected(self):
        m = master(raster())
        with self.assertRaises(CalibrationError):
            master_coherence({'idle': m, 'crouch': m}, [{'name': 'bad', 'idle': [[-1, 40], [60, 40]], 'crouch': [[40, 40], [70, 40]]}])

    def test_visual_scale_and_uniform_transform(self):
        image = raster()
        large = affine_rgba(image, 1.5, [0, 0], [250, 250])
        ref = master(image)
        frames = []
        for im in (large,):
            b = bounds(im)
            frames.append({'bbox': b, 'warnings': [], **compare_pose(im, b, ref)})
        cal = calibrate(frames, ref, 95, {}, .8)
        self.assertAlmostEqual(cal['scale_per_video'], 1/1.5, delta=.01)
        normalized = affine_rgba(large, cal['scale_per_video'], [0, 0], [250, 250])
        b = bounds(normalized)
        self.assertLessEqual(abs(b[3]-b[1]-95), 2)
        self.assertLessEqual(abs(b[2]-b[0]-60), 2)

    def test_horizontal_and_vertical_displacement_preserved(self):
        source = raster()
        moved = affine_rgba(source, 1, [18, -12], [140, 160])
        a = bounds(affine_rgba(source, 1.5, [8, 20], [300, 300]))
        b = bounds(affine_rgba(moved, 1.5, [8, 20], [300, 300]))
        self.assertEqual([b[i]-a[i] for i in range(4)], [27, -18, 27, -18])
        self.assertEqual([a[2]-a[0], a[3]-a[1]], [b[2]-b[0], b[3]-b[1]])

    def test_alpha_resampling_does_not_add_green_fringe(self):
        source = raster()
        source[source[:, :, 3] == 0, :3] = [0, 255, 0]
        transformed = affine_rgba(source, .73, [.2, .7], [140, 160])
        edge = (transformed[:, :, 3] > 0) & (transformed[:, :, 3] < 255)
        self.assertTrue(edge.any())
        self.assertLessEqual(int(transformed[:, :, 1][edge].max()), 81)

    def test_green_residuals_removed_without_deleting_detached_art(self):
        raw = np.zeros((160, 140, 3), np.uint8)
        raw[:] = [75, 175, 110]
        raw[35:130, 40:70] = [30, 80, 160]
        raw[20:30, 100:110] = [30, 80, 160]
        im = image_rgba(raw)
        self.assertEqual(int(im[0, 0, 3]), 0)
        self.assertEqual(int(im[25, 105, 3]), 255)
        self.assertEqual(bounds(im), [40, 20, 110, 130])

    def test_background_failure_never_approved(self):
        with self.assertRaises(CalibrationError):
            bounds(image_rgba(np.full((100, 100, 3), 100, np.uint8)))

    def test_wrong_pose_and_explicit_out_of_range_rejected(self):
        ref = master(raster())
        wrong = np.zeros((160, 140, 4), np.uint8)
        wrong[80:90, 10:130] = [160, 80, 30, 255]
        b = bounds(wrong)
        frames = [{'bbox': b, 'warnings': [], **compare_pose(wrong, b, ref)}]
        with self.assertRaises(CalibrationError):
            calibrate(frames, ref, 95, {}, .82)
        with self.assertRaises(CalibrationError):
            calibrate(frames, ref, 95, {'reference_frame': 2}, .82)

    def test_in_video_zoom_ambiguity_rejected(self):
        ref = master(raster())
        a = {'bbox': [0, 0, 60, 95], 'warnings': [], 'score': .99, 'silhouette': .99, 'appearance': .99, 'dimension_agreement': .99}
        b = {**a, 'bbox': [0, 0, 90, 142]}
        with self.assertRaises(CalibrationError):
            calibrate([a, b], ref, 95, {}, .82)

    def test_static_hold_is_not_claimed_as_a_cycle(self):
        feature = shape_feature(raster(), bounds(raster()))
        frames = [{'feature': feature, 'bbox': [40, 35, 100, 130], 'warnings': []} for _ in range(40)]
        self.assertEqual(loop_candidates(frames), [])

    def test_cycle_sequence_with_leadin_and_small_noise(self):
        # Feature masks only, no generated video/animation fixture.
        features = []
        for width in (8, 16, 24, 30, 24, 16):
            f = np.zeros((96, 96), np.uint8); f[20:70, 30:30+width] = 1; features.append(f)
        lead = np.zeros((96, 96), np.uint8); lead[5:15, 4:70] = 1
        frames = [{'feature': f, 'bbox': [20, 20, 70, 80], 'warnings': []} for f in [lead, lead]+features*3]
        found = loop_candidates(frames, minimum_period=6)
        self.assertTrue(any(c['period_frames'] == 6 and c['start_frame'] >= 2 for c in found))
        self.assertTrue(all(c['advisory_only'] for c in found))
        self.assertEqual(len(frames), 20)

    def test_html_escapes_script_injection(self):
        with tempfile.TemporaryDirectory() as tmp:
            write_viewer(Path(tmp), {'name': '</script><script>alert(1)</script>'})
            html = (Path(tmp)/'index.html').read_text()
            self.assertNotIn('"</script><script>alert(1)', html)

    def test_nonempty_output_is_preserved(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); out = root/'out'; out.mkdir()
            marker = out/'existing.png'; marker.write_bytes(b'original')
            config = root/'config.json'
            config.write_text(json.dumps({'references': {'idle':'missing.png'}, 'clips':[{'name':'clip','video':'missing.mp4'}]}))
            with self.assertRaisesRegex(CalibrationError, 'nova ou vazia'):
                process(config, out)
            self.assertEqual(marker.read_bytes(), b'original')

    def test_output_cannot_contain_original_inputs(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); config = root/'config.json'
            config.write_text(json.dumps({'references': {'idle':'original.png'}, 'clips':[{'name':'clip','video':'original.mp4'}]}))
            with self.assertRaisesRegex(CalibrationError, 'conter fontes'):
                process(config, root)

    def test_master_directory_name_is_reserved(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); config = root/'config.json'
            config.write_text(json.dumps({'references': {'idle':'missing.png'}, 'clips':[{'name':'masters','video':'missing.mp4'}]}))
            with self.assertRaisesRegex(CalibrationError, 'reservado'):
                process(config, root/'out')

    def test_missing_idle(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            (path/'config.json').write_text(json.dumps({'references': {}, 'clips': []}))
            with self.assertRaises(CalibrationError):
                process(path/'config.json', path/'out')


if __name__ == '__main__':
    unittest.main()
