package dev.sqwat.vela;

import dev.sqwat.vela.render.effect.ThrowPhysics;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThrowPhysicsTest {
    @Test void potionOffsetChangesVerticalComponentBeforeNormalization(){
        Vec3 expected=new Vec3(0,-Math.sin(Math.toRadians(25)),Math.cos(Math.toRadians(45))).normalize().scale(.5);
        assertEquals(expected,ThrowPhysics.launch(0,45,-20,.5,Vec3.ZERO,true));
    }
    @Test void airborneLaunchInheritsVerticalMotionOnlyOffGround(){
        var movement=new Vec3(.2,.42,.1);
        var grounded=ThrowPhysics.launch(0,0,0,1.5,movement,true);
        var jumping=ThrowPhysics.launch(0,0,0,1.5,movement,false);
        assertEquals(0,grounded.y,1e-8);assertEquals(.42,jumping.y,1e-8);
        assertEquals(.2,jumping.x,1e-8);assertEquals(1.6,jumping.z,1e-8);
    }
    @Test void gravityPrecedesAirDrag(){
        var v=ThrowPhysics.velocity(new Vec3(1.5,0,0),.03,false);
        assertEquals(1.485,v.x,1e-8);
        assertEquals(-.0297,v.y,1e-8);
    }
    @Test void waterSlowsTheWholeVelocity(){
        var v=ThrowPhysics.velocity(new Vec3(1,1,0),.03,true);
        assertEquals(.8,v.x,1e-8);
        assertEquals(.776,v.y,1e-8);
    }
}
