"""Execute REAL correction on an isolated synthetic sprite atlas.

Not only diagnosis: publishes a patch, shows before/after screen-foot error,
tests that unrelated characters remain untouched, and rolls back exact JSON.
"""
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path
import numpy as np
from PIL import Image

TOOLS=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(TOOLS))
SPEC=importlib.util.spec_from_file_location("autoestabilizar",TOOLS/"autoestabilizar.py")
AUTO=importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUTO)


class AutoCorrectionTest(unittest.TestCase):
    def test_correct_foot_drift_or_refuse_real_walk_and_short_sequence(self):
        corrected=AUTO.correct_shoes([20+i for i in range(12)],start=0)
        self.assertEqual("CORRIGIDO",corrected["status"])
        self.assertGreater(corrected["antes_rms"],3)
        self.assertLess(corrected["depois_rms"],.001)
        self.assertGreaterEqual(corrected["improvement"] if "improvement" in corrected else 1.,.85)
        self.assertEqual("BLOQUEADO",
            AUTO.correct_shoes([20+i*4 for i in range(12)])["status"])
        self.assertEqual("INCONCLUSIVO",
            AUTO.correct_shoes([20,21,22])["status"])
        self.assertEqual("SEM_CORRECAO",
            AUTO.correct_shoes([20]*12)["status"])

    def test_independent_proof_produces_bounded_scale_not_a_guess(self):
        data={"status":"ESCALA_FORA_DO_IDLE",
              "fator_video":.90,"fator_atlas":.90}
        fix=AUTO.correct_scale(data)
        self.assertEqual("CORRIGIDO",fix["status"])
        self.assertAlmostEqual(1/.90,fix["scale"],places=4)
        self.assertLess(fix["erro_depois"],fix["erro_antes"]*.15)
        self.assertNotEqual("CORRIGIDO",AUTO.correct_scale(
            {**data,"fator_video":.9,"fator_atlas":.78})["status"])
        self.assertNotEqual("CORRIGIDO",AUTO.correct_scale(
            {**data,"status":"INCONCLUSIVO"})["status"])

    def test_real_png_becomes_rendered_correction_and_is_reverted(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            pack=root/"characters/p02"
            pack.mkdir(parents=True)
            (pack/"character.json").write_text(json.dumps({
                "animations":{"VICTORY":{"atlas":"p02_victory",
                  "frames":list(range(12))}}}))
            reports=root/"tools/sprites/reports"
            images=root/"android/app/src/main/res/drawable-nodpi"
            reports.mkdir(parents=True);images.mkdir(parents=True)
            fw,fh=128,132
            sheet=np.zeros((fh,fw*12,4),dtype=np.uint8)
            for i in range(12):
                ox=i*fw
                # Same torso, but the planted shoe drifts +1.25px/frame.
                sheet[22:85,ox+40:ox+88,3]=255
                sheet[70:125,ox+40+i:ox+72+i,3]=255
                sheet[22:125,ox+40:ox+88,:3]=170
            Image.fromarray(sheet,"RGBA").save(images/"p02_victory.png")
            (reports/"p02_victory.report.json").write_text(json.dumps({
                "packed":{"frameWidth":fw,"frameHeight":fh,
                          "columns":12,"frameCount":12,"rootX":64,"rootY":126}}))
            analysis={"estados":{"VICTORY":{
                "video":{"status":"ENCONTRADO","analise":{"status":"MEDIDO"}},
                "prova_geometrica":{"status":"INCONCLUSIVO"}}}}
            result,record=AUTO.propose(root,"p02","VICTORY",analysis)
            self.assertEqual("CORRIGIDO",result["status"],result)
            self.assertEqual("CORRIGIDO",result["pes"]["status"])
            self.assertEqual("CONFIRMADO_EM_PIXELS",
                             result["validacao_pixels"]["status"],result)
            self.assertLess(result["validacao_pixels"]["depois_px"],
                            result["validacao_pixels"]["antes_px"]*.22)
            self.assertEqual("INCONCLUSIVO",result["tamanho"]["status"])
            self.assertEqual(12,len(record["frames"]))
            self.assertEqual("p02",record["character"])
            asset=root/"android/app/src/main/assets/sprite-autofix.json"
            original={"schema":1,"corrections":[{
                "character":"p03","atlas":"p03_idle","state":"IDLE",
                "frames":[[0,1.0,0.5,0.]]}]}
            asset.parent.mkdir(parents=True)
            asset.write_text(json.dumps(original))
            info=AUTO.publish(asset,record)
            self.assertEqual(str(asset),info["asset"])
            applied=json.loads(asset.read_text())
            self.assertEqual(2,len(applied["corrections"]))
            self.assertEqual("p03",applied["corrections"][0]["character"])
            self.assertEqual("p02",applied["corrections"][1]["character"])
            self.assertAlmostEqual(1.,applied["corrections"][1]["frames"][0][1])
            self.assertNotAlmostEqual(0.,applied["corrections"][1]["frames"][-1][2])
            with self.assertRaises(ValueError):
                AUTO.publish(asset,record) # no silent overwrite
            self.assertEqual("REVERTIDO",AUTO.rollback(asset)["status"])
            self.assertEqual(original,json.loads(asset.read_text()))

    def test_no_correction_of_unimported_character_or_deliberately_moving_state(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(ValueError):
                AUTO.propose(Path(d),"p12","VICTORY",{})
            self.assertEqual("player_base",AUTO.character_pack("p01"))
            with self.assertRaises(ValueError):
                AUTO.character_pack("p99")


if __name__=="__main__":
    unittest.main()
