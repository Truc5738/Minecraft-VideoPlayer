package dev.minecraftvideoplayer;
import dev.minecraftvideoplayer.command.*; import dev.minecraftvideoplayer.room.RoomManager; import dev.minecraftvideoplayer.web.WebPlayerServer; import dev.minecraftvideoplayer.youtube.YouTubeService; import org.bukkit.plugin.java.JavaPlugin;
public final class VideoPlayerPlugin extends JavaPlugin {
 private WebPlayerServer webServer; private RoomManager rooms; private YouTubeService youtube;
 public void onEnable(){saveDefaultConfig();rooms=new RoomManager();youtube=new YouTubeService(this);var vc=new VideoCommand(this);getCommand("video").setExecutor(vc);getCommand("video").setTabCompleter(vc);getCommand("videoadmin").setExecutor(new VideoAdminCommand(this));webServer=new WebPlayerServer(this);webServer.start();getLogger().info("Minecraft-VideoPlayer enabled for Minecraft 1.26.");}
 public void onDisable(){if(webServer!=null)webServer.stop();} public WebPlayerServer getWebServer(){return webServer;} public RoomManager getRooms(){return rooms;} public YouTubeService getYouTube(){return youtube;}
}