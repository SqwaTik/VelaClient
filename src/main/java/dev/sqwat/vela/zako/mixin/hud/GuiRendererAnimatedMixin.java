// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.hud;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.zako.hud.GuiItemRefresh;

@Mixin(GuiRenderer.class)
public class GuiRendererAnimatedMixin {
	@Inject(method = "prepareItemElements", at = @At("HEAD"))
	private void zakoopt$decideRefresh(CallbackInfo ci) {
		GuiItemRefresh.decide();
	}
}
