package dev.minecraftvideoplayer.screen;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScreenManager {
    private final Map<String, VideoScreen> screens = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> playbackTasks = new ConcurrentHashMap<>();
    private final Map<String, DecoderPlayback> decoders = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;
    private NativeScreenRenderer renderer;

    public ScreenManager(JavaPlugin plugin) { this.plugin = plugin; }
    public void setRenderer(NativeScreenRenderer renderer) { this.renderer = renderer; }

    public VideoScreen create(Player owner, int width, int height) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        VideoScreen screen = new VideoScreen(id, owner.getUniqueId(), owner.getLocation(), width, height);
        screens.put(id, screen);
        return screen;
    }
    public VideoScreen get(String id) { return screens.get(id); }

    public boolean remove(String id) {
        stop(id);
        VideoScreen screen = screens.remove(id);
        if (screen != null && renderer != null) renderer.removeScreen(screen);
        return screen != null;
    }

    public void clear() { for (String id : screens.keySet()) remove(id); }
    public Map<String, VideoScreen> all() { return Map.copyOf(screens); }
    public int size() { return screens.size(); }

    public void shutdown() {
        clear();
        playbackTasks.values().forEach(BukkitTask::cancel);
        decoders.values().forEach(DecoderPlayback::close);
        playbackTasks.clear();
        decoders.clear();
    }

    public void schedule(String id, Runnable frameTask, long periodMs) {
        stopSchedule(id);
        long ticks = Math.max(1L, Math.round(periodMs / 50.0));
        playbackTasks.put(id, plugin.getServer().getScheduler().runTaskTimer(plugin, frameTask, 0L, ticks));
    }
    public void stopSchedule(String id) {
        BukkitTask task = playbackTasks.remove(id);
        if (task != null) task.cancel();
    }

    public void play(String id, String videoId) {
        VideoScreen screen = screens.get(id);
        if (screen == null) return;
        screen.setVideo(videoId);
        screen.setPlaying(true);
        if (decoders.containsKey(id)) schedule(id, () -> pumpDecoder(id), 40);
    }
    public void pause(String id) {
        stopSchedule(id);
        VideoScreen screen = screens.get(id);
        if (screen != null) screen.setPlaying(false);
    }
    public void attachDecoder(String id, DecoderPlayback playback) {
        DecoderPlayback previous = decoders.put(id, playback);
        if (previous != null) previous.close();
        VideoScreen screen = screens.get(id);
        if (screen != null && screen.playing()) schedule(id, () -> pumpDecoder(id), 40);
    }
    public void detachDecoder(String id) {
        stopSchedule(id);
        DecoderPlayback playback = decoders.remove(id);
        if (playback != null) playback.close();
    }
    public DecoderPlayback decoder(String id) { return decoders.get(id); }
    public void pumpDecoder(String id) {
        DecoderPlayback playback = decoders.get(id);
        if (playback != null) playback.pump();
    }
    public void stop(String id) {
        stopSchedule(id);
        detachDecoder(id);
        VideoScreen screen = screens.get(id);
        if (screen != null) {
            if (renderer != null) renderer.removeScreen(screen);
            screen.setFrameIds(java.util.List.of());
            screen.setPlaying(false);
            screen.setVideo("");
        }
    }
}
