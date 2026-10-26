// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.entity;

/**
 * Vela: render states are pooled only while the level extracts its visible entities. Any other extraction of the same
 * entity in that frame (a Target HUD portrait, an inventory preview) must not overwrite the state the world is about
 * to draw: that made hit mobs twitch, because the portrait rotates the mob to face the cursor. Render thread only.
 */
public final class PoolScope {
    private static boolean level;
    private PoolScope() {}
    public static boolean level() { return level; }
    public static void level(boolean active) { level = active; }
}
