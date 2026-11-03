package dev.sqwat.vela.opt.mixin.particle;

import dev.sqwat.vela.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleColumnUpParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.ref.WeakReference;

@Mixin(BubbleColumnUpParticle.class)
public class BubbleColumnUpParticleMixin {
	@Unique
	private static final Long2ObjectOpenHashMap<FluidState> velaopt$fluids = new Long2ObjectOpenHashMap<>();
	@Unique
	private static long velaopt$tick = Long.MIN_VALUE;
	@Unique
	private static WeakReference<ClientLevel> velaopt$level = new WeakReference<>(null);

	// thousands of bubbles share a few columns; each checked its block's fluid every tick
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	private FluidState velaopt$cachedFluid(ClientLevel level, BlockPos pos, Operation<FluidState> original) {
		if (!Features.MICRO_OPTS.on) {
			return original.call(level, pos);
		}
		long tick = level.getGameTime();
		if (tick != velaopt$tick || level != velaopt$level.get()) {
			velaopt$fluids.clear();
			velaopt$tick = tick;
			velaopt$level = new WeakReference<>(level);
		}
		long key = pos.asLong();
		FluidState fluid = velaopt$fluids.get(key);
		if (fluid == null) {
			fluid = original.call(level, pos);
			velaopt$fluids.put(key, fluid);
		}
		return fluid;
	}
}
