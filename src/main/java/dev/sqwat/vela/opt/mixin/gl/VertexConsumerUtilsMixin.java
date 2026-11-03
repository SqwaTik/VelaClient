package dev.sqwat.vela.opt.mixin.gl;

import dev.sqwat.vela.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils", remap = false)
public class VertexConsumerUtilsMixin {
	// last consumer that passed the check; a whole model goes to one consumer, so this hits for nearly every cuboid
	@Unique
	private static volatile Object velaopt$lastWriter;

	// tryOf is an interface instanceof plus an interface call on a handful of alternating classes,
	// which keeps evicting HotSpot's per-class type check cache; an identity compare skips both
	@WrapMethod(method = "convertOrLog")
	private static VertexBufferWriter velaopt$cachedConvert(VertexConsumer consumer, Operation<VertexBufferWriter> original) {
		if (consumer == velaopt$lastWriter && Features.WRITER_CACHE.on) {
			return (VertexBufferWriter) consumer;
		}
		VertexBufferWriter writer = original.call(consumer);
		if (writer == consumer) {
			velaopt$lastWriter = consumer;
		}
		return writer;
	}
}
