#!/usr/bin/env python3
"""Análise conservadora de um único personagem: vídeo original -> arte -> jogo.

SEM modificar PNG, sem corrigir por bounding box, sem aprovar clipes sem evidência.
As métricas de cabeça/tronco são aproximações; nunca fazem resize automático.
"""
import re
import unicodedata
from pathlib import Path

import numpy as np

# Mapeamentos usados pelos autores no Drive: nomes diferentes, mesma intenção.
ALIASES = {
    "IDLE": ("idle", "parado"),
    "WALK_FORWARD": ("frente", "walkfward", "walkforward", "andarfrente"),
    "WALK_BACK": ("tras", "walkback", "andartras"),
    "DASH": ("dash",),
    "BACKDASH": ("backdash", "backdash"),
    "CROUCH": ("agachado", "agachar", "crounch", "crouch"),
    "JUMP": ("jump", "pulo", "saltar"),
    "FALL": ("jump", "pulo"),  # descida pode estar no mesmo vídeo
    "LAND": ("jump", "pulo"),
    "LIGHT_JAB": ("l", "ljab"),
    "MEDIUM_KICK": ("m", "msoco", "msocomedio", "mmediokick"),
    "HEAVY_STRAIGHT": ("h", "hchutealto"),
    "CROUCH_LIGHT": ("2l", "l2", "l2jab"),
    "CROUCH_MEDIUM": ("2m", "m2", "m2rasteira"),
    "CROUCH_HEAVY": ("2h", "upper", "uppercut"),
    "JUMP_LIGHT": ("jl",),
    "JUMP_MEDIUM": ("jm",),
    "JUMP_HEAVY": ("jh",),
    "JUMP_HEAVY_DOWN": ("jhbaixo", "j2h"),
    "THROW_GRAB": ("agarrao", "agarrar"),
    "VICTORY": ("vitoria", "victory"),
    "DEFEAT": ("derrota", "defeat"),
    "INTRO": ("intro",),
    "TAUNT": ("provocacao", "taunt"),
    "KNOCKDOWN": ("derrubalevanta", "derrubado", "knockdown"),
    "GETUP": ("derrubalevanta", "levanta"),
    "DEFENSE_STAND": ("defendecima", "defesaempe", "defesacima"),
    "DEFENSE_CROUCH": ("defendebaixo", "defesaagachado", "defesabaixo"),
    "DEFENSE_AIR": ("defesapulo", "defesanoar"),
    "HIT_STAND": ("danocima", "levargolpeempe"),
    "HIT_CROUCH": ("danobaixo", "levargolpeagachada"),
    "HIT_AIR": ("danopulo", "levargolpenoar"),
    "SPECIAL_ENERGY": ("s1", "especial1"),
    "SPECIAL_S2": ("s2",),
    "SPECIAL_S3": ("s3",),
    "SPECIAL_S4": ("s4",),
    "SUPER_WAVE": ("super",),
    "ULTRA_BEAM": ("ultra",),
}

# Movimento de cena e troca de pé não são tremor. Somente repousos são
# candidatos a "pé plantado"; a estabilidade anatômica é avaliada separadamente.
PLANTED = {"IDLE", "COMBAT", "VICTORY", "DEFENSE_STAND", "DEFENSE_CROUCH"}
MOVING = {"WALK_FORWARD", "WALK_BACK", "DASH", "BACKDASH", "JUMP",
          "FALL", "LAND", "INTRO", "KNOCKDOWN", "GETUP", "DEFEAT"}


def normalized(name):
    ascii_name = unicodedata.normalize("NFKD", name).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]", "", ascii_name.lower())


def match_videos(folder, states):
    """Devolve matches determinísticos e conflitos explícitos, nunca um chute."""
    folder = Path(folder)
    candidates = sorted(folder.glob("*.mp4")) if folder.is_dir() else []
    index = {}
    for f in candidates:
        index.setdefault(normalized(f.stem), []).append(f)
    out = {}
    for state in states:
        aliases = (normalized(state),) + ALIASES.get(state, ())
        valid = []
        for alias in dict.fromkeys(aliases):
            valid += index.get(alias, [])
        valid = list(dict.fromkeys(valid))
        out[state] = {
            "status": "ENCONTRADO" if len(valid) == 1 else
                      ("AMBIGUO" if valid else "SEM_VIDEO"),
            "files": [str(f) for f in valid],
        }
    return out


def properties(alpha):
    """Proxies observáveis por silhueta; não são detecção anatômica infalível."""
    a = np.asarray(alpha) > 128
    ys, xs = np.nonzero(a)
    if len(xs) < 75:
        return None
    x0, x1 = int(xs.min()), int(xs.max())
    y0, y1 = int(ys.min()), int(ys.max())
    h = y1-y0+1
    if h < 18:
        return None

    def band(lo, hi):
        left, right = y0+int(h*lo), y0+int(h*hi)
        yy, xx = np.nonzero(a[left:max(left+1,right)])
        if len(xx) < 10:
            return None
        return (float(np.quantile(xx, .90)-np.quantile(xx, .10)),
                float(np.median(xx)))

    head = band(.08, .27)
    torso = band(.36, .58)
    feet = band(.90, 1.00)
    if not head or not torso or not feet:
        return None
    return {"head_proxy": head[0], "torso_proxy": torso[0],
            "torso_x": torso[1], "shoe_x": feet[1],
            "height": float(h), "shoe_y":float(y1),
            "body_y":float((y0+y1)*.5), "confidence": "PROXY_SILHUETA"}


def isolated_spikes(values, ref_height, fraction=.015):
    """Outliers de um quadro em trajetória temporal, não velocidade natural.

    Mudança real contínua é mantida; alternância 0,8,0 é sinalizada.
    Usa mediana de vizinhos em janela 5, e reversão de direção local.
    """
    x = np.asarray(values,dtype=float)
    found = []
    if len(x) < 5:
        return found
    limit = max(1.5, float(ref_height) * fraction)
    for i in range(2,len(x)-2):
        local = np.median(x[i-2:i+3])
        prev_delta = x[i]-x[i-1]
        next_delta = x[i+1]-x[i]
        if abs(x[i]-local) > limit and prev_delta*next_delta < 0 and                 min(abs(prev_delta),abs(next_delta)) >= limit:
            found.append(i)
    return found


def metrics(measures, reference_height, state):
    """Sinaliza anomalias sem inventar continuidade onde quadros estão ausentes."""
    if not measures:
        return {"status":"INCONCLUSIVO","motivo":"sem quadros"}
    good = [(i,m) for i,m in enumerate(measures) if m is not None]
    invalid=[i for i,m in enumerate(measures) if m is None]
    if len(good) < 5 or len(good) < len(measures)*.75:
        return {"status":"INCONCLUSIVO",
                "motivo":"quadros ou silhuetas sem medida confiável",
                "quadros_invalidos":invalid}
    xs = [m["torso_x"] for _,m in good]
    heads = [m["head_proxy"] for _,m in good]
    heights = [m["height"] for _,m in good]
    bodies = [m["torso_proxy"] for _,m in good]
    feet = [m["shoe_x"] for _,m in good]
    pairs = {k:v for k,v in (("torso",xs),("cabeca",heads),("tronco",bodies))}
    # In the previous implementation, invalid frames were silently removed
    # and both sides joined as adjacent time positions. That fabricates motion
    # and could miss glitches. Only compare five consecutive original frames.
    def contiguous_spikes(field):
        detected=[]
        for idx in range(2,len(measures)-2):
            window=measures[idx-2:idx+3]
            if any(m is None for m in window):
                continue
            x=[m[field] for m in window]
            if isolated_spikes(x,reference_height):
                detected.append(idx)
        return detected

    defects={"torso":contiguous_spikes("torso_x"),
             "cabeca":contiguous_spikes("head_proxy"),
             "tronco":contiguous_spikes("torso_proxy")}
    # Plantar pé não significa impedir animação dos membros: somente reportar
    # oscilações, não aplicar deslocamento automático.
    drift=None
    if state in PLANTED:
        defects["pe_apoio"] = contiguous_spikes("shoe_x")
        # A continuous left-right wobble is different from an isolated spike.
        # A perfect alternating sequence can evade a five-frame median.
        # Only flag this in grounded, nominally stationary states.
        for idx in range(2,len(measures)-1):
            a,b,c=measures[idx-1:idx+2]
            if a is None or b is None or c is None:
                continue
            limit=max(1.5,reference_height*.015)
            d1=b["shoe_x"]-a["shoe_x"]
            d2=c["shoe_x"]-b["shoe_x"]
            if d1*d2<0 and min(abs(d1),abs(d2))>=limit:
                defects["pe_apoio"].append(idx)
        defects["pe_apoio"]=sorted(set(defects["pe_apoio"]))
        # Victory sometimes has an authored entrance walk. Assess only the
        # *planted celebration* portion, never treat a real walk as skating.
        window = feet[max(0,int(len(feet)*.45)):] if state=="VICTORY" else feet
        if len(window)>=8:
            first=float(np.median(window[:max(2,len(window)//5)]))
            last=float(np.median(window[-max(2,len(window)//5):]))
            shift=last-first
            changes=np.diff(window)
            meaningful=changes[np.abs(changes)>=.2]
            agreement=(float(np.mean(np.sign(meaningful)==np.sign(shift)))
                       if len(meaningful) else 0)
            threshold=max(3.,float(reference_height)*.03)
            if abs(shift)>threshold and agreement>=.67:
                drift={"status":"SUSPEITA_DE_DESLIZE","delta_x":round(shift,2),
                       "limite":round(threshold,2),
                       "trecho":"apos entrada" if state=="VICTORY" else "completo"}
    return {"status":"MEDIDO" if not invalid else "MEDIDO_COM_LACUNAS",
            "quadros":len(good),"quadros_invalidos":invalid,
            "deslize":drift,
            "tremor": {k:v for k,v in defects.items() if v},
            "medianas": {"altura":round(float(np.median(heights)),2),
                         "cabeca":round(float(np.median(heads)),2),
                         "tronco":round(float(np.median(bodies)),2)},
            "limite_px":round(max(1.5,reference_height*.015),2)}


def compare_body(idle_stats, other_stats):
    """Retorna *suspeita* de escala, nunca fator para aplicar cegamente.

    Cabeça e tronco precisam concordar sobre o mesmo erro. Braço levantado,
    roupa, pose agachada, zoom e baixa confiabilidade não devem ser 'corrigidos'.
    """
    if idle_stats.get("status")!="MEDIDO" or other_stats.get("status")!="MEDIDO":
        return {"status":"INCONCLUSIVO"}
    a,b=idle_stats["medianas"],other_stats["medianas"]
    if min(a["cabeca"],a["tronco"],b["cabeca"],b["tronco"])<=0:
        return {"status":"INCONCLUSIVO"}
    h=b["cabeca"]/a["cabeca"]
    t=b["tronco"]/a["tronco"]
    if abs(h-t)>.12:
        return {"status":"INCONCLUSIVO","motivo":"cabeca e tronco discordam; pose ou angulo diferentes",
                "proporcoes":[round(h,3),round(t,3)]}
    ratio=(h+t)*.5
    if abs(ratio-1) < .09:
        return {"status":"COMPATIVEL","proporcao_aproximada":round(ratio,3)}
    return {"status":"SUSPEITA_DE_ESCALA","proporcao_aproximada":round(ratio,3),
            "observacao":"nao aplicar sem referencia de imagem-base e exame das poses"}


def load_video(path, max_frames=250):
    """Só vídeos com chroma-key reconhecido; jamais segmentar cenário arbitrário."""
    import cv2
    from video_para_sprite import key
    cap=cv2.VideoCapture(str(path))
    if not cap.isOpened():
        return {"status":"FALHA_VIDEO","motivo":"arquivo nao abriu"},[]
    fps=cap.get(cv2.CAP_PROP_FPS)
    declared=cap.get(cv2.CAP_PROP_FRAME_COUNT)
    samples=[]; frames_read=0; green=0
    try:
        while len(samples)<max_frames:
            ok,bgr=cap.read()
            if not ok:
                break
            frames_read+=1
            if bgr.shape[1]>900:
                scale=900/bgr.shape[1]
                bgr=cv2.resize(bgr,(900,int(bgr.shape[0]*scale)))
            hsv=cv2.cvtColor(bgr,cv2.COLOR_BGR2HSV)
            corners=np.concatenate((hsv[:35,:35].reshape(-1,3),
                                    hsv[:35,-35:].reshape(-1,3)))
            fraction=np.mean((corners[:,0]>35)&(corners[:,0]<95)&(corners[:,1]>45))
            if fraction < .65:
                return {"status":"INCONCLUSIVO","motivo":"fundo não é chroma-key verde uniforme; segmentação não validada"},[]
            green+=1
            rgba=key(bgr)
            samples.append(properties(rgba[...,3]))
    finally:
        cap.release()
    if not samples:
        return {"status":"FALHA_VIDEO","motivo":"sem quadros"},[]
    if declared>0 and declared>frames_read+1:
        return {"status":"INCONCLUSIVO","motivo":"vídeo não foi percorrido até o final","quadros_lidos":frames_read,
                "quadros_no_arquivo":int(declared)},[]
    return {"status":"EXTRAIDO","fps":round(float(fps),3),
            "quadros_lidos":frames_read,"quadros_validos":sum(x is not None for x in samples)},samples


def inspect_video(path, state, idle_height):
    extract,frames=load_video(path)
    if extract["status"]!="EXTRAIDO":
        return extract
    return {**extract,"analise":metrics(frames,idle_height,state)}


def suggested_actions(inspection):
    """Somente recomendações até validação independente, sem escrita."""
    actions=[]
    if inspection.get("analise",{}).get("tremor"):
        actions.append("REVISAR_TREMOR_COMPARANDO_VIDEO_E_SPRITE")
    if inspection.get("escala",{}).get("status")=="SUSPEITA_DE_ESCALA":
        actions.append("MEDIR_COM_IMAGEM_BASE_E_HEAD_MATCH")
    return actions


def load_atlas_state(root, pack, state, limit=100):
    """Inspeciona imagens realmente empacotadas pelo jogo, sem carregar o elenco."""
    import json
    from PIL import Image
    root=Path(root)
    anim=pack["animations"][state]
    atlas=anim["atlas"]
    report_file=root/"tools/sprites/reports"/(atlas+".report.json")
    image_file=root/"android/app/src/main/res/drawable-nodpi"/(atlas+".png")
    if not image_file.exists() or not report_file.exists():
        return {"status":"SEM_SPRITE_EMPACOTADO","atlas":atlas},[]
    report=json.loads(report_file.read_text(encoding="utf-8"))
    packed=report["packed"]
    width,height,columns=(int(packed[k]) for k in ("frameWidth","frameHeight","columns"))
    # Make both silhouette proxies and SIFT inspect the SAME physical size.
    # The old proxy analyzer compared raw pixels, while SIFT normalized
    # pixelScale. That could report conflicting results for a correct fighter.
    idle_atlas=pack.get("animations",{}).get("IDLE",{}).get("atlas")
    reference_report=root/"tools/sprites/reports"/(str(idle_atlas)+".report.json")
    if not reference_report.is_file():
        return {"status":"SEM_REFERENCIA_IDLE","atlas":atlas},[]
    idle_packed=json.loads(reference_report.read_text(encoding="utf-8"))["packed"]
    ps=float(packed.get("pixelScale",0))
    idle_ps=float(idle_packed.get("pixelScale",0))
    if ps<=0 or idle_ps<=0 or not np.isfinite(ps*idle_ps):
        return {"status":"PIXEL_SCALE_INVALIDO","atlas":atlas},[]
    factor=idle_ps/ps
    if not .25<=factor<=4.:
        return {"status":"PIXEL_SCALE_INVALIDO","atlas":atlas},[]
    indices=anim["frames"]
    if not indices or width<=0 or height<=0 or columns<=0:
        return {"status":"LAYOUT_DIVERGENTE","atlas":atlas},[]
    # Verify EVERY authored index before inspecting a subset of frames.
    # Sampling is a cost optimization, not a license to miss corrupted frames.
    if any(not isinstance(frame,int) or frame<0 or
           frame>=int(packed.get("frameCount",0)) for frame in indices):
        return {"status":"FRAME_FORA_DO_ATLAS","atlas":atlas},[]
    step=max(1,(len(indices)+limit-1)//limit)
    with Image.open(image_file) as atlas_img:
        expected_width=width*columns
        if atlas_img.width!=expected_width:
            return {"status":"LAYOUT_DIVERGENTE","atlas":atlas},[]
        total_rows=atlas_img.height//height
        frames_count=int(packed.get("frameCount",columns*total_rows))
        samples=[]
        for frame in indices[::step]:
            if not isinstance(frame,int) or frame<0 or frame>=frames_count:
                return {"status":"FRAME_FORA_DO_ATLAS","atlas":atlas,"frame":frame},[]
            x0=(frame%columns)*width
            y0=(frame//columns)*height
            if x0+width>atlas_img.width or y0+height>atlas_img.height:
                return {"status":"FRAME_FORA_DO_ATLAS","atlas":atlas,"frame":frame},[]
            cell=np.asarray(atlas_img.crop((x0,y0,x0+width,y0+height)).getchannel("A"))
            if abs(factor-1.)>.001:
                import cv2
                cell=cv2.resize(cell,None,fx=factor,fy=factor,
                                interpolation=(cv2.INTER_AREA if factor<1
                                               else cv2.INTER_LINEAR))
            samples.append(properties(cell))
    return {"status":"EXTRAIDO","atlas":atlas,
            "pixel_scale_normalizado":round(factor,4),
            "quadros":len(samples)},samples


def jump_phase(samples, state):
    """Se JUMP/FALL compartilham um vídeo, NÃO avaliar ambos como clipe inteiro.

    Detectar ápice pela subida/descida da posição do pé. Caso a câmera
    acompanhe o salto ou não exista deslocamento vertical confiável, abortar.
    """
    if state not in ("JUMP", "FALL", "LAND"):
        return samples
    valid=[(i,m["shoe_y"],m["height"]) for i,m in enumerate(samples)
           if m and "shoe_y" in m]
    if len(valid)<8:
        return None
    ys=np.array([z[1] for z in valid],float)
    # Mediana de 3 quadros evita falso ápice causado por tremor isolado.
    filtered=np.array([np.median(ys[max(0,i-1):min(len(ys),i+2)])
                       for i in range(len(ys))])
    apex=int(np.argmin(filtered))
    scale=float(np.median([x[2] for x in valid]))
    span=float(max(filtered[0],filtered[-1])-filtered[apex])
    if apex<max(2,int(len(ys)*.15)) or apex>min(len(ys)-3,int(len(ys)*.85))             or span<max(10.0,.16*scale):
        return None
    cut=valid[apex][0]
    if state=="JUMP":
        return samples[:cut+1]
    if state=="FALL":
        return samples[cut:]
    return samples[max(cut,len(samples)-max(5,len(samples)//5)):]
