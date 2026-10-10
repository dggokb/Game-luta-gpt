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
                             "vitoria.mp4","jump.mp4","2H.mp4"]:
                (Path(d)/filename).touch()
            mapped=e.match_videos(d,["IDLE","CROUCH_MEDIUM","BACKDASH",
                                      "VICTORY","JUMP","FALL","CROUCH_HEAVY"])
            self.assertTrue(all(x["status"]=="ENCONTRADO" for x in mapped.values()))
            self.assertTrue(mapped["CROUCH_MEDIUM"]["files"][0].endswith("m2(rasteira).mp4"))

    def test_game_commands_are_resolved_to_actual_animation_states(self):
        self.assertEqual("CROUCH_MEDIUM",MOD.canonical_state("2M"))
        self.assertEqual("JUMP_HEAVY",MOD.canonical_state("jH"))
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
            (keys/"inicio_centro.png").write_bytes(b"dummy image: metadata check only")
            videos=root/"animations"/"p01"
            videos.mkdir(parents=True)
            (videos/"idle.mp4").touch()
            (videos/"dash.mp4").touch()
            def read_video(path):
                return {"status":"EXTRAIDO"},[body_frame(head=20,torso=40)]*12
            def read_pack(root,pack,state):
                return {"status":"EXTRAIDO","atlas":"test_"+state.lower()},[
                    body_frame(head=20,torso=40)]*12
            result=MOD.inspect_character("p01",videos,states=["DASH"],
                    root=root,original_video_inspector=read_video,
                    packed_inspector=read_pack)
            self.assertEqual("MEDICOES_CONCLUIDAS",result["status"])
            self.assertFalse(result["aprovado_automaticamente"])
            self.assertEqual("COMPATIVEL",result["estados"]["DASH"]["proporcao_video"]["status"])
            self.assertEqual("COMPATIVEL",result["estados"]["DASH"]["proporcao_sprite"]["status"])
            (videos/"dash.mp4").unlink()
            result=MOD.inspect_character("p01",videos,states=["DASH"],
                    root=root,original_video_inspector=read_video,
                    packed_inspector=read_pack)
            self.assertEqual("PENDENTE_FONTES",result["status"])

    def test_p12_without_import_is_not_audited_successfully(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            result=MOD.inspect_character("p12",root=root,states=["VICTORY"])
            self.assertEqual("PENDENTE_FONTES",result["status"])
            self.assertFalse(result["aprovado_automaticamente"])
            self.assertIn("VICTORY",result["faltantes"])


if __name__=="__main__":
    unittest.main()
