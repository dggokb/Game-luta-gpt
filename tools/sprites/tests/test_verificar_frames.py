"""Synthetic regression tests for the generic per-clip temporal checker."""
import math
import sys
import unittest
from pathlib import Path
from PIL import Image, ImageDraw

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import verificar_frames as F


def example_frames(count=56,broken=None,shift_end=False):
    result=[]
    for i in range(count):
        im=Image.new("RGBA",(128,176))
        draw=ImageDraw.Draw(im)
        # Reversible natural breathing: no teleports or frame swaps.
        lift=round(2*math.sin(2*math.pi*i/count))
        x=43+(2 if shift_end and i>=count-4 else 0)
        color=(19,98,182,255) if i != broken else (238,20,18,255)
        draw.rounded_rectangle((x,32+lift,x+38,138+lift),
                               radius=10,fill=color)
        draw.ellipse((x-13,125,x+11,150),fill=(25,48,88,255))
        draw.ellipse((x+26,125,x+51,150),fill=(25,48,88,255))
        result.append(im)
    return result


class TemporalContinuityTests(unittest.TestCase):
    def test_stable_breathing_loop_is_not_a_failure(self):
        report=F.analyze(example_frames())
        self.assertEqual(56,report["frame_count"])
        self.assertEqual(56,report["transitions_tested"])
        self.assertEqual("NO_AUTOMATIC_BREAK_DETECTED",report["status"])
        self.assertEqual([],report["suspect_transitions"])

    def test_real_frame_mutation_catches_both_adjacent_transitions(self):
        report=F.analyze(example_frames(broken=24))
        indices={(r["from"],r["to"]) for r in report["suspect_transitions"]}
        self.assertIn((23,24),indices)
        self.assertIn((24,25),indices)

    def test_loop_seam_is_independently_measured(self):
        report=F.analyze(example_frames(broken=55))
        index={(r["from"],r["to"]) for r in report["suspect_transitions"]}
        self.assertIn((54,55),index)
        self.assertIn((55,0),index)
        self.assertTrue(next(r for r in report["all_transitions"] if r["to"]==0)["loop_seam"])

    def test_non_loop_excludes_closure(self):
        report=F.analyze(example_frames(broken=55),loop=False)
        self.assertEqual(55,report["transitions_tested"])
        self.assertFalse(any(r["loop_seam"] for r in report["all_transitions"]))

    def test_runtime_offsets_are_visualized_without_modifying_inputs(self):
        frames=example_frames(count=6)
        before=frames[-1].tobytes()
        adjusted=F.apply_runtime(frames,"p02","p02_idle",{"corrections":[
            {"character":"p02","atlas":"p02_idle","frames":[[5,1,2,0]]},
            {"character":"player_base","atlas":"player_base_victory","frames":[[5,1,1,0]]}
        ]})
        self.assertEqual(before,frames[-1].tobytes())
        self.assertNotEqual(before,adjusted[-1].tobytes())
        self.assertEqual(frames[0].tobytes(),adjusted[0].tobytes())

    def test_clipping_cannot_be_silently_accepted(self):
        frames=example_frames(count=6)
        with self.assertRaises(ValueError):
            F.apply_runtime(frames,"p02","p02_idle",{"corrections":[
                {"character":"p02","atlas":"p02_idle","frames":[[3,1,99,0]]}
            ]})


if __name__=="__main__":
    unittest.main()
