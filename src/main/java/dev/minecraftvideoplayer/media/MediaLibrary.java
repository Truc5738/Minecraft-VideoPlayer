package dev.minecraftvideoplayer.media;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;
public final class MediaLibrary {
 public record Item(String id,String title,String channel){}
 private final Map<String,List<Item>> playlists=new ConcurrentHashMap<>(); private final Map<String,LinkedHashMap<String,Item>> favorites=new ConcurrentHashMap<>(); private final Map<String,Deque<Item>> history=new ConcurrentHashMap<>();
 private String key(String o,String n){return o+"::"+n;}
 public void create(String o,String n){playlists.putIfAbsent(key(o,n),new ArrayList<>());} public void delete(String o,String n){playlists.remove(key(o,n));}
 public List<Item> playlist(String o,String n){return List.copyOf(playlists.getOrDefault(key(o,n),List.of()));}
 public void add(String o,String n,Item i){playlists.computeIfAbsent(key(o,n),k->new ArrayList<>()).add(i);}
 public void remove(String o,String n,int i){var l=playlists.get(key(o,n));if(l!=null&&i>=0&&i<l.size())l.remove(i);}
 public Map<String,List<Item>> playlists(String o){Map<String,List<Item>> r=new HashMap<>();String p=o+"::";for(var e:playlists.entrySet())if(e.getKey().startsWith(p))r.put(e.getKey().substring(p.length()),List.copyOf(e.getValue()));return r;}
 public void favorite(String o,Item i){favorites.computeIfAbsent(o,k->new LinkedHashMap<>()).put(i.id(),i);} public void unfavorite(String o,String id){var f=favorites.get(o);if(f!=null)f.remove(id);}
 public Collection<Item> favorites(String o){return List.copyOf(favorites.getOrDefault(o,Map.of()).values());}
 public void history(String o,Item i){var q=history.computeIfAbsent(o,k->new ArrayDeque<>());q.removeIf(x->x.id().equals(i.id()));q.addFirst(i);while(q.size()>50)q.removeLast();}
 public Collection<Item> history(String o){return List.copyOf(history.getOrDefault(o,new ArrayDeque<>()));}
}