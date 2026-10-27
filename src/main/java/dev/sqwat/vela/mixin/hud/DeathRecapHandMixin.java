package dev.sqwat.vela.mixin.hud;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** No first-person hand during a Death Recap replay: the camera is never the local player's eyes. */
@Mixin(GameRenderer.class)
abstract class DeathRecapHandMixin {
    @Inject(method="renderItemInHand",at=@At("HEAD"),cancellable=true)
    private void vela$noHand(CallbackInfo ci){if(DeathReplay.active())ci.cancel();}
}
