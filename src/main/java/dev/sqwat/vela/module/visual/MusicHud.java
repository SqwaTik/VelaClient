package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.media.CoverLookup;
import dev.sqwat.vela.media.LyricsService;
import dev.sqwat.vela.media.MediaTrack;
import dev.sqwat.vela.media.MediaWatcher;
import dev.sqwat.vela.media.MusicTextures;
import dev.sqwat.vela.media.PlaybackClock;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ColorSetting;
import dev.sqwat.vela.ui.render.VelaTheme;

/**
 * "Music" HUD panel: the song any app on the system is playing (browser tab, Spotify, Yandex Music, any player that
 * reports to Windows), with its cover, the site or app it plays in, a live spectrum of its sound, the progress,
 * play controls and, optionally, timed lyrics. Drawn by HudLayoutService; this class owns the helper process, the
 * textures, the cover and lyrics lookups and the smoothed playback clock.
 */
public final class MusicHud extends ClientModule {
    /** How long a song whose player went away still shows (as paused) before the panel says nothing plays. */
    public static final long PAUSE_LINGER_MS=5000;
    /** Wait this long for the player's own cover before showing one found online. */
    static final long COVER_WAIT_MS=350;
    /** Right after a switch the player may still report the previous song's length: lyrics are picked after this. */
    static final long LYRICS_SETTLE_MS=500;
    /** What the panel shows while nothing plays: it never leaves the screen. */
    private static final MediaTrack IDLE=new MediaTrack("Idle","","","Ничего не играет","","",0,0,0,"",false,false,false,false,false);
    public static boolean idle(MediaTrack track){return track==IDLE;}
    private final BooleanSetting lyrics=setting(new BooleanSetting("lyrics","Показывать текст","Текст песни",false));
    {lyrics.experimental();}
    private final BooleanSetting lyricsClientColor=setting(new BooleanSetting("lyrics_client_color","Цвет клиента","Спетая часть строки цветом темы",true));
    private final ColorSetting lyricsColor=setting(new ColorSetting("lyrics_color","Цвет текста",0xFF8FB1));
    private final BooleanSetting mini=setting(new BooleanSetting("mini","Мини режим","Маленькая капсула сверху по центру экрана",false));
    // Every HUD panel has this switch; HudLayoutService reads it by id.
    private final BooleanSetting transparent=setting(new BooleanSetting("transparent","Прозрачный","Заменяет обычный фон прозрачным стеклом",false));
    {lyricsClientColor.visibleWhen(lyrics::value);lyricsColor.visibleWhen(()->lyrics.value()&&!lyricsClientColor.value());}
    private MediaWatcher watcher;
    private LyricsService lyricsService=new LyricsService(null);
    private CoverLookup covers;
    /** Players whose last song came without a cover of its own: an online one is shown right away for them. */
    private final java.util.Set<String> coverless=new java.util.HashSet<>();
    private boolean songHadCover;
    private final MusicTextures textures=new MusicTextures();
    private final PlaybackClock clock=new PlaybackClock();
    private long lastPlayingMs;
    private MediaTrack lastTrack;
    private long songSince;

    public MusicHud(){
        super("music","Music","Что играет сейчас: браузер, Яндекс Музыка или любой плеер",ModuleCategory.HUD);
        require(MediaWatcher::supported);
    }
    @Override protected void onDisable(){if(watcher!=null)watcher.stop();textures.clear();lastTrack=null;clock.update(null,0);}
    @Override public void onClientTick(){
        if(watcher==null){
            var folder=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("velaclient");
            watcher=new MediaWatcher(folder);covers=new CoverLookup(folder.resolve("covers"));
            lyricsService=new LyricsService(folder.resolve("lyrics"));
        }
        watcher.ensureRunning();
        var live=watcher.track();
        long now=System.currentTimeMillis();
        if(live!=null&&live.playing())lastPlayingMs=now;
        if(live!=null&&!live.sameSong(lastTrack)){
            // The song that just ended tells whether this player sends covers at all.
            if(lastTrack!=null&&now-songSince>4000){if(songHadCover)coverless.remove(lastTrack.app());else coverless.add(lastTrack.app());}
            songSince=now;songHadCover=false;
        }
        if(watcher.playerCover())songHadCover=true;
        if(live!=null)lastTrack=live;
        var shown=track();
        clock.update(shown,now);
        textures.sync(watcher,live);
        // Players without a cover of their own (Yandex Music's app): the lookup starts with the song, and its result is
        // shown once the player had a moment to send its own, or at once for a player known to send none.
        if(live!=null&&watcher.needsCover()){
            byte[] found=covers.get(live);
            if(found!=null&&(now-songSince>=COVER_WAIT_MS||coverless.contains(live.app())))watcher.offerCover(live,found);
        }
        // The lookup starts with the song, before it is drawn and before its length settled (the pick waits for that).
        if(!idle(shown)&&lyrics.value())lyricsService.get(shown);
    }
    /**
     * The song to show, paused ones included; a song whose player went away lingers as paused for
     * {@link #PAUSE_LINGER_MS}. Never null: with nothing playing it is the {@link #idle} placeholder.
     */
    public MediaTrack track(){
        var live=watcher==null?null:watcher.track();
        if(live!=null)return live;
        if(lastTrack!=null&&System.currentTimeMillis()-lastPlayingMs<PAUSE_LINGER_MS)return lastTrack.paused()?lastTrack:lastTrack.withState("Paused");
        return IDLE;
    }
    /** The panel is always on screen while the module is on. */
    public boolean visible(){return true;}
    /** Wall-clock moment the current song first appeared, for the change animation. */
    public long songSince(){return songSince;}
    /** Smoothed position of the shown song. */
    public long position(long nowMs){return clock.position(nowMs);}
    public boolean showLyrics(){return lyrics.value();}
    public boolean mini(){return mini.value();}
    /** Colour of the sung part of a lyric line. */
    public int lyricsColor(){return 0xFF000000|(lyricsClientColor.value()?VelaTheme.PRIMARY:lyricsColor.value())&0xFFFFFF;}
    /**
     * Lyrics of the shown song. The lookup waits until the song settled: right after a switch the player may still
     * report the previous song's length, and a search with it would reject the right text or accept a wrong version.
     */
    public LyricsService.Result lyrics(MediaTrack track){
        if(idle(track))return LyricsService.Result.LOADING;
        if(track.sameSong(lastTrack)&&System.currentTimeMillis()-songSince<LYRICS_SETTLE_MS)return LyricsService.Result.LOADING;
        return lyricsService.get(track);
    }
    public MusicTextures textures(){return textures;}
    /** Loudness and spectrum of the playing app; empty bands when only the loudness is known. */
    public float level(){return watcher==null?0:watcher.level();}
    public float[] bands(){return watcher==null?new float[0]:watcher.bands();}
    public boolean hearing(){return watcher!=null&&watcher.hearing();}
    /** "toggle", "next" or "prev" for the shown player. */
    public void command(String command){if(watcher!=null&&watcher.track()!=null)watcher.command(command);}
    /** Moves the song to {@code positionMs}; the bar jumps right away. */
    public void seek(long positionMs){
        var track=track();if(track==null||!track.hasTimeline())return;
        long target=Math.clamp(positionMs,0,track.durationMs());
        command("seek:"+target);clock.seek(target,System.currentTimeMillis());
    }
}
