package dev.sqwat.vela.ui.animation;

/** Time-based monotonic presentation of the real reload progress. */
public final class LoadingProgress {
    private float target,display;
    public float update(float actual,boolean done,float seconds){
        if(Float.isFinite(actual))target=Math.max(target,Math.max(0,Math.min(1,actual)));
        if(done)target=1;
        float dt=Float.isFinite(seconds)?Math.max(0,Math.min(.1f,seconds)):0;
        float change=(target-display)*(1-(float)Math.exp(-7*dt));
        display=Math.min(target,display+Math.min(change,(done?1.5f:.65f)*dt));
        if(done&&display>.999f)display=1;
        return display;
    }
}
