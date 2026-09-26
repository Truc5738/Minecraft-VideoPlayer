package dev.minecraftvideoplayer.screen;

import java.io.IOException;

public final class DecoderPlayback implements AutoCloseable {
    private final VideoDecoder decoder;
    private final ScreenManager screens;
    private final NativeScreenRenderer renderer;
    private final String screenId;
    private boolean opened;

    public DecoderPlayback(VideoDecoder decoder, ScreenManager screens,
                           NativeScreenRenderer renderer, String screenId) {
        this.decoder = decoder;
        this.screens = screens;
        this.renderer = renderer;
        this.screenId = screenId;
    }

    public void open(String source) throws IOException {
        decoder.open(source);
        opened = true;
    }

    public boolean pump() {
        if (!opened || !decoder.isOpen()) return false;
        try {
            VideoFrame frame = decoder.nextFrame();
            if (frame == null) return false;
            VideoScreen screen = screens.get(screenId);
            if (screen == null) return false;
            renderer.pushFrame(screen, frame);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public void seek(long positionMs) throws IOException {
        if (!opened) return;
        decoder.seek(Math.max(0L, positionMs));
    }

    public long positionMs() {
        return opened ? decoder.positionMs() : 0L;
    }

    public long durationMs() {
        return opened ? decoder.durationMs() : 0L;
    }

    @Override
    public void close() {
        opened = false;
        decoder.close();
    }
}
