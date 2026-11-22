package dev.sqwat.vela.opt.mixin.gl;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ByteBufferBuilder.class)
public interface ByteBufferBuilderAccessor {
	@Accessor("writeOffset")
	long velaopt$writeOffset();

	@Invoker("ensureCapacity")
	void velaopt$ensureCapacity(long requiredCapacity);
}
