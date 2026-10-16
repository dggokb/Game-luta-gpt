#!/usr/bin/env python3
"""Package only replacement candidates; never repackage the immutable baseline."""
import argparse,copy,hashlib,json,zipfile
from pathlib import Path


def package(folder,validation,output):
    if output.exists():raise ValueError('Use um arquivo novo')
    d=json.loads((folder/'assets/manifest.json').read_text());checks=json.loads(validation.read_text());assert checks['status']=='passed'
    names={c['clips']['after'] for c in d['cases'] if c['decision']=='corrected'}
    clips=copy.deepcopy([c for c in d['clips'] if c['name'] in names])
    for c in clips:
        for f in c['frames']:
            for key in ('normalized_file','normalized_rect','normalized_destination_rect','raw_file','raw_rect'):f.pop(key,None)
    manifest={'status':'selective_candidates_for_review','clips':clips,'cases':d['cases'],
        'source_timing_preserved':True,'configuration_sha256':d['configuration_sha256'],
        'baseline_manifest_sha256':d['structural_triage']['baseline_manifest_sha256'],
        'approved_visual_reference':d['approved_visual_reference'],'full_frame_anatomical_validation':False,
        'note':'Only corrected after clips have PNGs here. baseline_png paths refer to the previous collection, not this ZIP. Preview atlases are in the APK.'}
    output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted((folder/'normalized').rglob('*.png')):z.write(p,p.relative_to(folder).as_posix())
        z.writestr('manifest.json',json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
        for name in ('LEIA-ME.txt','triage-report.json','triage.csv'):z.write(folder/name,name)
        z.write(folder/'assets/project.snapshot.json','selective-triage.json');z.write(validation,'validation.json')
    with zipfile.ZipFile(output) as z:
        assert z.testzip() is None
        assert len([n for n in z.namelist() if n.endswith('.png')])==531
        for c in manifest['clips']:
            for f in c['frames']:assert f['normalized_png'] in z.namelist()
    report={'file':output.name,'bytes':output.stat().st_size,'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'normalized_pngs':531,'corrected_clips':8,'all_clips_replaced':False}
    output.with_suffix('.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--folder',type=Path,required=True);p.add_argument('--validation',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();package(a.folder,a.validation,a.output)
