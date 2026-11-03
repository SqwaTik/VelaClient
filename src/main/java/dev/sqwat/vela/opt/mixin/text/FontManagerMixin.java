package dev.sqwat.vela.opt.mixin.text;

import net.minecraft.client.Options;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.text.PreparedTextCache;

@Mixin(FontManager.class)
public class FontManagerMixin {
	@Inject(method = "apply", at = @At("TAIL"))
	private void velaopt$clearOnReload(CallbackInfo ci) {
		PreparedTextCache.clear();
	}

	@Inject(method = "updateOptions", at = @At("TAIL"))
	private void velaopt$clearOnOptions(Options options, CallbackInfo ci) {
		PreparedTextCache.clear();
	}
}
