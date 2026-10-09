#!/usr/bin/env python3
"""P01 real V2 trial on checked-in video-derived atlas PNGs.

Processes all p01 animation frames, creates detailed QA with 18 visual previews,
evaluates visual retiming on a copy of character JSON, and changes nothing.
The source Drive MP4s are not decoded in this CI trial.
"""
import argparse
import json
import sys
from pathlib import Path
from collections import Counter

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from video_quality import inspect, write_report
from sprite_auto_repair import retime_character_pack
from motion_harmony import assess

ROOT = Path(__file__).resolve().parents[2]
SHOW = set("IDLE WALK_FORWARD WALK_BACK DASH BACKDASH JUMP FALL LIGHT_JAB MEDIUM_KICK HEAVY_STRAIGHT CROUCH_LIGHT CROUCH_MEDIUM CROUCH_HEAVY JUMP_LIGHT JUMP_MEDIUM JUMP_HEAVY SPECIAL_ENERGY SUPER_WAVE".split())


def load_clips(root):
    return {d["id"]: d for path in (root/"tools/sprites/clips").glob("*.json")
            for d in [json.loads(path.read_text(encoding="utf-8"))]}


def atlas_frames(root, config, indices):
    report=json.loads((root/config["report"]).read_text(encoding="utf-8"))
    l=report.get("packed",report["layout"])
    w,h,n,cols=[l[x] for x in ("frameWidth","frameHeight","frameCount","columns")]
    if any(i<0 or i>=n for i in indices):
        raise ValueError("Frame outside atlas: "+config["id"])
    result=[]
    with Image.open(root/config["output"]) as image:
        if image.size != (w*cols,h*((n+cols-1)//cols)):
            raise ValueError("Invalid packed atlas dimensions: "+config["id"])
        for i in indices:
            x=(i%cols)*w
            y=(i//cols)*h
            result.append(np.array(image.crop((x,y,x+w,y+h)).convert("RGBA")))
    return result


def run(root=ROOT, out=None, character="player_base", visual_states=None):
    out=Path(out or root/"android/app/build/p01-motor-test")
    out.mkdir(parents=True,exist_ok=True)
    original=(root/"characters"/character/"character.json").read_bytes()
    pack=json.loads(original)
    clips=load_clips(root)
    changed, proposals=retime_character_pack(pack)
    harmony=assess(pack,clips)
    states={}
    problems=[]
    show=SHOW if visual_states is None else set(visual_states)
    for name, anim in pack["animations"].items():
        config=clips[anim["atlas"]]
        frames=atlas_frames(root,config,anim["frames"])
        durations=anim.get("durationsMs")
        fps=1000*len(durations)/sum(durations) if durations else 24.
        qa=inspect(frames,fps,loop=bool(anim["loop"]),target=12,
                   expected_impact=anim.get("impactFrame"))
        qa["state"]=name
        qa["sourceAtlas"]=config["output"]
        qa["sourceIsVideoDerived"]=True
        qa["originalMP4Inspected"]=False
        states[name]={k:v for k,v in qa.items() if k!="inspectedFrames"}
        for issue in qa["flags"]:
            if issue["severity"]!="info":
                problems.append(dict(state=name,**issue))
        if name in show:
            write_report(qa,frames,out/name.lower(),character="p01",clip=name)
    outcome={
        "character":character,
        "mode":"Dry-run, all shipped normalized sprite PNGs; no MP4 processing",
        "statesAudited":len(states),
        "framesAudited":sum(v["frameCount"] for v in states.values()),
        "flagsByType":dict(Counter(x["code"] for x in problems)),
        "issues":problems,
        "retimeProposals":proposals,
        "retimeSafeCount":sum(p["action"]=="adjust_visual_timing" for p in proposals),
        "retimeManualCount":sum(p["action"]=="needs_review" for p in proposals),
        "retimeUnchangedCount":sum(p["action"]=="no_change" for p in proposals),
        "motionHarmony":harmony,
        "perState":states,
        "gameplayUnchanged":pack["moves"]==changed["moves"] and pack.get("specialMoves")==changed.get("specialMoves"),
        "originalMP4Inspected":False,
        "imageChangesApplied":False,
        "timingChangesApplied":False,
    }
    (out/"summary.json").write_text(json.dumps(outcome,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    (out/"p01_visual_timing_preview.json").write_text(json.dumps(changed,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    rows="\n".join("| {} | {} | {} | {} |".format(p["binding"],p["state"],p["action"],p["reason"]) for p in proposals)
    md=("# Teste real do Motor Sprite V2 no P01\n\n"
        "Fonte: PNGs normalizados importados dos videos e usados no jogo; MP4s do Drive nao foram processados.\n"
        "Modo: analise e simulacao de tempo. Nenhuma mudanca aplicada.\n\n"
        "Animações: {}; quadros: {}; ocorrências suspeitas: {}.\n"
        "Retiming seguro proposto: {}; ajustes manuais: {}; sem mudancas: {}.\n\n"
        "| Golpe | Animacao | Acao | Motivo |\n|---|---|---|---|\n{}\n".format(
        len(states),outcome["framesAudited"],outcome["flagsByType"],
        outcome["retimeSafeCount"],outcome["retimeManualCount"],
        outcome["retimeUnchangedCount"],rows))
    (out/"REPORT.md").write_text(md,encoding="utf-8")
    assert pack["moves"]==changed["moves"],"Combat rules must remain identical"
    assert original==(root/"characters"/character/"character.json").read_bytes(),"Must not edit original p01"
    print("P01_RESULT "+json.dumps({k:outcome[k] for k in
          ("statesAudited","framesAudited","flagsByType","retimeSafeCount",
           "retimeManualCount","retimeUnchangedCount","gameplayUnchanged")}))
    print("OUTPUT "+str(out))
    return outcome


if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--character",default="player_base")
    parser.add_argument("--output",default="android/app/build/p01-motor-test")
    options=parser.parse_args()
    run(out=ROOT/options.output,character=options.character)
