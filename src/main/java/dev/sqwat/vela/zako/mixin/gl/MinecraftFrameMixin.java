// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.gl;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.block.FarLeaves;
import dev.sqwat.vela.zako.entity.AnimFreeze;
import dev.sqwat.vela.zako.gl.FrameFence;
import dev.sqwat.vela.zako.entity.SkinAtlas;

@Mixin(Minecraft.class)
public class MinecraftFrameMixin {
	@Inject(method = "renderFrame", at = @At("HEAD"))
	private void zakoopt$frame(boolean advanceGameTime, CallbackInfo ci) {
		FrameFence.endFrame();
		FarLeaves.frame();
		SkinAtlas.endFrame();
		AnimFreeze.frame++;
		dev.sqwat.vela.zako.IntegrationRuntime.beginFrame();
		ZakoOptConfig.refresh();
		dev.sqwat.vela.zako.IntegrationRuntime.frame();
		dev.sqwat.vela.zako.gl.GpuTimers.selectFrame();
		dev.sqwat.vela.zako.Guard.tell();
	}
}
