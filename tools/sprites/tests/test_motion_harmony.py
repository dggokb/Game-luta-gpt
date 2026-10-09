import sys
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from motion_harmony import assess


class MotionHarmonyTests(unittest.TestCase):
    def test_walk_is_driven_by_distance_and_overdrive(self):
        pack={"id":"test","animations":{"WALK_FORWARD":{"atlas":"a","frames":[0,1],"loop":True,
             "distancePerFrame":12}},"moves":{},"specialMoves":{}}
        result=assess(pack,{})
        walk=result["animations"]["WALK_FORWARD"]
        self.assertEqual("travel",walk["runtimeMode"])
        self.assertEqual(25.,walk["expectedFramesPerSecond"])
        self.assertEqual(31.25,walk["overdriveFramesPerSecond"])

    def test_detects_late_visual_impact(self):
        pack={"id":"test","animations":{"H":{"atlas":"a","frames":[0,1,2,3],"loop":False,
            "durationsMs":[10,10,10,970],"impactFrame":3}},
            "moves":{"H":{"animation":"H","startupFrames":3,"activeFrames":3,
                          "recoveryFrames":14}}}
        result=assess(pack,{})
        self.assertIn("impact_timing",[x["code"] for x in result["findings"]])


if __name__=="__main__":
    unittest.main()
