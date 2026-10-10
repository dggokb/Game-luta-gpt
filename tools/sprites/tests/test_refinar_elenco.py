import importlib.util
from pathlib import Path
import tempfile
import json
import unittest

SPEC=importlib.util.spec_from_file_location(
    "refinar_elenco",Path(__file__).resolve().parents[1]/"refinar_elenco.py")
MOD=importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MOD)


class RosterAutoAuditTest(unittest.TestCase):
    def test_routes_suspect_states_to_safe_review_workflow(self):
        def issue(kind,state,level="erro"):
            return dict(state=state,kind=kind,level=level,text="test",frames=[1,2])
        checks=[
            ("deslize","VICTORY","ANCORAR_PE"),
            ("pulo","HEAVY_STRAIGHT","ANCORAR_PE"),
            ("deslize","WALK_FORWARD","REVISAR_MOVIMENTO_INTENCIONAL"),
            ("tamanho","FALL","REESCALAR_POR_CABECA"),
            ("quebrado","CROUCH_HEAVY","REFAZER_ARTE"),
            ("ritmo","CROUCH_MEDIUM","REVER_RITMO_SEM_PULAR_QUADROS"),
        ]
        for kind,state,expected in checks:
            self.assertEqual(expected,MOD.route(issue(kind,state)))
        report=MOD.summarize([issue(kind,state) for kind,state,_ in checks])
        self.assertFalse(report["aprovado_automaticamente"])
        self.assertEqual(6,report["erros"])
        self.assertEqual(2,report["acoes"]["ANCORAR_PE"])

    def test_no_bad_art_is_mistaken_for_validated_without_sources(self):
        out=MOD.audit_batch(["nonexistent_character_xyz"],auditor=lambda cid: [])
        self.assertEqual("SEM_MANIFESTO",out["nonexistent_character_xyz"]["status"])

    def test_empty_report_only_succeeds_with_real_audit(self):
        self.assertTrue(MOD.summarize([])["aprovado_automaticamente"])
        self.assertFalse(MOD.summarize([dict(state="IDLE",kind="ciclo",
                  level="aviso",text="pulo",frames=[0])])["aprovado_automaticamente"])


if __name__=="__main__":
    unittest.main()
