package dev.sqwat.vela.mixin.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.3: swing lives on LivingEntity only; starts Swing Animation's timeline on every main-hand swing of the local player. */
@Mixin(LivingEntity.class)
abstract class SwingStartMixin {
    @Inject(method="swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z",at=@At("HEAD"))
    private void vela$swingStarted(InteractionHand hand,SwingAnimation animation,boolean sync,CallbackInfoReturnable<Boolean> cir){
        if(hand!=InteractionHand.MAIN_HAND||!((Object)this instanceof LocalPlayer player))return;
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||!(services.modules().byId("swing_animation") instanceof dev.sqwat.vela.module.visual.SwingAnimation module)||!module.enabled())return;
        float partial=Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        module.swingStarted(player.tickCount+partial,dev.sqwat.vela.module.visual.SwingAnimation.swingTicks(player));
    }
}
