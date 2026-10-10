"""Real engine behavior tested against known motion, not only route labels."""
import importlib.util
from pathlib import Path
import tempfile
import json
import sys
import unittest
from unittest.mock import patch
import numpy as np

SCRIPTS=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(SCRIPTS))
import estabilizador as e
SPEC=importlib.util.spec_from_file_location("refinar_elenco",SCRIPTS/"refinar_elenco.py")
MOD=importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MOD)


def body_frame(x=75,head=20,torso=44,height=100):
    return {"torso_x":float(x),"shoe_x":float(x+2),
            "head_proxy":float(head),"torso_proxy":float(torso),
            "height":float(height),"confidence":"PROXY_SILHUETA"}


class CharacterStabilizerTest(unittest.TestCase):
    def test_no_roster_argument_and_scope_validation(self):
        with self.assertRaises(ValueError):
            MOD.inspect_character("p01,p02")
        with self.assertRaises(ValueError):
            MOD.inspect_character("p13")
        self.assertEqual("player_base",MOD.character_pack_id("p01"))
        self.assertEqual("p12",MOD.character_pack_id("p12"))

    def test_real_aliases_from_p01_p02_and_p12_drive(self):
        with tempfile.TemporaryDirectory() as d:
            for filename in ["idle.mp4","m2(rasteira).mp4","back_dash.mp4",
                             "vitoria.mp4","jump.mp4","2H.mp4","defesa_pulo.MP4"]:
                (Path(d)/filename).touch()
            mapped=e.match_videos(d,["IDLE","CROUCH_MEDIUM","BACKDASH",
                                      "VICTORY","JUMP","FALL","CROUCH_HEAVY",
                                      "DEFENSE_AIR"])
            self.assertTrue(all(x["status"]=="ENCONTRADO" for x in mapped.values()))
            self.assertTrue(mapped["CROUCH_MEDIUM"]["files"][0].endswith("m2(rasteira).mp4"))

    def test_game_commands_are_resolved_to_actual_animation_states(self):
        self.assertEqual("CROUCH_MEDIUM",MOD.canonical_state("2M"))
        self.assertEqual("CROUCH_MEDIUM",MOD.canonical_state("2m"))
        self.assertEqual("JUMP_HEAVY",MOD.canonical_state("jH"))
        self.assertEqual("JUMP_HEAVY_DOWN",MOD.canonical_state("jh_baixo"))
        self.assertEqual("VICTORY",MOD.canonical_state("victory"))

    def test_jump_video_is_split_by_apex_and_never_assumed(self):
        # Foreground clearly rises, then descends; original jump.mp4 is
        # shared by both character states and MUST be segmented.
        ys=[105,95,85,76,66,55,44,38,43,51,62,75,88,102,110]
        samples=[dict(body_frame(),shoe_y=float(y)) for y in ys]
        up=e.jump_phase(samples,"JUMP")
        down=e.jump_phase(samples,"FALL")
        self.assertTrue(len(up)>4 and len(down)>4)
        self.assertLess(up[-1]["shoe_y"],up[0]["shoe_y"])
        self.assertGreater(down[-1]["shoe_y"],down[0]["shoe_y"])
        stuck=[dict(body_frame(),shoe_y=100.0)]*15
        self.assertIsNone(e.jump_phase(stuck,"FALL"))

    def test_ambiguous_sources_never_choose_random_file(self):
        with tempfile.TemporaryDirectory() as d:
            (Path(d)/"dash.mp4").touch()
            (Path(d)/"DASH.mp4").touch()
            result=e.match_videos(d,["DASH"])
            self.assertEqual("AMBIGUO",result["DASH"]["status"])

    def test_stable_trajectory_passes_and_isolated_bounce_is_detected(self):
        smooth=[body_frame(x=65+i*3) for i in range(15)]
        report=e.metrics(smooth,100,"DASH")
        self.assertEqual("MEDIDO",report["status"])
        self.assertFalse(report["tremor"],report)
        jitter=[body_frame(x=75+(11 if i in (5,9) else 0)) for i in range(15)]
        report=e.metrics(jitter,100,"VICTORY")
        self.assertEqual([5,9],report["tremor"]["torso"])
        self.assertEqual([5,9],report["tremor"]["pe_apoio"])

    def test_gaps_are_not_silently_joined_across_missing_frames(self):
        smooth=[body_frame(x=75) for _ in range(16)]
        smooth[6]=None
        smooth[7]=None
        result=e.metrics(smooth,100,"VICTORY")
        self.assertEqual("MEDIDO_COM_LACUNAS",result["status"])
        self.assertEqual([6,7],result["quadros_invalidos"])
        self.assertEqual([],result["tremor"].get("torso",[]))
        self.assertEqual([],result["tremor"].get("pe_apoio",[]))

    def test_repeated_foot_wobble_does_not_escape_median_filter(self):
        alternating=[dict(body_frame(),shoe_x=40+8*(i%2))
                     for i in range(18)]
        motion=e.metrics(alternating,100,"VICTORY")
        self.assertTrue(motion["tremor"].get("pe_apoio"),motion)
        legitimate=e.metrics(alternating,100,"DASH")
        self.assertFalse(legitimate["tremor"].get("pe_apoio"),legitimate)

    def test_slow_victory_foot_drift_detected(self):
        samples=[dict(body_frame(),shoe_x=40+i*.95) for i in range(30)]
        self.assertEqual("SUSPEITA_DE_DESLIZE",e.metrics(samples,100,"VICTORY")["deslize"]["status"])
        self.assertIsNone(e.metrics(samples,100,"DASH")["deslize"])
        self.assertIsNone(e.metrics([dict(body_frame(),shoe_x=40.)]*30,100,"VICTORY")["deslize"])

    def test_pose_change_cannot_be_mistaken_for_small_character(self):
        idle=e.metrics([body_frame(head=20,torso=40)]*12,100,"IDLE")
        changed=e.metrics([body_frame(head=25,torso=40)]*12,100,"CROUCH_HEAVY")
        self.assertEqual("INCONCLUSIVO",e.compare_body(idle,changed)["status"])
        small=e.metrics([body_frame(head=16,torso=32)]*12,100,"DASH")
        self.assertEqual("SUSPEITA_DE_ESCALA",e.compare_body(idle,small)["status"])

    def test_no_frames_means_inconclusive_not_approved(self):
        self.assertEqual("INCONCLUSIVO",e.metrics([None]*12,100,"VICTORY")["status"])

    def test_character_report_uses_both_video_and_packed_idle_references(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            p=root/"characters"/"player_base"
            p.mkdir(parents=True)
            (p/"character.json").write_text(json.dumps({"animations":{
                "IDLE":{"atlas":"test_idle","frames":list(range(12))},
                "DASH":{"atlas":"test_dash","frames":list(range(12))}}}))
            keys=root/"art/keys"/"p01"
            keys.mkdir(parents=True)
            (keys/"tamanho.json").write_text(json.dumps({
                "altura":224,"chaves":{"inicio_centro":.5}}))
            # Verified chroma-key image: body 448px tall at x0.5 -> 224px.
            import cv2
            base=np.full((600,550,3),(0,255,0),dtype=np.uint8)
            base[70:518,180:315]=(25,45,125)
            self.assertTrue(cv2.imwrite(str(keys/"inicio_centro.png"),base))
            reference=MOD.calibration_info(root,"p01")
            self.assertEqual("CALIBRADO",reference["status"])
            self.assertAlmostEqual(224,reference["altura_observada"],delta=.01)
            videos=root/"animations"/"p01"
            videos.mkdir(parents=True)
            (videos/"idle.mp4").touch()
            (videos/"dash.mp4").touch()
            counts={}
            def read_video(path):
                counts[str(path)]=counts.get(str(path),0)+1
                return {"status":"EXTRAIDO"},[body_frame(head=20,torso=40)]*12
            def read_pack(root,pack,state):
                return {"status":"EXTRAIDO","atlas":"test_"+state.lower()},[
                    body_frame(head=20,torso=40)]*12
            result=MOD.inspect_character("p01",videos,states=["DASH"],
                    root=root,original_video_inspector=read_video,
                    packed_inspector=read_pack,geometria=False)
            self.assertEqual("DIAGNOSTICO_PRELIMINAR",result["status"])
            self.assertFalse(result["aprovado_automaticamente"])
            self.assertEqual("COMPATIVEL",result["estados"]["DASH"]["proporcao_video"]["status"])
            self.assertEqual("COMPATIVEL",result["estados"]["DASH"]["proporcao_sprite"]["status"])
            self.assertEqual("INCONCLUSIVO",result["estados"]["DASH"]["prova_geometrica"]["status"])
            # Metadata and silhouette alone never prove size; a proven
            # geometric comparison must be supplied independently.
            result_geo=MOD.inspect_character("p01",videos,states=["DASH"],
                    root=root,original_video_inspector=read_video,
                    packed_inspector=read_pack,geometria=True,
                    geometry_inspector=lambda cid,state:{
                        "status":"PADRONIZADA_FONTE_ATLAS",
                        "diferenca_relativa":.015,
                        "validacao_runtime":"PENDENTE"})
            self.assertEqual("MEDICOES_CONCLUIDAS",result_geo["status"])
            self.assertEqual(["DASH"],result_geo["padronizacao_comprovada_nos_estados"])
            self.assertFalse(result_geo["aprovado_automaticamente"])
            counts.clear()
            # IDLE is needed as reference and may also be a requested state.
            # Its original MP4 must only be decoded once per invocation.
            MOD.inspect_character("p01",videos,states=["IDLE","DASH"],
                    root=root,original_video_inspector=read_video,
                    packed_inspector=read_pack,geometria=False)
            self.assertEqual(1,counts.get(str(videos/"idle.mp4")))
            self.assertEqual(1,counts.get(str(videos/"dash.mp4")))
            (videos/"dash.mp4").unlink()
            result=MOD.inspect_character("p01",videos,states=["DASH"],
                    root=root,original_video_inspector=read_video,
                    packed_inspector=read_pack,geometria=False)
            self.assertEqual("PENDENTE_FONTES",result["status"])

    def test_calibration_rejects_false_image_and_wrong_scale(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            keys=root/"art/keys/p01"
            keys.mkdir(parents=True)
            conf=keys/"tamanho.json"
            conf.write_text(json.dumps({"altura":224,"chaves":{"inicio_centro":.5}}))
            (keys/"inicio_centro.png").write_bytes(b"not a png")
            self.assertEqual("IMAGEM_BASE_INVALIDA",
                             MOD.calibration_info(root,"p01")["status"])
            import cv2
            base=np.full((600,550,3),(0,255,0),dtype=np.uint8)
            base[70:518,180:315]=(25,45,125)
            cv2.imwrite(str(keys/"inicio_centro.png"),base)
            conf.write_text(json.dumps({"altura":300,"chaves":{"inicio_centro":.5}}))
            self.assertEqual("REFERENCIA_INCONSISTENTE",
                             MOD.calibration_info(root,"p01")["status"])

    def test_atlas_proxies_use_same_pixel_scale_as_geometry(self):
        import cv2
        from PIL import Image
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            reports=root/"tools/sprites/reports"
            images=root/"android/app/src/main/res/drawable-nodpi"
            reports.mkdir(parents=True)
            images.mkdir(parents=True)
            pack={"animations":{
                "IDLE":{"atlas":"mock_idle","frames":[0,1,2,3,4,5]},
                "DASH":{"atlas":"mock_dash","frames":[0,1,2,3,4,5]}}}
            for atlas,ps in [("mock_idle",2),("mock_dash",1)]:
                h,w=140,140
                rgba=np.zeros((h,w*6,4),dtype=np.uint8)
                # Same physical silhouette. Source DASH at half pixelScale
                # must be enlarged to idle's pixel coordinate space.
                for frame in range(6):
                    size=100 if ps==2 else 50
                    left=15 if ps==2 else 8
                    top=5 if ps==2 else 3
                    x0=frame*w+left
                    rgba[top:top+size,x0:x0+size,3]=255
                    rgba[top:top+size,x0:x0+size,:3]=180
                Image.fromarray(rgba).save(images/(atlas+".png"))
                (reports/(atlas+".report.json")).write_text(json.dumps({
                    "packed":{"pixelScale":ps,"frameWidth":140,"frameHeight":140,
                              "columns":6,"frameCount":6}}))
            idle_meta,idle=e.load_atlas_state(root,pack,"IDLE")
            dash_meta,dash=e.load_atlas_state(root,pack,"DASH")
            self.assertEqual("EXTRAIDO",idle_meta["status"])
            self.assertEqual("EXTRAIDO",dash_meta["status"])
            self.assertEqual(2.,dash_meta["pixel_scale_normalizado"])
            self.assertAlmostEqual(100.,np.median([x["height"] for x in idle]))
            self.assertAlmostEqual(100.,np.median([x["height"] for x in dash]))
            # Declared frameCount may lie about physical PNG rows. Even an
            # invalid frame omitted by sampling must reject the entire clip.
            report=reports/"mock_dash.report.json"
            metadata=json.loads(report.read_text())
            metadata["packed"]["frameCount"]=7
            report.write_text(json.dumps(metadata))
            pack["animations"]["DASH"]["frames"]=[0,1,2,3,4,6]
            self.assertNotEqual("EXTRAIDO",
                e.load_atlas_state(root,pack,"DASH")[0]["status"])
            import verificar_escala as proof
            self.assertEqual([],proof.read_sprite_samples(root,pack,"DASH"))
            metadata["packed"]["frameCount"]=6
            report.write_text(json.dumps(metadata))
            # Negative atlas indices must not wrap around to another frame.
            pack["animations"]["DASH"]["frames"]=[-1]
            self.assertEqual("FRAME_FORA_DO_ATLAS",
                             e.load_atlas_state(root,pack,"DASH")[0]["status"])
            pack["animations"]["DASH"]["frames"]=list(range(151))
            pack["animations"]["DASH"]["frames"][1]=500
            # Even with a low sample limit that skips frame 1, this must fail.
            self.assertEqual("FRAME_FORA_DO_ATLAS",
                             e.load_atlas_state(root,pack,"DASH",limit=1)[0]["status"])

    def test_reference_rejects_nan_text_negative_and_fake_background(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            folder=root/"art/keys/p01"
            folder.mkdir(parents=True)
            config=folder/"tamanho.json"
            import cv2
            image=np.full((600,550,3),(0,255,0),dtype=np.uint8)
            image[70:518,180:315]=(25,45,125)
            cv2.imwrite(str(folder/"inicio_centro.png"),image)
            for height in ["NaN","abc",-224,0]:
                config.write_text(json.dumps({
                    "altura":height,"chaves":{"inicio_centro":.5}}))
                self.assertEqual("REFERENCIA_INVALIDA",
                                 MOD.calibration_info(root,"p01")["status"])
            config.write_text(json.dumps({
                "altura":224,"chaves":{"inicio_centro":float("inf")}}))
            self.assertEqual("REFERENCIA_INVALIDA",
                             MOD.calibration_info(root,"p01")["status"])
            config.write_text(json.dumps({
                "altura":300,"chaves":{"inicio_centro":.5}}))
            # All-red canvas can't impersonate a full-body image.
            image[:]=(0,0,255)
            cv2.imwrite(str(folder/"inicio_centro.png"),image)
            self.assertEqual("REFERENCIA_SEM_CHROMA",
                             MOD.calibration_info(root,"p01")["status"])

    def test_p12_without_import_is_not_audited_successfully(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            result=MOD.inspect_character("p12",root=root,states=["VICTORY"])
            self.assertEqual("PENDENTE_FONTES",result["status"])
            self.assertFalse(result["aprovado_automaticamente"])
            self.assertIn("VICTORY",result["faltantes"])


if __name__=="__main__":
    unittest.main()
