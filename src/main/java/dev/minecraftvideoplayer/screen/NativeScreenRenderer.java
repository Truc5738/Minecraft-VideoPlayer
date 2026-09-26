package dev.minecraftvideoplayer.screen;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.List;

public final class NativeScreenRenderer {
    private final ScreenManager manager;
    private final java.util.Map<String, VideoFrameRenderer> renderers = new java.util.concurrent.ConcurrentHashMap<>();

    public NativeScreenRenderer(ScreenManager manager) {
        this.manager = manager;
    }

    public void renderPreview(Player player, VideoScreen screen) {
        Location origin = screen.origin().clone();
        List<ItemFrame> frames = new ArrayList<>();

        for (int y = 0; y < screen.height(); y++) {
            for (int x = 0; x < screen.width(); x++) {
                Location location = origin.clone().add(x, screen.height() - 1 - y, 0);
                ItemFrame frame = (ItemFrame) location.getWorld().spawnEntity(location, EntityType.ITEM_FRAME);
                frame.setVisible(false);
                frame.setFixed(true);
                frame.setRotation(org.bukkit.Rotation.NONE);

                MapView map = Bukkit.createMap(location.getWorld());
                map.setTrackingPosition(false);
                map.setUnlimitedTracking(false);
                map.setScale(org.bukkit.map.MapView.Scale.CLOSE);
                map.getRenderers().forEach(map::removeRenderer);
                VideoFrameRenderer renderer = new VideoFrameRenderer();
                renderers.put(screen.id() + ":" + x + ":" + y, renderer);
                map.addRenderer(renderer);

                ItemStack item = new ItemStack(Material.FILLED_MAP);
                MapMeta meta = (MapMeta) item.getItemMeta();
                meta.setMapView(map);
                item.setItemMeta(meta);
                frame.setItem(item);
                frames.add(frame);
            }
        }

        screen.setFrameIds(frames.stream().map(e -> e.getUniqueId()).toList());
        player.sendMessage(ChatColor.WHITE + "Native screen preview created: " + screen.id());
    }

    public void pushFrame(VideoScreen screen, VideoFrame frame) {
        for (int y = 0; y < screen.height(); y++) {
            for (int x = 0; x < screen.width(); x++) {
                VideoFrameRenderer renderer = renderers.get(screen.id() + ":" + x + ":" + y);
                if (renderer != null) renderer.setFrame(frame);
            }
        }
    }

}
