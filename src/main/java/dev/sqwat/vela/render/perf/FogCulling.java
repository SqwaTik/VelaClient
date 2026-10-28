package dev.sqwat.vela.render.perf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.fog.FogData;
import java.util.List;

/**
 * Skips terrain sections that the fog hides completely. Environmental fog (the Nether, lava, powder snow, blindness,
 * boss fog) reaches full strength at {@code environmentalEnd}; a section whose nearest point is further away than that
 * is drawn in pure fog colour, so not drawing it changes nothing on screen. In the Nether with a 12-chunk render
 * distance the fog ends around 6 chunks out, so most sections in view are skipped. Same idea as Sodium's fog occlusion.
 */
public final class FogCulling {
    /** A little beyond the fog end: rounding of the fog curve, and the camera moving between frustum updates. */
    private static final float MARGIN=4;
    public static volatile boolean enabled=true;
    private static float distance=Float.POSITIVE_INFINITY,applied=Float.POSITIVE_INFINITY;
    public static int lastCulled;
    /** Last fog distances seen, for diagnostics. */
    public static volatile float lastEnvironmentalEnd,lastRenderDistanceEnd;
    private FogCulling(){}

    /** After the fog of this frame is known (FogRenderer.setupFog, including Vela's fog tweaks). */
    public static void fog(FogData fog){
        lastEnvironmentalEnd=fog.environmentalEnd;lastRenderDistanceEnd=fog.renderDistanceEnd;
        var options=dev.sqwat.vela.module.Optimization.current();
        boolean on=enabled&&(options==null||options.fogCulling());
        float next=on&&fog.environmentalEnd<fog.renderDistanceEnd-16?fog.environmentalEnd+MARGIN:Float.POSITIVE_INFINITY;
        distance=next;
        // Fog moved by more than a few blocks (lava, blindness, leaving the Nether): rebuild the section list.
        boolean changed=Float.isInfinite(next)!=Float.isInfinite(applied)||!Float.isInfinite(next)&&Math.abs(next-applied)>8;
        if(changed){
            applied=next;
            var renderer=Minecraft.getInstance().levelRenderer;
            if(renderer!=null)renderer.sectionOcclusionGraph().invalidate();
        }
    }
    /** Removes sections fully inside the fog from the lists the frustum pass just filled. */
    public static void filter(List<SectionRenderDispatcher.RenderSection> visible,List<SectionRenderDispatcher.RenderSection> nearby){
        float limit=applied;
        if(Float.isInfinite(limit)){lastCulled=0;return;}
        var camera=Minecraft.getInstance().gameRenderer.mainCamera().position();
        double cx=camera.x,cy=camera.y,cz=camera.z,limitSq=(double)limit*limit;
        int before=visible.size();
        visible.removeIf(section->{
            var origin=section.getRenderOrigin();
            double dx=Math.max(0,Math.max(origin.getX()-cx,cx-(origin.getX()+16)));
            double dy=Math.max(0,Math.max(origin.getY()-cy,cy-(origin.getY()+16)));
            double dz=Math.max(0,Math.max(origin.getZ()-cz,cz-(origin.getZ()+16)));
            return dx*dx+dy*dy+dz*dz>limitSq;
        });
        lastCulled=before-visible.size();
    }
}
