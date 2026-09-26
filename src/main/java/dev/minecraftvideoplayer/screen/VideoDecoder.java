package dev.minecraftvideoplayer.screen;

import java.io.IOException;

public interface VideoDecoder extends AutoCloseable {
    void open(String source) throws IOException;
    boolean isOpen();
    VideoFrame nextFrame() throws IOException;
    void seek(long positionMs) throws IOException;
    long positionMs();
    long durationMs();
    @Override void close();
}
