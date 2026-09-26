package dev.minecraftvideoplayer.screen;

public interface VideoDecoderFactory {
    boolean supports(String source);
    VideoDecoder create(String source);
}
