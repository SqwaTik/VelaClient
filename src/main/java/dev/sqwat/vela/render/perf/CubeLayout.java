package dev.sqwat.vela.render.perf;

import net.minecraft.client.model.geom.ModelPart;

import java.util.Arrays;

/** Where a cube's vertices sit: its distinct corners (a cuboid has 8 for its 24 vertices) and each vertex's corner. */
public final class CubeLayout {
    /** World-space x, y, z of each distinct corner. */
    final float[] corners;
    /** The corner of each vertex, in polygon order. */
    final byte[] cornerOf;

    private CubeLayout(float[] corners, byte[] cornerOf) {
        this.corners = corners;
        this.cornerOf = cornerOf;
    }

    public int cornerCount() {
        return corners.length / 3;
    }

    public int vertexCount() {
        return cornerOf.length;
    }

    public static CubeLayout of(ModelPart.Polygon[] polygons) {
        int count = 0;
        for (ModelPart.Polygon polygon : polygons) count += polygon.vertices().length;
        float[] corners = new float[count * 3];
        byte[] cornerOf = new byte[count];
        int distinct = 0, i = 0;
        for (ModelPart.Polygon polygon : polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                float x = vertex.worldX(), y = vertex.worldY(), z = vertex.worldZ();
                int corner = 0;
                // Bitwise equality, so the transform sees exactly the input vanilla gives it.
                while (corner < distinct && !(same(corners[corner * 3], x) && same(corners[corner * 3 + 1], y) && same(corners[corner * 3 + 2], z))) corner++;
                if (corner == distinct) {
                    corners[distinct * 3] = x;
                    corners[distinct * 3 + 1] = y;
                    corners[distinct * 3 + 2] = z;
                    distinct++;
                }
                cornerOf[i++] = (byte) corner;
            }
        }
        return new CubeLayout(Arrays.copyOf(corners, distinct * 3), cornerOf);
    }

    private static boolean same(float a, float b) {
        return Float.floatToRawIntBits(a) == Float.floatToRawIntBits(b);
    }
}
