package dev.sqwat.vela;
import dev.sqwat.vela.render.shader.CustomSkySource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CustomSkySourceTest {
    @Test void boundsAndEmptyInput(){
        assertThrows(IllegalArgumentException.class,()->CustomSkySource.validate(" "));
        assertThrows(IllegalArgumentException.class,()->CustomSkySource.validate("a".repeat(CustomSkySource.LIMIT+1)));
        assertThrows(IllegalArgumentException.class,()->CustomSkySource.validate("a\0b"));
        assertEquals("a".repeat(CustomSkySource.LIMIT),CustomSkySource.validate("a".repeat(CustomSkySource.LIMIT)));
    }
    @Test void keepsSourceAndMapsItsLineNumbers(){
        String wrapped=CustomSkySource.fragment(CustomSkySource.DEFAULT);
        assertTrue(wrapped.contains("#line 1\n"+CustomSkySource.DEFAULT));
        assertTrue(wrapped.contains("mainImage(color,texCoord*misc.yz)"));
    }
}
