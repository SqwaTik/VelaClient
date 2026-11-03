package dev.sqwat.vela.mixin.perf;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.PrimitiveTopology;
import dev.sqwat.vela.render.perf.QuadSink;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderQuadMixin implements QuadSink {
    @Shadow private int vertices;
    @Shadow private long vertexPointer;
    @Shadow private int elementsToFill;
    @Shadow private boolean building;
    @Shadow @Final private int vertexSize;
    @Shadow @Final private boolean entityFormat;
    @Shadow @Final private PrimitiveTopology primitiveTopology;
    @Shadow @Final private ByteBufferBuilder buffer;
    @Shadow private void endLastVertex() {}

    @Override public boolean vela$entityQuads() {
        return building && entityFormat && primitiveTopology != PrimitiveTopology.LINES;
    }

    @Override public long vela$reserveVertices(int count) {
        // Vanilla's checks on the vertex before, and its vertex limit.
        endLastVertex();
        if (vertices + count > 16777215) throw new IllegalStateException("Trying to write too many vertices (>16777215) into BufferBuilder");
        long pointer = buffer.reserve(count * vertexSize);
        vertices += count;
        vertexPointer = pointer + (long) (count - 1) * vertexSize;
        elementsToFill = 0;
        return pointer;
    }
}
