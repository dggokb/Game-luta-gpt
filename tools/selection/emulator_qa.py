"""Exercise the actual APK using its real landscape dimensions, not fixed taps."""
import json
import subprocess as sp
import time
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image, ImageChops

def adb(*args):
    return sp.check_output(['adb', *args])

def capture(name):
    path = Path('selection-' + name + '.png')
    path.write_bytes(adb('exec-out', 'screencap', '-p'))
    return Image.open(path).convert('RGB')

adb('install', '-r', 'android/app/build/outputs/apk/debug/app-debug.apk')
adb('logcat', '-c')
adb('shell', 'settings', 'put', 'secure', 'immersive_mode_confirmations', 'confirmed')
adb('shell', 'am', 'start', '-W', '-n', 'com.gamelutagpt/com.gamelutagpt.MainActivity')
time.sleep(10)
# A fresh Android user may overlay its first-use fullscreen tutorial. Dismiss
# only an observed system button using its actual bounds before baseline QA.
adb('shell', 'uiautomator', 'dump', '/sdcard/selection-ui.xml')
ui=ET.fromstring(adb('shell', 'cat', '/sdcard/selection-ui.xml'))
for node in ui.iter('node'):
    if node.attrib.get('package') == 'com.android.systemui' and node.attrib.get('text','').upper() == 'GOT IT':
        x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']))
        adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
        time.sleep(1)
initial = capture('default')
w, h = initial.size
assert w > h, (w, h)
results = {'resolution': [w, h], 'checks': []}

def tap(x, y):
    adb('shell', 'input', 'tap', str(round(x*w/1280)), str(round(y*h/720)))
    time.sleep(.5)

def differs(a, b, region=None):
    if region:
        region = tuple(round(v*(w/1280 if i%2==0 else h/720)) for i,v in enumerate(region))
        a,b = a.crop(region), b.crop(region)
    return ImageChops.difference(a.convert('RGB'), b.convert('RGB')).getbbox() is not None

def cell(i):
    cw=(440-18)/4
    ch=(400-18)/4
    y=137+(i//4)*(ch+6)+ch/2
    x=420+(i%4)*(cw+6)+cw/2
    return 640+(x-640)*(440+148*(y-137)/400)/440,y

# Decorations animate, while portrait cores and selection state stay stationary.
def stable_team(a,b,side,slot):
    x=70 if side==0 else 1124
    y=398+slot*130
    return not differs(a,b,(x,y,x+95,y+64))

time.sleep(1)
animated=capture('animated')
assert differs(initial,animated), 'Selection effects are static'
assert all(stable_team(initial,animated,side,slot) for side in range(2) for slot in range(2)), 'Idle effects move portraits'
results['checks'].append('effects animate while portrait cores stay stationary')

# Locked touches must leave both teams unchanged despite ambient animation.
for i in range(8,16):
    tap(*cell(i))
    locked=capture('locked')
    assert all(stable_team(initial,locked,side,slot) for side in range(2) for slot in range(2)), 'Locked cell changed teams: '+str(i)
results['checks'].append('all eight locked cells ignore touches')

# P1 TAG: choose P02 (visual cell 3), then choose it as point, swapping duplicate.
tap(110,560)
focused=capture('tag-focused')
tap(*cell(3))
tag=capture('tag')
assert differs(focused,tag,(33,513,193,609)), 'TAG art did not change'
results['checks'].append('P1 TAG changes its portrait')
tap(110,420)
tap(*cell(3))
point=capture('p02')
assert differs(initial,point,(195,55,330,350)), 'P02 atlas hero missing'
results['checks'].append('P02 atlas point art rendered')

# Every selectable fighter renders a distinct large concept hero on the same side.
previous=point
for i, name in [(1,'p03'),(5,'p04'),(6,'p05'),(2,'p06'),(7,'p07'),(4,'p08')]:
    tap(110,420)
    tap(*cell(i))
    current=capture(name)
    assert differs(previous,current,(195,55,330,350)), name+' large hero did not change'
    previous=current
    results['checks'].append(name+' concept hero rendered')

# Focus and edit the opposing team; P1 must remain unchanged.
tap(1150,420)
tap(*cell(5))
p2=capture('p2')
assert differs(previous,p2,(950,55,1080,350)), 'P2 point did not change'
assert stable_team(previous,p2,0,0) and stable_team(previous,p2,0,1), 'P2 changed P1 team'
tap(1150,560)
tap(*cell(7))
p2tag=capture('p2-tag')
assert differs(p2,p2tag,(1087,513,1247,609)), 'P2 TAG did not change'
tap(1150,420)
before=capture('before-swap')
tap(914,700)
swapped=capture('swapped')
assert differs(before,swapped,(1087,383,1247,609)), 'Team swap did not change slots'
results['checks'].extend(['P2 point and TAG independent','focused team point/TAG swap'])

# Actual APK video: ambient energy, cursor glow, pick flash, then fight transition.
record=sp.Popen(['adb','shell','screenrecord','--size','1280x720','--bit-rate','3000000','--time-limit','16','/sdcard/selection-effects.mp4'])
time.sleep(1)
tap(110,420)
tap(*cell(3))
time.sleep(1)
tap(110,420)
tap(*cell(0))
time.sleep(1)
ready=capture('ready')
tap(370,700)
time.sleep(3)
fight=capture('confirmed')
assert differs(ready,fight,(440,150,485,205)) and differs(ready,fight,(550,150,595,205)), 'Confirmation did not leave stationary portrait cores'
assert adb('shell','pidof','com.gamelutagpt').strip(), 'App exited'
logs=adb('logcat','-d','-v','brief').decode(errors='replace')
Path('selection-logcat.txt').write_text(logs)
assert 'FATAL EXCEPTION' not in logs, 'Android crash'
assert 'Missing selection art: selection/art/hero_' not in logs, 'Missing hero asset'
results['checks'].append('confirmation enters running fight without crash')
Path('selection-qa.json').write_text(json.dumps(results,indent=2))
print(json.dumps(results,indent=2))

record.wait(timeout=20)
adb('pull','/sdcard/selection-effects.mp4','selection-effects.mp4')
assert Path('selection-effects.mp4').stat().st_size>10000, 'Missing emulator video'
