package dev.sqwat.vela;
import dev.sqwat.vela.render.effect.HitParticles;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
class HitParticlesTest {
    @Test void customLifetimesExpireIndependentlyAndTwistRemainsFinite(){
        var pool=new HitParticles();var random=new Random(2);
        pool.burst(Vec3.ZERO,1,0,random,2_000_000_000L,.7,.2,1.8);
        pool.burst(Vec3.ZERO,1,1,random,350_000_000L,.7,.2,0);
        pool.expire(400_000_000L);assertEquals(1,pool.size());
        var p=pool.particles().iterator().next();assertTrue(Double.isFinite(p.position(1_000_000_000L).x));
        pool.expire(2_000_000_000L);assertEquals(0,pool.size());
    }
    @Test void burstsAreBoundedAndExpire(){
        var pool=new HitParticles();var random=new Random(7);
        for(int i=0;i<100;i++)pool.burst(Vec3.ZERO,32,0,random);
        assertEquals(HitParticles.LIMIT,pool.size());
        pool.expire(HitParticles.LIFETIME-1);assertEquals(HitParticles.LIMIT,pool.size());
        pool.expire(HitParticles.LIFETIME);assertEquals(0,pool.size());
        pool.burst(Vec3.ZERO,16,1_000_000_000L,random);pool.clear();assertEquals(0,pool.size());
    }
    @Test void movementAndFadeDependOnTimeNotFrameCount(){
        var p=new HitParticles.Particle(Vec3.ZERO,new Vec3(1,2,3),0,0);
        assertEquals(new Vec3(.5,.65,1.5),p.position(500_000_000));
        assertEquals(.25,p.opacity(400_000_000));assertEquals(0,p.opacity(HitParticles.LIFETIME));
        assertEquals(0,p.opacity(-1));
    }
    @Test void fallingHurtTimerDoesNotDuplicateBursts(){
        assertTrue(HitParticles.newDamage(0,10));assertTrue(HitParticles.newDamage(4,10));
        assertFalse(HitParticles.newDamage(10,9));assertFalse(HitParticles.newDamage(10,10));assertFalse(HitParticles.newDamage(1,0));
    }
}
