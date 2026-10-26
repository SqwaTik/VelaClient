package dev.sqwat.vela.module;
import java.util.HashSet;
import java.util.Set;
/** A gesture never repeats a server inventory action for the same slot. */
public final class SlotSweep {
    private final Set<Integer> visited=new HashSet<>();
    private long nextAllowedNanos;
    public boolean visit(int slot){return visit(slot,System.nanoTime(),0);}
    public boolean visit(int slot,long nowNanos,int delayMillis){
        if(slot<0||visited.contains(slot)||nowNanos<nextAllowedNanos)return false;
        visited.add(slot);nextAllowedNanos=nowNanos+Math.max(0,delayMillis)*1_000_000L;return true;
    }
    public void reset(){visited.clear();nextAllowedNanos=0;}
}
