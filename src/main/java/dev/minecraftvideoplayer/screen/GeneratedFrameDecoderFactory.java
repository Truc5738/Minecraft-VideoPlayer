package dev.minecraftvideoplayer.screen;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class GeneratedFrameDecoderFactory implements VideoDecoderFactory {
    private final HttpClient http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    @Override
    public boolean supports(String source) {
        if (source == null || source.isBlank()) return false;
        if (JCodecVideoDecoder.supportsFile(source)) return true;
        String s = source.toLowerCase(Locale.ROOT);
        return s.startsWith("http://") || s.startsWith("https://");
    }

    @Override
    public VideoDecoder create(String source) {
        if (JCodecVideoDecoder.supportsFile(source)) return new JCodecVideoDecoder();

        if (source != null && (source.startsWith("http://") || source.startsWith("https://"))) {
            return new RemoteVideoDecoder(http, source);
        }

        throw new IllegalArgumentException("Unsupported native video source: " + source);
    }

    public GeneratedFrameDecoder decoder() {
        return new GeneratedFrameDecoder();
    }

    /**
     * Downloads a direct video file URL to a temporary MP4/MOV file and
     * delegates decoding to the pure-Java JCodec decoder. This intentionally
     * does not attempt to extract protected YouTube media streams.
     */
    private static final class RemoteVideoDecoder implements VideoDecoder {
        private final HttpClient http;
        private final String source;
        private JCodecVideoDecoder delegate;
        private Path tempFile;

        private RemoteVideoDecoder(HttpClient http, String source) {
            this.http = http;
            this.source = source;
        }

        @Override
        public void open(String ignored) throws IOException {
            close();
            URI uri = URI.create(source);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            if (!(path.endsWith(".mp4") || path.endsWith(".mov"))) {
                throw new IOException("Native URL playback requires a direct .mp4 or .mov URL.");
            }

            HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", "Minecraft-VideoPlayer/1.1")
                .GET()
                .build();

            HttpResponse<Path> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofFile(
                    Files.createTempFile("minecraft-videoplayer-", path.endsWith(".mov") ? ".mov" : ".mp4")
                )
            );

            if (response.statusCode() / 100 != 2) {
                Files.deleteIfExists(response.body());
                throw new IOException("Video download failed: HTTP " + response.statusCode());
            }

            tempFile = response.body();
            delegate = new JCodecVideoDecoder();
            try {
                delegate.open(tempFile.toString());
            } catch (IOException ex) {
                close();
                throw ex;
            }
        }

        @Override public boolean isOpen() { return delegate != null && delegate.isOpen(); }
        @Override public VideoFrame nextFrame() throws IOException { return delegate == null ? null : delegate.nextFrame(); }
        @Override public void seek(long positionMs) throws IOException { if (delegate != null) delegate.seek(positionMs); }
        @Override public long positionMs() { return delegate == null ? 0L : delegate.positionMs(); }
        @Override public long durationMs() { return delegate == null ? 0L : delegate.durationMs(); }

        @Override
        public void close() {
            if (delegate != null) {
                delegate.close();
                delegate = null;
            }
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
                tempFile = null;
            }
        }
    }
}
