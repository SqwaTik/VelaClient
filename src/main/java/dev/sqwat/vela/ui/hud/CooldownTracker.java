package dev.sqwat.vela.ui.hud;
import net.minecraft.resources.Identifier;
import java.util.*;
public final class CooldownTracker {
    private static final Map<Identifier,Integer> remaining=new LinkedHashMap<>();
    public static synchronized void start(Identifier group,int ticks){if(ticks>0)remaining.put(group,ticks);else remaining.remove(group);}
    public static synchronized void end(Identifier group){remaining.remove(group);}
    public static synchronized void tick(){remaining.replaceAll((key,value)->value-1);remaining.values().removeIf(value->value<=0);}
    public static synchronized Map<Identifier,Integer> snapshot(){return new LinkedHashMap<>(remaining);}
    public static synchronized void clear(){remaining.clear();}
}
