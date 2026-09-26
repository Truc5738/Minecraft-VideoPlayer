package dev.minecraftvideoplayer.screen;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScreenManager {
    private final Map<String, VideoScreen> screens = new ConcurrentHashMap<>();

    public VideoScreen create(Player owner, int width, int height) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        VideoScreen screen = new VideoScreen(id, owner.getUniqueId(), owner.getLocation(), width, height);
        screens.put(id, screen);
        return screen;
    }

    public VideoScreen get(String id) {
        return screens.get(id);
    }

    public boolean remove(String id) {
        return screens.remove(id) != null;
    }

    public void clear() {
        screens.clear();
    }

    public Map<String, VideoScreen> all() {
        return Map.copyOf(screens);
    }

    public int size() {
        return screens.size();
    }

    public void play(String id, String videoId) {
        VideoScreen screen = screens.get(id);
        if (screen == null) return;
        screen.setVideo(videoId);
        screen.setPlaying(true);
    }

    public void pause(String id) {
        VideoScreen screen = screens.get(id);
        if (screen != null) screen.setPlaying(false);
    }

    public void stop(String id) {
        VideoScreen screen = screens.get(id);
        if (screen == null) return;
        screen.setPlaying(false);
        screen.setVideo("");
    }
}
