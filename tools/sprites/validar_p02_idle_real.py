#!/usr/bin/env python3
"""Pixel-real P02 IDLE before/after, using Android's actual atlas and frame indices.

Read-only: never create or redraw a sprite. The 56 RGBA cells stay unchanged.
"""
import json
import sys
from pathlib import Path
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/"tools/sprites"))
import verificar_frames as F

OUT=ROOT/"android/app/build/sprite-review/p02-idle-diagnose"
PACKED=json.loads((ROOT/"tools/sprites/reports/p02_idle.report.json").read_text())["packed"]
TIMELINE=list(range(15,45))


def preview(frames,path,title):
    result=[]
    for pos,frame in enumerate(frames):
        image=Image.new("RGBA",frame.size,(236,237,241,255))
        image.alpha_composite(frame)
        image.thumbnail((244,326),Image.Resampling.LANCZOS)
        canvas=Image.new("RGB",(260,357),(236,237,241))
        canvas.paste(image.convert("RGB"),((260-image.width)//2,25))
        ImageDraw.Draw(canvas).text((8,6),f"{title} {pos+1:02}/{len(frames)}",fill=(15,15,15))
        result.append(canvas)
    result[0].save(path,save_all=True,append_images=result[1:],
                   duration=41,loop=0,optimize=True)


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    source=ROOT/"android/app/src/main/res/drawable-nodpi/p02_idle.png"
    with Image.open(source) as atlas:
        assert atlas.mode=="RGBA","Original transparency must survive"
        assert atlas.size==(PACKED["frameWidth"]*PACKED["columns"],
            PACKED["frameHeight"]*6),"Original packed dimensions must not change"
    all_frames=F.source_sheet(source,PACKED["frameWidth"],PACKED["frameHeight"],
                              PACKED["columns"],56)
    fixes=json.loads((ROOT/"android/app/src/main/assets/sprite-autofix.json").read_text())["corrections"]
    assert not any(item.get("character")=="p02" and item.get("atlas")=="p02_idle" for item in fixes), "Stale P02 offsets"
    layers,timeline=F.project_report(ROOT,"p02","p02_idle",True)
    assert timeline==TIMELINE, "The checker must use Android playback order"
    after=layers["runtime_effective"]
    assert len(after)==30
    assert all(after[i].tobytes()==all_frames[index].tobytes() for i,index in enumerate(timeline)), "Bitmap pixels or source cells changed"
    original_patches={"corrections":[{"character":"p02","atlas":"p02_idle",
        "frames":[[j,1,2,0] for j in range(51,56)]}]}
    before=F.apply_runtime(all_frames,"p02","p02_idle",original_patches)
    prior_report=F.analyze(before)
    after_report=F.analyze(after)
    assert not after_report["suspect_transitions"],str(after_report["suspect_transitions"])
    assert len(after_report["all_transitions"])==30
    assert after_report["all_transitions"][-1]["to"]==0
    assert PACKED["pixelScale"]==2, "The approved 2x size may not change"
    preview(before,OUT/"idle_before.gif","ANTES")
    preview(after,OUT/"idle_after.gif","DEPOIS")
    # Compare at equal frame duration. Different timeline lengths loop normally.
    side=[]
    for step in range(90):
        left=before[step%len(before)]
        right=after[step%len(after)]
        card=Image.new("RGB",(520,357),(236,237,241))
        for k,frame in enumerate((left,right)):
            image=Image.new("RGBA",frame.size,(236,237,241,255))
            image.alpha_composite(frame)
            image.thumbnail((244,326),Image.Resampling.LANCZOS)
            card.paste(image.convert("RGB"),(260*k+(260-image.width)//2,25))
        draw=ImageDraw.Draw(card)
        draw.text((8,6),f"ANTES {step%56:02}",fill=(15,15,15))
        draw.text((268,6),f"DEPOIS frame {timeline[step%len(after)]:02}",fill=(15,15,15))
        side.append(card)
    side[0].save(OUT/"comparativo-lado-a-lado.gif",save_all=True,
                 append_images=side[1:],duration=41,loop=0,optimize=True)
    info={"physical_atlas_frames":56,"playback_frames":30,"playback_indices":timeline,
          "real_png":str(source.relative_to(ROOT)),
          "atlas_dimensions":list(Image.open(source).size),
          "frame_size":[PACKED["frameWidth"],PACKED["frameHeight"]],
          "pixelScale":PACKED["pixelScale"],"p02_runtime_offsets":[],
          "before":prior_report,"after":after_report}
    (OUT/"before_after.json").write_text(json.dumps(info,indent=2)+"\n")
    print("REAL ANDROID ATLAS 56 cells, 30 forward-only loop steps, 2x pixelScale preserved")
    print("BEFORE: "+str([(r["from"],r["to"]) for r in prior_report["suspect_transitions"]]))
    print("AFTER: "+str([(r["from"],r["to"]) for r in after_report["suspect_transitions"]]))
    print("SEAM BEFORE/AFTER:",prior_report["all_transitions"][-1]["score"],after_report["all_transitions"][-1]["score"])
if __name__=="__main__":main()
