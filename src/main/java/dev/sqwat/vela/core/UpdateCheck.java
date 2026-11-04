package dev.sqwat.vela.core;

import dev.sqwat.vela.ui.Lang;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * The newest Vela release on GitHub (SqwaTik/VelaClient), asked once per game. When it is newer than the running mod,
 * the title screen shows a download card and every world or server join posts a chat line with a download link.
 * GitHub's release API needs no key (CurseForge's does, and a key cannot ship inside the mod).
 */
public final class UpdateCheck {
    private static final String API="https://api.github.com/repos/SqwaTik/VelaClient/releases/latest";
    public static final String RELEASES="https://github.com/SqwaTik/VelaClient/releases/latest";
    /** A newer release: its version ("0.2.2") and the page to download it from. */
    public record Release(String version,String url){}
    private static volatile Release newer;
    private static boolean asked;
    /** A join happened before GitHub answered: the chat line follows the answer. */
    private static volatile boolean pending;
    private UpdateCheck(){}

    /** The newer release, or null (none, or GitHub has not answered yet). */
    public static Release newer(){ask();return newer;}

    public static synchronized void ask(){
        if(asked)return;
        asked=true;
        Thread.ofVirtual().name("Vela update check").start(()->{
            try{
                var http=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build();
                var response=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(API))
                    .timeout(java.time.Duration.ofSeconds(8)).header("Accept","application/vnd.github+json").header("User-Agent","VelaClient").GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()!=200)return;
                var json=com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
                var tag=json.get("tag_name");
                if(tag==null||tag.isJsonNull())return;
                String version=tag.getAsString().strip().replaceFirst("^[vV]","");
                var page=json.get("html_url");
                String url=page==null||page.isJsonNull()?RELEASES:page.getAsString();
                if(!isNewer(version,dev.sqwat.vela.ui.Changelog.current()))return;
                newer=new Release(version,url);
                if(pending){pending=false;Minecraft.getInstance().execute(UpdateCheck::postIfInWorld);}
            }catch(Exception ignored){}
        });
    }

    /** On every join: the chat line, now or once GitHub answers. */
    public static void joined(){
        ask();
        if(newer!=null)postIfInWorld();else pending=true;
    }
    public static void left(){pending=false;}

    private static void postIfInWorld(){
        Release release=newer;
        var client=Minecraft.getInstance();
        if(release==null||client.player==null)return;
        java.net.URI parsed;
        try{parsed=java.net.URI.create(release.url());}catch(IllegalArgumentException error){parsed=java.net.URI.create(RELEASES);}
        final java.net.URI uri=parsed;
        var link=Component.literal(Lang.pick("[Download]","[Скачать]")).withStyle(s->s.withColor(VelaTheme.PRIMARY&0xFFFFFF).withUnderlined(true));
        link=link.withStyle(s->s.withClickEvent(new ClickEvent.OpenUrl(uri))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal(Lang.pick("Open the release on GitHub","Открыть релиз на GitHub")))));
        client.gui.hud.getChat().addClientSystemMessage(Component.literal("Vela").withStyle(s->s.withColor(VelaTheme.PRIMARY&0xFFFFFF))
            .append(Component.literal(" -> ").withStyle(s->s.withColor(0x929AAA)))
            .append(Component.literal(Lang.pick("A new version is available: ","Доступна новая версия: ")+release.version()
                +Lang.pick(" (you have "," (у вас ")+shortVersion(dev.sqwat.vela.ui.Changelog.current())+"). ").withStyle(s->s.withColor(0xDCE2EE)))
            .append(link));
    }

    /** "0.2.1+1.21.11-fabric" -> "0.2.1". */
    public static String shortVersion(String version){
        String v=version==null?"":version.strip().replaceFirst("^[vV]","");
        int cut=v.indexOf('+');
        return cut<0?v:v.substring(0,cut);
    }

    /** Whether release is a newer version than current, by their numeric parts ("0.2.10" > "0.2.9"); a pre-release suffix counts as older. */
    public static boolean isNewer(String release,String current){
        String a=shortVersion(release),b=shortVersion(current);
        if(a.isEmpty()||b.isEmpty())return false;
        String[] x=a.split("-",2)[0].split("\\."),y=b.split("-",2)[0].split("\\.");
        for(int i=0;i<Math.max(x.length,y.length);i++){
            long p=i<x.length?number(x[i]):0,q=i<y.length?number(y[i]):0;
            if(p!=q)return p>q;
        }
        // same numbers: a release beats the running pre-release ("0.3.0" over "0.3.0-beta"), not the other way round
        return !a.contains("-")&&b.contains("-");
    }
    private static long number(String part){
        int end=0;while(end<part.length()&&Character.isDigit(part.charAt(end)))end++;
        return end==0?0:Long.parseLong(part.substring(0,Math.min(end,18)));
    }
}
