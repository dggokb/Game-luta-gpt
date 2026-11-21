const {chromium}=require('playwright');const path=require('path');
(async()=>{const b=await chromium.launch();const p=await b.newPage({viewport:{width:1280,height:720}});
 for(const c of ['A','B','C']){await p.goto('file://'+path.resolve(process.argv[2])+'?c='+c+'&ctl=1');await p.waitForTimeout(300);
  await p.screenshot({path:path.join(path.dirname(process.argv[2]),'hud_'+c+'.png'),omitBackground:true});}
 await b.close();})();
