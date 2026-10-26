package dev.sqwat.vela.render.entity;

import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import java.util.*;

/** Immutable presentation transformation; original components and identities are never edited. */
public final class NameMask {
    private final String name,alias;
    private final Map<FormattedCharSequence,FormattedCharSequence> sequences=new IdentityHashMap<>();
    private final Map<String,String> plainCache=new LinkedHashMap<>();
    private final Map<String,FormattedCharSequence> stringCache=new LinkedHashMap<>();
    public NameMask(String name,String alias){this.name=name;this.alias=alias;}
    private static boolean word(char c){return Character.isLetterOrDigit(c)||c=='_';}
    public int next(String text,int from){
        if(name.isEmpty())return -1;
        for(int i=from;i+name.length()<=text.length();i++)
            if(text.regionMatches(true,i,name,0,name.length())&&(i==0||!word(text.charAt(i-1)))
                &&(i+name.length()==text.length()||!word(text.charAt(i+name.length())))&&!insideAlias(text,i))return i;
        return -1;
    }
    private boolean insideAlias(String text,int at){
        if(alias.equalsIgnoreCase(name))return false;
        for(int start=Math.max(0,at-alias.length()+1);start<=at;start++)
            if(start+alias.length()<=text.length()&&text.regionMatches(start,alias,0,alias.length()))return true;
        return false;
    }
    public String plain(String text){
        var cached=plainCache.get(text);if(cached!=null)return cached;
        int at=next(text,0);if(at<0)return text;
        StringBuilder result=new StringBuilder();int start=0;
        do{result.append(text,start,at).append(alias);start=at+name.length();at=next(text,start);}while(at>=0);
        String output=result.append(text,start,text.length()).toString();
        if(plainCache.size()>=512)plainCache.clear();
        plainCache.put(text,output);plainCache.put(output,output);return output;
    }
    public boolean containsAlias(String text){return !alias.isEmpty()&&text.contains(alias);}
    public FormattedCharSequence sequence(String text){
        var cached=stringCache.get(text);if(cached!=null)return cached;
        var result=sequence(sink->StringDecomposer.iterateFormatted(text,Style.EMPTY,sink));
        if(stringCache.size()>=512)stringCache.clear();stringCache.put(text,result);return result;
    }
    public FormattedCharSequence sequence(FormattedCharSequence input){
        if(input instanceof Masked)return input;
        var cached=sequences.get(input);if(cached!=null)return cached;
        StringBuilder text=new StringBuilder();
        input.accept((index,style,cp)->{text.appendCodePoint(cp);return true;});
        if(next(text.toString(),0)<0){remember(input,input);return input;}
        List<Style> styles=new ArrayList<>();
        input.accept((index,style,cp)->{for(int i=0;i<Character.charCount(cp);i++)styles.add(style);return true;});
        List<FormattedCharSequence> parts=new ArrayList<>();String value=text.toString();int start=0;
        for(int at=next(value,0);at>=0;at=next(value,start)){
            append(parts,value,styles,start,at);
            parts.add(FormattedCharSequence.forward(alias,styles.get(at).withItalic(true)));
            start=at+name.length();
        }
        append(parts,value,styles,start,value.length());
        var result=new Masked(FormattedCharSequence.composite(parts));remember(input,result);return result;
    }
    private void remember(FormattedCharSequence key,FormattedCharSequence value){if(sequences.size()>=512)sequences.clear();sequences.put(key,value);}
    private static void append(List<FormattedCharSequence> parts,String text,List<Style> styles,int from,int to){
        for(int i=from;i<to;){int end=i+Character.charCount(text.codePointAt(i));
            while(end<to&&styles.get(i).equals(styles.get(end)))end+=Character.charCount(text.codePointAt(end));
            parts.add(FormattedCharSequence.forward(text.substring(i,end),styles.get(i)));i=end;}
    }
    public FormattedText formatted(FormattedText input){
        if(next(input.getString(),0)<0)return input;
        List<FormattedCharSequence> parts=new ArrayList<>();
        input.visit((style,text)->{parts.add(FormattedCharSequence.forward(text,style));return Optional.empty();},Style.EMPTY);
        var original=FormattedCharSequence.composite(parts);var masked=sequence(original);
        if(masked==original)return input;
        var result=Component.empty();StringBuilder run=new StringBuilder();Style[] previous={Style.EMPTY};
        masked.accept((i,style,cp)->{
            if(!style.equals(previous[0])&&!run.isEmpty()){result.append(Component.literal(run.toString()).setStyle(previous[0]));run.setLength(0);}
            previous[0]=style;run.appendCodePoint(cp);return true;
        });
        if(!run.isEmpty())result.append(Component.literal(run.toString()).setStyle(previous[0]));
        return result;
    }
    private record Masked(FormattedCharSequence value) implements FormattedCharSequence {
        @Override public boolean accept(FormattedCharSink sink){return value.accept(sink);}
    }
}
