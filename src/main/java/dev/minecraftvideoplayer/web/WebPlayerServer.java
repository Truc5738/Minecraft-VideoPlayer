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
            .get("/admin", ctx -> {
                if (!adminAllowed(ctx)) { ctx.status(401).html("Unauthorized"); return; }
                ctx.html(AdminPage.HTML);
            })
            .get("/api/admin/status", ctx -> {
                if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; }
                List<Map<String,Object>> rooms = new ArrayList<>();
                plugin.getRooms().all().forEach((id,r) -> rooms.add(Map.of(
                    "room", id, "host", r.host(), "video", r.videoId(),
                    "playing", r.playing(), "viewers", r.viewers().size()
                )));
                ctx.json(Map.of("web", getStatus(), "youtubeKeys", plugin.getYouTube().apiKeyCount(),
                    "port", plugin.getConfig().getInt("server.web-port",26467), "rooms", rooms));
            })
            .post("/api/admin/rotate-key", ctx -> {
                if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; }
                plugin.getYouTube().rotateApiKey();
                ctx.json(Map.of("ok",true));
            })
            .post("/api/admin/save", ctx -> {
                if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; }
                plugin.saveLibraries();
                ctx.json(Map.of("ok",true));
            })
            .post("/api/admin/reload", ctx -> {
                if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; }
                plugin.reloadConfig();
                ctx.json(Map.of("ok",true));
            })
            .delete("/api/admin/room/{id}", ctx -> {
                if (!adminAllowed(ctx)) { ctx.status(401).json(Map.of("error","Unauthorized")); return; }
                ctx.json(Map.of("ok", plugin.getRooms().close(ctx.pathParam("id")) > 0));
            })
            .get("/api/health", ctx -> ctx.json(Map.of("status","ok","plugin","Minecraft-VideoPlayer","minecraft","1.26")))
            .get("/api/search", ctx -> {
                try {
                    String q = Optional.ofNullable(ctx.queryParam("q")).orElse("").trim();
                    if (q.isBlank()) { ctx.json(Map.of("items", List.of())); return; }
                    ctx.json(Map.of("items", plugin.getYouTube().search(q)));
                } catch (Exception e) {
                    ctx.status(500).json(Map.of("error", String.valueOf(e.getMessage())));
                }
            })
            .get("/api/room/new", ctx -> {
                WatchRoom r = plugin.getRooms().create(owner(ctx));
                ctx.json(Map.of("room", r.id(), "host", r.host()));
            })
            .get("/api/room/{id}", ctx -> {
                WatchRoom r = plugin.getRooms().get(ctx.pathParam("id"));
                if (r == null) { ctx.status(404).json(Map.of("error","Room not found")); return; }
                ctx.json(Map.of("room",r.id(),"host",r.host(),"video",r.videoId(),"title",r.title(),
                    "time",r.time(),"playing",r.playing(),"viewers",r.viewers()));
            })
            .get("/api/media", ctx -> {
                String owner = owner(ctx);
                ctx.json(Map.of("owner",owner,"playlists",plugin.getMedia().playlists(owner),
                    "favorites",plugin.getMedia().favorites(owner),"history",plugin.getMedia().history(owner),
                    "queue",plugin.getQueue().get(owner),"repeat",plugin.getQueue().repeat(owner).name(),
                    "shuffle",plugin.getQueue().shuffle(owner)));
            })
            .post("/api/favorite", ctx -> {
                String owner = owner(ctx); Map<?,?> body=json.readValue(ctx.body(),Map.class);
                String id=value(body,"id","");
                if(id.isBlank()){ctx.status(400).json(Map.of("error","Video id required"));return;}
                plugin.getMedia().favorite(owner,item(body,id)); ctx.json(Map.of("ok",true));
            })
            .delete("/api/favorite/{id}", ctx -> { plugin.getMedia().unfavorite(owner(ctx),ctx.pathParam("id")); ctx.json(Map.of("ok",true)); })
            .post("/api/history", ctx -> {
                Map<?,?> body=json.readValue(ctx.body(),Map.class); String id=value(body,"id","");
                if(id.isBlank()){ctx.status(400).json(Map.of("error","Video id required"));return;}
                plugin.getMedia().history(owner(ctx),item(body,id)); ctx.json(Map.of("ok",true));
            })
            .post("/api/playlist", ctx -> {
                Map<?,?> body=json.readValue(ctx.body(),Map.class); String name=value(body,"name","").trim();
                if(name.isBlank()){ctx.status(400).json(Map.of("error","Playlist name required"));return;}
                plugin.getMedia().create(owner(ctx),name); ctx.json(Map.of("ok",true));
            })
            .delete("/api/playlist/{name}", ctx -> { plugin.getMedia().delete(owner(ctx),ctx.pathParam("name")); ctx.json(Map.of("ok",true)); })
            .post("/api/playlist/{name}", ctx -> {
                Map<?,?> body=json.readValue(ctx.body(),Map.class); String id=value(body,"id","");
                if(id.isBlank()){ctx.status(400).json(Map.of("error","Video id required"));return;}
                plugin.getMedia().add(owner(ctx),ctx.pathParam("name"),item(body,id)); ctx.json(Map.of("ok",true));
            })
            .delete("/api/playlist/{name}/{index}", ctx -> {
                try { plugin.getMedia().remove(owner(ctx),ctx.pathParam("name"),Integer.parseInt(ctx.pathParam("index"))); ctx.json(Map.of("ok",true)); }
                catch(NumberFormatException e){ctx.status(400).json(Map.of("error","Invalid index"));}
            })
            .post("/api/queue", ctx -> {
                Map<?,?> body=json.readValue(ctx.body(),Map.class); String id=value(body,"id","");
                if(id.isBlank()){ctx.status(400).json(Map.of("error","Video id required"));return;}
                plugin.getQueue().add(owner(ctx),queueItem(body,id));
                ctx.json(Map.of("ok",true,"queue",plugin.getQueue().get(owner(ctx))));
            })
            .delete("/api/queue/{index}", ctx -> {
                try {
                    var e=plugin.getQueue().remove(owner(ctx),Integer.parseInt(ctx.pathParam("index")));
                    if(e==null){ctx.status(404).json(Map.of("error","Queue item not found"));return;}
                    ctx.json(Map.of("ok",true));
                } catch(NumberFormatException e){ctx.status(400).json(Map.of("error","Invalid index"));}
            })
            .post("/api/queue/clear", ctx -> { plugin.getQueue().clear(owner(ctx)); ctx.json(Map.of("ok",true)); })
            .post("/api/queue/shuffle", ctx -> { boolean enabled=plugin.getQueue().toggleShuffle(owner(ctx)); ctx.json(Map.of("ok",true,"shuffle",enabled)); })
            .post("/api/queue/repeat", ctx -> { var mode=plugin.getQueue().cycleRepeat(owner(ctx)); ctx.json(Map.of("ok",true,"repeat",mode.name())); })
            .post("/api/queue/next", ctx -> {
                var e=plugin.getQueue().next(owner(ctx));
                if(e==null){ctx.status(404).json(Map.of("error","No next video"));return;}
                ctx.json(Map.of("ok",true,"video",e));
            })
            .post("/api/queue/previous", ctx -> {
                var e=plugin.getQueue().previous(owner(ctx));
                if(e==null){ctx.status(404).json(Map.of("error","No previous video"));return;}
                ctx.json(Map.of("ok",true,"video",e));
            })
            .ws("/ws", this::configureSocket)
            .start(host,port);

        plugin.getLogger().info("Web Player listening on " + host + ":" + port);
    }

    private boolean adminAllowed(io.javalin.http.Context ctx) {
        String configured=plugin.getConfig().getString("server.admin-token","");
        String supplied=ctx.queryParam("token");
        return !configured.isBlank() && configured.equals(supplied);
    }

    private String owner(io.javalin.http.Context ctx) {
        String value=ctx.queryParam("owner");
        return value==null||value.isBlank()?"web-"+ctx.ip():value.substring(0,Math.min(80,value.length()));
    }

    private String value(Map<?,?> map,String key,String fallback){Object v=map.get(key);return v==null?fallback:String.valueOf(v);}
    private MediaLibrary.Item item(Map<?,?> body,String id){return new MediaLibrary.Item(id,value(body,"title",id),value(body,"channel",""));}
    private PlaybackQueue.Entry queueItem(Map<?,?> body,String id){return new PlaybackQueue.Entry(id,value(body,"title",id),value(body,"channel",""));}

    private void configureSocket(WsConfig ws) {
        ws.onConnect(c -> c.send(json.writeValueAsString(Map.of("type","connected"))));
        ws.onClose(c -> {
            socketOwners.remove(c);
            sockets.values().forEach(v -> v.remove(c));
        });
        ws.onMessage(c -> {
            try {
                Map<?, ?> m = json.readValue(c.message(), Map.class);
                String type = value(m, "type", "");
                String roomId = value(m, "room", "");
                if (roomId.isBlank()) { c.send(json.writeValueAsString(Map.of("type","error","message","Room required"))); return; }

                WatchRoom r = plugin.getRooms().get(roomId);
                if (r == null) { c.send(json.writeValueAsString(Map.of("type","error","message","Room not found"))); return; }

                String ownerId = value(m, "owner", "");
                if (ownerId.isBlank()) ownerId = "web-socket-" + c.hashCode();
                ownerId = ownerId.substring(0, Math.min(80, ownerId.length()));
                socketOwners.put(c, ownerId);
                r.join(ownerId);
                sockets.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(c);

                c.send(json.writeValueAsString(Map.of(
                    "type","room-state","room",roomId,"host",r.host(),"isHost",r.host().equals(ownerId),
                    "video",r.videoId(),"title",r.title(),"time",r.time(),"playing",r.playing(),
                    "updatedAt",r.updatedAt(),"viewers",r.viewers()
                )));

                if (type.equals("join")) return;

                if (!r.host().equals(ownerId)) {
                    c.send(json.writeValueAsString(Map.of("type","error","message","Host control only")));
                    return;
                }

                if (type.equals("state")) {
                    String video = value(m, "video", r.videoId());
                    r.setVideo(video, value(m, "title", video));
                    r.state(currentTime(m, r.time()), Boolean.parseBoolean(value(m, "playing", "false")));
                } else if (type.equals("play")) {
                    r.state(currentTime(m, r.time()), true);
                } else if (type.equals("pause")) {
                    r.state(currentTime(m, r.time()), false);
                } else if (type.equals("stop")) {
                    r.state(0, false);
                } else if (type.equals("seek")) {
                    r.state(currentTime(m, r.time()), r.playing());
                } else if (type.equals("next") || type.equals("previous")) {
                    PlaybackQueue.Entry e = type.equals("next")
                        ? plugin.getQueue().next(ownerId)
                        : plugin.getQueue().previous(ownerId);
                    if (e == null) {
                        c.send(json.writeValueAsString(Map.of("type","error","message","No queue item")));
                        return;
                    }
                    r.setVideo(e.id(), e.title());
                    r.state(0, true);
                } else {
                    return;
                }

                broadcastState(roomId, r);
            } catch (Exception e) {
                try { c.send(json.writeValueAsString(Map.of("type","error","message","Invalid message"))); } catch (Exception ignored) {}
            }
        });
    }

    private double currentTime(Map<?, ?> m, double fallback) {
        try { return Math.max(0, Double.parseDouble(value(m, "time", String.valueOf(fallback)))); }
        catch (NumberFormatException ignored) { return Math.max(0, fallback); }
    }

    private void broadcastState(String roomId, WatchRoom r) {
        broadcast(roomId, Map.of(
            "type","room-state","room",roomId,"host",r.host(),
            "video",r.videoId(),"title",r.title(),"time",r.time(),"playing",r.playing(),
            "updatedAt",r.updatedAt(),"viewers",r.viewers()
        ));
    }
}