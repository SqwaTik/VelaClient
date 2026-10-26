// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.render.GuiItemAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.zako.hud.GuiItemRefresh;

@Mixin(GuiItemAtlas.class)
public class GuiItemAtlasMixin {
	// an animated item's slot is freed after every frame and redrawn; reported static in between, it is redrawn once per monitor refresh
	@ModifyExpressionValue(method = "getOrUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;isAnimated()Z"))
	private boolean zakoopt$throttleAnimated(boolean animated) {
		return animated && GuiItemRefresh.refresh();
	}
}
