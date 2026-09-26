package dev.minecraftvideoplayer.screen;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class VideoScreen {
    private final String id;
    private final UUID owner;
    private Location origin;
    private int width;
    private int height;
    private String videoId = "";
    private boolean playing;
    private java.util.List<UUID> frameIds = java.util.List.of();

    public VideoScreen(String id, UUID owner, Location origin, int width, int height) {
        this.id = id;
        this.owner = owner;
        this.origin = origin.clone();
        this.width = Math.max(1, Math.min(32, width));
        this.height = Math.max(1, Math.min(18, height));
    }

    public String id() { return id; }
    public UUID owner() { return owner; }
    public Location origin() { return origin.clone(); }
    public World world() { return origin.getWorld(); }
    public int width() { return width; }
    public int height() { return height; }
    public String videoId() { return videoId; }
    public boolean playing() { return playing; }
    public java.util.List<UUID> frameIds() { return frameIds; }
    public void setFrameIds(java.util.List<UUID> frameIds) { this.frameIds = java.util.List.copyOf(frameIds); }

    public void resize(int width, int height) {
        this.width = Math.max(1, Math.min(32, width));
        this.height = Math.max(1, Math.min(18, height));
    }

    public void move(Location location) {
        this.origin = location.clone();
    }

    public void setVideo(String videoId) {
        this.videoId = videoId == null ? "" : videoId;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }
}
