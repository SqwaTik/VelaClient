// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Hud.class)
public interface HudInvoker {
	@Invoker("extractCameraOverlays")
	void zakoopt$extractCameraOverlays(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

	@Invoker("extractCrosshair")
	void zakoopt$extractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
}
