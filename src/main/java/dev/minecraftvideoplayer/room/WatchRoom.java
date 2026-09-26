package dev.minecraftvideoplayer.room;
import java.util.Set; import java.util.concurrent.ConcurrentHashMap;
public final class WatchRoom {
 private final String id; private volatile String host; private volatile String videoId=""; private volatile double time; private volatile boolean playing;
 private final Set<String> viewers=ConcurrentHashMap.newKeySet();
 public WatchRoom(String id,String host){this.id=id;this.host=host;viewers.add(host);}
 public String id(){return id;} public String host(){return host;} public void transfer(String p){host=p;viewers.add(p);}
 public Set<String> viewers(){return Set.copyOf(viewers);} public void join(String p){viewers.add(p);} public void leave(String p){viewers.remove(p);}
 public String videoId(){return videoId;} public void setVideoId(String id){videoId=id==null?"":id;} public double time(){return time;} public boolean playing(){return playing;} public void state(double t,boolean p){time=t;playing=p;}
}