// Gera quadros de efeito com o Sōkon FX do dono (SokonFX_3.html), sem tela: abre a ferramenta num
// Chromium, põe os controles como no tipo pedido e desenha os 16 quadros, um PNG transparente cada.
//   node art/vfx/sokon/gerar.js art/vfx/sokon/fumacas.json SAIDA [SokonFX_4.html]
// O .json é uma lista de tipos {nome, seed, valores: {id do controle: valor}}; sai SAIDA/<nome>/q_01..q_16.png.
const {chromium}=require('playwright');
const fs=require('fs'),path=require('path');
(async()=>{
 const tipos=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));
 const saida=process.argv[3];
 const b=await chromium.launch();const p=await b.newPage();
 const erros=[];p.on('pageerror',e=>erros.push(e.message));
 await p.goto('file://'+path.resolve(__dirname,process.argv[4]||'SokonFX_4.html'));
 for(const tipo of tipos){
  const quadros=await p.evaluate(({valores,semente})=>{
   const el=id=>document.getElementById(id);
   // O tipo do efeito primeiro: trocar o tipo devolve os outros controles ao padrão dele.
   if(valores.kind){el('kind').value=valores.kind;el('kind').oninput({});}
   for(const [id,v] of Object.entries(valores)){
    if(id==='kind')continue;
    if(id==='halo'){if(haloEnabled!==v)el('halo').onclick();continue;}
    const e=el(id);
    if(e.type==='checkbox')e.checked=v;else e.value=v;
    if(e.oninput)e.oninput();else if(e.onchange)e.onchange();
   }
   seed=semente;
   const out=[];
   for(let i=0;i<16;i++){const c=document.createElement('canvas');c.width=c.height=512;draw(c.getContext('2d'),i);out.push(c.toDataURL('image/png'));}
   return out;
  },{valores:tipo.valores,semente:tipo.seed});
  const dir=path.join(saida,tipo.nome);fs.mkdirSync(dir,{recursive:true});
  quadros.forEach((q,i)=>fs.writeFileSync(path.join(dir,'q_'+String(i+1).padStart(2,'0')+'.png'),Buffer.from(q.split(',')[1],'base64')));
  console.log(tipo.nome,quadros.length,'quadros');
 }
 if(erros.length)console.log('erros',erros);
 await b.close();
})();
