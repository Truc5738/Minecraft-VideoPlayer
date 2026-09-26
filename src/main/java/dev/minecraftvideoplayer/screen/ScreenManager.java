package dev.minecraftvideoplayer.screen;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class ScreenManager {
    private final Map<String, VideoScreen> screens = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> playbackTasks = new ConcurrentHashMap<>();
    private final Map<String, DecoderPlayback> decoders = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;

    public ScreenManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

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
        playbackTasks.values().forEach(BukkitTask::cancel);
        decoders.values().forEach(DecoderPlayback::close);
        decoders.clear();
        playbackTasks.clear();
        
    }

    public void schedule(String id, Runnable frameTask, long periodMs) {
        stopSchedule(id);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, frameTask, 0L, Math.max(1L, periodMs / 50L));
        playbackTasks.put(id, task);
    }

    public void stopSchedule(String id) {
        ScheduledFuture<?> task = playbackTasks.remove(id);
        if (task != null) task.cancel();
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

    public void attachDecoder(String id, DecoderPlayback playback) {
        DecoderPlayback previous = decoders.put(id, playback);
        if (previous != null) previous.close();
    }

    public void detachDecoder(String id) {
        DecoderPlayback playback = decoders.remove(id);
        if (playback != null) playback.close();
    }

    public void pumpDecoder(String id) {
        DecoderPlayback playback = decoders.get(id);
        if (playback != null) playback.pump();
    }

    public void stop(String id) {
        stopSchedule(id);
        VideoScreen screen = screens.get(id);
        if (screen == null) return;
        screen.setPlaying(false);
        screen.setVideo("");
    }
}
