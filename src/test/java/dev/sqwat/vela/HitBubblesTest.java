package dev.sqwat.vela;
import dev.sqwat.vela.module.HitBubbles;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HitBubblesTest {
    @Test void shockwaveModeHasJumpCircleBudget(){
        var m=new HitBubbles();m.setEnabled(true);
        ((dev.sqwat.vela.module.setting.ChoiceSetting)m.settings().stream().filter(s->s.id().equals("mode")).findFirst().orElseThrow()).set("Ударная волна");
        assertTrue(m.shockwave());for(int i=0;i<30;i++)m.spawn(Vec3.ZERO,i);
        for(var setting:m.settings())if(java.util.Set.of("color_mode","color","color2").contains(setting.id()))assertFalse(setting.visible());
        assertEquals(dev.sqwat.vela.render.JumpWaveShape.MAX_WAVES,m.count());
        m.setEnabled(false);assertEquals(0,m.count());
    }
    @Test void boundedAndInertWhenDisabled(){
        var m=new HitBubbles();m.spawn(Vec3.ZERO,0);assertEquals(0,m.count());
        m.setEnabled(true);for(int i=0;i<100;i++)m.spawn(Vec3.ZERO,i);
        assertEquals(HitBubbles.LIMIT,m.count());m.setEnabled(false);assertEquals(0,m.count());
    }
    @Test void envelopeAndExpiryAreTimeBased(){
        var m=new HitBubbles();m.setEnabled(true);m.spawn(Vec3.ZERO,100);
        var wave=m.waves().iterator().next();
        assertEquals(0,wave.envelope(100));assertEquals(1,wave.envelope(500_000_100),.00001);
        assertEquals(0,wave.envelope(1_000_000_100));m.expire(1_000_000_100);assertEquals(0,m.count());
    }
}
