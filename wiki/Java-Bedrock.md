# Java and Bedrock

The project is designed for Java and Bedrock/PE players using the same server-side feature flow.

## Java
Java players can use the inventory-based Video Center and map/item-frame screens.

## Bedrock
Bedrock players connected through a compatible Java-Bedrock bridge can use the same server-side menu flow where the bridge supports the inventory interactions.

## Rendering limitation
Native rendering uses normal Minecraft map and Item Frame content. It does not inject a custom decoder into the Bedrock client. Video quality is therefore constrained by Minecraft map rendering.

## Testing
Test with a Java client, a Bedrock client, a small MP4 file and an 8 x 4 screen.
