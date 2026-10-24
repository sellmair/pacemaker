const $=s=>document.querySelector(s);
const $$=s=>[...document.querySelectorAll(s)];
function setIcon(el,name) {
  if(!el) return;
  el.innerHTML='<g fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">'+window.PACEMAKER_ICONS[name]+'</g>';
  el.dataset.icon=name;
}
$$('[data-icon]').forEach(el=>setIcon(el,el.dataset.icon));
let state='within',limit=150,voice=true,source='auto';
function paintLive() {
  const lost=state==='offline', above=state==='above';
  const hr=lost?'—':limit+(above?7:-8);
  const status=lost?'Signal lost':above?'Above your limit':'Within your limit';
  const color=lost?'var(--pm-warning)':above?'var(--pm-danger)':'var(--pm-success)';
  $('#heart-rate').textContent=hr;
  $('#self-value').textContent=hr;
  $('#watch-value').textContent=hr;
  $('#watch-value').style.color=lost?'#FFB86B':above?'#FF8E84':'#DBFF00';
  $('#status-message span').textContent=status;
  $('#status-message').style.color=color;
  setIcon($('#status-message svg'),lost?'disconnect':above?'warning':'check');
  $('#self-note').textContent=lost?'No current reading':above?'7 above limit':'8 below limit';
  $('#self-note').style.color=lost||above?color:'var(--pm-muted)';
  $('#live-tag').innerHTML=lost?'Offline':'<i class="dot"></i>Live';
  $('#live-tag').style.color=lost?'var(--pm-warning)':'var(--pm-success)';
  const scaleMax=Math.max(180,limit+20);
  $('#scale-max').textContent=scaleMax+' bpm';
  $('#meter-fill').style.width=lost?'0%':Math.max(0,Math.min(100,(hr-60)/(scaleMax-60)*100))+'%';
  $('#meter-fill').style.background=above?'var(--pm-danger)':'var(--pm-accent-text)';
  $('#meter-limit').style.left=Math.max(0,Math.min(100,(limit-60)/(scaleMax-60)*100))+'%';
  $('#watch-group').textContent=status+(lost?'':' · 3 together');
  $('#watch-group').style.color=lost?'#FFB86B':above?'#FF8E84':'#A3E8BC';
  $$('[data-state]').forEach(b=>b.setAttribute('aria-pressed',b.dataset.state===state));
  const device=source==='watch'?'Wear OS watch':'Polar H10';
  $('#live-source').textContent=lost?'No live signal · reconnect in Setup':'Source: '+device+' · connected';
}
$$('[data-state]').forEach(b=>b.addEventListener('click',()=>{state=b.dataset.state;paintLive();}));
$$('[data-theme-choice]').forEach(b=>b.addEventListener('click',()=>{
  const theme=b.dataset.themeChoice;
  $$('.phone,.component-area').forEach(el=>el.dataset.theme=theme);
  $$('[data-theme-choice]').forEach(el=>el.setAttribute('aria-pressed',el===b));
}));
function setVoice(value) {
  voice=value;
  $('#voice-toggle').setAttribute('aria-pressed',voice);
  $('#voice-toggle span').textContent='Voice alerts '+(voice?'on':'off');
  $('#voice-setting').checked=voice;
  setIcon($('#voice-toggle svg'),voice?'audio':'audio-off');
}
$('#voice-toggle').addEventListener('click',()=>setVoice(!voice));
$('#voice-setting').addEventListener('change',e=>setVoice(e.target.checked));
function setLimit(value) {
  limit=Math.min(200,Math.max(80,Number(value)));
  $$('[data-limit]').forEach(el=>el.textContent=limit);
  $('#limit-value').textContent=limit;
  $('#limit-range').value=limit;
  $('#limit-range').setAttribute('aria-valuetext',limit+' beats per minute');
  $('#limit-down').disabled=limit===80;
  $('#limit-up').disabled=limit===200;
  $('#save-message').textContent='';
  paintLive();
}
$('#limit-range').addEventListener('input',e=>setLimit(e.target.value));
$('#limit-down').addEventListener('click',()=>setLimit(limit-1));
$('#limit-up').addEventListener('click',()=>setLimit(limit+1));
function setSource(value) {
  source=value;
  $$('[data-source]').forEach(b=>b.setAttribute('aria-pressed',b.dataset.source===value));
  const watch=value==='watch';
  $('#sensor-card').classList.toggle('selected',!watch);
  $('#watch-card').classList.toggle('selected',watch);
  $('#sensor-status').textContent=watch?'Nearby · available':'Connected · live source';
  $('#watch-status').textContent=watch?'Connected · live source':'Nearby · available';
  $('#source-note').textContent=value==='auto'?'Automatic · using Polar H10':watch?'Selected · Wear OS watch':'Selected · Polar H10';
  paintLive();
}
$$('[data-source]').forEach(b=>b.addEventListener('click',()=>setSource(b.dataset.source)));
$$('[data-select-source]').forEach(b=>b.addEventListener('click',()=>setSource(b.dataset.selectSource)));
$('#save-settings').addEventListener('click',()=>{
  const name=$('#name').value.trim();
  if(!name) {$('#save-message').textContent='Enter a display name.';$('#save-message').style.color='var(--pm-danger)';$('#name').setAttribute('aria-invalid','true');$('#name').focus();return;}
  $('#name').removeAttribute('aria-invalid');
  $('#save-message').style.color='var(--pm-success)';
  $('#save-message').textContent='Saved in this preview.';
  const initials=name.split(/\s+/).slice(0,2).map(w=>w[0]).join('').toUpperCase();
  $('#live-screen .avatar').textContent=initials;
});
$$('[data-demo]').forEach(b=>b.addEventListener('click',()=>$('#component-feedback').textContent=b.dataset.demo));
paintLive();
