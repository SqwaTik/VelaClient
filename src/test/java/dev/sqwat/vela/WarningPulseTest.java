package dev.sqwat.vela;
import dev.sqwat.vela.ui.animation.WarningPulse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WarningPulseTest {
    @Test void fadesInAndOutWithoutExceedingOpacity(){
        long end=4_500_000_000L;
        assertEquals(0,WarningPulse.opacity(1_000_000_000L,end,false));
        assertEquals(0,WarningPulse.opacity(end,end,false));
        assertTrue(WarningPulse.opacity(1_010_000_000L,end,false)<.01);
        for(long now=1_000_000_000L;now<=end;now+=10_000_000L){float value=WarningPulse.opacity(now,end,false);assertTrue(value>=0&&value<=.36001f);}
        assertEquals(WarningPulse.opacity(2_000_000_000L,end,true),WarningPulse.opacity(3_000_000_000L,end,true));
    }
}
