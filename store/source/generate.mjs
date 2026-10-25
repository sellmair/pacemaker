import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {createRequire} from 'node:module';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const storeDir=path.join(root,'store');
const require=createRequire(path.join(root,'design/package.json'));
const {chromium}=require('playwright');
const sharp=require('sharp');
const catalog=JSON.parse(await fs.readFile(path.join(storeDir,'source/copy.json'),'utf8'));
const manifest={generated:'2026-10-25',brand:'Pacemaker / Shared Rhythm',appBaseline:catalog.sourceCommit,assets:[],sources:[]};
const checks=[];
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
const esc=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('"','&quot;');
async function put(relative,data){const p=path.join(storeDir,relative);await fs.mkdir(path.dirname(p),{recursive:true});await fs.writeFile(p,data);}
async function copy(source,dest){const bytes=await fs.readFile(path.join(root,source));await put(dest,bytes);manifest.sources.push({source,destination:dest,sha256:hash(bytes)});return bytes;}
const native={
androidLive:'design/implementation/android/live-within-limit-light-native.png',
androidLimit:'design/implementation/android/profile-dark-native.png',
androidDevices:'design/implementation/android/devices-dark-native.png',
androidDark:'design/implementation/android/live-above-limit-dark-native.png',
androidVoice:'design/implementation/android/profile-light-native.png'
};
for(const size of ['standard','large'])for(const [key,theme,screen] of [
['Live','dark','live'],['Limit','dark','profile-limit'],['Devices','dark','devices'],['Light','light','live'],['Voice','light','profile-lower']]){
native['ios'+size+key]='design/implementation/ios/captures/matrix/'+size+'-normal-en-'+theme+'/native-en-'+screen+'.png';
}
for(const [name,source]of Object.entries(native)) await copy(source,'source/assets/native/'+name+'.png');
for(const f of ['BarlowCondensed-SemiBold.ttf','Inter-Variable.ttf','BarlowCondensed-OFL.txt','Inter-OFL.txt'])await copy('design/fonts/'+f,'source/assets/fonts/'+f);
for(const f of ['lockup-charcoal.svg','lockup-chalk.svg','mark-acid.svg'])await copy('design/brand/'+f,'source/assets/brand/'+f);
await copy('design/icons/android/play-store-512.png','playstore/graphics/app-icon-512.png');
await copy('design/icons/ios/AppIcon.appiconset/AppIcon-1024.png','appstore/graphics/app-icon-1024.png');

const rules={playstore:{title:30,short_description:80,full_description:4000,release_notes:500},appstore:{name:30,subtitle:30,promotional_text:170,description:4000,keywords:100,whats_new:4000}};
for(const [locale,c]of Object.entries(catalog.locales))for(const platform of ['playstore','appstore']){
 const description=[c.intro,c.body,c[platform==='playstore'?'android':'ios'],c.requirements].join('\n\n');
 const fields=platform==='playstore'?{title:c.name,short_description:c.short,full_description:description,release_notes:c.whatsnew}:{name:c.name,subtitle:c.subtitle,promotional_text:c.promo,description,keywords:c.keywords,whats_new:c.whatsnew};
 for(const [field,value]of Object.entries(fields)){
  const count=field==='keywords'?Buffer.byteLength(value,'utf8'):[...value].length;
  const max=rules[platform][field];if(count>max)throw Error(locale+' '+field+': '+count+' > '+max);
  checks.push({check:platform+'/'+locale+'/'+field,count,unit:field==='keywords'?'UTF-8 bytes':'characters',maximum:max,passed:true});
  await put(platform+'/metadata/'+locale+'/'+field+'.txt',value+'\n');
 }
 await put(platform+'/metadata/'+locale+'/listing.json',JSON.stringify(fields,null,2)+'\n');
}
let browser;
const palette=[
{paper:'#DBFF00',ink:'#171A19',accent:'#171A19',edge:'#171A19'},
{paper:'#F3F5ED',ink:'#171A19',accent:'#4D6000',edge:'#748178'},
{paper:'#171A19',ink:'#F3F5ED',accent:'#DBFF00',edge:'#748178'},
{paper:'#A3E8BC',ink:'#171A19',accent:'#171A19',edge:'#171A19'},
{paper:'#171A19',ink:'#F3F5ED',accent:'#DBFF00',edge:'#748178'}
];
async function render({output,w,h,input,index,headline,sub,locale='en-US',feature=false,alt}){
 const buffer=await fs.readFile(path.join(storeDir,'source/assets/native/'+input+'.png'));
 const meta=await sharp(buffer).metadata();
 const s=w/1080,top=366*s,maxH=h-top-46*s;
 const shotW=Math.min(maxH*meta.width/meta.height,w-160*s),shotH=shotW*meta.height/meta.width;
 const colors=feature?palette[0]:(output.startsWith('playstore/')&&index===3?{...palette[3],paper:'#FF8E84'}:palette[index]);
 const name=output.replaceAll('/','_').replace('.png','.html');
 const logo=colors.ink==='#171A19'?'charcoal':'chalk';
 const html='<!doctype html><html lang="'+locale.split('-')[0]+'"><head><meta charset="utf-8"><title>'+esc(headline.join(' '))+'</title><link rel="stylesheet" href="../poster.css"></head><body class="'+(feature?'feature':'poster')+'" style="--w:'+w+'px;--h:'+h+'px;--s:'+s+';--paper:'+colors.paper+';--ink:'+colors.ink+';--accent:'+colors.accent+';--edge:'+colors.edge+';--top:'+top+'px;--shot-w:'+shotW+'px;--shot-h:'+shotH+'px"><div class="header"><img class="lockup" src="../assets/brand/lockup-'+logo+'.svg" alt="Pacemaker">'+(feature?'':'<span class="number">'+String(index+1).padStart(2,'0')+' / 05</span>')+'<h1><span>'+esc(headline[0])+'</span><span class="second">'+esc(headline[1])+'</span></h1><p class="support">'+esc(sub)+'</p></div><img class="native" src="../assets/native/'+input+'.png" alt="'+esc(alt)+'"></body></html>';
 await put('source/layouts/'+name,html);
 const page=await browser.newPage({viewport:{width:w,height:h},deviceScaleFactor:1});
 await page.goto(pathToFileURL(path.join(storeDir,'source/layouts/'+name)).href);
 await page.evaluate(()=>document.fonts.ready);
 const layout=await page.evaluate(()=>{
  const h=document.querySelector('h1').getBoundingClientRect(),p=document.querySelector('.support').getBoundingClientRect(),i=document.querySelector('.native').getBoundingClientRect();
  return {fonts:document.fonts.check('600 32px Barlow')&&document.fonts.check('400 24px Inter'),images:[...document.images].every(i=>i.complete&&i.naturalWidth>0),headingRight:h.right,supportRight:p.right,copyBottom:p.bottom,imageTop:i.top,imageBottom:i.bottom,imageRight:i.right,overflow:document.documentElement.scrollWidth>innerWidth};
 });
 if(!layout.fonts||!layout.images||layout.overflow||layout.imageBottom>h+1||layout.imageRight>w+1||layout.headingRight>w-30||layout.supportRight>w-30||(!feature&&layout.copyBottom>layout.imageTop-12))throw Error('Layout validation: '+output+' '+JSON.stringify(layout));
 if(output.startsWith('playstore')&&!feature&&layout.copyBottom>h*.2)throw Error('Caption exceeds 20%: '+output);
 if(feature&&layout.headingRight>w-280)throw Error('Feature headline overlaps screen: '+output);
 const png=await sharp(await page.screenshot({omitBackground:false})).removeAlpha().toColourspace('srgb').png().toBuffer();
 await put(output,png);
 const m=await sharp(png).metadata();
 checks.push({check:output,dimensions:[m.width,m.height],channels:m.channels,bytes:png.length,passed:m.channels===3&&m.width===w&&m.height===h});
 manifest.assets.push({path:output,width:w,height:h,format:'RGB PNG',locale,role:feature?'feature-graphic':'phone-screenshot',alt,source:'source/assets/native/'+input+'.png',layout:'source/layouts/'+name,sha256:hash(png)});
 await page.close();
}
try{
 browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH||'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'});
 const c=catalog.locales['en-US'];
 const androidInputs=['androidLive','androidLimit','androidDevices','androidDark','androidVoice'];
 const iosNames=['Live','Limit','Devices','Light','Voice'];
 const ids=['01-train-together','02-personal-limit','03-your-signal','04-clear-status','05-voice-choice'];
 const alts=[
 'Pacemaker Live shows 142 bpm, a 150 bpm limit and nearby participants with their own readings.',
 'Profile lets you choose a display name, heart-rate limit, identity color and voice alerts.',
 'Devices offers automatic, sensor and watch sources with visible connection status.',
 'Live makes the current heart-rate status and personal limit visible.',
 'Profile offers a choice of voice alerts alongside your personal settings.'
 ];
 for(let i=0;i<5;i++){
  let headline=c.headlines[i],sub=c.subs[i];
  if(i===3){headline=['WHEN YOUR PULSE','PASSES YOUR LIMIT.'];sub='A clear signal to ease the pace.';}
  await render({output:'playstore/screenshots/en-US/phone-1080x1920/'+ids[i]+'.png',w:1080,h:1920,input:androidInputs[i],index:i,headline,sub,alt:alts[i]});
  for(const [size,w,h]of [['standard',1206,2622],['large',1320,2868]]){
   await render({output:'appstore/screenshots/en-US/iphone-'+w+'x'+h+'/'+ids[i]+'.png',w,h,input:'ios'+size+iosNames[i],index:i,headline:c.headlines[i],sub:c.subs[i],alt:alts[i]});
  }
 }
 for(const [locale,c]of Object.entries(catalog.locales)){
  await render({output:'playstore/graphics/'+locale+'/feature-graphic-1024x500.png',w:1024,h:500,input:'androidLive',index:0,headline:c.feature,sub:c.featureSub,locale,feature:true,alt:{'en-US':'Keep pace together: live heart rate and personal limits in Pacemaker.','de-DE':'Gemeinsam im Tempo bleiben: Live-Puls und persönliche Limits in Pacemaker.','it-IT':'Allenati insieme agli altri: battito in tempo reale e limiti personali con Pacemaker.'}[locale]});
 }
 for(const [locale,lang]of [['en-US','en'],['de-DE','de'],['it-IT','it']]){
  for(const [n,screen,alt]of [
   ['01-live','home-opening','Heart rate, personal limit and group summary on Wear OS.'],
   ['02-controls','home-controls','Workout controls on Wear OS.'],
   ['03-group','group-opening','Group readings and individual limits on Wear OS.']]){
   const src='design/implementation/wear-os/captures/'+screen+'-large-'+lang+'-1.0.png';
   const bytes=await fs.readFile(path.join(root,src));const m=await sharp(bytes).metadata();
   const result=m.channels===3?bytes:await sharp(bytes).removeAlpha().toColourspace('srgb').png().toBuffer();
   const output='playstore/screenshots/'+locale+'/wear-os/'+n+'.png';await put(output,result);
   const localizedAlt={
    'de-DE':{'01-live':'Puls, persönliches Limit und Gruppenübersicht auf Wear OS.','02-controls':'Trainingssteuerung auf Wear OS.','03-group':'Messwerte der Gruppe und individuelle Limits auf Wear OS.'},
    'it-IT':{'01-live':'Battito, limite personale e riepilogo del gruppo su Wear OS.','02-controls':'Controlli di allenamento su Wear OS.','03-group':'Valori del gruppo e limiti individuali su Wear OS.'}
   }[locale]?.[n]||alt;
   manifest.assets.push({path:output,width:m.width,height:m.height,locale,role:'wear-screenshot',alt:localizedAlt,source:src,sha256:hash(result)});
   if(m.width!==m.height||m.width<384)throw Error('Invalid Wear size');
   checks.push({check:output,dimensions:[m.width,m.height],passed:true});
  }
 }
 for(const [file,w,alpha]of [['playstore/graphics/app-icon-512.png',512,true],['appstore/graphics/app-icon-1024.png',1024,false]]){
  const b=await fs.readFile(path.join(storeDir,file)),m=await sharp(b).metadata();
  if(m.width!==w||m.height!==w||(alpha?m.channels!==4:m.channels!==3)||(w===512&&b.length>1024*1024))throw Error('Icon format '+file);
  manifest.assets.push({path:file,width:w,height:w,role:'app-icon',locale:'all',alt:'Pacemaker: two angular rhythm lines in acid yellow on charcoal.',sha256:hash(b)});
  checks.push({check:file,dimensions:[w,w],channels:m.channels,bytes:b.length,passed:true});
 }
 const galleryCSS='@font-face{font-family:Barlow;src:url(source/assets/fonts/BarlowCondensed-SemiBold.ttf)}@font-face{font-family:Inter;src:url(source/assets/fonts/Inter-Variable.ttf)}*{box-sizing:border-box}body{margin:0;padding:48px;background:#171A19;color:#F3F5ED;font:16px Inter,sans-serif}h1{font:600 76px/.98 Barlow;margin:25px 0}h2{font:600 42px Barlow;margin:40px 0 18px}p{color:#A9B3AA;max-width:900px;line-height:1.6}a{color:#DBFF00}header{border-bottom:1px solid #748178;padding-bottom:30px}.row{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:18px}.tile img{width:100%;height:auto;display:block}.tile small{display:block;margin-top:10px;color:#A9B3AA;line-height:1.4}.features{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}.features img{width:100%}.watch{display:flex;gap:24px}.watch img{width:220px}.icon{width:80px;float:right}section{margin-bottom:32px}@media(max-width:800px){body{padding:24px}.row{grid-template-columns:repeat(2,1fr)}.features{grid-template-columns:1fr}.watch{flex-wrap:wrap}h1{font-size:50px}}';
 const tiles=list=>list.map(a=>'<a class="tile" href="'+a.path+'"><img loading="lazy" src="'+a.path+'" alt="'+esc(a.alt)+'"><small>'+a.width+' × '+a.height+' · '+path.basename(a.path)+'</small></a>').join('');
 const phone=manifest.assets.filter(a=>a.path.startsWith('playstore/')&&a.role==='phone-screenshot');
 const iphone=manifest.assets.filter(a=>a.path.includes('iphone-1320x2868/'));
 const features=manifest.assets.filter(a=>a.role==='feature-graphic');
 const wear=manifest.assets.filter(a=>a.role==='wear-screenshot'&&a.locale==='en-US');
 const html='<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Pacemaker — store kit</title><style>'+galleryCSS+'</style><header><img class="icon" src="playstore/graphics/app-icon-512.png" alt="Pacemaker icon"><div>STORE KIT / SHARED RHYTHM</div><h1>KEEP PACE.<br><span style="color:#DBFF00">TOGETHER.</span></h1><p>Copy in English, German and Italian. Native phone captures in English. Localized Wear OS captures and feature graphics. Open an image for the full upload file.</p><a href="README.md">Upload guide</a> · <a href="playstore/COPY.md">Google Play copy</a> · <a href="appstore/COPY.md">App Store copy</a></header><section><h2>Google Play / Phone</h2><div class="row">'+tiles(phone)+'</div></section><section><h2>App Store / iPhone</h2><p>1320 × 2868 shown. The matching 1206 × 2622 set is also included.</p><div class="row">'+tiles(iphone)+'</div></section><section><h2>Google Play / Feature graphics</h2><div class="features">'+tiles(features)+'</div></section><section><h2>Google Play / Wear OS</h2><div class="watch">'+tiles(wear)+'</div></section><footer><p>App UI is native capture with controlled sample data. Asset provenance and validation are in manifest.json and validation.json. Source layouts are editable HTML/CSS.</p></footer></html>';
 await put('index.html',html);
 const page=await browser.newPage({viewport:{width:1600,height:1000},deviceScaleFactor:1});
 await page.goto(pathToFileURL(path.join(storeDir,'index.html')).href);
 await page.evaluate(()=>document.fonts.ready);await page.locator('footer').scrollIntoViewIfNeeded();await page.evaluate(()=>scrollTo(0,0));
 await page.screenshot({path:path.join(storeDir,'preview.png'),fullPage:true});
 await page.close();
 for(const platform of ['playstore','appstore']){
  const assets=manifest.assets.filter(a=>a.path.startsWith(platform+'/'));
  if(assets.some(a=>[...a.alt].length>140))throw Error('Alt text exceeds 140 characters');
  await put(platform+'/ALT_TEXT.tsv','file\tlocale\talt_text\n'+assets.map(a=>a.path.slice(platform.length+1)+'\t'+a.locale+'\t'+a.alt).join('\n')+'\n');
  await put(platform+'/manifest.json',JSON.stringify({generated:manifest.generated,assets},null,2)+'\n');
 }
 await put('manifest.json',JSON.stringify(manifest,null,2)+'\n');
 await put('validation.json',JSON.stringify({passed:true,checks},null,2)+'\n');
 console.log(JSON.stringify({assets:manifest.assets.length,checks:checks.length,passed:true}));
}finally{await browser?.close();}
