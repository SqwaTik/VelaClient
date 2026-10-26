// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.text;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.sqwat.vela.zako.text.PreparedTextCache;

@Mixin(Font.class)
public class FontMixin {
	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cached(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background,
								CallbackInfoReturnable<Font.PreparedText> cir) {
		if (PreparedTextCache.enabled() && PreparedTextCache.isStable(text)) {
			Font.PreparedText cached = PreparedTextCache.get((Font)(Object)this, text, x, y, color, shadow, includeEmpty, background);
			if (cached != null) {
				cir.setReturnValue(cached);
			}
		}
	}

	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("RETURN"))
	private void zakoopt$store(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background,
							   CallbackInfoReturnable<Font.PreparedText> cir) {
		if (PreparedTextCache.enabled() && PreparedTextCache.isStable(text)) {
			PreparedTextCache.Key key = PreparedTextCache.key((Font)(Object)this, text, x, y, color, shadow, includeEmpty, background);
			if (!PreparedTextCache.known(key)) {
				PreparedTextCache.put(key, cir.getReturnValue());
			}
		}
	}
}
