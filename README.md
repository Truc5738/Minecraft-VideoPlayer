# Minecraft-VideoPlayer

Unified video player plugin for **Minecraft 1.26 / Paper 26.2**, designed around a Java + Bedrock/PE friendly UI.

The project currently contains two playback foundations:

1. **Native Minecraft screen playback** for local MP4/MOV files using JCodec and Minecraft map rendering.
2. The existing **web/WebSocket foundation** on port 26467 for browser-based playback and synchronized room features.

> Important: a Paper plugin cannot directly embed a YouTube WebView into an unmodified vanilla Java/Bedrock client. Native Minecraft playback therefore uses a server-side frame pipeline for supported local video files.

## Features

### Video Center

Use:

```
/video
```

The Video Center provides access to the main player features without requiring a large command list.

Available sections include:

- Play Video
- Search YouTube
- Queue
- Playlists
- Favorites
- History
- Watch Room
- Host Control
- Screen Manager
- Player Settings
- Settings
- Admin Center

### Native Screen Manager

The native screen system creates a Minecraft video screen from map-backed Item Frames.

A screen contains:

- Screen ID
- Owner
- Location
- Width and height
- Current video source
- Playback state
- Frame entities

The current UI supports:

- Create Screen
- Select Screen
- Screen Info
- Play
- Pause
- Stop
- Seek -10 seconds
- Seek +10 seconds
- Delete Screen
- Return to Screen Manager

Screens can be created from the Video Center's **Screen Manager**.

### Native MP4/MOV playback

Local MP4/MOV files are decoded with **JCodec**.

Pipeline:

```text
MP4/MOV
   |
   v
JCodecVideoDecoder
   |
   v
VideoFrame
   |
   v
NativeScreenRenderer
   |
   v
Minecraft MapRenderer
   |
   v
Map Item Frames
   |
   v
Java / Bedrock client view
```

The decoder is pure Java and does not require an FFmpeg executable for this MP4/MOV backend.

The current native decoder supports local files available to the Minecraft server, not arbitrary remote YouTube URLs.

## Commands

The intended public command surface is deliberately small:

| Command | Purpose |
|---|---|
| `/video` | Open Video Center |
| `/video help` | Open the help UI |
| `/video reload` | Reload configuration; admin permission required |

Permission:

```
videoplayer.use
videoplayer.admin
```

## Web / Watch Room foundation

The project also retains the existing web playback foundation:

- Javalin web server
- WebSocket communication
- Watch Room support
- Host controls
- Queue and playlist infrastructure
- Favorites and history
- YouTube service/API configuration
- Admin web foundation

Default web port:

```
26467
```

The web layer is separate from the native Minecraft screen renderer.

## Configuration

Example:

```yaml
server:
  web-host: "0.0.0.0"
  web-port: 26467
  public-url: ""
  admin-token: ""

youtube:
  enabled: true
  api-keys: []
  api-key: ""
  max-results: 10

player:
  default-volume: 80
  autoplay: true
  allow-youtube: true
  allow-mp4: true
  allow-hls: true

rooms:
  max-per-player: 3
  max-viewers: 50

language: "en"
```

Do not commit real API keys or administrator tokens.

## Build

### Requirements

- Java 25
- Maven 3.9+
- Paper 26.2 / Minecraft 1.26-compatible server

Build:

```bash
mvn clean package
```

The shaded plugin JAR is generated under:

```text
target/
```

For Termux, make sure `java -version` reports a Java version compatible with the configured Maven release before building.

## Project structure

```text
src/main/java/dev/minecraftvideoplayer/
├── command/
├── media/
├── room/
├── screen/
│   ├── DecoderFactory.java
│   ├── DecoderPlayback.java
│   ├── GeneratedFrameDecoder.java
│   ├── GeneratedFrameDecoderFactory.java
│   ├── JCodecVideoDecoder.java
│   ├── NativeScreenRenderer.java
│   ├── ScreenManager.java
│   ├── VideoDecoder.java
│   ├── VideoDecoderFactory.java
│   ├── VideoFrame.java
│   ├── VideoFrameRenderer.java
│   └── VideoScreen.java
├── ui/
├── web/
└── youtube/
```

## Compatibility notes

### Java Edition

The plugin targets the current Paper 26.2 API used by the project build.

### Bedrock / PE

The UI is designed to remain usable for Bedrock/PE players when they connect through a compatible Java-Bedrock bridge such as Floodgate/Geyser.

Native video rendering is still constrained by what the client can receive and render as normal Minecraft map/item-frame content.

### YouTube

YouTube search/API integration exists as a project foundation, but **native Minecraft playback of arbitrary YouTube URLs is not currently claimed as complete**.

A future YouTube-native backend would require a reliable media extraction/decoding pipeline and must account for YouTube's delivery and usage restrictions.

## Development status

Current milestone:

- Video Center UI: complete
- Native Screen Manager UI: complete
- Screen selection: complete
- Play/Pause/Stop controls: complete
- Seek controls: complete
- Screen deletion and frame cleanup: complete
- JCodec MP4/MOV decoder: implemented
- Native frame rendering pipeline: implemented
- Java 25 CI build: passing

The repository should be treated as an actively developed project; test native playback with representative MP4/MOV files before production deployment.

## License

See the repository license file for project licensing terms.
