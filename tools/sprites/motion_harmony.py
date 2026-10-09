#!/usr/bin/env python3
"""Audit video-sprite vs GAMEPLAY timing without mutating the combat engine.

For each configured character, map source visual impact to startup/active/recovery,
predict locomotion frame pace from the real CombatConfig walk/dash speeds, and
estimate decoded atlas bytes. Sources are authoritative for gameplay; this tool
recommends review, never modifies hitstun, damage or the physics.
"""
import argparse
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
FPS=60
SPEEDS={"WALK_FORWARD":300., "WALK_BACK":220., "DASH":620., "BACKDASH":760.}
OD_MULTIPLIER=1.25


def assess(pack, clips, speeds=None, fps=FPS):
    speeds=speeds or SPEEDS
    findings=[]
    animations={}
    for state,a in pack["animations"].items():
        frames=a["frames"]
        duration=sum(a.get("durationsMs",[]))/1000.
        pace=a.get("distancePerFrame")
        data={"frames":len(frames),"atlas":a["atlas"],"loop":a["loop"],
              "runtimeMode":"travel" if pace else "time"}
        if pace:
            speed=speeds.get(state)
            if speed is not None:
                base=speed/pace
                data["expectedFramesPerSecond"]=round(base,2)
                data["overdriveFramesPerSecond"]=round(base*OD_MULTIPLIER,2)
                data["distancePerFrame"]=pace
                if not 3 <= base <= 50:
                    findings.append(dict(level="review",state=state,code="step_cadence",
                                         message="Cadencia dos passos potencialmente estranha; revisar contato dos pes"))
        else:
            data["durationSeconds"]=round(duration,4)
        cfg=clips.get(a["atlas"],{})
        if "video" in cfg:
            data["sourceVideo"]=cfg["video"].get("arquivo")
            data["sourceFrameCount"]=cfg["expectedFrames"]
        animations[state]=data
    for bind,move in {**pack.get("moves",{}),**pack.get("specialMoves",{})}.items():
        state=move.get("animation")
        if not state or state not in pack["animations"]:
            continue
        a=pack["animations"][state]
        durations=a.get("durationsMs",[])
        impact=a.get("impactFrame")
        if impact is None or not durations:
            continue
        startup,active,recovery=(move[k] for k in ("startupFrames","activeFrames","recoveryFrames"))
        total=startup+active+recovery
        if total<=0:
            continue
        position=(sum(durations[:impact])+durations[impact]*.5)/sum(durations)
        mapped=position*total
        animations[state].setdefault("attackBindings",[]).append(
            {"button":bind,"visualImpactAtGameplayFrame":round(mapped,2),
             "activeWindow":[startup,startup+active],
             "startupFrames":startup,"activeFrames":active,"recoveryFrames":recovery})
        if mapped < startup-.5 or mapped > startup+active+.5:
            findings.append(dict(level="review",state=state,code="impact_timing",
                                 message=f"{bind}: impacto visual em {mapped:.1f}, janela ativa {startup}-{startup+active}"))
    return {"character":pack["id"],"animations":animations,"findings":findings,
            "note":"Velocidades sao a configuracao padrao; ajuste --speeds para configuracoes futuras."}


def audit(root=ROOT, char=None, speeds=None):
    clips={}
    for p in (root/"tools/sprites/clips").glob("*.json"):
        d=json.loads(p.read_text(encoding="utf-8"))
        clips[d["id"]]=d
    results=[]
    for p in sorted((root/"characters").glob("*/character.json")):
        pack=json.loads(p.read_text(encoding="utf-8"))
        if char and pack["id"]!=char:
            continue
        results.append(assess(pack,clips,speeds=speeds))
    if char and not results:
        raise ValueError(f"Personagem inexistente: {char}")
    return results


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--character",help="id do personagem; todos por padrao")
    parser.add_argument("--output",default="android/app/build/motion-harmony.json")
    parser.add_argument("--speeds",help="JSON {estado: unidades/segundo}")
    args=parser.parse_args()
    speeds=json.loads(args.speeds) if args.speeds else None
    results=audit(char=args.character,speeds=speeds)
    out=ROOT/args.output
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(results,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(f"{len(results)} personagens; {sum(len(x['findings']) for x in results)} observacoes -> {out}")


if __name__=="__main__":
    main()
