"""Execute REAL correction on an isolated synthetic sprite atlas.

Not only diagnosis: publishes a patch, shows before/after screen-foot error,
tests that unrelated characters remain untouched, and rolls back exact JSON.
"""
import importlib.util
import json
import sys
import tempfile
import unittest
from unittest.mock import patch
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
                          "columns":12,"frameCount":12,"rootX":64,"rootY":126,
                          "pixelScale":1}}))
            analysis={"estados":{"VICTORY":{
                "video":{"status":"ENCONTRADO","analise":{"status":"MEDIDO"}},
                "sprite":{"status":"MEDIDO","deslize":{"status":"SUSPEITA_DE_DESLIZE"}},
                "origem_tremor":{"status":"SUSPEITA_DE_TREMOR_INTRODUZIDO_NOS_SPRITES"},
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

    def test_batch_correction_of_one_fighter_is_atomic_and_rollback_all(self):
        with tempfile.TemporaryDirectory() as folder:
            asset=Path(folder)/"sprite-autofix.json"
            records=[{"character":"p02","atlas":atlas,"state":state,
                      "frames":[[0,1.,dx,0.]]}
                     for atlas,state,dx in (
                         ("p02_victory","VICTORY",-2.),
                         ("p02_idle","IDLE",1.))]
            saved=AUTO.publish_batch(asset,records)
            self.assertEqual(["VICTORY","IDLE"],saved["estados_corrigidos"])
            entries=json.loads(asset.read_text())["corrections"]
            self.assertEqual(2,len(entries))
            with self.assertRaises(ValueError):
                AUTO.publish_batch(asset,records)
            with self.assertRaises(ValueError):
                AUTO.rollback(asset,"p03")
            self.assertEqual(2,len(json.loads(asset.read_text())["corrections"]))
            self.assertEqual("REVERTIDO",AUTO.rollback(asset,"p02")["status"])
            self.assertEqual([],json.loads(asset.read_text())["corrections"])
            with self.assertRaises(ValueError):
                AUTO.publish_batch(asset,records+[{
                    "character":"p03","atlas":"p03_idle","state":"IDLE",
                    "frames":[[0,1.,0.,0.]]}])

    def test_rollback_rejects_external_changes_and_cannot_repeat(self):
        with tempfile.TemporaryDirectory() as folder:
            asset=Path(folder)/"sprite-autofix.json"
            patch={"character":"p02","atlas":"p02_idle","state":"IDLE",
                   "frames":[[0,1.0,2.0,0.0]]}
            AUTO.publish(asset,patch)
            published=asset.read_text()
            changed=json.loads(published)
            changed["corrections"].append({
                "character":"p03","atlas":"p03_idle","state":"IDLE",
                "frames":[[0,1.,0.,0.]]})
            asset.write_text(json.dumps(changed))
            with self.assertRaisesRegex(ValueError,"modificado"):
                AUTO.rollback(asset,"p02")
            self.assertEqual(changed,json.loads(asset.read_text()),
                             "Rollback must never discard unrelated changes")
            asset.write_text(published)
            self.assertEqual("REVERTIDO",AUTO.rollback(asset,"p02")["status"])
            self.assertFalse(asset.with_suffix(".expected.sha256").exists())
            with self.assertRaises(ValueError):
                AUTO.rollback(asset,"p02")

    def test_publisher_rejects_invalid_rows_atomically(self):
        with tempfile.TemporaryDirectory() as folder:
            asset=Path(folder)/"sprite-autofix.json"
            original='{"schema":1,"corrections":[]}\\n'
            asset.write_text(original)
            good={"character":"p02","atlas":"p02_idle","state":"IDLE",
                  "frames":[[0,1.,.4,0.]]}
            variants=[
                {**good,"frames":[[0,1.8,0.,0.]]},
                {**good,"frames":[[0,float("nan"),0.,0.]]},
                {**good,"frames":[[-1,1.,0.,0.]]},
                {**good,"frames":[[0,1.,0.,0.],[0,1.,0.,0.]]},
                {**good,"atlas":"!bad"},
                {**good,"frames":[]},
                {**good,"frames":[[0,1.,60.,0.]]},
            ]
            for item in variants:
                with self.assertRaises(ValueError):
                    AUTO.publish_batch(asset,[good,item])
                self.assertEqual(original,asset.read_text(),
                                 "Failed batch must leave prior asset unchanged")
                self.assertFalse(asset.with_suffix(".previous.json").exists(),
                                 "Failed batch must not create rollback metadata")

    def test_existing_invalid_patch_cannot_poison_new_batch(self):
        with tempfile.TemporaryDirectory() as folder:
            asset=Path(folder)/"sprite-autofix.json"
            valid={"character":"p02","atlas":"p02_idle","state":"IDLE",
                   "frames":[[0,1.0,0.0,0.0]]}
            new={"character":"p03","atlas":"p03_idle","state":"IDLE",
                 "frames":[[0,1.0,1.0,0.0]]}
            invalid={"schema":1,"corrections":[{
                **valid,"frames":[[0,99.0,0.,0.]]}]}
            asset.write_text(json.dumps(invalid))
            with self.assertRaises(ValueError):
                AUTO.publish(asset,new)
            self.assertEqual(invalid,json.loads(asset.read_text()))
            self.assertFalse(asset.with_suffix(".previous.json").exists())
            duplicates={"schema":1,"corrections":[valid,valid]}
            asset.write_text(json.dumps(duplicates))
            with self.assertRaisesRegex(ValueError,"duplicadas"):
                AUTO.publish(asset,new)
            self.assertEqual(duplicates,json.loads(asset.read_text()))


    def test_foot_autofix_requires_independent_stable_source(self):
        # Monotonic foot travel may be an intentional step, not a defect.
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            manifest=root/"characters/p02/character.json"
            manifest.parent.mkdir(parents=True)
            manifest.write_text(json.dumps({"animations":{
                "VICTORY":{"atlas":"p02_victory","frames":list(range(12))}
            }}))
            report=root/"tools/sprites/reports/p02_victory.report.json"
            report.parent.mkdir(parents=True)
            report.write_text(json.dumps({"packed":{"pixelScale":1}}))
            base={"status":"MEDIDO","deslize":None,"tremor":{}}
            source={"status":"ENCONTRADO","analise":base}
            evidence={"estados":{"VICTORY":{
                "video":source,"sprite":base,
                "prova_geometrica":{"status":"INCONCLUSIVO"}
            }}}
            # Extreme certainty in pixel offsets alone does not prove intent.
            with (patch.object(AUTO,"art_track",return_value=list(range(12))),
                  patch.object(AUTO,"validate_pixels_after",
                               return_value={"status":"CONFIRMADO_EM_PIXELS"})):
                result,record=AUTO.propose(root,"p02","VICTORY",evidence)
                self.assertEqual("SEM_AJUSTE_SEGURO",result["status"],result)
                self.assertIsNone(record)

                # Same movement in the author video is not introduced by atlas.
                observed=evidence["estados"]["VICTORY"]
                observed["origem_tremor"]={"status":"ANOMALIA_TAMBEM_PRESENTE_NO_VIDEO"}
                observed["video"]={"status":"ENCONTRADO","analise":{
                    **base,"deslize":{"status":"SUSPEITA_DE_DESLIZE"}}}
                observed["sprite"]={**base,"deslize":{"status":"SUSPEITA_DE_DESLIZE"}}
                result,record=AUTO.propose(root,"p02","VICTORY",evidence)
                self.assertEqual("SEM_AJUSTE_SEGURO",result["status"],result)
                self.assertIsNone(record)

                # Clean source and independently confirmed atlas-only motion.
                observed["video"]=source
                observed["origem_tremor"]={
                    "status":"SUSPEITA_DE_TREMOR_INTRODUZIDO_NOS_SPRITES"}
                result,record=AUTO.propose(root,"p02","VICTORY",evidence)
                self.assertEqual("CORRIGIDO",result["status"],result)
                self.assertIsNotNone(record)

                # One atlas can supply several states; a frame correction
                # keyed only by atlas+frame would also affect the other state.
                manifest.write_text(json.dumps({"animations":{
                    "VICTORY":{"atlas":"p02_victory","frames":list(range(12))},
                    "INTRO":{"atlas":"p02_victory","frames":[0,1,2]}
                }}))
                result,record=AUTO.propose(root,"p02","VICTORY",evidence)
                self.assertEqual("BLOQUEADO",result["status"],result)
                self.assertIsNone(record)

    def test_scale_only_cannot_be_published_without_rendered_pixel_confirmation(self):
        # A geometric prediction alone is not an independent AFTER render
        # measurement. Prevent a mathematically plausible false approval.
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            manifest=root/"characters/p02/character.json"
            manifest.parent.mkdir(parents=True)
            manifest.write_text(json.dumps({"animations":{
                "DASH":{"atlas":"p02_dash","frames":[0,1,2,3]}
            }}))
            report=root/"tools/sprites/reports/p02_dash.report.json"
            report.parent.mkdir(parents=True)
            report.write_text(json.dumps({"packed":{"pixelScale":1.0}}))
            evidence={"estados":{"DASH":{
                "video":{"status":"ENCONTRADO","analise":{"status":"MEDIDO"}},
                "prova_geometrica":{"status":"ESCALA_FORA_DO_IDLE",
                    "fator_video":.90,"fator_atlas":.90}
            }}}
            result,record=AUTO.propose(root,"p02","DASH",evidence)
            self.assertEqual("SEM_AJUSTE_SEGURO",result["status"],result)
            self.assertIsNone(record)
            self.assertEqual("AGUARDANDO_VALIDACAO_PIXELS",
                             result["tamanho"]["status"])
            self.assertAlmostEqual(1/.90,
                result["tamanho"]["proposta"]["scale"],delta=.0001)

    def test_rollback_restores_original_json_bytes_exactly(self):
        with tempfile.TemporaryDirectory() as folder:
            asset=Path(folder)/"sprite-autofix.json"
            original=b'{ "corrections": [], "schema": 1 }\n\n'
            asset.write_bytes(original)
            record={"character":"p02","atlas":"p02_idle","state":"IDLE",
                    "frames":[[0,1.0,-2.0,0.0]]}
            AUTO.publish(asset,record)
            self.assertNotEqual(original,asset.read_bytes())
            self.assertEqual("REVERTIDO",AUTO.rollback(asset,"p02")["status"])
            self.assertEqual(original,asset.read_bytes(),
                "Rollback must restore byte-exact prior asset, not reformat it")

    def test_no_correction_of_unimported_character_or_deliberately_moving_state(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(ValueError):
                AUTO.propose(Path(d),"p12","VICTORY",{})
            self.assertEqual("player_base",AUTO.character_pack("p01"))
            with self.assertRaises(ValueError):
                AUTO.character_pack("p99")


if __name__=="__main__":
    unittest.main()
