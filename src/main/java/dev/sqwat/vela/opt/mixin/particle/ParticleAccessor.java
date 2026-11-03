package dev.sqwat.vela.opt.mixin.particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
	@Accessor("x")
	double velaopt$x();

	@Accessor("y")
	double velaopt$y();

	@Accessor("z")
	double velaopt$z();
}
