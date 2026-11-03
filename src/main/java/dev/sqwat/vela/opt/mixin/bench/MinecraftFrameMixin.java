package dev.sqwat.vela.opt.mixin.bench;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.bench.Bench;
import dev.sqwat.vela.opt.bench.MonitorScreen;

@Mixin(Minecraft.class)
public class MinecraftFrameMixin {
	@Inject(method = "runTick", at = @At("HEAD"))
	private void velaopt$frame(boolean advanceGameTime, CallbackInfo ci) {
		Bench.frame(System.nanoTime());
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void velaopt$monitorKey(CallbackInfo ci) {
		MonitorScreen.pollKey();
	}
}
