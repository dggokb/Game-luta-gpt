#!/usr/bin/env python3
"""Corretor REAL: publica transformacoes por quadro aplicadas no renderer Android.

Escala: requer prova geometrica VIDEO + ATLAS ambos fora do idle, coerentes.
Pe: apenas pose plantada (IDLE/VICTORY/DEFENSE), com comparacao antes/depois.
Sem escrita de PNG. Um personagem por execucao; alteracoes reversiveis.
"""
import argparse
import json
import math
import os
import sys
from pathlib import Path
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
ASSET=ROOT/"android/app/src/main/assets/sprite-autofix.json"
PLANTED={"VICTORY","IDLE","DEFENSE_STAND","DEFENSE_CROUCH"}


def character_pack(cid):
    if cid not in (f"p{i:02d}" for i in range(1,13)):
        raise ValueError("Informe um único personagem p01..p12")
    return "player_base" if cid=="p01" else cid


def correct_scale(proof,baseline_scale=1.,idle_scale=1.):
    # A sprite can already be resized by the game's renderer. Correct the
    # FINAL body size, not the raw atlas measured by SIFT. Without this P01's
    # built-in calibration could be mistakenly resized a second time.
    if proof.get("status") not in ("ESCALA_FORA_DO_IDLE",
                                   "PADRONIZADA_FONTE_ATLAS"):
        return {"status":"INCONCLUSIVO","reason":"sem prova geometrica suficiente"}
    video=float(proof.get("fator_video",0))
    atlas=float(proof.get("fator_atlas",0))
    if not all(math.isfinite(x) and x>0 for x in (video,atlas)):
        return {"status":"BLOQUEADO","reason":"fator invalido"}
    if abs(video-atlas)/max(video,atlas)>.06:
        return {"status":"BLOQUEADO","reason":"video diverge do atlas"}
    if not all(math.isfinite(v) and v>0 for v in (baseline_scale,idle_scale)):
        return {"status":"BLOQUEADO","reason":"renderizar base com escala invalida"}
    rendered=atlas*baseline_scale/idle_scale
    scale=1/rendered
    if not .8<=scale<=1.25:
        return {"status":"BLOQUEADO","reason":"escala acima do limite seguro"}
    before=abs(1-rendered)
    after=abs(1-rendered*round(scale,5))
    if before<.08 or after>.015 or after>before*.15:
        return {"status":"REPROVADO","reason":"simulacao nao melhora tamanho"}
    return {"status":"CORRIGIDO","scale":round(scale,5),
            "erro_antes":round(before,5),"erro_depois":round(after,5)}


def correct_shoes(positions,start=0):
    """Validar a TRILHA FINAL da arte já calibrada, nao a bbox original."""
    data=np.asarray(positions,dtype=float)
    if data.ndim!=1 or len(data)<8 or start<0 or len(data)-start<5 or not np.isfinite(data).all():
        return {"status":"INCONCLUSIVO","reason":"sem pes rastreaveis"}
    original=data[start:]
    target=float(np.median(original[:min(3,len(original))]))
    excursion=float(np.ptp(original))
    if excursion<1.6:
        return {"status":"SEM_CORRECAO","deslize_px":round(excursion,3)}
    if excursion>32:
        return {"status":"BLOQUEADO","reason":"passo intencional ou deslize extremo"}
    dx=np.round(target-original,4)
    after=original+dx
    before_error=float(np.sqrt(np.mean((original-target)**2)))
    after_error=float(np.sqrt(np.mean((after-target)**2)))
    improvement=1-after_error/max(before_error,1e-6)
    if improvement<.85 or float(np.ptp(after))>.25:
        return {"status":"REPROVADO","reason":"estabilidade piorou ou nao melhorou"}
    return {"status":"CORRIGIDO","start":start,
            "dx":[float(x) for x in dx],
            "antes_rms":round(before_error,4),"depois_rms":round(after_error,4),
            "antes_amplitude":round(excursion,4),
            "depois_amplitude":round(float(np.ptp(after)),4)}


def art_track(root,atlas,ids,transforms,world_scale=1.):
    root=Path(root)
    report=root/"tools/sprites/reports"/(atlas+".report.json")
    png=root/"android/app/src/main/res/drawable-nodpi"/(atlas+".png")
    if not report.exists() or not png.exists():
        raise ValueError("Sprite real ou report ausente")
    meta=json.loads(report.read_text(encoding="utf8"))["packed"]
    w,h,cols=(int(meta[k]) for k in ("frameWidth","frameHeight","columns"))
    r_x=float(meta["rootX"])
    to_atlas=float(meta["pixelScale"])/world_scale
    if not np.isfinite(to_atlas) or to_atlas<=0:
        raise ValueError("Invalid world-to-atlas conversion")
    if len(transforms)!=len(ids) or any(f<0 or f>=int(meta["frameCount"]) for f in ids):
        raise ValueError("Quadros fora do atlas ou baseline incompleta")
    result=[]
    with Image.open(png) as sheet:
        if sheet.width!=cols*w or sheet.height%h:
            raise ValueError("PNG com layout incorreto")
        for frame,affine in zip(ids,transforms):
            x=(frame%cols)*w;y=(frame//cols)*h
            cell=np.asarray(sheet.crop((x,y,x+w,y+h)).getchannel("A"))
            yy,xx=np.nonzero(cell>90)
            if len(xx)<100:
                raise ValueError(f"Sem silhueta no quadro {frame}")
            floor=int(np.max(yy))
            shoe_x=float(np.median(xx[yy>=floor-max(8,int(h*.09))]))
            sx,sy,dx,dy=affine
            result.append((shoe_x-r_x)*sx+dx*to_atlas)
    return result


def base_calibration(root,atlas,ids):
    if atlas.startswith("player_base_"):
        sys.path.insert(0,str(Path(root)/"tools/sprites"))
        import generate_p01_calibration
        transforms,*_=generate_p01_calibration.compute()
        if atlas not in transforms:
            raise ValueError("P01 sem calibracao base")
        return [transforms[atlas][i] for i in ids]
    return [(1.,1.,0.,0.) for _ in ids]


def validate_pixels_after(root,atlas,ids,baseline,rows,start,world_scale=1.):
    """SECOND validation: render transformed alpha pixels, then remeasure feet.

    Never approve on the same algebra used to calculate corrections alone.
    This emulates the renderer's scale and translated foot root; no PNG write.
    """
    from PIL import Image
    import json
    root=Path(root)
    src=root/"android/app/src/main/res/drawable-nodpi"/(atlas+".png")
    meta=json.loads((root/"tools/sprites/reports"/(atlas+".report.json")).read_text())["packed"]
    w,h,cols=(int(meta[k]) for k in ("frameWidth","frameHeight","columns"))
    root_x=float(meta["rootX"])
    root_y=float(meta["rootY"])
    to_atlas=float(meta["pixelScale"])/world_scale
    corrections={int(row[0]):row for row in rows}
    def track(apply):
        shoes=[]
        with Image.open(src) as atlas_image:
            for i in range(start,len(ids)):
                frame=int(ids[i])
                sx,sy,bx,by=baseline[i]
                _,scale,dx,dy=corrections[frame]
                if not apply:
                    scale,dx,dy=1.,0.,0.
                scale_x=sx*scale
                scale_y=sy*scale
                cell=np.asarray(atlas_image.crop((
                    frame%cols*w,frame//cols*h,
                    (frame%cols+1)*w,(frame//cols+1)*h
                )).getchannel("A"))
                import cv2
                width=max(1,int(round(w*scale_x)))
                height=max(1,int(round(h*scale_y)))
                rendered=cv2.resize(cell,(width,height),interpolation=cv2.INTER_LINEAR)
                # Draw in a generous, COMMON world-space canvas, anchored
                # by authored root and existing user-calibrated offsets.
                cw=int(w*4)
                ch=int(h*3)
                x=int(round(w*2-root_x*scale_x+bx*to_atlas+dx))
                y=int(round(h*2-root_y*scale_y+by*to_atlas+dy))
                if x<0 or y<0 or x+width>cw or y+height>ch:
                    raise ValueError("frame leaves confirmation canvas")
                picture=np.zeros((ch,cw),dtype=np.uint8)
                picture[y:y+height,x:x+width]=rendered
                yy,xx=np.nonzero(picture>90)
                if len(xx)<80:
                    raise ValueError("image disappeared after transformation")
                foot_line=int(yy.max())
                shoes.append(float(np.median(xx[yy>=foot_line-max(8,int(h*.09))])))
        return np.asarray(shoes)
    original=track(False)
    corrected=track(True)
    before=float(np.ptp(original))
    after=float(np.ptp(corrected))
    if before<1.6 or after>max(1.6,before*.22):
        return {"status":"REPROVADO","reason":"independent pixel render did not stabilize",
                "antes_px":round(before,3),"depois_px":round(after,3)}
    return {"status":"CONFIRMADO_EM_PIXELS","antes_px":round(before,3),
            "depois_px":round(after,3),"melhoria":round(1-after/before,4)}


def propose(root,character,state,analysis):
    sys.path.insert(0,str(Path(root)/"tools/sprites"))
    import refinar_elenco
    cid=character_pack(character)
    state=refinar_elenco.canonical_state(state)
    manifest=Path(root)/"characters"/cid/"character.json"
    if not manifest.exists():
        raise ValueError("Personagem ainda nao importado no jogo")
    pack=json.loads(manifest.read_text(encoding="utf8"))
    animation=pack["animations"].get(state)
    if not animation:
        raise ValueError(f"Animacao ausente: {state}")
    atlas=animation["atlas"]
    ids=animation["frames"]
    if not ids:
        raise ValueError("Animacao sem quadros")
    source=analysis.get("estados",{}).get(state,{})
    video=source.get("video",{})
    # The SOURCE must exist and have measurable foreground. Never publish
    # an atlas-only guess when the author video is missing or ambiguous.
    if video.get("status")!="ENCONTRADO" or video.get("analise",{}).get("status")!="MEDIDO":
        return {"status":"SEM_AJUSTE_SEGURO","reason":"video original ausente, ambiguo ou nao mensuravel"},None
    proof=source.get("prova_geometrica",{})
    baseline=base_calibration(root,atlas,ids)
    profile=Path(root)/"tools/sprites/profiles"/(cid+".json")
    world_scale=(float(json.loads(profile.read_text(encoding="utf8"))["worldScale"])
                 if profile.exists() else 1.)
    report_path=Path(root)/"tools/sprites/reports"/(atlas+".report.json")
    atlas_meta=json.loads(report_path.read_text(encoding="utf8"))["packed"]
    to_atlas=float(atlas_meta["pixelScale"])/world_scale
    if world_scale<=0 or not np.isfinite(to_atlas) or to_atlas<=0:
        raise ValueError("Invalid atlas world scale")
    sx=[v[0] for v in baseline];sy=[v[1] for v in baseline]
    # Anisotropic poses require framewise anatomical evaluation; a uniform
    # factor cannot prove both dimensions correct if X/Y differ strongly.
    if max(abs(x-y) for x,y in zip(sx,sy))>.07:
        scale={"status":"INCONCLUSIVO","reason":"calibracao base nao uniforme"}
    else:
        idle_base=.98 if cid=="player_base" else 1.
        scale=correct_scale(proof,float(np.median(sx)),idle_base)
    shoe={"status":"INCONCLUSIVO","reason":"pose com deslocamento intencional"}
    if state in PLANTED:
        positions=art_track(root,atlas,ids,baseline,world_scale)
        start=7 if state=="VICTORY" and cid=="player_base" else 0
        # Existing framewise root shifts are world-space offsets, while new
        # corrections are atlas pixels. Scale only the bitmap-root component.
        scaled_positions=[
            (pos-v[2]*to_atlas)*scale.get("scale",1.)+v[2]*to_atlas
            for pos,v in zip(positions,baseline)]
        shoe=correct_shoes(scaled_positions,start)
    if scale["status"]!="CORRIGIDO" and shoe["status"]!="CORRIGIDO":
        return {"status":"SEM_AJUSTE_SEGURO","state":state,
                "tamanho":scale,"pes":shoe},None
    correction=scale.get("scale",1.)
    offsets=[0.]*len(ids)
    if shoe["status"]=="CORRIGIDO":
        # Foot offsets are already measured AFTER size correction and after
        # existing base offsets. Never multiply them by uniform scale again.
        offsets[shoe["start"]:]=shoe["dx"]
        # Renderer-equivalent post-fix position.
        next_positions=[scaled_positions[i]+offsets[i] for i in range(len(ids))]
        if np.ptp(next_positions[shoe["start"]:])>.25:
            return {"status":"REPROVADO","reason":"trilha final nao estabilizou"},None
    frames={}
    for i,index in enumerate(ids):
        row=[index,correction,offsets[i],0.]
        if index in frames and frames[index]!=row:
            return {"status":"BLOQUEADO","reason":"frame repetido exige ajustes diferentes"},None
        frames[index]=row
    record={"character":cid,"atlas":atlas,"state":state,"frames":list(frames.values()),
            "evidence":{"tamanho":scale,"pes":shoe}}
    image_proof={"status":"NAO_APLICAVEL"}
    if shoe["status"]=="CORRIGIDO":
        image_proof=validate_pixels_after(root,atlas,ids,baseline,
                                          record["frames"],shoe["start"],world_scale)
        if image_proof["status"]!="CONFIRMADO_EM_PIXELS":
            return {"status":"REPROVADO","reason":"render real divergiu da previsao",
                    "validacao":image_proof},None
    record["evidence"]["validacao_pixels"]=image_proof
    return {"status":"CORRIGIDO","state":state,"tamanho":scale,
            "pes":shoe,"validacao_pixels":image_proof,
            "quadros":len(frames)},record


def publish(asset,record):
    asset=Path(asset)
    doc=json.loads(asset.read_text(encoding="utf8")) if asset.exists() else {
        "schema":1,"corrections":[]}
    if doc.get("schema")!=1 or not isinstance(doc.get("corrections"),list):
        raise ValueError("Arquivo de correcoes invalido")
    if any(x.get("character")==record["character"] and
           x.get("atlas")==record["atlas"] for x in doc["corrections"]):
        raise ValueError("Ja existe correcao para esta animacao; reverta primeiro")
    previous=json.dumps(doc,indent=2,ensure_ascii=False)+"\n"
    asset.parent.mkdir(parents=True,exist_ok=True)
    backup=asset.with_suffix(".previous.json")
    backup.write_text(previous,encoding="utf8")
    doc["corrections"].append(record)
    tmp=asset.with_suffix(".tmp")
    tmp.write_text(json.dumps(doc,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
    os.replace(tmp,asset)
    return {"asset":str(asset),"backup":str(backup)}


def rollback(asset,expected_character=None):
    asset=Path(asset)
    backup=asset.with_suffix(".previous.json")
    if not backup.exists():
        raise ValueError("Nao existe snapshot para reverter")
    doc=json.loads(backup.read_text(encoding="utf8"))
    if doc.get("schema")!=1:
        raise ValueError("Snapshot invalido")
    current=json.loads(asset.read_text(encoding="utf8")) if asset.exists() else {}
    added=[x for x in current.get("corrections",[]) if x not in doc.get("corrections",[])]
    if expected_character is not None and (
        len(added)!=1 or added[0].get("character")!=expected_character):
        raise ValueError("Rollback pertence a outro personagem")
    tmp=asset.with_suffix(".tmp")
    tmp.write_text(json.dumps(doc,indent=2)+"\n",encoding="utf8")
    os.replace(tmp,asset)
    return {"status":"REVERTIDO","asset":str(asset)}


def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("personagem")
    parser.add_argument("--estado")
    parser.add_argument("--videos-dir",type=Path)
    parser.add_argument("--corrigir",action="store_true")
    parser.add_argument("--reverter",action="store_true")
    parser.add_argument("--asset",type=Path,default=ASSET)
    parser.add_argument("--saida",type=Path,default=ROOT/"android/app/build/sprite-review/correcao.json")
    args=parser.parse_args(argv)
    try:
        character_pack(args.personagem)
        if args.reverter:
            outcome=rollback(args.asset,character_pack(args.personagem))
        else:
            if not args.estado or args.videos_dir is None or not args.videos_dir.is_dir():
                parser.error("Para corrigir, informe --estado e --videos-dir")
            import refinar_elenco
            report=refinar_elenco.inspect_character(args.personagem,args.videos_dir,
                states=[args.estado],root=ROOT)
            outcome,record=propose(ROOT,args.personagem,args.estado,report)
            if record and args.corrigir:
                outcome["publicacao"]=publish(args.asset,record)
            elif record:
                outcome["previa"]=record
    except (ValueError,KeyError,OSError,IndexError) as exc:
        parser.error(str(exc))
    args.saida.parent.mkdir(parents=True,exist_ok=True)
    args.saida.write_text(json.dumps(outcome,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
    print(json.dumps(outcome,indent=2,ensure_ascii=False))
    return 0 if outcome["status"] in ("CORRIGIDO","REVERTIDO") else 2


if __name__=="__main__":
    raise SystemExit(main())
