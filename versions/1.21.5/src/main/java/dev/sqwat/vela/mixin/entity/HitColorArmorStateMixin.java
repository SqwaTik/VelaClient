package dev.sqwat.vela.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sqwat.vela.compat.ArmorTint;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the armor layer's render state to {@link HitColorArmorMixin}. */
@Mixin(HumanoidArmorLayer.class)
abstract class HitColorArmorStateMixin {
    @Inject(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",at=@At("HEAD"))
    private void vela$begin(PoseStack pose,MultiBufferSource buffers,int light,HumanoidRenderState state,float yaw,float pitch,CallbackInfo ci){ArmorTint.begin(state);}
    @Inject(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",at=@At("RETURN"))
    private void vela$end(PoseStack pose,MultiBufferSource buffers,int light,HumanoidRenderState state,float yaw,float pitch,CallbackInfo ci){ArmorTint.end();}
}
