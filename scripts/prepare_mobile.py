#!/usr/bin/env python3
"""Bundle every existing frame as a lightweight offline mobile preview.

Original PNGs, timestamps and anatomical metadata are never modified.
"""
import argparse
import copy
import json
from pathlib import Path
import shutil
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from sprite_pipeline.exporting import write_json


def prepare(result, output, width=720):
    result, output = Path(result).resolve(), Path(output).resolve()
    if output.exists() and any(output.iterdir()):
        raise ValueError('Use uma pasta de assets nova ou vazia')
    if not 320 <= width <= 1080:
        raise ValueError('Largura de prévia inválida')
    original = json.loads((result/'manifest.json').read_text())
    manifest = copy.deepcopy(original)
    output.mkdir(parents=True,exist_ok=True)
    total = 0
    for clip in manifest['clips']:
        for frame in clip['frames']:
            for field in ('raw_file','normalized_file'):
                if field not in frame:continue
                source = result/frame[field]
                relative = Path('images')/clip['name']/('before' if field=='raw_file' else 'after')/f"{frame['index']:06d}.webp"
                dest = output/relative
                dest.parent.mkdir(parents=True,exist_ok=True)
                with Image.open(source) as image:
                    image = image.convert('RGBA' if field=='normalized_file' else 'RGB')
                    size = (min(width,image.width),max(1,round(image.height*min(1,width/image.width))))
                    image = image.resize(size,Image.Resampling.LANCZOS)
                    image.save(dest,format='WEBP',quality=85,method=4)
                frame[field] = relative.as_posix()
            total += 1
    shutil.copytree(result/'masters',output/'masters')
    for file in ('calibration-report.json','calibration-comparison.png','project.snapshot.json'):
        shutil.copy2(result/file,output/file)
    manifest['mobile_preview']={'frame_count':total,'maximum_image_width':width,'format':'WebP quality 85',
        'all_frames_retained':True,'timestamps_changed':False,'full_resolution_pngs_included':False,
        'note':'Only display derivatives; original coordinate metadata, scale factors and PNG outputs are unchanged.'}
    write_json(output/'manifest.json',manifest)
    render_mobile(manifest, output)
    return {'frames':total,'images':len(list(output.rglob('*.webp'))),'bytes':sum(f.stat().st_size for f in output.rglob('*') if f.is_file())}


def render_mobile(manifest, output):
    template = (ROOT/'sprite_pipeline/viewer.html').read_text()
    # The desktop CSS is later in the document, so append the touch overrides last.
    css=(ROOT/'mobile/viewer.css').read_text()
    template=template.replace('</style>','</style><style>'+css+'</style>',1)
    template=template.replace('<h1>Padronizador universal de sprites</h1>',
        f'<h1>Sprites — Visualizador</h1><p class="mobileIntro">Offline · {len(manifest["clips"])} animações · todos os frames · sem precisar de computador</p>')
    template=template.replace('<div class="panes">',
        '<nav id="mobileTabs" aria-label="Visualização"><button data-view="before" aria-pressed="false">Antes</button><button data-view="after" aria-pressed="true">Depois</button><button data-view="compare" aria-pressed="false">Comparar</button></nav>'
        '<p id="mobileHint">Use o zoom para aproximar e arraste a imagem para inspecionar. As imagens são prévias reduzidas; os dados de escala permanecem originais.</p><div class="panes">')
    template=template.replace('<details open>', '<details>')
    template=template.replace('Antes — frame original integral','Antes — prévia do frame original')
    template=template.replace('frames brutos · estado:', 'frames disponíveis · estado:')
    template=template.replace('if(cache.size>80)', 'if(cache.size>32)')
    template=template.replace('if(raw)br.drawImage(raw,0,0)', 'if(raw)br.drawImage(raw,0,0,c.resolution[0],c.resolution[1])')
    template=template.replace('ctx.drawImage(ghost,0,0)', 'ctx.drawImage(ghost,0,0,data.canvas[0],data.canvas[1])')
    template=template.replace('ctx.drawImage(im,carry[0],carry[1])', 'ctx.drawImage(im,carry[0],carry[1],data.canvas[0],data.canvas[1])')
    template=template.replace('cc.drawImage(ci,0,0)', 'cc.drawImage(ci,0,0,data.canvas[0],data.canvas[1])')
    old="function commonZoom(){const width=Math.min($('after').parentElement.clientWidth,$('comparison').parentElement.clientWidth);return Math.min(width/data.canvas[0],460/data.canvas[1])*Number($('zoom').value||1)}"
    new="function commonZoom(){const widths=[$('after').parentElement.clientWidth,$('comparison').parentElement.clientWidth].filter(w=>w>0);const width=widths.length?Math.min(...widths):document.body.clientWidth-18;return Math.min(width/data.canvas[0],360/data.canvas[1])*Number($('zoom').value||1)}"
    if old not in template:raise ValueError('Template mudou; reveja o cálculo de zoom mobile')
    template=template.replace(old,new)
    template=template.replace('value="1" min="0.1"','value="1.5" min="0.1"')
    data=json.dumps(manifest,ensure_ascii=False).replace('<','\\u003c').replace('&','\\u0026')
    template=template.replace('__MANIFEST__',data)
    extra="""<script>
    document.body.dataset.view='after';
    const toolbar=document.querySelector('.bar');
    const selectors=document.createElement('div');selectors.className='mobileSelectors';
    selectors.append(document.getElementById('clip').parentElement,document.getElementById('compare').parentElement);
    const playback=document.createElement('div');playback.className='mobilePlayback';
    ['play','prev','next','seek','position'].forEach(id=>playback.append(document.getElementById(id)));
    const options=document.createElement('details');options.className='mobileOptions';
    const optionsTitle=document.createElement('summary');optionsTitle.textContent='Sequências, ciclos, mestre e zoom';options.append(optionsTitle);
    while(toolbar.firstChild)options.append(toolbar.firstChild);
    toolbar.replaceWith(selectors);
    document.querySelector('.panes').after(playback,options);
    document.querySelectorAll('.pane pre').forEach(pre=>{
      const details=document.createElement('details');details.className='mobileMetrics';
      const title=document.createElement('summary');title.textContent='Medidas deste frame';details.append(title);
      pre.replaceWith(details);details.append(pre);
    });
    const warning=document.querySelector('.warn');
    const limits=document.createElement('details');const limitsTitle=document.createElement('summary');limitsTitle.textContent='Limites e revisão do lote';
    limits.append(limitsTitle);warning.replaceWith(limits);limits.append(warning);options.after(limits);
    document.getElementById('summary').textContent=`${clips.length} animações · ${clips.reduce((a,c)=>a+c.frame_count,0)} frames · lote em revisão`;
    document.querySelector('.mobileIntro').textContent='Reproduza e compare offline, direto no celular.';
    document.getElementById('mobileHint').textContent='Arraste a imagem. Zoom em Opções. Prévia reduzida.';
    document.getElementById('prev').textContent='◀';document.getElementById('next').textContent='▶';

    document.querySelectorAll('#mobileTabs button').forEach(button=>button.onclick=()=>{
      document.body.dataset.view=button.dataset.view;
      document.querySelectorAll('#mobileTabs button').forEach(b=>b.setAttribute('aria-pressed',b===button?'true':'false'));
      document.getElementById('mobileHint').textContent=button.dataset.view==='compare'?'Os dois painéis usam o mesmo zoom. Gire o celular para uma comparação maior.':'Use o zoom para aproximar e arraste a imagem. Prévia reduzida; metadados nas coordenadas originais.';
      render();
    });
    render();
    </script>"""
    template=template.replace('</html>',extra+'</html>')
    (output/'index.html').write_text(template,encoding='utf-8')

def main():
    parser=argparse.ArgumentParser();parser.add_argument('result',type=Path);parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--width',type=int,default=720);parser.add_argument('--refresh-html',action='store_true');args=parser.parse_args()
    if args.refresh_html:
        render_mobile(json.loads((args.output/'manifest.json').read_text()), args.output)
    else:
        print(json.dumps(prepare(args.result,args.output,args.width),indent=2))


if __name__=='__main__':main()
