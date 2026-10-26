package dev.sqwat.vela;
import dev.sqwat.vela.module.*;
import dev.sqwat.vela.module.setting.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AtmosphereAnimationsTest {
    @Test void tabSpringSettlesAndCanReverseWithoutJump(){
        var motion=new dev.sqwat.vela.ui.animation.FiniteMotion();
        motion.reset(0);
        assertEquals(0,motion.update(1,0,.3f,true));
        assertTrue(motion.update(1,.2f,.3f,true)>1);
        assertEquals(1,motion.update(1,.11f,.3f,true));
        float closing=motion.update(0,.12f,.3f,true);
        assertEquals(closing,motion.update(1,0,.3f,true));
        assertEquals(1,motion.update(1,1,.3f,true));
        assertEquals(0,motion.update(0,1,.3f,true));
    }
    @Test void chatEntranceUsesBackCurveAndEndsAtRest(){
        assertEquals(1,dev.sqwat.vela.ui.animation.FiniteMotion.back(1),.00001);
        assertEquals(0,dev.sqwat.vela.ui.animation.FiniteMotion.back(0));
        assertTrue(dev.sqwat.vela.ui.animation.FiniteMotion.back(.3f)<0);
    }
    @Test void skyOverrideIsOptInAndUsesSelectedColor(){
        var a=new Ambience();int vanilla=0xFF4488BB;assertEquals(vanilla,a.skyColor(vanilla));
        ((BooleanSetting)a.settings().stream().filter(s->s.id().equals("sky")).findFirst().orElseThrow()).set(true);
        assertEquals(dev.sqwat.vela.ui.render.VelaTheme.PRIMARY,a.skyColor(vanilla));
        ((BooleanSetting)a.settings().stream().filter(s->s.id().equals("sky_client_color")).findFirst().orElseThrow()).set(false);
        ((ColorSetting)a.settings().stream().filter(s->s.id().equals("sky_color")).findFirst().orElseThrow()).set(0x123456);
        assertEquals(0xFF123456,a.skyColor(vanilla));
    }
    @Test void openingAnimationSettlesAndDoesNotOvershoot(){
        assertEquals(1,Animations.remaining(0,220));assertEquals(0,Animations.remaining(220,220));assertEquals(0,Animations.remaining(999,220));
        float previous=1;for(int elapsed=0;elapsed<=220;elapsed++){float current=Animations.remaining(elapsed,220);assertTrue(current>=0&&current<=previous);previous=current;}
        assertFalse(new Animations().enabled());
    }
}
