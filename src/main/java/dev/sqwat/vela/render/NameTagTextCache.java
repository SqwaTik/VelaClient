package dev.sqwat.vela.render;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

/** Reuses glyph layout, never entity poses, lighting, visibility or animated glyphs. */
public final class NameTagTextCache {
    private static final PreparedLabelCache<Font.PreparedText> CACHE = new PreparedLabelCache<>(1024);
    private static final LabelTextKey PROBE = new LabelTextKey();
    private static final PreparedLabelCache.Lookup LOOKUP = PreparedLabelCache.Key.lookup();
    private static long hits, misses;
    private NameTagTextCache() {}
    public static void clear() { CACHE.clear(); }
    public static void resetStatistics() { hits = misses = 0; }
    public static String statistics() { return "nameTagLayout hits=" + hits + ", misses=" + misses; }
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
