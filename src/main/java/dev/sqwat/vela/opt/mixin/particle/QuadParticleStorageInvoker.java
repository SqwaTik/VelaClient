package dev.sqwat.vela.opt.mixin.particle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.renderer.state.level.QuadParticleRenderState$Storage")
public interface QuadParticleStorageInvoker {
	@Invoker("grow")
	void velaopt$grow();

	@Accessor("capacity")
	int velaopt$capacity();

	@Accessor("floatValues")
	float[] velaopt$floats();

	@Accessor("intValues")
	int[] velaopt$ints();

	@Accessor("currentParticleIndex")
	int velaopt$count();

	@Accessor("currentParticleIndex")
	void velaopt$setCount(int count);
}
