#!/usr/bin/env python3
"""Patch a verified torn/truncated P01 2H (CROUCH_HEAVY) pant frame.

Atlas frame 2 has a transparent rectangular tear at local x 229..275,
y 431..461. Reconstruct only a small fabric region from adjacent opaque
pixels in the SAME frame; preserve all other atlas frames and gameplay data.
The patch is idempotent and is checked before Android APK compilation.
"""
import argparse
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
ATLAS=ROOT/"android/app/src/main/res/drawable-nodpi/player_base_crouch_heavy.png"
W,H,COLS,COUNT=378,692,10,12

def repair(image):
    atlas=image.convert("RGBA")
    if atlas.size != (W*COLS,H*2):
        raise ValueError("Unexpected P01 2H atlas geometry: "+str(atlas.size))
    frame=atlas.crop((W*2,0,W*3,H))
    original=frame.copy()
    pixels=frame.load();before=original.load()
    if before[245,447][3]>=225 and before[257,452][3]>=225:
        print("P01 2H frame 2 already repaired; skip.")
        return atlas,False
    for y in range(431,462):
        edge=round(257+(y-430)*0.81)
        left=226
        rgb_left=before[left,y][:3]
        for x in range(229,min(edge+1,284)):
            t=(x-229)/max(1,edge-229)
            donor=before[min(280,x),min(465,y+25)][:3]
            c1=[rgb_left[i]*(1-0.38*t)+donor[i]*0.38*t for i in range(3)]
            col=tuple(round(c1[i]*(1-0.4*t)+(236,233,233)[i]*0.4*t) for i in range(3))
            alpha=255 if x<edge-1 else max(150,255-(x-edge+1)*65)
            pixels[x,y]=(*col,alpha)
        for x in range(edge+2,285):
            original_rgba=before[x,y]
            if 439<=y<=459 and (original_rgba[3]<230 or max(original_rgba[:3])<150):
                pixels[x,y]=(0,0,0,0)
    assert pixels[245,447][3]==255
    assert pixels[257,452][3]==255
    atlas.paste(frame,(2*W,0))
    print("P01 2H frame 2 fabric restored without changing neighboring frames.")
    return atlas,True

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--apply",action="store_true")
    args=p.parse_args()
    atlas=Image.open(ATLAS)
    output,changed=repair(atlas)
    if args.apply and changed:
        output.save(ATLAS,optimize=True)
        print("Saved "+str(ATLAS))
    elif not args.apply:
        print("Dry run; add --apply to write.")

if __name__=="__main__":
    main()
