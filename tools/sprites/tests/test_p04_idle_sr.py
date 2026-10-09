import json
import unittest
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]

class P04IdleSuperResolutionTests(unittest.TestCase):
    def test_p04_idle_hd_preserves_alpha_sequence_and_budget(self):
        source=Image.open(ROOT/"art/sprites/source/p04_idle_video_normalized.png").convert("RGBA")
        hd=Image.open(ROOT/"art/sprites/source/p04_idle_sr_2x.png").convert("RGBA")
        self.assertEqual((2112,1792),source.size)
        self.assertEqual((4224,3584),hd.size)
        for i in range(68):
            a=source.crop((i%11*192,i//11*256,(i%11+1)*192,(i//11+1)*256)).getchannel("A")
            b=hd.crop((i%11*384,i//11*512,(i%11+1)*384,(i//11+1)*512)).getchannel("A")
            self.assertEqual(a.resize((384,512),Image.Resampling.LANCZOS).tobytes(),b.tobytes(),i)
        report=json.loads((ROOT/"tools/sprites/reports/p04_idle.report.json").read_text())
        self.assertEqual((384,512,192,476),tuple(report["layout"][k] for k in ("frameWidth","frameHeight","rootX","rootY")))
        packed=report["packed"]
        self.assertEqual(2,packed["pixelScale"])
        self.assertEqual(68,packed["frameCount"])
        self.assertLessEqual(packed["frameWidth"]*packed["columns"],4096)
        self.assertLessEqual(packed["frameHeight"]*7,4096)
        self.assertLess(packed["decodedBytes"]["packed"],64*1024*1024)
        self.assertEqual(192,packed["rootX"]+packed["cropOffset"][0])
        self.assertEqual(476,packed["rootY"]+packed["cropOffset"][1])
        self.assertIn("art/sprites/source/p04_idle_video_normalized.png",report["provenance"]["referenceSources"])
        char=json.loads((ROOT/"characters/p04/character.json").read_text())
        for state in ("IDLE","COMBAT"):
            self.assertEqual("p04_idle",char["animations"][state]["atlas"])
        self.assertEqual(list(range(68)),char["animations"]["IDLE"]["frames"])
        self.assertEqual([41]*68,char["animations"]["IDLE"]["durationsMs"])
        self.assertTrue(char["animations"]["IDLE"]["loop"])
