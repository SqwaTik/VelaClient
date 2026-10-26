package dev.sqwat.vela.ui;

import java.util.HashMap;
import java.util.Map;

/** Reject short-lived candidates before they can enter the visible top-k list. */
public final class NearPresence<T> {
    private static final class Seen {
        int first,last;
        Seen(int tick){first=last=tick;}
    }
    private final Map<T,Seen> entries=new HashMap<>();
    public boolean observe(T entity,int tick) {
        Seen seen=entries.get(entity);
        if(seen==null){seen=new Seen(tick);entries.put(entity,seen);}
        // Rendering may skip ticks when unfocused. Absence is handled by finish(),
        // not by demanding a frame for every simulation tick.
        if(tick<seen.last)seen.first=tick;
        seen.last=tick;
        return tick-seen.first>=10;
    }
    public void finish(int tick) { entries.values().removeIf(s->s.last!=tick); }
    public void clear() { entries.clear(); }
}
