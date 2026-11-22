package dev.sqwat.vela.mixin.camera;
import dev.sqwat.vela.module.utility.AspectRatio;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
/** 1.21.11: the world and culling projections both come from GameRenderer.getProjectionMatrix (26.x: Camera). */
@Mixin(GameRenderer.class)
abstract class AspectRatioMixin {
    @ModifyArg(method="getProjectionMatrix", at=@At(value="INVOKE", target="Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"), index=1)
    private float vela$aspect(float original) { return AspectRatio.apply(original); }
}
