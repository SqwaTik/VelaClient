package dev.sqwat.vela;

import dev.sqwat.vela.module.utility.ShulkerPreview;
import dev.sqwat.vela.module.ModuleCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShulkerPinTest {
    @Test void holdsVisiblePreviewUntilControlReleased() {
        var p=new ShulkerPreview();var screen=new Object();
        p.frame(screen,false);p.observe(new ShulkerPreview.Contents(List.of()),20,30,screen);
        p.frame(screen,true);assertTrue(p.pinned());assertTrue(p.contains(20,30));
        p.frame(screen,true);assertTrue(p.pinned());
        p.frame(screen,false);assertFalse(p.pinned());assertFalse(p.contains(20,30));
        assertEquals(ModuleCategory.UTILITIES,p.category());
    }
    @Test void changingScreenOrMissingPreviewDoesNotPinStaleContents() {
        var p=new ShulkerPreview();var screen=new Object();
        p.observe(new ShulkerPreview.Contents(List.of()),20,30,screen);
        p.frame(new Object(),true);assertFalse(p.pinned());
        p.observe(new ShulkerPreview.Contents(List.of()),20,30,screen);
        p.frame(screen,false);p.frame(screen,true);assertFalse(p.pinned());
    }
    @Test void slotsAndGuttersMatchDrawnGrid() {
        assertEquals(0,ShulkerPreview.slotAt(10,20,12,22));
        assertEquals(26,ShulkerPreview.slotAt(10,20,172,74));
        assertEquals(-1,ShulkerPreview.slotAt(10,20,29,22));
        assertEquals(-1,ShulkerPreview.slotAt(10,20,11.9,22));
        assertEquals(-1,ShulkerPreview.slotAt(10,20,174,22));
    }
}
