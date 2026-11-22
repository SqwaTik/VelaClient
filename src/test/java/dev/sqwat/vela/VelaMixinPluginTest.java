package dev.sqwat.vela;

import dev.sqwat.vela.core.compat.VelaMixinPlugin;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Regression: Vela's terrain mixins crashed the game at start next to Sodium (merged LevelRenderer method). */
class VelaMixinPluginTest {
    @Test void terrainMixinsStepAsideForSodiumAndForks(){
        for(var mod:Set.of("sodium","embeddium","celeritas"))
            assertTrue(VelaMixinPlugin.clashes("perf.TerrainAllocationMixin",mod::equals),mod);
        assertFalse(VelaMixinPlugin.clashes("perf.TerrainAllocationMixin",id->false));
    }
    @Test void textMixinStepsAsideForImmediatelyFast(){
        assertTrue(VelaMixinPlugin.clashes("perf.GlyphQuadMixin","immediatelyfast"::equals));
        assertFalse(VelaMixinPlugin.clashes("perf.GlyphQuadMixin","sodium"::equals));
    }
}
