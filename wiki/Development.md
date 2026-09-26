# Development

## Native package
Native playback code is under src/main/java/dev/minecraftvideoplayer/screen/.

Important classes:
- VideoScreen: screen state
- ScreenManager: screen lifecycle and playback tasks
- NativeScreenRenderer: map-backed screen creation
- VideoFrame: decoded RGB frame
- VideoFrameRenderer: map pixel rendering
- VideoDecoder: decoder interface
- VideoDecoderFactory: decoder factory abstraction
- DecoderPlayback: decoder-to-screen bridge
- JCodecVideoDecoder: MP4/MOV decoder

## UI
UI code is under src/main/java/dev/minecraftvideoplayer/ui/.
Main classes are VideoCenterMenu and VideoCenterListener.

## Adding a decoder
Implement VideoDecoder and VideoDecoderFactory, then register the factory with DecoderFactory.

## Build verification
Use mvn clean package. GitHub Actions validates Java 25 compilation.

## UI rule
Keep Minecraft UI text free of emoji characters. Use plain text and Minecraft-safe materials/icons for Java/Bedrock compatibility.
