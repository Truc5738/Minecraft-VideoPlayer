package dev.minecraftvideoplayer.command;

import dev.minecraftvideoplayer.VideoPlayerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class VideoAdminCommand implements CommandExecutor {
    private final VideoPlayerPlugin plugin;
    public VideoAdminCommand(VideoPlayerPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("videoplayer.admin")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/videoadmin reload | status");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("Minecraft-VideoPlayer configuration reloaded.");
            return true;
        }
        if (args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("Web Player: " + plugin.getWebServer().getStatus());
            return true;
        }
        sender.sendMessage("Unknown subcommand.");
        return true;
    }
}