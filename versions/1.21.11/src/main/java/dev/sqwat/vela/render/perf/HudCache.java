package dev.sqwat.vela.render.perf;

/** 1.21.11: the HUD cache hooks 26.x's GUI render state, so the HUD is drawn every frame. */
public final class HudCache {
    private HudCache(){}
    public static boolean active(){return false;}
    public static boolean reuse(){return false;}
    public static boolean refresh(){return false;}
    public static boolean cachedPass(){return false;}
    public static void close(){}
}
