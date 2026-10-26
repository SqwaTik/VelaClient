package dev.sqwat.vela;
import dev.sqwat.vela.ui.animation.HudMotion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudIdleWorkTest {
    @Test void skipsHiddenButFinishesExitAnimation(){
        var motion=new HudMotion();
        assertTrue(motion.skipHidden("inventory",false));
        motion.update("inventory",true,80,0,true);
        assertFalse(motion.skipHidden("inventory",false));
        for(int i=1;i<=8;i++)motion.update("inventory",false,80,i*100_000_000L,false);
        assertTrue(motion.skipHidden("inventory",false));
        assertFalse(motion.skipHidden("inventory",true));
        assertEquals(0,motion.update("inventory",true,80,2_000_000_000L,false).reveal());
    }
}
