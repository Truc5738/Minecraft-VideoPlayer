package dev.minecraftvideoplayer.ui;

import dev.minecraftvideoplayer.VideoPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class VideoCenterMenu {
    public static final String TITLE = "Video Center";
    public static final String SCREEN_TITLE = "Screen Manager";
    public static final String SCREEN_CONTROL_TITLE = "Screen Control";

    private VideoCenterMenu() {}

    public static void open(Player player, VideoPlayerPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        set(inv, 10, Material.PAPER, "Play Video", "Enter a YouTube URL in chat.");
        set(inv, 11, Material.COMPASS, "Search YouTube", "Search videos from the Video Center.");
        set(inv, 12, Material.HOPPER, "Queue", "Manage your playback queue.");
        set(inv, 13, Material.BOOK, "Playlists", "Manage saved playlists.");
        set(inv, 14, Material.NETHER_STAR, "Favorites", "Open your favorite videos.");
        set(inv, 15, Material.CLOCK, "History", "Open recently watched videos.");
        set(inv, 16, Material.ENDER_PEARL, "Watch Room", "Create or join a synchronized room.");
        set(inv, 19, Material.NOTE_BLOCK, "Host Control", "Play, pause, seek and change videos.");
        set(inv, 20, Material.PAINTING, "Screen Manager", "Manage Minecraft video screens.");
        set(inv, 21, Material.JUKEBOX, "Player Settings", "Volume, autoplay, repeat and shuffle.");
        set(inv, 22, Material.REDSTONE, "Settings", "Open plugin/player settings.");
        if (player.hasPermission("videoplayer.admin")) {
            set(inv, 23, Material.COMMAND_BLOCK, "Admin Center", "Administrative controls and diagnostics.");
        }
        set(inv, 26, Material.BARRIER, "Close", "Close this menu.");
        player.openInventory(inv);
    }

    public static void openHelp(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Video Player Help");
        set(inv, 10, Material.PAPER, "/video", "Open the Video Center.");
        set(inv, 12, Material.COMPASS, "Play a video", "Use Play Video and enter a YouTube URL.");
        set(inv, 14, Material.ENDER_PEARL, "Watch Room", "Use the Watch Room section to create or join a room.");
        set(inv, 16, Material.REDSTONE, "/video reload", "Reload configuration. Admin only.");
        set(inv, 22, Material.BOOK, "Controls", "Host controls affect synchronized room playback.");
        set(inv, 26, Material.BARRIER, "Close", "Close this menu.");
        player.openInventory(inv);
    }

    public static void openScreenManager(Player player, VideoPlayerPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, SCREEN_TITLE);
        var screens = plugin.getScreens().all().values().stream()
                .filter(s -> s.owner().equals(player.getUniqueId()))
                .toList();
        for (int i = 0; i < Math.min(45, screens.size()); i++) {
            var s = screens.get(i);
            String state = s.playing() ? "Playing" : (s.videoId().isBlank() ? "Idle" : "Paused");
            set(inv, i, Material.FILLED_MAP, "Screen " + s.id(), state, s.width() + "x" + s.height(), "Click to control");
        }
        set(inv, 49, Material.PAINTING, "Create Screen", "Create a new screen at your location.");
        set(inv, 50, Material.BARRIER, "Close", "Close this menu.");
        player.openInventory(inv);
    }

    public static void openScreenControl(Player player, VideoPlayerPlugin plugin, String screenId) {
        var screen = plugin.getScreens().get(screenId);
        if (screen == null || !screen.owner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.WHITE + "Screen not found or not owned by you.");
            openScreenManager(player, plugin);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, SCREEN_CONTROL_TITLE + " " + screen.id());
        set(inv, 10, Material.LIME_DYE, "Play", "Start or resume playback.");
        set(inv, 11, Material.YELLOW_DYE, "Pause", "Pause playback.");
        set(inv, 12, Material.RED_DYE, "Stop", "Stop and detach decoder.");
        set(inv, 14, Material.SPECTRAL_ARROW, "Seek -10s", "Seek backward 10 seconds.");
        set(inv, 15, Material.ARROW, "Seek +10s", "Seek forward 10 seconds.");
        set(inv, 16, Material.FILLED_MAP, "Screen Info", "ID: " + screen.id(), "State: " + (screen.playing() ? "Playing" : "Paused"));
        set(inv, 22, Material.BARRIER, "Delete Screen", "Remove this screen.");
        set(inv, 26, Material.PAINTING, "Back", "Return to Screen Manager.");
        player.openInventory(inv);
    }

    private static void set(Inventory inv, int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }
}
