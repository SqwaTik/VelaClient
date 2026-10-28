package dev.sqwat.vela;

import dev.sqwat.vela.render.effect.ShockFront;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShockFrontTest {
    @Test void openGroundIsACircle(){
        var front=ShockFront.open(new Vec3(.5,64,.5),8);
        var ring=front.contour(5);
        // One segment per crossed half-block cell (about 32 at radius 5), all about 5 blocks out.
        assertTrue(ring.size()>=28,"segments "+ring.size());
        for(var s:ring){
            double r=Math.hypot(s.a().x-.5,s.a().z-.5);
            assertEquals(5,r,.6);
            assertEquals(64,s.a().y,1e-6);
        }
    }
    @Test void flowsAroundAWall(){
        // A wall 2 blocks east of the centre, 9 blocks long north-south: the point right behind it is reached only
        // around the ends, so its path is much longer than the straight line, and the front there is faint.
        var center=new Vec3(.5,64,.5);
        var front=ShockFront.compute(center,10,(x,y,z)->y<64||(x==2&&Math.abs(z)<=4&&y<66));
        double behind=front.distanceAt(4.5,.5);
        assertTrue(behind>7,"path behind the wall "+behind);
        assertTrue(front.distanceAt(.5,4.5)<4.6);
    }
}
