#!/usr/bin/env python3
"""Render a normal/slow comparison from exported PNGs, without synthesizing poses."""
import argparse,bisect,json,subprocess
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont


def render(folder,output):
    if output.exists():raise ValueError('Use arquivo novo')
    d=json.loads((folder/'assets/manifest.json').read_text());cs={c['name']:c for c in d['clips']};old=cs['before'];new=cs['body']
    font_path='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf';font=ImageFont.truetype(font_path,22);big=ImageFont.truetype(font_path,32);small=ImageFont.truetype(font_path,18)
    rx,ry=d['root'];crop=(int(rx-304),int(ry-428),int(rx+304),int(ry+28));cache={}
    for c in (old,new):
        for f in c['frames']:
            im=Image.open(folder/f['normalized_png']).convert('RGBA').crop(crop).resize((600,450),Image.Resampling.LANCZOS);cache[(c['name'],f['index'])]=im
    offset=next(f['time_s'] for f in old['frames'] if f['source_frame_index']==37)
    def index(c,t):return max(0,bisect.bisect_right([f['time_s'] for f in c['frames']],t%c['duration_s'])-1)
    base=Image.new('RGB',(1280,720),'#10151d');p=ImageDraw.Draw(base)
    for px in (25,655):
        for y in range(140,590,24):
            for x in range(px,px+600,24):p.rectangle((x,y,min(x+23,px+599),min(y+23,589)),fill=('#26323f' if ((x-px)//24+(y-140)//24)%2 else '#1c2630'))
    p.text((25,22),'P04 — comparação da corrida',font=big,fill='white');p.text((25,93),'Entrega anterior · 12 frames',font=font,fill='#e8edf4');p.text((655,93),'Novo · 6 frames, corpo separado',font=font,fill='#e8edf4')
    p.text((25,663),'Mesmo zoom. Escala constante. Nenhuma pose nova.',font=small,fill='#c2cbd7')
    proc=subprocess.Popen(['ffmpeg','-v','error','-f','rawvideo','-pixel_format','rgb24','-video_size','1280x720','-framerate','60','-i','pipe:0','-an','-c:v','libx264','-preset','fast','-crf','20','-pix_fmt','yuv420p','-movflags','+faststart',str(output)],stdin=subprocess.PIPE)
    try:
        for k in range(600):
            t=k/60;elapsed=t if t<4 else 4+(t-4)*.25;ai=index(old,elapsed+offset);bi=index(new,elapsed);im=base.copy();p=ImageDraw.Draw(im)
            im.paste(cache[('before',ai)],(25,140),cache[('before',ai)]);im.paste(cache[('body',bi)],(655,140),cache[('body',bi)])
            for px in (25,655):p.line((px,562,px+599,562),fill='#6cbdac',width=1)
            p.text((900,31),'Normal' if t<4 else 'Câmera lenta · ¼',font=font,fill='#89dfca');p.text((25,610),f"Fonte {old['frames'][ai]['source_frame_index']+1}",font=font,fill='#c2cbd7');p.text((655,610),f"Fonte {new['frames'][bi]['source_frame_index']+1} · {bi+1}/6",font=font,fill='#c2cbd7');proc.stdin.write(im.tobytes())
        proc.stdin.close();assert proc.wait()==0
    finally:
        if proc.poll() is None:proc.kill()
    # Consecutive poses and explicit last -> first boundary for static review.
    sheet=Image.new('RGB',(1200,720),'#233040');pen=ImageDraw.Draw(sheet)
    for j,i in enumerate([3,4,5,0,1,2]):
        f=new['frames'][i];im=Image.open(folder/f['normalized_png']).convert('RGBA').crop(crop);im.thumbnail((395,300));x=j%3*400;y=j//3*360;sheet.paste(im,(x,y+35),im);pen.text((x+8,y+7),f"Fonte {f['source_frame_index']+1} · {i+1}/6"+(' · FIM' if i==5 else ' · INÍCIO' if i==0 else ''),font=small,fill='white')
    sheet.save(output.with_suffix('.jpg'))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--folder',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();render(a.folder,a.output)
