package dev.sqwat.vela.media;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds song text. Four free sources without keys are asked at the same time the moment a song starts: LRCLIB
 * (search by fields and free text), Deezer (a big licensed catalogue, Russian rap and pop included), NetEase Cloud
 * Music (many Russian and Asian songs, some timed word by word) and QQ Music (a large catalogue, Russian rap included). Every text they offer is kept as a candidate; which one is shown is decided locally, and again
 * whenever the song's length changes: title and artist agree, its length is within {@link #LENGTH_SLACK_S} of the
 * playing one (another version is off by seconds and every line comes late), and its stamps cover the song (see
 * {@link Lyrics#plausible}); of those, the one whose timing most of the others agree with. The first good candidate shows at once, without waiting for the slower source. Plain text
 * without times is shown only when no source has the song timed, paced over the song by syllables. Candidates are kept on
 * disk, so a song heard before has its text instantly, without the network. A source that is down (LRCLIB answers
 * "server busy" at times) is asked again a few times in a row, and a song left without fitting text is looked up again
 * a little later, so an outage does not leave it without text for the rest of the session.
 */
public final class LyricsService {
    public enum State{LOADING,FOUND,MISSING,INSTRUMENTAL}
    public record Result(State state,Lyrics lyrics){
        public static final Result LOADING=new Result(State.LOADING,Lyrics.NONE);
        static final Result MISSING=new Result(State.MISSING,Lyrics.NONE);
    }
    /** One text a source offered for a song, with the song's name and length as the source knows them. */
    static final class Candidate{
        final String artist,title,lrc,plain;final double seconds;final boolean instrumental;
        private Lyrics parsed;
        Candidate(String artist,String title,double seconds,String lrc,boolean instrumental){this(artist,title,seconds,lrc,"",instrumental);}
        Candidate(String artist,String title,double seconds,String lrc,String plain,boolean instrumental){
            this.artist=artist;this.title=title;this.seconds=seconds;this.lrc=unmangle(lrc==null?"":lrc);this.plain=unmangle(plain==null?"":plain);this.instrumental=instrumental;
        }
        Lyrics lyrics(){if(parsed==null)parsed=Lyrics.parseSynced(lrc);return parsed;}
        JsonObject json(){
            var o=new JsonObject();o.addProperty("artist",artist);o.addProperty("title",title);o.addProperty("seconds",seconds);
            o.addProperty("lrc",lrc);if(!plain.isBlank())o.addProperty("plain",plain);if(instrumental)o.addProperty("instrumental",true);return o;
        }
        static Candidate of(JsonObject o){return new Candidate(text(o,"artist"),text(o,"title"),number(o,"seconds"),text(o,"lrc"),text(o,"plain"),o.has("instrumental"));}
    }
    /** Seconds a hit's length may differ from the song's. */
    static final double LENGTH_SLACK_S=3;
    private static final int DISK_LIMIT=400;
    /** Pauses before a song still without fitting text is looked up again: after 15 s, 30 s, then 60 s. */
    private static final long[] RETRY_MS={15_000,30_000,60_000};
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("Vela/Lyrics");
    private static final String LRCLIB="https://lrclib.net/api/";
    private static final String BROWSER="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36";
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
    /** NetEase credit lines ("作词 : …", "作曲 : …") that come as timed lines before the text. */
    private static final Pattern CREDIT=Pattern.compile("(?m)^(\\[[^\\]]*])+\\s*[^\\[\\n]{0,12}?[\\p{IsHan}][^\\[\\n]{0,12}?[:：].*$");
    private final Map<String,Search> cache=new LinkedHashMap<>(16,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Search> eldest){return size()>64;}
    };
    private final Path folder;
    /** LRCLIB said "too many requests": no LRCLIB requests before this moment. */
    private static volatile long quietUntil;

    /** {@code folder} keeps the candidates of songs heard before; null keeps them in memory only. */
    public LyricsService(Path folder){this.folder=folder;}

    /** The lookup of one song: candidates arrive from the sources while it runs; the pick is redone when the length changes. */
    private static final class Search{
        final List<Lyrics.Query> queries;
        final List<Candidate> found=new CopyOnWriteArrayList<>();
        volatile boolean done,reached;
        volatile long retryAt;
        int attempts;
        int version,pickVersion=-1;long pickSeconds=-1;Result pick;
        Search(List<Lyrics.Query> queries){this.queries=queries;}
        synchronized void add(List<Candidate> more){if(!more.isEmpty()){found.addAll(more);version++;}}
        synchronized Result result(long seconds){
            if(version==pickVersion&&seconds==pickSeconds&&pick!=null)return pick;
            pickVersion=version;pickSeconds=seconds;
            Result best=pick(found,queries,seconds);
            // Plain text only once every source answered without timed text: a timed copy may still be on its way.
            if(best==null&&done)best=plainPick(found,queries,seconds);
            if(best==null)best=done?Result.MISSING:Result.LOADING;
            return pick=best;
        }
    }

    /** Text for a song, starting the lookup on first ask; {@link State#LOADING} until a candidate fits or all sources answered. */
    public Result get(MediaTrack track){
        if(track==null)return Result.MISSING;
        String key=Lyrics.key(track.artist(),track.title());
        Search search;
        synchronized(this){
            search=cache.get(key);
            if(search==null){
                search=new Search(Lyrics.queries(track.artist(),track.title()));
                cache.put(key,search);
                start(key,search,true);
            }else if(search.done&&search.attempts<=RETRY_MS.length&&System.currentTimeMillis()>=search.retryAt
                    &&!timed(search.result(track.durationMs()/1000))){
                // The cached pick: this runs every frame, the candidates are only looked through again when they change.
                // Nothing fitting yet (a source was down, or only other versions came): asked again, keeping what came.
                search.done=false;
                start(key,search,false);
            }
        }
        return search.result(track.durationMs()/1000);
    }

    private void start(String key,Search search,boolean disk){
        int attempt=search.attempts++;
        Thread.ofVirtual().name("Vela lyrics").start(()->{
            try{
                var known=disk?readDisk(key):null;
                if(known!=null){search.reached=true;search.add(known);return;}
                for(var query:search.queries){
                    // All sources of one query at once; the next query only when nothing fitting came.
                    var tasks=List.<java.util.concurrent.Callable<List<Candidate>>>of(
                        ()->lrclib(fieldsSearch(query)),()->lrclib(textSearch(query)),()->deezer(query),()->netease(query),()->qq(query));
                    try(var scope=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
                        var futures=tasks.stream().map(task->scope.submit(()->{
                            List<Candidate> got=task.call();
                            if(got!=null){search.reached=true;search.add(got);}
                            return got;
                        })).toList();
                        for(var future:futures)try{future.get();}catch(java.util.concurrent.ExecutionException error){LOG.debug("Lyrics source failed",error.getCause());}
                    }
                    if(timed(pick(search.found,search.queries,0)))break;
                }
                // A miss is not kept: the sources may get the text later.
                if(pick(search.found,search.queries,0)!=null||plainPick(search.found,search.queries,0)!=null)writeDisk(key,search.found);
            }catch(Exception error){
                LOG.debug("Lyrics lookup failed",error);
            }finally{
                search.retryAt=System.currentTimeMillis()+RETRY_MS[Math.min(attempt,RETRY_MS.length-1)];
                synchronized(search){search.done=true;search.version++;}
            }
        });
    }

    // ---- picking ----

    /**
     * The candidate that is this song with plausible timed text; instrumental when that is all; null when none fits
     * (plain text without times included). Copies of one song often disagree by a few hundred milliseconds or even
     * seconds (someone timed it by ear, or to another release), so the text the most other candidates agree with on
     * timing is taken (see {@link #agreement}); then word-by-word timing; then the closest length.
     */
    static Result pick(List<Candidate> candidates,List<Lyrics.Query> queries,long seconds){
        var fitting=new ArrayList<Candidate>();var texts=new java.util.HashSet<String>();boolean instrumental=false;
        for(var c:candidates){
            if(queries.stream().noneMatch(q->matches(c,q,seconds)))continue;
            if(c.instrumental){instrumental=true;continue;}
            // The same text found twice (two searches, two queries) is one vote, not two.
            if(c.lyrics().plausible(seconds*1000)&&texts.add(c.lrc))fitting.add(c);
        }
        if(fitting.isEmpty())return instrumental?new Result(State.INSTRUMENTAL,Lyrics.INSTRUMENTAL):null;
        var keys=fitting.stream().map(c->keyLines(c.lyrics())).toList();
        Candidate pick=null;int pickVotes=-1;boolean pickWords=false;double pickOff=Double.MAX_VALUE;
        for(int i=0;i<fitting.size();i++){
            var c=fitting.get(i);
            int votes=0;
            for(int j=0;j<fitting.size();j++){
                if(i==j)continue;
                Long off=agreement(keys.get(i),keys.get(j));
                if(off!=null&&Math.abs(off)<=AGREE_MS)votes++;
            }
            boolean words=wordTimed(c.lyrics());
            double off=seconds>0&&c.seconds>0?Math.abs(c.seconds-seconds):0;
            boolean better=votes!=pickVotes?votes>pickVotes:words!=pickWords?words:off<pickOff;
            if(better){pick=c;pickVotes=votes;pickWords=words;pickOff=off;}
        }
        return new Result(State.FOUND,pick.lyrics());
    }
    /**
     * Plain text of this song when no source has it timed: its lines are paced over the song by their syllables
     * ({@link Lyrics#parsePlain}). Rough, but a song with text everywhere except timed is better shown than blank.
     * The copy closest in length wins.
     */
    static Result plainPick(List<Candidate> candidates,List<Lyrics.Query> queries,long seconds){
        Candidate best=null;double bestOff=Double.MAX_VALUE;
        for(var c:candidates){
            if(c.instrumental||c.plain.isBlank()||queries.stream().noneMatch(q->matches(c,q,seconds)))continue;
            if(Lyrics.parsePlain(c.plain).lines().stream().filter(l->!l.text().isEmpty()).count()<6)continue;
            double off=seconds>0&&c.seconds>0?Math.abs(c.seconds-seconds):0;
            if(off<bestOff){best=c;bestOff=off;}
        }
        return best==null?null:new Result(State.FOUND,Lyrics.parsePlain(best.plain));
    }
    /**
     * Russian text that went through a wrong code page on the way (UTF-8 bytes read as
     * windows-1251, which some catalogue entries carry) turned back; any other text as it is.
     */
    static String unmangle(String text){
        if(text.isEmpty())return text;
        int letters=0,marks=0;
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(Character.isLetter(c))letters++;
            if((c=='\u0420'||c=='\u0421')&&i+1<text.length()&&!isPlainCyrillic(text.charAt(i+1)))marks++;
        }
        if(letters==0||marks*5<letters)return text;
        var bytes=text.getBytes(java.nio.charset.Charset.forName("windows-1251"));
        var decoder=StandardCharsets.UTF_8.newDecoder();
        try{return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();}
        catch(java.nio.charset.CharacterCodingException error){return text;}
    }
    /** Lower-case a..ya: the letters a mangled text's second byte is almost never. */
    private static boolean isPlainCyrillic(char c){return c>='\u0430'&&c<='\u044F';}
    /** Two texts whose lines start within this of each other are timed alike. */
    static final long AGREE_MS=250;
    record KeyLine(long timeMs,String key){}
    /** Lines long enough to tell apart, as letters only in lower case, with their start. */
    static List<KeyLine> keyLines(Lyrics lyrics){
        var out=new ArrayList<KeyLine>();
        for(var line:lyrics.lines()){
            var key=new StringBuilder();
            line.text().codePoints().filter(Character::isLetter).map(Character::toLowerCase).forEach(key::appendCodePoint);
            if(key.length()>=4)out.add(new KeyLine(line.timeMs(),key.toString()));
        }
        return out;
    }
    private static boolean wordTimed(Lyrics lyrics){
        for(var line:lyrics.lines())if(line.keyTimes()!=null&&line.keyTimes().length>0)return true;
        return false;
    }
    /**
     * How much later text {@code b} starts its lines than text {@code a}, in milliseconds: the most common difference
     * between lines with the same words (in 100 ms steps, then the median of those near it, so a chorus matched to
     * another chorus does not count). Null when fewer than three lines agree on one difference: texts too different to tell.
     */
    static Long agreement(List<KeyLine> a,List<KeyLine> b){
        var at=new java.util.HashMap<String,List<Long>>();
        for(var line:b)at.computeIfAbsent(line.key(),k->new ArrayList<>()).add(line.timeMs());
        var diffs=new ArrayList<Long>();
        for(var line:a){var times=at.get(line.key());if(times!=null)for(long t:times)diffs.add(t-line.timeMs());}
        if(diffs.size()<3)return null;
        var bins=new java.util.HashMap<Long,Integer>();
        long best=0;int bestCount=0;
        for(long d:diffs){
            long bin=Math.round(d/100.0);int count=bins.merge(bin,1,Integer::sum);
            if(count>bestCount){bestCount=count;best=bin;}
        }
        long center=best*100;
        var near=diffs.stream().filter(d->Math.abs(d-center)<=150).sorted().toList();
        return near.size()<3?null:near.get(near.size()/2);
    }
    /** Timed text or a known instrumental: nothing better can come from asking further. */
    private static boolean timed(Result r){return r!=null&&(r.state==State.INSTRUMENTAL||r.lyrics.synced());}
    /** LRCLIB search hits that are this song with plausible timed text; null when none is. Plain text alone never counts. */
    static Result best(JsonArray hits,Lyrics.Query query,long seconds){
        var r=pick(lrclibCandidates(hits),List.of(query),seconds);
        return r!=null&&r.state==State.FOUND&&r.lyrics.synced()?r:null;
    }
    /** Same title, same artist (when known), and the same length within the slack (when both are known). */
    static boolean matches(Candidate c,Lyrics.Query query,long seconds){
        boolean named=CoverLookup.similar(Lyrics.cleanTitle(c.title),query.title())
            &&(query.artist().isEmpty()||CoverLookup.similar(c.artist,query.artist()));
        // Some LRCLIB entries have the artist and the title the wrong way round ("Song" by "Band").
        boolean swapped=!query.artist().isEmpty()&&CoverLookup.similar(Lyrics.cleanTitle(c.artist),query.title())&&CoverLookup.similar(c.title,query.artist());
        if(!named&&!swapped)return false;
        if(seconds>0&&c.seconds>0&&Math.abs(c.seconds-seconds)>LENGTH_SLACK_S)return false;
        // Without the song's length the title alone is too weak a match: an artist must agree.
        return seconds>0||!query.artist().isEmpty();
    }

    // ---- sources ----

    static String fieldsSearch(Lyrics.Query query){
        return query.artist().isEmpty()?"search?q="+enc(query.title()):"search?track_name="+enc(query.title())+"&artist_name="+enc(query.artist());
    }
    static String textSearch(Lyrics.Query query){return "search?q="+enc((query.artist()+" "+query.title()).strip());}
    /** LRCLIB, asked up to three times when it is busy (503) or unreachable. */
    private static List<Candidate> lrclib(String path)throws InterruptedException{
        for(long pause:new long[]{700,1800,0}){
            var got=lrclibOnce(path);
            if(got!=BUSY)return got;
            if(pause>0)Thread.sleep(pause);
        }
        return null;
    }
    /** Marker answer of one LRCLIB request that is worth repeating. */
    private static final List<Candidate> BUSY=new ArrayList<>();
    private static List<Candidate> lrclibOnce(String path)throws InterruptedException{
        if(System.currentTimeMillis()<quietUntil)return null;
        var request=HttpRequest.newBuilder(URI.create(LRCLIB+path)).timeout(Duration.ofSeconds(8))
            .header("User-Agent","Vela Client (https://github.com/sqwat)").header("Accept","application/json").GET().build();
        try{
            var response=HTTP.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if(response.statusCode()==429){
                long wait=response.headers().firstValue("Retry-After").map(v->{try{return Long.parseLong(v.strip());}catch(NumberFormatException e){return 30L;}}).orElse(30L);
                quietUntil=System.currentTimeMillis()+Math.clamp(wait,5,300)*1000;
                return null;
            }
            if(response.statusCode()==404)return List.of();
            if(response.statusCode()/100==5)return BUSY;
            if(response.statusCode()/100!=2)return null;
            JsonElement body=JsonParser.parseString(response.body());
            return body.isJsonArray()?lrclibCandidates(body.getAsJsonArray()):List.of();
        }catch(java.io.IOException error){return BUSY;}
        catch(RuntimeException error){return null;}
    }
    static List<Candidate> lrclibCandidates(JsonArray hits){
        var out=new ArrayList<Candidate>();
        for(var hit:hits){
            if(!hit.isJsonObject())continue;var o=hit.getAsJsonObject();
            boolean instrumental=o.has("instrumental")&&!o.get("instrumental").isJsonNull()&&o.get("instrumental").getAsBoolean();
            String lrc=text(o,"syncedLyrics"),plain=text(o,"plainLyrics");
            if(lrc.isBlank()&&plain.isBlank()&&!instrumental)continue;
            out.add(new Candidate(text(o,"artistName"),text(o,"trackName"),number(o,"duration"),lrc,plain,instrumental));
        }
        return out;
    }
    // ---- Deezer ----

    /** Anonymous Deezer session (a short-lived token anyone gets without an account) and when it is renewed. */
    private static volatile String deezerJwt;
    private static volatile long deezerJwtUntil;
    private static final String DEEZER_LYRICS="query GetLyrics($trackId: String!) { track(trackId: $trackId) { lyrics { text synchronizedLines { line milliseconds } } } }";
    /** Deezer: the public search, then the timed (or plain) text of the hits that fit; null when Deezer could not be reached. */
    private static List<Candidate> deezer(Lyrics.Query query)throws Exception{
        String q=query.artist().isEmpty()?query.title():"artist:\""+query.artist()+"\" track:\""+query.title()+"\"";
        var songs=deezerSongs(q,query);
        if(songs!=null&&songs.isEmpty()&&!query.artist().isEmpty())songs=deezerSongs((query.artist()+" "+query.title()).strip(),query);
        if(songs==null)return null;
        if(songs.isEmpty())return List.of();
        String jwt=deezerJwt();
        if(jwt==null)return null;
        var out=new CopyOnWriteArrayList<Candidate>();
        try(var scope=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
            for(var o:songs)scope.submit(()->{
                var body=new JsonObject();body.addProperty("operationName","GetLyrics");body.addProperty("query",DEEZER_LYRICS);
                var vars=new JsonObject();vars.addProperty("trackId",String.valueOf(o.get("id").getAsLong()));body.add("variables",vars);
                var request=HttpRequest.newBuilder(URI.create("https://pipe.deezer.com/api")).timeout(Duration.ofSeconds(10))
                    .header("User-Agent",BROWSER).header("Authorization","Bearer "+jwt).header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
                try{
                    var response=HTTP.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if(response.statusCode()==401)deezerJwtUntil=0;
                    if(response.statusCode()/100!=2)return null;
                    var lyrics=deezerLyrics(JsonParser.parseString(response.body()));
                    if(lyrics!=null)out.add(new Candidate(text(o,"_artist"),text(o,"title"),number(o,"duration"),lyrics[0],lyrics[1],false));
                }catch(java.io.IOException|RuntimeException ignored){}
                return null;
            });
        }
        return List.copyOf(out);
    }
    /** {timed text as LRC, plain text} from a Deezer lyrics answer; null when it has neither. */
    static String[] deezerLyrics(JsonElement answer){
        try{
            var track=answer.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("track");
            if(track==null||!track.has("lyrics")||!track.get("lyrics").isJsonObject())return null;
            var lyrics=track.getAsJsonObject("lyrics");
            var lrc=new StringBuilder();
            if(lyrics.has("synchronizedLines")&&lyrics.get("synchronizedLines").isJsonArray())
                for(var e:lyrics.getAsJsonArray("synchronizedLines")){
                    var line=e.getAsJsonObject();
                    lrc.append('[').append(stamp((long)number(line,"milliseconds"))).append(']').append(text(line,"line")).append('\n');
                }
            String plain=text(lyrics,"text");
            return lrc.isEmpty()&&plain.isBlank()?null:new String[]{lrc.toString(),plain};
        }catch(RuntimeException error){return null;}
    }
    private static List<JsonObject> deezerSongs(String q,Lyrics.Query query)throws InterruptedException{
        var request=HttpRequest.newBuilder(URI.create("https://api.deezer.com/search?limit=10&q="+enc(q))).timeout(Duration.ofSeconds(8))
            .header("User-Agent",BROWSER).GET().build();
        try{
            var response=HTTP.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if(response.statusCode()/100!=2)return null;
            var body=JsonParser.parseString(response.body()).getAsJsonObject();
            if(!body.has("data")||!body.get("data").isJsonArray())return body.has("error")?null:List.of();
            var songs=new ArrayList<JsonObject>();
            for(var e:body.getAsJsonArray("data")){
                if(!e.isJsonObject())continue;var o=e.getAsJsonObject();
                String artist=o.has("artist")&&o.get("artist").isJsonObject()?text(o.getAsJsonObject("artist"),"name"):"";
                if(!CoverLookup.similar(Lyrics.cleanTitle(text(o,"title")),query.title()))continue;
                if(!query.artist().isEmpty()&&!CoverLookup.similar(artist,query.artist()))continue;
                o.addProperty("_artist",artist);songs.add(o);
                if(songs.size()>=3)break;
            }
            return songs;
        }catch(java.io.IOException|RuntimeException error){return null;}
    }
    private static String deezerJwt()throws InterruptedException{
        if(deezerJwt!=null&&System.currentTimeMillis()<deezerJwtUntil)return deezerJwt;
        synchronized(LyricsService.class){
            if(deezerJwt!=null&&System.currentTimeMillis()<deezerJwtUntil)return deezerJwt;
            var request=HttpRequest.newBuilder(URI.create("https://auth.deezer.com/login/anonymous?jo=p&rto=c&i=c")).timeout(Duration.ofSeconds(8))
                .header("User-Agent",BROWSER).GET().build();
            try{
                var response=HTTP.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if(response.statusCode()/100!=2)return null;
                String jwt=text(JsonParser.parseString(response.body()).getAsJsonObject(),"jwt");
                if(jwt.isBlank())return null;
                deezerJwt=jwt;deezerJwtUntil=System.currentTimeMillis()+10*60_000;
                return jwt;
            }catch(java.io.IOException|RuntimeException error){return null;}
        }
    }

    /** NetEase: the search's songs whose title and artist fit, each with its timed text (asked in parallel). */
    private static List<Candidate> netease(Lyrics.Query query)throws Exception{
        var songs=neteaseSongs((query.artist()+" "+query.title()).strip(),query,6);
        // Several artists in one name ("A, B") can throw the joint search off: the title alone, filtered by artist.
        if(songs!=null&&songs.isEmpty()&&!query.artist().isEmpty())songs=neteaseSongs(query.title(),query,20);
        if(songs==null)return null;
        var out=new CopyOnWriteArrayList<Candidate>();
        try(var scope=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
            for(var o:songs)scope.submit(()->{
                var lyric=neteaseJson("https://music.163.com/api/song/lyric?lv=1&yv=1&id="+o.get("id").getAsLong());
                if(lyric==null)return null;
                // Word-by-word timing where NetEase has it, the line timing otherwise.
                String lrc=lyric.has("yrc")&&lyric.get("yrc").isJsonObject()?yrcToLrc(text(lyric.getAsJsonObject("yrc"),"lyric")):"";
                if(Lyrics.parseSynced(lrc).lines().size()<6&&lyric.has("lrc")&&lyric.get("lrc").isJsonObject())
                    lrc=stripCredits(text(lyric.getAsJsonObject("lrc"),"lyric"));
                if(!lrc.isBlank())out.add(new Candidate(text(o,"_artist"),text(o,"name"),number(o,"duration")/1000,lrc,false));
                return null;
            });
        }
        return List.copyOf(out);
    }
    /** Up to three search hits whose title and artist fit; null when NetEase could not be reached. */
    private static List<JsonObject> neteaseSongs(String text,Lyrics.Query query,int limit)throws InterruptedException{
        var search=neteaseJson("https://music.163.com/api/search/get?type=1&limit="+limit+"&s="+enc(text));
        if(search==null)return null;
        var result=search.getAsJsonObject("result");
        if(result==null||!result.has("songs")||!result.get("songs").isJsonArray())return List.of();
        var songs=new ArrayList<JsonObject>();
        for(var song:result.getAsJsonArray("songs")){
            if(!song.isJsonObject())continue;var o=song.getAsJsonObject();
            String artist=o.has("artists")&&o.get("artists").isJsonArray()&&!o.getAsJsonArray("artists").isEmpty()?text(o.getAsJsonArray("artists").get(0).getAsJsonObject(),"name"):"";
            if(!CoverLookup.similar(Lyrics.cleanTitle(text(o,"name")),query.title()))continue;
            if(!query.artist().isEmpty()&&!CoverLookup.similar(artist,query.artist()))continue;
            o.addProperty("_artist",artist);songs.add(o);
            if(songs.size()>=3)break;
        }
        return songs;
    }
    private static final Pattern YRC_LINE=Pattern.compile("^\\[(\\d+),(\\d+)]");
    private static final Pattern YRC_WORD=Pattern.compile("\\((\\d+),(\\d+),-?\\d+\\)");
    private static final Pattern CREDIT_TEXT=Pattern.compile("^.{0,12}?\\p{IsHan}.{0,12}?[:：]");
    /**
     * NetEase's word timing ({@code [lineStart,lineLength](wordStart,wordLength,0)word…}, milliseconds) as enhanced
     * LRC with a stamp where every word starts and ends, so a held word and the gap after it keep their time. Credit
     * lines ("作词 : …") and the JSON lines some songs start with are left out.
     */
    static String yrcToLrc(String yrc){
        var out=new StringBuilder();
        for(String raw:yrc.split("\\R")){
            String line=raw.strip();
            Matcher head=YRC_LINE.matcher(line);
            if(!head.find())continue;
            var text=new StringBuilder();var words=new StringBuilder();
            Matcher word=YRC_WORD.matcher(line);word.region(head.end(),line.length());
            int at=-1;long wordEnd=-1;
            while(true){
                boolean found=word.find();
                if(at>=0){
                    String w=line.substring(at,found?word.start():line.length());
                    text.append(w);words.append(w.stripTrailing());
                    words.append('<').append(stamp(wordEnd)).append('>');
                    if(w.length()>w.stripTrailing().length())words.append(' ');
                }
                if(!found)break;
                long start=Long.parseLong(word.group(1));wordEnd=start+Long.parseLong(word.group(2));
                words.append('<').append(stamp(start)).append('>');at=word.end();
            }
            if(text.toString().isBlank()||CREDIT_TEXT.matcher(text.toString().strip()).find())continue;
            out.append('[').append(stamp(Long.parseLong(head.group(1)))).append(']').append(words).append('\n');
        }
        return out.toString();
    }
    private static String stamp(long ms){return String.format(java.util.Locale.ROOT,"%02d:%02d.%03d",ms/60_000,ms/1000%60,ms%1000);}
    static String stripCredits(String lrc){return CREDIT.matcher(lrc).replaceAll("");}
    private static JsonObject neteaseJson(String url)throws InterruptedException{
        var request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12))
            .header("User-Agent",BROWSER).header("Referer","https://music.163.com/").GET().build();
        CoverLookup.neteasePace();
        try{
            var response=HTTP.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if(response.statusCode()/100!=2)return null;
            var body=JsonParser.parseString(response.body());
            // "-460 Cheating" when asked too often: unreachable for now, not "no text".
            return body.isJsonObject()&&CoverLookup.neteaseOk(body.getAsJsonObject())?body.getAsJsonObject():null;
        }catch(java.io.IOException|RuntimeException error){return null;}
    }

    // ---- QQ Music ----

    private static final String QQ="https://c.y.qq.com/";
    /** QQ Music: a large catalogue (Russian rap included), timed text for many songs; null when it could not be reached. */
    private static List<Candidate> qq(Lyrics.Query query)throws Exception{
        var search=qqJson(QQ+"soso/fcgi-bin/client_search_cp?format=json&p=1&n=10&w="+enc((query.artist()+" "+query.title()).strip()));
        if(search==null)return null;
        var songs=qqSongs(search,query);
        var out=new CopyOnWriteArrayList<Candidate>();
        try(var scope=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
            for(var o:songs)scope.submit(()->{
                var lyric=qqJson(QQ+"lyric/fcgi-bin/fcg_query_lyric_new.fcg?format=json&nobase64=1&g_tk=5381&songmid="+enc(text(o,"songmid")));
                if(lyric==null)return null;
                String body=stripCredits(unescape(text(lyric,"lyric")));
                if(body.isBlank())return null;
                boolean timed=!Lyrics.parseSynced(body).lines().isEmpty();
                out.add(new Candidate(text(o,"_artist"),text(o,"songname"),number(o,"interval"),timed?body:"",timed?"":body,false));
                return null;
            });
        }
        return List.copyOf(out);
    }
    /** Up to three QQ search hits whose title fits and one of whose artists is the song's. */
    static List<JsonObject> qqSongs(JsonObject search,Lyrics.Query query){
        var songs=new ArrayList<JsonObject>();
        try{
            for(var e:search.getAsJsonObject("data").getAsJsonObject("song").getAsJsonArray("list")){
                if(!e.isJsonObject())continue;var o=e.getAsJsonObject();
                if(!CoverLookup.similar(Lyrics.cleanTitle(text(o,"songname")),query.title()))continue;
                var artists=new ArrayList<String>();
                if(o.has("singer")&&o.get("singer").isJsonArray())for(var a:o.getAsJsonArray("singer"))if(a.isJsonObject())artists.add(text(a.getAsJsonObject(),"name"));
                String artist=String.join(", ",artists);
                if(!query.artist().isEmpty()&&!CoverLookup.similar(artist,query.artist())&&artists.stream().noneMatch(a->CoverLookup.similar(a,query.artist())))continue;
                // The song's own artist line, so the match against the query later is the same as here.
                o.addProperty("_artist",query.artist().isEmpty()?artist:query.artist());songs.add(o);
                if(songs.size()>=3)break;
            }
        }catch(RuntimeException error){return List.of();}
        return songs;
    }
    private static final Pattern ENTITY=Pattern.compile("&(#\\d+|#x[0-9a-fA-F]+|amp|lt|gt|quot|apos);");
    /** QQ sends its text with HTML entities ("&#39;", "&amp;"). */
    static String unescape(String text){
        return ENTITY.matcher(text).replaceAll(m->{
            String e=m.group(1);
            String c=switch(e){
                case "amp"->"&";case "lt"->"<";case "gt"->">";case "quot"->"\"";case "apos"->"'";
                default->Character.toString(e.startsWith("#x")?Integer.parseInt(e.substring(2),16):Integer.parseInt(e.substring(1)));
            };
            return Matcher.quoteReplacement(c);
        });
    }
    private static JsonObject qqJson(String url)throws InterruptedException{
        var request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
            .header("User-Agent",BROWSER).header("Referer","https://y.qq.com/").GET().build();
        try{
            var response=HTTP.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if(response.statusCode()/100!=2)return null;
            var body=JsonParser.parseString(response.body());
            return body.isJsonObject()?body.getAsJsonObject():null;
        }catch(java.io.IOException|RuntimeException error){return null;}
    }

    // ---- disk ----

    private List<Candidate> readDisk(String key){
        if(folder==null)return null;
        var file=folder.resolve(fileName(key));
        try{
            if(!Files.isRegularFile(file))return null;
            Files.setLastModifiedTime(file,java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
            var array=JsonParser.parseString(Files.readString(file,StandardCharsets.UTF_8)).getAsJsonArray();
            var out=new ArrayList<Candidate>();
            for(var e:array)if(e.isJsonObject())out.add(Candidate.of(e.getAsJsonObject()));
            return out;
        }catch(java.io.IOException|RuntimeException error){return null;}
    }
    private void writeDisk(String key,List<Candidate> candidates){
        if(folder==null)return;
        try{
            Files.createDirectories(folder);
            var array=new JsonArray();
            for(var c:candidates)array.add(c.json());
            Files.writeString(folder.resolve(fileName(key)),array.toString(),StandardCharsets.UTF_8);
            // Least recently played songs go first once there are too many.
            try(var files=Files.list(folder)){
                var all=files.filter(f->f.toString().endsWith(".json")).sorted(java.util.Comparator.comparingLong(f->f.toFile().lastModified())).toList();
                for(int i=0;i<all.size()-DISK_LIMIT;i++)Files.deleteIfExists(all.get(i));
            }
        }catch(java.io.IOException error){LOG.debug("Lyrics cache write failed",error);}
    }
    private static String fileName(String key){return CoverLookup.fileName(key).replace(".png",".json");}

    private static String text(JsonObject o,String key){var v=o.get(key);return v==null||v.isJsonNull()?"":v.getAsString();}
    private static double number(JsonObject o,String key){try{var v=o.get(key);return v==null||v.isJsonNull()?0:v.getAsDouble();}catch(RuntimeException e){return 0;}}
    private static String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
