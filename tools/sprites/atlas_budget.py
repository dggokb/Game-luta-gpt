#!/usr/bin/env python3
"""Measure exact atlas decoded-byte budgets per roster matchup, without decoding PNGs."""
import argparse
import json
import struct
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]


def dimensions(path):
    with path.open("rb") as f:
        h=f.read(24)
    if h[:8]!=b"\x89PNG\r\n\x1a\n" or h[12:16]!=b"IHDR":
        raise ValueError(f"Not PNG: {path}")
    return struct.unpack(">II",h[16:24])


def inventory(root=ROOT):
    clips={}
    for p in (root/"tools/sprites/clips").glob("*.json"):
        c=json.loads(p.read_text(encoding="utf-8"))
        file=root/c["output"]
        if file.exists():
            w,h=dimensions(file)
            clips[c["id"]]={"file":c["output"],"width":w,"height":h,
                            "decodedBytes":w*h*4,"compressedBytes":file.stat().st_size}
    characters={}
    for p in (root/"characters").glob("*/character.json"):
        c=json.loads(p.read_text(encoding="utf-8"))
        assets={a["atlas"] for a in c["animations"].values()}
        characters[c["id"]]=assets
    return clips,characters


def estimate(clips,characters,team):
    ids=set()
    for char in team:
        ids.update(characters[char])
    subset=[clips[x] for x in ids if x in clips]
    missing=sorted(ids - clips.keys())
    return {"team":list(team),"uniqueAtlases":len(subset),
            "decodedMiB":round(sum(c["decodedBytes"] for c in subset)/1048576,2),
            "compressedMiB":round(sum(c["compressedBytes"] for c in subset)/1048576,2),
            "missingAtlases":missing}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--team",nargs="*",help="IDs carregados simultaneamente")
    p.add_argument("--limit-mib",type=float,default=256.)
    p.add_argument("--output",default="android/app/build/atlas-budget.json")
    a=p.parse_args()
    clips,chars=inventory()
    roster=json.loads((ROOT/"characters/roster.json").read_text(encoding="utf-8"))
    team=a.team or list(dict.fromkeys(roster["team"]+[roster["opponentCharacter"]]))
    report=estimate(clips,chars,team)
    report["memoryBudgetMiB"]=a.limit_mib
    report["overBudget"]=report["decodedMiB"]>a.limit_mib
    out=ROOT/a.output;out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(report,indent=2))


if __name__=="__main__":
    main()
