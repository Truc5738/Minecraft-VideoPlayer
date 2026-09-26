# Screen Manager

Screen Manager is the native Minecraft display system.

## Create
Open /video, choose Screen Manager, then Create Screen. The default size is 8 x 4 map tiles.

## Controls
- Select Screen
- Play
- Pause
- Stop
- Seek -10 seconds
- Seek +10 seconds
- Screen Info
- Delete Screen
- Back
- Create Screen

Deleting a screen removes its Item Frames.

## Rendering
Screens use Minecraft Item Frames containing map items. Each map has a custom MapRenderer. Video pixels are converted to Minecraft map colors.

## Performance
Large screens create many maps and Item Frames. Start with 8 x 4 when testing.
