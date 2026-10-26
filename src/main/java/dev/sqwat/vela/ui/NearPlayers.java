package dev.sqwat.vela.ui;

import java.util.*;
import java.util.function.*;

public final class NearPlayers {
    private NearPlayers(){}
    public static boolean eligible(boolean listed,UUID entityId,String entityName,UUID profileId,String profileName){
        return listed&&entityId.equals(profileId)&&entityName.equals(profileName);
    }
    /** Keep only the requested nearest entries; never sort the complete world player list. */
    public static <T> List<T> closest(Iterable<? extends T> entries,int limit,Predicate<T> eligible,ToDoubleFunction<T> distance){
        return closest(entries,limit,eligible,distance,e->false);
    }
    public static <T> List<T> closest(Iterable<? extends T> entries,int limit,Predicate<T> eligible,ToDoubleFunction<T> distance,Predicate<T> priority){
        if(limit<=0)return List.of();
        var result=new ArrayList<T>(limit);var distances=new double[limit];var priorities=new boolean[limit];
        for(T entry:entries){
            if(!eligible.test(entry))continue;
            double d=distance.applyAsDouble(entry);if(!Double.isFinite(d)||d>140*140)continue;
            boolean preferred=priority.test(entry);
            int i=0;while(i<result.size()&&(priorities[i]&&!preferred||priorities[i]==preferred&&distances[i]<=d))i++;
            if(i>=limit)continue;
            if(result.size()==limit)result.remove(limit-1);
            for(int j=result.size();j>i;j--){distances[j]=distances[j-1];priorities[j]=priorities[j-1];}
            distances[i]=d;priorities[i]=preferred;result.add(i,entry);
        }
        return List.copyOf(result);
    }
}
