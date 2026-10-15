#!/usr/bin/env python3
"""Reproduce the P02 dash scale regression and a separate corrected preview."""
import argparse
import base64
import copy
import json
from pathlib import Path
import shutil
import sys
import cv2

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from sprite_pipeline.corrections import frame_transforms
from sprite_pipeline.extraction import digest
from sprite_pipeline.exporting import write_json, contact_sheet
from sprite_pipeline.imaging import image_rgba, affine_rgba_region, bounds
from sprite_pipeline.root_motion import register_anchors
from sprite_pipeline.scale_audit import audit_scale, require_scale_audit
from sprite_pipeline.streaming import decode_frames
from process_collection import PreviewPages
from prepare_mobile import render_mobile


def review(source, baseline, annotations, output):
    if output.exists() and any(output.iterdir()):
        raise ValueError('Use uma pasta de revisão nova ou vazia')
    output.mkdir(parents=True,exist_ok=True)
    data=json.loads((baseline/'assets/manifest.json').read_text())
    original=next(c for c in data['clips'] if c['name']=='dash')
    setting=json.loads(annotations.read_text())['clips']['dash']
    assert digest(source)==original['source_sha256']==setting['source_sha256']
    before=copy.deepcopy(original);before.update(name='dash_antes',role=None,status='known_scale_drift_baseline')
    before['note']='Comparação: versão 1.5 com mudança de tamanho durante o dash.'
    before['scale_audit']=audit_scale(setting['scale_audit'],original,
                                    [f['transform_source_to_output'] for f in original['frames']])
    assert before['scale_audit']['status']=='drift_detected'
    transforms,correction=frame_transforms(setting,original,original)
    transforms,motion=register_anchors(original['root_motion']['anchor_x_source_px'],transforms,
                                       original['reference_frame'],original['source_sha256'])
    after=copy.deepcopy(original)
    after.update(name='dash_corrigido',role=None,status='requires_visual_review',
        scale_mode='annotated_segment_curve',scale_correction=correction,root_motion=motion,
        scale_audit=audit_scale(setting['scale_audit'],original,transforms),
        note='Correção experimental por medidas manuais da cabeça. Ombros e oito amostras de controle conferidos separadamente. Efeitos e proporções da fonte ainda exigem revisão.')
    require_scale_audit(after['scale_audit'])
    assets=output/'assets';assets.mkdir()
    shutil.copytree(baseline/'assets/masters',assets/'masters')
    copied=set()
    for clip in (before,after):
        for frame in clip['frames']:
            fields=('raw_file','normalized_file') if clip is before else ('raw_file',)
            for key in fields:
                name=frame[key]
                if name not in copied:
                    dest=assets/name;dest.parent.mkdir(parents=True,exist_ok=True)
                    shutil.copy2(baseline/'assets'/name,dest);copied.add(name)
    pages=PreviewPages(assets,'images/dash-corrected/after','normalized',data['canvas'])
    old_folder=output/'normalized/dash_antes';old_folder.mkdir(parents=True)
    new_folder=output/'normalized/dash_corrigido';new_folder.mkdir(parents=True)
    for i,raw in enumerate(decode_frames(source,original)):
        old=before['frames'][i]
        shutil.copy2(baseline/old['normalized_png'],old_folder/f'{i:06d}.png')
        old['normalized_png']=f'normalized/dash_antes/{i:06d}.png'
        m=copy.deepcopy(transforms[i]);m[0][2]+=data['root'][0];m[1][2]+=data['root'][1]
        rgba=image_rgba(raw,original['background_treatment'])
        result=affine_rgba_region(rgba,m[0][0],[m[0][2],m[1][2]],data['canvas'])
        assert cv2.imwrite(str(new_folder/f'{i:06d}.png'),cv2.cvtColor(result,cv2.COLOR_RGBA2BGRA),[cv2.IMWRITE_PNG_COMPRESSION,1])
        frame=after['frames'][i];box=bounds(result)
        frame.update(normalized_png=f'normalized/dash_corrigido/{i:06d}.png',
            transform_source_to_output=m,scale_applied=m[0][0],bbox_output=box,
            offset_from_root=[box[0]-data['root'][0],box[1]-data['root'][1]],
            removed_root_motion_output_px=[motion['removed_x_output_px'][i],0])
        # Recompute preview displacement from the transformed source box.
        b=frame['bbox_source'];reference=after['frames'][0]['bbox_output']
        frame['visual_displacement_from_reference']=[(b[0]+b[2])*m[0][0]/2+m[0][2]-(reference[0]+reference[2])/2,
                                                    b[3]*m[1][1]+m[1][2]-reference[3]]
        pages.add(result,frame)
    pages.flush()
    after['calibration_transform_source_to_output']=after['frames'][0]['transform_source_to_output']
    assert digest(source)==original['source_sha256']
    assert [f['pts'] for f in before['frames']]==[f['pts'] for f in after['frames']]
    data.update(clips=[before,after],status='experimental_scale_review',
        mobile_preview={'frame_count':194,'maximum_image_width':720,'layout':'cropped_tiles_v1',
                        'format':'WebP quality 85 atlas tiles','all_frames_retained':True,'timestamps_changed':False})
    write_json(assets/'manifest.json',data)
    write_json(assets/'calibration-report.json',{'before':before['scale_audit'],'after':after['scale_audit']})
    write_json(assets/'project.snapshot.json',setting)
    contact_sheet(assets/'calibration-comparison.png',[
        cv2.cvtColor(cv2.imread(str(old_folder/'000000.png'),-1),cv2.COLOR_BGRA2RGBA),
        cv2.cvtColor(cv2.imread(str(old_folder/'000026.png'),-1),cv2.COLOR_BGRA2RGBA),
        cv2.cvtColor(cv2.imread(str(new_folder/'000026.png'),-1),cv2.COLOR_BGRA2RGBA)],
        ['Idle no inicio','Dash anterior - frame 26','Dash corrigido - frame 26'],max_column=480)
    write_json(output/'source-verification.json',{'source_sha256':digest(source),'original_frames':97,
        'originals_unchanged':True,'pts_equal':True,'full_frame_anatomical_validation':False})
    preview_bytes=write_review_viewer(data,assets,output)
    print(json.dumps({'before':before['scale_audit']['status'],'after':after['scale_audit']['status'],
        'correction_factor_range':correction['factor_range'],'original_frames':97,
        'head_holdouts':after['scale_audit']['holdout_count'],'preview_bytes':preview_bytes}))


def write_review_viewer(data,assets,output):
    render_mobile(data,assets)
    page=(assets/'index.html').read_text().replace('<h1>Sprites — Visualizador</h1>','<h1>P02 — revisão do dash</h1>')
    start="""<script>
    // Inspect a fixed body-sized viewport; both versions share this zoom and pan.
    // This changes only the camera, never the sprite scale or per-frame geometry.
    commonZoom=function(){const stages=[$('after').parentElement,$('comparison').parentElement].filter(s=>s.clientWidth>0);
      return Math.min(Math.min(...stages.map(s=>s.clientWidth))/(data.inspection_width||480),
                      Math.min(...stages.map(s=>s.clientHeight))/520)*Number($('zoom').value||1)};
    let diagnosticViewport='';
    window.addEventListener('spriteframe',()=>{
      $('clipNote').hidden=false;$('clipNote').textContent=data.inspection_note||'Comparação experimental por amostras: corrigido e versão anterior, no mesmo zoom.';
      const k=commonZoom(),key=[k,innerWidth,document.body.dataset.view].join('/');
      if(key!==diagnosticViewport){diagnosticViewport=key;for(const id of ['after','comparison']){
        const stage=$(id).parentElement,center=data.inspection_camera_center||[data.root[0],data.root[1]-195];stage.scrollLeft=center[0]*k-stage.clientWidth/2;
        stage.scrollTop=center[1]*k-stage.clientHeight/2;
      }}
    });
    $('zoom').value=String(data.inspection_zoom||1.25);current=1;$('clip').value='1';$('compare').value='0';frame=data.inspection_start_frame??26;
    document.querySelector('[data-view=compare]').click();render();
    </script>"""
    page=page.replace('</html>',start+'</html>');(assets/'index.html').write_text(page)
    if data.get('status')=='action_only_experimental_review':
        page=page.replace('correção anatômica anotada','ajuste de escala do ciclo').replace('curva anatômica anotada','ajuste de escala do ciclo')
        (assets/'index.html').write_text(page)
    images={}
    for path in assets.rglob('*'):
        if path.suffix.lower() in ('.webp','.png','.json'):
            kind={'.webp':'image/webp','.png':'image/png','.json':'application/json'}[path.suffix]
            images[path.relative_to(assets).as_posix()]='data:'+kind+';base64,'+base64.b64encode(path.read_bytes()).decode()
    prefix='<script>const embeddedAssets='+json.dumps(images)+';function inlineSource(src){return embeddedAssets[src]||src}</script>'
    single=page.replace('<script>',prefix+'<script>',1).replace('im.src=src','im.src=inlineSource(src)')
    single=single.replace('im.src=ref.landmarks_file||ref.file','im.src=inlineSource(ref.landmarks_file||ref.file)')
    single=single.replace('a.href=ref.file','a.href=inlineSource(ref.file)')
    single=single.replace('</html>',"<script>document.querySelectorAll('a[href]').forEach(a=>{const name=a.getAttribute('href');if(embeddedAssets[name])a.href=inlineSource(name)})</script></html>")
    (output/'p02-dash-comparacao.html').write_text(single)
    return len(single.encode())


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True)
    p.add_argument('--baseline',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    p.add_argument('--annotations',type=Path,default=ROOT/'examples/collection/motion-landmarks/p02.json')
    a=p.parse_args();review(a.source,a.baseline,a.annotations,a.output)
