package dev.sqwat.vela;
import dev.sqwat.vela.render.JumpWaveShape;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JumpWaveShapeTest {
    @Test void waveStaysVisibleThenFadesWithoutPop(){
        assertEquals(0,JumpWaveShape.opacity(0));assertEquals(0,JumpWaveShape.opacity(1));
        assertTrue(JumpWaveShape.opacity(.5)>.8);
        for(int i=0;i<=100;i++)assertTrue(JumpWaveShape.opacity(i/100d)>=0&&JumpWaveShape.opacity(i/100d)<=1);
    }
    @Test void radiusGrowsMonotonicallyAndStaysBounded(){
        double before=0;
        for(int i=0;i<=100;i++){double r=JumpWaveShape.radius(i/100d,2);assertTrue(r>=before&&r<=2);before=r;}
        assertEquals(0,JumpWaveShape.radius(0,2));assertEquals(2,JumpWaveShape.radius(1,2));
    }
    @Test void removedVolumeCannotChangeWaveHeight(){
        assertTrue(new dev.sqwat.vela.module.JumpCircle().settings().stream().noneMatch(s->s.id().equals("volume")));
        assertEquals(4,JumpWaveShape.MAX_WAVES);
    }
}
