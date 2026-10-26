package dev.sqwat.vela;
import dev.sqwat.vela.ui.*;
import dev.sqwat.vela.ui.animation.HudMotion;
import dev.sqwat.vela.marker.MarkerService;
import dev.sqwat.vela.module.ModuleRegistry;
import dev.sqwat.vela.config.ConfigService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SharedUiRegressionTest {
    @TempDir Path root;
    @Test void configurationNoticeFitsTextWithSmallPadding(){assertEquals(94,dev.sqwat.vela.ui.render.NotificationChrome.compactWidth(60,48));assertEquals(136,dev.sqwat.vela.ui.render.NotificationChrome.compactWidth(60,120));assertEquals(208,dev.sqwat.vela.ui.render.NotificationChrome.compactWidth(800,0));}
    @Test void guiScaleOneTwoFourPreservesPhysicalHudAndHitBounds(){
        var layout=new HudLayoutService(root,new ModuleRegistry());layout.move("inventory",200,160,960,540,new HudLayoutService.RectSize(138,63));
        for(int gui:new int[]{1,2,4,2,1}){layout.viewport(gui);var p=layout.position("inventory",1920/gui,1080/gui);var b=layout.bounds(null,"inventory");assertEquals(400,p.x()*gui);assertEquals(320,p.y()*gui);assertEquals(276,b.width()*gui,2);assertEquals(126,b.height()*gui,2);}
    }
    @Test void draggingAtScaleFourPersistsInScaleTwoCoordinates(){
        var layout=new HudLayoutService(root,new ModuleRegistry());layout.viewport(4);layout.move("inventory",160,80,480,270,new HudLayoutService.RectSize(69,32));layout.save();
        var restored=new HudLayoutService(root,new ModuleRegistry());restored.viewport(2);assertEquals(new HudLayoutService.HudPosition(320,160),restored.position("inventory",960,540));
    }
    @Test void nativeDefaultStillFollowsServerBoundsAfterScaleChange(){
        var layout=new HudLayoutService(root,new ModuleRegistry());layout.viewport(4);layout.recordNative("scoreboard",new dev.sqwat.vela.ui.render.Rect(300,70,160,80));layout.move("scoreboard",300,70,480,270);layout.viewport(2);layout.recordNative("scoreboard",new dev.sqwat.vela.ui.render.Rect(800,180,160,120));assertEquals(new HudLayoutService.HudPosition(800,180),layout.position("scoreboard",960,540));
    }
    @Test void everyHudUsesSameRevealAndSmoothResize(){
        var motion=new HudMotion();for(String id:new String[]{"target","hotkeys","armor","inventory","near","potions","watermark","speakers","cooldowns","coordinates"}){
            assertEquals(0,motion.update(id,true,40,0,false).reveal());var next=motion.update(id,true,80,50_000_000,false);assertTrue(next.reveal()>0&&next.reveal()<1);assertTrue(next.height()>40&&next.height()<80);assertEquals(motion.update("reference-"+id,false,40,0,false).reveal(),0);assertEquals(1,motion.update(id,true,80,51_000_000,true).reveal());
        }
    }
    @Test void romanLevelsOmitLevelOne(){assertEquals("",EffectLevels.suffix(0));assertEquals(" II",EffectLevels.suffix(1));assertEquals(" IV",EffectLevels.suffix(3));assertEquals(" IX",EffectLevels.suffix(8));}
    @Test void controlBackspaceDeletesUnicodeWordAndSelection(){var edit=new TextEdit();edit.set("один два 😀  ");edit.eraseWord();assertEquals("один два ",edit.text());edit.eraseWord();assertEquals("один ",edit.text());edit.selectAll();edit.eraseWord();assertEquals("",edit.text());}
    @Test void markerNumbersAreScopedAndReuseRemovedNames(){
        var markers=new MarkerService(root);markers.state().nextNumber=99;assertEquals("Метка 1",markers.nextName("a","overworld"));
        markers.add("Метка 1","a","overworld",0,0,0,-1,"markers",false);assertEquals("Метка 2",markers.nextName("a","overworld"));assertEquals("Метка 1",markers.nextName("b","overworld"));assertEquals("Метка 1",markers.nextName("a","nether"));
        markers.remove(markers.state().points.getFirst().id());assertEquals("Метка 1",new MarkerService(root).nextName("a","overworld"));
    }
    @Test void notificationActionAddsBottomRowWithSameMotion(){assertEquals(28,Notifications.height(false,false));assertEquals(56,Notifications.height(false,true));assertEquals(84,Notifications.height(true,true));assertEquals(0,Notifications.reveal(0));assertEquals(1,Notifications.reveal(150));assertEquals(Notifications.reveal(75),Notifications.reveal(3425));}
    @Test void configKeyTransfersBetweenIndependentOfflineStores()throws Exception{
        try(var first=new ConfigService(root.resolve("one"),new ModuleRegistry());var second=new ConfigService(root.resolve("two"),new ModuleRegistry())){first.current().accentColor="#123456";first.requestSave();String key=first.shareProfile(first.current().activeProfile);second.importProfile(key);assertEquals("#123456",second.current().accentColor);}
    }
}
