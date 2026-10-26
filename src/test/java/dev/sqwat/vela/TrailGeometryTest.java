package dev.sqwat.vela;

import dev.sqwat.vela.render.effect.TrailGeometry;
import dev.sqwat.vela.render.effect.TrailHistory;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrailGeometryTest {
    private final TrailHistory.Point a=new TrailHistory.Point(Vec3.ZERO,0);
    private final TrailHistory.Point b=new TrailHistory.Point(new Vec3(2,0,0),50_000_000);
    @Test void ribbonUsesBothFullHeightEdges(){
        var segment=TrailGeometry.segment(a,b,1.8,5,false,1);
        assertEquals(.12,segment.firstStart().y);assertEquals(1.8,segment.secondStart().y);
        assertEquals(2,segment.firstEnd().x);assertEquals(2,segment.secondEnd().x);
    }
    @Test void headInterpolationDoesNotMovePublishedStart(){
        var half=TrailGeometry.segment(a,b,1.8,3,false,.5);
        var full=TrailGeometry.segment(a,b,1.8,3,false,1);
        assertEquals(full.firstStart(),half.firstStart());
        assertEquals(1,half.firstEnd().x);
    }
    @Test void spiralStrandsStayOppositeAroundBodyCenter(){
        var segment=TrailGeometry.segment(a,b,1.8,2.5,true,1);
        var mean=segment.firstStart().add(segment.secondStart()).scale(.5);
        assertEquals(0,mean.x,1e-9);assertEquals(.9,mean.y,1e-9);assertEquals(0,mean.z,1e-9);
    }
}
