package dev.minecraftvideoplayer.screen;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import java.awt.Color;

public final class VideoFrameRenderer extends MapRenderer {
    private final int tileX, tileY, tilesWide, tilesHigh;
    private volatile VideoFrame frame;

    public VideoFrameRenderer(int tileX, int tileY, int tilesWide, int tilesHigh) {
        super(false);
        this.tileX = tileX;
        this.tileY = tileY;
        this.tilesWide = tilesWide;
        this.tilesHigh = tilesHigh;
    }

    public void setFrame(VideoFrame frame) { this.frame = frame; }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        VideoFrame current = frame;
        if (current == null) return;
        int width = current.width(), height = current.height();
        int[] rgb = current.rgb();
        int left = (int) ((long) tileX * width / tilesWide);
        int right = Math.max(left + 1, Math.min(width, (int) ((long) (tileX + 1) * width / tilesWide)));
        int top = (int) ((long) tileY * height / tilesHigh);
        int bottom = Math.max(top + 1, Math.min(height, (int) ((long) (tileY + 1) * height / tilesHigh)));
        int tw = right - left, th = bottom - top;
        for (int y = 0; y < 128; y++) {
            int sy = top + Math.min(th - 1, y * th / 128);
            for (int x = 0; x < 128; x++) {
                int sx = left + Math.min(tw - 1, x * tw / 128);
                canvas.setPixel(x, y, toMapColor(rgb[sy * width + sx]));
            }
        }
    }

    private byte toMapColor(int rgb) {
        return MapPalette.matchColor(new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff));
    }
}
