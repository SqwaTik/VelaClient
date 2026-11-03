package dev.sqwat.vela.mixin.perf;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The vanilla title screen no longer polls Realms for news, invites and notifications (the subscriptions start in its
 *  tick, which this skips); the Realms button still works. */
@Mixin(TitleScreen.class)
abstract class RealmsNotificationsOffMixin {
    @Inject(method = "realmsNotificationsEnabled", at = @At("HEAD"), cancellable = true)
    private void vela$off(CallbackInfoReturnable<Boolean> ci) { ci.setReturnValue(false); }
}
