package dev.minecraftvideoplayer.youtube;

import java.net.URI;
import java.util.Locale;

public final class YouTubeMediaAdapter {
    public enum Status { METADATA_ONLY, NATIVE_SOURCE, UNSUPPORTED }

    public record MediaSource(String videoId, String sourceUrl, Status status) {}

    public MediaSource resolve(String input) {
        String id = YouTubeService.extractVideoId(input);
        if (id.isBlank()) return new MediaSource("", "", Status.UNSUPPORTED);

        // YouTube Data API identifies the video but does not expose a raw media URL.
        // A separate extractor/decoder must provide sourceUrl before native playback.
        return new MediaSource(id, "", Status.METADATA_ONLY);
    }

    public boolean isYouTube(String input) {
        if (input == null) return false;
        String s = input.toLowerCase(Locale.ROOT);
        return s.contains("youtube.com/") || s.contains("youtu.be/");
    }
}
