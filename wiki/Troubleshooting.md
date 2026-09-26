# Troubleshooting

## Release 25 unsupported
Check java -version and javac -version. The build requires Java 25.

## Screen appears but does not play
Check that the source is an existing local MP4/MOV file, the server can read it, and the file is complete. Check the server log for JCodec errors.

## Seek fails
Seek uses the active native decoder. Test with a standard MP4/H.264 file.

## Screen is too large
Reduce dimensions. Every tile creates a map-backed Item Frame. Start with 8 x 4.

## Bedrock looks different
The display is normal Minecraft map/item-frame content, not a client-side video surface.

## YouTube does not play natively
This is expected for the current native backend.

## Port 26467
The existing web foundation uses port 26467. Ensure it is available when the web server is enabled.
