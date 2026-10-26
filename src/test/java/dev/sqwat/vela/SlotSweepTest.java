package dev.sqwat.vela;
import dev.sqwat.vela.module.SlotSweep;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SlotSweepTest {
    @Test void transfersEachSlotOnlyOncePerGesture(){var sweep=new SlotSweep();assertTrue(sweep.visit(2));assertFalse(sweep.visit(2));assertTrue(sweep.visit(3));assertFalse(sweep.visit(-1));sweep.reset();assertTrue(sweep.visit(2));}
    @Test void appliesConfiguredDelayBetweenDistinctSlots(){
        var sweep=new SlotSweep();long start=1_000_000_000L;
        assertTrue(sweep.visit(2,start,50));assertFalse(sweep.visit(3,start+49_000_000,50));
        assertTrue(sweep.visit(3,start+50_000_000,50));assertFalse(sweep.visit(3,start+100_000_000,50));
    }
}
