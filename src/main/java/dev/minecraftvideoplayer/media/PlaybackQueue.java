package dev.minecraftvideoplayer.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaybackQueue {
    public enum RepeatMode { OFF, ALL, ONE }
    public record Entry(String id,String title,String channel) {}
    private final Map<String,Deque<Entry>> queues=new ConcurrentHashMap<>();
    private final Map<String,Integer> positions=new ConcurrentHashMap<>();
    private final Map<String,RepeatMode> repeat=new ConcurrentHashMap<>();
    private final Map<String,Boolean> shuffle=new ConcurrentHashMap<>();

    public synchronized List<Entry> get(String owner){return List.copyOf(queues.getOrDefault(owner,new ArrayDeque<>()));}
    public synchronized void add(String owner,Entry entry){queues.computeIfAbsent(owner,k->new ArrayDeque<>()).addLast(entry);}
    public synchronized Entry remove(String owner,int index){Deque<Entry> q=queues.get(owner);if(q==null||index<0||index>=q.size())return null;List<Entry> l=new ArrayList<>(q);Entry e=l.remove(index);q.clear();q.addAll(l);return e;}
    public synchronized void clear(String owner){queues.remove(owner);positions.remove(owner);}
    private void reshuffle(String owner){Deque<Entry> q=queues.computeIfAbsent(owner,k->new ArrayDeque<>());List<Entry> l=new ArrayList<>(q);Collections.shuffle(l);q.clear();q.addAll(l);}
    public synchronized Entry next(String owner){Deque<Entry> q=queues.get(owner);if(q==null||q.isEmpty())return null;int p=positions.getOrDefault(owner,-1)+1;if(p>=q.size()){if(repeat.getOrDefault(owner,RepeatMode.OFF)==RepeatMode.ALL)p=0;else return null;}positions.put(owner,p);return new ArrayList<>(q).get(p);}
    public synchronized Entry previous(String owner){Deque<Entry> q=queues.get(owner);if(q==null||q.isEmpty())return null;int p=positions.getOrDefault(owner,0)-1;if(p<0)p=repeat.getOrDefault(owner,RepeatMode.OFF)==RepeatMode.ALL?q.size()-1:0;positions.put(owner,p);return new ArrayList<>(q).get(p);}
    public synchronized RepeatMode repeat(String owner){return repeat.getOrDefault(owner,RepeatMode.OFF);}
    public synchronized RepeatMode cycleRepeat(String owner){RepeatMode n=switch(repeat(owner)){case OFF->RepeatMode.ALL;case ALL->RepeatMode.ONE;case ONE->RepeatMode.OFF;};repeat.put(owner,n);return n;}
    public synchronized boolean shuffle(String owner){return shuffle.getOrDefault(owner,false);}
    public synchronized boolean toggleShuffle(String owner){boolean n=!shuffle(owner);shuffle.put(owner,n);if(n)reshuffle(owner);return n;}

    public synchronized void load(Path file,ObjectMapper mapper)throws IOException{
        if(!Files.exists(file))return;
        Snapshot s=mapper.readValue(Files.readString(file),Snapshot.class);
        queues.clear();positions.clear();repeat.clear();shuffle.clear();
        if(s.queues()!=null)s.queues().forEach((k,v)->queues.put(k,new ArrayDeque<>(v)));
        if(s.positions()!=null)positions.putAll(s.positions());
        if(s.repeat()!=null)repeat.putAll(s.repeat());
        if(s.shuffle()!=null)shuffle.putAll(s.shuffle());
    }
    public synchronized void save(Path file,ObjectMapper mapper)throws IOException{
        Files.createDirectories(file.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(),new Snapshot(queues,positions,repeat,shuffle));
    }
    public record Snapshot(Map<String,Deque<Entry>> queues,Map<String,Integer> positions,Map<String,RepeatMode> repeat,Map<String,Boolean> shuffle){}
}