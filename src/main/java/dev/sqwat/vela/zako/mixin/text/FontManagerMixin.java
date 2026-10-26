// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.text;

import net.minecraft.client.Options;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.zako.text.PreparedTextCache;

@Mixin(FontManager.class)
public class FontManagerMixin {
	@Inject(method = "apply", at = @At("TAIL"))
	private void zakoopt$clearOnReload(CallbackInfo ci) {
		dev.sqwat.vela.zako.IntegrationRuntime.clear();
		PreparedTextCache.clear();
	}

	@Inject(method = "updateOptions", at = @At("TAIL"))
	private void zakoopt$clearOnOptions(Options options, CallbackInfo ci) {
		PreparedTextCache.clear();
	}
}
