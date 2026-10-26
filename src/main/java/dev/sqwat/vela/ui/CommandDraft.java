package dev.sqwat.vela.ui;

import dev.sqwat.vela.module.CommandEntries;
import java.util.UUID;

/** Explicit selection distinguishes editing from successive creations. */
public final class CommandDraft {
    private UUID selected;
    public UUID selected(){return selected;}
    public void select(UUID id){selected=id;}
    public void fresh(){selected=null;}
    public boolean save(CommandEntries entries,boolean interval,String name,String text,int key,int seconds,boolean enabled){
        boolean created=selected==null;
        entries.save(new CommandEntries.Entry(created?UUID.randomUUID():selected,interval,name,text,key,seconds,enabled));
        return created;
    }
}
