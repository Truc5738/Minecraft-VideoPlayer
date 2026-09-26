package dev.minecraftvideoplayer.screen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeFrameBridge {
    private final ScreenManager screens;
    private final NativeScreenRenderer renderer;
    private final Map<String, GeneratedFrameDecoder> decoders = new ConcurrentHashMap<>();

    public NativeFrameBridge(ScreenManager screens, NativeScreenRenderer renderer) {
        this.screens = screens;
        this.renderer = renderer;
    }

    public boolean submit(String screenId, byte[] imageBytes, long timestampMs) throws IOException {
        VideoScreen screen = screens.get(screenId);
        if (screen == null) return false;
        if (imageBytes == null || imageBytes.length == 0) throw new IOException("Empty frame body");

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) throw new IOException("Unsupported image frame. Use PNG or JPEG.");

        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgb = new int[width * height];
        image.getRGB(0, 0, width, height, rgb, 0, width);

        GeneratedFrameDecoder decoder = decoders.computeIfAbsent(screenId, id -> {
            GeneratedFrameDecoder value = new GeneratedFrameDecoder();
            try {
                value.open("bridge:" + id);
                screens.attachDecoder(id, new DecoderPlayback(value, screens, renderer, id));
                return value;
            } catch (IOException ex) {
                throw new BridgeInitException(ex);
            }
        });

        decoder.submitFrame(new VideoFrame(width, height, rgb,
            timestampMs > 0L ? timestampMs : System.currentTimeMillis()));

        if (!screen.playing()) screens.play(screenId, "bridge:" + screenId);
        return true;
    }

    public void remove(String screenId) {
        GeneratedFrameDecoder decoder = decoders.remove(screenId);
        if (decoder != null) screens.detachDecoder(screenId);
    }

    public void clear() {
        decoders.keySet().forEach(this::remove);
    }

    private static final class BridgeInitException extends RuntimeException {
        private BridgeInitException(IOException cause) { super(cause); }
    }
}
