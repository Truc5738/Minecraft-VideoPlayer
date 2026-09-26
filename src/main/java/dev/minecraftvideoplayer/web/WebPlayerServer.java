package dev.minecraftvideoplayer.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.minecraftvideoplayer.VideoPlayerPlugin; import dev.minecraftvideoplayer.room.WatchRoom;
import io.javalin.Javalin; import io.javalin.websocket.WsConfig; import java.util.*; import java.util.concurrent.ConcurrentHashMap;
public final class WebPlayerServer{
 private final VideoPlayerPlugin plugin; private final Map<String,Set<io.javalin.websocket.WsContext>> sockets=new ConcurrentHashMap<>(); private final ObjectMapper json=new ObjectMapper(); private Javalin app;
 public WebPlayerServer(VideoPlayerPlugin p){plugin=p;}
 public void start(){int port=plugin.getConfig().getInt("server.web-port",26467);String host=plugin.getConfig().getString("server.web-host","0.0.0.0");
  app=Javalin.create(c->c.showJavalinBanner=false).get("/",x->x.redirect("/player")).get("/player",x->x.html(WebPage.HTML))
   .get("/api/health",x->x.json(Map.of("status","ok","plugin","Minecraft-VideoPlayer","minecraft","1.26")))
   .get("/api/search",x->{try{x.json(Map.of("items",plugin.getYouTube().search(Optional.ofNullable(x.queryParam("q")).orElse(""))));}catch(Exception e){x.status(500).json(Map.of("error",String.valueOf(e.getMessage())));}})
   .get("/api/room/new",x->{WatchRoom r=plugin.getRooms().create("web");x.json(Map.of("room",r.id()));})
   .get("/api/room/{id}",x->{WatchRoom r=plugin.getRooms().get(x.pathParam("id"));if(r==null){x.status(404).json(Map.of("error","Room not found"));return;}x.json(Map.of("room",r.id(),"host",r.host(),"video",r.videoId(),"time",r.time(),"playing",r.playing(),"viewers",r.viewers()));})
   .ws("/ws",this::configureSocket).start(host,port);
  plugin.getLogger().info("Web Player listening on "+host+":"+port);}
 private void configureSocket(WsConfig ws){
  ws.onConnect(c->{c.send("{\"type\":\"connected\"}");});
  ws.onClose(c->{sockets.values().forEach(v->v.remove(c));});
  ws.onMessage(c->{try{Map<?,?> m=json.readValue(c.message(),Map.class);String type=String.valueOf(m.getOrDefault("type",""));String roomId=String.valueOf(m.getOrDefault("room",""));
    if(roomId.isBlank()){c.send("{\"type\":\"error\",\"message\":\"Room required\"}");return;}
    sockets.computeIfAbsent(roomId,k->ConcurrentHashMap.newKeySet()).add(c); WatchRoom r=plugin.getRooms().get(roomId);
    if(r==null){c.send("{\"type\":\"error\",\"message\":\"Room not found\"}");return;}
    if(type.equals("state")){String video=String.valueOf(m.getOrDefault("video",""));double time=Double.parseDouble(String.valueOf(m.getOrDefault("time","0")));boolean playing=Boolean.parseBoolean(String.valueOf(m.getOrDefault("playing","false")));r.setVideoId(video);r.state(time,playing);}
    broadcast(roomId,Map.of("type",type,"video",r.videoId(),"time",r.time(),"playing",r.playing()));
  }catch(Exception e){c.send("{\"type\":\"error\",\"message\":\"Invalid message\"}");}});
 }
 private void broadcast(String room,Map<String,Object> msg){try{String s=json.writeValueAsString(msg);for(var c:sockets.getOrDefault(room,Set.of()))if(c.session.isOpen())c.send(s);}catch(Exception ignored){}}
 public String getPublicUrl(org.bukkit.entity.Player p){String u=plugin.getConfig().getString("server.public-url","");if(u!=null&&!u.isBlank())return u.replaceAll("/$","")+"/player";return "http://127.0.0.1:"+plugin.getConfig().getInt("server.web-port",26467)+"/player";}
 public String getStatus(){return app==null?"OFFLINE":"ONLINE";} public void stop(){if(app!=null)app.stop();}
}