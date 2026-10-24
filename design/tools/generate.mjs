import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const sharp = require('sharp');
const fontkit = require('fontkit');
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const brand = JSON.parse(await fs.readFile(path.join(root, 'src/brand.json'), 'utf8'));
const { acid, charcoal, chalk } = brand.palette;
const manifest = [];
async function put(file, data) {
  await fs.mkdir(path.dirname(path.join(root, file)), {recursive: true});
  await fs.writeFile(path.join(root, file), data);
}
function svg(body, vb='0 0 1024 1024', title='Pacemaker — shared rhythm') {
  return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="'+vb+'" role="img" aria-label="'+title+'">'+body+'</svg>\n';
}
const paths = '<path d="'+brand.mark.path+'"/><path transform="translate(0 170)" d="'+brand.mark.path+'"/>';
function mark(color=acid, scale=1) {
  return '<g fill="'+color+'" transform="translate(512 512) scale('+scale+') translate(-512 -512)">'+paths+'</g>';
}
function icon({color=acid, background=charcoal, scale=1, shape='square'}={}) {
  const bg = background ? shape==='circle' ? '<circle cx="512" cy="512" r="512" fill="'+background+'"/>' :
    '<rect width="1024" height="1024" '+(shape==='round'?'rx="196" ':'')+'fill="'+background+'"/>' : '';
  return svg(bg+mark(color,scale));
}
async function png(file, content, size, mode='rgb') {
  let img=sharp(Buffer.from(content)).resize(size,size).toColourspace('srgb');
  img=mode==='rgb' ? img.removeAlpha() : img.ensureAlpha();
  await put(file,await img.png({compressionLevel:9}).toBuffer());
  manifest.push({file,width:size,height:size,mode});
}
await put('brand/app-icon.svg',icon());
await put('brand/app-icon-acid.svg',icon({background:acid,color:charcoal}));
await put('brand/mark-acid.svg',svg('<g fill="'+acid+'">'+paths+'</g>','180 308 664 404'));
await put('brand/mark-charcoal.svg',svg('<g fill="'+charcoal+'">'+paths+'</g>','180 308 664 404'));
await put('brand/mark-chalk.svg',svg('<g fill="'+chalk+'">'+paths+'</g>','180 308 664 404'));
await put('brand/mark-currentcolor.svg',svg('<g fill="currentColor">'+paths+'</g>','180 308 664 404'));
await png('brand/app-icon-1024.png',icon(),1024);
await png('brand/app-icon-2048.png',icon(),2048);
await png('brand/mark-transparent-1024.png',icon({background:null}),1024,'rgba');

// Path-only wordmarks: no font dependency in the SVG deliverables.
const font=fontkit.openSync(path.join(root,'fonts/BarlowCondensed-SemiBold.ttf'));
const run=font.layout('PACEMAKER');
let x=0;
const outlines=run.glyphs.map((glyph,i)=>{
  const d='<path transform="translate('+x+' 0)" d="'+glyph.path.toSVG()+'"/>';
  x+=run.positions[i].xAdvance+24;
  return d;
}).join('');
const height=font.capHeight;
const s=122/height, wordWidth=x*s;
for (const [suffix,color] of [['chalk',chalk],['charcoal',charcoal],['acid',acid]]) {
  const word='<g fill="'+color+'" transform="translate(0 122) scale('+s+' '+(-s)+')">'+outlines+'</g>';
  await put('brand/wordmark-'+suffix+'.svg',svg(word,'0 0 '+wordWidth.toFixed(2)+' 128','Pacemaker'));
  const symbol='<g transform="translate(0 12) scale(.25) translate(-180 -308)" fill="'+color+'">'+paths+'</g>';
  await put('brand/lockup-'+suffix+'.svg',svg(symbol+'<g transform="translate(206 0)">'+word+'</g>','0 0 '+(wordWidth+206).toFixed(2)+' 128','Pacemaker'));
}
await put('brand/rhythm-pattern.svg',svg('<defs><pattern id="rhythm" width="480" height="320" patternUnits="userSpaceOnUse"><g transform="scale(.4) translate(-120 -200)" fill="'+acid+'">'+paths+'</g></pattern></defs><rect width="1440" height="960" fill="'+charcoal+'"/><rect width="1440" height="960" fill="url(#rhythm)" opacity=".12"/>','0 0 1440 960'));

// iOS / iPadOS single-size catalog: default opaque, dark transparent, tinted grayscale.
const catalog='icons/ios/AppIcon.appiconset/';
await png(catalog+'AppIcon-1024.png',icon(),1024);
await png(catalog+'AppIcon-Dark-1024.png',icon({background:null}),1024,'rgba');
await png(catalog+'AppIcon-Tinted-1024.png',icon({background:'#000000',color:'#FFFFFF'}),1024);
await put(catalog+'Contents.json',JSON.stringify({images:[
  {filename:'AppIcon-1024.png',idiom:'universal',platform:'ios',size:'1024x1024'},
  {appearances:[{appearance:'luminosity',value:'dark'}],filename:'AppIcon-Dark-1024.png',idiom:'universal',platform:'ios',size:'1024x1024'},
  {appearances:[{appearance:'luminosity',value:'tinted'}],filename:'AppIcon-Tinted-1024.png',idiom:'universal',platform:'ios',size:'1024x1024'}
],info:{author:'xcode',version:1}},null,2)+'\n');
for (const size of [20,29,40,58,60,76,80,87,120,152,167,180]) await png('icons/ios/legacy/AppIcon-'+size+'.png',icon(),size);
await put('icons/ios/composer-layers/foreground.svg',icon({background:null}));
await put('icons/ios/composer-layers/background.svg',svg('<rect width="1024" height="1024" fill="'+charcoal+'"/>'));

// Android: artwork scaled into the central safe area of a 108 dp canvas.
const android='icons/android/res/';
const xmlHeader='<?xml version="1.0" encoding="utf-8"?>\n';
const vector=(color,scale=.72)=>xmlHeader+'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="1024" android:viewportHeight="1024">\n  <group android:pivotX="512" android:pivotY="512" android:scaleX="'+scale+'" android:scaleY="'+scale+'">\n    <path android:fillColor="'+color+'" android:pathData="'+brand.mark.path+'"/>\n    <group android:translateY="170"><path android:fillColor="'+color+'" android:pathData="'+brand.mark.path+'"/></group>\n  </group>\n</vector>\n';
await put(android+'drawable/pacemaker_launcher_foreground.xml',vector(acid));
await put(android+'drawable/pacemaker_launcher_monochrome.xml',vector('#FFFFFF'));
await put(android+'drawable/pacemaker_launcher_background.xml',xmlHeader+'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="'+charcoal+'" android:pathData="M0,0H108V108H0Z"/></vector>\n');
for(const v of [26,33]) for(const name of ['pacemaker_launcher','pacemaker_launcher_round']) {
  await put(android+'mipmap-anydpi-v'+v+'/'+name+'.xml',xmlHeader+'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n  <background android:drawable="@drawable/pacemaker_launcher_background"/>\n  <foreground android:drawable="@drawable/pacemaker_launcher_foreground"/>\n'+(v>=33?'  <monochrome android:drawable="@drawable/pacemaker_launcher_monochrome"/>\n':'')+'</adaptive-icon>\n');
}
for (const [density,size] of [['mdpi',48],['hdpi',72],['xhdpi',96],['xxhdpi',144],['xxxhdpi',192]]) {
  await png(android+'mipmap-'+density+'/pacemaker_launcher.png',icon({shape:'round',scale:.9}),size,'rgba');
  await png(android+'mipmap-'+density+'/pacemaker_launcher_round.png',icon({shape:'circle',scale:.9}),size,'rgba');
}
await png('icons/android/play-store-512.png',icon(),512,'rgba');
await png('icons/android/layers/foreground-432.png',icon({background:null,scale:.72}),432,'rgba');
await png('icons/android/layers/monochrome-432.png',icon({background:null,color:'#FFFFFF',scale:.72}),432,'rgba');
await png('icons/android/layers/background-432.png',svg('<rect width="1024" height="1024" fill="'+charcoal+'"/>'),432);
await put('icons/android/layers/foreground.svg',icon({background:null,scale:.72}));
await put('icons/android/AndroidManifest.snippet.xml', '<!-- Add these attributes to your existing application element; do not replace the manifest. -->\n<application xmlns:android="http://schemas.android.com/apk/res/android"\n    android:icon="@mipmap/pacemaker_launcher"\n    android:roundIcon="@mipmap/pacemaker_launcher_round" />\n');

// Web SVGs, favicons, touch icon and PWA assets.
await put('icons/web/icon.svg',icon());
await put('icons/web/favicon.svg',icon());
await put('icons/web/mark.svg',svg('<g fill="currentColor">'+paths+'</g>','180 308 664 404'));
await put('icons/web/mask-icon.svg',svg('<g fill="#000000">'+paths+'</g>'));
for(const size of [16,32,48]) await png('icons/web/favicon-'+size+'.png',icon(),size,'rgba');
for(const size of [192,512]) {
  await png('icons/web/icon-'+size+'.png',icon(),size);
  await png('icons/web/maskable-'+size+'.png',icon({scale:.88}),size);
}
await png('icons/web/apple-touch-icon.png',icon(),180);
// PNG-backed ICO directory with 16, 32 and 48 px entries.
const images=await Promise.all([16,32,48].map(n=>fs.readFile(path.join(root,'icons/web/favicon-'+n+'.png'))));
const icoHeader=Buffer.alloc(6+16*images.length); icoHeader.writeUInt16LE(1,2); icoHeader.writeUInt16LE(images.length,4);
let icoOffset=icoHeader.length;
images.forEach((bytes,i)=>{const entry=6+16*i, size=[16,32,48][i]; icoHeader[entry]=size; icoHeader[entry+1]=size; icoHeader.writeUInt16LE(1,entry+4); icoHeader.writeUInt16LE(32,entry+6); icoHeader.writeUInt32LE(bytes.length,entry+8); icoHeader.writeUInt32LE(icoOffset,entry+12); icoOffset+=bytes.length;});
await put('icons/web/favicon.ico',Buffer.concat([icoHeader,...images]));
await put('icons/web/site.webmanifest',JSON.stringify({name:'Pacemaker',short_name:'Pacemaker',description:'Keep pace. Together.',start_url:'/',display:'standalone',background_color:charcoal,theme_color:charcoal,icons:[192,512].flatMap(size=>[
  {src:'icon-'+size+'.png',sizes:size+'x'+size,type:'image/png',purpose:'any'},
  {src:'maskable-'+size+'.png',sizes:size+'x'+size,type:'image/png',purpose:'maskable'}
])},null,2)+'\n');
await put('icons/web/head-snippet.html','<!-- Copy this directory to /assets/pacemaker and adjust start_url in site.webmanifest for your site. -->\n<link rel="icon" href="/assets/pacemaker/favicon.ico" sizes="any">\n<link rel="icon" href="/assets/pacemaker/favicon.svg" type="image/svg+xml">\n<link rel="apple-touch-icon" href="/assets/pacemaker/apple-touch-icon.png">\n<link rel="mask-icon" href="/assets/pacemaker/mask-icon.svg" color="'+charcoal+'">\n<link rel="manifest" href="/assets/pacemaker/site.webmanifest">\n<meta name="theme-color" content="'+charcoal+'">\n');

// A small purpose-built UI icon family: 24px viewbox, 2px strokes, no fonts.
const ui={
'rhythm':'<path d="M2 9h4l4-4 5 6 3-2h4M2 15h4l4-4 5 6 3-2h4"/>',
'group':'<circle cx="9" cy="8" r="3"/><path d="M3 20v-2a6 6 0 0 1 12 0v2M16 5a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 4v2"/>',
'settings':'<path d="M4 7h16M4 17h16"/><rect x="7" y="4" width="4" height="6" fill="currentColor" stroke="none"/><rect x="14" y="14" width="4" height="6" fill="currentColor" stroke="none"/>',
'sensor':'<rect x="6" y="7" width="12" height="10" rx="2"/><path d="M2 9v6M22 9v6M9 12h6"/>',
'watch':'<rect x="6" y="6" width="12" height="12" rx="3"/><path d="M9 2h6l1 4M9 22h6l1-4M8 6l1-4M8 18l1 4M12 9v3l2 1"/>',
'bluetooth':'<path d="M12 2v20l7-6-14-8M5 16l14-8-7-6"/>',
'audio':'<path d="M4 9h4l4-4v14l-4-4H4ZM16 8a6 6 0 0 1 0 8M19 5a10 10 0 0 1 0 14"/>',
'audio-off':'<path d="M4 9h4l4-4v14l-4-4H4ZM17 9l5 6M22 9l-5 6"/>',
'check':'<path d="m5 12 4 4L19 6"/>',
'warning':'<path d="M12 3 2 21h20ZM12 9v5"/><path d="M12 17v1"/>',
'disconnect':'<path d="m3 3 18 18M8 6l3-3a5 5 0 0 1 7 7l-1 1M7 13l-1 1a4 4 0 0 0 6 6l2-2"/>',
'arrow-right':'<path d="M3 12h18m-7-7 7 7-7 7"/>',
'arrow-left':'<path d="M21 12H3m7-7-7 7 7 7"/>',
'plus':'<path d="M12 4v16M4 12h16"/>',
'minus':'<path d="M4 12h16"/>',
'close':'<path d="m5 5 14 14M19 5 5 19"/>',
'limit':'<path d="M3 7h18M3 17h18M8 4v6M16 14v6"/>',
'search':'<circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/>',
'refresh':'<path d="M20 7a9 9 0 1 0 1 8M20 2v6h-6"/>',
'info':'<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7v1"/>'
};
let symbols='';
for(const [name,body] of Object.entries(ui)) {
  const attributes='fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';
  await put('icons/ui/'+name+'.svg',svg('<g '+attributes+'>'+body+'</g>','0 0 24 24',name.replaceAll('-',' ')));
  symbols+='<symbol id="'+name+'" viewBox="0 0 24 24"><g '+attributes+'>'+body+'</g></symbol>';
}
await put('icons/ui/sprite.svg','<svg xmlns="http://www.w3.org/2000/svg">'+symbols+'</svg>\n');
await put('icons/ui/inline.js','window.PACEMAKER_ICONS='+JSON.stringify(ui)+';\n');

// Portable theme tokens and CSS: these are samples to adopt, not app wiring.
await put('tokens/tokens.json',JSON.stringify(brand,null,2)+'\n');
const kebab=x=>x.replace(/[A-Z]/g,c=>'-'+c.toLowerCase());
let css='/* Generated from src/brand.json by tools/generate.mjs. */\n@font-face{font-family:Inter;src:url("../fonts/Inter-Variable.ttf") format("truetype");font-weight:100 900;font-style:normal;font-display:swap}\n@font-face{font-family:"Barlow Condensed";src:url("../fonts/BarlowCondensed-SemiBold.ttf") format("truetype");font-weight:600;font-style:normal;font-display:swap}\n';
for(const [theme,values] of Object.entries(brand.themes)) {
  css+=(theme==='dark'?':root, [data-theme="dark"]':'[data-theme="light"]')+'{\n';
  css+=Object.entries(values).map(([k,v])=>'  --pm-'+kebab(k)+': '+v+';').join('\n')+'\n  color-scheme: '+theme+';\n}\n';
}
css+=':root{--pm-font-body:Inter,system-ui,sans-serif;--pm-font-display:"Barlow Condensed",Impact,sans-serif;--pm-radius-control:8px;--pm-radius-card:12px;--pm-radius-sheet:20px;--pm-duration:180ms;--pm-easing:cubic-bezier(.2,0,0,1);}\n';
css+='@media(prefers-reduced-motion:reduce){:root{--pm-duration:0ms}}\n';
await put('tokens/tokens.css',css);
for(const [theme,values] of Object.entries(brand.themes)) {
  await put('tokens/android/'+(theme==='dark'?'values-night':'values')+'/colors.xml',xmlHeader+'<resources>\n'+Object.entries(values).map(([k,v])=>'  <color name="pm_'+kebab(k).replaceAll('-','_')+'">'+v+'</color>').join('\n')+'\n</resources>\n');
}
await put('tokens/material3-mapping.json',JSON.stringify({primary:'primary',onPrimary:'onPrimary',primaryContainer:'raised',onPrimaryContainer:'accentText',secondary:'text',onSecondary:'background',secondaryContainer:'raised',onSecondaryContainer:'text',background:'background',onBackground:'text',surface:'surface',onSurface:'text',surfaceVariant:'raised',onSurfaceVariant:'muted',outline:'border',error:'danger',onError:'background',errorContainer:'dangerSurface',onErrorContainer:'danger'},null,2)+'\n');
const fontSources=[
{file:'BarlowCondensed-SemiBold.ttf',url:'https://raw.githubusercontent.com/google/fonts/main/ofl/barlowcondensed/BarlowCondensed-SemiBold.ttf',license:'BarlowCondensed-OFL.txt'},
{file:'Inter-Variable.ttf',url:'https://raw.githubusercontent.com/google/fonts/main/ofl/inter/Inter%5Bopsz%2Cwght%5D.ttf',license:'Inter-OFL.txt'}
];
await put('fonts/SOURCES.json',JSON.stringify({retrieved:'2026-10-24',fonts:fontSources},null,2)+'\n');
await put('exports.json',JSON.stringify({version:1,source:'src/brand.json',images:manifest},null,2)+'\n');
console.log('Generated '+manifest.length+' raster assets, SVG logos, Android resources, iOS catalog, web assets, UI icons and theme tokens.');
