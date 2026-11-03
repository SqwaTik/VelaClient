package de.johni0702.minecraft.bobby;

/**
 * Vela: fixed settings instead of the HOCON config file (configurate is not bundled). The Far module turns the cache
 * on and off; it keeps terrain only, starts a separate cache per world and unloads promptly.
 */
public class BobbyConfig {
    public static final BobbyConfig DEFAULT = new BobbyConfig();

    public boolean isNoBlockEntities() { return true; }
    public boolean isEnabled() { return true; }
    public boolean isDynamicMultiWorld() { return true; }
    public boolean isTaintFakeChunks() { return false; }
    public int getUnloadDelaySecs() { return 0; }
    public int getDeleteUnusedRegionsAfterDays() { return -1; }
    public int getMaxRenderDistance() { return 64; }
    public int getViewDistanceOverwrite() { return 0; }
}
