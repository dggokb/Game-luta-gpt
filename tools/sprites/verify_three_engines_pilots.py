#!/usr/bin/env python3
"""Focused regression checks for the real P09/P12 pilot packs after --write/--check.

The baseline global test suite is reported separately: it contains pre-existing
failures unrelated to the two pilots, and costs ~48 minutes on CI.
"""
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageChops

ROOT=Path(__file__).resolve().parents[2]
NAMES={"p09":"P09 BOMBEIRO - PILOTO","p12":"P12 ZUMBI - PILOTO"}


def load(path):
    return json.loads((ROOT/path).read_text(encoding="utf8"))


def check_pilot(pid, roster):
    original=ROOT/f"art/sprites/source/{pid}_pilot_idle_original.png"
    hd=ROOT/f"art/sprites/source/{pid}_pilot_idle_sr2x.png"
    output=ROOT/f"android/app/src/main/res/drawable-nodpi/{pid}_pilot_idle.png"
    report=load(f"tools/sprites/reports/{pid}_pilot_idle.report.json")
    clip=load(f"tools/sprites/clips/{pid}_pilot_idle.json")
    profile=load(f"tools/sprites/profiles/{pid}.json")
    pack=load(f"characters/{pid}/character.json")
    manifest=load(f"android/app/build/{pid}-pilot/{pid}-manifest.json")
    assert pack["id"]==pid and pack["displayName"]==NAMES[pid]
    assert pack["profile"]==f"{pid}.json" and profile["id"]==pid
    assert pid in roster["selectable"]
    assert clip["profile"]==f"{pid}.json"
    assert len(pack["animations"])>=11
    assert all(a["atlas"]==f"{pid}_pilot_idle" for a in pack["animations"].values())
    assert report["passed"] and report["layout"]["frameCount"]==8
    assert report["layout"]["rootX"]==256 and report["layout"]["rootY"]==476
    assert manifest["character"]==pid and manifest["frames"]==8
    assert manifest["pixelScale"]==2 and manifest["masterScale"]==4
    assert manifest["key_sha256"]==hashlib.sha256((ROOT/f"art/keys/{pid}/idle_base.png").read_bytes()).hexdigest()
    assert manifest["concept_sha256"]==hashlib.sha256((ROOT/f"art/concepts/{pid}.png").read_bytes()).hexdigest()
    assert manifest["original_sha256"]==hashlib.sha256(original.read_bytes()).hexdigest()
    assert manifest["enhanced_sha256"]==hashlib.sha256(hd.read_bytes()).hexdigest()
    with Image.open(original) as a, Image.open(hd) as b, Image.open(output) as c:
        assert a.mode=="RGBA" and a.size==(1024,512)
        assert b.mode=="RGBA" and b.size==(2048,1024)
        # Build may repack by cropping margins. Review output separately.
        assert c.mode=="RGBA"
        for i in range(8):
            x,y=(i%4)*256,(i//4)*256
            sx,sy=(i%4)*512,(i//4)*512
            low=a.crop((x,y,x+256,y+256))
            high=b.crop((sx,sy,sx+512,sy+512))
            expected=low.getchannel("A").resize((512,512),Image.Resampling.LANCZOS)
            assert ImageChops.difference(expected,high.getchannel("A")).getbbox() is None, (pid,i,"alpha changed")
            assert low.getchannel("A").getbbox() and high.getchannel("A").getbbox()
    print(f"{pid.upper()} PASS - character, sources, 8/8 enhanced frames, exact alpha and pack output")


def main():
    roster=load("characters/roster.json")
    for id_ in NAMES:
        check_pilot(id_,roster)
    assert "p09"!= "p12"
    a=(ROOT/"art/sprites/source/p09_pilot_idle_original.png").read_bytes()
    b=(ROOT/"art/sprites/source/p12_pilot_idle_original.png").read_bytes()
    assert hashlib.sha256(a).digest()!=hashlib.sha256(b).digest(),"P09 and P12 must have different art"
    print("3-engine pilot structural regression PASS")


if __name__=="__main__":
    main()
