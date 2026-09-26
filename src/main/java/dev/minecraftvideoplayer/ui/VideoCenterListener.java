package dev.minecraftvideoplayer.ui;

import dev.minecraftvideoplayer.VideoPlayerPlugin;
import dev.minecraftvideoplayer.youtube.YouTubeService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class VideoCenterListener implements Listener {
    private final VideoPlayerPlugin plugin;
    private final Map<UUID, String> pendingSource = new ConcurrentHashMap<>();

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
            case 10 -> { pendingSource.put(player.getUniqueId(), "play"); prompt(player, "Enter a local MP4/MOV file path in chat. Type 'cancel' to stop."); }
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
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String action = pendingSource.remove(player.getUniqueId());
        if (action == null) return;
        event.setCancelled(true);

        String source = event.getMessage().trim();
        if (source.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.WHITE + "Video input cancelled.");
            return;
        }
        if (!dev.minecraftvideoplayer.screen.JCodecVideoDecoder.supportsFile(source) || !new File(source).isFile()) {
            player.sendMessage(ChatColor.WHITE + "Only an existing local MP4/MOV file is supported by the native decoder.");
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            var screen = plugin.getScreens().all().values().stream()
                    .filter(s -> s.owner().equals(player.getUniqueId()))
                    .findFirst()
                    .orElseGet(() -> {
                        var created = plugin.getScreens().create(player, 8, 4);
                        plugin.getScreenRenderer().renderPreview(player, created);
                        return created;
                    });
            try {
                var decoder = dev.minecraftvideoplayer.screen.DecoderFactory.create(source);
                var playback = new dev.minecraftvideoplayer.screen.DecoderPlayback(
                        decoder, plugin.getScreens(), plugin.getScreenRenderer(), screen.id());
                playback.open(source);
                plugin.getScreens().attachDecoder(screen.id(), playback);
                plugin.getScreens().play(screen.id(), source);
                player.sendMessage(ChatColor.WHITE + "Playing native video on screen " + screen.id() + ".");
            } catch (Exception ex) {
                player.sendMessage(ChatColor.WHITE + "Could not open video: " + ex.getMessage());
            }
        });
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
