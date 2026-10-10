#!/usr/bin/env python3
"""P12 semantic source gate.

A different set of pixel hashes does NOT prove a new animation. The rejected
2026-10-10 pilot moved the same idle drawing for 42 states, yielding obviously
idle-like attacks and walking. This check demands independently authored /
generated action *poses* or motion clips BEFORE the pack can be approved.

Accepted motion sources for each non-neutral state:
- art/videos/p12/<state>.mp4 (or .webm): full action video, or
- art/keys/p12/poses/<state>_start.png AND <state>_peak.png: distinct
  action-relevant key poses. A state may also have additional key poses.

These are minimum evidence requirements, not automatic semantic/art approval.
Neither the IDLE art nor atlases produced by p12_full_cycle.py qualify.
"""
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "art/keys/p12/idle_base.png"
PACK = ROOT / "characters/p12/character.json"
POSE_DIR = ROOT / "art/keys/p12/poses"
VIDEO_DIR = ROOT / "art/videos/p12"
NEUTRAL = {"IDLE", "COMBAT"}


def digest(path):
    return hashlib.sha256(path.read_bytes()).digest()


def main():
    if not PACK.exists() or not BASE.exists():
        raise SystemExit("P12 semantic gate: character pack or canonical source art missing.")
    pack = json.loads(PACK.read_text(encoding="utf-8"))
    base_sha = digest(BASE)
    lacking = []
    accepted = []
    for state in pack["animations"]:
        if state in NEUTRAL:
            continue
        tag = state.lower()
        videos = [VIDEO_DIR / f"{tag}{ext}" for ext in (".mp4", ".webm", ".mov")]
        video = next((f for f in videos if f.is_file() and f.stat().st_size > 1024), None)
        start = POSE_DIR / f"{tag}_start.png"
        peak = POSE_DIR / f"{tag}_peak.png"
        key_pair = (start.exists() and peak.exists()
                    and start.stat().st_size > 1024 and peak.stat().st_size > 1024
                    and digest(start) != digest(peak)
                    and digest(start) != base_sha and digest(peak) != base_sha)
        # Owning independent files is insufficient: the generated animation
        # MUST prove that its clip actually consumed those source files.
        bound = set()
        cfg = ROOT / "tools/sprites/clips" / f"p12_full_{tag}.json"
        if cfg.is_file():
            clip = json.loads(cfg.read_text(encoding="utf-8"))
            bound = set(clip.get("sourceReferences", []))
        used_video = video is not None and video.relative_to(ROOT).as_posix() in bound
        used_keys = (bool(key_pair)
                     and start.relative_to(ROOT).as_posix() in bound
                     and peak.relative_to(ROOT).as_posix() in bound)
        if used_video or used_keys:
            accepted.append(state)
        else:
            lacking.append(state)

    print(f"P12 semantic source gate: {len(accepted)}/{len(pack['animations']) - len(NEUTRAL)}"
          " action states have independent motion source.", flush=True)
    if lacking:
        print("REJECTED: P12 remains an IDLE-warp prototype. Real distinct poses or"
              " independent movement video are missing OR not actually used by the renderer for:", file=sys.stderr)
        for state in lacking:
            print(f"  {state}", file=sys.stderr)
        print("Accepted examples: art/videos/p12/light_jab.mp4 OR "
              "art/keys/p12/poses/light_jab_start.png + light_jab_peak.png.",
              file=sys.stderr)
        print("The existing generated p12_full_* atlases do not constitute action"
              " sources. Do not publish APK as art-approved.", file=sys.stderr)
        return 1

    print("All states have independent sources. A human visual review is STILL required.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
