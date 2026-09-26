package dev.minecraftvideoplayer.media;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MediaLibrary {
    public record Item(String id, String title, String channel) {}
    private final Map<String,List<Item>> playlists = new ConcurrentHashMap<>();
    private final Map<String,LinkedHashMap<String,Item>> favorites = new ConcurrentHashMap<>();
    private final Map<String,Deque<Item>> history = new ConcurrentHashMap<>();

    private String key(String owner, String name) { return owner + "::" + name; }
    public synchronized void create(String o,String n){playlists.putIfAbsent(key(o,n),new ArrayList<>());}
    public synchronized void delete(String o,String n){playlists.remove(key(o,n));}
    public synchronized List<Item> playlist(String o,String n){return List.copyOf(playlists.getOrDefault(key(o,n),List.of()));}
    public synchronized void add(String o,String n,Item i){playlists.computeIfAbsent(key(o,n),k->new ArrayList<>()).add(i);}
    public synchronized void remove(String o,String n,int i){var l=playlists.get(key(o,n));if(l!=null&&i>=0&&i<l.size())l.remove(i);}
    public synchronized Map<String,List<Item>> playlists(String o){
        Map<String,List<Item>> r=new HashMap<>(); String p=o+"::";
        for(var e:playlists.entrySet())if(e.getKey().startsWith(p))r.put(e.getKey().substring(p.length()),List.copyOf(e.getValue()));
        return r;
    }
    public synchronized void favorite(String o,Item i){favorites.computeIfAbsent(o,k->new LinkedHashMap<>()).put(i.id(),i);}
    public synchronized void unfavorite(String o,String id){var f=favorites.get(o);if(f!=null)f.remove(id);}
    public synchronized Collection<Item> favorites(String o){return List.copyOf(favorites.getOrDefault(o,Map.of()).values());}
    public synchronized void history(String o,Item i){var q=history.computeIfAbsent(o,k->new ArrayDeque<>());q.removeIf(x->x.id().equals(i.id()));q.addFirst(i);while(q.size()>50)q.removeLast();}
    public synchronized Collection<Item> history(String o){return List.copyOf(history.getOrDefault(o,new ArrayDeque<>()));}

    public synchronized void load(Path file, ObjectMapper mapper) throws IOException {
        if(!Files.exists(file)) return;
        Snapshot s=mapper.readValue(Files.readString(file),Snapshot.class);
        playlists.clear(); favorites.clear(); history.clear();
        if(s.playlists()!=null)s.playlists().forEach((k,v)->playlists.put(k,new ArrayList<>(v)));
        if(s.favorites()!=null)s.favorites().forEach((k,v)->favorites.put(k,new LinkedHashMap<>(v)));
        if(s.history()!=null)s.history().forEach((k,v)->history.put(k,new ArrayDeque<>(v)));
    }

    public synchronized void save(Path file, ObjectMapper mapper) throws IOException {
        Files.createDirectories(file.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(),
            new Snapshot(playlists, favorites, history));
    }

    public record Snapshot(
        Map<String,List<Item>> playlists,
        Map<String,LinkedHashMap<String,Item>> favorites,
        Map<String,Deque<Item>> history
    ) {}
}