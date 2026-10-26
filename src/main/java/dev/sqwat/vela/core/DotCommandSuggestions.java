package dev.sqwat.vela.core;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Locale;

/** Local candidates, rendered and accepted by Minecraft's existing suggestion list. */
public final class DotCommandSuggestions {
    private DotCommandSuggestions(){}
    public static Suggestions suggest(String input,int cursor,List<String> profiles){
        String prefix=input.substring(0,Math.clamp(cursor,0,input.length()));
        int start=prefix.lastIndexOf(' ')+1;
        var builder=new SuggestionsBuilder(prefix,start);
        String[] parts=prefix.toLowerCase(Locale.ROOT).split(" +",-1);
        List<String> candidates=parts.length==1?List.of(".cfg",".gps"):
            parts[0].equals(".cfg")&&parts.length==2?List.of("load","save","remove","dir"):
            parts[0].equals(".cfg")&&parts.length==3&&List.of("load","save","remove").contains(parts[1])?profiles:List.of();
        String remaining=builder.getRemainingLowerCase();
        for(String candidate:candidates)if(candidate.toLowerCase(Locale.ROOT).startsWith(remaining))builder.suggest(candidate);
        return builder.build();
    }
}
