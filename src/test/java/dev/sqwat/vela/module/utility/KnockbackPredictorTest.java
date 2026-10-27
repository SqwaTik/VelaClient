package dev.sqwat.vela.module.utility;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** LivingEntity.knockback as vanilla computes it: half the old motion minus the push, a hop on the ground. */
class KnockbackPredictorTest {
    @Test void standingTargetIsPushedAwayAndUp(){
        // Attacker to the south (+z): the target flies north (-z) and hops 0.4.
        Vec3 v=KnockbackPredictor.knockback(Vec3.ZERO,.4,0,1,true);
        assertEquals(0,v.x,1e-9);assertEquals(.4,v.y,1e-9);assertEquals(-.4,v.z,1e-9);
    }
    @Test void sprintHitAddsAlongTheLookAndHalvesTheFirstPush(){
        Vec3 first=KnockbackPredictor.knockback(Vec3.ZERO,.4,0,1,true);
        // Yaw 0 looks south: sin(0)=0, -cos(0)=-1, so the extra push goes south... away from an attacker facing it.
        Vec3 second=KnockbackPredictor.knockback(first,.5,0,-1,true);
        assertEquals(-.4/2+.5,second.z,1e-9);
        assertEquals(.4,second.y,1e-9); // capped at 0.4 on the ground
    }
    @Test void airborneTargetKeepsItsVerticalSpeed(){
        Vec3 v=KnockbackPredictor.knockback(new Vec3(0,-.3,0),.4,1,0,false);
        assertEquals(-.3,v.y,1e-9);assertEquals(-.4,v.x,1e-9);
    }
}
