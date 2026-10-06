#!/usr/bin/env python3
import json
import math
import re
import statistics
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
CLIPS_DIR = ROOT / "tools" / "sprites" / "clips"
PROFILES_DIR = ROOT / "tools" / "sprites" / "profiles"
GENERATED_JAVA = ROOT / "android" / "app" / "src" / "main" / "java" / "com" / "gamelutagpt" / "GeneratedSpriteLayouts.java"

def load_json(path):
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh)

def threshold_alpha(image, threshold):
    alpha = image.getchannel("A")
    return alpha.point(lambda p: 255 if p > threshold else 0)

def occupied_intervals(mask, axis):
    # axis="x": intervals of columns containing any opaque pixel.
    # axis="y": intervals of rows containing any opaque pixel.
    px = mask.load()
    width, height = mask.size
    length = width if axis == "x" else height
    other = height if axis == "x" else width
    occupied = []
    for i in range(length):
        present = False
        for j in range(other):
            value = px[i, j] if axis == "x" else px[j, i]
            if value:
                present = True
                break
        occupied.append(present)
    result = []
    start = None
    for i, present in enumerate(occupied):
        if present and start is None:
            start = i
        elif not present and start is not None:
            result.append((start, i))
            start = None
    if start is not None:
        result.append((start, length))
    return result

def bbox_for(image, threshold):
    bbox = threshold_alpha(image, threshold).getbbox()
    if bbox is None:
        raise ValueError("frame is fully transparent")
    return bbox

def detect_ground_root(image, bbox, threshold, foot_band_ratio, min_foot_width):
    x0, y0, x1, y1 = bbox
    max_y = y1 - 1
    band = max(4, int((y1 - y0) * foot_band_ratio))
    mask = threshold_alpha(image, threshold)
    foot_band = mask.crop((0, max(y0, max_y - band + 1), image.width, max_y + 1))
    intervals = [
        interval for interval in occupied_intervals(foot_band, "x")
        if interval[1] - interval[0] >= min_foot_width
    ]
    centers = [(left + right - 1) / 2.0 for left, right in intervals]
    if len(centers) >= 2:
        # Registration point is the midpoint between the outer grounded feet.
        root_x = (min(centers) + max(centers)) / 2.0
    elif len(centers) == 1:
        root_x = centers[0]
    else:
        root_x = (x0 + x1 - 1) / 2.0
    return root_x, float(max_y), intervals

def ceil_step(value, step):
    return int(math.ceil(value / step) * step)

def sanitize_java_name(value):
    return re.sub(r"[^A-Z0-9_]", "_", value.upper())

def anatomy_signature(image, bbox, threshold, bands):
    """Measure a compact silhouette signature for one comparable standing pose."""
    mask = threshold_alpha(image, threshold)
    px = mask.load()
    x0, y0, x1, y1 = bbox
    height = y1 - y0
    values = []
    for pair in bands:
        if not isinstance(pair, list) or len(pair) != 2:
            raise ValueError("anatomy bands must be [start,end] pairs")
        start, end = float(pair[0]), float(pair[1])
        if not 0.0 <= start < end <= 1.0:
            raise ValueError("anatomy bands must stay inside 0..1")
        top = max(y0, int(math.floor(y0 + start * height)))
        bottom = min(y1, int(math.ceil(y0 + end * height)))
        widths = []
        for y in range(top, bottom):
            xs = [x for x in range(image.width) if px[x, y]]
            if xs:
                widths.append(max(xs) - min(xs) + 1)
        if not widths:
            raise ValueError("anatomy band contains no visible pixels")
        values.append(float(statistics.median(widths)))
    return {"height": height, "bands": values}

def canonical_anatomy_reference(profile, threshold, override=None):
    """Reviewed pose the clip is scaled against.

    The profile's reference is the canonical Idle guard. A clip whose comparable pose is
    not standing (e.g. a crouching attack) may point to another reviewed master of the
    same character instead, with its own cell size; scale stays tied to approved art.
    """
    anatomy = override if override is not None else profile.get("anatomyReference")
    if not isinstance(anatomy, dict):
        raise ValueError(f"{profile['id']}: canonical-anatomy requires anatomyReference")
    path = (ROOT / anatomy["source"]).resolve()
    if not path.is_relative_to(ROOT.resolve()):
        raise ValueError("anatomy reference path escapes project")
    sheet = Image.open(path).convert("RGBA")
    width = int(anatomy.get("frameWidth", profile["baseFrameWidth"]))
    height = int(anatomy.get("frameHeight", profile["baseFrameHeight"]))
    columns = int(anatomy["columns"])
    frame = int(anatomy["frame"])
    if columns <= 0 or sheet.width % width or sheet.height % height:
        raise ValueError(f"{profile['id']}: invalid anatomy reference grid")
    actual_columns = sheet.width // width
    rows = sheet.height // height
    if columns != actual_columns or frame < 0 or frame >= columns * rows:
        raise ValueError(f"{profile['id']}: anatomy reference frame is outside the grid")
    x = frame % columns * width
    y = frame // columns * height
    cell = sheet.crop((x, y, x + width, y + height))
    bbox = bbox_for(cell, threshold)
    bands = anatomy.get("bands", [[0.0, 0.12], [0.05, 0.20], [0.25, 0.40]])
    return anatomy, anatomy_signature(cell, bbox, threshold, bands)

def process_clip(config_path):
    cfg = load_json(config_path)
    profile = load_json(PROFILES_DIR / cfg["profile"])
    validation = profile["validation"]
    threshold = int(validation["alphaThreshold"])
    min_margin = int(validation["minMargin"])
    root_tolerance = float(validation["rootTolerance"])
    width_step = int(validation["widthStep"])
    height_step = int(validation.get("heightStep", width_step))
    foot_band_ratio = float(validation["footBandRatio"])
    min_foot_width = int(validation["minFootWidth"])
    min_opaque = int(validation["minOpaquePixels"])
    max_upscale = float(validation["maxUpscale"])

    source_path = ROOT / cfg["source"]
    output_path = ROOT / cfg["output"]
    report_path = ROOT / cfg["report"]
    preview_path = ROOT / cfg["preview"]

    if cfg.get("segmentation") == "prepared-grid":
        return process_prepared(cfg, profile)

    source = Image.open(source_path).convert("RGBA")
    mask = threshold_alpha(source, threshold)

    segmentation = cfg.get("segmentation", "horizontal-alpha-components")
    expected = int(cfg["expectedFrames"])

    if segmentation == "horizontal-alpha-components":
        x_intervals = occupied_intervals(mask, "x")
        regions = [
            (left, 0, right, source.height)
            for left, right in x_intervals
        ]
    elif segmentation == "grid-alpha-components":
        x_intervals = occupied_intervals(mask, "x")
        y_intervals = occupied_intervals(mask, "y")
        regions = []
        for top, bottom in y_intervals:
            for left, right in x_intervals:
                region_mask = mask.crop((left, top, right, bottom))
                if region_mask.getbbox() is not None:
                    regions.append((left, top, right, bottom))
    else:
        raise ValueError(f"unsupported segmentation mode: {segmentation}")

    if len(regions) != expected:
        raise ValueError(
            f"{cfg['id']}: detected {len(regions)} frame regions, expected {expected}. "
            f"Regions={regions}"
        )

    intervals = [(left, right) for left, _, right, _ in regions]
    frames = []
    for index, (left, top, right, bottom) in enumerate(regions):
        frame = source.crop((left, top, right, bottom))
        bbox = bbox_for(frame, threshold)
        root_mode = cfg.get("rootMode", "ground-feet")
        if root_mode != "ground-feet":
            raise ValueError(f"unsupported root mode: {root_mode}")
        root_x, root_y, foot_intervals = detect_ground_root(
            frame, bbox, threshold, foot_band_ratio, min_foot_width
        )
        frame_info = {
            "index": index,
            "image": frame,
            "bbox": bbox,
            "sourceInterval": [left, right],
            "sourceRoot": [root_x, root_y],
            "footIntervals": foot_intervals,
        }
        if segmentation == "grid-alpha-components":
            frame_info["sourceRect"] = [left, top, right, bottom]
        frames.append(frame_info)

    scale_mode = cfg.get("scaleMode", "median-standing-height")
    anatomy_report = None
    if scale_mode == "canonical-anatomy":
        reference_index = int(cfg.get("anatomyReferenceFrame", -1))
        if reference_index < 0 or reference_index >= expected:
            raise ValueError(f"{cfg['id']}: invalid anatomyReferenceFrame")
        override = cfg.get("anatomyReference")
        if override is not None:
            # Inherit bands/tolerance from the profile unless the clip overrides them.
            override = {**profile.get("anatomyReference", {}), **override}
        anatomy_cfg, canonical_signature = canonical_anatomy_reference(
            profile, threshold, override
        )
        bands = anatomy_cfg.get(
            "bands", [[0.0, 0.12], [0.05, 0.20], [0.25, 0.40]]
        )
        source_signature = anatomy_signature(
            frames[reference_index]["image"],
            frames[reference_index]["bbox"],
            threshold,
            bands,
        )
        candidates = [
            canonical_signature["height"] / source_signature["height"]
        ] + [
            canonical / source
            for canonical, source in zip(
                canonical_signature["bands"], source_signature["bands"]
            )
        ]
        scale = float(statistics.median(candidates))
        spread = (max(candidates) - min(candidates)) / scale
        max_spread = float(anatomy_cfg.get("maxScaleSpreadRatio", 0.15))
        if spread > max_spread:
            raise ValueError(
                f"{cfg['id']}: anatomy reference frame {reference_index} is not "
                f"comparable to the canonical pose (spread={spread:.4f}, "
                f"limit={max_spread:.4f})"
            )
        anatomy_report = {
            "mode": "canonical-anatomy",
            "clipReferenceFrame": reference_index,
            "referenceSource": anatomy_cfg["source"],
            "referenceFrame": int(anatomy_cfg["frame"]),
            "canonicalSignature": canonical_signature,
            "sourceSignature": source_signature,
            "scaleCandidates": candidates,
            "scaleSpreadRatio": spread,
            "maxScaleSpreadRatio": max_spread,
            "passed": True,
        }
    elif scale_mode == "median-standing-height":
        reference_frames = cfg.get("scaleReferenceFrames")
        if reference_frames is None:
            reference_frames = list(range(expected))
        heights = []
        for index in reference_frames:
            index = int(index)
            if index < 0 or index >= expected:
                raise ValueError(f"{cfg['id']}: invalid scaleReferenceFrames")
            x0, y0, x1, y1 = frames[index]["bbox"]
            heights.append(y1 - y0)
        source_reference_height = statistics.median(heights)
        target_height = float(profile["standingVisualHeight"])
        scale = target_height / source_reference_height
    else:
        raise ValueError(f"{cfg['id']}: unsupported scaleMode {scale_mode}")
    if scale > max_upscale:
        raise ValueError(
            f"{cfg['id']}: source would require upscaling {scale:.3f}x; "
            f"maximum is {max_upscale:.3f}x"
        )

    max_left = max_right = max_top = max_bottom = 0.0
    for frame in frames:
        x0, y0, x1, y1 = frame["bbox"]
        root_x, root_y = frame["sourceRoot"]
        left_extent = (root_x - x0) * scale
        right_extent = (x1 - 1 - root_x) * scale
        top_extent = (root_y - y0) * scale
        bottom_extent = (y1 - 1 - root_y) * scale
        frame["scaledExtents"] = [
            left_extent, right_extent, top_extent, bottom_extent
        ]
        max_left = max(max_left, left_extent)
        max_right = max(max_right, right_extent)
        max_top = max(max_top, top_extent)
        max_bottom = max(max_bottom, bottom_extent)

    preferred_root_x = int(profile["preferredRootX"])
    preferred_root_y = int(profile["preferredRootY"])
    base_width = int(profile["baseFrameWidth"])
    base_height = int(profile["baseFrameHeight"])

    # Keep the canonical root whenever the content fits. Move the local root only
    # when extra transparent space is required on the left/top; world registration
    # remains unchanged because the renderer uses this generated local root.
    root_x = max(preferred_root_x, int(math.ceil(max_left + min_margin)))
    root_y = max(preferred_root_y, int(math.ceil(max_top + min_margin)))
    root_x = ceil_step(root_x, 8)
    root_y = ceil_step(root_y, 1)

    frame_width = max(
        base_width,
        ceil_step(root_x + max_right + min_margin, width_step)
    )
    frame_height = max(
        base_height,
        ceil_step(root_y + max_bottom + min_margin, height_step)
    )

    max_frame_width = int(validation["maxFrameWidth"])
    max_frame_height = int(validation["maxFrameHeight"])
    if frame_width > max_frame_width or frame_height > max_frame_height:
        raise ValueError(
            f"{cfg['id']}: required canvas {frame_width}x{frame_height} exceeds "
            f"limit {max_frame_width}x{max_frame_height}"
        )

    sheet = Image.new("RGBA", (frame_width * expected, frame_height), (0, 0, 0, 0))
    frame_reports = []
    passed = True

    for frame in frames:
        index = frame["index"]
        x0, y0, x1, y1 = frame["bbox"]
        source_root_x, source_root_y = frame["sourceRoot"]
        crop = frame["image"].crop((x0, y0, x1, y1))
        resized = crop.resize(
            (
                max(1, int(round(crop.width * scale))),
                max(1, int(round(crop.height * scale))),
            ),
            Image.Resampling.LANCZOS,
        )
        local_root_x = (source_root_x - x0) * scale
        local_root_y = (source_root_y - y0) * scale
        left = int(round(index * frame_width + root_x - local_root_x))
        top = int(round(root_y - local_root_y))
        sheet.alpha_composite(resized, (left, top))

        cell = sheet.crop((
            index * frame_width, 0,
            (index + 1) * frame_width, frame_height
        ))
        out_bbox = bbox_for(cell, threshold)
        out_root_x, out_root_y, out_feet = detect_ground_root(
            cell, out_bbox, threshold, foot_band_ratio, min_foot_width
        )
        opaque_pixels = sum(threshold_alpha(cell, threshold).histogram()[1:])
        margins = {
            "left": out_bbox[0],
            "right": frame_width - out_bbox[2],
            "top": out_bbox[1],
            "bottom": frame_height - out_bbox[3],
        }
        checks = {
            "noClipping": min(margins.values()) >= min_margin,
            "rootXWithinTolerance": abs(out_root_x - root_x) <= root_tolerance,
            "rootYWithinTolerance": abs(out_root_y - root_y) <= root_tolerance,
            "enoughOpaquePixels": opaque_pixels >= min_opaque,
        }
        frame_passed = all(checks.values())
        passed = passed and frame_passed
        frame_report = {
            "index": index,
            "sourceInterval": frame["sourceInterval"],
            "sourceBbox": list(frame["bbox"]),
            "sourceRoot": frame["sourceRoot"],
            "sourceFootIntervals": frame["footIntervals"],
            "outputBbox": list(out_bbox),
            "outputMargins": margins,
            "detectedOutputRoot": [out_root_x, out_root_y],
            "detectedOutputFootIntervals": out_feet,
            "opaquePixels": opaque_pixels,
            "checks": checks,
            "passed": frame_passed,
        }
        if "sourceRect" in frame:
            frame_report["sourceRect"] = frame["sourceRect"]
        frame_reports.append(frame_report)

    report = {
        "id": cfg["id"],
        "characterProfile": profile["id"],
        "source": cfg["source"],
        "detectedSourceIntervals": [list(value) for value in intervals],
        "scale": scale,
        "worldScale": float(profile["worldScale"]),
        "layout": {
            "frameWidth": frame_width,
            "frameHeight": frame_height,
            "rootX": root_x,
            "rootY": root_y,
            "frameCount": expected,
        },
        "validation": validation,
        **({"anatomy": anatomy_report} if anatomy_report is not None else {}),
        "frames": frame_reports,
        "passed": passed,
    }

    if not passed:
        failures = [
            f"frame {item['index']}: {item['checks']}"
            for item in frame_reports if not item["passed"]
        ]
        raise ValueError(f"{cfg['id']} validation failed: {'; '.join(failures)}")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    preview_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output_path, optimize=True)
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    preview = Image.new(
        "RGBA", sheet.size,
        tuple(cfg.get("previewBackground", [32, 36, 44, 255]))
    )
    preview.alpha_composite(sheet)
    draw = ImageDraw.Draw(preview)
    for item in frame_reports:
        index = item["index"]
        offset = index * frame_width
        bbox = item["outputBbox"]
        draw.line(
            (offset + root_x, 0, offset + root_x, frame_height),
            fill=(0, 220, 255, 210),
            width=1,
        )
        draw.line(
            (offset, root_y, offset + frame_width, root_y),
            fill=(0, 255, 120, 210),
            width=1,
        )
        draw.rectangle(
            (
                offset + bbox[0], bbox[1],
                offset + bbox[2] - 1, bbox[3] - 1
            ),
            outline=(255, 0, 255, 255),
            width=1,
        )
    preview.save(preview_path, optimize=True)

    return cfg, report

def process_prepared(cfg, profile):
    """Validate reviewed masters without resampling or re-grounding airborne poses."""
    import shutil
    source = Image.open(ROOT / cfg["source"]).convert("RGBA")
    width = int(cfg.get("frameWidth", profile["baseFrameWidth"]))
    height = int(cfg.get("frameHeight", profile["baseFrameHeight"]))
    root_x = int(cfg.get("rootX", profile["preferredRootX"]))
    root_y = int(cfg.get("rootY", profile["preferredRootY"]))
    count, columns = cfg["expectedFrames"], cfg["columns"]
    validation = profile["validation"]
    if width <= 0 or height <= 0:
        raise ValueError(f"{cfg['id']}: prepared-grid cell must be positive")
    if width > int(validation["maxFrameWidth"]) or height > int(validation["maxFrameHeight"]):
        raise ValueError(f"{cfg['id']}: prepared-grid cell exceeds profile limits")
    if not 0 <= root_x < width or not 0 <= root_y < height:
        raise ValueError(f"{cfg['id']}: authored root is outside the prepared-grid cell")
    if count <= 0 or columns <= 0 or source.size != (width * columns, height * math.ceil(count / columns)):
        raise ValueError(f"{cfg['id']}: prepared-grid dimensions do not match frame count")
    if cfg.get("rootMode") != "authored":
        raise ValueError("Prepared masters require an explicitly authored root")
    frames = []
    for i in range(count):
        x, y = i % columns * width, i // columns * height
        cell = source.crop((x, y, x + width, y + height))
        bbox = bbox_for(cell, profile["validation"]["alphaThreshold"])
        margins = [bbox[0], bbox[1], width - bbox[2], height - bbox[3]]
        if min(margins) < profile["validation"]["minMargin"]:
            raise ValueError(f"{cfg['id']}: frame {i} clips the safety margin")
        opaque = sum(threshold_alpha(cell, profile["validation"]["alphaThreshold"]).histogram()[1:])
        min_opaque = int(cfg.get("minOpaquePixels", profile["validation"]["minOpaquePixels"]))
        if opaque < min_opaque:
            raise ValueError(f"{cfg['id']}: frame {i} has insufficient visible pixels")
        frames.append({"index": i, "outputBbox": list(bbox), "minimumMargin": min(margins), "opaquePixels": opaque})
    report = {"id": cfg["id"], "characterProfile": profile["id"], "source": cfg["source"],
              "sourceNote": cfg.get("sourceNote", "Reviewed normalized master"),
              "rootMode": "authored", "scale": 1, "worldScale": profile["worldScale"],
              "layout": {"frameWidth": width, "frameHeight": height,
                         "rootX": root_x, "rootY": root_y,
                         "columns": columns, "frameCount": count}, "frames": frames, "passed": True}
    for key in ("output", "report", "preview"):
        (ROOT / cfg[key]).parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ROOT / cfg["source"], ROOT / cfg["output"])
    (ROOT / cfg["report"]).write_text(json.dumps(report, indent=2) + "\n")
    preview = Image.new("RGBA", source.size, (32, 36, 44, 255))
    preview.alpha_composite(source)
    preview.save(ROOT / cfg["preview"])
    return cfg, report

def write_generated_java(results):
    lines = [
        "package com.gamelutagpt;",
        "",
        "/** Generated by tools/sprites/import_sprites.py. Do not edit by hand. */",
        "final class GeneratedSpriteLayouts {",
        "    private GeneratedSpriteLayouts() {}",
        "",
    ]
    for cfg, report in results:
        name = sanitize_java_name(cfg["javaName"])
        # Packed geometry is what the APK atlas actually contains.
        layout = report.get("packed", report["layout"])
        lines += [
            f"    static final int {name}_FRAME_WIDTH = {layout['frameWidth']};",
            f"    static final int {name}_FRAME_HEIGHT = {layout['frameHeight']};",
            f"    static final int {name}_ROOT_X = {layout['rootX']};",
            f"    static final int {name}_ROOT_Y = {layout['rootY']};",
            f"    static final int {name}_FRAME_COUNT = {layout['frameCount']};",
            "",
        ]
    lines.append("}")
    lines.append("")
    GENERATED_JAVA.parent.mkdir(parents=True, exist_ok=True)
    GENERATED_JAVA.write_text("\n".join(lines), encoding="utf-8")

def main():
    config_paths = sorted(CLIPS_DIR.glob("*.json"))
    if not config_paths:
        raise SystemExit("No sprite clip configs found")
    results = []
    for path in config_paths:
        cfg, report = process_clip(path)
        results.append((cfg, report))
        layout = report["layout"]
        print(
            f"[sprite] {cfg['id']}: PASS "
            f"{layout['frameCount']} frames, "
            f"{layout['frameWidth']}x{layout['frameHeight']}, "
            f"root=({layout['rootX']},{layout['rootY']}), "
            f"scale={report['scale']:.4f}, worldScale={report['worldScale']:.1f}"
        )
    write_generated_java(results)

if __name__ == "__main__":
    main()