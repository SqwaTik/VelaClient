package dev.sqwat.vela;

import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.hud.NearPresence;
import dev.sqwat.vela.ui.render.ModuleSettingsGeometry;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpdateControlsTest {
    @Test void legacyHeadTransformMatchesVanillaAndDoesNotMutateParent(){
        var parent=new org.joml.Matrix4f().translation(2,3,4);
        var result=dev.sqwat.vela.render.entity.LegacyHeadTransform.compose(parent);
        var point=result.transformPosition(new org.joml.Vector3f(0,1,0));
        assertEquals(2.5f,point.x,1e-6);assertEquals(2f,point.y,1e-6);assertEquals(4.5f,point.z,1e-6);
        assertEquals(2f,parent.m30());assertEquals(3f,parent.m31());assertEquals(4f,parent.m32());
    }
    @Test void segmentBoundsCoverBothOptionsWithoutOverlap(){
        var bounds=new dev.sqwat.vela.ui.render.Rect(10,20,201,21);
        var a=dev.sqwat.vela.ui.render.SegmentedControl.segment(bounds,0,2);
        var b=dev.sqwat.vela.ui.render.SegmentedControl.segment(bounds,1,2);
        assertEquals(11,a.x());assertEquals(19,a.height());
        assertEquals(210,b.x()+b.width());assertTrue(a.x()+a.width()<b.x());
    }
    @Test void segmentedFlagsAreIndependentAndUseTheExistingSettings(){
        var attack=new BooleanSetting("attack","Attack","",true);
        var use=new BooleanSetting("use","Use","",false);
        var group=new BooleanGroup("actions","Действия",List.of(attack,use),true);
        assertTrue(group.segmented());
        group.toggle("Use");assertTrue(attack.value());assertTrue(use.value());
        assertEquals("Attack, Use",group.label());
        group.toggle("Attack");assertFalse(attack.value());assertTrue(use.value());
        use.set(false);assertEquals("",group.label());
        assertEquals(44,ModuleSettingsGeometry.rowHeight(group));
    }
    @Test void transientEntitiesNeverBecomeVisible(){
        var presence=new NearPresence<String>();
        for(int t=0;t<4;t++){assertFalse(presence.observe("bot",t));presence.finish(t);}
        presence.finish(4);
        assertFalse(presence.observe("bot",20));
    }
    @Test void stablePlayersQualifyAndWorldResetRequiresQualificationAgain(){
        var presence=new NearPresence<String>();
        for(int t=0;t<10;t++){assertFalse(presence.observe("player",t));presence.finish(t);}
        assertTrue(presence.observe("player",10));presence.finish(10);
        assertTrue(presence.observe("player",11));
        presence.clear();assertFalse(presence.observe("player",12));
    }
    @Test void MissingOrReplacedEntityDoesNotInheritQualification(){
        var presence=new NearPresence<Object>();Object old=new Object(),replacement=new Object();
        for(int t=0;t<=10;t++){presence.observe(old,t);presence.finish(t);}
        assertFalse(presence.observe(replacement,11));presence.finish(11);
        assertFalse(presence.observe(old,12));
    }
}
