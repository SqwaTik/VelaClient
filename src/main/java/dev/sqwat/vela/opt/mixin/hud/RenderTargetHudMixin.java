package dev.sqwat.vela.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;
import dev.sqwat.vela.opt.hud.HudCache;

@Mixin(RenderTarget.class)
public class RenderTargetHudMixin {
	@ModifyReturnValue(method = "getColorTexture", at = @At("RETURN"))
	private GpuTexture velaopt$hudColor(GpuTexture texture) {
		RenderTarget hud = HudCache.redirect((RenderTarget) (Object) this);
		return hud != null ? hud.getColorTexture() : texture;
	}

	@ModifyReturnValue(method = "getColorTextureView", at = @At("RETURN"))
	private GpuTextureView velaopt$hudColorView(GpuTextureView view) {
		RenderTarget hud = HudCache.redirect((RenderTarget) (Object) this);
		return hud != null ? hud.getColorTextureView() : view;
	}

	@ModifyReturnValue(method = "getDepthTexture", at = @At("RETURN"))
	private GpuTexture velaopt$hudDepth(GpuTexture texture) {
		RenderTarget hud = HudCache.redirect((RenderTarget) (Object) this);
		return hud != null ? hud.getDepthTexture() : texture;
	}

	@ModifyReturnValue(method = "getDepthTextureView", at = @At("RETURN"))
	private GpuTextureView velaopt$hudDepthView(GpuTextureView view) {
		RenderTarget hud = HudCache.redirect((RenderTarget) (Object) this);
		return hud != null ? hud.getDepthTextureView() : view;
	}
}
