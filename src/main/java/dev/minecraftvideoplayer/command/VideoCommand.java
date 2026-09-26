package dev.minecraftvideoplayer.command;
import dev.minecraftvideoplayer.VideoPlayerPlugin;
import dev.minecraftvideoplayer.room.WatchRoom;
import dev.minecraftvideoplayer.youtube.YouTubeService;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class VideoCommand implements CommandExecutor, TabCompleter {
 private final VideoPlayerPlugin plugin;
 public VideoCommand(VideoPlayerPlugin p){plugin=p;}
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){
  if(c.getName().equalsIgnoreCase("videoroom")){String[] b=new String[a.length+1];b[0]="room";System.arraycopy(a,0,b,1,a.length);a=b;}
  if(!(s instanceof Player p)){s.sendMessage("Players only.");return true;}
  if(!p.hasPermission("videoplayer.use")){p.sendMessage("You do not have permission.");return true;}
  if(a.length==0){p.sendMessage("Video Center: "+plugin.getWebServer().getPublicUrl(p));return true;}
  switch(a[0].toLowerCase()){
   case "play" -> {if(a.length<2){p.sendMessage("/video play <YouTube URL>");return true;}String id=YouTubeService.extractVideoId(a[1]);if(id.isBlank()){p.sendMessage("Invalid YouTube URL.");return true;}p.sendMessage(plugin.getWebServer().getPublicUrl(p)+"?video="+id);}
   case "search" -> {if(a.length<2){p.sendMessage("/video search <query>");return true;}try{for(var v:plugin.getYouTube().search(String.join(" ",Arrays.copyOfRange(a,1,a.length))))p.sendMessage(v.id()+" | "+v.title()+" | "+v.channel());}catch(Exception e){p.sendMessage("YouTube search failed: "+e.getMessage());}}
   case "queue" -> queue(p,a);
   case "history" -> {for(var i:plugin.getMedia().history(p.getName()))p.sendMessage(i.id()+" | "+i.title());}
   case "favorite" -> {if(a.length>=2){String id=YouTubeService.extractVideoId(a[1]);if(!id.isBlank()){plugin.getMedia().favorite(p.getName(),new dev.minecraftvideoplayer.media.MediaLibrary.Item(id,id,"YouTube"));p.sendMessage("Added to favorites.");}}}
   case "playlist" -> {if(a.length<2){p.sendMessage("/video playlist <list|create|add|remove>");return true;}playlist(p,a);}
   case "room" -> {if(a.length>=2&&a[1].equalsIgnoreCase("create")){WatchRoom r=plugin.getRooms().create(p.getName());p.sendMessage("Room created: "+r.id());p.sendMessage(plugin.getWebServer().getPublicUrl(p)+"?room="+r.id());}else if(a.length>=3&&a[1].equalsIgnoreCase("join")){WatchRoom r=plugin.getRooms().get(a[2]);if(r==null)p.sendMessage("Room not found.");else{r.join(p.getName());p.sendMessage("Join room: "+plugin.getWebServer().getPublicUrl(p)+"?room="+r.id());}}else p.sendMessage("/video room <create|join> [room]");}
   default -> p.sendMessage("/video | /video play <url> | /video search <query> | /video room create");
  } return true;
 }
 private void queue(Player p,String[] a){
  String owner=p.getName();
  if(a.length<2){for(var e:plugin.getQueue().get(owner))p.sendMessage(e.id()+" | "+e.title());return;}
  switch(a[1].toLowerCase()){
   case "add" -> {if(a.length<3){p.sendMessage("/video queue add <YouTube URL>");return;}String id=YouTubeService.extractVideoId(a[2]);if(id.isBlank()){p.sendMessage("Invalid YouTube URL.");return;}plugin.getQueue().add(owner,new dev.minecraftvideoplayer.media.PlaybackQueue.Entry(id,id,"YouTube"));p.sendMessage("Added to queue.");}
   case "clear" -> {plugin.getQueue().clear(owner);p.sendMessage("Queue cleared.");}
   case "next" -> {var e=plugin.getQueue().next(owner);p.sendMessage(e==null?"No next video.":"Next: "+e.title());}
   case "previous" -> {var e=plugin.getQueue().previous(owner);p.sendMessage(e==null?"No previous video.":"Previous: "+e.title());}
   case "shuffle" -> p.sendMessage("Shuffle: "+plugin.getQueue().toggleShuffle(owner));
   case "repeat" -> p.sendMessage("Repeat: "+plugin.getQueue().cycleRepeat(owner).name());
   default -> p.sendMessage("/video queue <add|clear|next|previous|shuffle|repeat>");
  }
 }
 private void playlist(Player p,String[] a){
  String o=p.getName();
  switch(a[1].toLowerCase()){
   case "list" -> plugin.getMedia().playlists(o).forEach((n,v)->p.sendMessage(n+" ("+v.size()+")"));
   case "create" -> {if(a.length<3){p.sendMessage("/video playlist create <name>");return;}plugin.getMedia().create(o,String.join(" ",Arrays.copyOfRange(a,2,a.length)));p.sendMessage("Playlist created.");}
   case "add" -> {if(a.length<4){p.sendMessage("/video playlist add <name> <YouTube URL>");return;}String id=YouTubeService.extractVideoId(a[3]);if(id.isBlank()){p.sendMessage("Invalid YouTube URL.");return;}plugin.getMedia().add(o,a[2],new dev.minecraftvideoplayer.media.MediaLibrary.Item(id,id,"YouTube"));p.sendMessage("Added to playlist.");}
   case "remove" -> {if(a.length<4){p.sendMessage("/video playlist remove <name> <index>");return;}try{plugin.getMedia().remove(o,a[2],Integer.parseInt(a[3]));p.sendMessage("Removed from playlist.");}catch(NumberFormatException e){p.sendMessage("Invalid index.");}}
   default -> p.sendMessage("/video playlist <list|create|add|remove>");
  }
 }
 public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length==1)return List.of("play","search","room","queue","playlist","favorite","history");if(a.length==2&&a[0].equalsIgnoreCase("room"))return List.of("create","join");if(a.length==2&&a[0].equalsIgnoreCase("queue"))return List.of("add","clear","next","previous","shuffle","repeat");if(a.length==2&&a[0].equalsIgnoreCase("playlist"))return List.of("list","create","add","remove");return List.of();}
}