#!/usr/bin/env python3
"""Redesenho imagem-para-imagem do Templo da Lua usando AnimeGANv2 ONNX.

Lê as DEZ artes originais de um commit fixo, e não os resultados de filtros
anteriores. O modelo recebe cada camada individualmente; nenhum elemento novo
é criado por composição de texto, e as coordenadas/alpha ficam intactas.
Não usa preparar_cenario.py, que alteraria os bounding boxes existentes.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from io import BytesIO
from pathlib import Path
from urllib.request import urlopen

import cv2
import numpy as np
import onnxruntime as ort
from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[2]
STAGE = ROOT / "android/app/src/main/assets/stages/templo_lua"
ORIGINAL_SHA = "7ff11cde385d358e93159096c18ed50bf6e97752"
MODEL_URL = "https://huggingface.co/vumichien/AnimeGANv2_Hayao/resolve/main/AnimeGANv2_Hayao.onnx"
MODEL_SHA256 = "5a84ca468f3c4fd891fe8c883a3a507ed3e463f4f059985735f6449dae7590b5"
FILES = (
    "ceu.jpg", "montanhas.png", "cidade.png", "portal.png", "grade.png",
    "pilar.png", "teto.png", "braseiro.png", "chao.jpg", "chao_lajotas.jpg",
)
MODEL_CACHE = Path("/tmp/templo_lua_animeganv2_hayao.onnx")
PREVIEW_BG = (17, 23, 44)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def original_bytes(filename: str) -> bytes:
    rel = (STAGE / filename).relative_to(ROOT).as_posix()
    try:
        return subprocess.check_output(
            ["git", "show", f"{ORIGINAL_SHA}:{rel}"],
            cwd=ROOT, stderr=subprocess.DEVNULL,
        )
    except subprocess.CalledProcessError:
        url = ("https://raw.githubusercontent.com/dggokb/Game-luta-gpt/"
               f"{ORIGINAL_SHA}/{rel}")
        with urlopen(url, timeout=120) as handle:
            return handle.read()


def get_model() -> ort.InferenceSession:
    if not MODEL_CACHE.exists() or sha256(MODEL_CACHE.read_bytes()) != MODEL_SHA256:
        print("Baixando o modelo de image-to-image (SHA256 fixado)...", flush=True)
        with urlopen(MODEL_URL, timeout=600) as response:
            model_data = response.read()
        if sha256(model_data) != MODEL_SHA256:
            raise RuntimeError("Modelo remoto não corresponde ao SHA256 esperado")
        MODEL_CACHE.write_bytes(model_data)
    options = ort.SessionOptions()
    options.intra_op_num_threads = 4
    session = ort.InferenceSession(
        str(MODEL_CACHE), sess_options=options,
        providers=["CPUExecutionProvider"],
    )
    print("Modelo carregado:", session.get_inputs()[0].name,
          session.get_inputs()[0].shape, flush=True)
    return session


def animegan_transform(source: Image.Image, session: ort.InferenceSession,
                       filename: str) -> Image.Image:
    rgba = np.asarray(source.convert("RGBA"), dtype=np.uint8)
    original_rgb = rgba[:, :, :3].copy()
    alpha = rgba[:, :, 3].copy()
    f = alpha.astype(np.float32)[:, :, None] / 255.
    bg = np.asarray(PREVIEW_BG, dtype=np.float32)[None, None, :]
    matted = (original_rgb * f + bg * (1. - f)).astype(np.uint8)
    h, w = matted.shape[:2]

    # Reduz apenas para inferência; restaura resolução exata depois.
    factor = min(1., 1280. / max(w, h))
    mw = max(256, int(round(w * factor / 32)) * 32)
    mh = max(256, int(round(h * factor / 32)) * 32)
    scaled = cv2.resize(matted, (mw, mh), interpolation=cv2.INTER_AREA)
    inp = scaled.astype(np.float32) / 127.5 - 1.

    input_data = session.get_inputs()[0]
    if len(input_data.shape) != 4:
        raise RuntimeError("Formato de entrada ONNX inesperado")
    if input_data.shape[-1] == 3 or isinstance(input_data.shape[-1], str):
        feed = inp[None]
        is_nchw = False
    elif input_data.shape[1] == 3:
        feed = inp.transpose(2, 0, 1)[None]
        is_nchw = True
    else:
        raise RuntimeError(f"Layout ONNX não suportado: {input_data.shape}")

    output = session.run(None, {input_data.name: feed})[0]
    if output.ndim != 4:
        raise RuntimeError(f"Saída ONNX inválida: {output.shape}")
    output = output[0]
    if is_nchw:
        output = output.transpose(1, 2, 0)
    if output.shape[-1] != 3:
        raise RuntimeError(f"Saída RGB inválida: {output.shape}")
    styled = np.clip((output + 1.) * 127.5, 0, 255).astype(np.uint8)
    styled = cv2.resize(styled, (w, h), interpolation=cv2.INTER_CUBIC)

    # Linhas de tinta sobre DESCONTINUIDADES existentes, sem mover objetos.
    # Diferente da antiga mera quantização/filtro, os valores-base vêm da
    # pintura reconstruída por uma rede generativa de imagem-para-imagem.
    edges = cv2.Canny(
        cv2.cvtColor(matted, cv2.COLOR_RGB2GRAY), 68, 153, L2gradient=True
    )
    width = 2 if filename in ("portal.png", "pilar.png", "braseiro.png",
                               "grade.png", "teto.png") else 1
    if width > 1:
        edges = cv2.dilate(edges, np.ones((2, 2), np.uint8))
    coverage = (edges > 0) & (alpha >= 240)
    strength = 0.65 if width > 1 else 0.45
    styled[coverage] = np.clip(
        styled[coverage].astype(np.float32) * (1. - strength)
        + np.array([13, 16, 31]) * strength, 0, 255
    ).astype(np.uint8)

    if source.mode in ("RGBA", "LA") or "transparency" in source.info:
        # Preservar exatamente a máscara, inclusive anti-alias de recortes.
        # Na franja translúcida, usar RGB do original para evitar halo escuro.
        fringe = alpha < 224
        styled[fringe] = original_rgb[fringe]
        result = Image.fromarray(np.dstack((styled, alpha)), "RGBA")
    else:
        result = Image.fromarray(styled, "RGB")
    return result


def metrics(before: Image.Image, after: Image.Image, filename: str) -> dict:
    a = np.asarray(before.convert("RGBA"))
    b = np.asarray(after.convert("RGBA"))
    if before.size != after.size:
        raise RuntimeError(f"{filename}: mudou resolução")
    if not np.array_equal(a[:, :, 3], b[:, :, 3]):
        raise RuntimeError(f"{filename}: mudou máscara alpha!")
    old = a[:, :, :3].astype(np.int16)
    new = b[:, :, :3].astype(np.int16)
    valid = a[:, :, 3] > 200
    if not valid.any():
        raise RuntimeError(f"{filename}: camada vazia")
    mad = float(np.abs(old[valid] - new[valid]).mean())
    if mad < 6.0:
        raise RuntimeError(f"{filename}: transformação fraca (MAD={mad:.2f})")
    # Verificação estrutural: as linhas originais devem permanecer em posição.
    old_gray = cv2.cvtColor(a[:, :, :3], cv2.COLOR_RGB2GRAY)
    new_gray = cv2.cvtColor(b[:, :, :3], cv2.COLOR_RGB2GRAY)
    old_edge = cv2.Canny(old_gray, 60, 140) > 0
    new_edge = cv2.Canny(new_gray, 60, 140) > 0
    grown = cv2.dilate(new_edge.astype(np.uint8),
                       np.ones((5, 5), dtype=np.uint8)).astype(bool)
    edged = old_edge & valid
    overlap = float(np.count_nonzero(edged & grown) /
                    max(1, np.count_nonzero(edged)))
    if overlap < 0.38:
        raise RuntimeError(
            f"{filename}: linhas perderam alinhamento (cobertura={overlap:.3f})")
    return {
        "file": filename, "size": list(before.size),
        "alpha_exact": True, "alpha_bbox_exact": True,
        "color_change_mad": round(mad, 2),
        "original_edges_covered_2px": round(overlap, 3),
    }


def comparison(original: dict, changed: dict, destination: Path):
    w, h = 550, 260
    canvas = Image.new("RGB", (w * 2, (h + 32) * len(FILES)), "#121529")
    draw = ImageDraw.Draw(canvas)
    for i, filename in enumerate(FILES):
        for col, source in enumerate((original[filename], changed[filename])):
            bg = Image.new("RGBA", source.size, (*PREVIEW_BG, 255))
            bg.alpha_composite(source.convert("RGBA"))
            thumb = ImageOps.contain(bg.convert("RGB"), (w, h))
            x = col * w + (w - thumb.width) // 2
            y = i * (h + 32) + (h - thumb.height) // 2
            canvas.paste(thumb, (x, y))
        draw.text((12, (i+1)*(h+32)-24), f"{filename} - ORIGINAL", fill="white")
        draw.text((w+12, (i+1)*(h+32)-24),
                  f"{filename} - ANIMEGANv2 IMAGE-TO-IMAGE", fill="white")
    destination.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(destination, quality=94)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--preview", type=Path)
    args = parser.parse_args()
    config = json.loads((STAGE / "stage.json").read_text(encoding="utf-8"))
    actual = {layer["imagem"] for layer in config["camadas"]
              if "imagem" in layer}
    actual |= {config["chao"]["imagem"], config["chao"]["imagemLajotas"]}
    if set(FILES) != actual:
        raise RuntimeError(f"stage.json inesperado: {actual ^ set(FILES)}")

    session = get_model()
    originals, redraws, records = {}, {}, []
    for filename in FILES:
        raw = original_bytes(filename)
        original = Image.open(BytesIO(raw)).copy()
        print("Image-to-image:", filename, original.size, flush=True)
        redesigned = animegan_transform(original, session, filename)
        info = metrics(original, redesigned, filename)
        info["original_sha256"] = sha256(raw)
        print("Validação:", info, flush=True)
        originals[filename], redraws[filename] = original, redesigned
        records.append(info)

    if args.preview:
        comparison(originals, redraws, args.preview)

    if args.apply:
        for filename in FILES:
            path = STAGE / filename
            image = redraws[filename]
            if filename.endswith(".jpg"):
                image.convert("RGB").save(path, "JPEG", quality=94,
                                          subsampling=0)
            else:
                image.save(path, "PNG", optimize=True)
        for item in records:
            item["output_sha256"] = sha256((STAGE / item["file"]).read_bytes())
        (STAGE / "anime-style-manifest.json").write_text(
            json.dumps({
                "mode": "animeganv2_hayao_onnx_image_to_image",
                "model": MODEL_URL, "model_sha256": MODEL_SHA256,
                "original_commit": ORIGINAL_SHA,
                "notes": "Redesenho por rede neural; necessita inspeção visual no jogo.",
                "files": records,
            }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Fim: verificadas", len(records), "camadas.", flush=True)


if __name__ == "__main__":
    main()
