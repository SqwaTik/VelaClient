package dev.sqwat.vela.render.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import java.util.IdentityHashMap;

/** 1.21.5 has no prepared text, so only the per-tick display names are kept; the glyph layout is not cached. */
public final class NameTagTextCache {
    /** Display names built this tick. Player names are rebuilt (team colours, hover events) on every frame otherwise. */
    private static final IdentityHashMap<Entity, Component> NAMES = new IdentityHashMap<>();
    private NameTagTextCache() {}
    public static void clear() { tick(); }
    /** Called at the end of every client tick: names never outlive one tick. */
    public static void tick() { NAMES.clear(); }
    public static Component name(Entity entity) { return NAMES.get(entity); }
    public static void remember(Entity entity, Component name) { if (name != null) NAMES.put(entity, name); }
    public static void resetStatistics() {}
    public static String statistics() { return "nameTagLayout off (1.21.5)"; }
    public static boolean cacheable(FormattedCharSequence text) {
        return new LabelTextKey().read(text);
    }
}
