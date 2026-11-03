package dev.sqwat.vela.opt.mixin.gl;

import dev.sqwat.vela.opt.feature.Features;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.gl.SharedDepthFbos;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class GlCommandEncoderDepthClearMixin {
	// vanilla attaches the depth texture to a scratch FBO, clears, detaches and unbinds; a cached FBO already holds it
	@Inject(method = "clearDepthTexture", at = @At("HEAD"), cancellable = true)
	private void velaopt$clearInPlace(GpuTexture depth, double value, CallbackInfo ci) {
		if (Features.FBO_SHARE.on && depth instanceof GlTexture gl && !gl.isClosed() && SharedDepthFbos.clear(gl.glId(), value)) {
			ci.cancel();
		}
	}
}
