package dev.sqwat.vela.opt.mixin.particle;

import dev.sqwat.vela.opt.feature.Features;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.BubbleColumnUpParticle;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.sqwat.vela.opt.particle.CameraPosition;

@Mixin(Particle.class)
public class ParticleMixin {
	@Shadow
	@Final
	protected ClientLevel level;

	@Shadow
	protected boolean hasPhysics;
	@Shadow
	protected double x;
	@Shadow
	protected double y;
	@Shadow
	protected double z;

	@Unique
	private boolean velaopt$physicsBefore;
	@Unique
	private long velaopt$lightTick = Long.MIN_VALUE;
	@Unique
	// kept boxed: the value is far outside Integer's cache, and setReturnValue would box it anew on every hit
	private Integer velaopt$light;

	// vanilla reads the non-interpolated x/y/z, which only change in tick(), so the value is constant within a tick
	@Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
	private void velaopt$cachedLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
		if (velaopt$lightTick == level.getGameTime() && Features.PARTICLE_LIGHT.on) {
			cir.setReturnValue(velaopt$light);
		}
	}

	@Inject(method = "getLightCoords", at = @At("RETURN"))
	private void velaopt$storeLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
		velaopt$light = cir.getReturnValue();
		velaopt$lightTick = level.getGameTime();
	}

	@Inject(method = "move", at = @At("HEAD"))
	private void velaopt$cheapPhysics(double dx, double dy, double dz, CallbackInfo ci) {
		velaopt$physicsBefore = hasPhysics;
		if (hasPhysics && Features.PARTICLE_PHYSICS.on && velaopt$skipCollision()) {
			hasPhysics = false;
		}
	}

	@Inject(method = "move", at = @At("RETURN"))
	private void velaopt$restorePhysics(double dx, double dy, double dz, CallbackInfo ci) {
		hasPhysics = velaopt$physicsBefore;
	}

	@Unique
	private boolean velaopt$skipCollision() {
		Object self = this;
		if (self instanceof BaseAshSmokeParticle || self instanceof CampfireSmokeParticle || self instanceof BubbleColumnUpParticle) {
			return true;
		}
		Vec3 cam = CameraPosition.get();
		double ddx = x - cam.x, ddy = y - cam.y, ddz = z - cam.z;
		return ddx * ddx + ddy * ddy + ddz * ddz > 16 * 16;
	}
}
