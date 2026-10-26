// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.gl;

import dev.sqwat.vela.zako.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class GlCommandEncoderClearMixin {
	// the next render pass binds its own target anyway; GlStateManager's cache then skips the redundant bind
	@WrapOperation(method = "clearColorAndDepthTextures", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_glBindFramebuffer(II)V"))
	private void zakoopt$keepBound(int target, int framebuffer, Operation<Void> original) {
		if (framebuffer != 0 || !Features.LAZY_CLEAR.on) {
			original.call(target, framebuffer);
		}
	}
}
