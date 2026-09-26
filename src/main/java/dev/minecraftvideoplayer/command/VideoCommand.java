package dev.minecraftvideoplayer.command;

import dev.minecraftvideoplayer.VideoPlayerPlugin;
import dev.minecraftvideoplayer.ui.VideoCenterMenu;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class VideoCommand implements CommandExecutor, TabCompleter {
    private final VideoPlayerPlugin plugin;

    public VideoCommand(VideoPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        if (!player.hasPermission("videoplayer.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 0) {
            VideoCenterMenu.open(player, plugin);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> VideoCenterMenu.openHelp(player);
            case "reload" -> reload(player);
            default -> {
                if (args.length == 1 && args[0].startsWith("http")) {
                    String id = dev.minecraftvideoplayer.youtube.YouTubeService.extractVideoId(args[0]);
                    if (id.isBlank()) {
                        player.sendMessage(ChatColor.RED + "Invalid YouTube URL.");
                    } else {
                        player.sendMessage(ChatColor.WHITE + "Video selected: " + id);
                        VideoCenterMenu.open(player, plugin);
                    }
                } else {
                    player.sendMessage(ChatColor.WHITE + "Use /video, /video help, or /video reload.");
                }
            }
        }
        return true;
    }

    private void reload(Player player) {
        if (!player.hasPermission("videoplayer.admin")) {
            player.sendMessage(ChatColor.RED + "Admin permission required.");
            return;
        }
        plugin.reloadConfig();
        player.sendMessage(ChatColor.WHITE + "Minecraft-VideoPlayer configuration reloaded.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("help", "reload");
        return List.of();
    }
}
