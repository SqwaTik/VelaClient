// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.particle;

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
import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.particle.CameraPosition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

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
	private long zakoopt$lightTick = Long.MIN_VALUE;
	@Unique
	private Integer zakoopt$light;
	@Unique private double zakoopt$lightX, zakoopt$lightY, zakoopt$lightZ;

	// vanilla reads the non-interpolated x/y/z, which only change in tick(), so the value is constant within a tick
	@Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cachedLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
		if (ZakoOptConfig.particleLight() && zakoopt$lightTick == level.getGameTime() && x == zakoopt$lightX && y == zakoopt$lightY && z == zakoopt$lightZ) {
			cir.setReturnValue(zakoopt$light);
		}
	}

	@Inject(method = "getLightCoords", at = @At("RETURN"))
	private void zakoopt$storeLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
		if (!ZakoOptConfig.particleLight()) return;
		zakoopt$light = cir.getReturnValue();
		zakoopt$lightTick = level.getGameTime();
		zakoopt$lightX=x; zakoopt$lightY=y; zakoopt$lightZ=z;
	}

	@WrapMethod(method = "move")
	private void zakoopt$cheapPhysics(double dx, double dy, double dz, Operation<Void> original) {
		boolean before = hasPhysics;
		if (hasPhysics && ZakoOptConfig.particlePhysics() && zakoopt$skipCollision()) {
			hasPhysics = false;
		}
		try { original.call(dx, dy, dz); } finally { hasPhysics = before; }
	}

	@Unique
	private boolean zakoopt$skipCollision() {
		Object self = this;
		if (self instanceof BaseAshSmokeParticle || self instanceof CampfireSmokeParticle || self instanceof BubbleColumnUpParticle) {
			return true;
		}
		Vec3 cam = CameraPosition.get();
		double ddx = x - cam.x, ddy = y - cam.y, ddz = z - cam.z;
		return ddx * ddx + ddy * ddy + ddz * ddz > 16 * 16;
	}
}
