package dev.sqwat.vela.mixin.camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
abstract class HurtCameraMixin {
    // The same vanilla transform is used for both the world and first-person hands.
    @Inject(method="bobHurt",at=@At("HEAD"),cancellable=true)
    private void vela$noHurtShake(CallbackInfo ci){if(dev.sqwat.vela.module.utility.RenderTweaks.hide("hurt_camera"))ci.cancel();}
}
