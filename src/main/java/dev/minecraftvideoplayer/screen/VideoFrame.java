package dev.minecraftvideoplayer.screen;

import java.util.Arrays;

public final class VideoFrame {
    private final int width;
    private final int height;
    private final int[] rgb;
    private final long timestampMs;

    public VideoFrame(int width, int height, int[] rgb) {
        this(width, height, rgb, System.currentTimeMillis());
    }

    public VideoFrame(int width, int height, int[] rgb, long timestampMs) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Frame dimensions must be positive");
        if (rgb == null || rgb.length != width * height) throw new IllegalArgumentException("RGB buffer size mismatch");
        this.width = width;
        this.height = height;
        this.rgb = Arrays.copyOf(rgb, rgb.length);
        this.timestampMs = timestampMs;
    }

    public int width() { return width; }
    public int height() { return height; }
    public int[] rgb() { return Arrays.copyOf(rgb, rgb.length); }
    public long timestampMs() { return timestampMs; }
}
