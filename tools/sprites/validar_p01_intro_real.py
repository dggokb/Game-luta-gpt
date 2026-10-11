#!/usr/bin/env python3
"""P01 INTRO evidence from unchanged shipped PNG alpha and renderer-equivalent math.

Only P01 INTRO is measured. Source: normalized frames derived from the authored
video (raw INTRO MP4 not identified in animations/p01). No sprite regeneration.
"""
import hashlib
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/sprites"))
import generate_p01_calibration as calibration

OUT = ROOT / "android/app/build/sprite-review/p01-intro-real"
SCALE = 1.075 / 2.0  # worldScale / atlas pixelScale

def read_json(path):
    return json.loads(path.read_text(encoding="utf8"))

def foot(im):
    alpha=np.asarray(im.getchannel("A"))
    ys,xs=np.nonzero(alpha>90)
    if len(xs)<100:
        raise AssertionError("Missing alpha silhouette")
    band=xs[ys>=int(ys.max())-14]
    if len(band)<10:
        raise AssertionError("Missing shoe contact")
    return {"left":float(np.quantile(band,.12)),
            "right":float(np.quantile(band,.88)),
            "height":int(ys.max()-ys.min()+1),
            "floor":int(ys.max())}

def render(cell,root_x,root_y,affine,fix=(1.,0.,0.)):
    sx,sy,bx,by=affine
    fs,fx,fy=fix
    image=Image.new("RGBA",(700,700))
    w=round(cell.width*SCALE*sx*fs)
    h=round(cell.height*SCALE*sy*fs)
    resized=cell.resize((w,h),Image.Resampling.BILINEAR)
    x=round(350+bx+fx*SCALE-root_x*SCALE*sx*fs)
    y=round(580+by+fy*SCALE-root_y*SCALE*sy*fs)
    image.alpha_composite(resized,(x,y))
    return image

def main():
    manifest=read_json(ROOT/"characters/player_base/character.json")
    intro=manifest["animations"]["INTRO"]
    idle=manifest["animations"]["IDLE"]
    ids=intro["frames"]
    assert ids==list(range(27)) and len(intro["durationsMs"])==27
    assert len(set(ids))==len(ids) and intro["atlas"]=="player_base_intro"
    assert sum(intro["durationsMs"])==2400 and idle["atlas"]=="player_base_idle"
    report=read_json(ROOT/"tools/sprites/reports/player_base_intro.report.json")
    ir=read_json(ROOT/"tools/sprites/reports/player_base_idle.report.json")
    packed=report["packed"]
    assert (packed["frameWidth"],packed["frameHeight"],packed["columns"],packed["frameCount"])==(347,569,9,27)
    data=read_json(ROOT/"android/app/src/main/assets/sprite-autofix.json")
    assert data["schema"]==1
    assert {(x["character"],x["atlas"],x["state"]) for x in data["corrections"]}=={
        ("player_base","player_base_victory","VICTORY"),
        ("player_base","player_base_intro","INTRO")}
    victory=next(e for e in data["corrections"] if e["state"]=="VICTORY")
    victory_hash=hashlib.sha256(json.dumps(victory,sort_keys=True).encode()).hexdigest()
    assert victory_hash=="a9d2d766d8491cd08ce90f74d68c735bb7d26eefb595ea32e0f118a04e15ceed", "VICTORY CHANGED"
    entry=next(e for e in data["corrections"] if e["state"]=="INTRO")
    assert [x[0] for x in entry["frames"]]==ids
    assert all(len(x)==4 and x[1]==1 and x[3]==0 and abs(x[2])<=45 for x in entry["frames"])
    assert all(not e["atlas"]=="player_base_intro" for e in data["corrections"] if e is not entry)
    baseline=calibration.compute()[0]["player_base_intro"]
    assert len(baseline)==27
    p=ROOT/"android/app/src/main/res/drawable-nodpi"
    source=Image.open(ROOT/"art/sprites/source/player_base_intro_video_normalized.png").convert("RGBA")
    assert source.size==(2592,960), "Authored source sheet unexpectedly changed"
    sh=[]
    for i in ids:
        sc=source.crop(((i%9)*288,(i//9)*320,(i%9+1)*288,(i//9+1)*320))
        sh.append(foot(sc))
    source_foot=[v["left"] for v in sh]
    source_plant=max(source_foot[:17])-min(source_foot[:17])
    source_step=source_foot[22]-source_foot[16]
    assert source_plant<=1.5 and source_step<=-30, "Source does not support assumed stationary and stance-change windows"
    tracks=[]
    previews=[]
    with Image.open(p/"player_base_intro.png") as sheet:
        assert sheet.size==(347*9,569*3)
        for i in ids:
            cell=sheet.crop(((i%9)*347,(i//9)*569,(i%9+1)*347,(i//9+1)*569)).convert("RGBA")
            original=render(cell,packed["rootX"],packed["rootY"],baseline[i])
            corrected=render(cell,packed["rootX"],packed["rootY"],baseline[i],entry["frames"][i][1:])
            track={"frame":i,"source":sh[i],"before":foot(original),"after":foot(corrected),
                   "autofix":entry["frames"][i]}
            assert track["before"]["height"]==track["after"]["height"], "Unexpected size change"
            assert track["before"]["floor"]==track["after"]["floor"], "Vertical root changed"
            tracks.append(track)
            if i in (0,10,14,16,17,19,22):
                previews.append((i,original,corrected))
    before=[r["before"]["left"] for r in tracks]
    after=[r["after"]["left"] for r in tracks]
    def spread(xs):
        return round(max(xs)-min(xs),3)
    assert spread(before[:17])>=15, "No actual INPUT bug to fix"
    assert spread(after[:17])<=1.25, "Planted foot still slides"
    assert after[22]-after[16]<=-35, "Legitimate guard-stance travel removed"
    assert abs((after[22]-after[16])-(source_step*SCALE*2))<=30, "Intentional step diverged excessively"
    # Verify the last visibly present INTRO-only frame against the real IDLE root.
    with Image.open(p/"player_base_idle.png") as idlesheet:
        idlecell=idlesheet.crop((0,0,ir["packed"]["frameWidth"],ir["packed"]["frameHeight"])).convert("RGBA")
        irender=render(idlecell,ir["packed"]["rootX"],ir["packed"]["rootY"],(.98,.98,0.,0.))
        final_idle=foot(irender)["left"]
    assert abs(after[22]-final_idle)<=1.5, "Pre-fade INTRO does not meet actual IDLE foot anchor"
    report_out={
      "status":"PASSA_PIXELS_REAIS_SEM_EMULADOR",
      "branch":"refino/p01-animacoes-20261009",
      "frames":len(ids),"duration_ms":sum(intro["durationsMs"]),
      "source":"authored normalized 27-frame source sheet (INTRO MP4 not confirmed in Google Drive)",
      "victory_object_sha256":victory_hash,
      "planted_window":[0,16],
      "intentional_stance_window":[17,22],
      "source_stationary_amplitude_px":source_plant,
      "source_stance_motion_px":source_step,
      "planted_foot_amplitude_before_px":spread(before[:17]),
      "planted_foot_amplitude_after_px":spread(after[:17]),
      "planted_foot_drift_before_px":round(before[16]-before[0],2),
      "planted_foot_drift_after_px":round(after[16]-after[0],2),
      "stance_travel_after_px":round(after[22]-after[16],2),
      "pre_fade_intro_vs_idle_before_px":round(before[22]-final_idle,2),
      "pre_fade_intro_vs_idle_after_px":round(after[22]-final_idle,2),
      "corrections_unchanged_size_y":True,
      "frames_data":tracks,
      "runtime_capture":"NAO_REALIZADA"}
    OUT.mkdir(parents=True,exist_ok=True)
    (OUT/"metricas.json").write_text(json.dumps(report_out,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
    chart=Image.new("RGB",(1000,460),"white")
    dr=ImageDraw.Draw(chart)
    dr.text((35,12),"P01 INTRO - Posicao real do pe antes/depois (0-16: apoio; 17-22: passo legitimo)",fill="black")
    def yy(x):return round(415-(x-250)*3.6)
    for x in range(250,360,20):
        dr.line([(55,yy(x)),(958,yy(x))],fill=(230,230,230))
        dr.text((12,yy(x)-8),str(x),fill="black")
    for j,values,color in ((0,before,(200,40,40)),(1,after,(35,100,210))):
        line=[(round(55+i*33),yy(v)) for i,v in enumerate(values)]
        dr.line(line,fill=color,width=3)
        for x,y in line:dr.ellipse((x-3,y-3,x+3,y+3),fill=color)
    dr.text((690,26),"Vermelho: ANTES",fill=(200,40,40))
    dr.text((690,44),"Azul: DEPOIS",fill=(35,100,210))
    chart.save(OUT/"grafico_pes.png")
    comparison=Image.new("RGB",(800,len(previews)*420),(240,240,240))
    d=ImageDraw.Draw(comparison)
    for row,(i,a,b) in enumerate(previews):
        for col,img in enumerate((a,b)):
            bg=Image.new("RGBA",(400,400),(239,240,242,255))
            bg.alpha_composite(img.crop((150,230,550,630)))
            comparison.paste(bg.convert("RGB"),(col*400,row*420+20))
            d.text((col*400+8,row*420+3),f"FRAME {i:02d} - {'ANTES' if col==0 else 'DEPOIS'}",fill="black")
    comparison.save(OUT/"quadro_a_quadro.png")
    print(json.dumps({k:v for k,v in report_out.items() if k!="frames_data"},indent=2,ensure_ascii=False))
    print("WARNING: no full Android device/emulator capture; source raw INTRO MP4 not confirmed.")

if __name__=="__main__":
    main()
