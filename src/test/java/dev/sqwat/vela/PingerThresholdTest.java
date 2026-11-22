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
    /** Regression: the warning repeated on every further loss; now it fires once and re-arms only after recovery. */
    @Test void rearmsOnlyClearlyAboveTheThreshold(){
        assertFalse(Pinger.rearms(6,6,2));         // at the threshold
        assertFalse(Pinger.rearms(7.5,6,2));       // regeneration just above it
        assertTrue(Pinger.rearms(8.5,6,2));        // healed by more than a heart
        assertFalse(Pinger.rearms(24,20,5));assertTrue(Pinger.rearms(26,20,5));
        assertFalse(Pinger.rearms(Double.NaN,20,5));
    }
}
