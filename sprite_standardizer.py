"""Padronizador de sprites independente de qualquer jogo."""
import argparse
import json
import math
import re
from pathlib import Path

import cv2
import numpy as np


class CalibrationError(ValueError):
    pass


def image_rgba(frame):
    """Aceita imagens transparentes ou chroma key verde."""
    if frame is None or frame.ndim != 3:
        raise CalibrationError("Imagem inválida")
    if frame.shape[2] == 4 and np.any(frame[:, :, 3] < 255):
        return cv2.cvtColor(frame, cv2.COLOR_BGRA2RGBA)
    rgb = cv2.cvtColor(frame[:, :, :3], cv2.COLOR_BGR2RGB)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    channels = rgb.astype(np.int16)
    diff = channels[:, :, 1] - np.maximum(channels[:, :, 0], channels[:, :, 2])
    green = ((hsv[:, :, 0] >= 34) & (hsv[:, :, 0] <= 94)
             & (hsv[:, :, 1] > 65) & (diff > 18))
    alpha = np.full(diff.shape, 255, np.uint8)
    alpha[green] = np.clip(255 - (diff[green] - 18) * 255 / 42, 0, 255).astype(np.uint8)
    edge = green & (alpha < 255)
    rgb[:, :, 1][edge] = np.minimum(
        channels[:, :, 1][edge],
        np.maximum(channels[:, :, 0][edge], channels[:, :, 2][edge]) + 5
    )
    return np.dstack((rgb, alpha))


def bounds(rgba):
    binary = (rgba[:, :, 3] > 32).astype(np.uint8)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(binary, 8)
    keep = np.zeros_like(binary, dtype=bool)
    min_area = max(16, int(binary.size * 0.00004))
    for idx in range(1, n):
        if stats[idx, cv2.CC_STAT_AREA] >= min_area:
            keep |= labels == idx
    y, x = np.nonzero(keep)
    if len(x) == 0 or len(x) > binary.size * 0.90:
        raise CalibrationError("Não foi possível separar personagem e fundo")
    return (int(x.min()), int(y.min()), int(x.max()) + 1, int(y.max()) + 1)


def shape_feature(rgba, bbox, size=96):
    x0, y0, x1, y1 = bbox
    mask = (rgba[y0:y1, x0:x1, 3] > 64).astype(np.uint8)
    h, w = mask.shape
    k = (size - 4) / max(w, h)
    nw, nh = max(1, round(w * k)), max(1, round(h * k))
    resized = cv2.resize(mask, (nw, nh), interpolation=cv2.INTER_NEAREST)
    result = np.zeros((size, size), dtype=np.uint8)
    x, y = (size - nw) // 2, (size - nh) // 2
    result[y:y+nh, x:x+nw] = resized
    return result


def similarity(a, b):
    kernel = np.ones((3, 3), np.uint8)
    aa = cv2.dilate(a, kernel)
    bb = cv2.dilate(b, kernel)
    overlap = np.count_nonzero(aa & bb)
    union = np.count_nonzero(aa | bb)
    return float(min(1.0, overlap / union)) if union else 0.0


def read_video(path):
    cap = cv2.VideoCapture(str(path))
    if not cap.isOpened():
        raise CalibrationError(f"Não abriu vídeo: {path}")
    fps = float(cap.get(cv2.CAP_PROP_FPS))
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            yield frame, fps
    finally:
        cap.release()


def scan(path, reference):
    result, fps = [], 0.0
    for frame, fps in read_video(path):
        rgba = image_rgba(frame)
        bbox = bounds(rgba)
        feature = shape_feature(rgba, bbox)
        result.append({"bbox": bbox, "feature": feature,
                       "score": similarity(feature, reference)})
    if not result:
        raise CalibrationError(f"Vídeo sem frames: {path}")
    return result, fps


def reference_index(frames, explicit, threshold):
    count = len(frames)
    if explicit is not None:
        if not 0 <= explicit < count:
            raise CalibrationError("Frame de referência fora do vídeo")
        idx = explicit
    else:
        edge = max(1, math.ceil(count * .22))
        options = sorted(set(range(edge)) | set(range(count-edge, count)))
        idx = max(options, key=lambda i: frames[i]["score"])
    if frames[idx]["score"] < threshold:
        raise CalibrationError(
            f"Pose de referência incerta: {frames[idx]['score']:.3f}; "
            "não normalizei o vídeo. Escolha outro frame ou revise a fonte."
        )
    return idx


def loop_hint(frames):
    """Compara sequências, não apenas dois quadros; apenas sugere ciclo."""
    n = len(frames)
    best = None
    for step in range(6, n // 2 + 1):
        k = min(32, n - step)
        if k < 6:
            continue
        positions = np.linspace(0, k-1, min(k, 16), dtype=int)
        mean = float(np.mean([
            similarity(frames[int(i)]["feature"], frames[int(i)+step]["feature"])
            for i in positions
        ]))
        if best is None or mean > best["similarity"]:
            best = {"period_frames": step, "similarity": round(mean, 4)}
    return best if best and best["similarity"] >= .76 else None


def load_master(path):
    frame = cv2.imread(str(path), cv2.IMREAD_UNCHANGED)
    if frame is None:
        raise CalibrationError(f"Imagem mestre não encontrada: {path}")
    rgba = image_rgba(frame)
    bbox = bounds(rgba)
    return {"bbox": bbox, "feature": shape_feature(rgba, bbox)}


def process(config_path, output):
    cfg = json.loads(Path(config_path).read_text(encoding="utf-8"))
    folder = Path(config_path).resolve().parent
    if "idle" not in cfg["references"]:
        raise CalibrationError("É necessária uma imagem mestre idle")
    masters = {key: load_master(folder / value)
               for key, value in cfg["references"].items()}
    idle_h = masters["idle"]["bbox"][3] - masters["idle"]["bbox"][1]
    target_idle = float(cfg.get("idle_target_height", 256))
    if target_idle <= 0:
        raise CalibrationError("Altura alvo deve ser positiva")
    target_heights = {}
    for key, val in masters.items():
        relative_h = val["bbox"][3] - val["bbox"][1]
        target_heights[key] = float(cfg.get("target_heights", {}).get(
            key, target_idle * relative_h / idle_h
        ))
    threshold = float(cfg.get("minimum_similarity", .62))
    padding = int(cfg.get("padding", 24))
    if not 0 < threshold <= 1 or padding < 0:
        raise CalibrationError("Configuração inválida: limite ou margem")
    clips = cfg.get("clips") or []
    if not clips:
        raise CalibrationError("Nenhum vídeo configurado")

    jobs, all_boxes, names = [], [], set()
    for clip in clips:
        name = re.sub(r"[^a-zA-Z0-9_-]", "_", clip["name"]).strip("_")
        if not name or name in names:
            raise CalibrationError(f"Nome inválido ou repetido: {name}")
        names.add(name)
        pose = clip.get("reference_pose", "idle")
        if pose not in masters:
            raise CalibrationError(f"Imagem mestre não cadastrada: {pose}")
        src = folder / clip["video"]
        frames, fps = scan(src, masters[pose]["feature"])
        idx = reference_index(frames, clip.get("reference_frame"), threshold)
        x0, y0, x1, y1 = frames[idx]["bbox"]
        scale = target_heights[pose] / (y1 - y0)
        origin_x, origin_y = (x0+x1)/2, y1
        rectangles = []
        for frame in frames:
            x0, y0, x1, y1 = frame["bbox"]
            rectangles.append((
                (x0-origin_x)*scale, (y0-origin_y)*scale,
                (x1-origin_x)*scale, (y1-origin_y)*scale
            ))
        all_boxes.extend(rectangles)
        jobs.append({"name": name, "src": src, "frames": frames, "fps": fps,
                     "pose": pose, "ref_idx": idx, "scale": scale,
                     "origin": (origin_x, origin_y), "rectangles": rectangles,
                     "loop": loop_hint(frames) if clip.get("detect_loop") else None})

    # Calcula canvas e root compartilhados por TODOS os clips.
    min_x, min_y = min(b[0] for b in all_boxes), min(b[1] for b in all_boxes)
    max_x, max_y = max(b[2] for b in all_boxes), max(b[3] for b in all_boxes)
    root_x, root_y = math.ceil(-min_x+padding), math.ceil(-min_y+padding)
    width = max(int(cfg.get("minimum_width", 0)), math.ceil(max_x-min_x+2*padding)+3)
    height = max(int(cfg.get("minimum_height", 0)), math.ceil(max_y-min_y+2*padding)+3)
    if width*height > 25000000:
        raise CalibrationError("Canvas excessivo; confira os deslocamentos dos vídeos")
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    manifest = {"format_version": 1, "canvas": [width, height],
                "root": [root_x, root_y], "clips": []}

    # Segundo passe: TODOS os frames preservados, sem mudar a escala durante o clip.
    for job in jobs:
        dest = output / job["name"]
        dest.mkdir(parents=True, exist_ok=True)
        frame_meta = []
        count = 0
        for i, (source, _) in enumerate(read_video(job["src"])):
            if i >= len(job["frames"]):
                raise CalibrationError("Vídeo mudou durante o processamento")
            rgba = image_rgba(source)
            x0, y0, x1, y1 = job["frames"][i]["bbox"]
            crop = rgba[y0:y1, x0:x1]
            w = max(1, round((x1-x0)*job["scale"]))
            h = max(1, round((y1-y0)*job["scale"]))
            interpolation = cv2.INTER_AREA if job["scale"] < 1 else cv2.INTER_CUBIC
            sprite = cv2.resize(crop, (w, h), interpolation=interpolation)
            x = round(root_x+(x0-job["origin"][0])*job["scale"])
            y = round(root_y+(y0-job["origin"][1])*job["scale"])
            if x < 0 or y < 0 or x+w > width or y+h > height:
                raise CalibrationError(f"Clip em {job['name']}: frame {i} cortado")
            canvas = np.zeros((height, width, 4), dtype=np.uint8)
            canvas[y:y+h, x:x+w] = sprite
            file = f"{i:05d}.png"
            cv2.imwrite(str(dest/file), cv2.cvtColor(canvas, cv2.COLOR_RGBA2BGRA))
            frame_meta.append({
                "index": i, "file": file, "bbox_source": [x0, y0, x1, y1],
                "time_s": round(i/job["fps"], 6) if job["fps"] else None
            })
            count += 1
        if count != len(job["frames"]):
            raise CalibrationError("Vídeo mudou durante o processamento")
        info = {"name": job["name"], "reference_pose": job["pose"],
                "reference_frame": job["ref_idx"],
                "reference_similarity": round(job["frames"][job["ref_idx"]]["score"], 4),
                "scale_per_video": round(job["scale"], 8),
                "fps_original": job["fps"], "frame_count": count,
                "loop_suggestion": job["loop"], "frames": frame_meta}
        manifest["clips"].append(info)
        (dest/"metadata.json").write_text(json.dumps(info, indent=2, ensure_ascii=False), encoding="utf-8")
    (output/"manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
    return manifest


def main():
    parser = argparse.ArgumentParser(description="Extração e padronização de imagens a partir de vídeos")
    parser.add_argument("project", type=Path, help="Projeto JSON")
    parser.add_argument("--output", type=Path, default=Path("output"))
    opts = parser.parse_args()
    try:
        result = process(opts.project, opts.output)
    except (CalibrationError, KeyError, ValueError, OSError) as error:
        parser.exit(2, f"Erro: {error}\n")
    print(json.dumps({"canvas": result["canvas"], "root": result["root"],
                      "clips": [{"name": c["name"], "frames": c["frame_count"],
                                 "scale": c["scale_per_video"]} for c in result["clips"]]},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
