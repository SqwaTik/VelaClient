package dev.sqwat.vela.mixin.perf;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.sqwat.vela.render.perf.VertexPositions;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexConsumer.class)
public interface VertexPositionsMixin {
    /**
     * @author Vela
     * @reason Preserve position arithmetic without allocating a vector for every vertex.
     */
    @Overwrite
    default VertexConsumer addVertex(Matrix4fc pose, float x, float y, float z) {
        return VertexPositions.add((VertexConsumer)this,pose,x,y,z);
    }
}
