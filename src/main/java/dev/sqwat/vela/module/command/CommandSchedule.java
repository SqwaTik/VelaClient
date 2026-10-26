package dev.sqwat.vela.module.command;
import java.util.*;

/** Monotonic scheduling, no catch-up bursts and no repeats from held macro keys. */
public final class CommandSchedule {
    private final Map<UUID,Long> due=new HashMap<>();
    private final Set<UUID> down=new HashSet<>();
    private final Map<UUID,CommandEntries.Entry> previous=new HashMap<>();
    private long sentAt=Long.MIN_VALUE/2;
    private int nextInterval;
    public CommandEntries.Entry interval(List<CommandEntries.Entry> entries,long now,boolean allowed){
        if(entries.isEmpty())return null;
        for(int offset=0;offset<entries.size();offset++){
            int index=(nextInterval+offset)%entries.size();var e=entries.get(index);
            if(e.interval()&&poll(e,now,allowed,false)){
                nextInterval=(index+1)%entries.size();return e;
            }
        }
        return null;
    }
    public boolean poll(CommandEntries.Entry e,long now,boolean allowed,boolean pressed){
        boolean edge=pressed&&!down.contains(e.id());if(pressed)down.add(e.id());else down.remove(e.id());
        if(!e.equals(previous.put(e.id(),e)))due.remove(e.id());
        if(!allowed||!e.enabled()){due.remove(e.id());return false;}
        if(!e.interval())return edge&&e.key()>=0;
        long deadline=due.computeIfAbsent(e.id(),id->now+e.seconds()*1000L);
        if(now<deadline||now-sentAt<1000)return false;
        due.put(e.id(),now+e.seconds()*1000L);sentAt=now;return true;
    }
    public void retain(Set<UUID> ids){due.keySet().retainAll(ids);down.retainAll(ids);previous.keySet().retainAll(ids);}
    public void reset(){due.clear();down.clear();previous.clear();sentAt=Long.MIN_VALUE/2;nextInterval=0;}
}
