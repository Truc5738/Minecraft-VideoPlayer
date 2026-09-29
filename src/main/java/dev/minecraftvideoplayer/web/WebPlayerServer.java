package dev.minecraftvideoplayer.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.minecraftvideoplayer.VideoPlayerPlugin;
import dev.minecraftvideoplayer.media.MediaLibrary;
import dev.minecraftvideoplayer.media.PlaybackQueue;
import dev.minecraftvideoplayer.room.WatchRoom;
import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class WebPlayerServer {
    private final VideoPlayerPlugin plugin;
    private final Map<String, Set<io.javalin.websocket.WsContext>> sockets = new ConcurrentHashMap<>();
    private final Map<io.javalin.websocket.WsContext, String> socketOwners = new ConcurrentHashMap<>();
    private final ObjectMapper json = new ObjectMapper();
    private Javalin app;

    public WebPlayerServer(VideoPlayerPlugin plugin) { this.plugin = plugin; }

    public void start() {
        int port = plugin.getConfig().getInt("server.web-port", 26467);
        String host = plugin.getConfig().getString("server.web-host", "0.0.0.0");
        app = Javalin.create(c -> c.showJavalinBanner = false)
            .get("/", ctx -> ctx.redirect("/player"))
            .get("/player", ctx -> ctx.html(WebPage.HTML))
            .get("/admin", ctx -> { if (!adminAllowed(ctx)) { ctx.status(401).html("Unauthorized"); return; } ctx.html(AdminPage.HTML); })
            .get("/api/admin/status", ctx -> {
                if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; }
                List<Map<String,Object>> rooms = new ArrayList<>();
                plugin.getRooms().all().forEach((id,r) -> rooms.add(Map.of("room",id,"host",r.host(),"video",r.videoId(),"playing",r.playing(),"viewers",r.viewers().size())));
                ctx.json(Map.of("web",getStatus(),"youtubeKeys",plugin.getYouTube().apiKeyCount(),"port",port,"rooms",rooms));
            })
            .post("/api/admin/rotate-key", ctx -> { if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; } plugin.getYouTube().rotateApiKey(); ctx.json(Map.of("ok",true)); })
            .post("/api/admin/save", ctx -> { if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; } plugin.saveLibraries(); ctx.json(Map.of("ok",true)); })
            .post("/api/admin/reload", ctx -> { if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; } plugin.reloadConfig(); ctx.json(Map.of("ok",true)); })
            .delete("/api/admin/room/{id}", ctx -> { if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; } ctx.json(Map.of("ok",plugin.getRooms().close(ctx.pathParam("id")) > 0)); })
            .get("/api/health", ctx -> ctx.json(Map.of("status","ok","plugin","Minecraft-VideoPlayer","minecraft","1.26")))
            .get("/api/movies", ctx -> {
                List<Map<String,Object>> items = new ArrayList<>();
                for (java.nio.file.Path p : movieDirectory().toFile().listFiles() == null ? List.<java.nio.file.Path>of() : java.util.Arrays.stream(movieDirectory().toFile().listFiles()).map(java.io.File::toPath).toList()) {
                    if (!java.nio.file.Files.isRegularFile(p) || !isAllowedMovie(p.getFileName().toString())) continue;
                    try {
                        items.add(Map.of("name", p.getFileName().toString(), "size", java.nio.file.Files.size(p), "modified", java.nio.file.Files.getLastModifiedTime(p).toMillis()));
                    } catch (Exception ignored) {}
                }
                ctx.json(Map.of("items", items));
            })
            .post("/api/upload", ctx -> {
                if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; }
                if (!plugin.getConfig().getBoolean("upload.enabled", true)) { ctx.status(403).json(Map.of("error","Uploads disabled")); return; }
                var upload = ctx.uploadedFile("file");
                if (upload == null) { ctx.status(400).json(Map.of("error","No file supplied")); return; }
                String original = java.nio.file.Paths.get(upload.filename()).getFileName().toString();
                if (!isAllowedMovie(original)) { ctx.status(415).json(Map.of("error","Only configured movie formats are allowed")); return; }
                long max = Math.max(1L, plugin.getConfig().getLong("upload.max-size-mb", 2048L)) * 1024L * 1024L;
                if (upload.size() > max) { ctx.status(413).json(Map.of("error","File exceeds upload.max-size-mb")); return; }
                java.nio.file.Files.createDirectories(movieDirectory());
                String safe = original.replaceAll("[^A-Za-z0-9._ -]", "_");
                java.nio.file.Path target = movieDirectory().resolve(safe).normalize();
                if (!target.getParent().equals(movieDirectory().toAbsolutePath().normalize())) { ctx.status(400).json(Map.of("error","Invalid filename")); return; }
                try (var in = upload.content()) {
                    java.nio.file.Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                ctx.json(Map.of("ok",true,"name",target.getFileName().toString(),"size",java.nio.file.Files.size(target)));
            })
            .delete("/api/movies/{name}", ctx -> {
                if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; }
                String safe = java.nio.file.Paths.get(ctx.pathParam("name")).getFileName().toString();
                if (!isAllowedMovie(safe)) { ctx.status(400).json(Map.of("error","Invalid movie filename")); return; }
                java.nio.file.Path target = movieDirectory().resolve(safe).normalize();
                boolean deleted = java.nio.file.Files.deleteIfExists(target);
                ctx.json(Map.of("ok",deleted));
            })
            .post("/api/native/frame/{screenId}", ctx -> {
                if (!adminAllowed(ctx)) {
                    ctx.status(401).json(Map.of("error", "Unauthorized"));
                    return;
                }
                String screenId = ctx.pathParam("screenId");
                long timestamp = 0L;
                try {
                    String header = ctx.header("X-Frame-Timestamp");
                    if (header != null && !header.isBlank()) timestamp = Long.parseLong(header);
                } catch (NumberFormatException ignored) {}
                try {
                    boolean accepted = plugin.getFrameBridge().submit(screenId, ctx.bodyAsBytes(), timestamp);
                    if (!accepted) {
                        ctx.status(404).json(Map.of("error", "Screen not found"));
                        return;
                    }
                    ctx.json(Map.of("ok", true, "screen", screenId, "format", ctx.contentType() == null ? "binary" : ctx.contentType()));
                } catch (Exception e) {
                    ctx.status(400).json(Map.of("error", String.valueOf(e.getMessage())));
                }
            })
            .get("/api/search", ctx -> { try { String q=Optional.ofNullable(ctx.queryParam("q")).orElse("").trim(); if(q.isBlank()){ctx.json(Map.of("items",List.of()));return;} ctx.json(Map.of("items",plugin.getYouTube().search(q))); } catch(Exception e){ctx.status(500).json(Map.of("error",String.valueOf(e.getMessage())));} })
            .get("/api/room/new", ctx -> { WatchRoom r=plugin.getRooms().create(owner(ctx)); ctx.json(Map.of("room",r.id(),"host",r.host())); })
            .get("/api/room/{id}", ctx -> { WatchRoom r=plugin.getRooms().get(ctx.pathParam("id")); if(r==null){ctx.status(404).json(Map.of("error","Room not found"));return;} ctx.json(Map.of("room",r.id(),"host",r.host(),"video",r.videoId(),"title",r.title(),"time",r.time(),"playing",r.playing(),"viewers",r.viewers())); })
            .get("/api/media", ctx -> { String o=owner(ctx); ctx.json(Map.of("owner",o,"playlists",plugin.getMedia().playlists(o),"favorites",plugin.getMedia().favorites(o),"history",plugin.getMedia().history(o),"queue",plugin.getQueue().get(o),"repeat",plugin.getQueue().repeat(o).name(),"shuffle",plugin.getQueue().shuffle(o))); })
            .post("/api/favorite", ctx -> { String o=owner(ctx); Map<?,?> b=json.readValue(ctx.body(),Map.class); String id=value(b,"id",""); if(id.isBlank()){ctx.status(400).json(Map.of("error","Video id required"));return;} plugin.getMedia().favorite(o,item(b,id)); ctx.json(Map.of("ok",true)); })
            .delete("/api/favorite/{id}", ctx -> { plugin.getMedia().unfavorite(owner(ctx),ctx.pathParam("id")); ctx.json(Map.of("ok",true)); })
            .post("/api/history", ctx -> { Map<?,?> b=json.readValue(ctx.body(),Map.class); String id=value(b,"id",""); if(id.isBlank()){ctx.status(400).json(Map.of("error","Video id required"));return;} plugin.getMedia().history(owner(ctx),item(b,id)); ctx.json(Map.of("ok",true)); })
            .post("/api/playlist", ctx -> { Map<?,?> b=json.readValue(ctx.body(),Map.class); String n=value(b,"name","").trim(); if(n.isBlank()){ctx.status(400).json(Map.of("error","Playlist name required"));return;} plugin.getMedia().create(owner(ctx),n); ctx.json(Map.of("ok",true)); })
            .delete("/api/playlist/{name}", ctx -> { plugin.getMedia().delete(owner(ctx),ctx.pathParam("name")); ctx.json(Map.of("ok",true)); })
            .post("/api/playlist/{name}", ctx -> { Map<?,?> b=json.readValue(ctx.body(),Map.class); String id=value(b,"id",""); if(id.isBlank()){ctx.status(400).json(Map.of("error","Video id required"));return;} plugin.getMedia().add(owner(ctx),ctx.pathParam("name"),item(b,id)); ctx.json(Map.of("ok",true)); })
            .delete("/api/playlist/{name}/{index}", ctx -> { try{plugin.getMedia().remove(owner(ctx),ctx.pathParam("name"),Integer.parseInt(ctx.pathParam("index")));ctx.json(Map.of("ok",true));}catch(NumberFormatException e){ctx.status(400).json(Map.of("error","Invalid index"));} })
            .post("/api/queue", ctx -> { Map<?,?> b=json.readValue(ctx.body(),Map.class); String id=value(b,"id",""); if(id.isBlank()){ctx.status(400).json(Map.of("error","Video id required"));return;} plugin.getQueue().add(owner(ctx),queueItem(b,id)); ctx.json(Map.of("ok",true,"queue",plugin.getQueue().get(owner(ctx)))); })
            .delete("/api/queue/{index}", ctx -> { try{var e=plugin.getQueue().remove(owner(ctx),Integer.parseInt(ctx.pathParam("index")));if(e==null){ctx.status(404).json(Map.of("error","Queue item not found"));return;}ctx.json(Map.of("ok",true));}catch(NumberFormatException e){ctx.status(400).json(Map.of("error","Invalid index"));} })
            .post("/api/queue/clear", ctx -> { plugin.getQueue().clear(owner(ctx)); ctx.json(Map.of("ok",true)); })
            .post("/api/queue/shuffle", ctx -> { boolean enabled=plugin.getQueue().toggleShuffle(owner(ctx)); ctx.json(Map.of("ok",true,"shuffle",enabled)); })
            .post("/api/queue/repeat", ctx -> { var mode=plugin.getQueue().cycleRepeat(owner(ctx)); ctx.json(Map.of("ok",true,"repeat",mode.name())); })
            .post("/api/queue/next", ctx -> { var e=plugin.getQueue().next(owner(ctx)); if(e==null){ctx.status(404).json(Map.of("error","No next video"));return;}ctx.json(Map.of("ok",true,"video",e)); })
            .post("/api/queue/previous", ctx -> { var e=plugin.getQueue().previous(owner(ctx)); if(e==null){ctx.status(404).json(Map.of("error","No previous video"));return;}ctx.json(Map.of("ok",true,"video",e)); })
            .ws("/ws", this::configureSocket).start(host,port);
        plugin.getLogger().info("Web Player listening on " + host + ":" + port);
    }

    public void stop() { if(app!=null){ app.stop(); app=null; } sockets.clear(); socketOwners.clear(); }
    public boolean getStatus() { return app != null; }
    public String getPublicUrl(org.bukkit.entity.Player player) {
        String configured=plugin.getConfig().getString("server.public-url","");
        if(configured!=null && !configured.isBlank()) return configured.replaceAll("/$","");
        return "http://127.0.0.1:" + plugin.getConfig().getInt("server.web-port",26467);
    }
    public String getPublicUrl(org.bukkit.command.CommandSender sender) { return getPublicUrl(sender instanceof org.bukkit.entity.Player p ? p : null); }

    private java.nio.file.Path movieDirectory() {
        String configured = plugin.getConfig().getString("upload.movies-directory", "movies");
        java.nio.file.Path p = plugin.getDataFolder().toPath().resolve(configured).normalize();
        java.nio.file.Path root = plugin.getDataFolder().toPath().normalize();
        return p.startsWith(root) ? p : root.resolve("movies");
    }

    private boolean isAllowedMovie(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        for (String ext : plugin.getConfig().getStringList("upload.allowed-extensions")) {
            String e = ext.toLowerCase(java.util.Locale.ROOT).replace(".", "");
            if (lower.endsWith("." + e)) return true;
        }
        return false;
    }

    private boolean adminAllowed(io.javalin.http.Context ctx){String configured=plugin.getConfig().getString("server.admin-token","");String supplied=ctx.queryParam("token");return !configured.isBlank()&&configured.equals(supplied);}
    private String owner(io.javalin.http.Context ctx){String v=ctx.queryParam("owner");return v==null||v.isBlank()?"web-"+ctx.ip():v.substring(0,Math.min(80,v.length()));}
    private String value(Map<?,?> m,String k,String f){Object v=m.get(k);return v==null?f:String.valueOf(v);}
    private MediaLibrary.Item item(Map<?,?> b,String id){return new MediaLibrary.Item(id,value(b,"title",id),value(b,"channel",""));}
    private PlaybackQueue.Entry queueItem(Map<?,?> b,String id){return new PlaybackQueue.Entry(id,value(b,"title",id),value(b,"channel",""));}

    private void configureSocket(WsConfig ws){
        ws.onConnect(c->{try{c.send(json.writeValueAsString(Map.of("type","connected")));}catch(Exception ignored){}});
        ws.onClose(c->{String room=socketOwners.remove(c);sockets.values().forEach(v->v.remove(c));if(room!=null){ }});
        ws.onMessage(c->{
            try{
                Map<?,?> m=json.readValue(c.message(),Map.class);String type=value(m,"type","");String roomId=value(m,"room","");
                if(roomId.isBlank()){c.send(json.writeValueAsString(Map.of("type","error","message","Room required")));return;}
                WatchRoom r=plugin.getRooms().get(roomId);if(r==null){c.send(json.writeValueAsString(Map.of("type","error","message","Room not found")));return;}
                String ownerId=value(m,"owner","");if(ownerId.isBlank())ownerId="web-socket-"+c.hashCode();ownerId=ownerId.substring(0,Math.min(80,ownerId.length()));
                socketOwners.put(c,ownerId);r.join(ownerId);sockets.computeIfAbsent(roomId,k->ConcurrentHashMap.newKeySet()).add(c);
                c.send(json.writeValueAsString(Map.of("type","room-state","room",roomId,"host",r.host(),"isHost",r.host().equals(ownerId),"video",r.videoId(),"title",r.title(),"time",r.time(),"playing",r.playing(),"updatedAt",r.updatedAt(),"viewers",r.viewers())));
                if(type.equals("join"))return;
                if(!r.host().equals(ownerId)){c.send(json.writeValueAsString(Map.of("type","error","message","Host control only")));return;}
                if(type.equals("state")){String video=value(m,"video",r.videoId());r.setVideo(video,value(m,"title",video));r.state(currentTime(m,r.time()),Boolean.parseBoolean(value(m,"playing","false")));}
                else if(type.equals("play"))r.state(currentTime(m,r.time()),true);
                else if(type.equals("pause"))r.state(currentTime(m,r.time()),false);
                else if(type.equals("stop"))r.state(0,false);
                else if(type.equals("seek"))r.state(currentTime(m,r.time()),r.playing());
                else if(type.equals("next")||type.equals("previous")){PlaybackQueue.Entry e=type.equals("next")?plugin.getQueue().next(ownerId):plugin.getQueue().previous(ownerId);if(e==null){c.send(json.writeValueAsString(Map.of("type","error","message","No queue item")));return;}r.setVideo(e.id(),e.title());r.state(0,true);}
                else return;
                broadcastState(roomId,r);
            }catch(Exception e){try{c.send(json.writeValueAsString(Map.of("type","error","message","Invalid message")));}catch(Exception ignored){}}
        });
    }
    private double currentTime(Map<?,?> m,double f){try{return Math.max(0,Double.parseDouble(value(m,"time",String.valueOf(f))));}catch(NumberFormatException e){return Math.max(0,f);}}
    private void broadcastState(String roomId,WatchRoom r){broadcast(roomId,Map.of("type","room-state","room",roomId,"host",r.host(),"video",r.videoId(),"title",r.title(),"time",r.time(),"playing",r.playing(),"updatedAt",r.updatedAt(),"viewers",r.viewers()));}
    private void broadcast(String roomId,Map<String,Object> message){
        String payload;
        try{payload=json.writeValueAsString(message);}catch(Exception e){return;}
        Set<io.javalin.websocket.WsContext> set=sockets.get(roomId);if(set==null)return;
        for(var c:set){try{c.send(payload);}catch(Exception ignored){}}
    }
}