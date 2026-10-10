#!/usr/bin/env python3
"""Conservative repair of enclosed transparent artifacts in P01 heavy leg frames.

Preserves the original sprite wherever it is opaque. Open gaps or incorrectly
painted regions are reported as unverified rather than redrawn blindly.
"""
from pathlib import Path
import argparse
import numpy as np
from scipy import ndimage as ndi
from PIL import Image

ATLAS=Path(__file__).resolve().parents[2]/"android/app/src/main/res/drawable-nodpi/player_base_heavy_straight.png"
W,H,COLS,COUNT=570,485,7,23

def repair(image):
    arr=np.array(image.convert("RGBA"),dtype=np.uint8)
    if arr.shape != (H*4,W*COLS,4):
        raise ValueError(f"P01 heavy atlas dimensions unexpected: {arr.shape}")
    changed=[]
    for f in range(COUNT):
        row,col=divmod(f,COLS)
        cell=arr[row*H:(row+1)*H,col*W:(col+1)*W]
        solid=cell[:,:,3]>=24
        ys,xs=np.where(solid)
        if not len(ys):
            raise ValueError(f"Empty H frame: {f}")
        low=ys.min()+0.46*(ys.max()-ys.min())
        enclosed=ndi.binary_fill_holes(solid)&~solid
        labels,num=ndi.label(enclosed)
        patched=0
        # Strictly enclosed alpha holes; never bridge the actual space between legs.
        for n in range(1,num+1):
            hole=labels==n
            size=int(hole.sum())
            if not 5<=size<=1800:continue
            cy,cx=np.where(hole)
            if cy.min()<low or cy.max()>ys.max()-4:continue
            ring=ndi.binary_dilation(hole,iterations=2)&solid
            colors=cell[:,:,:3][ring]
            if len(colors)<8:continue
            index=ndi.distance_transform_edt(~solid,return_distances=False,return_indices=True)
            neighbor=cell[index[0][hole],index[1][hole],:3].astype(np.float32)
            median=np.median(colors.astype(np.float32),axis=0)
            cell[:,:,:3][hole]=np.clip(.8*neighbor+.2*median,0,255).astype(np.uint8)
            cell[:,:,3][hole]=255
            solid[hole]=True
            patched+=size
        changed.append(patched)
    return Image.fromarray(arr,"RGBA"),changed

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--apply",action="store_true")
    args=parser.parse_args()
    img,changed=repair(Image.open(ATLAS))
    print("P01 H enclosed holes:",[(f,c) for f,c in enumerate(changed) if c])
    print("P01 H repaired pixels:",sum(changed))
    if args.apply and sum(changed):
        img.save(ATLAS,optimize=True)
        print("Atlas updated")
    if not sum(changed):
        print("No enclosed hole detected; source art may require a precise frame-specific repair.")

if __name__=="__main__":main()
