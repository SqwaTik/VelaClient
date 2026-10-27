package dev.sqwat.vela.mixin.world;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

/** Fog culling: drops sections hidden by fog after vanilla's frustum pass (FogCulling). */
@Mixin(SectionOcclusionGraph.class)
abstract class FogCullingMixin {
    @Inject(method="addSectionsInFrustum",at=@At("TAIL"))
    private void vela$fogCulling(Frustum frustum,List<SectionRenderDispatcher.RenderSection> visible,List<SectionRenderDispatcher.RenderSection> nearby,CallbackInfo ci){
        dev.sqwat.vela.render.perf.FogCulling.filter(visible,nearby);
    }
}
