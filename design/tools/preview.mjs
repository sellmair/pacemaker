import fs from 'node:fs/promises';
import path from 'node:path';
import http from 'node:http';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const {chromium}=require('playwright');
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
await fs.mkdir(path.join(root,'previews'),{recursive:true});
await fs.mkdir(path.join(root,'validation'),{recursive:true});
const mime={'.html':'text/html','.css':'text/css','.js':'text/javascript','.svg':'image/svg+xml','.png':'image/png','.ttf':'font/ttf','.json':'application/json','.md':'text/plain','.ico':'image/x-icon','.webmanifest':'application/manifest+json'};
const server=http.createServer(async(req,res)=>{
  try {
    const requestPath=decodeURIComponent(new URL(req.url,'http://localhost').pathname);
    const file=path.resolve(root,'.'+(requestPath==='/'?'/index.html':requestPath));
    if(file!==root&&!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
    const data=await fs.readFile(file);
    res.writeHead(200,{'Content-Type':mime[path.extname(file)]||'application/octet-stream'});res.end(data);
  }catch{res.writeHead(404);res.end('Not found');}
});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
let browser;
const errors=[],checks=[];
function check(name,value){checks.push({name,passed:!!value});if(!value)throw Error(name);}
try {
  let executablePath=process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH;
  if(!executablePath){
    const macChrome='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
    try{await fs.access(macChrome);executablePath=macChrome;}catch{}
  }
  browser=await chromium.launch({headless:true,...(executablePath?{executablePath}:{})});
  const page=await browser.newPage({viewport:{width:1440,height:1000},deviceScaleFactor:2,reducedMotion:'reduce'});
  page.on('pageerror',e=>errors.push(e.message));
  page.on('console',msg=>{if(msg.type()==='error')errors.push(msg.text());});
  await page.goto('http://127.0.0.1:'+server.address().port,{waitUntil:'networkidle'});
  await page.evaluate(()=>document.fonts.ready);
  check('Fonts loaded',await page.evaluate(()=>document.fonts.check('600 32px "Barlow Condensed"')&&document.fonts.check('400 16px Inter')));
  check('All displayed images loaded',await page.evaluate(()=>[...document.images].every(i=>i.complete&&i.naturalWidth>0)));
  check('UI icon paths loaded',await page.evaluate(()=>[...document.querySelectorAll('[data-icon]')].every(el=>el.querySelector('path,circle,rect'))));
  await page.locator('#identity-board').screenshot({path:path.join(root,'previews/identity-overview.png')});
  await page.locator('#screen-board').screenshot({path:path.join(root,'previews/app-screens-dark.png')});
  await page.locator('#component-board').screenshot({path:path.join(root,'previews/components-dark.png')});
  await page.locator('#watch-board').screenshot({path:path.join(root,'previews/wearable.png')});
  await page.locator('[data-state="above"]').click();
  check('Above-limit state',await page.locator('#heart-rate').textContent()==='157');
  await page.locator('#live-screen').screenshot({path:path.join(root,'previews/live-above-limit.png')});
  await page.locator('[data-state="offline"]').click();
  check('Signal-loss state has no stale reading',await page.locator('#heart-rate').textContent()==='—');
  await page.locator('#live-screen').screenshot({path:path.join(root,'previews/live-signal-lost.png')});
  await page.locator('[data-state="within"]').click();
  await page.locator('[data-theme-choice="light"]').click();
  await page.locator('#screen-board').screenshot({path:path.join(root,'previews/app-screens-light.png')});
  await page.locator('#component-board').screenshot({path:path.join(root,'previews/components-light.png')});
  await page.locator('[data-select-source="watch"]').click();
  check('Source selection updates live source', (await page.locator('#live-source').textContent()).includes('Wear OS watch'));
  await page.locator('#limit-up').click();
  check('Limit adjustment propagates',await page.locator('#limit-value').textContent()==='151'&&await page.locator('#heart-rate').textContent()==='143');
  await page.locator('#limit-range').focus();
  await page.locator('#limit-range').press('End');
  await page.locator('[data-state="above"]').click();
  check('Meter expands for a high limit',await page.locator('#scale-max').textContent()==='220 bpm'&&await page.locator('#heart-rate').textContent()==='207');
  await page.locator('#voice-toggle').click();
  check('Voice settings are synchronized',!(await page.locator('#voice-setting').isChecked()));
  await page.locator('#name').fill('');
  await page.locator('#save-settings').click();
  check('Name validation',await page.locator('#name').getAttribute('aria-invalid')==='true');
  await page.locator('#name').fill('Sam');
  await page.locator('#save-settings').click();
  check('Preference save feedback', (await page.locator('#save-message').textContent()).includes('Saved'));
  for(const width of [320,390,768,1440]){
    await page.setViewportSize({width,height:1000});
    const overflow=await page.evaluate(()=>[...document.querySelectorAll('body *')].filter(el=>el.getBoundingClientRect().right>window.innerWidth+1).map(el=>({tag:el.tagName,class:el.className,right:Math.round(el.getBoundingClientRect().right),width:Math.round(el.getBoundingClientRect().width)})).slice(0,20));
    if(overflow.length) console.log(JSON.stringify({width,overflow}));
    check('No horizontal overflow at '+width+'px', await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth));
  }
  check('No browser errors',errors.length===0);
  // Verify offline file opening, including local fonts and inline icon definitions.
  await page.goto('file://'+path.join(root,'index.html'),{waitUntil:'load'});
  await page.evaluate(()=>document.fonts.ready);
  check('Offline guide loads without a server',await page.locator('#heart-rate').textContent()==='142');
  check('Offline images loaded',await page.evaluate(()=>[...document.images].every(i=>i.complete&&i.naturalWidth>0)));
  await fs.writeFile(path.join(root,'validation/browser-checks.json'),JSON.stringify({passed:true,checks,errors},null,2)+'\n');
  console.log('Rendered eight previews; '+checks.length+' browser checks passed.');
}catch(error){
  await fs.writeFile(path.join(root,'validation/browser-checks.json'),JSON.stringify({passed:false,checks,errors,error:String(error)},null,2)+'\n');
  throw error;
}finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
