import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import harmony


class SpriteHarmonyTests(unittest.TestCase):
    """Shipped atlases must animate without jumps, floating feet or size pops."""

    def test_checked_in_packs_are_harmonious(self):
        rows, problems = harmony.audit()
        self.assertTrue(rows)
        self.assertEqual([], problems)

    def test_reaction_poses_match_idle_body_size(self):
        rows, _ = harmony.audit()
        idle = [r['height'] for r in rows if r['character'] == 'player_base' and r['state'] == 'IDLE']
        defense = [r['height'] for r in rows if r['character'] == 'player_base' and r['state'] == 'DEFENSE_STAND']
        self.assertGreater(defense[0] / (sum(idle) / len(idle)), harmony.REACTION_MIN_RATIO)


if __name__ == '__main__':
    unittest.main()
