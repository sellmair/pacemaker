import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
import {spawnSync} from 'node:child_process';
const require=createRequire(import.meta.url),sharp=require('sharp');
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const brand=JSON.parse(await fs.readFile(path.join(root,'src/brand.json'),'utf8'));
const outputs=JSON.parse(await fs.readFile(path.join(root,'exports.json'),'utf8'));
const checks=[],contrast=[],native={};
const check=(name,passed,detail)=>{checks.push({name,passed:!!passed,...(detail?{detail}:{})});};
await fs.mkdir(path.join(root,'validation'),{recursive:true});
for(const item of outputs.images){
  const file=path.join(root,item.file), meta=await sharp(file).metadata();
  check(item.file+': dimensions',meta.width===item.width&&meta.height===item.height);
  check(item.file+': alpha format',item.mode==='rgba'?meta.hasAlpha:!meta.hasAlpha);
  check(item.file+': sRGB',meta.space==='srgb');
}
const tinted=await sharp(path.join(root,'icons/ios/AppIcon.appiconset/AppIcon-Tinted-1024.png')).raw().toBuffer();
check('iOS tinted artwork is grayscale RGB',tinted.every((v,i)=>i%3===0||v===tinted[i-i%3]));
const play=await sharp(path.join(root,'icons/android/play-store-512.png')).stats();
check('Play icon is fully opaque RGBA',play.isOpaque);
check('Play icon is below 1 MiB',(await fs.stat(path.join(root,'icons/android/play-store-512.png'))).size<1048576);
function luminance(hex){
 const v=hex.slice(1).match(/../g).map(v=>parseInt(v,16)/255).map(v=>v<=.04045?v/12.92:((v+.055)/1.055)**2.4);
 return v[0]*.2126+v[1]*.7152+v[2]*.0722;
}
function ratio(a,b){const aa=luminance(a),bb=luminance(b);return(Math.max(aa,bb)+.05)/(Math.min(aa,bb)+.05);}
for(const [theme,t] of Object.entries(brand.themes)){
 const pairs=[['primary action',t.onPrimary,t.primary],['text on canvas',t.text,t.background],['text on surface',t.text,t.surface],['secondary text on canvas',t.muted,t.background],['secondary text on surface',t.muted,t.surface],['secondary text on raised',t.muted,t.raised],['accent text on raised',t.accentText,t.raised],['success',t.success,t.successSurface],['warning',t.warning,t.warningSurface],['danger',t.danger,t.dangerSurface],['information',t.info,t.infoSurface]];
 for(const [name,fg,bg] of pairs){const value=ratio(fg,bg);contrast.push({theme,name,foreground:fg,background:bg,ratio:+value.toFixed(2),minimum:4.5,passed:value>=4.5});}
 for(const surface of ['background','surface','raised']){
  const value=ratio(t.border,t[surface]);contrast.push({theme,name:'control boundary on '+surface,foreground:t.border,background:t[surface],ratio:+value.toFixed(2),minimum:3,passed:value>=3});
 }
}
check('All intended text and control contrast pairs pass',contrast.every(c=>c.passed));
// Vertices from the filled master path, including the translated second line.
const vertices=[];let x=0,y=0;
for(const m of brand.mark.path.matchAll(/([MLHVZ])([^MLHVZ]*)/g)){
 const n=m[2].trim().split(/[ ,]+/).filter(Boolean).map(Number);
 if(m[1]==='M'||m[1]==='L'){[x,y]=n;}else if(m[1]==='H')x=n[0];else if(m[1]==='V')y=n[0];else continue;
 vertices.push([x,y],[x,y+brand.mark.secondOffsetY]);
}
const radialDp=Math.max(...vertices.map(([x,y])=>Math.hypot(x-512,y-512)*brand.mark.adaptiveScale*108/1024));
check('Adaptive mark stays inside the central 66 dp safe circle',radialDp<=33,{maximumRadiusDp:+radialDp.toFixed(2),safeRadiusDp:33});
check('Android foreground is at least 48 dp wide',(844-180)*brand.mark.adaptiveScale*108/1024>=48);
const catalog=JSON.parse(await fs.readFile(path.join(root,'icons/ios/AppIcon.appiconset/Contents.json'),'utf8'));
for(const image of catalog.images)check('Catalog references '+image.filename,!!(await fs.stat(path.join(root,'icons/ios/AppIcon.appiconset',image.filename))));
const web=JSON.parse(await fs.readFile(path.join(root,'icons/web/site.webmanifest'),'utf8'));
for(const image of web.icons)check('Web manifest references '+image.src,!!(await fs.stat(path.join(root,'icons/web',image.src))));
const ico=await fs.readFile(path.join(root,'icons/web/favicon.ico'));
check('ICO includes 16, 32 and 48 px entries',ico.readUInt16LE(2)===1&&ico.readUInt16LE(4)===3&&ico[6]===16&&ico[22]===32&&ico[38]===48);
// Self-contained proofs of the crops and micro sizes.
const paths='<path d="'+brand.mark.path+'"/><path transform="translate(0 170)" d="'+brand.mark.path+'"/>';
function crop(x,y,type,bg='#171A19',fg='#DBFF00'){
 const mask=type==='circle'?'<circle cx="128" cy="128" r="128"/>':'<rect width="256" height="256" rx="56"/>';
 return '<g transform="translate('+x+' '+y+')"><defs><clipPath id="'+type+x+'">'+mask+'</clipPath></defs><g clip-path="url(#'+type+x+')"><rect width="256" height="256" fill="'+bg+'"/><g transform="scale(.25)" fill="'+fg+'">'+paths+'</g></g></g>';
}
let proof='<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="560" viewBox="0 0 1200 560"><rect width="1200" height="560" fill="#101311"/><g font-family="sans-serif" fill="#F3F5ED"><text x="48" y="55" font-size="26">PACEMAKER / ICON PROOF</text>';
proof+=crop(48,96,'round')+crop(340,96,'circle')+crop(632,96,'round','#E4E6DC','#344039')+crop(924,96,'round','#000000','#FFFFFF');
for(const [x,label] of [[48,'Rounded square'],[340,'Circle'],[632,'Themed preview'],[924,'Tinted preview']])proof+='<text x="'+x+'" y="382" font-size="15">'+label+'</text>';
let pos=48;
for(const size of [16,24,32,48,64]){
 proof+='<g transform="translate('+pos+' 432)"><rect width="'+size+'" height="'+size+'" fill="#171A19"/><g transform="scale('+(size/1024)+')" fill="#DBFF00">'+paths+'</g><text y="'+(size+22)+'" font-size="12">'+size+' px</text></g>';pos+=size+48;
}
proof+='</g></svg>';
await fs.writeFile(path.join(root,'previews/icon-proof.svg'),proof);
await sharp(Buffer.from(proof)).png().toFile(path.join(root,'previews/icon-proof.png'));
// Actual platform resource compilation, when local SDKs are present.
const temp=await fs.mkdtemp(path.join(os.tmpdir(),'pacemaker-design-'));
function command(bin,args){const r=spawnSync(bin,args,{encoding:'utf8',timeout:120000});return{passed:r.status===0,exitCode:r.status,stdout:r.stdout,stderr:r.stderr,error:r.error?.message};}
try {
 const sdk=process.env.ANDROID_SDK_ROOT||process.env.ANDROID_HOME||path.join(os.homedir(),'Library/Android/sdk');
 try {
  const versions=(await fs.readdir(path.join(sdk,'build-tools'))).sort((a,b)=>a.localeCompare(b,undefined,{numeric:true}));
  const platforms=(await fs.readdir(path.join(sdk,'platforms'))).filter(s=>/^android-\d+$/.test(s)).sort((a,b)=>a.localeCompare(b,undefined,{numeric:true}));
  const aapt=path.join(sdk,'build-tools',versions.at(-1),'aapt2'),jar=path.join(sdk,'platforms',platforms.at(-1),'android.jar');
  const manifestFile=path.join(temp,'AndroidManifest.xml');
  await fs.writeFile(manifestFile,'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="io.sellmair.pacemaker.design"><uses-sdk android:minSdkVersion="21" android:targetSdkVersion="36"/><application android:label="Pacemaker" android:icon="@mipmap/pacemaker_launcher" android:roundIcon="@mipmap/pacemaker_launcher_round"/></manifest>');
  native.androidCompile=command(aapt,['compile','--dir',path.join(root,'icons/android/res'),'-o',path.join(temp,'compiled.zip')]);
  native.androidLink=native.androidCompile.passed?command(aapt,['link','-I',jar,'--manifest',manifestFile,'-o',path.join(temp,'icons.apk'),path.join(temp,'compiled.zip')]):{passed:false,skipped:'Compile failed'};
  native.androidSdk={buildTools:versions.at(-1),platform:platforms.at(-1)};
 }catch(e){native.androidCompile={skipped:true,reason:String(e)};}
 if(process.platform==='darwin'){
  const find=command('xcrun',['--find','actool']);
  if(find.passed){
   const assets=path.join(temp,'Assets.xcassets'),out=path.join(temp,'ios-output');
   await fs.mkdir(assets,{recursive:true});await fs.mkdir(out,{recursive:true});
   await fs.cp(path.join(root,'icons/ios/AppIcon.appiconset'),path.join(assets,'AppIcon.appiconset'),{recursive:true});
   await fs.writeFile(path.join(assets,'Contents.json'),JSON.stringify({info:{author:'xcode',version:1}}));
   native.ios=command('xcrun',['actool','--compile',out,'--platform','iphonesimulator','--minimum-deployment-target','15.0','--target-device','iphone','--target-device','ipad','--app-icon','AppIcon','--output-partial-info-plist',path.join(temp,'asset-info.plist'),assets]);
  }else native.ios={skipped:true,reason:'actool unavailable'};
 }
}finally{await fs.rm(temp,{recursive:true,force:true});}
await fs.writeFile(path.join(root,'validation/native-compilers.json'),JSON.stringify(native,null,2)+'\n');
const nativeFailures=Object.values(native).filter(v=>v.passed===false);
check('Available native icon compilers passed',nativeFailures.length===0);
const report={passed:checks.every(c=>c.passed),imagesChecked:outputs.images.length,checks,contrast,nativeSummary:Object.fromEntries(Object.entries(native).map(([k,v])=>[k,{passed:v.passed,skipped:v.skipped,reason:v.reason}]))};
await fs.writeFile(path.join(root,'validation/report.json'),JSON.stringify(report,null,2)+'\n');
const failed=checks.filter(c=>!c.passed);
console.log(JSON.stringify({passed:report.passed,images:outputs.images.length,checks:checks.length,contrastFailures:contrast.filter(c=>!c.passed),failures:failed,native:report.nativeSummary},null,2));
if(!report.passed)process.exitCode=1;
