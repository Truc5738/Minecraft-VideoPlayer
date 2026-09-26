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
    private final ObjectMapper json = new ObjectMapper();
    private Javalin app;

    public WebPlayerServer(VideoPlayerPlugin plugin) { this.plugin = plugin; }

    public void start() {
        int port = plugin.getConfig().getInt("server.web-port", 26467);
        String host = plugin.getConfig().getString("server.web-host", "0.0.0.0");

        app = Javalin.create(c -> c.showJavalinBanner = false)
            .get("/", ctx -> ctx.redirect("/player"))
            .get("/player", ctx -> ctx.html(WebPage.HTML))
            .get("/api/health", ctx -> ctx.json(Map.of(
                "status", "ok", "plugin", "Minecraft-VideoPlayer", "minecraft", "1.26"
            )))
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
                WatchRoom r = plugin.getRooms().create("web");
                ctx.json(Map.of("room", r.id()));
            })
            .get("/api/room/{id}", ctx -> {
                WatchRoom r = plugin.getRooms().get(ctx.pathParam("id"));
                if (r == null) { ctx.status(404).json(Map.of("error", "Room not found")); return; }
                ctx.json(Map.of("room", r.id(), "host", r.host(), "video", r.videoId(),
                    "time", r.time(), "playing", r.playing(), "viewers", r.viewers()));
            })

            // Browser media/library API. "owner" is a browser session identifier for now.
            .get("/api/media", ctx -> {
                String owner = owner(ctx);
                ctx.json(Map.of(
                    "owner", owner,
                    "playlists", plugin.getMedia().playlists(owner),
                    "favorites", plugin.getMedia().favorites(owner),
                    "history", plugin.getMedia().history(owner),
                    "queue", plugin.getQueue().get(owner),
                    "repeat", plugin.getQueue().repeat(owner).name(),
                    "shuffle", plugin.getQueue().shuffle(owner)
                ));
            })
            .post("/api/favorite", ctx -> {
                String owner = owner(ctx);
                Map<?, ?> body = json.readValue(ctx.body(), Map.class);
                String id = String.valueOf(body.getOrDefault("id", ""));
                if (id.isBlank()) { ctx.status(400).json(Map.of("error", "Video id required")); return; }
                plugin.getMedia().favorite(owner, item(body, id));
                ctx.json(Map.of("ok", true));
            })
            .delete("/api/favorite/{id}", ctx -> {
                plugin.getMedia().unfavorite(owner(ctx), ctx.pathParam("id"));
                ctx.json(Map.of("ok", true));
            })
            .post("/api/history", ctx -> {
                Map<?, ?> body = json.readValue(ctx.body(), Map.class);
                String id = String.valueOf(body.getOrDefault("id", ""));
                if (id.isBlank()) { ctx.status(400).json(Map.of("error", "Video id required")); return; }
                plugin.getMedia().history(owner(ctx), item(body, id));
                ctx.json(Map.of("ok", true));
            })
            .post("/api/playlist", ctx -> {
                Map<?, ?> body = json.readValue(ctx.body(), Map.class);
                String name = String.valueOf(body.getOrDefault("name", "")).trim();
                if (name.isBlank()) { ctx.status(400).json(Map.of("error", "Playlist name required")); return; }
                plugin.getMedia().create(owner(ctx), name);
                ctx.json(Map.of("ok", true));
            })
            .delete("/api/playlist/{name}", ctx -> {
                plugin.getMedia().delete(owner(ctx), ctx.pathParam("name"));
                ctx.json(Map.of("ok", true));
            })
            .post("/api/playlist/{name}", ctx -> {
                Map<?, ?> body = json.readValue(ctx.body(), Map.class);
                String id = String.valueOf(body.getOrDefault("id", ""));
                if (id.isBlank()) { ctx.status(400).json(Map.of("error", "Video id required")); return; }
                plugin.getMedia().add(owner(ctx), ctx.pathParam("name"), item(body, id));
                ctx.json(Map.of("ok", true));
            })
            .delete("/api/playlist/{name}/{index}", ctx -> {
                try {
                    plugin.getMedia().remove(owner(ctx), ctx.pathParam("name"), Integer.parseInt(ctx.pathParam("index")));
                    ctx.json(Map.of("ok", true));
                } catch (NumberFormatException e) {
                    ctx.status(400).json(Map.of("error", "Invalid index"));
                }
            })
            .post("/api/queue", ctx -> {
                Map<?, ?> body = json.readValue(ctx.body(), Map.class);
                String id = String.valueOf(body.getOrDefault("id", ""));
                if (id.isBlank()) { ctx.status(400).json(Map.of("error", "Video id required")); return; }
                plugin.getQueue().add(owner(ctx), queueItem(body, id));
                ctx.json(Map.of("ok", true, "queue", plugin.getQueue().get(owner(ctx))));
            })
            .delete("/api/queue/{index}", ctx -> {
                try {
                    var e = plugin.getQueue().remove(owner(ctx), Integer.parseInt(ctx.pathParam("index")));
                    if (e == null) { ctx.status(404).json(Map.of("error", "Queue item not found")); return; }
                    ctx.json(Map.of("ok", true));
                } catch (NumberFormatException e) {
                    ctx.status(400).json(Map.of("error", "Invalid index"));
                }
            })
            .post("/api/queue/clear", ctx -> {
                plugin.getQueue().clear(owner(ctx)); ctx.json(Map.of("ok", true));
            })
            .post("/api/queue/shuffle", ctx -> {
                boolean enabled = plugin.getQueue().toggleShuffle(owner(ctx));
                ctx.json(Map.of("ok", true, "shuffle", enabled));
            })
            .post("/api/queue/repeat", ctx -> {
                var mode = plugin.getQueue().cycleRepeat(owner(ctx));
                ctx.json(Map.of("ok", true, "repeat", mode.name()));
            })
            .post("/api/queue/next", ctx -> {
                var e = plugin.getQueue().next(owner(ctx));
                if (e == null) { ctx.status(404).json(Map.of("error", "No next video")); return; }
                ctx.json(Map.of("ok", true, "video", e));
            })
            .post("/api/queue/previous", ctx -> {
                var e = plugin.getQueue().previous(owner(ctx));
                if (e == null) { ctx.status(404).json(Map.of("error", "No previous video")); return; }
                ctx.json(Map.of("ok", true, "video", e));
            })
            .ws("/ws", this::configureSocket)
            .start(host, port);

        plugin.getLogger().info("Web Player listening on " + host + ":" + port);
    }

    private String owner(io.javalin.http.Context ctx) {
        String value = ctx.queryParam("owner");
        return value == null || value.isBlank() ? "web-" + ctx.ip() : value.substring(0, Math.min(80, value.length()));
    }

    private MediaLibrary.Item item(Map<?, ?> body, String id) {
        return new MediaLibrary.Item(id,
            String.valueOf(body.getOrDefault("title", id)),
            String.valueOf(body.getOrDefault("channel", "")));
    }

    private PlaybackQueue.Entry queueItem(Map<?, ?> body, String id) {
        return new PlaybackQueue.Entry(id,
            String.valueOf(body.getOrDefault("title", id)),
            String.valueOf(body.getOrDefault("channel", "")));
    }

    private void configureSocket(WsConfig ws) {
        ws.onConnect(c -> c.send("{\"type\":\"connected\"}"));
        ws.onClose(c -> sockets.values().forEach(v -> v.remove(c)));
        ws.onMessage(c -> {
            try {
                Map<?, ?> m = json.readValue(c.message(), Map.class);
                String type = String.valueOf(m.getOrDefault("type", ""));
                String roomId = String.valueOf(m.getOrDefault("room", ""));
                if (roomId.isBlank()) { c.send("{\"type\":\"error\",\"message\":\"Room required\"}"); return; }

                WatchRoom r = plugin.getRooms().get(roomId);
                if (r == null) { c.send("{\"type\":\"error\",\"message\":\"Room not found\"}"); return; }

                sockets.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(c);
                if (type.equals("state")) {
                    String video = String.valueOf(m.getOrDefault("video", ""));
                    double time = Double.parseDouble(String.valueOf(m.getOrDefault("time", "0")));
                    boolean playing = Boolean.parseBoolean(String.valueOf(m.getOrDefault("playing", "false")));
                    r.setVideoId(video);
                    r.state(time, playing);
                }
                broadcast(roomId, Map.of(
                    "type", type, "video", r.videoId(), "time", r.time(), "playing", r.playing()
                ));
            } catch (Exception e) {
                c.send("{\"type\":\"error\",\"message\":\"Invalid message\"}");
            }
        });
    }

    private void broadcast(String room, Map<String, Object> msg) {
        try {
            String s = json.writeValueAsString(msg);
            for (var c : sockets.getOrDefault(room, Set.of())) {
                if (c.session.isOpen()) c.send(s);
            }
        } catch (Exception ignored) {}
    }

    public String getPublicUrl(org.bukkit.entity.Player p) {
        String u = plugin.getConfig().getString("server.public-url", "");
        if (u != null && !u.isBlank()) return u.replaceAll("/$", "") + "/player";
        return "http://127.0.0.1:" + plugin.getConfig().getInt("server.web-port", 26467) + "/player";
    }

    public String getStatus() { return app == null ? "OFFLINE" : "ONLINE"; }
    public void stop() { if (app != null) app.stop(); }
}