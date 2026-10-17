"""Gera uma página HTML única, com os frames incorporados, para abrir no celular sem instalar nada.

Uso: python autoscale/make_portable_html.py /pasta/assets --output /pasta/teste.html [--width 300] [--quality 80]
Lê o manifest.json criado por make_mobile_test.py.
"""
import sys, json, base64, io, argparse, pathlib
from PIL import Image

TEMPLATE = r'''<title>Sprites — Escala automática</title>
<meta name="claude:viewport-fit-sides" content="cover">
<style>
/* Layout: controles em cima, dois painéis lado a lado (antes | depois), linha do chão compartilhada. */
:root{--bg:#f3f1ec;--fg:#1f2328;--muted:#6b7280;--panel:#1b1f27;--line:#d9d4ca;--ctl:#ffffff;--ctl-on:#2458d6;--ctl-on-fg:#ffffff;--before:#c7791a;--after:#1e8f4e;
  --font:ui-sans-serif,system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;--mono:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#15181e;--fg:#e8ecf2;--muted:#9aa3b2;--panel:#0d1015;--line:#2c3340;--ctl:#232936;--ctl-on:#4f86ff;--ctl-on-fg:#0b1020;--before:#ffb454;--after:#5ee07a;color-scheme:dark}}
:root[data-theme="dark"]{--bg:#15181e;--fg:#e8ecf2;--muted:#9aa3b2;--panel:#0d1015;--line:#2c3340;--ctl:#232936;--ctl-on:#4f86ff;--ctl-on-fg:#0b1020;--before:#ffb454;--after:#5ee07a;color-scheme:dark}
body{background:var(--bg);color:var(--fg);font:15px/1.4 var(--font);-webkit-user-select:none;user-select:none}
.wrap{padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px));padding-block:10px 24px;max-width:960px;margin:0 auto;display:grid;gap:10px}
h1{font-size:18px;margin:0;text-wrap:balance}
.sub{margin:0;color:var(--muted);font-size:13px}
.row{display:flex;gap:6px;flex-wrap:wrap;align-items:center}
.row .grow{flex:1;min-width:120px}
button,select{background:var(--ctl);color:var(--fg);border:1px solid var(--line);border-radius:8px;padding:9px 12px;font:inherit;font-size:15px}
button:focus-visible,select:focus-visible,input:focus-visible{outline:2px solid var(--ctl-on);outline-offset:2px}
button[aria-pressed="true"]{background:var(--ctl-on);border-color:var(--ctl-on);color:var(--ctl-on-fg)}
.panes{display:flex;gap:4px}
.pane{flex:1;min-width:0;background:var(--panel);border-radius:8px;overflow:hidden;position:relative}
.pane h2{position:absolute;left:8px;top:6px;margin:0;font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;z-index:2}
.pane.before h2{color:var(--before)}.pane.after h2{color:var(--after)}
.pane small{position:absolute;left:8px;bottom:6px;font:12px var(--mono);color:#c9d1dc;z-index:2;font-variant-numeric:tabular-nums}
canvas{display:block;width:100%;height:auto;touch-action:none}
.panes.single .pane.before{display:none}
input[type=range]{accent-color:var(--ctl-on)}
.status{font:13px var(--mono);color:var(--muted);font-variant-numeric:tabular-nums}
.hint{font-size:13px;color:var(--muted);margin:0}
.legend{display:grid;gap:4px;font-size:13px;color:var(--muted);margin:0;padding:0;list-style:none}
.legend b{color:var(--fg)}
</style>
<div class="wrap">
<h1>Escala automática: P02 e P10, Idle e dash</h1>
<p class="sub">Esquerda: pipeline atual (escala fixa pela altura da silhueta). Direita: escala por frame, automática, alinhada ao mestre. Mesmo zoom e mesma linha do chão nos dois lados.</p>
<div class="row"><select id="char" aria-label="Personagem"></select><select id="mode" aria-label="Sequência">
<option value="seq">Idle → dash → Idle</option><option value="cycle">Só o ciclo de corrida</option><option value="dash">Dash completo</option><option value="idle">Idle</option></select>
<button id="play" aria-pressed="true">Pausar</button></div>
<div class="row"><button id="prev" aria-label="Frame anterior">◀</button><input class="grow" id="seek" type="range" min="0" max="1" value="0" aria-label="Posição"><button id="next" aria-label="Próximo frame">▶</button></div>
<div class="row"><span>Velocidade</span><button data-speed="1" aria-pressed="true">1×</button><button data-speed="0.5" aria-pressed="false">½</button><button data-speed="0.25" aria-pressed="false">¼</button>
<span style="margin-left:auto">Zoom</span><input id="zoom" type="range" min="0.6" max="2.5" step="0.05" value="1" style="width:110px" aria-label="Zoom"><button id="single" aria-pressed="false">Só depois</button></div>
<div class="panes" id="panes">
<div class="pane before"><h2>Antes · atual</h2><canvas id="cb"></canvas><small id="ib"></small></div>
<div class="pane after"><h2>Depois · automático</h2><canvas id="ca"></canvas><small id="ia"></small></div></div>
<div class="row"><span class="status" id="status"></span></div>
<p class="hint" id="hint"></p>
<ul class="legend">
<li><b>O que conferir:</b> a cabeça fica do mesmo tamanho ao entrar na corrida e ao voltar ao Idle; a corrida não pulsa de tamanho; a emenda do ciclo não dá salto.</li>
<li><b>"Corpo 1,20× do mestre"</b> quer dizer que o corpo naquele frame está 20% maior que na imagem mestre do personagem. O alvo é 1,00.</li>
<li>Prévia reduzida a __WIDTH__ px de largura por lado. Os PNGs integrais ficam na saída da ferramenta.</li>
</ul>
</div>
<script>
const data=__MANIFEST__;
const chars=Object.keys(data.characters);const W=data.width,H=data.height,GROUND=data.ground;
const $=id=>document.getElementById(id);
chars.forEach(c=>{const o=document.createElement('option');o.value=c;o.textContent=c.toUpperCase();$('char').append(o)});
let char=chars[0],mode='seq',playing=true,speed=1,pos=0,acc=0,last=null,seq=[];
const images=new Map();
function img(src){let im=images.get(src);if(!im){im=new Image();im.src=src;images.set(src,im)}return im}
function build(){const ch=data.characters[char];const idle=ch.clips.idle,dash=ch.clips.dash;seq=[];
  const cyc=dash.cycle?Array.from({length:dash.cycle.stop_exclusive-dash.cycle.start},(_,i)=>dash.cycle.start+i):dash.frames.map((f,i)=>i);
  if(mode==='idle')seq=idle.frames.map((f,i)=>['idle',i]);
  else if(mode==='dash')seq=dash.frames.map((f,i)=>['dash',i]);
  else if(mode==='cycle')seq=cyc.map(i=>['dash',i]);
  else{for(let i=0;i<48;i++)seq.push(['idle',i]);for(let r=0;r<3;r++)cyc.forEach(i=>seq.push(['dash',i]));for(let i=48;i<idle.frames.length;i++)seq.push(['idle',i])}
  $('seek').max=seq.length-1;pos=Math.min(pos,seq.length-1);
  const c=dash.cycle;$('hint').textContent=`${char.toUpperCase()}: antes, o corpo no dash vai de ${dash.before_minmax[0].toFixed(2)} a ${dash.before_minmax[1].toFixed(2)} do mestre; depois, de ${dash.after_p5_p95[0].toFixed(2)} a ${dash.after_p5_p95[1].toFixed(2)} (percentis 5 e 95). `+(c?`Ciclo escolhido: frames ${c.start} a ${c.stop_exclusive-1} da fonte (${c.period} frames), emenda IoU ${c.seam_iou}.`:'');}
function draw(){const [clip,i]=seq[pos];const f=data.characters[char].clips[clip].frames[i];const z=Number($('zoom').value);
  const panels=[['cb',f.b,`${clip} ${i} · corpo ${f.sb.toFixed(2)}× do mestre`],['ca',f.a,`${clip} ${i} · corpo ${f.sa.toFixed(2)}× · escala ${f.s.toFixed(3)}`]];
  for(const [id,src,info] of panels){
    const cv=$(id);if(cv.width!==W){cv.width=W;cv.height=H}const ctx=cv.getContext('2d');ctx.fillStyle='#0d1015';ctx.fillRect(0,0,W,H);
    ctx.save();ctx.translate(W/2,GROUND);ctx.scale(z,z);ctx.translate(-W/2,-GROUND);
    ctx.strokeStyle='#3c4557';ctx.beginPath();ctx.moveTo(-W,GROUND);ctx.lineTo(2*W,GROUND);ctx.stroke();
    const im=img(src);if(im.complete&&im.naturalWidth)ctx.drawImage(im,0,0);ctx.restore();
    $(id==='cb'?'ib':'ia').textContent=info;}
  $('seek').value=pos;$('status').textContent=`frame ${pos+1} de ${seq.length}`;}
function tick(t){if(last!==null&&playing){acc+=(t-last)*speed;const step=1000/data.fps;while(acc>=step){acc-=step;pos=(pos+1)%seq.length}}last=t;draw();requestAnimationFrame(tick)}
function pause(){playing=false;$('play').textContent='Reproduzir';$('play').setAttribute('aria-pressed','false')}
$('char').onchange=e=>{char=e.target.value;pos=0;build()};$('mode').onchange=e=>{mode=e.target.value;pos=0;build()};
$('play').onclick=()=>{playing=!playing;$('play').textContent=playing?'Pausar':'Reproduzir';$('play').setAttribute('aria-pressed',String(playing))};
$('prev').onclick=()=>{pause();pos=(pos-1+seq.length)%seq.length};
$('next').onclick=()=>{pause();pos=(pos+1)%seq.length};
$('seek').oninput=e=>{pause();pos=Number(e.target.value)};
document.querySelectorAll('[data-speed]').forEach(b=>b.onclick=()=>{speed=Number(b.dataset.speed);document.querySelectorAll('[data-speed]').forEach(x=>x.setAttribute('aria-pressed',String(x===b)))});
$('single').onclick=()=>{const on=!$('panes').classList.contains('single');$('panes').classList.toggle('single',on);$('single').setAttribute('aria-pressed',String(on))};
build();requestAnimationFrame(tick);
</script>
'''


def main():
    ap = argparse.ArgumentParser(); ap.add_argument('assets'); ap.add_argument('--output', required=True)
    ap.add_argument('--width', type=int, default=300); ap.add_argument('--quality', type=int, default=80)
    a = ap.parse_args(); assets = pathlib.Path(a.assets)
    m = json.loads((assets/'manifest.json').read_text())
    x0, y0, x1, y1 = m['crop']; scale = a.width/(x1-x0); height = round((y1-y0)*scale)
    def enc(rel):
        with Image.open(assets/rel) as im:
            im = im.convert('RGBA').resize((a.width, height), Image.Resampling.LANCZOS)
            buf = io.BytesIO(); im.save(buf, format='WEBP', quality=a.quality, method=6)
        return 'data:image/webp;base64,'+base64.b64encode(buf.getvalue()).decode()
    chars = {}
    for name, ch in m['characters'].items():
        chars[name] = {'clips': {}}
        for clip_name, clip in ch['clips'].items():
            chars[name]['clips'][clip_name] = {'cycle': clip['cycle'], 'before_minmax': clip['before_minmax'], 'after_p5_p95': clip['after_p5_p95'],
                'frames': [{'b': enc(f['raw_file']), 'a': enc(f['normalized_file']), 'sb': f['size_before'], 'sa': f['size_after'], 's': f['scale']} for f in clip['frames']]}
    payload = {'characters': chars, 'fps': m['fps'], 'width': a.width, 'height': height, 'ground': round((y1-y0-40)*scale)}
    html = TEMPLATE.replace('__MANIFEST__', json.dumps(payload, ensure_ascii=False).replace('<', '\\u003c')).replace('__WIDTH__', str(a.width))
    pathlib.Path(a.output).write_text(html, encoding='utf-8')
    print(json.dumps({'bytes': len(html.encode()), 'frames': sum(len(c['frames']) for ch in chars.values() for c in ch['clips'].values())}))


if __name__ == '__main__':
    main()
