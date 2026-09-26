package dev.minecraftvideoplayer.room;
import java.util.Map; import java.util.UUID; import java.util.concurrent.ConcurrentHashMap;
public final class RoomManager {
 private final Map<String,WatchRoom> rooms=new ConcurrentHashMap<>();
 public WatchRoom create(String host){String id=UUID.randomUUID().toString().replace("-","").substring(0,8);WatchRoom r=new WatchRoom(id,host);rooms.put(id,r);return r;}
 public WatchRoom get(String id){return rooms.get(id);} public void remove(String id){rooms.remove(id);} public int size(){return rooms.size();}
}