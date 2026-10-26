package dev.sqwat.vela.ui.animation;

/** Smooth attack/release envelope with a low-frequency heartbeat; no flashing. */
public final class WarningPulse {
    private WarningPulse(){}
    public static float opacity(long now,long until,boolean reducedMotion){
        double age=(now-(until-3_500_000_000L))/1e9,remaining=(until-now)/1e9;
        if(until==0||age<0||remaining<=0)return 0;
        double t=Math.clamp(Math.min(age/.35,remaining/.65),0,1),envelope=t*t*(3-2*t);
        double pulse=reducedMotion?1:.78+.22*Math.cos(age*Math.PI*2/1.25);
        return (float)(envelope*pulse*.36);
    }
}
