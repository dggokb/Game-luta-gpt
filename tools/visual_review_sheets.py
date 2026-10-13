#!/usr/bin/env python3
"""Pack the VisualReviewTest frames into a few JPEG stills and contact sheets.

Usage: visual_review_sheets.py <android/app/build/visual-review> <output dir>
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

src, out = Path(sys.argv[1]), Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)


def still(path, name):
    if path.exists():
        Image.open(path).convert("RGB").save(out / name, quality=88)


def sheet(frames, name, cols=4, scale=0.5):
    frames = [f for f in frames if f.exists()]
    if not frames:
        return
    first = Image.open(frames[0])
    w, h = int(first.width * scale), int(first.height * scale)
    rows = (len(frames) + cols - 1) // cols
    canvas = Image.new("RGB", (cols * w, rows * h), "black")
    draw = ImageDraw.Draw(canvas)
    for i, f in enumerate(frames):
        img = Image.open(f).convert("RGB").resize((w, h), Image.LANCZOS)
        x, y = (i % cols) * w, (i // cols) * h
        canvas.paste(img, (x, y))
        draw.text((x + 6, y + 4), f.stem, fill="yellow")
    canvas.save(out / name, quality=85)


sel = src / "selection"
for n in ["step0", "step1_flash", "step1", "step3", "ready"]:
    still(sel / f"{n}.png", f"selection_{n}.jpg")
anim = sorted((sel / "anim").glob("*.png"))
sheet(anim[::5], "selection_anim_sheet.jpg")

fight = src / "fight"
still(fight / "idle.png", "fight_idle.jpg")
seq = sorted((fight / "seq").glob("*.png"))
for start in range(0, len(seq), 16):
    sheet(seq[start:start + 16], f"fight_seq_{start // 16:02d}.jpg")
for f in seq[::3]:
    # Full-size samples for close inspection of single moments.
    Image.open(f).convert("RGB").save(out / f"f_{f.stem}.jpg", quality=82)
still(src.parent / "hud-review.png", "hud_review.jpg")
