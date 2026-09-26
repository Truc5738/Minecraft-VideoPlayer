package dev.minecraftvideoplayer.screen;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.Color;

public final class VideoFrameRenderer extends MapRenderer {
    private volatile VideoFrame frame;

    public VideoFrameRenderer() {
        super(false);
    }

    public void setFrame(VideoFrame frame) {
        this.frame = frame;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        VideoFrame current = frame;
        if (current == null) return;

        int width = current.width();
        int height = current.height();
        int[] rgb = current.rgb();

        for (int y = 0; y < 128; y++) {
            int sourceY = Math.min(height - 1, y * height / 128);
            for (int x = 0; x < 128; x++) {
                int sourceX = Math.min(width - 1, x * width / 128);
                canvas.setPixel(x, y, toMapColor(rgb[sourceY * width + sourceX]));
            }
        }
    }

    private byte toMapColor(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return MapPalette.matchColor(new Color(r, g, b));
    }
}
