package dev.sqwat.vela.opt.mixin.gl;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.OptConfig;
import dev.sqwat.vela.opt.block.FarLeaves;
import dev.sqwat.vela.opt.entity.AnimFreeze;
import dev.sqwat.vela.opt.gl.FrameFence;
import dev.sqwat.vela.opt.entity.SkinAtlas;

@Mixin(Minecraft.class)
public class MinecraftFrameMixin {
	@Inject(method = "renderFrame", at = @At("HEAD"))
	private void velaopt$frame(boolean advanceGameTime, CallbackInfo ci) {
		FrameFence.endFrame();
		FarLeaves.frame();
		SkinAtlas.endFrame();
		AnimFreeze.frame++;
		OptConfig.refresh();
	}
}
