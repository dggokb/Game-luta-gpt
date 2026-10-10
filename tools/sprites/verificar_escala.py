#!/usr/bin/env python3
"""Confirmação de escala por evidência GEOMÉTRICA, não só por bbox/silhueta.

Uma diferença de pose NÃO prova que o corpo mudou de tamanho.
Aprovar só com keypoints consistentes entre quadros, RANSAC não degenerado,
cobertura espacial de cabeça+tronco e uma transformação reversível.
Resultados incertos => INCONCLUSIVO. Nunca redimensiona arte.
"""
import math
import numpy as np


def _points(rgba):
    import cv2
    a=np.asarray(rgba)
    if a.ndim!=3 or a.shape[2]!=4:
        return None
    alpha=a[:,:,3]
    yy,xx=np.nonzero(alpha>90)
    if len(xx)<400:
        return None
    x0,x1=int(xx.min()),int(xx.max())
    y0,y1=int(yy.min()),int(yy.max())
    width,height=x1-x0+1,y1-y0+1
    if min(width,height)<35:
        return None
    mask=np.uint8(alpha>90)*255
    # Cabeça e tronco. Braço estendido/perna levantada fora desta região
    # NÃO devem definir a escala inteira de um lutador.
    mask[y0+int(.65*height):,:]=0
    gray=cv2.cvtColor(a[:,:,:3],cv2.COLOR_RGB2GRAY)
    gray=np.where(alpha>90,gray,128).astype(np.uint8)
    sift=cv2.SIFT_create(nfeatures=1500,contrastThreshold=.02)
    kp,desc=sift.detectAndCompute(gray,mask)
    if desc is None or len(kp)<24:
        return None
    return (kp,desc,(width,height))


def _one_direction(src,dst,min_inliers=22):
    import cv2
    a,b=_points(src),_points(dst)
    if a is None or b is None:
        return {"status":"INCONCLUSIVO","motivo":"sem detalhes suficientes no corpo"}
    ka,da,sz_a=a
    kb,db,sz_b=b
    matches=cv2.BFMatcher(cv2.NORM_L2).knnMatch(da,db,k=2)
    chosen=[m for m,n in matches if m.distance<.72*n.distance]
    if len(chosen)<min_inliers:
        return {"status":"INCONCLUSIVO","motivo":"poucas correspondencias",
                "matches":len(chosen)}
    points_a=np.float32([ka[m.queryIdx].pt for m in chosen]).reshape(-1,1,2)
    points_b=np.float32([kb[m.trainIdx].pt for m in chosen]).reshape(-1,1,2)
    matrix,matched=cv2.estimateAffinePartial2D(points_a,points_b,
        method=cv2.RANSAC,ransacReprojThreshold=3.5,
        maxIters=2000,confidence=.995)
    if matrix is None or matched is None:
        return {"status":"INCONCLUSIVO","motivo":"sem transformacao geometrica estavel"}
    good=np.asarray(matched[:,0],dtype=bool)
    count=int(good.sum())
    fraction=count/len(chosen)
    if count<min_inliers or fraction<.43:
        return {"status":"INCONCLUSIVO","motivo":"muitos matches contraditorios",
                "inliers":count,"matches":len(chosen)}
    used=points_a[good,0,:]
    horizontal=float(np.ptp(used[:,0]))
    vertical=float(np.ptp(used[:,1]))
    if horizontal<sz_a[0]*.15 or vertical<sz_a[1]*.16:
        return {"status":"INCONCLUSIVO","motivo":"correspondencias cobrem parte pequena do corpo",
                "inliers":count}
    scale=float(np.linalg.norm(matrix[:,0]))
    theta=math.degrees(math.atan2(matrix[1,0],matrix[0,0]))
    if not np.isfinite(scale) or scale<.4 or scale>2.5 or abs(theta)>28:
        return {"status":"INCONCLUSIVO","motivo":"zoom ou rotacao nao plausivel",
                "zoom":round(scale,4)}
    fitted=cv2.transform(points_a[good],matrix)[:,0,:]
    residual=float(np.median(np.linalg.norm(fitted-points_b[good,0,:],axis=1)))
    if residual>3.8:
        return {"status":"INCONCLUSIVO","motivo":"transformacao nao alinha os pontos",
                "erro_px":round(residual,3)}
    return {"status":"MEDICAO_GEOMETRICA","zoom":round(scale,5),
            "angulo_graus":round(theta,2),"inliers":count,
            "fracoes_corretas":round(fraction,3),
            "erro_px":round(residual,3)}


def estimate_pair(reference_rgba,candidate_rgba):
    """Rejeita escalas falsas por pose/roupa via ida+volta independentes."""
    forward=_one_direction(reference_rgba,candidate_rgba)
    if forward["status"]!="MEDICAO_GEOMETRICA":
        return forward
    backward=_one_direction(candidate_rgba,reference_rgba)
    if backward["status"]!="MEDICAO_GEOMETRICA":
        return {"status":"INCONCLUSIVO","motivo":"geometria reversa nao confirmada"}
    product=forward["zoom"]*backward["zoom"]
    if abs(product-1)>.035:
        return {"status":"INCONCLUSIVO","motivo":"zoom nao reversivel",
                "fator_ida_volta":round(product,4)}
    return {"status":"CONFIRMADO_GEOMETRIA","fator":forward["zoom"],
            "inliers_min":min(forward["inliers"],backward["inliers"]),
            "erro_px":max(forward["erro_px"],backward["erro_px"])}


def estimate_sequence(idle_rgba,frames,required=3):
    """Não marca todos os frames como corretos por poucos exemplos bons."""
    values=[estimate_pair(idle_rgba,f) for f in frames]
    valid=[v["fator"] for v in values if v["status"]=="CONFIRMADO_GEOMETRIA"]
    if len(valid)<required:
        return {"status":"INCONCLUSIVO",
                "motivo":"confirmacao de tamanho insuficiente em poses reais",
                "confirmados":len(valid),"testados":len(values),
                "medidas":values}
    median=float(np.median(valid))
    spread=float(np.max(np.abs(np.asarray(valid)-median))/median)
    if spread>.07:
        return {"status":"INSTAVEL_GEOMETRICAMENTE","fator_mediano":round(median,4),
                "variacao_relativa":round(spread,4),
                "confirmados":len(valid),"testados":len(values)}
    return {"status":"ESCALA_GEOMETRICA_ESTAVEL",
            "fator_mediano":round(median,4),
            "variacao_relativa":round(spread,4),
            "confirmados":len(valid),"testados":len(values)}


def read_video_samples(path,selection=(.16,.28,.40,.52,.64,.76,.88)):
    """Descompacta apenas alguns quadros, não mantém o vídeo na memória."""
    import cv2
    from video_para_sprite import key
    cap=cv2.VideoCapture(str(path))
    if not cap.isOpened():
        return []
    try:
        count=int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        if count<8:
            return []
        samples=[]
        for fraction in selection:
            cap.set(cv2.CAP_PROP_POS_FRAMES,max(0,min(count-1,int((count-1)*fraction))))
            ok,frame=cap.read()
            if not ok:
                continue
            samples.append(key(frame))
        return samples
    finally:
        cap.release()


def read_sprite_samples(root,pack,state,selection=(.16,.28,.40,.52,.64,.76,.88)):
    """Original do atlas empacotado, sem normalização da câmera/render."""
    from pathlib import Path
    from PIL import Image
    root=Path(root)
    if state not in pack.get("animations",{}):
        return []
    anim=pack["animations"][state]
    atlas=anim["atlas"]
    report_file=root/"tools/sprites/reports"/(atlas+".report.json")
    file=root/"android/app/src/main/res/drawable-nodpi"/(atlas+".png")
    if not (report_file.is_file() and file.is_file()):
        return []
    import json
    meta=json.loads(report_file.read_text(encoding="utf-8"))["packed"]
    w,h,cols=(int(meta[k]) for k in ("frameWidth","frameHeight","columns"))
    # Packed PNG resolution is not always the rendering resolution.
    # Normalize candidate RGBA into the IDLE atlas' pixelScale before
    # comparing detail sizes. Otherwise a 1x atlas appears 50% smaller
    # than a 2x atlas even when the actual fighter is the same size.
    idle_atlas=pack["animations"]["IDLE"]["atlas"]
    idle_report=root/"tools/sprites/reports"/(idle_atlas+".report.json")
    if not idle_report.is_file():
        return []
    idle_meta=json.loads(idle_report.read_text(encoding="utf-8"))["packed"]
    ps=float(meta.get("pixelScale",0))
    reference_ps=float(idle_meta.get("pixelScale",0))
    if ps<=0 or reference_ps<=0 or not np.isfinite(ps*reference_ps):
        return []
    normalization=reference_ps/ps
    if not .25<=normalization<=4.:
        return []
    indices=anim.get("frames",[])
    if not indices:
        return []
    with Image.open(file) as img:
        output=[]
        for ratio in selection:
            at=min(len(indices)-1,round((len(indices)-1)*ratio))
            f=int(indices[at])
            x=(f%cols)*w
            y=(f//cols)*h
            if x+w>img.width or y+h>img.height:
                return []
            raw=np.asarray(img.crop((x,y,x+w,y+h)).convert("RGBA"))
            if abs(normalization-1.)>.001:
                import cv2
                raw=cv2.resize(raw,None,fx=normalization,fy=normalization,
                    interpolation=(cv2.INTER_AREA if normalization<1
                                   else cv2.INTER_LINEAR))
            output.append(raw)
        return output


def compare_source_and_atlas(video,atlas,max_difference=.06):
    """Gate do tamanho relativo ao IDLE medido em domínios independentes.

    O jogo ainda exige conferência em runtime. Nunca transformar 'prova da
    fonte' em 'aprovação visual do jogo'.
    """
    if video.get("status")!="ESCALA_GEOMETRICA_ESTAVEL" or             atlas.get("status")!="ESCALA_GEOMETRICA_ESTAVEL":
        return {"status":"INCONCLUSIVO","video":video,"atlas":atlas}
    vs=float(video["fator_mediano"])
    ps=float(atlas["fator_mediano"])
    if min(vs,ps)<=0 or not np.isfinite(vs*ps):
        return {"status":"INCONCLUSIVO","motivo":"fatores invalidos"}
    difference=abs(vs-ps)/max(vs,ps)
    return {
        "status":"PADRONIZADA_FONTE_ATLAS" if difference<=max_difference
                 else "ESCALA_DIVERGENTE",
        "diferenca_relativa":round(difference,4),
        "fator_video":vs,"fator_atlas":ps,
        "validacao_runtime":"PENDENTE",
        "video":video,"atlas":atlas
    }
