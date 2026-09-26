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
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NativeScreenRenderer {
    private final ScreenManager manager;

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
                map.addRenderer(new PreviewRenderer());

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

    private static final class PreviewRenderer extends MapRenderer {
        private boolean rendered;

        @Override
        public void render(MapView view, MapCanvas canvas, Player player) {
            if (rendered) return;
            rendered = true;
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    canvas.setPixel(x, y, (byte) ((x / 16 + y / 16) % 2 == 0 ? 34 : 119));
                }
            }
        }
    }
}
