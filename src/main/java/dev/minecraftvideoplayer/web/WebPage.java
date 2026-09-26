package dev.minecraftvideoplayer.web;

public final class WebPage {
    private WebPage() {}
    public static final String HTML = """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Minecraft Video Player</title>
<style>
body{margin:0;background:#0b0d12;color:#f4f6f8;font-family:system-ui,sans-serif}
main{max-width:1100px;margin:auto;padding:24px}
.card{background:#151922;border:1px solid #292f3d;border-radius:14px;padding:18px;margin-bottom:16px}
input,button{padding:11px;border-radius:9px;border:1px solid #343b4a;background:#0e1118;color:#fff}
input{width:65%}button{cursor:pointer}
video{width:100%;max-height:65vh;background:#000;border-radius:12px}
.row{display:flex;gap:10px;flex-wrap:wrap}
small{color:#9ca6b5}
</style>
</head>
<body>
<main>
<div class="card">
<h1>Video Center</h1>
<small>Java + Bedrock/PE compatible web player</small>
</div>
<div class="card">
<div class="row">
<input id="url" placeholder="YouTube, MP4 or HLS URL">
<button onclick="loadVideo()">Load</button>
<button onclick="newRoom()">New Room</button>
</div>
</div>
<div class="card">
<video id="player" controls playsinline></video>
<p id="status">Ready.</p>
</div>
</main>
<script>
const video=document.getElementById('player');
const statusEl=document.getElementById('status');
let ws;
function connect(){
  const proto=location.protocol==='https:'?'wss':'ws';
  ws=new WebSocket(proto+'://'+location.host+'/ws');
  ws.onopen=()=>statusEl.textContent='Connected.';
  ws.onclose=()=>setTimeout(connect,1500);
  ws.onmessage=e=>{
    try{const m=JSON.parse(e.data); if(m.type==='sync'&&Math.abs(video.currentTime-m.time)>1) video.currentTime=m.time;}catch{}
  };
}
function loadVideo(){
  const url=document.getElementById('url').value.trim();
  if(!url)return;
  if(/\.m3u8($|\?)/i.test(url)){video.src=url;video.load();return;}
  if(/\.(mp4|webm|ogg)($|\?)/i.test(url)){video.src=url;video.load();return;}
  statusEl.textContent='For YouTube, enter the URL in the player integration layer.';
}
function newRoom(){
  fetch('/api/room/new').then(r=>r.json()).then(x=>statusEl.textContent='Room: '+x.room);
}
video.addEventListener('play',()=>ws&&ws.readyState===1&&ws.send(JSON.stringify({type:'play',time:video.currentTime})));
video.addEventListener('pause',()=>ws&&ws.readyState===1&&ws.send(JSON.stringify({type:'pause',time:video.currentTime})));
connect();
</script>
</body>
</html>
""";
}