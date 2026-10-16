import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import harmony


class SpriteHarmonyTests(unittest.TestCase):
    """Shipped atlases must animate without jumps, floating feet or size pops."""

    @classmethod
    def setUpClass(cls):
        # The audit decodes every shipped atlas (minutes); both tests read the same result.
        cls.rows, cls.problems = harmony.audit()

    def test_checked_in_packs_are_harmonious(self):
        self.assertTrue(self.rows)
        self.assertEqual([], self.problems)

    def test_reaction_poses_match_idle_body_size(self):
        rows = self.rows
        idle = [r['height'] for r in rows if r['character'] == 'player_base' and r['state'] == 'IDLE']
        defense = [r['height'] for r in rows if r['character'] == 'player_base' and r['state'] == 'DEFENSE_STAND']
        self.assertGreater(defense[0] / (sum(idle) / len(idle)), harmony.REACTION_MIN_RATIO)


if __name__ == '__main__':
    unittest.main()
