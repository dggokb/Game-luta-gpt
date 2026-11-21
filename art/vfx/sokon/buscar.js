// Busca os ajustes do Sōkon FX (Fumaça) cuja animação mais se parece, quadro a quadro, com uma fumaça
// de referência: a forma (alfa) de cada um dos 16 instantes, numa caixa comum presa embaixo no meio,
// comparada por IoU. Busca aleatória e depois refinamento em volta do melhor.
//   node art/vfx/sokon/buscar.js REFS.json NOME AVALIACOES SAIDA.json [base.json]
const {chromium}=require('playwright');
const fs=require('fs'),path=require('path');
const [refsArq,nome,nAval,saida]=process.argv.slice(2);
const ref=JSON.parse(fs.readFileSync(refsArq,'utf8'))[nome];
const ESPACO={plumes:[1,8,1],gap:[0,120,1],tipGap:[0,120,1],joinAt:[10,100,1],rise:[30,200,1],fall:[0,40,1],
 power:[3,14,1],windForce:[0,100,1],windAngle:[-180,180,1],windX:[60,452,1],windY:[60,420,1],windWidth:[12,240,1],
 curl:[0,100,1],rimWidth:[50,220,1],coreWidth:[50,200,1]};
const ESCOLHAS={smokeMirror:['off','flip','pairs'],drawing:['star','cross','saw','twin']};
function sorteio(r){const v={};for(const[k,[a,b]]of Object.entries(ESPACO))v[k]=Math.round(a+r()*(b-a));for(const[k,o]of Object.entries(ESCOLHAS))v[k]=o[Math.floor(r()*o.length)];return v}
function vizinho(base,r,f){const v={...base};for(const[k,[a,b]]of Object.entries(ESPACO))if(r()<.35)v[k]=Math.max(a,Math.min(b,Math.round(v[k]+(r()-.5)*2*f*(b-a))));for(const[k,o]of Object.entries(ESCOLHAS))if(r()<.08)v[k]=o[Math.floor(r()*o.length)];return v}
let s=12345;const rnd=()=>{s=(Math.imul(s,1664525)+1013904223)>>>0;return s/4294967296};
(async()=>{
 const b=await chromium.launch();const p=await b.newPage();
 await p.goto('file://'+path.resolve(__dirname,'SokonFX_4.html'));
 await p.evaluate(()=>{const el=id=>document.getElementById(id);el('kind').value='Fumaça';el('kind').oninput({});
  el('angle').value=0;el('tint').value='#ffffff';el('particles').value=0;el('particles').oninput();el('light').value=0;el('light').oninput();
  el('styleMix').value=30;el('styleMix').oninput();el('shadeLight').value=60;el('windPick');});
 async function avaliar(v,semente){
  return await p.evaluate(({v,semente,ref})=>{
   const el=id=>document.getElementById(id);
   for(const[k,x]of Object.entries(v)){const e=el(k);e.value=x;if(e.oninput)e.oninput();}
   seed=semente;
   const N=64,big=document.createElement('canvas');big.width=big.height=512;const g=big.getContext('2d');
   const sm=document.createElement('canvas');sm.width=sm.height=128;const h=sm.getContext('2d');
   const al=[];let x0=128,x1=-1,y0=128,y1=-1;
   for(let i=0;i<16;i++){g.clearRect(0,0,512,512);draw(g,i);h.clearRect(0,0,128,128);h.drawImage(big,0,0,128,128);
    const d=h.getImageData(0,0,128,128).data,a=new Uint8Array(128*128);
    for(let j=0;j<a.length;j++){a[j]=d[j*4+3];if(a[j]>60){const x=j%128,y=(j/128)|0;if(x<x0)x0=x;if(x>x1)x1=x;if(y<y0)y0=y;if(y>y1)y1=y;}}
    al.push(a);}
   if(x1<0)return 0;
   const w=x1-x0+1,hh=y1-y0+1,L=Math.max(w,hh);
   let tot=0;
   for(let t=0;t<16;t++){let inter=0,uni=0;const R=ref[t];
    for(let yy=0;yy<N;yy++)for(let xx=0;xx<N;xx++){
     // célula (xx,yy) da caixa comum -> ponto da animação
     const cx=(xx+.5)/N*L,cy=(yy+.5)/N*L;const sx=Math.floor(x0+cx-(L-w)/2),sy=Math.floor(y0+cy-(L-hh));
     const m=(sx>=0&&sx<128&&sy>=0&&sy<128&&al[t][sy*128+sx]>76)?1:0,r=R[yy*N+xx];
     inter+=m&r;uni+=m|r;}
    tot+=uni?inter/uni:1;}
   return tot/16;
  },{v,semente,ref});
 }
 let melhor=null;const total=+nAval,ini=Math.floor(total*.55);const lista=[];
 for(let i=0;i<total;i++){
  const v=i<ini||!melhor?sorteio(rnd):vizinho(melhor.v,rnd,i<total*.8?.15:.06);
  const semente=i<ini||!melhor||rnd()<.3?Math.floor(rnd()*1e9):melhor.semente;
  const nota=await avaliar(v,semente);lista.push({nota,v,semente});
  if(!melhor||nota>melhor.nota){melhor={nota,v,semente};console.log(i,nota.toFixed(3));}
 }
 lista.sort((a,b)=>b.nota-a.nota);
 fs.writeFileSync(saida,JSON.stringify(lista.slice(0,8),null,1));
 await b.close();
})();
