package dev.sqwat.vela.mixin.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Starts Swing Animation's timeline on every swing of the local player's main hand. */
@Mixin(LocalPlayer.class)
abstract class SwingStartMixin {
    @Inject(method="swing(Lnet/minecraft/world/InteractionHand;)V",at=@At("HEAD"))
    private void vela$swingStarted(InteractionHand hand,CallbackInfo ci){
        if(hand!=InteractionHand.MAIN_HAND)return;
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||!(services.modules().byId("swing_animation") instanceof dev.sqwat.vela.module.visual.SwingAnimation module)||!module.enabled())return;
        var player=(LocalPlayer)(Object)this;
        float partial=Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        module.swingStarted(player.tickCount+partial,dev.sqwat.vela.module.visual.SwingAnimation.swingTicks(player));
    }
}
