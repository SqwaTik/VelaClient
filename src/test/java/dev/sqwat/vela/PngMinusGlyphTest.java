package dev.sqwat.vela;

import dev.sqwat.vela.ui.render.PngFontMetrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The minus sign is not in the baked atlas; it must render as a dash, not as the '?' fallback. */
class PngMinusGlyphTest {
    @Test void minusSignIsNotAQuestionMark()throws Exception{
        String base="/assets/velaclient/ui/";
        var metrics=new PngFontMetrics(getClass().getResourceAsStream(base+"font_semibold_12.csv"),getClass().getResourceAsStream(base+"kerning_semibold.csv"));
        var minus=metrics.line("\u2212").glyphs().getFirst().glyph();
        assertNotSame(metrics.line("?").glyphs().getFirst().glyph(),minus);
        assertSame(metrics.line("\u2013").glyphs().getFirst().glyph(),minus);
    }
}
