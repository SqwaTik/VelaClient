package dev.sqwat.vela;
import dev.sqwat.vela.ui.hud.HudLayoutService;
import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.module.ModuleRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class NativePreviewIsolationTest {
    @TempDir Path root;
    @Test void previewDoesNotBecomeServerGeometry(){
        var h=new HudLayoutService(root,new ModuleRegistry());h.setEditing(true);
        h.previewNative("scoreboard",new Rect(188,54,132,72));
        assertTrue(h.editableIds().contains("scoreboard"));assertFalse(h.nativeVisible("scoreboard"));
        h.setEditing(false);assertNull(h.nativeOrigin("scoreboard"));
        h.recordNative("scoreboard",new Rect(200,33,120,110));
        assertEquals(new HudLayoutService.HudPosition(0,0),h.translation("scoreboard",320,180));
    }
}
