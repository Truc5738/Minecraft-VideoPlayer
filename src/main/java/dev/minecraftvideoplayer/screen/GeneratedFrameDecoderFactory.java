package dev.minecraftvideoplayer.screen;

public final class GeneratedFrameDecoderFactory implements VideoDecoderFactory {
    @Override
    public boolean supports(String source) {
        return source != null && !source.isBlank();
    }

    @Override
    public VideoDecoder create(String source) {
        if (JCodecVideoDecoder.supportsFile(source)) {
            return new JCodecVideoDecoder();
        }

        try {
            GeneratedFrameDecoder decoder = new GeneratedFrameDecoder();
            decoder.open(source);
            return decoder;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to open external frame source", ex);
        }
    }

    /**
     * Compatibility helper for external frame bridges.
     * Each call returns an independent decoder instance.
     */
    public GeneratedFrameDecoder decoder() {
        return new GeneratedFrameDecoder();
    }
}
