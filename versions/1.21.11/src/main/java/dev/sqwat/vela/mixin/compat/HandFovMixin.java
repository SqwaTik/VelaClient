package dev.sqwat.vela.mixin.compat;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The hand's field of view, for {@link dev.sqwat.vela.compat.LegacyCamera#handProjection}. */
@Mixin(GameRenderer.class)
abstract class HandFovMixin {
    @ModifyArg(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/CachedPerspectiveProjectionMatrixBuffer;getBuffer(IIF)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),index=2)
    private float vela$handFov(float fov){dev.sqwat.vela.compat.LegacyCamera.captureHandFov(fov);return fov;}
}
