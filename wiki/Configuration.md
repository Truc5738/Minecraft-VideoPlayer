# Configuration

The default configuration is plugins/Minecraft-VideoPlayer/config.yml.

## Server
server.web-host defaults to 0.0.0.0.
server.web-port defaults to 26467.
server.public-url can contain the public web URL.
server.admin-token must not contain a published secret.

## YouTube
YouTube supports an enabled flag, API keys, a single API key, and max-results.
Never publish real API keys.

## Player
player.default-volume, autoplay, allow-youtube, allow-mp4 and allow-hls control player defaults.

## Rooms
rooms.max-per-player and rooms.max-viewers control room limits.

## Reload
Admins can run /video reload after configuration changes.
