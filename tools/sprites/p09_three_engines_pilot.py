#!/usr/bin/env python3
"""Disposable P09 pilot: real P09 concept art -> loop -> Real-ESRGAN -> Character Pack.

All generated assets live in the CI checkout, never in the source branch. This is
an engineering fixture (idle in every combat state), NOT finished P09 animation.
"""
import argparse
import hashlib
import json
import math
import sys
from pathlib import Path

from PIL import Image
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
FRAMES = 8
WIDTH = HEIGHT = 256
ROOT_X, ROOT_Y = 128, 238
MODEL_SHA256 = "b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d"


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf8")


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build_master(key):
    picture = Image.open(key).convert("RGBA")
    alpha = picture.getchannel("A")
    bounds = alpha.point(lambda a: 255 if a >= 12 else 0).getbbox()
    if not bounds:
        raise ValueError("P09 idle_base is empty")
    if alpha.getextrema()[0] == 255:
        raise ValueError("P09 idle_base has opaque background; need a transparent fighter")
    cropped = picture.crop(bounds)
    factor = min(209 / cropped.height, 216 / cropped.width)
    size = (max(1, round(cropped.width * factor)), max(1, round(cropped.height * factor)))
    cropped = cropped.resize(size, Image.Resampling.LANCZOS)
    image = Image.new("RGBA", (WIDTH, HEIGHT))
    image.alpha_composite(cropped, ((WIDTH - size[0]) // 2, ROOT_Y - size[1] + 1))
    if not image.getchannel("A").getbbox():
        raise ValueError("P09 master lost its pixels")
    return image


def breathing_frame(master, index):
    # Deform only the upper half by <=2px; base/feet stay fixed. Loop is periodic.
    arr = np.asarray(master).copy()
    result = np.zeros_like(arr)
    phase = math.sin(2 * math.pi * index / FRAMES)
    for y in range(HEIGHT):
        shift = round(2.0 * phase * max(0, 1 - y / 235) ** 2)
        if shift >= 0:
            result[y, shift:, :] = arr[y, :WIDTH-shift, :]
        else:
            result[y, :shift, :] = arr[y, -shift:, :]
    return Image.fromarray(result, "RGBA")


def super_resolution(frames, weights):
    import torch
    from scipy.ndimage import distance_transform_edt
    sys.path.insert(0, str(ROOT / "art/redraws/p03_hd/idle_sr_4x"))
    from srvgg_arch import SRVGGNetCompact

    if digest(weights) != MODEL_SHA256:
        raise ValueError("Wrong model SHA256 (expected official AnimeVideo-v3)")
    torch.set_num_threads(3)
    torch.manual_seed(0)
    model = SRVGGNetCompact(num_in_ch=3, num_out_ch=3, num_feat=64,
                           num_conv=16, upscale=4, act_type="prelu")
    state = torch.load(weights, map_location="cpu", weights_only=True)
    model.load_state_dict(state["params"], strict=True)
    model.eval()
    improved = []
    for index, frame in enumerate(frames):
        rgba = np.asarray(frame).copy()
        missing = rgba[:, :, 3] < 128
        _, neighbors = distance_transform_edt(missing, return_indices=True)
        rgb = rgba[:, :, :3].copy()
        rgb[missing] = rgb[neighbors[0][missing], neighbors[1][missing]]
        tensor = torch.from_numpy(np.ascontiguousarray(rgb.transpose(2, 0, 1))).float()[None] / 255
        with torch.inference_mode():
            upscale = model(tensor)[0].clamp(0, 1).permute(1, 2, 0).numpy()
        master4 = Image.fromarray(np.rint(upscale * 255).astype("uint8"), "RGB")
        master4.putalpha(frame.getchannel("A").resize((1024, 1024), Image.Resampling.LANCZOS))
        cell = master4.convert("RGBa").resize((512, 512), Image.Resampling.LANCZOS).convert("RGBA")
        final_alpha = frame.getchannel("A").resize((512, 512), Image.Resampling.LANCZOS)
        cell.putalpha(final_alpha)
        if cell.getchannel("A").tobytes() != final_alpha.tobytes():
            raise ValueError(f"Frame {index} lost original alpha")
        improved.append(cell)
        print(f"P09 pilot SR {index+1}/{len(frames)}", flush=True)
    return improved


def grid(frames, width, height):
    sheet = Image.new("RGBA", (width * 4, height * 2))
    for index, frame in enumerate(frames):
        sheet.paste(frame, ((index % 4) * width, (index // 4) * height))
    return sheet


def prepare(weights, results):
    key = ROOT / "art/keys/p09/idle_base.png"
    concept = ROOT / "art/concepts/p09.png"
    if not key.is_file() or not concept.is_file():
        raise FileNotFoundError("P09 concept or transparent idle_base missing")
    existing = ROOT / "characters/p09/character.json"
    if existing.exists():
        registered = json.loads(existing.read_text())
        if registered.get("displayName") != "P09 BOMBEIRO - PILOTO":
            raise RuntimeError("P09 is already officially registered; refusing to overwrite a real fighter")
    master = build_master(key)
    frames = [breathing_frame(master, i) for i in range(FRAMES)]
    original = ROOT / "art/sprites/source/p09_pilot_idle_original.png"
    original.parent.mkdir(parents=True, exist_ok=True)
    grid(frames, 256, 256).save(original)
    better = super_resolution(frames, weights)
    improved = ROOT / "art/sprites/source/p09_pilot_idle_sr2x.png"
    grid(better, 512, 512).save(improved)

    profile = json.loads((ROOT / "tools/sprites/profiles/player_base.json").read_text())
    profile["id"] = "p09"
    profile["standingVisualHeight"] = 209
    profile["anatomyReference"] = {
        "source": "art/sprites/source/p09_pilot_idle_original.png",
        "frame": 0, "columns": 4, "frameWidth": 256, "frameHeight": 256,
        "bands": [[0, 0.12], [0.05, 0.20], [0.25, 0.40]],
        "maxScaleSpreadRatio": 0.15
    }
    profile["validation"]["minOpaquePixels"] = 800
    dump(ROOT / "tools/sprites/profiles/p09.json", profile)
    clip = {
        "id": "p09_pilot_idle", "javaName": "P09_PILOT_IDLE",
        "profile": "p09.json",
        "source": "art/sprites/source/p09_pilot_idle_sr2x.png",
        "sourceReferences": ["art/sprites/source/p09_pilot_idle_original.png"],
        "output": "android/app/src/main/res/drawable-nodpi/p09_pilot_idle.png",
        "report": "tools/sprites/reports/p09_pilot_idle.report.json",
        "preview": "android/app/build/sprite-review/p09_pilot_idle.png",
        "segmentation": "prepared-grid", "rootMode": "authored",
        "expectedFrames": 8, "columns": 4, "frameWidth": 512, "frameHeight": 512,
        "rootX": 256, "rootY": 476, "pixelScale": 2,
        "sourceNote": "P09 authentic concept idle; 8 procedural breathing frames; shared placeholder for every other state"
    }
    dump(ROOT / "tools/sprites/clips/p09_pilot_idle.json", clip)

    # Exercise real Character Pack generation without changing any existing fighter.
    # All states use a P09 pose as a temporary PLACEHOLDER; no p01 art is borrowed.
    pack = json.loads((ROOT / "characters/player_base/character.json").read_text())
    pack.update(id="p09", displayName="P09 BOMBEIRO - PILOTO", profile="p09.json")
    for animation in pack["animations"].values():
        animation["atlas"] = "p09_pilot_idle"
        animation["frames"] = [frame % FRAMES for frame in animation["frames"]]
    dump(ROOT / "characters/p09/character.json", pack)
    roster_path = ROOT / "characters/roster.json"
    roster = json.loads(roster_path.read_text())
    if "p09" not in roster["selectable"]:
        roster["selectable"].append("p09")
    if ["p09", "p02"] not in roster["teams"]:
        roster["teams"].append(["p09", "p02"])
    dump(roster_path, roster)

    results.mkdir(parents=True, exist_ok=True)
    manifest = {
        "character": "p09", "character_source": str(key.relative_to(ROOT)),
        "concept_sha256": digest(concept), "key_sha256": digest(key),
        "model_sha256": digest(weights), "original_sha256": digest(original),
        "enhanced_sha256": digest(improved), "frames": FRAMES,
        "pixelScale": 2, "masterScale": 4,
        "status": "ENGINEERING FIXTURE - only IDLE artwork, placeholder states",
        "claim": "Validates ingestion and enhancement, not final P09 combat animations"
    }
    dump(results / "p09-manifest.json", manifest)
    grid(better, 512, 512).save(results / "p09-idle-enhanced.png")
    grid(frames, 256, 256).save(results / "p09-idle-original.png")
    print("P09 PILOT PREPARED: authentic P09 artwork / 8 SR frames / temporary Character Pack")


def audit(results):
    sys.path.insert(0, str(ROOT / "tools/sprites"))
    import harmony
    import auditoria
    rows, problems = harmony.audit(only_characters={"p09"})
    p09_rows = [row for row in rows if row["character"] == "p09"]
    p09_problems = [p for p in problems if p.startswith("p09/")]
    notices = auditoria.audit("p09")
    errors = [n for n in notices if n["level"] == "erro"]
    report = {
        "character": "p09", "harmony_frames": len(p09_rows),
        "harmony_errors": p09_problems,
        "auditoria": notices,
        "auditoria_error_count": len(errors),
        "limitations": ["only genuine P09 IDLE pose", "other states are explicit placeholders",
                        "moves/timing cloned for smoke test, not balanced gameplay"]
    }
    dump(results / "p09-audit.json", report)
    if not p09_rows or p09_problems:
        raise SystemExit("P09 pilot failed harmony audit: " + str(p09_problems))
    print(f"P09 HARMONY PASS ({len(p09_rows)} animation-frame samples); "
          f"full audit produced {len(notices)} observations, {len(errors)} pending errors.")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["prepare", "audit"])
    ap.add_argument("--weights", type=Path)
    ap.add_argument("--results", type=Path, default=ROOT / "android/app/build/p09-pilot")
    args = ap.parse_args()
    if args.mode == "prepare":
        if args.weights is None:
            ap.error("prepare requires --weights")
        prepare(args.weights, args.results)
    else:
        audit(args.results)
