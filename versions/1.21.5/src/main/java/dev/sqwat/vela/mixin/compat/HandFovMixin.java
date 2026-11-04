package dev.sqwat.vela.mixin.compat;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The hand's field of view, for {@link dev.sqwat.vela.compat.LegacyCamera#handProjection}; 1.21.5 builds it in renderItemInHand. */
@Mixin(GameRenderer.class)
abstract class HandFovMixin {
    @ModifyArg(method="renderItemInHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(F)Lorg/joml/Matrix4f;"),index=0)
    private float vela$handFov(float fov){dev.sqwat.vela.compat.LegacyCamera.captureHandFov(fov);return fov;}
}
