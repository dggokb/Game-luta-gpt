#!/usr/bin/env python3
"""QC real dos MP4 do Drive, sem modificar a fonte nem os clipes do jogo.

Exemplo:
  python3 tools/sprites/audit_video_content.py --root "/videos/animations"

Percorre p01/*.mp4 ... p12/*.mp4; decodifica 12 quadros reais com FFmpeg,
confere se o vídeo abre, calcula movimento e geometria aproximada (fundo
chroma verde), assina o MP4 inteiro e identifica arquivos idênticos.

Atenção: diferenças de bounding box sao *avisos*, não sentença de zoom
(quedas, golpes altos e crouch variam a altura), e não há reconhecimento
semântico de golpes nem garantia de anatomia ou loops.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path


def check_video(path: Path, root: Path, np):
    row = {"file": str(path.relative_to(root)), "character": path.parent.name,
           "filename": path.name, "bytes": path.stat().st_size}
    w, h, fps = 96, 54, 3
    frame_bytes = w*h*3
    try:
        cmd = ["ffmpeg", "-hide_banner", "-loglevel", "error", "-threads", "1",
               "-i", str(path), "-vf", f"fps={fps},scale={w}:{h},format=bgr24",
               "-frames:v", "12", "-f", "rawvideo", "pipe:1"]
        proc = subprocess.run(cmd, capture_output=True, timeout=45)
        if proc.returncode:
            raise RuntimeError(proc.stderr.decode("utf-8", "replace")[:250])
        n = len(proc.stdout)//frame_bytes
        if n < 2:
            raise RuntimeError(f"only {n} sampled frames")
        rgb = np.frombuffer(proc.stdout[:n*frame_bytes], dtype=np.uint8)
        rgb = rgb.reshape(n, h, w, 3).astype(np.int16)
        change = np.abs(np.diff(rgb, axis=0))
        b, g, r = rgb[:, :, :, 0], rgb[:, :, :, 1], rgb[:, :, :, 2]
        green = (g > 75) & (g > r*1.22) & (g > b*1.22) & ((g-r) > 24)
        fg = ~green
        heights, feet, centers = [], [], []
        for mask in fg:
            ys, xs = np.where(mask)
            if len(xs):
                heights.append(int(ys.max()-ys.min()+1))
                feet.append(int(ys.max()))
                centers.append(float(xs.mean()))
        corner = float(np.median(fg[:, :5, :7].mean(axis=(1, 2)))) > 0.08
        row.update(
            decoded_frames=n,
            motion_mean=round(float(change.mean()), 4),
            bbox_height_ratio=round(max(heights)/max(1,min(heights)), 3)
                if heights else None,
            foot_vertical_range=max(feet)-min(feet) if feet else None,
            bbox_center_x_range=round(max(centers)-min(centers), 2)
                if centers else None,
            possible_corner_overlay=corner,
            sample_sha256=hashlib.sha256(proc.stdout).hexdigest(),
            flags=[])
        if row["motion_mean"] < 0.6:
            row["flags"].append("possible_static_or_idle")
        if row["bbox_height_ratio"] and row["bbox_height_ratio"] > 1.8:
            row["flags"].append("large_bbox_change_review")
        if corner:
            row["flags"].append("non_green_upper_left_corner")
        if n < 9:
            row["flags"].append("short_video_sample")
        if not heights:
            row["flags"].append("no_foreground_green_screen")
        if path.stem.lower() == "idle":
            if row["bbox_center_x_range"] and row["bbox_center_x_range"] > 13:
                row["flags"].append("idle_center_drift_review")
            if row["foot_vertical_range"] and row["foot_vertical_range"] > 11:
                row["flags"].append("idle_feet_drift_review")
    except (subprocess.TimeoutExpired, OSError, ValueError, RuntimeError) as exc:
        row.update(error=str(exc), flags=["decode_error"])
    # Hash dos bytes INTEIROS: diferente do hash dos quadros amostrados.
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024*1024), b""):
            digest.update(chunk)
    row["sha256"] = digest.hexdigest()
    return row


def scan(root: Path, workers: int = 4):
    try:
        import numpy as np
    except ImportError as exc:
        raise RuntimeError("numpy necessario: pip install numpy") from exc
    from shutil import which
    if not which("ffmpeg"):
        raise RuntimeError("ffmpeg nao encontrado no PATH")
    files = sorted(root.glob("p*/*.mp4"))
    if not files:
        raise RuntimeError(f"nenhum mp4 encontrado em {root}/pNN/")
    results = []
    with ThreadPoolExecutor(max_workers=workers) as pool:
        jobs = {pool.submit(check_video, file, root, np): file for file in files}
        for completed in as_completed(jobs):
            results.append(completed.result())
    results.sort(key=lambda x: x["file"])
    hashes, samples = defaultdict(list), defaultdict(list)
    for entry in results:
        hashes[entry["sha256"]].append(entry["file"])
        if entry.get("sample_sha256"):
            samples[entry["sample_sha256"]].append(entry["file"])
    summary = {
        "videos": len(results),
        "characters": dict(sorted(Counter(r["character"] for r in results).items())),
        "decoded_ok": sum(not r.get("error") for r in results),
        "errors": [{"file": r["file"], "error": r["error"]}
                   for r in results if r.get("error")],
        "exact_duplicates": [group for group in hashes.values() if len(group) > 1],
        "sample_duplicates": [group for group in samples.values() if len(group) > 1],
        "flags": dict(Counter(flag for r in results for flag in r["flags"])),
        "method": "12 real frames (RGB), green-screen estimate and full-file SHA256",
        "limitations": "A flag is not a semantic or artistic verdict. Review poses in-game.",
    }
    return {"summary": summary, "videos": results}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True, help="Animations folder containing p01..p12")
    parser.add_argument("--output", type=Path, default=Path("android/app/build/sprite-audit/qc_videos.json"))
    parser.add_argument("--workers", type=int, default=4)
    args = parser.parse_args()
    if not 1 <= args.workers <= 12:
        parser.error("--workers must be between 1 and 12")
    result = scan(args.root, args.workers)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False)+"\n", encoding="utf-8")
    print(json.dumps(result["summary"], indent=2, ensure_ascii=False))
    print("Detalhes:", args.output)


if __name__ == "__main__":
    main()
