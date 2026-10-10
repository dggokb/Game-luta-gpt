"""The production source reader cannot replace a video frame with new art."""
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import p12_video_cycle as V


class OriginalFrameProofTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.override = patch.object(V, 'ROOT', self.root)
        self.override.start()
        self.path = self.root / 'art/keys/p12/poses/light_jab_start.png'
        self.path.parent.mkdir(parents=True)
        self.original = Image.new('RGBA', (12, 10), (180, 130, 100, 255))
        self.original.save(self.path)
        self.record = {'sha256': V.sha(self.path), 'sourceBox': [70, 100, 82, 110], 'time': 2.25}
        self.origin = {'driveId': 'original-drive-video', 'videoSha256': 'recorded-video-hash'}

    def tearDown(self):
        self.override.stop()
        self.temp.cleanup()

    def load(self, frame_spec=None):
        return V.original_frame('LIGHT_JAB', 54, 0, 2, self.origin, self.record, frame_spec or {})

    def test_actual_source_pixels_are_read(self):
        im, refs, recipe = self.load()
        self.assertEqual(im.tobytes(), self.original.tobytes())
        self.assertEqual(refs, ['art/keys/p12/poses/light_jab_start.png'])
        self.assertEqual(recipe['videoFrame'], 54)
        self.assertEqual(recipe['sourceTimeSeconds'], 2.25)

    def test_new_drawing_with_video_metadata_is_rejected(self):
        Image.new('RGBA', self.original.size, (90, 20, 170, 255)).save(self.path)
        with self.assertRaisesRegex(ValueError, 'not the recorded original'):
            self.load()

    def test_mask_only_removes_alpha_and_keeps_body_rgb(self):
        relative = 'art/keys/p12/video_masks/light_jab/0054.png'
        mask_path = self.root / relative
        mask_path.parent.mkdir(parents=True)
        mask = np.full((10, 12), 255, dtype=np.uint8)
        mask[:, :3] = 0
        Image.fromarray(mask).save(mask_path)
        spec = {'alphaMask': {'path': relative, 'sha256': V.sha(mask_path)}}
        im, refs, recipe = self.load(spec)
        pixels = np.asarray(im)
        self.assertTrue((pixels[:, :3] == 0).all())
        self.assertTrue((pixels[:, 3:] == np.asarray(self.original)[:, 3:]).all())
        self.assertIn(relative, refs)

    def test_stale_mask_is_rejected(self):
        relative = 'art/keys/p12/video_masks/light_jab/0054.png'
        path = self.root / relative
        path.parent.mkdir(parents=True)
        Image.new('L', self.original.size, 255).save(path)
        with self.assertRaisesRegex(ValueError, 'mask provenance'):
            self.load({'alphaMask': {'path': relative, 'sha256': 'stale-mask'}})


class BatchGateTest(unittest.TestCase):
    def test_full_gate_still_rejects_a_pending_state(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            contract = {k:{} for k in ['fighter','moves','specialMoves','specialAnimations']}
            contract['animations'] = {'IDLE':{'frames':[0]},'LIGHT_JAB':{'frames':[0,1]}}
            pack_path = root/'characters/p12/character.json'
            V.write_json(pack_path,contract)
            contract_path = root/'contract.json'
            V.write_json(contract_path,contract)
            source = root/'art/keys/p12/poses/idle_start.png'
            source.parent.mkdir(parents=True)
            Image.new('RGBA',(12,10),'white').save(source)
            origin = {'driveId':'idle-video','videoSha256':'original-sha','selectedVideoFrames':[0]}
            record = {'sha256':V.sha(source),'sourceBox':[10,20,22,30],'time':0}
            V.write_json(root/'docs/art/p12-video-sources/idle.frames.json',{'videoSha256':'original-sha','frames':[record]})
            V.write_json(root/'tools/sprites/videos/p12-drive-inventory.json',{'files':[
                {'name':'idle.mp4','mimeType':'video/mp4','id':'idle-video'},
                {'name':'L.mp4','mimeType':'video/mp4','id':'light-video'}]})
            registration_path = root/'registration.json'
            V.write_json(registration_path,{'sourcePolicy':'EXISTING_DRIVE_VIDEOS_ONLY','states':{
                'IDLE':{'videoOrigin':origin,'mainIndex':0,'frames':[{'videoFrame':0,'root':[6,9],'scale':1}],
                    'scaleEvidence':'measured source head','review':{'status':'reviewed','findings':[],
                    'movementEvidence':'real breathing','transitionsReviewed':True,'supportReviewed':True,'alphaReviewed':True}},
                'LIGHT_JAB':{'review':{'status':'pending'}}}})
            with patch.object(V,'ROOT',root), patch.object(V,'REGISTRATION',registration_path), patch.object(V,'CONTRACT',contract_path):
                self.assertEqual(set(V.preflight(['IDLE'])[2]),{'IDLE'})
                with self.assertRaisesRegex(ValueError,'review incomplete'):
                    V.preflight()
                with self.assertRaisesRegex(ValueError,'review incomplete'):
                    V.preflight(['LIGHT_JAB'])
                with self.assertRaisesRegex(ValueError,'existing P12 states'):
                    V.preflight(['FAKE'])


class VideoCanvasTest(unittest.TestCase):
    def test_runtime_cannot_point_at_old_idle_atlas_using_video_references(self):
        cfg = {'source':'art/sprites/source/p12_full_idle_sr2x.png',
               'sourceReferences':['art/keys/p12/poses/light_jab_peak.png']}
        with self.assertRaisesRegex(ValueError,'binding.*source'):
            V.verify_clip_binding('LIGHT_JAB',cfg,{},10)

    def test_air_anchor_preserves_pixels_with_room_below_engine_root(self):
        source = Image.new('RGBA', (40, 160), (180, 130, 100, 255))
        result = V.render_pose(source, 1, [20, 100], [320, 256])
        self.assertEqual(result.crop((300,156,340,316)).tobytes(), source.tobytes())
        self.assertGreater(result.getchannel('A').getbbox()[3],256)

    def test_extended_pose_uses_logical_canvas_and_uniform_pixels(self):
        source = Image.new('RGBA', (280, 250), (180, 130, 100, 255))
        result = V.render_pose(source, 1, [90, 249])
        self.assertEqual(result.size, (768, 384))
        self.assertEqual(result.crop((230,117,510,367)).tobytes(), source.tobytes())

    def test_clipped_pose_is_rejected_instead_of_rescaled(self):
        with self.assertRaisesRegex(ValueError, 'clipped'):
            V.render_pose(Image.new('RGBA',(900,250),'white'), 1, [90,249])


class CroppedRefinementTest(unittest.TestCase):
    def test_crop_matches_full_model_on_visible_pixels_and_exact_alpha(self):
        import importlib.util
        import torch
        from scipy.ndimage import distance_transform_edt
        torch.set_num_threads(1)
        torch.manual_seed(1)
        spec = importlib.util.spec_from_file_location('test_vgg', V.ROOT / 'art/redraws/p03_hd/idle_sr_4x/srvgg_arch.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        # The same convolution depth/receptive field as the real SR engine.
        model = module.SRVGGNetCompact(num_feat=8,num_conv=16,upscale=4,act_type='prelu').eval()
        frame = Image.new('RGBA',(140,130))
        frame.paste(Image.new('RGBA',(28,30),(170,130,90,255)),(54,50))
        actual = V.refine_video_frame(model,frame)
        pixels = np.array(frame)
        invisible = pixels[...,3] < 128
        _, indices = distance_transform_edt(invisible, return_indices=True)
        rgb = pixels[indices[0],indices[1],:3]
        tensor = torch.from_numpy(np.ascontiguousarray(rgb.transpose(2,0,1))).float()[None]/255
        with torch.inference_mode():
            prediction = model(tensor)[0].clamp(0,1).permute(1,2,0).numpy()
        full = Image.fromarray(np.rint(prediction*255).astype(np.uint8))
        full.putalpha(frame.getchannel('A').resize((560,520),Image.Resampling.LANCZOS))
        full = full.convert('RGBa').resize((280,260),Image.Resampling.LANCZOS).convert('RGBA')
        alpha = frame.getchannel('A').resize((280,260),Image.Resampling.LANCZOS)
        self.assertEqual(actual.getchannel('A').tobytes(),alpha.tobytes())
        visible = np.array(alpha)>0
        difference = np.abs(np.array(actual).astype(int)[...,:3]-np.array(full).astype(int)[...,:3])
        self.assertLessEqual(difference[visible].max(),1)


if __name__ == '__main__':
    unittest.main()
