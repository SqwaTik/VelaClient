package dev.sqwat.vela.mixin.perf;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.sqwat.vela.render.perf.FastCubes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import com.mojang.blaze3d.PrimitiveTopology;
import dev.sqwat.vela.render.perf.QuadSink;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderQuadMixin implements QuadSink, VertexConsumer {
    @Shadow private int vertices;
    @Shadow private long vertexPointer;
    @Shadow private int elementsToFill;
    @Shadow private boolean building;
    @Shadow @Final private int vertexSize;
    @Shadow @Final private boolean entityFormat;
    @Shadow @Final private PrimitiveTopology primitiveTopology;
    @Shadow @Final private VertexFormat format;
    @Shadow @Final private ByteBufferBuilder buffer;
    @Shadow private void endLastVertex() {}

    @Override public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        if (!FastCubes.bakedQuad(this, pose, quad, instance)) VertexConsumer.super.putBakedQuad(pose, quad, instance);
    }

    @Override public boolean vela$entityQuads() {
        return building && entityFormat && primitiveTopology != PrimitiveTopology.LINES;
    }

    @Override public boolean vela$textQuads() {
        return building && format == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP && vertexSize == 28 && primitiveTopology != PrimitiveTopology.LINES;
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
