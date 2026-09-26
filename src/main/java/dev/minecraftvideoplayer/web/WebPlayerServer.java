package dev.minecraftvideoplayer.web;

import dev.minecraftvideoplayer.VideoPlayerPlugin;
import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WebPlayerServer {
    private final VideoPlayerPlugin plugin;
    private final Map<String, String> rooms = new ConcurrentHashMap<>();
    private Javalin app;

    public WebPlayerServer(VideoPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int port = plugin.getConfig().getInt("server.web-port", 26467);
        String host = plugin.getConfig().getString("server.web-host", "0.0.0.0");
        app = Javalin.create(config -> config.showJavalinBanner = false)
            .get("/", ctx -> ctx.redirect("/player"))
            .get("/player", ctx -> ctx.html(WebPage.HTML))
            .get("/api/health", ctx -> ctx.json(Map.of("status", "ok", "plugin", "Minecraft-VideoPlayer")))
            .get("/api/room/new", ctx -> {
                String id = UUID.randomUUID().toString().substring(0, 8);
                rooms.putIfAbsent(id, "");
                ctx.json(Map.of("room", id));
            })
            .ws("/ws", this::configureSocket)
            .start(host, port);
        plugin.getLogger().info("Web Player listening on " + host + ":" + port);
    }

    private void configureSocket(WsConfig ws) {
        ws.onConnect(ctx -> ctx.send("{\"type\":\"connected\"}"));
        ws.onMessage(ctx -> {
            String message = ctx.message();
            ctx.send(message);
        });
    }

    public String getPublicUrl(org.bukkit.entity.Player player) {
        String configured = plugin.getConfig().getString("server.public-url", "");
        if (configured != null && !configured.isBlank()) return configured + "/player";
        return "http://" + player.getAddress().getAddress().getHostAddress() + ":" +
            plugin.getConfig().getInt("server.web-port", 26467) + "/player";
    }

    public String getStatus() {
        return app == null ? "OFFLINE" : "ONLINE";
    }

    public void stop() {
        if (app != null) app.stop();
    }
}