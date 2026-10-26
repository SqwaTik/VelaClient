package dev.sqwat.vela.mixin;

import dev.sqwat.vela.module.AspectRatio;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
abstract class AspectRatioMixin {
    @ModifyArg(method="createProjectionMatrixForCulling", at=@At(value="INVOKE", target="Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"), index=1)
    private float vela$cullingAspect(float original) { return AspectRatio.apply(original); }
    @ModifyArgs(method="setupPerspective", at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V"))
    private void vela$cameraAspect(Args args) {
        float width=args.get(3), height=args.get(4);
        if (height > 0) args.set(3, height * AspectRatio.apply(width/height));
    }
}
