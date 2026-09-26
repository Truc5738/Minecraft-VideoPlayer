package dev.minecraftvideoplayer.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.minecraftvideoplayer.VideoPlayerPlugin;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class YouTubeService {
    public record VideoResult(String id,String title,String channel){}
    private final VideoPlayerPlugin plugin;
    private final HttpClient http=HttpClient.newHttpClient();
    private final ObjectMapper mapper=new ObjectMapper();
    private int cursor=0;

    public YouTubeService(VideoPlayerPlugin p){plugin=p;}

    public synchronized List<String> apiKeys(){
        List<String> keys=new ArrayList<>();
        for(String k:plugin.getConfig().getStringList("youtube.api-keys")) if(k!=null&&!k.isBlank()) keys.add(k.trim());
        String legacy=plugin.getConfig().getString("youtube.api-key","");
        if(keys.isEmpty()&&!legacy.isBlank()) keys.add(legacy.trim());
        return keys;
    }

    public synchronized String currentApiKey(){
        List<String> keys=apiKeys(); return keys.isEmpty()?"":keys.get(Math.min(cursor,keys.size()-1));
    }

    public synchronized void rotateApiKey(){
        List<String> keys=apiKeys(); if(!keys.isEmpty()) cursor=(cursor+1)%keys.size();
    }

    public synchronized int apiKeyCount(){return apiKeys().size();}

    public List<VideoResult> search(String q)throws Exception{
        if(q==null||q.isBlank())return List.of();
        List<String> keys=apiKeys();
        if(keys.isEmpty())throw new IllegalStateException("YouTube API key is not configured.");
        int max=Math.max(1,Math.min(50,plugin.getConfig().getInt("youtube.max-results",10)));
        Exception last=null;
        for(int attempt=0;attempt<keys.size();attempt++){
            String key=keys.get((cursor+attempt)%keys.size());
            try{
                String u="https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults="+max+
                    "&q="+URLEncoder.encode(q,StandardCharsets.UTF_8)+"&key="+URLEncoder.encode(key,StandardCharsets.UTF_8);
                var r=http.send(HttpRequest.newBuilder(URI.create(u)).GET().build(),HttpResponse.BodyHandlers.ofString());
                if(r.statusCode()/100!=2){
                    last=new IllegalStateException("YouTube API HTTP "+r.statusCode());
                    if(r.statusCode()==400||r.statusCode()==401||r.statusCode()==403) rotateApiKey();
                    continue;
                }
                JsonNode root=mapper.readTree(r.body()); List<VideoResult> out=new ArrayList<>();
                for(JsonNode i:root.path("items")){
                    String id=i.path("id").path("videoId").asText("");
                    if(!id.isBlank())out.add(new VideoResult(id,i.path("snippet").path("title").asText("Untitled"),i.path("snippet").path("channelTitle").asText("Unknown channel")));
                }
                cursor=(cursor+attempt)%keys.size();
                return out;
            }catch(Exception e){last=e;}
        }
        throw new IllegalStateException("All configured YouTube API keys failed.",last);
    }

    public static String extractVideoId(String s){
        if(s==null)return ""; s=s.trim(); if(s.matches("[A-Za-z0-9_-]{11}"))return s;
        try{
            URI u=URI.create(s); String h=u.getHost()==null?"":u.getHost().toLowerCase();
            if(h.endsWith("youtu.be"))return u.getPath().replace("/","");
            if(h.contains("youtube.com")){
                String q=u.getQuery();
                if(q!=null)for(String p:q.split("&")){String[] kv=p.split("=",2);if(kv.length==2&&kv[0].equals("v"))return kv[1];}
                String[] a=u.getPath().split("/"); for(int i=0;i<a.length-1;i++)if(a[i].equals("embed")||a[i].equals("shorts"))return a[i+1];
            }
        }catch(Exception ignored){}
        return "";
    }
}