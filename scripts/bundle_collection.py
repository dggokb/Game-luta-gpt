#!/usr/bin/env python3
"""Assemble an offline character chooser and every completed viewer."""
import argparse
import html
import json
from pathlib import Path
import shutil
import sys
from urllib.parse import quote

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
from sprite_pipeline.exporting import write_json
from prepare_mobile import render_mobile


def bundle(processed, output, inventory):
    if output.exists() and any(output.iterdir()):
        raise ValueError('Use uma pasta de assets nova ou vazia')
    output.mkdir(parents=True,exist_ok=True)
    summaries=[json.loads(p.read_text()) for p in sorted(processed.glob('*/complete.json'))]
    summaries.sort(key=lambda s:(0 if s['character'].startswith('p') and s['character'][1:].isdigit() else 1,s['character']))
    expected={c['id'] for c in json.loads(inventory.read_text())}
    actual={s['character'] for s in summaries}
    if actual!=expected:
        raise ValueError(f'Coleção incompleta: faltam {sorted(expected-actual)}; extras {sorted(actual-expected)}')
    for summary in summaries:
        if summary['videos']:
            clips=json.loads((processed/summary['character']/'assets/manifest.json').read_text())['clips']
            summary['in_place_clips']=[c['name'] for c in clips if c['status']!='rejected' and c.get('root_motion')]
    root_manifest={'format_version':4,'status':'needs_review','clips':[], 'characters':summaries,
                   'mobile_preview':{'all_frames_retained':True,'timestamps_changed':False,
                       'format':'WebP quality 85 atlas tiles','maximum_image_width':720,
                       'full_resolution_pngs_included':False}}
    cards=[]
    for summary in summaries:
        cid=summary['character'];encoded=quote(cid)
        if not summary['videos']:
            cards.append(f'<article><h2>{html.escape(cid)}</h2><p>Sem vídeos disponíveis. Somente a imagem do personagem.</p></article>')
            continue
        source=processed/cid/'assets';dest=output/'characters'/cid
        shutil.copytree(source,dest,copy_function=shutil.copy2)
        manifest=json.loads((dest/'manifest.json').read_text())
        render_mobile(manifest,dest)
        page=(dest/'index.html').read_text()
        page=page.replace('<h1>Sprites — Visualizador</h1>',
             f'<p><a href="../../index.html">← Personagens</a></p><h1>Sprites — {html.escape(cid)}</h1>')
        (dest/'index.html').write_text(page)
        rejected=len(summary['rejected'])
        cards.append(f'<a class="card" href="characters/{encoded}/index.html"><h2>{html.escape(cid)}</h2>'
            f'<img src="characters/{encoded}/masters/idle-aligned.png" alt="{html.escape(cid)}">'
            f'<p>{summary["videos"]} vídeos · {summary["frames"]:,} frames</p>'
            f'<p>{summary["converted"]} convertidos · {rejected} sem calibração segura</p></a>')
        for clip in manifest['clips']:
            bundled={k:clip.get(k) for k in ('name','status','frame_count','source_sha256','source_name')}
            bundled.update(name=cid+'/'+clip['name'],character=cid,frames=[])
            for frame in clip['frames']:
                entry={k:frame[k] for k in ('index','pts')}
                for key in ('raw_file','normalized_file'):
                    if key in frame:
                        entry[key]=f'characters/{cid}/'+frame[key]
                bundled['frames'].append(entry)
            root_manifest['clips'].append(bundled)
    total=sum(s['videos'] for s in summaries)
    frames=sum(s['frames'] for s in summaries)
    converted=sum(s['converted'] for s in summaries)
    root_manifest['mobile_preview']['frame_count']=frames
    write_json(output/'manifest.json',root_manifest)
    page='''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sprites — Personagens</title><style>
:root{color-scheme:dark;font:16px system-ui;background:#10151d;color:#e7ecf5}body{max-width:1100px;margin:auto;padding:18px}h1{font-size:26px}p{line-height:1.5;color:#b9c7d9}a{color:inherit;text-decoration:none}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:14px}.card,article{border:1px solid #40536b;border-radius:12px;padding:16px;background:#18222f}.card:active,.card:hover{border-color:#63e7be}.card img{display:block;height:180px;width:100%;object-fit:contain}.card h2{margin:0;color:#8de4d1}.card p{font-size:14px;margin:6px 0}article{opacity:.65}details{margin:18px 0;padding:14px;border:1px solid #40536b;border-radius:9px}summary{cursor:pointer}</style>
<h1>Sprites — Personagens</h1><p>Escolha um personagem para reproduzir e comparar suas animações. Tudo funciona offline.</p>
'''+f'<p id="counts">{total} vídeos · {frames:,} frames · {converted} convertidos</p>'+'''
<details><summary>Como conferir</summary><p>Andar e dash usam registro horizontal no lugar. O jogo controla a posição. Os saltos conservam a fase aérea nesta prévia.</p><p>Os vídeos sem uma referência confiável aparecem como REJEITADO: o original está disponível em Antes e o motivo aparece na tela. Cortes e deformações da fonte continuam sinalizados.</p><p>Comparar exibe os dois movimentos no mesmo zoom. Para comparar poses específicas, arraste a barra de frames da direita.</p></details><main class="cards">'''+''.join(cards)+'</main></html>'
    (output/'index.html').write_text(page)
    return {'characters':len(summaries),'videos':total,'frames':frames,'converted':converted,
            'rejected':total-converted,'asset_bytes':sum(p.stat().st_size for p in output.rglob('*') if p.is_file())}


def main():
    p=argparse.ArgumentParser();p.add_argument('processed',type=Path);p.add_argument('--output',type=Path,required=True)
    p.add_argument('--inventory',type=Path,required=True)
    args=p.parse_args();print(json.dumps(bundle(args.processed,args.output,args.inventory),indent=2))


if __name__=='__main__':main()
