package dev.sqwat.vela.mixin.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.render.view.ViewBob;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the level camera's walking bob for {@link ViewBob}. */
@Mixin(GameRenderer.class)
abstract class ViewBobCaptureMixin {
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void vela$resetBob(CallbackInfo ci){ViewBob.reset();}

    @WrapOperation(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void vela$captureBob(GameRenderer renderer,CameraRenderState camera,PoseStack pose,Operation<Void> original){
        var before=new Matrix4f(pose.last().pose());
        original.call(renderer,camera,pose);
        ViewBob.capture(before.invertAffine().mul(pose.last().pose()),camera.viewRotationMatrix);
    }
}
