package dev.sqwat.vela.mixin.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.sqwat.vela.compat.RenderOutput;
import dev.sqwat.vela.render.shader.HandShaderRenderer;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3 version: vanilla opens the "Item in hand" pass on the main target before drawing, and render outputs can no
 * longer be overridden. The pass is closed unused (closing is idempotent) and reopened on Vela's hand targets.
 */
@Mixin(GameRenderer.class)
abstract class HandShaderMixin {
    @WrapOperation(method = "renderItemInHand", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lcom/mojang/renderpearl/api/commands/RenderPass;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;)V"))
    private void vela$hands(RenderPass pass, FeatureRenderDispatcher.PreparedFrame frame, Operation<Void> original,
                            @Local(argsOnly = true) GpuTextureView depthTextureView) {
        var self = (GameRenderer) (Object) this;
        pass.close();
        HandShaderRenderer.render(() -> {
            var color = RenderOutput.color != null ? RenderOutput.color : self.mainRenderTarget().getColorTextureView();
            var depth = RenderOutput.depth != null ? RenderOutput.depth : depthTextureView;
            try (var hands = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "Item in hand", color, Optional.empty(), depth, OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(hands);
                original.call(hands, frame);
            }
        });
    }
}
