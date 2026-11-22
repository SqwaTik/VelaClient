package dev.sqwat.vela.opt.hud;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

// the HUD cache's GPU side on 1.21.5. Its mixins wrap 1.21.9+ HUD extraction and are not built here, so the cache never
// turns on (HudCacheFeature shows as unavailable) and nothing composites
@UtilityClass
public class HudGpu {
	public boolean noScreen(Minecraft mc) {
		return mc.screen == null && mc.getOverlay() == null;
	}

	public RenderTarget mainTarget(Minecraft mc) {
		return mc.getMainRenderTarget();
	}

	public TextureTarget createTarget(int w, int h) {
		return new TextureTarget("velaopt hud", w, h, true);
	}

	public void clear(TextureTarget target) {
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
	}

	public void composite(TextureTarget hud, RenderTarget main) {}
}
