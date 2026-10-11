#!/usr/bin/env python3
"""Auditoria de pixels REAIS do P01 VICTORY. Nunca valida runtime Android."""
import json
import sys
from pathlib import Path
from statistics import median
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/sprites"))
import generate_p01_calibration as calibracao

def shoe(image):
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    if not bbox:
        raise ValueError("Frame sem pixels")
    left, top, right, bottom = bbox
    pixels = alpha.load()
    values = [x for y in range(max(top, bottom - 18), bottom)
              for x in range(left, right) if pixels[x, y] > 90]
    if not values:
        raise ValueError("Pes nao rastreaveis")
    return float(median(values)), bottom - top

def render(cell, packed, baseline, correction, kind):
    sx, sy, dx, dy = baseline
    fs, fx, fy = correction
    if kind == "raw":
        sx = sy = fs = 1.0
        dx = dy = fx = fy = 0.0
    elif kind == "calibrado":
        fs, fx, fy = 1.0, 0.0, 0.0
    scale = 1.075 / packed["pixelScale"]
    width_scale, height_scale = scale * sx * fs, scale * sy * fs
    width = max(1, round(cell.width * width_scale))
    height = max(1, round(cell.height * height_scale))
    bitmap = cell.resize((width, height), Image.Resampling.BILINEAR)
    image = Image.new("RGBA", (550, 720), (0,0,0,0))
    x = round(275 + dx + fx * scale - packed["rootX"] * width_scale)
    y = round(595 + dy + fy * scale - packed["rootY"] * height_scale)
    image.alpha_composite(bitmap, (x,y))
    return image

def main():
    report = json.loads((ROOT / "tools/sprites/reports/player_base_victory.report.json").read_text())
    packed = report["packed"]
    w,h,columns,count = (int(packed[v]) for v in ("frameWidth","frameHeight","columns","frameCount"))
    assert count == 33
    corrections = json.loads((ROOT / "android/app/src/main/assets/sprite-autofix.json").read_text())
    assert corrections["schema"] == 1
    transforms = calibracao.compute()[0]["player_base_victory"]
    assert len(transforms) == count
    fix_by_frame = {}
    for entry in corrections["corrections"]:
        if entry.get("character") == "player_base" and entry.get("atlas") == "player_base_victory":
            fix_by_frame.update({int(row[0]):tuple(row[1:]) for row in entry["frames"]})
    output = ROOT / "android/app/build/sprite-review/p01-victory-real"
    output.mkdir(parents=True,exist_ok=True)
    snapshots = {}
    track = []
    path = ROOT / "android/app/src/main/res/drawable-nodpi/player_base_victory.png"
    with Image.open(path) as sheet:
        assert sheet.size[0] == w * columns
        for i in range(count):
            cell = sheet.crop(((i%columns)*w,(i//columns)*h,(i%columns+1)*w,(i//columns+1)*h)).convert("RGBA")
            row = {"frame":i,"source_video_index_expected":i*3,
                   "segment":"entrada_na_fonte" if i < 7 else "plantado"}
            for kind in ("raw","calibrado","autofix"):
                frame = render(cell,packed,transforms[i],fix_by_frame.get(i,(1.,0.,0.)),kind)
                row[kind + "_foot_x"], row[kind + "_height"] = shoe(frame)
                if i in (0,6,7,13,20,32):
                    snapshots[i,kind] = frame
            track.append(row)
    summary = {}
    for kind in ("raw","calibrado","autofix"):
        foot = [row[kind+"_foot_x"] for row in track[7:]]
        heights = [row[kind+"_height"] for row in track[7:]]
        summary[kind] = {"amplitude_x_px":max(foot)-min(foot),
                         "deslocamento_x_px":foot[-1]-foot[0],
                         "min_height_px":min(heights),"max_height_px":max(heights)}
    data={"status":"PARCIAL_SEM_CAPTURA_ANDROID","fonte":"atlas integrado P01/Victory",
          "frame_count":count,"time_per_frame_ms":125,
          "runtime_starts_at_frame":7,"autofix_frames":sorted(fix_by_frame),
          "summary":summary,"frames":track,
          "limite":"Reproducao matemática das transformacoes de SpriteFighterRenderer, nao da camera nem do Android real"}
    (output/"atlas_real_medicao.json").write_text(json.dumps(data,indent=2,ensure_ascii=False)+"\n")
    preview = Image.new("RGB",(3*250,6*346),(245,245,245))
    draw = ImageDraw.Draw(preview)
    for r,i in enumerate((0,6,7,13,20,32)):
        for col,kind in enumerate(("raw","calibrado","autofix")):
            img = snapshots[i,kind]
            bg=Image.new("RGBA",img.size,(245,245,245,255))
            bg.alpha_composite(img)
            preview.paste(bg.convert("RGB").resize((250,320)),(col*250,r*346+26))
            draw.text((col*250+8,r*346+7),f"frame={i}  {kind}",fill=(0,0,0))
    preview.save(output/"atlas_real_comparativo.png")
    print(json.dumps({k:v for k,v in data.items() if k!="frames"},indent=2,ensure_ascii=False))
    print("RESULTADO NAO APROVADO: sem captura real da camera e renderizador Android.")

if __name__ == "__main__":
    main()
