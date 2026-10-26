// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.zako.hud.HudCache;
import dev.sqwat.vela.zako.hud.HudDelta;

@Mixin(DeltaTracker.Timer.class)
public class DeltaTrackerHudMixin {
	// while the cached HUD is drawn, a frame's delta is the time since its previous refresh
	@ModifyReturnValue(method = "getGameTimeDeltaTicks", at = @At("RETURN"))
	private float zakoopt$hudDelta(float delta) {
		return HudCache.suppressLiveParts ? HudDelta.sinceRefresh() : delta;
	}
}
