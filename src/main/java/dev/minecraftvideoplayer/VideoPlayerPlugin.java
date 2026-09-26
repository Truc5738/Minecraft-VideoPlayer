package dev.minecraftvideoplayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.minecraftvideoplayer.command.VideoCommand;
import dev.minecraftvideoplayer.media.MediaLibrary;
import dev.minecraftvideoplayer.media.PlaybackQueue;
import dev.minecraftvideoplayer.room.RoomManager;
import dev.minecraftvideoplayer.ui.VideoCenterListener;
import dev.minecraftvideoplayer.web.WebPlayerServer;
import dev.minecraftvideoplayer.youtube.YouTubeService;
import dev.minecraftvideoplayer.screen.GeneratedFrameDecoderFactory;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class VideoPlayerPlugin extends JavaPlugin {
    private final ObjectMapper json = new ObjectMapper();
    private WebPlayerServer webServer;
    private RoomManager rooms;
    private YouTubeService youtube;
    private MediaLibrary media;
    private PlaybackQueue queue;
    private dev.minecraftvideoplayer.screen.ScreenManager screens;
    private dev.minecraftvideoplayer.screen.NativeScreenRenderer screenRenderer;
    private GeneratedFrameDecoderFactory frameDecoderFactory;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        rooms = new RoomManager();
        youtube = new YouTubeService(this);
        media = new MediaLibrary();
        queue = new PlaybackQueue();
        screens = new dev.minecraftvideoplayer.screen.ScreenManager(this);
        screenRenderer = new dev.minecraftvideoplayer.screen.NativeScreenRenderer(screens);
        screens.setRenderer(screenRenderer);
        frameDecoderFactory = new GeneratedFrameDecoderFactory();
        dev.minecraftvideoplayer.screen.DecoderFactory.register(frameDecoderFactory);
        loadLibraries();

        VideoCommand videoCommand = new VideoCommand(this);
        getCommand("video").setExecutor(videoCommand);
        getCommand("video").setTabCompleter(videoCommand);
        getServer().getPluginManager().registerEvents(new VideoCenterListener(this), this);

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

    @Override
    public void onDisable() {
        if (screens != null) screens.shutdown();
        saveLibraries();
        if (webServer != null) webServer.stop();
    }

    public WebPlayerServer getWebServer() { return webServer; }
    public RoomManager getRooms() { return rooms; }
    public YouTubeService getYouTube() { return youtube; }
    public MediaLibrary getMedia() { return media; }
    public PlaybackQueue getQueue() { return queue; }
    public dev.minecraftvideoplayer.screen.ScreenManager getScreens() { return screens; }
    public dev.minecraftvideoplayer.screen.NativeScreenRenderer getScreenRenderer() { return screenRenderer; }
    public GeneratedFrameDecoderFactory getFrameDecoderFactory() { return frameDecoderFactory; }
}
