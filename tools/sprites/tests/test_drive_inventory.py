#!/usr/bin/env python3
"""Regressões da auditoria — roda apenas com a biblioteca padrão."""
import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
from audit_drive_inventory import MANIFEST, audit, make_markdown
from video_names import CANONICAL, normalize, resolve


class DriveInventoryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        cls.report = audit(cls.manifest)

    def test_inventory_snapshot_counts(self):
        self.assertEqual(len(self.manifest["characters"]), 12)
        self.assertEqual(self.report["totals"]["videos"], 399)
        self.assertEqual(len(CANONICAL), 35)
        self.assertEqual(self.report["totals"]["states"], 420)
        self.assertFalse(self.report["media_checked"])

    def test_all_12_have_some_mp4(self):
        for pid, data in self.manifest["characters"].items():
            self.assertGreaterEqual(len(data["videos"]), 20, pid)

    def test_specific_typo_aliases(self):
        expected = {
            "p09": ("WALK_FORWARD", "ente"),
            "p11": ("HIT_STAND", "danoAlto"),
            "p11_knock": ("KNOCKDOWN", "deruba_levanta"),
            "p04": ("DEFENSE_CROUCH", "defesaAgaxado"),
            "p03": ("CROUCH_MEDIUM", "rasteiraBaixo"),
            "p12": ("DEFENSE_CROUCH", "defendeBaixo"),
        }
        for case, (state, name) in expected.items():
            pid=case.split("_")[0]
            data=self.report["characters"][pid]["states"][state]
            self.assertEqual(normalize(data["video"]), normalize(name), (pid,state))

    def test_do_not_assume_p08_s2_exists(self):
        p08=self.report["characters"]["p08"]["states"]["SPECIAL_S2"]
        self.assertIsNone(p08["video"])
        self.assertIn("known_provisional", p08["warnings"])
        self.assertEqual(p08["mapping"], "H")

    def test_p03_borrowed_atlas(self):
        p03=self.report["characters"]["p03"]["states"]
        self.assertIn("other_character_atlas", p03["JUMP_MEDIUM"]["warnings"])
        self.assertIn("other_character_atlas", p03["THROW"]["warnings"])

    def test_p12_missing_video_not_fake_filled(self):
        p12=self.report["characters"]["p12"]["states"]
        self.assertIsNone(p12["HIT_STAND"]["video"])
        self.assertIsNone(p12["JUMP_HEAVY_DOWN"]["video"])
        self.assertFalse(self.report["characters"]["p12"]["integrated"])

    def test_new_video_maps_have_real_names(self):
        for pid in ("p09","p10","p11","p12"):
            character=self.report["characters"][pid]
            names={normalize(x["name"]) for x in self.manifest["characters"][pid]["videos"]}
            path=TOOLS/"videos"/f"{pid}.json"
            mapping=json.loads(path.read_text(encoding="utf-8"))
            for state, name in mapping.items():
                self.assertIn(state, CANONICAL)
                self.assertIn(normalize(name), names, (pid,state,name))
            self.assertFalse(character["scale_calibrated"])

    def test_dont_guess_generic_file_names(self):
        p01=self.report["characters"]["p01"]
        self.assertTrue(any("dreamina-" in name for name in p01["unclassified"]))
        self.assertIn("manual_semantic_review",p01["states"]["CROUCH_HEAVY"]["warnings"])

    def test_detect_ambiguous_video_names(self):
        matches, ambiguous, unclassified=resolve(["jM.mp4","jm.MP4","raro.mp4"])
        self.assertIn("JUMP_MEDIUM",ambiguous)
        self.assertNotIn("JUMP_MEDIUM",matches)
        self.assertIn("raro",unclassified)

    def test_markdown_contains_breakdown(self):
        md=make_markdown(self.report)
        self.assertIn("p12",md)
        self.assertIn("missing_named_video",md)
        self.assertIn("nao foram lidos quadros",md)


if __name__=="__main__":
    unittest.main()
