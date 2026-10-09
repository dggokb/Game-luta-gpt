import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from autonomous_transitions import ROOT,compute,java_code


class TransitionAutonomyTests(unittest.TestCase):
    def test_all_p01_sources_map_into_loop_targets(self):
        maps,diagnostics=compute(ROOT,"player_base")
        self.assertGreaterEqual(len(diagnostics),100)
        self.assertIn("player_base|INTRO|IDLE",maps)
        self.assertIn("player_base|LIGHT_JAB|IDLE",maps)
        self.assertIn("player_base|WALK_FORWARD|WALK_BACK",maps)
        self.assertTrue(all(x>=0 for values in maps.values() for x in values))

    def test_generator_is_byte_identical(self):
        m,_=compute(ROOT,"player_base")
        self.assertEqual(java_code(m),java_code(m))
        self.assertIn("new int[]{",java_code(m))
