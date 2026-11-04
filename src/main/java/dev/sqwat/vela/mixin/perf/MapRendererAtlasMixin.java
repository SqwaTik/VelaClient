package dev.sqwat.vela.mixin.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.render.perf.MapAtlas;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The map picture drawn from its {@link MapAtlas} page: the same quad, depth and light, with the slot's UVs. */
@Mixin(MapRenderer.class)
public abstract class MapRendererAtlasMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"))
    private void vela$fromAtlas(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer,
                                Operation<Void> original, MapRenderState state, PoseStack stack, SubmitNodeCollector submitNodeCollector, boolean showOnlyFrame, int lightCoords) {
        MapAtlas.Slot slot = MapAtlas.slot(state.texture);
        if (slot == null) {
            original.call(collector, poseStack, renderType, renderer);
            return;
        }
        float u0 = slot.u0(), v0 = slot.v0(), u1 = slot.u1(), v1 = slot.v1();
        collector.submitCustomGeometry(poseStack, slot.renderType(), (pose, buffer) -> {
            buffer.addVertex(pose, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(u0, v1).setLight(lightCoords);
            buffer.addVertex(pose, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(u1, v1).setLight(lightCoords);
            buffer.addVertex(pose, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(u1, v0).setLight(lightCoords);
            buffer.addVertex(pose, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(u0, v0).setLight(lightCoords);
        });
    }
}
