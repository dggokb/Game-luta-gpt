#!/usr/bin/env python3
"""Portable, reduced visual evidence. Full PNG outputs are never modified."""
import argparse
import base64
import io
import json
from pathlib import Path
import shutil
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import cv2
from sprite_pipeline.exporting import contact_sheet
from PIL import Image


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('result', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args=parser.parse_args()
    m=json.loads((args.result/'manifest.json').read_text())
    args.output.mkdir(parents=True,exist_ok=True)
    shutil.copy2(args.result/'calibration-comparison.png',args.output/'calibration-comparison.png')
    shutil.copy2(args.result/'calibration-report.json',args.output/'calibration-report.json')
    for file in ('provenance.json',):
        if (args.result.parent/file).exists():shutil.copy2(args.result.parent/file,args.output/file)
    factor=min(1,420/m['canvas'][0])
    size=[round(x*factor) for x in m['canvas']]
    clips=[]
    for c in m['clips']:
        if c['status']=='rejected':continue
        frames=[]
        for f in c['frames']:
            im=Image.open(args.result/f['normalized_file']).convert('RGBA').resize(size,Image.Resampling.LANCZOS)
            stream=io.BytesIO();im.save(stream,format='WEBP',quality=80,method=4)
            frames.append({'image':'data:image/webp;base64,'+base64.b64encode(stream.getvalue()).decode(),
                           'duration_s':f['duration_s'],'time_s':f['time_s'],
                           'scale_applied':f.get('scale_applied',c['scale_per_video'])})
        clips.append({'name':c['name'],'scale':c['scale_per_video'],'reference_frame':c['reference_frame'],
                      'scale_mode':c.get('scale_mode','constant_per_video'),
                      'source_height':c['frames'][c['reference_frame']]['bbox_source'][3]-c['frames'][c['reference_frame']]['bbox_source'][1],
                      'frames':frames})
    by_name={c['name']:c for c in m['clips']}
    images,labels=[],[]
    for name,axis in [('dash',0),('jump',1)]:
        c=by_name[name]
        def support(f):
            b=f['bbox_source']
            return (b[0]+b[2])/2 if axis==0 else b[3]
        start=support(c['frames'][0])
        peak=max(range(c['frame_count']),key=lambda i:abs(support(c['frames'][i])-start))
        for index in (0,peak,c['frame_count']-1):
            images.append(cv2.cvtColor(cv2.imread(str(args.result/c['frames'][index]['normalized_file']),-1),cv2.COLOR_BGRA2RGBA))
            labels.append(f"{name}: frame {index}, t={c['frames'][index]['time_s']:.3f}s")
    contact_sheet(args.output/'motion-comparison.png',images,labels,max_column=540)
    from PIL import ImageDraw
    cells=[]
    for name in ('jab','jab_half'):
        c=by_name[name];f=c['frames'][c['reference_frame']]
        im=Image.open(args.result/f['raw_file']).convert('RGB')
        cell=Image.new('RGB',(1280,760),(30,40,50));cell.paste(im,((1280-im.width)//2,760-im.height))
        ImageDraw.Draw(cell).text((20,8),f"BEFORE: {name} | visual source height {f['bbox_source'][3]-f['bbox_source'][1]} px",fill='white',font_size=26)
        cells.append(cell)
    for name in ('jab','jab_half'):
        c=by_name[name];f=c['frames'][c['reference_frame']]
        im=Image.open(args.result/f['normalized_file']).convert('RGBA')
        cell=Image.new('RGB',(1280,760),(30,40,50));cell.paste(im,((1280-im.width)//2,760-im.height),im)
        ImageDraw.Draw(cell).text((20,8),f"AFTER: {name} | visual output height {f['bbox_output'][3]-f['bbox_output'][1]} px",fill='white',font_size=26)
        cells.append(cell)
    proof=Image.new('RGB',(1280,760))
    for i,cell in enumerate(cells):proof.paste(cell.resize((640,380),Image.Resampling.LANCZOS),((i%2)*640,(i//2)*380))
    proof.save(args.output/'scale-proof.png')
    data={'clips':clips,'size':size,'original_canvas':m['canvas'],'root':[v*factor for v in m['root']]}
    template='''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Evidência real — Padronizador</title>
<style>body{background:#111a25;color:#e4edf7;font:16px system-ui;max-width:1200px;margin:auto;padding:24px}a{color:#8dd5ff}button,select{font:inherit;padding:9px;margin:5px;background:#25364b;color:white}canvas{background:repeating-conic-gradient(#263544 0 25%,#1c2936 0 50%) 0/24px 24px;max-width:47%;border:1px solid #547087}img{max-width:100%}p{line-height:1.5}.warning{color:#ffd98c}@media(max-width:600px){body{padding:12px}h1{font-size:22px}button,select{min-height:44px;max-width:100%}canvas{display:block;max-width:100%;margin:8px auto}#seek{width:95%}}</style>
<h1>Calibração com vídeos reais existentes</h1><p>Prévia portátil reduzida, com todos os frames normalizados e tempos do lote. Nenhuma pose ou movimento foi criado. As duas vistas usam o mesmo canvas e zoom; à direita fica a pose de referência da animação comparada. Os PNGs integrais e o visualizador antes/depois estão na saída local da execução.</p>
<p class="warning">O Jab em meia resolução é uma derivada controlada de um vídeo existente. Crouch pequeno vem de outra fonte nativa. A anotação entre mestres é aproximada. Deformações, oscilações e cortes da fonte permanecem; este relatório não declara aprovação total da arte.</p>
<select id="clip"></select><select id="compare"></select><button id="play">Reproduzir</button><button id="prev">◀</button><button id="next">▶</button><input id="seek" type="range" min="0" value="0"><span id="time"></span><br>
<canvas id="view"></canvas> <canvas id="other"></canvas><p id="info"></p>
<p><a href="calibration-report.json">Métricas e alertas</a> · <a href="provenance.json">Procedência e hashes</a> · <a href="different-scales.png">Antes/depois no navegador</a></p>
<img src="calibration-comparison.png" alt="Poses equivalentes após calibração">
<script id="data" type="application/json">__DATA__</script><script>
const data=JSON.parse(document.getElementById('data').textContent),$=id=>document.getElementById(id);let current=0,index=0,play=false,last=0,elapsed=0,token=0;const images=new Map();
for(const id of ['clip','compare'])data.clips.forEach((c,i)=>{const o=document.createElement('option');o.value=i;o.textContent=c.name;$(id).append(o)});
for(const id of ['view','other']){$(id).width=data.size[0];$(id).height=data.size[1]}
function image(src){if(!images.has(src))images.set(src,new Promise(resolve=>{const im=new Image();im.onload=()=>resolve(im);im.src=src}));return images.get(src)}
async function render(){const n=++token,c=data.clips[current],other=data.clips[Number($('compare').value)],f=c.frames[index];$('seek').max=c.frames.length-1;$('seek').value=index;$('time').textContent=`${index+1}/${c.frames.length} · ${f.time_s.toFixed(4)} s`;
const [a,b]=await Promise.all([image(f.image),image(other.frames[other.reference_frame].image)]);if(n!==token)return;for(const [id,im] of [['view',a],['other',b]]){const ctx=$(id).getContext('2d');ctx.clearRect(0,0,...data.size);ctx.drawImage(im,0,0);ctx.strokeStyle='#66dfb5';ctx.beginPath();ctx.moveTo(0,data.root[1]);ctx.lineTo(data.size[0],data.root[1]);ctx.stroke()}
$('info').textContent=`${c.name}: fonte de referência ${c.source_height} px; fator deste frame ${f.scale_applied.toFixed(7)}${c.scale_mode==='annotated_segment_curve'?' · correção anatômica anotada':''}. Comparação: ${other.name}, fonte ${other.source_height} px; fator de referência ${other.scale.toFixed(7)}. Canvas original ${data.original_canvas.join(' × ')} px.`}
$('clip').onchange=()=>{current=Number($('clip').value);index=0;elapsed=0;render()};$('compare').onchange=render;$('play').onclick=()=>{play=!play;$('play').textContent=play?'Pausar':'Reproduzir'};
$('next').onclick=()=>{play=false;index=(index+1)%data.clips[current].frames.length;render()};$('prev').onclick=()=>{play=false;index=Math.max(0,index-1);render()};$('seek').oninput=()=>{play=false;index=Number($('seek').value);render()};
function tick(t){if(play){elapsed+=Math.min((t-last)/1000,.2);let changed=false;while(elapsed>=data.clips[current].frames[index].duration_s){elapsed-=data.clips[current].frames[index].duration_s;index=(index+1)%data.clips[current].frames.length;changed=true}if(changed)render()}last=t;requestAnimationFrame(tick)}render();requestAnimationFrame(tick);
</script></html>'''
    (args.output/'preview.html').write_text(template.replace('__DATA__',json.dumps(data).replace('<','\\u003c')),encoding='utf-8')
    print(json.dumps({'frames':sum(len(c['frames']) for c in clips),'preview':str(args.output/'preview.html'),'size_bytes':(args.output/'preview.html').stat().st_size}))


if __name__=='__main__':main()
