package dev.sqwat.vela;

import dev.sqwat.vela.ui.*;
import dev.sqwat.vela.ui.render.*;
import dev.sqwat.vela.module.ModuleRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class InteractionQualityTest {
    @TempDir Path root;
    @Test void reloadNotificationPreviewOnlyAppearsWhileEditing(){var h=new HudLayoutService(root,new ModuleRegistry());assertFalse(h.editableIds().contains("notifications"));h.setEditing(true);assertTrue(h.editableIds().contains("notifications"));h.setEditing(false);assertFalse(h.editableIds().contains("notifications"));}
    @Test void whiteAndDarkAccentsHaveContrastingLabels(){assertEquals(0xFF10141C,VelaTheme.contrastText(0xFFFFF5FA));assertEquals(0xFFF3F5FA,VelaTheme.contrastText(0xFF10141C));assertEquals(0xFF10141C,VelaTheme.contrastText(0xFFFFFF00));}
    @Test void gridAndEdgesSnap(){assertEquals(16,HudSnap.grid(18));assertEquals(80,HudSnap.align(82,20,100,100));assertEquals(100,HudSnap.align(98,20,100,140));assertEquals(50,HudSnap.align(50,20,100,140));}
    @Test void noFakeVanillaHudWhenServerHasNone(){var h=new HudLayoutService(root,new ModuleRegistry());assertTrue(h.editableIds().isEmpty());h.recordNative("bossbar",new Rect(70,3,182,14));assertTrue(h.editableIds().contains("bossbar"));h.recordNative("bossbar",null);assertFalse(h.editableIds().contains("bossbar"));}
    @Test void nativeMeasuredOriginHasNoDefaultOffset(){var h=new HudLayoutService(root,new ModuleRegistry());h.recordNative("scoreboard",new Rect(213,41,104,92));assertEquals(new HudLayoutService.HudPosition(0,0),h.translation("scoreboard",320,180));h.move("scoreboard",180,30,320,180,new HudLayoutService.RectSize(104,92));assertEquals(new HudLayoutService.HudPosition(-33,-11),h.translation("scoreboard",320,180));}
    @Test void compactAndNativeScalesPersist(){var h=new HudLayoutService(root,new ModuleRegistry());assertEquals(.75f,h.scale("armor"));assertEquals(1f,h.scale("bossbar"));h.setScale("armor",.5f);assertEquals(.5f,new HudLayoutService(root,new ModuleRegistry()).scale("armor"));h.setScale("bossbar",.5f);assertEquals(.5f,new HudLayoutService(root,new ModuleRegistry()).scale("bossbar"));}
}
