package dev.sqwat.vela;
import dev.sqwat.vela.ui.input.KeyLabels;
import dev.sqwat.vela.ui.render.*;
import dev.sqwat.vela.render.entity.VisibilityBounds;
import dev.sqwat.vela.core.ConfigService;
import dev.sqwat.vela.module.ModuleRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ReferenceRegressionTest {
    @TempDir Path root;
    @Test void bindsHaveStableCompactNames(){assertEquals("RShift",KeyLabels.of(344,"Правый Shift"));assertEquals("Space",KeyLabels.of(32,"Пробел"));assertEquals("F12",KeyLabels.of(301,""));assertEquals("A",KeyLabels.of(65,"ф"));assertEquals("None",KeyLabels.of(-1,""));}
    @Test void storageBoundsIncludeEveryDoubleChestOrientationAndOpenLids(){
        var b=VisibilityBounds.storage(12,64,-3);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<=2;dy++)assertTrue(b.contains(12+dx,64+dy,-3+dz));
        assertTrue(b.contains(14,65,-3));assertFalse(b.contains(16,65,-3));
    }
    @Test void accountManagerIsCenteredAndHasNoCountBasedLimit(){var l=AccountChrome.layout(800,500);assertEquals(400,l.x()+l.width()/2);assertEquals(250,l.y()+l.height()/2);assertEquals(l.microsoft().y(),l.offline().y());assertTrue(l.microsoft().x()+l.microsoft().width()<l.offline().x());assertTrue(l.list().y()+l.list().height()<l.microsoft().y());assertEquals(l,AccountChrome.layout(800,500,50));}
    @Test void profileDuplicationAndDeletionAreRecoverable()throws Exception{
        try(var service=new ConfigService(root,new ModuleRegistry())){
            service.createProfile("sample");service.current().uiScale=1.2f;service.flush();
            String copy=service.duplicateProfile("sample");assertTrue(service.profiles().contains(copy));service.selectProfile(copy);assertEquals(1.2f,service.current().uiScale);
            service.archiveProfile(copy);assertNotEquals(copy,service.current().activeProfile);assertFalse(service.profiles().contains(copy));
            try(var archived=Files.list(root.resolve("profiles/archive"))){assertEquals(1,archived.count());}
        }
    }
    @Test void startupRasterLabelsArePackaged(){for(String label:new String[]{"title","resources","initializing","ready"})assertNotNull(getClass().getResource("/assets/velaclient/textures/gui/startup_"+label+".png"));}
    @Test void cameraTurnMakesPreviouslyHiddenStorageVisibleImmediately(){
        var projection=new org.joml.Matrix4f().perspective((float)Math.toRadians(70),16f/9f,.1f,256);
        var front=new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f(),projection);front.prepare(0,0,0);
        var back=new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f().rotateY((float)Math.PI),projection);back.prepare(0,0,0);
        var box=new net.minecraft.world.phys.AABB(-1,-1,14,1,1,16);
        assertFalse(front.isVisible(box));assertTrue(back.isVisible(box));
    }
}
