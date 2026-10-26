// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.gl;
import dev.sqwat.vela.zako.ZakoOptConfig;

import dev.sqwat.vela.zako.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.zako.gl.TexBufferCache;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class GlCommandEncoderTexBufferMixin {
	@org.spongepowered.asm.mixin.injection.Inject(method = "createRenderPass", at = @At("HEAD"))
	private void zakoopt$beginPass(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<com.mojang.blaze3d.systems.RenderPass> cir) {
		TexBufferCache.forget();
	}
	@WrapOperation(method = "trySetup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glBindTexture(II)V"))
	private void zakoopt$trackTbo(int target, int texture, Operation<Void> original) {
		if (ZakoOptConfig.tboCache() && target == org.lwjgl.opengl.GL31C.GL_TEXTURE_BUFFER) TexBufferCache.boundTexture = texture;
		original.call(target, texture);
	}

	// re-attaching the same buffer every draw makes the driver revalidate the texture (clouds: ~2% of a frame in glDrawElements)
	@WrapOperation(method = "trySetup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glTexBuffer(III)V"))
	private void zakoopt$texBufferOnce(int target, int format, int buffer, Operation<Void> original) {
		if (Features.MICRO_OPTS.texBufferOnce && TexBufferCache.attached(format, buffer)) {
			return;
		}
		original.call(target, format, buffer);
		if (ZakoOptConfig.tboCache()) TexBufferCache.attach(format, buffer);
	}
}
