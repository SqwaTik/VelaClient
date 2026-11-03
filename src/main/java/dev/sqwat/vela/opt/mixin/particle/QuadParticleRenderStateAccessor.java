package dev.sqwat.vela.opt.mixin.particle;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(QuadParticleRenderState.class)
public interface QuadParticleRenderStateAccessor {
	@Accessor("particles")
	Map<SingleQuadParticle.Layer, Object> velaopt$particles();

	@Accessor("particleCount")
	int velaopt$particleCount();

	@Accessor("particleCount")
	void velaopt$setParticleCount(int count);
}
