package dev.minecraftvideoplayer.screen;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decoder backend for frame producers supplied by the plugin or an external bridge.
 * It intentionally does not spawn native processes, making it safe for Alpine/musl hosts.
 */
public final class GeneratedFrameDecoder implements VideoDecoder {
    private final AtomicBoolean open = new AtomicBoolean();
    private volatile VideoFrame latestFrame;
    private volatile long positionMs;
    private volatile long durationMs;

    @Override
    public void open(String source) throws IOException {
        if (source == null || source.isBlank()) throw new IOException("Empty video source");
        positionMs = 0L;
        durationMs = 0L;
        latestFrame = null;
        open.set(true);
    }

    public void submitFrame(VideoFrame frame) {
        if (frame == null) return;
        latestFrame = frame;
        positionMs = frame.timestampMs();
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = Math.max(0L, durationMs);
    }

    @Override public boolean isOpen() { return open.get(); }

    @Override
    public VideoFrame nextFrame() {
        if (!open.get()) return null;
        VideoFrame frame = latestFrame;
        latestFrame = null;
        return frame;
    }

    @Override
    public void seek(long positionMs) {
        this.positionMs = Math.max(0L, positionMs);
    }

    @Override public long positionMs() { return positionMs; }
    @Override public long durationMs() { return durationMs; }

    @Override
    public void close() {
        open.set(false);
        latestFrame = null;
    }
}
