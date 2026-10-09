#!/usr/bin/env python3
"""Conservative repairs for Seedance video sprites, without hallucinating new poses.

Two independent operations:
- suggest_replacements: swap a corrupted selected frame for a *real* nearby
  source-video frame only when the two surrounding poses agree and the new frame
  demonstrably improves registration, silhouette and anatomy.
- retime_visual: redistribute per-frame display milliseconds so the authored
  impact aligns with the center of the engine's active window. NEVER changes
  startup/active/recovery, physics, damage or the frame count.

Both return decisions for the preview report; callers explicitly opt into writing.
"""
import json
import math
from pathlib import Path
import cv2
import numpy as np

from video_quality import _features

MAX_RETIME_RATIO = 1.25
MIN_RETIME_RATIO = .80


def _cost(left, candidate, right):
    """The middle frame should interpolate the two surrounding poses."""
    if not left or not candidate or not right:
        return math.inf
    def deviation(attr):
        l,r,c=left[attr],right[attr],candidate[attr]
        scale=max(1.,abs((l+r)/2))
        return abs(c-(l+r)/2)/scale
    return (deviation("torso")*.65 + deviation("height")*.8 +
            deviation("head")*.6 + deviation("area")*.7)


def suggest_replacements(selected, keyed, get_keyed, source_count, radius=5,
                         protected=()):
    """Return (new source indices, decision log); no mutation of frames or files.

    Must not invent intermediate art, change selected count, reorder the impact
    or modify endpoints. The replacement is always from existing source video.
    """
    if len(selected)!=len(keyed):
        raise ValueError("source indices and keyed frames must align")
    indices=list(selected)
    protected=set(protected)
    feats=[_features(f) for f in keyed]
    edits=[]
    for i in range(1,len(indices)-1):
        if i in protected:
            continue
        left,mid,right=feats[i-1:i+2]
        if not all((left,mid,right)):
            continue
        body=max(1.,(left["height"]+right["height"])/2)
        # Neighbors must agree. Otherwise a rapidly moving / attacking pose is
        # intentional: do not mistake it for a temporal corruption.
        if abs(left["torso"]-right["torso"])>max(8,body*.12):
            continue
        if abs(left["head"]-right["head"])>max(3,body*.11):
            continue
        orig=_cost(left,mid,right)
        if orig<.30:
            continue
        low=max(0,min(indices[i-1],indices[i+1])+1,indices[i]-radius)
        high=min(source_count-1,max(indices[i-1],indices[i+1])-1,indices[i]+radius)
        if low>high:
            continue
        best=(orig,indices[i],mid)
        for src in range(low,high+1):
            if src==indices[i] or src in indices:
                continue
            alt=_features(get_keyed(src))
            score=_cost(left,alt,right)
            # Prefer a nearby real frame, not one from another phase.
            score+=.01*abs(src-indices[i])/max(1,radius)
            if score<best[0]:
                best=(score,src,alt)
        if best[1]!=indices[i] and best[0]<orig*.45 and best[0]<.25:
            edits.append({"selectedIndex":i,"sourceFrom":indices[i],"sourceTo":best[1],
                          "scoreBefore":round(orig,3),"scoreAfter":round(best[0],3),
                          "action":"use_verified_source_frame"})
            indices[i]=best[1]
            feats[i]=best[2]
    return indices,edits


def visual_impact_position(durations, impact):
    total=sum(durations)
    if not durations or total<=0 or not 0<=impact<len(durations):
        raise ValueError("invalid visual animation durations/impact")
    return (sum(durations[:impact])+durations[impact]*.5)/total


def retime_visual(durations, impact, startup, active, recovery,
                  max_ratio=MAX_RETIME_RATIO):
    """Return safe visual-only correction proposal, or a reason to request review.

    Preserve total duration and the count/order of frames; the engine still
    stretches the clip onto authoritative combat frames.
    """
    if not durations or any(type(d) not in (int,float) or not math.isfinite(d) or d<=0 for d in durations):
        return None,"invalid frame durations"
    if not isinstance(impact,int) or not 0<=impact<len(durations):
        return None,"impact frame missing or out of bounds"
    if min(startup,active,recovery)<0 or active==0 or startup+active+recovery<=0:
        return None,"invalid combat timing"
    duration=float(sum(durations))
    current=visual_impact_position(durations,impact)
    ideal=(startup+active/2)/(startup+active+recovery)
    if abs(current-ideal)<.008:
        return list(durations),"already synchronized"
    left_ratio=ideal/current if current>0 else math.inf
    right_ratio=(1-ideal)/(1-current) if current<1 else math.inf
    lower=max(MIN_RETIME_RATIO,1/max_ratio)
    if min(left_ratio,right_ratio)<lower or max(left_ratio,right_ratio)>max_ratio:
        return None,f"needs new poses / manual retime: relative factors {left_ratio:.2f} and {right_ratio:.2f}"
    # Piecewise-linear time warp, with impact frame split at its center.
    before=[float(d)*left_ratio for d in durations[:impact]]
    impact_length=float(durations[impact])*(left_ratio+right_ratio)/2
    after=[float(d)*right_ratio for d in durations[impact+1:]]
    continuous=before+[impact_length]+after
    if min(continuous)<1.0:
        return None,"unsafe sub-millisecond frame duration"
    target_sum=round(duration)
    integers=[int(math.floor(t)) for t in continuous]
    remain=target_sum-sum(integers)
    priorities=sorted(range(len(integers)),key=lambda i:continuous[i]-integers[i],reverse=True)
    for i in priorities[:max(0,remain)]:
        integers[i]+=1
    if sum(integers)!=target_sum or min(integers)<=0:
        return None,"could not preserve duration"
    return integers,"visual durations only; gameplay frames unchanged"


def retime_character_pack(pack,max_ratio=MAX_RETIME_RATIO):
    """Safely adjust each move animation once; shared animations must agree."""
    data=json.loads(json.dumps(pack))
    proposals=[]
    updates={}
    for group in ("moves","specialMoves"):
        for binding,move in data.get(group,{}).items():
            state=move.get("animation")
            if not state or state not in data["animations"]:
                continue
            anim=data["animations"][state]
            if "impactFrame" not in anim or not anim.get("durationsMs"):
                continue
            durations=anim["durationsMs"]
            replacement,reason=retime_visual(durations,anim["impactFrame"],
                move["startupFrames"],move["activeFrames"],move["recoveryFrames"],
                max_ratio=max_ratio)
            entry={"binding":binding,"state":state,"action":"adjust_visual_timing" if
                   replacement is not None and replacement!=durations else
                   ("no_change" if replacement is not None else "needs_review"),
                   "reason":reason,
                   "beforeMs":durations,
                   "afterMs":replacement}
            proposals.append(entry)
            if replacement is not None and replacement!=durations:
                if state in updates and updates[state]!=replacement:
                    entry["action"]="needs_review"
                    entry["reason"]="shared animation has conflicting timings"
                    updates.pop(state,None)
                else:
                    updates[state]=replacement
    for state,durations in updates.items():
        data["animations"][state]["durationsMs"]=durations
    return data,proposals


def art_requests(report,character="character",clip="animation"):
    """Human-readable AI art prompt proposals for defects no source frame can fix.

    Prompts are review instructions, not proof that an image generator can
    reconstruct the exact identity. No external model is silently invoked.
    """
    actions=[]
    kind={
        "empty_frame":("REGENERATE_POSE","Quadro vazio / membro desaparecido"),
        "clip_edge":("REGENERATE_WITH_SPACE","Personagem ou acessorio cortado"),
        "head_pop":("REDRAW_POSE","Cabeca mudou de tamanho ou formato"),
        "area_pop":("REVIEW_POSE","Possivel deformacao do corpo / efeito misturado"),
        "root_pop":("ADJUST_ALIGNMENT","Possivel deslocamento indevido"),
        "loop_seam":("REGENERATE_LOOP","Ponto de loop com salto visual"),
    }
    for issue in report.get("flags",[]):
        if issue["code"] not in kind:
            continue
        action,reason=kind[issue["code"]]
        msg=(f"Personagem {character}; animacao {clip}; quadros {issue['frames']}. "
             "Manter identicos o concept, rosto, roupas, cores, anatomia, escala, "
             "traco, pose anterior e posterior, sem nova camera, sem fundo diferente. "
             f"Corrigir: {reason}. Arte 2D de luta, fundo verde puro, corpo inteiro, "
             "sem texto, sombras no chao ou efeitos de ataque. "
             "Comparar com frames vizinhos antes de aprovar.")
        actions.append({"kind":action,"frames":issue["frames"],"reason":reason,
                        "prompt":msg,"automatic":action=="ADJUST_ALIGNMENT"})
    return actions


def save_art_requests(report,path,character="character",clip="animation"):
    tasks=art_requests(report,character,clip)
    dest=Path(path)
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text(json.dumps({"character":character,"clip":clip,"requests":tasks},
        indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    return tasks
