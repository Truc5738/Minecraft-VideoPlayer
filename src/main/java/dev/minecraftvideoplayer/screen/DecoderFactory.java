package dev.minecraftvideoplayer.screen;

public final class DecoderFactory {
    private DecoderFactory() {}

    public static VideoDecoder create(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Video source is empty");
        }
        throw new UnsupportedOperationException(
            "No native decoder is installed for this server. Configure a VideoDecoder implementation before playback."
        );
    }
}
