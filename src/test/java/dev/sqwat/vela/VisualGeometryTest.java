package dev.sqwat.vela;

import dev.sqwat.vela.render.HandOriginGuide;
import dev.sqwat.vela.render.NeonGeometry;
import dev.sqwat.vela.render.TrailHistory;
import net.minecraft.gizmos.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class VisualGeometryTest {
    @Test void neonHasBoundedLayersAndUpwardFaces(){
        var shapes=new ArrayList<Gizmo>();
        try(var collector=Gizmos.withCollector(shape->{shapes.add(shape);return GizmoCollector.IGNORED;})){
            NeonGeometry.ring(Vec3.ZERO,1,.09,0xFF3355,1);
        }
        assertEquals(NeonGeometry.SEGMENTS*5,shapes.size());
        for(var shape:shapes){
            var rect=(RectGizmo)shape;
            assertTrue(rect.b().subtract(rect.a()).cross(rect.c().subtract(rect.a())).y>0);
            assertTrue((rect.style().fill()>>>24)>0);
        }
        assertTrue((((RectGizmo)shapes.getFirst()).style().fill()>>>24)<40);
        assertEquals(255,((RectGizmo)shapes.getLast()).style().fill()>>>24);
    }
    @Test void originalGuidesAreMirroredAndStayInsideViewport(){
        for(int width:new int[]{610,1280,1920}){
            int height=720;
            var projection=new Matrix4f().perspective((float)Math.toRadians(70),(float)width/height,.05f,100);
            var right=HandOriginGuide.bounds(projection,true,width,height);
            var left=HandOriginGuide.bounds(projection,false,width,height);
            assertEquals(width-right.x()-right.width(),left.x(),1);
            assertEquals(left.y(),right.y());assertEquals(left.height(),right.height());
            assertTrue(right.width()>0&&right.height()>0);
            assertTrue(right.y()+right.height()<=height&&left.x()>=0);
        }
    }
    @Test void trailExpiresDoesNotBridgeTeleportsAndIsBounded(){
        var history=new TrailHistory();
        for(int i=0;i<100;i++)history.update(new Vec3(i*.1,0,0),i,1000);
        assertEquals(TrailHistory.LIMIT,history.snapshot().size());
        var snapshot=history.snapshot();history.update(new Vec3(100,0,0),101,1000);
        assertEquals(1,history.snapshot().size());assertEquals(TrailHistory.LIMIT,snapshot.size());
        history.update(new Vec3(100,0,0),2000,1000);assertEquals(1,history.snapshot().size());
        assertEquals(2000,history.snapshot().getFirst().born());history.clear();assertTrue(history.snapshot().isEmpty());
    }
    @Test void stationaryTrailDoesNotAccumulate(){
        var history=new TrailHistory();for(int i=0;i<100;i++)history.update(Vec3.ZERO,i,1000);
        assertEquals(1,history.snapshot().size());
        history.expire(2000,1000);assertTrue(history.snapshot().isEmpty());
    }
    @Test void wavePipelineDoesNotCullGroundFacingMeshes()throws Exception{
        var field=dev.sqwat.vela.ui.render.GlassSurface.class.getDeclaredField("WAVE");field.setAccessible(true);
        var pipeline=(com.mojang.blaze3d.pipeline.RenderPipeline)field.get(null);
        assertFalse(pipeline.isCull());
    }
    @Test void newVisualsAreOptInAndHaveUniqueSettings(){
        for(var module:java.util.List.of(new dev.sqwat.vela.module.ChinaHat(),new dev.sqwat.vela.module.DashTrail())){
            assertFalse(module.enabled());assertEquals(dev.sqwat.vela.module.ModuleCategory.VISUALS,module.category());
            assertEquals(module.settings().size(),module.settings().stream().map(s->s.id()).distinct().count());
        }
    }
}
