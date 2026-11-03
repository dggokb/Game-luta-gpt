#!/usr/bin/env python3
"""P01 Shinkai: 26-frame 2x HD sprite (ESRGAN from original 1x), plus 1.5x effect.
Uses the exact current P01 2x sprite alpha, WebP runtime packing and effect timings.
Do not edit gameplay data or other characters.
"""
import argparse
import hashlib
import json
import math
import os
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CFG = ROOT/"tools/sprites/clips/player_base_shinkai.json"
EFFECT = ROOT/"android/app/src/main/assets/efeitos/shinkai_player_base"
MODEL_SHA = "b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d"
EFFECT_SCALE = 1.5

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def load():
    return json.loads(CFG.read_text()), json.loads((EFFECT/"efeito.json").read_text())

def geo(c):
    return tuple(int(c[k]) for k in ("frameWidth","frameHeight","rootX","rootY","columns","expectedFrames"))

def sources(c):
    one = ROOT/"art/sprites/source/player_base_shinkai_video_normalized.png"
    two = ROOT/c["source"]
    return one,two

def plan():
    from PIL import Image
    c,e=load()
    w,h,rx,ry,cols,n=geo(c)
    assert c["id"]=="player_base_shinkai" and c["pixelScale"]==2 and n==26 and cols==7
    assert (w,h,rx,ry)==(512,640,256,604)
    assert c["segmentation"]=="prepared-grid" and "transform" not in c
    assert c["output"].endswith(".webp")
    original,current=sources(c)
    with Image.open(original) as source, Image.open(current) as hd, Image.open(EFFECT/"folha.webp") as ef:
        assert source.size==(cols*w//2,h//2*math.ceil(n/cols)),source.size
        assert hd.size==(cols*w,h*math.ceil(n/cols)),hd.size
        assert ef.width>0 and ef.height>0
        newdims=(round(ef.width*EFFECT_SCALE),round(ef.height*EFFECT_SCALE))
        assert max(newdims)<=8192 and newdims[0]*newdims[1]*4<64*1024*1024
    assert len(e["quadros"])==len(e["duracoesMs"])==35
    assert e["pixelScale"]==0.75 and len(e["quadros"][0])==6
    print("P01 Shinkai: 26 source frames, 2x runtime geometry, 35 effect frames",flush=True)
    print("EFFECT OUTPUT DIMENSIONS:",newdims,flush=True)

def neural(weights):
    import torch
    sys.path.insert(0,str(ROOT/"art/redraws/p03_hd/idle_sr_4x"))
    from srvgg_arch import SRVGGNetCompact
    if sha(weights)!=MODEL_SHA:
        raise ValueError("Unexpected Real-ESRGAN AnimeVideo-v3 model checksum")
    torch.set_num_threads(4)
    torch.set_num_interop_threads(1)
    torch.manual_seed(0)
    torch.use_deterministic_algorithms(True)
    model=SRVGGNetCompact(num_in_ch=3,num_out_ch=3,num_feat=64,num_conv=16,upscale=4,act_type="prelu")
    state=torch.load(weights,map_location="cpu",weights_only=True)
    model.load_state_dict(state["params"],strict=True)
    model.eval()
    return model

def sr_cell(img,alpha,model):
    """4x model, downsample to exact output, preserve supplied authoring alpha."""
    import numpy as np
    import torch
    from scipy.ndimage import distance_transform_edt
    from PIL import Image
    rgba=np.asarray(img.convert("RGBA")).copy()
    opaque=rgba[:,:,3]>=128
    if not opaque.any():
        raise ValueError("Transparent Shinkai frame: refuse to invent artwork")
    invisible=~opaque
    _,idx=distance_transform_edt(invisible,return_indices=True)
    rgb=rgba[:,:,:3].copy()
    rgb[invisible]=rgb[idx[0][invisible],idx[1][invisible]]
    tensor=torch.from_numpy(np.ascontiguousarray(rgb.transpose(2,0,1))).float().unsqueeze(0)/255.
    with torch.inference_mode():
        recon=model(tensor).clamp_(0,1)[0].permute(1,2,0).numpy()
    rgb4=np.rint(recon*255).astype(np.uint8)
    a4=img.getchannel("A").resize((img.width*4,img.height*4),Image.Resampling.LANCZOS)
    rgb4[np.asarray(a4)==0]=0
    master=Image.fromarray(rgb4,"RGB")
    master.putalpha(a4)
    master_hash=hashlib.sha256(master.tobytes()).hexdigest()
    target=master.convert("RGBa").resize(alpha.size,Image.Resampling.LANCZOS).convert("RGBA")
    target.putalpha(alpha)
    assert target.getchannel("A").tobytes()==alpha.tobytes()
    return target,master_hash

def render_sprite(out,model):
    from PIL import Image
    c,_=load()
    orig,current=sources(c)
    w,h,rx,ry,cols,n=geo(c)
    base=Image.open(orig).convert("RGBA")
    existing=Image.open(current).convert("RGBA")
    built=Image.new("RGBA",existing.size)
    masters=[]
    for i in range(n):
        one=base.crop((i%cols*w//2,i//cols*h//2,(i%cols+1)*w//2,(i//cols+1)*h//2))
        alpha=existing.crop((i%cols*w,i//cols*h,(i%cols+1)*w,(i//cols+1)*h)).getchannel("A")
        cell,digest=sr_cell(one,alpha,model)
        masters.append(digest)
        built.paste(cell,(i%cols*w,i//cols*h))
        print("SHINKAI SPRITE:",i+1,"/",n,flush=True)
    out.mkdir(parents=True,exist_ok=True)
    result=out/"player_base_shinkai.png"
    built.save(result)
    detail={"type":"sprite","clip_sha256":sha(CFG),"original_1x_sha256":sha(orig),
            "original_2x_sha256":sha(current),"model_sha256":MODEL_SHA,
            "frameCount":n,"frameWidth":w,"frameHeight":h,"pixelScale":2,
            "master_hashes":masters,"output_sha256":sha(result)}
    (out/"sprite-manifest.json").write_text(json.dumps(detail,indent=2)+"\n")
    print("SHINKAI SPRITE RENDERED:",result.stat().st_size,flush=True)

def region(rect,scale):
    x,y,w,h=map(int,rect[:4])
    xx=round(x*scale); yy=round(y*scale)
    ww=round((x+w)*scale)-xx
    hh=round((y+h)*scale)-yy
    return xx,yy,ww,hh

def render_effect(out,model):
    from PIL import Image
    _,e=load()
    original=Image.open(EFFECT/"folha.webp").convert("RGBA")
    dims=(round(original.width*EFFECT_SCALE),round(original.height*EFFECT_SCALE))
    if max(dims)>8192 or dims[0]*dims[1]*4>=64*1024*1024:
        raise ValueError("Shinkai effect atlas exceeds 64MiB/8192 texture budget")
    improved=Image.new("RGBA",dims)
    rects=[]
    masters=[]
    for i,row in enumerate(e["quadros"]):
        x,y,w,h=map(int,row[:4])
        src=original.crop((x,y,x+w,y+h))
        nx,ny,nw,nh=region(row,EFFECT_SCALE)
        alpha=src.getchannel("A").resize((nw,nh),Image.Resampling.LANCZOS)
        tile,digest=sr_cell(src,alpha,model)
        improved.paste(tile,(nx,ny))
        rects.append([nx,ny,nw,nh,round(row[4]*EFFECT_SCALE,4),round(row[5]*EFFECT_SCALE,4)])
        masters.append(digest)
        print("SHINKAI EFFECT:",i+1,"/",len(e["quadros"]),flush=True)
    updated=dict(e)
    updated["quadros"]=rects
    updated["pixelScale"]=round(e["pixelScale"]*EFFECT_SCALE,6)
    updated["hdInfo"]={"method":"Real-ESRGAN AnimeVideo-v3","imageScale":EFFECT_SCALE,
                       "originalPixelScale":e["pixelScale"],"sourceSha256":sha(EFFECT/"folha.webp"),
                       "modelSha256":MODEL_SHA}
    out.mkdir(parents=True,exist_ok=True)
    img=out/"shinkai-effect.webp"
    improved.save(img,format="WEBP",lossless=True,quality=100,method=4)
    (out/"shinkai-effect.json").write_text(json.dumps(updated,indent=2,ensure_ascii=False)+"\n")
    meta={"type":"effect","effect_json_sha256":sha(EFFECT/"efeito.json"),
          "effect_atlas_sha256":sha(EFFECT/"folha.webp"),
          "model_sha256":MODEL_SHA,"frameCount":len(rects),
          "outputDimensions":list(dims),"scale":EFFECT_SCALE,
          "master_hashes":masters,
          "output_sha256":sha(img),"output_json_sha256":sha(out/"shinkai-effect.json")}
    (out/"effect-manifest.json").write_text(json.dumps(meta,indent=2)+"\n")
    print("SHINKAI EFFECT RENDERED:",dims,img.stat().st_size,flush=True)

def rebuild_sprite():
    """Touch only P01 Shinkai WebP + report; reuse every other validated report."""
    import import_sprites as importer
    import build_characters as pipeline
    from PIL import Image
    prior=(importer.ROOT,importer.PROFILES_DIR,importer.GENERATED_JAVA)
    importer.ROOT=ROOT
    importer.PROFILES_DIR=ROOT/"tools/sprites/profiles"
    importer.GENERATED_JAVA=ROOT/pipeline.JAVA/"GeneratedSpriteLayouts.java"
    results=[]
    try:
        for p in sorted((ROOT/"tools/sprites/clips").glob("*.json")):
            c=json.loads(p.read_text())
            if p==CFG:
                c,r=importer.process_clip(p)
                pipeline.pack_atlas(ROOT,c,r)
                r["provenance"]={"pipelineVersion":2,"sha256":{
                    "source":sha(ROOT/c["source"]),"config":sha(p),
                    "profile":sha(importer.PROFILES_DIR/c["profile"])},
                    "referenceSources":{x:sha(ROOT/x) for x in c.get("sourceReferences",[])}}
                (ROOT/c["report"]).write_text(json.dumps(r,indent=2)+"\n")
            else:
                r=json.loads((ROOT/c["report"]).read_text())
            if r["id"]!=c["id"]:
                raise ValueError("Other atlas report was stale: "+c["id"])
            results.append((c,r))
        importer.write_generated_java(results)
        pipeline.compile_packs(ROOT,results)
        c,r=next((c,r) for c,r in results if c["id"]=="player_base_shinkai")
        assert r["passed"] and len(r["frames"])==26 and r["packed"]["frameCount"]==26
        assert c["pixelScale"]==r["packed"].get("pixelScale")==2
        assert len(r["packed"]["frames"])==26
        with Image.open(ROOT/c["output"]) as bmp:
            assert bmp.size==(r["packed"]["sheetWidth"],r["packed"]["sheetHeight"])
            assert bmp.width<=8192 and bmp.height<=8192
        assert r["packed"]["decodedBytes"]["packed"]<64*1024*1024
        print("Shinkai runtime WebP and global Java validation passed",flush=True)
    finally:
        importer.ROOT,importer.PROFILES_DIR,importer.GENERATED_JAVA=prior

def integrate(folder):
    from PIL import Image
    c,e=load()
    orig,existing=sources(c)
    png=folder/"player_base_shinkai.png"
    em=folder/"shinkai-effect.webp"
    ej=folder/"shinkai-effect.json"
    sprite_meta=json.loads((folder/"sprite-manifest.json").read_text())
    effect_meta=json.loads((folder/"effect-manifest.json").read_text())
    target_effect=json.loads(ej.read_text())
    assert sprite_meta["clip_sha256"]==sha(CFG)
    assert sprite_meta["original_1x_sha256"]==sha(orig)
    assert sprite_meta["original_2x_sha256"]==sha(existing)
    assert sprite_meta["model_sha256"]==effect_meta["model_sha256"]==MODEL_SHA
    assert sprite_meta["output_sha256"]==sha(png) and len(sprite_meta["master_hashes"])==26
    assert effect_meta["effect_json_sha256"]==sha(EFFECT/"efeito.json")
    assert effect_meta["effect_atlas_sha256"]==sha(EFFECT/"folha.webp")
    assert effect_meta["output_sha256"]==sha(em)
    assert effect_meta["output_json_sha256"]==sha(ej)
    assert len(effect_meta["master_hashes"])==35
    with Image.open(existing) as src,Image.open(png) as improved:
        assert src.size==improved.size
        src=src.convert("RGBA");improved=improved.convert("RGBA")
        assert src.getchannel("A").tobytes()==improved.getchannel("A").tobytes(),"P01 original HD alpha must not change"
        assert src.tobytes()!=improved.tobytes(),"No RGB improvement generated"
    assert target_effect["duracoesMs"]==e["duracoesMs"]
    assert target_effect["marcaMs"]==e["marcaMs"]
    assert target_effect["pixelScale"]==round(e["pixelScale"]*EFFECT_SCALE,6)
    assert len(target_effect["quadros"])==len(e["quadros"])==35
    with Image.open(EFFECT/"folha.webp") as src,Image.open(em) as improved:
        src=src.convert("RGBA");improved=improved.convert("RGBA")
        assert improved.size==(round(src.width*EFFECT_SCALE),round(src.height*EFFECT_SCALE))
        for i,(a,b) in enumerate(zip(e["quadros"],target_effect["quadros"])):
            x,y,w,h=region(a,EFFECT_SCALE)
            assert b[:4]==[x,y,w,h],i
            assert abs(b[4]/target_effect["pixelScale"]-a[4]/e["pixelScale"])<0.001
            assert abs(b[5]/target_effect["pixelScale"]-a[5]/e["pixelScale"])<0.001
            s=src.crop((a[0],a[1],a[0]+a[2],a[1]+a[3])).getchannel("A")
            t=improved.crop((x,y,x+w,y+h)).getchannel("A")
            assert s.resize((w,h),Image.Resampling.LANCZOS).tobytes()==t.tobytes(),i
    newsource=ROOT/"art/sprites/source/player_base_shinkai_sr_hd.png"
    shutil.copyfile(png,newsource)
    oldsource=c["source"]
    c["sourceReferences"]=list(dict.fromkeys([*c.get("sourceReferences",[]),oldsource]))
    c["source"]="art/sprites/source/player_base_shinkai_sr_hd.png"
    c["sourceNote"]=c["sourceNote"]+" Real-ESRGAN AnimeVideo-v3 4x from original 1x, downsampled to 2x; preserves original approved HD alpha."
    CFG.write_text(json.dumps(c,indent=2,ensure_ascii=False)+"\n")
    shutil.copyfile(em,EFFECT/"folha.webp")
    shutil.copyfile(ej,EFFECT/"efeito.json")
    rebuild_sprite()
    report={"status":"P01 Shinkai HD source + effect integrated on test branch; visual review required",
            "fighterFrames":26,"fighterPixelScale":2,
            "fighterMasterModel":MODEL_SHA,"fighterAtlasSha256":sha(ROOT/c["output"]),
            "effectFrames":35,"effectPixelScale":target_effect["pixelScale"],
            "effectAtlasSha256":sha(EFFECT/"folha.webp"),
            "unchangedFrameCounts":True,"unchangedEffectTiming":True,"unchangedFighterAlpha":True}
    docs=ROOT/"docs/art"
    docs.mkdir(parents=True,exist_ok=True)
    (docs/"p01-shinkai-hd-results.json").write_text(json.dumps(report,indent=2)+"\n")
    print(json.dumps(report,indent=2),flush=True)

def main():
    p=argparse.ArgumentParser()
    sub=p.add_subparsers(dest="command",required=True)
    sub.add_parser("plan")
    r=sub.add_parser("render")
    r.add_argument("--mode",choices=("sprite","effect"),required=True)
    r.add_argument("--weights",type=Path,required=True)
    r.add_argument("--output-dir",type=Path,required=True)
    x=sub.add_parser("integrate")
    x.add_argument("--input-dir",type=Path,required=True)
    args=p.parse_args()
    if args.command=="plan":plan()
    elif args.command=="render":
        plan()
        model=neural(args.weights)
        if args.mode=="sprite": render_sprite(args.output_dir,model)
        else: render_effect(args.output_dir,model)
    else:integrate(args.input_dir)

if __name__=="__main__":
    main()
