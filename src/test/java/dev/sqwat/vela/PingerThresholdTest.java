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
}
