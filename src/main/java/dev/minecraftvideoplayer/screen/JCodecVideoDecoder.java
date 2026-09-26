package dev.minecraftvideoplayer.screen;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.scale.AWTUtil;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Pure-Java MP4/MOV decoder backed by JCodec.
 * No native executable is spawned, so it is suitable for Alpine/musl hosts.
 *
 * Supported input: local .mp4/.mov files that JCodec can decode, notably AVC/H.264.
 */
public final class JCodecVideoDecoder implements VideoDecoder {
    private SeekableByteChannel channel;
    private FrameGrab grab;
    private boolean open;
    private long positionMs;

    @Override
    public void open(String source) throws IOException {
        close();

        if (source == null || source.isBlank()) {
            throw new IOException("Empty video source");
        }

        File file = new File(source);
        if (!file.isFile()) {
            throw new IOException("Video file not found: " + source);
        }

        try {
            channel = NIOUtils.readableChannel(file);
            grab = FrameGrab.createFrameGrab(channel);
            open = true;
            positionMs = 0L;
        } catch (JCodecException ex) {
            close();
            throw new IOException("JCodec could not open video: " + source, ex);
        }
    }

    @Override
    public boolean isOpen() {
        return open && grab != null;
    }

    @Override
    public VideoFrame nextFrame() throws IOException {
        if (!isOpen()) return null;

        Picture picture = grab.getNativeFrame();
        if (picture == null) {
            return null;
        }

        BufferedImage image = AWTUtil.toBufferedImage(picture);
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgb = new int[width * height];
        image.getRGB(0, 0, width, height, rgb, 0, width);

        // JCodec does not expose a stable frame timestamp through the generic
        // FrameGrab API used here, so advance by one frame at a conservative
        // default of 25 FPS. The renderer can still consume every decoded frame.
        long timestamp = positionMs;
        positionMs += 40L;

        return new VideoFrame(width, height, rgb, timestamp);
    }

    @Override
    public void seek(long positionMs) throws IOException {
        if (!isOpen()) return;

        long target = Math.max(0L, positionMs);
        try {
            grab.seekToSecondPrecise(target / 1000.0);
            this.positionMs = target;
        } catch (JCodecException ex) {
            throw new IOException("JCodec seek failed", ex);
        }
    }

    @Override
    public long positionMs() {
        return positionMs;
    }

    @Override
    public long durationMs() {
        return 0L;
    }

    @Override
    public void close() {
        open = false;
        grab = null;
        if (channel != null) {
            NIOUtils.closeQuietly(channel);
            channel = null;
        }
        positionMs = 0L;
    }

    public static boolean supportsFile(String source) {
        if (source == null || source.isBlank()) return false;
        String s = source.toLowerCase(Locale.ROOT);
        return s.endsWith(".mp4") || s.endsWith(".mov");
    }
}
