#!/usr/bin/env python3
"""Verify P01 visual geometry with shipped atlas PNGs, not source note guesses.

Produces inspection sheets in android/app/build/sprite-review. No art edits.
"""
from pathlib import Path
from PIL import Image,ImageDraw
import json,re

ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/"android/app/src/main/java/com/gamelutagpt/SpriteFighterRenderer.java"
SPRITES=ROOT/"android/app/src/main/res/drawable-nodpi"
MANIFEST=ROOT/"characters/player_base/character.json"
PREVIEW=ROOT/"android/app/build/sprite-review"
CELLS={"idle":(331,476,10),"intro":(347,569,9),"crouch":(373,475,8),"rise":(370,460,6),"heavy_straight":(570,485,7)}

def calibration(key):
    # Confirm the actual previously-broken H thigh and silhouette pixels.
    h20=cell("heavy_straight",20)
    assert all(h20.getchannel("A").getpixel(p)==255 for p in [(314,251),(307,257),(327,263)]), "H20 still has a hole"
    assert cell("heavy_straight",21).tobytes()==cell("heavy_straight",22).tobytes(), "H21 still has corrupted legs"
    # Damaged frame 2 of the CROUCH_HEAVY atlas must be repaired before build.
    crouch_h=Image.open(SPRITES/"player_base_crouch_heavy.png").convert("RGBA")
    assert crouch_h.size == (3780,1384)
    assert crouch_h.getpixel((2*378+245,447))[3] >= 225, "2H frame 2 still has a torn pant"
    assert crouch_h.getpixel((2*378+257,452))[3] >= 225, "2H frame 2 right thigh still torn"
    java=JAVA.read_text()
    m=re.search(r"private static final float\[\]\s+P01_"+key+r"\s*=\s*\{([^}]+)\}",java,re.S)
    if not m:raise AssertionError("Missing renderer calibration "+key)
    return [float(x) for x in re.findall(r"(-?\d+\.\d+)f",m.group(1))]

def cell(name,f):
    w,h,cols=CELLS[name]
    img=Image.open(SPRITES/("player_base_"+name+".png")).convert("RGBA")
    x,y=f%cols*w,f//cols*h
    return img.crop((x,y,x+w,y+h))

def bb(img):
    return img.getchannel("A").point(lambda p:255 if p>50 else 0).getbbox()

def dims(img):
    x0,y0,x1,y1=bb(img)
    return (x1-x0,y1-y0)

def check_intro_handoff():
    # Source-pixel comparison of the last authored intro pose with the
    # slightly smaller actual idle. All coordinates are atlas-root relative.
    java=JAVA.read_text()
    def scalar(name):
        m=re.search(r"private static final float "+name+r"\\s*=\\s*(\\d+\\.\\d+)f;",java)
        assert m, "Missing P01 calibration scalar "+name
        return float(m.group(1))
    idle_scale=scalar("P01_IDLE_SCALE")
    victory_scale=scalar("P01_VICTORY_SCALE")
    assert 0.97 <= idle_scale <= .99
    assert .92 <= victory_scale <= .96
    ix,iy,idle_x,idle_y=161,551,165,462
    sx=calibration("INTRO_X")
    sy=calibration("INTRO_Y")
    dx=calibration("INTRO_OFFSET_X")
    dy=calibration("INTRO_OFFSET_Y")
    src=bb(cell("intro",26))
    idle=bb(cell("idle",0))
    received=((src[0]-ix)*sx[-1]+dx[-1],(src[2]-ix)*sx[-1]+dx[-1],
              (src[1]-iy)*sy[-1]+dy[-1],(src[3]-iy)*sy[-1]+dy[-1])
    target=((idle[0]-idle_x)*idle_scale,(idle[2]-idle_x)*idle_scale,
            (idle[1]-idle_y)*idle_scale,(idle[3]-idle_y)*idle_scale)
    for got,wanted in zip(received,target):
        assert abs(got-wanted)<2.5, (received,target)
    # The stationary victory pose should be roughly the same painted height
    # as the smaller opening pose of the intro (not the large victory atlas).
    victory=Image.open(SPRITES/"player_base_victory.png").convert("RGBA")
    assert victory.size==(326*11,577*3)
    victory7=victory.crop((326*7,0,326*8,577))
    vh=dims(victory7)[1]*victory_scale
    ih=dims(cell("intro",0))[1]*.965
    assert abs(vh-ih)<4.0, (vh,ih)
    # Old source victory frames 0-6 depict a walk-in, not a fixed celebration.
    view=(ROOT/"android/app/src/main/java/com/gamelutagpt/GameView.java").read_text()
    assert "win.timeOfFrame(7)" in view, "Victory still plays walking frames"
    print("Intro / scaled IDLE & stationary victory size verified.")


def preview():
    PREVIEW.mkdir(parents=True,exist_ok=True)
    idle=cell("idle",0)
    steps=[("Idle",idle,1,1),("Intro last source",cell("intro",26),1,1)]
    ix,iy=calibration("INTRO_X"),calibration("INTRO_Y")
    steps.append(("Intro aligned",cell("intro",26),ix[-1],iy[-1]))
    steps.append(("Intro final = Idle",idle,1,1))
    k=calibration("CROUCH")
    for f in [0,2,4,6,7]:steps.append((f"Crouch {f}",cell("crouch",f),k[f],k[f]))
    rx,ry=calibration("RISE_X"),calibration("RISE_Y")
    for f in [0,2,3,4]:steps.append((f"Rise {f}",cell("rise",f),rx[f],ry[f]))
    steps.append(("Rise final = Idle",idle,1,1))
    W,H=280,335
    canvas=Image.new("RGB",(W*7,H*2),(54,54,54));draw=ImageDraw.Draw(canvas)
    for i,(title,img,sx,sy) in enumerate(steps):
        alpha=img.getchannel("A");box=alpha.getbbox()
        piece=img.crop(box)
        piece=piece.resize((round(piece.width*sx*.78),round(piece.height*sy*.58)),Image.Resampling.LANCZOS)
        tile=Image.new("RGBA",piece.size,(56,56,56,255));tile.alpha_composite(piece)
        xx=i%7*W+(W-piece.width)//2;yy=i//7*H+H-15-piece.height
        canvas.paste(tile.convert("RGB"),(xx,yy))
        draw.text((i%7*W+8,i//7*H+5),title,fill=(255,255,255))
    canvas.save(PREVIEW/"p01-calibration.png")
    Hframes=23
    hw,hh=225,225
    sh=Image.new("RGB",(hw*6,(hh+25)*4),(56,56,56))
    d=ImageDraw.Draw(sh)
    for idx in range(Hframes):
        im=cell("heavy_straight",idx)
        bounds=bb(im);im=im.crop(bounds)
        scale=min((hw-10)/im.width,(hh-10)/im.height)
        im=im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.LANCZOS)
        bg=Image.new("RGBA",im.size,(56,56,56,255));bg.alpha_composite(im)
        x=idx%6*hw+(hw-im.width)//2;y=idx//6*(hh+25)+(hh-im.height)
        sh.paste(bg.convert("RGB"),(x,y))
        d.text((idx%6*hw+7,idx//6*(hh+25)+hh),f"H frame {idx}",fill=(255,255,255))
    sh.save(PREVIEW/"p01-heavy-frames.png")

def main():
    pack=json.loads(MANIFEST.read_text())
    check_intro_handoff()
    h=pack["moves"]["H"]
    assert (h["startupFrames"],h["activeFrames"],h["recoveryFrames"])==(10,5,37)
    assert sum([h["startupFrames"],h["activeFrames"],h["recoveryFrames"]])==52
    assert len(pack["animations"]["HEAVY_STRAIGHT"]["frames"])==23
    timings=pack["animations"]["HEAVY_STRAIGHT"]["durationsMs"]
    impact_frame=pack["animations"]["HEAVY_STRAIGHT"]["impactFrame"]
    impact_ratio=sum(timings[:impact_frame])/sum(timings)
    contact_ratio=h["startupFrames"]/(h["startupFrames"]+h["activeFrames"]+h["recoveryFrames"])
    assert abs(impact_ratio-contact_ratio)<0.02, (impact_ratio,contact_ratio)
    assert sum(pack["animations"]["RISE"]["durationsMs"])==150
    k=calibration("CROUCH")
    assert len(k)==8
    x=calibration("RISE_X");y=calibration("RISE_Y")
    assert len(x)==len(y)==6
    ix=calibration("INTRO_X");iy=calibration("INTRO_Y")
    assert len(ix)==len(iy)==7
    stand_w,stand_h=dims(cell("idle",0))
    widths_c=[dims(cell("crouch",i))[0]*s for i,s in enumerate(k)]
    widths_r=[dims(cell("rise",i))[0]*s for i,s in enumerate(x)]
    for widths,name in [(widths_c,"crouch"),(widths_r,"rise")]:
        for w in widths:
            assert abs(w-stand_w)<5, f"{name} width {w} differs from idle {stand_w}"
    intro_w,intro_h=dims(cell("intro",26))
    assert abs(intro_w*ix[-1]-stand_w)<5
    assert abs(intro_h*iy[-1]-stand_h)<5
    java=JAVA.read_text()
    assert "drawP01Idle(canvas,x,baseY" in java
    assert "clamp01((frame-22f)/4f)" in java
    assert "clamp01((i-3f)/2f)" in java
    print("P01 visual test PASS: idle width",stand_w,
        "crouch",[round(v) for v in widths_c],"rise",[round(v) for v in widths_r],
        "intro end",[round(intro_w*ix[-1]),round(intro_h*iy[-1])],
        "H 52 gameplay frames / H active at frame 10 / 23 artwork frames")
    preview()

if __name__=="__main__":main()
