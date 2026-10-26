// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.particle;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(QuadParticleRenderState.class)
public interface QuadParticleRenderStateAccessor {
	@Accessor("particles")
	Map<SingleQuadParticle.Layer, Object> zakoopt$particles();

	@Accessor("particleCount")
	int zakoopt$particleCount();

	@Accessor("particleCount")
	void zakoopt$setParticleCount(int count);
}
