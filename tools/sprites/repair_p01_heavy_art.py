#!/usr/bin/env python3
"""Verified artwork repairs in P01's H atlas, preserving all other frames.

H frame 20 has a semi-transparent puncture on the raised thigh: affected
pixels already have correct pant RGB but the alpha is 0..240 instead of opaque.
H frame 21 has heavily distorted/smeared legs: replace that single damaged
transition pose with the clean adjacent frame 22 until a redraw is available.
Idempotent. Refuses to touch unexpected atlas dimensions.
"""
from pathlib import Path
from PIL import Image,ImageDraw
import argparse

ROOT=Path(__file__).resolve().parents[2]
ATLAS=ROOT/"android/app/src/main/res/drawable-nodpi/player_base_heavy_straight.png"
W,H,COLS,FRAMES=570,485,7,23
PUNCTURE=[(301,241),(313,241),(329,249),(334,258),(325,270),(304,260)]

def cell_box(n):
    x=n%COLS*W;y=n//COLS*H
    return (x,y,x+W,y+H)

def repair(img):
    image=img.convert("RGBA")
    if image.size != (W*COLS,H*4):
        raise ValueError(f"Unexpected P01 H PNG dimensions: {image.size}")
    # Repair actual opacity of the fabric, not the wrong RGB inpainting.
    f20=image.crop(cell_box(20))
    alpha=f20.getchannel("A")
    original=[alpha.getpixel(p) for p in [(314,251),(307,257),(327,263)]]
    ImageDraw.Draw(alpha).polygon(PUNCTURE,fill=255)
    f20.putalpha(alpha)
    left,top,_,_=cell_box(20)
    image.paste(f20,(left,top))
    # This frame contains a badly broken lower-body silhouette. Frame 22
    # is the verified clean resting transition immediately after it.
    f22=image.crop(cell_box(22))
    left,top,_,_=cell_box(21)
    image.paste(f22,(left,top))
    assert all(f20.getchannel("A").getpixel(p)==255 for p in [(314,251),(307,257),(327,263)])
    assert image.crop(cell_box(21)).tobytes()==image.crop(cell_box(22)).tobytes()
    return image,original

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--apply",action="store_true")
    a=parser.parse_args()
    original=Image.open(ATLAS)
    result,alpha=repair(original)
    print("P01 H frame20 alpha before:",alpha,"after: [255,255,255]")
    print("P01 H frame21: replaced corrupted silhouette with clean frame22")
    if a.apply:
        result.save(ATLAS,optimize=True)
        print("P01 H atlas repaired and saved.")
if __name__=="__main__":main()
