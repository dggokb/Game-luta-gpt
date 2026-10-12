"""Actual APK QA for sequential four-slot selection and sixteen concept portraits."""
import json
import subprocess as sp
import time
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image, ImageChops

def adb(*args): return sp.check_output(['adb',*args])
def capture(name):
    p=Path('selection-'+name+'.png');p.write_bytes(adb('exec-out','screencap','-p'))
    return Image.open(p).convert('RGB')
adb('install','-r','android/app/build/outputs/apk/debug/app-debug.apk')
adb('logcat','-c')
adb('shell','settings','put','secure','immersive_mode_confirmations','confirmed')
adb('shell','am','start','-W','-n','com.gamelutagpt/com.gamelutagpt.MainActivity')
time.sleep(10)
adb('shell','uiautomator','dump','/sdcard/selection-ui.xml')
ui=ET.fromstring(adb('shell','cat','/sdcard/selection-ui.xml'))
for n in ui.iter('node'):
    if n.attrib.get('text','').upper()=='GOT IT' and n.attrib.get('package') in ['com.android.systemui','android']:
        x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']))
        adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(1)
initial=capture('default');w,h=initial.size;assert w>h
results={'resolution':[w,h],'checks':[]}
def tap(x,y):
    adb('shell','input','tap',str(round(x*w/1280)),str(round(y*h/720)));time.sleep(.6)
def differs(a,b,region=None):
    if region:
        q=tuple(round(v*(w/1280 if i%2==0 else h/720)) for i,v in enumerate(region));a,b=a.crop(q),b.crop(q)
    return ImageChops.difference(a.convert('RGB'),b.convert('RGB')).getbbox() is not None
def portrait_region(side,slot):
    # The empty slot label changes AGUARDANDO → ESCOLHA on focus. Compare
    # a fixed inner portrait area above that label, away from the animated rim.
    x=80 if side==0 else 1134;y=400+130*slot
    return x,y,x+75,y+22

def stable_slots(a,b):
    return all(not differs(a,b,portrait_region(side,slot)) for side in range(2) for slot in range(2))
def cell(i):
    cw=(440-18)/4;ch=(400-18)/4;y=137+(i//4)*(ch+6)+ch/2;x=420+(i%4)*(cw+6)+cw/2
    return 640+(x-640)*(440+148*(y-137)/400)/440,y

time.sleep(1);animated=capture('animated')
assert differs(initial,animated) and stable_slots(initial,animated),'Effects must animate without moving slot contents'
results['checks'].append('strong effects animate; slot contents stay stationary')
for x,y in [(110,420),(110,560),(1150,420),(1150,560)]:tap(x,y)
slots=capture('slot-taps');assert stable_slots(initial,slots),'TEAM taps jumped sequence'
results['checks'].append('TEAM panels do not change the active step')
for i in range(8,16):
    tap(*cell(i));locked=capture('locked');assert stable_slots(initial,locked),'Unavailable concept changed teams: '+str(i)
results['checks'].append('P09–P16 are visible concepts and all remain blocked')

# Select a point, then Back automatically returns to P1 preview. This exercises
# every actual side illustration without manually choosing a TEAM slot.
last=initial
for i,name in [(0,'p01'),(3,'p02'),(1,'p03'),(5,'p04'),(6,'p05'),(2,'p06'),(7,'p07'),(4,'p08')]:
    tap(*cell(i));selected=capture(name+'-chosen')
    assert differs(initial,selected,portrait_region(0,0)),name+' did not fill first point slot'
    tap(503,700);preview=capture(name)
    if name!='p01':assert differs(last,preview,(170,55,330,340)),name+' upper-right action art not rendered'
    assert stable_slots(initial,preview),'Back failed to reopen the first slot'
    last=preview
results['checks'].append('all eight playable action illustrations render; Back reopens previous step')

# A real video includes the entire automatic sequence and the fight reveal.
record=sp.Popen(['adb','shell','screenrecord','--size','1280x720','--bit-rate','3000000','--time-limit','90','/sdcard/selection-effects.mp4'])
time.sleep(1)
tap(*cell(0));one=capture('step-1')
assert differs(initial,one,portrait_region(0,0)),'P1 point not chosen'
assert not differs(initial,one,portrait_region(0,1)),'TAG P1 filled before second choice'
tap(*cell(0));duplicate=capture('duplicate-tag');assert stable_slots(one,duplicate),'Duplicate TAG replaced point'
tap(*cell(4));two=capture('step-2')
assert differs(one,two,portrait_region(0,1)),'Second portrait did not choose TAG P1'
assert not differs(initial,two,portrait_region(1,0)),'P2 filled too early'
tap(*cell(1));three=capture('step-3')
assert differs(two,three,portrait_region(1,0)),'Third portrait did not choose P2 point'
assert not differs(initial,three,portrait_region(1,1)),'TAG P2 filled too early'
tap(*cell(2));four=capture('ready')
assert differs(three,four,portrait_region(1,1)),'Fourth portrait did not choose TAG P2'
results['checks'].append('four portrait taps choose P1 → TAG P1 → P2 → TAG P2 automatically')
results['checks'].append('duplicate partner is rejected without replacing the point')
for i in range(8,16):tap(*cell(i))
locked_ready=capture('locked-ready');assert stable_slots(four,locked_ready),'Blocked concepts changed finished teams'
tap(914,700);swapped=capture('swapped')
assert differs(four,swapped,portrait_region(1,0)),'Finished team did not swap point/TAG'
results['checks'].append('point/TAG swap works after completing the sequence')
ready=swapped
tap(370,700);deadline=time.monotonic()+35
while time.monotonic()<deadline:
    time.sleep(.5);fight=capture('confirmed')
    if differs(ready,fight,(440,150,485,205)) and differs(ready,fight,(550,150,595,205)):break
    logs=adb('logcat','-d','-v','brief').decode(errors='replace');Path('selection-logcat.txt').write_text(logs)
    assert 'FATAL EXCEPTION' not in logs,'Crash during sprite loading'
else:raise AssertionError('Fight did not render within 35 seconds')
time.sleep(1);capture('fight')
assert adb('shell','pidof','com.gamelutagpt').strip(),'App exited'
logs=adb('logcat','-d','-v','brief').decode(errors='replace');Path('selection-logcat.txt').write_text(logs)
assert 'FATAL EXCEPTION' not in logs,'Android crash'
assert 'Missing selection art:' not in logs,'Missing concept or action illustration'
results['checks'].append('confirmation enters the actual fight without crash')
sp.run(['adb','shell','pkill','-2','screenrecord'],check=False);record.wait(timeout=20)
adb('pull','/sdcard/selection-effects.mp4','selection-effects.mp4')
assert Path('selection-effects.mp4').stat().st_size>10000,'Missing actual APK video'
Path('selection-qa.json').write_text(json.dumps(results,indent=2));print(json.dumps(results,indent=2))
