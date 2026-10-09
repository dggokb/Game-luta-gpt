#!/usr/bin/env python3
"""P03 full roster super-resolution: 4x AnimeVideo-v3 RGB; source alpha, 2x game textures."""
import argparse
import hashlib
import json
import math
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CLIPS = ROOT / "tools/sprites/clips"
SOURCES = ROOT / "art/sprites/source"
SKIP = {"p03_idle"}
MODEL_SHA = "b8a8376811077954d82ca3fcf476f1ac3da3e8a68a4f4d71363008000a18b75d"
MAX_SIZE, MAX_DECODED, MARGIN = 4096, 64*1024*1024, 8

def sha(data):
    return hashlib.sha256(data).hexdigest()

def clip_paths():
    return sorted(CLIPS.glob("p03_*.json"))

def all_pending():
    return [p for p in clip_paths() if p.stem not in SKIP]

def geometry(cfg):
    profile = json.loads((ROOT/"tools/sprites/profiles"/cfg["profile"]).read_text())
    w = cfg.get("frameWidth",profile["baseFrameWidth"])
    h = cfg.get("frameHeight",profile["baseFrameHeight"])
    rx = cfg.get("rootX",profile["preferredRootX"])
    ry = cfg.get("rootY",profile["preferredRootY"])
    n, cols = cfg["expectedFrames"], cfg["columns"]
    return (int(w),int(h),int(rx),int(ry),int(n),int(cols))

def plan():
    entries = all_pending()
    assert len(clip_paths()) == 37 and len(entries) == 36, "P03 atlas inventory changed"
    for path in entries:
        cfg = json.loads(path.read_text())
        if cfg.get("segmentation") != "prepared-grid" or cfg.get("transform"):
            raise ValueError(path.name+": only reviewed, untransformed prepared-grid is supported")
        if cfg.get("pixelScale",1) != 1:
            raise ValueError(path.name+": already has a density override; review before changing")
        w,h,rx,ry,n,cols = geometry(cfg)
        from PIL import Image
        with Image.open(ROOT/cfg["source"]) as source:
            if source.size != (w*cols,h*math.ceil(n/cols)):
                raise ValueError(path.name+": source grid mismatch")
        print(f"{path.stem}: {n} frames / {w}x{h} / {cols} columns")
    matrix = json.dumps({"include":[{"clip":p.stem} for p in entries]},separators=(",",":"))
    print("PENDING COUNT:",len(entries))
    if "GITHUB_OUTPUT" in os.environ:
        with open(os.environ["GITHUB_OUTPUT"],"a") as output:
            output.write("matrix="+matrix+"\n")
    else:
        print("MATRIX:",matrix)

def validate_model(weights):
    if sha(weights.read_bytes())!=MODEL_SHA:
        raise ValueError("Real-ESRGAN official model SHA256 mismatch")

def candidate_grid(cells, rootx, rooty, preferred):
    boxes=[c.getchannel("A").getbbox() for c in cells]
    if any(b is None for b in boxes):
        raise ValueError("Unexpected fully transparent sprite cell")
    w,h=cells[0].size
    x0=max(0,min(min(b[0] for b in boxes)-MARGIN,rootx))
    y0=max(0,min(min(b[1] for b in boxes)-MARGIN,rooty))
    x1=min(w,max(max(b[2] for b in boxes)+MARGIN,rootx+1))
    y1=min(h,max(max(b[3] for b in boxes)+MARGIN,rooty+1))
    pw,ph=x1-x0,y1-y0
    options=[]
    for cols in range(1,len(cells)+1):
        rows=math.ceil(len(cells)/cols)
        if pw*cols<=MAX_SIZE and ph*rows<=MAX_SIZE and pw*ph*cols*rows*4<MAX_DECODED:
            options.append(cols)
    if not options:
        return None
    cols=min(options,key=lambda c:(abs(c-preferred),-c))
    return {"columns":cols,"rows":math.ceil(len(cells)/cols),
            "packedWidth":pw,"packedHeight":ph,
            "decodedBytes":pw*ph*cols*math.ceil(len(cells)/cols)*4}

def render(name, weights, output_dir):
    from PIL import Image
    import numpy as np
    import torch
    from scipy.ndimage import distance_transform_edt
    # Imported from the approved P03/P02/P03/P04 implementation.
    sys.path.insert(0,str(ROOT/"art/redraws/p03_hd/idle_sr_4x"))
    from srvgg_arch import SRVGGNetCompact
    if name in SKIP or not name.startswith("p03_"):
        raise ValueError("Only pending P03 atlases may be generated")
    cfg_path=CLIPS/(name+".json")
    cfg_bytes=cfg_path.read_bytes()
    cfg=json.loads(cfg_bytes)
    w,h,rx,ry,n,cols=geometry(cfg)
    if cfg.get("transform") or cfg.get("segmentation")!="prepared-grid":
        raise ValueError("Unexpected source transformation")
    source_path=ROOT/cfg["source"]
    source_hash=sha(source_path.read_bytes())
    with Image.open(source_path) as atlas:
        original=atlas.convert("RGBA")
    if original.size!=(w*cols,h*math.ceil(n/cols)):
        raise ValueError("Source sheet size mismatch")
    validate_model(weights)
    torch.set_num_threads(4)
    torch.set_num_interop_threads(1)
    torch.manual_seed(0)
    torch.use_deterministic_algorithms(True)
    model=SRVGGNetCompact(num_in_ch=3,num_out_ch=3,num_feat=64,num_conv=16,upscale=4,act_type="prelu")
    state=torch.load(weights,map_location="cpu",weights_only=True)
    model.load_state_dict(state["params"],strict=True)
    model.eval()
    cells=[]; master_hashes=[]
    for i in range(n):
        box=(i%cols*w,i//cols*h,(i%cols+1)*w,(i//cols+1)*h)
        src=original.crop(box)
        rgba=np.asarray(src).copy()
        invisible=rgba[:,:,3]<128
        _,indices=distance_transform_edt(invisible,return_indices=True)
        rgb=rgba[:,:,:3].copy()
        rgb[invisible]=rgb[indices[0][invisible],indices[1][invisible]]
        tensor=torch.from_numpy(np.ascontiguousarray(rgb.transpose(2,0,1))).float().unsqueeze(0)/255
        with torch.inference_mode():
            reconstructed=model(tensor).clamp_(0,1)[0].permute(1,2,0).numpy()
        rgb4=np.rint(reconstructed*255).astype(np.uint8)
        alpha4=src.getchannel("A").resize((w*4,h*4),Image.Resampling.LANCZOS)
        rgb4[np.asarray(alpha4)==0]=0
        master=Image.fromarray(rgb4,"RGB")
        master.putalpha(alpha4)
        master_hashes.append(sha(master.tobytes()))
        cell=master.convert("RGBa").resize((w*2,h*2),Image.Resampling.LANCZOS).convert("RGBA")
        alpha2=src.getchannel("A").resize((w*2,h*2),Image.Resampling.LANCZOS)
        cell.putalpha(alpha2)
        if cell.getchannel("A").tobytes()!=alpha2.tobytes():
            raise ValueError(f"{name} frame {i}: alpha mismatch")
        cells.append(cell)
        del tensor,reconstructed,rgb4,master
        print(f"{name} {i+1}/{n}",flush=True)
    # Repack columns rather than reducing fidelity where possible.
    scale=2.0
    chosen=candidate_grid(cells,rx*2,ry*2,cols)
    if chosen is None:
        scale=1.5
        down=[]
        for i,old in enumerate(cells):
            src=original.crop((i%cols*w,i//cols*h,(i%cols+1)*w,(i//cols+1)*h))
            cell=old.convert("RGBa").resize((round(w*scale),round(h*scale)),Image.Resampling.LANCZOS).convert("RGBA")
            cell.putalpha(src.getchannel("A").resize(cell.size,Image.Resampling.LANCZOS))
            down.append(cell)
        cells=down
        chosen=candidate_grid(cells,round(rx*scale),round(ry*scale),cols)
    if chosen is None:
        raise ValueError(f"{name}: 1.5x atlas still cannot fit 4096x4096 / 64MiB; no source was published")
    sw,sh=cells[0].size
    chosen_cols=chosen["columns"]
    sheet=Image.new("RGBA",(sw*chosen_cols,sh*chosen["rows"]))
    for i,cell in enumerate(cells):
        sheet.paste(cell,(i%chosen_cols*sw,i//chosen_cols*sh))
    output_dir.mkdir(parents=True,exist_ok=True)
    png=output_dir/(name+".png")
    sheet.save(png)
    meta={"id":name,"source":cfg["source"],"source_sha256":source_hash,
          "config_sha256":sha(cfg_bytes),"master_model_sha256":MODEL_SHA,
          "expectedFrames":n,"sourceFrame":[w,h],"sourceColumns":cols,
          "columns":chosen_cols,"frameWidth":sw,"frameHeight":sh,
          "rootX":round(rx*scale),"rootY":round(ry*scale),
          "pixelScale":scale,"packed":chosen,"master_raw_hashes":master_hashes,
          "atlas_sha256":sha(png.read_bytes())}
    (output_dir/(name+".json")).write_text(json.dumps(meta,indent=2)+"\n")
    print("GENERATED:",json.dumps({k:meta[k] for k in ("id","expectedFrames","columns","pixelScale","packed")}),flush=True)

def integrate(input_dir):
    from PIL import Image
    entries=all_pending()
    if len(entries)!=36: raise ValueError("Changed P03 inventory")
    report=[]
    for path in entries:
        name=path.stem
        cfgbytes=path.read_bytes();cfg=json.loads(cfgbytes)
        png=input_dir/(name+".png");mpath=input_dir/(name+".json")
        if not png.exists() or not mpath.exists(): raise ValueError("Missing validated artifact: "+name)
        meta=json.loads(mpath.read_text())
        if meta["id"]!=name or meta["source"]!=cfg["source"]:
            raise ValueError(name+": wrong artifact identity")
        if meta["config_sha256"]!=sha(cfgbytes) or meta["source_sha256"]!=sha((ROOT/cfg["source"]).read_bytes()):
            raise ValueError(name+": source or clip changed during batch processing")
        if meta["master_model_sha256"]!=MODEL_SHA or len(meta["master_raw_hashes"])!=cfg["expectedFrames"]:
            raise ValueError(name+": incomplete or wrong model")
        if sha(png.read_bytes())!=meta["atlas_sha256"]:
            raise ValueError(name+": staged source checksum mismatch")
        w,h,rx,ry,n,columns=geometry(cfg)
        density=meta["pixelScale"]
        if density not in (2.0,1.5) or meta["expectedFrames"]!=n:
            raise ValueError(name+": unexpected density or frame count")
        if tuple((meta["frameWidth"],meta["frameHeight"],meta["rootX"],meta["rootY"])) != tuple(round(v*density) for v in (w,h,rx,ry)):
            raise ValueError(name+": scaled root/frame mismatch")
        newcols=meta["columns"];newwidth=meta["frameWidth"];newheight=meta["frameHeight"]
        source=Image.open(ROOT/cfg["source"]).convert("RGBA")
        improved=Image.open(png).convert("RGBA")
        if improved.size!=(newwidth*newcols,newheight*math.ceil(n/newcols)):
            raise ValueError(name+": output grid invalid")
        for i in range(n):
            original=source.crop((i%columns*w,i//columns*h,(i%columns+1)*w,(i//columns+1)*h))
            cell=improved.crop((i%newcols*newwidth,i//newcols*newheight,(i%newcols+1)*newwidth,(i//newcols+1)*newheight))
            if original.getchannel("A").resize(cell.size,Image.Resampling.LANCZOS).tobytes()!=cell.getchannel("A").tobytes():
                raise ValueError(f"{name} frame {i}: lost alpha or wrong order")
        newsource="art/sprites/source/"+name+"_sr_hd.png"
        shutil.copyfile(png,ROOT/newsource)
        cfg["sourceReferences"]=list(dict.fromkeys([*cfg.get("sourceReferences",[]),cfg["source"]]))
        cfg.update({"source":newsource,"frameWidth":newwidth,"frameHeight":newheight,
                    "rootX":meta["rootX"],"rootY":meta["rootY"],
                    "columns":newcols,"pixelScale":density})
        path.write_text(json.dumps(cfg,indent=2,ensure_ascii=False)+"\n")
        report.append({k:meta[k] for k in ("id","expectedFrames","pixelScale","columns","packed")})
        print("INTEGRATED:",name,n,density,flush=True)
    docs=ROOT/"docs/art"
    docs.mkdir(parents=True,exist_ok=True)
    (docs/"p03-all-sr-results.json").write_text(json.dumps({"totalAtlases":37,"alreadyApproved":sorted(SKIP),
          "generatedAtlases":len(report),"status":"batch generated and integrated into test branch; visual review required",
          "atlases":report},indent=2)+"\n")
    incremental_build(entries)
    print("P03 fast SR: 36/36 changed atlases repacked; global Java metadata regenerated; targeted validations PASS",flush=True)

def incremental_build(changed_entries):
    """Rebuild changed atlases only. Reuse existing validated reports for untouched clips.

    Avoids build_characters.py --write + --check and the 54 tests that copy
    the entire 200+MB art tree every time. All character/move frame metadata
    still passes the real compile_packs validation.
    """
    import build_characters as pipeline
    import import_sprites as importer
    from PIL import Image
    changed = {p.stem for p in changed_entries}
    allpaths = sorted(CLIPS.glob("*.json"))
    profile_dir=ROOT/"tools/sprites/profiles"
    prior=(importer.ROOT,importer.PROFILES_DIR,importer.GENERATED_JAVA)
    importer.ROOT=ROOT
    importer.PROFILES_DIR=profile_dir
    importer.GENERATED_JAVA=ROOT/pipeline.JAVA/"GeneratedSpriteLayouts.java"
    results=[]
    try:
        for path in allpaths:
            cfg=json.loads(path.read_text())
            if path.stem in changed:
                cfg, rep=importer.process_clip(path)
                pipeline.pack_atlas(ROOT,cfg,rep)
                sources=cfg.get("sourceReferences",[])
                rep["provenance"]={
                    "pipelineVersion":2,
                    "sha256":{
                        "source":sha((ROOT/cfg["source"]).read_bytes()),
                        "config":sha(path.read_bytes()),
                        "profile":sha((profile_dir/cfg["profile"]).read_bytes())
                    }
                }
                if sources:
                    rep["provenance"]["referenceSources"]={
                        original:sha((ROOT/original).read_bytes()) for original in sources}
                (ROOT/cfg["report"]).write_text(json.dumps(rep,indent=2)+"\n")
                print("REPACKED:",cfg["id"],rep["packed"]["decodedBytes"]["packed"],flush=True)
            else:
                rep=json.loads((ROOT/cfg["report"]).read_text())
            if rep["id"]!=cfg["id"]:
                raise ValueError("Wrong report assigned to clip "+cfg["id"])
            results.append((cfg,rep))
        importer.write_generated_java(results)
        pipeline.compile_packs(ROOT,results)
        for cfg,rep in results:
            if cfg["id"] not in changed:
                continue
            pic=ROOT/cfg["output"]
            with Image.open(pic) as packed_image:
                grid=rep["packed"]
                if packed_image.size!=(
                    grid["frameWidth"]*grid["columns"],
                    grid["frameHeight"]*math.ceil(grid["frameCount"]/grid["columns"])):
                    raise ValueError("Packed geometry is incorrect for "+cfg["id"])
                if max(packed_image.size)>MAX_SIZE:
                    raise ValueError("Texture larger than 4096 for "+cfg["id"])
            if grid["decodedBytes"]["packed"]>=MAX_DECODED:
                raise ValueError("Packed atlas exceeds 64MiB: "+cfg["id"])
            if grid.get("pixelScale")!=cfg["pixelScale"]:
                raise ValueError("Runtime density mismatch: "+cfg["id"])
            if rep["provenance"]["sha256"]["source"]!=sha((ROOT/cfg["source"]).read_bytes()):
                raise ValueError("Source hash mismatch: "+cfg["id"])
            if len(rep["frames"])!=cfg["expectedFrames"] or not rep["passed"]:
                raise ValueError("Unvalidated frame or incomplete report: "+cfg["id"])
        if len(changed)!=36:
            raise ValueError("Cannot partially commit P03 batch")
    finally:
        importer.ROOT,importer.PROFILES_DIR,importer.GENERATED_JAVA=prior


def main():
    a=argparse.ArgumentParser()
    p=a.add_subparsers(dest="command",required=True)
    p.add_parser("plan")
    r=p.add_parser("render")
    r.add_argument("--clip",required=True)
    r.add_argument("--weights",type=Path,required=True)
    r.add_argument("--output-dir",type=Path,required=True)
    i=p.add_parser("integrate")
    i.add_argument("--input-dir",type=Path,required=True)
    args=a.parse_args()
    if args.command=="plan":plan()
    elif args.command=="render":render(args.clip,args.weights,args.output_dir)
    else:integrate(args.input_dir)

if __name__=="__main__": main()
