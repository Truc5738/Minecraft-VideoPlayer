package dev.minecraftvideoplayer.screen;

import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.entity.Player;

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

        for (int y = 0; y < 128; y++) {
            int sourceY = y * height / 128;
            for (int x = 0; x < 128; x++) {
                int sourceX = x * width / 128;
                int rgb = current.rgb(sourceX, sourceY);
                canvas.setPixel(x, y, toMapColor(rgb));
            }
        }
    }

    private byte toMapColor(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;

        int best = 0;
        int bestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < 64; i++) {
            int mr = ((i * 53) + 20) & 0xff;
            int mg = ((i * 97) + 35) & 0xff;
            int mb = ((i * 151) + 50) & 0xff;
            int dr = r - mr;
            int dg = g - mg;
            int db = b - mb;
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return (byte) best;
    }
}
