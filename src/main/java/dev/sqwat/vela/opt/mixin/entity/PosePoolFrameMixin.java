package dev.sqwat.vela.opt.mixin.entity;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.entity.PosePool;

@Mixin(Minecraft.class)
public class PosePoolFrameMixin {
	@Inject(method = "runTick", at = @At("HEAD"))
	private void velaopt$resetPosePool(boolean advanceGameTime, CallbackInfo ci) {
		PosePool.reset();
	}
}
