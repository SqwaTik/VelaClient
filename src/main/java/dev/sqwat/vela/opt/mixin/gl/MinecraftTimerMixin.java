package dev.sqwat.vela.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.TimerQuery;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.bench.ProgressScreen;
import dev.sqwat.vela.opt.gl.GpuTimers;

// the frame's own elapsed-time query would overlap ours, and two of them cannot run at once; F3's GPU share comes from ours
@Mixin(Minecraft.class)
public class MinecraftTimerMixin {
	@Shadow
	private double gpuUtilization;

	@WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;beginProfile()V"))
	private void velaopt$ownTimers(TimerQuery query, Operation<Void> original) {
		if (!GpuTimers.supported()) {
			original.call(query);
		}
	}

	// vanilla ends the profile it began under the same condition: with no begin, its end would throw; 26.1.2 ends it with a
	// return value and has its own mixin for that
	@WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;endProfile()V"), require = 0)
	private void velaopt$ownTimersEnd(TimerQuery query, Operation<Void> original) {
		if (!GpuTimers.supported()) {
			original.call(query);
		}
	}

	@Inject(method = "renderFrame", at = @At("HEAD"))
	private void velaopt$utilization(CallbackInfo ci) {
		if (GpuTimers.supported()) {
			gpuUtilization = GpuTimers.utilization();
		} else if (ProgressScreen.open()) {
			GpuTimers.vanilla(Math.min(100, gpuUtilization));
		}
	}

	// the tuning panel shows the GPU share; with no GL context (Vulkan) only vanilla's query can measure it
	@ModifyExpressionValue(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntryList;isCurrentlyEnabled(Lnet/minecraft/resources/Identifier;)Z"))
	private boolean velaopt$benchGpu(boolean enabled) {
		return enabled || ProgressScreen.open();
	}
}
