package dev.minecraftvideoplayer.web;
public final class WebPage {
 private WebPage(){}
 public static final String HTML = """
<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Minecraft Video Center</title>
<style>:root{color-scheme:dark}body{margin:0;background:#090b10;color:#f3f4f6;font:14px system-ui,sans-serif}main{max-width:1180px;margin:auto;padding:24px}.box{background:#121620;border:1px solid #272e3b;border-radius:16px;padding:18px;margin-bottom:14px}.row{display:flex;gap:8px;flex-wrap:wrap}input,button,select{background:#0b0e14;color:#fff;border:1px solid #303746;border-radius:9px;padding:11px}input{flex:1;min-width:180px}button{cursor:pointer}.player{position:relative;aspect-ratio:16/9;background:#000;border-radius:12px;overflow:hidden}iframe{width:100%;height:100%;border:0}.grid{display:grid;grid-template-columns:2fr 1fr;gap:14px}.result{padding:11px;border-top:1px solid #272e3b;cursor:pointer}.result:hover{background:#1a1f2a}.muted{color:#9ca6b5}.controls{display:flex;gap:6px;flex-wrap:wrap;margin-top:10px}@media(max-width:800px){.grid{grid-template-columns:1fr}}</style></head>
<body><main>
<div class="box"><h1>Video Center</h1><span class="muted">Minecraft 1.26 | Java + Bedrock/PE | Web Player</span></div>
<div class="box"><div class="row"><input id="url" placeholder="YouTube URL or video ID"><button onclick="playInput()">Play</button><input id="q" placeholder="Search YouTube"><button onclick="searchYT()">Search</button><button onclick="newRoom()">New Room</button><input id="roomInput" placeholder="Room ID"><button onclick="joinRoom()">Join</button></div></div>
<div class="grid"><div class="box"><div class="player" id="frame"></div><div class="controls"><button onclick="sendState(false)">Pause</button><button onclick="sendState(true)">Play</button><button onclick="newRoom()">New Room</button></div><p id="status" class="muted">Ready.</p></div>
<div class="box"><h3>Search Results</h3><div id="results" class="muted">Search to begin.</div><h3>Watch Room</h3><div id="room" class="muted">No room.</div></div></div>
</main><script>
let ws,room=new URLSearchParams(location.search).get('room'),currentVideo='';
const statusEl=document.getElementById('status'),frame=document.getElementById('frame');
function idOf(s){s=s.trim();let m=s.match(/[?&]v=([A-Za-z0-9_-]{11})/)||s.match(/youtu\.be\/([A-Za-z0-9_-]{11})/)||s.match(/youtube\.com\/(?:shorts|embed)\/([A-Za-z0-9_-]{11})/);return m?m[1]:(/^[A-Za-z0-9_-]{11}$/.test(s)?s:'')}
function player(id){currentVideo=id;frame.innerHTML='<iframe src="https://www.youtube.com/embed/'+id+'?enablejsapi=1&autoplay=1" allow="autoplay;encrypted-media;picture-in-picture" allowfullscreen></iframe>';statusEl.textContent='Playing '+id;sendState(true)}
function playInput(){let id=idOf(document.getElementById('url').value);if(id)player(id);else statusEl.textContent='Invalid YouTube URL or video ID.'}
function sendState(playing){if(!ws||ws.readyState!==1||!room)return;ws.send(JSON.stringify({type:'state',room,video:currentVideo,time:0,playing}))}
function connect(){ws=new WebSocket((location.protocol==='https:'?'wss':'ws')+'://'+location.host+'/ws');ws.onopen=()=>{statusEl.textContent='Connected';if(room)ws.send(JSON.stringify({type:'join',room}))};ws.onmessage=e=>{let m=JSON.parse(e.data);if(m.type==='state'&&m.video&&m.video!==currentVideo)playerRemote(m.video,m.playing);};ws.onclose=()=>setTimeout(connect,1500)}
function playerRemote(id,playing){currentVideo=id;frame.innerHTML='<iframe src="https://www.youtube.com/embed/'+id+'?enablejsapi=1&autoplay='+(playing?1:0)+'" allow="autoplay;encrypted-media;picture-in-picture" allowfullscreen></iframe>';statusEl.textContent='Room synchronized: '+id}
function searchYT(){let q=document.getElementById('q').value.trim();if(!q)return;fetch('/api/search?q='+encodeURIComponent(q)).then(r=>r.json()).then(x=>{let box=document.getElementById('results');box.innerHTML='';(x.items||[]).forEach(v=>{let d=document.createElement('div');d.className='result';d.textContent=v.title+' — '+v.channel;d.onclick=()=>player(v.id);box.appendChild(d)})}).catch(e=>document.getElementById('results').textContent='Search failed: '+e)}
function newRoom(){fetch('/api/room/new').then(r=>r.json()).then(x=>{room=x.room;document.getElementById('room').textContent='Room: '+room;history.replaceState(null,'','?room='+room);connect()})}
function joinRoom(){let id=document.getElementById('roomInput').value.trim();if(id){room=id;document.getElementById('room').textContent='Room: '+room;history.replaceState(null,'','?room='+room);connect()}}
connect();if(room)document.getElementById('room').textContent='Room: '+room;
</script></body></html>""";
}