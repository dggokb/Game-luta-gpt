#!/usr/bin/env python3
"""Deterministic per-frame P01 visual calibration from authored sprite reports.

Only drawing transforms change. Never alter source PNGs, hitboxes or combat timing.
Run automatically by Gradle; source reports ship with the repository.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REPORTS = ROOT / "tools/sprites/reports"
STAND_SCALE = .980
SKIP = {"player_base_crouch", "player_base_rise"}  # pre-calibrated transitions


def clamp(value, lo, hi):
    return min(hi, max(lo, value))


def compute():
    idle = json.loads((REPORTS / "player_base_idle.report.json").read_text())
    left, top, right, bottom = idle["frames"][0]["outputBbox"]
    idle_height = (bottom - top) * STAND_SCALE
    idle_width = (right - left) * STAND_SCALE
    data, audit = {}, {}
    for filename in sorted(REPORTS.glob("player_base_*.report.json")):
        report = json.loads(filename.read_text())
        key = report["id"]
        if key in SKIP:
            audit[key] = {"frames": len(report["frames"]), "mode": "existing calibrated transition"}
            continue
        root_x = report["layout"]["rootX"]
        root_y = report["layout"]["rootY"]
        rows = []
        heights = []
        feet = []
        for entry in report["frames"]:
            box = entry["outputBbox"]
            width = box[2] - box[0]
            height = box[3] - box[1]
            cx = (box[0] + box[2]) * .5 - root_x
            foot = box[3] - root_y
            # A crouch or an aerial tuck is meant to be SHORTER than idle.
            # Normalizing it to a full upright figure would magnify it.
            if key == "player_base_idle":
                sx = sy = STAND_SCALE
            elif key == "player_base_dash":
                # v0.98: P01's low running pose was SMALLER than the idle
                # in real device tests (not larger). Lift both draw axes by
                # 15% from v0.97; a crouched running pose remains naturally
                # shorter, but the fighter's anatomy stops looking miniature.
                # Width is allowed to exceed idle from the extended stride.
                sx = min(.8395, idle_width * 1.518 / max(1, width))
                sy = 1.10975
            elif key in ("player_base_intro", "player_base_victory"):
                # The victory silhouette has arms compact against the body.
                # Matching its *overall* bbox to idle yielded a tiny figure.
                # 0.880 stands between the overlarge 0.95 and undersized 0.96
                # builds. Natural intro sizes continue using the idle reference.
                if key == "player_base_victory":
                    sx = sy = .880
                else:
                    sy = clamp(idle_height / max(1, height), .68, 1.10)
                    sx = min(sy, idle_width / max(1, width)) if entry["index"] >= 20 else sy
            elif key == "player_base_crouch_light":
                sx = sy = .92
            elif key == "player_base_crouch_medium":
                sx = sy = .94
            elif key == "player_base_crouch_heavy":
                # 2H naturally extends one arm above the standing fighter.
                # Keep the body WIDTH close to idle; don't shrink the whole
                # character merely because its active pose raises a hand.
                target_height = idle_height * (.76 if height < 380 else 1.16)
                sy = clamp(target_height / max(1, height), .73, 1.12)
                sx = clamp(idle_width * 1.02 / max(1, width), .86, 1.05)
            elif key == "player_base_backdash":
                sy = min(.98, idle_height / max(1, height))
                sx = min(sy, idle_width * 1.40 / max(1, width))
            elif key in ("player_base_jump", "player_base_fall_air",
                         "player_base_jump_light", "player_base_jump_medium",
                         "player_base_jump_heavy", "player_base_jump_heavy_down"):
                sx = sy = min(.98, idle_height / max(1, height))
            else:
                sx = sy = min(.98, idle_height / max(1, height))
                if key not in ("player_base_fall", "player_base_getup"):
                    sx = min(sx, idle_width * 1.55 / max(1, width))
            if key in ("player_base_intro", "player_base_victory"):
                dx = -cx * sx
            else:
                dx = 0.0  # animation extensions must NOT shift the fighter root
            # Leave airborne/hop feet elevated; ground-based poses align to the floor.
            airborne = key in ("player_base_jump", "player_base_fall_air",
                              "player_base_jump_light", "player_base_jump_medium",
                              "player_base_jump_heavy", "player_base_jump_heavy_down",
                              "player_base_backdash", "player_base_defense_air",
                              "player_base_hit_air", "player_base_special_s2",
                              "player_base_special_s4")
            dy = 0.0 if airborne else -foot * sy
            rows.append((sx, sy, dx, dy))
            heights.append(round(height * sy, 2))
            feet.append(round(foot * sy + dy, 3))
        data[key] = rows
        audit[key] = {
            "frames": len(rows),
            "mode": "per-frame idle calibrated",
            "min_drawn_height": min(heights),
            "max_drawn_height": max(heights),
            "max_abs_ground_foot_error": max(abs(x) for x in feet)
        }
    return data, audit, idle_height, idle_width


def make_java(data):
    out = [
        "package com.gamelutagpt;",
        "import java.util.HashMap;",
        "/** Auto-generated per-frame drawing offsets from real alpha bounds.",
        " * Generated by tools/sprites/generate_p01_calibration.py. */",
        "final class P01SpriteCalibration {",
        " private static final HashMap<String,float[][]> VALUES = new HashMap<>();",
        " private static final float[] DEFAULT = {1f,1f,0f,0f};",
        " static {"
    ]
    for name, rows in sorted(data.items()):
        out.append('  VALUES.put("' + name + '",new float[][]{')
        for i in range(0, len(rows), 4):
            chunk = rows[i:i+4]
            out.append("   " + ",".join("{" + ",".join(f"{value:.4f}f" for value in row) + "}" for row in chunk) + ("," if i+4 < len(rows) else ""))
        out.append("  });")
    out += [
        " }",
        " static float[] get(String atlas,int frame) {",
        "  float[][] entries=VALUES.get(atlas);",
        "  if(entries==null)return DEFAULT;",
        "  return entries[Math.max(0,Math.min(frame,entries.length-1))];",
        " }",
        " static int frames(String atlas) {",
        "  float[][] entries=VALUES.get(atlas);return entries==null?0:entries.length;",
        " }",
        " private P01SpriteCalibration() {}",
        "}"
    ]
    return "\n".join(out)+"\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    data, audit, stand_h, stand_w = compute()
    expected = len(list(REPORTS.glob("player_base_*.report.json")))
    assert len(data) + len(SKIP) == expected, "Missing atlas calibration"
    assert len(data) >= 35, "Missing P01 sprite states"
    for key in ("player_base_dash", "player_base_crouch_light",
                "player_base_crouch_medium", "player_base_crouch_heavy",
                "player_base_jump", "player_base_victory", "player_base_intro"):
        assert key in data and data[key]
    assert len(data["player_base_victory"]) == 33, "Victory must normalize every source frame"
    assert all(item["max_abs_ground_foot_error"] < .01 for key,item in audit.items()
               if "max_abs_ground_foot_error" in item and key not in
               ("player_base_jump", "player_base_fall_air", "player_base_backdash",
                "player_base_hit_air", "player_base_defense_air",
                "player_base_jump_light", "player_base_jump_medium",
                "player_base_jump_heavy", "player_base_jump_heavy_down",
                "player_base_special_s2", "player_base_special_s4")), "Ground foot drift"
    destination = Path(args.output)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(make_java(data))
    audit_file = ROOT / "android/app/build/sprite-review/p01-idle-master-audit.json"
    audit_file.parent.mkdir(parents=True, exist_ok=True)
    audit_file.write_text(json.dumps({"idle_visible_width":stand_w,"idle_visible_height":stand_h,
                                       "atlas_count":expected,"states":audit},indent=2))
    print(f"P01 calibration PASS: {expected} sprite atlases, {sum(map(len,data.values()))} frames; idle {stand_w:.1f} x {stand_h:.1f}")


if __name__ == "__main__":
    main()
