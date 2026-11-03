package dev.sqwat.vela.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Follows whatever the system is playing. Windows lists every app that publishes media (browsers, Spotify, Yandex
 * Music, AIMP, any other player) through the media transport controls; a small hidden PowerShell helper reads them
 * and prints one JSON line about twice a second. The helper exits by itself when the game process is gone.
 * Readers call {@link #track()} from any thread; covers and app icons arrive as PNG bytes. While a song plays the
 * helper also streams the app's loudness and spectrum ({@link #level()}, {@link #bands()}), and it takes the
 * play/pause, next and previous commands of the panel's buttons ({@link #command}).
 */
public final class MediaWatcher {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("Vela/Music");
    private static final String SCRIPT="/assets/velaclient/media/now_playing.ps1";
    public static boolean supported(){return System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT).startsWith("windows");}

    private final Path folder;
    private volatile MediaTrack track;
    private volatile byte[] cover;
    /** Bumped with every new cover (also a missing one), so the HUD knows to swap textures. */
    private volatile int coverVersion;
    private final Map<String,byte[]> appIcons=new ConcurrentHashMap<>();
    private volatile long lastLineMs;
    /** The helper had no cover for this song and one was found online instead. */
    private volatile boolean coverFound;
    private volatile float level;
    private volatile float[] bands=new float[0];
    private volatile long levelMs;
    private Process process;
    private Thread reader;
    private volatile boolean running;
    private int failures;
    private long retryAt;

    public MediaWatcher(Path folder){this.folder=folder;}

    public MediaTrack track(){
        // A helper that stopped talking (hung, killed) must not leave a song on screen forever.
        return System.currentTimeMillis()-lastLineMs>6000?null:track;
    }
    public byte[] cover(){return cover;}
    public int coverVersion(){return coverVersion;}
    public byte[] appIcon(String app){return app==null?null:appIcons.get(app);}
    /** Loudness of the playing app (0..1), 0 when nothing came for a moment (pause, mute). */
    public float level(){return System.currentTimeMillis()-levelMs>250?0:level;}
    /** Spectrum of the playing app, low to high (0..1 each); empty when only the loudness is known. */
    public float[] bands(){return System.currentTimeMillis()-levelMs>250?new float[0]:bands;}
    /** True while the helper streams levels: the panel can follow the real sound instead of animating. */
    public boolean hearing(){return System.currentTimeMillis()-levelMs<=250;}
    /** Uses a cover found elsewhere while the player gives none; ignored once the song changed or got a cover. */
    public void offerCover(MediaTrack song,byte[] png){
        if(png==null||cover!=null)return;
        var current=track;
        if(current==null||!current.sameSong(song))return;
        cover=png;coverFound=true;coverVersion++;
    }
    /** The player's own cover is missing for the current song. */
    public boolean needsCover(){return cover==null;}
    /** The current cover came from the player itself, not from an online lookup. */
    public boolean playerCover(){return cover!=null&&!coverFound;}

    /** Sends "toggle", "next" or "prev" to the player that is shown. */
    public synchronized void command(String command){
        if(process==null||!process.isAlive())return;
        try{
            var out=process.getOutputStream();
            out.write((command+"\n").getBytes(StandardCharsets.UTF_8));out.flush();
        }catch(IOException error){LOG.debug("Music command failed",error);}
    }

    /** Starts the helper, or restarts it after a crash with growing pauses. Cheap to call every tick. */
    public synchronized void ensureRunning(){
        if(!supported())return;
        if(process!=null&&process.isAlive())return;
        long now=System.currentTimeMillis();
        if(now<retryAt)return;
        if(process!=null){failures++;process=null;}
        retryAt=now+Math.min(60_000,2000L<<Math.min(5,failures));
        try{start();}catch(IOException|RuntimeException error){LOG.warn("Music helper could not start",error);}
    }
    public synchronized void stop(){
        running=false;
        if(process!=null){process.descendants().forEach(ProcessHandle::destroy);process.destroy();process=null;}
        track=null;cover=null;coverVersion++;failures=0;retryAt=0;
    }

    private void start()throws IOException{
        Path script=folder.resolve("now_playing.ps1");
        try(InputStream in=MediaWatcher.class.getResourceAsStream(SCRIPT)){
            if(in==null)throw new IOException("missing "+SCRIPT);
            byte[] bytes=in.readAllBytes();
            Files.createDirectories(folder);
            if(!Files.exists(script)||!java.util.Arrays.equals(Files.readAllBytes(script),bytes))Files.write(script,bytes);
        }
        var builder=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass",
            "-WindowStyle","Hidden","-File",script.toString(),Long.toString(ProcessHandle.current().pid()));
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        builder.redirectInput(ProcessBuilder.Redirect.PIPE);
        Process started=builder.start();
        process=started;running=true;
        reader=new Thread(()->read(started),"Vela music helper");
        reader.setDaemon(true);reader.start();
    }
    private void read(Process source){
        try(var lines=new BufferedReader(new InputStreamReader(source.getInputStream(),StandardCharsets.UTF_8))){
            String line;
            while(running&&(line=lines.readLine())!=null){
                line=line.strip();
                if(line.startsWith("﻿"))line=line.substring(1);
                if(!line.startsWith("{"))continue;
                try{accept(JsonParser.parseString(line).getAsJsonObject());}catch(RuntimeException error){LOG.debug("Bad music helper line",error);}
            }
        }catch(IOException ignored){}
    }
    void accept(JsonObject json){
        lastLineMs=System.currentTimeMillis();
        if(json.has("error")){track=null;return;}
        if(json.has("lv")&&!json.has("state")){acceptLevel(json);return;}
        MediaTrack next=MediaTrack.parse(json,track);
        String app=next!=null?next.app():"";
        if(json.has("appIcon")&&!app.isEmpty()){byte[] icon=decode(json,"appIcon");if(icon!=null)appIcons.put(app,icon);}
        boolean newSong=next==null||!next.sameSong(track);
        if(json.has("cover")){
            byte[] sent=decode(json,"cover");
            // A "no cover yet" at the start of a song must not throw away one found online for the same song.
            if(sent!=null||newSong||!coverFound){cover=sent;coverFound=false;coverVersion++;}
        }else if(newSong){if(cover!=null){cover=null;coverFound=false;coverVersion++;}}
        track=next;
        if(next!=null)synchronized(this){failures=0;}
    }
    private void acceptLevel(JsonObject json){
        try{level=Math.clamp(json.get("lv").getAsFloat(),0,1);}catch(RuntimeException e){level=0;}
        float[] next=new float[0];
        if(json.has("b")&&json.get("b").isJsonArray()){
            var array=json.getAsJsonArray("b");
            next=new float[array.size()];
            for(int i=0;i<next.length;i++)try{next[i]=Math.clamp(array.get(i).getAsFloat(),0,1);}catch(RuntimeException e){next[i]=0;}
        }
        bands=next;levelMs=System.currentTimeMillis();
    }
    private static byte[] decode(JsonObject json,String key){
        var value=json.get(key);
        if(value==null||value.isJsonNull())return null;
        try{byte[] bytes=Base64.getDecoder().decode(value.getAsString());return bytes.length>8?bytes:null;}catch(IllegalArgumentException e){return null;}
    }
}
