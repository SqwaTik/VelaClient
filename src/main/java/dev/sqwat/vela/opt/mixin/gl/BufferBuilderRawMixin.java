package dev.sqwat.vela.opt.mixin.gl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.sqwat.vela.opt.gl.RawBuffer;

// what Sodium's VertexBufferWriter does for a BufferBuilder, for running without Sodium
@Mixin(BufferBuilder.class)
public class BufferBuilderRawMixin implements RawBuffer {
	@Shadow
	private int vertices;
	@Shadow
	@Final
	private int vertexSize;
	@Shadow
	private long vertexPointer;
	@Shadow
	@Final
	private ByteBufferBuilder buffer;
	@Shadow
	private int elementsToFill;
	@Shadow
	@Final
	private VertexFormat format;

	@Override
	public boolean velaopt$accepts(VertexFormat format) {
		return format == this.format;
	}

	@Override
	public void velaopt$push(long src, int count) {
		MemoryUtil.memCopy(src, velaopt$reserve(count), (long) count * vertexSize);
	}

	@Override
	public long velaopt$reserve(int count) {
		if (count == 0) {
			return 0L;
		}
		int length = count * vertexSize;
		long dst = buffer.reserve(length);
		vertices += count;
		// the reserved vertices count as complete: the next addVertex must not see a half-filled one
		vertexPointer = dst + length - vertexSize;
		elementsToFill = 0;
		return dst;
	}

	@Override
	public void velaopt$ensureFree(long bytes) {
		ByteBufferBuilderAccessor access = (ByteBufferBuilderAccessor) buffer;
		access.velaopt$ensureCapacity(access.velaopt$writeOffset() + bytes);
	}
}
