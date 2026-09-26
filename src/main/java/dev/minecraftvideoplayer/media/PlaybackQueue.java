package dev.minecraftvideoplayer.media;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaybackQueue {
    public enum RepeatMode { OFF, ALL, ONE }
    public record Entry(String id, String title, String channel) {}

    private final Map<String, Deque<Entry>> queues = new ConcurrentHashMap<>();
    private final Map<String, Integer> positions = new ConcurrentHashMap<>();
    private final Map<String, RepeatMode> repeat = new ConcurrentHashMap<>();
    private final Map<String, Boolean> shuffle = new ConcurrentHashMap<>();

    public List<Entry> get(String owner) {
        return List.copyOf(queues.getOrDefault(owner, new ArrayDeque<>()));
    }
    public void add(String owner, Entry entry) {
        queues.computeIfAbsent(owner, k -> new ArrayDeque<>()).addLast(entry);
    }
    public Entry remove(String owner, int index) {
        Deque<Entry> q=queues.get(owner); if(q==null||index<0||index>=q.size()) return null;
        List<Entry> l=new ArrayList<>(q); Entry e=l.remove(index); q.clear(); q.addAll(l); return e;
    }
    public void clear(String owner){queues.remove(owner);positions.remove(owner);}
    public void shuffle(String owner){
        Deque<Entry> q=queues.computeIfAbsent(owner,k->new ArrayDeque<>());
        List<Entry> l=new ArrayList<>(q); Collections.shuffle(l); q.clear(); q.addAll(l);
    }
    public Entry next(String owner){
        Deque<Entry> q=queues.get(owner); if(q==null||q.isEmpty()) return null;
        int p=positions.getOrDefault(owner,-1)+1;
        if(p>=q.size()){
            if(repeat.getOrDefault(owner,RepeatMode.OFF)==RepeatMode.ALL) p=0; else return null;
        }
        positions.put(owner,p); return new ArrayList<>(q).get(p);
    }
    public Entry previous(String owner){
        Deque<Entry> q=queues.get(owner); if(q==null||q.isEmpty()) return null;
        int p=positions.getOrDefault(owner,0)-1; if(p<0) p=repeat.getOrDefault(owner,RepeatMode.OFF)==RepeatMode.ALL?q.size()-1:0;
        positions.put(owner,p); return new ArrayList<>(q).get(p);
    }
    public RepeatMode repeat(String owner){return repeat.getOrDefault(owner,RepeatMode.OFF);}
    public RepeatMode cycleRepeat(String owner){RepeatMode n=switch(repeat(owner)){case OFF->RepeatMode.ALL;case ALL->RepeatMode.ONE;case ONE->RepeatMode.OFF;};repeat.put(owner,n);return n;}
    public boolean shuffle(String owner){return shuffle.getOrDefault(owner,false);}
    public boolean toggleShuffle(String owner){boolean n=!shuffle(owner);shuffle.put(owner,n);if(n)shuffle(owner);return n;}
}