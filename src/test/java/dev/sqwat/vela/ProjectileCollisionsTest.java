package dev.sqwat.vela;

import dev.sqwat.vela.render.ProjectileCollisions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProjectileCollisionsTest {
    @Test void nearestHitIsIndependentOfCandidateOrder(){
        var near=new AABB(2,-1,-1,3,1,1);var far=new AABB(6,-1,-1,7,1,1);
        assertEquals(new Vec3(2,0,0),ProjectileCollisions.nearest(Vec3.ZERO,new Vec3(10,0,0),List.of(far,near)));
        assertEquals(new Vec3(2,0,0),ProjectileCollisions.nearest(Vec3.ZERO,new Vec3(10,0,0),List.of(near,far)));
    }
    @Test void entitiesBehindTerrainAndOutsideSegmentCannotExtendImpact(){
        var end=new Vec3(1,0,0);
        assertSame(end,ProjectileCollisions.nearest(Vec3.ZERO,end,List.of(new AABB(2,-1,-1,3,1,1),new AABB(0,2,0,1,3,1))));
    }
    @Test void optimizedClipMatchesNativeClip(){
        var random=new java.util.Random(42);
        for(int i=0;i<2000;i++){
            var from=new Vec3(random.nextDouble()*10,random.nextDouble()*10,random.nextDouble()*10);
            var to=new Vec3(random.nextDouble()*10,random.nextDouble()*10,random.nextDouble()*10);
            var box=new AABB(random.nextDouble()*10,random.nextDouble()*10,random.nextDouble()*10,
                random.nextDouble()*10,random.nextDouble()*10,random.nextDouble()*10);
            assertEquals(box.clip(from,to).orElse(to),ProjectileCollisions.nearest(from,to,List.of(box)));
        }
    }
}
