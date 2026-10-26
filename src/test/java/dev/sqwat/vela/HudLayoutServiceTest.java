package dev.sqwat.vela;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.ui.hud.HudLayoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutServiceTest {
    @Test void snappedNativePositionFollowsNewServerBounds(){
        var layout=new HudLayoutService(directory,new ModuleRegistry());
        layout.recordNative("scoreboard",new dev.sqwat.vela.ui.render.Rect(200,70,110,80));
        layout.move("scoreboard",200,70,320,240);
        layout.recordNative("scoreboard",new dev.sqwat.vela.ui.render.Rect(160,40,150,130));
        assertEquals(new HudLayoutService.HudPosition(160,40),layout.position("scoreboard",320,240));
        assertEquals(new HudLayoutService.HudPosition(0,0),layout.translation("scoreboard",320,240));
    }
    @TempDir Path directory;
    @Test void migratesLegacyGeometryAndKeepsRecoverableBackup()throws Exception{
        java.nio.file.Files.writeString(directory.resolve("hud-layout.json"),"{\"near\":{\"x\":45,\"y\":67}}");
        java.nio.file.Files.writeString(directory.resolve("hud-scale.json"),"{\"near\":1.1}");
        var layout=new HudLayoutService(directory,new ModuleRegistry());
        assertEquals(new HudLayoutService.HudPosition(45,67),layout.position("near"));
        assertEquals(1.1f,layout.scale("near"));
        assertTrue(java.nio.file.Files.exists(directory.resolve("hud.vela")));
        assertTrue(java.nio.file.Files.exists(directory.resolve("legacy-hud-backup/hud-layout.json")));
        var restored=new HudLayoutService(directory,new ModuleRegistry());
        assertEquals(layout.position("near"),restored.position("near"));assertEquals(layout.scale("near"),restored.scale("near"));
    }

    @Test
    void clampsAndPersistsDraggedPositions() {
        ModuleRegistry modules = new ModuleRegistry();
        TestHudModule coordinates = modules.register(new TestHudModule("coordinates"));
        coordinates.setEnabled(true);
        HudLayoutService layout = new HudLayoutService(directory, modules);

        layout.move("coordinates", 999, -20, 320, 180);
        layout.save();
        assertEquals(248, layout.position("coordinates").x());
        assertEquals(0, layout.position("coordinates").y());
        assertTrue(layout.enabledIds().contains("coordinates"));

        HudLayoutService restored = new HudLayoutService(directory, modules);
        assertEquals(layout.position("coordinates"), restored.position("coordinates"));
    }

    @Test
    void clampsUsingActualWideElementSize() {
        HudLayoutService layout = new HudLayoutService(directory, new ModuleRegistry());
        layout.move("coordinates", 999, 999, 320, 180, new HudLayoutService.RectSize(180, 22));
        assertEquals(new HudLayoutService.HudPosition(140, 158), layout.position("coordinates"));
    }

    private static final class TestHudModule extends ClientModule {
        private TestHudModule(String id) { super(id, id, "", ModuleCategory.HUD); }
    }

    @Test void nativeBoundsSurviveAnEmptyFrameWhileEditing(){
        var layout=new HudLayoutService(directory,new ModuleRegistry());
        var rect=new dev.sqwat.vela.ui.render.Rect(40,60,132,72);
        layout.recordNative("scoreboard",rect);layout.setEditing(true);
        layout.recordNative("scoreboard",null);
        assertEquals(rect,layout.nativeOrigin("scoreboard"));
        assertTrue(layout.editableIds().contains("scoreboard"));
        org.junit.jupiter.api.Assertions.assertFalse(layout.nativeVisible("scoreboard"));
        layout.setEditing(false);layout.recordNative("scoreboard",null);
        org.junit.jupiter.api.Assertions.assertNull(layout.nativeOrigin("scoreboard"));
    }

    @Test void previewDoesNotReplaceMeasuredNativePosition(){
        var layout=new HudLayoutService(directory,new ModuleRegistry());
        var real=new dev.sqwat.vela.ui.render.Rect(220,10,180,30);
        layout.recordNative("tablist",real);
        layout.seedNative("tablist",new dev.sqwat.vela.ui.render.Rect(0,0,40,10));
        assertEquals(real,layout.nativeOrigin("tablist"));
        assertTrue(layout.nativeVisible("tablist"));
    }
}
