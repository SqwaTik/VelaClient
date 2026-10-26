// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.gl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.zako.gl.LunarMultiDraw;

// 26.x: Sodium draws terrain through the vanilla encoder, where Lunar keeps its flag
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class LunarMultiDrawMixin {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void zakoopt$keepMultiDraw(CallbackInfo ci) {
		LunarMultiDraw.keep(this);
	}
}
