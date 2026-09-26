# Minecraft-VideoPlayer Wiki

Documentation for Minecraft-VideoPlayer.

## Contents
- Installation
- Configuration
- Video Center
- Screen Manager
- Native Video
- Java and Bedrock
- Troubleshooting
- Development

## Current status
The native milestone supports local MP4/MOV files through JCodec and Minecraft map-backed screens. The project also contains a web/WebSocket foundation on port 26467.

## Important limitation
Native playback of arbitrary YouTube URLs is not currently implemented.

## Quick start
1. Build with Java 25 and Maven.
2. Copy the shaded JAR to plugins/.
3. Start Paper 26.2.
4. Run /video.
5. Open Screen Manager and create a screen.
6. Use Play Video with an existing server-local MP4/MOV path.
7. Select the screen and use its controls.
