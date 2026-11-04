package dev.sqwat.vela.mixin.entity;

import dev.sqwat.vela.module.visual.SelfTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.5: PlayerRenderer has no shouldShowName of its own, so the living renderer's one is hooked for players. */
@Mixin(LivingEntityRenderer.class)
abstract class SelfTagMixin {
    @Inject(method="shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",at=@At("HEAD"),cancellable=true)
    private void vela$selfTag(LivingEntity entity,double distance,CallbackInfoReturnable<Boolean> ci){
        if(!(entity instanceof Player))return;
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&services.modules().byId("self_tag") instanceof SelfTag tag
            &&tag.visible(entity==mc.player,mc.options.getCameraType().isFirstPerson(),mc.options.hideGui||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc)||entity.isInvisible(),entity.isAlive()))ci.setReturnValue(true);
    }
}
