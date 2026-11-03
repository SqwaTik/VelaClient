package dev.sqwat.vela.render.entity;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import java.util.IdentityHashMap;

/** Reuses glyph layout, never entity poses, lighting, visibility or animated glyphs. */
public final class NameTagTextCache {
    private static final PreparedLabelCache<Font.PreparedText> CACHE = new PreparedLabelCache<>(1024);
    private static final LabelTextKey PROBE = new LabelTextKey();
    private static final PreparedLabelCache.Lookup LOOKUP = PreparedLabelCache.Key.lookup();
    /** Display names built this tick. Player names are rebuilt (team colours, hover events) on every frame otherwise,
     *  and a stable component lets the layout below be found by identity instead of walking its characters. */
    private static final IdentityHashMap<Entity, Component> NAMES = new IdentityHashMap<>();
    private static final IdentityHashMap<Component, Recent[]> RECENT = new IdentityHashMap<>();
    private record Recent(Font font, float x, float y, int color, int background, Font.PreparedText text) {
        boolean matches(Font f, float px, float py, int c, int b) { return font == f && x == px && y == py && color == c && background == b; }
    }
    private static long hits, misses, fast;
    private NameTagTextCache() {}
    public static void clear() { CACHE.clear(); tick(); }
    /** Called at the end of every client tick: names and identity hits never outlive one tick. */
    public static void tick() { NAMES.clear(); RECENT.clear(); }
    public static Component name(Entity entity) { return NAMES.get(entity); }
    public static void remember(Entity entity, Component name) { if (name != null) NAMES.put(entity, name); }
    public static void resetStatistics() { hits = misses = fast = 0; }
    public static String statistics() { return "nameTagLayout hits=" + hits + ", identity=" + fast + ", misses=" + misses; }
    public static Font.PreparedText prepare(Font font, Component text, float x, float y, int color, int background) {
        Recent[] slots = RECENT.get(text);
        if (slots != null) for (Recent r : slots) if (r != null && r.matches(font, x, y, color, background)) { fast++; return r.text(); }
        Font.PreparedText result = prepare(font, text.getVisualOrderText(), x, y, color, background);
        if (slots == null) RECENT.put(text, slots = new Recent[2]);
        // See-through tags are submitted twice with different colours; keep both.
        slots[slots[0] == null ? 0 : 1] = new Recent(font, x, y, color, background, result);
        return result;
    }
    public static Font.PreparedText prepare(Font font, FormattedCharSequence text,
                                            float x, float y, int color, int background) {
        if (!PROBE.read(text)) {
            misses++;
            return font.prepareText(text, x, y, color, false, false, background);
        }
        Font.PreparedText result = CACHE.get(LOOKUP.set(font, PROBE, x, y, color, background));
        if (result != null) { hits++; return result; }
        misses++;
        result = font.prepareText(text, x, y, color, false, false, background);
        CACHE.put(new PreparedLabelCache.Key(font, PROBE.snapshot(), x, y, color, background), result);
        return result;
    }
    public static boolean cacheable(FormattedCharSequence text) {
        return new LabelTextKey().read(text);
    }
}
