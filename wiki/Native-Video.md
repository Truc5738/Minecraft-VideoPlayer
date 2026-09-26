# Native Video

## Backend
The native backend uses JCodec and JCodec JavaSE helpers. It does not require an FFmpeg executable for the current MP4/MOV backend.

## Pipeline
MP4/MOV -> JCodecVideoDecoder -> VideoFrame -> NativeScreenRenderer -> VideoFrameRenderer -> Minecraft maps -> Item Frames.

## Sources
The current backend accepts existing server-local .mp4 and .mov files. The Minecraft server process must be able to read the file.

## YouTube limitation
A YouTube URL is not a native decoder input. Native YouTube playback would require a separate media acquisition/extraction layer. The project does not currently claim arbitrary YouTube native playback.

## Audio
The current native screen pipeline focuses on video frames and should not be treated as a complete synchronized audio/video player.
