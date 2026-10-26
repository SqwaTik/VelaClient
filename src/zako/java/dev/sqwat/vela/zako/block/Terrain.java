// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.block;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class Terrain {
	public void rebuild() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.level != null && mc.levelRenderer != null) {
			mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options, mc.gameRenderer.mainCamera(), mc.getBlockColors());
		}
	}
}
