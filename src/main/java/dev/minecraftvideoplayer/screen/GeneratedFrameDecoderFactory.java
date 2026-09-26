package dev.minecraftvideoplayer.screen;

public final class GeneratedFrameDecoderFactory implements VideoDecoderFactory {
    private final GeneratedFrameDecoder decoder = new GeneratedFrameDecoder();

    @Override
    public boolean supports(String source) {
        return source != null && !source.isBlank();
    }

    @Override
    public VideoDecoder create(String source) {
        try {
            decoder.open(source);
            return decoder;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to open external frame source", ex);
        }
    }

    public GeneratedFrameDecoder decoder() {
        return decoder;
    }
}
