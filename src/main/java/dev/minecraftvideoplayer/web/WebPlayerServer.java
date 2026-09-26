package dev.minecraftvideoplayer.web;
import dev.minecraftvideoplayer.VideoPlayerPlugin; import io.javalin.Javalin; import io.javalin.websocket.WsConfig; import java.util.*; import java.util.concurrent.ConcurrentHashMap;
public final class WebPlayerServer{
 private final VideoPlayerPlugin plugin; private final Map<String,String> rooms=new ConcurrentHashMap<>(); private Javalin app;
 public WebPlayerServer(VideoPlayerPlugin p){plugin=p;}
 public void start(){int port=plugin.getConfig().getInt("server.web-port",26467);String host=plugin.getConfig().getString("server.web-host","0.0.0.0");
  app=Javalin.create(c->c.showJavalinBanner=false).get("/",x->x.redirect("/player")).get("/player",x->x.html(WebPage.HTML))
   .get("/api/health",x->x.json(Map.of("status","ok","plugin","Minecraft-VideoPlayer","minecraft","1.26")))
   .get("/api/search",x->{try{x.json(Map.of("items",plugin.getYouTube().search(x.queryParam("q")==null?"":x.queryParam("q"))));}catch(Exception e){x.status(500).json(Map.of("error",e.getMessage()));}})
   .get("/api/room/new",x->{String id=UUID.randomUUID().toString().substring(0,8);rooms.put(id,"");x.json(Map.of("room",id));}).ws("/ws",this::configureSocket).start(host,port);
  plugin.getLogger().info("Web Player listening on "+host+":"+port);}
 private void configureSocket(WsConfig ws){ws.onConnect(x->x.send("{\"type\":\"connected\"}"));ws.onMessage(x->x.send(x.message()));}
 public String getPublicUrl(org.bukkit.entity.Player p){String u=plugin.getConfig().getString("server.public-url","");if(u!=null&&!u.isBlank())return u+"/player";return "http://"+p.getAddress().getAddress().getHostAddress()+":"+plugin.getConfig().getInt("server.web-port",26467)+"/player";}
 public String getStatus(){return app==null?"OFFLINE":"ONLINE";} public void stop(){if(app!=null)app.stop();}
}