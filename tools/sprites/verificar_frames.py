#!/usr/bin/env python3
"""Verify temporal continuity of ONE real sprite clip, including last -> first.

Read-only diagnostic. Examines RGBA pixels and the frame sequence at three
levels where available: normalized video source, HD source, and the *actual*
packed Android atlas with SpriteAutoFix applied. Never redraws or reorders art.

Usage:
  python tools/sprites/verificar_frames.py --character p02 --clip p02_idle \
      --compare-source --output android/app/build/sprite-review/p02-idle-continuity
  python tools/sprites/verificar_frames.py --character p02 --clip p02_idle \
      --fail-on-break
"""
import argparse
import json
import math
import statistics
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SIZE = (128, 176)
RESAMPLE = Image.Resampling.BILINEAR


def source_sheet(path, width, height, columns, count):
    """Never accept a truncated/implicitly resized or blank sprite sheet."""
    with Image.open(path) as image:
        if image.mode != "RGBA":
            raise ValueError(f"{path}: sprite sheet must be RGBA")
        expected = (width * columns, height * math.ceil(count / columns))
        if image.size != expected:
            raise ValueError(f"{path}: {image.size}, expected {expected}")
        result = [image.crop(((i % columns) * width,
                              (i // columns) * height,
                              (i % columns + 1) * width,
                              (i // columns + 1) * height))
                  for i in range(count)]
    if any(frame.getchannel("A").getbbox() is None for frame in result):
        raise ValueError(f"{path}: blank frame")
    return result


def apply_runtime(frames, character, atlas_id, fixes, packed=None):
    """Project actual SpriteAutoFix uniform scale around authored atlas root.

    Normal renders are unchanged. For size patches, all projected frames use
    one equally padded diagnostic canvas so no frame is truncated and temporal
    comparisons are made at identical pixel density and on the same ground.
    Size, dx, and dy are exactly the parameters passed to drawAtlas().
    """
    matches = [x for x in fixes.get("corrections", [])
               if x.get("character") == character and x.get("atlas") == atlas_id]
    if len(matches) > 1:
        raise ValueError("Duplicate character/atlas correction")
    table = {}
    for patch in matches:
        for f, size, dx, dy in patch.get("frames", []):
            if f in table or not 0 <= f < len(frames):
                raise ValueError(f"Duplicate/out-of-range frame {f}")
            if not all(math.isfinite(float(v)) for v in (size, dx, dy)):
                raise ValueError("Nonfinite sprite correction")
            if not 0.8 <= size <= 1.25:
                raise ValueError("Unsafe/non-uniform correction")
            table[f] = (float(size), float(dx), float(dy))
    if not any(size != 1 for size, _, _ in table.values()):
        adjusted = []
        for i, frame in enumerate(frames):
            size, dx, dy = table.get(i, (1.0, 0.0, 0.0))
            if not float(dx).is_integer() or not float(dy).is_integer():
                raise ValueError(f"Frame {i}: fractional patch requires renderer projection")
            x, y = int(dx), int(dy)
            rgba = Image.new("RGBA", frame.size)
            rgba.paste(frame, (x, y))
            if np.count_nonzero(np.asarray(rgba)[..., 3]) != np.count_nonzero(np.asarray(frame)[..., 3]):
                raise ValueError(f"Frame {i}: runtime translation clips artwork")
            adjusted.append(rgba)
        return adjusted
    if packed is None:
        raise ValueError("Renderer projection requires authored atlas layout/root")
    px = int(packed["rootX"])
    py = int(packed["rootY"])
    pad = math.ceil(max(frames[0].size) * .3)
    resized=[]
    for i, frame in enumerate(frames):
        size, dx, dy = table.get(i, (1.0, 0.0, 0.0))
        width, height = frame.size
        scaled=frame.resize((round(width*size),round(height*size)),Image.Resampling.BILINEAR)
        x=round(pad+px+dx-px*size)
        y=round(pad+py+dy-py*size)
        result=Image.new("RGBA",(width+2*pad,height+2*pad))
        result.alpha_composite(scaled,(x,y))
        if np.count_nonzero(np.asarray(result)[..., 3]) < np.count_nonzero(np.asarray(scaled)[..., 3])*.99:
            raise ValueError(f"Frame {i}: runtime projection loses visible art")
        resized.append(result)
    return resized


def feature(image):
    """Pixels are resized only for comparison, NEVER changed in game resources."""
    thumb = image.resize(SIZE, RESAMPLE)
    rgba = np.asarray(thumb).astype(np.float32)
    alpha = rgba[..., 3] / 255.0
    mask = alpha > (32 / 255)
    yy, xx = np.where(mask)
    if len(xx) < 100:
        raise ValueError("Frame has no usable silhouette")
    # Composite, preserving color and silhouette, not comparing transparent RGB.
    rgb = rgba[..., :3] * alpha[..., None] + 235 * (1 - alpha[..., None])
    bbox = [int(xx.min()), int(yy.min()), int(xx.max()), int(yy.max())]
    centroid = [float(xx.mean()), float(yy.mean())]
    # Foot region is relative to *ground contact*, not an alpha-quantile
    # that shifts whenever breathing changes the number of opaque pixels.
    ground_band = yy >= (int(yy.max()) - max(3, int(SIZE[1] * .07)))
    foot = [float(np.median(xx[ground_band])), float(yy.max())]
    return {"rgb": rgb, "mask": mask, "bbox": bbox,
            "centroid": centroid, "foot": foot}


def distance(a, b):
    """Visible color change plus silhouette change, with explicit registration."""
    rgb = float(np.mean(np.abs(a["rgb"] - b["rgb"])) / 255.0)
    union = np.count_nonzero(a["mask"] | b["mask"])
    iou = np.count_nonzero(a["mask"] & b["mask"]) / max(union, 1)
    silhouette = float(1 - iou)
    move = math.hypot(a["centroid"][0] - b["centroid"][0],
                      a["centroid"][1] - b["centroid"][1])
    feet = math.hypot(a["foot"][0] - b["foot"][0], a["foot"][1] - b["foot"][1])
    # Contact-foot center can switch between two planted feet during
    # ordinary breathing. Keep it in diagnostics, not as dominant evidence.
    score = rgb + .20 * silhouette + .005 * move + .0005 * feet
    return {"score": float(score), "rgb": rgb, "silhouette": silhouette,
            "centroid_px": float(move), "feet_px": float(feet), "iou": float(iou)}


def analyze(frames, loop=True, min_floor=.020, ratio=2.25):
    """Detects isolated temporal jumps while preserving smooth breathing cycles.

    Every consecutive pair is measured. Circular local baseline is computed
    without the pair itself; stronger candidates stand above both local and
    global motion. Closing seam participates in that same analysis.
    """
    n = len(frames)
    if n < 4:
        raise ValueError("A minimum of four frames is required for temporal analysis")
    ff = [feature(f) for f in frames]
    transitions = [(i, (i+1) % n) for i in range(n if loop else n-1)]
    rows = [{"from": i, "to": j, "loop_seam": j == 0, **distance(ff[i], ff[j])}
            for i,j in transitions]
    scores = [r["score"] for r in rows]
    median = float(statistics.median(scores))
    mad = float(statistics.median(abs(v - median) for v in scores))
    for i, row in enumerate(rows):
        indices = ([(i+d) % len(scores) for d in (-3,-2,-1,1,2,3)]
                   if loop else [k for k in range(max(0,i-3),min(len(scores),i+4)) if k != i])
        local = float(statistics.median(scores[k] for k in indices))
        # Ignore harmless one-pixel breathing raster steps, even if the median is zero.
        # Require an appreciable absolute change as well as statistical spike.
        threshold = max(min_floor, ratio*median, ratio*local, median + 4*mad)
        if row["loop_seam"]:
            # Restart is especially perceptible: a moderate pose mismatch
            # at N-1 -> 0 must not hide behind the threshold for ordinary
            # breathing raster variation. It is a review flag, not an edit.
            seam_threshold = max(min_floor, 1.85*median, 1.60*local)
            threshold = min(threshold, seam_threshold)
        row["local_reference"] = round(local, 6)
        row["threshold"] = round(threshold, 6)
        row["ratio_to_median"] = round(row["score"]/max(median,1e-9), 3)
        row["suspect"] = bool(row["score"] > threshold)
        for key in ("score","rgb","silhouette","centroid_px","feet_px","iou"):
            row[key] = round(row[key], 6)
    suspects = [row for row in rows if row["suspect"]]
    top = sorted(rows,key=lambda r:r["score"],reverse=True)[:min(12,len(rows))]
    return {
        "frame_count":n, "transitions_tested":len(rows), "loop":bool(loop),
        "median_motion":round(median,6), "motion_mad":round(mad,6),
        "suspect_transitions":suspects,
        "top_transitions":top,
        "all_transitions":rows,
        "status":"REVIEW_REQUIRED" if suspects else "NO_AUTOMATIC_BREAK_DETECTED",
        "note":"Automatic visual signal, not a guarantee of perceptual continuity."
    }


def review_sheet(frames, analysis, filename):
    """Pairs at flagged/strongest boundaries; images remain unchanged."""
    ranked = analysis["suspect_transitions"] or analysis["top_transitions"]
    seen, chosen = set(), []
    for row in ranked + analysis["top_transitions"]:
        key = (row["from"], row["to"])
        if key not in seen and len(chosen) < 8:
            seen.add(key)
            chosen.append(row)
    w, h = 440, 268
    canvas = Image.new("RGB",(w*2,h*len(chosen)),(245,246,249))
    draw = ImageDraw.Draw(canvas)
    for k,row in enumerate(chosen):
        for side,idx in enumerate((row["from"],row["to"])):
            im=frames[idx]
            backdrop=Image.new("RGBA",im.size,(235,235,235,255))
            backdrop.alpha_composite(im)
            backdrop.thumbnail((w-18,h-38),RESAMPLE)
            x=side*w+(w-backdrop.width)//2
            y=k*h+28
            canvas.paste(backdrop.convert("RGB"),(x,y))
            draw.text((side*w+9,k*h+7),
                      f"frame {idx:02d} / {row['score']:.4f} "
                      + ("SUSPECT" if row["suspect"] else "candidate")
                      + (" / seam" if row["loop_seam"] else ""),
                      fill=(20,20,20))
    canvas.save(filename)


def project_report(root, character, clip_id, compare_source=False):
    clip_file=root/"tools/sprites/clips"/(clip_id+".json")
    cfg=json.loads(clip_file.read_text(encoding="utf-8"))
    if cfg["id"] != clip_id or Path(cfg["profile"]).stem != character:
        raise ValueError("Clip is not owned by the requested character")
    report=json.loads((root/cfg["report"]).read_text(encoding="utf-8"))
    packed=report["packed"]
    count=cfg["expectedFrames"]
    runtime=source_sheet(root/cfg["output"],packed["frameWidth"],
        packed["frameHeight"],packed["columns"],count)
    fixes=json.loads((root/"android/app/src/main/assets/sprite-autofix.json").read_text())
    stages = {"runtime_effective": apply_runtime(runtime,character,clip_id,fixes,packed)}
    if compare_source:
        layout=report["layout"]
        stages["hd_source"]=source_sheet(root/cfg["source"],layout["frameWidth"],
            layout["frameHeight"],layout["columns"],count)
        originals=cfg.get("sourceReferences",[])
        if originals:
            original_path=root/originals[0]
            if original_path.exists():
                # Video-normalized sheet has its own pre-SR frame size, but the
                # same order and columns; never infer continuity from canvas.
                with Image.open(original_path) as image:
                    fw=image.width//layout["columns"]
                    fh=image.height//math.ceil(count/layout["columns"])
                stages["video_normalized"]=source_sheet(
                    original_path,fw,fh,layout["columns"],count)
    # Pixel corrections use physical atlas indices. After applying those
    # corrections, inspect the real timeline of the character's animation,
    # rather than blindly considering every cell of a (possibly larger) atlas.
    timeline=list(range(count))
    character_file=root/"characters"/character/"character.json"
    if character_file.exists():
        definition=json.loads(character_file.read_text(encoding="utf-8"))
        dedicated_state=clip_id[len(character)+1:].upper()
        animation=definition.get("animations",{}).get(dedicated_state,{})
        if animation.get("atlas")==clip_id:
            timeline=animation["frames"]
            if not timeline or any(type(i) is not int or i<0 or i>=count for i in timeline):
                raise ValueError("Invalid atlas index in authored playback timeline")
    return {key:[frames[i] for i in timeline] for key,frames in stages.items()},timeline


def run(root,character,clip_id,output,compare_source=True):
    stages,playback_indices=project_report(root,character,clip_id,compare_source)
    output.mkdir(parents=True,exist_ok=True)
    results={}
    for name,frames in stages.items():
        result=analyze(frames,loop=True)
        results[name]=result
        review_sheet(frames,result,output/(name+"_suspicious_pairs.png"))
    stage_names=list(results)
    all_flags={stage:{f"{x['from']}->{x['to']}" for x in r["suspect_transitions"]}
               for stage,r in results.items()}
    output_data={"character":character,"clip":clip_id,"playback_indices":playback_indices,"source_layers":stage_names,
                 "stages":results,"flags_by_stage":{k:sorted(v) for k,v in all_flags.items()}}
    (output/"continuity.json").write_text(json.dumps(output_data,indent=2)+"\n")
    lines=[f"{character}/{clip_id} actual frame-continuity review"]
    for name,result in results.items():
        lines.append(f"{name}: {result['frame_count']} frames; "
                     f"{len(result['suspect_transitions'])} flagged; "
                     f"median={result['median_motion']:.6f}")
        for row in result["top_transitions"][:8]:
            lines.append(f"  {row['from']:02}->{row['to']:02} "
                         f"score={row['score']:.6f} "
                         f"reference={row['threshold']:.6f} "
                         f"{'FLAG' if row['suspect'] else 'inspect'} "
                         f"{'CLOSING-LOOP' if row['loop_seam'] else ''}")
    (output/"review.txt").write_text("\n".join(lines)+"\n")
    return output_data, "\n".join(lines)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--character",required=True,help="Character ID, e.g. p02")
    parser.add_argument("--clip",required=True,help="Only this atlas/animation, e.g. p02_idle")
    parser.add_argument("--compare-source",action="store_true")
    parser.add_argument("--output",type=Path,default=ROOT/"android/app/build/sprite-review/continuity")
    parser.add_argument("--fail-on-break",action="store_true",help="Opt-in quality gate")
    args=parser.parse_args()
    result,summary=run(ROOT,args.character,args.clip,args.output,args.compare_source)
    print(summary)
    # Always publish diagnostic evidence. Severity is a candidate for human
    # review; only explicit quality-gated invocations fail the pipeline.
    if args.fail_on_break and result["stages"]["runtime_effective"]["suspect_transitions"]:
        raise SystemExit("POTENTIAL_FRAME_BREAK: inspect the published report")


if __name__=="__main__":
    main()
