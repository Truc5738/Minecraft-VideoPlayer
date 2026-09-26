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
