package dev.sqwat.vela;
import dev.sqwat.vela.ui.animation.CrosshairMotion;
import dev.sqwat.vela.module.visual.Crosshair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairMotionTest {
    @Test void circleShrinksArcNotRadiusDuringSwing(){
        assertEquals(1,CrosshairMotion.arc(0,true));assertEquals(.5f,CrosshairMotion.arc(.25f,true));
        assertEquals(0,CrosshairMotion.arc(.5f,true));assertEquals(.5f,CrosshairMotion.arc(.75f,true));
        assertEquals(1,CrosshairMotion.arc(1,true));
        assertEquals(1,CrosshairMotion.arc(.5f,false));
    }
    @Test void dynamicOffCancelsInertiaAndReenableStartsAtCurrentCamera(){
        var m=new CrosshairMotion();m.update(0,0,false,true,.016f);
        m.update(30,20,true,true,.016f);assertTrue(m.x()>0);assertTrue(m.y()>0);
        m.update(30,20,true,false,.016f);assertEquals(0,m.x());assertEquals(0,m.y());
        m.update(30,20,true,true,.016f);assertEquals(0,m.x());assertEquals(0,m.y());
        m.reset();assertEquals(0,m.reaction());
    }
    @Test void wrappingYawDoesNotThrowReticleAcrossScreen(){
        var m=new CrosshairMotion();m.update(179,0,false,true,.016f);m.update(-179,0,false,true,.016f);
        assertTrue(m.x()>0&&m.x()<2);
    }
    @Test void targetReactionIsSmoothAndVisibleEvenWithPinkTheme(){
        var m=new CrosshairMotion();m.update(0,0,true,false,.016f);assertTrue(m.reaction()>0&&m.reaction()<1);
        for(int i=0;i<100;i++)m.update(0,0,true,false,.016f);
        assertTrue(m.reaction()>.99f);
        assertNotEquals(0xFFFF6178,Crosshair.reactionColor(0xFFFF6178));
        m.update(0,0,false,false,.016f);assertTrue(m.reaction()<.99f);
    }
}
