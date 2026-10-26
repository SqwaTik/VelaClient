// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.entity;

import dev.sqwat.vela.zako.entity.PoolScope;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Vela: marks the only extraction allowed to reuse pooled entity render states.
@Mixin(LevelExtractor.class)
public class StatePoolScopeMixin {
	@Inject(method = "extractVisibleEntities", at = @At("HEAD"))
	private void zakoopt$poolBegin(CallbackInfo ci) {
		PoolScope.level(true);
	}

	@Inject(method = "extractVisibleEntities", at = @At("RETURN"))
	private void zakoopt$poolEnd(CallbackInfo ci) {
		PoolScope.level(false);
	}
}
