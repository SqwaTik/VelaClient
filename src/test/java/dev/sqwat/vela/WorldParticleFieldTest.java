package dev.sqwat.vela;

import dev.sqwat.vela.render.WorldParticleField;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class WorldParticleFieldTest {
    @Test void populationIsBoundedAndRefillsGradually() {
        var field=new WorldParticleField();var random=new Random(1);
        field.tick(Vec3.ZERO,999,12,0,random);
        assertEquals(4,field.particles().size());
        for(int i=0;i<100;i++)field.tick(Vec3.ZERO,999,12,i*50_000_000L,random);
        assertEquals(WorldParticleField.LIMIT,field.particles().size());
        field.tick(Vec3.ZERO,8,12,5_000_000_000L,random);
        assertEquals(8,field.particles().size());
    }
    @Test void teleportAndClearDiscardOldParticles() {
        var field=new WorldParticleField();var random=new Random(2);
        field.tick(Vec3.ZERO,32,12,0,random);
        var old=field.particles().getFirst();
        field.tick(new Vec3(1000,0,0),32,12,50_000_000L,random);
        assertFalse(field.particles().contains(old));
        field.clear();assertTrue(field.particles().isEmpty());
    }
    @Test void lifetimeFadesAtBothEndsAndMotionIsContinuous() {
        var field=new WorldParticleField();field.tick(Vec3.ZERO,1,12,0,new Random(3));
        var p=field.particles().getFirst();
        assertEquals(0,p.opacity(0));assertTrue(p.opacity(1_000_000_000L)>.9);
        assertEquals(0,p.opacity(8_000_000_000L));
        assertTrue(p.position(1_000_000_000L).distanceTo(p.position(1_001_000_000L))<.01);
    }
}
