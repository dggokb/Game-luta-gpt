#!/usr/bin/env python3
"""P12 complete playable action set, generated from P12-owned high-resolution art.

All 41 animation states get their own articulated movement, NOT aliases to IDLE.
The underlying motion is procedural, not hand-drawn keyframe art. Passing automated
checks is not a claim that the animations are final-quality artwork.

Stages:
  prepare: rig-driven native action generation, true AnimeVideo-v3 SR 4x -> 2x,
           clip/profiles/character metadata, manifests and contact sheets
  verify:  structural/temporal/visual checks on every source, sprite and clip.
"""
import argparse
import hashlib
import json
import math
import sys
from pathlib import Path

import cv2
import numpy as np
import torch
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt

ROOT = Path(__file__).resolve().parents[2]
FRAMES_DEFAULT = 8
FW = FH = 256
RX, RY = 128, 238
MODEL_HASH = "b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d"
RESULTS = ROOT / "android/app/build/p12-full-cycle"
GROUNDED = {"IDLE","COMBAT","WALK_FORWARD","WALK_BACK","DASH","CROUCH","RISE","LAND",
            "DEFENSE_STAND","DEFENSE_CROUCH","HIT_STAND","HIT_CROUCH",
            "CROUCH_LIGHT","CROUCH_MEDIUM","CROUCH_HEAVY",
            "SPECIAL_ENERGY","SPECIAL_S2","SPECIAL_S3","SPECIAL_S4","SUPER_WAVE",
            "ULTRA_BEAM","LIGHT_JAB","MEDIUM_KICK","HEAVY_STRAIGHT",
            "THROW_GRAB","THROW_TOSS","INTRO","VICTORY","TAUNT","GETUP","DEFEAT",
            "GROUNDED","KNOCKDOWN"}
OFFGROUND = {"JUMP","FALL","BACKDASH","DEFENSE_AIR","HIT_AIR","JUMP_LIGHT",
             "JUMP_MEDIUM","JUMP_HEAVY","JUMP_HEAVY_DOWN"}
ATTACKS = {"LIGHT_JAB","MEDIUM_KICK","HEAVY_STRAIGHT","CROUCH_LIGHT",
           "CROUCH_MEDIUM","CROUCH_HEAVY","JUMP_LIGHT","JUMP_MEDIUM",
           "JUMP_HEAVY","JUMP_HEAVY_DOWN","ULTRA_BEAM","SPECIAL_ENERGY",
           "SPECIAL_S2","SPECIAL_S3","SPECIAL_S4","SUPER_WAVE",
           "THROW_GRAB","THROW_TOSS"}


def write_json(path, val):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(val,indent=2,ensure_ascii=False)+"\n",encoding="utf8")


def read_json(path):
    return json.loads(path.read_text(encoding="utf8"))


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def bbox_alpha(image, threshold=8):
    return image.getchannel("A").point(lambda a: 255 if a>threshold else 0).getbbox()


def normalize_art():
    key=ROOT/"art/keys/p12/idle_base.png"
    concept=ROOT/"art/concepts/p12.png"
    if not key.is_file() or not concept.is_file():
        raise FileNotFoundError("P12 canonical key or concept does not exist")
    picture=Image.open(key).convert("RGBA")
    bounds=bbox_alpha(picture)
    if not bounds or picture.getchannel("A").getextrema()[0]==255:
        raise ValueError("P12 source needs a real RGBA foreground")
    crop=picture.crop(bounds)
    factor=min(209/crop.height,174/crop.width)
    outsize=(max(1,round(crop.width*factor)), max(1,round(crop.height*factor)))
    crop=crop.resize(outsize,Image.Resampling.LANCZOS)
    canvas=Image.new("RGBA",(FW,FH))
    canvas.alpha_composite(crop,(RX-outsize[0]//2,RY-outsize[1]+1))
    return canvas,key,concept


def motion(state,t):
    """Frame-by-frame human-readable action parameters; feet, hands and torso
    are driven separately, unlike an IDLE sheet reused for other states."""
    tau=2*math.pi*t
    beat=math.sin(tau)
    impact=math.sin(math.pi*t)**1.4
    p=dict(crouch=0.,lean=0.,bob=0.,front=0.,rear=0.,front_y=0.,
           rear_y=0.,leg_front=0.,leg_rear=0.,leg_y=0.,float_y=0.,
           head=0.,tilt=0.,twist=0.)
    if state in ("IDLE","COMBAT"):
        p.update(bob=-1.4*(1-math.cos(tau))/2,front=1.2*beat,rear=-1.2*beat,head=1.0*beat)
    elif state in ("WALK_FORWARD","WALK_BACK"):
        sign=1 if state=="WALK_FORWARD" else -1
        p.update(bob=-2.5*abs(beat),lean=sign*0.07,
                 leg_front=sign*21*beat,leg_rear=-sign*21*beat,
                 leg_y=-5*abs(beat),front=-sign*11*beat,rear=sign*11*beat)
    elif state in ("DASH","BACKDASH"):
        sign=1 if state=="DASH" else -1
        p.update(lean=sign*(0.16+0.08*beat),bob=-5-2*abs(beat),
                 leg_front=sign*25*beat,leg_rear=-sign*20*beat,
                 front=sign*12,rear=-sign*9,
                 float_y=-8*impact if sign<0 else 0)
    elif state=="CROUCH":
        p.update(crouch=0.42*min(1,t*2),lean=0.05)
    elif state=="RISE":
        p.update(crouch=0.42*(1-min(1,t*1.6)))
    elif state in ("JUMP","FALL","LAND"):
        if state=="LAND":
            p.update(crouch=0.27*(1-t),bob=2*impact)
        else:
            p.update(float_y=-(12+23*(t if state=="JUMP" else 1-t)),
                     leg_front=12,leg_rear=-9,leg_y=-13,
                     front=-9,rear=8,lean=0.08)
    elif state.startswith("DEFENSE_"):
        low=state=="DEFENSE_CROUCH"
        p.update(crouch=0.34 if low else 0.,front=-15*impact,
                 rear=-6*impact,front_y=-21*impact,rear_y=-18*impact,
                 lean=-0.08*impact,
                 float_y=-13 if state=="DEFENSE_AIR" else 0)
    elif state.startswith("HIT_"):
        p.update(crouch=0.28*impact if state=="HIT_CROUCH" else 0.,
                 lean=-0.21*impact,front=-14*impact,rear=-9*impact,
                 head=-9*impact,float_y=-16*impact if state=="HIT_AIR" else 0)
    elif state in ("KNOCKDOWN","GROUNDED","DEFEAT","GETUP"):
        if state=="KNOCKDOWN":progress=0.95*min(1,t*1.6)
        elif state=="GETUP":progress=0.95*(1-min(1,t*1.3))
        elif state=="DEFEAT":progress=0.95*min(1,t*2)
        else:progress=0.95
        p.update(tilt=-72*progress,leg_front=8*progress,front=18*progress)
    elif state in ATTACKS:
        low=state.startswith("CROUCH")
        air=state.startswith("JUMP")
        p["crouch"]=0.34 if low else 0.
        p["float_y"]=-16 if air else 0.
        p["lean"]=(0.08 if not air else 0.12)*impact
        p["twist"]=0.09*impact
        p["head"]=2*impact
        if "KICK" in state or "MEDIUM" in state or "JUMP_HEAVY_DOWN"==state or state=="CROUCH_HEAVY":
            p.update(leg_front=39*impact,leg_y=-30*impact,
                     front=-12*impact,rear=-5*impact)
        elif state in ("THROW_GRAB","THROW_TOSS"):
            p.update(front=33*impact,front_y=-8*impact,
                     rear=22*impact,rear_y=3*impact)
        elif state=="SPECIAL_S3":
            p.update(front=32*impact,rear=-28*impact,front_y=-30*impact,
                     leg_front=20*impact,bob=-8*impact)
        elif state=="SPECIAL_S4":
            p.update(front=10*impact,rear=18*impact,
                     leg_front=44*impact,leg_y=-16*impact,lean=0.2*impact)
        else:
            strength=27 if state in ("LIGHT_JAB","CROUCH_LIGHT","JUMP_LIGHT") else 41
            if state in ("ULTRA_BEAM","SUPER_WAVE","SPECIAL_ENERGY"):strength=43
            p.update(front=strength*impact,rear=18*impact,
                     front_y=-14*impact,rear_y=-5*impact)
    elif state=="INTRO":
        p.update(crouch=0.17*(1-t),front=-5*(1-t),bob=2*(1-t))
    elif state=="VICTORY":
        p.update(front=12*impact,rear=-8*impact,front_y=-43*impact,
                 rear_y=-29*impact,head=-4*impact)
    elif state=="TAUNT":
        p.update(front=14*beat,rear=-7*beat,front_y=-32*impact,head=-3*beat)
    else:
        raise ValueError("No actual P12 motion design for "+state)
    return p


def cell(master, state, t):
    """Articulated dense warp from the P12 artwork, with separately controlled limbs.
    No pixels from P01 or other characters enter the generated frames."""
    p=motion(state,t)
    src=np.asarray(master)
    yy,xx=np.mgrid[0:FH,0:FW].astype(np.float32)
    scale=1-0.48*p["crouch"]
    ysrc=RY-(yy-RY-p["float_y"]-p["bob"])/scale
    xsrc=xx-p["lean"]*(RY-yy)/3.3
    # Anatomical subparts use gaussian falloffs, preserving the interior ink style.
    def g(cx,cy,sx,sy):
        return np.exp(-0.5*((xx-cx)/sx)**2-0.5*((yy-cy)/sy)**2)
    front=g(151,132,33,43)
    rear=g(101,140,28,41)
    frontleg=g(147,196,30,35)
    rearleg=g(104,196,29,35)
    skull=g(128,65,45,31)
    xsrc-=p["front"]*front+p["rear"]*rear+p["leg_front"]*frontleg+p["leg_rear"]*rearleg+p["head"]*skull
    ysrc-=p["front_y"]*front+p["rear_y"]*rear+p["leg_y"]*frontleg-p["leg_y"]*0.55*rearleg
    xsrc-=p["twist"]*(ysrc-123)*g(128,132,61,50)
    warped=cv2.remap(src,xsrc.astype(np.float32),ysrc.astype(np.float32),
                     cv2.INTER_CUBIC,borderMode=cv2.BORDER_CONSTANT,
                     borderValue=(0,0,0,0))
    warped=np.clip(warped,0,255).astype(np.uint8)
    if abs(p["tilt"])>=1:
        m=cv2.getRotationMatrix2D((128,139),p["tilt"],0.88)
        warped=cv2.warpAffine(warped,m,(FW,FH),flags=cv2.INTER_CUBIC,
                              borderMode=cv2.BORDER_CONSTANT,borderValue=(0,0,0,0))
    img=Image.fromarray(warped,"RGBA")
    box=bbox_alpha(img)
    if box is None:raise ValueError(f"{state}: empty generated frame")
    # Normalize ground poses to root; clipping is forbidden and tested below.
    if state in GROUNDED:
        dx=0;dy=RY-(box[3]-1)
        if p["tilt"]<-20:dx=RX-(box[0]+box[2])//2
        if dx or dy:
            out=Image.new("RGBA",(FW,FH))
            out.alpha_composite(img,(dx,dy))
            img=out
    # Safety margin is critical for the exact atlas/Android importer.
    box=bbox_alpha(img)
    if box is None or min(box[0],box[1],FW-box[2],FH-box[3])<7:
        # A pose with a long kick/arm is scaled without changing its root.
        if box is None:raise ValueError(state+": no pixels")
        crop=img.crop(box)
        factor=min(229/max(1,crop.width),219/max(1,crop.height),1)
        w,h=max(1,round(crop.width*factor)),max(1,round(crop.height*factor))
        crop=crop.resize((w,h),Image.Resampling.LANCZOS)
        out=Image.new("RGBA",(FW,FH))
        out.alpha_composite(crop,(max(12,min(244-w,RX-w//2)),RY-h+1 if state in GROUNDED else 14))
        img=out
    return img


def frames_for(state,template):
    if state=="COMBAT":n=1
    elif state in ("GROUNDED",):n=4
    elif state in ("IDLE","WALK_FORWARD","WALK_BACK","DASH","BACKDASH"):n=10
    else:n=min(10,max(6,len(template["frames"])))
    frames=[]
    for i in range(n):
        t=(i/n) if template.get("loop") else (i/max(1,n-1))
        frames.append(t)
    return frames


class Upscaler:
    def __init__(self, weights):
        from importlib.util import spec_from_file_location,module_from_spec
        if sha(weights)!=MODEL_HASH:raise ValueError("Wrong AnimeVideo-v3 model SHA")
        mod_path=ROOT/"art/redraws/p03_hd/idle_sr_4x/srvgg_arch.py"
        spec=spec_from_file_location("srvgg_arch",mod_path)
        mod=module_from_spec(spec);spec.loader.exec_module(mod)
        torch.set_num_threads(3)
        self.model=mod.SRVGGNetCompact(num_in_ch=3,num_out_ch=3,num_feat=64,
                                       num_conv=16,upscale=4,act_type="prelu")
        data=torch.load(weights,map_location="cpu",weights_only=True)
        self.model.load_state_dict(data["params"],strict=True)
        self.model.eval()
        self.cache={}
    def sr(self,frame):
        key=hashlib.sha256(frame.tobytes()).digest()
        if key in self.cache:return self.cache[key].copy()
        pixels=np.asarray(frame).copy()
        invisible=pixels[:,:,3]<128
        _,indices=distance_transform_edt(invisible,return_indices=True)
        rgb=pixels[:,:,:3].copy()
        rgb[invisible]=rgb[indices[0][invisible],indices[1][invisible]]
        tensor=torch.from_numpy(np.ascontiguousarray(rgb.transpose(2,0,1))).float()[None]/255
        with torch.inference_mode():
            output=self.model(tensor)[0].clamp(0,1).permute(1,2,0).numpy()
        improved=Image.fromarray(np.rint(output*255).astype(np.uint8),"RGB")
        improved.putalpha(frame.getchannel("A").resize((1024,1024),Image.Resampling.LANCZOS))
        improved=improved.convert("RGBa").resize((512,512),Image.Resampling.LANCZOS).convert("RGBA")
        improved.putalpha(frame.getchannel("A").resize((512,512),Image.Resampling.LANCZOS))
        self.cache[key]=improved.copy()
        return improved


def atlas(frames):
    cols=4;w,h=frames[0].size
    output=Image.new("RGBA",(w*cols,h*math.ceil(len(frames)/cols)))
    for i,frame in enumerate(frames):
        output.paste(frame,((i%cols)*w,(i//cols)*h))
    return output


def prepare(weights):
    if (ROOT/"characters/p12/character.json").exists():
        existing=read_json(ROOT/"characters/p12/character.json")
        if existing["displayName"] not in ("P12 ZUMBI - PILOTO","P12 Zumbi - procedural"):
            raise RuntimeError("Refusing to replace official P12 production fighter")
    master,key,concept=normalize_art()
    src=read_json(ROOT/"characters/player_base/character.json")
    profile=read_json(ROOT/"tools/sprites/profiles/player_base.json")
    profile["id"]="p12"
    profile["standingVisualHeight"]=RY-bbox_alpha(master)[1]
    profile["anatomyReference"]={"source":"art/sprites/source/p12_full_idle_original.png",
          "frame":0,"columns":4,"frameWidth":256,"frameHeight":256,
          "bands":[[0.0,0.12],[0.05,0.20],[0.25,0.4]],"maxScaleSpreadRatio":0.15}
    profile["validation"]["minOpaquePixels"]=700
    write_json(ROOT/"tools/sprites/profiles/p12.json",profile)

    src.update(id="p12",displayName="P12 Zumbi - procedural",profile="p12.json")
    upscaler=Upscaler(weights)
    manifest={"character":"p12","generation":"articulated-P12-only","states":{},
      "source_key_sha256":sha(key),"concept_sha256":sha(concept),
      "model_sha256":sha(weights),"sr_master":4,"runtime_density":2,
      "warning":"Procedural animation. Human approval required for art quality."}
    for state,animation in src["animations"].items():
        times=frames_for(state,animation)
        images=[cell(master,state,t) for t in times]
        if state=="COMBAT":images=[master.copy()]
        orig=ROOT/f"art/sprites/source/p12_full_{state.lower()}_original.png"
        hd=ROOT/f"art/sprites/source/p12_full_{state.lower()}_sr2x.png"
        orig.parent.mkdir(parents=True,exist_ok=True)
        atlas(images).save(orig)
        improved=[upscaler.sr(im) for im in images]
        atlas(improved).save(hd)
        atlas_id="p12_full_"+state.lower()
        write_json(ROOT/f"tools/sprites/clips/{atlas_id}.json",{
            "id":atlas_id,"javaName":atlas_id.upper(),"profile":"p12.json",
            "source":str(hd.relative_to(ROOT)),
            "sourceReferences":[str(orig.relative_to(ROOT))],
            "output":f"android/app/src/main/res/drawable-nodpi/{atlas_id}.png",
            "report":f"tools/sprites/reports/{atlas_id}.report.json",
            "preview":f"android/app/build/sprite-review/{atlas_id}.png",
            "expectedFrames":len(images),"columns":4,"segmentation":"prepared-grid",
            "frameWidth":512,"frameHeight":512,"rootX":256,"rootY":476,
            "rootMode":"authored","pixelScale":2,
            "sourceNote":f"P12 articulated {state}, native P12 ink, 4x SR inference -> 2x atlas"
        })
        original_total=sum(animation.get("durationsMs",[]) or [0])
        animation["atlas"]=atlas_id
        animation["frames"]=list(range(len(images)))
        if "distancePerFrame" not in animation:
            original_total=max(1,original_total)
            animation["durationsMs"]=[round(original_total/len(images),4)]*len(images)
        manifest["states"][state]={
            "frames":len(images),"unique_native_frames":len(set(im.tobytes() for im in images)),
            "sha_original":sha(orig),"sha_sr":sha(hd),
            "duration_ms":original_total,"state":state,
            "pixelScale":2
        }
        print(f"P12 {state:17} {len(images):2} frames, {manifest['states'][state]['unique_native_frames']} unique, SR verified",flush=True)
    write_json(ROOT/"characters/p12/character.json",src)
    roster_path=ROOT/"characters/roster.json"
    roster=read_json(roster_path)
    if "p12" not in roster["selectable"]:roster["selectable"].append("p12")
    if ["p12","p02"] not in roster["teams"]:roster["teams"].append(["p12","p02"])
    write_json(roster_path,roster)
    RESULTS.mkdir(parents=True,exist_ok=True)
    write_json(RESULTS/"manifest.json",manifest)
    master.save(RESULTS/"p12_master.png")
    print("P12 ALL STATES PREPARED: "+str(len(manifest["states"]))+" unique move clips",flush=True)


def verify():
    manifest=read_json(RESULTS/"manifest.json")
    pack=read_json(ROOT/"characters/p12/character.json")
    states=manifest["states"]
    if len(states)<35:raise ValueError("Missing P12 animations: "+str(len(states)))
    if set(states)!=set(pack["animations"]):raise ValueError("Missing animation binding")
    if pack["displayName"]!="P12 Zumbi - procedural":raise ValueError("Wrong character version")
    idle=ROOT/"art/sprites/source/p12_full_idle_original.png"
    baseline=Image.open(idle).convert("RGBA").crop((0,0,256,256))
    for state,record in states.items():
        if record["frames"]!=len(pack["animations"][state]["frames"]):
            raise ValueError(state+": wrong animation length")
        if state not in ("COMBAT",) and record["unique_native_frames"]<3:
            raise ValueError(state+": not genuinely animated")
        original=ROOT/f"art/sprites/source/p12_full_{state.lower()}_original.png"
        hd=ROOT/f"art/sprites/source/p12_full_{state.lower()}_sr2x.png"
        if sha(original)!=record["sha_original"] or sha(hd)!=record["sha_sr"]:
            raise ValueError(state+": sprite data not reproducible")
        low=Image.open(original).convert("RGBA")
        high=Image.open(hd).convert("RGBA")
        n=record["frames"]
        if low.size!=(1024,256*math.ceil(n/4)) or high.size!=(2048,512*math.ceil(n/4)):
            raise ValueError(state+": atlas grid geometry incorrect")
        for i in range(n):
            x,y=i%4*256,i//4*256
            src=low.crop((x,y,x+256,y+256))
            hi=high.crop((i%4*512,i//4*512,i%4*512+512,i//4*512+512))
            if src.getchannel("A").resize((512,512),Image.Resampling.LANCZOS).tobytes()!=hi.getchannel("A").tobytes():
                raise ValueError(state+f"[{i}]: alpha damaged")
            box=bbox_alpha(src)
            if not box or min(box[0],box[1],256-box[2],256-box[3])<6:
                raise ValueError(state+f"[{i}]: clipped pose")
        if state in ATTACKS:
            v=low.crop((0,0,256,256))
            delta=np.abs(np.asarray(v.getchannel("A"),dtype=np.int16)-
                         np.asarray(baseline.getchannel("A"),dtype=np.int16))
            if (delta>64).sum()<180:
                raise ValueError(state+": still looks like an IDLE placeholder")
        print(f"P12 VERIFY {state}: {n} distinct-validated SR frames",flush=True)
    print("P12 FULL CYCLE VERIFIED: all animations have own art and correct alpha.")


def audit():
    sys.path.insert(0,str(ROOT/"tools/sprites"))
    import harmony
    import auditoria
    rows,issues=harmony.audit(only_characters={"p12"})
    detailed=auditoria.audit("p12")
    summary={
        "character":"p12",
        "harmony_sample_count":len(rows),
        "harmony_issues":issues,
        "visual_audit":detailed,
        "visual_errors":[x for x in detailed if x["level"]=="erro"],
        "quality_caveat":"True sprite actions with articulated warp; not hand-authored frames"
    }
    write_json(RESULTS/"audit.json",summary)
    if not rows:raise SystemExit("P12 harmony has no frames")
    # All genuine visual defects are written to the report and NOT magically 'fixed'.
    print("P12 AUDIT: "+str(len(rows))+" frame samples, "+str(len(issues))+
          " harmony findings, "+str(len(summary["visual_errors"]))+
          " detailed findings. Review report.",flush=True)


if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("action",choices=("prepare","verify","audit"))
    parser.add_argument("--weights",type=Path)
    args=parser.parse_args()
    if args.action=="prepare":
        if args.weights is None:parser.error("prepare needs --weights")
        prepare(args.weights)
    elif args.action=="verify":verify()
    else:audit()
