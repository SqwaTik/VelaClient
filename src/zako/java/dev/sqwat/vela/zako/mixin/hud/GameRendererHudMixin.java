// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.zako.hud.HudCache;

@Mixin(GameRenderer.class)
public class GameRendererHudMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	// refresh frames: draw the extracted HUD into the texture; every frame: live parts on screen, then the cached HUD over them
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
	private void zakoopt$drawHud(GuiRenderer renderer, Operation<Void> original, DeltaTracker deltaTracker, boolean advanceGameTime) {
		if (!HudCache.active()) {
			original.call(renderer);
			return;
		}
		if (!HudCache.skipExtraction()) {
			// Never capture Vela's glass/history from the redirected transparent HUD target.
			HudCache.additionalPass = true;
			try { HudCache.drawIntoTexture(() -> original.call(renderer)); }
			finally { HudCache.additionalPass = false; }
		}
		if (!minecraft.gui.hud.isHidden()) {
			GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(minecraft, ((GameRenderer) (Object) this).gameRenderState().guiRenderState, 0, 0);
			dev.sqwat.vela.render.shader.TemporalBlur.draw(graphics);
			HudInvoker hud = (HudInvoker) minecraft.gui.hud;
			hud.zakoopt$extractCameraOverlays(graphics, deltaTracker);
			hud.zakoopt$extractCrosshair(graphics, deltaTracker);
			HudCache.extractComposite(graphics, ((GameRenderer) (Object) this).gameRenderState().guiRenderState);
			dev.sqwat.vela.VelaClient.extractLiveHud(graphics, deltaTracker);
			HudCache.additionalPass = false;
			try { original.call(renderer); } finally { HudCache.additionalPass = false; }
		}
	}
}
