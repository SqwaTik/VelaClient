package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.render.shader.WorldShaderRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(GameRenderer.class)
abstract class ShaderProjectionMixin {
    // Capture the actual projection, including hurt/bobbing/portal transforms.
    @ModifyArg(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),index=0)
    private Matrix4f vela$projection(Matrix4f matrix){WorldShaderRenderer.captureProjection(matrix);dev.sqwat.vela.render.shader.SkyShaderRenderer.captureProjection(matrix);dev.sqwat.vela.render.effect.HitBubbleRenderer.captureProjection(matrix);return matrix;}
}
