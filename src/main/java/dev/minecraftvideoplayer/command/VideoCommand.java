package dev.minecraftvideoplayer.command;
import dev.minecraftvideoplayer.VideoPlayerPlugin; import dev.minecraftvideoplayer.room.WatchRoom; import dev.minecraftvideoplayer.youtube.YouTubeService; import org.bukkit.command.*; import org.bukkit.entity.Player; import java.util.*;
public final class VideoCommand implements CommandExecutor,TabCompleter{
 private final VideoPlayerPlugin plugin; public VideoCommand(VideoPlayerPlugin p){plugin=p;}
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!(s instanceof Player p)){s.sendMessage("Players only.");return true;}if(!p.hasPermission("videoplayer.use")){p.sendMessage("You do not have permission.");return true;}
  if(a.length==0){p.sendMessage("Video Center: "+plugin.getWebServer().getPublicUrl(p));return true;}
  switch(a[0].toLowerCase()){case "play"->{if(a.length<2){p.sendMessage("/video play <YouTube URL>");return true;}String id=YouTubeService.extractVideoId(a[1]);if(id.isBlank()){p.sendMessage("Invalid YouTube URL.");return true;}p.sendMessage(plugin.getWebServer().getPublicUrl(p)+"?video="+id);}
  case "search"->{if(a.length<2){p.sendMessage("/video search <query>");return true;}try{var r=plugin.getYouTube().search(String.join(" ",Arrays.copyOfRange(a,1,a.length)));for(var v:r)p.sendMessage(v.id()+" | "+v.title()+" | "+v.channel());}catch(Exception e){p.sendMessage("YouTube search failed: "+e.getMessage());}}
  case "queue"->{queue(p,a);}
   case "history"->{for(var i:plugin.getMedia().history(p.getName()))p.sendMessage(i.id()+" | "+i.title());}
   case "favorite"->{if(a.length>=2){String id=YouTubeService.extractVideoId(a[1]);if(!id.isBlank()){plugin.getMedia().favorite(p.getName(),new dev.minecraftvideoplayer.media.MediaLibrary.Item(id,id,"YouTube"));p.sendMessage("Added to favorites.");}}}
   case "playlist"->{if(a.length<2){p.sendMessage("/video playlist <list|create|add|remove>");return true;}playlist(p,a);}
   case "room"->{if(a.length>=2&&a[1].equalsIgnoreCase("create")){WatchRoom r=plugin.getRooms().create(p.getName());p.sendMessage("Room created: "+r.id());p.sendMessage(plugin.getWebServer().getPublicUrl(p)+"?room="+r.id());}else p.sendMessage("/video room create");}
  default->p.sendMessage("/video | /video play <url> | /video search <query> | /video room create");}return true;}
 public List<String> onTabComplete(CommandSender s,Command c,String a,String[] x){if(x.length==1)return List.of("play","search","room","queue","playlist","favorite","history");if(x.length==2&&x[0].equalsIgnoreCase("room"))return List.of("create");return List.of();}
}