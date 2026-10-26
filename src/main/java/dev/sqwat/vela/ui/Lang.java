package dev.sqwat.vela.ui;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface language. The client's own text is written in Russian in code; in English mode it is translated where it
 * is drawn (PngTextRenderer) or created (chat components) from assets/velaclient/lang/en.json. Entries with {} are
 * templates for text assembled at run time. Unknown text is shown as is. English is the default.
 */
public final class Lang {
    public enum Language{EN,RU}
    private static volatile Language current=Language.EN;
    private static final Map<String,String> EXACT=new HashMap<>();
    private record Template(Pattern pattern,String english){}
    private static final List<Template> TEMPLATES=new ArrayList<>();
    private static final Pattern CYRILLIC=Pattern.compile("[\\u0400-\\u04FF]");
    private static final Pattern SLOT=Pattern.compile("\\{\\}");
    /** Translations of strings seen recently, so a repeated label costs one map lookup per frame. */
    private static final Map<String,String> CACHE=Collections.synchronizedMap(new LinkedHashMap<>(256,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,String> e){return size()>2048;}
    });
    static{load();}
    private Lang(){}

    private static void load(){
        try(var in=Lang.class.getResourceAsStream("/assets/velaclient/lang/en.json")){
            if(in==null)return;
            var json=com.google.gson.JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var e:json.entrySet()){
                String ru=e.getKey(),en=e.getValue().getAsString();
                if(!ru.contains("{}")){EXACT.put(ru,en);continue;}
                StringBuilder regex=new StringBuilder();
                String[] parts=SLOT.split(ru,-1);
                for(int i=0;i<parts.length;i++){if(i>0)regex.append("(.+?)");regex.append(Pattern.quote(parts[i]));}
                TEMPLATES.add(new Template(Pattern.compile(regex.toString()),en));
            }
        }catch(Exception error){org.slf4j.LoggerFactory.getLogger("Vela").warn("Cannot load English text",error);}
    }
    public static Language current(){return current;}
    public static void set(Language language){if(language!=null&&language!=current){current=language;CACHE.clear();}}
    public static boolean english(){return current==Language.EN;}
    /** Picks the text for the current language. */
    public static String pick(String english,String russian){return english()?english:russian;}

    /** English for a Russian interface string, or the string unchanged. */
    public static String tr(String text){
        if(current==Language.RU||text==null||text.isEmpty())return text;
        String hit=CACHE.get(text);
        if(hit!=null)return hit;
        String result=translate(text);
        CACHE.put(text,result);
        return result;
    }
    static String translate(String text){
        String exact=EXACT.get(text);
        if(exact!=null)return exact;
        if(!CYRILLIC.matcher(text).find())return text;
        for(var t:TEMPLATES){
            Matcher m=t.pattern.matcher(text);
            if(!m.matches())continue;
            String out=t.english;
            for(int i=1;i<=m.groupCount();i++)out=SLOT.matcher(out).replaceFirst(Matcher.quoteReplacement(tr(m.group(i))));
            return out;
        }
        return text;
    }
    static int size(){return EXACT.size()+TEMPLATES.size();}
}
