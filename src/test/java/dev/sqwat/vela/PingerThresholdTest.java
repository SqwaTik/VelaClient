package dev.sqwat.vela;
import dev.sqwat.vela.module.utility.Pinger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PingerThresholdTest {
    @Test void recoveryClearsWithoutHysteresisOrExpiry(){
        assertTrue(Pinger.shouldWarn(2,3));
        assertTrue(Pinger.shouldWarn(3,3));
        assertFalse(Pinger.shouldWarn(3.01,3));
        assertFalse(Pinger.shouldWarn(20,3));
    }
    @Test void armorUsesTheSameCurrentStateRule(){
        assertTrue(Pinger.shouldWarn(20,20));
        assertFalse(Pinger.shouldWarn(21,20));
        assertFalse(Pinger.shouldWarn(Double.NaN,20));
    }
    /** Regression: the armour warning was re-shown every tick and never went away. */
    @Test void warnsOnceThenOnlyOnFurtherDrop(){
        assertTrue(Pinger.due(18,Double.NaN,5));   // first crossing
        assertFalse(Pinger.due(18,18,5));          // same value next tick: no new warning
        assertFalse(Pinger.due(14,18,5));          // small further loss
        assertTrue(Pinger.due(13,18,5));           // 5 % further down
        assertFalse(Pinger.due(5.5,6,2));assertTrue(Pinger.due(4,6,2));
    }
}
