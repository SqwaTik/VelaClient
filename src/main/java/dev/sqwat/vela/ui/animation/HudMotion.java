package dev.sqwat.vela.ui.animation;
import java.util.*;
public final class HudMotion {
    public record Frame(float reveal,float height,long time){}
    private final Map<String,Frame> frames=new HashMap<>();
    public boolean skipHidden(String id,boolean visible){
        if(visible)return false;
        var old=frames.get(id);
        if(old!=null&&old.reveal()>=.005f)return false;
        frames.remove(id);return true;
    }
    public Frame update(String id,boolean visible,float height,long now,boolean immediate){
        Frame old=frames.get(id);if(old==null)old=new Frame(0,height,now);float dt=Math.clamp((now-old.time)/1e9f,0,.1f);
        var next=new Frame(immediate?(visible?1:0):Motion.approach(old.reveal,visible?1:0,12,dt),immediate?height:Motion.approach(old.height,height,12,dt),now);frames.put(id,next);return next;
    }
    public void clear(){frames.clear();}
}
