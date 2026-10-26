package dev.sqwat.vela.mixin.hud;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** The game HUD is hidden during a Death Recap replay, as with F1. */
@Mixin(Hud.class)
abstract class DeathRecapHudMixin {
    @Inject(method="isHidden",at=@At("HEAD"),cancellable=true)
    private void vela$hide(CallbackInfoReturnable<Boolean> cir){if(DeathReplay.active())cir.setReturnValue(true);}
}
