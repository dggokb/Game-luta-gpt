#!/usr/bin/env python3
"""Diagnose the 56 actual packed P02 IDLE RGBA frames. No sprite modifications."""
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw

R=Path(__file__).resolve().parents[2]
OUT=R/"android/app/build/sprite-review/p02-idle-diagnose"
CFG=json.loads((R/"tools/sprites/reports/p02_idle.report.json").read_text())["packed"]
PATH=R/"android/app/src/main/res/drawable-nodpi/p02_idle.png"
N,W,H,C=56,CFG["frameWidth"],CFG["frameHeight"],CFG["columns"]

def frame(i):
    x=i%C*W; y=i//C*H
    return Image.open(PATH).convert("RGBA").crop((x,y,x+W,y+H))

def box(mask):
    yy,xx=np.where(mask)
    if len(xx)<100: raise ValueError("Blank/invalid frame")
    return [int(xx.min()),int(yy.min()),int(xx.max()),int(yy.max())]

def features(p):
    a=np.asarray(p)[...,3]>128
    b=box(a); l,t,r,bottom=b
    foot=np.zeros_like(a)
    foot[max(0,bottom-32):bottom+1]=a[max(0,bottom-32):bottom+1]
    ground=a[max(0,bottom-12):bottom+1]
    gx=np.where(ground)[1]
    torso=a[t+int((bottom-t)*.30):t+int((bottom-t)*.60)]
    tx=np.where(torso)[1]
    return dict(bbox=b,width=r-l+1,height=bottom-t+1,
        bottom=bottom,feet=float(np.median(gx)),
        torso_x=float(l+np.median(tx)) if len(tx) else None,
        opaque=int(a.sum()),alpha=a,foot=foot)

def shift(mask,dx):
    m=np.zeros_like(mask)
    if dx==0:return mask
    if dx>0:m[:,dx:]=mask[:,:-dx]
    else:m[:,:dx]=mask[:,-dx:]
    return m

def iou(a,b):
    return float(np.logical_and(a,b).sum())/max(1,int(np.logical_or(a,b).sum()))

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    pngs=[frame(i) for i in range(N)]
    ff=[features(p) for p in pngs]
    shapes=[f["foot"] for f in ff]
    # Poses close to the reference should retain planted feet. This only
    # proposes whole-frame translations, never generative image replacement.
    scores=[]; proposed=[]
    for i,m in enumerate(shapes):
        vals=[(iou(shapes[0],shift(m,dx))-.002*abs(dx),dx) for dx in range(-8,9)]
        chosen=max(vals,key=lambda z:(z[0],-abs(z[1])))[1]
        proposed.append(chosen)
        scores.append({"original":round(iou(shapes[0],m),5),
                       "aligned":round(iou(shapes[0],shift(m,chosen)),5)})
    # Circular temporal median to prevent introducing correction jitter.
    smoothed=[int(np.median([proposed[(i+d)%N] for d in (-2,-1,0,1,2)])) for i in range(N)]

    # Validate the ACTUAL Android runtime corrections rather than candidates.
    fixes=json.loads((R/"android/app/src/main/assets/sprite-autofix.json").read_text())["corrections"]
    selected=[e for e in fixes if e.get("character")=="p02" and e.get("atlas")=="p02_idle"]
    assert len(selected)==1, "P02 IDLE must have exactly one runtime adjustment scope"
    rows=selected[0]["frames"]
    assert sorted(int(row[0]) for row in rows)==list(range(51,56))
    assert all(len(row)==4 and row[1]==1 and row[3]==0 and row[2]==2 for row in rows)
    offsets=[0]*N
    for i,size,dx,dy in rows: offsets[int(i)]=int(dx)
    assert offsets==smoothed, "Runtime offsets deviate from measured candidate"
    with Image.open(PATH) as atlas:
        assert atlas.mode=="RGBA", "Alpha channel removed"
        assert atlas.size==(W*C,H*((N+C-1)//C)), "Invalid packed Android resource"
    assert all(f["opaque"]>1000 for f in ff), "Transparent or empty frame"
    assert all(0<f["bbox"][0] and f["bbox"][2]+max(offsets)<W for f in ff), "Sprite clipped after shift"

    def measures(offsets):
        feet=[f["feet"]+d for f,d in zip(ff,offsets)]
        torso=[f["torso_x"]+d for f,d in zip(ff,offsets)]
        def pspan(values):return float(max(values)-min(values))
        return {"foot_range_px":round(pspan(feet),3),
                "foot_step_max_px":round(max(abs(feet[i]-feet[i-1]) for i in range(1,N)),3),
                "foot_seam_px":round(abs(feet[-1]-feet[0]),3),
                "torso_range_px":round(pspan(torso),3),
                "height_range_px":[min(f["height"] for f in ff),max(f["height"] for f in ff)],
                "width_range_px":[min(f["width"] for f in ff),max(f["width"] for f in ff)],
                "ground_y_range_px":[min(f["bottom"] for f in ff),max(f["bottom"] for f in ff)],
                "foot_mask_loop_iou":round(iou(shift(shapes[-1],offsets[-1]),shift(shapes[0],offsets[0])),5)}
    orig=measures([0]*N); aligned=measures(offsets)
    assert aligned["foot_range_px"] < orig["foot_range_px"]
    assert aligned["foot_seam_px"] < orig["foot_seam_px"]
    assert aligned["foot_mask_loop_iou"] > orig["foot_mask_loop_iou"]
    assert aligned["ground_y_range_px"]==orig["ground_y_range_px"]
    assert aligned["height_range_px"]==orig["height_range_px"]
    assert aligned["width_range_px"]==orig["width_range_px"]

    # Contact pixel shifts are presented as candidates only. Preserve video
    # movement unless a human reviews the measurements and visual overlay.
    info={"status":"RUNTIME_P02_IDLE_FIX_VERIFIED_REAL_PACKED_PNG",
      "source":"android/app/src/main/res/drawable-nodpi/p02_idle.png",
      "packed":CFG,"count":N,"before":orig,"after":aligned,
      "applied_shifts_px_2x":offsets,"raw_shifts":proposed,
      "changed_frames":sum(d!=0 for d in offsets),
      "frame_scores":scores,
      "frames":[{"index":i,"bbox":f["bbox"],"height":f["height"],
           "width":f["width"],"feet":round(f["feet"],2),
           "torso_x":round(f["torso_x"],2),"opaque":f["opaque"]} for i,f in enumerate(ff)]}
    (OUT/"measurements.json").write_text(json.dumps(info,indent=2)+"\n")
    compare=Image.new("RGB",(6*240,2*310),(239,240,244))
    d=ImageDraw.Draw(compare)
    for k,i in enumerate((0,11,22,33,44,55)):
        for row in range(2):
            pic=Image.new("RGBA",(W,H),(239,240,244,255))
            dx=offsets[i] if row else 0
            pic.alpha_composite(pngs[i],(dx,0))
            compare.paste(pic.convert("RGB").resize((215,280)),(k*240,row*310+23))
            d.text((k*240+6,row*310+4),f"f{i:02} "+("after" if row else "before")+f" dx={dx}",fill=(0,0,0))
    compare.save(OUT/"preview_before_after.png")
    # 56 actual PNG cells, two synchronized loops at the authored 41 ms.
    frames=[]
    for i,source in enumerate(pngs):
        pair=Image.new("RGB",(480,324),(239,240,244))
        for side,dx in enumerate((0,offsets[i])):
            target=Image.new("RGBA",(W,H),(239,240,244,255))
            target.alpha_composite(source,(dx,0))
            small=target.convert("RGB").resize((240,322),Image.Resampling.LANCZOS)
            pair.paste(small,(240*side,1))
        frames.append(pair.quantize(colors=128))
    frames[0].save(OUT/"loop_before_after.gif",save_all=True,
                   append_images=frames[1:],duration=41,loop=0,optimize=False)

    print(json.dumps({k:v for k,v in info.items() if k not in ("frames","frame_scores")},indent=2))
if __name__=="__main__":main()
