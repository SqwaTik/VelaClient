// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.entity;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.zako.entity.PosePool;

@Mixin(Minecraft.class)
public class PosePoolFrameMixin {
	@Inject(method = "renderFrame", at = @At("HEAD"))
	private void zakoopt$resetPosePool(boolean advanceGameTime, CallbackInfo ci) {
		PosePool.reset();
	}
}
