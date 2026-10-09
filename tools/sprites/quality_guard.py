#!/usr/bin/env python3
"""PC sprite-quality gate. Native-pixel audit with non-destructive optional repair.

Compares real opaque interior ink/detail to the character's own idle.
The numerical score is heuristic, not a proof of anatomy or original detail.
"""
import argparse
import json
import statistics
from collections import defaultdict
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageFilter

ROOT=Path(__file__).resolve().parents[2]
SIZES={"720p":720,"1080p":1080,"1440p":1440,"4K":2160}


def measure(rgba):
    img=np.asarray(rgba,dtype=np.uint8)
    if img.ndim!=3 or img.shape[2]!=4: raise ValueError("expected RGBA frame")
    mask=(img[:,:,3]>=250).astype(np.uint8)
    if int(mask.sum())<100:
        return dict(valid=False,sharpness=0.,height=0,opaquePixels=int(mask.sum()))
    interior=cv2.erode(mask,np.ones((3,3),np.uint8))>0
    if interior.sum()<90: interior=mask>0
    gray=cv2.cvtColor(img[:,:,:3],cv2.COLOR_RGB2GRAY).astype(np.float32)
    lap=cv2.Laplacian(gray,cv2.CV_32F,ksize=3)
    ys=np.nonzero(mask)[0]
    return dict(valid=True,sharpness=round(float(np.std(lap[interior])),3),
                height=int(ys.max()-ys.min()+1),opaquePixels=int(mask.sum()))


def config_files(root):
    return {d["id"]:d for p in (root/"tools/sprites/clips").glob("*.json")
            for d in (json.loads(p.read_text(encoding="utf-8")),)}


def unpack_layout(root,cfg):
    report=json.loads((root/cfg["report"]).read_text(encoding="utf-8"))
    x=report.get("packed",report["layout"])
    return (x["frameWidth"],x["frameHeight"],x["frameCount"],x["columns"])


def sample_frames(root,cfg,indices,count=5):
    fw,fh,n,cols=unpack_layout(root,cfg)
    src=root/cfg["output"]
    if not src.resolve().is_relative_to(root.resolve()): raise ValueError("unsafe path")
    unique=list(dict.fromkeys(indices))
    if len(unique)>count:
        unique=[unique[round(i)] for i in np.linspace(0,len(unique)-1,count)]
    with Image.open(src) as original:
        expected=(fw*cols,fh*((n+cols-1)//cols))
        if original.size!=expected:
            raise ValueError(f"{src}: unexpected packed size {original.size} vs {expected}")
        frames=[]
        for idx in unique:
            if not 0<=idx<n: raise ValueError(f"{src}: invalid frame {idx}")
            x=(idx%cols)*fw; y=(idx//cols)*fh
            frames.append(np.array(original.crop((x,y,x+fw,y+fh)).convert("RGBA")))
    return frames


def conservative_sharpen(frame):
    """Sharpen only fully opaque RGB interior, preserve alpha and all edge pixels."""
    original=np.asarray(frame,dtype=np.uint8)
    base=Image.fromarray(original,"RGBA")
    candidate=np.asarray(base.convert("RGB").filter(
        ImageFilter.UnsharpMask(radius=.8,percent=85,threshold=3)))
    out=original.copy()
    interior=cv2.erode((original[:,:,3]>=250).astype(np.uint8),
                       np.ones((3,3),np.uint8))>0
    out[:,:,:3][interior]=candidate[interior]
    return out


def safe_gain(original,updated):
    """Accept only measurable, limited sharpening without introducing harsh clipping."""
    if not np.array_equal(original[:,:,3],updated[:,:,3]):
        return False,"alpha altered",0.
    a,b=measure(original),measure(updated)
    if not a["valid"] or not b["valid"]:return False,"empty frame",0.
    gain=b["sharpness"]/max(.01,a["sharpness"])
    inner=cv2.erode((original[:,:,3]>=250).astype(np.uint8),
                    np.ones((3,3),np.uint8))>0
    before=original[:,:,:3][inner]
    after=updated[:,:,:3][inner]
    clip1=np.any((before<=1)|(before>=254),axis=1)
    clip2=np.any((after<=1)|(after>=254),axis=1)
    new_clipping=float(np.mean(clip2&~clip1)) if len(clip1) else 1.
    if not (1.06<=gain<=1.85):return False,"unhelpful or excessive sharpening",round(gain,3)
    if new_clipping>.035:return False,"newly clipped color values",round(gain,3)
    return True,"safe internal edge refinement",round(gain,3)


def improve_atlas(root,cfg,output,used_frames):
    """Optional PC atlas (same pixels/layout/alpha); never overwrite shipped asset."""
    fw,fh,n,cols=unpack_layout(root,cfg)
    with Image.open(root/cfg["output"]) as f:img=np.asarray(f.convert("RGBA"))
    edited=img.copy()
    changed=0
    for idx in sorted(set(used_frames)):
        x=(idx%cols)*fw;y=(idx//cols)*fh
        original=img[y:y+fh,x:x+fw]
        candidate=conservative_sharpen(original)
        ok,_,_=safe_gain(original,candidate)
        if ok:
            edited[y:y+fh,x:x+fw]=candidate
            changed+=1
    if not changed:return dict(exported=False,improvedFrames=0)
    assert np.array_equal(edited[:,:,3],img[:,:,3])
    output.parent.mkdir(parents=True,exist_ok=True)
    Image.fromarray(edited,"RGBA").save(output,optimize=True)
    return dict(exported=True,improvedFrames=changed,path=str(output))


def audit(root=ROOT,character="player_base",output=None,enhance=False):
    data=json.loads((root/"characters"/character/"character.json").read_text(encoding="utf-8"))
    out=Path(output or root/"android/app/build/quality-pc"/character)
    out.mkdir(parents=True,exist_ok=True)
    config=config_files(root)
    scores={}
    for state,anim in data["animations"].items():
        frames=sample_frames(root,config[anim["atlas"]],anim["frames"])
        metrics=[measure(x) for x in frames]
        good=[m for m in metrics if m["valid"]]
        score=float(statistics.median(m["sharpness"] for m in good)) if good else 0.
        height=float(statistics.median(m["height"] for m in good)) if good else 0.
        scores[state]=dict(atlas=anim["atlas"],sharpness=round(score,3),
                           height=height,valid=bool(good),inspected=len(frames),
                           nativeToScreen={k:{"scale":round(v/720,2),
                                "projectedHeight":round(height*v/720)}
                                for k,v in SIZES.items()})
    idle=scores.get("IDLE",{}).get("sharpness") or 1.
    soft=[]
    for state,m in scores.items():
        m["relativeToIdle"]=round(m["sharpness"]/idle,3)
        m["suspectedSoft"]=m["valid"] and m["relativeToIdle"]<.53
        m["needs4KSource"]=True
        if m["suspectedSoft"]:soft.append(state)
    exports=[]
    if enhance:
        unique=defaultdict(set)
        for state,anim in data["animations"].items():
            if scores[state]["relativeToIdle"]<.85:
                unique[anim["atlas"]].update(anim["frames"])
        for atlas,ids in sorted(unique.items()):
            cfg=config[atlas]
            result=improve_atlas(root,cfg,out/"optional-pc-atlases"/Path(cfg["output"]).name,ids)
            exports.append(dict(atlas=atlas,**result))
    report=dict(character=character,statesAudited=len(scores),suspectedSoft=soft,
                potential4KUpscale=3.,sourceVideoInspected=False,
                originalArtModified=False,usedByPCCurrently=False,
                exports=exports,perState=scores)
    (out/"QUALITY.json").write_text(json.dumps(report,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    lines=["# Sprite PC quality — "+character,"",
           "Original art unchanged; scores are relative heuristics, not proof of resolution.",
           "At 4K, the 720p virtual stage is enlarged ~3x before camera zoom.",
           "Without high-resolution original art, sharpening cannot restore missing features.","",
           "|State|Sharpness vs idle|4K apparent height|Status|",
           "|---|---:|---:|---|"]
    for state,metrics in scores.items():
        lines.append("|{}|{:.2f}x|{}px|{}|".format(
            state,metrics["relativeToIdle"],
            metrics["nativeToScreen"]["4K"]["projectedHeight"],
            "REVIEW SOURCE" if metrics["suspectedSoft"] else "OK / SCALE REVIEW"))
    (out/"QUALITY.md").write_text("\n".join(lines)+"\n",encoding="utf-8")
    print(json.dumps({"character":character,"statesAudited":len(scores),
          "suspectedSoft":soft,"optionalPCAtlasesExported":sum(bool(x["exported"]) for x in exports)}))
    return report


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--character",default="player_base")
    p.add_argument("--output")
    p.add_argument("--enhance",action="store_true")
    p.add_argument("--strict",action="store_true")
    args=p.parse_args()
    report=audit(character=args.character,
                 output=ROOT/args.output if args.output else None,enhance=args.enhance)
    if args.strict and any(not x["valid"] for x in report["perState"].values()):
        raise SystemExit("Invalid sprite frame(s)")


if __name__=="__main__":
    main()
