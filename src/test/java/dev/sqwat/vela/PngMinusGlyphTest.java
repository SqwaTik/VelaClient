package dev.sqwat.vela;

import dev.sqwat.vela.ui.render.PngFontMetrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The minus sign is not in the baked atlas; it must render as a dash, not as the '?' fallback. */
class PngMinusGlyphTest {
    @Test void minusSignIsNotAQuestionMark()throws Exception{
        var metrics=dev.sqwat.vela.ui.render.PngFontBaker.bake(dev.sqwat.vela.ui.render.PngFontBaker.font("semibold"),12).metrics("semibold");
        var minus=metrics.line("\u2212").glyphs().getFirst().glyph();
        assertNotSame(metrics.line("?").glyphs().getFirst().glyph(),minus);
        assertSame(metrics.line("\u2013").glyphs().getFirst().glyph(),minus);
    }
}
