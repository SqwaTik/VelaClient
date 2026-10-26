package dev.sqwat.vela;
import dev.sqwat.vela.module.Crosshair;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairTest {
    @Test void cooldownOnlyOpensGapWithinConfiguredRange(){
        assertEquals(2,Crosshair.attackGap(2,10,1));
        assertEquals(12,Crosshair.attackGap(2,10,0));
        assertEquals(7,Crosshair.attackGap(2,10,.5f));
        assertEquals(2,Crosshair.attackGap(2,10,2));
        assertEquals(12,Crosshair.attackGap(2,10,-1));
    }
    @Test void newVisualDoesNotEnableItself(){assertFalse(new Crosshair().enabled());}
}
