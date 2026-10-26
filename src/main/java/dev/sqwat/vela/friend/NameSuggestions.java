package dev.sqwat.vela.friend;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class NameSuggestions {
    private NameSuggestions(){}
    public static List<String> matching(Collection<String> names,String query){
        return matching(names,query,5);
    }
    public static List<String> matching(Collection<String> names,String query,int limit){
        String prefix=query.strip().toLowerCase(Locale.ROOT);
        if(prefix.isEmpty())return List.of();
        return names.stream().filter(n->n.matches("[A-Za-z0-9_]{3,16}"))
            .filter(n->n.toLowerCase(Locale.ROOT).startsWith(prefix)&&!n.equalsIgnoreCase(prefix))
            .distinct().sorted(String.CASE_INSENSITIVE_ORDER).limit(Math.clamp(limit,0,100)).toList();
    }
}
