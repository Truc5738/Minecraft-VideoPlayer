package dev.minecraftvideoplayer.command;

import dev.minecraftvideoplayer.VideoPlayerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class VideoCommand implements CommandExecutor {
    private final VideoPlayerPlugin plugin;
    public VideoCommand(VideoPlayerPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players.");
            return true;
        }
        if (!player.hasPermission("videoplayer.use")) {
            player.sendMessage("You do not have permission.");
            return true;
        }
        String url = plugin.getWebServer().getPublicUrl(player);
        player.sendMessage("Video Center: " + url);
        return true;
    }
}