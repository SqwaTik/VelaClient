package dev.sqwat.vela.render.perf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteOrder;

/**
 * Entity cuboids written straight into the vertex buffer. Vanilla's entity-format addVertex reserves, checks and packs
 * per vertex; here a cube reserves once, and color and normal are packed once per face. The bytes are the same as
 * vanilla's: same position transform, same normal rounding, same layout.
 */
public final class FastCubes {
    // Sodium replaces cuboid rendering with its own writer.
    private static final boolean AVAILABLE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN && !FabricLoader.getInstance().isModLoaded("sodium");
    private static final int VERTEX_SIZE = 36;
    private static final Vector3f scratch = new Vector3f();

    private FastCubes() {}

    /** Writes the cube and returns true, or returns false so vanilla does it (other formats, other threads, switched off). */
    public static boolean compile(ModelPart.Polygon[] polygons, PoseStack.Pose pose, VertexConsumer builder, int light, int overlay, int color) {
        if (!AVAILABLE || !(builder instanceof QuadSink sink) || !sink.vela$entityQuads() || !RenderSystem.isOnRenderThread()
            || !dev.sqwat.vela.module.Optimization.fastModelsOn()) return false;
        int count = 0;
        for (ModelPart.Polygon polygon : polygons) count += polygon.vertices().length;
        if (count == 0) return true;
        long p = sink.vela$reserveVertices(count);
        Matrix4f matrix = pose.pose();
        Vector3f v = scratch;
        int abgr = ARGB.toABGR(color);
        for (ModelPart.Polygon polygon : polygons) {
            pose.transformNormal(polygon.normal(), v);
            int normal = (normal(v.x()) & 0xFF) | (normal(v.y()) & 0xFF) << 8 | (normal(v.z()) & 0xFF) << 16;
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                matrix.transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), v);
                MemoryUtil.memPutFloat(p, v.x);
                MemoryUtil.memPutFloat(p + 4, v.y);
                MemoryUtil.memPutFloat(p + 8, v.z);
                MemoryUtil.memPutInt(p + 12, abgr);
                MemoryUtil.memPutFloat(p + 16, vertex.u());
                MemoryUtil.memPutFloat(p + 20, vertex.v());
                MemoryUtil.memPutInt(p + 24, overlay);
                MemoryUtil.memPutInt(p + 28, light);
                MemoryUtil.memPutInt(p + 32, normal);
                p += VERTEX_SIZE;
            }
        }
        return true;
    }

    private static int normal(float c) {
        return (int) (Mth.clamp(c, -1.0F, 1.0F) * 127.0F);
    }
}
