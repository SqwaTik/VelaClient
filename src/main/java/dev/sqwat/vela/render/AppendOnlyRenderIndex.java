package dev.sqwat.vela.render;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;

/** First-equal lookup for an append-only draw list owned by one frame's render group. */
public final class AppendOnlyRenderIndex<T> {
    private static final int MIN_INDEXED=16, MAX_INDEXED=4096;
    private Object2IntOpenHashMap<T> firstIndices;
    private int indexed;

    public int indexOf(List<T> draws,T value){
        int size=draws.size();
        if(size<MIN_INDEXED || size>MAX_INDEXED){
            // No retained index for tiny groups or pathological modded groups.
            firstIndices=null;indexed=0;
            return draws.indexOf(value);
        }
        if(firstIndices==null){firstIndices=new Object2IntOpenHashMap<>();firstIndices.defaultReturnValue(-1);}
        while(indexed<size){
            firstIndices.putIfAbsent(draws.get(indexed),indexed);
            indexed++;
        }
        return firstIndices.getInt(value);
    }
}
