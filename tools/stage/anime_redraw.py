#!/usr/bin/env python3
"""Reestiliza o Templo da Lua sem redesenhar geometria, recortar camadas ou alterar alpha.

O tratamento é determinístico: suavização de tinta, paleta de cor chapada,
contraste e linhas de contorno. É uma estilização de pixels, NÃO geração por IA.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from io import BytesIO
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageOps

REPO = Path(__file__).resolve().parents[2]
BASE_SHA = "7ff11cde385d358e93159096c18ed50bf6e97752"
STAGE = REPO / "android/app/src/main/assets/stages/templo_lua"
REPORT = STAGE / "anime-style-manifest.json"
IMAGES = (
    "ceu.jpg", "montanhas.png", "cidade.png", "portal.png",
    "grade.png", "pilar.png", "teto.png", "braseiro.png",
    "chao.jpg", "chao_lajotas.jpg",
)
# Céu e pisos mantêm detalhes mais finos; silhuetas recebem contornos mais fortes.
PARAMS = {
    "ceu.jpg": (23, 23, .92, .40, 70, 150),
    "montanhas.png": (31, 29, .96, .66, 48, 108),
    "cidade.png": (25, 26, .94, .70, 46, 103),
    "portal.png": (35, 32, .98, .83, 36, 90),
    "grade.png": (36, 33, .98, .86, 36, 93),
    "pilar.png": (35, 33, .98, .85, 37, 88),
    "teto.png": (35, 33, .98, .80, 42, 96),
    "braseiro.png": (37, 35, .98, .89, 36, 83),
    "chao.jpg": (34, 31, .97, .72, 48, 105),
    "chao_lajotas.jpg": (34, 31, .97, .72, 48, 105),
}


def digest(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def transform(image: Image.Image, filename: str) -> Image.Image:
    had_alpha = image.mode in ("RGBA", "LA") or "transparency" in image.info
    src = np.asarray(image.convert("RGBA"), dtype=np.uint8).copy()
    rgb, alpha = src[:, :, :3], src[:, :, 3]
    light_step, chroma_step, flat_strength, ink_strength, low, high = PARAMS[filename]

    smooth = cv2.bilateralFilter(rgb, d=9, sigmaColor=75, sigmaSpace=7)
    lab = cv2.cvtColor(smooth, cv2.COLOR_RGB2LAB)
    quant = lab.astype(np.uint16)
    quant[:, :, 0] = np.clip(
        ((quant[:, :, 0] + light_step // 2) // light_step) * light_step, 0, 255)
    for channel in (1, 2):
        quant[:, :, channel] = np.clip(
            ((quant[:, :, channel] + chroma_step // 2) // chroma_step) * chroma_step, 0, 255)
    flat_rgb = cv2.cvtColor(quant.astype(np.uint8), cv2.COLOR_LAB2RGB)
    mixed = cv2.addWeighted(rgb, 1 - flat_strength, flat_rgb, flat_strength, 0)

    gray = cv2.cvtColor(smooth, cv2.COLOR_RGB2GRAY)
    edges = cv2.Canny(gray, low, high, L2gradient=True)
    # O traço segue os contornos originais; nunca modifica a máscara de recorte.
    if filename in ("portal.png", "grade.png", "pilar.png", "braseiro.png"):
        edges = cv2.dilate(edges, np.ones((2, 2), np.uint8), iterations=1)
    mask = (edges.astype(np.float32) / 255) * (alpha.astype(np.float32) / 255)
    strength = (mask * ink_strength)[:, :, None]
    ink = np.array([14, 16, 38], np.float32)
    result = np.clip(mixed.astype(np.float32) * (1 - strength)
                     + ink * strength, 0, 255).astype(np.uint8)
    if had_alpha:
        return Image.fromarray(np.dstack([result, alpha]), mode="RGBA")
    return Image.fromarray(result, mode="RGB")


def preview(before: dict[str, Image.Image], after: dict[str, Image.Image], path: Path):
    tw, th = 480, 210
    sheet = Image.new("RGB", (tw * 2, len(IMAGES) * (th + 27)), "#161722")
    draw = ImageDraw.Draw(sheet)
    for i, name in enumerate(IMAGES):
        y = i * (th + 27)
        for j, source in enumerate((before[name], after[name])):
            composite = Image.new("RGBA", source.size, (45, 42, 65, 255))
            composite.alpha_composite(source.convert("RGBA"))
            thumb = ImageOps.contain(composite.convert("RGB"), (tw, th))
            sheet.paste(thumb, (j * tw + (tw - thumb.width) // 2, y + (th - thumb.height) // 2))
        draw.text((8, y + th + 5), f"{name}: ORIGINAL", fill="white")
        draw.text((tw + 8, y + th + 5), "ANIME - CEL SHADING", fill="white")
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path, quality=92)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="substituir os dez arquivos reais")
    parser.add_argument("--preview", type=Path, help="comparativo original / novo")
    args = parser.parse_args()

    stage = json.loads((STAGE / "stage.json").read_text(encoding="utf-8"))
    names_in_stage = {x["imagem"] for x in stage["camadas"] if "imagem" in x}
    names_in_stage |= {stage["chao"]["imagem"], stage["chao"]["imagemLajotas"]}
    assert names_in_stage == set(IMAGES), ("arquivos diferentes no stage.json", names_in_stage)

    # Sempre parte da arte original, nunca de uma imagem já pós-processada.
    if REPORT.exists():
        print("Refinamento: recuperando fontes do commit-base original.")

    before, after, metadata = {}, {}, []
    for name in IMAGES:
        file = STAGE / name
        rel = file.relative_to(REPO).as_posix()
        raw = subprocess.check_output(["git", "show", f"{BASE_SHA}:{rel}"], cwd=REPO)
        with Image.open(BytesIO(raw)) as source:
            original = source.copy()
        changed = transform(original, name)
        if original.size != changed.size:
            raise RuntimeError(f"Dimensões alteradas: {name}")
        old_alpha = np.asarray(original.convert("RGBA"))[:, :, 3]
        new_alpha = np.asarray(changed.convert("RGBA"))[:, :, 3]
        if not np.array_equal(old_alpha, new_alpha):
            raise RuntimeError(f"Transparência alterada: {name}")
        old_rgb = np.asarray(original.convert("RGB")).astype(np.int16)
        new_rgb = np.asarray(changed.convert("RGB")).astype(np.int16)
        mad = float(np.abs(old_rgb - new_rgb).mean())
        if mad < 1.0:
            raise RuntimeError(f"Transformação pouco perceptível: {name}, MAD={mad:.2f}")
        before[name], after[name] = original, changed
        metadata.append({
            "file": name, "size": list(original.size), "source_sha256": digest(raw),
            "color_change_mad": round(mad, 3), "alpha_exact": True,
        })
        print(f"{name}: {original.size}, diferença média RGB={mad:.2f}, alpha intacto")

    if args.preview:
        preview(before, after, args.preview)
        print("Preview:", args.preview)

    if args.apply:
        for name in IMAGES:
            dest = STAGE / name
            if name.endswith(".jpg"):
                after[name].convert("RGB").save(dest, format="JPEG", quality=94, subsampling=0)
            else:
                after[name].save(dest, format="PNG", optimize=True)
        for m in metadata:
            m["output_sha256"] = digest((STAGE / m["file"]).read_bytes())
        REPORT.write_text(json.dumps({
            "mode": "deterministic_90s_anime_cel_shading_v2_not_generational_redraw",
            "source_branch": "game-luta-sprite-gpt",
            "preserves": ["image_size", "png_alpha", "stage_json", "layer_positions"],
            "files": metadata,
        }, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print("10 arquivos substituídos e relatório salvo:", REPORT)


if __name__ == "__main__":
    main()
