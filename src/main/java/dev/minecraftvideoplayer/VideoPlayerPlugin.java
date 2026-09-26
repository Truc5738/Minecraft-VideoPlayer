package dev.minecraftvideoplayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.minecraftvideoplayer.command.*;
import dev.minecraftvideoplayer.media.MediaLibrary;
import dev.minecraftvideoplayer.media.PlaybackQueue;
import dev.minecraftvideoplayer.room.RoomManager;
import dev.minecraftvideoplayer.web.WebPlayerServer;
import dev.minecraftvideoplayer.youtube.YouTubeService;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class VideoPlayerPlugin extends JavaPlugin {
    private final ObjectMapper json = new ObjectMapper();
    private WebPlayerServer webServer;
    private RoomManager rooms;
    private YouTubeService youtube;
    private MediaLibrary media;
    private PlaybackQueue queue;

    public void onEnable() {
        saveDefaultConfig();
        rooms = new RoomManager();
        youtube = new YouTubeService(this);
        media = new MediaLibrary();
        queue = new PlaybackQueue();
        loadLibraries();

        var vc = new VideoCommand(this);
        getCommand("video").setExecutor(vc);
        getCommand("video").setTabCompleter(vc);
        getCommand("videoadmin").setExecutor(new VideoAdminCommand(this));
        getCommand("videoroom").setExecutor(vc);
        getCommand("videoroom").setTabCompleter(vc);

        webServer = new WebPlayerServer(this);
        webServer.start();
        getLogger().info("Minecraft-VideoPlayer enabled for Minecraft 1.26.");
    }

    private void loadLibraries() {
        try {
            media.load(dataPath("media-library.json"), json);
            queue.load(dataPath("playback-queue.json"), json);
        } catch (Exception e) {
            getLogger().warning("Could not load saved media data: " + e.getMessage());
        }
    }

    public void saveLibraries() {
        try {
            media.save(dataPath("media-library.json"), json);
            queue.save(dataPath("playback-queue.json"), json);
        } catch (Exception e) {
            getLogger().warning("Could not save media data: " + e.getMessage());
        }
    }

    private Path dataPath(String name) {
        return getDataFolder().toPath().resolve(name);
    }

    public void onDisable() {
        saveLibraries();
        if (webServer != null) webServer.stop();
    }

    public WebPlayerServer getWebServer(){return webServer;}
    public RoomManager getRooms(){return rooms;}
    public YouTubeService getYouTube(){return youtube;}
    public MediaLibrary getMedia(){return media;}
    public PlaybackQueue getQueue(){return queue;}
}