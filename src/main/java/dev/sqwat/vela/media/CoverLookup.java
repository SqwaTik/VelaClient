package dev.sqwat.vela.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Album art for players that publish none (Yandex Music's desktop app, some web players): the Deezer, iTunes and
 * NetEase Cloud Music searches (free, no key) by artist and title, asked at the same time; a hit counts only when it
 * really is that song. NetEase often has fresh Russian releases the other two do not have yet. A song neither knows gets its album's cover, or none: an artist's picture found by name was
 * too often someone else's. A YouTube video gets its own thumbnail instead, found by the YouTube search for its title and
 * channel. Covers are kept on disk, so a song heard before has its cover at once.
 */
public final class CoverLookup {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("Vela/Music");
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final int DISK_LIMIT=200;
    private final Map<String,Optional<byte[]>> cache=new LinkedHashMap<>(16,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Optional<byte[]>> eldest){return size()>48;}
    };
    private final java.util.Set<String> pending=new java.util.HashSet<>();
    private final Map<String,Long> missedAt=new java.util.HashMap<>();
    private final Map<String,Long> failedAt=new java.util.HashMap<>();
    private static final long MISS_RETRY_MS=3*60_000,FAIL_RETRY_MS=20_000;
    private final java.nio.file.Path folder;

    public CoverLookup(java.nio.file.Path folder){this.folder=folder;}

    /** PNG bytes of the cover, or null while it is looked up or when there is none. Cheap to call every tick. */
    public synchronized byte[] get(MediaTrack track){
        if(track==null||track.title().isBlank())return null;
        boolean video=MusicBrand.of(track)==MusicBrand.YOUTUBE;
        String key=(video?"youtube|":"cover2|")+Lyrics.key(track.artist(),track.title());
        var known=cache.get(key);
        // A miss is asked again after a while: a source may have been down or limiting requests.
        long now=System.currentTimeMillis();
        if(known!=null&&(known.isPresent()||now-missedAt.getOrDefault(key,0L)<MISS_RETRY_MS))return known.orElse(null);
        // A failed lookup is not repeated every tick (that is what gets a source to start refusing).
        if(now-failedAt.getOrDefault(key,0L)<FAIL_RETRY_MS)return null;
        if(!pending.add(key))return null;
        String artist=track.artist(),title=track.title(),album=track.album()==null?"":track.album();
        Thread.ofVirtual().name("Vela cover").start(()->{
            byte[] found=null;boolean done=true;Exception failed=null;
            try{
                found=readDisk(key);
                if(found==null){found=video?youtube(artist,title):find(artist,title,album);if(found!=null)writeDisk(key,found);}
            }catch(Exception error){done=false;failed=error;LOG.debug("Cover lookup failed",error);}
            if(done&&found==null)LOG.info("No cover found for \"{}\" - \"{}\" (album \"{}\")",artist,title,album);
            synchronized(this){
                pending.remove(key);
                // A network failure is retried shortly; a clean miss after a few minutes.
                if(done){cache.put(key,Optional.ofNullable(found));if(found==null)missedAt.put(key,System.currentTimeMillis());else missedAt.remove(key);failedAt.remove(key);}
                else{
                    // Said once per song, so a cover that never comes can be explained from the log.
                    if(!failedAt.containsKey(key))LOG.info("Cover lookup failed for \"{}\" - \"{}\": {}",artist,title,String.valueOf(failed));
                    failedAt.put(key,System.currentTimeMillis());
                }
            }
        });
        return null;
    }

    private byte[] readDisk(String key){
        if(folder==null)return null;
        var file=folder.resolve(fileName(key));
        try{
            if(!java.nio.file.Files.isRegularFile(file))return null;
            java.nio.file.Files.setLastModifiedTime(file,java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
            return java.nio.file.Files.readAllBytes(file);
        }catch(java.io.IOException error){return null;}
    }
    private void writeDisk(String key,byte[] png){
        if(folder==null)return;
        try{
            java.nio.file.Files.createDirectories(folder);
            java.nio.file.Files.write(folder.resolve(fileName(key)),png);
            // Least recently shown covers go first once there are too many.
            try(var files=java.nio.file.Files.list(folder)){
                var all=files.filter(f->f.toString().endsWith(".png")).sorted(java.util.Comparator.comparingLong(f->f.toFile().lastModified())).toList();
                for(int i=0;i<all.size()-DISK_LIMIT;i++)java.nio.file.Files.deleteIfExists(all.get(i));
            }
        }catch(java.io.IOException error){LOG.debug("Cover cache write failed",error);}
    }
    static String fileName(String key){
        try{
            var digest=java.security.MessageDigest.getInstance("SHA-1").digest(key.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest,0,10)+".png";
        }catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}
    }

    /** The song's own cover, else its album's (by name), else none. */
    private static byte[] find(String artist,String title,String album)throws Exception{
        byte[] song=findSong(artist,title);
        if(song!=null)return song;
        String a=Lyrics.cleanArtist(artist);
        if(a.isEmpty())return null;
        String url=album.isBlank()?null:deezerAlbum(a,album);
        if(url==null&&!album.isBlank())url=itunesAlbum(a,album);
        return url==null?null:download(url);
    }
    private static byte[] findSong(String artist,String title)throws Exception{
        // A title alone matches other songs too: only artist + title queries, unless the song names no artist at all.
        var queries=new java.util.ArrayList<>(Lyrics.queries(artist,title).stream().filter(q->!q.artist().isEmpty()).toList());
        if(queries.isEmpty())queries.addAll(Lyrics.queries(artist,title));
        else{
            // "MellSher, 5opka": the catalogue may list the song under the second artist only, so each one is asked;
            // then the title alone, hits still checked against every artist the song names.
            String t=Lyrics.cleanTitle(title);
            for(var name:Lyrics.artistNames(artist)){var q=new Lyrics.Query(name,t);if(!queries.contains(q))queries.add(q);}
            queries.add(new Lyrics.Query("",t));
        }
        // Deezer, iTunes and NetEase race: the first one with this song wins, the others are not waited for.
        var winner=new java.util.concurrent.CompletableFuture<String>();
        var left=new java.util.concurrent.atomic.AtomicInteger(3);
        var failure=new java.util.concurrent.atomic.AtomicReference<Exception>();
        var all=queries;
        for(int source=0;source<3;source++){int which=source;Thread.ofVirtual().name("Vela cover "+which).start(()->{
            try{
                for(var query:all){
                    if(winner.isDone())return;
                    // Searched by the query, matched against the song's own artist line.
                    String match=query.artist().isEmpty()?artist:query.artist()+", "+artist;
                    // One refused query does not end this source: the next query may still find the song.
                    try{
                        String url=switch(which){
                            case 0->deezer(query.artist(),query.title(),match);
                            case 1->itunes(query.artist(),query.title(),match);
                            default->netease(query.artist(),query.title(),match);
                        };
                        if(url!=null){winner.complete(url);return;}
                    }catch(InterruptedException error){Thread.currentThread().interrupt();return;}
                    catch(Exception error){failure.compareAndSet(null,error);}
                }
            }finally{if(left.decrementAndGet()==0)winner.complete(null);}
        });}
        String url=winner.get(20,java.util.concurrent.TimeUnit.SECONDS);
        if(url!=null)return download(url);
        if(failure.get()!=null)throw failure.get();
        return null;
    }
    /** The thumbnail of the YouTube video with this title from this channel: what the video shows in its tab. */
    private static byte[] youtube(String channel,String title)throws Exception{
        var response=HTTP.send(HttpRequest.newBuilder(URI.create("https://www.youtube.com/results?search_query="+URLEncoder.encode((channel+" "+title).trim(),StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(8)).header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64)").header("Accept-Language","ru,en").GET().build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new java.io.IOException("youtube "+response.statusCode());
        String id=pickVideo(response.body(),channel,title);
        // The 16:9 "medium" thumbnail: the larger ones are 4:3 with black bars.
        return id==null?null:download("https://i.ytimg.com/vi/"+id+"/mqdefault.jpg");
    }
    private static final java.util.regex.Pattern VIDEO=java.util.regex.Pattern.compile("\"videoRenderer\":\\{\"videoId\":\"([\\w-]{11})\"");
    private static final java.util.regex.Pattern RUN=java.util.regex.Pattern.compile("\"runs\":\\[\\{\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    /** The id of the first search result whose title and channel are the video's, else null. */
    static String pickVideo(String page,String channel,String title){
        var videos=VIDEO.matcher(page);
        int count=0;
        while(videos.find()&&count++<12){
            int start=videos.end(),end=page.indexOf("\"videoRenderer\"",start);
            String block=page.substring(start,end<0?Math.min(page.length(),start+20000):end);
            String hitTitle=run(block,"\"title\":"),owner=run(block,"\"ownerText\":");
            if(!similar(hitTitle,title))continue;
            if(!channel.isEmpty()&&!owner.isEmpty()&&!similar(owner,channel))continue;
            return videos.group(1);
        }
        return null;
    }
    private static String run(String block,String field){
        int at=block.indexOf(field);
        if(at<0)return "";
        var m=RUN.matcher(block);
        if(!m.find(at))return "";
        try{return JsonParser.parseString("\""+m.group(1)+"\"").getAsString();}catch(RuntimeException error){return m.group(1);}
    }
    private static String deezer(String search,String title,String artist)throws Exception{
        String q=(search.isEmpty()?"":search+" ")+title;
        var data=json("https://api.deezer.com/search?limit=5&q="+URLEncoder.encode(q,StandardCharsets.UTF_8)).getAsJsonArray("data");
        if(data!=null)for(var hit:data)if(hit.isJsonObject()){String url=pick(hit.getAsJsonObject(),artist,title);if(url!=null)return url;}
        return null;
    }
    private static final String NETEASE="https://music.163.com/api/";
    /** NetEase: the cover of the first search hit that is this song, read from the song's details (the search has no address). */
    private static String netease(String search,String title,String artist)throws Exception{
        String q=(search.isEmpty()?"":search+" ")+title;
        var result=neteaseJson(NETEASE+"search/get?type=1&limit=10&s="+URLEncoder.encode(q,StandardCharsets.UTF_8)).getAsJsonObject("result");
        if(result==null||!result.has("songs")||!result.get("songs").isJsonArray())return null;
        for(var hit:result.getAsJsonArray("songs")){
            if(!hit.isJsonObject()||!pickNetease(hit.getAsJsonObject(),artist,title))continue;
            var songs=neteaseJson(NETEASE+"song/detail?ids=%5B"+hit.getAsJsonObject().get("id").getAsLong()+"%5D").getAsJsonArray("songs");
            if(songs==null||songs.isEmpty())return null;
            var album=songs.get(0).getAsJsonObject().getAsJsonObject("album");
            String url=album==null?"":text(album,"picUrl");
            if(url.startsWith("http://"))url="https://"+url.substring(7);
            // NetEase scales on request; 250 px like the Deezer covers.
            return url.startsWith("https://")?url+"?param=250y250":null;
        }
        return null;
    }
    /** A NetEase song hit with this title, one of its artists being the song's (when it names one). */
    static boolean pickNetease(JsonObject hit,String artist,String title){
        if(!same(text(hit,"name"),title))return false;
        if(artist.isEmpty())return true;
        if(!hit.has("artists")||!hit.get("artists").isJsonArray())return false;
        for(var a:hit.getAsJsonArray("artists"))if(a.isJsonObject()&&sameArtist(text(a.getAsJsonObject(),"name"),artist))return true;
        return false;
    }
    private static long neteaseNext;
    /** Spaces all NetEase requests (covers and lyrics) a little apart: bursts are what it refuses. */
    static void neteasePace()throws InterruptedException{
        long wait;
        synchronized(CoverLookup.class){
            long now=System.currentTimeMillis();
            wait=Math.max(0,neteaseNext-now);
            neteaseNext=Math.max(now,neteaseNext)+NETEASE_GAP_MS;
        }
        if(wait>0)Thread.sleep(wait);
    }
    private static final long NETEASE_GAP_MS=180;
    private static JsonObject neteaseJson(String url)throws Exception{
        try{return neteaseOnce(url);}
        catch(java.io.IOException refused){
            // Refused once: a short pause usually clears it.
            Thread.sleep(1500);
            return neteaseOnce(url);
        }
    }
    private static JsonObject neteaseOnce(String url)throws Exception{
        neteasePace();
        var response=HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
            .header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64)").header("Referer","https://music.163.com/").GET().build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new java.io.IOException("netease "+response.statusCode());
        var body=JsonParser.parseString(response.body()).getAsJsonObject();
        // Asked too often, NetEase still answers 200 but with an error code ("-460 Cheating") and no songs: an
        // outage, not a "no such song" that would be remembered.
        if(!neteaseOk(body))throw new java.io.IOException("netease code "+body.get("code"));
        return body;
    }
    static boolean neteaseOk(JsonObject body){
        try{return !body.has("code")||body.get("code").getAsInt()==200;}catch(RuntimeException error){return false;}
    }
    private static String deezerAlbum(String artist,String album)throws Exception{
        var data=json("https://api.deezer.com/search/album?limit=5&q="+URLEncoder.encode(artist+" "+album,StandardCharsets.UTF_8)).getAsJsonArray("data");
        if(data!=null)for(var hit:data)if(hit.isJsonObject()){String url=pickAlbum(hit.getAsJsonObject(),artist,album);if(url!=null)return url;}
        return null;
    }
    private static String itunesAlbum(String artist,String album)throws Exception{
        var results=json("https://itunes.apple.com/search?media=music&entity=album&limit=5&term="+URLEncoder.encode(artist+" "+album,StandardCharsets.UTF_8)).getAsJsonArray("results");
        if(results!=null)for(var hit:results)if(hit.isJsonObject()){
            var o=hit.getAsJsonObject();
            if(!same(text(o,"collectionName"),album)||!sameArtist(text(o,"artistName"),artist))continue;
            String url=text(o,"artworkUrl100");
            if(url.startsWith("https://"))return url.replace("100x100bb","200x200bb");
        }
        return null;
    }
    /** The cover of a Deezer album hit with this name by this artist, else null. */
    static String pickAlbum(JsonObject hit,String artist,String album){
        String hitArtist=hit.has("artist")&&hit.get("artist").isJsonObject()?text(hit.getAsJsonObject("artist"),"name"):"";
        if(!same(text(hit,"title"),album)||!sameArtist(hitArtist,artist))return null;
        String url=text(hit,"cover_medium");
        return url.startsWith("https://")?url:null;
    }
    private static String itunes(String search,String title,String artist)throws Exception{
        String q=(search.isEmpty()?"":search+" ")+title;
        var results=json("https://itunes.apple.com/search?media=music&entity=song&limit=5&term="+URLEncoder.encode(q,StandardCharsets.UTF_8)).getAsJsonArray("results");
        if(results!=null)for(var hit:results)if(hit.isJsonObject()){String url=pickItunes(hit.getAsJsonObject(),artist,title);if(url!=null)return url;}
        return null;
    }
    private static JsonObject json(String url)throws Exception{
        var response=HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6)).header("User-Agent","VelaClient").GET().build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new java.io.IOException(URI.create(url).getHost()+" "+response.statusCode());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
    private static byte[] download(String url)throws Exception{
        return png(HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6)).GET().build(),HttpResponse.BodyHandlers.ofByteArray()).body());
    }
    /** The 200 px artwork of an iTunes hit that matches the song, else null. */
    static String pickItunes(JsonObject hit,String artist,String title){
        if(!same(text(hit,"trackName"),title))return null;
        if(!artist.isEmpty()&&!sameArtist(text(hit,"artistName"),artist))return null;
        String url=text(hit,"artworkUrl100");
        return url.startsWith("https://")?url.replace("100x100bb","200x200bb"):null;
    }
    /** The cover address of a search hit that matches the song, else null. */
    static String pick(JsonObject hit,String artist,String title){
        String hitTitle=text(hit,"title"),hitArtist=hit.has("artist")&&hit.get("artist").isJsonObject()?text(hit.getAsJsonObject("artist"),"name"):"";
        if(!same(hitTitle,title))return null;
        if(!artist.isEmpty()&&!sameArtist(hitArtist,artist))return null;
        if(!hit.has("album")||!hit.get("album").isJsonObject())return null;
        var album=hit.getAsJsonObject("album");
        // 250 px is plenty for a 96 px texture and comes much faster than the 500 px "big" one.
        String url=text(album,"cover_medium");
        if(url.isEmpty())url=text(album,"cover_big");
        return url.startsWith("https://")?url:null;
    }
    /** One name contains the other once case, punctuation and spacing are ignored. */
    static boolean similar(String a,String b){
        String x=normal(a),y=normal(b);
        return !x.isEmpty()&&!y.isEmpty()&&(x.contains(y)||y.contains(x));
    }
    /**
     * The same song or album name: equal once cleaned (no "(Remastered)", "feat."), or one is the other with words added
     * at the end. Unlike {@link #similar}, a short name inside a longer one ("Love" in "Lovely Day") is no match.
     */
    static boolean same(String hit,String wanted){
        String x=normal(Lyrics.cleanTitle(hit)),y=normal(Lyrics.cleanTitle(wanted));
        if(x.isEmpty()||y.isEmpty())return false;
        if(x.equals(y))return true;
        String shorter=x.length()<y.length()?x:y,longer=shorter==x?y:x;
        return shorter.length()>=4&&longer.startsWith(shorter)&&shorter.length()*2>=longer.length();
    }
    /**
     * The same artist: one named on each side is equal. Players and catalogues order a collaboration differently
     * ("MellSher, 5opka" against "5opka feat. MellSher").
     */
    static boolean sameArtist(String hit,String wanted){
        var mine=Lyrics.artistNames(wanted).stream().map(CoverLookup::normal).filter(n->!n.isEmpty()).toList();
        for(var name:Lyrics.artistNames(hit))if(mine.contains(normal(name)))return true;
        return false;
    }
    private static String normal(String text){
        return text.toLowerCase(Locale.ROOT).replace('ё','е').replaceAll("[^\\p{L}\\p{N}]+","");
    }
    private static String text(JsonObject json,String key){
        var value=json.get(key);return value==null||value.isJsonNull()?"":value.getAsString();
    }
    /** Minecraft textures read only PNG; Deezer sends JPEG. */
    private static byte[] png(byte[] image)throws Exception{
        var decoded=javax.imageio.ImageIO.read(new ByteArrayInputStream(image));
        if(decoded==null)return null;
        var out=new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(decoded,"png",out);
        return out.toByteArray();
    }
}
