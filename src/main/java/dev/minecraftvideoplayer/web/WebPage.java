package dev.minecraftvideoplayer.web;

public final class WebPage {
    private WebPage() {}

    public static final String HTML = """
<!doctype html>
<html>
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Minecraft Video Center</title>
<style>
:root{color-scheme:dark}*{box-sizing:border-box}body{margin:0;background:#090b10;color:#f3f4f6;font:14px system-ui,sans-serif}
main{max-width:1280px;margin:auto;padding:18px}.box{background:#121620;border:1px solid #272e3b;border-radius:14px;padding:16px;margin-bottom:12px}
h1,h2,h3{margin:0 0 10px}.muted{color:#9ca6b5}.row{display:flex;gap:7px;flex-wrap:wrap}
input,button{background:#0b0e14;color:#fff;border:1px solid #303746;border-radius:8px;padding:10px}input{flex:1;min-width:170px}button{cursor:pointer}
.grid{display:grid;grid-template-columns:minmax(0,2fr) minmax(280px,1fr);gap:12px}.player{position:relative;aspect-ratio:16/9;background:#000;border-radius:10px;overflow:hidden}
iframe,#nativeVideo{width:100%;height:100%;border:0}.controls{display:flex;gap:6px;flex-wrap:wrap;margin-top:9px}
.result,.item{padding:9px;border-top:1px solid #272e3b;cursor:pointer}.result:hover,.item:hover{background:#1a1f2a}
.thumb{width:110px;height:62px;object-fit:cover;border-radius:6px;margin-right:8px;vertical-align:middle}.pill{display:inline-block;border:1px solid #303746;border-radius:999px;padding:3px 8px;margin:2px}
@media(max-width:850px){.grid{grid-template-columns:1fr}.thumb{width:90px;height:50px}}
</style>
</head>
<body><main>
<div class="box"><h1>Video Center</h1><span class="muted">Minecraft 1.26 | Java + Bedrock/PE | Web Player</span></div>
<div class="box"><div class="row">
<input id="url" placeholder="YouTube URL, video ID, MP4 or HLS URL"><button onclick="playInput()">Play</button>
<input id="q" placeholder="Search YouTube"><button onclick="searchYT()">Search</button>
<button onclick="newRoom()">New Room</button><input id="roomInput" placeholder="Room ID"><button onclick="joinRoom()">Join</button>
</div><div id="status" class="muted" style="margin-top:8px">Ready.</div></div>

<div class="grid"><section>
<div class="box"><div class="player" id="frame"></div>
<div class="controls">
<button class="host-control" data-control onclick="playVideo()">Play</button><button class="host-control" data-control onclick="pauseVideo()">Pause</button><button class="host-control" data-control onclick="stopVideo()">Stop</button>
<button class="host-control" data-control onclick="seek(-10)">-10s</button><button class="host-control" data-control onclick="seek(10)">+10s</button><button class="host-control" data-control onclick="nextVideo()">Next</button>
<button class="host-control" data-control onclick="previousVideo()">Previous</button><button onclick="toggleFavorite()">Favorite</button>
<button onclick="addQueue()">Add Queue</button><button onclick="cycleRepeat()">Repeat</button><button onclick="toggleShuffle()">Shuffle</button>
<button onclick="fullscreen()">Fullscreen</button>
</div><div class="row" style="margin-top:9px"><label>Volume <input id="volume" type="range" min="0" max="100" value="80" oninput="setVolume(this.value)"></label></div></div>
<div class="box"><h2>Search Results</h2><div id="results" class="muted">Search to begin.</div></div>
</section>

<aside>
<div class="box"><h2>Watch Room</h2><div id="room" class="muted">No room.</div><div id="viewers" class="muted"></div></div>
<div class="box"><h2>Queue</h2><div id="queue" class="muted">Empty.</div><button onclick="clearQueue()">Clear Queue</button></div>
<div class="box"><h2>Library</h2><div class="row"><button onclick="loadLibrary()">Refresh</button><button onclick="createPlaylist()">New Playlist</button></div>
<div id="library" class="muted" style="margin-top:8px">Loading...</div></div>
</aside></div></main>

<script src="https://www.youtube.com/iframe_api"></script>
<script>
let ws=null,yt=null,currentVideo='',currentType='youtube',room=new URLSearchParams(location.search).get('room'),isHost=false,remoteApplying=false;
const ownerKey='web-'+(localStorage.getItem('mvp-owner')||crypto.randomUUID());localStorage.setItem('mvp-owner',ownerKey);const frame=document.getElementById('frame'),statusEl=document.getElementById('status');
function sendControl(type,extra={}){if(!ws||ws.readyState!==1||!room){statusEl.textContent='Join a room first.';return}ws.send(JSON.stringify(Object.assign({type:type,room:room,owner:ownerKey},extra)))}
function api(path,opts={}){let sep=path.includes('?')?'&':'?';return fetch(path+sep+'owner='+encodeURIComponent(ownerKey),Object.assign({headers:{'Content-Type':'application/json'}},opts)).then(r=>r.json())}
function idOf(s){s=s.trim();let m=s.match(/[?&]v=([A-Za-z0-9_-]{11})/)||s.match(/youtu\\.be/([A-Za-z0-9_-]{11})/)||s.match(/youtube\\.com/(?:shorts|embed)\/([A-Za-z0-9_-]{11})/);return m?m[1]:(/^[A-Za-z0-9_-]{11}$/.test(s)?s:'')}
function onYouTubeIframeAPIReady(){if(currentType==='youtube'&&currentVideo)createYT(currentVideo,true)}
function createYT(id,autoplay){frame.innerHTML='<div id="ytplayer"></div>';yt=new YT.Player('ytplayer',{videoId:id,width:'100%',height:'100%',playerVars:{autoplay:autoplay?1:0,rel:0,playsinline:1},events:{onReady:e=>e.target.setVolume(+document.getElementById('volume').value),onStateChange:e=>{if(e.data===1)broadcast(true);else if(e.data===2||e.data===0)broadcast(false)}}})}
function playMedia(id,autoplay=true){if(room&&!isHost&&!remoteApplying){statusEl.textContent='Host controls playback in this room.';return}currentVideo=id;currentType='youtube';createYT(id,autoplay);statusEl.textContent='Video: '+id;api('/api/history',{method:'POST',body:JSON.stringify({id:id,title:id,channel:''})}).catch(()=>{})}
function playInput(){if(room&&!isHost){statusEl.textContent='Host controls playback in this room.';return}let raw=document.getElementById('url').value.trim(),id=idOf(raw);if(id){playMedia(id,true);return}if(raw.startsWith('http://')||raw.startsWith('https://')){currentType='native';currentVideo=raw;frame.innerHTML='<video id="nativeVideo" controls autoplay></video>';let v=document.getElementById('nativeVideo');v.src=raw;statusEl.textContent='Direct media';return}statusEl.textContent='Invalid video URL or ID.'}
function playVideo(){if(room&&!remoteApplying){sendControl('play');return}if(yt)yt.playVideo();else{let v=document.getElementById('nativeVideo');if(v)v.play()}}
function pauseVideo(){if(room&&!remoteApplying){sendControl('pause');return}if(yt)yt.pauseVideo();else{let v=document.getElementById('nativeVideo');if(v)v.pause()}}
function stopVideo(){if(room&&!remoteApplying){let t=0;sendControl('seek',{time:t});sendControl('pause');if(yt)yt.seekTo(0,true);else{let v=document.getElementById('nativeVideo');if(v)v.currentTime=0}return}if(yt)yt.stopVideo();else{let v=document.getElementById('nativeVideo');if(v){v.pause();v.currentTime=0}}}
function seek(d){let t=yt?Math.max(0,yt.getCurrentTime()+d):Math.max(0,(document.getElementById('nativeVideo')?.currentTime||0)+d);if(room&&!remoteApplying){sendControl('seek',{time:t});return}if(yt)yt.seekTo(t,true);else{let v=document.getElementById('nativeVideo');if(v)v.currentTime=t}}
function setVolume(v){if(yt)yt.setVolume(+v);else{let e=document.getElementById('nativeVideo');if(e)e.volume=+v/100}}
function fullscreen(){frame.requestFullscreen?.()}
function broadcast(playing){if(remoteApplying||!isHost||!ws||ws.readyState!==1||!room)return;let t=yt?yt.getCurrentTime():(document.getElementById('nativeVideo')?.currentTime||0);ws.send(JSON.stringify({type:'state',room:room,owner:ownerKey,video:currentVideo,title:currentVideo,time:t,playing:playing}))}
function sendHostCommand(type,time){if(!ws||ws.readyState!==1||!room||!isHost)return;let t=time??(yt?yt.getCurrentTime():(document.getElementById('nativeVideo')?.currentTime||0));ws.send(JSON.stringify({type:type,room:room,owner:ownerKey,video:currentVideo,title:currentVideo,time:t}))}
function updateHostUI(){document.querySelectorAll('[data-control]').forEach(b=>b.disabled=!!room&&!isHost);document.getElementById('room').textContent=room?('Room: '+room+' | '+(isHost?'Host':'Viewer')):'No room.'}
function applyRoomState(m){if(m.host){isHost=m.host===ownerKey;updateHostUI()}if(!m.video)return;remoteApplying=true;try{if(m.video!==currentVideo){playMedia(m.video,false)}let target=Number(m.time||0),playing=!!m.playing;let local=yt?yt.getCurrentTime():(document.getElementById('nativeVideo')?.currentTime||0);let drift=Math.abs(local-target);if(drift>0.75){if(yt)yt.seekTo(target,true);else{let v=document.getElementById('nativeVideo');if(v)v.currentTime=target}}if(playing)playVideo();else pauseVideo();statusEl.textContent='Room synced. Drift: '+drift.toFixed(2)+'s'}finally{remoteApplying=false}}
function connect(){if(!room)return;if(ws)try{ws.close()}catch(e){}ws=new WebSocket((location.protocol==='https:'?'wss':'ws')+'://'+location.host+'/ws');ws.onopen=()=>{statusEl.textContent='Room connected: '+room;ws.send(JSON.stringify({type:'join',room:room,owner:ownerKey}))};ws.onmessage=e=>{let m=JSON.parse(e.data);if(m.type==='room-state'){isHost=m.host===ownerKey;updateHostUI();if(m.video)applyRoomState(m)}else if(m.type==='state')applyRoomState(m);else if(m.type==='error')statusEl.textContent=m.message};ws.onclose=()=>setTimeout(connect,2000)}
function searchYT(){let q=document.getElementById('q').value.trim();if(!q)return;fetch('/api/search?q='+encodeURIComponent(q)).then(r=>r.json()).then(x=>{let box=document.getElementById('results');box.innerHTML='';(x.items||[]).forEach(v=>{let d=document.createElement('div');d.className='result';d.innerHTML='<img class="thumb" src="https://i.ytimg.com/vi/'+v.id+'/mqdefault.jpg"><b>'+esc(v.title)+'</b><br><span class="muted">'+esc(v.channel)+'</span>';d.onclick=()=>playMedia(v.id,true);box.appendChild(d)})}).catch(e=>document.getElementById('results').textContent='Search failed: '+e)}
function esc(s){return String(s).replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]))}
function newRoom(){fetch('/api/room/new?owner='+encodeURIComponent(ownerKey)).then(r=>r.json()).then(x=>{room=x.room;isHost=true;showRoom();connect()})}
function joinRoom(){let id=document.getElementById('roomInput').value.trim();if(id){room=id;showRoom();connect()}}
function showRoom(){document.getElementById('room').textContent='Room: '+room+' | '+(isHost?'Host':'Viewer');history.replaceState(null,'','?room='+encodeURIComponent(room));updateHostUI()}
function addQueue(){if(!currentVideo)return;api('/api/queue',{method:'POST',body:JSON.stringify({id:currentVideo,title:currentVideo,channel:''})}).then(loadLibrary)}
function clearQueue(){api('/api/queue/clear',{method:'POST'}).then(loadLibrary)}
function toggleFavorite(){if(!currentVideo)return;api('/api/favorite',{method:'POST',body:JSON.stringify({id:currentVideo,title:currentVideo,channel:''})}).then(loadLibrary)}
function cycleRepeat(){api('/api/queue/repeat',{method:'POST'}).then(x=>statusEl.textContent='Repeat: '+x.repeat)}
function toggleShuffle(){api('/api/queue/shuffle',{method:'POST'}).then(x=>statusEl.textContent='Shuffle: '+x.shuffle)}
function nextVideo(){if(room){sendControl('next');return}api('/api/queue/next',{method:'POST'}).then(x=>{if(x.video)playMedia(x.video.id,true)})}
function previousVideo(){if(room){sendControl('previous');return}api('/api/queue/previous',{method:'POST'}).then(x=>{if(x.video)playMedia(x.video.id,true)})}
function createPlaylist(){let n=prompt('Playlist name');if(n)api('/api/playlist',{method:'POST',body:JSON.stringify({name:n})}).then(loadLibrary)}
function loadLibrary(){api('/api/media').then(x=>{let q=document.getElementById('queue');q.innerHTML=(x.queue||[]).map((v,i)=>'<div class="item">'+(i+1)+'. '+esc(v.title)+'</div>').join('')||'<span class="muted">Empty.</span>';let lib=document.getElementById('library'),out='<h3>Favorites</h3>';out+=(x.favorites||[]).map(v=>'<div class="item" onclick="playMedia(\''+esc(v.id)+'\',true)">'+esc(v.title)+'</div>').join('')||'<span class="muted">None</span>';out+='<h3>Playlists</h3>';for(let n in (x.playlists||{}))out+='<div class="pill">'+esc(n)+' ('+x.playlists[n].length+')</div>';out+='<h3>History</h3>'+((x.history||[]).slice(0,10).map(v=>'<div class="item" onclick="playMedia(\''+esc(v.id)+'\',true)">'+esc(v.title)+'</div>').join('')||'<span class="muted">None</span>');lib.innerHTML=out}).catch(()=>{})}
loadLibrary();if(room){showRoom();connect()}else updateHostUI()
</script></body></html>
""";
}
