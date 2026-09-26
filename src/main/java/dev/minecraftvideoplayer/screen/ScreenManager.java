package dev.minecraftvideoplayer.screen;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ScreenManager {
    private final Map<String, VideoScreen> screens = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> playbackTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> { Thread t = new Thread(r, "Minecraft-VideoPlayer-Frames"); t.setDaemon(true); return t; });

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

    public void shutdown() {
        playbackTasks.values().forEach(task -> task.cancel(false));
        playbackTasks.clear();
        scheduler.shutdownNow();
    }

    public void schedule(String id, Runnable frameTask, long periodMs) {
        stopSchedule(id);
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(frameTask, 0, Math.max(20, periodMs), TimeUnit.MILLISECONDS);
        playbackTasks.put(id, task);
    }

    public void stopSchedule(String id) {
        ScheduledFuture<?> task = playbackTasks.remove(id);
        if (task != null) task.cancel(false);
    }

    public void play(String id, String videoId) {
        VideoScreen screen = screens.get(id);
        if (screen == null) return;
        screen.setVideo(videoId);
        screen.setPlaying(true);
        if (!playbackTasks.containsKey(id)) schedule(id, () -> { }, 50);
    }

    public void pause(String id) {
        stopSchedule(id);
        VideoScreen screen = screens.get(id);
        if (screen != null) screen.setPlaying(false);
    }

    public void stop(String id) {
        stopSchedule(id);
        VideoScreen screen = screens.get(id);
        if (screen == null) return;
        screen.setPlaying(false);
        screen.setVideo("");
    }
}
