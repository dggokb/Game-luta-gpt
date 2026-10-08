#!/usr/bin/env python3
import json
import math
import re
import shutil
import statistics
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
PROFILES_DIR = ROOT / "tools" / "sprites" / "profiles"
GENERATED_JAVA = ROOT / "android" / "app" / "src" / "main" / "java" / "com" / "gamelutagpt" / "GeneratedSpriteLayouts.java"

# Anatomy bands (fractions of the body height) compared to set a drawn sheet's scale.
DEFAULT_ANATOMY_BANDS = [[0.0, 0.12], [0.05, 0.20], [0.25, 0.40]]
PREVIEW_BACKGROUND = (32, 36, 44, 255)

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
    bands = anatomy.get("bands", DEFAULT_ANATOMY_BANDS)
    return anatomy, anatomy_signature(cell, bbox, threshold, bands)

def process_clip(config_path):
    cfg = load_json(config_path)
    profile = load_json(PROFILES_DIR / cfg["profile"])
    if cfg.get("segmentation") == "prepared-grid":
        return process_prepared(cfg, profile)
    validation = profile["validation"]
    threshold = int(validation["alphaThreshold"])
    min_margin = int(validation["minMargin"])
    root_tolerance = float(validation["rootTolerance"])
    width_step = int(validation["widthStep"])
    height_step = int(validation.get("heightStep", width_step))
    # A clip whose feet do not rest on exactly the same row (wide stances drawn by AI)
    # may widen the band so both feet are measured the same way before and after scaling.
    foot_band_ratio = float(cfg.get("footBandRatio", validation["footBandRatio"]))
    min_foot_width = int(validation["minFootWidth"])
    min_opaque = int(cfg.get("minOpaquePixels", validation["minOpaquePixels"]))
    max_upscale = float(validation["maxUpscale"])

    source_path = ROOT / cfg["source"]
    output_path = ROOT / cfg["output"]
    report_path = ROOT / cfg["report"]
    preview_path = ROOT / cfg["preview"]

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
    elif segmentation == "alpha-components":
        regions, component_images = connected_frames(source, threshold)
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
        if segmentation == "alpha-components":
            # Bounding boxes may overlap (a fist passing over the next pose); each
            # frame keeps only its own pixels.
            frame = component_images[index]
        else:
            frame = source.crop((left, top, right, bottom))
        if index in set(int(i) for i in cfg.get("mirrorFrames", [])):
            # Poses drawn facing the other way (e.g. lying head-first toward the
            # opponent) are flipped to match the frames they must connect with.
            frame = frame.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
        bbox = bbox_for(frame, threshold)
        root_mode = cfg.get("rootMode", "ground-feet")
        if root_mode == "ground-feet":
            root_x, root_y, foot_intervals = detect_ground_root(
                frame, bbox, threshold, foot_band_ratio, min_foot_width
            )
        elif root_mode == "bbox-bottom-center":
            # Bodies with no feet on the ground (lying, tumbling): centre of the body
            # on its lowest row; refine per frame with frameShift when needed.
            root_x, root_y, foot_intervals = (bbox[0] + bbox[2]) / 2, bbox[3] - 1, []
        else:
            raise ValueError(f"unsupported root mode: {root_mode}")
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

    scale_mode = cfg.get("scaleMode")
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
        bands = anatomy_cfg.get("bands", DEFAULT_ANATOMY_BANDS)
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
    elif scale_mode == "fixed":
        # For poses with no comparable reference (e.g. a body lying down): the scale is
        # declared, justified and calibrated against sibling sheets of the same art set.
        if not cfg.get("scaleReason"):
            raise ValueError(f"{cfg['id']}: fixed scaleMode requires scaleReason")
        scale = float(cfg["scale"])
        if scale <= 0:
            raise ValueError(f"{cfg['id']}: fixed scale must be positive")
    else:
        raise ValueError(f"{cfg['id']}: scaleMode must be canonical-anatomy or fixed (got {scale_mode})")
    if scale > max_upscale:
        raise ValueError(
            f"{cfg['id']}: source would require upscaling {scale:.3f}x; "
            f"maximum is {max_upscale:.3f}x"
        )

    # Declarative re-registration of single frames whose foot detection picked one foot:
    # frameShift {"7": [dx, dy]} moves that frame's body by dx/dy output pixels.
    shifts = {int(k): v for k, v in cfg.get("frameShift", {}).items()}
    for index, (dx, dy) in shifts.items():
        if index < 0 or index >= expected:
            raise ValueError(f"{cfg['id']}: invalid frameShift index {index}")
        root_x, root_y = frames[index]["sourceRoot"]
        frames[index]["sourceRoot"] = [root_x - dx / scale, root_y - dy / scale]
        frames[index]["frameShift"] = [dx, dy]

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
        if cfg.get("rootMode", "ground-feet") == "bbox-bottom-center":
            out_root_x, out_root_y, out_feet = (out_bbox[0] + out_bbox[2]) / 2, out_bbox[3] - 1, []
        else:
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
        shift_x, shift_y = frame.get("frameShift", (0, 0))
        checks = {
            "noClipping": min(margins.values()) >= min_margin,
            # A declared frameShift moves the detected feet by exactly that amount.
            "rootXWithinTolerance": abs(out_root_x - (root_x + shift_x)) <= root_tolerance,
            "rootYWithinTolerance": abs(out_root_y - (root_y + shift_y)) <= root_tolerance,
            "enoughOpaquePixels": opaque_pixels >= min_opaque,
        }
        frame_passed = all(checks.values())
        passed = passed and frame_passed
        frame_report = {
            "index": index,
            "sourceInterval": frame["sourceInterval"],
            "sourceBbox": list(frame["bbox"]),
            "sourceRoot": frame["sourceRoot"],
            "frameShift": list(frame.get("frameShift", (0, 0))),
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

    preview = Image.new("RGBA", sheet.size, PREVIEW_BACKGROUND)
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
    # Previews only go to the review page (android/app/build): fast compression is enough.
    preview.save(preview_path, compress_level=1)

    return cfg, report

def process_prepared(cfg, profile):
    """Validate reviewed masters (video sheets, separated sheets) cell by cell.

    The cells are used as authored unless the clip declares a "transform" (regroup
    pieces, scale, keep frames, snap to the ground).
    """
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
        raise ValueError(f"{cfg['id']}: prepared masters require an explicitly authored root")
    source_cells = [
        source.crop((i % columns * width, i // columns * height,
                     i % columns * width + width, i // columns * height + height))
        for i in range(count)
    ]
    transform = cfg.get("transform")
    transform_report = None
    regroup_report = None
    if transform is not None and "regroupComponents" in transform:
        source_cells, width, height, root_x, root_y, regroup_report = regroup_components(
            cfg, source, count, columns, width, height, root_x, root_y, transform["regroupComponents"]
        )
    if transform is not None:
        source_cells, width, height, root_x, root_y, columns, transform_report = apply_transform(
            cfg, profile, source_cells, width, height, root_x, root_y, columns, transform
        )
        count = len(source_cells)
    threshold = int(validation["alphaThreshold"])
    min_margin = validation["minMargin"]
    min_opaque = int(cfg.get("minOpaquePixels", validation["minOpaquePixels"]))
    frames = []
    for i in range(count):
        cell = source_cells[i]
        bbox = bbox_for(cell, threshold)
        margins = [bbox[0], bbox[1], width - bbox[2], height - bbox[3]]
        if min(margins) < min_margin:
            raise ValueError(f"{cfg['id']}: frame {i} clips the safety margin")
        opaque = sum(threshold_alpha(cell, threshold).histogram()[1:])
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
    if transform_report is None:
        shutil.copyfile(ROOT / cfg["source"], ROOT / cfg["output"])
        output = source
    else:
        report["transform"] = transform_report
        if regroup_report is not None:
            report["transform"]["regroup"] = regroup_report
        rows = math.ceil(count / columns)
        output = Image.new("RGBA", (width * columns, height * rows))
        for i, cell in enumerate(source_cells):
            output.paste(cell, (i % columns * width, i // columns * height))
        output.save(ROOT / cfg["output"], format="PNG")
    (ROOT / cfg["report"]).write_text(json.dumps(report, indent=2) + "\n")
    preview = Image.new("RGBA", output.size, PREVIEW_BACKGROUND)
    preview.alpha_composite(output)
    preview.save(ROOT / cfg["preview"], compress_level=1)
    return cfg, report

def connected_frames(source, threshold, min_ratio=0.02, attach_distance=6):
    """One frame per connected body (8-connectivity), ordered left to right.

    Used when poses overlap in x so column projection would merge them. Pieces smaller
    than min_ratio of the largest body join the body whose box (grown by
    attach_distance) contains them; anything else is dropped as a speck.
    """
    alpha = source.getchannel("A")
    w, h = alpha.size
    px = alpha.load()
    seen = bytearray(w * h)
    comps = []
    for y in range(h):
        for x in range(w):
            if px[x, y] <= threshold or seen[y * w + x]:
                continue
            seen[y * w + x] = 1
            stack, points = [(x, y)], []
            while stack:
                cx, cy = stack.pop()
                points.append((cx, cy))
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        nx, ny = cx + dx, cy + dy
                        if 0 <= nx < w and 0 <= ny < h and px[nx, ny] > threshold and not seen[ny * w + nx]:
                            seen[ny * w + nx] = 1
                            stack.append((nx, ny))
            xs = [p[0] for p in points]
            ys = [p[1] for p in points]
            comps.append({"points": points, "box": [min(xs), min(ys), max(xs) + 1, max(ys) + 1]})
    if not comps:
        return [], []
    largest = max(len(c["points"]) for c in comps)
    bodies = [c for c in comps if len(c["points"]) >= largest * min_ratio]
    for c in comps:
        if c in bodies:
            continue
        x0, y0, x1, y1 = c["box"]
        for body in bodies:
            bx0, by0, bx1, by1 = body["box"]
            if (x0 >= bx0 - attach_distance and x1 <= bx1 + attach_distance
                    and y0 >= by0 - attach_distance and y1 <= by1 + attach_distance):
                body["points"].extend(c["points"])
                break
    bodies.sort(key=lambda c: c["box"][0])
    regions, images = [], []
    src = source.load()
    for body in bodies:
        x0, y0, x1, y1 = body["box"]
        image = Image.new("RGBA", (x1 - x0, y1 - y0))
        out = image.load()
        for x, y in body["points"]:
            if x0 <= x < x1 and y0 <= y < y1:
                out[x - x0, y - y0] = src[x, y]
        # Anti-aliased fringe (alpha <= threshold) next to the body's own pixels.
        for y in range(y0, y1):
            for x in range(x0, x1):
                a = src[x, y][3]
                if 0 < a <= threshold and out[x - x0, y - y0][3] == 0:
                    near = any(out[nx - x0, ny - y0][3] > threshold
                               for nx in (x - 1, x, x + 1) for ny in (y - 1, y, y + 1)
                               if x0 <= nx < x1 and y0 <= ny < y1)
                    if near:
                        out[x - x0, y - y0] = src[x, y]
        regions.append((x0, y0, x1, y1))
        images.append(image)
    return regions, images

def regroup_components(cfg, sheet, count, columns, width, height, root_x, root_y, options):
    """Gives every connected piece of art back to the frame that owns it.

    Masters drawn on a tight grid sometimes let a limb cross into the neighbouring cell:
    the owner looks clipped and the neighbour shows floating fragments. Pieces are
    labelled on the whole sheet (8-connectivity) and assigned to the cell holding most
    of their pixels; isolated specks below dropSmallerThan pixels are removed. Each frame
    is rebuilt on a cell padded by `pad` pixels on every side.
    """
    pad = int(options.get("pad", 32))
    drop = int(options.get("dropSmallerThan", 0))
    alpha = sheet.getchannel("A")
    w, h = sheet.size
    px = alpha.load()
    label = [0] * (w * h)
    frames = [Image.new("RGBA", (width + 2 * pad, height + 2 * pad)) for _ in range(count)]
    moved, dropped, current = [], 0, 0
    src = sheet.load()
    for y in range(h):
        for x in range(w):
            if px[x, y] <= 8 or label[y * w + x]:
                continue
            current += 1
            stack = [(x, y)]
            label[y * w + x] = current
            points = []
            while stack:
                cx, cy = stack.pop()
                points.append((cx, cy))
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        nx, ny = cx + dx, cy + dy
                        if 0 <= nx < w and 0 <= ny < h and px[nx, ny] > 8 and not label[ny * w + nx]:
                            label[ny * w + nx] = current
                            stack.append((nx, ny))
            if len(points) < drop:
                dropped += len(points)
                continue
            votes = {}
            for cx, cy in points:
                cell = cx // width + (cy // height) * columns
                votes[cell] = votes.get(cell, 0) + 1
            owner = max(votes, key=votes.get)
            if owner >= count:
                continue
            ox, oy = owner % columns * width, owner // columns * height
            stray = sum(n for cell, n in votes.items() if cell != owner)
            if stray:
                moved.append({"frame": owner, "pixelsRecovered": stray})
            target = frames[owner].load()
            for cx, cy in points:
                tx, ty = cx - ox + pad, cy - oy + pad
                if 0 <= tx < width + 2 * pad and 0 <= ty < height + 2 * pad:
                    target[tx, ty] = src[cx, cy]
    # Faint anti-aliasing (alpha <= 8) next to each piece follows its nearest owner cell.
    for i in range(count):
        ox, oy = i % columns * width, i // columns * height
        target = frames[i].load()
        for y in range(height):
            for x in range(width):
                if 0 < px[ox + x, oy + y] <= 8:
                    target[x + pad, y + pad] = src[ox + x, oy + y]
    report = {"pad": pad, "dropSmallerThan": drop, "droppedPixels": dropped, "recovered": moved}
    return frames, width + 2 * pad, height + 2 * pad, root_x + pad, root_y + pad, report

def remove_magenta(cell, box):
    """Repaints magenta artefact pixels inside box with the mean of their clean neighbours."""
    cell = cell.copy()
    px = cell.load()
    x0, y0, x1, y1 = box

    def magenta(p):
        # Pink/magenta: blue well above green. Skin, red cloth and shoes keep blue <= green.
        r, g, b, a = p
        return a > 10 and r > g + 30 and b > g + 18 and b > 0.40 * r

    core = {(x, y) for y in range(y0, y1) for x in range(x0, x1) if magenta(px[x, y])}
    # One-pixel dilation catches the anti-aliased halo around the glow.
    bad = {(x + dx, y + dy) for x, y in core for dx in (-1, 0, 1) for dy in (-1, 0, 1)
           if x0 <= x + dx < x1 and y0 <= y + dy < y1 and px[x + dx, y + dy][3] > 10}
    total = len(bad)
    while bad:
        filled = {}
        for x, y in bad:
            near = [px[nx, ny] for nx in range(x - 2, x + 3) for ny in range(y - 2, y + 3)
                    if 0 <= nx < cell.width and 0 <= ny < cell.height
                    and (nx, ny) not in bad and px[nx, ny][3] > 10]
            if near:
                filled[(x, y)] = tuple(round(sum(c[k] for c in near) / len(near)) for k in range(3)) + (px[x, y][3],)
        if not filled:
            break
        for xy, colour in filled.items():
            px[xy] = colour
        bad -= set(filled)
    return cell, total

def apply_transform(cfg, profile, cells, width, height, root_x, root_y, columns, t):
    """Declarative fix-ups of a reviewed master, recorded in the report.

    Order: keep a subset of frames, scale about the root (premultiplied Lanczos), place
    in the output cell at the output root, shift per frame, then snap chosen frames so
    their lowest visible row sits on the root (feet on the ground). The source art is
    never modified; every number lives in the clip config.
    """
    threshold = int(profile["validation"]["alphaThreshold"])
    keep = [int(i) for i in t.get("keepFrames", range(len(cells)))]
    if any(i < 0 or i >= len(cells) for i in keep) or len(set(keep)) != len(keep):
        raise ValueError(f"{cfg['id']}: invalid keepFrames")
    scale = float(t.get("scale", 1.0))
    if scale <= 0:
        raise ValueError(f"{cfg['id']}: transform scale must be positive")
    max_upscale = float(profile["validation"]["maxUpscale"])
    if scale > max_upscale and not (t.get("allowUpscale") is True and t.get("reason")):
        raise ValueError(f"{cfg['id']}: upscaling {scale:.3f}x requires allowUpscale and a reason")
    out_w = int(t.get("outputFrameWidth", width))
    out_h = int(t.get("outputFrameHeight", height))
    out_rx = int(t.get("outputRootX", root_x))
    out_ry = int(t.get("outputRootY", root_y))
    v = profile["validation"]
    if out_w > int(v["maxFrameWidth"]) or out_h > int(v["maxFrameHeight"]):
        raise ValueError(f"{cfg['id']}: transform output cell exceeds profile limits")
    if not 0 <= out_rx < out_w or not 0 <= out_ry < out_h:
        raise ValueError(f"{cfg['id']}: transform output root is outside the cell")
    offsets = {int(k): v for k, v in t.get("offsets", {}).items()}
    ground = {int(i) for i in t.get("groundFrames", [])}
    if not set(offsets) <= set(keep) or not ground <= set(keep):
        raise ValueError(f"{cfg['id']}: offsets/groundFrames must refer to kept frames")
    cleanups = {}
    for item in t.get("cleanup", []):
        frame, box = int(item["frame"]), [int(n) for n in item["box"]]
        if frame not in keep or not (0 <= box[0] < box[2] <= width and 0 <= box[1] < box[3] <= height):
            raise ValueError(f"{cfg['id']}: invalid cleanup entry {item}")
        cleanups.setdefault(frame, []).append(box)
    result, details = [], []
    for i in keep:
        cell = cells[i]
        cleaned = 0
        for box in cleanups.get(i, []):
            cell, n = remove_magenta(cell, box)
            cleaned += n
        if scale != 1.0:
            scaled = cell.convert("RGBa").resize(
                (round(width * scale), round(height * scale)), Image.LANCZOS
            ).convert("RGBA")
        else:
            scaled = cell
        dx, dy = (int(n) for n in offsets.get(i, (0, 0)))
        placed = Image.new("RGBA", (out_w, out_h))
        left = round(out_rx - root_x * scale) + dx
        top = round(out_ry - root_y * scale) + dy
        placed.paste(scaled, (left, top))
        snap = 0
        if i in ground:
            bbox = bbox_for(placed, threshold)
            snap = out_ry - (bbox[3] - 1)
            if snap:
                moved = Image.new("RGBA", (out_w, out_h))
                moved.paste(placed, (0, snap))
                placed = moved
        result.append(placed)
        details.append({"sourceFrame": i, "offset": [dx, dy], "groundSnap": snap, "cleanedPixels": cleaned})
    report = {"scale": scale, "upscaled": scale > 1.0, "reason": t.get("reason", ""),
              "sourceCell": [width, height, root_x, root_y], "frames": details}
    return result, out_w, out_h, out_rx, out_ry, int(t.get("columns", columns)), report

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

if __name__ == "__main__":
    raise SystemExit("Run tools/sprites/build_characters.py --write: it imports every clip, "
                     "packs the atlases and writes the generated Java together.")
