#!/usr/bin/env python3
"""Publish a compact coverage report and an actionable source review table."""
import argparse
import csv
import json
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from sprite_pipeline.extraction import digest
from sprite_pipeline.exporting import write_json


def report(inventory, inputs, processed, output):
    output.mkdir(parents=True,exist_ok=True)
    inventory=json.loads(inventory.read_text())
    characters=[];rows=[];sources=[]
    for character in inventory:
        cid=character['id'];folder=processed/cid
        summary=json.loads((folder/'complete.json').read_text())
        characters.append(summary)
        manifest=json.loads((folder/'assets/manifest.json').read_text()) if summary['videos'] else None
        for source in character['files']:
            if not source['mime_type'].startswith(('video/','image/')):continue
            path=inputs/cid/source['name'];sha=digest(path)
            if manifest:assert sha==manifest['original_sha256'][source['name']]
            sources.append({'character':cid,'file_name':source['name'],'drive_file_id':source['id'],
                            'bytes':path.stat().st_size,'sha256':sha})
        if not manifest:continue
        for clip in manifest['clips']:
            rows.append({'character':cid,'animation':clip['name'],'state':clip.get('state'),
                'source':clip['source_name'],'frames':clip['frame_count'],'status':clip['status'],
                'reference_pose':clip.get('reference_pose'),'reference_frame':clip.get('reference_frame'),
                'reference_similarity':clip.get('reference_similarity'),'scale':clip.get('scale_per_video'),
                'root_motion':clip.get('root_motion',{}).get('mode','preserved'),
                'frames_with_source_warnings':len(clip['frame_warnings']),
                'reason':clip.get('rejection_reason',''),'note':clip.get('note','')})
    video_ids={s['id'] for c in inventory for s in c['files'] if s['mime_type'].startswith('video/')}
    assert len(rows)==len(video_ids)
    totals={key:sum(c.get(key,0) for c in characters) for key in ('videos','frames','converted')}
    totals.update(rejected=totals['videos']-totals['converted'],
        characters_with_videos=sum(c['videos']>0 for c in characters),
        normalized_frames=sum(r['frames'] for r in rows if r['status']!='rejected'),
        in_place_clips=sum(r['root_motion']=='in_place_x' and r['status']!='rejected' for r in rows),
        clips_with_source_warnings=sum(r['frames_with_source_warnings']>0 for r in rows))
    result={'source_snapshot_date':'2026-10-15','totals':totals,'characters':characters,
        'without_videos':[c['character'] for c in characters if not c['videos']],
        'pending':[r for r in rows if r['status']=='rejected'],
        'visual_approval':False,
        'note':'Converted means exported for review. Clipping, deformation and uncertain references remain explicit; this does not approve integration into the game.'}
    write_json(output/'collection-summary.json',result)
    write_json(output/'collection-source-hashes.json',{'originals_unchanged':True,'sources':sources})
    with (output/'collection-review.csv').open('w',newline='',encoding='utf-8-sig') as stream:
        writer=csv.DictWriter(stream,fieldnames=list(rows[0]),quoting=csv.QUOTE_ALL)
        writer.writeheader();writer.writerows(rows)
    print(json.dumps(totals,indent=2))


def main():
    p=argparse.ArgumentParser();p.add_argument('--inventory',type=Path,required=True)
    p.add_argument('--inputs',type=Path,required=True);p.add_argument('--processed',type=Path,required=True)
    p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    report(a.inventory,a.inputs,a.processed,a.output)


if __name__=='__main__':main()
