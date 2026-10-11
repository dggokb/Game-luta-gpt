#!/usr/bin/env python3
"""P02 DASH regression: true rendered body size, foot registration and native loop.

Read-only: compares the shipped real RGBA PNGs of the four DASH poses against
the approved P02 IDLE. Sprite cell dimensions or pixelScale ALONE do not
prove apparent character height: scan opaque pixels on each actual frame.
"""
import json
import statistics
import sys
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/"tools/sprites"))
import verificar_frames as continuity

P02=ROOT/"characters/p02/character.json"
PROFILE=ROOT/"tools/sprites/profiles/p02.json"
OUT=ROOT/"android/app/build/sprite-review/p02-dash-approved"
TOLERANCE=0.05


def pack(clip):
    cfg=json.loads((ROOT/f"tools/sprites/clips/{clip}.json").read_text())
    report=json.loads((ROOT/cfg["report"]).read_text())
    packed=report["packed"]
    if packed["pixelScale"] != 2:
        raise AssertionError(f"{clip}: the approved pixelScale is 2x, got {packed['pixelScale']}")
    if report.get("worldScale") != json.loads(PROFILE.read_text())["worldScale"]:
        raise AssertionError(f"{clip}: worldScale mismatch")
    frames=continuity.source_sheet(ROOT/cfg["output"],packed["frameWidth"],
        packed["frameHeight"],packed["columns"],cfg["expectedFrames"])
    return frames,packed


def appearance(frame,packed,reference):
    mask=frame.getchannel("A").point(lambda v:255 if v>10 else 0)
    bbox=mask.getbbox()
    if not bbox:
        raise AssertionError(f"{reference}: empty visible body")
    pixel_scale=packed["pixelScale"]
    # root-to-head projected world visual height (pixelScale undone).
    height=(packed["rootY"]-bbox[1])/pixel_scale
    ground=(bbox[3]-1-packed["rootY"])/pixel_scale
    return {"frame":reference,"body_height":round(height,3),
            "ground_offset":round(ground,3),
            "visible_bbox":[int(x) for x in bbox]}


def main():
    hero=json.loads(P02.read_text())
    profile=json.loads(PROFILE.read_text())
    idle_state=hero["animations"]["IDLE"]
    dash_state=hero["animations"]["DASH"]
    assert idle_state["frames"]==list(range(15,45)), "Keep the approved IDLE"
    assert idle_state["atlas"]=="p02_idle"
    assert dash_state["atlas"]=="p02_dash" and dash_state["frames"]==[0,1,2,3]
    assert dash_state["loop"] and dash_state["durationsMs"]==[65]*4
    idle,idle_layout=pack("p02_idle")
    dash,dash_layout=pack("p02_dash")
    assert len(idle)==56 and len(dash)==6, "Original HD atlas cells must remain unchanged"
    fixes=json.loads((ROOT/"android/app/src/main/assets/sprite-autofix.json").read_text())
    assert not any(x.get("character")=="p02" and x.get("atlas") in ("p02_idle","p02_dash")
        for x in fixes["corrections"]), "Unexpected P02 size/position runtime correction"
    idle_metrics=[appearance(idle[i],idle_layout,f"IDLE[{i}]") for i in idle_state["frames"]]
    dash_metrics=[appearance(dash[i],dash_layout,f"DASH[{i}]") for i in dash_state["frames"]]
    baseline=statistics.median(row["body_height"] for row in idle_metrics)
    nominal=float(profile["standingVisualHeight"])
    assert abs(baseline-nominal)<=TOLERANCE*nominal, (
        f"IDLE already deviates from the approved P02 profile: {baseline:.1f} vs {nominal:.1f}")
    for row in dash_metrics:
        h=row["body_height"]
        if abs(h-baseline)>TOLERANCE*baseline:
            raise AssertionError(f"{row['frame']}: visible body size {h:.1f} vs "
                f"IDLE baseline {baseline:.1f}; >5% divergence")
        if abs(row["ground_offset"])>4:
            raise AssertionError(f"{row['frame']}: feet are {row['ground_offset']:+.1f}px from root")
    for left,right in zip(dash_metrics,dash_metrics[1:]):
        delta=abs(left["body_height"]-right["body_height"])
        if delta>TOLERANCE*baseline:
            raise AssertionError(f"{left['frame']} -> {right['frame']}: "
                f"size jump {delta:.1f}px")
    # The approved natural 0-1-2-3 order closes with a substantially
    # smaller seam than the original 0-1-2-3-4-5 playback.
    features=[continuity.feature(x) for x in dash]
    old_close=continuity.distance(features[5],features[0])["score"]
    new_close=continuity.distance(features[3],features[0])["score"]
    if not new_close < old_close*0.85:
        raise AssertionError(f"DASH natural loop seam did not improve enough: {old_close} -> {new_close}")
    real_stages,timeline=continuity.project_report(ROOT,"p02","p02_dash",True)
    assert timeline==[0,1,2,3]
    results={name:continuity.analyze(frames,loop=True) for name,frames in real_stages.items()}
    assert not results["runtime_effective"]["suspect_transitions"]
    OUT.mkdir(parents=True,exist_ok=True)
    report={"size_reference":"P02 approved IDLE real Android pixels",
            "idle_median_body_height_px":baseline,
            "profile_standing_visual_height":nominal,
            "relative_tolerance":TOLERANCE,
            "dash_physical_atlas_cells":len(dash),
            "dash_playback_indices":timeline,
            "dash_frames":dash_metrics,
            "source_comparison":{name:{"median_motion":v["median_motion"],
                "suspected_transitions":v["suspect_transitions"]} for name,v in results.items()},
            "seam_score_before":old_close,
            "seam_score_after":new_close,
            "status":"PASS"}
    (OUT/"dash_size_and_loop.json").write_text(json.dumps(report,indent=2)+"\n")
    print(f"P02 DASH TRUE BODY SIZE PASS: IDLE={baseline:.2f}px, official={nominal:.2f}px")
    for row in dash_metrics:
        print(f"  {row['frame']}: height={row['body_height']:.2f}px, ground={row['ground_offset']:+.2f}px")
    print(f"DASH LOOP SEAM PASS: {old_close:.6f} -> {new_close:.6f}")
if __name__=="__main__":
    main()
