package dev.sqwat.vela.render.perf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteOrder;

/**
 * Entity cuboids and baked quads written straight into the vertex buffer. Vanilla's entity-format addVertex reserves,
 * checks and packs per vertex; here a cube or quad reserves once, the normal is packed once per face and each cube corner
 * is transformed once. The bytes are the same as
 * vanilla's: same position transform, same normal rounding, same layout.
 */
public final class FastCubes {
    // Sodium replaces cuboid rendering with its own writer.
    private static final boolean AVAILABLE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN && !FabricLoader.getInstance().isModLoaded("sodium");
    private static final int VERTEX_SIZE = 36;
    private static final int TEXT_VERTEX_SIZE = 28;
    private static final Vector3f scratch = new Vector3f();
    // Cubes have at most 6 faces of 4 vertices, so at most 24 corners.
    private static final float[] transformed = new float[24 * 3];

    private FastCubes() {}

    /** Writes the cube and returns true, or returns false so vanilla does it (other formats, other threads, switched off). */
    public static boolean compile(ModelPart.Polygon[] polygons, CubeLayout layout, PoseStack.Pose pose, VertexConsumer builder, int light, int overlay, int color) {
        if (!AVAILABLE || !(builder instanceof QuadSink sink) || !sink.vela$entityQuads() || !RenderSystem.isOnRenderThread()
            || !dev.sqwat.vela.module.Optimization.fastModelsOn()) return false;
        byte[] cornerOf = layout.cornerOf;
        if (cornerOf.length == 0) return true;
        // Each corner is transformed once, not once per face that shares it; the same call on the same input gives the same floats.
        Matrix4f matrix = pose.pose();
        Vector3f v = scratch;
        float[] in = layout.corners, out = transformed;
        if (in.length > out.length) return false;
        for (int c = 0; c < in.length; c += 3) {
            matrix.transformPosition(in[c], in[c + 1], in[c + 2], v);
            out[c] = v.x;
            out[c + 1] = v.y;
            out[c + 2] = v.z;
        }
        long p = sink.vela$reserveVertices(cornerOf.length);
        int abgr = ARGB.toABGR(color);
        int i = 0;
        for (ModelPart.Polygon polygon : polygons) {
            pose.transformNormal(polygon.normal(), v);
            int normal = (normal(v.x()) & 0xFF) | (normal(v.y()) & 0xFF) << 8 | (normal(v.z()) & 0xFF) << 16;
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                int c = cornerOf[i++] * 3;
                MemoryUtil.memPutFloat(p, out[c]);
                MemoryUtil.memPutFloat(p + 4, out[c + 1]);
                MemoryUtil.memPutFloat(p + 8, out[c + 2]);
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

    /** A baked quad (items, block models on entities), written the same way; false leaves it to vanilla. */
    public static boolean bakedQuad(QuadSink sink, PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        if (!AVAILABLE || !sink.vela$entityQuads() || !RenderSystem.isOnRenderThread() || !dev.sqwat.vela.module.Optimization.fastModelsOn()) return false;
        Matrix4f matrix = pose.pose();
        Vector3f v = scratch;
        pose.transformNormal(quad.direction().getUnitVec3f(), v);
        int normal = (normal(v.x()) & 0xFF) | (normal(v.y()) & 0xFF) << 8 | (normal(v.z()) & 0xFF) << 16;
        int lightEmission = quad.materialInfo().lightEmission();
        int overlay = instance.overlayCoords();
        long p = sink.vela$reserveVertices(4);
        for (int vertex = 0; vertex < 4; vertex++) {
            long packedUv = quad.packedUV(vertex);
            matrix.transformPosition(quad.position(vertex), v);
            MemoryUtil.memPutFloat(p, v.x);
            MemoryUtil.memPutFloat(p + 4, v.y);
            MemoryUtil.memPutFloat(p + 8, v.z);
            MemoryUtil.memPutInt(p + 12, ARGB.toABGR(instance.getColor(vertex)));
            MemoryUtil.memPutFloat(p + 16, UVPair.unpackU(packedUv));
            MemoryUtil.memPutFloat(p + 20, UVPair.unpackV(packedUv));
            MemoryUtil.memPutInt(p + 24, overlay);
            MemoryUtil.memPutInt(p + 28, instance.getLightCoordsWithEmission(vertex, lightEmission));
            MemoryUtil.memPutInt(p + 32, normal);
            p += VERTEX_SIZE;
        }
        return true;
    }

    /**
     * A text glyph quad (name tags, signs, holograms): the four corners as BakedSheetGlyph computes them, written in one
     * reserve instead of four vertices of four element writes each; false leaves it to vanilla.
     */
    public static boolean textQuad(QuadSink sink, Matrix4fc pose, float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy,
                                   float z, int color, float u0, float v0, float u1, float v1, int light) {
        if (!AVAILABLE || !sink.vela$textQuads() || !RenderSystem.isOnRenderThread() || !dev.sqwat.vela.module.Optimization.fastModelsOn()) return false;
        int abgr = ARGB.toABGR(color);
        long p = sink.vela$reserveVertices(4);
        textVertex(p, pose, ax, ay, z, abgr, u0, v0, light);
        textVertex(p + TEXT_VERTEX_SIZE, pose, bx, by, z, abgr, u0, v1, light);
        textVertex(p + 2 * TEXT_VERTEX_SIZE, pose, cx, cy, z, abgr, u1, v1, light);
        textVertex(p + 3 * TEXT_VERTEX_SIZE, pose, dx, dy, z, abgr, u1, v0, light);
        return true;
    }

    private static void textVertex(long p, Matrix4fc pose, float x, float y, float z, int abgr, float u, float v, int light) {
        Vector3f t = pose.transformPosition(x, y, z, scratch);
        MemoryUtil.memPutFloat(p, t.x);
        MemoryUtil.memPutFloat(p + 4, t.y);
        MemoryUtil.memPutFloat(p + 8, t.z);
        MemoryUtil.memPutInt(p + 12, abgr);
        MemoryUtil.memPutFloat(p + 16, u);
        MemoryUtil.memPutFloat(p + 20, v);
        MemoryUtil.memPutInt(p + 24, light);
    }

    private static int normal(float c) {
        return (int) (Mth.clamp(c, -1.0F, 1.0F) * 127.0F);
    }
}
