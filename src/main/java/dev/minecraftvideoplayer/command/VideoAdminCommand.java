package dev.minecraftvideoplayer.command;

import dev.minecraftvideoplayer.VideoPlayerPlugin;
import org.bukkit.command.*;
import java.util.*;

public final class VideoAdminCommand implements CommandExecutor, TabCompleter {
    private final VideoPlayerPlugin plugin;
    public VideoAdminCommand(VideoPlayerPlugin plugin){this.plugin=plugin;}

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!sender.hasPermission("videoplayer.admin")){sender.sendMessage("You do not have permission.");return true;}
        if(args.length==0){help(sender);return true;}
        switch(args[0].toLowerCase()){
            case "reload" -> {plugin.reloadConfig();sender.sendMessage("Minecraft-VideoPlayer configuration reloaded.");}
            case "status" -> {
                sender.sendMessage("Web Player: "+plugin.getWebServer().getStatus());
                sender.sendMessage("YouTube API keys: "+plugin.getYouTube().apiKeyCount());
            }
            case "apikeys" -> sender.sendMessage("Configured YouTube API keys: "+plugin.getYouTube().apiKeyCount());
            case "rotate" -> {plugin.getYouTube().rotateApiKey();sender.sendMessage("YouTube API key rotated.");}
            case "save" -> {plugin.saveLibraries();sender.sendMessage("Video library and queue saved.");}
            case "web" -> sender.sendMessage("Web Player: "+plugin.getWebServer().getPublicUrl(sender instanceof org.bukkit.entity.Player p?p:null));
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender s){s.sendMessage("/videoadmin reload | status | apikeys | rotate | save | web");}

    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(args.length==1)return List.of("reload","status","apikeys","rotate","save","web");
        return List.of();
    }
}