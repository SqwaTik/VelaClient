// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.particle;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class CameraPosition {
	public Vec3 get() {
		return Minecraft.getInstance().gameRenderer.mainCamera().position();
	}
}
