package dev.sqwat.vela.media;

import dev.sqwat.vela.ui.Lang;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The music service behind a song: a site playing in a browser tab (YouTube, Twitch…) or a known player app. Gives the
 * panel a proper name and the service's own logo (bundled in textures/music) instead of the browser's icon.
 */
public enum MusicBrand {
    YOUTUBE_MUSIC("YouTube Music",0xFFFF0033,"youtube music","music.youtube"),
    YOUTUBE("YouTube",0xFFFF0033,"youtube","youtu.be"),
    TWITCH("Twitch",0xFF9146FF,"twitch"),
    YANDEX_MUSIC("Яндекс Музыка",0xFFFFBC0D,
        "яндекс музык","yandex music","yandex.music","music.yandex","yandexmusic","yandex.desktop.music"), // not UI
    SPOTIFY("Spotify",0xFF1ED760,"spotify"),
    SOUNDCLOUD("SoundCloud",0xFFFF5500,"soundcloud"),
    VK("VK Музыка",0xFF0077FF,
        "vk музыка","vk music","вконтакте","vkontakte","vk.com","| vk","- vk"), // not UI
    DEEZER("Deezer",0xFFA238FF,"deezer"),
    APPLE_MUSIC("Apple Music",0xFFFA2D48,"apple music","applemusic","music.apple"),
    KICK("Kick",0xFF53FC18,"kick.com","live on kick","| kick","- kick"),
    RUTUBE("RUTUBE",0xFFED143B,"rutube");

    public final String title;
    public final int color;
    private final String[] keys;
    MusicBrand(String title,int color,String... keys){this.title=title;this.color=color;this.keys=keys;}

    /** Browsers frame a tab name ("Вкладка \"…\": воспроизводится звук"); this is what is left of the page title. */
    private static final Pattern QUOTED=Pattern.compile("^[^\"«“]*[\"«“](.+)[\"»”][^\"»”]*$");
    private static final Pattern AUDIO_SUFFIX=Pattern.compile("(?i)\\s*[-–—:]\\s*(audio playing|playing audio|audio muted|tab muted|воспроизв\\S*\\s+звук\\S*|звук\\s+\\S+)\\s*$");
    private static final Pattern BROWSER_SUFFIX=Pattern.compile("(?i)\\s*[-–—]\\s*(google chrome|brave|microsoft\\S*\\s*edge|opera( gx)?|mozilla firefox|firefox|vivaldi|yandex|яндекс\\s*браузер|chromium|arc)\\s*$");

    static String cleanTab(String tab){
        if(tab==null)return "";
        String text=tab.strip();
        var quoted=QUOTED.matcher(text);
        if(quoted.matches())text=quoted.group(1);
        for(int i=0;i<3;i++){
            String next=BROWSER_SUFFIX.matcher(AUDIO_SUFFIX.matcher(text).replaceAll("")).replaceAll("");
            if(next.equals(text))break;
            text=next;
        }
        return text.replaceFirst("^\\(\\d+\\+?\\)\\s*","").strip();
    }
    /** The browser marks a muted tab in its name. */
    static boolean tabMuted(String tab){
        if(tab==null)return false;
        String lower=tab.toLowerCase(Locale.ROOT);
        return lower.contains("звук отключ")||lower.contains("звук выключ")||lower.contains("без звука")||lower.contains("audio muted")||lower.contains("tab muted"); // not UI
    }

    /** The service of a track: from its tab in a browser, from the app otherwise; null when unknown. */
    public static MusicBrand of(MediaTrack track){
        if(track==null)return null;
        // The HUD asks every frame; the regular expressions only need to run when the track changes.
        Known known=last;
        if(known!=null&&track.equals(known.track()))return known.brand();
        MusicBrand brand=track.browser()?match(cleanTab(track.site())):match(track.app()+" "+track.appName());
        last=new Known(track,brand);
        return brand;
    }
    private record Known(MediaTrack track,MusicBrand brand){}
    private static volatile Known last;
    static MusicBrand match(String text){
        if(text==null||text.isBlank())return null;
        String lower=" "+text.toLowerCase(Locale.ROOT)+" ";
        for(var brand:values())for(String key:brand.keys)if(lower.contains(key))return brand;
        return null;
    }
    /** A readable name for well-known player apps whose ids say little ("ru.yandex.desktop.music"); null otherwise. */
    static String knownApp(String app){
        if(app==null)return null;
        String lower=app.toLowerCase(Locale.ROOT);
        if(lower.contains("yandex")&&lower.contains("music"))return YANDEX_MUSIC.title;
        if(lower.contains("spotify"))return SPOTIFY.title;
        if(lower.contains("applemusic")||lower.contains("apple music"))return APPLE_MUSIC.title;
        if(lower.contains("deezer"))return DEEZER.title;
        if(lower.contains("soundcloud"))return SOUNDCLOUD.title;
        if(lower.contains("zunemusic"))return "Media Player";
        if(lower.contains("aimp"))return "AIMP";
        if(lower.contains("foobar2000"))return "foobar2000";
        if(lower.contains("itunes"))return "iTunes";
        return null;
    }
    /** What the panel names as the source: "YouTube · Brave" for a site, the app's name otherwise. */
    public static String source(MediaTrack track){
        var brand=of(track);
        if(brand==null)return Lang.tr(track.appName());
        String title=Lang.tr(brand.title);
        if(!track.browser()||track.appName().equalsIgnoreCase(brand.title))return title;
        return title+" · "+track.appName();
    }
    public static boolean muted(MediaTrack track){return track.muted()||track.browser()&&tabMuted(track.site());}
    /** Class-path location of the bundled logo. */
    public String logo(){return "/assets/velaclient/textures/music/brand_"+name().toLowerCase(Locale.ROOT)+".png";}
}
