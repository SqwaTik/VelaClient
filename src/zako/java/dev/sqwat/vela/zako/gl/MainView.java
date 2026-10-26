// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class MainView {
	public RenderTarget target() {
		return Minecraft.getInstance().gameRenderer.mainRenderTarget();
	}

	public Vec3 camera() {
		return Minecraft.getInstance().gameRenderer.mainCamera().position();
	}
}
