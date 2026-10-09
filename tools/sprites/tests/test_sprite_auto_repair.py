import sys
import unittest
import numpy as np
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from sprite_auto_repair import (suggest_replacements,retime_visual,
    visual_impact_position,retime_character_pack,art_requests)


def pose(x,head=8,canvas=130):
    a=np.zeros((75,canvas,4),dtype=np.uint8)
    a[5:20,x:x+head]=[210,160,140,255]
    a[20:65,x-4:x+head+4]=[90,60,150,255]
    return a


class SpriteAutoRepairTests(unittest.TestCase):
    def test_replaces_corrupted_frame_using_only_real_source_frames(self):
        full={0:pose(21),1:pose(22),2:pose(65),3:pose(23),4:pose(24)}
        chosen=[0,2,4]
        indices,changes=suggest_replacements(chosen,
            [full[i] for i in chosen],lambda i: full[i],5)
        self.assertEqual([0,3,4],indices)
        self.assertEqual(2,changes[0]["sourceFrom"])
        self.assertEqual(3,changes[0]["sourceTo"])

    def test_does_not_repair_intentional_large_lunge(self):
        full={0:pose(12),1:pose(34),2:pose(56),3:pose(78),4:pose(90)}
        chosen=[0,2,4]
        indices,changes=suggest_replacements(chosen,
            [full[i] for i in chosen],lambda i:full[i],5)
        self.assertEqual(chosen,indices)
        self.assertFalse(changes)

    def test_protected_impact_is_never_replaced(self):
        full={0:pose(21),1:pose(22),2:pose(65),3:pose(23),4:pose(24)}
        chosen=[0,2,4]
        indices,changes=suggest_replacements(chosen,
            [full[i] for i in chosen],lambda i:full[i],5,protected=[1])
        self.assertEqual(chosen,indices)

    def test_retime_changes_only_display_durations_and_preserves_total(self):
        original=[30,40,30,70,60]
        updated,reason=retime_visual(original,2,3,4,9,max_ratio=1.8)
        self.assertIsNotNone(updated,reason)
        self.assertEqual(len(original),len(updated))
        self.assertEqual(sum(original),sum(updated))
        expected=(3+2)/(3+4+9)
        self.assertLess(abs(visual_impact_position(updated,2)-expected),
                        abs(visual_impact_position(original,2)-expected))

    def test_large_retime_is_review_not_broken_clip(self):
        data,reason=retime_visual([5,5,5,900],3,2,4,14,max_ratio=1.25)
        self.assertIsNone(data)
        self.assertIn("manual",reason)

    def test_pack_retime_never_changes_gameplay_frame_data(self):
        pack={"animations":{"PUNCH":{"frames":[0,1,2],"durationsMs":[60,80,60],
             "impactFrame":1,"loop":False}},"moves":{"L":{"animation":"PUNCH",
             "startupFrames":4,"activeFrames":6,"recoveryFrames":10,"damage":900}}}
        updated,proposals=retime_character_pack(pack,max_ratio=1.8)
        self.assertEqual(pack["moves"],updated["moves"])
        self.assertEqual(pack["animations"]["PUNCH"]["frames"],
                         updated["animations"]["PUNCH"]["frames"])
        self.assertEqual(900,updated["moves"]["L"]["damage"])
        self.assertTrue(proposals)

    def test_cut_off_character_generates_regeneration_prompt(self):
        actions=art_requests({"flags":[{"code":"clip_edge","frames":[2],"severity":"warning"}]},
                             "p08","heavy_whip")
        self.assertEqual("REGENERATE_WITH_SPACE",actions[0]["kind"])
        self.assertIn("p08",actions[0]["prompt"])
        self.assertFalse(actions[0]["automatic"])


if __name__=="__main__":
    unittest.main()
