package dev.sqwat.vela.render.perf;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import static org.joml.Math.fma;

/** Scalar position transform avoids one temporary vector per submitted text vertex. */
public final class VertexPositions {
    private VertexPositions() {}
    public static VertexConsumer add(VertexConsumer sink, Matrix4fc m, float x, float y, float z) {
        // Preserve custom Matrix4fc implementations supplied by other integrations.
        if (m.getClass() != Matrix4f.class) {
            var p=m.transformPosition(x,y,z,new Vector3f());
            return sink.addVertex(p.x,p.y,p.z);
        }
        int properties=m.properties();
        if ((properties & Matrix4fc.PROPERTY_IDENTITY)!=0) return sink.addVertex(x,y,z);
        if ((properties & Matrix4fc.PROPERTY_TRANSLATION)!=0) return sink.addVertex(x+m.m30(),y+m.m31(),z+m.m32());
        return sink.addVertex(
            fma(m.m00(),x,fma(m.m10(),y,fma(m.m20(),z,m.m30()))),
            fma(m.m01(),x,fma(m.m11(),y,fma(m.m21(),z,m.m31()))),
            fma(m.m02(),x,fma(m.m12(),y,fma(m.m22(),z,m.m32()))));
    }
}
