package dev.minecraftvideoplayer;

import dev.minecraftvideoplayer.command.VideoAdminCommand;
import dev.minecraftvideoplayer.command.VideoCommand;
import dev.minecraftvideoplayer.web.WebPlayerServer;
import org.bukkit.plugin.java.JavaPlugin;

public final class VideoPlayerPlugin extends JavaPlugin {
    private WebPlayerServer webServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("video").setExecutor(new VideoCommand(this));
        getCommand("videoadmin").setExecutor(new VideoAdminCommand(this));
        webServer = new WebPlayerServer(this);
        webServer.start();
        getLogger().info("Minecraft-VideoPlayer enabled.");
    }

    @Override
    public void onDisable() {
        if (webServer != null) webServer.stop();
    }

    public WebPlayerServer getWebServer() {
        return webServer;
    }
}