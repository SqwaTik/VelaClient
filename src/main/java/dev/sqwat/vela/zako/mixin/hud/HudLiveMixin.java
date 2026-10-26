// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.hud;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.zako.hud.HudCache;

@Mixin(Hud.class)
public class HudLiveMixin {
	// these blend with the world behind them, so they are drawn live every frame instead of into the HUD texture
	@Inject(method = {"extractCameraOverlays", "extractCrosshair"}, at = @At("HEAD"), cancellable = true)
	private void zakoopt$live(CallbackInfo ci) {
		if (HudCache.suppressLiveParts) {
			ci.cancel();
		}
	}
}
