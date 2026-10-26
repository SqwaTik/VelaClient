package dev.sqwat.vela.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Release notes shipped in assets/velaclient/changelog.json, newest first, in both interface languages. */
public final class Changelog {
    public enum Kind{NEW,IMPROVED,FIXED}
    public record Section(Kind kind,List<String> en,List<String> ru){
        public List<String> items(){return Lang.english()?en:ru;}
    }
    public record Version(String version,String date,String titleEn,String titleRu,List<Section> sections){
        public String title(){return Lang.english()?titleEn:titleRu;}
    }
    private static List<Version> versions;
    private Changelog(){}

    public static synchronized List<Version> versions(){
        if(versions==null)versions=load();
        return versions;
    }
    /** The running client version, e.g. "0.2.0". */
    public static String current(){
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("velaclient")
            .map(c->c.getMetadata().getVersion().getFriendlyString()).orElse(versions().isEmpty()?"":versions().getFirst().version());
    }
    static List<Version> load(){
        try(var in=Changelog.class.getResourceAsStream("/assets/velaclient/changelog.json")){
            if(in==null)return List.of();
            var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            var out=new ArrayList<Version>();
            for(var v:root.getAsJsonArray("versions")){
                var o=v.getAsJsonObject();var title=o.getAsJsonObject("title");
                var sections=new ArrayList<Section>();
                for(var s:o.getAsJsonArray("sections")){
                    var so=s.getAsJsonObject();var en=new ArrayList<String>();var ru=new ArrayList<String>();
                    for(var item:so.getAsJsonArray("items")){var io=item.getAsJsonObject();en.add(text(io,"en"));ru.add(text(io,"ru"));}
                    sections.add(new Section(Kind.valueOf(so.get("kind").getAsString().toUpperCase(java.util.Locale.ROOT)),List.copyOf(en),List.copyOf(ru)));
                }
                out.add(new Version(o.get("version").getAsString(),o.get("date").getAsString(),text(title,"en"),text(title,"ru"),List.copyOf(sections)));
            }
            return List.copyOf(out);
        }catch(Exception error){
            org.slf4j.LoggerFactory.getLogger("Vela").warn("Cannot read changelog",error);
            return List.of();
        }
    }
    private static String text(JsonObject o,String language){return o.has(language)?o.get(language).getAsString():o.get("en").getAsString();}
}
