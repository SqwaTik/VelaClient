package dev.sqwat.vela.media;

import com.google.gson.JsonObject;

/**
 * One reading of the system's media session (Windows SMTC). {@code updatedMs} is the wall-clock moment the player
 * last reported {@code positionMs}; players report rarely, so the shown position is extrapolated from it
 * (see {@link PlaybackClock}). {@code durationMs} is 0 for live streams and players that publish no timeline.
 * {@code site} is the browser tab the sound comes from (empty for apps), {@code muted} whether that sound is off.
 */
public record MediaTrack(String state,String app,String appName,String title,String artist,String album,
                         long positionMs,long durationMs,long updatedMs,
                         String site,boolean muted,boolean browser,boolean canPrev,boolean canNext,boolean canToggle){
    /** A track without the extras: no tab, not muted, every button available. */
    public MediaTrack(String state,String app,String appName,String title,String artist,String album,long positionMs,long durationMs,long updatedMs){
        this(state,app,appName,title,artist,album,positionMs,durationMs,updatedMs,"",false,false,true,true,true);
    }
    public boolean playing(){return "Playing".equals(state);}
    public boolean paused(){return "Paused".equals(state);}
    public boolean hasTimeline(){return durationMs>0;}
    /** Same song from the same app; position and play state may differ. */
    public boolean sameSong(MediaTrack other){
        return other!=null&&app.equals(other.app)&&title.equals(other.title)&&artist.equals(other.artist);
    }
    /** Position at wall-clock {@code nowMs}, clamped to the song. */
    public long position(long nowMs){
        long position=positionMs;
        if(playing()&&updatedMs>0&&nowMs>updatedMs)position+=nowMs-updatedMs;
        if(durationMs>0)position=Math.min(position,durationMs);
        return Math.max(0,position);
    }
    /** The same reading in another play state, for a song that lingers after its player went away. */
    public MediaTrack withState(String newState){
        return new MediaTrack(newState,app,appName,title,artist,album,positionMs,durationMs,updatedMs,site,muted,browser,canPrev,canNext,canToggle);
    }

    /** A helper line, or null for one without a session ({@code state:"none"}) or without a title. */
    static MediaTrack parse(JsonObject json,MediaTrack previous){
        String state=string(json,"state");
        if(state.isEmpty()||state.equals("none"))return null;
        String title=string(json,"title").strip();
        if(title.isEmpty())return null;
        String app=string(json,"app");
        // The helper names the app only when it changes.
        String appName=json.has("appName")?string(json,"appName"):previous!=null&&previous.app.equals(app)?previous.appName:"";
        if(appName.isBlank())appName=prettyApp(app);
        return new MediaTrack(state,app,appName,title,string(json,"artist").strip(),string(json,"album").strip(),
            number(json,"position"),number(json,"duration"),number(json,"updated"),
            string(json,"site").strip(),flag(json,"muted",false),flag(json,"browser",false),
            flag(json,"canPrev",true),flag(json,"canNext",true),flag(json,"canToggle",true));
    }
    /** "brave.exe" → "Brave", "Microsoft.ZuneMusic_8wekyb3d8bbwe!Microsoft.ZuneMusic" → "ZuneMusic". */
    static String prettyApp(String app){
        String known=MusicBrand.knownApp(app);
        if(known!=null)return known;
        String name=app.replaceAll("^.*[\\\\/]","").replaceAll("!.*$","").replaceAll("(?i)\\.exe$","").replaceAll("_[a-z0-9]{13}$","");
        int dot=name.lastIndexOf('.');if(dot>=0&&dot<name.length()-1)name=name.substring(dot+1);
        return name.isEmpty()?"Плеер":Character.toUpperCase(name.charAt(0))+name.substring(1);
    }
    private static String string(JsonObject json,String key){
        var value=json.get(key);return value==null||value.isJsonNull()?"":value.getAsString();
    }
    private static boolean flag(JsonObject json,String key,boolean fallback){
        try{var value=json.get(key);return value==null||value.isJsonNull()?fallback:value.getAsBoolean();}catch(RuntimeException e){return fallback;}
    }
    private static long number(JsonObject json,String key){
        try{var value=json.get(key);return value==null||value.isJsonNull()?0:Math.max(0,value.getAsLong());}catch(RuntimeException e){return 0;}
    }
}
