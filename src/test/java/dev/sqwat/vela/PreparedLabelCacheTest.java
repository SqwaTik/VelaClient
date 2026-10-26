package dev.sqwat.vela;

import dev.sqwat.vela.render.entity.PreparedLabelCache;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PreparedLabelCacheTest {
    @Test void underlineRunBoundariesArePartOfLayoutKey(){
        var style=net.minecraft.network.chat.Style.EMPTY.withUnderlined(true);
        var a=new dev.sqwat.vela.render.entity.LabelTextKey();var b=new dev.sqwat.vela.render.entity.LabelTextKey();
        a.read(net.minecraft.util.FormattedCharSequence.forward("AB",style));
        b.read(net.minecraft.util.FormattedCharSequence.composite(net.minecraft.util.FormattedCharSequence.forward("A",style),net.minecraft.util.FormattedCharSequence.forward("B",style)));
        assertNotEquals(a,b);
    }
    @Test void animatedAndOversizedTextBypassesCache() {
        var normal = net.minecraft.network.chat.Style.EMPTY;
        assertTrue(dev.sqwat.vela.render.entity.NameTagTextCache.cacheable(net.minecraft.util.FormattedCharSequence.forward("Player", normal)));
        assertFalse(dev.sqwat.vela.render.entity.NameTagTextCache.cacheable(net.minecraft.util.FormattedCharSequence.forward("Player", normal.withObfuscated(true))));
        assertFalse(dev.sqwat.vela.render.entity.NameTagTextCache.cacheable(net.minecraft.util.FormattedCharSequence.forward("x".repeat(257), normal)));
    }
    @Test void reusesOnlyIdenticalInputs() {
        var cache = new PreparedLabelCache<String>(4);
        Object font = new Object(), text = new String("name");
        var key = new PreparedLabelCache.Key(font, text, 1, 2, 3, 4);
        cache.put(key, "prepared");
        assertEquals("prepared", cache.get(new PreparedLabelCache.Key(font, text, 1, 2, 3, 4)));
        assertEquals("prepared", cache.get(new PreparedLabelCache.Key(font, new String("name"), 1, 2, 3, 4)));
        assertNull(cache.get(new PreparedLabelCache.Key(new Object(), text, 1, 2, 3, 4)));
        assertNull(cache.get(new PreparedLabelCache.Key(font, text, 2, 2, 3, 4)));
        assertNull(cache.get(new PreparedLabelCache.Key(font, text, 1, 3, 3, 4)));
        assertNull(cache.get(new PreparedLabelCache.Key(font, text, 1, 2, 4, 4)));
        assertNull(cache.get(new PreparedLabelCache.Key(font, text, 1, 2, 3, 5)));
    }

    @Test void boundedAndInvalidated() {
        var cache = new PreparedLabelCache<String>(2);
        var a = new PreparedLabelCache.Key(this, new Object(), 0, 0, 0, 0);
        var b = new PreparedLabelCache.Key(this, new Object(), 0, 0, 0, 0);
        var c = new PreparedLabelCache.Key(this, new Object(), 0, 0, 0, 0);
        cache.put(a, "a"); cache.put(b, "b");
        assertEquals("a", cache.get(a));
        cache.put(c, "c");
        assertNull(cache.get(b));
        assertEquals("a", cache.get(a));
        cache.clear();
        assertNull(cache.get(a)); assertNull(cache.get(c));
    }

    @Test void rebuiltTextHitsButChangedStyleMissesAndProbeCannotMutateStoredKey() {
        var probe = new dev.sqwat.vela.render.entity.LabelTextKey();
        var cache = new PreparedLabelCache<String>(4);
        var style = net.minecraft.network.chat.Style.EMPTY;
        probe.read(net.minecraft.util.FormattedCharSequence.forward("Player", style));
        var frozen = probe.snapshot();
        cache.put(new PreparedLabelCache.Key(this, frozen, 0, 0, -1, 0), "layout");
        probe.read(net.minecraft.util.FormattedCharSequence.forward(new String("Player"), style));
        assertEquals("layout", cache.get(new PreparedLabelCache.Key(this, probe, 0, 0, -1, 0)));
        probe.read(net.minecraft.util.FormattedCharSequence.forward("Player", style.withBold(true)));
        assertNull(cache.get(new PreparedLabelCache.Key(this, probe, 0, 0, -1, 0)));
        probe.read(net.minecraft.util.FormattedCharSequence.forward("Player2", style));
        assertNotEquals(frozen, probe);
        assertThrows(IllegalStateException.class, () -> frozen.read(net.minecraft.util.FormattedCharSequence.EMPTY));
    }
}
