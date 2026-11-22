package dev.sqwat.vela.mixin.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.render.view.ViewBob;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the level camera's walking bob for {@link ViewBob}. */
@Mixin(GameRenderer.class)
abstract class ViewBobCaptureMixin {
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void vela$resetBob(CallbackInfo ci){ViewBob.reset();}

    @WrapOperation(method="renderLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private void vela$captureBob(GameRenderer renderer,PoseStack pose,float partial,Operation<Void> original){
        var before=new Matrix4f(pose.last().pose());
        original.call(renderer,pose,partial);
        var view=new Matrix4f().rotation(renderer.getMainCamera().rotation().conjugate(new Quaternionf()));
        ViewBob.capture(before.invertAffine().mul(pose.last().pose()),view);
    }
}
