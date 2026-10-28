package dev.sqwat.vela.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Render Tweaks → hand bob: the walking bob is not applied to the hands (the camera keeps its own bob). */
@Mixin(GameRenderer.class)
abstract class SteadyHandsMixin {
    @WrapOperation(method="renderItemInHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void vela$steadyHands(GameRenderer renderer,CameraRenderState camera,PoseStack pose,Operation<Void> original){
        if(!dev.sqwat.vela.module.utility.RenderTweaks.hide("hand_bob"))original.call(renderer,camera,pose);
    }
}
