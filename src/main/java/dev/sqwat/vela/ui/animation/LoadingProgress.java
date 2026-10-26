package dev.sqwat.vela.ui.animation;

/**
 * Time-based monotonic presentation of the real reload progress.
 * Frame gaps up to 0.25 s count in full, so a busy reload thread does not make the bar fall behind and then jump;
 * after completion the bar finishes within roughly a fifth of a second.
 */
public final class LoadingProgress {
    static final float MAX_STEP=.25f;
    private float target,display;
    public float update(float actual,boolean done,float seconds){
        if(Float.isFinite(actual))target=Math.max(target,Math.max(0,Math.min(1,actual)));
        if(done)target=1;
        float dt=Float.isFinite(seconds)?Math.max(0,Math.min(MAX_STEP,seconds)):0;
        float change=(target-display)*(1-(float)Math.exp(-(done?14:7)*dt));
        display=Math.min(target,display+Math.min(change,(done?5f:.65f)*dt));
        if(done&&display>.995f)display=1;
        return display;
    }
    /** True once the bar shows 100%. */
    public boolean finished(){return display>=1;}
}
