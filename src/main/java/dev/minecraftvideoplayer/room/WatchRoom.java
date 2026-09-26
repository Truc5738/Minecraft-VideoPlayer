package dev.minecraftvideoplayer.room;
import java.util.Set; import java.util.concurrent.ConcurrentHashMap;
public final class WatchRoom {
 private final String id; private volatile String host; private volatile String videoId=""; private volatile String title=""; private volatile double time; private volatile boolean playing; private volatile long updatedAt=System.currentTimeMillis();
 private final Set<String> viewers=ConcurrentHashMap.newKeySet();
 public WatchRoom(String id,String host){this.id=id;this.host=host;viewers.add(host);}
 public String id(){return id;} public String host(){return host;} public void transfer(String p){host=p;viewers.add(p);touch();}
 public Set<String> viewers(){return Set.copyOf(viewers);} public void join(String p){viewers.add(p);} public void leave(String p){viewers.remove(p);}
 public String videoId(){return videoId;} public String title(){return title;} public void setVideo(String id,String t){videoId=id==null?"":id;title=t==null?"":t;touch();} public void setVideoId(String id){setVideo(id,"");}
 public double time(){return time;} public boolean playing(){return playing;} public long updatedAt(){return updatedAt;} public void state(double t,boolean p){time=Math.max(0,t);playing=p;touch();}
 public State snapshot(){return new State(id,host,videoId,title,time,playing,viewers(),updatedAt);} private void touch(){updatedAt=System.currentTimeMillis();}
 public record State(String id,String host,String videoId,String title,double time,boolean playing,Set<String> viewers,long updatedAt){}
}