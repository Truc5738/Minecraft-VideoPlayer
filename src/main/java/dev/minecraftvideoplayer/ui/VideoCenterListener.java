package dev.minecraftvideoplayer.ui;

import dev.minecraftvideoplayer.VideoPlayerPlugin;
import dev.minecraftvideoplayer.youtube.YouTubeService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class VideoCenterListener implements Listener {
    private final VideoPlayerPlugin plugin;

    public VideoCenterListener(VideoPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!VideoCenterMenu.TITLE.equals(title) && !"Video Player Help".equals(title)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if ("Video Player Help".equals(title)) {
            if (slot == 26) player.closeInventory();
            return;
        }

        switch (slot) {
            case 10 -> prompt(player, "Paste a YouTube URL in chat. Type 'cancel' to stop.");
            case 11 -> prompt(player, "Type a YouTube search query in chat. Type 'cancel' to stop.");
            case 12 -> player.sendMessage(ChatColor.WHITE + "Queue manager is available from this menu.");
            case 13 -> player.sendMessage(ChatColor.WHITE + "Playlist manager is available from this menu.");
            case 14 -> player.sendMessage(ChatColor.WHITE + "Favorites are stored per player.");
            case 15 -> player.sendMessage(ChatColor.WHITE + "History is stored per player.");
            case 16 -> player.sendMessage(ChatColor.WHITE + "Watch Room: room creation and joining are managed here.");
            case 19 -> player.sendMessage(ChatColor.WHITE + "Host Control: playback control is restricted to the room host.");
            case 20 -> { player.closeInventory(); player.sendMessage(ChatColor.WHITE + "Screen Manager"); player.sendMessage(ChatColor.WHITE + "Creating a screen at your current location..."); var screen = plugin.getScreens().create(player, 8, 4); plugin.getScreenRenderer().renderPreview(player, screen); player.sendMessage(ChatColor.WHITE + "Screen created: " + screen.id() + " (" + screen.width() + "x" + screen.height() + ")."); }
            case 21 -> player.sendMessage(ChatColor.WHITE + "Player Settings: volume, autoplay, repeat and shuffle.");
            case 22 -> player.sendMessage(ChatColor.WHITE + "Settings are loaded from config.yml.");
            case 23 -> {
                if (player.hasPermission("videoplayer.admin")) {
                    player.sendMessage(ChatColor.WHITE + "Admin Center: use /video reload to reload configuration.");
                }
            }
            case 26 -> player.closeInventory();
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (VideoCenterMenu.TITLE.equals(title) || "Video Player Help".equals(title)) {
            event.setCancelled(true);
        }
    }

    private void prompt(Player player, String message) {
        player.closeInventory();
        player.sendMessage(ChatColor.WHITE + message);
    }

    public static String normalizeYouTubeId(String input) {
        return YouTubeService.extractVideoId(input);
    }
}
