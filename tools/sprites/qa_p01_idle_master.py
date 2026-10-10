#!/usr/bin/env python3
"""Fail CI if any P01 atlas is missing its frame-by-frame idle-master calibration."""
from pathlib import Path
from generate_p01_calibration import (
    compute, ROOT, REPORTS, SKIP, victory_foot_track, VICTORY_PLANTED_START
)
import json

def run():
    transforms, stats, stand_h, stand_w = compute()
    atlases = list(REPORTS.glob("player_base_*.report.json"))
    assert len(atlases) >= 39, len(atlases)
    assert len(transforms) + len(SKIP) == len(atlases)
    for p in atlases:
        report = json.loads(p.read_text())
        name = report["id"]
        if name in SKIP:
            assert "existing" in stats[name]["mode"]
            continue
        frames = transforms[name]
        assert len(frames) == len(report["frames"]), name
        for index, (sx, sy, dx, dy) in enumerate(frames):
            assert 0.50 <= sx <= 1.12, (name,index,sx)
            assert 0.50 <= sy <= 1.12, (name,index,sy)
            assert abs(dx) <= 150 and abs(dy) <= 120, (name,index,dx,dy)
            bounds = report["frames"][index]["outputBbox"]
            foot = bounds[3]-report["layout"]["rootY"]
            if name not in ("player_base_jump", "player_base_fall_air",
                            "player_base_jump_light", "player_base_jump_medium",
                            "player_base_jump_heavy", "player_base_jump_heavy_down",
                            "player_base_backdash", "player_base_defense_air",
                            "player_base_hit_air", "player_base_special_s2",
                            "player_base_special_s4"):
                assert abs(foot*sy + dy) < .02, (name,index,"feet",foot*sy+dy)
    for name in ("player_base_intro","player_base_victory"):
        atlas=json.loads((REPORTS/(name+".report.json")).read_text())
        for i,(sx,sy,dx,dy) in enumerate(transforms[name]):
            x0,y0,x1,y1=atlas["frames"][i]["outputBbox"]
            center=(x0+x1)*.5-atlas["layout"]["rootX"]
            if name=="player_base_intro":
                assert abs(center*sx+dx)<.01, (name,i,"intro lateral drift")
                assert abs((y1-y0)*sy-stand_h)<.6, (name,i,"size")
            else:
                # Victory has a narrow standing silhouette; full-height
                # bounding-box equality made it visually undersized.
                assert .85<=sy<=.93, (name,i,"victory calibrated size")
    victory=json.loads((REPORTS/"player_base_victory.report.json").read_text())
    planted=transforms["player_base_victory"]
    shoes=victory_foot_track(victory)
    root=victory["packed"]["rootX"]
    world_shoe=[(shoes[i]-root)*planted[i][0]+planted[i][2]
                for i in range(VICTORY_PLANTED_START,len(planted))]
    assert max(world_shoe)-min(world_shoe)<.002, (
        "Victory shoe drift is not locked",world_shoe)
    assert all(abs(planted[i][0]-.880)<.0001 and abs(planted[i][1]-.880)<.0001
               for i in range(VICTORY_PLANTED_START,len(planted))), "Victory size changed"
    # This tracks planted shoes; tests based on the whole silhouette's
    # center would hide the source artwork's real lateral displacement.
    dash=json.loads((REPORTS/"player_base_dash.report.json").read_text())
    for i,frame in enumerate(dash["frames"]):
        a,b,c,d=frame["outputBbox"]
        assert (c-a)*transforms["player_base_dash"][i][0] <= stand_w*1.525
    # v0.98 boosts dash uniformly relative to v0.97; the naturally low
    # running pose must stay shorter than idle without miniaturizing its body.
    for sx,sy,dx,dy in transforms["player_base_dash"]:
        assert .80 <= sx <= .845, (sx,sy)
        assert 1.105 <= sy <= 1.115, (sx,sy)

    # Keep the extended arm of the crouching launcher, while preserving
    # the torso width near idle (v0.96 shrank it by 34%).
    h=json.loads((REPORTS/"player_base_crouch_heavy.report.json").read_text())
    for frame in (4,5,6,7):
        a,b,c,d=h["frames"][frame]["outputBbox"]
        sx,sy,dx,dy=transforms["player_base_crouch_heavy"][frame]
        assert .94*stand_w <= (c-a)*sx <= 1.08*stand_w
        assert 1.13*stand_h <= (d-b)*sy <= 1.18*stand_h

    # Ensure squat and aerial poses are not enlarged into standing characters.
    for name in ("player_base_crouch_light","player_base_crouch_medium",
                 "player_base_jump","player_base_fall_air"):
        assert all(sx <= 1 for sx,_,_,_ in transforms[name])
    print("P01 IDLE MASTER PASS:",len(atlases),"atlases;",
          sum(len(x) for x in transforms.values()),"frames;",
          "idle",stand_w,stand_h)

if __name__=="__main__":
    run()
