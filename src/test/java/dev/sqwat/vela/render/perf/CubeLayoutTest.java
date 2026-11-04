package dev.sqwat.vela.render.perf;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class CubeLayoutTest {
    private static ModelPart.Cube cube(boolean mirror, float grow, EnumSet<Direction> faces) {
        return new ModelPart.Cube(0, 0, -4, -8, -2, 8, 12, 4, grow, grow, grow, mirror, 64, 32, faces);
    }

    @Test void cuboidHasEightCornersForItsVertices() {
        for (boolean mirror : new boolean[]{false, true}) {
            ModelPart.Cube cube = cube(mirror, 0.25F, EnumSet.allOf(Direction.class));
            CubeLayout layout = CubeLayout.of(cube.polygons);
            assertEquals(24, layout.vertexCount());
            assertEquals(8, layout.cornerCount());
            assertCornersMatch(cube.polygons, layout);
        }
    }

    @Test void hiddenFacesAndFlatCubes() {
        ModelPart.Cube twoFaces = cube(false, 0, EnumSet.of(Direction.UP, Direction.NORTH));
        CubeLayout layout = CubeLayout.of(twoFaces.polygons);
        assertEquals(8, layout.vertexCount());
        assertCornersMatch(twoFaces.polygons, layout);

        ModelPart.Cube flat = new ModelPart.Cube(0, 0, 0, 0, 0, 4, 4, 0, 0, 0, 0, false, 16, 16, EnumSet.allOf(Direction.class));
        CubeLayout flatLayout = CubeLayout.of(flat.polygons);
        assertEquals(4, flatLayout.cornerCount());
        assertCornersMatch(flat.polygons, flatLayout);
    }

    private static void assertCornersMatch(ModelPart.Polygon[] polygons, CubeLayout layout) {
        int i = 0;
        for (ModelPart.Polygon polygon : polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                int c = layout.cornerOf[i++] * 3;
                assertEquals(Float.floatToRawIntBits(vertex.worldX()), Float.floatToRawIntBits(layout.corners[c]));
                assertEquals(Float.floatToRawIntBits(vertex.worldY()), Float.floatToRawIntBits(layout.corners[c + 1]));
                assertEquals(Float.floatToRawIntBits(vertex.worldZ()), Float.floatToRawIntBits(layout.corners[c + 2]));
            }
        }
    }
}
