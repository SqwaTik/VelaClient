package dev.sqwat.vela.render.effect;

/** Vanilla repeated-hit rule: within the immunity window only excess damage applies. */
public final class LocalHitWindow {
    private LocalHitWindow(){}
    public static float damage(float incoming,float previous,boolean protectedHit){
        return Math.max(0,protectedHit?incoming-previous:incoming);
    }
}
