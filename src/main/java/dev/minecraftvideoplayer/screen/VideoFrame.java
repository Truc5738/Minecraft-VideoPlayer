package dev.minecraftvideoplayer.screen;

public final class VideoFrame {
    private final int width;
    private final int height;
    private final int[] rgb;

    public VideoFrame(int width, int height, int[] rgb) {
        if (width <= 0 || height <= 0 || rgb.length != width * height) {
            throw new IllegalArgumentException("Invalid video frame dimensions");
        }
        this.width = width;
        this.height = height;
        this.rgb = rgb.clone();
    }

    public int width() { return width; }
    public int height() { return height; }

    public int rgb(int x, int y) {
        return rgb[y * width + x];
    }
}
