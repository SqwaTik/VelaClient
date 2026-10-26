package dev.sqwat.vela.mixin.world;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ScreenEffectRenderer.class)
public abstract class RenderFireMixin {
    @Inject(method="submitFire",at=@At("HEAD"),cancellable=true)
    private static void vela$fire(CallbackInfo ci){if(dev.sqwat.vela.module.utility.RenderTweaks.hide("fire"))ci.cancel();}
}
