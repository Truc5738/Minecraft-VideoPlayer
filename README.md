# Minecraft-VideoPlayer

Cross-platform Minecraft 1.26 video player for Java and Bedrock/PE.

## Current foundation

- Paper plugin
- Java 21 build target
- Web Player server
- WebSocket endpoint
- Port 26467
- Video Center
- MP4/HLS browser playback foundation
- Room creation endpoint
- YouTube configuration foundation
- Admin command and permissions
- No emoji in plugin UI/messages

## Build

Use Java 21:

```bash
mvn clean package
```

The shaded plugin JAR is produced in `target/`.

## Configuration

Edit `config.yml`:

```yaml
server:
  web-port: 26467
  public-url: "https://your-domain.example"

youtube:
  enabled: true
  api-key: ""
```

Never commit a real YouTube API key.

## Architecture

Minecraft handles commands, permissions and room state. The browser handles video rendering. WebSocket communication is used for room/player synchronization.

YouTube playback is intended to use the official embedded player/API rather than downloading and re-streaming YouTube media through the Minecraft server.

## Roadmap

- YouTube Data API search
- YouTube IFrame Player integration
- playlists
- queue, shuffle and repeat
- favorites and history
- synchronized watch rooms
- host controls
- admin dashboard
- multilingual UI
- Java/Bedrock-friendly flow
