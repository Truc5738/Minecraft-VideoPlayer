package dev.minecraftvideoplayer.web;
public final class WebPage {
 private WebPage(){}
 public static final String HTML = """
<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Minecraft Video Center</title>
<style>:root{color-scheme:dark}body{margin:0;background:#090b10;color:#f3f4f6;font:14px system-ui,sans-serif}main{max-width:1180px;margin:auto;padding:24px}.box{background:#121620;border:1px solid #272e3b;border-radius:16px;padding:18px;margin-bottom:14px}.row{display:flex;gap:8px;flex-wrap:wrap}input,button{background:#0b0e14;color:#fff;border:1px solid #303746;border-radius:9px;padding:11px}input{flex:1;min-width:220px}button{cursor:pointer}iframe{width:100%;aspect-ratio:16/9;border:0;border-radius:12px;background:#000}.grid{display:grid;grid-template-columns:2fr 1fr;gap:14px}.result{padding:11px;border-top:1px solid #272e3b;cursor:pointer}.result:hover{background:#1a1f2a}.muted{color:#9ca6b5}@media(max-width:800px){.grid{grid-template-columns:1fr}}</style></head>
<body><main><div class="box"><h1>Video Center</h1><span class="muted">Minecraft 1.26 | Java + Bedrock/PE</span></div>
<div class="box"><div class="row"><input id="url" placeholder="YouTube URL or video ID"><button onclick="playInput()">Play</button><input id="q" placeholder="Search YouTube"><button onclick="searchYT()">Search</button><button onclick="newRoom()">New Room</button></div></div>
<div class="grid"><div class="box"><div id="frame"></div><p id="status" class="muted">Ready.</p></div><div class="box"><h3>Search Results</h3><div id="results" class="muted">Search to begin.</div><h3>Room</h3><div id="room" class="muted">No room.</div></div></div>
<script>
const statusEl=document.getElementById('status'),frame=document.getElementById('frame'),results=document.getElementById('results');
function idOf(s){s=s.trim();let m=s.match(/[?&]v=([A-Za-z0-9_-]{11})/)||s.match(/youtu\.be\/([A-Za-z0-9_-]{11})/)||s.match(/youtube\.com\/(?:shorts|embed)\/([A-Za-z0-9_-]{11})/);return m?m[1]:(/^[A-Za-z0-9_-]{11}$/.test(s)?s:'')}
function play(id){frame.innerHTML='<iframe src="https://www.youtube.com/embed/'+id+'?autoplay=1&enablejsapi=1" allow="autoplay;encrypted-media;picture-in-picture" allowfullscreen></iframe>';statusEl.textContent='Playing: '+id}
function playInput(){let id=idOf(document.getElementById('url').value);if(id)play(id);else statusEl.textContent='Invalid YouTube URL or video ID.'}
function searchYT(){let q=document.getElementById('q').value.trim();if(!q)return;fetch('/api/search?q='+encodeURIComponent(q)).then(r=>r.json()).then(x=>{results.innerHTML='';(x.items||[]).forEach(v=>{let d=document.createElement('div');d.className='result';d.textContent=v.title+' — '+v.channel;d.onclick=()=>play(v.id);results.appendChild(d)})}).catch(e=>results.textContent='Search failed: '+e)}
function newRoom(){fetch('/api/room/new').then(r=>r.json()).then(x=>{document.getElementById('room').textContent='Room: '+x.room;history.replaceState(null,'','?room='+x.room)})}
const p=new URLSearchParams(location.search);if(p.get('video'))play(idOf(p.get('video')));if(p.get('room'))document.getElementById('room').textContent='Room: '+p.get('room');
</script></body></html>""";
