package dev.minecraftvideoplayer.screen;

import java.util.Locale;

public final class DecoderFactory {
    private static volatile VideoDecoderFactory factory;

    private DecoderFactory() {}

    public static void register(VideoDecoderFactory decoderFactory) {
        factory = decoderFactory;
    }

    public static VideoDecoder create(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Video source is empty");
        }
        VideoDecoderFactory current = factory;
        if (current == null) {
            throw new UnsupportedOperationException(
                "No native decoder is installed. Register a VideoDecoderFactory during plugin startup."
            );
        }
        return current.create(source);
    }

    public static boolean supports(String source) {
        VideoDecoderFactory current = factory;
        return current != null && current.supports(source);
    }

    public static String typeOf(String source) {
        if (source == null) return "unknown";
        String s = source.toLowerCase(Locale.ROOT);
        if (s.contains("youtube.com") || s.contains("youtu.be")) return "youtube";
        if (s.endsWith(".mp4") || s.endsWith(".mkv") || s.endsWith(".webm") || s.endsWith(".mov")) return "file";
        return "url";
    }
}
