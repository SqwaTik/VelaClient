package dev.sqwat.vela.mixin.perf;
import dev.sqwat.vela.render.perf.HudCache;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** HUD cache: the crosshair inverts the world behind it and camera overlays tint it, so they stay out of the texture. */
@Mixin(Hud.class)
abstract class HudLivePartsMixin {
    @Inject(method={"extractCameraOverlays","extractCrosshair"},at=@At("HEAD"),cancellable=true)
    private void vela$live(CallbackInfo ci){if(HudCache.cachedPass())ci.cancel();}
}
