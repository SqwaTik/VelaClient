package dev.sqwat.vela.render;

import dev.sqwat.vela.render.entity.PreparedLabelCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PreparedLabelCacheProbeTest {
    @Test void reusableLookupDoesNotMutateTheStoredKey() {
        var cache = new PreparedLabelCache<String>(2);
        Object font = new Object();
        cache.put(new PreparedLabelCache.Key(font, "first", 1, 2, 3, 4), "prepared");

        var lookup = PreparedLabelCache.Key.lookup();
        lookup.set(font, "first", 1, 2, 3, 4);
        assertEquals("prepared", cache.get(lookup));

        lookup.set(font, "second", 1, 2, 3, 4);
        assertNull(cache.get(lookup));
        assertEquals("prepared", cache.get(new PreparedLabelCache.Key(font, "first", 1, 2, 3, 4)));
    }
}
