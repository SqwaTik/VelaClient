package dev.sqwat.vela.ui.animation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Bounded, monotonic-clock animation storage keyed by semantic control identity. */
public final class AnimationState {
    private record Value(float value,long at){}
    private final Map<String,Value> values=new LinkedHashMap<>();
    private final LongSupplier clock;
    public AnimationState(){this(System::nanoTime);}
    public AnimationState(LongSupplier clock){this.clock=clock;}
    public float value(String id,float target,boolean immediate){
        return value(id,target,immediate,24);
    }
    public float value(String id,float target,boolean immediate,float speed){
        long now=clock.getAsLong();Value previous=values.get(id);
        float result=previous==null||immediate?target:Motion.approach(previous.value(),target,speed,Math.max(0,Math.min(.1f,(now-previous.at())/1e9f)));
        if(!values.containsKey(id)&&values.size()>=1024)values.remove(values.keySet().iterator().next());
        values.put(id,new Value(result,now));return result;
    }
}
