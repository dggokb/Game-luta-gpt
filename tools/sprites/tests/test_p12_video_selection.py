"""Real source bridges preserve impact, chronology and the action itself."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from p12_video_selection import bind_sources


class SourceBridgeTest(unittest.TestCase):
    def setUp(self):
        self.records = {name: {'frames': [{'index': i, 'time': i / 24} for i in range(97)]}
                        for name in ['idle.mp4', 'M.mp4', 'intro.mp4']}
        self.names = ['idle.mp4', 'M.mp4', 'M.mp4', 'intro.mp4']
        self.indices = [0, 43, 48, 96]

    def check(self, names=None, indices=None):
        return bind_sources('MEDIUM_KICK', self.names if names is None else names,
                            self.indices if indices is None else indices,
                            2, 'M.mp4', 4, self.records)

    def test_existing_bridges_preserve_original_action_at_impact(self):
        self.assertEqual(self.check(), list(zip(self.names, self.indices)))

    def test_equal_indices_from_different_videos_are_independent(self):
        self.assertEqual(self.check(indices=[43, 43, 48, 48])[-1], ('intro.mp4', 48))

    def test_repeated_or_reversed_frames_in_one_video_are_rejected(self):
        for indices in ([0, 48, 48, 96], [0, 49, 48, 96]):
            with self.subTest(indices=indices), self.assertRaisesRegex(ValueError, 'sequential, unique'):
                self.check(indices=indices)

    def test_neutral_bridge_cannot_replace_impact(self):
        with self.assertRaisesRegex(ValueError, 'independent action'):
            self.check(names=['M.mp4', 'M.mp4', 'idle.mp4', 'intro.mp4'])

    def test_single_action_frame_cannot_be_disguised_as_a_complete_action(self):
        with self.assertRaisesRegex(ValueError, 'replace the actual action'):
            self.check(names=['idle.mp4', 'idle.mp4', 'M.mp4', 'intro.mp4'])

    def test_unrecorded_video_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'existing source video'):
            self.check(names=['unrecorded.mp4', 'M.mp4', 'M.mp4', 'intro.mp4'])

    def test_fabricated_original_timestamp_is_rejected(self):
        self.records['M.mp4']['frames'][48]['time'] = 0
        with self.assertRaisesRegex(ValueError, 'chronological'):
            self.check()

    def test_nan_or_negative_timestamp_is_rejected(self):
        for time in (float('nan'), float('inf'), -1):
            with self.subTest(time=time):
                self.records['M.mp4']['frames'][48]['time'] = time
                with self.assertRaisesRegex(ValueError, 'finite and nonnegative'):
                    self.check()


if __name__ == '__main__':
    unittest.main()
