#!/usr/bin/env python3
"""Compare the actual exported walking PNGs in a fixed root-relative window."""
import argparse
import json
from pathlib import Path
import sys
import cv2
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from sprite_pipeline.root_motion import torso_anchor_x
from sprite_pipeline.exporting import write_json


def main():
    p=argparse.ArgumentParser();p.add_argument('before',type=Path);p.add_argument('after',type=Path)
    p.add_argument('--output',type=Path,required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
    sheet=Image.new('RGB',(1280,570),'#10151d');draw=ImageDraw.Draw(sheet)
    font=ImageFont.truetype('DejaVuSans.ttf',16)
    report={}
    for row,(root,label) in enumerate(((a.before,'Antes: recuo embutido'),(a.after,'Depois: movimento no lugar'))):
        manifest=json.loads((root/'manifest.json').read_text());clip=next(c for c in manifest['clips'] if c['name']=='walk_back')
        rx,ry=manifest['root'];anchors=[]
        for f in clip['frames']:
            im=cv2.cvtColor(cv2.imread(str(root/f['normalized_file']),-1),cv2.COLOR_BGRA2RGBA)
            anchors.append(torso_anchor_x(im)-rx)
        report['before' if row==0 else 'after']={'torso_range_x_px':max(anchors)-min(anchors),
            'torso_first_to_last_dx_px':anchors[-1]-anchors[0], 'frame_count':len(anchors),
            'root_motion':clip.get('root_motion'), 'source_sha256':clip['source_sha256']}
        for col,index in enumerate((0,40,80,120)):
            f=clip['frames'][index]
            im=Image.open(root/f['normalized_file']).convert('RGBA').crop((rx-320,ry-455,rx+320,ry+15))
            background=Image.new('RGBA',im.size,'#202c38');d=ImageDraw.Draw(background)
            for yy in range(0,im.height,20):
                for xx in range(0,im.width,20):
                    if (xx//20+yy//20)%2:d.rectangle((xx,yy,xx+19,yy+19),fill='#283744')
            background.alpha_composite(im)
            d.line((0,455,640,455),fill='#63e7be',width=2)
            d.line((320,0,320,470),fill='#ffc76b',width=1)
            view=background.convert('RGB').resize((320,235),Image.Resampling.LANCZOS)
            x,y=col*320,row*285
            sheet.paste(view,(x,y+42));draw.text((x+8,y+4),label,font=font,fill='white')
            draw.text((x+8,y+23),f'Frame {index} | {f["time_s"]:.2f} s',font=font,fill='#b9c7d9')
    sheet.save(a.output/'walk-back-in-place.png')
    write_json(a.output/'in-place-checks.json',report)
    print(json.dumps({k:{x:v[x] for x in ('torso_range_x_px','torso_first_to_last_dx_px','frame_count')} for k,v in report.items()},indent=2))


if __name__=='__main__':main()
