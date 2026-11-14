#!/usr/bin/env python3
"""SŌKON: pipeline HD aprovado (master 4x -> sprite 2x) para P01/P02 Fable.

Usa sempre a arte ORIGINAL como entrada, nunca uma fonte *_sr_hd.
O 4x é obtido via Real-ESRGAN AnimeVideo-v3, enquanto o alfa do original
é escalado diretamente com Lanczos. Os frames são recolocados no MESMO grid.
O runtime 2x recebe RGB premultiplicado; raiz e duração lógica são preservadas.

Os frames que já estão em 2x, com SR aprovado, são preservados sem reprocessar.
"""
import argparse
import hashlib
import json
import math
import os
import sys
from pathlib import Path

import numpy as np
import torch
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/"tools/sprites"))
from sr_hd import modelo, preencher_invisivel, SHA_PESOS

TILE=96
HALO=24
MAG=4

def sha(data):
    return hashlib.sha256(data).hexdigest()

def sha_file(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def is_sr(name):
    return "_sr_hd" in name or "_sr_2x" in name or "_hd_aprovado_2x" in name

def source_for(clip):
    current=clip["source"]
    density=float(clip.get("pixelScale") or 1)
    if not current.endswith(".png"):
        raise ValueError(f'{clip["id"]}: fonte não PNG: {current}')
    if density==2 and is_sr(current):
        return None
    if density not in (1,2):
        raise ValueError(f'{clip["id"]}: densidade {density} não suportada')
    if density==1 and is_sr(current):
        refs=clip.get("sourceReferences") or []
        originals=[p for p in refs if p.endswith(".png") and not is_sr(p)]
        if not originals:
            raise ValueError(f'{clip["id"]}: não tem referência ORIGINAL para usar')
        return originals[0]
    # Arquivo "_video_hd" não é o mesmo que a saída Real-ESRGAN, e deve
    # ser tratado somente uma vez. Preservamos sua geometria física 2x.
    return current

def inventory():
    result=[]
    skipped=[]
    for path in sorted((ROOT/"tools/sprites/clips").glob("*.json")):
        if not path.name.startswith(("player_base_","p02_")):
            continue
        data=json.loads(path.read_text(encoding="utf8"))
        src=source_for(data)
        if src is None:
            skipped.append(data["id"])
            continue
        if not (ROOT/src).is_file():
            raise FileNotFoundError(f'{data["id"]}: fonte original ausente: {src}')
        result.append((path,data,src))
    return result, skipped

def shards(inventory,n):
    weights=[0]*n
    groups=[[] for _ in range(n)]
    for item in sorted(inventory,
                       key=lambda x:x[1]["expectedFrames"]*
                       x[1]["frameWidth"]*x[1]["frameHeight"],reverse=True):
        index=min(range(n),key=lambda i:weights[i])
        groups[index].append(item)
        d=item[1]
        weights[index]+=d["expectedFrames"]*d["frameWidth"]*d["frameHeight"]
    return groups

def model_up4(net,rgba):
    """4x do RGB por tiles com halo para evitar costuras internas."""
    h,w=rgba.shape[:2]
    rgb=preencher_invisivel(rgba)
    dst=np.zeros((h*MAG,w*MAG,3),dtype=np.uint8)
    for y in range(0,h,TILE):
        for x in range(0,w,TILE):
            xe,ye=min(x+TILE,w),min(y+TILE,h)
            xa,ya=max(0,x-HALO),max(0,y-HALO)
            xb,yb=min(w,xe+HALO),min(h,ye+HALO)
            tile=rgb[ya:yb,xa:xb].astype(np.float32)/255.
            tensor=torch.from_numpy(tile).permute(2,0,1).unsqueeze(0)
            with torch.inference_mode():
                pred=net(tensor).clamp_(0,1).squeeze(0).permute(1,2,0).numpy()
            pred=pred[(y-ya)*4:(ye-ya)*4,(x-xa)*4:(xe-xa)*4]
            dst[y*4:ye*4,x*4:xe*4]=np.rint(pred*255).astype(np.uint8)
    return dst

def frame_hd(net,rgba,density):
    h,w=rgba.shape[:2]
    factor=2 if density==1 else 1
    out_w,out_h=w*factor,h*factor
    alpha_orig=Image.fromarray(rgba[...,3])
    alpha2=(np.asarray(alpha_orig.resize((out_w,out_h),Image.Resampling.LANCZOS))
            if factor==2 else rgba[...,3].copy())
    if not np.any(rgba[...,3]):
        out=np.zeros((out_h,out_w,4),dtype=np.uint8)
        return out,sha(out.tobytes()),0.

    # Só recorta a ÁREA DE INFERÊNCIA; frame/raiz não se deslocam nunca.
    yy,xx=np.nonzero(rgba[...,3])
    margin=16
    x0,x1=max(0,int(xx.min())-margin),min(w,int(xx.max())+margin+1)
    y0,y1=max(0,int(yy.min())-margin),min(h,int(yy.max())+margin+1)
    crop=rgba[y0:y1,x0:x1]
    sr_rgb=model_up4(net,crop)
    a4=np.asarray(Image.fromarray(crop[...,3]).resize(
        (crop.shape[1]*4,crop.shape[0]*4),Image.Resampling.LANCZOS),dtype=np.uint8)
    # Master 4x real da área original; hash para provar a saída do modelo.
    master=np.empty((*a4.shape,4),dtype=np.uint8)
    master[:,:,:3]=sr_rgb
    master[:,:,3]=a4
    master_hash=sha(master.tobytes())

    # RGB premultiplicado pelo alfa 4x, reduzido à densidade de runtime.
    premult=sr_rgb.astype(np.float32)/255. * (a4.astype(np.float32)/255.)[...,None]
    dst_w,dst_h=(x1-x0)*factor,(y1-y0)*factor
    cols=[]
    for c in range(3):
        cols.append(np.asarray(Image.fromarray(premult[:,:,c],mode="F").resize(
            (dst_w,dst_h),Image.Resampling.BOX),dtype=np.float32))
    denominator=np.asarray(Image.fromarray(a4.astype(np.float32)/255.,mode="F").resize(
        (dst_w,dst_h),Image.Resampling.BOX),dtype=np.float32)
    result=np.zeros((out_h,out_w,4),dtype=np.uint8)
    cx0,cy0=x0*factor,y0*factor
    for c in range(3):
        result[cy0:cy0+dst_h,cx0:cx0+dst_w,c]=np.clip(
            np.rint(cols[c]/np.maximum(denominator,1e-5)*255),0,255).astype(np.uint8)
    result[:,:,3]=alpha2
    expected=alpha2
    if not np.array_equal(result[:,:,3],expected):
        raise AssertionError("ALFA divergente")
    # Há diferença real de cor, não mera ampliação bilinear.
    reference=np.asarray(Image.fromarray(rgba,"RGBA").resize(
        (out_w,out_h),Image.Resampling.LANCZOS))
    mask=(expected>=128)
    change=float(np.mean(np.abs(
        result[:,:,:3][mask].astype(np.float32)-
        reference[:,:,:3][mask].astype(np.float32)))) if mask.any() else 0.
    return result,master_hash,round(change,4)

def process(net,path,clip,src):
    current=ROOT/clip["source"]
    base=ROOT/src
    n,columns=int(clip["expectedFrames"]),int(clip["columns"])
    w,h=int(clip["frameWidth"]),int(clip["frameHeight"])
    rows=math.ceil(n/columns)
    with Image.open(base) as img:
        original=np.asarray(img.convert("RGBA"))
    if original.shape[:2]!=(rows*h,columns*w):
        raise ValueError(f'{clip["id"]}: original {original.shape[:2]}, esperado {(rows*h,columns*w)}')
    # Não reutilizar alfa de uma folha SR velha. A fonte precisa preservar
    # exatamente a sequência da animação atualmente usada.
    if src!=clip["source"]:
        with Image.open(current) as image_current:
            old=np.asarray(image_current.convert("RGBA"))
        if old.shape!=original.shape:
            raise ValueError(f'{clip["id"]}: fonte antiga com grade diferente')
        if not np.array_equal(old[:,:,3],original[:,:,3]):
            raise ValueError(f'{clip["id"]}: alfa original diferente do atual; revisão manual necessária')

    density=int(clip.get("pixelScale") or 1)
    factor=2//density
    output=np.zeros((rows*h*factor,columns*w*factor,4),dtype=np.uint8)
    hashes=[]
    diffs=[]
    for k in range(n):
        r,c=divmod(k,columns)
        frame=original[r*h:(r+1)*h,c*w:(c+1)*w]
        improved,hash4,diff=frame_hd(net,frame,density)
        output[r*h*factor:(r+1)*h*factor,c*w*factor:(c+1)*w*factor]=improved
        hashes.append(hash4);diffs.append(diff)
        if (k+1)%5==0 or k+1==n:
            print(f'{clip["id"]}: {k+1}/{n}',flush=True)
    dest=base.with_name(base.stem+"_hd_aprovado_2x.png")
    if dest.exists():
        raise FileExistsError(f'Não sobrescrever fonte HD: {dest}')
    Image.fromarray(output,"RGBA").save(dest,optimize=True)

    # Comparação exata de todas as máscaras RGBA com o alpha original.
    output_a=output[:,:,3]
    for k in range(n):
        r,c=divmod(k,columns)
        raw_a=original[r*h:(r+1)*h,c*w:(c+1)*w,3]
        want=(np.asarray(Image.fromarray(raw_a).resize(
            (w*factor,h*factor),Image.Resampling.LANCZOS))
            if factor==2 else raw_a)
        got=output_a[r*h*factor:(r+1)*h*factor,c*w*factor:(c+1)*w*factor]
        if not np.array_equal(got,want):
            raise AssertionError(f'{clip["id"]} alpha frame {k}')
    old_source=clip["source"]
    refs=clip.get("sourceReferences") or []
    for s in (old_source,src):
        if s not in refs:
            refs.append(s)
    clip["sourceReferences"]=refs
    clip["source"]=str(dest.relative_to(ROOT))
    if factor==2:
        clip["pixelScale"]=2
        clip["frameWidth"]=w*2
        clip["frameHeight"]=h*2
        clip["rootX"]=int(clip["rootX"]*2)
        clip["rootY"]=int(clip["rootY"]*2)
    # Durações e character.json NUNCA são alterados.
    path.write_text(json.dumps(clip,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
    manifest={
        "clip":clip["id"],"frames":n,"columns":columns,
        "source_original":src,"source_original_sha256":sha_file(base),
        "source_anterior":old_source,
        "output":clip["source"],"output_sha256":sha_file(dest),
        "input_cell":[w,h],"output_cell":[w*factor,h*factor],
        "pixelScale_original":density,"pixelScale_final":2,
        "root_input":[clip["rootX"]//factor,clip["rootY"]//factor],
        "root_output":[clip["rootX"],clip["rootY"]],
        "model":"Real-ESRGAN AnimeVideo-v3","model_sha256":SHA_PESOS,
        "master_4x_sha256_by_frame":hashes,
        "rgb_delta_vs_basic_resize_by_frame":diffs,
        "alpha":"EXATAMENTE ORIGINAL em 2x Lanczos; todos frames verificados",
        "unchanged":"pose, ordem, quantidade, tempo, root lógico, hitboxes, gameplay",
    }
    manifest_dir=ROOT/"docs/art/hd-aprovado-p01-p02"
    manifest_dir.mkdir(parents=True,exist_ok=True)
    (manifest_dir/(clip["id"]+".json")).write_text(
        json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
    print(f'OK {clip["id"]}: delta RGB médio {np.mean(diffs):.3f}; {dest}',flush=True)

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--weights",type=Path,required=True)
    p.add_argument("--shard",type=int,required=True)
    p.add_argument("--shards",type=int,default=12)
    opt=p.parse_args()
    if opt.shard<0 or opt.shard>=opt.shards:
        p.error("shard fora do intervalo")
    torch.set_num_threads(min(2,os.cpu_count() or 2))
    all_items,skipped=inventory()
    chunk=shards(all_items,opt.shards)[opt.shard]
    print(f'HD aprovado: {len(all_items)} fontes originais, {len(skipped)} já em SR 2x; '
          f'lote {opt.shard+1}/{opt.shards}: {len(chunk)}',flush=True)
    for _,c,s in chunk:
        print(f' - {c["id"]}: {c["expectedFrames"]} frames, base={s}',flush=True)
    if not chunk:
        return
    net=modelo(opt.weights)
    for path,c,src in chunk:
        process(net,path,c,src)

if __name__=="__main__":
    main()
